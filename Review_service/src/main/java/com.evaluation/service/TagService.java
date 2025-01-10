package com.evaluation.service;

import com.evaluation.dto.TagDTO;

import java.util.List;

/**
 * 标签服务接口，定义标签相关的业务操作
 */
public interface TagService {

    /**
     * 获取所有标签
     *
     * @return 标签列表
     */
    List<TagDTO> getAllTags();

    /**
     * 创建新的标签
     *
     * @param tagDTO 标签数据传输对象
     * @return 创建后的标签数据传输对象
     */
    TagDTO createTag(TagDTO tagDTO);
}
