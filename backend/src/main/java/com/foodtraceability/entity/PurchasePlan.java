package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购计划主表实体
 * 对应数据库表: purchase_plan
 *
 * <p>状态编码（status 字段）：
 * <ul>
 *   <li>0 = 草稿 (draft)</li>
 *   <li>1 = 待审批 (pending)</li>
 *   <li>2 = 已审批 (approved)</li>
 *   <li>3 = 执行中 (executing)</li>
 *   <li>4 = 已完成 (completed)</li>
 *   <li>5 = 已拒绝 (rejected)</li>
 * </ul>
 * </p>
 *
 * <p>金额单位：total_amount 字段以"分"为单位（Long），前端展示时转换为"元"。</p>
 */
@TableName("purchase_plan")
@Schema(description = "采购计划主表")
public class PurchasePlan {

    /** 计划ID（主键，自增） */
    @TableId(value = "plan_id", type = IdType.AUTO)
    @Schema(description = "计划ID")
    private Long planId;

    /** 计划编号（业务唯一，格式 PL+yyyyMMdd+3位序号） */
    @TableField("plan_no")
    @Schema(description = "计划编号")
    private String planNo;

    /** 计划日期 */
    @TableField("plan_date")
    @Schema(description = "计划日期")
    private LocalDate planDate;

    /** 部门ID（关联 departments） */
    @TableField("department_id")
    @Schema(description = "部门ID")
    private Long departmentId;

    /** 部门名称（冗余存储，避免 JOIN） */
    @TableField("department_name")
    @Schema(description = "部门名称")
    private String departmentName;

    /** 总金额（单位：分） */
    @TableField("total_amount")
    @Schema(description = "总金额（分）")
    private Long totalAmount;

    /** 物料项数 */
    @TableField("item_count")
    @Schema(description = "物料项数")
    private Integer itemCount;

    /** 创建人ID */
    @TableField("creator_id")
    @Schema(description = "创建人ID")
    private Long creatorId;

    /** 创建人名称（冗余） */
    @TableField("creator_name")
    @Schema(description = "创建人名称")
    private String creatorName;

    /** 状态：0草稿 1待审批 2已审批 3执行中 4已完成 5已拒绝 */
    @TableField("status")
    @Schema(description = "状态")
    private Integer status;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /** 审批人 */
    @TableField("approve_by")
    @Schema(description = "审批人")
    private String approveBy;

    /** 审批时间 */
    @TableField("approve_time")
    @Schema(description = "审批时间")
    private LocalDateTime approveTime;

    /** 拒绝原因 */
    @TableField("reject_reason")
    @Schema(description = "拒绝原因")
    private String rejectReason;

    /** 创建时间 */
    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除")
    private Integer deleted;

    // ==================== Getter & Setter ====================

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getPlanNo() {
        return planNo;
    }

    public void setPlanNo(String planNo) {
        this.planNo = planNo;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getApproveBy() {
        return approveBy;
    }

    public void setApproveBy(String approveBy) {
        this.approveBy = approveBy;
    }

    public LocalDateTime getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(LocalDateTime approveTime) {
        this.approveTime = approveTime;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
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
