package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 供应商视图对象
 * 用于返回供应商详情信息给前端
 */
@Schema(description = "供应商视图对象")
public class SupplierVO {

    /**
     * 供应商主键ID
     */
    @Schema(description = "供应商主键ID", example = "1")
    private Long supplierId;

    /**
     * 供应商编码
     */
    @Schema(description = "供应商编码", example = "SUP20260425001")
    private String supplierCode;

    /**
     * 供应商名称
     */
    @Schema(description = "供应商名称", example = "北京新鲜蔬菜有限公司")
    private String supplierName;

    /**
     * 联系人
     */
    @Schema(description = "联系人", example = "张经理")
    private String contactPerson;

    /**
     * 联系电话
     */
    @Schema(description = "联系电话", example = "13800138000")
    private String phone;

    /**
     * 地址
     */
    @Schema(description = "地址")
    private String address;

    /**
     * 营业执照号
     */
    @Schema(description = "营业执照号")
    private String licenseNo;

    /**
     * 营业执照有效期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "营业执照有效期")
    private LocalDate licenseExpiry;

    /**
     * 银行账号
     */
    @Schema(description = "银行账号")
    private String bankAccount;

    /**
     * 开户银行
     */
    @Schema(description = "开户银行")
    private String bankName;

    /**
     * 税号
     */
    @Schema(description = "税号")
    private String taxNo;

    /**
     * 供应商分类
     */
    @Schema(description = "分类")
    private String category;

    /**
     * 状态描述
     */
    @Schema(description = "状态描述", example = "合作中")
    private String statusDesc;

    /**
     * 状态值
     */
    @Schema(description = "状态值", example = "1")
    private Integer status;

    /**
     * 信用等级
     */
    @Schema(description = "信用等级", example = "A")
    private String creditLevel;

    /**
     * 综合评分
     */
    @Schema(description = "综合评分", example = "5.00")
    private BigDecimal rating;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;

    /**
     * 默认结算方式
     */
    @Schema(description = "默认结算方式", example = "monthly")
    private String settlementMethod;

    /**
     * 默认账期天数
     */
    @Schema(description = "默认账期天数", example = "30")
    private Integer paymentTerms;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

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

    public String getStatusDesc() {
        return statusDesc;
    }

    public void setStatusDesc(String statusDesc) {
        this.statusDesc = statusDesc;
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
