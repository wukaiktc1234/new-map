package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.OnboardingArchive;
import com.foodtraceability.mapper.OnboardingArchiveMapper;
import com.foodtraceability.service.ApprovalService;
import com.foodtraceability.service.OnboardingArchiveService;
import com.foodtraceability.util.OnboardingCodeGenerator;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 入职档案服务实现类
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-01-31
 */
@Service
public class OnboardingArchiveServiceImpl extends ServiceImpl<OnboardingArchiveMapper, OnboardingArchive> implements OnboardingArchiveService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OnboardingArchiveServiceImpl.class);

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * 使用 @Lazy 解决循环依赖
     * @param codeGenerator 入职编码生成器
     * @param approvalService 审批服务
     */
    public OnboardingArchiveServiceImpl(OnboardingCodeGenerator codeGenerator, @Lazy ApprovalService approvalService) {
        this.codeGenerator = codeGenerator;
        this.approvalService = approvalService;
    }

    private final OnboardingCodeGenerator codeGenerator;
    private final ApprovalService approvalService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OnboardingArchive createArchive(OnboardingArchive archive) {
        // 设置初始状态
        archive.setStatus(OnboardingArchive.STATUS_CREATED);
        // 自动生成员工编号（如果未提供）
        if (archive.getEmployeeCode() == null || archive.getEmployeeCode().isEmpty()) {
            String employeeCode = codeGenerator.generateEmployeeCode();
            archive.setEmployeeCode(employeeCode);
        }
        // 自动生成预设用户名（如果未提供）
        if (archive.getPresetUsername() == null || archive.getPresetUsername().isEmpty()) {
            String presetUsername = codeGenerator.generatePresetUsername(archive.getCandidateName());
            archive.setPresetUsername(presetUsername);
        }
        // 保存档案
        this.save(archive);
        log.info("创建入职档案成功，档案ID：{}，员工编号：{}，预设用户名：{}，候选人：{}", archive.getId(), archive.getEmployeeCode(), archive.getPresetUsername(), archive.getCandidateName());
        return archive;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OnboardingArchive updateArchive(OnboardingArchive archive) {
        // 更新档案
        this.updateById(archive);
        log.info("更新入职档案成功，档案ID：{}，候选人：{}", archive.getId(), archive.getCandidateName());
        return this.getById(archive.getId());
    }

    @Override
    public List<OnboardingArchive> getArchivesByStatus(String status) {
        return baseMapper.selectByStatus(status);
    }

    @Override
    public List<OnboardingArchive> getArchivesByDepartment(String departmentId) {
        return baseMapper.selectByDepartmentId(departmentId);
    }

    @Override
    public List<OnboardingArchive> getPendingHrReviewArchives() {
        return baseMapper.selectPendingHrReview();
    }

    @Override
    public List<OnboardingArchive> getPendingSubstantiveReviewArchives() {
        return baseMapper.selectPendingSubstantiveReview();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitForApproval(Long archiveId, Long hrId) {
        OnboardingArchive archive = this.getById(archiveId);
        if (archive == null) {
            log.error("提交审批失败，档案不存在，档案ID：{}", archiveId);
            return false;
        }
        // 检查档案状态是否为已创建
        if (!OnboardingArchive.STATUS_CREATED.equals(archive.getStatus())) {
            log.error("提交审批失败，档案状态不正确，档案ID：{}，当前状态：{}", archiveId, archive.getStatus());
            return false;
        }
        // 调用审批服务启动审批流程（会创建审批记录并更新档案状态）
        boolean success = approvalService.startApprovalProcess(archiveId, hrId);
        if (success) {
            log.info("提交档案审批成功，档案ID：{}，HR ID：{}", archiveId, hrId);
        } else {
            log.error("提交档案审批失败，档案ID：{}，HR ID：{}", archiveId, hrId);
        }
        return success;
    }

    @Override
    public OnboardingArchive getArchiveByInvitationCodeId(Long invitationCodeId) {
        return baseMapper.selectByInvitationCodeId(invitationCodeId);
    }
}
