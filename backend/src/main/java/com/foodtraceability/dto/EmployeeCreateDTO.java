package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

@Schema(description = "员工创建DTO")
public class EmployeeCreateDTO {

    @NotBlank(message = "员工姓名不能为空")
    @Schema(description = "员工姓名", example = "张三", required = true)
    private String name;

    @NotBlank(message = "性别不能为空")
    @Pattern(regexp = "^(male|female|other)$", message = "性别值必须为male、female或other")
    @Schema(description = "性别", example = "male", required = true)
    private String gender;

    @Schema(description = "部门ID", example = "1")
    private Long departmentId;

    @Schema(description = "职位ID", example = "1")
    private Long positionId;

    @Pattern(regexp = "^(STORE|WAREHOUSE|HEADQUARTERS)$", message = "工作归属类型必须为STORE、WAREHOUSE或HEADQUARTERS")
    @Schema(description = "工作归属类型：STORE/WAREHOUSE/HEADQUARTERS", example = "STORE")
    private String workLocationType;

    @Schema(description = "所属门店ID", example = "1")
    private Long storeId;

    @Schema(description = "归属仓库ID", example = "1")
    private Long warehouseId;

    @NotNull(message = "入职日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "入职日期", example = "2026-01-12 00:00:00", required = true)
    private LocalDateTime hireDate;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不能小于0")
    @Max(value = 2, message = "状态值不能大于2")
    @Schema(description = "状态(1:在职,0:离职,2:试用期)", example = "1", required = true)
    private Integer status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public String getWorkLocationType() {
        return workLocationType;
    }

    public void setWorkLocationType(String workLocationType) {
        this.workLocationType = workLocationType;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public LocalDateTime getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDateTime hireDate) {
        this.hireDate = hireDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
