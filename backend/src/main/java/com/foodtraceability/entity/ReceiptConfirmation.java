package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 收货确认单主表实体类
 * 用于管理采购商品的实物确认信息
 */
@TableName("receipt_confirmations")
@Schema(description = "收货确认单主表实体")
public class ReceiptConfirmation {

    /**
     * 收货确认单主键ID（自增）
     */
    @TableId(value = "confirmation_id", type = IdType.AUTO)
    @Schema(description = "收货确认单主键ID", example = "1")
    private Long confirmationId;

    /**
     * 收货确认单编号
     */
    @TableField("confirmation_code")
    @Schema(description = "收货确认单编号", example = "RC20260425001")
    private String confirmationCode;

    /**
     * 防伪验证码（16位随机，不可预测，唯一索引）
     */
    @TableField("verify_code")
    @Schema(description = "防伪验证码（16位随机）", example = "RMJ6FNHJFQ4YUU7E")
    private String verifyCode;

    /**
     * 关联到货单ID
     */
    @TableField("arrival_id")
    @Schema(description = "关联到货单ID", example = "1")
    private Long arrivalId;

    /**
     * 收货来源：arrival-到货确认 / direct-无单直收
     */
    @TableField("receipt_source")
    @Schema(description = "收货来源（arrival-到货确认, direct-无单直收）", example = "arrival")
    private String receiptSource;

    /**
     * 关联采购订单ID
     */
    @TableField("order_id")
    @Schema(description = "关联采购订单ID", example = "1")
    private Long orderId;

    /**
     * 收货方类型：STORE 门店 / WAREHOUSE 仓库
     */
    @TableField("receiver_type")
    @Schema(description = "收货方类型（STORE-门店, WAREHOUSE-仓库）")
    private String receiverType;

    /**
     * 收货门店ID
     */
    @TableField("store_id")
    @Schema(description = "收货门店ID")
    private String storeId;

    /**
     * 收货仓库ID
     */
    @TableField("warehouse_id")
    @Schema(description = "收货仓库ID", example = "1")
    private Long warehouseId;

    /**
     * 确认人ID
     */
    @TableField("confirm_user_id")
    @Schema(description = "确认人ID", example = "1")
    private Long confirmUserId;

    /**
     * 确认时间
     */
    @TableField("confirm_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    /**
     * 总数量
     */
    @TableField("total_quantity")
    @Schema(description = "总数量", example = "100.000")
    private BigDecimal totalQuantity;

    /**
     * 总金额（单位：分）
     */
    @TableField("total_amount")
    @Schema(description = "总金额（分）", example = "30000")
    private Long totalAmount;

    /**
     * 质检结果（1合格 2不合格）
     */
    @TableField("quality_check_result")
    @Schema(description = "质检结果（1-合格, 2-不合格）", example = "1")
    private Integer qualityCheckResult;

    /**
     * 质检备注
     */
    @TableField("quality_remark")
    @Schema(description = "质检备注")
    private String qualityRemark;

    /**
     * 状态（1-CONFIRMED 2-REJECTED）
     */
    @TableField("status")
    @Schema(description = "状态（1-已确认, 2-拒收）", example = "1")
    private Integer status;

    /**
     * 备注
     */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 打印次数（每次打印 A4 递增，用于防打印泛滥追溯）
     */
    @TableField("print_count")
    @Schema(description = "打印次数")
    private Integer printCount;

    /**
     * 最近一次打印时间
     */
    @TableField("last_print_time")
    @Schema(description = "最近一次打印时间")
    private LocalDateTime lastPrintTime;

    /**
     * 逻辑删除标记（0未删除 1已删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    /**
     * 收货确认明细列表（非数据库字段）
     */
    @TableField(exist = false)
    @Schema(description = "收货确认明细列表")
    private List<ReceiptConfirmationItem> items;

    /** 供应商名称（非数据库字段，用于展示/打印） */
    @TableField(exist = false)
    @Schema(description = "供应商名称")
    private String supplierName;

    /** 货物条形码（商品码/外箱条码） */
    @TableField("barcode")
    @Schema(description = "货物条形码")
    private String barcode;

    /** 到货单编号（非数据库字段，用于展示/打印） */
    @TableField(exist = false)
    @Schema(description = "到货单编号")
    private String arrivalCode;

    // ==================== Getter & Setter ====================

    public Long getConfirmationId() {
        return confirmationId;
    }

    public void setConfirmationId(Long confirmationId) {
        this.confirmationId = confirmationId;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public Long getArrivalId() {
        return arrivalId;
    }

    public void setArrivalId(Long arrivalId) {
        this.arrivalId = arrivalId;
    }

    public String getReceiptSource() {
        return receiptSource;
    }

    public void setReceiptSource(String receiptSource) {
        this.receiptSource = receiptSource;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getReceiverType() {
        return receiverType;
    }

    public void setReceiverType(String receiverType) {
        this.receiverType = receiverType;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getConfirmUserId() {
        return confirmUserId;
    }

    public void setConfirmUserId(Long confirmUserId) {
        this.confirmUserId = confirmUserId;
    }

    public LocalDateTime getConfirmTime() {
        return confirmTime;
    }

    public void setConfirmTime(LocalDateTime confirmTime) {
        this.confirmTime = confirmTime;
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

    public Integer getQualityCheckResult() {
        return qualityCheckResult;
    }

    public void setQualityCheckResult(Integer qualityCheckResult) {
        this.qualityCheckResult = qualityCheckResult;
    }

    public String getQualityRemark() {
        return qualityRemark;
    }

    public void setQualityRemark(String qualityRemark) {
        this.qualityRemark = qualityRemark;
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

    public Integer getPrintCount() {
        return printCount;
    }

    public void setPrintCount(Integer printCount) {
        this.printCount = printCount;
    }

    public LocalDateTime getLastPrintTime() {
        return lastPrintTime;
    }

    public void setLastPrintTime(LocalDateTime lastPrintTime) {
        this.lastPrintTime = lastPrintTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public List<ReceiptConfirmationItem> getItems() {
        return items;
    }

    public void setItems(List<ReceiptConfirmationItem> items) {
        this.items = items;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getArrivalCode() {
        return arrivalCode;
    }

    public void setArrivalCode(String arrivalCode) {
        this.arrivalCode = arrivalCode;
    }
}
