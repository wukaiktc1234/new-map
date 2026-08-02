package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.OnboardingRecord;
import com.foodtraceability.entity.RegistrationCode;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.mapper.OnboardingRecordMapper;
import com.foodtraceability.mapper.RegistrationCodeMapper;
import com.foodtraceability.service.EmployeeService;
import com.foodtraceability.service.OnboardingRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class OnboardingRecordServiceImpl extends ServiceImpl<OnboardingRecordMapper, OnboardingRecord> implements OnboardingRecordService {
    
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom random = new SecureRandom();


    public OnboardingRecordServiceImpl(RegistrationCodeMapper registrationCodeMapper, EmployeeService employeeService) {
        this.registrationCodeMapper = registrationCodeMapper;
        this.employeeService = employeeService;
    }

    private final RegistrationCodeMapper registrationCodeMapper;
    
    private final EmployeeService employeeService;
    
    @Override
    public Map<String, Object> getOnboardingRecords(int current, int size, String status, String interviewId) {
        Page<OnboardingRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<OnboardingRecord> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(OnboardingRecord::getStatus, status);
        }
        if (StringUtils.hasText(interviewId)) {
            queryWrapper.eq(OnboardingRecord::getInterviewId, interviewId);
        }
        
        queryWrapper.orderByDesc(OnboardingRecord::getCreatedAt);
        IPage<OnboardingRecord> result = this.page(page, queryWrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("records", result.getRecords());
        response.put("total", result.getTotal());
        response.put("current", result.getCurrent());
        response.put("size", result.getSize());
        response.put("pages", result.getPages());
        
        return response;
    }
    
    @Override
    public OnboardingRecord createOnboardingRecord(OnboardingRecord record) {
        record.setOnboardingCode(generateOnboardingCode());
        record.setStatus("pending");
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        this.save(record);
        return record;
    }
    
    @Override
    public void updateOnboardingStatus(String id, String status) {
        OnboardingRecord record = this.getById(id);
        if (record != null) {
            record.setStatus(status);
            record.setUpdatedAt(LocalDateTime.now());
            this.updateById(record);
        }
    }
    
    @Override
    public String generateRegistrationCode(String onboardingId) {
        OnboardingRecord record = this.getById(onboardingId);
        if (record != null) {
            String code = generateRandomCode();
            
            // 创建注册码记录
            RegistrationCode registrationCode = new RegistrationCode();
            registrationCode.setCode(code);
            registrationCode.setType("INTERNAL");
            registrationCode.setValidityStart(LocalDateTime.now());
            registrationCode.setValidityEnd(LocalDateTime.now().plusDays(7));
            registrationCode.setStatus("UNUSED");
            registrationCode.setOnboardingRecordId(onboardingId);
            registrationCode.setCreatedAt(LocalDateTime.now());
            registrationCode.setUpdatedAt(LocalDateTime.now());
            registrationCodeMapper.insert(registrationCode);
            
            // 更新入职记录
            record.setRegistrationCode(code);
            record.setCodeExpiryTime(LocalDateTime.now().plusDays(7));
            record.setStatus("code_sent");
            record.setUpdatedAt(LocalDateTime.now());
            this.updateById(record);
            
            return code;
        }
        return null;
    }
    
    @Override
    public OnboardingRecord getOnboardingByInterviewId(String interviewId) {
        LambdaQueryWrapper<OnboardingRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OnboardingRecord::getInterviewId, interviewId);
        queryWrapper.orderByDesc(OnboardingRecord::getCreatedAt);
        queryWrapper.last("LIMIT 1");
        return this.getOne(queryWrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOnboarding(String id) {
        OnboardingRecord record = this.getById(id);
        if (record != null) {
            record.setStatus("completed");
            record.setUpdatedAt(LocalDateTime.now());
            this.updateById(record);
            
            createEmployeeProfile(record);
            generateRegistrationCode(id);
        }
    }
    
    private void createEmployeeProfile(OnboardingRecord onboardingRecord) {
        if (onboardingRecord.getEmployeeId() != null && !onboardingRecord.getEmployeeId().isEmpty()) {
            return;
        }
        
        Employee employee = new Employee();
        employee.setEmployeeCode(generateEmployeeCode());
        if (onboardingRecord.getDepartmentId() != null && !onboardingRecord.getDepartmentId().isEmpty()) {
            employee.setDepartmentId(Long.valueOf(onboardingRecord.getDepartmentId()));
        }
        employee.setDepartmentName(onboardingRecord.getDepartmentName());
        if (onboardingRecord.getPositionId() != null && !onboardingRecord.getPositionId().isEmpty()) {
            employee.setPositionId(Long.valueOf(onboardingRecord.getPositionId()));
        }
        employee.setPositionName(onboardingRecord.getPositionName());
        employee.setStatus(1); // 1: 在职（Employee.status 为 Integer）
        
        employeeService.save(employee);
        
        onboardingRecord.setEmployeeId(employee.getId().toString());
        onboardingRecord.setEmployeeCode(employee.getEmployeeCode());
        onboardingRecord.setUpdatedAt(LocalDateTime.now());
        this.updateById(onboardingRecord);
    }
    
    private String generateOnboardingCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ONB" + timestamp;
    }
    
    private String generateEmployeeCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "EMP" + timestamp;
    }
    
    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }
}
