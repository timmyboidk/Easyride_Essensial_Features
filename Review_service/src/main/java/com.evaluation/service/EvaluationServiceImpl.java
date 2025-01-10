package com.evaluation.service;

import com.evaluation.client.UserClient;
import com.evaluation.dto.EvaluationDTO;
import com.evaluation.exception.ResourceNotFoundException;
import com.evaluation.mapper.EvaluationMapper;
import com.evaluation.model.Evaluation;
import com.evaluation.model.Tag;
import com.evaluation.repository.EvaluationRepository;
import com.evaluation.repository.TagRepository;
import com.evaluation.util.Constants;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评价服务实现类，处理评价相关的业务逻辑
 */
@Service
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final TagRepository tagRepository;
    private final EvaluationMapper evaluationMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final UserClient userClient; // 用于远程调用用户服务

    @Autowired
    public EvaluationServiceImpl(EvaluationRepository evaluationRepository,
                                 TagRepository tagRepository,
                                 EvaluationMapper evaluationMapper,
                                 RocketMQTemplate rocketMQTemplate,
                                 UserClient userClient) {
        this.evaluationRepository = evaluationRepository;
        this.tagRepository = tagRepository;
        this.evaluationMapper = evaluationMapper;
        this.rocketMQTemplate = rocketMQTemplate;
        this.userClient = userClient;
    }

    /**
     * 创建新的评价，处理标签并发送消息到RocketMQ
     *
     * @param evaluationDTO 评价数据传输对象
     * @return 创建后的评价数据传输对象
     */
    @Override
    @Transactional
    public EvaluationDTO createEvaluation(EvaluationDTO evaluationDTO) {
        // 将DTO转换为实体
        Evaluation evaluation = evaluationMapper.toEntity(evaluationDTO);

        // 处理标签
        Set<Tag> tags = new HashSet<>();
        if (evaluationDTO.getTags() != null && !evaluationDTO.getTags().isEmpty()) {
            tags = evaluationDTO.getTags().stream()
                    .map(tagName -> tagRepository.findByName(tagName)
                            .orElseGet(() -> {
                                Tag newTag = new Tag();
                                newTag.setName(tagName);
                                return tagRepository.save(newTag);
                            }))
                    .collect(Collectors.toSet());
        }
        evaluation.setTags(tags);

        // 设置申诉状态为默认值
        evaluation.setComplaintStatus(Constants.COMPLAINT_STATUS_PENDING);

        // 保存评价到数据库
        Evaluation savedEvaluation = evaluationRepository.save(evaluation);

        // 将实体转换回DTO
        EvaluationDTO savedEvaluationDTO = evaluationMapper.toDTO(savedEvaluation);

        // 发送消息到RocketMQ，通知其他微服务有新的评价创建
        rocketMQTemplate.convertAndSend("evaluation-topic", savedEvaluationDTO);

        return savedEvaluationDTO;
    }

    /**
     * 根据被评价者ID获取所有评价
     *
     * @param evaluateeId 被评价者ID
     * @return 评价列表
     */
    @Override
    public List<EvaluationDTO> getEvaluationsByEvaluatee(Long evaluateeId) {
        // 检查被评价者是否存在（可选，根据业务需求）
        // UserDTO user = userClient.getUserById(evaluateeId);
        // if (user == null) {
        //     throw new ResourceNotFoundException("被评价者不存在，ID: " + evaluateeId);
        // }

        List<Evaluation> evaluations = evaluationRepository.findByEvaluateeId(evaluateeId);
        return evaluations.stream()
                .map(evaluationMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据评价者ID获取所有评价
     *
     * @param evaluatorId 评价者ID
     * @return 评价列表
     */
    @Override
    public List<EvaluationDTO> getEvaluationsByEvaluator(Long evaluatorId) {
        // 检查评价者是否存在（可选，根据业务需求）
        // UserDTO user = userClient.getUserById(evaluatorId);
        // if (user == null) {
        //     throw new ResourceNotFoundException("评价者不存在，ID: " + evaluatorId);
        // }

        List<Evaluation> evaluations = evaluationRepository.findByEvaluatorId(evaluatorId);
        return evaluations.stream()
                .map(evaluationMapper::toDTO)
                .collect(Collectors.toList());
    }
}
