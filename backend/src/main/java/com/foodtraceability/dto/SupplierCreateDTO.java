package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 供应商创建DTO
 * 用于接收创建供应商时的请求数据
 */
@Schema(description = "供应商创建请求")
public class SupplierCreateDTO {

    /**
     * 供应商名称
     */
    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 100, message = "供应商名称不能超过100个字符")
    @Schema(description = "供应商名称", example = "北京新鲜蔬菜有限公司", requiredMode = Schema.RequiredMode.REQUIRED)
    private String supplierName;

    /**
     * 联系人
     */
    @Size(max = 50, message = "联系人不能超过50个字符")
    @Schema(description = "联系人", example = "张经理")
    private String contactPerson;

    /**
     * 联系电话
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$|^0\\d{2,3}-?\\d{7,8}$", message = "手机号格式不正确")
    @Size(max = 20, message = "电话不能超过20个字符")
    @Schema(description = "联系电话", example = "13800138000")
    private String phone;

    /**
     * 地址
     */
    @Size(max = 500, message = "地址不能超过500个字符")
    @Schema(description = "地址", example = "北京市朝阳区xxx路xx号")
    private String address;

    /**
     * 营业执照号
     */
    @Size(max = 100, message = "营业执照号不能超过100个字符")
    @Schema(description = "营业执照号", example = "91110100MA01xxxxxx")
    private String licenseNo;

    /**
     * 营业执照有效期
     */
    @Schema(description = "营业执照有效期")
    private LocalDate licenseExpiry;

    /**
     * 银行账号
     */
    @Size(max = 50, message = "银行账号不能超过50个字符")
    @Schema(description = "银行账号", example = "6222021234567890123")
    private String bankAccount;

    /**
     * 开户银行
     */
    @Size(max = 100, message = "开户银行不能超过100个字符")
    @Schema(description = "开户银行", example = "中国工商银行北京分行")
    private String bankName;

    /**
     * 税号
     */
    @Size(max = 50, message = "税号不能超过50个字符")
    @Schema(description = "税号", example = "91110100MA01xxxxxx")
    private String taxNo;

    /**
     * 供应商分类
     */
    @Pattern(regexp = "^(原材料|包装|设备|其他)$", message = "分类必须是：原材料/包装/设备/其他")
    @Schema(description = "分类（原材料/包装/设备/其他）", example = "原材料")
    private String category;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注不能超过500个字符")
    @Schema(description = "备注")
    private String remark;

    /**
     * 默认结算方式
     */
    @Pattern(regexp = "^(monthly|immediate|prepaid|check|bank_transfer)$", message = "结算方式必须是：monthly/immediate/prepaid/check/bank_transfer")
    @Schema(description = "默认结算方式（monthly/immediate/prepaid/check/bank_transfer）", example = "monthly")
    private String settlementMethod;

    /**
     * 默认账期天数
     */
    @Min(value = 0, message = "账期天数不能小于0")
    @Max(value = 365, message = "账期天数不能超过365")
    @Schema(description = "默认账期天数", example = "30")
    private Integer paymentTerms;

    // ==================== Getter & Setter ====================

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
}
