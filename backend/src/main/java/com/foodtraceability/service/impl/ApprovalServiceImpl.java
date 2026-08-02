package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.ApprovalRecordWithArchiveDTO;
import com.foodtraceability.entity.ApprovalRecord;
import com.foodtraceability.entity.EmployeeLaborContract;
import com.foodtraceability.entity.OnboardingArchive;
import com.foodtraceability.mapper.ApprovalRecordMapper;
import com.foodtraceability.service.ApprovalService;
import com.foodtraceability.service.EmployeeLaborContractService;
import com.foodtraceability.service.OnboardingArchiveService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ApprovalServiceImpl extends ServiceImpl<ApprovalRecordMapper, ApprovalRecord> implements ApprovalService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApprovalServiceImpl.class);
    private final OnboardingArchiveService archiveService;
    private final EmployeeLaborContractService contractService;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * 使用 @Lazy 解决循环依赖
     * @param archiveService 入职档案服务
     * @param contractService 劳动合同服务
     */
    public ApprovalServiceImpl(@Lazy OnboardingArchiveService archiveService, @Lazy EmployeeLaborContractService contractService) {
        this.archiveService = archiveService;
        this.contractService = contractService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApprovalRecord createApprovalRecord(Long archiveId, Integer stepNumber, String stepName, String reviewType) {
        ApprovalRecord record = new ApprovalRecord();
        record.setArchiveId(archiveId);
        record.setStepNumber(stepNumber);
        record.setStepName(stepName);
        record.setReviewType(reviewType);
        record.setStatus(ApprovalRecord.STATUS_PENDING);
        this.save(record);
        log.info("创建审批记录成功，档案ID：{}，步骤：{}，步骤名称：{}，审批类型：{}", archiveId, stepNumber, stepName, reviewType);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long recordId, Long reviewerId, String comment) {
        ApprovalRecord record = this.getById(recordId);
        if (record == null) {
            log.error("审批失败，记录不存在，记录ID：{}", recordId);
            return false;
        }
        // 检查审批记录状态
        if (!ApprovalRecord.STATUS_PENDING.equals(record.getStatus())) {
            log.error("审批失败，审批记录状态不正确，记录ID：{}，当前状态：{}", recordId, record.getStatus());
            return false;
        }
        // 更新审批记录
        record.setReviewerId(reviewerId);
        record.setAction(ApprovalRecord.ACTION_APPROVE);
        record.setComment(comment);
        record.setStatus(ApprovalRecord.STATUS_APPROVED);
        record.setActionTime(LocalDateTime.now()); // 设置审批时间
        this.updateById(record);
        // 更新档案状态
        updateArchiveStatusAfterApproval(record.getArchiveId());
        log.info("审批通过，记录ID：{}，审批人：{}，审批时间：{}", recordId, reviewerId, record.getActionTime());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Long recordId, Long reviewerId, String comment) {
        ApprovalRecord record = this.getById(recordId);
        if (record == null) {
            log.error("审批拒绝失败，记录不存在，记录ID：{}", recordId);
            return false;
        }
        // 检查审批记录状态
        if (!ApprovalRecord.STATUS_PENDING.equals(record.getStatus())) {
            log.error("审批拒绝失败，审批记录状态不正确，记录ID：{}，当前状态：{}", recordId, record.getStatus());
            return false;
        }
        // 更新审批记录
        record.setReviewerId(reviewerId);
        record.setAction(ApprovalRecord.ACTION_REJECT);
        record.setComment(comment);
        record.setStatus(ApprovalRecord.STATUS_REJECTED);
        record.setActionTime(LocalDateTime.now()); // 设置审批时间
        this.updateById(record);
        // 更新档案状态为已拒绝
        OnboardingArchive archive = archiveService.getById(record.getArchiveId());
        if (archive != null) {
            archive.setStatus(OnboardingArchive.STATUS_REJECTED);
            archiveService.updateById(archive);
        }
        log.info("审批拒绝，记录ID：{}，审批人：{}，审批时间：{}", recordId, reviewerId, record.getActionTime());
        return true;
    }

    @Override
    public List<ApprovalRecord> getApprovalsByArchiveId(Long archiveId) {
        return baseMapper.selectByArchiveId(archiveId);
    }

    @Override
    public List<ApprovalRecord> getPendingApprovals(Long reviewerId) {
        return baseMapper.selectPendingByReviewerId(reviewerId);
    }

    @Override
    public List<ApprovalRecordWithArchiveDTO> getPendingApprovalsWitArchive(Long reviewerId) {
        // 获取待审批记录
        List<ApprovalRecord> records = baseMapper.selectPendingByReviewerId(reviewerId);
        if (records.isEmpty()) {
            return new ArrayList<>();
        }
        // 提取所有档案ID
        List<Long> archiveIds = records.stream().map(ApprovalRecord::getArchiveId).distinct().collect(Collectors.toList());
        // 批量查询档案信息
        List<OnboardingArchive> archives = archiveService.listByIds(archiveIds);
        // 转换为Map便于查找
        Map<Long, OnboardingArchive> archiveMap = archives.stream().collect(Collectors.toMap(OnboardingArchive::getId, archive -> archive));
        // 组装结果
        return records.stream().map(record -> ApprovalRecordWithArchiveDTO.of(record, archiveMap.get(record.getArchiveId()))).collect(Collectors.toList());
    }

    @Override
    public ApprovalRecord getLatestApproval(Long archiveId) {
        return baseMapper.selectLatestByArchiveId(archiveId);
    }

    @Override
    public boolean isApprovalProcessComplete(Long archiveId) {
        OnboardingArchive archive = archiveService.getById(archiveId);
        if (archive == null) {
            return false;
        }
        List<ApprovalStep> flow = getApprovalFlowByPositionLevel(archive.getPositionLevel());
        List<ApprovalRecord> records = getApprovalsByArchiveId(archiveId);
        long passedCount = records.stream().filter(r -> ApprovalRecord.STATUS_APPROVED.equals(r.getStatus())).count();
        return passedCount >= flow.size();
    }

    @Override
    public List<ApprovalStep> getApprovalFlowByPositionLevel(String positionLevel) {
        List<ApprovalStep> steps = new ArrayList<>();
        switch (positionLevel) {
        case OnboardingArchive.LEVEL_STAFF: 
            steps.add(new ApprovalStep(1, "HR形式审查", "FORMAL"));
            steps.add(new ApprovalStep(2, "直接主管实质审查", "SUBSTANTIVE"));
            break;
        case OnboardingArchive.LEVEL_MANAGER: 
            steps.add(new ApprovalStep(1, "HR形式审查", "FORMAL"));
            steps.add(new ApprovalStep(2, "部门总监实质审查", "SUBSTANTIVE"));
            break;
        case OnboardingArchive.LEVEL_DIRECTOR: 
            steps.add(new ApprovalStep(1, "HR形式审查+建议", "FORMAL"));
            steps.add(new ApprovalStep(2, "总经理审批", "SUBSTANTIVE"));
            break;
        case OnboardingArchive.LEVEL_EXECUTIVE: 
            steps.add(new ApprovalStep(1, "HR形式审查+建议+风险评估", "FORMAL"));
            steps.add(new ApprovalStep(2, "董事长审批", "SUBSTANTIVE"));
            break;
        default: 
            steps.add(new ApprovalStep(1, "HR形式审查", "FORMAL"));
            steps.add(new ApprovalStep(2, "部门主管实质审查", "SUBSTANTIVE"));
        }
        return steps;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startApprovalProcess(Long archiveId, Long hrId) {
        OnboardingArchive archive = archiveService.getById(archiveId);
        if (archive == null) {
            log.error("启动审批流程失败，档案不存在，档案ID：{}", archiveId);
            return false;
        }
        List<ApprovalStep> flow = getApprovalFlowByPositionLevel(archive.getPositionLevel());
        ApprovalStep firstStep = flow.get(0);
        // 创建审批记录并设置审批人ID
        ApprovalRecord record = createApprovalRecord(archiveId, firstStep.getOrder(), firstStep.getName(), firstStep.getType());
        // 更新审批记录的审批人ID
        record.setReviewerId(hrId);
        this.updateById(record);
        // 更新档案状态为待HR审查
        archive.setStatus(OnboardingArchive.STATUS_PENDING_HR);
        archiveService.updateById(archive);
        log.info("启动审批流程成功，档案ID：{}，HR ID：{}，审批记录ID：{}", archiveId, hrId, record.getId());
        return true;
    }

    @Override
    public List<ApprovalRecordWithArchiveDTO> getApprovalHistory() {
        // 获取所有已完成的审批记录（已通过或已拒绝）
        List<ApprovalRecord> records = baseMapper.selectCompletedApprovals();
        if (records.isEmpty()) {
            return new ArrayList<>();
        }
        // 提取所有档案ID
        List<Long> archiveIds = records.stream().map(ApprovalRecord::getArchiveId).distinct().collect(Collectors.toList());
        // 批量查询档案信息
        List<OnboardingArchive> archives = archiveService.listByIds(archiveIds);
        // 转换为Map便于查找
        Map<Long, OnboardingArchive> archiveMap = archives.stream().collect(Collectors.toMap(OnboardingArchive::getId, archive -> archive));
        // 组装结果
        return records.stream().map(record -> ApprovalRecordWithArchiveDTO.of(record, archiveMap.get(record.getArchiveId()))).collect(Collectors.toList());
    }

    private void updateArchiveStatusAfterApproval(Long archiveId) {
        OnboardingArchive archive = archiveService.getById(archiveId);
        if (archive == null) {
            return;
        }
        List<ApprovalStep> flow = getApprovalFlowByPositionLevel(archive.getPositionLevel());
        List<ApprovalRecord> records = getApprovalsByArchiveId(archiveId);
        long passedCount = records.stream().filter(r -> ApprovalRecord.STATUS_APPROVED.equals(r.getStatus())).count();
        if (passedCount >= flow.size()) {
            // 审批全部通过，创建劳动合同并更新状态为待签合同
            EmployeeLaborContract contract = contractService.createContractFromArchive(archive);
            if (contract != null) {
                archive.setContractId(contract.getId());
                archive.setStatus(OnboardingArchive.STATUS_CONTRACT_PENDING);
                log.info("审批流程完成，已创建劳动合同，档案ID：{}，合同ID：{}", archiveId, contract.getId());
            } else {
                archive.setStatus(OnboardingArchive.STATUS_APPROVED);
                log.warn("审批流程完成，但创建劳动合同失败，档案ID：{}", archiveId);
            }
            archiveService.updateById(archive);
        } else {
            ApprovalStep nextStep = flow.get((int) passedCount);
            createApprovalRecord(archiveId, nextStep.getOrder(), nextStep.getName(), nextStep.getType());
            archive.setStatus(OnboardingArchive.STATUS_PENDING_SUBSTANTIVE);
            archiveService.updateById(archive);
            log.info("进入下一步审批，档案ID：{}，步骤：{}", archiveId, nextStep.getName());
        }
    }
}
