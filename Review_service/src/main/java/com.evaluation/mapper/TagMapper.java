package com.evaluation.mapper;

import com.evaluation.dto.TagDTO;
import com.evaluation.model.Tag;
import org.springframework.stereotype.Component;

/**
 * 标签映射器，用于实体和DTO之间的转换
 */
@Component
public class TagMapper {

    /**
     * 将 Tag 实体转换为 TagDTO
     *
     * @param tag 标签实体
     * @return 标签DTO
     */
    public TagDTO toDTO(Tag tag) {
        TagDTO dto = new TagDTO();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        return dto;
    }

    /**
     * 将 TagDTO 转换为 Tag 实体
     *
     * @param dto 标签DTO
     * @return 标签实体
     */
    public Tag toEntity(TagDTO dto) {
        Tag tag = new Tag();
        tag.setName(dto.getName());
        return tag;
    }
}
