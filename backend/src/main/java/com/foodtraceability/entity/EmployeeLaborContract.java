package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 员工劳动合同实体类
 * 对应数据库表：employee_labor_contract
 *
 * 【法律合规】劳动合同前置签署流程
 * 入职流程：审批通过 → 创建合同 → 员工签署 → 生成邀请码 → 注册入职
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@TableName("employee_labor_contract")
@Schema(description = "员工劳动合同实体")
public class EmployeeLaborContract {
    @TableId(type = IdType.AUTO)
    @Schema(description = "合同ID")
    private Long id;
    @Schema(description = "员工ID（签署后填充）")
    private String employeeId;
    @Schema(description = "员工编号")
    private String employeeCode;
    @Schema(description = "员工姓名")
    private String employeeName;
    @Schema(description = "合同编号")
    private String contractNo;
    @Schema(description = "合同类型: fixed-term固定期限, open-ended无固定期限, project项目制")
    private String contractType;
    @Schema(description = "合同开始日期")
    private LocalDate startDate;
    @Schema(description = "合同结束日期(无固定期限可为空)")
    private LocalDate endDate;
    @Schema(description = "试用期月数")
    private Integer probationMonths;
    @Schema(description = "试用期结束日期")
    private LocalDate probationEndDate;
    @Schema(description = "合同约定薪资")
    private BigDecimal salary;
    @Schema(description = "工作地点")
    private String workLocation;
    @Schema(description = "合同约定岗位")
    private String position;
    @Schema(description = "合同状态: draft草稿, pending待签, signed已签, active生效中, expired已过期, terminated已终止")
    private String status;
    @Schema(description = "签署日期")
    private LocalDate signDate;
    @Schema(description = "签署方式: electronic电子签, paper纸质签")
    private String signMethod;
    @Schema(description = "合同文件URL")
    private String contractFileUrl;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "关联入职档案ID")
    private Long archiveId;
    @Schema(description = "合同模板ID")
    private Long templateId;
    @Schema(description = "合同生成时间")
    private Date generatedTime;
    @Schema(description = "PDF文件URL")
    private String pdfFileUrl;
    @Schema(description = "已签署PDF文件URL")
    private String signedPdfUrl;
    @Schema(description = "公司签署时间")
    private Date companySignTime;
    @Schema(description = "员工签署时间")
    private Date employeeSignTime;
    @Schema(description = "创建人")
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记：0-未删除, 1-已删除")
    private Integer deleted;
    // 合同类型常量
    public static final String TYPE_FIXED_TERM = "fixed-term";
    public static final String TYPE_OPEN_ENDED = "open-ended";
    public static final String TYPE_PROJECT = "project";
    // 合同状态常量
    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_SIGNED = "signed";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_EXPIRED = "expired";
    public static final String STATUS_TERMINATED = "terminated";
    // 签署方式常量
    public static final String SIGN_ELECTRONIC = "electronic";
    public static final String SIGN_PAPER = "paper";

    public EmployeeLaborContract() {
    }

    public Long getId() {
        return this.id;
    }

    public String getEmployeeId() {
        return this.employeeId;
    }

    public String getEmployeeCode() {
        return this.employeeCode;
    }

    public String getEmployeeName() {
        return this.employeeName;
    }

    public String getContractNo() {
        return this.contractNo;
    }

    public String getContractType() {
        return this.contractType;
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public LocalDate getEndDate() {
        return this.endDate;
    }

    public Integer getProbationMonths() {
        return this.probationMonths;
    }

    public LocalDate getProbationEndDate() {
        return this.probationEndDate;
    }

    public BigDecimal getSalary() {
        return this.salary;
    }

    public String getWorkLocation() {
        return this.workLocation;
    }

    public String getPosition() {
        return this.position;
    }

    public String getStatus() {
        return this.status;
    }

    public LocalDate getSignDate() {
        return this.signDate;
    }

    public String getSignMethod() {
        return this.signMethod;
    }

    public String getContractFileUrl() {
        return this.contractFileUrl;
    }

    public String getRemark() {
        return this.remark;
    }

    public Long getArchiveId() {
        return this.archiveId;
    }

    public Long getTemplateId() {
        return this.templateId;
    }

    public Date getGeneratedTime() {
        return this.generatedTime;
    }

    public String getPdfFileUrl() {
        return this.pdfFileUrl;
    }

    public String getSignedPdfUrl() {
        return this.signedPdfUrl;
    }

    public Date getCompanySignTime() {
        return this.companySignTime;
    }

    public Date getEmployeeSignTime() {
        return this.employeeSignTime;
    }

    public Long getCreateBy() {
        return this.createBy;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setEmployeeId(final String employeeId) {
        this.employeeId = employeeId;
    }

    public void setEmployeeCode(final String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public void setEmployeeName(final String employeeName) {
        this.employeeName = employeeName;
    }

    public void setContractNo(final String contractNo) {
        this.contractNo = contractNo;
    }

    public void setContractType(final String contractType) {
        this.contractType = contractType;
    }

    public void setStartDate(final LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(final LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setProbationMonths(final Integer probationMonths) {
        this.probationMonths = probationMonths;
    }

    public void setProbationEndDate(final LocalDate probationEndDate) {
        this.probationEndDate = probationEndDate;
    }

    public void setSalary(final BigDecimal salary) {
        this.salary = salary;
    }

    public void setWorkLocation(final String workLocation) {
        this.workLocation = workLocation;
    }

    public void setPosition(final String position) {
        this.position = position;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setSignDate(final LocalDate signDate) {
        this.signDate = signDate;
    }

    public void setSignMethod(final String signMethod) {
        this.signMethod = signMethod;
    }

    public void setContractFileUrl(final String contractFileUrl) {
        this.contractFileUrl = contractFileUrl;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setArchiveId(final Long archiveId) {
        this.archiveId = archiveId;
    }

    public void setTemplateId(final Long templateId) {
        this.templateId = templateId;
    }

    public void setGeneratedTime(final Date generatedTime) {
        this.generatedTime = generatedTime;
    }

    public void setPdfFileUrl(final String pdfFileUrl) {
        this.pdfFileUrl = pdfFileUrl;
    }

    public void setSignedPdfUrl(final String signedPdfUrl) {
        this.signedPdfUrl = signedPdfUrl;
    }

    public void setCompanySignTime(final Date companySignTime) {
        this.companySignTime = companySignTime;
    }

    public void setEmployeeSignTime(final Date employeeSignTime) {
        this.employeeSignTime = employeeSignTime;
    }

    public void setCreateBy(final Long createBy) {
        this.createBy = createBy;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof EmployeeLaborContract)) return false;
        final EmployeeLaborContract other = (EmployeeLaborContract) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$probationMonths = this.getProbationMonths();
        final java.lang.Object other$probationMonths = other.getProbationMonths();
        if (this$probationMonths == null ? other$probationMonths != null : !this$probationMonths.equals(other$probationMonths)) return false;
        final java.lang.Object this$archiveId = this.getArchiveId();
        final java.lang.Object other$archiveId = other.getArchiveId();
        if (this$archiveId == null ? other$archiveId != null : !this$archiveId.equals(other$archiveId)) return false;
        final java.lang.Object this$templateId = this.getTemplateId();
        final java.lang.Object other$templateId = other.getTemplateId();
        if (this$templateId == null ? other$templateId != null : !this$templateId.equals(other$templateId)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$employeeId = this.getEmployeeId();
        final java.lang.Object other$employeeId = other.getEmployeeId();
        if (this$employeeId == null ? other$employeeId != null : !this$employeeId.equals(other$employeeId)) return false;
        final java.lang.Object this$employeeCode = this.getEmployeeCode();
        final java.lang.Object other$employeeCode = other.getEmployeeCode();
        if (this$employeeCode == null ? other$employeeCode != null : !this$employeeCode.equals(other$employeeCode)) return false;
        final java.lang.Object this$employeeName = this.getEmployeeName();
        final java.lang.Object other$employeeName = other.getEmployeeName();
        if (this$employeeName == null ? other$employeeName != null : !this$employeeName.equals(other$employeeName)) return false;
        final java.lang.Object this$contractNo = this.getContractNo();
        final java.lang.Object other$contractNo = other.getContractNo();
        if (this$contractNo == null ? other$contractNo != null : !this$contractNo.equals(other$contractNo)) return false;
        final java.lang.Object this$contractType = this.getContractType();
        final java.lang.Object other$contractType = other.getContractType();
        if (this$contractType == null ? other$contractType != null : !this$contractType.equals(other$contractType)) return false;
        final java.lang.Object this$startDate = this.getStartDate();
        final java.lang.Object other$startDate = other.getStartDate();
        if (this$startDate == null ? other$startDate != null : !this$startDate.equals(other$startDate)) return false;
        final java.lang.Object this$endDate = this.getEndDate();
        final java.lang.Object other$endDate = other.getEndDate();
        if (this$endDate == null ? other$endDate != null : !this$endDate.equals(other$endDate)) return false;
        final java.lang.Object this$probationEndDate = this.getProbationEndDate();
        final java.lang.Object other$probationEndDate = other.getProbationEndDate();
        if (this$probationEndDate == null ? other$probationEndDate != null : !this$probationEndDate.equals(other$probationEndDate)) return false;
        final java.lang.Object this$salary = this.getSalary();
        final java.lang.Object other$salary = other.getSalary();
        if (this$salary == null ? other$salary != null : !this$salary.equals(other$salary)) return false;
        final java.lang.Object this$workLocation = this.getWorkLocation();
        final java.lang.Object other$workLocation = other.getWorkLocation();
        if (this$workLocation == null ? other$workLocation != null : !this$workLocation.equals(other$workLocation)) return false;
        final java.lang.Object this$position = this.getPosition();
        final java.lang.Object other$position = other.getPosition();
        if (this$position == null ? other$position != null : !this$position.equals(other$position)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$signDate = this.getSignDate();
        final java.lang.Object other$signDate = other.getSignDate();
        if (this$signDate == null ? other$signDate != null : !this$signDate.equals(other$signDate)) return false;
        final java.lang.Object this$signMethod = this.getSignMethod();
        final java.lang.Object other$signMethod = other.getSignMethod();
        if (this$signMethod == null ? other$signMethod != null : !this$signMethod.equals(other$signMethod)) return false;
        final java.lang.Object this$contractFileUrl = this.getContractFileUrl();
        final java.lang.Object other$contractFileUrl = other.getContractFileUrl();
        if (this$contractFileUrl == null ? other$contractFileUrl != null : !this$contractFileUrl.equals(other$contractFileUrl)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$generatedTime = this.getGeneratedTime();
        final java.lang.Object other$generatedTime = other.getGeneratedTime();
        if (this$generatedTime == null ? other$generatedTime != null : !this$generatedTime.equals(other$generatedTime)) return false;
        final java.lang.Object this$pdfFileUrl = this.getPdfFileUrl();
        final java.lang.Object other$pdfFileUrl = other.getPdfFileUrl();
        if (this$pdfFileUrl == null ? other$pdfFileUrl != null : !this$pdfFileUrl.equals(other$pdfFileUrl)) return false;
        final java.lang.Object this$signedPdfUrl = this.getSignedPdfUrl();
        final java.lang.Object other$signedPdfUrl = other.getSignedPdfUrl();
        if (this$signedPdfUrl == null ? other$signedPdfUrl != null : !this$signedPdfUrl.equals(other$signedPdfUrl)) return false;
        final java.lang.Object this$companySignTime = this.getCompanySignTime();
        final java.lang.Object other$companySignTime = other.getCompanySignTime();
        if (this$companySignTime == null ? other$companySignTime != null : !this$companySignTime.equals(other$companySignTime)) return false;
        final java.lang.Object this$employeeSignTime = this.getEmployeeSignTime();
        final java.lang.Object other$employeeSignTime = other.getEmployeeSignTime();
        if (this$employeeSignTime == null ? other$employeeSignTime != null : !this$employeeSignTime.equals(other$employeeSignTime)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof EmployeeLaborContract;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $probationMonths = this.getProbationMonths();
        result = result * PRIME + ($probationMonths == null ? 43 : $probationMonths.hashCode());
        final java.lang.Object $archiveId = this.getArchiveId();
        result = result * PRIME + ($archiveId == null ? 43 : $archiveId.hashCode());
        final java.lang.Object $templateId = this.getTemplateId();
        result = result * PRIME + ($templateId == null ? 43 : $templateId.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $employeeId = this.getEmployeeId();
        result = result * PRIME + ($employeeId == null ? 43 : $employeeId.hashCode());
        final java.lang.Object $employeeCode = this.getEmployeeCode();
        result = result * PRIME + ($employeeCode == null ? 43 : $employeeCode.hashCode());
        final java.lang.Object $employeeName = this.getEmployeeName();
        result = result * PRIME + ($employeeName == null ? 43 : $employeeName.hashCode());
        final java.lang.Object $contractNo = this.getContractNo();
        result = result * PRIME + ($contractNo == null ? 43 : $contractNo.hashCode());
        final java.lang.Object $contractType = this.getContractType();
        result = result * PRIME + ($contractType == null ? 43 : $contractType.hashCode());
        final java.lang.Object $startDate = this.getStartDate();
        result = result * PRIME + ($startDate == null ? 43 : $startDate.hashCode());
        final java.lang.Object $endDate = this.getEndDate();
        result = result * PRIME + ($endDate == null ? 43 : $endDate.hashCode());
        final java.lang.Object $probationEndDate = this.getProbationEndDate();
        result = result * PRIME + ($probationEndDate == null ? 43 : $probationEndDate.hashCode());
        final java.lang.Object $salary = this.getSalary();
        result = result * PRIME + ($salary == null ? 43 : $salary.hashCode());
        final java.lang.Object $workLocation = this.getWorkLocation();
        result = result * PRIME + ($workLocation == null ? 43 : $workLocation.hashCode());
        final java.lang.Object $position = this.getPosition();
        result = result * PRIME + ($position == null ? 43 : $position.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $signDate = this.getSignDate();
        result = result * PRIME + ($signDate == null ? 43 : $signDate.hashCode());
        final java.lang.Object $signMethod = this.getSignMethod();
        result = result * PRIME + ($signMethod == null ? 43 : $signMethod.hashCode());
        final java.lang.Object $contractFileUrl = this.getContractFileUrl();
        result = result * PRIME + ($contractFileUrl == null ? 43 : $contractFileUrl.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $generatedTime = this.getGeneratedTime();
        result = result * PRIME + ($generatedTime == null ? 43 : $generatedTime.hashCode());
        final java.lang.Object $pdfFileUrl = this.getPdfFileUrl();
        result = result * PRIME + ($pdfFileUrl == null ? 43 : $pdfFileUrl.hashCode());
        final java.lang.Object $signedPdfUrl = this.getSignedPdfUrl();
        result = result * PRIME + ($signedPdfUrl == null ? 43 : $signedPdfUrl.hashCode());
        final java.lang.Object $companySignTime = this.getCompanySignTime();
        result = result * PRIME + ($companySignTime == null ? 43 : $companySignTime.hashCode());
        final java.lang.Object $employeeSignTime = this.getEmployeeSignTime();
        result = result * PRIME + ($employeeSignTime == null ? 43 : $employeeSignTime.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "EmployeeLaborContract(id=" + this.getId() + ", employeeId=" + this.getEmployeeId() + ", employeeCode=" + this.getEmployeeCode() + ", employeeName=" + this.getEmployeeName() + ", contractNo=" + this.getContractNo() + ", contractType=" + this.getContractType() + ", startDate=" + this.getStartDate() + ", endDate=" + this.getEndDate() + ", probationMonths=" + this.getProbationMonths() + ", probationEndDate=" + this.getProbationEndDate() + ", salary=" + this.getSalary() + ", workLocation=" + this.getWorkLocation() + ", position=" + this.getPosition() + ", status=" + this.getStatus() + ", signDate=" + this.getSignDate() + ", signMethod=" + this.getSignMethod() + ", contractFileUrl=" + this.getContractFileUrl() + ", remark=" + this.getRemark() + ", archiveId=" + this.getArchiveId() + ", templateId=" + this.getTemplateId() + ", generatedTime=" + this.getGeneratedTime() + ", pdfFileUrl=" + this.getPdfFileUrl() + ", signedPdfUrl=" + this.getSignedPdfUrl() + ", companySignTime=" + this.getCompanySignTime() + ", employeeSignTime=" + this.getEmployeeSignTime() + ", createBy=" + this.getCreateBy() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ")";
    }
}
