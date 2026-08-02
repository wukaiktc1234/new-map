package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 邀请码发送记录实体类
 * 对应数据库表：invitation_send_record
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-01-31
 */
@TableName("invitation_send_record")
@Schema(description = "邀请码发送记录实体")
public class InvitationSendRecord {
    @TableId(type = IdType.AUTO)
    @Schema(description = "记录ID")
    private Long id;
    @Schema(description = "档案ID")
    private Long archiveId;
    @Schema(description = "邀请码（唯一）")
    private String invitationCode;
    @Schema(description = "绑定的邮箱")
    private String boundEmail;
    @Schema(description = "绑定的手机")
    private String boundPhone;
    @Schema(description = "绑定的姓名")
    private String boundName;
    @Schema(description = "员工编号")
    private String employeeCode;
    @Schema(description = "预设用户名")
    private String presetUsername;
    @Schema(description = "已使用次数")
    private Integer useCount;
    @Schema(description = "最大使用次数")
    private Integer maxUseCount;
    @Schema(description = "状态：UNUSED-未使用, USED-已使用, EXPIRED-已过期, REVOKED-已撤销")
    private String status;
    @Schema(description = "有效天数")
    private Integer validDays;
    @Schema(description = "过期时间")
    private LocalDateTime expireTime;
    @Schema(description = "使用时间")
    private LocalDateTime usedTime;
    @Schema(description = "使用人ID（注册用户ID）")
    private Long usedBy;
    @Schema(description = "发送方式：EMAIL-邮件, SMS-短信, BOTH-两者")
    private String sendMethod;
    @Schema(description = "发送状态：SENT-已发送, FAILED-发送失败, PENDING-待发送")
    private String sendStatus;
    @Schema(description = "发送时间")
    private LocalDateTime sendTime;
    @Schema(description = "错误信息（发送失败时记录）")
    private String errorMessage;
    @Schema(description = "批次ID（批量发送时使用）")
    private String batchId;
    @Schema(description = "创建人")
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    // 状态常量
    public static final String STATUS_UNUSED = "UNUSED";
    public static final String STATUS_USED = "USED";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_REVOKED = "REVOKED";
    // 发送方式常量
    public static final String METHOD_EMAIL = "EMAIL";
    public static final String METHOD_SMS = "SMS";
    public static final String METHOD_BOTH = "BOTH";
    // 发送状态常量
    public static final String SEND_STATUS_SENT = "SENT";
    public static final String SEND_STATUS_FAILED = "FAILED";
    public static final String SEND_STATUS_PENDING = "PENDING";

    public InvitationSendRecord() {
    }

    public Long getId() {
        return this.id;
    }

    public Long getArchiveId() {
        return this.archiveId;
    }

    public String getInvitationCode() {
        return this.invitationCode;
    }

    public String getBoundEmail() {
        return this.boundEmail;
    }

    public String getBoundPhone() {
        return this.boundPhone;
    }

    public String getBoundName() {
        return this.boundName;
    }

    public String getEmployeeCode() {
        return this.employeeCode;
    }

    public String getPresetUsername() {
        return this.presetUsername;
    }

    public Integer getUseCount() {
        return this.useCount;
    }

    public Integer getMaxUseCount() {
        return this.maxUseCount;
    }

    public String getStatus() {
        return this.status;
    }

    public Integer getValidDays() {
        return this.validDays;
    }

    public LocalDateTime getExpireTime() {
        return this.expireTime;
    }

    public LocalDateTime getUsedTime() {
        return this.usedTime;
    }

    public Long getUsedBy() {
        return this.usedBy;
    }

    public String getSendMethod() {
        return this.sendMethod;
    }

    public String getSendStatus() {
        return this.sendStatus;
    }

    public LocalDateTime getSendTime() {
        return this.sendTime;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public String getBatchId() {
        return this.batchId;
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

    public void setId(final Long id) {
        this.id = id;
    }

    public void setArchiveId(final Long archiveId) {
        this.archiveId = archiveId;
    }

    public void setInvitationCode(final String invitationCode) {
        this.invitationCode = invitationCode;
    }

    public void setBoundEmail(final String boundEmail) {
        this.boundEmail = boundEmail;
    }

    public void setBoundPhone(final String boundPhone) {
        this.boundPhone = boundPhone;
    }

    public void setBoundName(final String boundName) {
        this.boundName = boundName;
    }

    public void setEmployeeCode(final String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public void setPresetUsername(final String presetUsername) {
        this.presetUsername = presetUsername;
    }

    public void setUseCount(final Integer useCount) {
        this.useCount = useCount;
    }

    public void setMaxUseCount(final Integer maxUseCount) {
        this.maxUseCount = maxUseCount;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setValidDays(final Integer validDays) {
        this.validDays = validDays;
    }

    public void setExpireTime(final LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public void setUsedTime(final LocalDateTime usedTime) {
        this.usedTime = usedTime;
    }

    public void setUsedBy(final Long usedBy) {
        this.usedBy = usedBy;
    }

    public void setSendMethod(final String sendMethod) {
        this.sendMethod = sendMethod;
    }

    public void setSendStatus(final String sendStatus) {
        this.sendStatus = sendStatus;
    }

    public void setSendTime(final LocalDateTime sendTime) {
        this.sendTime = sendTime;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setBatchId(final String batchId) {
        this.batchId = batchId;
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

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof InvitationSendRecord)) return false;
        final InvitationSendRecord other = (InvitationSendRecord) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$archiveId = this.getArchiveId();
        final java.lang.Object other$archiveId = other.getArchiveId();
        if (this$archiveId == null ? other$archiveId != null : !this$archiveId.equals(other$archiveId)) return false;
        final java.lang.Object this$useCount = this.getUseCount();
        final java.lang.Object other$useCount = other.getUseCount();
        if (this$useCount == null ? other$useCount != null : !this$useCount.equals(other$useCount)) return false;
        final java.lang.Object this$maxUseCount = this.getMaxUseCount();
        final java.lang.Object other$maxUseCount = other.getMaxUseCount();
        if (this$maxUseCount == null ? other$maxUseCount != null : !this$maxUseCount.equals(other$maxUseCount)) return false;
        final java.lang.Object this$validDays = this.getValidDays();
        final java.lang.Object other$validDays = other.getValidDays();
        if (this$validDays == null ? other$validDays != null : !this$validDays.equals(other$validDays)) return false;
        final java.lang.Object this$usedBy = this.getUsedBy();
        final java.lang.Object other$usedBy = other.getUsedBy();
        if (this$usedBy == null ? other$usedBy != null : !this$usedBy.equals(other$usedBy)) return false;
        final java.lang.Object this$createBy = this.getCreateBy();
        final java.lang.Object other$createBy = other.getCreateBy();
        if (this$createBy == null ? other$createBy != null : !this$createBy.equals(other$createBy)) return false;
        final java.lang.Object this$invitationCode = this.getInvitationCode();
        final java.lang.Object other$invitationCode = other.getInvitationCode();
        if (this$invitationCode == null ? other$invitationCode != null : !this$invitationCode.equals(other$invitationCode)) return false;
        final java.lang.Object this$boundEmail = this.getBoundEmail();
        final java.lang.Object other$boundEmail = other.getBoundEmail();
        if (this$boundEmail == null ? other$boundEmail != null : !this$boundEmail.equals(other$boundEmail)) return false;
        final java.lang.Object this$boundPhone = this.getBoundPhone();
        final java.lang.Object other$boundPhone = other.getBoundPhone();
        if (this$boundPhone == null ? other$boundPhone != null : !this$boundPhone.equals(other$boundPhone)) return false;
        final java.lang.Object this$boundName = this.getBoundName();
        final java.lang.Object other$boundName = other.getBoundName();
        if (this$boundName == null ? other$boundName != null : !this$boundName.equals(other$boundName)) return false;
        final java.lang.Object this$employeeCode = this.getEmployeeCode();
        final java.lang.Object other$employeeCode = other.getEmployeeCode();
        if (this$employeeCode == null ? other$employeeCode != null : !this$employeeCode.equals(other$employeeCode)) return false;
        final java.lang.Object this$presetUsername = this.getPresetUsername();
        final java.lang.Object other$presetUsername = other.getPresetUsername();
        if (this$presetUsername == null ? other$presetUsername != null : !this$presetUsername.equals(other$presetUsername)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$expireTime = this.getExpireTime();
        final java.lang.Object other$expireTime = other.getExpireTime();
        if (this$expireTime == null ? other$expireTime != null : !this$expireTime.equals(other$expireTime)) return false;
        final java.lang.Object this$usedTime = this.getUsedTime();
        final java.lang.Object other$usedTime = other.getUsedTime();
        if (this$usedTime == null ? other$usedTime != null : !this$usedTime.equals(other$usedTime)) return false;
        final java.lang.Object this$sendMethod = this.getSendMethod();
        final java.lang.Object other$sendMethod = other.getSendMethod();
        if (this$sendMethod == null ? other$sendMethod != null : !this$sendMethod.equals(other$sendMethod)) return false;
        final java.lang.Object this$sendStatus = this.getSendStatus();
        final java.lang.Object other$sendStatus = other.getSendStatus();
        if (this$sendStatus == null ? other$sendStatus != null : !this$sendStatus.equals(other$sendStatus)) return false;
        final java.lang.Object this$sendTime = this.getSendTime();
        final java.lang.Object other$sendTime = other.getSendTime();
        if (this$sendTime == null ? other$sendTime != null : !this$sendTime.equals(other$sendTime)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$batchId = this.getBatchId();
        final java.lang.Object other$batchId = other.getBatchId();
        if (this$batchId == null ? other$batchId != null : !this$batchId.equals(other$batchId)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        final java.lang.Object this$updateTime = this.getUpdateTime();
        final java.lang.Object other$updateTime = other.getUpdateTime();
        if (this$updateTime == null ? other$updateTime != null : !this$updateTime.equals(other$updateTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof InvitationSendRecord;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $archiveId = this.getArchiveId();
        result = result * PRIME + ($archiveId == null ? 43 : $archiveId.hashCode());
        final java.lang.Object $useCount = this.getUseCount();
        result = result * PRIME + ($useCount == null ? 43 : $useCount.hashCode());
        final java.lang.Object $maxUseCount = this.getMaxUseCount();
        result = result * PRIME + ($maxUseCount == null ? 43 : $maxUseCount.hashCode());
        final java.lang.Object $validDays = this.getValidDays();
        result = result * PRIME + ($validDays == null ? 43 : $validDays.hashCode());
        final java.lang.Object $usedBy = this.getUsedBy();
        result = result * PRIME + ($usedBy == null ? 43 : $usedBy.hashCode());
        final java.lang.Object $createBy = this.getCreateBy();
        result = result * PRIME + ($createBy == null ? 43 : $createBy.hashCode());
        final java.lang.Object $invitationCode = this.getInvitationCode();
        result = result * PRIME + ($invitationCode == null ? 43 : $invitationCode.hashCode());
        final java.lang.Object $boundEmail = this.getBoundEmail();
        result = result * PRIME + ($boundEmail == null ? 43 : $boundEmail.hashCode());
        final java.lang.Object $boundPhone = this.getBoundPhone();
        result = result * PRIME + ($boundPhone == null ? 43 : $boundPhone.hashCode());
        final java.lang.Object $boundName = this.getBoundName();
        result = result * PRIME + ($boundName == null ? 43 : $boundName.hashCode());
        final java.lang.Object $employeeCode = this.getEmployeeCode();
        result = result * PRIME + ($employeeCode == null ? 43 : $employeeCode.hashCode());
        final java.lang.Object $presetUsername = this.getPresetUsername();
        result = result * PRIME + ($presetUsername == null ? 43 : $presetUsername.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $expireTime = this.getExpireTime();
        result = result * PRIME + ($expireTime == null ? 43 : $expireTime.hashCode());
        final java.lang.Object $usedTime = this.getUsedTime();
        result = result * PRIME + ($usedTime == null ? 43 : $usedTime.hashCode());
        final java.lang.Object $sendMethod = this.getSendMethod();
        result = result * PRIME + ($sendMethod == null ? 43 : $sendMethod.hashCode());
        final java.lang.Object $sendStatus = this.getSendStatus();
        result = result * PRIME + ($sendStatus == null ? 43 : $sendStatus.hashCode());
        final java.lang.Object $sendTime = this.getSendTime();
        result = result * PRIME + ($sendTime == null ? 43 : $sendTime.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $batchId = this.getBatchId();
        result = result * PRIME + ($batchId == null ? 43 : $batchId.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        final java.lang.Object $updateTime = this.getUpdateTime();
        result = result * PRIME + ($updateTime == null ? 43 : $updateTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "InvitationSendRecord(id=" + this.getId() + ", archiveId=" + this.getArchiveId() + ", invitationCode=" + this.getInvitationCode() + ", boundEmail=" + this.getBoundEmail() + ", boundPhone=" + this.getBoundPhone() + ", boundName=" + this.getBoundName() + ", employeeCode=" + this.getEmployeeCode() + ", presetUsername=" + this.getPresetUsername() + ", useCount=" + this.getUseCount() + ", maxUseCount=" + this.getMaxUseCount() + ", status=" + this.getStatus() + ", validDays=" + this.getValidDays() + ", expireTime=" + this.getExpireTime() + ", usedTime=" + this.getUsedTime() + ", usedBy=" + this.getUsedBy() + ", sendMethod=" + this.getSendMethod() + ", sendStatus=" + this.getSendStatus() + ", sendTime=" + this.getSendTime() + ", errorMessage=" + this.getErrorMessage() + ", batchId=" + this.getBatchId() + ", createBy=" + this.getCreateBy() + ", createTime=" + this.getCreateTime() + ", updateTime=" + this.getUpdateTime() + ")";
    }
}
