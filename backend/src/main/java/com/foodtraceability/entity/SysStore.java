package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 门店实体类
 */
@TableName("sys_stores")
@Schema(description = "门店表")
public class SysStore {
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "门店ID")
    private Long id;
    @TableField("store_code")
    @Schema(description = "门店编码")
    private String storeCode;
    @TableField("store_name")
    @Schema(description = "门店名称")
    private String storeName;
    @TableField("store_type")
    @Schema(description = "门店类型：direct-直营/franchise-加盟")
    private String storeType;
    @TableField("contact_name")
    @Schema(description = "联系人")
    private String contactName;
    @TableField("contact_phone")
    @Schema(description = "联系电话")
    private String contactPhone;
    @TableField("address")
    @Schema(description = "详细地址")
    private String address;
    @TableField("business_license")
    @Schema(description = "营业执照号")
    private String businessLicense;
    @TableField("license_image")
    @Schema(description = "营业执照图片")
    private String licenseImage;
    @TableField("business_hours")
    @Schema(description = "营业时间")
    private String businessHours;
    @TableField("service_radius")
    @Schema(description = "服务半径（米）")
    private Integer serviceRadius;
    @TableField("delivery_radius")
    @Schema(description = "配送半径（米）")
    private Integer deliveryRadius;
    @TableField("region_id")
    @Schema(description = "所属区域ID")
    private Long regionId;
    @TableField("parent_id")
    @Schema(description = "上级门店ID")
    private Long parentId;
    @TableField("status")
    @Schema(description = "状态：active-营业/inactive-停业/closed-关闭")
    private String status;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public SysStore() {
    }

    public Long getId() {
        return this.id;
    }

    public String getStoreCode() {
        return this.storeCode;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public String getStoreType() {
        return this.storeType;
    }

    public String getContactName() {
        return this.contactName;
    }

    public String getContactPhone() {
        return this.contactPhone;
    }

    public String getAddress() {
        return this.address;
    }

    public String getBusinessLicense() {
        return this.businessLicense;
    }

    public String getLicenseImage() {
        return this.licenseImage;
    }

    public String getBusinessHours() {
        return this.businessHours;
    }

    public Integer getServiceRadius() {
        return this.serviceRadius;
    }

    public Integer getDeliveryRadius() {
        return this.deliveryRadius;
    }

    public Long getRegionId() {
        return this.regionId;
    }

    public Long getParentId() {
        return this.parentId;
    }

    public String getStatus() {
        return this.status;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setStoreCode(final String storeCode) {
        this.storeCode = storeCode;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setStoreType(final String storeType) {
        this.storeType = storeType;
    }

    public void setContactName(final String contactName) {
        this.contactName = contactName;
    }

    public void setContactPhone(final String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

    public void setBusinessLicense(final String businessLicense) {
        this.businessLicense = businessLicense;
    }

    public void setLicenseImage(final String licenseImage) {
        this.licenseImage = licenseImage;
    }

    public void setBusinessHours(final String businessHours) {
        this.businessHours = businessHours;
    }

    public void setServiceRadius(final Integer serviceRadius) {
        this.serviceRadius = serviceRadius;
    }

    public void setDeliveryRadius(final Integer deliveryRadius) {
        this.deliveryRadius = deliveryRadius;
    }

    public void setRegionId(final Long regionId) {
        this.regionId = regionId;
    }

    public void setParentId(final Long parentId) {
        this.parentId = parentId;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof SysStore)) return false;
        final SysStore other = (SysStore) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$serviceRadius = this.getServiceRadius();
        final java.lang.Object other$serviceRadius = other.getServiceRadius();
        if (this$serviceRadius == null ? other$serviceRadius != null : !this$serviceRadius.equals(other$serviceRadius)) return false;
        final java.lang.Object this$deliveryRadius = this.getDeliveryRadius();
        final java.lang.Object other$deliveryRadius = other.getDeliveryRadius();
        if (this$deliveryRadius == null ? other$deliveryRadius != null : !this$deliveryRadius.equals(other$deliveryRadius)) return false;
        final java.lang.Object this$regionId = this.getRegionId();
        final java.lang.Object other$regionId = other.getRegionId();
        if (this$regionId == null ? other$regionId != null : !this$regionId.equals(other$regionId)) return false;
        final java.lang.Object this$parentId = this.getParentId();
        final java.lang.Object other$parentId = other.getParentId();
        if (this$parentId == null ? other$parentId != null : !this$parentId.equals(other$parentId)) return false;
        final java.lang.Object this$storeCode = this.getStoreCode();
        final java.lang.Object other$storeCode = other.getStoreCode();
        if (this$storeCode == null ? other$storeCode != null : !this$storeCode.equals(other$storeCode)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$storeType = this.getStoreType();
        final java.lang.Object other$storeType = other.getStoreType();
        if (this$storeType == null ? other$storeType != null : !this$storeType.equals(other$storeType)) return false;
        final java.lang.Object this$contactName = this.getContactName();
        final java.lang.Object other$contactName = other.getContactName();
        if (this$contactName == null ? other$contactName != null : !this$contactName.equals(other$contactName)) return false;
        final java.lang.Object this$contactPhone = this.getContactPhone();
        final java.lang.Object other$contactPhone = other.getContactPhone();
        if (this$contactPhone == null ? other$contactPhone != null : !this$contactPhone.equals(other$contactPhone)) return false;
        final java.lang.Object this$address = this.getAddress();
        final java.lang.Object other$address = other.getAddress();
        if (this$address == null ? other$address != null : !this$address.equals(other$address)) return false;
        final java.lang.Object this$businessLicense = this.getBusinessLicense();
        final java.lang.Object other$businessLicense = other.getBusinessLicense();
        if (this$businessLicense == null ? other$businessLicense != null : !this$businessLicense.equals(other$businessLicense)) return false;
        final java.lang.Object this$licenseImage = this.getLicenseImage();
        final java.lang.Object other$licenseImage = other.getLicenseImage();
        if (this$licenseImage == null ? other$licenseImage != null : !this$licenseImage.equals(other$licenseImage)) return false;
        final java.lang.Object this$businessHours = this.getBusinessHours();
        final java.lang.Object other$businessHours = other.getBusinessHours();
        if (this$businessHours == null ? other$businessHours != null : !this$businessHours.equals(other$businessHours)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof SysStore;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $serviceRadius = this.getServiceRadius();
        result = result * PRIME + ($serviceRadius == null ? 43 : $serviceRadius.hashCode());
        final java.lang.Object $deliveryRadius = this.getDeliveryRadius();
        result = result * PRIME + ($deliveryRadius == null ? 43 : $deliveryRadius.hashCode());
        final java.lang.Object $regionId = this.getRegionId();
        result = result * PRIME + ($regionId == null ? 43 : $regionId.hashCode());
        final java.lang.Object $parentId = this.getParentId();
        result = result * PRIME + ($parentId == null ? 43 : $parentId.hashCode());
        final java.lang.Object $storeCode = this.getStoreCode();
        result = result * PRIME + ($storeCode == null ? 43 : $storeCode.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $storeType = this.getStoreType();
        result = result * PRIME + ($storeType == null ? 43 : $storeType.hashCode());
        final java.lang.Object $contactName = this.getContactName();
        result = result * PRIME + ($contactName == null ? 43 : $contactName.hashCode());
        final java.lang.Object $contactPhone = this.getContactPhone();
        result = result * PRIME + ($contactPhone == null ? 43 : $contactPhone.hashCode());
        final java.lang.Object $address = this.getAddress();
        result = result * PRIME + ($address == null ? 43 : $address.hashCode());
        final java.lang.Object $businessLicense = this.getBusinessLicense();
        result = result * PRIME + ($businessLicense == null ? 43 : $businessLicense.hashCode());
        final java.lang.Object $licenseImage = this.getLicenseImage();
        result = result * PRIME + ($licenseImage == null ? 43 : $licenseImage.hashCode());
        final java.lang.Object $businessHours = this.getBusinessHours();
        result = result * PRIME + ($businessHours == null ? 43 : $businessHours.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "SysStore(id=" + this.getId() + ", storeCode=" + this.getStoreCode() + ", storeName=" + this.getStoreName() + ", storeType=" + this.getStoreType() + ", contactName=" + this.getContactName() + ", contactPhone=" + this.getContactPhone() + ", address=" + this.getAddress() + ", businessLicense=" + this.getBusinessLicense() + ", licenseImage=" + this.getLicenseImage() + ", businessHours=" + this.getBusinessHours() + ", serviceRadius=" + this.getServiceRadius() + ", deliveryRadius=" + this.getDeliveryRadius() + ", regionId=" + this.getRegionId() + ", parentId=" + this.getParentId() + ", status=" + this.getStatus() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}
