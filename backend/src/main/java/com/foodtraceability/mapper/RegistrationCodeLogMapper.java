package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.RegistrationCodeLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 注册码日志Mapper接口
 */
public interface RegistrationCodeLogMapper extends BaseMapper<RegistrationCodeLog> {

    /**
     * 根据注册码查询日志
     */
    @Select("SELECT * FROM registration_code_log WHERE code = #{code} ORDER BY created_at DESC")
    List<RegistrationCodeLog> findByCode(@Param("code") String code);

    /**
     * 根据用户ID查询日志
     */
    @Select("SELECT * FROM registration_code_log WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<RegistrationCodeLog> findByUserId(@Param("userId") String userId);

    /**
     * 统计注册码使用情况
     */
    @Select("SELECT status, COUNT(*) as count FROM registration_code_log GROUP BY status")
    List<Map<String, Object>> countByStatus();
}