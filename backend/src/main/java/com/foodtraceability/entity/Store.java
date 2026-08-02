package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 门店实体类
 * 用于管理系统中的门店信息
 */
@TableName("stores")
@Schema(description = "门店实体")
public class Store {
    /**
     * 门店ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("store_id")
    @Schema(description = "门店ID", example = "1234567890")
    private String storeId;
    /**
     * 门店名称
     */
    @TableField("store_name")
    @Schema(description = "门店名称", example = "中心店")
    private String storeName;
    /**
     * 门店编码（唯一）
     */
    @TableField("store_code")
    @Schema(description = "门店编码（唯一）", example = "ST001")
    private String storeCode;
    /**
     * 门店地址
     */
    @TableField("address")
    @Schema(description = "门店地址", example = "北京市朝阳区建国路88号")
    private String address;
    /**
     * 联系电话
     */
    @TableField("phone")
    @Schema(description = "联系电话", example = "010-12345678")
    private String phone;
    /**
     * 负责人ID
     */
    @TableField("manager_id")
    @Schema(description = "负责人ID", example = "1234567890")
    private String managerId;
    /**
     * 负责人姓名
     */
    @TableField("manager_name")
    @Schema(description = "负责人姓名", example = "张三")
    private String managerName;
    /**
     * 门店状态（active: 正常, inactive: 禁用）
     */
    @TableField("status")
    @Schema(description = "门店状态（active: 正常, inactive: 禁用）", example = "active")
    private String status;
    /**
     * 所属公司ID
     */
    @TableField("company_id")
    @Schema(description = "所属公司ID", example = "1234567890")
    private String companyId;
    /**
     * 所属区域
     */
    @TableField("region")
    @Schema(description = "所属区域", example = "华北区")
    private String region;
    /**
     * 门店类型（single: 单店, chain: 连锁店）
     */
    @TableField("store_type")
    @Schema(description = "门店类型（single: 单店, chain: 连锁店）", example = "single")
    private String storeType;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    /**
     * 创建人
     */
    @TableField(value = "created_by", fill = FieldFill.INSERT)
    @Schema(description = "创建人", example = "system")
    private String createdBy;
    /**
     * 更新人
     */
    @TableField(value = "updated_by", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新人", example = "admin")
    private String updatedBy;
    /**
     * 逻辑删除标记（0:未删除 1:已删除）
     */
    @TableField("deleted")
    @TableLogic
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;
    /**
     * 版本号（乐观锁）
     */
    @TableField("version")
    @Version
    @Schema(description = "版本号（乐观锁）", example = "1")
    private Integer version;
    /**
     * 扩展字段（JSON格式存储）
     */
    @TableField("ext_data")
    @Schema(description = "扩展字段（JSON格式存储）", example = "{\"customField\": \"value\"}")
    private String extData;

    // Getter methods
    public String getStoreId() {
        return storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public String getManagerId() {
        return managerId;
    }

    public String getManagerName() {
        return managerName;
    }

    public String getStatus() {
        return status;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getRegion() {
        return region;
    }

    public String getStoreType() {
        return storeType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public Integer getVersion() {
        return version;
    }

    public String getExtData() {
        return extData;
    }

    // Setter methods
    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public void setExtData(String extData) {
        this.extData = extData;
    }

    public Store() {
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof Store)) return false;
        final Store other = (Store) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$storeCode = this.getStoreCode();
        final java.lang.Object other$storeCode = other.getStoreCode();
        if (this$storeCode == null ? other$storeCode != null : !this$storeCode.equals(other$storeCode)) return false;
        final java.lang.Object this$address = this.getAddress();
        final java.lang.Object other$address = other.getAddress();
        if (this$address == null ? other$address != null : !this$address.equals(other$address)) return false;
        final java.lang.Object this$phone = this.getPhone();
        final java.lang.Object other$phone = other.getPhone();
        if (this$phone == null ? other$phone != null : !this$phone.equals(other$phone)) return false;
        final java.lang.Object this$managerId = this.getManagerId();
        final java.lang.Object other$managerId = other.getManagerId();
        if (this$managerId == null ? other$managerId != null : !this$managerId.equals(other$managerId)) return false;
        final java.lang.Object this$managerName = this.getManagerName();
        final java.lang.Object other$managerName = other.getManagerName();
        if (this$managerName == null ? other$managerName != null : !this$managerName.equals(other$managerName)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$companyId = this.getCompanyId();
        final java.lang.Object other$companyId = other.getCompanyId();
        if (this$companyId == null ? other$companyId != null : !this$companyId.equals(other$companyId)) return false;
        final java.lang.Object this$region = this.getRegion();
        final java.lang.Object other$region = other.getRegion();
        if (this$region == null ? other$region != null : !this$region.equals(other$region)) return false;
        final java.lang.Object this$storeType = this.getStoreType();
        final java.lang.Object other$storeType = other.getStoreType();
        if (this$storeType == null ? other$storeType != null : !this$storeType.equals(other$storeType)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        final java.lang.Object this$createdBy = this.getCreatedBy();
        final java.lang.Object other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) return false;
        final java.lang.Object this$updatedBy = this.getUpdatedBy();
        final java.lang.Object other$updatedBy = other.getUpdatedBy();
        if (this$updatedBy == null ? other$updatedBy != null : !this$updatedBy.equals(other$updatedBy)) return false;
        final java.lang.Object this$extData = this.getExtData();
        final java.lang.Object other$extData = other.getExtData();
        if (this$extData == null ? other$extData != null : !this$extData.equals(other$extData)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Store;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $storeCode = this.getStoreCode();
        result = result * PRIME + ($storeCode == null ? 43 : $storeCode.hashCode());
        final java.lang.Object $address = this.getAddress();
        result = result * PRIME + ($address == null ? 43 : $address.hashCode());
        final java.lang.Object $phone = this.getPhone();
        result = result * PRIME + ($phone == null ? 43 : $phone.hashCode());
        final java.lang.Object $managerId = this.getManagerId();
        result = result * PRIME + ($managerId == null ? 43 : $managerId.hashCode());
        final java.lang.Object $managerName = this.getManagerName();
        result = result * PRIME + ($managerName == null ? 43 : $managerName.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $companyId = this.getCompanyId();
        result = result * PRIME + ($companyId == null ? 43 : $companyId.hashCode());
        final java.lang.Object $region = this.getRegion();
        result = result * PRIME + ($region == null ? 43 : $region.hashCode());
        final java.lang.Object $storeType = this.getStoreType();
        result = result * PRIME + ($storeType == null ? 43 : $storeType.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        final java.lang.Object $createdBy = this.getCreatedBy();
        result = result * PRIME + ($createdBy == null ? 43 : $createdBy.hashCode());
        final java.lang.Object $updatedBy = this.getUpdatedBy();
        result = result * PRIME + ($updatedBy == null ? 43 : $updatedBy.hashCode());
        final java.lang.Object $extData = this.getExtData();
        result = result * PRIME + ($extData == null ? 43 : $extData.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Store(storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", storeCode=" + this.getStoreCode() + ", address=" + this.getAddress() + ", phone=" + this.getPhone() + ", managerId=" + this.getManagerId() + ", managerName=" + this.getManagerName() + ", status=" + this.getStatus() + ", companyId=" + this.getCompanyId() + ", region=" + this.getRegion() + ", storeType=" + this.getStoreType() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", createdBy=" + this.getCreatedBy() + ", updatedBy=" + this.getUpdatedBy() + ", deleted=" + this.getDeleted() + ", version=" + this.getVersion() + ", extData=" + this.getExtData() + ")";
    }
}
