package com.foodtraceability.service.approval.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.approval.EmployeeApprovalCreateDTO;
import com.foodtraceability.dto.approval.EmployeeApprovalQueryDTO;
import com.foodtraceability.dto.approval.LeaveCreateDTO;
import com.foodtraceability.dto.approval.OvertimeCreateDTO;
import com.foodtraceability.dto.approval.SwapCreateDTO;
import com.foodtraceability.dto.approval.TravelCreateDTO;
import com.foodtraceability.dto.approval.ReimbursementCreateDTO;
import com.foodtraceability.dto.approval.RequisitionCreateDTO;
import com.foodtraceability.dto.approval.vo.ApprovalDetailVO;
import com.foodtraceability.dto.approval.vo.ApprovalStatsVO;
import com.foodtraceability.dto.approval.vo.ApprovalFlowNodeVO;
import com.foodtraceability.dto.approval.vo.ApprovalOperationLogVO;
import com.foodtraceability.dto.approval.vo.EmployeeApprovalVO;
import com.foodtraceability.dto.approval.vo.RiskWarningVO;
import com.foodtraceability.entity.approval.EmployeeApproval;
import com.foodtraceability.entity.approval.LeaveRequestEntity;
import com.foodtraceability.entity.approval.OvertimeRequestEntity;
import com.foodtraceability.entity.approval.SwapRequestDetailEntity;
import com.foodtraceability.entity.approval.TravelRequestEntity;
import com.foodtraceability.entity.approval.ReimbursementRequestEntity;
import com.foodtraceability.entity.approval.RequisitionRequestEntity;
import com.foodtraceability.dataservice.approval.EmployeeApprovalDataService;
import com.foodtraceability.mapper.approval.EmployeeApprovalMapper;
import com.foodtraceability.mapper.approval.LeaveRequestMapper;
import com.foodtraceability.mapper.approval.OvertimeRequestMapper;
import com.foodtraceability.mapper.approval.SwapRequestDetailMapper;
import com.foodtraceability.mapper.approval.TravelRequestMapper;
import com.foodtraceability.mapper.approval.ReimbursementRequestMapper;
import com.foodtraceability.mapper.approval.RequisitionRequestMapper;
import com.foodtraceability.service.EmployeeService;
import com.foodtraceability.service.approval.EmployeeApprovalService;
import com.foodtraceability.service.approval.RiskRuleEngine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 员工日常审批服务实现类
 * 核心业务逻辑：审批提交、审批流转、撤回催办、统计查询
 */
@Service
public class EmployeeApprovalServiceImpl extends ServiceImpl<EmployeeApprovalMapper, EmployeeApproval>
        implements EmployeeApprovalService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeApprovalServiceImpl.class);

    /** 审批编号前缀 */
    private static final String APPROVAL_NO_PREFIX = "AP";
    /** 日期格式化器（用于生成审批编号） */
    private static final DateTimeFormatter NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final EmployeeApprovalMapper employeeApprovalMapper;
    private final LeaveRequestMapper leaveRequestMapper;
    private final OvertimeRequestMapper overtimeRequestMapper;
    private final SwapRequestDetailMapper swapRequestDetailMapper;
    private final TravelRequestMapper travelRequestMapper;
    private final ReimbursementRequestMapper reimbursementRequestMapper;
    private final RequisitionRequestMapper requisitionRequestMapper;
    private final EmployeeApprovalDataService approvalDataService;
    private final RiskRuleEngine riskRuleEngine;
    private final EmployeeService employeeService;

    /**
     * 构造函数注入所有依赖
     */
    public EmployeeApprovalServiceImpl(
            EmployeeApprovalMapper employeeApprovalMapper,
            LeaveRequestMapper leaveRequestMapper,
            OvertimeRequestMapper overtimeRequestMapper,
            SwapRequestDetailMapper swapRequestDetailMapper,
            TravelRequestMapper travelRequestMapper,
            ReimbursementRequestMapper reimbursementRequestMapper,
            RequisitionRequestMapper requisitionRequestMapper,
            EmployeeApprovalDataService approvalDataService,
            RiskRuleEngine riskRuleEngine,
            EmployeeService employeeService) {
        this.employeeApprovalMapper = employeeApprovalMapper;
        this.leaveRequestMapper = leaveRequestMapper;
        this.overtimeRequestMapper = overtimeRequestMapper;
        this.swapRequestDetailMapper = swapRequestDetailMapper;
        this.travelRequestMapper = travelRequestMapper;
        this.reimbursementRequestMapper = reimbursementRequestMapper;
        this.requisitionRequestMapper = requisitionRequestMapper;
        this.approvalDataService = approvalDataService;
        this.riskRuleEngine = riskRuleEngine;
        this.employeeService = employeeService;
    }

    /**
     * 分页查询当前员工的审批列表（我发起的）
     */
    @Override
    public IPage<EmployeeApprovalVO> getMyApprovalPage(EmployeeApprovalQueryDTO query, String employeeId) {
        try {
            Page<EmployeeApproval> page = new Page<>(query.getCurrent(), query.getSize());

            LambdaQueryWrapper<EmployeeApproval> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(EmployeeApproval::getApplicantId, employeeId);
            // 按条件过滤
            if (query.getType() != null && !query.getType().isEmpty()) {
                wrapper.eq(EmployeeApproval::getType, query.getType());
            }
            if (query.getStatus() != null && !query.getStatus().isEmpty()) {
                wrapper.eq(EmployeeApproval::getStatus, query.getStatus());
            }
            wrapper.orderByDesc(EmployeeApproval::getCreatedTime);

            IPage<EmployeeApproval> pageResult = employeeApprovalMapper.selectPage(page, wrapper);
            
            // 转换为VO列表
            List<EmployeeApprovalVO> voList = pageResult.getRecords().stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());

            // 构建返回的分页VO
            Page<EmployeeApprovalVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
            voPage.setRecords(voList);

            log.debug("查询我的审批列表成功，员工ID：{}，共{}条", employeeId, pageResult.getTotal());
            return voPage;
        } catch (Exception e) {
            log.error("查询我的审批列表失败，员工ID：{}", employeeId, e);
            throw new RuntimeException("查询我的审批列表失败");
        }
    }

    /**
     * 分页查询待当前员工审批的列表（待我审批的）
     */
    @Override
    public IPage<EmployeeApprovalVO> getPendingReviewPage(EmployeeApprovalQueryDTO query, String reviewerId) {
        try {
            Page<EmployeeApproval> page = new Page<>(query.getCurrent(), query.getSize());

            LambdaQueryWrapper<EmployeeApproval> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(EmployeeApproval::getCurrentApproverId, reviewerId);
            wrapper.eq(EmployeeApproval::getStatus, "pending");  // 仅查待审批状态
            if (query.getType() != null && !query.getType().isEmpty()) {
                wrapper.eq(EmployeeApproval::getType, query.getType());
            }
            wrapper.orderByAsc(EmployeeApproval::getCreatedTime);  // 待审批按时间升序，优先处理早期申请

            IPage<EmployeeApproval> pageResult = employeeApprovalMapper.selectPage(page, wrapper);

            List<EmployeeApprovalVO> voList = pageResult.getRecords().stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());

            Page<EmployeeApprovalVO> voPage = new Page<>(pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
            voPage.setRecords(voList);

            log.debug("查询待我审批列表成功，审批人ID：{}，共{}条", reviewerId, pageResult.getTotal());
            return voPage;
        } catch (Exception e) {
            log.error("查询待我审批列表失败，审批人ID：{}", reviewerId, e);
            throw new RuntimeException("查询待我审批列表失败");
        }
    }

    /**
     * 获取审批详情（含上下文数据和风险预警）
     */
    @Override
    public ApprovalDetailVO getApprovalDetail(String approvalId) {
        try {
            // 1. 查询主表
            EmployeeApproval approval = employeeApprovalMapper.selectById(approvalId);
            if (approval == null) {
                log.warn("审批记录不存在，审批ID：{}", approvalId);
                throw new RuntimeException("审批记录不存在");
            }

            // 2. 组装基础VO
            ApprovalDetailVO detailVO = new ApprovalDetailVO();
            detailVO.setId(approval.getId());
            detailVO.setApprovalNo(approval.getApprovalNo());
            detailVO.setType(approval.getType());
            detailVO.setTitle(approval.getTitle());
            detailVO.setStatus(approval.getStatus());
            detailVO.setApplicantId(approval.getApplicantId());
            detailVO.setCurrentApproverName(getEmployeeName(approval.getCurrentApproverId()));
            detailVO.setSubmitTime(approval.getCreatedTime());
            detailVO.setPriority(approval.getPriority());

            // 3. 根据 type 查对应的详情表并组装 contextData
            Object contextData = buildContextDataByType(approval);
            detailVO.setContextData(contextData);

            // 4. 调用风控引擎评估风险
            RiskWarningVO riskWarning = riskRuleEngine.evaluate(approval, contextData);
            detailVO.setRiskWarning(riskWarning);

            // 5. 构建审批流程节点信息
            List<ApprovalFlowNodeVO> flowNodes = buildFlowNodes(approval);
            detailVO.setApprovalFlow(flowNodes);

            // 6. 构建操作日志（TODO: 对接操作日志表）
            List<ApprovalOperationLogVO> operationLogs = buildOperationLogs(approval);
            detailVO.setOperationLog(operationLogs);

            log.debug("获取审批详情完成，审批ID：{}", approvalId);
            return detailVO;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取审批详情失败，审批ID：{}", approvalId, e);
            throw new RuntimeException("获取审批详情失败");
        }
    }

    /**
     * 提交新的审批申请
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeApprovalVO submitApproval(EmployeeApprovalCreateDTO dto, String employeeId) {
        try {
            // 1. 生成审批编号: AP + 年月日 + 4位序号
            String approvalNo = generateApprovalNo();

            // 2. 创建主表记录
            EmployeeApproval approval = new EmployeeApproval();
            approval.setId(UUID.randomUUID().toString().replace("-", ""));
            approval.setApprovalNo(approvalNo);
            approval.setType(dto.getType());
            approval.setTitle(dto.getTitle());
            approval.setApplicantId(employeeId);
            // TODO: 可根据审批类型自动分配审批人，当前暂设为空
            approval.setCurrentApproverId(null);
            approval.setStatus("pending");  // 初始状态为待审批
            approval.setPriority(dto.getPriority() != null ? dto.getPriority() : "normal");
            approval.setRemark(dto.getRemark());
            approval.setCreatedTime(LocalDateTime.now());
            approval.setUpdatedTime(LocalDateTime.now());

            int inserted = employeeApprovalMapper.insert(approval);
            if (inserted <= 0) {
                throw new RuntimeException("创建审批主记录失败");
            }

            // 3. 根据 type 创建对应的详情实体并保存
            saveDetailByType(approval.getApprovalId(), dto);

            // 4. 风控评估（异步或同步均可，此处同步执行以便返回预警信息）
            Object contextData = buildContextDataByType(approval);
            RiskWarningVO riskWarning = riskRuleEngine.evaluate(approval, contextData);
            if (riskWarning != null) {
                log.info("审批申请触发风控预警，审批编号：{}，级别：{}，规则：{}",
                        approvalNo, riskWarning.getLevel(), riskWarning.getRuleCode());
                // TODO: 可在此处将风控预警持久化到数据库
            }

            // 5. 返回VO
            EmployeeApprovalVO vo = convertToVO(approval);
            log.info("提交审批申请成功，审批编号：{}，类型：{}，申请人：{}", 
                    approvalNo, dto.getType(), employeeId);
            return vo;
        } catch (RuntimeException e) {
            log.error("提交审批申请失败，员工ID：{}，错误：{}", employeeId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("提交审批申请异常，员工ID：{}", employeeId, e);
            throw new RuntimeException("提交审批申请失败：" + e.getMessage());
        }
    }

    /**
     * 审批操作：通过
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveApproval(String approvalId, String reviewerId, String comment) {
        try {
            EmployeeApproval approval = employeeApprovalMapper.selectById(approvalId);
            if (approval == null) {
                throw new RuntimeException("审批记录不存在");
            }
            if (!"pending".equals(approval.getStatus())) {
                throw new RuntimeException("当前审批状态不允许通过操作，当前状态：" + approval.getStatus());
            }
            if (!reviewerId.equals(approval.getCurrentApproverId())) {
                throw new RuntimeException("您不是该审批的审批人");
            }

            // 更新状态为已通过
            approval.setStatus("approved");
            approval.setRemark(comment);
            approval.setUpdatedTime(LocalDateTime.now());
            employeeApprovalMapper.updateById(approval);

            // 记录操作日志
            logOperationLog(approvalId, reviewerId, "approve", comment);

            log.info("审批通过成功，审批编号：{}，审批人：{}", approval.getApprovalNo(), reviewerId);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("审批通过异常，审批ID：{}", approvalId, e);
            throw new RuntimeException("审批通过失败");
        }
    }

    /**
     * 审批操作：驳回
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectApproval(String approvalId, String reviewerId, String comment) {
        try {
            EmployeeApproval approval = employeeApprovalMapper.selectById(approvalId);
            if (approval == null) {
                throw new RuntimeException("审批记录不存在");
            }
            if (!"pending".equals(approval.getStatus())) {
                throw new RuntimeException("当前审批状态不允许驳回操作，当前状态：" + approval.getStatus());
            }
            if (!reviewerId.equals(approval.getCurrentApproverId())) {
                throw new RuntimeException("您不是该审批的审批人");
            }

            // 更新状态为已驳回
            approval.setStatus("rejected");
            approval.setRemark(comment);
            approval.setUpdatedTime(LocalDateTime.now());
            employeeApprovalMapper.updateById(approval);

            // 记录操作日志
            logOperationLog(approvalId, reviewerId, "reject", comment);

            log.info("审批驳回成功，审批编号：{}，审批人：{}，原因：{}", 
                    approval.getApprovalNo(), reviewerId, comment);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("审批驳回异常，审批ID：{}", approvalId, e);
            throw new RuntimeException("审批驳回失败");
        }
    }

    /**
     * 撤回审批申请（仅pending状态允许撤回）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawApproval(String approvalId, String employeeId) {
        try {
            EmployeeApproval approval = employeeApprovalMapper.selectById(approvalId);
            if (approval == null) {
                throw new RuntimeException("审批记录不存在");
            }
            if (!employeeId.equals(approval.getApplicantId())) {
                throw new RuntimeException("您不是该审批的申请人，无法撤回");
            }
            if (!"pending".equals(approval.getStatus())) {
                throw new RuntimeException("仅待审批状态允许撤回，当前状态：" + approval.getStatus());
            }

            // 更新状态为已撤回
            approval.setStatus("withdrawn");
            approval.setUpdatedTime(LocalDateTime.now());
            employeeApprovalMapper.updateById(approval);

            // 记录操作日志
            logOperationLog(approvalId, employeeId, "withdraw", "申请人主动撤回");

            log.info("撤回审批成功，审批编号：{}，申请人：{}", approval.getApprovalNo(), employeeId);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("撤回审批异常，审批ID：{}", approvalId, e);
            throw new RuntimeException("撤回审批失败");
        }
    }

    /**
     * 催办审批（向审批人发送催办通知）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void urgeApproval(String approvalId, String employeeId) {
        try {
            EmployeeApproval approval = employeeApprovalMapper.selectById(approvalId);
            if (approval == null) {
                throw new RuntimeException("审批记录不存在");
            }
            if (!employeeId.equals(approval.getApplicantId())) {
                throw new RuntimeException("您不是该审批的申请人，无法催办");
            }
            if (!"pending".equals(approval.getStatus())) {
                throw new RuntimeException("仅待审批状态的申请可以催办");
            }

            // TODO: 发送催办通知（可通过消息队列/站内信/邮件等方式）
            // 此处更新催办时间和次数
            approval.setLastUrgeTime(LocalDateTime.now());
            int urgeCount = approval.getUrgeCount() != null ? approval.getUrgeCount() + 1 : 1;
            approval.setUrgeCount(urgeCount);
            approval.setUpdatedTime(LocalDateTime.now());
            employeeApprovalMapper.updateById(approval);

            // 记录操作日志
            logOperationLog(approvalId, employeeId, "urge", "第" + urgeCount + "次催办");

            log.info("催办审批成功，审批编号：{}，催办次数：{}", approval.getApprovalNo(), urgeCount);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("催办审批异常，审批ID：{}", approvalId, e);
            throw new RuntimeException("催办审批失败");
        }
    }

    /**
     * 获取当前员工的审批统计数据
     */
    @Override
    public ApprovalStatsVO getMyApprovalStats(String employeeId) {
        try {
            ApprovalStatsVO stats = new ApprovalStatsVO();

            // 统计各状态的审批数量（我发起的）
            LambdaQueryWrapper<EmployeeApproval> myWrapper = new LambdaQueryWrapper<>();
            myWrapper.eq(EmployeeApproval::getApplicantId, employeeId);

            long myTotal = employeeApprovalMapper.selectCount(myWrapper);

            LambdaQueryWrapper<EmployeeApproval> myPendingWrapper = new LambdaQueryWrapper<>();
            myPendingWrapper.eq(EmployeeApproval::getApplicantId, employeeId);
            myPendingWrapper.eq(EmployeeApproval::getStatus, "pending");
            long myPending = employeeApprovalMapper.selectCount(myPendingWrapper);

            LambdaQueryWrapper<EmployeeApproval> myApprovedWrapper = new LambdaQueryWrapper<>();
            myApprovedWrapper.eq(EmployeeApproval::getApplicantId, employeeId);
            myApprovedWrapper.eq(EmployeeApproval::getStatus, "approved");
            long myApproved = employeeApprovalMapper.selectCount(myApprovedWrapper);

            LambdaQueryWrapper<EmployeeApproval> myRejectedWrapper = new LambdaQueryWrapper<>();
            myRejectedWrapper.eq(EmployeeApproval::getApplicantId, employeeId);
            myRejectedWrapper.eq(EmployeeApproval::getStatus, "rejected");
            long myRejected = employeeApprovalMapper.selectCount(myRejectedWrapper);

            // 统计待我审批的数量
            LambdaQueryWrapper<EmployeeApproval> pendingReviewWrapper = new LambdaQueryWrapper<>();
            pendingReviewWrapper.eq(EmployeeApproval::getCurrentApproverId, employeeId);
            pendingReviewWrapper.eq(EmployeeApproval::getStatus, "pending");
            long pendingReview = employeeApprovalMapper.selectCount(pendingReviewWrapper);

            stats.setMyTotal((int) myTotal);
            stats.setMyPending((int) myPending);
            stats.setMyApproved((int) myApproved);
            stats.setMyRejected((int) myRejected);
            stats.setPendingReview((int) pendingReview);

            log.debug("获取审批统计完成，员工ID：{}", employeeId);
            return stats;
        } catch (Exception e) {
            log.error("获取审批统计失败，员工ID：{}", employeeId, e);
            throw new RuntimeException("获取审批统计失败");
        }
    }

    /**
     * 批量查询审批基本信息
     */
    @Override
    public Map<String, EmployeeApprovalVO> batchGetBasicInfo(List<String> approvalIds) {
        if (approvalIds == null || approvalIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            LambdaQueryWrapper<EmployeeApproval> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(EmployeeApproval::getApprovalId, approvalIds);
            List<EmployeeApproval> approvals = employeeApprovalMapper.selectList(wrapper);

            Map<String, EmployeeApprovalVO> result = new HashMap<>();
            for (EmployeeApproval approval : approvals) {
                result.put(approval.getApprovalId(), convertToVO(approval));
            }

            log.debug("批量查询审批基本信息完成，请求{}条，返回{}条", approvalIds.size(), result.size());
            return result;
        } catch (Exception e) {
            log.error("批量查询审批基本信息失败", e);
            throw new RuntimeException("批量查询审批基本信息失败");
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将实体转换为VO
     */
    private EmployeeApprovalVO convertToVO(EmployeeApproval approval) {
        if (approval == null) {
            return null;
        }
        EmployeeApprovalVO vo = new EmployeeApprovalVO();
        vo.setId(approval.getId());
        vo.setApprovalNo(approval.getApprovalNo());
        vo.setType(approval.getType());
        vo.setTitle(approval.getTitle());
        vo.setStatus(approval.getStatus());
        vo.setApplicantId(approval.getApplicantId());
        vo.setCurrentApproverName(getEmployeeName(approval.getCurrentApproverId()));
        vo.setPriority(approval.getPriority());
        vo.setFormSummary(approval.getFormSummary());
        vo.setSubmitTime(approval.getCreatedTime());
        vo.setApproveTime(approval.getUpdatedTime());
        vo.setRiskLevel(approval.getRiskLevel());
        return vo;
    }

    /**
     * 生成审批编号
     * 格式：AP + 年月日 + 4位序号（如 AP202606030001）
     */
    private synchronized String generateApprovalNo() {
        String dateStr = LocalDateTime.now().format(NO_DATE_FORMATTER);
        String prefix = APPROVAL_NO_PREFIX + dateStr;

        // 查询当天最大序号
        LambdaQueryWrapper<EmployeeApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(EmployeeApproval::getApprovalNo, prefix)
               .orderByDesc(EmployeeApproval::getApprovalNo)
               .last("LIMIT 1");

        EmployeeApproval lastOne = employeeApprovalMapper.selectOne(wrapper);
        
        int seq = 1;
        if (lastOne != null && lastOne.getApprovalNo() != null) {
            String lastNo = lastOne.getApprovalNo();
            String seqStr = lastNo.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }

        return prefix + String.format("%04d", seq);
    }

    /**
     * 根据审批类型保存详情数据
     * 从 formJson JSON字符串反序列化为对应的 CreateDTO，再保存到详情表
     */
    private void saveDetailByType(String approvalId, EmployeeApprovalCreateDTO dto) {
        ObjectMapper objectMapper = new ObjectMapper();
        // 注册 Java 8 时间模块以支持 LocalDate/LocalDateTime 反序列化
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        try {
            switch (dto.getType()) {
                case "leave": {
                    LeaveCreateDTO leaveDto = objectMapper.readValue(dto.getFormJson(), LeaveCreateDTO.class);
                    saveLeaveDetail(approvalId, leaveDto);
                    break;
                }
                case "overtime": {
                    OvertimeCreateDTO overtimeDto = objectMapper.readValue(dto.getFormJson(), OvertimeCreateDTO.class);
                    saveOvertimeDetail(approvalId, overtimeDto);
                    break;
                }
                case "swap": {
                    SwapCreateDTO swapDto = objectMapper.readValue(dto.getFormJson(), SwapCreateDTO.class);
                    saveSwapDetail(approvalId, swapDto);
                    break;
                }
                case "travel": {
                    TravelCreateDTO travelDto = objectMapper.readValue(dto.getFormJson(), TravelCreateDTO.class);
                    saveTravelDetail(approvalId, travelDto);
                    break;
                }
                case "reimbursement": {
                    ReimbursementCreateDTO reimbursementDto = objectMapper.readValue(dto.getFormJson(), ReimbursementCreateDTO.class);
                    saveReimbursementDetail(approvalId, reimbursementDto);
                    break;
                }
                case "requisition": {
                    RequisitionCreateDTO requisitionDto = objectMapper.readValue(dto.getFormJson(), RequisitionCreateDTO.class);
                    saveRequisitionDetail(approvalId, requisitionDto);
                    break;
                }
                default:
                    throw new RuntimeException("不支持的审批类型：" + dto.getType());
            }
        } catch (JsonProcessingException e) {
            log.error("解析审批表单JSON失败，类型：{}，formJson：{}", dto.getType(), dto.getFormJson(), e);
            throw new RuntimeException("审批表单数据格式错误：" + e.getMessage());
        }
    }

    /**
     * 保存请假详情
     * LeaveCreateDTO 无 days 字段，需从 startDate/endDate 自动计算请假天数
     */
    private void saveLeaveDetail(String approvalId, LeaveCreateDTO leaveDto) {
        if (leaveDto == null) {
            throw new RuntimeException("请假信息不能为空");
        }
        LeaveRequestEntity entity = new LeaveRequestEntity();
        entity.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        entity.setApprovalId(approvalId);
        entity.setLeaveType(leaveDto.getLeaveType());
        entity.setStartDate(leaveDto.getStartDate());
        entity.setEndDate(leaveDto.getEndDate());
        // LeaveCreateDTO 无 days 字段，根据起止日期计算天数（含首尾）
        long calculatedDays = ChronoUnit.DAYS.between(
                leaveDto.getStartDate(), leaveDto.getEndDate()) + 1;
        entity.setDays(BigDecimal.valueOf(calculatedDays));
        entity.setReason(leaveDto.getReason());
        entity.setContactPhone(leaveDto.getContactPhone());
        entity.setHandoverTo(leaveDto.getHandoverTo());
        entity.setHandoverNote(leaveDto.getHandoverNote());
        // Entity 使用 createdTime（非 createTime）
        entity.setCreatedTime(LocalDateTime.now());
        leaveRequestMapper.insert(entity);
    }

    /**
     * 保存加班详情
     * OvertimeCreateDTO 的 startTime/endTime 为 String(HH:mm)，需转换为 LocalTime；
     * 无 hours 字段，根据起止时间自动计算加班小时数
     */
    private void saveOvertimeDetail(String approvalId, OvertimeCreateDTO overtimeDto) {
        if (overtimeDto == null) {
            throw new RuntimeException("加班信息不能为空");
        }
        OvertimeRequestEntity entity = new OvertimeRequestEntity();
        entity.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        entity.setApprovalId(approvalId);
        entity.setOvertimeDate(overtimeDto.getOvertimeDate());
        // DTO 的 startTime/endTime 是 String(HH:mm)，Entity 需要 LocalTime，需解析转换
        LocalTime startLocalTime = LocalTime.parse(overtimeDto.getStartTime());
        LocalTime endLocalTime = LocalTime.parse(overtimeDto.getEndTime());
        entity.setStartTime(startLocalTime);
        entity.setEndTime(endLocalTime);
        // DTO 无 hours 字段，根据起止时间计算小时数（精度保留1位小数）
        long minutesDiff = ChronoUnit.MINUTES.between(startLocalTime, endLocalTime);
        double hoursCalculated = minutesDiff / 60.0;
        entity.setHours(BigDecimal.valueOf(hoursCalculated).setScale(1, RoundingMode.HALF_UP));
        entity.setReason(overtimeDto.getReason());
        // 补偿方式和加班类型（注意 Entity 字段名为 overtimeType，非 oimeType）
        entity.setCompensateType(overtimeDto.getCompensateType());
        entity.setOvertimeType(overtimeDto.getOvertimeType());
        // Entity 使用 createdTime（非 createTime）
        entity.setCreatedTime(LocalDateTime.now());
        overtimeRequestMapper.insert(entity);
    }

    /**
     * 保存换班详情
     * SwapCreateDTO 使用 originalDate/targetDate/reason（非 originalShiftDate/targetShiftDate/swapReason）
     */
    private void saveSwapDetail(String approvalId, SwapCreateDTO swapDto) {
        if (swapDto == null) {
            throw new RuntimeException("换班信息不能为空");
        }
        SwapRequestDetailEntity entity = new SwapRequestDetailEntity();
        entity.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        entity.setApprovalId(approvalId);
        // 目标换班人
        entity.setTargetEmployeeId(swapDto.getTargetEmployeeId());
        // 原班次信息（DTO 字段名 originalDate，对应 Entity originalDate）
        entity.setOriginalDate(swapDto.getOriginalDate());
        entity.setOriginalShiftType(swapDto.getOriginalShiftType());
        // 目标班次信息（DTO 字段名 targetDate，对应 Entity targetDate）
        entity.setTargetDate(swapDto.getTargetDate());
        entity.setTargetShiftType(swapDto.getTargetShiftType());
        // 换班原因（DTO 字段名 reason，非 swapReason）
        entity.setReason(swapDto.getReason());
        // 覆盖方案
        entity.setCoveragePlan(swapDto.getCoveragePlan());
        // 对方确认状态默认 false
        entity.setPartnerConfirmed(false);
        // Entity 使用 createdTime（非 createTime）
        entity.setCreatedTime(LocalDateTime.now());
        swapRequestDetailMapper.insert(entity);
    }

    /**
     * 保存出差详情
     * TravelCreateDTO 无 days 字段，需从 startDate/endDate 计算；
     * DTO 使用 travelPurpose/estimatedBudget（非 purpose/estimatedCost）；
     * estimatedBudget 前端传入元，Entity 存储分，需做元转分
     */
    private void saveTravelDetail(String approvalId, TravelCreateDTO travelDto) {
        if (travelDto == null) {
            throw new RuntimeException("出差信息不能为空");
        }
        TravelRequestEntity entity = new TravelRequestEntity();
        entity.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        entity.setApprovalId(approvalId);
        entity.setDestination(travelDto.getDestination());
        // Entity 字段名 travelPurpose（非 purpose）
        entity.setTravelPurpose(travelDto.getTravelPurpose());
        entity.setStartDate(travelDto.getStartDate());
        entity.setEndDate(travelDto.getEndDate());
        // DTO 无 days 字段，根据起止日期计算出差天数（含首尾）
        int calculatedDays = (int) (ChronoUnit.DAYS.between(
                travelDto.getStartDate(), travelDto.getEndDate()) + 1);
        entity.setDays(calculatedDays);
        // 交通方式和住宿需求
        entity.setTransportType(travelDto.getTransportType());
        entity.setHotelRequired(travelDto.getHotelRequired());
        // 预估预算：前端传入元，后端转换为分存储（Math.round(yuan * 100)）
        if (travelDto.getEstimatedBudget() != null) {
            long budgetFen = Math.round(travelDto.getEstimatedBudget().doubleValue() * 100);
            entity.setEstimatedBudget(BigDecimal.valueOf(budgetFen));
        }
        // 关联任务ID
        entity.setRelatedTaskId(travelDto.getRelatedTaskId());
        // Entity 使用 createdTime（非 createTime）
        entity.setCreatedTime(LocalDateTime.now());
        travelRequestMapper.insert(entity);
    }

    /**
     * 保存报销详情
     * ReimbursementCreateDTO 使用 items(列表)/reimbursementCategory/bankAccount/payeeName；
     * 无 amount/category/invoiceNumber/invoiceDate/relatedTravelApprovalId 等字段
     */
    private void saveReimbursementDetail(String approvalId, ReimbursementCreateDTO reimbursementDto) {
        if (reimbursementDto == null) {
            throw new RuntimeException("报销信息不能为空");
        }
        ReimbursementRequestEntity entity = new ReimbursementRequestEntity();
        entity.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        entity.setApprovalId(approvalId);
        // 报销类别（Entity 字段名 reimbursementCategory，非 category）
        entity.setReimbursementCategory(reimbursementDto.getReimbursementCategory());
        // 报销说明
        entity.setDescription(reimbursementDto.getDescription());
        // 收款账号和收款人
        entity.setBankAccount(reimbursementDto.getBankAccount());
        entity.setPayeeName(reimbursementDto.getPayeeName());
        // 关联出差申请ID（Entity 字段名 relatedTravelId，非 relatedTravelApprovalId）
        entity.setRelatedTravelId(reimbursementDto.getRelatedTravelId());
        // 从 items 列表计算总金额和票据数量
        if (reimbursementDto.getItems() != null && !reimbursementDto.getItems().isEmpty()) {
            entity.setReceiptCount(reimbursementDto.getItems().size());
            // TODO: 从 ReimbursementItemDTO 累加各项目金额得到总金额（单位：分）
            // 当前暂设为0，待确认 ReimbursementItemDTO 的金额字段后补充累加逻辑
            entity.setTotalAmount(BigDecimal.ZERO);
        } else {
            entity.setReceiptCount(0);
            entity.setTotalAmount(BigDecimal.ZERO);
        }
        // Entity 使用 createdTime（非 createTime）
        entity.setCreatedTime(LocalDateTime.now());
        reimbursementRequestMapper.insert(entity);
    }

    /**
     * 保存领用详情
     * RequisitionCreateDTO 使用 urgencyLevel/supplierPreference（非 isUrgent/supplierId）；
     * 无 itemId 字段；estimatedCost 前端传入元，Entity 存储分，需做元转分
     */
    private void saveRequisitionDetail(String approvalId, RequisitionCreateDTO requisitionDto) {
        if (requisitionDto == null) {
            throw new RuntimeException("领用信息不能为空");
        }
        RequisitionRequestEntity entity = new RequisitionRequestEntity();
        entity.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        entity.setApprovalId(approvalId);
        // 物品基本信息（Entity 使用 itemName，无 itemId 字段）
        entity.setItemName(requisitionDto.getItemName());
        // 分类ID（Long 类型）
        entity.setCategoryId(requisitionDto.getCategoryId());
        // 数量和单位
        entity.setQuantity(requisitionDto.getQuantity());
        entity.setUnit(requisitionDto.getUnit());
        // 用途说明
        entity.setPurpose(requisitionDto.getPurpose());
        // 期望供应商（Entity 字段名 supplierPreference，非 supplierId）
        entity.setSupplierPreference(requisitionDto.getSupplierPreference());
        // 紧急程度（Entity 字段名 urgencyLevel，非 isUrgent）
        entity.setUrgencyLevel(requisitionDto.getUrgencyLevel() != null ? requisitionDto.getUrgencyLevel() : "normal");
        // 期望到货日期
        entity.setExpectedDate(requisitionDto.getExpectedDate());
        // 预估费用：前端传入元，后端转换为分存储（Math.round(yuan * 100)）
        if (requisitionDto.getEstimatedCost() != null) {
            long costFen = Math.round(requisitionDto.getEstimatedCost().doubleValue() * 100);
            entity.setEstimatedCost(BigDecimal.valueOf(costFen));
        }
        // Entity 使用 createdTime（非 createTime）
        entity.setCreatedTime(LocalDateTime.now());
        requisitionRequestMapper.insert(entity);
    }

    /**
     * 根据审批类型构建上下文数据
     */
    private Object buildContextDataByType(EmployeeApproval approval) {
        switch (approval.getType()) {
            case "leave": {
                LeaveRequestEntity request = getLeaveRequest(approval.getApprovalId());
                return request != null ? approvalDataService.buildLeaveContext(approval.getApprovalId(), request) : null;
            }
            case "overtime": {
                OvertimeRequestEntity request = getOvertimeRequest(approval.getApprovalId());
                return request != null ? approvalDataService.buildOvertimeContext(approval.getApprovalId(), request) : null;
            }
            case "swap": {
                SwapRequestDetailEntity request = getSwapRequest(approval.getApprovalId());
                return request != null ? approvalDataService.buildSwapContext(approval.getApprovalId(), request) : null;
            }
            case "travel": {
                TravelRequestEntity request = getTravelRequest(approval.getApprovalId());
                return request != null ? approvalDataService.buildTravelContext(approval.getApprovalId(), request) : null;
            }
            case "reimbursement": {
                ReimbursementRequestEntity request = getReimbursementRequest(approval.getApprovalId());
                return request != null ? approvalDataService.buildReimbursementContext(approval.getApprovalId(), request) : null;
            }
            case "requisition": {
                RequisitionRequestEntity request = getRequisitionRequest(approval.getApprovalId());
                return request != null ? approvalDataService.buildRequisitionContext(approval.getApprovalId(), request) : null;
            }
            default:
                return null;
        }
    }

    /**
     * 获取请假详情
     */
    private LeaveRequestEntity getLeaveRequest(String approvalId) {
        LambdaQueryWrapper<LeaveRequestEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeaveRequestEntity::getApprovalId, approvalId);
        return leaveRequestMapper.selectOne(wrapper);
    }

    /**
     * 获取加班详情
     */
    private OvertimeRequestEntity getOvertimeRequest(String approvalId) {
        LambdaQueryWrapper<OvertimeRequestEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OvertimeRequestEntity::getApprovalId, approvalId);
        return overtimeRequestMapper.selectOne(wrapper);
    }

    /**
     * 获取换班详情
     */
    private SwapRequestDetailEntity getSwapRequest(String approvalId) {
        LambdaQueryWrapper<SwapRequestDetailEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SwapRequestDetailEntity::getApprovalId, approvalId);
        return swapRequestDetailMapper.selectOne(wrapper);
    }

    /**
     * 获取出差详情
     */
    private TravelRequestEntity getTravelRequest(String approvalId) {
        LambdaQueryWrapper<TravelRequestEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TravelRequestEntity::getApprovalId, approvalId);
        return travelRequestMapper.selectOne(wrapper);
    }

    /**
     * 获取报销详情
     */
    private ReimbursementRequestEntity getReimbursementRequest(String approvalId) {
        LambdaQueryWrapper<ReimbursementRequestEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReimbursementRequestEntity::getApprovalId, approvalId);
        return reimbursementRequestMapper.selectOne(wrapper);
    }

    /**
     * 获取领用详情
     */
    private RequisitionRequestEntity getRequisitionRequest(String approvalId) {
        LambdaQueryWrapper<RequisitionRequestEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RequisitionRequestEntity::getApprovalId, approvalId);
        return requisitionRequestMapper.selectOne(wrapper);
    }

    /**
     * 构建审批流程节点信息
     */
    private List<ApprovalFlowNodeVO> buildFlowNodes(EmployeeApproval approval) {
        List<ApprovalFlowNodeVO> nodes = new ArrayList<>();

        // 节点1: 申请提交
        ApprovalFlowNodeVO submitNode = new ApprovalFlowNodeVO();
        submitNode.setNodeName("提交申请");
        submitNode.setOperatorId(approval.getApplicantId());
        submitNode.setOperatorName(getEmployeeName(approval.getApplicantId()));
        submitNode.setActionTime(approval.getCreatedTime());
        submitNode.setStatus("completed");
        nodes.add(submitNode);

        // 节点2: 审批处理
        ApprovalFlowNodeVO reviewNode = new ApprovalFlowNodeVO();
        reviewNode.setNodeName("审批处理");
        reviewNode.setOperatorId(approval.getCurrentApproverId());
        reviewNode.setOperatorName(getEmployeeName(approval.getCurrentApproverId()));
        if ("pending".equals(approval.getStatus())) {
            reviewNode.setStatus("current");  // 当前节点
        } else if ("approved".equals(approval.getStatus()) || "rejected".equals(approval.getStatus())) {
            reviewNode.setStatus("completed");
            reviewNode.setActionTime(approval.getUpdatedTime());
        } else if ("withdrawn".equals(approval.getStatus())) {
            reviewNode.setStatus("cancelled");
        }
        nodes.add(reviewNode);

        // 节点3: 完成（已通过时显示）
        if ("approved".equals(approval.getStatus())) {
            ApprovalFlowNodeVO doneNode = new ApprovalFlowNodeVO();
            doneNode.setNodeName("审批完成");
            doneNode.setStatus("completed");
            doneNode.setActionTime(approval.getUpdatedTime());
            nodes.add(doneNode);
        }

        return nodes;
    }

    /**
     * 构建操作日志列表
     * TODO: 对接真实的操作日志表 approval_operation_log
     */
    private List<ApprovalOperationLogVO> buildOperationLogs(EmployeeApproval approval) {
        List<ApprovalOperationLogVO> logs = new ArrayList<>();

        // 提交日志
        ApprovalOperationLogVO submitLog = new ApprovalOperationLogVO();
        submitLog.setOperatorId(approval.getApplicantId());
        submitLog.setOperatorName(getEmployeeName(approval.getApplicantId()));
        submitLog.setAction("submit");
        submitLog.setActionName("提交申请");
        submitLog.setComment(approval.getRemark());
        submitLog.setActionTime(approval.getCreatedTime());
        logs.add(submitLog);

        // 如果已有审批结果，添加审批日志
        if (approval.getUpdatedTime() != null && !"pending".equals(approval.getStatus())) {
            ApprovalOperationLogVO reviewLog = new ApprovalOperationLogVO();
            reviewLog.setOperatorId(approval.getCurrentApproverId());
            reviewLog.setOperatorName(getEmployeeName(approval.getCurrentApproverId()));
            reviewLog.setAction(approval.getStatus());
            reviewLog.setActionName("approved".equals(approval.getStatus()) ? "通过审批" :
                                   "rejected".equals(approval.getStatus()) ? "驳回审批" : "");
            reviewLog.setComment(approval.getRemark());
            reviewLog.setActionTime(approval.getUpdatedTime());
            logs.add(reviewLog);
        }

        return logs;
    }

    /**
     * 记录操作日志
     * TODO: 对接真实的操作日志表，当前仅打印日志
     */
    private void logOperationLog(String approvalId, String operatorId, String action, String comment) {
        log.info("审批操作日志 - 审批ID：{}，操作人：{}，动作：{}，备注：{}",
                approvalId, operatorId, action, comment);
        // TODO: 持久化到 approval_operation_log 表
    }

    /**
     * 获取员工姓名（简化处理，实际应从EmployeeService或缓存获取）
     */
    private String getEmployeeName(String employeeId) {
        if (employeeId == null || employeeId.isEmpty()) {
            return "系统";
        }
        // TODO: 从 EmployeeDataService 或缓存中获取员工姓名
        // 当前返回占位符
        return "员工-" + employeeId.substring(0, Math.min(6, employeeId.length()));
    }
}
