package com.foodtraceability.mapper.marketing;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.marketing.RechargeSystemSetting;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 储值系统设置 Mapper
 */
@Mapper
@Repository
public interface RechargeSystemSettingMapper extends BaseMapper<RechargeSystemSetting> {
}
