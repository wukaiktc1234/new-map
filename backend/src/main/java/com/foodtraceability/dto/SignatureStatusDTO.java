package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 合同签署状态看板数据DTO
 *
 * <p>用于展示合同签署进度，包含总签署人数、已签署人数、待签署人数、
 * 已拒绝人数、签署进度百分比以及各方签署详情。</p>
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Schema(description = "合同签署状态看板数据")
public class SignatureStatusDTO {

    @Schema(description = "合同ID")
    private Long contractId;

    @Schema(description = "合同编号", example = "LC20260320ABC123")
    private String contractNo;

    @Schema(description = "员工姓名", example = "张三")
    private String employeeName;

    @Schema(description = "总签署人数", example = "2")
    private Integer totalSigners;

    @Schema(description = "已签署人数", example = "1")
    private Integer signedCount;

    @Schema(description = "待签署人数", example = "1")
    private Integer pendingCount;

    @Schema(description = "已拒绝人数", example = "0")
    private Integer rejectedCount;

    @Schema(description = "签署进度百分比（0-100）", example = "50")
    private Integer progressPercent;

    @Schema(description = "各方签署详情")
    private List<SignerDetail> signers;

    public SignatureStatusDTO() {
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public Integer getTotalSigners() {
        return totalSigners;
    }

    public void setTotalSigners(Integer totalSigners) {
        this.totalSigners = totalSigners;
    }

    public Integer getSignedCount() {
        return signedCount;
    }

    public void setSignedCount(Integer signedCount) {
        this.signedCount = signedCount;
    }

    public Integer getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Integer pendingCount) {
        this.pendingCount = pendingCount;
    }

    public Integer getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(Integer rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public List<SignerDetail> getSigners() {
        return signers;
    }

    public void setSigners(List<SignerDetail> signers) {
        this.signers = signers;
    }

    /**
     * 签署方详情
     *
     * <p>描述单个签署方的签署状态、签署时间、签署IP等信息。</p>
     */
    @Schema(description = "签署方详情")
    public static class SignerDetail {

        @Schema(description = "签署人类型: employee-员工, company-公司", example = "employee")
        private String signerType;

        @Schema(description = "签署人姓名", example = "张三")
        private String signerName;

        @Schema(description = "签署状态: pending-待签, signed-已签, rejected-已拒绝", example = "signed")
        private String status;

        @Schema(description = "签署时间", example = "2026-03-20T15:30:00")
        private LocalDateTime signTime;

        @Schema(description = "签署IP地址", example = "192.168.1.100")
        private String signIp;

        public SignerDetail() {
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getSignTime() {
            return signTime;
        }

        public void setSignTime(LocalDateTime signTime) {
            this.signTime = signTime;
        }

        public String getSignIp() {
            return signIp;
        }

        public void setSignIp(String signIp) {
            this.signIp = signIp;
        }
    }
}
