package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 组织架构信息DTO
 */
@Schema(description = "组织架构信息DTO")
public class OrganizationInfoDTO {
    @Schema(description = "门店列表")
    private List<StoreInfo> stores;
    @Schema(description = "部门列表")
    private List<DepartmentInfo> departments;
    @Schema(description = "职位列表")
    private List<PositionInfo> positions;


    /**
     * 门店信息
     */
    @Schema(description = "门店信息")
    public static class StoreInfo {
        @Schema(description = "门店名称")
        private String name;
        @Schema(description = "门店编码")
        private String code;
        @Schema(description = "门店地址")
        private String address;
        @Schema(description = "联系人")
        private String contactPerson;
        @Schema(description = "联系电话")
        private String contactPhone;
        @Schema(description = "区域")
        private String region;

        public StoreInfo() {
        }

        public String getName() {
            return this.name;
        }

        public String getCode() {
            return this.code;
        }

        public String getAddress() {
            return this.address;
        }

        public String getContactPerson() {
            return this.contactPerson;
        }

        public String getContactPhone() {
            return this.contactPhone;
        }

        public String getRegion() {
            return this.region;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public void setAddress(final String address) {
            this.address = address;
        }

        public void setContactPerson(final String contactPerson) {
            this.contactPerson = contactPerson;
        }

        public void setContactPhone(final String contactPhone) {
            this.contactPhone = contactPhone;
        }

        public void setRegion(final String region) {
            this.region = region;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof OrganizationInfoDTO.StoreInfo)) return false;
            final OrganizationInfoDTO.StoreInfo other = (OrganizationInfoDTO.StoreInfo) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$name = this.getName();
            final java.lang.Object other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
            final java.lang.Object this$code = this.getCode();
            final java.lang.Object other$code = other.getCode();
            if (this$code == null ? other$code != null : !this$code.equals(other$code)) return false;
            final java.lang.Object this$address = this.getAddress();
            final java.lang.Object other$address = other.getAddress();
            if (this$address == null ? other$address != null : !this$address.equals(other$address)) return false;
            final java.lang.Object this$contactPerson = this.getContactPerson();
            final java.lang.Object other$contactPerson = other.getContactPerson();
            if (this$contactPerson == null ? other$contactPerson != null : !this$contactPerson.equals(other$contactPerson)) return false;
            final java.lang.Object this$contactPhone = this.getContactPhone();
            final java.lang.Object other$contactPhone = other.getContactPhone();
            if (this$contactPhone == null ? other$contactPhone != null : !this$contactPhone.equals(other$contactPhone)) return false;
            final java.lang.Object this$region = this.getRegion();
            final java.lang.Object other$region = other.getRegion();
            if (this$region == null ? other$region != null : !this$region.equals(other$region)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof OrganizationInfoDTO.StoreInfo;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $name = this.getName();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
            final java.lang.Object $code = this.getCode();
            result = result * PRIME + ($code == null ? 43 : $code.hashCode());
            final java.lang.Object $address = this.getAddress();
            result = result * PRIME + ($address == null ? 43 : $address.hashCode());
            final java.lang.Object $contactPerson = this.getContactPerson();
            result = result * PRIME + ($contactPerson == null ? 43 : $contactPerson.hashCode());
            final java.lang.Object $contactPhone = this.getContactPhone();
            result = result * PRIME + ($contactPhone == null ? 43 : $contactPhone.hashCode());
            final java.lang.Object $region = this.getRegion();
            result = result * PRIME + ($region == null ? 43 : $region.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "OrganizationInfoDTO.StoreInfo(name=" + this.getName() + ", code=" + this.getCode() + ", address=" + this.getAddress() + ", contactPerson=" + this.getContactPerson() + ", contactPhone=" + this.getContactPhone() + ", region=" + this.getRegion() + ")";
        }
    }


    /**
     * 部门信息
     */
    @Schema(description = "部门信息")
    public static class DepartmentInfo {
        @Schema(description = "部门名称")
        private String name;
        @Schema(description = "部门编码")
        private String code;
        @Schema(description = "父部门ID")
        private String parentId;
        @Schema(description = "负责人ID")
        private String managerId;

        public DepartmentInfo() {
        }

        public String getName() {
            return this.name;
        }

        public String getCode() {
            return this.code;
        }

        public String getParentId() {
            return this.parentId;
        }

        public String getManagerId() {
            return this.managerId;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public void setParentId(final String parentId) {
            this.parentId = parentId;
        }

        public void setManagerId(final String managerId) {
            this.managerId = managerId;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof OrganizationInfoDTO.DepartmentInfo)) return false;
            final OrganizationInfoDTO.DepartmentInfo other = (OrganizationInfoDTO.DepartmentInfo) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$name = this.getName();
            final java.lang.Object other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
            final java.lang.Object this$code = this.getCode();
            final java.lang.Object other$code = other.getCode();
            if (this$code == null ? other$code != null : !this$code.equals(other$code)) return false;
            final java.lang.Object this$parentId = this.getParentId();
            final java.lang.Object other$parentId = other.getParentId();
            if (this$parentId == null ? other$parentId != null : !this$parentId.equals(other$parentId)) return false;
            final java.lang.Object this$managerId = this.getManagerId();
            final java.lang.Object other$managerId = other.getManagerId();
            if (this$managerId == null ? other$managerId != null : !this$managerId.equals(other$managerId)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof OrganizationInfoDTO.DepartmentInfo;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $name = this.getName();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
            final java.lang.Object $code = this.getCode();
            result = result * PRIME + ($code == null ? 43 : $code.hashCode());
            final java.lang.Object $parentId = this.getParentId();
            result = result * PRIME + ($parentId == null ? 43 : $parentId.hashCode());
            final java.lang.Object $managerId = this.getManagerId();
            result = result * PRIME + ($managerId == null ? 43 : $managerId.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "OrganizationInfoDTO.DepartmentInfo(name=" + this.getName() + ", code=" + this.getCode() + ", parentId=" + this.getParentId() + ", managerId=" + this.getManagerId() + ")";
        }
    }


    /**
     * 职位信息
     */
    @Schema(description = "职位信息")
    public static class PositionInfo {
        @Schema(description = "职位名称")
        private String name;
        @Schema(description = "职位编码")
        private String code;
        @Schema(description = "部门ID")
        private String departmentId;

        public PositionInfo() {
        }

        public String getName() {
            return this.name;
        }

        public String getCode() {
            return this.code;
        }

        public String getDepartmentId() {
            return this.departmentId;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public void setCode(final String code) {
            this.code = code;
        }

        public void setDepartmentId(final String departmentId) {
            this.departmentId = departmentId;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof OrganizationInfoDTO.PositionInfo)) return false;
            final OrganizationInfoDTO.PositionInfo other = (OrganizationInfoDTO.PositionInfo) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$name = this.getName();
            final java.lang.Object other$name = other.getName();
            if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
            final java.lang.Object this$code = this.getCode();
            final java.lang.Object other$code = other.getCode();
            if (this$code == null ? other$code != null : !this$code.equals(other$code)) return false;
            final java.lang.Object this$departmentId = this.getDepartmentId();
            final java.lang.Object other$departmentId = other.getDepartmentId();
            if (this$departmentId == null ? other$departmentId != null : !this$departmentId.equals(other$departmentId)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof OrganizationInfoDTO.PositionInfo;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $name = this.getName();
            result = result * PRIME + ($name == null ? 43 : $name.hashCode());
            final java.lang.Object $code = this.getCode();
            result = result * PRIME + ($code == null ? 43 : $code.hashCode());
            final java.lang.Object $departmentId = this.getDepartmentId();
            result = result * PRIME + ($departmentId == null ? 43 : $departmentId.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "OrganizationInfoDTO.PositionInfo(name=" + this.getName() + ", code=" + this.getCode() + ", departmentId=" + this.getDepartmentId() + ")";
        }
    }

    public OrganizationInfoDTO() {
    }

    public List<StoreInfo> getStores() {
        return this.stores;
    }

    public List<DepartmentInfo> getDepartments() {
        return this.departments;
    }

    public List<PositionInfo> getPositions() {
        return this.positions;
    }

    public void setStores(final List<StoreInfo> stores) {
        this.stores = stores;
    }

    public void setDepartments(final List<DepartmentInfo> departments) {
        this.departments = departments;
    }

    public void setPositions(final List<PositionInfo> positions) {
        this.positions = positions;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OrganizationInfoDTO)) return false;
        final OrganizationInfoDTO other = (OrganizationInfoDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$stores = this.getStores();
        final java.lang.Object other$stores = other.getStores();
        if (this$stores == null ? other$stores != null : !this$stores.equals(other$stores)) return false;
        final java.lang.Object this$departments = this.getDepartments();
        final java.lang.Object other$departments = other.getDepartments();
        if (this$departments == null ? other$departments != null : !this$departments.equals(other$departments)) return false;
        final java.lang.Object this$positions = this.getPositions();
        final java.lang.Object other$positions = other.getPositions();
        if (this$positions == null ? other$positions != null : !this$positions.equals(other$positions)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OrganizationInfoDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $stores = this.getStores();
        result = result * PRIME + ($stores == null ? 43 : $stores.hashCode());
        final java.lang.Object $departments = this.getDepartments();
        result = result * PRIME + ($departments == null ? 43 : $departments.hashCode());
        final java.lang.Object $positions = this.getPositions();
        result = result * PRIME + ($positions == null ? 43 : $positions.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OrganizationInfoDTO(stores=" + this.getStores() + ", departments=" + this.getDepartments() + ", positions=" + this.getPositions() + ")";
    }
}
