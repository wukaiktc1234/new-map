package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.RegistrationCode;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 注册码Mapper接口
 */
public interface RegistrationCodeMapper extends BaseMapper<RegistrationCode> {

    /**
     * 根据注册码查询
     */
    @Select("SELECT * FROM registration_code WHERE code = #{code}")
    RegistrationCode findByCode(@Param("code") String code);

    /**
     * 根据入职记录ID查询注册码
     */
    @Select("SELECT * FROM registration_code WHERE onboarding_record_id = #{onboardingRecordId}")
    RegistrationCode findByOnboardingRecordId(@Param("onboardingRecordId") String onboardingRecordId);

    /**
     * 查询未使用且未过期的注册码数量
     */
    @Select("SELECT COUNT(*) FROM registration_code WHERE status = 'UNUSED' AND validity_end > NOW()")
    long countValidCodes();

    /**
     * 更新注册码为已使用
     */
    @Update("UPDATE registration_code SET status = 'USED', updated_at = NOW() WHERE code = #{code} AND status = 'UNUSED' AND validity_end > NOW()")
    int updateToUsed(@Param("code") String code);

    /**
     * 更新过期的注册码状态
     */
    @Update("UPDATE registration_code SET status = 'EXPIRED', updated_at = NOW() WHERE status = 'UNUSED' AND validity_end <= NOW()")
    int updateExpiredCodes();
}