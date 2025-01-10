package com.evaluation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签数据传输对象，用于在不同层之间传输标签数据
 */
@Data
@NoArgsConstructor
public class TagDTO {

    private Long id;
    private String name;

    /**
     * 全参构造函数
     *
     * @param id   标签ID
     * @param name 标签名称
     */
    public TagDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
