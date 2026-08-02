package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * POS登录请求DTO
 * 用于收银端登录认证的请求参数
 */
@Schema(description = "POS登录请求DTO")
public class PosLoginDTO {

    @NotBlank(message = "员工号不能为空")
    @Schema(description = "员工号", example = "EMP001")
    private String employeeId;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "终端ID", example = "POS-001")
    private String terminalId;

    public PosLoginDTO() {
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }
}
