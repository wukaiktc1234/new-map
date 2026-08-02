package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.OnboardingRecord;
import com.foodtraceability.entity.RegistrationCode;
import com.foodtraceability.entity.RegistrationCodeLog;
import com.foodtraceability.mapper.OnboardingRecordMapper;
import com.foodtraceability.mapper.RegistrationCodeLogMapper;
import com.foodtraceability.mapper.RegistrationCodeMapper;
import com.foodtraceability.service.RegistrationCodeService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注册码服务实现类
 */
@Service
public class RegistrationCodeServiceImpl implements RegistrationCodeService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationCodeServiceImpl.class);


    public RegistrationCodeServiceImpl(RegistrationCodeMapper registrationCodeMapper, RegistrationCodeLogMapper registrationCodeLogMapper, OnboardingRecordMapper onboardingRecordMapper) {
        this.registrationCodeMapper = registrationCodeMapper;
        this.registrationCodeLogMapper = registrationCodeLogMapper;
        this.onboardingRecordMapper = onboardingRecordMapper;
    }

    private final RegistrationCodeMapper registrationCodeMapper;

    private final RegistrationCodeLogMapper registrationCodeLogMapper;

    private final OnboardingRecordMapper onboardingRecordMapper;

    // 内存验证码缓存：手机号 -> 验证码条目
    private final ConcurrentHashMap<String, SmsCodeEntry> smsCodeCache = new ConcurrentHashMap<>();

    // 手机验证码有效期（分钟）
    private static final long SMS_CODE_EXPIRATION_MINUTES = 5;
    // 手机验证码长度
    private static final int SMS_CODE_LENGTH = 6;

    /**
     * 短信验证码内存缓存条目，记录验证码和过期时间
     */
    private static class SmsCodeEntry {
        final String code;
        final long expireTimeMillis;

        SmsCodeEntry(String code, long expireTimeMillis) {
            this.code = code;
            this.expireTimeMillis = expireTimeMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTimeMillis;
        }
    }

    @Override
    @Transactional
    public RegistrationCode generateCode(GenerateCodeRequest request) {
        // 生成唯一注册码
        String code = "RC" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        // 创建注册码实体
        RegistrationCode registrationCode = new RegistrationCode();
        registrationCode.setCode(code);
        registrationCode.setType(request.getType() != null ? request.getType() : "INTERNAL");
        registrationCode.setValidityStart(request.getValidityStart() != null ? request.getValidityStart() : LocalDateTime.now());
        registrationCode.setValidityEnd(request.getValidityEnd() != null ? request.getValidityEnd() : LocalDateTime.now().plusDays(30));
        registrationCode.setStatus("UNUSED");
        registrationCode.setCreatedBy(request.getCreatedBy());
        registrationCode.setCreatedAt(LocalDateTime.now());
        registrationCode.setUpdatedAt(LocalDateTime.now());

        // 保存到数据库
        registrationCodeMapper.insert(registrationCode);

        return registrationCode;
    }

    @Override
    @Transactional
    public List<RegistrationCode> batchGenerateCode(BatchGenerateCodeRequest request) {
        List<RegistrationCode> generatedCodes = new ArrayList<>();

        for (int i = 0; i < request.getCount(); i++) {
            // 生成唯一注册码
            String code = "RC" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

            // 创建注册码实体
            RegistrationCode registrationCode = new RegistrationCode();
            registrationCode.setCode(code);
            registrationCode.setType(request.getType() != null ? request.getType() : "INTERNAL");
            registrationCode.setValidityStart(request.getValidityStart() != null ? request.getValidityStart() : LocalDateTime.now());
            registrationCode.setValidityEnd(request.getValidityEnd() != null ? request.getValidityEnd() : LocalDateTime.now().plusDays(30));
            registrationCode.setStatus("UNUSED");
            registrationCode.setCreatedBy(request.getCreatedBy());
            registrationCode.setCreatedAt(LocalDateTime.now());
            registrationCode.setUpdatedAt(LocalDateTime.now());

            // 保存到数据库
            registrationCodeMapper.insert(registrationCode);
            generatedCodes.add(registrationCode);
        }

        return generatedCodes;
    }

    @Override
    public boolean validateCode(String code) {
        if (StringUtils.isBlank(code)) {
            return false;
        }

        // 查询注册码
        RegistrationCode registrationCode = registrationCodeMapper.findByCode(code);
        if (registrationCode == null) {
            return false;
        }

        // 检查状态
        if (!"UNUSED".equals(registrationCode.getStatus())) {
            return false;
        }

        // 检查有效期
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(registrationCode.getValidityStart()) || now.isAfter(registrationCode.getValidityEnd())) {
            return false;
        }

        return true;
    }

    @Override
    @Transactional
    public boolean useCode(String code, String userId, String username, String ipAddress) {
        RegistrationCode registrationCode = registrationCodeMapper.findByCode(code);
        
        if (!validateCode(code)) {
            RegistrationCodeLog log = new RegistrationCodeLog();
            log.setCode(code);
            log.setCodeId(registrationCode != null ? registrationCode.getId() : null);
            log.setAction("USE");
            log.setUserId(userId);
            log.setUsername(username);
            log.setUseTime(LocalDateTime.now());
            log.setStatus("FAILED");
            log.setIpAddress(ipAddress);
            log.setCreatedAt(LocalDateTime.now());
            registrationCodeLogMapper.insert(log);
            return false;
        }

        int updated = registrationCodeMapper.updateToUsed(code);
        if (updated == 0) {
            RegistrationCodeLog log = new RegistrationCodeLog();
            log.setCode(code);
            log.setCodeId(registrationCode != null ? registrationCode.getId() : null);
            log.setAction("USE");
            log.setUserId(userId);
            log.setUsername(username);
            log.setUseTime(LocalDateTime.now());
            log.setStatus("FAILED");
            log.setIpAddress(ipAddress);
            log.setCreatedAt(LocalDateTime.now());
            registrationCodeLogMapper.insert(log);
            return false;
        }

        RegistrationCodeLog log = new RegistrationCodeLog();
        log.setCode(code);
        log.setCodeId(registrationCode != null ? registrationCode.getId() : null);
        log.setAction("USE");
        log.setUserId(userId);
        log.setUsername(username);
        log.setUseTime(LocalDateTime.now());
        log.setStatus("SUCCESS");
        log.setIpAddress(ipAddress);
        log.setCreatedAt(LocalDateTime.now());
        registrationCodeLogMapper.insert(log);

        return true;
    }

    @Override
    @Transactional
    public UseCodeResult useCodeWithOnboarding(String code, String userId, String username, String ipAddress) {
        RegistrationCode registrationCode = registrationCodeMapper.findByCode(code);
        
        if (!validateCode(code)) {
            RegistrationCodeLog log = new RegistrationCodeLog();
            log.setCode(code);
            log.setCodeId(registrationCode != null ? registrationCode.getId() : null);
            log.setAction("USE");
            log.setUserId(userId);
            log.setUsername(username);
            log.setUseTime(LocalDateTime.now());
            log.setStatus("FAILED");
            log.setIpAddress(ipAddress);
            log.setCreatedAt(LocalDateTime.now());
            registrationCodeLogMapper.insert(log);
            return new UseCodeResult(false, code, null);
        }

        int updated = registrationCodeMapper.updateToUsed(code);
        if (updated == 0) {
            RegistrationCodeLog log = new RegistrationCodeLog();
            log.setCode(code);
            log.setCodeId(registrationCode != null ? registrationCode.getId() : null);
            log.setAction("USE");
            log.setUserId(userId);
            log.setUsername(username);
            log.setUseTime(LocalDateTime.now());
            log.setStatus("FAILED");
            log.setIpAddress(ipAddress);
            log.setCreatedAt(LocalDateTime.now());
            registrationCodeLogMapper.insert(log);
            return new UseCodeResult(false, code, null);
        }

        RegistrationCodeLog log = new RegistrationCodeLog();
        log.setCode(code);
        log.setCodeId(registrationCode != null ? registrationCode.getId() : null);
        log.setAction("USE");
        log.setUserId(userId);
        log.setUsername(username);
        log.setUseTime(LocalDateTime.now());
        log.setStatus("SUCCESS");
        log.setIpAddress(ipAddress);
        log.setCreatedAt(LocalDateTime.now());
        registrationCodeLogMapper.insert(log);

        OnboardingRecord onboardingRecord = null;

        if (registrationCode != null && StringUtils.isNotBlank(registrationCode.getOnboardingRecordId())) {
            onboardingRecord = onboardingRecordMapper.selectById(registrationCode.getOnboardingRecordId());
        }

        return new UseCodeResult(true, code, onboardingRecord);
    }

    @Override
    public void sendCode(SendCodeRequest request) {
        // 实际项目中应根据发送类型调用短信或邮箱服务
        // 这里简化处理，只记录日志
        logger.info("发送注册码: {}，发送方式: {}，接收地址: {}", 
                request.getCode(), request.getSendType(), request.getReceiveAddress());
    }

    @Override
    public void batchSendCode(BatchSendCodeRequest request) {
        // 实际项目中应根据发送类型调用短信或邮箱服务
        // 这里简化处理，只记录日志
        logger.info("批量发送注册码: {}个，发送方式: {}", 
                request.getCodes().size(), request.getSendType());
    }

    @Override
    public IPage<RegistrationCode> getCodesByPage(CodeQueryRequest request) {
        // 创建分页对象
        Page<RegistrationCode> page = new Page<>(request.getCurrent(), request.getSize());

        // 构建查询条件
        QueryWrapper<RegistrationCode> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(request.getCode())) {
            queryWrapper.like("code", request.getCode());
        }
        if (StringUtils.isNotBlank(request.getType())) {
            queryWrapper.eq("type", request.getType());
        }
        if (StringUtils.isNotBlank(request.getStatus())) {
            queryWrapper.eq("status", request.getStatus());
        }
        if (request.getStartTime() != null) {
            queryWrapper.ge("created_at", request.getStartTime());
        }
        if (request.getEndTime() != null) {
            queryWrapper.le("created_at", request.getEndTime());
        }
        queryWrapper.orderByDesc("created_at");

        // 执行分页查询
        return registrationCodeMapper.selectPage(page, queryWrapper);
    }

    @Override
    public IPage<RegistrationCodeLog> getCodeLogsByPage(CodeLogQueryRequest request) {
        // 创建分页对象
        Page<RegistrationCodeLog> page = new Page<>(request.getCurrent(), request.getSize());

        // 构建查询条件
        QueryWrapper<RegistrationCodeLog> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(request.getCode())) {
            queryWrapper.like("code", request.getCode());
        }
        if (StringUtils.isNotBlank(request.getUserId())) {
            queryWrapper.eq("user_id", request.getUserId());
        }
        if (StringUtils.isNotBlank(request.getUsername())) {
            queryWrapper.like("username", request.getUsername());
        }
        if (StringUtils.isNotBlank(request.getStatus())) {
            queryWrapper.eq("status", request.getStatus());
        }
        if (request.getStartTime() != null) {
            queryWrapper.ge("created_at", request.getStartTime());
        }
        if (request.getEndTime() != null) {
            queryWrapper.le("created_at", request.getEndTime());
        }
        queryWrapper.orderByDesc("created_at");

        // 执行分页查询
        return registrationCodeLogMapper.selectPage(page, queryWrapper);
    }

    @Override
    @Transactional
    public boolean updateCodeStatus(String id, String status) {
        // 查询注册码
        RegistrationCode registrationCode = registrationCodeMapper.selectById(id);
        if (registrationCode == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "注册码不存在");
        }

        // 更新状态
        registrationCode.setStatus(status);
        registrationCode.setUpdatedAt(LocalDateTime.now());
        int updated = registrationCodeMapper.updateById(registrationCode);

        return updated > 0;
    }

    /**
     * 批量更新注册码状态
     * @param codes 注册码列表
     * @param status 新状态
     * @return 是否更新成功
     */
    @Transactional
    public boolean batchUpdateStatus(List<String> codes, String status) {
        if (codes == null || codes.isEmpty()) {
            return true;
        }

        // 构建更新条件
        QueryWrapper<RegistrationCode> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("code", codes);

        // 执行批量更新
        RegistrationCode updateEntity = new RegistrationCode();
        updateEntity.setStatus(status);
        updateEntity.setUpdatedAt(LocalDateTime.now());

        int updated = registrationCodeMapper.update(updateEntity, queryWrapper);
        return updated > 0;
    }

    @Override
    public String sendSmsCode(String phone) {
        if (StringUtils.isBlank(phone)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号不能为空");
        }

        // 生成6位随机验证码
        String code = String.format("%06d", (int) (Math.random() * 1000000));

        // 存储验证码到内存缓存，有效期5分钟
        long expireTimeMillis = System.currentTimeMillis() + SMS_CODE_EXPIRATION_MINUTES * 60 * 1000;
        smsCodeCache.put(phone, new SmsCodeEntry(code, expireTimeMillis));

        // 实际项目中应调用短信服务发送验证码
        // 这里简化处理，只记录日志
        logger.info("发送手机验证码: {}，手机号: {}", code, phone);

        return code;
    }

    @Override
    public boolean validateSmsCode(String phone, String code) {
        if (StringUtils.isBlank(phone) || StringUtils.isBlank(code)) {
            return false;
        }

        // 从内存缓存获取验证码
        SmsCodeEntry entry = smsCodeCache.get(phone);
        if (entry == null || entry.isExpired()) {
            // 验证码不存在或已过期，清理过期项
            if (entry != null) {
                smsCodeCache.remove(phone, entry);
            }
            return false;
        }

        // 验证验证码
        boolean isValid = entry.code.equals(code);
        if (isValid) {
            // 验证成功后移除验证码
            smsCodeCache.remove(phone, entry);
        }

        return isValid;
    }
}