package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "用户基本信息DTO")
public class UserBasicInfo {

    @Schema(description = "用户ID", example = "1234567890")
    private String userId;

    @Schema(description = "用户名（登录名）", example = "admin")
    private String username;

    @Schema(description = "用户姓名", example = "张三")
    private String fullName;

    @Schema(description = "用户邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "用户手机号", example = "13800138000")
    private String phone;

    @Schema(description = "用户状态（1: 正常, 0: 禁用）", example = "1")
    private Integer status;

    @Schema(description = "所属部门ID", example = "1234567890")
    private Long departmentId;

    @Schema(description = "所属部门名称", example = "生产部")
    private String departmentName;

    @Schema(description = "所属门店ID", example = "1234567890")
    private Long storeId;

    @Schema(description = "所属门店名称", example = "中心店")
    private String storeName;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "数据版本号", example = "1")
    private Long version;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public UserBasicInfo() {}

    public UserBasicInfo(String userId, String username, String fullName, String email,
                        String phone, Integer status, Long departmentId, String departmentName,
                        Long storeId, String storeName, String avatar, Long version,
                        LocalDateTime updateTime) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.storeId = storeId;
        this.storeName = storeName;
        this.avatar = avatar;
        this.version = version;
        this.updateTime = updateTime;
    }

    public static UserBasicInfoBuilder builder() {
        return new UserBasicInfoBuilder();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
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

    public static class UserBasicInfoBuilder {
        private String userId;
        private String username;
        private String fullName;
        private String email;
        private String phone;
        private Integer status;
        private Long departmentId;
        private String departmentName;
        private Long storeId;
        private String storeName;
        private String avatar;
        private Long version;
        private LocalDateTime updateTime;

        public UserBasicInfoBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public UserBasicInfoBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserBasicInfoBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public UserBasicInfoBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBasicInfoBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public UserBasicInfoBuilder status(Integer status) {
            this.status = status;
            return this;
        }

        public UserBasicInfoBuilder departmentId(Long departmentId) {
            this.departmentId = departmentId;
            return this;
        }

        public UserBasicInfoBuilder departmentName(String departmentName) {
            this.departmentName = departmentName;
            return this;
        }

        public UserBasicInfoBuilder storeId(Long storeId) {
            this.storeId = storeId;
            return this;
        }

        public UserBasicInfoBuilder storeName(String storeName) {
            this.storeName = storeName;
            return this;
        }

        public UserBasicInfoBuilder avatar(String avatar) {
            this.avatar = avatar;
            return this;
        }

        public UserBasicInfoBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public UserBasicInfoBuilder updateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        public UserBasicInfo build() {
            return new UserBasicInfo(userId, username, fullName, email, phone, status,
                    departmentId, departmentName, storeId, storeName, avatar, version, updateTime);
        }
    }
}
