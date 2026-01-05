package com.evaluation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evaluation.dto.TagDTO;
import com.evaluation.exception.BadRequestException;
import com.evaluation.mapper.TagDtoMapper;
import com.evaluation.model.Tag;
import com.evaluation.repository.TagMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签服务实现类，处理标签相关的业务逻辑
 */
@Service
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final TagDtoMapper tagDtoMapper;

    public TagServiceImpl(TagMapper tagMapper, TagDtoMapper tagDtoMapper) {
        this.tagMapper = tagMapper;
        this.tagDtoMapper = tagDtoMapper;
    }

    /**
     * 获取所有标签
     *
     * @return 标签列表
     */
    @Override
    public List<TagDTO> getAllTags() {
        List<Tag> tags = tagMapper.selectList(null);
        return tags.stream()
                .map(tagDtoMapper::toDTO)
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
        Tag existingTag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, tagDTO.getName()));
        if (existingTag != null) {
            throw new BadRequestException("标签名称已存在: " + tagDTO.getName());
        }

        Tag tag = tagDtoMapper.toEntity(tagDTO);
        tagMapper.insert(tag);
        return tagDtoMapper.toDTO(tag);
    }
}
