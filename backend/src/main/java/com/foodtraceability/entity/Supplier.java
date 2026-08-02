package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 供应商实体类
 * 用于管理采购系统中的供应商信息
 */
@TableName("suppliers")
@Schema(description = "供应商实体")
public class Supplier {

    /**
     * 供应商主键ID（自增）
     */
    @TableId(value = "supplier_id", type = IdType.AUTO)
    @Schema(description = "供应商主键ID", example = "1")
    private Long supplierId;

    /**
     * 供应商编码（对外唯一编号）
     */
    @TableField("supplier_code")
    @Schema(description = "供应商编码", example = "SUP20260425001")
    private String supplierCode;

    /**
     * 供应商名称
     */
    @TableField("supplier_name")
    @Schema(description = "供应商名称", example = "北京新鲜蔬菜有限公司")
    private String supplierName;

    /**
     * 联系人
     */
    @TableField("contact_person")
    @Schema(description = "联系人", example = "张经理")
    private String contactPerson;

    /**
     * 联系电话
     */
    @TableField("phone")
    @Schema(description = "联系电话", example = "13800138000")
    private String phone;

    /**
     * 地址
     */
    @TableField("address")
    @Schema(description = "地址", example = "北京市朝阳区xxx路xx号")
    private String address;

    /**
     * 营业执照号
     */
    @TableField("license_no")
    @Schema(description = "营业执照号", example = "91110100MA01xxxxxx")
    private String licenseNo;

    /**
     * 营业执照有效期
     */
    @TableField("license_expiry")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "营业执照有效期")
    private LocalDate licenseExpiry;

    /**
     * 银行账号
     */
    @TableField("bank_account")
    @Schema(description = "银行账号", example = "6222021234567890123")
    private String bankAccount;

    /**
     * 开户银行
     */
    @TableField("bank_name")
    @Schema(description = "开户银行", example = "中国工商银行北京分行")
    private String bankName;

    /**
     * 税号
     */
    @TableField("tax_no")
    @Schema(description = "税号", example = "91110100MA01xxxxxx")
    private String taxNo;

    /**
     * 供应商分类（原材料/包装/设备/其他）
     */
    @TableField("category")
    @Schema(description = "分类", example = "原材料")
    private String category;

    /**
     * 状态（1合作中 0停用 2黑名单）
     */
    @TableField("status")
    @Schema(description = "状态（1-合作中, 0-停用, 2-黑名单）", example = "1")
    private Integer status;

    /**
     * 信用等级（A/B/C/D）
     */
    @TableField("credit_level")
    @Schema(description = "信用等级", example = "A")
    private String creditLevel;

    /**
     * 综合评分
     */
    @TableField("rating")
    @Schema(description = "综合评分", example = "5.00")
    private BigDecimal rating;

    /**
     * 备注
     */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /**
     * 默认结算方式（monthly/immediate/prepaid/check/bank_transfer）
     */
    @TableField("settlement_method")
    @Schema(description = "默认结算方式", example = "monthly")
    private String settlementMethod;

    /**
     * 默认账期天数
     */
    @TableField("payment_terms")
    @Schema(description = "默认账期天数", example = "30")
    private Integer paymentTerms;

    /**
     * 可抵扣退货余额（单位：分）
     */
    @TableField("return_credit_balance")
    @Schema(description = "可抵扣退货余额（单位：分）", example = "0")
    private Long returnCreditBalance;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记（0未删除 1已删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    // ==================== Getter & Setter ====================

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLicenseNo() {
        return licenseNo;
    }

    public void setLicenseNo(String licenseNo) {
        this.licenseNo = licenseNo;
    }

    public LocalDate getLicenseExpiry() {
        return licenseExpiry;
    }

    public void setLicenseExpiry(LocalDate licenseExpiry) {
        this.licenseExpiry = licenseExpiry;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getTaxNo() {
        return taxNo;
    }

    public void setTaxNo(String taxNo) {
        this.taxNo = taxNo;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCreditLevel() {
        return creditLevel;
    }

    public void setCreditLevel(String creditLevel) {
        this.creditLevel = creditLevel;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getSettlementMethod() {
        return settlementMethod;
    }

    public void setSettlementMethod(String settlementMethod) {
        this.settlementMethod = settlementMethod;
    }

    public Integer getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(Integer paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public Long getReturnCreditBalance() {
        return returnCreditBalance;
    }

    public void setReturnCreditBalance(Long returnCreditBalance) {
        this.returnCreditBalance = returnCreditBalance;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
