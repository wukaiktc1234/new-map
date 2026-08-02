package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.PasswordPolicy;
import com.foodtraceability.mapper.PasswordPolicyMapper;
import com.foodtraceability.service.PasswordPolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 密码策略服务实现类
 */
@Service
public class PasswordPolicyServiceImpl extends ServiceImpl<PasswordPolicyMapper, PasswordPolicy> implements PasswordPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordPolicyServiceImpl.class);

    private final PasswordPolicyMapper passwordPolicyMapper;

    public PasswordPolicyServiceImpl(PasswordPolicyMapper passwordPolicyMapper) {
        this.passwordPolicyMapper = passwordPolicyMapper;
    }

    @Override
    public PasswordPolicy getDefaultPolicy() {
        PasswordPolicy defaultPolicy = passwordPolicyMapper.selectDefaultPolicy();
        if (defaultPolicy == null) {
            defaultPolicy = createDefaultPolicy();
        }
        return defaultPolicy;
    }

    @Override
    public boolean validatePassword(String password, PasswordPolicy policy) {
        if (policy == null) {
            policy = getDefaultPolicy();
        }

        if (password.length() < policy.getMinLength()) {
            return false;
        }
        if (password.length() > policy.getMaxLength()) {
            return false;
        }
        if (policy.getRequireUppercase() && !hasUpperCase(password)) {
            return false;
        }
        if (policy.getRequireLowercase() && !hasLowerCase(password)) {
            return false;
        }
        if (policy.getRequireDigit() && !hasDigit(password)) {
            return false;
        }
        if (policy.getRequireSpecial() && !hasSpecialChar(password)) {
            return false;
        }

        return true;
    }

    @Override
    public String validatePasswordStrength(String password) {
        PasswordPolicy policy = getDefaultPolicy();

        if (password.length() < policy.getMinLength()) {
            return "密码长度不能少于" + policy.getMinLength() + "位";
        }
        if (password.length() > policy.getMaxLength()) {
            return "密码长度不能超过" + policy.getMaxLength() + "位";
        }
        if (policy.getRequireUppercase() && !hasUpperCase(password)) {
            return "密码必须包含大写字母";
        }
        if (policy.getRequireLowercase() && !hasLowerCase(password)) {
            return "密码必须包含小写字母";
        }
        if (policy.getRequireDigit() && !hasDigit(password)) {
            return "密码必须包含数字";
        }
        if (policy.getRequireSpecial() && !hasSpecialChar(password)) {
            return "密码必须包含特殊字符(@$!%*?&)";
        }

        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefaultPolicy(Long policyId) {
        try {
            PasswordPolicy target = passwordPolicyMapper.selectById(policyId);
            if (target == null) {
                logger.warn("设置默认策略失败: 策略不存在, policyId={}", policyId);
                return false;
            }

            // 使用数据库原子操作：先将所有策略设为非默认，再设置目标策略为默认
            // 数据库层有唯一索引约束 uk_password_policies_is_default，防止并发冲突
            passwordPolicyMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PasswordPolicy>()
                    .set(PasswordPolicy::getIsDefault, false)
            );

            target.setIsDefault(true);
            int updated = passwordPolicyMapper.updateById(target);
            
            if (updated == 0) {
                // 乐观锁冲突或记录已被删除
                logger.warn("设置默认策略失败: 乐观锁冲突或记录已删除, policyId={}", policyId);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            // 唯一索引约束冲突会被捕获
            logger.error("设置默认策略失败: {}", e.getMessage());
            return false;
        }
    }

    private boolean hasUpperCase(String password) {
        return password.chars().anyMatch(Character::isUpperCase);
    }

    private boolean hasLowerCase(String password) {
        return password.chars().anyMatch(Character::isLowerCase);
    }

    private boolean hasDigit(String password) {
        return password.chars().anyMatch(Character::isDigit);
    }

    private boolean hasSpecialChar(String password) {
        return password.chars().anyMatch(ch -> "@$!%*?&".indexOf(ch) >= 0);
    }

    @Override
    public PasswordPolicy createFromDTO(com.foodtraceability.dto.PasswordPolicyDTO dto) {
        PasswordPolicy policy = new PasswordPolicy();
        policy.setPolicyName(dto.getPolicyName());
        policy.setMinLength(dto.getMinLength());
        policy.setMaxLength(dto.getMaxLength());
        policy.setRequireUppercase(dto.getRequireUppercase());
        policy.setRequireLowercase(dto.getRequireLowercase());
        policy.setRequireDigit(dto.getRequireDigit());
        policy.setRequireSpecial(dto.getRequireSpecial());
        policy.setPasswordExpiryDays(dto.getPasswordExpiryDays());
        policy.setHistoryCheckCount(dto.getHistoryCheckCount());
        policy.setMaxLoginFailures(dto.getMaxLoginFailures());
        policy.setLockDurationMinutes(dto.getLockDurationMinutes());
        policy.setStatus(1);
        policy.setIsDefault(false);
        return policy;
    }

    /**
     * 创建默认密码策略
     */
    @Transactional(rollbackFor = Exception.class)
    private PasswordPolicy createDefaultPolicy() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.setPolicyName("默认密码策略");
        policy.setMinLength(8);
        policy.setMaxLength(32);
        policy.setRequireUppercase(true);
        policy.setRequireLowercase(true);
        policy.setRequireDigit(true);
        policy.setRequireSpecial(true);
        policy.setPasswordExpiryDays(90);
        policy.setHistoryCheckCount(5);
        policy.setMaxLoginFailures(5);
        policy.setLockDurationMinutes(30);
        policy.setStatus(1);
        policy.setIsDefault(true);

        passwordPolicyMapper.insert(policy);
        logger.info("创建默认密码策略成功");
        return policy;
    }
}
