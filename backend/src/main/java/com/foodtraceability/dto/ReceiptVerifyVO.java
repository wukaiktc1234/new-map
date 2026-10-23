package com.foodtraceability.dto;

import com.foodtraceability.entity.ReceiptPrintLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 防伪码查验结果
 * 输入防伪码查询：单据基本信息 + 完整打印历史
 */
@Schema(description = "防伪码查验结果")
public class ReceiptVerifyVO {

    @Schema(description = "查验是否有效")
    private boolean valid;

    @Schema(description = "无效原因（valid=false 时）")
    private String message;

    @Schema(description = "确认单编号")
    private String confirmationCode;

    @Schema(description = "防伪码")
    private String verifyCode;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "确认金额（分）")
    private Long totalAmount;

    @Schema(description = "实收数量")
    private BigDecimal totalQuantity;

    @Schema(description = "打印次数")
    private Integer printCount;

    @Schema(description = "最近打印时间")
    private LocalDateTime lastPrintTime;

    @Schema(description = "完整打印历史（按时间正序）")
    private List<ReceiptPrintLog> printLogs;

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public LocalDateTime getConfirmTime() {
        return confirmTime;
    }

    public void setConfirmTime(LocalDateTime confirmTime) {
        this.confirmTime = confirmTime;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
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

    public List<ReceiptPrintLog> getPrintLogs() {
        return printLogs;
    }

    public void setPrintLogs(List<ReceiptPrintLog> printLogs) {
        this.printLogs = printLogs;
    }
}
