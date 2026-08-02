package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.PasswordPolicy;

/**
 * 密码策略服务接口
 */
public interface PasswordPolicyService extends IService<PasswordPolicy> {

    /**
     * 获取默认密码策略
     */
    PasswordPolicy getDefaultPolicy();

    /**
     * 验证密码是否符合策略
     */
    boolean validatePassword(String password, PasswordPolicy policy);

    /**
     * 验证密码强度并返回错误信息
     */
    String validatePasswordStrength(String password);

    /**
     * 设置默认策略
     */
    boolean setDefaultPolicy(Long policyId);

    /**
     * 从DTO创建实体
     */
    PasswordPolicy createFromDTO(com.foodtraceability.dto.PasswordPolicyDTO dto);
}
