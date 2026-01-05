package com.evaluation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 标签实体类
 */
@TableName("tags")
@Data
@NoArgsConstructor
public class Tag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
}
