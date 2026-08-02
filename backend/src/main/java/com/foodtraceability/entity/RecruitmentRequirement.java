package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("recruitment_requirements")
@Schema(description = "招聘需求实体")
public class RecruitmentRequirement {
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;
    @Schema(description = "需求编号")
    private String requirementCode;
    @Schema(description = "职位名称")
    private String positionName;
    @Schema(description = "部门ID")
    private String departmentId;
    @Schema(description = "部门名称")
    private String departmentName;
    @Schema(description = "门店ID")
    private String storeId;
    @Schema(description = "门店名称")
    private String storeName;
    @Schema(description = "职位ID")
    private String positionId;
    @Schema(description = "需求人数")
    private Integer requirementNum;
    @Schema(description = "需求类型（hr：人事发布，store：门店提报）")
    private String type;
    @Schema(description = "状态（open：进行中，closed：已关闭，filled：已招满）")
    private String status;
    @Schema(description = "审批状态（pending：待审批，approved：已通过，rejected：已拒绝）")
    private String approvalStatus;
    @Schema(description = "职位描述")
    private String description;
    @Schema(description = "任职要求")
    private String requirements;
    @Schema(description = "薪资范围")
    private String salaryRange;
    @Schema(description = "学历要求")
    private String educationRequirement;
    @Schema(description = "工作地点")
    private String workLocation;
    @Schema(description = "申请截止时间")
    private LocalDateTime applyDeadline;
    @Schema(description = "创建人ID")
    private String createdBy;
    @Schema(description = "创建人姓名")
    private String createdByName;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记（0未删除 1已删除）")
    private Integer deleted;

    @Schema(description = "关联招聘名额ID(可空,门店提报时必填,HR提报时可空)")
    private Long quotaId;

    @TableField(exist = false)
    @Schema(description = "应聘人数（统计）")
    private Integer applicantCount;

    @TableField(exist = false)
    @Schema(description = "已录用人数（统计）")
    private Integer hiredCount;

    // Getter methods
    public String getId() {
        return id;
    }

    public String getRequirementCode() {
        return requirementCode;
    }

    public String getPositionName() {
        return positionName;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getStoreId() {
        return storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getPositionId() {
        return positionId;
    }

    public Integer getRequirementNum() {
        return requirementNum;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public String getDescription() {
        return description;
    }

    public String getRequirements() {
        return requirements;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public String getEducationRequirement() {
        return educationRequirement;
    }

    public String getWorkLocation() {
        return workLocation;
    }

    public LocalDateTime getApplyDeadline() {
        return applyDeadline;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public Long getQuotaId() {
        return quotaId;
    }

    public Integer getApplicantCount() {
        return applicantCount;
    }

    public Integer getHiredCount() {
        return hiredCount;
    }

    // Setter methods
    public void setId(String id) {
        this.id = id;
    }

    public void setRequirementCode(String requirementCode) {
        this.requirementCode = requirementCode;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public void setRequirementNum(Integer requirementNum) {
        this.requirementNum = requirementNum;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public void setEducationRequirement(String educationRequirement) {
        this.educationRequirement = educationRequirement;
    }

    public void setWorkLocation(String workLocation) {
        this.workLocation = workLocation;
    }

    public void setApplyDeadline(LocalDateTime applyDeadline) {
        this.applyDeadline = applyDeadline;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public void setQuotaId(Long quotaId) {
        this.quotaId = quotaId;
    }

    public void setApplicantCount(Integer applicantCount) {
        this.applicantCount = applicantCount;
    }

    public void setHiredCount(Integer hiredCount) {
        this.hiredCount = hiredCount;
    }

    public RecruitmentRequirement() {
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof RecruitmentRequirement)) return false;
        final RecruitmentRequirement other = (RecruitmentRequirement) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$requirementNum = this.getRequirementNum();
        final java.lang.Object other$requirementNum = other.getRequirementNum();
        if (this$requirementNum == null ? other$requirementNum != null : !this$requirementNum.equals(other$requirementNum)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$requirementCode = this.getRequirementCode();
        final java.lang.Object other$requirementCode = other.getRequirementCode();
        if (this$requirementCode == null ? other$requirementCode != null : !this$requirementCode.equals(other$requirementCode)) return false;
        final java.lang.Object this$positionName = this.getPositionName();
        final java.lang.Object other$positionName = other.getPositionName();
        if (this$positionName == null ? other$positionName != null : !this$positionName.equals(other$positionName)) return false;
        final java.lang.Object this$departmentId = this.getDepartmentId();
        final java.lang.Object other$departmentId = other.getDepartmentId();
        if (this$departmentId == null ? other$departmentId != null : !this$departmentId.equals(other$departmentId)) return false;
        final java.lang.Object this$departmentName = this.getDepartmentName();
        final java.lang.Object other$departmentName = other.getDepartmentName();
        if (this$departmentName == null ? other$departmentName != null : !this$departmentName.equals(other$departmentName)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$positionId = this.getPositionId();
        final java.lang.Object other$positionId = other.getPositionId();
        if (this$positionId == null ? other$positionId != null : !this$positionId.equals(other$positionId)) return false;
        final java.lang.Object this$type = this.getType();
        final java.lang.Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$approvalStatus = this.getApprovalStatus();
        final java.lang.Object other$approvalStatus = other.getApprovalStatus();
        if (this$approvalStatus == null ? other$approvalStatus != null : !this$approvalStatus.equals(other$approvalStatus)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$requirements = this.getRequirements();
        final java.lang.Object other$requirements = other.getRequirements();
        if (this$requirements == null ? other$requirements != null : !this$requirements.equals(other$requirements)) return false;
        final java.lang.Object this$salaryRange = this.getSalaryRange();
        final java.lang.Object other$salaryRange = other.getSalaryRange();
        if (this$salaryRange == null ? other$salaryRange != null : !this$salaryRange.equals(other$salaryRange)) return false;
        final java.lang.Object this$workLocation = this.getWorkLocation();
        final java.lang.Object other$workLocation = other.getWorkLocation();
        if (this$workLocation == null ? other$workLocation != null : !this$workLocation.equals(other$workLocation)) return false;
        final java.lang.Object this$applyDeadline = this.getApplyDeadline();
        final java.lang.Object other$applyDeadline = other.getApplyDeadline();
        if (this$applyDeadline == null ? other$applyDeadline != null : !this$applyDeadline.equals(other$applyDeadline)) return false;
        final java.lang.Object this$createdBy = this.getCreatedBy();
        final java.lang.Object other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) return false;
        final java.lang.Object this$createdByName = this.getCreatedByName();
        final java.lang.Object other$createdByName = other.getCreatedByName();
        if (this$createdByName == null ? other$createdByName != null : !this$createdByName.equals(other$createdByName)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RecruitmentRequirement;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $requirementNum = this.getRequirementNum();
        result = result * PRIME + ($requirementNum == null ? 43 : $requirementNum.hashCode());
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $requirementCode = this.getRequirementCode();
        result = result * PRIME + ($requirementCode == null ? 43 : $requirementCode.hashCode());
        final java.lang.Object $positionName = this.getPositionName();
        result = result * PRIME + ($positionName == null ? 43 : $positionName.hashCode());
        final java.lang.Object $departmentId = this.getDepartmentId();
        result = result * PRIME + ($departmentId == null ? 43 : $departmentId.hashCode());
        final java.lang.Object $departmentName = this.getDepartmentName();
        result = result * PRIME + ($departmentName == null ? 43 : $departmentName.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $positionId = this.getPositionId();
        result = result * PRIME + ($positionId == null ? 43 : $positionId.hashCode());
        final java.lang.Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $approvalStatus = this.getApprovalStatus();
        result = result * PRIME + ($approvalStatus == null ? 43 : $approvalStatus.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $requirements = this.getRequirements();
        result = result * PRIME + ($requirements == null ? 43 : $requirements.hashCode());
        final java.lang.Object $salaryRange = this.getSalaryRange();
        result = result * PRIME + ($salaryRange == null ? 43 : $salaryRange.hashCode());
        final java.lang.Object $workLocation = this.getWorkLocation();
        result = result * PRIME + ($workLocation == null ? 43 : $workLocation.hashCode());
        final java.lang.Object $applyDeadline = this.getApplyDeadline();
        result = result * PRIME + ($applyDeadline == null ? 43 : $applyDeadline.hashCode());
        final java.lang.Object $createdBy = this.getCreatedBy();
        result = result * PRIME + ($createdBy == null ? 43 : $createdBy.hashCode());
        final java.lang.Object $createdByName = this.getCreatedByName();
        result = result * PRIME + ($createdByName == null ? 43 : $createdByName.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "RecruitmentRequirement(id=" + this.getId() + ", requirementCode=" + this.getRequirementCode() + ", positionName=" + this.getPositionName() + ", departmentId=" + this.getDepartmentId() + ", departmentName=" + this.getDepartmentName() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", positionId=" + this.getPositionId() + ", requirementNum=" + this.getRequirementNum() + ", type=" + this.getType() + ", status=" + this.getStatus() + ", approvalStatus=" + this.getApprovalStatus() + ", description=" + this.getDescription() + ", requirements=" + this.getRequirements() + ", salaryRange=" + this.getSalaryRange() + ", workLocation=" + this.getWorkLocation() + ", applyDeadline=" + this.getApplyDeadline() + ", createdBy=" + this.getCreatedBy() + ", createdByName=" + this.getCreatedByName() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ")";
    }
}
