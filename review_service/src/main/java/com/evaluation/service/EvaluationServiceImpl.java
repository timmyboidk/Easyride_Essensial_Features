package com.evaluation.service;

import com.evaluation.dto.EvaluationDTO;
import com.evaluation.exception.BadRequestException;
import com.evaluation.exception.ResourceNotFoundException;
import com.evaluation.mapper.EvaluationMapper;
import com.evaluation.model.Evaluation;
import com.evaluation.model.EvaluationStatus;
import com.evaluation.model.ReviewWindow;
import com.evaluation.model.Tag;
import com.evaluation.repository.EvaluationRepository;
import com.evaluation.repository.ReviewWindowRepository;
import com.evaluation.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评价服务实现类，处理评价相关的业务逻辑
 */
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EvaluationServiceImpl.class);

    private final EvaluationRepository evaluationRepository;
    private final TagRepository tagRepository;
    private final EvaluationMapper evaluationMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final SensitiveWordService sensitiveWordService;
    private final ReviewWindowRepository reviewWindowRepository;

    /**
     * 创建新的评价，处理标签并发送消息到RocketMQ
     *
     * @param evaluationDTO 评价数据传输对象
     * @return 创建后的评价数据传输对象
     */
    @Override
    @Transactional
    public EvaluationDTO createEvaluation(EvaluationDTO evaluationDTO) {
        log.info("Attempting to create evaluation from evaluator {} to evaluatee {} for order {}",
                evaluationDTO.getEvaluatorId(), evaluationDTO.getEvaluateeId(), evaluationDTO.getOrderId());

        if (evaluationDTO.getOrderId() == null) {
            throw new BadRequestException("Order ID is required to submit an evaluation.");
        }

        ReviewWindow window = reviewWindowRepository.findById(evaluationDTO.getOrderId())
                .orElseThrow(
                        () -> new BadRequestException("Review window not open or order not found for evaluation."));

        boolean canReview = false;
        if (evaluationDTO.getEvaluatorId().equals(window.getPassengerId()) && window.isPassengerCanReview()
                && !window.isPassengerReviewed()) {
            canReview = true;
        } else if (evaluationDTO.getEvaluatorId().equals(window.getDriverId()) && window.isDriverCanReview()
                && !window.isDriverReviewed()) {
            canReview = true;
        }

        if (!canReview
                || (window.getWindowCloseTime() != null && LocalDateTime.now().isAfter(window.getWindowCloseTime()))) {
            log.warn("User {} cannot submit review for order {}. CanReview: {}",
                    evaluationDTO.getEvaluatorId(), evaluationDTO.getOrderId(), canReview);
            throw new BadRequestException("无法提交评价：评价窗口已关闭或您已评价过。");
        }

        // Check for sensitive words
        if (evaluationDTO.getComment() != null
                && sensitiveWordService.containsSensitiveWords(evaluationDTO.getComment())) {
            log.warn("Evaluation comment from {} contains sensitive words.", evaluationDTO.getEvaluatorId());
            evaluationDTO.setComment(sensitiveWordService.filterContent(evaluationDTO.getComment()));
        }

        Evaluation evaluation = evaluationMapper.toEntity(evaluationDTO);
        if (evaluationDTO.getTags() != null && !evaluationDTO.getTags().isEmpty()) {
            Set<Tag> managedTags = evaluationDTO.getTags().stream()
                    .map(tagName -> tagRepository.findByName(tagName)
                            .orElseGet(() -> {
                                Tag newTag = new Tag();
                                newTag.setName(tagName);
                                return tagRepository.save(newTag);
                            }))
                    .collect(Collectors.toSet());
            evaluation.setTags(managedTags);

            // Also set string representation if needed
            evaluation.setTagsString(String.join(",", evaluationDTO.getTags()));
        }

        evaluation.setReviewTime(LocalDateTime.now());
        evaluation.setStatus(EvaluationStatus.ACTIVE);
        evaluation.setComplaintStatus("NONE");

        Evaluation savedEvaluation = evaluationRepository.save(evaluation);
        log.info("Evaluation (ID: {}) created successfully.", savedEvaluation.getId());

        // Update review window
        if (evaluationDTO.getEvaluatorId().equals(window.getPassengerId())) {
            window.setPassengerReviewed(true);
        } else if (evaluationDTO.getEvaluatorId().equals(window.getDriverId())) {
            window.setDriverReviewed(true);
        }
        reviewWindowRepository.save(window);

        // Send MQ message
        try {
            rocketMQTemplate.convertAndSend("evaluation-topic:EVALUATION_CREATED", savedEvaluation);
        } catch (Exception e) {
            log.error("Failed to send EVALUATION_CREATED event for evaluation ID {}: ", savedEvaluation.getId(), e);
        }
        return evaluationMapper.toDto(savedEvaluation);
    }

    @Override
    public List<EvaluationDTO> getEvaluationsByEvaluatee(Long evaluateeId) {
        List<Evaluation> evaluations = evaluationRepository.findByEvaluateeId(evaluateeId);
        return evaluations.stream()
                .map(evaluationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<EvaluationDTO> getEvaluationsByEvaluator(Long evaluatorId) {
        List<Evaluation> evaluations = evaluationRepository.findByEvaluatorId(evaluatorId);
        return evaluations.stream()
                .map(evaluationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<EvaluationDTO> getAllEvaluationsForAdmin(Pageable pageable, String statusFilter) {
        Page<Evaluation> evaluationsPage;
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                EvaluationStatus status = EvaluationStatus.valueOf(statusFilter.toUpperCase());
                evaluationsPage = evaluationRepository.findByStatus(status, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status filter '{}', fetching all.", statusFilter);
                evaluationsPage = evaluationRepository.findAll(pageable);
            }
        } else {
            evaluationsPage = evaluationRepository.findAll(pageable);
        }
        return evaluationsPage.map(evaluationMapper::toDto);
    }

    @Override
    @Transactional
    public EvaluationDTO adminUpdateEvaluationStatus(Long evaluationId, String newStatusStr, String adminNotes,
            Long adminId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found with id: " + evaluationId));

        try {
            EvaluationStatus newStatus = EvaluationStatus.valueOf(newStatusStr.toUpperCase());
            evaluation.setStatus(newStatus);
            evaluation.setAdminNotes(adminNotes);
            evaluation.setReviewedByAdminId(adminId);
            // evaluation.setLastUpdated(LocalDateTime.now()); // Assuming this is handled
            // by PreUpdate
            Evaluation updatedEvaluation = evaluationRepository.save(evaluation);
            return evaluationMapper.toDto(updatedEvaluation);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("无效的评价状态: " + newStatusStr);
        }
    }
}
