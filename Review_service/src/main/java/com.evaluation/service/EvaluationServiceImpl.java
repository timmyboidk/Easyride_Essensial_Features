package com.evaluation.service;

import com.evaluation.client.UserClient;
import com.evaluation.dto.EvaluationDTO;
import com.evaluation.exception.ResourceNotFoundException;
import com.evaluation.mapper.EvaluationMapper;
import com.evaluation.model.Evaluation;
import com.evaluation.model.Tag;
import com.evaluation.repository.EvaluationRepository;
import com.evaluation.repository.TagRepository;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import com.evaluation.exception.BadRequestException; // Assuming you have this

/**
 * 评价服务实现类，处理评价相关的业务逻辑
 */
@Service
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final TagRepository tagRepository;
    private final EvaluationMapper evaluationMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final UserClient userClient; // 用于远程调用用户服务
    private final SensitiveWordService sensitiveWordService;

    @Autowired
    public EvaluationServiceImpl(EvaluationRepository evaluationRepository,
                                 TagRepository tagRepository,
                                 EvaluationMapper evaluationMapper,
                                 RocketMQTemplate rocketMQTemplate,
                                 UserClient userClient,SensitiveWordService sensitiveWordService) {
        this.evaluationRepository = evaluationRepository;
        this.tagRepository = tagRepository;
        this.evaluationMapper = evaluationMapper;
        this.rocketMQTemplate = rocketMQTemplate;
        this.userClient = userClient;
        this.sensitiveWordService = sensitiveWordService;
    }

    /**
     * 创建新的评价，处理标签并发送消息到RocketMQ
     *
     * @param evaluationDTO 评价数据传输对象
     * @return 创建后的评价数据传输对象
     */
    @Override
    @Transactional
    public EvaluationDTO createEvaluation(EvaluationDTO evaluationDTO) {
        log.info("Attempting to create evaluation from evaluator {} to evaluatee {}", evaluationDTO.getEvaluatorId(), evaluationDTO.getEvaluateeId());
        // Validate user existence (uncomment when UserClient is confirmed working)
        /*
        try {
            UserDTO evaluator = userClient.getUserById(evaluationDTO.getEvaluatorId());
            UserDTO evaluatee = userClient.getUserById(evaluationDTO.getEvaluateeId());
            if (evaluator == null || evaluatee == null) {
                throw new BadRequestException("Evaluator or Evaluatee not found.");
            }
            log.debug("Evaluator and Evaluatee validated via UserClient.");
        } catch (Exception e) {
            log.error("Error validating users via UserClient: {}", e.getMessage());
            throw new BadRequestException("Error validating user information: " + e.getMessage());
        }
        */

        // Check for sensitive words
        if (evaluationDTO.getComment() != null && sensitiveWordService.containsSensitiveWords(evaluationDTO.getComment())) {
            log.warn("Evaluation comment from {} contains sensitive words. Comment: '{}'", evaluationDTO.getEvaluatorId(), evaluationDTO.getComment());
            // Option 1: Reject the evaluation
            // throw new BadRequestException("评价包含不当内容，请修改。");
            // Option 2: Filter and proceed (or flag for moderation)
            evaluationDTO.setComment(sensitiveWordService.filterContent(evaluationDTO.getComment()));
            // Add a flag for moderation if needed on the Evaluation entity
            // evaluation.setNeedsModeration(true);
        }

        Evaluation evaluation = evaluationMapper.toEntity(evaluationDTO);
        if (evaluationDTO.getTags() != null && !evaluationDTO.getTags().isEmpty()) {
            Set<Tag> managedTags = evaluationDTO.getTags().stream()
                    .map(tagDTO -> tagService.findOrCreateTag(tagDTO.getName()))
                    .collect(Collectors.toSet());
            evaluation.setTags(managedTags);
        }
        evaluation.setEvaluationTime(LocalDateTime.now());
        evaluation.setStatus(EvaluationStatus.ACTIVE); // New: Default status
        evaluation.setComplaintStatus("NONE"); // Existing

        Evaluation savedEvaluation = evaluationRepository.save(evaluation);
        log.info("Evaluation (ID: {}) created successfully.", savedEvaluation.getId());

        // Send MQ message
        try {
            rocketMQTemplate.convertAndSend("evaluation-topic:EVALUATION_CREATED", savedEvaluation);
            log.info("EVALUATION_CREATED event sent for evaluation ID: {}", savedEvaluation.getId());
        } catch (Exception e) {
            log.error("Failed to send EVALUATION_CREATED event for evaluation ID {}: ", savedEvaluation.getId(), e);
        }
        return evaluationMapper.toDto(savedEvaluation);
    }
    /**
     * 根据被评价者ID获取所有评价
     *
     * @param evaluateeId 被评价者ID
     * @return 评价列表
     */
    @Override
    public List<EvaluationDTO> getEvaluationsByEvaluatee(Long evaluateeId) {
        // 检查被评价者是否存在（可选，根据业务需求）
        // UserDTO user = userClient.getUserById(evaluateeId);
        // if (user == null) {
        //     throw new ResourceNotFoundException("被评价者不存在，ID: " + evaluateeId);
        // }

        List<Evaluation> evaluations = evaluationRepository.findByEvaluateeId(evaluateeId);
        return evaluations.stream()
                .map(evaluationMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据评价者ID获取所有评价
     *
     * @param evaluatorId 评价者ID
     * @return 评价列表
     */
    @Override
    public List<EvaluationDTO> getEvaluationsByEvaluator(Long evaluatorId) {
        // 检查评价者是否存在（可选，根据业务需求）
        // UserDTO user = userClient.getUserById(evaluatorId);
        // if (user == null) {
        //     throw new ResourceNotFoundException("评价者不存在，ID: " + evaluatorId);
        // }

        List<Evaluation> evaluations = evaluationRepository.findByEvaluatorId(evaluatorId);
        return evaluations.stream()
                .map(evaluationMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<EvaluationDTO> getAllEvaluationsForAdmin(Pageable pageable, String statusFilter) {
        log.debug("Admin fetching evaluations. Page: {}, Size: {}, StatusFilter: {}", pageable.getPageNumber(), pageable.getPageSize(), statusFilter);
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
    public EvaluationDTO adminUpdateEvaluationStatus(Long evaluationId, String newStatusStr, String adminNotes, Long adminId) {
        log.info("Admin {} updating status of evaluation {} to {} with notes: {}", adminId, evaluationId, newStatusStr, adminNotes);
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation not found with id: " + evaluationId));

        try {
            EvaluationStatus newStatus = EvaluationStatus.valueOf(newStatusStr.toUpperCase());
            evaluation.setStatus(newStatus);
            evaluation.setAdminNotes(adminNotes);
            evaluation.setReviewedByAdminId(adminId);
            evaluation.setReviewTime(LocalDateTime.now());
            evaluation.setLastUpdated(LocalDateTime.now());
            Evaluation updatedEvaluation = evaluationRepository.save(evaluation);

            // Optionally, publish an EVALUATION_STATUS_UPDATED_BY_ADMIN event
            // rocketMQTemplate.convertAndSend("evaluation-topic:EVALUATION_ADMIN_UPDATED", updatedEvaluation);

            return evaluationMapper.toDto(updatedEvaluation);
        } catch (IllegalArgumentException e) {
            log.error("Invalid new status string provided by admin: {}", newStatusStr);
            throw new BadRequestException("无效的评价状态: " + newStatusStr);
        }
    }

    @Autowired // Add this
    private ReviewWindowRepository reviewWindowRepository; // Or ReviewWindowService

    @Override
    @Transactional
    public EvaluationDTO createEvaluation(EvaluationDTO evaluationDTO) {
        log.info("Attempting to create evaluation from evaluator {} to evaluatee {} for order {}",
                evaluationDTO.getEvaluatorId(), evaluationDTO.getEvaluateeId(), evaluationDTO.getOrderId());

        // Check review window (Assuming evaluationDTO has orderId)
        if (evaluationDTO.getOrderId() == null) {
            throw new BadRequestException("Order ID is required to submit an evaluation.");
        }
        ReviewWindow window = reviewWindowRepository.findById(evaluationDTO.getOrderId())
                .orElseThrow(() -> new BadRequestException("Review window not open or order not found for evaluation."));

        // Determine if evaluator is passenger or driver and check if they can review
        boolean canReview = false;
        if (evaluationDTO.getEvaluatorId().equals(window.getPassengerId()) && window.isPassengerCanReview() && !window.isPassengerReviewed()) {
            canReview = true;
        } else if (evaluationDTO.getEvaluatorId().equals(window.getDriverId()) && window.isDriverCanReview() && !window.isDriverReviewed()) {
            canReview = true;
        }

        if (!canReview || LocalDateTime.now().isAfter(window.getWindowCloseTime())) {
            log.warn("User {} cannot submit review for order {}. CanReview: {}, PastCloseTime: {}",
                    evaluationDTO.getEvaluatorId(), evaluationDTO.getOrderId(), canReview, LocalDateTime.now().isAfter(window.getWindowCloseTime()));
            throw new BadRequestException("无法提交评价：评价窗口已关闭或您已评价过。");
        }

        // ... (existing user validation, sensitive word filtering, save logic) ...

        Evaluation savedEvaluation = evaluationRepository.save(evaluation); // After all checks

        // Update review window after successful submission
        if (evaluationDTO.getEvaluatorId().equals(window.getPassengerId())) {
            window.setPassengerReviewed(true);
        } else if (evaluationDTO.getEvaluatorId().equals(window.getDriverId())) {
            window.setDriverReviewed(true);
        }
        reviewWindowRepository.save(window);
        log.info("Review window updated for order {}.", evaluationDTO.getOrderId());

        // ... (existing MQ message sending) ...
        return evaluationMapper.toDto(savedEvaluation);
    }
}
