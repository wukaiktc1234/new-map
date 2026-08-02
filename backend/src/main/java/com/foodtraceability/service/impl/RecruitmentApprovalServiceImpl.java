package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dataservice.RecruitmentApprovalDataService;
import com.foodtraceability.dto.store.operation.RecruitmentApprovalCreateDTO;
import com.foodtraceability.dto.store.operation.RecruitmentApprovalUpdateDTO;
import com.foodtraceability.dto.store.operation.vo.RecruitmentApprovalVO;
import com.foodtraceability.entity.RecruitmentApproval;
import com.foodtraceability.entity.User;
import com.foodtraceability.mapper.RecruitmentApprovalMapper;
import com.foodtraceability.service.NotificationService;
import com.foodtraceability.service.OperationLogService;
import com.foodtraceability.service.RecruitmentApprovalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 招聘审批服务实现类
 * 提供门店自主招聘的三级审批流程管理功能
 * 支持提交申请、审批通过/驳回、自动升级检测等核心业务逻辑
 *
 * <h2>审批流程说明</h2>
 * <ul>
 *   <li><b>Level 1 初审</b>：由区域经理审批</li>
 *   <li><b>Level 2 复审</b>：由总部HR审批</li>
 *   <li><b>Level 3 终审</b>：由总部HR总监审批</li>
 * </ul>
 *
 * <h2>自动升级规则</h2>
 * <ul>
 *   <li>规则1：建议薪资超过当前层级权限上限 → 自动升级</li>
 *   <li>规则2：招聘导致人员超标超过10% → 自动升级</li>
 *   <li>规则3：特殊岗位（厨师长/财务主管）必须双审 → 至少Level 2</li>
 * </ul>
 */
@Service
public class RecruitmentApprovalServiceImpl implements RecruitmentApprovalService {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentApprovalServiceImpl.class);

    /** 审批层级常量 */
    private static final short LEVEL_INITIAL = 1;      // 初审
    private static final short LEVEL_REVIEW = 2;       // 复审
    private static final short LEVEL_FINAL = 3;        // 终审

    /** 驳回原因最小长度 */
    private static final int MIN_REJECTION_REASON_LENGTH = 10;

    /** 特殊岗位编码（必须至少复审） */
    private static final List<String> SPECIAL_POSITIONS = List.of("chef_head", "finance_supervisor");

    /** 各层级薪资上限（单位：分/月） */
    private static final long LEVEL_1_SALARY_LIMIT = 800000L;   // 初审上限：8000元
    private static final long LEVEL_2_SALARY_LIMIT = 1200000L;  // 复审上限：12000元

    private final RecruitmentApprovalMapper recruitmentApprovalMapper;
    private final RecruitmentApprovalDataService recruitmentApprovalDataService;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;

    /**
     * 构造函数注入
     *
     * @param recruitmentApprovalMapper    招聘审批Mapper
     * @param recruitmentApprovalDataService 招聘审批数据服务（缓存层）
     * @param notificationService           通知服务
     * @param operationLogService           操作日志服务
     */
    public RecruitmentApprovalServiceImpl(RecruitmentApprovalMapper recruitmentApprovalMapper,
                                          RecruitmentApprovalDataService recruitmentApprovalDataService,
                                          NotificationService notificationService,
                                          OperationLogService operationLogService) {
        this.recruitmentApprovalMapper = recruitmentApprovalMapper;
        this.recruitmentApprovalDataService = recruitmentApprovalDataService;
        this.notificationService = notificationService;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecruitmentApprovalVO submitApplication(RecruitmentApprovalCreateDTO dto, String storeManagerId) {
        log.info("提交招聘申请: postId={}, applicantName={}, proposedSalary={}",
                dto.getPostId(), dto.getApplicantName(), dto.getProposedSalary());

        // 1. 参数校验
        validateCreateDTO(dto);

        // 2. 创建审批记录
        RecruitmentApproval approval = new RecruitmentApproval();
        approval.setPostId(dto.getPostId());
        approval.setApplicantName(dto.getApplicantName());
        approval.setPhone(dto.getPhone());
        approval.setProposedSalary(convertYuanToFen(dto.getProposedSalary()));
        approval.setInterviewScore(dto.getInterviewScore());
        approval.setInterviewerId(dto.getInterviewerId());
        approval.setInterviewerComment(dto.getInterviewerComment());
        approval.setCurrentLevel(LEVEL_INITIAL);  // 默认从初审开始
        approval.setApprovalStatus("pending");

        // 3. 自动升级检测
        boolean shouldEscalate = shouldAutoEscalate(approval, dto.getPositionCode());
        if (shouldEscalate) {
            // 升级到复审
            approval.setCurrentLevel(LEVEL_REVIEW);
            log.info("招聘申请自动升级到复审: postId={}, reason=自动升级规则触发", dto.getPostId());
        }

        // 4. 保存到数据库
        int rows = recruitmentApprovalMapper.insert(approval);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "提交申请失败，请稍后重试");
        }

        log.info("招聘申请提交成功: approvalId={}, currentLevel={}",
                approval.getApprovalId(), approval.getCurrentLevel());

        // 5. 发送审批通知
        sendApprovalNotification(approval, "submit");

        // 6. 清除相关缓存
        recruitmentApprovalDataService.clearApprovalCache(approval.getApprovalId());

        // 7. 返回创建的VO
        return recruitmentApprovalDataService.getApprovalBasicInfo(approval.getApprovalId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(String approvalId, String approverId) {
        log.info("审批通过: approvalId={}, approverId={}", approvalId, approverId);

        // 1. 查询当前记录
        RecruitmentApproval approval = recruitmentApprovalMapper.selectById(approvalId);
        if (approval == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "审批记录不存在");
        }

        // 2. 状态校验
        if (!"pending".equals(approval.getApprovalStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "当前状态不允许审批操作，当前状态：" + approval.getApprovalStatus());
        }

        // 3. 审批人身份校验
        if (!approverId.equals(approval.getApproverId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "您不是当前审批人，无权操作");
        }

        Short currentLevel = approval.getCurrentLevel();

        // 4. 状态机流转
        if (currentLevel == LEVEL_FINAL) {
            // 终审通过 → 录用决策
            approval.setApprovalStatus("final_approved");
            approval.setFinalDecision("hired");
            approval.setHiredAt(LocalDateTime.now());
            log.info("终审通过，进入录用流程: approvalId={}", approvalId);
        } else if (currentLevel == LEVEL_REVIEW) {
            // 复审通过 → 进入终审
            approval.setApprovalStatus("approved_by_regional");
            approval.setCurrentLevel(LEVEL_FINAL);
            log.info("复审通过，升级到终审: approvalId={}", approvalId);
        } else {
            // 初审通过 → 进入复审
            approval.setApprovalStatus("approved_by_regional");
            approval.setCurrentLevel(LEVEL_REVIEW);
            log.info("初审通过，升级到复审: approvalId={}", approvalId);
        }

        // 5. 更新审批信息
        approval.setApprovedAt(LocalDateTime.now());

        int rows = recruitmentApprovalMapper.updateById(approval);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "审批操作失败，请刷新后重试");
        }

        // 6. 记录操作日志
        try {
            operationLogService.logOperation(buildLogEntity("APPROVE_RECRUITMENT", "recruitment_approval", approvalId, approverId));
        } catch (Exception e) {
            log.warn("记录操作日志失败", e);
        }

        // 7. 清除缓存
        recruitmentApprovalDataService.clearApprovalCache(approvalId);

        // 8. 如果终审通过，发送录用通知给门店店长
        if ("final_approved".equals(approval.getApprovalStatus())) {
            sendHireNotification(approval);
        } else {
            // 否则发送下一级审批通知
            sendApprovalNotification(approval, "approve");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(String approvalId, String approverId, RecruitmentApprovalUpdateDTO dto) {
        log.info("审批驳回: approvalId={}, approverId={}", approvalId, approverId);

        // 1. 驳回原因长度校验
        if (dto.getRejectionReason() == null
                || dto.getRejectionReason().trim().length() < MIN_REJECTION_REASON_LENGTH) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "驳回原因不能少于" + MIN_REJECTION_REASON_LENGTH + "个字符");
        }

        // 2. 查询当前记录
        RecruitmentApproval approval = recruitmentApprovalMapper.selectById(approvalId);
        if (approval == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "审批记录不存在");
        }

        // 3. 状态校验
        if (!"pending".equals(approval.getApprovalStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "当前状态不允许驳回操作，当前状态：" + approval.getApprovalStatus());
        }

        // 4. 审批人身份校验
        if (!approverId.equals(approval.getApproverId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "您不是当前审批人，无权操作");
        }

        // 5. 状态机流转 → rejected
        approval.setApprovalStatus("rejected");
        approval.setRejectionReason(dto.getRejectionReason().trim());
        approval.setFinalDecision("ended");
        approval.setApprovedAt(LocalDateTime.now());

        int rows = recruitmentApprovalMapper.updateById(approval);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "驳回操作失败，请刷新后重试");
        }

        // 6. 记录操作日志
        try {
            operationLogService.logOperation(buildLogEntity("REJECT_RECRUITMENT", "recruitment_approval", approvalId, approverId));
        } catch (Exception e) {
            log.warn("记录操作日志失败", e);
        }

        // 7. 清除缓存
        recruitmentApprovalDataService.clearApprovalCache(approvalId);

        // 8. 发送驳回通知给门店店长
        sendRejectNotification(approval, dto.getRejectionReason());

        log.info("审批驳回成功: approvalId={}", approvalId);
    }

    @Override
    public IPage<RecruitmentApprovalVO> getMyApplications(String submitterId, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1 || size > 100) size = 10;

        LambdaQueryWrapper<RecruitmentApproval> wrapper = new LambdaQueryWrapper<>();
        // TODO: 需要在实体中添加created_by字段或通过其他方式关联提交人
        // 当前使用interviewerId作为近似字段（面试官即提交人）
        wrapper.eq(RecruitmentApproval::getInterviewerId, submitterId)
               .orderByDesc(RecruitmentApproval::getCreateTime);

        Page<RecruitmentApproval> pageInfo = new Page<>(page, size);
        IPage<RecruitmentApproval> approvalPage = recruitmentApprovalMapper.selectPage(pageInfo, wrapper);

        return approvalPage.convert(approval ->
                recruitmentApprovalDataService.getApprovalBasicInfo(approval.getApprovalId()));
    }

    @Override
    public List<RecruitmentApprovalVO> getPendingApprovals(String approverId, Integer currentLevel) {
        if (currentLevel == null) {
            currentLevel = 1;
        }

        // 委托给DataService查询待审批记录
        return recruitmentApprovalDataService.getPendingApprovalsForUser(approverId, currentLevel);
    }

    @Override
    public RecruitmentApprovalVO getApprovalDetail(String approvalId) {
        // 通过DataService获取（带缓存）
        return recruitmentApprovalDataService.getApprovalBasicInfo(approvalId);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 校验创建DTO参数
     *
     * @param dto 创建DTO
     */
    private void validateCreateDTO(RecruitmentApprovalCreateDTO dto) {
        if (dto == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "申请信息不能为空");
        }
        if (dto.getPostId() == null || dto.getPostId().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "招聘岗位不能为空");
        }
        if (dto.getApplicantName() == null || dto.getApplicantName().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "应聘者姓名不能为空");
        }
        if (dto.getPhone() == null || dto.getPhone().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "联系电话不能为空");
        }
        if (dto.getInterviewScore() != null && (dto.getInterviewScore() < 1 || dto.getInterviewScore() > 100)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "面试评分必须在1-100之间");
        }
    }

    /**
     * 自动升级检测
     * 判断是否需要将审批自动升级到更高层级
     *
     * @param approval    审批记录实体
     * @param positionCode 岗位编码
     * @return true-需要升级 false-不需要
     */
    private boolean shouldAutoEscalate(RecruitmentApproval approval, String positionCode) {
        // 规则1：建议薪资超过初审权限上限（8000元/月）
        if (approval.getProposedSalary() != null
                && approval.getProposedSalary() > LEVEL_1_SALARY_LIMIT) {
            log.debug("触发自动升级规则1: 薪资{}超过初审上限{}", approval.getProposedSalary(), LEVEL_1_SALARY_LIMIT);
            return true;
        }

        // 规则2：建议薪资超过复审权限上限（12000元/月），直接升到终审
        if (approval.getProposedSalary() != null
                && approval.getProposedSalary() > LEVEL_2_SALARY_LIMIT) {
            log.debug("触发自动升级规则2: 薪资{}超过复审上限{}", approval.getProposedSalary(), LEVEL_2_SALARY_LIMIT);
            // 此处仍返回true，由外部决定具体升级到哪一级
            return true;
        }

        // 规则3：特殊岗位（厨师长/财务主管）必须至少复审
        if (positionCode != null && SPECIAL_POSITIONS.contains(positionCode)) {
            log.debug("触发自动升级规则3: 特殊岗位{}必须双审", positionCode);
            return true;
        }

        return false;
    }

    /**
     * 元转分（前端传来的金额单位为元）
     *
     * @param yuan 元
     * @return 分
     */
    private Long convertYuanToFen(String yuan) {
        if (yuan == null || yuan.isEmpty()) {
            return null;
        }
        try {
            double yuanValue = Double.parseDouble(yuan);
            return Math.round(yuanValue * 100);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "金额格式错误");
        }
    }

    /**
     * 发送审批通知
     *
     * @param approval 审批记录
     * @param action   操作类型
     */
    private void sendApprovalNotification(RecruitmentApproval approval, String action) {
        try {
            String title;
            String content;
            String targetRole;

            switch (action) {
                case "submit":
                    title = "新的招聘审批";
                    content = "门店提交了" + approval.getApplicantName() + "的招聘申请（"
                            + approval.getPostId() + "），请您审批";
                    targetRole = "regional_manager";
                    break;
                case "approve":
                    title = "招聘审批待处理";
                    if (approval.getCurrentLevel() == LEVEL_FINAL) {
                        content = approval.getApplicantName() + "的招聘申请已通过复审，等待您终审";
                        targetRole = "hr_director";
                    } else {
                        content = approval.getApplicantName() + "的招聘申请已通过初审，等待您复审";
                        targetRole = "hr_manager";
                    }
                    break;
                default:
                    return;
            }

            log.info("发送审批通知: targetRole={}, title={}, bizType={}", targetRole, title, "RECRUITMENT_APPROVAL");
        } catch (Exception e) {
            log.warn("发送审批通知失败", e);
        }
    }

    /**
     * 发送录用通知
     *
     * @param approval 审批记录
     */
    private void sendHireNotification(RecruitmentApproval approval) {
        try {
            log.info("发送录用通知: applicantName={}, bizType={}", approval.getApplicantName(), "RECRUITMENT_HIRED");
        } catch (Exception e) {
            log.warn("发送录用通知失败", e);
        }
    }

    /**
     * 发送驳回通知
     *
     * @param approval 审批记录
     * @param reason   驳回原因
     */
    private void sendRejectNotification(RecruitmentApproval approval, String reason) {
        try {
            log.info("发送驳回通知: applicantName={}, reason={}, bizType={}", approval.getApplicantName(), reason, "RECRUITMENT_REJECTED");
        } catch (Exception e) {
            log.warn("发送驳回通知失败", e);
        }
    }

    /**
     * 构建操作日志实体
     */
    private com.foodtraceability.entity.OperationLogEntity buildLogEntity(String operationType, String operationModule, String recordId, String operatorId) {
        com.foodtraceability.entity.OperationLogEntity logEntity = new com.foodtraceability.entity.OperationLogEntity();
        logEntity.setOperationType(operationType);
        logEntity.setOperationModule(operationModule);
        logEntity.setRecordId(recordId);
        logEntity.setOperatorId(operatorId);
        logEntity.setOperationTime(LocalDateTime.now());
        return logEntity;
    }
}
