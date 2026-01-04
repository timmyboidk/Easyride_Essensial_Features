package com.evaluation.service;

import com.evaluation.dto.TagDTO;
import com.evaluation.exception.BadRequestException;
import com.evaluation.mapper.TagMapper;
import com.evaluation.model.Tag;
import com.evaluation.repository.TagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签服务实现类，处理标签相关的业务逻辑
 */
@Service
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Autowired
    public TagServiceImpl(TagRepository tagRepository, TagMapper tagMapper) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
    }

    /**
     * 获取所有标签
     *
     * @return 标签列表
     */
    @Override
    public List<TagDTO> getAllTags() {
        List<Tag> tags = tagRepository.findAll();
        return tags.stream()
                .map(tagMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建新的标签
     *
     * @param tagDTO 标签数据传输对象
     * @return 创建后的标签数据传输对象
     */
    @Override
    @Transactional
    public TagDTO createTag(TagDTO tagDTO) {
        // 检查标签名称是否已存在
        tagRepository.findByName(tagDTO.getName()).ifPresent(tag -> {
            throw new BadRequestException("标签名称已存在: " + tagDTO.getName());
        });

        Tag tag = tagMapper.toEntity(tagDTO);
        Tag savedTag = tagRepository.save(tag);
        return tagMapper.toDTO(savedTag);
    }
}
