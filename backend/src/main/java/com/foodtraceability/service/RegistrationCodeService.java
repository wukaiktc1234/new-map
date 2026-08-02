package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.OnboardingRecord;
import com.foodtraceability.entity.RegistrationCode;
import com.foodtraceability.entity.RegistrationCodeLog;

import java.util.List;

/**
 * 注册码服务接口
 */
public interface RegistrationCodeService {

    /**
     * 生成单个注册码
     * @param request 生成注册码请求
     * @return 注册码信息
     */
    RegistrationCode generateCode(GenerateCodeRequest request);

    /**
     * 批量生成注册码
     * @param request 批量生成注册码请求
     * @return 生成的注册码列表
     */
    List<RegistrationCode> batchGenerateCode(BatchGenerateCodeRequest request);

    /**
     * 验证注册码
     * @param code 注册码
     * @return 是否有效
     */
    boolean validateCode(String code);

    /**
     * 使用注册码
     * @param code 注册码
     * @param userId 用户ID
     * @param username 用户名
     * @param ipAddress IP地址
     * @return 是否使用成功
     */
    boolean useCode(String code, String userId, String username, String ipAddress);

    /**
     * 使用注册码并返回关联的入职记录
     * @param code 注册码
     * @param userId 用户ID
     * @param username 用户名
     * @param ipAddress IP地址
     * @return 使用结果，包含是否成功和关联的入职记录
     */
    UseCodeResult useCodeWithOnboarding(String code, String userId, String username, String ipAddress);

    /**
     * 发送注册码
     * @param request 发送注册码请求
     */
    void sendCode(SendCodeRequest request);

    /**
     * 批量发送注册码
     * @param request 批量发送注册码请求
     */
    void batchSendCode(BatchSendCodeRequest request);

    /**
     * 分页查询注册码
     * @param request 分页查询请求
     * @return 分页结果
     */
    IPage<RegistrationCode> getCodesByPage(CodeQueryRequest request);

    /**
     * 分页查询注册码日志
     * @param request 分页查询请求
     * @return 分页结果
     */
    IPage<RegistrationCodeLog> getCodeLogsByPage(CodeLogQueryRequest request);

    /**
     * 更新注册码状态
     * @param id 注册码ID
     * @param status 新状态
     * @return 是否更新成功
     */
    boolean updateCodeStatus(String id, String status);

    /**
     * 发送手机验证码
     * @param phone 手机号
     * @return 验证码
     */
    String sendSmsCode(String phone);

    /**
     * 验证手机验证码
     * @param phone 手机号
     * @param code 验证码
     * @return 是否有效
     */
    boolean validateSmsCode(String phone, String code);
}