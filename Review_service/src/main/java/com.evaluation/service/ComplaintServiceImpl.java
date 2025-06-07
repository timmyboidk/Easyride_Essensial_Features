package com.evaluation.service;

import com.evaluation.dto.ComplaintDTO;
import com.evaluation.mapper.ComplaintMapper;
import com.evaluation.model.Complaint;
import com.evaluation.repository.ComplaintRepository;
import com.evaluation.util.Constants;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 投诉服务实现类，处理投诉相关的业务逻辑
 */
@Service
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintMapper complaintMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final SensitiveWordService sensitiveWordService = null;

    @Autowired
    public ComplaintServiceImpl(ComplaintRepository complaintRepository,
                                ComplaintMapper complaintMapper,
                                FileStorageService fileStorageService, RocketMQTemplate rocketMQTemplate,
            /*UserClient userClient,*/ SensitiveWordService sensitiveWordService) {
        this.complaintRepository = complaintRepository;
        this.complaintMapper = complaintMapper;
        this.rocketMQTemplate = rocketMQTemplate;
        this.sensitiveWordService = sensitiveWordService;
    }

    /**
     * 提交新的投诉，处理上传的证据并发送消息到RocketMQ
     *
     * @param complaintDTO 投诉数据传输对象
     * @return 创建后的投诉数据传输对象
     */
    @Override
    @Transactional
    public ComplaintDTO fileComplaint(ComplaintDTO complaintDTO, List<MultipartFile> evidenceFiles) {
        // ... (User validation logic - should be uncommented and verified)
        /*
        try {
            UserDTO complainant = userClient.getUserById(complaintDTO.getComplainantId());
            if (complainant == null) throw new BadRequestException("Complainant user not found.");
             // If complaint is about an evaluation, check that evaluatee makes sense
        } catch (Exception e) {
            log.error("Error validating complainant user: {}", e.getMessage());
            throw new BadRequestException("Error validating user: " + e.getMessage());
        }
        */

        // Check if the related evaluation exists
        if (complaintDTO.getEvaluationId() != null) {
            evaluationRepository.findById(complaintDTO.getEvaluationId())
                    .orElseThrow(() -> new ResourceNotFoundException("投诉关联的评价 (ID: " + complaintDTO.getEvaluationId() + ") 未找到。"));
        } else {
            log.warn("Filing a complaint not directly linked to an evaluation by complainant {}.", complaintDTO.getComplainantId());
            // Allow complaints not tied to a specific evaluation if design permits
        }


        // Sensitive word check for reason
        if (complaintDTO.getReason() != null && sensitiveWordService.containsSensitiveWords(complaintDTO.getReason())) {
            log.warn("Complaint reason from {} contains sensitive words. Reason: '{}'", complaintDTO.getComplainantId(), complaintDTO.getReason());
            // throw new BadRequestException("投诉原因包含不当内容，请修改。");
            complaintDTO.setReason(sensitiveWordService.filterContent(complaintDTO.getReason()));
        }

        Complaint complaint = complaintMapper.toEntity(complaintDTO);
        // ... (file storage logic for evidenceFiles - existing logic seems fine) ...
        complaint.setStatus("PENDING_REVIEW"); // Default status for new complaints
        complaint.setComplaintTime(LocalDateTime.now());

        Complaint savedComplaint = complaintRepository.save(complaint);
        log.info("Complaint (ID: {}) filed successfully by complainant {}.", savedComplaint.getId(), savedComplaint.getComplainantId());

        // Update evaluation's complaint status if linked
        if (savedComplaint.getEvaluationId() != null) {
            evaluationRepository.findById(savedComplaint.getEvaluationId()).ifPresent(eval -> {
                eval.setComplaintStatus("FILED"); // Or link to complaint ID
                eval.setLastUpdated(LocalDateTime.now());
                evaluationRepository.save(eval);
            });
        }

        // Send MQ message
        try {
            rocketMQTemplate.convertAndSend("complaint-topic:COMPLAINT_FILED", savedComplaint);
            log.info("COMPLAINT_FILED event sent for complaint ID: {}", savedComplaint.getId());
        } catch (Exception e) {
            log.error("Failed to send COMPLAINT_FILED event for complaint ID {}: ", savedComplaint.getId(), e);
        }

        return complaintMapper.toDto(savedComplaint);
    }

    /**
     * 根据评价ID获取所有相关投诉
     *
     * @param evaluationId 评价ID
     * @return 投诉列表
     */
    @Override
    public List<ComplaintDTO> getComplaintsByEvaluation(Long evaluationId) {
        List<Complaint> complaints = complaintRepository.findByEvaluationId(evaluationId);
        return complaints.stream()
                .map(complaintMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据申诉者ID获取所有投诉
     *
     * @param complainantId 申诉者ID
     * @return 投诉列表
     */
    @Override
    public List<ComplaintDTO> getComplaintsByComplainant(Long complainantId) {
        List<Complaint> complaints = complaintRepository.findByComplainantId(complainantId);
        return complaints.stream()
                .map(complaintMapper::toDTO)
                .collect(Collectors.toList());
    }
}
