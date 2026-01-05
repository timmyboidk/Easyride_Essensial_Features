package com.evaluation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evaluation.dto.ComplaintDTO;
import com.evaluation.exception.ResourceNotFoundException;
import com.evaluation.mapper.ComplaintDtoMapper;
import com.evaluation.model.Complaint;
import com.evaluation.model.Evaluation;
import com.evaluation.repository.ComplaintMapper;
import com.evaluation.repository.EvaluationMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

    private final ComplaintMapper complaintMapper;
    private final ComplaintDtoMapper complaintDtoMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final SensitiveWordService sensitiveWordService;
    private final EvaluationMapper evaluationMapper;
    private final FileStorageService fileStorageService;

    public ComplaintServiceImpl(ComplaintMapper complaintMapper,
            ComplaintDtoMapper complaintDtoMapper,
            FileStorageService fileStorageService,
            RocketMQTemplate rocketMQTemplate,
            SensitiveWordService sensitiveWordService,
            EvaluationMapper evaluationMapper) {
        this.complaintMapper = complaintMapper;
        this.complaintDtoMapper = complaintDtoMapper;
        this.fileStorageService = fileStorageService;
        this.rocketMQTemplate = rocketMQTemplate;
        this.sensitiveWordService = sensitiveWordService;
        this.evaluationMapper = evaluationMapper;
    }

    @Override
    @Transactional
    public ComplaintDTO fileComplaint(ComplaintDTO complaintDTO, List<MultipartFile> evidenceFiles) {
        if (complaintDTO.getEvaluationId() != null) {
            Evaluation eval = evaluationMapper.selectById(complaintDTO.getEvaluationId());
            if (eval == null) {
                throw new ResourceNotFoundException(
                        "投诉关联的评价 (ID: " + complaintDTO.getEvaluationId() + ") 未找到。");
            }
        } else {
            log.warn("Filing a complaint not directly linked to an evaluation by complainant {}.",
                    complaintDTO.getComplainantId());
        }

        if (complaintDTO.getReason() != null && sensitiveWordService.containsSensitiveWords(complaintDTO.getReason())) {
            log.warn("Complaint reason from {} contains sensitive words. Reason: '{}'", complaintDTO.getComplainantId(),
                    complaintDTO.getReason());
            complaintDTO.setReason(sensitiveWordService.filterContent(complaintDTO.getReason()));
        }

        Complaint complaint = complaintDtoMapper.toEntity(complaintDTO);

        if (evidenceFiles != null && !evidenceFiles.isEmpty()) {
            List<String> evidencePaths = fileStorageService.storeFiles(evidenceFiles.toArray(new MultipartFile[0]));
            complaint.setEvidencePathsString(String.join(",", evidencePaths));
        }

        complaint.setStatus("PENDING_REVIEW");
        complaint.setCreatedAt(LocalDateTime.now());

        complaintMapper.insert(complaint);
        Complaint savedComplaint = complaint;
        log.info("Complaint (ID: {}) filed successfully by complainant {}.", savedComplaint.getId(),
                savedComplaint.getComplainantId());

        if (savedComplaint.getEvaluationId() != null) {
            Evaluation eval = evaluationMapper.selectById(savedComplaint.getEvaluationId());
            if (eval != null) {
                eval.setComplaintStatus("FILED");
                eval.setUpdatedAt(LocalDateTime.now());
                evaluationMapper.updateById(eval);
            }
        }

        try {
            rocketMQTemplate.convertAndSend("complaint-topic:COMPLAINT_FILED", savedComplaint);
            log.info("COMPLAINT_FILED event sent for complaint ID: {}", savedComplaint.getId());
        } catch (Exception e) {
            log.error("Failed to send COMPLAINT_FILED event for complaint ID {}: ", savedComplaint.getId(), e);
        }

        return complaintDtoMapper.toDTO(savedComplaint);
    }

    @Override
    public List<ComplaintDTO> getComplaintsByEvaluation(Long evaluationId) {
        List<Complaint> complaints = complaintMapper.selectList(new LambdaQueryWrapper<Complaint>()
                .eq(Complaint::getEvaluationId, evaluationId));
        return complaints.stream()
                .map(complaintDtoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ComplaintDTO> getComplaintsByComplainant(Long complainantId) {
        List<Complaint> complaints = complaintMapper.selectList(new LambdaQueryWrapper<Complaint>()
                .eq(Complaint::getComplainantId, complainantId));
        return complaints.stream()
                .map(complaintDtoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ComplaintDTO> getAllComplaintsForAdmin(Pageable pageable, String status) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Complaint> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                pageable.getPageNumber() + 1, pageable.getPageSize());
        LambdaQueryWrapper<Complaint> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            queryWrapper.eq(Complaint::getStatus, status);
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Complaint> result = complaintMapper.selectPage(page,
                queryWrapper);
        List<ComplaintDTO> dtos = result.getRecords().stream()
                .map(complaintDtoMapper::toDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, result.getTotal());
    }

    @Override
    @Transactional
    public ComplaintDTO adminUpdateComplaintStatus(Long complaintId, String newStatus, String adminNotes,
            Long adminId) {
        Complaint complaint = complaintMapper.selectById(complaintId);
        if (complaint == null) {
            throw new ResourceNotFoundException("Complaint not found with id: " + complaintId);
        }
        complaint.setStatus(newStatus);
        complaint.setAdminNotes(adminNotes);
        complaint.setHandledByAdminId(adminId);
        complaint.setResolutionTime(LocalDateTime.now());
        complaint.setUpdatedAt(LocalDateTime.now());
        complaintMapper.updateById(complaint);
        return complaintDtoMapper.toDTO(complaint);
    }
}