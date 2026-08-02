package com.foodtraceability.service.approval;

import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.approval.ApprovalActionDTO;
import com.foodtraceability.dto.approval.ApprovalAuditLogVO;
import com.foodtraceability.dto.approval.ApprovalCurrentNodeVO;
import com.foodtraceability.dto.approval.ApprovalWorkflowCreateDTO;
import com.foodtraceability.dto.approval.ApprovalWorkflowQueryDTO;
import com.foodtraceability.dto.approval.ApprovalWorkflowVO;

import java.util.List;

/**
 * 审批工作流服务接口
 * 提供审批流程的 CRUD、启用/禁用、审批操作（提交/通过/驳回/撤回）、审批记录查询等功能
 *
 * <p>设计说明：
 * <ul>
 *   <li>nodes / conditions 字段在 Entity 中以 JSON 字符串存储，VO/DTO 中以结构化对象数组传输</li>
 *   <li>Service 层负责 JSON 序列化/反序列化</li>
 *   <li>审批操作（提交/通过/驳回/撤回）均会写入 approval_audit_logs 表</li>
 * </ul>
 */
public interface ApprovalWorkflowService {

    /**
     * 分页查询审批流程列表
     * 支持按业务类型、权限模板、启用状态、关键词筛选
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<ApprovalWorkflowVO> getWorkflowList(ApprovalWorkflowQueryDTO queryDTO);

    /**
     * 获取审批流程详情
     * @param workflowId 流程ID
     * @return 流程详情（不存在返回 null）
     */
    ApprovalWorkflowVO getWorkflowById(String workflowId);

    /**
     * 创建审批流程
     * @param createDTO 创建数据
     * @return 创建后的流程
     */
    ApprovalWorkflowVO createWorkflow(ApprovalWorkflowCreateDTO createDTO);

    /**
     * 更新审批流程
     * @param workflowId 流程ID
     * @param updateDTO 更新数据
     * @return 更新后的流程
     */
    ApprovalWorkflowVO updateWorkflow(String workflowId, ApprovalWorkflowCreateDTO updateDTO);

    /**
     * 删除审批流程（逻辑删除）
     * @param workflowId 流程ID
     */
    void deleteWorkflow(String workflowId);

    /**
     * 启用/禁用审批流程
     * @param workflowId 流程ID
     * @param enabled 是否启用
     */
    void toggleEnabled(String workflowId, Boolean enabled);

    /**
     * 获取默认审批流程（按业务类型 + 权限模板）
     * @param businessType 业务类型
     * @param templateCode 权限模板编码
     * @return 流程详情（不存在返回 null）
     */
    ApprovalWorkflowVO getDefaultWorkflow(String businessType, String templateCode);

    /**
     * 获取当前审批节点信息
     * 根据业务ID和业务类型查询最近的审批记录，推断当前审批节点
     * @param businessId 业务ID
     * @param businessType 业务类型
     * @return 当前节点信息（无记录返回 null）
     */
    ApprovalCurrentNodeVO getCurrentNode(String businessId, String businessType);

    /**
     * 提交审批
     * @param businessType 业务类型
     * @param businessId 业务ID
     */
    void submitApproval(String businessType, String businessId);

    /**
     * 审批通过
     * @param actionDTO 审批操作参数（businessId/businessType/comment）
     */
    void approve(ApprovalActionDTO actionDTO);

    /**
     * 审批驳回
     * @param actionDTO 审批操作参数（businessId/businessType/comment）
     */
    void reject(ApprovalActionDTO actionDTO);

    /**
     * 撤回审批
     * @param businessId 业务ID
     */
    void withdraw(String businessId);

    /**
     * 获取审批记录列表
     * @param businessId 业务ID
     * @return 审批记录列表（按操作时间倒序）
     */
    List<ApprovalAuditLogVO> getApprovalLogs(String businessId);
}
