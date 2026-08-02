package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("electronic_flight_ticket")
@Schema(description = "航空电子客票")
public class ElectronicFlightTicket {
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    @TableField("tenant_id")
    @Schema(description = "租户ID")
    private Long tenantId;
    @TableField("voucher_id")
    @Schema(description = "关联凭证ID")
    private Long voucherId;
    @TableField("e_ticket_number")
    @Schema(description = "电子票号")
    private String eTicketNumber;
    @TableField("electronic_invoice_number")
    @Schema(description = "电子发票号码")
    private String electronicInvoiceNumber;
    @TableField("issue_party")
    @Schema(description = "开票方")
    private String issueParty;
    @TableField("issue_date")
    @Schema(description = "开票日期")
    private LocalDate issueDate;
    @TableField("passenger_name")
    @Schema(description = "乘客姓名")
    private String passengerName;
    @TableField("id_number")
    @Schema(description = "证件号码")
    private String idNumber;
    @TableField("flight_segments")
    @Schema(description = "航段信息JSON")
    private String flightSegments;
    @TableField("fare")
    @Schema(description = "票价")
    private BigDecimal fare;
    @TableField("fuel_surcharge")
    @Schema(description = "燃油附加费")
    private BigDecimal fuelSurcharge;
    @TableField("tax_rate")
    @Schema(description = "税率")
    private BigDecimal taxRate;
    @TableField("tax_amount")
    @Schema(description = "税额")
    private BigDecimal taxAmount;
    @TableField("civil_aviation_fund")
    @Schema(description = "民航发展基金")
    private BigDecimal civilAviationFund;
    @TableField("total_amount")
    @Schema(description = "合计金额")
    private BigDecimal totalAmount;
    @TableField("buyer_name")
    @Schema(description = "购买方名称")
    private String buyerName;
    @TableField("buyer_tax_no")
    @Schema(description = "购买方税号")
    private String buyerTaxNo;
    @TableField("verification_code")
    @Schema(description = "验证码")
    private String verificationCode;
    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    @TableField("deleted")
    @TableLogic
    @Schema(description = "逻辑删除")
    private Integer deleted;

    public ElectronicFlightTicket() {
    }

    public Long getId() {
        return this.id;
    }

    public Long getTenantId() {
        return this.tenantId;
    }

    public Long getVoucherId() {
        return this.voucherId;
    }

    public String getETicketNumber() {
        return this.eTicketNumber;
    }

    public String getElectronicInvoiceNumber() {
        return this.electronicInvoiceNumber;
    }

    public String getIssueParty() {
        return this.issueParty;
    }

    public LocalDate getIssueDate() {
        return this.issueDate;
    }

    public String getPassengerName() {
        return this.passengerName;
    }

    public String getIdNumber() {
        return this.idNumber;
    }

    public String getFlightSegments() {
        return this.flightSegments;
    }

    public BigDecimal getFare() {
        return this.fare;
    }

    public BigDecimal getFuelSurcharge() {
        return this.fuelSurcharge;
    }

    public BigDecimal getTaxRate() {
        return this.taxRate;
    }

    public BigDecimal getTaxAmount() {
        return this.taxAmount;
    }

    public BigDecimal getCivilAviationFund() {
        return this.civilAviationFund;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public String getBuyerName() {
        return this.buyerName;
    }

    public String getBuyerTaxNo() {
        return this.buyerTaxNo;
    }

    public String getVerificationCode() {
        return this.verificationCode;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTenantId(final Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setVoucherId(final Long voucherId) {
        this.voucherId = voucherId;
    }

    public void setETicketNumber(final String eTicketNumber) {
        this.eTicketNumber = eTicketNumber;
    }

    public void setElectronicInvoiceNumber(final String electronicInvoiceNumber) {
        this.electronicInvoiceNumber = electronicInvoiceNumber;
    }

    public void setIssueParty(final String issueParty) {
        this.issueParty = issueParty;
    }

    public void setIssueDate(final LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public void setPassengerName(final String passengerName) {
        this.passengerName = passengerName;
    }

    public void setIdNumber(final String idNumber) {
        this.idNumber = idNumber;
    }

    public void setFlightSegments(final String flightSegments) {
        this.flightSegments = flightSegments;
    }

    public void setFare(final BigDecimal fare) {
        this.fare = fare;
    }

    public void setFuelSurcharge(final BigDecimal fuelSurcharge) {
        this.fuelSurcharge = fuelSurcharge;
    }

    public void setTaxRate(final BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public void setTaxAmount(final BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public void setCivilAviationFund(final BigDecimal civilAviationFund) {
        this.civilAviationFund = civilAviationFund;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setBuyerName(final String buyerName) {
        this.buyerName = buyerName;
    }

    public void setBuyerTaxNo(final String buyerTaxNo) {
        this.buyerTaxNo = buyerTaxNo;
    }

    public void setVerificationCode(final String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ElectronicFlightTicket)) return false;
        final ElectronicFlightTicket other = (ElectronicFlightTicket) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$tenantId = this.getTenantId();
        final java.lang.Object other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId)) return false;
        final java.lang.Object this$voucherId = this.getVoucherId();
        final java.lang.Object other$voucherId = other.getVoucherId();
        if (this$voucherId == null ? other$voucherId != null : !this$voucherId.equals(other$voucherId)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$eTicketNumber = this.getETicketNumber();
        final java.lang.Object other$eTicketNumber = other.getETicketNumber();
        if (this$eTicketNumber == null ? other$eTicketNumber != null : !this$eTicketNumber.equals(other$eTicketNumber)) return false;
        final java.lang.Object this$electronicInvoiceNumber = this.getElectronicInvoiceNumber();
        final java.lang.Object other$electronicInvoiceNumber = other.getElectronicInvoiceNumber();
        if (this$electronicInvoiceNumber == null ? other$electronicInvoiceNumber != null : !this$electronicInvoiceNumber.equals(other$electronicInvoiceNumber)) return false;
        final java.lang.Object this$issueParty = this.getIssueParty();
        final java.lang.Object other$issueParty = other.getIssueParty();
        if (this$issueParty == null ? other$issueParty != null : !this$issueParty.equals(other$issueParty)) return false;
        final java.lang.Object this$issueDate = this.getIssueDate();
        final java.lang.Object other$issueDate = other.getIssueDate();
        if (this$issueDate == null ? other$issueDate != null : !this$issueDate.equals(other$issueDate)) return false;
        final java.lang.Object this$passengerName = this.getPassengerName();
        final java.lang.Object other$passengerName = other.getPassengerName();
        if (this$passengerName == null ? other$passengerName != null : !this$passengerName.equals(other$passengerName)) return false;
        final java.lang.Object this$idNumber = this.getIdNumber();
        final java.lang.Object other$idNumber = other.getIdNumber();
        if (this$idNumber == null ? other$idNumber != null : !this$idNumber.equals(other$idNumber)) return false;
        final java.lang.Object this$flightSegments = this.getFlightSegments();
        final java.lang.Object other$flightSegments = other.getFlightSegments();
        if (this$flightSegments == null ? other$flightSegments != null : !this$flightSegments.equals(other$flightSegments)) return false;
        final java.lang.Object this$fare = this.getFare();
        final java.lang.Object other$fare = other.getFare();
        if (this$fare == null ? other$fare != null : !this$fare.equals(other$fare)) return false;
        final java.lang.Object this$fuelSurcharge = this.getFuelSurcharge();
        final java.lang.Object other$fuelSurcharge = other.getFuelSurcharge();
        if (this$fuelSurcharge == null ? other$fuelSurcharge != null : !this$fuelSurcharge.equals(other$fuelSurcharge)) return false;
        final java.lang.Object this$taxRate = this.getTaxRate();
        final java.lang.Object other$taxRate = other.getTaxRate();
        if (this$taxRate == null ? other$taxRate != null : !this$taxRate.equals(other$taxRate)) return false;
        final java.lang.Object this$taxAmount = this.getTaxAmount();
        final java.lang.Object other$taxAmount = other.getTaxAmount();
        if (this$taxAmount == null ? other$taxAmount != null : !this$taxAmount.equals(other$taxAmount)) return false;
        final java.lang.Object this$civilAviationFund = this.getCivilAviationFund();
        final java.lang.Object other$civilAviationFund = other.getCivilAviationFund();
        if (this$civilAviationFund == null ? other$civilAviationFund != null : !this$civilAviationFund.equals(other$civilAviationFund)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$buyerName = this.getBuyerName();
        final java.lang.Object other$buyerName = other.getBuyerName();
        if (this$buyerName == null ? other$buyerName != null : !this$buyerName.equals(other$buyerName)) return false;
        final java.lang.Object this$buyerTaxNo = this.getBuyerTaxNo();
        final java.lang.Object other$buyerTaxNo = other.getBuyerTaxNo();
        if (this$buyerTaxNo == null ? other$buyerTaxNo != null : !this$buyerTaxNo.equals(other$buyerTaxNo)) return false;
        final java.lang.Object this$verificationCode = this.getVerificationCode();
        final java.lang.Object other$verificationCode = other.getVerificationCode();
        if (this$verificationCode == null ? other$verificationCode != null : !this$verificationCode.equals(other$verificationCode)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ElectronicFlightTicket;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $tenantId = this.getTenantId();
        result = result * PRIME + ($tenantId == null ? 43 : $tenantId.hashCode());
        final java.lang.Object $voucherId = this.getVoucherId();
        result = result * PRIME + ($voucherId == null ? 43 : $voucherId.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $eTicketNumber = this.getETicketNumber();
        result = result * PRIME + ($eTicketNumber == null ? 43 : $eTicketNumber.hashCode());
        final java.lang.Object $electronicInvoiceNumber = this.getElectronicInvoiceNumber();
        result = result * PRIME + ($electronicInvoiceNumber == null ? 43 : $electronicInvoiceNumber.hashCode());
        final java.lang.Object $issueParty = this.getIssueParty();
        result = result * PRIME + ($issueParty == null ? 43 : $issueParty.hashCode());
        final java.lang.Object $issueDate = this.getIssueDate();
        result = result * PRIME + ($issueDate == null ? 43 : $issueDate.hashCode());
        final java.lang.Object $passengerName = this.getPassengerName();
        result = result * PRIME + ($passengerName == null ? 43 : $passengerName.hashCode());
        final java.lang.Object $idNumber = this.getIdNumber();
        result = result * PRIME + ($idNumber == null ? 43 : $idNumber.hashCode());
        final java.lang.Object $flightSegments = this.getFlightSegments();
        result = result * PRIME + ($flightSegments == null ? 43 : $flightSegments.hashCode());
        final java.lang.Object $fare = this.getFare();
        result = result * PRIME + ($fare == null ? 43 : $fare.hashCode());
        final java.lang.Object $fuelSurcharge = this.getFuelSurcharge();
        result = result * PRIME + ($fuelSurcharge == null ? 43 : $fuelSurcharge.hashCode());
        final java.lang.Object $taxRate = this.getTaxRate();
        result = result * PRIME + ($taxRate == null ? 43 : $taxRate.hashCode());
        final java.lang.Object $taxAmount = this.getTaxAmount();
        result = result * PRIME + ($taxAmount == null ? 43 : $taxAmount.hashCode());
        final java.lang.Object $civilAviationFund = this.getCivilAviationFund();
        result = result * PRIME + ($civilAviationFund == null ? 43 : $civilAviationFund.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $buyerName = this.getBuyerName();
        result = result * PRIME + ($buyerName == null ? 43 : $buyerName.hashCode());
        final java.lang.Object $buyerTaxNo = this.getBuyerTaxNo();
        result = result * PRIME + ($buyerTaxNo == null ? 43 : $buyerTaxNo.hashCode());
        final java.lang.Object $verificationCode = this.getVerificationCode();
        result = result * PRIME + ($verificationCode == null ? 43 : $verificationCode.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ElectronicFlightTicket(id=" + this.getId() + ", tenantId=" + this.getTenantId() + ", voucherId=" + this.getVoucherId() + ", eTicketNumber=" + this.getETicketNumber() + ", electronicInvoiceNumber=" + this.getElectronicInvoiceNumber() + ", issueParty=" + this.getIssueParty() + ", issueDate=" + this.getIssueDate() + ", passengerName=" + this.getPassengerName() + ", idNumber=" + this.getIdNumber() + ", flightSegments=" + this.getFlightSegments() + ", fare=" + this.getFare() + ", fuelSurcharge=" + this.getFuelSurcharge() + ", taxRate=" + this.getTaxRate() + ", taxAmount=" + this.getTaxAmount() + ", civilAviationFund=" + this.getCivilAviationFund() + ", totalAmount=" + this.getTotalAmount() + ", buyerName=" + this.getBuyerName() + ", buyerTaxNo=" + this.getBuyerTaxNo() + ", verificationCode=" + this.getVerificationCode() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}
