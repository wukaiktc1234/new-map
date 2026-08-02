package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.SysSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统设置Mapper接口
 */
@Mapper
public interface SysSettingMapper extends BaseMapper<SysSetting> {

    /**
     * 根据分组查询设置
     */
    @Select("SELECT * FROM sys_settings WHERE setting_group = #{group} AND is_enabled = 1 ORDER BY sort_order")
    List<SysSetting> selectByGroup(@Param("group") String group);

    /**
     * 根据作用域查询设置
     */
    @Select("SELECT * FROM sys_settings WHERE setting_key = #{key} AND scope_type = #{scopeType} AND (scope_id = #{scopeId} OR scope_id IS NULL) AND is_enabled = 1")
    List<SysSetting> selectByScope(@Param("key") String key, @Param("scopeType") String scopeType, @Param("scopeId") String scopeId);

    /**
     * 查询全局设置
     */
    @Select("SELECT * FROM sys_settings WHERE setting_key = #{key} AND scope_type = 'global' AND is_enabled = 1 LIMIT 1")
    SysSetting selectGlobalSetting(@Param("key") String key);

    /**
     * 根据设置键查询
     */
    @Select("SELECT * FROM sys_settings WHERE setting_key = #{key} AND is_enabled = 1 LIMIT 1")
    SysSetting selectBySettingKey(@Param("key") String key);
}
