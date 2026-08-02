package com.foodtraceability.dataservice.approval;

import com.foodtraceability.entity.approval.LeaveRequestEntity;
import com.foodtraceability.entity.approval.OvertimeRequestEntity;
import com.foodtraceability.entity.approval.SwapRequestDetailEntity;
import com.foodtraceability.entity.approval.TravelRequestEntity;
import com.foodtraceability.entity.approval.ReimbursementRequestEntity;
import com.foodtraceability.entity.approval.RequisitionRequestEntity;

/**
 * 员工审批数据服务接口
 * 负责组装各类型审批的上下文数据（contextData），聚合多个数据源信息
 */
public interface EmployeeApprovalDataService {

    /**
     * 组装请假类型的上下文数据
     * 包含：假期余额、历史请假记录、同部门请假情况等
     * @param approvalId 审批ID
     * @param request 请假申请实体
     * @return 上下文数据Map
     */
    Object buildLeaveContext(String approvalId, LeaveRequestEntity request);

    /**
     * 组装加班类型的上下文数据
     * 包含：本月/本季度加班时长、部门平均加班、考勤记录等
     * @param approvalId 审批ID
     * @param request 加班申请实体
     * @return 上下文数据Map
     */
    Object buildOvertimeContext(String approvalId, OvertimeRequestEntity request);

    /**
     * 组装换班类型的上下文数据
     * 包含：换班双方信息、班次详情、排班冲突检测等
     * @param approvalId 审批ID
     * @param request 换班申请实体
     * @return 上下文数据Map
     */
    Object buildSwapContext(String approvalId, SwapRequestDetailEntity request);

    /**
     * 组装出差类型的上下文数据
     * 包含：差旅政策限额、预算信息、关联任务等
     * @param approvalId 审批ID
     * @param request 出差申请实体
     * @return 上下文数据Map
     */
    Object buildTravelContext(String approvalId, TravelRequestEntity request);

    /**
     * 组装报销类型的上下文数据
     * 包含：本月/年度报销统计、预算使用情况、发票信息等
     * @param approvalId 审批ID
     * @param request 报销申请实体
     * @return 上下文数据Map
     */
    Object buildReimbursementContext(String approvalId, ReimbursementRequestEntity request);

    /**
     * 组装领用类型的上下文数据
     * 包含：库存信息、供应商信息、预算使用情况等
     * @param approvalId 审批ID
     * @param request 领用申请实体
     * @return 上下文数据Map
     */
    Object buildRequisitionContext(String approvalId, RequisitionRequestEntity request);
}
