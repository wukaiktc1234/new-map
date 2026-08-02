package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约记录实体类
 * 管理桌台预约信息
 */
@TableName("table_reservations")
@Schema(description = "预约记录实体")
public class TableReservationNew {

    /** 预约ID，主键自增 */
    @TableId(value = "reservation_id", type = IdType.AUTO)
    @Schema(description = "预约ID", example = "1")
    private Long reservationId;

    /** 预约编码，唯一 */
    @TableField("reservation_code")
    @Schema(description = "预约编码", example = "RES20260425001")
    private String reservationCode;

    /** 顾客姓名 */
    @TableField("customer_name")
    @Schema(description = "顾客姓名", example = "张三")
    private String customerName;

    /** 顾客电话 */
    @TableField("customer_phone")
    @Schema(description = "顾客电话", example = "13800138000")
    private String customerPhone;

    /** 预约桌台ID */
    @TableField("table_id")
    @Schema(description = "预约桌台ID", example = "1")
    private Long tableId;

    /** 门店ID */
    @TableField("store_id")
    @Schema(description = "门店ID", example = "1")
    private Long storeId;

    /** 预约日期 */
    @TableField("reservation_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "预约日期")
    private LocalDate reservationDate;

    /** 预约时间 */
    @TableField("reservation_time")
    @JsonFormat(pattern = "HH:mm:ss")
    @Schema(description = "预约时间")
    private LocalTime reservationTime;

    /** 用餐人数 */
    @TableField("people_count")
    @Schema(description = "用餐人数", example = "4")
    private Integer peopleCount;

    /** 定金（分） */
    @TableField("deposit_amount")
    @Schema(description = "定金（分）", example = "5000")
    private Long depositAmount;

    /**
     * 状态：
     * 1待确认 2已确认 3已到店 4已取消 5未到
     */
    @TableField("status")
    @Schema(description = "状态: 1待确认 2已确认 3已到店 4已取消 5未到", example = "1")
    private Integer status;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 确认时间 */
    @TableField("confirm_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    /** 到店时间 */
    @TableField("arrive_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "到店时间")
    private LocalDateTime arriveTime;

    /** 取消时间 */
    @TableField("cancel_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;

    /** 确认操作人ID（审计字段） */
    @TableField("confirm_operator_id")
    @Schema(description = "确认操作人ID")
    private Long confirmOperatorId;

    /** 到店操作人ID（审计字段） */
    @TableField("arrive_operator_id")
    @Schema(description = "到店操作人ID")
    private Long arriveOperatorId;

    /** 取消操作人ID（审计字段） */
    @TableField("cancel_operator_id")
    @Schema(description = "取消操作人ID")
    private Long cancelOperatorId;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 备注/特殊要求 */
    @TableField("remark")
    @Schema(description = "备注/特殊要求", example = "靠窗位置，需要儿童椅")
    private String remark;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    // Getter方法
    public Long getReservationId() { return reservationId; }
    public String getReservationCode() { return reservationCode; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public Long getTableId() { return tableId; }
    public Long getStoreId() { return storeId; }
    public LocalDate getReservationDate() { return reservationDate; }
    public LocalTime getReservationTime() { return reservationTime; }
    public Integer getPeopleCount() { return peopleCount; }
    public Long getDepositAmount() { return depositAmount; }
    public Integer getStatus() { return status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getConfirmTime() { return confirmTime; }
    public LocalDateTime getArriveTime() { return arriveTime; }
    public LocalDateTime getCancelTime() { return cancelTime; }
    public Long getConfirmOperatorId() { return confirmOperatorId; }
    public Long getArriveOperatorId() { return arriveOperatorId; }
    public Long getCancelOperatorId() { return cancelOperatorId; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Integer getDeleted() { return deleted; }
    public String getRemark() { return remark; }

    // Setter方法
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public void setReservationCode(String reservationCode) { this.reservationCode = reservationCode; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public void setTableId(Long tableId) { this.tableId = tableId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public void setReservationDate(LocalDate reservationDate) { this.reservationDate = reservationDate; }
    public void setReservationTime(LocalTime reservationTime) { this.reservationTime = reservationTime; }
    public void setPeopleCount(Integer peopleCount) { this.peopleCount = peopleCount; }
    public void setDepositAmount(Long depositAmount) { this.depositAmount = depositAmount; }
    public void setStatus(Integer status) { this.status = status; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public void setConfirmTime(LocalDateTime confirmTime) { this.confirmTime = confirmTime; }
    public void setArriveTime(LocalDateTime arriveTime) { this.arriveTime = arriveTime; }
    public void setCancelTime(LocalDateTime cancelTime) { this.cancelTime = cancelTime; }
    public void setConfirmOperatorId(Long confirmOperatorId) { this.confirmOperatorId = confirmOperatorId; }
    public void setArriveOperatorId(Long arriveOperatorId) { this.arriveOperatorId = arriveOperatorId; }
    public void setCancelOperatorId(Long cancelOperatorId) { this.cancelOperatorId = cancelOperatorId; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public void setRemark(String remark) { this.remark = remark; }
}
