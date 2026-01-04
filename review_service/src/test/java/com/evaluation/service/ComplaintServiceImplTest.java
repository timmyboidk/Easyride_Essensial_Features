package com.evaluation.service;

import com.evaluation.dto.ComplaintDTO;
import com.evaluation.mapper.ComplaintMapper;
import com.evaluation.model.Complaint;
import com.evaluation.model.Evaluation;
import com.evaluation.repository.ComplaintRepository;
import com.evaluation.repository.EvaluationRepository;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceImplTest {

    @Mock
    private ComplaintRepository complaintRepository;
    @Mock
    private ComplaintMapper complaintMapper;
    @Mock
    private RocketMQTemplate rocketMQTemplate;
    @Mock
    private SensitiveWordService sensitiveWordService;
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ComplaintServiceImpl complaintService;

    @Test
    void fileComplaint_Success() {
        ComplaintDTO dto = new ComplaintDTO();
        dto.setEvaluationId(100L);
        dto.setComplainantId(1L);
        dto.setReason("Service was bad");

        Evaluation evaluation = new Evaluation();
        evaluation.setId(100L);

        Complaint complaint = new Complaint();
        complaint.setId(1L);
        complaint.setComplainantId(1L);

        when(evaluationRepository.findById(100L)).thenReturn(Optional.of(evaluation));
        when(sensitiveWordService.containsSensitiveWords(anyString())).thenReturn(false);
        when(complaintMapper.toEntity(dto)).thenReturn(complaint);
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);
        when(complaintMapper.toDTO(complaint)).thenReturn(dto);

        ComplaintDTO result = complaintService.fileComplaint(dto, null);

        assertNotNull(result);
        verify(rocketMQTemplate, times(1)).convertAndSend(anyString(), eq(complaint));
    }
}
