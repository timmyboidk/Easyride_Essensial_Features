package com.easyride.matching_service.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easyride.matching_service.model.GrabbableOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GrabbableOrderMapper extends BaseMapper<GrabbableOrder> {
}
