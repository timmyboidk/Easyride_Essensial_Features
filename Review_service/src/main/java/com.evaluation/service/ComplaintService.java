package com.evaluation.service;

import com.evaluation.dto.ComplaintDTO;

import java.util.List;

/**
 * 投诉服务接口，定义投诉相关的业务操作
 */
public interface ComplaintService {

    /**
     * 提交新的投诉
     *
     * @param complaintDTO 投诉数据传输对象
     * @return 创建后的投诉数据传输对象
     */
    ComplaintDTO fileComplaint(ComplaintDTO complaintDTO);

    /**
     * 根据评价ID获取所有相关投诉
     *
     * @param evaluationId 评价ID
     * @return 投诉列表
     */
    List<ComplaintDTO> getComplaintsByEvaluation(Long evaluationId);

    /**
     * 根据申诉者ID获取所有投诉
     *
     * @param complainantId 申诉者ID
     * @return 投诉列表
     */
    List<ComplaintDTO> getComplaintsByComplainant(Long complainantId);
}
