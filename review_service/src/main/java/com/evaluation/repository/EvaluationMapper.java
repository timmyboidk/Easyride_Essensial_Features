package com.evaluation.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evaluation.model.Evaluation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EvaluationMapper extends BaseMapper<Evaluation> {
}
