package com.foodtraceability.service.impl;

import com.foodtraceability.entity.User;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.security.service.VerificationCodeSender;
import com.foodtraceability.security.utils.PasswordUtils;
import com.foodtraceability.service.CacheService;
import com.foodtraceability.service.PasswordResetService;
import com.foodtraceability.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetServiceImpl.class);

    private static final String VERIFY_CODE_PREFIX = "verify:";
    private static final String RESET_TOKEN_PREFIX = "reset:token:";
    private static final String VERIFY_RATE_LIMIT_PREFIX = "verify:rate:";
    private static final long VERIFY_CODE_EXPIRE_MINUTES = 10;
    private static final long RESET_TOKEN_EXPIRE_MINUTES = 30;


    public PasswordResetServiceImpl(UserService userService, CacheService cacheService, VerificationCodeSender verificationCodeSender) {
        this.userService = userService;
        this.cacheService = cacheService;
        this.verificationCodeSender = verificationCodeSender;
    }

    private final UserService userService;

    private final CacheService cacheService;

    private final VerificationCodeSender verificationCodeSender;

    @Override
    public void sendVerificationCode(String usernameOrEmail, String type) {
        logger.info("发送验证码: usernameOrEmail={}, type={}", usernameOrEmail, type);

        User user = findUserByUsernameOrEmail(usernameOrEmail);

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        boolean hasEmail = user.getEmail() != null && !user.getEmail().isEmpty();
        boolean hasPhone = user.getPhone() != null && !user.getPhone().isEmpty();

        if (!hasEmail && !hasPhone) {
            throw new BusinessException("该账号未绑定邮箱或手机号，无法通过验证码重置密码");
        }

        String rateLimitKey = VERIFY_RATE_LIMIT_PREFIX + usernameOrEmail;
        String rateLimitValue = cacheService.get(rateLimitKey);
        if (rateLimitValue != null) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }

        String code = generateRandomNumericCode(6);

        if ("EMAIL".equals(type)) {
            if (!hasEmail) {
                throw new BusinessException("用户未绑定邮箱");
            }
            String codeKey = VERIFY_CODE_PREFIX + usernameOrEmail + ":EMAIL";
            cacheService.put(codeKey, code, VERIFY_CODE_EXPIRE_MINUTES);
            cacheService.put(rateLimitKey, "1", 1);

            boolean sent = verificationCodeSender.sendEmailCode(user.getEmail(), code);
            if (!sent) {
                cacheService.delete(codeKey);
                cacheService.delete(rateLimitKey);
                throw new BusinessException("验证码发送失败，请稍后重试");
            }
            logger.info("邮箱验证码发送成功: usernameOrEmail={}, email={}", usernameOrEmail, maskEmail(user.getEmail()));
        } else if ("PHONE".equals(type)) {
            if (!hasPhone) {
                throw new BusinessException("用户未绑定手机号");
            }
            String codeKey = VERIFY_CODE_PREFIX + usernameOrEmail + ":PHONE";
            cacheService.put(codeKey, code, VERIFY_CODE_EXPIRE_MINUTES);
            cacheService.put(rateLimitKey, "1", 1);

            boolean sent = verificationCodeSender.sendSmsCode(user.getPhone(), code);
            if (!sent) {
                cacheService.delete(codeKey);
                cacheService.delete(rateLimitKey);
                throw new BusinessException("验证码发送失败，请稍后重试");
            }
            logger.info("短信验证码发送成功: usernameOrEmail={}, phone={}", usernameOrEmail, maskPhone(user.getPhone()));
        } else {
            throw new BusinessException("不支持的验证码类型");
        }
    }

    @Override
    public String verifyForResetPassword(String usernameOrEmail, String emailCode, String phoneCode) {
        logger.info("验证重置密码验证码: usernameOrEmail={}", usernameOrEmail);

        User user = findUserByUsernameOrEmail(usernameOrEmail);

        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        boolean hasEmail = user.getEmail() != null && !user.getEmail().isEmpty();
        boolean hasPhone = user.getPhone() != null && !user.getPhone().isEmpty();

        if (hasEmail) {
            if (emailCode == null || emailCode.isEmpty()) {
                throw new BusinessException("邮箱验证码不能为空");
            }
            String emailCodeKey = VERIFY_CODE_PREFIX + usernameOrEmail + ":EMAIL";
            String storedEmailCode = cacheService.get(emailCodeKey);
            if (storedEmailCode == null || !storedEmailCode.equals(emailCode)) {
                throw new BusinessException("邮箱验证码错误或已过期");
            }
            cacheService.delete(emailCodeKey);
            logger.info("邮箱验证码验证成功: usernameOrEmail={}", usernameOrEmail);
        } else {
            throw new BusinessException("该账号未绑定邮箱，无法通过验证码重置密码");
        }

        if (hasPhone && phoneCode != null && !phoneCode.isEmpty()) {
            String phoneCodeKey = VERIFY_CODE_PREFIX + usernameOrEmail + ":PHONE";
            String storedPhoneCode = cacheService.get(phoneCodeKey);
            if (storedPhoneCode == null || !storedPhoneCode.equals(phoneCode)) {
                throw new BusinessException("手机验证码错误或已过期");
            }
            cacheService.delete(phoneCodeKey);
            logger.info("手机验证码验证成功: usernameOrEmail={}", usernameOrEmail);
        }

        String resetToken = UUID.randomUUID().toString();
        String tokenKey = RESET_TOKEN_PREFIX + resetToken;
        cacheService.put(tokenKey, String.valueOf(user.getId()), RESET_TOKEN_EXPIRE_MINUTES);

        String rateLimitKey = VERIFY_RATE_LIMIT_PREFIX + usernameOrEmail;
        cacheService.delete(rateLimitKey);

        logger.info("验证码验证通过，生成重置令牌: userId={}", user.getId());
        return resetToken;
    }

    @Override
    public void resetPasswordWithToken(String resetToken, String newPassword) {
        logger.info("使用令牌重置密码");

        String tokenKey = RESET_TOKEN_PREFIX + resetToken;
        String userId = cacheService.get(tokenKey);

        if (userId == null) {
            throw new BusinessException("重置令牌无效或已过期");
        }

        User user = userService.getById(Long.parseLong(userId));
        if (user == null) {
            cacheService.delete(tokenKey);
            throw new BusinessException("用户不存在");
        }

        if (!PasswordUtils.isValidPassword(newPassword)) {
            throw new BusinessException("密码强度不足，需包含大小写字母、数字和特殊字符，且长度至少8位");
        }

        user.setPassword(PasswordUtils.encryptPassword(newPassword));
        user.setLastPasswordChangeTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        userService.updateById(user);

        cacheService.delete(tokenKey);

        String username = user.getUsername();
        String email = user.getEmail();
        if (username != null) {
            cacheService.delete(VERIFY_CODE_PREFIX + username + ":EMAIL");
            cacheService.delete(VERIFY_CODE_PREFIX + username + ":PHONE");
        }
        if (email != null) {
            cacheService.delete(VERIFY_CODE_PREFIX + email + ":EMAIL");
            cacheService.delete(VERIFY_CODE_PREFIX + email + ":PHONE");
        }

        logger.info("密码重置成功: userId={}", userId);
    }

    @Override
    public boolean validateResetToken(String resetToken) {
        if (resetToken == null || resetToken.isEmpty()) {
            return false;
        }
        String key = RESET_TOKEN_PREFIX + resetToken;
        String userId = cacheService.get(key);
        return userId != null;
    }

    private User findUserByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail.contains("@")) {
            return userService.getUserByEmail(usernameOrEmail);
        } else {
            return userService.getUserByUsername(usernameOrEmail);
        }
    }

    private String generateRandomNumericCode(int length) {
        String chars = "0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        int atIndex = email.indexOf("@");
        if (atIndex <= 0) {
            return email;
        }
        String prefix = email.substring(0, Math.min(2, atIndex));
        return prefix + "***" + email.substring(atIndex);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
