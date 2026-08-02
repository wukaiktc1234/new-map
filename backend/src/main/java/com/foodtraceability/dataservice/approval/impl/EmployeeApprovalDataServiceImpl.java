package com.foodtraceability.dataservice.approval.impl;

import com.foodtraceability.dataservice.approval.EmployeeApprovalDataService;
import com.foodtraceability.entity.approval.LeaveRequestEntity;
import com.foodtraceability.entity.approval.OvertimeRequestEntity;
import com.foodtraceability.entity.approval.SwapRequestDetailEntity;
import com.foodtraceability.entity.approval.TravelRequestEntity;
import com.foodtraceability.entity.approval.ReimbursementRequestEntity;
import com.foodtraceability.entity.approval.RequisitionRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 员工审批数据服务实现类
 * 负责组装各类型审批的上下文数据，聚合多个数据源信息
 */
@Service
public class EmployeeApprovalDataServiceImpl implements EmployeeApprovalDataService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeApprovalDataServiceImpl.class);

    /**
     * 组装请假类型的上下文数据
     */
    @Override
    public Object buildLeaveContext(String approvalId, LeaveRequestEntity request) {
        Map<String, Object> context = new HashMap<>();
        context.put("approvalId", approvalId);
        context.put("leaveType", request.getLeaveType());
        context.put("startDate", request.getStartDate());
        context.put("endDate", request.getEndDate());
        context.put("days", request.getDays());
        context.put("reason", request.getReason());

        // TODO: 对接真实员工假期余额表，当前使用模拟数据
        // 模拟假期余额数据（后续需从 employee_leave_balance 表查询）
        Map<String, Object> balance = new HashMap<>();
        balance.put("annualLeaveRemaining", 5.0);  // 年假剩余天数
        balance.put("sickLeaveRemaining", 10.0);   // 病假剩余天数
        balance.put("personalLeaveRemaining", 3.0); // 事假剩余天数
        context.put("leaveBalance", balance);

        // TODO: 对接真实历史请假记录表，统计30天内请假次数
        // 模拟近期请假记录
        context.put("recentLeaveCount30Days", 1);  // 近30天请假次数

        log.debug("组装请假上下文数据完成，审批ID：{}", approvalId);
        return context;
    }

    /**
     * 组装加班类型的上下文数据
     */
    @Override
    public Object buildOvertimeContext(String approvalId, OvertimeRequestEntity request) {
        Map<String, Object> context = new HashMap<>();
        context.put("approvalId", approvalId);
        context.put("overtimeDate", request.getOvertimeDate());
        context.put("startTime", request.getStartTime());
        context.put("endTime", request.getEndTime());
        context.put("hours", request.getHours());
        context.put("reason", request.getReason());
        context.put("overtimeType", request.getOvertimeType());

        // TODO: 对接 AttendanceRecord 聚合本月/本季度加班时长
        // 模拟本月加班统计数据
        context.put("monthlyOvertimeHours", 12.5);       // 本月已加班小时数
        context.put("quarterlyOvertimeHours", 35.0);      // 本季度已加班小时数
        context.put("monthlyOvertimeLimit", 36.0);        // 月度加班上限（36小时）
        context.put("monthlyWorkdayOvertimeCount", 2);    // 本月工作日加班次数

        // TODO: 计算部门平均加班时长
        context.put("departmentAvgOvertimeHours", 15.0);  // 部门月均加班时长

        log.debug("组装加班上下文数据完成，审批ID：{}", approvalId);
        return context;
    }

    /**
     * 组装换班类型的上下文数据
     */
    @Override
    public Object buildSwapContext(String approvalId, SwapRequestDetailEntity request) {
        Map<String, Object> context = new HashMap<>();
        context.put("approvalId", approvalId);
        context.put("originalShiftDate", request.getOriginalDate());
        context.put("targetShiftDate", request.getTargetDate());
        context.put("targetEmployeeId", request.getTargetEmployeeId());
        context.put("swapReason", request.getReason());

        // TODO: 对接 SwapRequest + ScheduleEntry 获取班次详情
        // 模拟换班双方信息
        context.put("originalShiftName", "早班 (08:00-16:00)");  // 原班次名称
        context.put("targetShiftName", "晚班 (16:00-24:00)");    // 目标班次名称
        context.put("targetEmployeeName", "待确认");              // 换班对方姓名

        // TODO: 检测排班冲突
        context.put("hasConflict", false);                        // 是否存在排班冲突
        context.put("isTargetConfirmed", false);                  // 对方是否已确认

        // TODO: 统计本月换班次数
        context.put("monthlySwapCount", 1);                       // 本月换班次数

        log.debug("组装换班上下文数据完成，审批ID：{}", approvalId);
        return context;
    }

    /**
     * 组装出差类型的上下文数据
     */
    @Override
    public Object buildTravelContext(String approvalId, TravelRequestEntity request) {
        Map<String, Object> context = new HashMap<>();
        context.put("approvalId", approvalId);
        context.put("destination", request.getDestination());
        context.put("startDate", request.getStartDate());
        context.put("endDate", request.getEndDate());
        context.put("days", request.getDays());
        context.put("purpose", request.getTravelPurpose());
        context.put("estimatedCost", request.getEstimatedBudget());

        // TODO: 对接 Budget 获取差旅政策限额
        // 模拟差旅预算信息
        context.put("travelPolicyLimit", 5000.0);         // 差旅单次限额
        context.put("annualTravelBudget", 30000.0);        // 年度差旅总预算
        context.put("usedAnnualTravelBudget", 12000.0);    // 已使用年度差旅预算

        // TODO: 对接任务系统获取关联任务
        context.put("relatedTaskId", request.getRelatedTaskId());  // 关联任务ID
        context.put("hasRelatedTask", request.getRelatedTaskId() != null);

        // TODO: 统计本年度出差次数
        context.put("yearlyTravelCount", 3);               // 本年度出差次数

        log.debug("组装出差上下文数据完成，审批ID：{}", approvalId);
        return context;
    }

    /**
     * 组装报销类型的上下文数据
     */
    @Override
    public Object buildReimbursementContext(String approvalId, ReimbursementRequestEntity request) {
        Map<String, Object> context = new HashMap<>();
        context.put("approvalId", approvalId);
        context.put("amount", request.getTotalAmount());
        context.put("category", request.getReimbursementCategory());
        // 注意：ReimbursementRequestEntity 无 invoiceDate 字段，该字段已移除
        context.put("invoiceNumber", request.getInvoiceNos());
        context.put("description", request.getDescription());
        context.put("relatedTravelApprovalId", request.getRelatedTravelId());

        // TODO: 对接 InvoiceReimbursement + Budget 聚合报销数据
        // 模拟本月/年度报销统计
        context.put("monthlyReimbursedAmount", 4500.0);     // 本月已报销金额
        context.put("monthlyBudget", 8000.0);               // 月度报销预算
        context.put("yearlyReimbursedAmount", 38000.0);     // 年度已报销金额
        context.put("yearlyBudget", 100000.0);              // 年度报销预算

        // TODO: 关联出差的预算检查
        if (request.getRelatedTravelId() != null) {
            context.put("travelBudgetUsed", 2000.0);        // 该出差已用预算
            context.put("travelBudgetLimit", 5000.0);       // 该出差预算上限
        }

        log.debug("组装报销上下文数据完成，审批ID：{}", approvalId);
        return context;
    }

    /**
     * 组装领用类型的上下文数据
     */
    @Override
    public Object buildRequisitionContext(String approvalId, RequisitionRequestEntity request) {
        Map<String, Object> context = new HashMap<>();
        context.put("approvalId", approvalId);
        // 注意：RequisitionRequestEntity 无 itemId 字段（主键为 id），该字段已移除
        context.put("itemName", request.getItemName());
        context.put("quantity", request.getQuantity());
        context.put("unit", request.getUnit());
        context.put("purpose", request.getPurpose());
        context.put("isUrgent", request.getUrgencyLevel());
        context.put("supplierId", request.getSupplierPreference());

        // TODO: 对接 Inventory 获取库存信息
        // 模拟库存数据
        context.put("currentStock", 50);                    // 当前库存数量
        context.put("safetyStock", 20);                     // 安全库存线
        context.put("monthlyAvgUsage", 25);                 // 月均用量

        // TODO: 对接 Suppliers 获取供应商信息
        context.put("supplierName", "默认供应商");          // 供应商名称
        context.put("isPreferredSupplier", true);           // 是否首选供应商

        // TODO: 对接 Budget 获取部门领用预算
        context.put("departmentMonthlyBudget", 10000.0);    // 部门月度领用预算
        context.put("departmentMonthlyUsed", 7500.0);      // 部门本月已用预算

        log.debug("组装领用上下文数据完成，审批ID：{}", approvalId);
        return context;
    }
}
