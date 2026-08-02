package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 入职档案DTO
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-01-31
 */
@Schema(description = "入职档案DTO")
public class OnboardingArchiveDTO {
    @Schema(description = "档案ID（创建时不需要）")
    private Long id;
    @NotBlank(message = "候选人姓名不能为空")
    @Schema(description = "候选人姓名", required = true)
    private String candidateName;
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱", required = true)
    private String email;
    @Schema(description = "手机号")
    private String phone;
    @Schema(description = "身份证号")
    private String idCard;
    @NotBlank(message = "职位不能为空")
    @Schema(description = "职位名称", required = true)
    private String position;
    @NotBlank(message = "职位级别不能为空")
    @Schema(description = "职位级别：STAFF-普通员工, MANAGER-主管/经理, DIRECTOR-部门总监, EXECUTIVE-高管", required = true)
    private String positionLevel;
    @NotBlank(message = "部门ID不能为空")
    @Schema(description = "部门ID", required = true)
    private String departmentId;
    @Schema(description = "部门名称")
    private String departmentName;
    @Schema(description = "角色ID")
    private String roleId;
    @Schema(description = "角色名称")
    private String roleName;
    @Schema(description = "期望薪资")
    private BigDecimal expectedSalary;
    @Schema(description = "最终薪资")
    private BigDecimal finalSalary;
    @Schema(description = "预计入职日期")
    private LocalDate onboardDate;

    public OnboardingArchiveDTO() {
    }

    public Long getId() {
        return this.id;
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

    public void setId(final Long id) {
        this.id = id;
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

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OnboardingArchiveDTO)) return false;
        final OnboardingArchiveDTO other = (OnboardingArchiveDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
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
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OnboardingArchiveDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
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
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OnboardingArchiveDTO(id=" + this.getId() + ", candidateName=" + this.getCandidateName() + ", email=" + this.getEmail() + ", phone=" + this.getPhone() + ", idCard=" + this.getIdCard() + ", position=" + this.getPosition() + ", positionLevel=" + this.getPositionLevel() + ", departmentId=" + this.getDepartmentId() + ", departmentName=" + this.getDepartmentName() + ", roleId=" + this.getRoleId() + ", roleName=" + this.getRoleName() + ", expectedSalary=" + this.getExpectedSalary() + ", finalSalary=" + this.getFinalSalary() + ", onboardDate=" + this.getOnboardDate() + ")";
    }
}
