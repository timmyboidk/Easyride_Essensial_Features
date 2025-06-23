package com.evaluation.service;

import com.evaluation.dto.ComplaintDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * 投诉服务接口，定义投诉相关的业务操作
 */
public interface ComplaintService {

    /**
     * 提交新的投诉
     *
     * @param complaintDTO 投诉数据传输对象
     * @param evidenceFiles 证据文件
     * @return 创建后的投诉数据传输对象
     */
    ComplaintDTO fileComplaint(ComplaintDTO complaintDTO, List<MultipartFile> evidenceFiles);

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

    Page<ComplaintDTO> getAllComplaintsForAdmin(Pageable pageable, String status);

    ComplaintDTO adminUpdateComplaintStatus(Long complaintId, String newStatus, String adminNotes, Long adminId);
}