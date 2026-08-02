package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;

/**
 * OCR识别记录实体类
 * @author example
 * @since 2025-12-06
 */
@TableName("ocr_record")
public class OcrRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 业务ID（如发票ID、凭证ID）
     */
    @TableField("business_id")
    private Long businessId;

    /**
     * 业务类型：INVOICE（发票）、CERTIFICATE（凭证）、PACKAGE（外包装）
     */
    @TableField("business_type")
    private String businessType;

    /**
     * 图片URL
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 识别结果JSON
     */
    @TableField("ocr_result")
    private String ocrResult;

    /**
     * 识别置信度（0-100）
     */
    @TableField("confidence")
    private Integer confidence;

    /**
     * 识别状态：SUCCESS（成功）、FAILED（失败）、PENDING（待处理）
     */
    @TableField("status")
    private String status;

    /**
     * 失败原因
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 识别时间
     */
    @TableField("ocr_time")
    private LocalDateTime ocrTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;

    /**
     * 更新人
     */
    @TableField("update_by")
    private String updateBy;

    // getter and setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getOcrResult() {
        return ocrResult;
    }

    public void setOcrResult(String ocrResult) {
        this.ocrResult = ocrResult;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getOcrTime() {
        return ocrTime;
    }

    public void setOcrTime(LocalDateTime ocrTime) {
        this.ocrTime = ocrTime;
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

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    @Override
    public String toString() {
        return "OcrRecord{" +
            "id=" + id +
            ", businessId=" + businessId +
            ", businessType='" + businessType + '\'' +
            ", imageUrl='" + imageUrl + '\'' +
            ", ocrResult='" + ocrResult + '\'' +
            ", confidence=" + confidence +
            ", status='" + status + '\'' +
            ", errorMessage='" + errorMessage + '\'' +
            ", ocrTime=" + ocrTime +
            ", createTime=" + createTime +
            ", updateTime=" + updateTime +
            ", createBy='" + createBy + '\'' +
            ", updateBy='" + updateBy + '\'' +
            '}';
    }
}
