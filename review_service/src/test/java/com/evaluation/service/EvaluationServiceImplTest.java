package com.evaluation.service;

import com.evaluation.dto.EvaluationDTO;
import com.evaluation.dto.TagDTO;
import com.evaluation.exception.BadRequestException;
import com.evaluation.mapper.EvaluationMapper;
import com.evaluation.model.Evaluation;
import com.evaluation.model.EvaluationStatus;
import com.evaluation.model.ReviewWindow;
import com.evaluation.repository.EvaluationRepository;
import com.evaluation.repository.ReviewWindowRepository;
import com.evaluation.repository.TagRepository;
import com.evaluation.client.UserClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {

    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private EvaluationMapper evaluationMapper;
    @Mock
    private RocketMQTemplate rocketMQTemplate;
    @Mock
    private UserClient userClient;
    @Mock
    private SensitiveWordService sensitiveWordService;
    @Mock
    private TagService tagService;
    @Mock
    private ReviewWindowRepository reviewWindowRepository;

    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    private EvaluationDTO sampleDto;
    private ReviewWindow openWindow;

    @BeforeEach
    void setUp() {
        sampleDto = new EvaluationDTO();
        sampleDto.setEvaluatorId(101L);
        sampleDto.setEvaluateeId(202L);
        sampleDto.setOrderId(1001L);
        sampleDto.setScore(5);
        sampleDto.setComment("Great ride!");

        openWindow = new ReviewWindow();
        openWindow.setOrderId(1001L);
        openWindow.setPassengerId(101L);
        openWindow.setDriverId(202L);
        openWindow.setPassengerCanReview(true);
        openWindow.setPassengerReviewed(false);
        openWindow.setWindowCloseTime(LocalDateTime.now().plusHours(24));
    }

    @Test
    void createEvaluation_Success() {
        when(reviewWindowRepository.findById(1001L)).thenReturn(Optional.of(openWindow));
        when(sensitiveWordService.containsSensitiveWords(anyString())).thenReturn(false);

        Evaluation evaluationEntity = new Evaluation();
        evaluationEntity.setId(1L);
        when(evaluationMapper.toEntity(any(EvaluationDTO.class))).thenReturn(evaluationEntity);
        when(evaluationRepository.save(any(Evaluation.class))).thenReturn(evaluationEntity);
        when(evaluationMapper.toDto(any(Evaluation.class))).thenReturn(sampleDto);

        EvaluationDTO result = evaluationService.createEvaluation(sampleDto);

        assertNotNull(result);
        verify(evaluationRepository).save(any(Evaluation.class));
        verify(reviewWindowRepository).save(any(ReviewWindow.class));
        verify(rocketMQTemplate).convertAndSend(anyString(), any(Evaluation.class));
        assertTrue(openWindow.isPassengerReviewed());
    }

    @Test
    void createEvaluation_ReviewWindowClosed() {
        openWindow.setWindowCloseTime(LocalDateTime.now().minusHours(1));
        when(reviewWindowRepository.findById(1001L)).thenReturn(Optional.of(openWindow));

        assertThrows(BadRequestException.class, () -> evaluationService.createEvaluation(sampleDto));
    }

    @Test
    void createEvaluation_AlreadyReviewed() {
        openWindow.setPassengerReviewed(true);
        when(reviewWindowRepository.findById(1001L)).thenReturn(Optional.of(openWindow));

        assertThrows(BadRequestException.class, () -> evaluationService.createEvaluation(sampleDto));
    }

    @Test
    void createEvaluation_SensitiveWordsFiltered() {
        sampleDto.setComment("Bad words");

        when(reviewWindowRepository.findById(1001L)).thenReturn(Optional.of(openWindow));
        when(sensitiveWordService.containsSensitiveWords("Bad words")).thenReturn(true);
        when(sensitiveWordService.filterContent("Bad words")).thenReturn("*** words");

        Evaluation evaluationEntity = new Evaluation();
        when(evaluationMapper.toEntity(any(EvaluationDTO.class))).thenReturn(evaluationEntity);
        when(evaluationRepository.save(any(Evaluation.class))).thenReturn(evaluationEntity);
        when(evaluationMapper.toDto(any(Evaluation.class))).thenReturn(sampleDto);

        evaluationService.createEvaluation(sampleDto);

        verify(sensitiveWordService).filterContent("Bad words");
        assertEquals("*** words", sampleDto.getComment());
    }

    @Test
    void adminUpdateEvaluationStatus_Success() {
        Evaluation eval = new Evaluation();
        eval.setId(1L);
        eval.setStatus(EvaluationStatus.ACTIVE);

        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(eval));
        when(evaluationRepository.save(any(Evaluation.class))).thenReturn(eval);

        EvaluationDTO resultDto = new EvaluationDTO();
        resultDto.setStatus("HIDDEN_BY_ADMIN");
        when(evaluationMapper.toDto(any(Evaluation.class))).thenReturn(resultDto);

        EvaluationDTO result = evaluationService.adminUpdateEvaluationStatus(1L, "HIDDEN_BY_ADMIN", "Inappropriate",
                999L);

        assertEquals("HIDDEN_BY_ADMIN", result.getStatus());
        assertEquals(EvaluationStatus.HIDDEN_BY_ADMIN, eval.getStatus());
        assertEquals("Inappropriate", eval.getAdminNotes());
    }
}
