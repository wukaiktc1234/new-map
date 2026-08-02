package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 公司初始化记录实体
 * 记录系统首次使用时完成的公司基础信息初始化数据
 */
@TableName("company_init_record")
@Schema(description = "公司初始化记录")
public class CompanyInitRecord {

    /**
     * 记录ID
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "记录ID")
    private Long recordId;

    /**
     * 公司名称
     */
    @TableField("company_name")
    @Schema(description = "公司名称")
    private String companyName;

    /**
     * 公司编码
     */
    @TableField("company_code")
    @Schema(description = "公司编码")
    private String companyCode;

    /**
     * 法人代表
     */
    @TableField("legal_person")
    @Schema(description = "法人代表")
    private String legalPerson;

    /**
     * 联系电话
     */
    @TableField("contact_phone")
    @Schema(description = "联系电话")
    private String contactPhone;

    /**
     * 公司地址
     */
    @TableField("address")
    @Schema(description = "公司地址")
    private String address;

    /**
     * 初始化状态：completed(已完成) / draft(草稿)
     */
    @TableField("status")
    @Schema(description = "初始化状态")
    private String status;

    /**
     * 初始化版本
     */
    @TableField("init_version")
    @Schema(description = "初始化版本")
    private String initVersion;

    /**
     * 备注
     */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    public CompanyInitRecord() {
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
