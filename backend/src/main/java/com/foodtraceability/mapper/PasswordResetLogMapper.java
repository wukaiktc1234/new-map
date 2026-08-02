package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PasswordResetLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 密码重置操作日志Mapper
 */
@Mapper
public interface PasswordResetLogMapper extends BaseMapper<PasswordResetLog> {

    /**
     * 查询用户的最近操作记录
     */
    List<PasswordResetLog> findRecentOperations(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查询异常操作记录
     */
    List<PasswordResetLog> findAbnormalOperations(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计用户操作次数
     */
    int countUserOperations(@Param("userId") Long userId, @Param("operationType") String operationType, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计IP地址的操作次数
     */
    int countIpOperations(@Param("clientIp") String clientIp, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查询最近失败的操作
     */
    List<PasswordResetLog> findRecentFailedOperations(@Param("userId") Long userId, @Param("limit") int limit);
}