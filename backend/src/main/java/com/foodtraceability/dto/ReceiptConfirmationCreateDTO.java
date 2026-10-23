package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 收货确认单创建请求
 * 门店/仓库现场人员根据到货单创建收货确认
 */
@Schema(description = "收货确认单创建请求")
public class ReceiptConfirmationCreateDTO {

    @NotNull(message = "到货单ID不能为空")
    @Schema(description = "关联到货单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long arrivalId;

    @NotEmpty(message = "收货明细不能为空")
    @Valid
    @Schema(description = "收货确认明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ReceiptConfirmationItemDTO> items;

    @Schema(description = "质检结果（1-合格, 2-不合格）")
    private Integer qualityCheckResult;

    @Size(max = 500, message = "质检备注不能超过500个字符")
    @Schema(description = "质检备注")
    private String qualityRemark;

    @Size(max = 500, message = "备注不能超过500个字符")
    @Schema(description = "备注")
    private String remark;

    @Size(max = 64, message = "条形码不能超过64个字符")
    @Schema(description = "货物条形码（商品码/外箱条码）")
    private String barcode;

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public Long getArrivalId() {
        return arrivalId;
    }

    public void setArrivalId(Long arrivalId) {
        this.arrivalId = arrivalId;
    }

    public List<ReceiptConfirmationItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ReceiptConfirmationItemDTO> items) {
        this.items = items;
    }

    public Integer getQualityCheckResult() {
        return qualityCheckResult;
    }

    public void setQualityCheckResult(Integer qualityCheckResult) {
        this.qualityCheckResult = qualityCheckResult;
    }

    public String getQualityRemark() {
        return qualityRemark;
    }

    public void setQualityRemark(String qualityRemark) {
        this.qualityRemark = qualityRemark;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 收货确认明细项
     */
    @Schema(description = "收货确认明细项")
    public static class ReceiptConfirmationItemDTO {

        @NotNull(message = "到货明细ID不能为空")
        @Schema(description = "关联到货明细ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long arrivalItemId;

        @NotNull(message = "确认收货数量不能为空")
        @Schema(description = "确认收货数量", requiredMode = Schema.RequiredMode.REQUIRED)
        private BigDecimal confirmedQuantity;

        @Schema(description = "拒收数量")
        private BigDecimal rejectedQuantity;

        @Size(max = 100, message = "批次号不能超过100个字符")
        @Schema(description = "批次号")
        private String batchNo;

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "生产日期")
        private LocalDate productionDate;

        @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "有效期至")
        private LocalDate expiryDate;

        @Size(max = 500, message = "明细备注不能超过500个字符")
        @Schema(description = "备注")
        private String remark;

        @Size(max = 20, message = "问题类型不能超过20个字符")
        @Schema(description = "问题类型（damaged/short/expiry/quality/other）")
        private String issueType;

        @Schema(description = "留证照片列表（拒收/人工关联必拍）")
        private List<ReceiptEvidenceDTO> evidence;

        @Schema(description = "质检结果（1-合格 2-不合格 3-部分合格）")
        private Integer qualityCheckResult;

        public Integer getQualityCheckResult() {
            return qualityCheckResult;
        }

        public void setQualityCheckResult(Integer qualityCheckResult) {
            this.qualityCheckResult = qualityCheckResult;
        }

        public List<ReceiptEvidenceDTO> getEvidence() {
            return evidence;
        }

        public void setEvidence(List<ReceiptEvidenceDTO> evidence) {
            this.evidence = evidence;
        }

        public Long getArrivalItemId() {
            return arrivalItemId;
        }

        public void setArrivalItemId(Long arrivalItemId) {
            this.arrivalItemId = arrivalItemId;
        }

        public BigDecimal getConfirmedQuantity() {
            return confirmedQuantity;
        }

        public void setConfirmedQuantity(BigDecimal confirmedQuantity) {
            this.confirmedQuantity = confirmedQuantity;
        }

        public BigDecimal getRejectedQuantity() {
            return rejectedQuantity;
        }

        public void setRejectedQuantity(BigDecimal rejectedQuantity) {
            this.rejectedQuantity = rejectedQuantity;
        }

        public String getBatchNo() {
            return batchNo;
        }

        public void setBatchNo(String batchNo) {
            this.batchNo = batchNo;
        }

        public LocalDate getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(LocalDate productionDate) {
            this.productionDate = productionDate;
        }

        public LocalDate getExpiryDate() {
            return expiryDate;
        }

        public void setExpiryDate(LocalDate expiryDate) {
            this.expiryDate = expiryDate;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public String getIssueType() {
            return issueType;
        }

        public void setIssueType(String issueType) {
            this.issueType = issueType;
        }
    }

    /**
     * 留证照片信息
     */
    @Schema(description = "留证照片")
    public static class ReceiptEvidenceDTO {

        @Schema(description = "证据类型：MANUAL_MATCH-人工关联 / QC_REJECT-拒收或不合格")
        private String evidenceType;

        @Schema(description = "照片访问地址")
        private String photoUrl;

        @Schema(description = "照片拍摄时间（Exif）")
        private String photoTakenAt;

        @Schema(description = "拍摄设备型号（Exif）")
        private String deviceModel;

        @Schema(description = "定位信息")
        private String gps;

        public String getEvidenceType() {
            return evidenceType;
        }

        public void setEvidenceType(String evidenceType) {
            this.evidenceType = evidenceType;
        }

        public String getPhotoUrl() {
            return photoUrl;
        }

        public void setPhotoUrl(String photoUrl) {
            this.photoUrl = photoUrl;
        }

        public String getPhotoTakenAt() {
            return photoTakenAt;
        }

        public void setPhotoTakenAt(String photoTakenAt) {
            this.photoTakenAt = photoTakenAt;
        }

        public String getDeviceModel() {
            return deviceModel;
        }

        public void setDeviceModel(String deviceModel) {
            this.deviceModel = deviceModel;
        }

        public String getGps() {
            return gps;
        }

        public void setGps(String gps) {
            this.gps = gps;
        }
    }
}
