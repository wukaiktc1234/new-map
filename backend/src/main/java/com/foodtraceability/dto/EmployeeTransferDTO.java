package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 员工人事变动DTO
 * 用于员工调岗、调部门等人事变动
 */
@Schema(description = "员工人事变动DTO")
public class EmployeeTransferDTO {

    @Schema(description = "新部门ID", example = "1")
    private Long newDepartmentId;

    @Schema(description = "新职位ID", example = "1")
    private Long newPositionId;

    @NotNull(message = "变动日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "变动生效日期", example = "2026-07-25", required = true)
    private LocalDate transferDate;

    @Schema(description = "变动原因", example = "业务调整")
    private String reason;

    public Long getNewDepartmentId() {
        return newDepartmentId;
    }

    public void setNewDepartmentId(Long newDepartmentId) {
        this.newDepartmentId = newDepartmentId;
    }

    public Long getNewPositionId() {
        return newPositionId;
    }

    public void setNewPositionId(Long newPositionId) {
        this.newPositionId = newPositionId;
    }

    public LocalDate getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(LocalDate transferDate) {
        this.transferDate = transferDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
