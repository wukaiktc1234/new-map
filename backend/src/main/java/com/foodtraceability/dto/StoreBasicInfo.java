package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "门店基本信息DTO")
public class StoreBasicInfo {

    @Schema(description = "门店ID", example = "1234567890")
    private String storeId;

    @Schema(description = "门店名称", example = "中心店")
    private String storeName;

    @Schema(description = "门店编码", example = "ST001")
    private String storeCode;

    @Schema(description = "门店地址", example = "北京市朝阳区建国路88号")
    private String address;

    @Schema(description = "联系电话", example = "010-12345678")
    private String phone;

    @Schema(description = "负责人ID", example = "1234567890")
    private String managerId;

    @Schema(description = "负责人姓名", example = "张三")
    private String managerName;

    @Schema(description = "门店状态（active: 正常, inactive: 禁用）", example = "active")
    private String status;

    @Schema(description = "所属公司ID", example = "1234567890")
    private String companyId;

    @Schema(description = "所属区域", example = "华北区")
    private String region;

    @Schema(description = "门店类型（single: 单店, chain: 连锁店）", example = "single")
    private String storeType;

    @Schema(description = "数据版本号", example = "1")
    private Long version;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public StoreBasicInfo() {}

    public StoreBasicInfo(String storeId, String storeName, String storeCode,
                         String address, String phone, String managerId,
                         String managerName, String status, String companyId,
                         String region, String storeType, Long version,
                         LocalDateTime updateTime) {
        this.storeId = storeId;
        this.storeName = storeName;
        this.storeCode = storeCode;
        this.address = address;
        this.phone = phone;
        this.managerId = managerId;
        this.managerName = managerName;
        this.status = status;
        this.companyId = companyId;
        this.region = region;
        this.storeType = storeType;
        this.version = version;
        this.updateTime = updateTime;
    }

    public static StoreBasicInfoBuilder builder() {
        return new StoreBasicInfoBuilder();
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getStoreType() {
        return storeType;
    }

    public void setStoreType(String storeType) {
        this.storeType = storeType;
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

    public static class StoreBasicInfoBuilder {
        private String storeId;
        private String storeName;
        private String storeCode;
        private String address;
        private String phone;
        private String managerId;
        private String managerName;
        private String status;
        private String companyId;
        private String region;
        private String storeType;
        private Long version;
        private LocalDateTime updateTime;

        public StoreBasicInfoBuilder storeId(String storeId) {
            this.storeId = storeId;
            return this;
        }

        public StoreBasicInfoBuilder storeName(String storeName) {
            this.storeName = storeName;
            return this;
        }

        public StoreBasicInfoBuilder storeCode(String storeCode) {
            this.storeCode = storeCode;
            return this;
        }

        public StoreBasicInfoBuilder address(String address) {
            this.address = address;
            return this;
        }

        public StoreBasicInfoBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public StoreBasicInfoBuilder managerId(String managerId) {
            this.managerId = managerId;
            return this;
        }

        public StoreBasicInfoBuilder managerName(String managerName) {
            this.managerName = managerName;
            return this;
        }

        public StoreBasicInfoBuilder status(String status) {
            this.status = status;
            return this;
        }

        public StoreBasicInfoBuilder companyId(String companyId) {
            this.companyId = companyId;
            return this;
        }

        public StoreBasicInfoBuilder region(String region) {
            this.region = region;
            return this;
        }

        public StoreBasicInfoBuilder storeType(String storeType) {
            this.storeType = storeType;
            return this;
        }

        public StoreBasicInfoBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public StoreBasicInfoBuilder updateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        public StoreBasicInfo build() {
            return new StoreBasicInfo(storeId, storeName, storeCode,
                    address, phone, managerId, managerName, status, companyId,
                    region, storeType, version, updateTime);
        }
    }
}
