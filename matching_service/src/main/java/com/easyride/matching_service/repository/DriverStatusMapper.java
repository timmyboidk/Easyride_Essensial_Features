package com.easyride.matching_service.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easyride.matching_service.model.DriverStatus;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DriverStatusMapper extends BaseMapper<DriverStatus> {
}
