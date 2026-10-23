package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 收货确认单打印日志实体
 * 每次打印 A4 登记一条记录，用于防打印泛滥与查验追溯
 */
@TableName("receipt_print_log")
@Schema(description = "收货确认单打印日志")
public class ReceiptPrintLog {

    @TableId(value = "print_id", type = IdType.AUTO)
    @Schema(description = "打印记录ID")
    private Long printId;

    @TableField("confirmation_id")
    @Schema(description = "关联收货确认单ID")
    private Long confirmationId;

    @TableField("operator_user_id")
    @Schema(description = "操作人用户ID")
    private Long operatorUserId;

    @TableField("operator_name")
    @Schema(description = "操作人姓名")
    private String operatorName;

    @TableField("print_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "打印时间")
    private LocalDateTime printTime;

    @TableField("source_ip")
    @Schema(description = "来源IP")
    private String sourceIp;

    public Long getPrintId() {
        return printId;
    }

    public void setPrintId(Long printId) {
        this.printId = printId;
    }

    public Long getConfirmationId() {
        return confirmationId;
    }

    public void setConfirmationId(Long confirmationId) {
        this.confirmationId = confirmationId;
    }

    public Long getOperatorUserId() {
        return operatorUserId;
    }

    public void setOperatorUserId(Long operatorUserId) {
        this.operatorUserId = operatorUserId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public LocalDateTime getPrintTime() {
        return printTime;
    }

    public void setPrintTime(LocalDateTime printTime) {
        this.printTime = printTime;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }
}
