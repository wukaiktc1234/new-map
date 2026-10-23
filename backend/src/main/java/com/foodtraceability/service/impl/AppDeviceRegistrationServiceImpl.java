package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.entity.AppDeviceRegistration;
import com.foodtraceability.mapper.AppDeviceRegistrationMapper;
import com.foodtraceability.service.AppDeviceRegistrationService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * APK 设备激活白名单服务实现
 */
@Service
public class AppDeviceRegistrationServiceImpl extends ServiceImpl<AppDeviceRegistrationMapper, AppDeviceRegistration>
        implements AppDeviceRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(AppDeviceRegistrationServiceImpl.class);

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    /** 激活码有效期（7 天：现场安装/交接有足够时间，无需频繁重发） */
    private static final Duration CODE_EXPIRE = Duration.ofDays(7);

    /** 激活码字符集（排除易混淆字符 0O1I） */
    private static final String CODE_CHARSET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 8;

    @Override
    @Transactional
    public AppDeviceRegistration generateActivationCode() {
        AppDeviceRegistration registration = new AppDeviceRegistration();
        registration.setActivationCode(generateCode());
        registration.setStatus(STATUS_PENDING);
        registration.setCodeExpireAt(LocalDateTime.now().plus(CODE_EXPIRE));
        // 唯一索引兜底，冲突重试
        for (int i = 0; i < 5; i++) {
            try {
                baseMapper.insert(registration);
                break;
            } catch (org.springframework.dao.DuplicateKeyException e) {
                registration.setActivationCode(generateCode());
                if (i == 4) {
                    throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "激活码生成冲突，请重试");
                }
            }
        }
        log.info("生成设备激活码：registrationId={}, code={}, operator={}",
                registration.getRegistrationId(), registration.getActivationCode(),
                SecurityUtils.getCurrentUsername());
        return registration;
    }

    @Override
    @Transactional
    public AppDeviceRegistration activate(String activationCode, String deviceId, String deviceName) {
        if (!StringUtils.hasText(activationCode) || !StringUtils.hasText(deviceId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "激活码与设备标识不能为空");
        }
        AppDeviceRegistration registration = baseMapper.selectOne(
                new LambdaQueryWrapper<AppDeviceRegistration>()
                        .eq(AppDeviceRegistration::getActivationCode, activationCode.trim().toUpperCase()));
        if (registration == null) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND, "激活码无效，请核对后重试");
        }
        if (STATUS_ACTIVE.equals(registration.getStatus())) {
            throw new BusinessException(ErrorCode.DEVICE_OPERATION_FAILED, "该激活码已被使用");
        }
        if (STATUS_DISABLED.equals(registration.getStatus())) {
            throw new BusinessException(ErrorCode.DEVICE_PERMISSION_DENIED, "该激活码已被禁用，请联系管理员");
        }
        if (registration.getCodeExpireAt() != null && registration.getCodeExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.DEVICE_PERMISSION_DENIED, "激活码已过期，请联系管理员重新发放");
        }
        registration.setDeviceId(deviceId.trim());
        registration.setDeviceName(StringUtils.hasText(deviceName) ? deviceName.trim() : null);
        registration.setStatus(STATUS_ACTIVE);
        registration.setBoundUsername(SecurityUtils.getCurrentUsername());
        registration.setActivatedAt(LocalDateTime.now());
        registration.setLastSeenAt(LocalDateTime.now());
        baseMapper.updateById(registration);
        log.info("设备激活成功：registrationId={}, deviceId={}, deviceName={}, user={}",
                registration.getRegistrationId(), deviceId, registration.getDeviceName(),
                registration.getBoundUsername());
        return registration;
    }

    @Override
    @Transactional(readOnly = true)
    public String getDeviceStatus(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return "UNKNOWN";
        }
        AppDeviceRegistration registration = findByDeviceId(deviceId.trim());
        if (registration == null) {
            return "UNKNOWN";
        }
        return registration.getStatus();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWhitelisted(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return false;
        }
        AppDeviceRegistration registration = findByDeviceId(deviceId.trim());
        return registration != null && STATUS_ACTIVE.equals(registration.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppDeviceRegistration> getPage(long current, long size, String keyword) {
        Page<AppDeviceRegistration> page = new Page<>(current, size);
        LambdaQueryWrapper<AppDeviceRegistration> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AppDeviceRegistration::getDeviceId, keyword.trim())
                    .or().like(AppDeviceRegistration::getDeviceName, keyword.trim())
                    .or().like(AppDeviceRegistration::getActivationCode, keyword.trim())
                    .or().like(AppDeviceRegistration::getBoundUsername, keyword.trim()));
        }
        wrapper.orderByDesc(AppDeviceRegistration::getCreateTime);
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public void disable(Long registrationId) {
        AppDeviceRegistration registration = getRequired(registrationId);
        registration.setStatus(STATUS_DISABLED);
        baseMapper.updateById(registration);
        log.info("禁用设备：registrationId={}, deviceId={}, operator={}",
                registrationId, registration.getDeviceId(), SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional
    public void enable(Long registrationId) {
        AppDeviceRegistration registration = getRequired(registrationId);
        registration.setStatus(STATUS_ACTIVE);
        baseMapper.updateById(registration);
        log.info("恢复启用设备：registrationId={}, deviceId={}, operator={}",
                registrationId, registration.getDeviceId(), SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional
    public void touchLastSeen(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return;
        }
        AppDeviceRegistration registration = findByDeviceId(deviceId.trim());
        if (registration != null) {
            AppDeviceRegistration update = new AppDeviceRegistration();
            update.setRegistrationId(registration.getRegistrationId());
            update.setLastSeenAt(LocalDateTime.now());
            baseMapper.updateById(update);
        }
    }

    private AppDeviceRegistration findByDeviceId(String deviceId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<AppDeviceRegistration>()
                .eq(AppDeviceRegistration::getDeviceId, deviceId));
    }

    private AppDeviceRegistration getRequired(Long registrationId) {
        AppDeviceRegistration registration = baseMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException(ErrorCode.DEVICE_NOT_FOUND, "设备登记不存在");
        }
        return registration;
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARSET.charAt(random.nextInt(CODE_CHARSET.length())));
        }
        return sb.toString();
    }
}
