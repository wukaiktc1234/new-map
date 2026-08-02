package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 密码历史实体类
 * 用于记录用户历史密码，防止重复使用
 */
@TableName("password_history")
public class PasswordHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 密码哈希（BCrypt）
     */
    private String passwordHash;
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /**
     * 是否删除（0-未删除，1-已删除）
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public PasswordHistory() {
    }

    public Long getId() {
        return this.id;
    }

    /**
     * 用户ID
     */
    public Long getUserId() {
        return this.userId;
    }

    /**
     * 密码哈希（BCrypt）
     */
    public String getPasswordHash() {
        return this.passwordHash;
    }

    /**
     * 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 用户ID
     */
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    /**
     * 密码哈希（BCrypt）
     */
    public void setPasswordHash(final String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * 创建时间
     */
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 是否删除（0-未删除，1-已删除）
     */
    public Integer getDeleted() {
        return this.deleted;
    }

    /**
     * 是否删除（0-未删除，1-已删除）
     */
    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof PasswordHistory)) return false;
        final PasswordHistory other = (PasswordHistory) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$passwordHash = this.getPasswordHash();
        final java.lang.Object other$passwordHash = other.getPasswordHash();
        if (this$passwordHash == null ? other$passwordHash != null : !this$passwordHash.equals(other$passwordHash)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof PasswordHistory;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $passwordHash = this.getPasswordHash();
        result = result * PRIME + ($passwordHash == null ? 43 : $passwordHash.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "PasswordHistory(id=" + this.getId() + ", userId=" + this.getUserId() + ", passwordHash=" + this.getPasswordHash() + ", createdAt=" + this.getCreatedAt() + ")";
    }
}
