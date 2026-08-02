package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * POS开始班次请求DTO
 * 用于收银端开始新班次的请求参数
 */
@Schema(description = "POS开始班次请求DTO")
public class PosStartShiftDTO {

    @NotBlank(message = "终端ID不能为空")
    @Schema(description = "终端ID", example = "POS-001")
    private String terminalId;

    @NotBlank(message = "员工ID不能为空")
    @Schema(description = "员工ID", example = "1001")
    private String employeeId;

    @Schema(description = "员工姓名", example = "张三")
    private String employeeName;

    @Schema(description = "班次类型", example = "morning")
    private String shiftType;

    @Schema(description = "开班现金", example = "500.00")
    private BigDecimal openingCash;

    public PosStartShiftDTO() {
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public BigDecimal getOpeningCash() {
        return openingCash;
    }

    public void setOpeningCash(BigDecimal openingCash) {
        this.openingCash = openingCash;
    }
}
