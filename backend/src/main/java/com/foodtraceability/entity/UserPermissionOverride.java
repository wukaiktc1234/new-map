package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户权限覆盖实体
 *
 * <p>用于在角色权限之外，为特定用户临时添加（ADD）或移除（REMOVE）某项权限。
 * 创建后默认进入 PENDING 待审批状态，审批通过后变为 ACTIVE 生效。
 * 支持设置过期时间，过期后状态自动变为 EXPIRED；也可手动撤销（REVOKED）或拒绝（REJECTED）。</p>
 */
@TableName("user_permission_overrides")
@Schema(description = "用户权限覆盖实体")
public class UserPermissionOverride implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /** 被覆盖的用户ID */
    @Schema(description = "被覆盖的用户ID")
    private String userId;

    /** 用户姓名（冗余字段，便于列表展示） */
    @Schema(description = "用户姓名")
    private String userName;

    /** 权限码（如 "product:*"） */
    @Schema(description = "权限码")
    private String permissionCode;

    /** 权限名称（冗余字段） */
    @Schema(description = "权限名称")
    private String permissionName;

    /** 业务域（如 "product"） */
    @Schema(description = "业务域")
    private String domainCode;

    /** 覆盖类型：ADD-添加权限 / REMOVE-移除权限 */
    @Schema(description = "覆盖类型：ADD / REMOVE")
    private String overrideType;

    /** 操作原因 */
    @Schema(description = "操作原因")
    private String reason;

    /** 过期时间（可空，空表示永久） */
    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    /** 状态：ACTIVE / PENDING / EXPIRED / REVOKED / REJECTED */
    @Schema(description = "状态")
    private String status;

    /** 审批人ID（可空） */
    @Schema(description = "审批人ID")
    private String approveBy;

    /** 审批人姓名（可空） */
    @Schema(description = "审批人姓名")
    private String approveName;

    /** 审批时间（可空） */
    @Schema(description = "审批时间")
    private LocalDateTime approveTime;

    /** 创建人ID */
    @Schema(description = "创建人ID")
    private String createdBy;

    /** 创建人姓名 */
    @Schema(description = "创建人姓名")
    private String createdByName;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /** 逻辑删除：0-未删除，1-已删除 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除")
    private Integer deleted;

    public UserPermissionOverride() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public String getDomainCode() {
        return domainCode;
    }

    public void setDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    public String getOverrideType() {
        return overrideType;
    }

    public void setOverrideType(String overrideType) {
        this.overrideType = overrideType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApproveBy() {
        return approveBy;
    }

    public void setApproveBy(String approveBy) {
        this.approveBy = approveBy;
    }

    public String getApproveName() {
        return approveName;
    }

    public void setApproveName(String approveName) {
        this.approveName = approveName;
    }

    public LocalDateTime getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(LocalDateTime approveTime) {
        this.approveTime = approveTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
