package com.evaluation.service;

import com.evaluation.dto.ComplaintDTO;
import com.evaluation.mapper.ComplaintMapper;
import com.evaluation.model.Complaint;
import com.evaluation.repository.ComplaintRepository;
import com.evaluation.util.Constants;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 投诉服务实现类，处理投诉相关的业务逻辑
 */
@Service
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintMapper complaintMapper;
    private final RocketMQTemplate rocketMQTemplate;

    @Autowired
    public ComplaintServiceImpl(ComplaintRepository complaintRepository,
                                ComplaintMapper complaintMapper,
                                RocketMQTemplate rocketMQTemplate) {
        this.complaintRepository = complaintRepository;
        this.complaintMapper = complaintMapper;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /**
     * 提交新的投诉，处理上传的证据并发送消息到RocketMQ
     *
     * @param complaintDTO 投诉数据传输对象
     * @return 创建后的投诉数据传输对象
     */
    @Override
    @Transactional
    public ComplaintDTO fileComplaint(ComplaintDTO complaintDTO) {
        // 检查关联的评价是否存在
        // Evaluation evaluation = evaluationRepository.findById(complaintDTO.getEvaluationId())
        //         .orElseThrow(() -> new ResourceNotFoundException("评价未找到，ID: " + complaintDTO.getEvaluationId()));

        // 将DTO转换为实体
        Complaint complaint = complaintMapper.toEntity(complaintDTO);

        // 设置申诉状态为默认值
        complaint.setStatus(Constants.COMPLAINT_STATUS_PENDING);

        // 保存投诉到数据库
        Complaint savedComplaint = complaintRepository.save(complaint);

        // 将实体转换回DTO
        ComplaintDTO savedComplaintDTO = complaintMapper.toDTO(savedComplaint);

        // 发送消息到RocketMQ，通知其他微服务有新的投诉提交
        rocketMQTemplate.convertAndSend("complaint-topic", savedComplaintDTO);

        return savedComplaintDTO;
    }

    /**
     * 根据评价ID获取所有相关投诉
     *
     * @param evaluationId 评价ID
     * @return 投诉列表
     */
    @Override
    public List<ComplaintDTO> getComplaintsByEvaluation(Long evaluationId) {
        List<Complaint> complaints = complaintRepository.findByEvaluationId(evaluationId);
        return complaints.stream()
                .map(complaintMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据申诉者ID获取所有投诉
     *
     * @param complainantId 申诉者ID
     * @return 投诉列表
     */
    @Override
    public List<ComplaintDTO> getComplaintsByComplainant(Long complainantId) {
        List<Complaint> complaints = complaintRepository.findByComplainantId(complainantId);
        return complaints.stream()
                .map(complaintMapper::toDTO)
                .collect(Collectors.toList());
    }
}
