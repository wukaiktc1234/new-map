package com.foodtraceability.service.impl;

import com.foodtraceability.dto.*;
import com.foodtraceability.entity.InvitationSendRecord;
import com.foodtraceability.entity.OnboardingArchive;
import com.foodtraceability.entity.User;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.exception.CaptchaRequiredException;
import com.foodtraceability.mapper.UserMapper;
import com.foodtraceability.security.model.JwtToken;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.security.constants.AdminPermissions;
import com.foodtraceability.security.service.TokenService;
import com.foodtraceability.security.utils.JwtUtils;
import com.foodtraceability.security.utils.PasswordUtils;
import com.foodtraceability.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthServiceImpl implements AuthService {

    // 管理员权限常量已迁移至 AdminPermissions.ALL
    // 保留此引用仅为向后兼容（如有外部引用），实际使用请直接调用 AdminPermissions.ALL
    private static final Set<String> ALL_PERMISSIONS = AdminPermissions.ALL;

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final String EMAIL_CODE_PREFIX = "email:code:";
    private static final long EMAIL_CODE_EXPIRE_MINUTES = 10;
    // TODO: 开发调试阶段临时禁用验证码触发（阈值设到极大），前端/后端验收完成后恢复为 3
    private static final int CAPTCHA_REQUIRED_THRESHOLD = 99999;


    public AuthServiceImpl(UserService userService, UserMapper userMapper, JwtUtils jwtUtils, JavaMailSender mailSender, TokenService tokenService, OnboardingInvitationService onboardingInvitationService, OnboardingArchiveService onboardingArchiveService, CacheService cacheService, CaptchaService captchaService, PasswordResetService passwordResetService, MenuService menuService, SessionService sessionService, PasswordPolicyService passwordPolicyService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
        this.mailSender = mailSender;
        this.tokenService = tokenService;
        this.onboardingInvitationService = onboardingInvitationService;
        this.onboardingArchiveService = onboardingArchiveService;
        this.cacheService = cacheService;
        this.captchaService = captchaService;
        this.passwordResetService = passwordResetService;
        this.menuService = menuService;
        this.sessionService = sessionService;
        this.passwordPolicyService = passwordPolicyService;
    }

    private final UserService userService;

    private final UserMapper userMapper;

    private final JwtUtils jwtUtils;

    private final JavaMailSender mailSender;

    private final TokenService tokenService;

    private final OnboardingInvitationService onboardingInvitationService;

    private final OnboardingArchiveService onboardingArchiveService;

    private final CacheService cacheService;

    private final CaptchaService captchaService;

    private final PasswordResetService passwordResetService;

    private final MenuService menuService;
    
    private final SessionService sessionService;
    
    private final PasswordPolicyService passwordPolicyService;

    @Override
    public CaptchaResponse generateCaptcha() {
        return captchaService.generateCaptcha();
    }

    @Override
    public boolean validateCaptcha(String captchaId, String captcha) {
        return captchaService.validateCaptcha(captchaId, captcha);
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        logger.info("用户登录: username={}", loginRequest.getUsername());

        User user = userService.getUserByUsername(loginRequest.getUsername());
        if (user == null) {
            logger.debug("登录失败: 用户名或密码错误");
            throw new BusinessException(401, "用户名或密码错误");
        }

        int currentFailCount = user.getPasswordErrorCount() != null ? user.getPasswordErrorCount() : 0;
        boolean captchaRequired = currentFailCount >= CAPTCHA_REQUIRED_THRESHOLD;

        if (captchaRequired) {
            String captchaId = loginRequest.getCaptchaId();
            String captcha = loginRequest.getCaptcha();

            if (isEmpty(captchaId) || isEmpty(captcha)) {
                logger.warn("需要验证码但未提供: username={}, failCount={}", loginRequest.getUsername(), currentFailCount);
                CaptchaResponse captchaResponse = captchaService.generateCaptcha();
                LoginResponse response = new LoginResponse();
                response.setRequireCaptcha(true);
                response.setCaptchaId(captchaResponse.getCaptchaId());
                response.setCaptchaImage(captchaResponse.getCaptchaImage());
                response.setFailedAttempts(currentFailCount);
                throw new CaptchaRequiredException("登录失败次数过多，请输入验证码", response);
            }

            boolean captchaValid = captchaService.validateCaptcha(captchaId, captcha);
            if (!captchaValid) {
                logger.warn("验证码错误: username={}, captchaId={}", loginRequest.getUsername(), captchaId);
                CaptchaResponse captchaResponse = captchaService.generateCaptcha();
                LoginResponse response = new LoginResponse();
                response.setRequireCaptcha(true);
                response.setCaptchaId(captchaResponse.getCaptchaId());
                response.setCaptchaImage(captchaResponse.getCaptchaImage());
                response.setFailedAttempts(currentFailCount);
                throw new CaptchaRequiredException("验证码错误，请重新输入", response);
            }
        }

        // 修复：User.status 是 Integer 类型，不能用 String "1"/"active" 比较
        // （String.equals(非String) 永远返回 false，导致管理端永远判定为禁用）
        // 统一采用 status != 1 表示禁用，与 AuthenticationServiceImpl 保持一致
        if (user.getStatus() == null || user.getStatus() != 1) {
            logger.warn("用户已被禁用: username={}, status={}", loginRequest.getUsername(), user.getStatus());
            throw new BusinessException(401, "账号已被禁用，请联系管理员");
        }

        if (Boolean.TRUE.equals(user.getIsLocked()) && user.getLockTime() != null) {
            if (user.getLockTime().plusMinutes(30).isAfter(LocalDateTime.now())) {
                logger.warn("账户已锁定，登录被拒绝: username={}", loginRequest.getUsername());
                throw new BusinessException(401, "账号已被锁定，请30分钟后再试");
            } else {
                user.setIsLocked(false);
                user.setPasswordErrorCount(0);
                user.setLockTime(null);
                userService.updateById(user);
                logger.info("账户锁定已过期，自动解锁: username={}", loginRequest.getUsername());
            }
        }

        boolean passwordMatches = PasswordUtils.matches(loginRequest.getPassword(), user.getPassword());
        if (!passwordMatches) {
            int errorCount = (user.getPasswordErrorCount() != null ? user.getPasswordErrorCount() : 0) + 1;
            user.setPasswordErrorCount(errorCount);
            user.setLastPasswordErrorTime(LocalDateTime.now());

            if (errorCount >= 5) {
                user.setIsLocked(true);
                user.setLockTime(LocalDateTime.now());
                logger.warn("账户因密码错误次数过多被锁定: username={}, errorCount={}", loginRequest.getUsername(), errorCount);
            }
            userService.updateById(user);

            logger.warn("密码错误: username={}, errorCount={}", loginRequest.getUsername(), errorCount);
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (user.getPasswordErrorCount() != null && user.getPasswordErrorCount() > 0) {
            user.setPasswordErrorCount(0);
            user.setIsLocked(false);
            user.setLockTime(null);
        }

        SecurityUser securityUser = buildSecurityUser(user);

        String token = jwtUtils.generateToken(securityUser);
        String refreshToken = jwtUtils.generateRefreshToken(securityUser);

        jwtUtils.storeTokenInfo(String.valueOf(user.getId()), token, refreshToken);

        user.setLastLoginTime(LocalDateTime.now());
        userService.updateById(user);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setRefreshToken(refreshToken);

        UserInfoDTO userInfo = new UserInfoDTO();
        userInfo.setId(String.valueOf(user.getId()));
        userInfo.setUsername(user.getUsername());
        userInfo.setName(user.getFullName());
        userInfo.setEmail(maskEmail(user.getEmail()));
        userInfo.setAvatar(user.getAvatar());
        userInfo.setRoles(securityUser.getRoles());
        // 前端需要完整权限列表用于菜单/按钮可见性控制
        // securityUser.getPermissions() 只含 "*"（admin），需展开为完整列表
        userInfo.setPermissions(expandPermissionsForFrontend(securityUser.getPermissions()));

        response.setUserInfo(userInfo);

        logger.info("用户登录成功: username={}", loginRequest.getUsername());
        return response;
    }

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        logger.info("用户注册: username={}", registerRequest.getUsername());

        if (!(registerRequest instanceof InviteRegisterRequest)) {
            throw new BusinessException("请使用邀请码注册");
        }

        InviteRegisterRequest inviteRequest = (InviteRegisterRequest) registerRequest;
        String invitationCode = inviteRequest.getInvitationCode();

        InvitationSendRecord invitationRecord = onboardingInvitationService.getInvitationCode(invitationCode);
        OnboardingArchive archive = null;

        if (invitationRecord != null) {
            boolean valid = onboardingInvitationService.validateInvitationCode(
                    invitationCode,
                    inviteRequest.getEmail(),
                    inviteRequest.getPhone(),
                    inviteRequest.getFullName()
            );
            if (!valid) {
                throw new BusinessException("邀请码无效或已过期");
            }
            archive = onboardingArchiveService.getById(invitationRecord.getArchiveId());
        } else {
            throw new BusinessException("邀请码不存在或已失效");
        }

        if (userService.checkUsernameExists(registerRequest.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        if (registerRequest.getEmail() != null && userService.checkEmailExists(registerRequest.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }

        if (registerRequest.getPhone() != null && userService.checkPhoneExists(registerRequest.getPhone())) {
            throw new BusinessException("手机号已被注册");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(PasswordUtils.encryptPassword(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setPhone(registerRequest.getPhone());
        user.setFullName(inviteRequest.getFullName());
        user.setStatus(1);

        if (archive != null) {
            user.setDepartmentId(archive.getDepartmentId() != null ? Long.valueOf(archive.getDepartmentId()) : null);
            user.setStoreId(null);
            user.setRoles(archive.getRoleId() != null ? "[\"" + archive.getRoleId() + "\"]" : "[]");
            user.setEmployeeCode(archive.getEmployeeCode());
        }

        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());

        userService.save(user);

        if (invitationRecord != null) {
            onboardingInvitationService.useInvitationCode(invitationCode, user.getId());
            if (archive != null) {
                archive.setStatus(OnboardingArchive.STATUS_REGISTERED);
                archive.setUserId(user.getId());
                onboardingArchiveService.updateById(archive);
                logger.info("档案状态已更新为已注册，档案ID：{}，用户ID：{}", archive.getId(), user.getId());
            }
        }

        RegisterResponse response = new RegisterResponse();
        response.setId(String.valueOf(user.getId()));
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());

        logger.info("用户注册成功: username={}, 员工编号={}", registerRequest.getUsername(),
                archive != null ? archive.getEmployeeCode() : "N/A");
        return response;
    }

    @Override
    public UserInfoResponse getUserInfo(String userId) {
        logger.info("获取用户信息: userId={}", userId);

        if (userId == null || userId.isEmpty()) {
            throw new BusinessException("用户ID不能为空");
        }

        try {
            Long userIdLong = Long.parseLong(userId);
            User user = userService.getById(userIdLong);

            if (user == null) {
                logger.warn("用户不存在: userId={}", userId);
                throw new BusinessException("用户不存在");
            }

            UserInfoResponse response = new UserInfoResponse();
            response.setId(String.valueOf(user.getId()));
            response.setUsername(user.getUsername());
            response.setName(user.getFullName());
            response.setEmail(maskEmail(user.getEmail()));
            response.setPhone(maskPhone(user.getPhone()));
            response.setStatus(user.getStatus());
            response.setAvatar(user.getAvatar());
            response.setDepartmentId(user.getDepartmentId());
            response.setDepartment(user.getDepartment());
            response.setStoreId(user.getStoreId());
            response.setStoreName(user.getStoreName());
            response.setCreatedAt(user.getCreatedTime());
            response.setUpdatedAt(user.getUpdatedTime());

            List<String> roles = userService.getUserRoleNames(user.getId());
            response.setRoles(roles);

            List<String> permissions = getUserPermissions(user.getId());

            boolean isAdmin = "admin".equals(user.getUsername());
            if (!isAdmin) {
                List<String> userRoles = userMapper.getUserRoles(user.getId());
                if (userRoles != null) {
                    for (String role : userRoles) {
                        String normalizedRole = role.toLowerCase();
                        if ("admin".equalsIgnoreCase(role)
                            || "z".equals(normalizedRole)
                            || "role_z".equals(normalizedRole)) {
                            isAdmin = true;
                            break;
                        }
                    }
                }
            }
            if (isAdmin && !permissions.contains(AdminPermissions.WILDCARD)) {
                // admin 用户确保有 "*" 通配符
                permissions.add(AdminPermissions.WILDCARD);
            }

            // 前端需要完整权限列表用于菜单/按钮可见性控制
            // 展开通配符 "*" 为完整权限码列表
            response.setPermissions(expandPermissionsForFrontend(permissions));

            return response;
        } catch (NumberFormatException e) {
            logger.error("用户ID格式错误: userId={}", userId, e);
            throw new BusinessException("用户ID格式错误");
        } catch (Exception e) {
            logger.error("获取用户信息失败: userId={}", userId, e);
            throw new BusinessException("获取用户信息失败: " + e.getMessage());
        }
    }

    @Override
    public void logout(String token) {
        logger.info("用户登出");

        if (token != null && !token.isEmpty()) {
            jwtUtils.addToBlacklist(token);

            String userId = getUserIdFromToken(token);
            if (userId != null) {
                jwtUtils.removeTokenInfo(userId);
                // 安全修复（P1-2）：清理 SessionService 中的会话记录
                // 原实现仅将token加入黑名单并移除token信息，但未清理Redis中的会话记录，
                // 导致用户登出后仍可在会话列表中看到已登出的设备
                try {
                    sessionService.removeSessionByToken(userId, token);
                } catch (Exception e) {
                    logger.warn("清理会话记录失败（不影响登出流程）: userId={}", userId, e);
                }
            }
        }
    }

    @Override
    public void changePassword(String userId, ChangePasswordRequest changePasswordRequest) {
        logger.info("修改密码: userId={}", userId);

        if (userId == null || userId.isEmpty()) {
            throw new BusinessException("用户ID不能为空");
        }

        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!PasswordUtils.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        
        String validationResult = passwordPolicyService.validatePasswordStrength(changePasswordRequest.getNewPassword());
        if (validationResult != null) {
            throw new BusinessException(validationResult);
        }

        user.setPassword(PasswordUtils.encryptPassword(changePasswordRequest.getNewPassword()));
        user.setLastPasswordChangeTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        userService.updateById(user);

        logger.info("密码修改成功: userId={}", userId);
    }

    @Override
    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {
        logger.info("重置密码: email={}", resetPasswordRequest.getEmail());

        User user = userService.getUserByEmail(resetPasswordRequest.getEmail());
        if (user == null) {
            throw new BusinessException("该邮箱未注册");
        }
        
        String validationResult = passwordPolicyService.validatePasswordStrength(resetPasswordRequest.getNewPassword());
        if (validationResult != null) {
            throw new BusinessException(validationResult);
        }

        String key = EMAIL_CODE_PREFIX + resetPasswordRequest.getEmail();
        String storedCode = cacheService.get(key);
        if (storedCode == null || !storedCode.equals(resetPasswordRequest.getCaptcha())) {
            throw new BusinessException("验证码错误或已过期");
        }

        user.setPassword(PasswordUtils.encryptPassword(resetPasswordRequest.getNewPassword()));
        user.setLastPasswordChangeTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        userService.updateById(user);

        cacheService.delete(key);

        logger.info("密码重置成功: email={}", resetPasswordRequest.getEmail());
    }

    @Override
    public String refreshToken(String token) {
        logger.info("刷新令牌");

        Optional<JwtToken> jwtToken = tokenService.refreshToken(token);
        if (jwtToken.isPresent()) {
            JwtToken newToken = jwtToken.get();
            // TokenServiceImpl.refreshToken内部已通过generateToken完成令牌存储和jti列表管理
            // 此处无需再次调用storeTokenInfo，避免重复存储
            return newToken.getToken();
        }

        throw new BusinessException("刷新令牌失败");
    }

    @Override
    public boolean checkUsername(String username) {
        return !userService.checkUsernameExists(username);
    }

    @Override
    public boolean checkEmail(String email) {
        return !userService.checkEmailExists(email);
    }

    @Override
    public void sendEmailCode(String email, String type) {
        logger.info("发送邮箱验证码: email={}, type={}", email, type);

        String code = generateRandomNumericCode(6);

        String key = EMAIL_CODE_PREFIX + email;
        cacheService.put(key, code, EMAIL_CODE_EXPIRE_MINUTES);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);

            if ("register".equals(type)) {
                helper.setSubject("注册验证码");
                helper.setText("您的注册验证码是: " + code + "，有效期" + EMAIL_CODE_EXPIRE_MINUTES + "分钟。");
            } else if ("reset".equals(type)) {
                helper.setSubject("密码重置验证码");
                helper.setText("您的密码重置验证码是: " + code + "，有效期" + EMAIL_CODE_EXPIRE_MINUTES + "分钟。");
            } else {
                helper.setSubject("验证码");
                helper.setText("您的验证码是: " + code + "，有效期" + EMAIL_CODE_EXPIRE_MINUTES + "分钟。");
            }

            mailSender.send(message);
            logger.info("邮箱验证码发送成功: email={}", email);
        } catch (Exception e) {
            logger.error("发送邮件失败", e);
            throw new BusinessException("发送验证码失败，请稍后重试");
        }
    }

    @Override
    public boolean verifyEmailCode(String email, String code) {
        String key = EMAIL_CODE_PREFIX + email;
        String storedCode = cacheService.get(key);
        return storedCode != null && storedCode.equals(code);
    }

    @Override
    public PermissionResponse getPermissions(String userId) {
        logger.info("获取用户权限: userId={}", userId);

        PermissionResponse response = new PermissionResponse();

        List<String> roles = userService.getUserRoleNames(Long.parseLong(userId));
        response.setRoles(roles);

        List<String> permissions = getUserPermissions(Long.parseLong(userId));
        // 展开通配符 "*" 为完整权限码列表（前端需要完整列表用于菜单/按钮可见性）
        response.setPermissions(expandPermissionsForFrontend(permissions));

        return response;
    }

    /**
     * 展开通配符 "*" 为完整权限码列表
     * 用于前端响应（userInfo/permissions 接口），前端需要完整权限列表做菜单/按钮可见性控制。
     * 注意：不要在 JWT token 中存储展开后的列表，否则 token 会超过 8KB 限制。
     *
     * @param permissions 原始权限列表（可能含 "*"）
     * @return 展开后的权限列表
     */
    private List<String> expandPermissionsForFrontend(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }
        if (!permissions.contains(AdminPermissions.WILDCARD)) {
            return permissions;
        }
        // 包含 "*" → 展开为完整权限集
        List<String> expanded = new ArrayList<>(AdminPermissions.ALL);
        // 保留原始非通配符权限（去重）
        for (String p : permissions) {
            if (!AdminPermissions.WILDCARD.equals(p) && !expanded.contains(p)) {
                expanded.add(p);
            }
        }
        return expanded;
    }

    @Override
    public MenuResponse getMenus(String userId) {
        return menuService.getMenus(userId);
    }

    @Override
    public UserInfoResponse updateUserInfo(String userId, UpdateUserRequest updateUserRequest) {
        logger.info("更新用户信息: userId={}", userId);

        if (userId == null || userId.isEmpty()) {
            throw new BusinessException("用户ID不能为空");
        }

        User user = userService.getById(Long.parseLong(userId));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (updateUserRequest.getName() != null) {
            user.setFullName(updateUserRequest.getName());
        }
        if (updateUserRequest.getEmail() != null) {
            user.setEmail(updateUserRequest.getEmail());
        }
        if (updateUserRequest.getPhone() != null) {
            user.setPhone(updateUserRequest.getPhone());
        }
        if (updateUserRequest.getAvatar() != null) {
            user.setAvatar(updateUserRequest.getAvatar());
        }
        if (updateUserRequest.getStoreId() != null) {
            user.setStoreId(Long.valueOf(updateUserRequest.getStoreId()));
        }
        if (updateUserRequest.getStoreName() != null) {
            user.setStoreName(updateUserRequest.getStoreName());
        }
        if (updateUserRequest.getDepartmentId() != null) {
            user.setDepartmentId(Long.valueOf(updateUserRequest.getDepartmentId()));
        }
        if (updateUserRequest.getDepartment() != null) {
            user.setDepartment(updateUserRequest.getDepartment());
        }

        user.setUpdatedTime(LocalDateTime.now());
        userService.updateById(user);

        logger.info("用户信息更新成功: userId={}", userId);

        return getUserInfo(userId);
    }

    @Override
    public User getUserByUsername(String username) {
        return userService.getUserByUsername(username);
    }

    @Override
    public User getUserByEmail(String email) {
        return userService.getUserByEmail(email);
    }

    @Override
    public String getUserIdFromToken(String token) {
        Optional<SecurityUser> user = jwtUtils.getUserFromToken(token);
        return user.map(SecurityUser::getUserId).orElse(null);
    }

    @Override
    public void sendVerificationCode(String usernameOrEmail, String type) {
        passwordResetService.sendVerificationCode(usernameOrEmail, type);
    }

    @Override
    public String verifyForResetPassword(String usernameOrEmail, String emailCode, String phoneCode) {
        return passwordResetService.verifyForResetPassword(usernameOrEmail, emailCode, phoneCode);
    }

    @Override
    public void resetPasswordWithToken(String resetToken, String newPassword) {
        passwordResetService.resetPasswordWithToken(resetToken, newPassword);
    }

    @Override
    public boolean validateResetToken(String resetToken) {
        return passwordResetService.validateResetToken(resetToken);
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

    /**
     * 构建安全用户对象
     * 包含用户基本信息、角色和权限
     *
     * @param user 用户实体
     * @return 安全用户信息
     */
    private SecurityUser buildSecurityUser(User user) {
        logger.debug("开始构建SecurityUser: userId={}, username={}", user.getId(), user.getUsername());

        SecurityUser securityUser = new SecurityUser();
        securityUser.setUserId(String.valueOf(user.getId()));
        securityUser.setUsername(user.getUsername());
        securityUser.setPassword(user.getPassword());
        securityUser.setName(user.getFullName());
        securityUser.setEmail(user.getEmail());
        securityUser.setPhone(user.getPhone());
        securityUser.setStatus(user.getStatus());

        List<String> roles;
        boolean isAdmin = "admin".equals(user.getUsername());
        if (isAdmin) {
            // admin用户使用硬编码角色
            roles = Arrays.asList("admin", "user");
            logger.debug("admin用户使用硬编码角色: userId={}, roles={}", user.getId(), roles);
        } else {
            try {
                roles = userMapper.getUserRoles(user.getId());
                if (roles == null) {
                    roles = new ArrayList<>();
                    logger.warn("用户角色查询返回null，使用空列表: userId={}", user.getId());
                }
                for (String role : roles) {
                    if (role == null) {
                        continue;  // 跳过null角色
                    }
                    String normalizedRole = role.toLowerCase();
                    if ("admin".equalsIgnoreCase(role)
                        || "z".equals(normalizedRole)
                        || "role_z".equals(normalizedRole)) {
                        isAdmin = true;
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("查询用户角色失败: userId={}, error={}", user.getId(), e.getMessage());
                roles = new ArrayList<>();  // 降级处理：使用空角色列表
            }
        }
        securityUser.setRoles(roles);

        Set<String> permissions = new HashSet<>();

        try {
            // 查询直接权限
            List<String> directPermissions = userMapper.getUserPermissions(user.getId());
            if (directPermissions != null) {
                permissions.addAll(directPermissions);
            }

            // 查询角色权限
            List<String> rolePermissions = userMapper.getUserRolePermissions(user.getId());
            if (rolePermissions != null) {
                permissions.addAll(rolePermissions);
            }
        } catch (Exception e) {
            logger.error("查询用户权限失败: userId={}, error={}", user.getId(), e.getMessage());
            // 降级处理：继续使用已获取的权限（可能为空）
        }

        if (isAdmin) {
            // 优化：admin 用户只添加 "*" 通配符，不添加所有权限码。
            // 原因：ALL_PERMISSIONS 有 150+ 权限码，全部存入 JWT token 会导致 token 超过 9KB，
            //       超过 Tomcat 默认 8KB header 限制，所有 API 返回 400 Bad Request。
            // 方案：JWT token 只存 "*"，SecurityUser.getAuthorities() 运行时展开为完整权限集。
            // 前端 userInfo 仍返回完整权限列表（见 login() 和 getUserInfo()）。
            permissions.add(AdminPermissions.WILDCARD);
            logger.debug("管理员用户添加通配权限 '*': userId={}", user.getId());
        }

        securityUser.setPermissions(new ArrayList<>(permissions));

        logger.info("SecurityUser构建完成: userId={}, username={}, rolesCount={}, permissionsCount={}",
                user.getId(),
                user.getUsername(),
                roles.size(),
                permissions.size());

        return securityUser;
    }

    private List<String> getUserPermissions(Long userId) {
        Set<String> permissions = new HashSet<>();

        List<String> directPermissions = userMapper.getUserPermissions(userId);
        if (directPermissions != null) {
            permissions.addAll(directPermissions);
        }

        List<String> rolePermissions = userMapper.getUserRolePermissions(userId);
        if (rolePermissions != null) {
            permissions.addAll(rolePermissions);
        }

        return new ArrayList<>(permissions);
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

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    @Override
    public void resetLoginFailCount(String username) {
        logger.info("重置用户登录失败计数: username={}", username);

        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new BusinessException("用户不存在: " + username);
        }

        user.setPasswordErrorCount(0);
        user.setIsLocked(false);
        user.setLockTime(null);
        user.setLastPasswordErrorTime(null);
        userService.updateById(user);

        logger.info("登录失败计数已重置: username={}", username);
    }

    /**
     * 校验邀请码有效性（注册前预校验）
     * 仅校验邀请码是否存在、未使用、未过期，不校验绑定信息（绑定信息在注册提交时校验）
     *
     * @param code 邀请码
     * @return Map 包含：valid、boundName、message
     */
    @Override
    public Map<String, Object> validateInvitationCode(String code) {
        Map<String, Object> result = new java.util.HashMap<>();
        if (code == null || code.trim().isEmpty()) {
            result.put("valid", false);
            result.put("message", "邀请码不能为空");
            return result;
        }
        // 调用 OnboardingInvitationService 真实校验（不传绑定信息，仅校验邀请码本身有效性）
        boolean valid = onboardingInvitationService.validateInvitationCode(code.trim(), null, null, null);
        result.put("valid", valid);
        if (valid) {
            // 返回绑定姓名用于前端友好提示
            InvitationSendRecord record = onboardingInvitationService.getInvitationCode(code.trim());
            if (record != null && record.getBoundName() != null) {
                result.put("boundName", record.getBoundName());
            }
            result.put("message", "邀请码有效");
        } else {
            result.put("message", "邀请码无效或已被使用");
        }
        return result;
    }
}
