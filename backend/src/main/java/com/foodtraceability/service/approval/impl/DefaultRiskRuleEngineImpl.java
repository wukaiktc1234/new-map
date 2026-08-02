package com.foodtraceability.service.approval.impl;

import com.foodtraceability.dto.approval.vo.RiskWarningVO;
import com.foodtraceability.entity.approval.EmployeeApproval;
import com.foodtraceability.service.approval.RiskRuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 默认风控规则引擎实现
 * 内置24条规则，覆盖6种审批类型：请假(4)、加班(4)、换班(3)、出差(3)、报销(5)、领用(5)
 *
 * 风险等级优先级（从高到低）：
 * prohibited(禁止) > danger(危险) > warning(警告) > info(提示)
 */
@Service
public class DefaultRiskRuleEngineImpl implements RiskRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultRiskRuleEngineImpl.class);

    /**
     * 评估单个审批申请的风险等级
     */
    @Override
    public RiskWarningVO evaluate(EmployeeApproval approval, Object contextData) {
        if (approval == null || approval.getType() == null) {
            return null;
        }

        List<RiskWarningVO> warnings = new ArrayList<>();

        // 根据审批类型执行对应的规则检查
        switch (approval.getType()) {
            case "leave":
                evaluateLeaveRules(approval, contextData, warnings);
                break;
            case "overtime":
                evaluateOvertimeRules(approval, contextData, warnings);
                break;
            case "swap":
                evaluateSwapRules(approval, contextData, warnings);
                break;
            case "travel":
                evaluateTravelRules(approval, contextData, warnings);
                break;
            case "reimbursement":
                evaluateReimbursementRules(approval, contextData, warnings);
                break;
            case "requisition":
                evaluateRequisitionRules(approval, contextData, warnings);
                break;
            default:
                log.warn("未知的审批类型：{}", approval.getType());
                break;
        }

        // 返回最高级别的预警，无风险时返回null
        return warnings.stream()
                .max(Comparator.comparingInt(this::levelPriority))
                .orElse(null);
    }

    /**
     * 批量评估多个审批申请的风险等级
     */
    @Override
    public Map<String, RiskWarningVO> batchEvaluate(List<EmployeeApproval> approvals) {
        if (approvals == null || approvals.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, RiskWarningVO> result = new HashMap<>();
        for (EmployeeApproval approval : approvals) {
            // 批量评估时不传入详细contextData，仅基于approval本身的信息进行基础规则检查
            RiskWarningVO warning = evaluate(approval, null);
            if (warning != null) {
                result.put(approval.getApprovalId(), warning);
            }
        }

        log.debug("批量风控评估完成，共{}条审批，命中风险{}", approvals.size(), result.size());
        return result;
    }

    // ==================== 请假规则（4条）====================

    /**
     * 请假类风控规则
     * 规则1-4: 连续请假天数、年假余额、近期请假频率、年假耗尽预警
     */
    @SuppressWarnings("unchecked")
    private void evaluateLeaveRules(EmployeeApproval approval, Object contextData, List<RiskWarningVO> warnings) {
        Map<String, Object> ctx = safeCastToMap(contextData);

        // 规则1: 连续请假超过3天 → info "注意工作交接"
        Number days = getNumberFromMap(ctx, "days");
        if (days != null && days.doubleValue() > 3) {
            warnings.add(createWarning("info", "注意工作交接",
                    "连续请假" + days.intValue() + "天，请提前做好工作交接安排", "LEAVE_001"));
        }

        // 规则2-4: 基于假期余额的规则检查
        Map<String, Object> balance = getMapFromMap(ctx, "leaveBalance");
        String leaveType = getStringFromMap(ctx, "leaveType");
        Number remaining = balance != null ? getNumberFromMap(balance, "annualLeaveRemaining") : null;

        // 规则2: 年假余额不足 → warning "年假余额不足"
        if ("annual".equals(leaveType) && remaining != null && days != null
                && remaining.doubleValue() < days.doubleValue()) {
            warnings.add(createWarning("warning", "年假余额不足",
                    "当前年假剩余" + remaining.intValue() + "天，申请" + days.intValue() + "天", "LEAVE_002"));
        }

        // 规则3: 30天内请假>=3次 → warning "近期请假频繁"
        Number recentCount = getNumberFromMap(ctx, "recentLeaveCount30Days");
        if (recentCount != null && recentCount.intValue() >= 3) {
            warnings.add(createWarning("warning", "近期请假频繁",
                    "近30天内已请假" + recentCount.intValue() + "次，请注意考勤管理", "LEAVE_003"));
        }

        // 规则4: 年假余额<1天且申请年假 → danger "年假即将耗尽"
        if ("annual".equals(leaveType) && remaining != null && remaining.doubleValue() < 1.0) {
            warnings.add(createWarning("danger", "年假即将耗尽",
                    "年假余额仅剩" + remaining.doubleValue() + "天，本年度年假即将用尽", "LEAVE_004"));
        }
    }

    // ==================== 加班规则（4条）====================

    /**
     * 加班类风控规则
     * 规则5-8: 月度额度、工作日加班频率、连续加班、法定限制
     */
    @SuppressWarnings("unchecked")
    private void evaluateOvertimeRules(EmployeeApproval approval, Object contextData, List<RiskWarningVO> warnings) {
        Map<String, Object> ctx = safeCastToMap(contextData);

        Number monthlyHours = getNumberFromMap(ctx, "monthlyOvertimeHours");
        Number monthlyLimit = getNumberFromMap(ctx, "monthlyOvertimeLimit");
        Number workdayCount = getNumberFromMap(ctx, "monthlyWorkdayOvertimeCount");
        Number currentHours = getNumberFromMap(ctx, "hours");

        // 规则5: 本月加班>80%上限 → warning "本月加班额度即将用尽"
        if (monthlyHours != null && monthlyLimit != null
                && monthlyHours.doubleValue() > monthlyLimit.doubleValue() * 0.8) {
            double usedPercent = (monthlyHours.doubleValue() / monthlyLimit.doubleValue()) * 100;
            warnings.add(createWarning("warning", "本月加班额度即将用尽",
                    "本月已加班" + monthlyHours.intValue() + "小时，使用率达" + String.format("%.0f%%", usedPercent), "OT_001"));
        }

        // 规则6: 本月工作日加班>=3次 → info "注意劳逸结合"
        if (workdayCount != null && workdayCount.intValue() >= 3) {
            warnings.add(createWarning("info", "注意劳逸结合",
                    "本月工作日已加班" + workdayCount.intValue() + "次，请注意休息", "OT_002"));
        }

        // 规则7: 连续3天内有加班 → info "连续加班请注意休息"
        // TODO: 需要基于具体日期判断连续性，此处简化处理
        if (workdayCount != null && workdayCount.intValue() >= 2) {
            warnings.add(createWarning("info", "连续加班请注意休息",
                    "检测到近期有连续加班情况，请关注员工身体状况", "OT_003"));
        }

        // 规则8: 加班时长>36h/月 → danger "超出法定加班限制"
        if (monthlyHours != null && monthlyLimit != null
                && monthlyHours.doubleValue() > 36.0) {
            warnings.add(createWarning("danger", "超出法定加班限制",
                    "本月累计加班" + monthlyHours.intValue() + "小时，已超过36小时/月的法定限制", "OT_004"));
        }
    }

    // ==================== 换班规则（3条）====================

    /**
     * 换班类风控规则
     * 规则9-11: 换班频率、对方确认、排班冲突
     */
    @SuppressWarnings("unchecked")
    private void evaluateSwapRules(EmployeeApproval approval, Object contextData, List<RiskWarningVO> warnings) {
        Map<String, Object> ctx = safeCastToMap(contextData);

        // 规则9: 本月换班次数>=3次 → info "本月换班较频繁"
        Number swapCount = getNumberFromMap(ctx, "monthlySwapCount");
        if (swapCount != null && swapCount.intValue() >= 3) {
            warnings.add(createWarning("info", "本月换班较频繁",
                    "本月已换班" + swapCount.intValue() + "次，建议合理安排排班", "SWAP_001"));
        }

        // 规则10: 对方未确认 → info "等待对方确认"
        Boolean isConfirmed = getBooleanFromMap(ctx, "isTargetConfirmed");
        if (isConfirmed != null && !isConfirmed.booleanValue()) {
            warnings.add(createWarning("info", "等待对方确认",
                    "换班对方尚未确认，请等待对方响应后再提交审批", "SWAP_002"));
        }

        // 规则11: 与目标班次冲突 → warning "可能存在排班冲突"
        Boolean hasConflict = getBooleanFromMap(ctx, "hasConflict");
        if (hasConflict != null && hasConflict.booleanValue()) {
            warnings.add(createWarning("warning", "可能存在排班冲突",
                    "目标班次时间与现有排班存在冲突，请联系排班管理员确认", "SWAP_003"));
        }
    }

    // ==================== 出差规则（3条）====================

    /**
     * 出差类风控规则
     * 规则12-14: 预算超标、出差频率、关联任务
     */
    @SuppressWarnings("unchecked")
    private void evaluateTravelRules(EmployeeApproval approval, Object contextData, List<RiskWarningVO> warnings) {
        Map<String, Object> ctx = safeCastToMap(contextData);

        Number estimatedCost = getNumberFromMap(ctx, "estimatedCost");
        Number policyLimit = getNumberFromMap(ctx, "travelPolicyLimit");

        // 规则12: 预算超出差旅政策 → warning "超出差旅标准"
        if (estimatedCost != null && policyLimit != null
                && estimatedCost.doubleValue() > policyLimit.doubleValue()) {
            warnings.add(createWarning("warning", "超出差旅标准",
                    "预计费用" + estimatedCost.intValue() + "元，超出差旅政策限额" + policyLimit.intValue() + "元", "TRAVEL_001"));
        }

        // 规则13: 本年度出差>=6次 → info "本年度出差较多"
        Number yearlyCount = getNumberFromMap(ctx, "yearlyTravelCount");
        if (yearlyCount != null && yearlyCount.intValue() >= 6) {
            warnings.add(createWarning("info", "本年度出差较多",
                    "本年度已出差" + yearlyCount.intValue() + "次，请注意工作效率平衡", "TRAVEL_002"));
        }

        // 规则14: 无关联任务 → info "建议关联工作任务"
        Boolean hasTask = getBooleanFromMap(ctx, "hasRelatedTask");
        if (hasTask != null && !hasTask.booleanValue()) {
            warnings.add(createWarning("info", "建议关联工作任务",
                    "本次出差未关联工作任务，建议补充关联以便费用归集和效果追踪", "TRAVEL_003"));
        }
    }

    // ==================== 报销规则（5条）====================

    /**
     * 报销类风控规则
     * 规则15-19: 月预算超标、接近上限、大额报销、发票缺失、出差预算超限
     */
    @SuppressWarnings("unchecked")
    private void evaluateReimbursementRules(EmployeeApproval approval, Object contextData, List<RiskWarningVO> warnings) {
        Map<String, Object> ctx = safeCastToMap(contextData);

        Number amount = getNumberFromMap(ctx, "amount");
        Number monthlyUsed = getNumberFromMap(ctx, "monthlyReimbursedAmount");
        Number monthlyBudget = getNumberFromMap(ctx, "monthlyBudget");
        String invoiceNumber = getStringFromMap(ctx, "invoiceNumber");

        // 规则15: 本月报销>90%月预算 → danger "本月报销额度严重超标"
        if (monthlyUsed != null && monthlyBudget != null) {
            double ratio = monthlyUsed.doubleValue() / monthlyBudget.doubleValue();
            if (ratio > 0.9) {
                warnings.add(createWarning("danger", "本月报销额度严重超标",
                        "本月已报销" + monthlyUsed.intValue() + "元，使用率已达" + String.format("%.0f%%", ratio * 100),
                        "REIMB_001"));
            }
            // 规则16: 本月报销>70%月预算 → warning "本月报销接近预算上限"
            else if (ratio > 0.7) {
                warnings.add(createWarning("warning", "本月报销接近预算上限",
                        "本月已报销" + monthlyUsed.intValue() + "元，使用率达" + String.format("%.0f%%", ratio * 100),
                        "REIMB_002"));
            }
        }

        // 规则17: 单笔>5000元 → info "大额报销需特别审核"
        if (amount != null && amount.doubleValue() > 5000) {
            warnings.add(createWarning("info", "大额报销需特别审核",
                    "单笔报销金额" + amount.intValue() + "元，超过5000元需财务主管复核", "REIMB_003"));
        }

        // 规则18: 缺少发票号 → warning "请确保票据齐全"
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            warnings.add(createWarning("warning", "请确保票据齐全",
                    "该报销单缺少发票号码信息，请补充完整票据信息", "REIMB_004"));
        }

        // 规则19: 关联出差但金额超出差预算 → warning "报销金额超出出差预算"
        Number travelBudgetUsed = getNumberFromMap(ctx, "travelBudgetUsed");
        Number travelBudgetLimit = getNumberFromMap(ctx, "travelBudgetLimit");
        if (travelBudgetUsed != null && travelBudgetLimit != null && amount != null) {
            double totalAfterThis = travelBudgetUsed.doubleValue() + amount.doubleValue();
            if (totalAfterThis > travelBudgetLimit.doubleValue()) {
                warnings.add(createWarning("warning", "报销金额超出出差预算",
                        "关联出差已用" + travelBudgetUsed.intValue() + "元，本次报销后将超出" + travelBudgetLimit.intValue() + "元限额",
                        "REIMB_005"));
            }
        }
    }

    // ==================== 领用规则（5条）====================

    /**
     * 领用类风控规则
     * 规则20-24: 库存偏低、低于安全线、部门预算、紧急领用、非首选供应商
     */
    @SuppressWarnings("unchecked")
    private void evaluateRequisitionRules(EmployeeApproval approval, Object contextData, List<RiskWarningVO> warnings) {
        Map<String, Object> ctx = safeCastToMap(contextData);

        Number currentStock = getNumberFromMap(ctx, "currentStock");
        Number safetyStock = getNumberFromMap(ctx, "safetyStock");
        Number monthlyAvgUsage = getNumberFromMap(ctx, "monthlyAvgUsage");
        Boolean isUrgent = getBooleanFromMap(ctx, "isUrgent");
        Boolean isPreferredSupplier = getBooleanFromMap(ctx, "isPreferredSupplier");

        // 规则20: 库存<月均用量*2 → warning "库存偏低建议及时补充"
        if (currentStock != null && monthlyAvgUsage != null
                && currentStock.doubleValue() < monthlyAvgUsage.doubleValue() * 2) {
            warnings.add(createWarning("warning", "库存偏低建议及时补充",
                    "当前库存" + currentStock.intValue() + "，低于月均用量2倍(" + (monthlyAvgUsage.intValue() * 2) + ")",
                    "REQ_001"));
        }

        // 规则21: 库存<安全库存 → danger "库存低于安全线"
        if (currentStock != null && safetyStock != null
                && currentStock.doubleValue() < safetyStock.doubleValue()) {
            warnings.add(createWarning("danger", "库存低于安全线",
                    "当前库存" + currentStock.intValue() + "，已低于安全库存线" + safetyStock.intValue(),
                    "REQ_002"));
        }

        // 规则22: 本月领用超部门预算80% → warning "部门预算使用率较高"
        Number deptMonthlyUsed = getNumberFromMap(ctx, "departmentMonthlyUsed");
        Number deptMonthlyBudget = getNumberFromMap(ctx, "departmentMonthlyBudget");
        if (deptMonthlyUsed != null && deptMonthlyBudget != null
                && deptMonthlyUsed.doubleValue() > deptMonthlyBudget.doubleValue() * 0.8) {
            double usedPercent = (deptMonthlyUsed.doubleValue() / deptMonthlyBudget.doubleValue()) * 100;
            warnings.add(createWarning("warning", "部门预算使用率较高",
                    "部门本月领用预算使用率达" + String.format("%.0f%%", usedPercent), "REQ_003"));
        }

        // 规则23: 紧急领用 → info "紧急领用需优先处理"
        if (isUrgent != null && isUrgent.booleanValue()) {
            warnings.add(createWarning("info", "紧急领用需优先处理",
                    "本次为紧急领用申请，建议优先审批处理", "REQ_004"));
        }

        // 规则24: 供应商非首选 → info "使用了非首选供应商"
        if (isPreferredSupplier != null && !isPreferredSupplier.booleanValue()) {
            warnings.add(createWarning("info", "使用了非首选供应商",
                    "本次领用使用的供应商非系统首选供应商，采购成本可能偏高", "REQ_005"));
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 创建风险预警对象
     * @param level 风险级别：prohibited/danger/warning/info
     * @param title 预警标题
     * @param description 预警描述
     * @param ruleCode 规则编码
     * @return 风险预警对象
     */
    private RiskWarningVO createWarning(String level, String title, String description, String ruleCode) {
        RiskWarningVO warning = new RiskWarningVO();
        warning.setLevel(level);
        warning.setTitle(title);
        warning.setDescription(description);
        warning.setRuleCode(ruleCode);
        warning.setEvaluateTime(LocalDateTime.now());
        return warning;
    }

    /**
     * 获取风险级别的优先级数值（用于比较）
     */
    private int levelPriority(RiskWarningVO w) {
        if (w == null || w.getLevel() == null) {
            return 0;
        }
        return switch (w.getLevel()) {
            case "prohibited" -> 4;
            case "danger" -> 3;
            case "warning" -> 2;
            case "info" -> 1;
            default -> 0;
        };
    }

    /**
     * 安全地将Object转为Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> safeCastToMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return new HashMap<>();
    }

    /**
     * 从Map中获取Number类型的值
     */
    private Number getNumberFromMap(Map<String, Object> map, String key) {
        if (map == null || key == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return (Number) value;
        }
        return null;
    }

    /**
     * 从Map中获取String类型的值
     */
    private String getStringFromMap(Map<String, Object> map, String key) {
        if (map == null || key == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 从Map中获取Boolean类型的值
     */
    private Boolean getBooleanFromMap(Map<String, Object> map, String key) {
        if (map == null || key == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }

    /**
     * 从Map中获取嵌套的Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapFromMap(Map<String, Object> map, String key) {
        if (map == null || key == null || !map.containsKey(key)) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }
}
