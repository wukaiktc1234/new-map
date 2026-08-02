package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 公司初始化请求 DTO
 */
@Schema(description = "公司初始化请求")
public class CompanyInitDTO {

    /** 公司名称 */
    @NotBlank(message = "公司名称不能为空")
    @Size(max = 200, message = "公司名称长度不能超过200字")
    @Schema(description = "公司名称", example = "XX餐饮有限公司", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyName;

    /** 公司编码 */
    @Size(max = 100, message = "公司编码长度不能超过100字")
    @Schema(description = "公司编码", example = "COMPANY001")
    private String companyCode;

    /** 法人代表 */
    @Size(max = 100, message = "法人代表长度不能超过100字")
    @Schema(description = "法人代表", example = "张三")
    private String legalPerson;

    /** 联系电话 */
    @Size(max = 50, message = "联系电话长度不能超过50字")
    @Schema(description = "联系电话", example = "13800138000")
    private String contactPhone;

    /** 公司地址 */
    @Size(max = 500, message = "公司地址长度不能超过500字")
    @Schema(description = "公司地址", example = "北京市朝阳区XX路1号")
    private String address;

    /** 初始化版本 */
    @Size(max = 20, message = "初始化版本长度不能超过20字")
    @Schema(description = "初始化版本", example = "1.0.0")
    private String initVersion;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500字")
    @Schema(description = "备注")
    private String remark;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getLegalPerson() {
        return legalPerson;
    }

    public void setLegalPerson(String legalPerson) {
        this.legalPerson = legalPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getInitVersion() {
        return initVersion;
    }

    public void setInitVersion(String initVersion) {
        this.initVersion = initVersion;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
