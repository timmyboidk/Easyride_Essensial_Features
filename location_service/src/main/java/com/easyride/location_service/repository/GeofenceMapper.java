package com.easyride.location_service.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easyride.location_service.model.Geofence;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GeofenceMapper extends BaseMapper<Geofence> {
}
