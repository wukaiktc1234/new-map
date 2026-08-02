package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.recruitment.QuotaValidateResult;
import com.foodtraceability.entity.Department;
import com.foodtraceability.entity.Position;
import com.foodtraceability.entity.RecruitmentRequirement;
import com.foodtraceability.entity.Store;
import com.foodtraceability.mapper.RecruitmentRequirementMapper;
import com.foodtraceability.mapper.ResumeMapper;
import com.foodtraceability.service.DepartmentService;
import com.foodtraceability.service.PositionService;
import com.foodtraceability.service.RecruitmentQuotaService;
import com.foodtraceability.service.RecruitmentRequirementService;
import com.foodtraceability.service.StoreService;
import com.foodtraceability.service.UserService;
import com.foodtraceability.service.event.RecruitmentNotificationEvent;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 招聘需求服务实现类
 *
 * <p>Sprint 2 T-016 改造：在 createRequirement 写流程增加
 * {@code @Transactional(rollbackFor = Exception.class)} + 名额校验 + 事件发布。</p>
 *
 * <p>遵循 ADR-001（事件 AFTER_COMMIT + try-catch 隔离）、
 * ADR-004（recruitment_requirements 扩展 quota_id 字段，门店提报时校验）。</p>
 *
 * <p>注意：recruitment_requirements 表使用 created_at/updated_at 字段命名
 * （spec 用户决策 1：保持现状不重命名为 create_time/update_time）。</p>
 */
@Service
public class RecruitmentRequirementServiceImpl extends ServiceImpl<RecruitmentRequirementMapper, RecruitmentRequirement>
        implements RecruitmentRequirementService {

    private static final Logger logger = LoggerFactory.getLogger(RecruitmentRequirementServiceImpl.class);

    private final PositionService positionService;
    private final DepartmentService departmentService;
    private final StoreService storeService;
    private final ApplicationEventPublisher eventPublisher;
    private final RecruitmentQuotaService quotaService;
    private final UserService userService;
    private final ResumeMapper resumeMapper;

    public RecruitmentRequirementServiceImpl(PositionService positionService,
                                             DepartmentService departmentService,
                                             StoreService storeService,
                                             ApplicationEventPublisher eventPublisher,
                                             RecruitmentQuotaService quotaService,
                                             UserService userService,
                                             ResumeMapper resumeMapper) {
        this.positionService = positionService;
        this.departmentService = departmentService;
        this.storeService = storeService;
        this.eventPublisher = eventPublisher;
        this.quotaService = quotaService;
        this.userService = userService;
        this.resumeMapper = resumeMapper;
    }

    @Override
    public Map<String, Object> getRecruitmentRequirements(int current, int size, String type, String status,
                                                          String keyword, String departmentId, String startDate, String endDate) {
        Page<RecruitmentRequirement> page = new Page<>(current, size);
        LambdaQueryWrapper<RecruitmentRequirement> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(type)) {
            queryWrapper.eq(RecruitmentRequirement::getType, type);
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(RecruitmentRequirement::getStatus, status);
        }
        if (StringUtils.hasText(departmentId)) {
            queryWrapper.eq(RecruitmentRequirement::getDepartmentId, departmentId);
        }
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(RecruitmentRequirement::getPositionName, keyword)
                    .or()
                    .like(RecruitmentRequirement::getDepartmentName, keyword));
        }
        if (StringUtils.hasText(startDate)) {
            LocalDate start = LocalDate.parse(startDate);
            queryWrapper.ge(RecruitmentRequirement::getCreatedAt, start.atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            LocalDate end = LocalDate.parse(endDate);
            queryWrapper.le(RecruitmentRequirement::getCreatedAt, LocalDateTime.of(end, LocalTime.MAX));
        }

        queryWrapper.orderByDesc(RecruitmentRequirement::getCreatedAt);
        IPage<RecruitmentRequirement> result = this.page(page, queryWrapper);

        for (RecruitmentRequirement req : result.getRecords()) {
            fillResumeStatistics(req);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("records", result.getRecords());
        response.put("total", result.getTotal());
        response.put("current", result.getCurrent());
        response.put("size", result.getSize());
        response.put("pages", result.getPages());

        return response;
    }

    private void fillResumeStatistics(RecruitmentRequirement req) {
        if (req.getId() == null) {
            req.setApplicantCount(0);
            req.setHiredCount(0);
            return;
        }
        LambdaQueryWrapper<com.foodtraceability.entity.Resume> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.foodtraceability.entity.Resume::getRequirementId, req.getId());
        int applicantCount = resumeMapper.selectCount(wrapper).intValue();
        req.setApplicantCount(applicantCount);

        LambdaQueryWrapper<com.foodtraceability.entity.Resume> hiredWrapper = new LambdaQueryWrapper<>();
        hiredWrapper.eq(com.foodtraceability.entity.Resume::getRequirementId, req.getId());
        hiredWrapper.eq(com.foodtraceability.entity.Resume::getStatus, "hired");
        int hiredCount = resumeMapper.selectCount(hiredWrapper).intValue();
        req.setHiredCount(hiredCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecruitmentRequirement createRequirement(RecruitmentRequirement requirement) {
        requirement.setRequirementCode(generateRequirementCode());
        // 根据前端发布设置决定初始状态：approved 立即发布，其他保存为草稿
        if ("approved".equals(requirement.getApprovalStatus())) {
            requirement.setStatus("open");
            requirement.setApprovalStatus("approved");
        } else {
            requirement.setStatus("draft");
            requirement.setApprovalStatus("pending");
        }
        requirement.setCreatedAt(LocalDateTime.now());
        requirement.setUpdatedAt(LocalDateTime.now());

        // 自动注入当前登录用户的门店ID（不信任前端传入，防止越权指定其他门店）
        // 门店用户storeId为其所属门店；HR用户storeId为null（HR提报需求不需要门店）
        String currentUserStoreId = SecurityUtils.getCurrentUserStoreId();
        requirement.setStoreId(currentUserStoreId);

        logger.info("开始填充招聘需求的名称字段");

        if (StringUtils.hasText(requirement.getPositionId())) {
            try {
                Position position = positionService.getPositionById(Long.valueOf(requirement.getPositionId()));
                if (position != null) {
                    requirement.setPositionName(position.getPositionName());
                    logger.info("填充职位名称: {}", position.getPositionName());
                }
            } catch (Exception e) {
                logger.error("获取职位信息失败，职位ID: {}", requirement.getPositionId(), e);
            }
        }

        if (StringUtils.hasText(requirement.getDepartmentId())) {
            try {
                Department department = departmentService.getDepartmentById(Long.valueOf(requirement.getDepartmentId()));
                if (department != null) {
                    requirement.setDepartmentName(department.getDepartmentName());
                    logger.info("填充部门名称: {}", department.getDepartmentName());
                }
            } catch (Exception e) {
                logger.error("获取部门信息失败，部门ID: {}", requirement.getDepartmentId(), e);
            }
        }

        if (StringUtils.hasText(requirement.getStoreId())) {
            try {
                Store store = storeService.getStoreById(requirement.getStoreId());
                if (store != null) {
                    requirement.setStoreName(store.getStoreName());
                    logger.info("填充门店名称: {}", store.getStoreName());
                }
            } catch (Exception e) {
                logger.error("获取门店信息失败，门店ID: {}", requirement.getStoreId(), e);
            }
        }

        // 名额校验：门店提报需求（type=store）且 quotaId 不为空时，必须通过名额可用性校验
        // ADR-004：门店端提报需求必须传 quota_id 并通过校验；HR 端提报需求不需要名额
        if ("store".equals(requirement.getType()) && requirement.getQuotaId() != null) {
            int requirementNum = requirement.getRequirementNum() == null ? 1 : requirement.getRequirementNum();
            QuotaValidateResult validateResult = quotaService.validateQuota(requirement.getQuotaId(), requirementNum);
            if (!validateResult.isValid()) {
                logger.warn("名额校验失败, quotaId={}, requirementNum={}, errorCode={}",
                        requirement.getQuotaId(), requirementNum, validateResult.getErrorCode());
                throw new BusinessException(ErrorCode.QUOTA_EXHAUSTED,
                        "名额不足，剩余名额: " + validateResult.getRemaining());
            }
            logger.info("名额校验通过, quotaId={}, remaining={}",
                    requirement.getQuotaId(), validateResult.getRemaining());
        }

        this.save(requirement);
        logger.info("招聘需求创建成功，ID: {}", requirement.getId());

        // 发布 recruitment.requirement.submitted 事件（接收人: HR 招聘专员）
        // ADR-001：try-catch 隔离，事件失败不影响业务事务
        try {
            List<Long> recipientUserIds = userService.getHrRecruiters();
            Map<String, Object> variables = new HashMap<>();
            variables.put("requirementId", requirement.getId());
            variables.put("requirementCode", requirement.getRequirementCode());
            variables.put("positionName", requirement.getPositionName());
            variables.put("storeName", requirement.getStoreName());
            variables.put("departmentName", requirement.getDepartmentName());
            variables.put("requirementNum", requirement.getRequirementNum());
            variables.put("type", requirement.getType());
            eventPublisher.publishEvent(RecruitmentNotificationEvent.requirementSubmitted(
                    this, null, variables, recipientUserIds));
            logger.info("招聘需求提报事件已发布, requirementId={}", requirement.getId());
        } catch (Exception e) {
            logger.error("发布招聘需求提报事件失败, requirementId={}", requirement.getId(), e);
        }

        return requirement;
    }

    @Override
    public RecruitmentRequirement updateRequirement(String id, RecruitmentRequirement requirement) {
        requirement.setId(id);
        requirement.setUpdatedAt(LocalDateTime.now());
        this.updateById(requirement);
        return requirement;
    }

    @Override
    public void deleteRequirement(String id) {
        this.removeById(id);
    }

    @Override
    public void approveRequirement(String id, String approvalStatus) {
        RecruitmentRequirement requirement = this.getById(id);
        if (requirement == null) {
            return;
        }
        // 兼容前端“发布/下架”操作：published 发布，closed 关闭
        if ("published".equals(approvalStatus)) {
            requirement.setStatus("open");
            requirement.setApprovalStatus("approved");
        } else if ("closed".equals(approvalStatus)) {
            requirement.setStatus("closed");
            requirement.setApprovalStatus("closed");
        } else {
            requirement.setApprovalStatus(approvalStatus);
        }
        requirement.setUpdatedAt(LocalDateTime.now());
        this.updateById(requirement);
    }

    @Override
    public List<RecruitmentRequirement> getRequirementsByStore(String storeId) {
        LambdaQueryWrapper<RecruitmentRequirement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RecruitmentRequirement::getStoreId, storeId);
        queryWrapper.eq(RecruitmentRequirement::getStatus, "open");
        queryWrapper.orderByDesc(RecruitmentRequirement::getCreatedAt);
        return this.list(queryWrapper);
    }

    @Override
    public List<RecruitmentRequirement> getRequirementsByDepartment(String departmentId) {
        LambdaQueryWrapper<RecruitmentRequirement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(RecruitmentRequirement::getDepartmentId, departmentId);
        queryWrapper.eq(RecruitmentRequirement::getStatus, "open");
        queryWrapper.orderByDesc(RecruitmentRequirement::getCreatedAt);
        return this.list(queryWrapper);
    }

    private String generateRequirementCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "REQ" + timestamp;
    }
}
