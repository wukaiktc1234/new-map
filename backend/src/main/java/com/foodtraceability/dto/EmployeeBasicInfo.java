package com.foodtraceability.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 员工基础信息DTO
 * 用于跨模块数据共享，只包含必要的基本信息
 */
@Schema(description = "员工基本信息DTO")
public class EmployeeBasicInfo {
    @Schema(description = "员工ID")
    private String employeeId;
    @Schema(description = "员工姓名")
    private String employeeName;
    @Schema(description = "员工编码")
    private String employeeCode;
    @Schema(description = "部门ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long departmentId;
    @Schema(description = "部门名称")
    private String departmentName;
    @Schema(description = "职位ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long positionId;
    @Schema(description = "职位名称")
    private String positionName;
    @Schema(description = "门店ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long storeId;
    @Schema(description = "门店名称")
    private String storeName;
    @Schema(description = "状态（1: 在职, 0: 离职, 2: 试用期）")
    private Integer status;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "数据版本号")
    private Long version;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;


    public static class EmployeeBasicInfoBuilder {
        private String employeeId;
        private String employeeName;
        private String employeeCode;
        private Long departmentId;
        private String departmentName;
        private Long positionId;
        private String positionName;
        private Long storeId;
        private String storeName;
        private Integer status;
        private String phone;
        private Long version;
        private LocalDateTime updateTime;

        EmployeeBasicInfoBuilder() {
        }

        /**
         * @return {@code this}.
         */
        public EmployeeBasicInfo.EmployeeBasicInfoBuilder employeeId(final String employeeId) {
            this.employeeId = employeeId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public EmployeeBasicInfo.EmployeeBasicInfoBuilder employeeName(final String employeeName) {
            this.employeeName = employeeName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public EmployeeBasicInfo.EmployeeBasicInfoBuilder employeeCode(final String employeeCode) {
            this.employeeCode = employeeCode;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public EmployeeBasicInfo.EmployeeBasicInfoBuilder departmentId(final Long departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public EmployeeBasicInfo.EmployeeBasicInfoBuilder departmentName(final String departmentName) {
            this.departmentName = departmentName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public EmployeeBasicInfo.EmployeeBasicInfoBuilder positionId(final Long positionId) {
            this.positionId = positionId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public EmployeeBasicInfo.EmployeeBasicInfoBuilder positionName(final String positionName) {
            this.positionName = positionName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public EmployeeBasicInfo.EmployeeBasicInfoBuilder storeId(final Long storeId) {
            this.storeId = storeId;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public EmployeeBasicInfo.EmployeeBasicInfoBuilder storeName(final String storeName) {
            this.storeName = storeName;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public EmployeeBasicInfo.EmployeeBasicInfoBuilder status(final Integer status) {
            this.status = status;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public EmployeeBasicInfo.EmployeeBasicInfoBuilder phone(final String phone) {
            this.phone = phone;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public EmployeeBasicInfo.EmployeeBasicInfoBuilder version(final Long version) {
            this.version = version;
            return this;
        }

        /**
         * @return {@code this}.
         */
        public EmployeeBasicInfo.EmployeeBasicInfoBuilder updateTime(final LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        public EmployeeBasicInfo build() {
            return new EmployeeBasicInfo(this.employeeId, this.employeeName, this.employeeCode, this.departmentId, this.departmentName, this.positionId, this.positionName, this.storeId, this.storeName, this.status, this.phone, this.version, this.updateTime);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "EmployeeBasicInfo.EmployeeBasicInfoBuilder(employeeId=" + this.employeeId + ", employeeName=" + this.employeeName + ", employeeCode=" + this.employeeCode + ", departmentId=" + this.departmentId + ", departmentName=" + this.departmentName + ", positionId=" + this.positionId + ", positionName=" + this.positionName + ", storeId=" + this.storeId + ", storeName=" + this.storeName + ", status=" + this.status + ", phone=" + this.phone + ", version=" + this.version + ", updateTime=" + this.updateTime + ")";
        }
    }

    public static EmployeeBasicInfo.EmployeeBasicInfoBuilder builder() {
        return new EmployeeBasicInfo.EmployeeBasicInfoBuilder();
    }

    public String getEmployeeId() {
        return this.employeeId;
    }

    public String getEmployeeName() {
        return this.employeeName;
    }

    public String getEmployeeCode() {
        return this.employeeCode;
    }

    public Long getDepartmentId() {
        return this.departmentId;
    }

    public String getDepartmentName() {
        return this.departmentName;
    }

    public Long getPositionId() {
        return this.positionId;
    }

    public String getPositionName() {
        return this.positionName;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public Integer getStatus() {
        return this.status;
    }

    public String getPhone() {
        return this.phone;
    }

    public Long getVersion() {
        return this.version;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public void setEmployeeId(final String employeeId) {
        this.employeeId = employeeId;
    }

    public void setEmployeeName(final String employeeName) {
        this.employeeName = employeeName;
    }

    public void setEmployeeCode(final String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public void setDepartmentId(final Long departmentId) {
        this.departmentId = departmentId;
    }

    public void setDepartmentName(final String departmentName) {
        this.departmentName = departmentName;
    }

    public void setPositionId(final Long positionId) {
        this.positionId = positionId;
    }

    public void setPositionName(final String positionName) {
        this.positionName = positionName;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    public void setVersion(final Long version) {
        this.version = version;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof EmployeeBasicInfo)) return false;
        final EmployeeBasicInfo other = (EmployeeBasicInfo) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final java.lang.Object this$employeeId = this.getEmployeeId();
        final java.lang.Object other$employeeId = other.getEmployeeId();
        if (this$employeeId == null ? other$employeeId != null : !this$employeeId.equals(other$employeeId)) return false;
        final java.lang.Object this$employeeName = this.getEmployeeName();
        final java.lang.Object other$employeeName = other.getEmployeeName();
        if (this$employeeName == null ? other$employeeName != null : !this$employeeName.equals(other$employeeName)) return false;
        final java.lang.Object this$employeeCode = this.getEmployeeCode();
        final java.lang.Object other$employeeCode = other.getEmployeeCode();
        if (this$employeeCode == null ? other$employeeCode != null : !this$employeeCode.equals(other$employeeCode)) return false;
        final java.lang.Object this$departmentId = this.getDepartmentId();
        final java.lang.Object other$departmentId = other.getDepartmentId();
        if (this$departmentId == null ? other$departmentId != null : !this$departmentId.equals(other$departmentId)) return false;
        final java.lang.Object this$departmentName = this.getDepartmentName();
        final java.lang.Object other$departmentName = other.getDepartmentName();
        if (this$departmentName == null ? other$departmentName != null : !this$departmentName.equals(other$departmentName)) return false;
        final java.lang.Object this$positionId = this.getPositionId();
        final java.lang.Object other$positionId = other.getPositionId();
        if (this$positionId == null ? other$positionId != null : !this$positionId.equals(other$positionId)) return false;
        final java.lang.Object this$positionName = this.getPositionName();
        final java.lang.Object other$positionName = other.getPositionName();
        if (this$positionName == null ? other$positionName != null : !this$positionName.equals(other$positionName)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$phone = this.getPhone();
        final java.lang.Object other$phone = other.getPhone();
        if (this$phone == null ? other$phone != null : !this$phone.equals(other$phone)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof EmployeeBasicInfo;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $employeeId = this.getEmployeeId();
        result = result * PRIME + ($employeeId == null ? 43 : $employeeId.hashCode());
        final java.lang.Object $employeeName = this.getEmployeeName();
        result = result * PRIME + ($employeeName == null ? 43 : $employeeName.hashCode());
        final java.lang.Object $employeeCode = this.getEmployeeCode();
        result = result * PRIME + ($employeeCode == null ? 43 : $employeeCode.hashCode());
        final java.lang.Object $departmentId = this.getDepartmentId();
        result = result * PRIME + ($departmentId == null ? 43 : $departmentId.hashCode());
        final java.lang.Object $departmentName = this.getDepartmentName();
        result = result * PRIME + ($departmentName == null ? 43 : $departmentName.hashCode());
        final java.lang.Object $positionId = this.getPositionId();
        result = result * PRIME + ($positionId == null ? 43 : $positionId.hashCode());
        final java.lang.Object $positionName = this.getPositionName();
        result = result * PRIME + ($positionName == null ? 43 : $positionName.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $phone = this.getPhone();
        result = result * PRIME + ($phone == null ? 43 : $phone.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "EmployeeBasicInfo(employeeId=" + this.getEmployeeId() + ", employeeName=" + this.getEmployeeName() + ", employeeCode=" + this.getEmployeeCode() + ", departmentId=" + this.getDepartmentId() + ", departmentName=" + this.getDepartmentName() + ", positionId=" + this.getPositionId() + ", positionName=" + this.getPositionName() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", status=" + this.getStatus() + ", phone=" + this.getPhone() + ", version=" + this.getVersion() + ", updateTime=" + this.getUpdateTime() + ")";
    }

    public EmployeeBasicInfo() {
    }

    public EmployeeBasicInfo(final String employeeId, final String employeeName, final String employeeCode, final Long departmentId, final String departmentName, final Long positionId, final String positionName, final Long storeId, final String storeName, final Integer status, final String phone, final Long version, final LocalDateTime updateTime) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.employeeCode = employeeCode;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.positionId = positionId;
        this.positionName = positionName;
        this.storeId = storeId;
        this.storeName = storeName;
        this.status = status;
        this.phone = phone;
        this.version = version;
        this.updateTime = updateTime;
    }
}
