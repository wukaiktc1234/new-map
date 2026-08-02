package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 索证索票管理实体类
 * 管理各类证照的电子档案，支持到期提醒和关联查询
 */
@TableName("certificate_managements")
@Schema(description = "索证索票管理实体")
public class CertificateManagement {

    /** 主键ID */
    @TableId(value = "cert_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long certId;

    /** 证件号码（唯一） */
    @TableField("cert_no")
    @Schema(description = "证件号码", example = "SC10650010012345")
    private String certNo;

    /** 证件类型：1营业执照 2食品经营许可证 3卫生许可证 4检疫证 5合格证 6检测报告 7其他 */
    @TableField("cert_type")
    @Schema(description = "证件类型", example = "2")
    private Integer certType;

    /** 持证方（供应商）ID */
    @TableField("supplier_id")
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 持证方名称 */
    @TableField("supplier_name")
    @Schema(description = "持证方名称", example = "XX食品有限公司")
    private String supplierName;

    /** 证件名称 */
    @TableField("cert_name")
    @Schema(description = "证件名称", example = "食品经营许可证")
    private String certName;

    /** 发证日期 */
    @TableField("issue_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "发证日期")
    private LocalDate issueDate;

    /** 有效期至 */
    @TableField("expiry_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "有效期至")
    private LocalDate expiryDate;

    /** 发证机构 */
    @TableField("issuing_authority")
    @Schema(description = "发证机构", example = "XX市市场监督管理局")
    private String issuingAuthority;

    /** 证件扫描件/电子件URL */
    @TableField("cert_image_url")
    @Schema(description = "证件图片URL")
    private String certImageUrl;

    /** 状态：1有效 2即将到期 3已过期 4已注销 */
    @TableField("cert_status")
    @Schema(description = "状态", example = "1")
    private Integer certStatus;

    /** 提前提醒天数 */
    @TableField("reminder_days")
    @Schema(description = "提前提醒天数", example = "30")
    private Integer reminderDays;

    /** 关联的进货台账ID数组 */
    @TableField("related_ledger_ids")
    @Schema(description = "关联台账ID列表")
    private Object relatedLedgerIds;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记")
    private Integer deleted;

    // Getter和Setter方法

    public Long getCertId() { return certId; }
    public void setCertId(Long certId) { this.certId = certId; }
    public String getCertNo() { return certNo; }
    public void setCertNo(String certNo) { this.certNo = certNo; }
    public Integer getCertType() { return certType; }
    public void setCertType(Integer certType) { this.certType = certType; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getCertName() { return certName; }
    public void setCertName(String certName) { this.certName = certName; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public String getIssuingAuthority() { return issuingAuthority; }
    public void setIssuingAuthority(String issuingAuthority) { this.issuingAuthority = issuingAuthority; }
    public String getCertImageUrl() { return certImageUrl; }
    public void setCertImageUrl(String certImageUrl) { this.certImageUrl = certImageUrl; }
    public Integer getCertStatus() { return certStatus; }
    public void setCertStatus(Integer certStatus) { this.certStatus = certStatus; }
    public Integer getReminderDays() { return reminderDays; }
    public void setReminderDays(Integer reminderDays) { this.reminderDays = reminderDays; }
    public Object getRelatedLedgerIds() { return relatedLedgerIds; }
    public void setRelatedLedgerIds(Object relatedLedgerIds) { this.relatedLedgerIds = relatedLedgerIds; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
