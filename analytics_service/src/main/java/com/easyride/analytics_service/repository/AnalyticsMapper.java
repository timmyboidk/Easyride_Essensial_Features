package com.easyride.analytics_service.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easyride.analytics_service.model.AnalyticsRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnalyticsMapper extends BaseMapper<AnalyticsRecord> {
}
