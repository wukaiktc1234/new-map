package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 采购溯源链路节点视图对象
 * 用于展示采购流程中单个环节的关键信息
 */
@Schema(description = "采购溯源链路节点")
public class ProcurementTraceNodeVO {

    /**
     * 节点类型：request/contract/order/arrival/stockin/settlement/inventory/asset
     */
    @Schema(description = "节点类型")
    private String nodeType;

    /**
     * 节点编号
     */
    @Schema(description = "节点编号")
    private String nodeCode;

    /**
     * 节点ID
     */
    @Schema(description = "节点ID")
    private String nodeId;

    /**
     * 节点标题
     */
    @Schema(description = "节点标题")
    private String title;

    /**
     * 节点状态
     */
    @Schema(description = "节点状态")
    private String status;

    /**
     * 状态描述
     */
    @Schema(description = "状态描述")
    private String statusText;

    /**
     * 操作时间
     */
    @Schema(description = "操作时间")
    private LocalDateTime operateTime;

    /**
     * 操作人员
     */
    @Schema(description = "操作人员")
    private String operatorName;

    /**
     * 节点备注
     */
    @Schema(description = "节点备注")
    private String remark;

    public ProcurementTraceNodeVO() {
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getNodeCode() {
        return nodeCode;
    }

    public void setNodeCode(String nodeCode) {
        this.nodeCode = nodeCode;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public LocalDateTime getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(LocalDateTime operateTime) {
        this.operateTime = operateTime;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
