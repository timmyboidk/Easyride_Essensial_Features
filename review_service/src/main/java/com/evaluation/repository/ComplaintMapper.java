package com.evaluation.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evaluation.model.Complaint;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ComplaintMapper extends BaseMapper<Complaint> {
}
