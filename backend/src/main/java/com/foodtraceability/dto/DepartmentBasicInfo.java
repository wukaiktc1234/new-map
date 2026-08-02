package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "部门基本信息DTO")
public class DepartmentBasicInfo {

    @Schema(description = "部门ID", example = "1234567890")
    private Long departmentId;

    @Schema(description = "部门名称", example = "生产部")
    private String departmentName;

    @Schema(description = "部门编码", example = "DEP001")
    private String departmentCode;

    @Schema(description = "父部门ID", example = "1234567890")
    private Long parentId;

    @Schema(description = "部门层级", example = "1")
    private Integer level;

    @Schema(description = "部门状态（1: 正常, 0: 禁用）", example = "1")
    private Integer status;

    @Schema(description = "部门负责人ID", example = "1234567890")
    private Long managerId;

    @Schema(description = "部门负责人姓名", example = "张三")
    private String managerName;

    @Schema(description = "部门描述", example = "负责生产管理")
    private String description;

    @Schema(description = "排序号", example = "1")
    private Integer sortOrder;

    @Schema(description = "员工数量", example = "5")
    private Integer employeeCount;

    @Schema(description = "数据版本号", example = "1")
    private Long version;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public DepartmentBasicInfo() {}

    public DepartmentBasicInfo(Long departmentId, String departmentName, String departmentCode,
                               Long parentId, Integer level, Integer status, Long managerId,
                               String managerName, String description, Integer sortOrder,
                               Integer employeeCount, Long version, LocalDateTime updateTime) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.departmentCode = departmentCode;
        this.parentId = parentId;
        this.level = level;
        this.status = status;
        this.managerId = managerId;
        this.managerName = managerName;
        this.description = description;
        this.sortOrder = sortOrder;
        this.employeeCount = employeeCount;
        this.version = version;
        this.updateTime = updateTime;
    }

    public static DepartmentBasicInfoBuilder builder() {
        return new DepartmentBasicInfoBuilder();
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

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(Integer employeeCount) {
        this.employeeCount = employeeCount;
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

    public static class DepartmentBasicInfoBuilder {
        private Long departmentId;
        private String departmentName;
        private String departmentCode;
        private Long parentId;
        private Integer level;
        private Integer status;
        private Long managerId;
        private String managerName;
        private String description;
        private Integer sortOrder;
        private Integer employeeCount;
        private Long version;
        private LocalDateTime updateTime;

        public DepartmentBasicInfoBuilder departmentId(Long departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        public DepartmentBasicInfoBuilder departmentName(String departmentName) {
            this.departmentName = departmentName;
            return this;
        }

        public DepartmentBasicInfoBuilder departmentCode(String departmentCode) {
            this.departmentCode = departmentCode;
            return this;
        }

        public DepartmentBasicInfoBuilder parentId(Long parentId) {
            this.parentId = parentId;
            return this;
        }

        public DepartmentBasicInfoBuilder level(Integer level) {
            this.level = level;
            return this;
        }

        public DepartmentBasicInfoBuilder status(Integer status) {
            this.status = status;
            return this;
        }

        public DepartmentBasicInfoBuilder managerId(Long managerId) {
            this.managerId = managerId;
            return this;
        }

        public DepartmentBasicInfoBuilder managerName(String managerName) {
            this.managerName = managerName;
            return this;
        }

        public DepartmentBasicInfoBuilder description(String description) {
            this.description = description;
            return this;
        }

        public DepartmentBasicInfoBuilder sortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public DepartmentBasicInfoBuilder employeeCount(Integer employeeCount) {
            this.employeeCount = employeeCount;
            return this;
        }

        public DepartmentBasicInfoBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public DepartmentBasicInfoBuilder updateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        public DepartmentBasicInfo build() {
            return new DepartmentBasicInfo(departmentId, departmentName, departmentCode,
                    parentId, level, status, managerId, managerName, description,
                    sortOrder, employeeCount, version, updateTime);
        }
    }
}
