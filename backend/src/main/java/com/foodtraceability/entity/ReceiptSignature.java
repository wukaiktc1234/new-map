package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 收货确认单签名存证实体
 * 记录收货方与供应商（司机）的确认签字，作为电子凭证留存
 */
@TableName("receipt_confirmation_signatures")
@Schema(description = "收货确认单签名存证")
public class ReceiptSignature {

    @TableId(value = "signature_id", type = IdType.AUTO)
    @Schema(description = "签名记录ID")
    private Long signatureId;

    @TableField("confirmation_id")
    @Schema(description = "关联收货确认单ID")
    private Long confirmationId;

    /** 签名方：RECEIVER 收货方 / SUPPLIER 供应商（司机） */
    @TableField("signer_type")
    @Schema(description = "签名方（RECEIVER-收货方, SUPPLIER-供应商/司机）")
    private String signerType;

    @TableField("signer_name")
    @Schema(description = "签名人姓名")
    private String signerName;

    /** 签名图片 base64（PNG） */
    @TableField("signature_image")
    @Schema(description = "签名图片（base64 PNG）")
    private String signatureImage;

    @TableField("sign_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "签名时间")
    private LocalDateTime signTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    public Long getSignatureId() {
        return signatureId;
    }

    public void setSignatureId(Long signatureId) {
        this.signatureId = signatureId;
    }

    public Long getConfirmationId() {
        return confirmationId;
    }

    public void setConfirmationId(Long confirmationId) {
        this.confirmationId = confirmationId;
    }

    public String getSignerType() {
        return signerType;
    }

    public void setSignerType(String signerType) {
        this.signerType = signerType;
    }

    public String getSignerName() {
        return signerName;
    }

    public void setSignerName(String signerName) {
        this.signerName = signerName;
    }

    public String getSignatureImage() {
        return signatureImage;
    }

    public void setSignatureImage(String signatureImage) {
        this.signatureImage = signatureImage;
    }

    public LocalDateTime getSignTime() {
        return signTime;
    }

    public void setSignTime(LocalDateTime signTime) {
        this.signTime = signTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
