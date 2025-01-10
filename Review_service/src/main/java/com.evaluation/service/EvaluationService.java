package com.evaluation.service;

import com.evaluation.dto.EvaluationDTO;

import java.util.List;

/**
 * 评价服务接口，定义评价相关的业务操作
 */
public interface EvaluationService {

    /**
     * 创建新的评价
     *
     * @param evaluationDTO 评价数据传输对象
     * @return 创建后的评价数据传输对象
     */
    EvaluationDTO createEvaluation(EvaluationDTO evaluationDTO);

    /**
     * 根据被评价者ID获取所有评价
     *
     * @param evaluateeId 被评价者ID
     * @return 评价列表
     */
    List<EvaluationDTO> getEvaluationsByEvaluatee(Long evaluateeId);

    /**
     * 根据评价者ID获取所有评价
     *
     * @param evaluatorId 评价者ID
     * @return 评价列表
     */
    List<EvaluationDTO> getEvaluationsByEvaluator(Long evaluatorId);
}
