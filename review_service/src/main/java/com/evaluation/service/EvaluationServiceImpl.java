package com.evaluation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evaluation.dto.EvaluationDTO;
import com.evaluation.exception.BadRequestException;
import com.evaluation.exception.ResourceNotFoundException;
import com.evaluation.mapper.EvaluationDtoMapper;
import com.evaluation.model.Evaluation;
import com.evaluation.model.EvaluationStatus;
import com.evaluation.model.ReviewWindow;
import com.evaluation.model.Tag;
import com.evaluation.repository.EvaluationMapper;
import com.evaluation.repository.ReviewWindowMapper;
import com.evaluation.repository.TagMapper;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评价服务实现类，处理评价相关的业务逻辑
 */
@Service
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EvaluationServiceImpl.class);

    private final EvaluationMapper evaluationMapper;
    private final TagMapper tagMapper;
    private final EvaluationDtoMapper evaluationDtoMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final SensitiveWordService sensitiveWordService;
    private final ReviewWindowMapper reviewWindowMapper;

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

        ReviewWindow window = reviewWindowMapper.selectById(evaluationDTO.getOrderId());
        if (window == null) {
            throw new BadRequestException("Review window not open or order not found for evaluation.");
        }

        Evaluation evaluation = evaluationDtoMapper.toEntity(evaluationDTO);
        if (evaluationDTO.getTags() != null && !evaluationDTO.getTags().isEmpty()) {
            // Ensure tags exist in the database
            evaluationDTO.getTags().forEach(tagName -> {
                Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, tagName));
                if (tag == null) {
                    tag = new Tag();
                    tag.setName(tagName);
                    tagMapper.insert(tag);
                }
            });

            // Set string representation of tags
            evaluation.setTagsString(String.join(",", evaluationDTO.getTags()));
        }

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

        evaluation.setReviewTime(LocalDateTime.now());
        evaluation.setStatus(EvaluationStatus.ACTIVE);
        evaluation.setComplaintStatus("NONE");

        evaluationMapper.insert(evaluation);
        Evaluation savedEvaluation = evaluation;
        log.info("Evaluation (ID: {}) created successfully.", savedEvaluation.getId());

        // Update review window
        if (evaluationDTO.getEvaluatorId().equals(window.getPassengerId())) {
            window.setPassengerReviewed(true);
        } else if (evaluationDTO.getEvaluatorId().equals(window.getDriverId())) {
            window.setDriverReviewed(true);
        }
        reviewWindowMapper.updateById(window);

        // Send MQ message
        try {
            rocketMQTemplate.convertAndSend("evaluation-topic:EVALUATION_CREATED", savedEvaluation);
        } catch (Exception e) {
            log.error("Failed to send EVALUATION_CREATED event for evaluation ID {}: ", savedEvaluation.getId(), e);
        }
        return evaluationDtoMapper.toDto(savedEvaluation);
    }

    @Override
    public List<EvaluationDTO> getEvaluationsByEvaluatee(Long evaluateeId) {
        List<Evaluation> evaluations = evaluationMapper.selectList(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getEvaluateeId, evaluateeId));
        return evaluations.stream()
                .map(evaluationDtoMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<EvaluationDTO> getEvaluationsByEvaluator(Long evaluatorId) {
        List<Evaluation> evaluations = evaluationMapper.selectList(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getEvaluatorId, evaluatorId));
        return evaluations.stream()
                .map(evaluationDtoMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<EvaluationDTO> getAllEvaluationsForAdmin(Pageable pageable, String statusFilter) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Evaluation> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pageable.getPageNumber() + 1, pageable.getPageSize());
        LambdaQueryWrapper<Evaluation> queryWrapper = new LambdaQueryWrapper<>();
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                EvaluationStatus status = EvaluationStatus.valueOf(statusFilter.toUpperCase());
                queryWrapper.eq(Evaluation::getStatus, status);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status filter '{}', fetching all.", statusFilter);
            }
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Evaluation> evaluationsPage = evaluationMapper
                .selectPage(page, queryWrapper);
        List<EvaluationDTO> dtos = evaluationsPage.getRecords().stream()
                .map(evaluationDtoMapper::toDto)
                .collect(Collectors.toList());

        // Note: Returning MyBatis-Plus Page or adapting to Spring Data Page.
        // For now, let's keep it simple or use a custom wrapper if needed.
        // Actually, the interface likely returns org.springframework.data.domain.Page.
        return new PageImpl<>(dtos, pageable, evaluationsPage.getTotal());
    }

    @Override
    @Transactional
    public EvaluationDTO adminUpdateEvaluationStatus(Long evaluationId, String newStatusStr, String adminNotes,
            Long adminId) {
        Evaluation evaluation = evaluationMapper.selectById(evaluationId);
        if (evaluation == null) {
            throw new ResourceNotFoundException("Evaluation not found with id: " + evaluationId);
        }

        try {
            EvaluationStatus newStatus = EvaluationStatus.valueOf(newStatusStr.toUpperCase());
            evaluation.setStatus(newStatus);
            evaluation.setAdminNotes(adminNotes);
            evaluation.setReviewedByAdminId(adminId);
            evaluation.setUpdatedAt(LocalDateTime.now());
            evaluationMapper.updateById(evaluation);
            return evaluationDtoMapper.toDto(evaluation);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("无效的评价状态: " + newStatusStr);
        }
    }
}
