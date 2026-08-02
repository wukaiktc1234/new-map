package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存出库单实体类
 * 用于管理领料出库、销售出库、退货出库等操作
 *
 * 出库类型：
 * - requisition: 领料出库（后厨领用原材料）
 * - sale: 销售出库（成品/半成品出库到门店）
 * - return: 退货出库（退回供应商）
 * - other: 其他出库
 *
 * 状态流转：
 * - pending(待审批) → approved(已审批) → completed(已出库)
 * - pending(待审批) → rejected(已驳回)
 */
@TableName("inventory_outbound")
public class InventoryOutbound {

    /**
     * 出库单ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long outboundId;

    /**
     * 出库单号（业务编号，唯一）
     */
    @TableField("outbound_code")
    private String outboundCode;

    /**
     * 出库类型（requisition/sale/return/other）
     */
    @TableField("outbound_type")
    private String outboundType;

    /**
     * 出库仓库ID
     */
    @TableField("warehouse_id")
    private String warehouseId;

    /**
     * 出库仓库名称（冗余字段）
     */
    @TableField("warehouse_name")
    private String warehouseName;

    /**
     * 目标门店/部门ID
     */
    @TableField("target_id")
    private String targetId;

    /**
     * 目标门店/部门名称
     */
    @TableField("target_name")
    private String targetName;

    /**
     * 关联单号（采购订单号/调拨单号等）
     */
    @TableField("reference_no")
    private String referenceNo;

    /**
     * 出库日期
     */
    @TableField("outbound_date")
    private LocalDate outboundDate;

    /**
     * 总数量
     */
    @TableField("total_quantity")
    private BigDecimal totalQuantity;

    /**
     * 总金额（分）
     */
    @TableField("total_amount")
    private Long totalAmount;

    /**
     * 出库状态（pending/approved/completed/rejected）
     */
    @TableField("status")
    private String status;

    /**
     * 申请人ID
     */
    @TableField("apply_user_id")
    private String applyUserId;

    /**
     * 申请人名称
     */
    @TableField("apply_user_name")
    private String applyUserName;

    /**
     * 申请时间
     */
    @TableField("apply_time")
    private LocalDateTime applyTime;

    /**
     * 审批人ID
     */
    @TableField("approve_user_id")
    private String approveUserId;

    /**
     * 审批人名称
     */
    @TableField("approve_user_name")
    private String approveUserName;

    /**
     * 审批时间
     */
    @TableField("approve_time")
    private LocalDateTime approveTime;

    /**
     * 完成时间
     */
    @TableField("complete_time")
    private LocalDateTime completeTime;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记（0:未删除 1:已删除）
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public Long getOutboundId() {
        return outboundId;
    }

    public void setOutboundId(Long outboundId) {
        this.outboundId = outboundId;
    }

    public String getOutboundCode() {
        return outboundCode;
    }

    public void setOutboundCode(String outboundCode) {
        this.outboundCode = outboundCode;
    }

    public String getOutboundType() {
        return outboundType;
    }

    public void setOutboundType(String outboundType) {
        this.outboundType = outboundType;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public LocalDate getOutboundDate() {
        return outboundDate;
    }

    public void setOutboundDate(LocalDate outboundDate) {
        this.outboundDate = outboundDate;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApplyUserId() {
        return applyUserId;
    }

    public void setApplyUserId(String applyUserId) {
        this.applyUserId = applyUserId;
    }

    public String getApplyUserName() {
        return applyUserName;
    }

    public void setApplyUserName(String applyUserName) {
        this.applyUserName = applyUserName;
    }

    public LocalDateTime getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(LocalDateTime applyTime) {
        this.applyTime = applyTime;
    }

    public String getApproveUserId() {
        return approveUserId;
    }

    public void setApproveUserId(String approveUserId) {
        this.approveUserId = approveUserId;
    }

    public String getApproveUserName() {
        return approveUserName;
    }

    public void setApproveUserName(String approveUserName) {
        this.approveUserName = approveUserName;
    }

    public LocalDateTime getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(LocalDateTime approveTime) {
        this.approveTime = approveTime;
    }

    public LocalDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(LocalDateTime completeTime) {
        this.completeTime = completeTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
