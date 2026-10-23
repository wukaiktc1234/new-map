package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.AppDeviceRegistration;
import org.apache.ibatis.annotations.Mapper;

/**
 * APK 设备激活白名单数据访问接口
 */
@Mapper
public interface AppDeviceRegistrationMapper extends BaseMapper<AppDeviceRegistration> {
}
