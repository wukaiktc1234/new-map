package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "职位基本信息DTO")
public class PositionBasicInfo {

    @Schema(description = "职位ID", example = "1234567890")
    private Long positionId;

    @Schema(description = "职位名称", example = "经理")
    private String positionName;

    @Schema(description = "职位编码", example = "POS001")
    private String positionCode;

    @Schema(description = "所属部门ID", example = "1234567890")
    private Long departmentId;

    @Schema(description = "所属部门名称", example = "技术部")
    private String departmentName;

    @Schema(description = "职位描述", example = "负责部门管理工作")
    private String description;

    @Schema(description = "员工数量", example = "5")
    private Integer employeeCount;

    @Schema(description = "职位状态（true: 启用, false: 禁用）", example = "true")
    private Boolean status;

    @Schema(description = "数据版本号", example = "1")
    private Long version;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public PositionBasicInfo() {}

    public PositionBasicInfo(Long positionId, String positionName, String positionCode,
                            Long departmentId, String departmentName, String description,
                            Integer employeeCount, Boolean status, Long version,
                            LocalDateTime updateTime) {
        this.positionId = positionId;
        this.positionName = positionName;
        this.positionCode = positionCode;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.description = description;
        this.employeeCount = employeeCount;
        this.status = status;
        this.version = version;
        this.updateTime = updateTime;
    }

    public static PositionBasicInfoBuilder builder() {
        return new PositionBasicInfoBuilder();
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getPositionCode() {
        return positionCode;
    }

    public void setPositionCode(String positionCode) {
        this.positionCode = positionCode;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public static class PositionBasicInfoBuilder {
        private Long positionId;
        private String positionName;
        private String positionCode;
        private Long departmentId;
        private String departmentName;
        private String description;
        private Integer employeeCount;
        private Boolean status;
        private Long version;
        private LocalDateTime updateTime;

        public PositionBasicInfoBuilder positionId(Long positionId) {
            this.positionId = positionId;
            return this;
        }

        public PositionBasicInfoBuilder positionName(String positionName) {
            this.positionName = positionName;
            return this;
        }

        public PositionBasicInfoBuilder positionCode(String positionCode) {
            this.positionCode = positionCode;
            return this;
        }

        public PositionBasicInfoBuilder departmentId(Long departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        public PositionBasicInfoBuilder departmentName(String departmentName) {
            this.departmentName = departmentName;
            return this;
        }

        public PositionBasicInfoBuilder description(String description) {
            this.description = description;
            return this;
        }

        public PositionBasicInfoBuilder employeeCount(Integer employeeCount) {
            this.employeeCount = employeeCount;
            return this;
        }

        public PositionBasicInfoBuilder status(Boolean status) {
            this.status = status;
            return this;
        }

        public PositionBasicInfoBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public PositionBasicInfoBuilder updateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        public PositionBasicInfo build() {
            return new PositionBasicInfo(positionId, positionName, positionCode,
                    departmentId, departmentName, description, employeeCount,
                    status, version, updateTime);
        }
    }
}
