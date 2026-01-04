package com.evaluation.controller;

import com.evaluation.dto.EvaluationDTO;
import com.evaluation.service.EvaluationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvaluationController.class)
@AutoConfigureMockMvc(addFilters = false)
class EvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvaluationService evaluationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createEvaluation_Success() throws Exception {
        EvaluationDTO dto = new EvaluationDTO();
        dto.setEvaluatorId(101L);
        dto.setEvaluateeId(202L);
        dto.setScore(5);
        dto.setComment("Good");

        when(evaluationService.createEvaluation(any(EvaluationDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/evaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.comment").value("Good"));
    }

    @Test
    void getEvaluationsByEvaluatee_Success() throws Exception {
        EvaluationDTO dto = new EvaluationDTO();
        dto.setId(1L);
        dto.setComment("Good");

        when(evaluationService.getEvaluationsByEvaluatee(anyLong())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/evaluations/evaluatee/202"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].comment").value("Good"));
    }

    @Test
    void getEvaluationsByEvaluator_Success() throws Exception {
        EvaluationDTO dto = new EvaluationDTO();
        dto.setId(1L);
        dto.setComment("Good");

        when(evaluationService.getEvaluationsByEvaluator(anyLong())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/evaluations/evaluator/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].comment").value("Good"));
    }
}
