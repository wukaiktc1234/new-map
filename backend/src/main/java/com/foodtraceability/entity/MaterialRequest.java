package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 物资需求提报主表实体
 * 对应数据库表: material_request
 *
 * <p>状态编码（status 字段）：
 * <ul>
 *   <li>0 = 草稿 (draft)</li>
 *   <li>1 = 待审核 (pending)</li>
 *   <li>2 = 已审核 (approved)</li>
 *   <li>3 = 已驳回 (rejected)</li>
 *   <li>4 = 已转采购申请 (converted)</li>
 * </ul>
 * </p>
 *
 * <p>金额单位：total_amount 字段以"分"为单位（Long），前端展示时转换为"元"。</p>
 */
@TableName("material_request")
@Schema(description = "物资需求提报主表")
public class MaterialRequest {

    /** 提报ID（主键，自增） */
    @TableId(value = "request_id", type = IdType.AUTO)
    @Schema(description = "提报ID")
    private Long requestId;

    /** 提报单号（业务唯一，格式 MR+yyyyMMdd+4位序号） */
    @TableField("request_no")
    @Schema(description = "提报单号")
    private String requestNo;

    /** 需求标题 */
    @TableField("title")
    @Schema(description = "需求标题")
    private String title;

    /** 提报门店名称 */
    @TableField("store_name")
    @Schema(description = "提报门店")
    private String storeName;

    /** 申请人ID */
    @TableField("applicant_id")
    @Schema(description = "申请人ID")
    private Long applicantId;

    /** 申请人姓名（冗余存储） */
    @TableField("applicant_name")
    @Schema(description = "申请人姓名")
    private String applicantName;

    /** 期望到货日期 */
    @TableField("expected_date")
    @Schema(description = "期望到货日期")
    private LocalDate expectedDate;

    /** 状态：0草稿 1待审核 2已审核 3已驳回 4已转采购申请 */
    @TableField("status")
    @Schema(description = "状态")
    private Integer status;

    /** 总金额（单位：分） */
    @TableField("total_amount")
    @Schema(description = "总金额（分）")
    private Long totalAmount;

    /** 转换后的采购申请单号（未转换为空） */
    @TableField("converted_request_no")
    @Schema(description = "转换后的采购申请单号")
    private String convertedRequestNo;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

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

    /** 明细列表（不映射到数据库字段） */
    @TableField(exist = false)
    @Schema(description = "明细列表")
    private List<MaterialRequestItem> items;

    // ==================== Getter & Setter ====================

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Long getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(Long applicantId) {
        this.applicantId = applicantId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getConvertedRequestNo() {
        return convertedRequestNo;
    }

    public void setConvertedRequestNo(String convertedRequestNo) {
        this.convertedRequestNo = convertedRequestNo;
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

    public List<MaterialRequestItem> getItems() {
        return items;
    }

    public void setItems(List<MaterialRequestItem> items) {
        this.items = items;
    }
}
