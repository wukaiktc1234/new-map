package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    @Select("SELECT * FROM system_config WHERE tenant_id = #{tenantId} AND deleted = 0 AND config_key = #{configKey}")
    SystemConfig selectByTenantAndKey(Long tenantId, String configKey);

    @Select("SELECT * FROM system_config WHERE tenant_id = #{tenantId} AND deleted = 0 AND config_type = #{configType}")
    List<SystemConfig> selectByTenantAndType(Long tenantId, String configType);

    @Select("SELECT * FROM system_config WHERE tenant_id = #{tenantId} AND deleted = 0")
    List<SystemConfig> selectByTenant(Long tenantId);
}
