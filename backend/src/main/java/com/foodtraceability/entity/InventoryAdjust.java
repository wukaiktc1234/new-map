package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存调整单实体类
 * 用于管理库存盘点差异、温度损耗、称重差异等调整操作
 *
 * 调整类型：
 * - gain: 盘盈调整
 * - loss: 盘亏调整
 * - temp_loss: 温度损耗
 * - weight_diff: 称重差异
 * - other: 其他调整
 *
 * 状态流转：
 * - pending(待审批) → approved(已审批) → completed(已完成)
 * - pending(待审批) → rejected(已驳回)
 */
@TableName("inventory_adjust")
public class InventoryAdjust {

    /**
     * 调整单ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long adjustId;

    /**
     * 调整单号（业务编号，唯一）
     */
    @TableField("adjust_code")
    private String adjustCode;

    /**
     * 调整类型（gain/loss/temp_loss/weight_diff/other）
     */
    @TableField("adjust_type")
    private String adjustType;

    /**
     * 仓库ID
     */
    @TableField("warehouse_id")
    private String warehouseId;

    /**
     * 仓库名称（冗余字段，便于查询展示）
     */
    @TableField("warehouse_name")
    private String warehouseName;

    /**
     * 关联盘点单号
     */
    @TableField("reference_check_code")
    private String referenceCheckCode;

    /**
     * 调整原因分类
     */
    @TableField("adjust_reason")
    private String adjustReason;

    /**
     * 关联单号（采购单/调拨单等）
     */
    @TableField("reference_no")
    private String referenceNo;

    /**
     * 调整总数量
     */
    @TableField("total_adjust_quantity")
    private BigDecimal totalAdjustQuantity;

    /**
     * 调整总金额（分）
     */
    @TableField("total_adjust_amount")
    private Long totalAdjustAmount;

    /**
     * 调整状态（pending/approved/completed/rejected）
     */
    @TableField("status")
    private String status;

    /**
     * 参与部门（JSON数组字符串）
     */
    @TableField("participating_depts")
    private String participatingDepts;

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

    public Long getAdjustId() {
        return adjustId;
    }

    public void setAdjustId(Long adjustId) {
        this.adjustId = adjustId;
    }

    public String getAdjustCode() {
        return adjustCode;
    }

    public void setAdjustCode(String adjustCode) {
        this.adjustCode = adjustCode;
    }

    public String getAdjustType() {
        return adjustType;
    }

    public void setAdjustType(String adjustType) {
        this.adjustType = adjustType;
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

    public String getReferenceCheckCode() {
        return referenceCheckCode;
    }

    public void setReferenceCheckCode(String referenceCheckCode) {
        this.referenceCheckCode = referenceCheckCode;
    }

    public String getAdjustReason() {
        return adjustReason;
    }

    public void setAdjustReason(String adjustReason) {
        this.adjustReason = adjustReason;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public BigDecimal getTotalAdjustQuantity() {
        return totalAdjustQuantity;
    }

    public void setTotalAdjustQuantity(BigDecimal totalAdjustQuantity) {
        this.totalAdjustQuantity = totalAdjustQuantity;
    }

    public Long getTotalAdjustAmount() {
        return totalAdjustAmount;
    }

    public void setTotalAdjustAmount(Long totalAdjustAmount) {
        this.totalAdjustAmount = totalAdjustAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getParticipatingDepts() {
        return participatingDepts;
    }

    public void setParticipatingDepts(String participatingDepts) {
        this.participatingDepts = participatingDepts;
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
