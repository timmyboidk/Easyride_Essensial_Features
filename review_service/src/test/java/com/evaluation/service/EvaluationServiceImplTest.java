package com.evaluation.service;

import com.evaluation.client.UserClient;
import com.evaluation.dto.EvaluationDTO;
import com.evaluation.mapper.EvaluationMapper;
import com.evaluation.model.Evaluation;
import com.evaluation.model.ReviewWindow;
import com.evaluation.model.Tag;
import com.evaluation.repository.EvaluationRepository;
import com.evaluation.repository.ReviewWindowRepository;
import com.evaluation.repository.TagRepository;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    // @Mock private TagService tagService; // Removed
    @Mock
    private ReviewWindowRepository reviewWindowRepository;

    @InjectMocks
    private EvaluationServiceImpl evaluationService;

    private EvaluationDTO evaluationDTO;
    private Evaluation evaluation;
    private ReviewWindow reviewWindow;

    @BeforeEach
    void setUp() {
        evaluationDTO = new EvaluationDTO();
        evaluationDTO.setOrderId(1L);
        evaluationDTO.setEvaluatorId(101L);
        evaluationDTO.setEvaluateeId(202L);
        evaluationDTO.setScore(5);
        evaluationDTO.setComment("Great!");
        evaluationDTO.setTags(Collections.singletonList("Safe"));

        evaluation = new Evaluation();
        evaluation.setId(1L);

        reviewWindow = new ReviewWindow();
        reviewWindow.setOrderId(1L);
        reviewWindow.setPassengerId(101L);
        reviewWindow.setDriverId(202L);
        reviewWindow.setPassengerCanReview(true);
        reviewWindow.setWindowOpenTime(LocalDateTime.now().minusHours(1));
        reviewWindow.setWindowCloseTime(LocalDateTime.now().plusHours(1));
    }

    @Test
    void createEvaluation_Success() {
        when(reviewWindowRepository.findById(1L)).thenReturn(Optional.of(reviewWindow));
        when(sensitiveWordService.containsSensitiveWords(anyString())).thenReturn(false);
        when(evaluationMapper.toEntity(any(EvaluationDTO.class))).thenReturn(evaluation);
        when(evaluationRepository.save(any(Evaluation.class))).thenReturn(evaluation);
        when(evaluationMapper.toDto(any(Evaluation.class))).thenReturn(evaluationDTO);

        when(tagRepository.findByName("Safe")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(new Tag());

        EvaluationDTO result = evaluationService.createEvaluation(evaluationDTO);

        assertNotNull(result);
        verify(evaluationRepository).save(any(Evaluation.class));
        verify(rocketMQTemplate).convertAndSend(eq("evaluation-topic:EVALUATION_CREATED"), any(Evaluation.class));
    }
}
