package com.foodtraceability.entity.approval;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审批流程定义实体类
 * 对应数据库表 approval_workflows
 * 存储按 业务类型 + 权限模板 配置的审批流程定义
 *
 * <p>nodes / conditions 字段以 JSON 字符串形式存储，由 Service 层负责序列化/反序列化
 */
@TableName("approval_workflows")
@Schema(description = "审批流程定义实体")
public class ApprovalWorkflow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 流程ID */
    @TableId(value = "workflow_id", type = IdType.ASSIGN_ID)
    @Schema(description = "流程ID")
    private String workflowId;

    /** 流程名称 */
    @TableField("workflow_name")
    @Schema(description = "流程名称")
    private String workflowName;

    /** 业务类型 */
    @TableField("business_type")
    @Schema(description = "业务类型")
    private String businessType;

    /** 权限模板编码 */
    @TableField("template_code")
    @Schema(description = "权限模板编码")
    private String templateCode;

    /** 是否启用 */
    @TableField("enabled")
    @Schema(description = "是否启用")
    private Boolean enabled;

    /** 审批节点列表(JSON字符串) */
    @TableField("nodes")
    @Schema(description = "审批节点列表(JSON字符串)")
    private String nodes;

    /** 条件分支列表(JSON字符串) */
    @TableField("conditions")
    @Schema(description = "条件分支列表(JSON字符串)")
    private String conditions;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记: 0-未删除 1-已删除")
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ==================== 业务类型常量 ====================

    /** 采购申请 */
    public static final String BUSINESS_TYPE_PURCHASE_REQUEST = "purchase_request";
    /** 采购订单 */
    public static final String BUSINESS_TYPE_PURCHASE_ORDER = "purchase_order";
    /** 采购计划 */
    public static final String BUSINESS_TYPE_PURCHASE_PLAN = "purchase_plan";
    /** 采购合同 */
    public static final String BUSINESS_TYPE_PURCHASE_CONTRACT = "purchase_contract";
    /** 采购结算 */
    public static final String BUSINESS_TYPE_PURCHASE_SETTLEMENT = "purchase_settlement";
    /** 合同签署 */
    public static final String BUSINESS_TYPE_CONTRACT = "contract";
    /** 付款审批 */
    public static final String BUSINESS_TYPE_PAYMENT = "payment";
    /** 费用报销 */
    public static final String BUSINESS_TYPE_EXPENSE = "expense";
    /** 物资需求审核 */
    public static final String BUSINESS_TYPE_MATERIAL_REQUEST = "material_request";
    /** 证件续期 */
    public static final String BUSINESS_TYPE_CERT_RENEWAL = "cert_renewal";
    /** 证件费用报销 */
    public static final String BUSINESS_TYPE_CERT_EXPENSE_REIMBURSE = "cert_expense_reimburse";
    /** 员工入职 */
    public static final String BUSINESS_TYPE_EMPLOYEE_ONBOARDING = "employee_onboarding";
    /** 员工请假 */
    public static final String BUSINESS_TYPE_EMPLOYEE_LEAVE = "employee_leave";
    /** 财务报销 */
    public static final String BUSINESS_TYPE_FINANCE_EXPENSE = "finance_expense";

    // ==================== 权限模板编码常量 ====================

    /** 大型连锁 */
    public static final String TEMPLATE_CODE_ENTERPRISE_CHAIN = "enterprise-chain";
    /** 标准连锁 */
    public static final String TEMPLATE_CODE_STANDARD_CHAIN = "standard-chain";
    /** 集中式单店 */
    public static final String TEMPLATE_CODE_CENTRALIZED_SINGLE = "centralized-single";
    /** 完全自定义 */
    public static final String TEMPLATE_CODE_CUSTOM = "custom";

    // ==================== Getter & Setter ====================

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
