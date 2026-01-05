package com.evaluation.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evaluation.model.Tag;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
}
