package com.foodtraceability.security.service.impl;

import com.foodtraceability.security.model.MfaToken;
import com.foodtraceability.security.service.MfaService;
import com.foodtraceability.security.utils.MfaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MFA（多因素认证）服务实现
 * 基于内存 ConcurrentHashMap 存储验证码和令牌，自动清理过期项
 */
@Service
public class MfaServiceImpl implements MfaService {

    private static final Logger logger = LoggerFactory.getLogger(MfaServiceImpl.class);

    /** 验证码过期时间（秒） */
    private static final long CODE_EXPIRE_SECONDS = 300;

    /** MFA配置缓存键前缀（仅用于日志，内存存储不使用key前缀） */
    private static final String MFA_CONFIG_KEY_PREFIX = "mfa:config:";

    /** 短信验证码缓存键前缀 */
    private static final String SMS_CODE_KEY_PREFIX = "mfa:sms:";

    /** 邮箱验证码缓存键前缀 */
    private static final String EMAIL_CODE_KEY_PREFIX = "mfa:email:";

    /** MFA令牌缓存键前缀 */
    private static final String MFA_TOKEN_KEY_PREFIX = "mfa:token:";

    /** 清理阈值：存储条目超过此数量时触发清理 */
    private static final int CLEANUP_THRESHOLD = 1000;

    /** MFA令牌内存存储：token -> MfaToken */
    private final Map<String, MfaToken> mfaTokenStore = new ConcurrentHashMap<>();

    /** MFA配置内存存储：userId -> MfaConfig */
    private final Map<String, MfaConfig> mfaConfigStore = new ConcurrentHashMap<>();

    /** 短信验证码内存存储：userId -> 验证码 */
    private final Map<String, TimedEntry> smsCodeStore = new ConcurrentHashMap<>();

    /** 邮箱验证码内存存储：userId -> 验证码 */
    private final Map<String, TimedEntry> emailCodeStore = new ConcurrentHashMap<>();

    /**
     * 带过期时间的存储条目，用于验证码的过期管理
     */
    private static class TimedEntry {
        private final String value;
        private final Instant expireAt;

        TimedEntry(String value, long expireInSeconds) {
            this.value = value;
            this.expireAt = Instant.now().plusSeconds(expireInSeconds);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expireAt);
        }

        String getValue() {
            return value;
        }
    }

    @Override
    public MfaToken generateMfaToken(String userId) {
        // 触发过期清理，防止内存泄漏
        cleanupExpiredEntriesIfNeeded();

        String token = MfaUtils.generateMfaToken();
        MfaToken mfaToken = new MfaToken(token, userId, CODE_EXPIRE_SECONDS);
        mfaTokenStore.put(token, mfaToken);
        logger.debug("MFA令牌已保存到内存: userId={}", userId);

        return mfaToken;
    }

    @Override
    public boolean verifyMfaCode(String userId, String mfaToken, String code) {
        // 第一步：验证MfaToken有效性
        MfaToken storedToken = getMfaToken(mfaToken);
        if (storedToken == null) {
            logger.warn("MFA令牌不存在: userId={}", userId);
            return false;
        }
        if (!storedToken.isValid()) {
            logger.warn("MFA令牌无效或已过期: userId={}", userId);
            return false;
        }
        if (!userId.equals(storedToken.getUserId())) {
            logger.warn("MFA令牌与用户不匹配: userId={}", userId);
            return false;
        }

        // 第二步：获取用户MFA配置
        Optional<MfaConfig> configOpt = getMfaConfig(userId);
        if (configOpt.isEmpty()) {
            logger.warn("用户未配置MFA: userId={}", userId);
            return false;
        }

        MfaConfig config = configOpt.get();
        String mfaType = config.getMfaType();
        boolean verified = false;

        // 第三步：根据MFA类型调用对应验证方法
        switch (mfaType) {
            case "totp":
                String secret = config.getSecret();
                if (secret == null || secret.isEmpty()) {
                    logger.warn("用户TOTP密钥为空: userId={}", userId);
                    return false;
                }
                verified = MfaUtils.verifyTotpCode(secret, code);
                break;
            case "sms":
                verified = verifySmsCode(userId, code);
                break;
            case "email":
                verified = verifyEmailCode(userId, code);
                break;
            default:
                logger.warn("不支持的MFA类型: userId={}, mfaType={}", userId, mfaType);
                return false;
        }

        // 第四步：验证成功后标记令牌为已使用
        if (verified) {
            storedToken.markAsUsed();
            removeMfaToken(mfaToken);
            logger.info("MFA验证成功: userId={}, mfaType={}", userId, mfaType);
        } else {
            logger.warn("MFA验证失败: userId={}, mfaType={}", userId, mfaType);
        }

        return verified;
    }

    @Override
    public String generateTotpSecret(String userId) {
        // TOTP密钥通过MFA配置管理，此处仅生成
        return MfaUtils.generateTotpSecret();
    }

    @Override
    public boolean verifyTotpCode(String secret, String code) {
        return MfaUtils.verifyTotpCode(secret, code);
    }

    @Override
    public Optional<String> sendSmsCode(String userId, String phoneNumber) {
        String code = MfaUtils.generateSmsCode();
        saveCode(SMS_CODE_KEY_PREFIX, userId, code);

        // TODO: 短信服务发送验证码（当前为占位实现）
        // 日志脱敏：验证码不能明文输出
        logger.info("短信验证码已发送到 {}: ***", phoneNumber);

        return Optional.of(code);
    }

    @Override
    public Optional<String> sendEmailCode(String userId, String email) {
        String code = MfaUtils.generateEmailCode();
        saveCode(EMAIL_CODE_KEY_PREFIX, userId, code);

        // TODO: 邮件服务发送验证码（当前为占位实现）
        // 日志脱敏：验证码不能明文输出
        logger.info("邮箱验证码已发送到 {}: ***", email);

        return Optional.of(code);
    }

    @Override
    public boolean verifySmsCode(String userId, String code) {
        String storedCode = getCode(SMS_CODE_KEY_PREFIX, userId);
        if (storedCode == null) {
            logger.warn("短信验证码不存在或已过期: userId={}", userId);
            return false;
        }
        boolean result = code.equals(storedCode);
        if (result) {
            // 验证成功后删除验证码，防止重复使用
            removeCode(SMS_CODE_KEY_PREFIX, userId);
        }
        return result;
    }

    @Override
    public boolean verifyEmailCode(String userId, String code) {
        String storedCode = getCode(EMAIL_CODE_KEY_PREFIX, userId);
        if (storedCode == null) {
            logger.warn("邮箱验证码不存在或已过期: userId={}", userId);
            return false;
        }
        boolean result = code.equals(storedCode);
        if (result) {
            // 验证成功后删除验证码，防止重复使用
            removeCode(EMAIL_CODE_KEY_PREFIX, userId);
        }
        return result;
    }

    @Override
    public boolean enableMfa(String userId, String mfaType, String secret) {
        MfaConfig config = new MfaConfig();
        config.setEnabled(true);
        config.setMfaType(mfaType);
        config.setSecret(secret);
        config.setVerified(true);

        mfaConfigStore.put(userId, config);
        logger.info("MFA配置已保存: userId={}, mfaType={}", userId, mfaType);

        return true;
    }

    @Override
    public boolean disableMfa(String userId) {
        MfaConfig config = mfaConfigStore.get(userId);
        if (config != null) {
            config.setEnabled(false);
            config.setVerified(false);
        } else {
            mfaConfigStore.remove(userId);
        }
        logger.info("MFA已禁用: userId={}", userId);
        return true;
    }

    @Override
    public boolean isMfaEnabled(String userId) {
        Optional<MfaConfig> configOpt = getMfaConfig(userId);
        return configOpt.isPresent() && configOpt.get().isEnabled() && configOpt.get().isVerified();
    }

    @Override
    public Optional<MfaConfig> getMfaConfig(String userId) {
        MfaConfig config = mfaConfigStore.get(userId);
        return Optional.ofNullable(config);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 获取MFA令牌
     * @param token 令牌字符串
     * @return MfaToken对象，不存在返回null
     */
    private MfaToken getMfaToken(String token) {
        return mfaTokenStore.get(token);
    }

    /**
     * 删除MFA令牌
     * @param token 令牌字符串
     */
    private void removeMfaToken(String token) {
        mfaTokenStore.remove(token);
    }

    /**
     * 保存验证码到内存
     * @param keyPrefix 缓存键前缀
     * @param userId 用户ID
     * @param code 验证码
     */
    private void saveCode(String keyPrefix, String userId, String code) {
        TimedEntry entry = new TimedEntry(code, CODE_EXPIRE_SECONDS);
        if (SMS_CODE_KEY_PREFIX.equals(keyPrefix)) {
            smsCodeStore.put(userId, entry);
        } else if (EMAIL_CODE_KEY_PREFIX.equals(keyPrefix)) {
            emailCodeStore.put(userId, entry);
        }
        logger.debug("验证码已保存到内存: keyPrefix={}, userId={}", keyPrefix, userId);
    }

    /**
     * 获取验证码（自动检查过期）
     * @param keyPrefix 缓存键前缀
     * @param userId 用户ID
     * @return 验证码，不存在或已过期返回null
     */
    private String getCode(String keyPrefix, String userId) {
        TimedEntry entry = null;
        if (SMS_CODE_KEY_PREFIX.equals(keyPrefix)) {
            entry = smsCodeStore.get(userId);
        } else if (EMAIL_CODE_KEY_PREFIX.equals(keyPrefix)) {
            entry = emailCodeStore.get(userId);
        }

        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            removeCode(keyPrefix, userId);
            return null;
        }
        return entry.getValue();
    }

    /**
     * 删除验证码
     * @param keyPrefix 缓存键前缀
     * @param userId 用户ID
     */
    private void removeCode(String keyPrefix, String userId) {
        if (SMS_CODE_KEY_PREFIX.equals(keyPrefix)) {
            smsCodeStore.remove(userId);
        } else if (EMAIL_CODE_KEY_PREFIX.equals(keyPrefix)) {
            emailCodeStore.remove(userId);
        }
    }

    /**
     * 清理过期条目，防止内存泄漏
     * 当存储条目超过阈值时触发清理
     */
    private void cleanupExpiredEntriesIfNeeded() {
        if (mfaTokenStore.size() < CLEANUP_THRESHOLD
                && smsCodeStore.size() < CLEANUP_THRESHOLD
                && emailCodeStore.size() < CLEANUP_THRESHOLD) {
            return;
        }

        // 清理过期的短信验证码
        smsCodeStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
        // 清理过期的邮箱验证码
        emailCodeStore.entrySet().removeIf(entry -> entry.getValue().isExpired());
        // 清理无效的MFA令牌
        mfaTokenStore.entrySet().removeIf(entry -> !entry.getValue().isValid());

        logger.debug("内存清理完成: mfaToken={}, smsCode={}, emailCode={}",
                mfaTokenStore.size(), smsCodeStore.size(), emailCodeStore.size());
    }
}
