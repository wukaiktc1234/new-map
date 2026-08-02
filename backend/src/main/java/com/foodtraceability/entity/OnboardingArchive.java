package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 入职档案实体类
 * 对应数据库表：onboarding_archive
 *
 * 【V2.2 新版本】入职档案模块 - 档案管理
 * 创建日期：2026-01-31
 * 用于：管理候选人入职档案
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-01-31
 */
@TableName("onboarding_archive")
@Schema(description = "入职档案实体")
public class OnboardingArchive {
    @TableId(type = IdType.AUTO)
    @Schema(description = "档案ID")
    private Long id;
    @Schema(description = "候选人ID（来自招聘系统）")
    private String candidateId;
    @Schema(description = "候选人姓名")
    private String candidateName;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "身份证号")
    private String idCard;
    @Schema(description = "职位名称")
    private String position;
    @Schema(description = "职位级别：STAFF-普通员工, MANAGER-主管/经理, DIRECTOR-部门总监, EXECUTIVE-高管")
    private String positionLevel;
    @Schema(description = "部门ID")
    private String departmentId;
    @Schema(description = "部门名称（冗余字段，方便查询）")
    private String departmentName;
    @Schema(description = "角色ID")
    private String roleId;
    @Schema(description = "角色名称（冗余字段）")
    private String roleName;
    @Schema(description = "期望薪资")
    private BigDecimal expectedSalary;
    @Schema(description = "最终薪资")
    private BigDecimal finalSalary;
    @Schema(description = "预计入职日期")
    private LocalDate onboardDate;
    @Schema(description = "现住址")
    private String address;
    @Schema(description = "人员类型：social-社招, school-校招/应届生, intern-实习生, returnee-返聘")
    private String personnelType;
    @Schema(description = "员工编号，格式：EMP+年份+序号，如EMP2024001")
    private String employeeCode;
    @Schema(description = "预设用户名，如zs001")
    private String presetUsername;
    @Schema(description = "关联邀请码ID")
    private Long invitationCodeId;
    @Schema(description = "关联劳动合同ID")
    private Long contractId;
    @Schema(description = "关联用户ID（注册后填充）")
    private Long userId;
    @Schema(description = "状态：CREATED-已创建, PENDING_HR-待HR审查, PENDING_SUBSTANTIVE-待实质审查, APPROVED-已通过, CONTRACT_PENDING-待签合同, CONTRACT_SIGNED-合同已签, REGISTERED-已注册, REJECTED-已拒绝")
    private String status;
    @Schema(description = "创建人（HR）")
    private Long createBy;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记：0-未删除, 1-已删除")
    private Integer deleted;
    // 状态常量
    public static final String STATUS_CREATED = "CREATED";
    public static final String STATUS_PENDING_HR = "PENDING_HR";
    public static final String STATUS_PENDING_SUBSTANTIVE = "PENDING_SUBSTANTIVE";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_CONTRACT_PENDING = "CONTRACT_PENDING";
    public static final String STATUS_CONTRACT_SIGNED = "CONTRACT_SIGNED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_REGISTERED = "REGISTERED";
    // 职位级别常量
    public static final String LEVEL_STAFF = "STAFF";
    public static final String LEVEL_MANAGER = "MANAGER";
    public static final String LEVEL_DIRECTOR = "DIRECTOR";
    public static final String LEVEL_EXECUTIVE = "EXECUTIVE";

    public OnboardingArchive() {
    }

    public Long getId() {
        return this.id;
    }

    public String getCandidateId() {
        return this.candidateId;
    }

    public String getCandidateName() {
        return this.candidateName;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getIdCard() {
        return this.idCard;
    }

    public String getPosition() {
        return this.position;
    }

    public String getPositionLevel() {
        return this.positionLevel;
    }

    public String getDepartmentId() {
        return this.departmentId;
    }

    public String getDepartmentName() {
        return this.departmentName;
    }

    public String getRoleId() {
        return this.roleId;
    }

    public String getRoleName() {
        return this.roleName;
    }

    public BigDecimal getExpectedSalary() {
        return this.expectedSalary;
    }

    public BigDecimal getFinalSalary() {
        return this.finalSalary;
    }

    public LocalDate getOnboardDate() {
        return this.onboardDate;
    }

    public String getAddress() {
        return this.address;
    }

    public String getPersonnelType() {
        return this.personnelType;
    }

    public String getEmployeeCode() {
        return this.employeeCode;
    }

    public String getPresetUsername() {
        return this.presetUsername;
    }

    public Long getInvitationCodeId() {
        return this.invitationCodeId;
    }

    public Long getContractId() {
        return this.contractId;
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getStatus() {
        return this.status;
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

    public void setCandidateId(final String candidateId) {
        this.candidateId = candidateId;
    }

    public void setCandidateName(final String candidateName) {
        this.candidateName = candidateName;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    public void setIdCard(final String idCard) {
        this.idCard = idCard;
    }

    public void setPosition(final String position) {
        this.position = position;
    }

    public void setPositionLevel(final String positionLevel) {
        this.positionLevel = positionLevel;
    }

    public void setDepartmentId(final String departmentId) {
        this.departmentId = departmentId;
    }

    public void setDepartmentName(final String departmentName) {
        this.departmentName = departmentName;
    }

    public void setRoleId(final String roleId) {
        this.roleId = roleId;
    }

    public void setRoleName(final String roleName) {
        this.roleName = roleName;
    }

    public void setExpectedSalary(final BigDecimal expectedSalary) {
        this.expectedSalary = expectedSalary;
    }

    public void setFinalSalary(final BigDecimal finalSalary) {
        this.finalSalary = finalSalary;
    }

    public void setOnboardDate(final LocalDate onboardDate) {
        this.onboardDate = onboardDate;
    }

    public void setEmployeeCode(final String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public void setPresetUsername(final String presetUsername) {
        this.presetUsername = presetUsername;
    }

    public void setInvitationCodeId(final Long invitationCodeId) {
        this.invitationCodeId = invitationCodeId;
    }

    public void setContractId(final Long contractId) {
        this.contractId = contractId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setStatus(final String status) {
        this.status = status;
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
        if (!(o instanceof OnboardingArchive)) return false;
        final OnboardingArchive other = (OnboardingArchive) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$invitationCodeId = this.getInvitationCodeId();
        final java.lang.Object other$invitationCodeId = other.getInvitationCodeId();
        if (this$invitationCodeId == null ? other$invitationCodeId != null : !this$invitationCodeId.equals(other$invitationCodeId)) return false;
        final java.lang.Object this$contractId = this.getContractId();
        final java.lang.Object other$contractId = other.getContractId();
        if (this$contractId == null ? other$contractId != null : !this$contractId.equals(other$contractId)) return false;
        final java.lang.Object this$userId = this.getUserId();
        final java.lang.Object other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !this$userId.equals(other$userId)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$candidateId = this.getCandidateId();
        final java.lang.Object other$candidateId = other.getCandidateId();
        if (this$candidateId == null ? other$candidateId != null : !this$candidateId.equals(other$candidateId)) return false;
        final java.lang.Object this$candidateName = this.getCandidateName();
        final java.lang.Object other$candidateName = other.getCandidateName();
        if (this$candidateName == null ? other$candidateName != null : !this$candidateName.equals(other$candidateName)) return false;
        final java.lang.Object this$email = this.getEmail();
        final java.lang.Object other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) return false;
        final java.lang.Object this$phone = this.getPhone();
        final java.lang.Object other$phone = other.getPhone();
        if (this$phone == null ? other$phone != null : !this$phone.equals(other$phone)) return false;
        final java.lang.Object this$idCard = this.getIdCard();
        final java.lang.Object other$idCard = other.getIdCard();
        if (this$idCard == null ? other$idCard != null : !this$idCard.equals(other$idCard)) return false;
        final java.lang.Object this$position = this.getPosition();
        final java.lang.Object other$position = other.getPosition();
        if (this$position == null ? other$position != null : !this$position.equals(other$position)) return false;
        final java.lang.Object this$positionLevel = this.getPositionLevel();
        final java.lang.Object other$positionLevel = other.getPositionLevel();
        if (this$positionLevel == null ? other$positionLevel != null : !this$positionLevel.equals(other$positionLevel)) return false;
        final java.lang.Object this$departmentId = this.getDepartmentId();
        final java.lang.Object other$departmentId = other.getDepartmentId();
        if (this$departmentId == null ? other$departmentId != null : !this$departmentId.equals(other$departmentId)) return false;
        final java.lang.Object this$departmentName = this.getDepartmentName();
        final java.lang.Object other$departmentName = other.getDepartmentName();
        if (this$departmentName == null ? other$departmentName != null : !this$departmentName.equals(other$departmentName)) return false;
        final java.lang.Object this$roleId = this.getRoleId();
        final java.lang.Object other$roleId = other.getRoleId();
        if (this$roleId == null ? other$roleId != null : !this$roleId.equals(other$roleId)) return false;
        final java.lang.Object this$roleName = this.getRoleName();
        final java.lang.Object other$roleName = other.getRoleName();
        if (this$roleName == null ? other$roleName != null : !this$roleName.equals(other$roleName)) return false;
        final java.lang.Object this$expectedSalary = this.getExpectedSalary();
        final java.lang.Object other$expectedSalary = other.getExpectedSalary();
        if (this$expectedSalary == null ? other$expectedSalary != null : !this$expectedSalary.equals(other$expectedSalary)) return false;
        final java.lang.Object this$finalSalary = this.getFinalSalary();
        final java.lang.Object other$finalSalary = other.getFinalSalary();
        if (this$finalSalary == null ? other$finalSalary != null : !this$finalSalary.equals(other$finalSalary)) return false;
        final java.lang.Object this$onboardDate = this.getOnboardDate();
        final java.lang.Object other$onboardDate = other.getOnboardDate();
        if (this$onboardDate == null ? other$onboardDate != null : !this$onboardDate.equals(other$onboardDate)) return false;
        final java.lang.Object this$employeeCode = this.getEmployeeCode();
        final java.lang.Object other$employeeCode = other.getEmployeeCode();
        if (this$employeeCode == null ? other$employeeCode != null : !this$employeeCode.equals(other$employeeCode)) return false;
        final java.lang.Object this$presetUsername = this.getPresetUsername();
        final java.lang.Object other$presetUsername = other.getPresetUsername();
        if (this$presetUsername == null ? other$presetUsername != null : !this$presetUsername.equals(other$presetUsername)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OnboardingArchive;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $invitationCodeId = this.getInvitationCodeId();
        result = result * PRIME + ($invitationCodeId == null ? 43 : $invitationCodeId.hashCode());
        final java.lang.Object $contractId = this.getContractId();
        result = result * PRIME + ($contractId == null ? 43 : $contractId.hashCode());
        final java.lang.Object $userId = this.getUserId();
        result = result * PRIME + ($userId == null ? 43 : $userId.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $candidateId = this.getCandidateId();
        result = result * PRIME + ($candidateId == null ? 43 : $candidateId.hashCode());
        final java.lang.Object $candidateName = this.getCandidateName();
        result = result * PRIME + ($candidateName == null ? 43 : $candidateName.hashCode());
        final java.lang.Object $email = this.getEmail();
        result = result * PRIME + ($email == null ? 43 : $email.hashCode());
        final java.lang.Object $phone = this.getPhone();
        result = result * PRIME + ($phone == null ? 43 : $phone.hashCode());
        final java.lang.Object $idCard = this.getIdCard();
        result = result * PRIME + ($idCard == null ? 43 : $idCard.hashCode());
        final java.lang.Object $position = this.getPosition();
        result = result * PRIME + ($position == null ? 43 : $position.hashCode());
        final java.lang.Object $positionLevel = this.getPositionLevel();
        result = result * PRIME + ($positionLevel == null ? 43 : $positionLevel.hashCode());
        final java.lang.Object $departmentId = this.getDepartmentId();
        result = result * PRIME + ($departmentId == null ? 43 : $departmentId.hashCode());
        final java.lang.Object $departmentName = this.getDepartmentName();
        result = result * PRIME + ($departmentName == null ? 43 : $departmentName.hashCode());
        final java.lang.Object $roleId = this.getRoleId();
        result = result * PRIME + ($roleId == null ? 43 : $roleId.hashCode());
        final java.lang.Object $roleName = this.getRoleName();
        result = result * PRIME + ($roleName == null ? 43 : $roleName.hashCode());
        final java.lang.Object $expectedSalary = this.getExpectedSalary();
        result = result * PRIME + ($expectedSalary == null ? 43 : $expectedSalary.hashCode());
        final java.lang.Object $finalSalary = this.getFinalSalary();
        result = result * PRIME + ($finalSalary == null ? 43 : $finalSalary.hashCode());
        final java.lang.Object $onboardDate = this.getOnboardDate();
        result = result * PRIME + ($onboardDate == null ? 43 : $onboardDate.hashCode());
        final java.lang.Object $employeeCode = this.getEmployeeCode();
        result = result * PRIME + ($employeeCode == null ? 43 : $employeeCode.hashCode());
        final java.lang.Object $presetUsername = this.getPresetUsername();
        result = result * PRIME + ($presetUsername == null ? 43 : $presetUsername.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OnboardingArchive(id=" + this.getId() + ", candidateId=" + this.getCandidateId() + ", candidateName=" + this.getCandidateName() + ", email=" + this.getEmail() + ", phone=" + this.getPhone() + ", idCard=" + this.getIdCard() + ", position=" + this.getPosition() + ", positionLevel=" + this.getPositionLevel() + ", departmentId=" + this.getDepartmentId() + ", departmentName=" + this.getDepartmentName() + ", roleId=" + this.getRoleId() + ", roleName=" + this.getRoleName() + ", expectedSalary=" + this.getExpectedSalary() + ", finalSalary=" + this.getFinalSalary() + ", onboardDate=" + this.getOnboardDate() + ", employeeCode=" + this.getEmployeeCode() + ", presetUsername=" + this.getPresetUsername() + ", invitationCodeId=" + this.getInvitationCodeId() + ", contractId=" + this.getContractId() + ", userId=" + this.getUserId() + ", status=" + this.getStatus() + ", createBy=" + this.getCreateBy() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ")";
    }
}
