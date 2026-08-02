package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.InvitationSendRecord;
import com.foodtraceability.entity.OnboardingArchive;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.mapper.InvitationSendRecordMapper;
import com.foodtraceability.service.OnboardingInvitationService;
import com.foodtraceability.service.OnboardingArchiveService;
import com.foodtraceability.service.EmployeeService;
import com.foodtraceability.utils.SecurityUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 入职邀请码服务实现类
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-01-31
 */
@Service
public class OnboardingInvitationServiceImpl extends ServiceImpl<InvitationSendRecordMapper, InvitationSendRecord> implements OnboardingInvitationService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OnboardingInvitationServiceImpl.class);
    private final OnboardingArchiveService archiveService;
    private final EmployeeService employeeService;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * 使用 @Lazy 解决循环依赖
     * @param archiveService 入职档案服务
     * @param employeeService 员工服务
     */
    public OnboardingInvitationServiceImpl(@Lazy OnboardingArchiveService archiveService, @Lazy EmployeeService employeeService) {
        this.archiveService = archiveService;
        this.employeeService = employeeService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvitationSendRecord generateInvitationCode(Long archiveId) {
        OnboardingArchive archive = archiveService.getById(archiveId);
        if (archive == null) {
            log.error("生成邀请码失败，档案不存在，档案ID：{}", archiveId);
            return null;
        }
        // 检查档案状态是否为合同已签署（法律合规：必须先签署劳动合同）
        if (!OnboardingArchive.STATUS_CONTRACT_SIGNED.equals(archive.getStatus())) {
            log.error("生成邀请码失败，档案状态不正确，档案ID：{}，当前状态：{}，期望状态：CONTRACT_SIGNED", archiveId, archive.getStatus());
            return null;
        }
        // 检查是否已存在邀请码
        InvitationSendRecord existingRecord = baseMapper.selectByArchiveId(archiveId);
        if (existingRecord != null) {
            log.info("档案已存在邀请码，档案ID：{}，邀请码：{}", archiveId, existingRecord.getInvitationCode());
            return existingRecord;
        }
        // 生成唯一邀请码
        String invitationCode = generateUniqueCode();
        // 创建邀请码记录
        InvitationSendRecord record = new InvitationSendRecord();
        record.setArchiveId(archiveId);
        record.setInvitationCode(invitationCode);
        record.setBoundEmail(archive.getEmail());
        record.setBoundPhone(archive.getPhone());
        record.setBoundName(archive.getCandidateName());
        record.setEmployeeCode(archive.getEmployeeCode()); // 关联员工编号
        record.setPresetUsername(archive.getPresetUsername()); // 关联预设用户名
        record.setUseCount(0);
        record.setMaxUseCount(1); // 默认只能使用一次
        record.setStatus(InvitationSendRecord.STATUS_UNUSED);
        record.setValidDays(7); // 默认7天有效期
        record.setExpireTime(LocalDateTime.now().plusDays(7));
        record.setSendStatus(InvitationSendRecord.SEND_STATUS_PENDING);
        record.setCreateBy(SecurityUtils.getCurrentUserId());
        this.save(record);
        // 更新档案的邀请码ID和状态
        archive.setInvitationCodeId(record.getId());
        archive.setStatus(OnboardingArchive.STATUS_REGISTERED); // 更新状态为已注册
        archiveService.updateById(archive);
        // 创建员工记录（入职完成）
        Employee employee = employeeService.createEmployeeFromArchive(archive);
        if (employee != null) {
            log.info("入职完成，已创建员工记录，档案ID：{}，员工编号：{}，员工ID：{}", archiveId, employee.getEmployeeCode(), employee.getId());
        } else {
            log.warn("入职完成，但创建员工记录失败，档案ID：{}", archiveId);
        }
        log.info("生成邀请码成功，档案ID：{}，员工编号：{}，预设用户名：{}，邀请码：{}，档案状态更新为REGISTERED", archiveId, archive.getEmployeeCode(), archive.getPresetUsername(), invitationCode);
        return record;
    }

    @Override
    public boolean validateInvitationCode(String code, String email, String phone, String name) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        InvitationSendRecord record = baseMapper.selectByCode(code);
        if (record == null) {
            log.warn("邀请码不存在：{}", code);
            return false;
        }
        // 检查状态
        if (!InvitationSendRecord.STATUS_UNUSED.equals(record.getStatus())) {
            log.warn("邀请码状态无效：{}，状态：{}", code, record.getStatus());
            return false;
        }
        // 检查是否过期
        if (LocalDateTime.now().isAfter(record.getExpireTime())) {
            log.warn("邀请码已过期：{}，过期时间：{}", code, record.getExpireTime());
            // 更新状态为已过期
            record.setStatus(InvitationSendRecord.STATUS_EXPIRED);
            this.updateById(record);
            return false;
        }
        // 验证绑定信息
        if (email != null && !email.equals(record.getBoundEmail())) {
            log.warn("邀请码邮箱不匹配：{}，期望：{}，实际：{}", code, record.getBoundEmail(), email);
            return false;
        }
        if (phone != null && !phone.equals(record.getBoundPhone())) {
            log.warn("邀请码手机号不匹配：{}，期望：{}，实际：{}", code, record.getBoundPhone(), phone);
            return false;
        }
        if (name != null && !name.equals(record.getBoundName())) {
            log.warn("邀请码姓名不匹配：{}，期望：{}，实际：{}", code, record.getBoundName(), name);
            return false;
        }
        log.info("邀请码验证成功：{}", code);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean useInvitationCode(String code, Long userId) {
        InvitationSendRecord record = baseMapper.selectByCode(code);
        if (record == null) {
            log.error("使用邀请码失败，邀请码不存在：{}", code);
            return false;
        }
        // 检查状态
        if (!InvitationSendRecord.STATUS_UNUSED.equals(record.getStatus())) {
            log.error("使用邀请码失败，状态无效：{}，状态：{}", code, record.getStatus());
            return false;
        }
        // 检查是否过期
        if (LocalDateTime.now().isAfter(record.getExpireTime())) {
            log.error("使用邀请码失败，邀请码已过期：{}", code);
            record.setStatus(InvitationSendRecord.STATUS_EXPIRED);
            this.updateById(record);
            return false;
        }
        // 检查使用次数
        if (record.getUseCount() >= record.getMaxUseCount()) {
            log.error("使用邀请码失败，使用次数已达上限：{}", code);
            return false;
        }
        // 更新邀请码状态
        record.setStatus(InvitationSendRecord.STATUS_USED);
        record.setUsedBy(userId);
        record.setUsedTime(LocalDateTime.now());
        record.setUseCount(record.getUseCount() + 1);
        this.updateById(record);
        log.info("使用邀请码成功，邀请码：{}，用户ID：{}", code, userId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revokeInvitationCode(Long codeId) {
        InvitationSendRecord record = this.getById(codeId);
        if (record == null) {
            log.error("撤销邀请码失败，邀请码不存在：{}", codeId);
            return false;
        }
        // 检查状态
        if (!InvitationSendRecord.STATUS_UNUSED.equals(record.getStatus())) {
            log.error("撤销邀请码失败，状态无效：{}，状态：{}", codeId, record.getStatus());
            return false;
        }
        // 更新状态为已撤销
        record.setStatus(InvitationSendRecord.STATUS_REVOKED);
        this.updateById(record);
        log.info("撤销邀请码成功，邀请码ID：{}", codeId);
        return true;
    }

    @Override
    public InvitationSendRecord getInvitationCode(String code) {
        return baseMapper.selectByCode(code);
    }

    @Override
    public InvitationSendRecord getInvitationCodeByArchiveId(Long archiveId) {
        return baseMapper.selectByArchiveId(archiveId);
    }

    @Override
    public List<InvitationSendRecord> getExpiringInvitationCodes() {
        return baseMapper.selectExpiringIn24Hours();
    }

    @Override
    public List<InvitationSendRecord> getExpiredInvitationCodes() {
        return baseMapper.selectExpired();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean extendExpiration(Long codeId, int addDays) {
        InvitationSendRecord record = this.getById(codeId);
        if (record == null) {
            log.error("延长邀请码有效期失败，邀请码不存在：{}", codeId);
            return false;
        }
        // 检查状态
        if (!InvitationSendRecord.STATUS_UNUSED.equals(record.getStatus())) {
            log.error("延长邀请码有效期失败，状态无效：{}，状态：{}", codeId, record.getStatus());
            return false;
        }
        // 延长有效期
        record.setExpireTime(record.getExpireTime().plusDays(addDays));
        record.setValidDays(record.getValidDays() + addDays);
        this.updateById(record);
        log.info("延长邀请码有效期成功，邀请码ID：{}，增加天数：{}", codeId, addDays);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvitationSendRecord regenerateInvitationCode(Long oldCodeId) {
        InvitationSendRecord oldRecord = this.getById(oldCodeId);
        if (oldRecord == null) {
            log.error("重新生成邀请码失败，原邀请码不存在：{}", oldCodeId);
            return null;
        }
        // 检查状态
        if (!InvitationSendRecord.STATUS_UNUSED.equals(oldRecord.getStatus()) && !InvitationSendRecord.STATUS_EXPIRED.equals(oldRecord.getStatus())) {
            log.error("重新生成邀请码失败，原邀请码状态无效：{}，状态：{}", oldCodeId, oldRecord.getStatus());
            return null;
        }
        // 生成新的邀请码
        String newCode = generateUniqueCode();
        // 创建新的邀请码记录
        InvitationSendRecord newRecord = new InvitationSendRecord();
        newRecord.setArchiveId(oldRecord.getArchiveId());
        newRecord.setInvitationCode(newCode);
        newRecord.setBoundEmail(oldRecord.getBoundEmail());
        newRecord.setBoundPhone(oldRecord.getBoundPhone());
        newRecord.setBoundName(oldRecord.getBoundName());
        newRecord.setUseCount(0);
        newRecord.setMaxUseCount(oldRecord.getMaxUseCount());
        newRecord.setStatus(InvitationSendRecord.STATUS_UNUSED);
        newRecord.setValidDays(oldRecord.getValidDays());
        newRecord.setExpireTime(LocalDateTime.now().plusDays(oldRecord.getValidDays()));
        newRecord.setSendStatus(InvitationSendRecord.SEND_STATUS_PENDING);
        newRecord.setCreateBy(SecurityUtils.getCurrentUserId());
        this.save(newRecord);
        // 更新原邀请码状态为已撤销
        oldRecord.setStatus(InvitationSendRecord.STATUS_REVOKED);
        this.updateById(oldRecord);
        log.info("重新生成邀请码成功，原邀请码ID：{}，新邀请码：{}", oldCodeId, newCode);
        return newRecord;
    }

    /**
     * 生成唯一邀请码
     *
     * @return 唯一邀请码
     */
    private String generateUniqueCode() {
        // 生成8位随机码（包含字母和数字）
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        // 检查是否已存在
        while (baseMapper.selectByCode(code) != null) {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        }
        return code;
    }
}
