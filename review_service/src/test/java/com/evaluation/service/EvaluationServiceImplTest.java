package com.evaluation.service;

import com.evaluation.dto.EvaluationDTO;
import com.evaluation.exception.BadRequestException;
import com.evaluation.mapper.EvaluationDtoMapper;
import com.evaluation.model.Evaluation;
import com.evaluation.model.EvaluationStatus;
import com.evaluation.model.ReviewWindow;
import com.evaluation.repository.EvaluationMapper;
import com.evaluation.repository.ReviewWindowMapper;
import com.evaluation.repository.TagMapper;
import com.evaluation.client.UserClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceImplTest {

    @Mock
    private EvaluationMapper evaluationMapper;
    @Mock
    private TagMapper tagMapper;
    @Mock
    private EvaluationDtoMapper evaluationDtoMapper;
    @Mock
    private RocketMQTemplate rocketMQTemplate;
    @Mock
    private UserClient userClient;
    @Mock
    private SensitiveWordService sensitiveWordService;
    @Mock
    private TagService tagService;
    @Mock
    private ReviewWindowMapper reviewWindowMapper;

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
        when(reviewWindowMapper.selectById(1001L)).thenReturn(openWindow);
        when(sensitiveWordService.containsSensitiveWords(anyString())).thenReturn(false);

        Evaluation evaluationEntity = new Evaluation();
        evaluationEntity.setId(1L);
        when(evaluationDtoMapper.toEntity(any(EvaluationDTO.class))).thenReturn(evaluationEntity);
        when(evaluationMapper.insert(any(Evaluation.class))).thenReturn(1);
        when(evaluationDtoMapper.toDto(any(Evaluation.class))).thenReturn(sampleDto);

        EvaluationDTO result = evaluationService.createEvaluation(sampleDto);

        assertNotNull(result);
        verify(evaluationMapper).insert(any(Evaluation.class));
        verify(reviewWindowMapper).updateById(any(ReviewWindow.class));
        verify(rocketMQTemplate).convertAndSend(anyString(), any(Evaluation.class));
        assertTrue(openWindow.isPassengerReviewed());
    }

    @Test
    void createEvaluation_ReviewWindowClosed() {
        openWindow.setWindowCloseTime(LocalDateTime.now().minusHours(1));
        when(reviewWindowMapper.selectById(1001L)).thenReturn(openWindow);

        assertThrows(BadRequestException.class, () -> evaluationService.createEvaluation(sampleDto));
    }

    @Test
    void createEvaluation_AlreadyReviewed() {
        openWindow.setPassengerReviewed(true);
        when(reviewWindowMapper.selectById(1001L)).thenReturn(openWindow);

        assertThrows(BadRequestException.class, () -> evaluationService.createEvaluation(sampleDto));
    }

    @Test
    void createEvaluation_SensitiveWordsFiltered() {
        sampleDto.setComment("Bad words");

        when(reviewWindowMapper.selectById(1001L)).thenReturn(openWindow);
        when(sensitiveWordService.containsSensitiveWords("Bad words")).thenReturn(true);
        when(sensitiveWordService.filterContent("Bad words")).thenReturn("*** words");

        Evaluation evaluationEntity = new Evaluation();
        when(evaluationDtoMapper.toEntity(any(EvaluationDTO.class))).thenReturn(evaluationEntity);
        when(evaluationMapper.insert(any(Evaluation.class))).thenReturn(1);
        when(evaluationDtoMapper.toDto(any(Evaluation.class))).thenReturn(sampleDto);

        evaluationService.createEvaluation(sampleDto);

        verify(sensitiveWordService).filterContent("Bad words");
        assertEquals("*** words", sampleDto.getComment());
    }

    @Test
    void adminUpdateEvaluationStatus_Success() {
        Evaluation eval = new Evaluation();
        eval.setId(1L);
        eval.setStatus(EvaluationStatus.ACTIVE);

        when(evaluationMapper.selectById(1L)).thenReturn(eval);
        when(evaluationMapper.updateById(any(Evaluation.class))).thenReturn(1);

        EvaluationDTO resultDto = new EvaluationDTO();
        resultDto.setStatus("HIDDEN_BY_ADMIN");
        when(evaluationDtoMapper.toDto(any(Evaluation.class))).thenReturn(resultDto);

        EvaluationDTO result = evaluationService.adminUpdateEvaluationStatus(1L, "HIDDEN_BY_ADMIN", "Inappropriate",
                999L);

        assertEquals("HIDDEN_BY_ADMIN", result.getStatus());
        assertEquals(EvaluationStatus.HIDDEN_BY_ADMIN, eval.getStatus());
        assertEquals("Inappropriate", eval.getAdminNotes());
    }
}
