package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工档案详细实体类
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@TableName("employee_archive_detail")
@Schema(description = "员工档案详细实体")
public class EmployeeArchiveDetail {
    @TableId(type = IdType.AUTO)
    @Schema(description = "档案详情ID")
    private Long id;
    @Schema(description = "关联入职档案ID")
    private Long archiveId;
    @Schema(description = "关联员工ID")
    private String employeeId;
    @Schema(description = "真实姓名")
    private String realName;
    @Schema(description = "性别: male-男, female-女, other-其他")
    private String gender;
    @Schema(description = "出生日期")
    private LocalDate birthday;
    @Schema(description = "民族")
    private String nation;
    @Schema(description = "政治面貌")
    private String politicalStatus;
    @Schema(description = "婚姻状况: single-未婚, married-已婚, divorced-离异, widowed-丧偶")
    private String maritalStatus;
    @Schema(description = "籍贯")
    private String nativePlace;
    @Schema(description = "户籍地址")
    private String residenceAddress;
    @Schema(description = "现居住地址")
    private String currentAddress;
    @Schema(description = "最高学历")
    private String educationLevel;
    @Schema(description = "毕业院校")
    private String graduationSchool;
    @Schema(description = "专业")
    private String major;
    @Schema(description = "毕业时间")
    private LocalDate graduationDate;
    @Schema(description = "学位")
    private String degree;
    @Schema(description = "工作经历（JSON）")
    private String workExperience;
    @Schema(description = "家庭成员（JSON）")
    private String familyMembers;
    @Schema(description = "紧急联系人")
    private String emergencyContact;
    @Schema(description = "与本人关系")
    private String emergencyRelationship;
    @Schema(description = "紧急联系电话")
    private String emergencyPhone;
    @Schema(description = "紧急联系人地址")
    private String emergencyAddress;
    @Schema(description = "开户银行")
    private String bankName;
    @Schema(description = "开户支行")
    private String bankBranch;
    @Schema(description = "银行卡号")
    private String bankCard;
    @Schema(description = "社保账号")
    private String socialSecurityNo;
    @Schema(description = "公积金账号")
    private String housingFundNo;
    @Schema(description = "身份证正面照URL")
    private String idCardFrontUrl;
    @Schema(description = "身份证背面照URL")
    private String idCardBackUrl;
    @Schema(description = "学历证书URL")
    private String diplomaUrl;
    @Schema(description = "学位证书URL")
    private String degreeCertificateUrl;
    @Schema(description = "个人照片URL")
    private String photoUrl;
    @Schema(description = "其他附件（JSON）")
    private String otherAttachments;
    @Schema(description = "隐私协议签署: 0-未签署, 1-已签署")
    private Integer privacyAgreementSigned;
    @Schema(description = "隐私协议签署时间")
    private LocalDateTime privacyAgreementTime;
    @Schema(description = "数据准确性确认: 0-未确认, 1-已确认")
    private Integer dataAccuracyConfirmed;
    @Schema(description = "数据确认时间")
    private LocalDateTime dataConfirmTime;
    @Schema(description = "档案完整度评分（0-100）")
    private Integer completenessScore;
    @Schema(description = "缺失字段列表（JSON）")
    private String missingFields;
    @Schema(description = "审核状态: pending-待审核, approved-已通过, rejected-已拒绝")
    private String reviewStatus;
    @Schema(description = "审核人")
    private Long reviewBy;
    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;
    @Schema(description = "审核意见")
    private String reviewComment;
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
    @Schema(description = "逻辑删除标记")
    private Integer deleted;
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";

    public EmployeeArchiveDetail() {
    }

    public Long getId() {
        return this.id;
    }

    public Long getArchiveId() {
        return this.archiveId;
    }

    public String getEmployeeId() {
        return this.employeeId;
    }

    public String getRealName() {
        return this.realName;
    }

    public String getGender() {
        return this.gender;
    }

    public LocalDate getBirthday() {
        return this.birthday;
    }

    public String getNation() {
        return this.nation;
    }

    public String getPoliticalStatus() {
        return this.politicalStatus;
    }

    public String getMaritalStatus() {
        return this.maritalStatus;
    }

    public String getNativePlace() {
        return this.nativePlace;
    }

    public String getResidenceAddress() {
        return this.residenceAddress;
    }

    public String getCurrentAddress() {
        return this.currentAddress;
    }

    public String getEducationLevel() {
        return this.educationLevel;
    }

    public String getGraduationSchool() {
        return this.graduationSchool;
    }

    public String getMajor() {
        return this.major;
    }

    public LocalDate getGraduationDate() {
        return this.graduationDate;
    }

    public String getDegree() {
        return this.degree;
    }

    public String getWorkExperience() {
        return this.workExperience;
    }

    public String getFamilyMembers() {
        return this.familyMembers;
    }

    public String getEmergencyContact() {
        return this.emergencyContact;
    }

    public String getEmergencyRelationship() {
        return this.emergencyRelationship;
    }

    public String getEmergencyPhone() {
        return this.emergencyPhone;
    }

    public String getEmergencyAddress() {
        return this.emergencyAddress;
    }

    public String getBankName() {
        return this.bankName;
    }

    public String getBankBranch() {
        return this.bankBranch;
    }

    public String getBankCard() {
        return this.bankCard;
    }

    public String getSocialSecurityNo() {
        return this.socialSecurityNo;
    }

    public String getHousingFundNo() {
        return this.housingFundNo;
    }

    public String getIdCardFrontUrl() {
        return this.idCardFrontUrl;
    }

    public String getIdCardBackUrl() {
        return this.idCardBackUrl;
    }

    public String getDiplomaUrl() {
        return this.diplomaUrl;
    }

    public String getDegreeCertificateUrl() {
        return this.degreeCertificateUrl;
    }

    public String getPhotoUrl() {
        return this.photoUrl;
    }

    public String getOtherAttachments() {
        return this.otherAttachments;
    }

    public Integer getPrivacyAgreementSigned() {
        return this.privacyAgreementSigned;
    }

    public LocalDateTime getPrivacyAgreementTime() {
        return this.privacyAgreementTime;
    }

    public Integer getDataAccuracyConfirmed() {
        return this.dataAccuracyConfirmed;
    }

    public LocalDateTime getDataConfirmTime() {
        return this.dataConfirmTime;
    }

    public Integer getCompletenessScore() {
        return this.completenessScore;
    }

    public String getMissingFields() {
        return this.missingFields;
    }

    public String getReviewStatus() {
        return this.reviewStatus;
    }

    public Long getReviewBy() {
        return this.reviewBy;
    }

    public LocalDateTime getReviewTime() {
        return this.reviewTime;
    }

    public String getReviewComment() {
        return this.reviewComment;
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

    public void setArchiveId(final Long archiveId) {
        this.archiveId = archiveId;
    }

    public void setEmployeeId(final String employeeId) {
        this.employeeId = employeeId;
    }

    public void setRealName(final String realName) {
        this.realName = realName;
    }

    public void setGender(final String gender) {
        this.gender = gender;
    }

    public void setBirthday(final LocalDate birthday) {
        this.birthday = birthday;
    }

    public void setNation(final String nation) {
        this.nation = nation;
    }

    public void setPoliticalStatus(final String politicalStatus) {
        this.politicalStatus = politicalStatus;
    }

    public void setMaritalStatus(final String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public void setNativePlace(final String nativePlace) {
        this.nativePlace = nativePlace;
    }

    public void setResidenceAddress(final String residenceAddress) {
        this.residenceAddress = residenceAddress;
    }

    public void setCurrentAddress(final String currentAddress) {
        this.currentAddress = currentAddress;
    }

    public void setEducationLevel(final String educationLevel) {
        this.educationLevel = educationLevel;
    }

    public void setGraduationSchool(final String graduationSchool) {
        this.graduationSchool = graduationSchool;
    }

    public void setMajor(final String major) {
        this.major = major;
    }

    public void setGraduationDate(final LocalDate graduationDate) {
        this.graduationDate = graduationDate;
    }

    public void setDegree(final String degree) {
        this.degree = degree;
    }

    public void setWorkExperience(final String workExperience) {
        this.workExperience = workExperience;
    }

    public void setFamilyMembers(final String familyMembers) {
        this.familyMembers = familyMembers;
    }

    public void setEmergencyContact(final String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public void setEmergencyRelationship(final String emergencyRelationship) {
        this.emergencyRelationship = emergencyRelationship;
    }

    public void setEmergencyPhone(final String emergencyPhone) {
        this.emergencyPhone = emergencyPhone;
    }

    public void setEmergencyAddress(final String emergencyAddress) {
        this.emergencyAddress = emergencyAddress;
    }

    public void setBankName(final String bankName) {
        this.bankName = bankName;
    }

    public void setBankBranch(final String bankBranch) {
        this.bankBranch = bankBranch;
    }

    public void setBankCard(final String bankCard) {
        this.bankCard = bankCard;
    }

    public void setSocialSecurityNo(final String socialSecurityNo) {
        this.socialSecurityNo = socialSecurityNo;
    }

    public void setHousingFundNo(final String housingFundNo) {
        this.housingFundNo = housingFundNo;
    }

    public void setIdCardFrontUrl(final String idCardFrontUrl) {
        this.idCardFrontUrl = idCardFrontUrl;
    }

    public void setIdCardBackUrl(final String idCardBackUrl) {
        this.idCardBackUrl = idCardBackUrl;
    }

    public void setDiplomaUrl(final String diplomaUrl) {
        this.diplomaUrl = diplomaUrl;
    }

    public void setDegreeCertificateUrl(final String degreeCertificateUrl) {
        this.degreeCertificateUrl = degreeCertificateUrl;
    }

    public void setPhotoUrl(final String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setOtherAttachments(final String otherAttachments) {
        this.otherAttachments = otherAttachments;
    }

    public void setPrivacyAgreementSigned(final Integer privacyAgreementSigned) {
        this.privacyAgreementSigned = privacyAgreementSigned;
    }

    public void setPrivacyAgreementTime(final LocalDateTime privacyAgreementTime) {
        this.privacyAgreementTime = privacyAgreementTime;
    }

    public void setDataAccuracyConfirmed(final Integer dataAccuracyConfirmed) {
        this.dataAccuracyConfirmed = dataAccuracyConfirmed;
    }

    public void setDataConfirmTime(final LocalDateTime dataConfirmTime) {
        this.dataConfirmTime = dataConfirmTime;
    }

    public void setCompletenessScore(final Integer completenessScore) {
        this.completenessScore = completenessScore;
    }

    public void setMissingFields(final String missingFields) {
        this.missingFields = missingFields;
    }

    public void setReviewStatus(final String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public void setReviewBy(final Long reviewBy) {
        this.reviewBy = reviewBy;
    }

    public void setReviewTime(final LocalDateTime reviewTime) {
        this.reviewTime = reviewTime;
    }

    public void setReviewComment(final String reviewComment) {
        this.reviewComment = reviewComment;
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
        if (!(o instanceof EmployeeArchiveDetail)) return false;
        final EmployeeArchiveDetail other = (EmployeeArchiveDetail) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$archiveId = this.getArchiveId();
        final java.lang.Object other$archiveId = other.getArchiveId();
        if (this$archiveId == null ? other$archiveId != null : !this$archiveId.equals(other$archiveId)) return false;
        final java.lang.Object this$privacyAgreementSigned = this.getPrivacyAgreementSigned();
        final java.lang.Object other$privacyAgreementSigned = other.getPrivacyAgreementSigned();
        if (this$privacyAgreementSigned == null ? other$privacyAgreementSigned != null : !this$privacyAgreementSigned.equals(other$privacyAgreementSigned)) return false;
        final java.lang.Object this$dataAccuracyConfirmed = this.getDataAccuracyConfirmed();
        final java.lang.Object other$dataAccuracyConfirmed = other.getDataAccuracyConfirmed();
        if (this$dataAccuracyConfirmed == null ? other$dataAccuracyConfirmed != null : !this$dataAccuracyConfirmed.equals(other$dataAccuracyConfirmed)) return false;
        final java.lang.Object this$completenessScore = this.getCompletenessScore();
        final java.lang.Object other$completenessScore = other.getCompletenessScore();
        if (this$completenessScore == null ? other$completenessScore != null : !this$completenessScore.equals(other$completenessScore)) return false;
        final java.lang.Object this$reviewBy = this.getReviewBy();
        final java.lang.Object other$reviewBy = other.getReviewBy();
        if (this$reviewBy == null ? other$reviewBy != null : !this$reviewBy.equals(other$reviewBy)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$employeeId = this.getEmployeeId();
        final java.lang.Object other$employeeId = other.getEmployeeId();
        if (this$employeeId == null ? other$employeeId != null : !this$employeeId.equals(other$employeeId)) return false;
        final java.lang.Object this$realName = this.getRealName();
        final java.lang.Object other$realName = other.getRealName();
        if (this$realName == null ? other$realName != null : !this$realName.equals(other$realName)) return false;
        final java.lang.Object this$gender = this.getGender();
        final java.lang.Object other$gender = other.getGender();
        if (this$gender == null ? other$gender != null : !this$gender.equals(other$gender)) return false;
        final java.lang.Object this$birthday = this.getBirthday();
        final java.lang.Object other$birthday = other.getBirthday();
        if (this$birthday == null ? other$birthday != null : !this$birthday.equals(other$birthday)) return false;
        final java.lang.Object this$nation = this.getNation();
        final java.lang.Object other$nation = other.getNation();
        if (this$nation == null ? other$nation != null : !this$nation.equals(other$nation)) return false;
        final java.lang.Object this$politicalStatus = this.getPoliticalStatus();
        final java.lang.Object other$politicalStatus = other.getPoliticalStatus();
        if (this$politicalStatus == null ? other$politicalStatus != null : !this$politicalStatus.equals(other$politicalStatus)) return false;
        final java.lang.Object this$maritalStatus = this.getMaritalStatus();
        final java.lang.Object other$maritalStatus = other.getMaritalStatus();
        if (this$maritalStatus == null ? other$maritalStatus != null : !this$maritalStatus.equals(other$maritalStatus)) return false;
        final java.lang.Object this$nativePlace = this.getNativePlace();
        final java.lang.Object other$nativePlace = other.getNativePlace();
        if (this$nativePlace == null ? other$nativePlace != null : !this$nativePlace.equals(other$nativePlace)) return false;
        final java.lang.Object this$residenceAddress = this.getResidenceAddress();
        final java.lang.Object other$residenceAddress = other.getResidenceAddress();
        if (this$residenceAddress == null ? other$residenceAddress != null : !this$residenceAddress.equals(other$residenceAddress)) return false;
        final java.lang.Object this$currentAddress = this.getCurrentAddress();
        final java.lang.Object other$currentAddress = other.getCurrentAddress();
        if (this$currentAddress == null ? other$currentAddress != null : !this$currentAddress.equals(other$currentAddress)) return false;
        final java.lang.Object this$educationLevel = this.getEducationLevel();
        final java.lang.Object other$educationLevel = other.getEducationLevel();
        if (this$educationLevel == null ? other$educationLevel != null : !this$educationLevel.equals(other$educationLevel)) return false;
        final java.lang.Object this$graduationSchool = this.getGraduationSchool();
        final java.lang.Object other$graduationSchool = other.getGraduationSchool();
        if (this$graduationSchool == null ? other$graduationSchool != null : !this$graduationSchool.equals(other$graduationSchool)) return false;
        final java.lang.Object this$major = this.getMajor();
        final java.lang.Object other$major = other.getMajor();
        if (this$major == null ? other$major != null : !this$major.equals(other$major)) return false;
        final java.lang.Object this$graduationDate = this.getGraduationDate();
        final java.lang.Object other$graduationDate = other.getGraduationDate();
        if (this$graduationDate == null ? other$graduationDate != null : !this$graduationDate.equals(other$graduationDate)) return false;
        final java.lang.Object this$degree = this.getDegree();
        final java.lang.Object other$degree = other.getDegree();
        if (this$degree == null ? other$degree != null : !this$degree.equals(other$degree)) return false;
        final java.lang.Object this$workExperience = this.getWorkExperience();
        final java.lang.Object other$workExperience = other.getWorkExperience();
        if (this$workExperience == null ? other$workExperience != null : !this$workExperience.equals(other$workExperience)) return false;
        final java.lang.Object this$familyMembers = this.getFamilyMembers();
        final java.lang.Object other$familyMembers = other.getFamilyMembers();
        if (this$familyMembers == null ? other$familyMembers != null : !this$familyMembers.equals(other$familyMembers)) return false;
        final java.lang.Object this$emergencyContact = this.getEmergencyContact();
        final java.lang.Object other$emergencyContact = other.getEmergencyContact();
        if (this$emergencyContact == null ? other$emergencyContact != null : !this$emergencyContact.equals(other$emergencyContact)) return false;
        final java.lang.Object this$emergencyRelationship = this.getEmergencyRelationship();
        final java.lang.Object other$emergencyRelationship = other.getEmergencyRelationship();
        if (this$emergencyRelationship == null ? other$emergencyRelationship != null : !this$emergencyRelationship.equals(other$emergencyRelationship)) return false;
        final java.lang.Object this$emergencyPhone = this.getEmergencyPhone();
        final java.lang.Object other$emergencyPhone = other.getEmergencyPhone();
        if (this$emergencyPhone == null ? other$emergencyPhone != null : !this$emergencyPhone.equals(other$emergencyPhone)) return false;
        final java.lang.Object this$emergencyAddress = this.getEmergencyAddress();
        final java.lang.Object other$emergencyAddress = other.getEmergencyAddress();
        if (this$emergencyAddress == null ? other$emergencyAddress != null : !this$emergencyAddress.equals(other$emergencyAddress)) return false;
        final java.lang.Object this$bankName = this.getBankName();
        final java.lang.Object other$bankName = other.getBankName();
        if (this$bankName == null ? other$bankName != null : !this$bankName.equals(other$bankName)) return false;
        final java.lang.Object this$bankBranch = this.getBankBranch();
        final java.lang.Object other$bankBranch = other.getBankBranch();
        if (this$bankBranch == null ? other$bankBranch != null : !this$bankBranch.equals(other$bankBranch)) return false;
        final java.lang.Object this$bankCard = this.getBankCard();
        final java.lang.Object other$bankCard = other.getBankCard();
        if (this$bankCard == null ? other$bankCard != null : !this$bankCard.equals(other$bankCard)) return false;
        final java.lang.Object this$socialSecurityNo = this.getSocialSecurityNo();
        final java.lang.Object other$socialSecurityNo = other.getSocialSecurityNo();
        if (this$socialSecurityNo == null ? other$socialSecurityNo != null : !this$socialSecurityNo.equals(other$socialSecurityNo)) return false;
        final java.lang.Object this$housingFundNo = this.getHousingFundNo();
        final java.lang.Object other$housingFundNo = other.getHousingFundNo();
        if (this$housingFundNo == null ? other$housingFundNo != null : !this$housingFundNo.equals(other$housingFundNo)) return false;
        final java.lang.Object this$idCardFrontUrl = this.getIdCardFrontUrl();
        final java.lang.Object other$idCardFrontUrl = other.getIdCardFrontUrl();
        if (this$idCardFrontUrl == null ? other$idCardFrontUrl != null : !this$idCardFrontUrl.equals(other$idCardFrontUrl)) return false;
        final java.lang.Object this$idCardBackUrl = this.getIdCardBackUrl();
        final java.lang.Object other$idCardBackUrl = other.getIdCardBackUrl();
        if (this$idCardBackUrl == null ? other$idCardBackUrl != null : !this$idCardBackUrl.equals(other$idCardBackUrl)) return false;
        final java.lang.Object this$diplomaUrl = this.getDiplomaUrl();
        final java.lang.Object other$diplomaUrl = other.getDiplomaUrl();
        if (this$diplomaUrl == null ? other$diplomaUrl != null : !this$diplomaUrl.equals(other$diplomaUrl)) return false;
        final java.lang.Object this$degreeCertificateUrl = this.getDegreeCertificateUrl();
        final java.lang.Object other$degreeCertificateUrl = other.getDegreeCertificateUrl();
        if (this$degreeCertificateUrl == null ? other$degreeCertificateUrl != null : !this$degreeCertificateUrl.equals(other$degreeCertificateUrl)) return false;
        final java.lang.Object this$photoUrl = this.getPhotoUrl();
        final java.lang.Object other$photoUrl = other.getPhotoUrl();
        if (this$photoUrl == null ? other$photoUrl != null : !this$photoUrl.equals(other$photoUrl)) return false;
        final java.lang.Object this$otherAttachments = this.getOtherAttachments();
        final java.lang.Object other$otherAttachments = other.getOtherAttachments();
        if (this$otherAttachments == null ? other$otherAttachments != null : !this$otherAttachments.equals(other$otherAttachments)) return false;
        final java.lang.Object this$privacyAgreementTime = this.getPrivacyAgreementTime();
        final java.lang.Object other$privacyAgreementTime = other.getPrivacyAgreementTime();
        if (this$privacyAgreementTime == null ? other$privacyAgreementTime != null : !this$privacyAgreementTime.equals(other$privacyAgreementTime)) return false;
        final java.lang.Object this$dataConfirmTime = this.getDataConfirmTime();
        final java.lang.Object other$dataConfirmTime = other.getDataConfirmTime();
        if (this$dataConfirmTime == null ? other$dataConfirmTime != null : !this$dataConfirmTime.equals(other$dataConfirmTime)) return false;
        final java.lang.Object this$missingFields = this.getMissingFields();
        final java.lang.Object other$missingFields = other.getMissingFields();
        if (this$missingFields == null ? other$missingFields != null : !this$missingFields.equals(other$missingFields)) return false;
        final java.lang.Object this$reviewStatus = this.getReviewStatus();
        final java.lang.Object other$reviewStatus = other.getReviewStatus();
        if (this$reviewStatus == null ? other$reviewStatus != null : !this$reviewStatus.equals(other$reviewStatus)) return false;
        final java.lang.Object this$reviewTime = this.getReviewTime();
        final java.lang.Object other$reviewTime = other.getReviewTime();
        if (this$reviewTime == null ? other$reviewTime != null : !this$reviewTime.equals(other$reviewTime)) return false;
        final java.lang.Object this$reviewComment = this.getReviewComment();
        final java.lang.Object other$reviewComment = other.getReviewComment();
        if (this$reviewComment == null ? other$reviewComment != null : !this$reviewComment.equals(other$reviewComment)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof EmployeeArchiveDetail;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $archiveId = this.getArchiveId();
        result = result * PRIME + ($archiveId == null ? 43 : $archiveId.hashCode());
        final java.lang.Object $privacyAgreementSigned = this.getPrivacyAgreementSigned();
        result = result * PRIME + ($privacyAgreementSigned == null ? 43 : $privacyAgreementSigned.hashCode());
        final java.lang.Object $dataAccuracyConfirmed = this.getDataAccuracyConfirmed();
        result = result * PRIME + ($dataAccuracyConfirmed == null ? 43 : $dataAccuracyConfirmed.hashCode());
        final java.lang.Object $completenessScore = this.getCompletenessScore();
        result = result * PRIME + ($completenessScore == null ? 43 : $completenessScore.hashCode());
        final java.lang.Object $reviewBy = this.getReviewBy();
        result = result * PRIME + ($reviewBy == null ? 43 : $reviewBy.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $employeeId = this.getEmployeeId();
        result = result * PRIME + ($employeeId == null ? 43 : $employeeId.hashCode());
        final java.lang.Object $realName = this.getRealName();
        result = result * PRIME + ($realName == null ? 43 : $realName.hashCode());
        final java.lang.Object $gender = this.getGender();
        result = result * PRIME + ($gender == null ? 43 : $gender.hashCode());
        final java.lang.Object $birthday = this.getBirthday();
        result = result * PRIME + ($birthday == null ? 43 : $birthday.hashCode());
        final java.lang.Object $nation = this.getNation();
        result = result * PRIME + ($nation == null ? 43 : $nation.hashCode());
        final java.lang.Object $politicalStatus = this.getPoliticalStatus();
        result = result * PRIME + ($politicalStatus == null ? 43 : $politicalStatus.hashCode());
        final java.lang.Object $maritalStatus = this.getMaritalStatus();
        result = result * PRIME + ($maritalStatus == null ? 43 : $maritalStatus.hashCode());
        final java.lang.Object $nativePlace = this.getNativePlace();
        result = result * PRIME + ($nativePlace == null ? 43 : $nativePlace.hashCode());
        final java.lang.Object $residenceAddress = this.getResidenceAddress();
        result = result * PRIME + ($residenceAddress == null ? 43 : $residenceAddress.hashCode());
        final java.lang.Object $currentAddress = this.getCurrentAddress();
        result = result * PRIME + ($currentAddress == null ? 43 : $currentAddress.hashCode());
        final java.lang.Object $educationLevel = this.getEducationLevel();
        result = result * PRIME + ($educationLevel == null ? 43 : $educationLevel.hashCode());
        final java.lang.Object $graduationSchool = this.getGraduationSchool();
        result = result * PRIME + ($graduationSchool == null ? 43 : $graduationSchool.hashCode());
        final java.lang.Object $major = this.getMajor();
        result = result * PRIME + ($major == null ? 43 : $major.hashCode());
        final java.lang.Object $graduationDate = this.getGraduationDate();
        result = result * PRIME + ($graduationDate == null ? 43 : $graduationDate.hashCode());
        final java.lang.Object $degree = this.getDegree();
        result = result * PRIME + ($degree == null ? 43 : $degree.hashCode());
        final java.lang.Object $workExperience = this.getWorkExperience();
        result = result * PRIME + ($workExperience == null ? 43 : $workExperience.hashCode());
        final java.lang.Object $familyMembers = this.getFamilyMembers();
        result = result * PRIME + ($familyMembers == null ? 43 : $familyMembers.hashCode());
        final java.lang.Object $emergencyContact = this.getEmergencyContact();
        result = result * PRIME + ($emergencyContact == null ? 43 : $emergencyContact.hashCode());
        final java.lang.Object $emergencyRelationship = this.getEmergencyRelationship();
        result = result * PRIME + ($emergencyRelationship == null ? 43 : $emergencyRelationship.hashCode());
        final java.lang.Object $emergencyPhone = this.getEmergencyPhone();
        result = result * PRIME + ($emergencyPhone == null ? 43 : $emergencyPhone.hashCode());
        final java.lang.Object $emergencyAddress = this.getEmergencyAddress();
        result = result * PRIME + ($emergencyAddress == null ? 43 : $emergencyAddress.hashCode());
        final java.lang.Object $bankName = this.getBankName();
        result = result * PRIME + ($bankName == null ? 43 : $bankName.hashCode());
        final java.lang.Object $bankBranch = this.getBankBranch();
        result = result * PRIME + ($bankBranch == null ? 43 : $bankBranch.hashCode());
        final java.lang.Object $bankCard = this.getBankCard();
        result = result * PRIME + ($bankCard == null ? 43 : $bankCard.hashCode());
        final java.lang.Object $socialSecurityNo = this.getSocialSecurityNo();
        result = result * PRIME + ($socialSecurityNo == null ? 43 : $socialSecurityNo.hashCode());
        final java.lang.Object $housingFundNo = this.getHousingFundNo();
        result = result * PRIME + ($housingFundNo == null ? 43 : $housingFundNo.hashCode());
        final java.lang.Object $idCardFrontUrl = this.getIdCardFrontUrl();
        result = result * PRIME + ($idCardFrontUrl == null ? 43 : $idCardFrontUrl.hashCode());
        final java.lang.Object $idCardBackUrl = this.getIdCardBackUrl();
        result = result * PRIME + ($idCardBackUrl == null ? 43 : $idCardBackUrl.hashCode());
        final java.lang.Object $diplomaUrl = this.getDiplomaUrl();
        result = result * PRIME + ($diplomaUrl == null ? 43 : $diplomaUrl.hashCode());
        final java.lang.Object $degreeCertificateUrl = this.getDegreeCertificateUrl();
        result = result * PRIME + ($degreeCertificateUrl == null ? 43 : $degreeCertificateUrl.hashCode());
        final java.lang.Object $photoUrl = this.getPhotoUrl();
        result = result * PRIME + ($photoUrl == null ? 43 : $photoUrl.hashCode());
        final java.lang.Object $otherAttachments = this.getOtherAttachments();
        result = result * PRIME + ($otherAttachments == null ? 43 : $otherAttachments.hashCode());
        final java.lang.Object $privacyAgreementTime = this.getPrivacyAgreementTime();
        result = result * PRIME + ($privacyAgreementTime == null ? 43 : $privacyAgreementTime.hashCode());
        final java.lang.Object $dataConfirmTime = this.getDataConfirmTime();
        result = result * PRIME + ($dataConfirmTime == null ? 43 : $dataConfirmTime.hashCode());
        final java.lang.Object $missingFields = this.getMissingFields();
        result = result * PRIME + ($missingFields == null ? 43 : $missingFields.hashCode());
        final java.lang.Object $reviewStatus = this.getReviewStatus();
        result = result * PRIME + ($reviewStatus == null ? 43 : $reviewStatus.hashCode());
        final java.lang.Object $reviewTime = this.getReviewTime();
        result = result * PRIME + ($reviewTime == null ? 43 : $reviewTime.hashCode());
        final java.lang.Object $reviewComment = this.getReviewComment();
        result = result * PRIME + ($reviewComment == null ? 43 : $reviewComment.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "EmployeeArchiveDetail(id=" + this.getId() + ", archiveId=" + this.getArchiveId() + ", employeeId=" + this.getEmployeeId() + ", realName=" + this.getRealName() + ", gender=" + this.getGender() + ", birthday=" + this.getBirthday() + ", nation=" + this.getNation() + ", politicalStatus=" + this.getPoliticalStatus() + ", maritalStatus=" + this.getMaritalStatus() + ", nativePlace=" + this.getNativePlace() + ", residenceAddress=" + this.getResidenceAddress() + ", currentAddress=" + this.getCurrentAddress() + ", educationLevel=" + this.getEducationLevel() + ", graduationSchool=" + this.getGraduationSchool() + ", major=" + this.getMajor() + ", graduationDate=" + this.getGraduationDate() + ", degree=" + this.getDegree() + ", workExperience=" + this.getWorkExperience() + ", familyMembers=" + this.getFamilyMembers() + ", emergencyContact=" + this.getEmergencyContact() + ", emergencyRelationship=" + this.getEmergencyRelationship() + ", emergencyPhone=" + this.getEmergencyPhone() + ", emergencyAddress=" + this.getEmergencyAddress() + ", bankName=" + this.getBankName() + ", bankBranch=" + this.getBankBranch() + ", bankCard=" + this.getBankCard() + ", socialSecurityNo=" + this.getSocialSecurityNo() + ", housingFundNo=" + this.getHousingFundNo() + ", idCardFrontUrl=" + this.getIdCardFrontUrl() + ", idCardBackUrl=" + this.getIdCardBackUrl() + ", diplomaUrl=" + this.getDiplomaUrl() + ", degreeCertificateUrl=" + this.getDegreeCertificateUrl() + ", photoUrl=" + this.getPhotoUrl() + ", otherAttachments=" + this.getOtherAttachments() + ", privacyAgreementSigned=" + this.getPrivacyAgreementSigned() + ", privacyAgreementTime=" + this.getPrivacyAgreementTime() + ", dataAccuracyConfirmed=" + this.getDataAccuracyConfirmed() + ", dataConfirmTime=" + this.getDataConfirmTime() + ", completenessScore=" + this.getCompletenessScore() + ", missingFields=" + this.getMissingFields() + ", reviewStatus=" + this.getReviewStatus() + ", reviewBy=" + this.getReviewBy() + ", reviewTime=" + this.getReviewTime() + ", reviewComment=" + this.getReviewComment() + ", createBy=" + this.getCreateBy() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ", deleted=" + this.getDeleted() + ")";
    }
}
