package com.foodtraceability.mapper.marketing;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.marketing.AnomalyAlert;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 异常交易告警 Mapper
 */
@Mapper
@Repository
public interface AnomalyAlertMapper extends BaseMapper<AnomalyAlert> {
}
