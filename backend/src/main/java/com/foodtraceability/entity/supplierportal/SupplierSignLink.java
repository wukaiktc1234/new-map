package com.foodtraceability.entity.supplierportal;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 供应商签署链接实体类
 * 存储管理端生成的供应商合同签署链接及其状态流转信息
 *
 * <p>状态流转：
 * <ul>
 *   <li>pending（待发送）→ sent（已发送）→ viewed（已查看）→ signed（已签署）</li>
 *   <li>viewed → rejected（已拒绝）</li>
 *   <li>任意状态 → expired（已过期/已作废）</li>
 * </ul>
 */
@TableName("supplier_sign_links")
public class SupplierSignLink implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 链接ID（主键，雪花算法生成） */
    @TableId(value = "link_id", type = IdType.ASSIGN_ID)
    private String linkId;

    /** 关联电子合同ID */
    @TableField("e_contract_id")
    private String eContractId;

    /** 电子合同编号（冗余） */
    @TableField("e_contract_no")
    private String eContractNo;

    /** 合同名称（冗余） */
    @TableField("contract_name")
    private String contractName;

    /** 供应商ID（冗余） */
    @TableField("supplier_id")
    private String supplierId;

    /** 供应商名称（冗余） */
    @TableField("supplier_name")
    private String supplierName;

    /** 供应商联系人手机号 */
    @TableField("contact_phone")
    private String contactPhone;

    /** 供应商联系人邮箱 */
    @TableField("contact_email")
    private String contactEmail;

    /**
     * 链接状态
     * pending-待发送 sent-已发送 viewed-已查看 signed-已签署 rejected-已拒绝 expired-已过期
     */
    @TableField("status")
    private String status;

    /** H5访问token（唯一，用于供应商端鉴权） */
    @TableField("token")
    private String token;

    /** 链接过期时间 */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /** 链接发送时间 */
    @TableField("send_time")
    private LocalDateTime sendTime;

    /** 供应商首次查看时间 */
    @TableField("view_time")
    private LocalDateTime viewTime;

    /** 供应商签署时间 */
    @TableField("sign_time")
    private LocalDateTime signTime;

    /** 供应商拒绝时间 */
    @TableField("reject_time")
    private LocalDateTime rejectTime;

    /** 拒绝原因 */
    @TableField("reject_reason")
    private String rejectReason;

    /** 实名认证信息（JSON字符串） */
    @TableField("verification")
    private String verification;

    /** 创建人（冗余） */
    @TableField("create_by")
    private String createBy;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 状态常量 ====================

    /** 待发送 */
    public static final String STATUS_PENDING = "pending";
    /** 已发送 */
    public static final String STATUS_SENT = "sent";
    /** 已查看 */
    public static final String STATUS_VIEWED = "viewed";
    /** 已签署 */
    public static final String STATUS_SIGNED = "signed";
    /** 已拒绝 */
    public static final String STATUS_REJECTED = "rejected";
    /** 已过期/已作废 */
    public static final String STATUS_EXPIRED = "expired";

    // ==================== Getter & Setter ====================

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    public String getEContractId() {
        return eContractId;
    }

    public void setEContractId(String eContractId) {
        this.eContractId = eContractId;
    }

    public String getEContractNo() {
        return eContractNo;
    }

    public void setEContractNo(String eContractNo) {
        this.eContractNo = eContractNo;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public LocalDateTime getSendTime() {
        return sendTime;
    }

    public void setSendTime(LocalDateTime sendTime) {
        this.sendTime = sendTime;
    }

    public LocalDateTime getViewTime() {
        return viewTime;
    }

    public void setViewTime(LocalDateTime viewTime) {
        this.viewTime = viewTime;
    }

    public LocalDateTime getSignTime() {
        return signTime;
    }

    public void setSignTime(LocalDateTime signTime) {
        this.signTime = signTime;
    }

    public LocalDateTime getRejectTime() {
        return rejectTime;
    }

    public void setRejectTime(LocalDateTime rejectTime) {
        this.rejectTime = rejectTime;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getVerification() {
        return verification;
    }

    public void setVerification(String verification) {
        this.verification = verification;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
