package com.evaluation.service;

import com.evaluation.dto.ComplaintDTO;
import com.evaluation.exception.BadRequestException;
import com.evaluation.exception.ResourceNotFoundException;
import com.evaluation.mapper.ComplaintMapper;
import com.evaluation.model.Complaint;
import com.evaluation.repository.ComplaintRepository;
import com.evaluation.repository.EvaluationRepository;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComplaintServiceImpl implements ComplaintService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintServiceImpl.class);

    private final ComplaintRepository complaintRepository;
    private final ComplaintMapper complaintMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final SensitiveWordService sensitiveWordService;
    private final EvaluationRepository evaluationRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public ComplaintServiceImpl(ComplaintRepository complaintRepository,
                                ComplaintMapper complaintMapper,
                                FileStorageService fileStorageService,
                                RocketMQTemplate rocketMQTemplate,
                                SensitiveWordService sensitiveWordService,
                                EvaluationRepository evaluationRepository) {
        this.complaintRepository = complaintRepository;
        this.complaintMapper = complaintMapper;
        this.fileStorageService = fileStorageService;
        this.rocketMQTemplate = rocketMQTemplate;
        this.sensitiveWordService = sensitiveWordService;
        this.evaluationRepository = evaluationRepository;
    }

    @Override
    @Transactional
    public ComplaintDTO fileComplaint(ComplaintDTO complaintDTO, List<MultipartFile> evidenceFiles) {
        if (complaintDTO.getEvaluationId() != null) {
            evaluationRepository.findById(complaintDTO.getEvaluationId())
                    .orElseThrow(() -> new ResourceNotFoundException("投诉关联的评价 (ID: " + complaintDTO.getEvaluationId() + ") 未找到。"));
        } else {
            log.warn("Filing a complaint not directly linked to an evaluation by complainant {}.", complaintDTO.getComplainantId());
        }

        if (complaintDTO.getReason() != null && sensitiveWordService.containsSensitiveWords(complaintDTO.getReason())) {
            log.warn("Complaint reason from {} contains sensitive words. Reason: '{}'", complaintDTO.getComplainantId(), complaintDTO.getReason());
            complaintDTO.setReason(sensitiveWordService.filterContent(complaintDTO.getReason()));
        }

        Complaint complaint = complaintMapper.toEntity(complaintDTO);

        if (evidenceFiles != null && !evidenceFiles.isEmpty()) {
            List<String> evidencePaths = fileStorageService.storeFiles(evidenceFiles.toArray(new MultipartFile[0]));
            complaint.setEvidencePaths(evidencePaths);
        }

        complaint.setStatus("PENDING_REVIEW");

        Complaint savedComplaint = complaintRepository.save(complaint);
        log.info("Complaint (ID: {}) filed successfully by complainant {}.", savedComplaint.getId(), savedComplaint.getComplainantId());

        if (savedComplaint.getEvaluationId() != null) {
            evaluationRepository.findById(savedComplaint.getEvaluationId()).ifPresent(eval -> {
                eval.setComplaintStatus("FILED");
                eval.setUpdatedAt(LocalDateTime.now());
                evaluationRepository.save(eval);
            });
        }

        try {
            rocketMQTemplate.convertAndSend("complaint-topic:COMPLAINT_FILED", savedComplaint);
            log.info("COMPLAINT_FILED event sent for complaint ID: {}", savedComplaint.getId());
        } catch (Exception e) {
            log.error("Failed to send COMPLAINT_FILED event for complaint ID {}: ", savedComplaint.getId(), e);
        }

        return complaintMapper.toDTO(savedComplaint);
    }

    @Override
    public List<ComplaintDTO> getComplaintsByEvaluation(Long evaluationId) {
        List<Complaint> complaints = complaintRepository.findByEvaluationId(evaluationId);
        return complaints.stream()
                .map(complaintMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ComplaintDTO> getComplaintsByComplainant(Long complainantId) {
        List<Complaint> complaints = complaintRepository.findByComplainantId(complainantId);
        return complaints.stream()
                .map(complaintMapper::toDTO)
                .collect(Collectors.toList());
    }
    // Other methods for admin operations need to be implemented
    @Override
    public Page<ComplaintDTO> getAllComplaintsForAdmin(Pageable pageable, String status) {
        // Implementation needed
        return Page.empty();
    }

    @Override
    public ComplaintDTO adminUpdateComplaintStatus(Long complaintId, String newStatus, String adminNotes, Long adminId) {
        // Implementation needed
        return null;
    }
}