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

@TableName("electronic_train_ticket")
@Schema(description = "铁路电子客票")
public class ElectronicTrainTicket {
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
    @TableField("issue_party_code")
    @Schema(description = "开票方代码")
    private String issuePartyCode;
    @TableField("date_of_issue")
    @Schema(description = "开票日期")
    private LocalDate dateOfIssue;
    @TableField("departure_station")
    @Schema(description = "出发站")
    private String departureStation;
    @TableField("destination_station")
    @Schema(description = "到达站")
    private String destinationStation;
    @TableField("train_number")
    @Schema(description = "车次")
    private String trainNumber;
    @TableField("travel_date")
    @Schema(description = "乘车日期")
    private LocalDate travelDate;
    @TableField("departure_time")
    @Schema(description = "出发时间")
    private String departureTime;
    @TableField("seat_level")
    @Schema(description = "座位等级")
    private String seatLevel;
    @TableField("carriage")
    @Schema(description = "车厢")
    private String carriage;
    @TableField("seat")
    @Schema(description = "座位号")
    private String seat;
    @TableField("passenger_name")
    @Schema(description = "乘客姓名")
    private String passengerName;
    @TableField("id_number")
    @Schema(description = "证件号码")
    private String idNumber;
    @TableField("fare")
    @Schema(description = "票价")
    private BigDecimal fare;
    @TableField("tax_rate")
    @Schema(description = "税率")
    private BigDecimal taxRate;
    @TableField("tax_amount")
    @Schema(description = "税额")
    private BigDecimal taxAmount;
    @TableField("total_amount")
    @Schema(description = "合计金额")
    private BigDecimal totalAmount;
    @TableField("buyer_name")
    @Schema(description = "购买方名称")
    private String buyerName;
    @TableField("buyer_tax_no")
    @Schema(description = "购买方税号")
    private String buyerTaxNo;
    @TableField("seller_name")
    @Schema(description = "销售方名称")
    private String sellerName;
    @TableField("seller_tax_no")
    @Schema(description = "销售方税号")
    private String sellerTaxNo;
    @TableField("remarks")
    @Schema(description = "备注")
    private String remarks;
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

    public ElectronicTrainTicket() {
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

    public String getIssuePartyCode() {
        return this.issuePartyCode;
    }

    public LocalDate getDateOfIssue() {
        return this.dateOfIssue;
    }

    public String getDepartureStation() {
        return this.departureStation;
    }

    public String getDestinationStation() {
        return this.destinationStation;
    }

    public String getTrainNumber() {
        return this.trainNumber;
    }

    public LocalDate getTravelDate() {
        return this.travelDate;
    }

    public String getDepartureTime() {
        return this.departureTime;
    }

    public String getSeatLevel() {
        return this.seatLevel;
    }

    public String getCarriage() {
        return this.carriage;
    }

    public String getSeat() {
        return this.seat;
    }

    public String getPassengerName() {
        return this.passengerName;
    }

    public String getIdNumber() {
        return this.idNumber;
    }

    public BigDecimal getFare() {
        return this.fare;
    }

    public BigDecimal getTaxRate() {
        return this.taxRate;
    }

    public BigDecimal getTaxAmount() {
        return this.taxAmount;
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

    public String getSellerName() {
        return this.sellerName;
    }

    public String getSellerTaxNo() {
        return this.sellerTaxNo;
    }

    public String getRemarks() {
        return this.remarks;
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

    public void setIssuePartyCode(final String issuePartyCode) {
        this.issuePartyCode = issuePartyCode;
    }

    public void setDateOfIssue(final LocalDate dateOfIssue) {
        this.dateOfIssue = dateOfIssue;
    }

    public void setDepartureStation(final String departureStation) {
        this.departureStation = departureStation;
    }

    public void setDestinationStation(final String destinationStation) {
        this.destinationStation = destinationStation;
    }

    public void setTrainNumber(final String trainNumber) {
        this.trainNumber = trainNumber;
    }

    public void setTravelDate(final LocalDate travelDate) {
        this.travelDate = travelDate;
    }

    public void setDepartureTime(final String departureTime) {
        this.departureTime = departureTime;
    }

    public void setSeatLevel(final String seatLevel) {
        this.seatLevel = seatLevel;
    }

    public void setCarriage(final String carriage) {
        this.carriage = carriage;
    }

    public void setSeat(final String seat) {
        this.seat = seat;
    }

    public void setPassengerName(final String passengerName) {
        this.passengerName = passengerName;
    }

    public void setIdNumber(final String idNumber) {
        this.idNumber = idNumber;
    }

    public void setFare(final BigDecimal fare) {
        this.fare = fare;
    }

    public void setTaxRate(final BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public void setTaxAmount(final BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
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

    public void setSellerName(final String sellerName) {
        this.sellerName = sellerName;
    }

    public void setSellerTaxNo(final String sellerTaxNo) {
        this.sellerTaxNo = sellerTaxNo;
    }

    public void setRemarks(final String remarks) {
        this.remarks = remarks;
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
        if (!(o instanceof ElectronicTrainTicket)) return false;
        final ElectronicTrainTicket other = (ElectronicTrainTicket) o;
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
        final java.lang.Object this$issuePartyCode = this.getIssuePartyCode();
        final java.lang.Object other$issuePartyCode = other.getIssuePartyCode();
        if (this$issuePartyCode == null ? other$issuePartyCode != null : !this$issuePartyCode.equals(other$issuePartyCode)) return false;
        final java.lang.Object this$dateOfIssue = this.getDateOfIssue();
        final java.lang.Object other$dateOfIssue = other.getDateOfIssue();
        if (this$dateOfIssue == null ? other$dateOfIssue != null : !this$dateOfIssue.equals(other$dateOfIssue)) return false;
        final java.lang.Object this$departureStation = this.getDepartureStation();
        final java.lang.Object other$departureStation = other.getDepartureStation();
        if (this$departureStation == null ? other$departureStation != null : !this$departureStation.equals(other$departureStation)) return false;
        final java.lang.Object this$destinationStation = this.getDestinationStation();
        final java.lang.Object other$destinationStation = other.getDestinationStation();
        if (this$destinationStation == null ? other$destinationStation != null : !this$destinationStation.equals(other$destinationStation)) return false;
        final java.lang.Object this$trainNumber = this.getTrainNumber();
        final java.lang.Object other$trainNumber = other.getTrainNumber();
        if (this$trainNumber == null ? other$trainNumber != null : !this$trainNumber.equals(other$trainNumber)) return false;
        final java.lang.Object this$travelDate = this.getTravelDate();
        final java.lang.Object other$travelDate = other.getTravelDate();
        if (this$travelDate == null ? other$travelDate != null : !this$travelDate.equals(other$travelDate)) return false;
        final java.lang.Object this$departureTime = this.getDepartureTime();
        final java.lang.Object other$departureTime = other.getDepartureTime();
        if (this$departureTime == null ? other$departureTime != null : !this$departureTime.equals(other$departureTime)) return false;
        final java.lang.Object this$seatLevel = this.getSeatLevel();
        final java.lang.Object other$seatLevel = other.getSeatLevel();
        if (this$seatLevel == null ? other$seatLevel != null : !this$seatLevel.equals(other$seatLevel)) return false;
        final java.lang.Object this$carriage = this.getCarriage();
        final java.lang.Object other$carriage = other.getCarriage();
        if (this$carriage == null ? other$carriage != null : !this$carriage.equals(other$carriage)) return false;
        final java.lang.Object this$seat = this.getSeat();
        final java.lang.Object other$seat = other.getSeat();
        if (this$seat == null ? other$seat != null : !this$seat.equals(other$seat)) return false;
        final java.lang.Object this$passengerName = this.getPassengerName();
        final java.lang.Object other$passengerName = other.getPassengerName();
        if (this$passengerName == null ? other$passengerName != null : !this$passengerName.equals(other$passengerName)) return false;
        final java.lang.Object this$idNumber = this.getIdNumber();
        final java.lang.Object other$idNumber = other.getIdNumber();
        if (this$idNumber == null ? other$idNumber != null : !this$idNumber.equals(other$idNumber)) return false;
        final java.lang.Object this$fare = this.getFare();
        final java.lang.Object other$fare = other.getFare();
        if (this$fare == null ? other$fare != null : !this$fare.equals(other$fare)) return false;
        final java.lang.Object this$taxRate = this.getTaxRate();
        final java.lang.Object other$taxRate = other.getTaxRate();
        if (this$taxRate == null ? other$taxRate != null : !this$taxRate.equals(other$taxRate)) return false;
        final java.lang.Object this$taxAmount = this.getTaxAmount();
        final java.lang.Object other$taxAmount = other.getTaxAmount();
        if (this$taxAmount == null ? other$taxAmount != null : !this$taxAmount.equals(other$taxAmount)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$buyerName = this.getBuyerName();
        final java.lang.Object other$buyerName = other.getBuyerName();
        if (this$buyerName == null ? other$buyerName != null : !this$buyerName.equals(other$buyerName)) return false;
        final java.lang.Object this$buyerTaxNo = this.getBuyerTaxNo();
        final java.lang.Object other$buyerTaxNo = other.getBuyerTaxNo();
        if (this$buyerTaxNo == null ? other$buyerTaxNo != null : !this$buyerTaxNo.equals(other$buyerTaxNo)) return false;
        final java.lang.Object this$sellerName = this.getSellerName();
        final java.lang.Object other$sellerName = other.getSellerName();
        if (this$sellerName == null ? other$sellerName != null : !this$sellerName.equals(other$sellerName)) return false;
        final java.lang.Object this$sellerTaxNo = this.getSellerTaxNo();
        final java.lang.Object other$sellerTaxNo = other.getSellerTaxNo();
        if (this$sellerTaxNo == null ? other$sellerTaxNo != null : !this$sellerTaxNo.equals(other$sellerTaxNo)) return false;
        final java.lang.Object this$remarks = this.getRemarks();
        final java.lang.Object other$remarks = other.getRemarks();
        if (this$remarks == null ? other$remarks != null : !this$remarks.equals(other$remarks)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ElectronicTrainTicket;
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
        final java.lang.Object $issuePartyCode = this.getIssuePartyCode();
        result = result * PRIME + ($issuePartyCode == null ? 43 : $issuePartyCode.hashCode());
        final java.lang.Object $dateOfIssue = this.getDateOfIssue();
        result = result * PRIME + ($dateOfIssue == null ? 43 : $dateOfIssue.hashCode());
        final java.lang.Object $departureStation = this.getDepartureStation();
        result = result * PRIME + ($departureStation == null ? 43 : $departureStation.hashCode());
        final java.lang.Object $destinationStation = this.getDestinationStation();
        result = result * PRIME + ($destinationStation == null ? 43 : $destinationStation.hashCode());
        final java.lang.Object $trainNumber = this.getTrainNumber();
        result = result * PRIME + ($trainNumber == null ? 43 : $trainNumber.hashCode());
        final java.lang.Object $travelDate = this.getTravelDate();
        result = result * PRIME + ($travelDate == null ? 43 : $travelDate.hashCode());
        final java.lang.Object $departureTime = this.getDepartureTime();
        result = result * PRIME + ($departureTime == null ? 43 : $departureTime.hashCode());
        final java.lang.Object $seatLevel = this.getSeatLevel();
        result = result * PRIME + ($seatLevel == null ? 43 : $seatLevel.hashCode());
        final java.lang.Object $carriage = this.getCarriage();
        result = result * PRIME + ($carriage == null ? 43 : $carriage.hashCode());
        final java.lang.Object $seat = this.getSeat();
        result = result * PRIME + ($seat == null ? 43 : $seat.hashCode());
        final java.lang.Object $passengerName = this.getPassengerName();
        result = result * PRIME + ($passengerName == null ? 43 : $passengerName.hashCode());
        final java.lang.Object $idNumber = this.getIdNumber();
        result = result * PRIME + ($idNumber == null ? 43 : $idNumber.hashCode());
        final java.lang.Object $fare = this.getFare();
        result = result * PRIME + ($fare == null ? 43 : $fare.hashCode());
        final java.lang.Object $taxRate = this.getTaxRate();
        result = result * PRIME + ($taxRate == null ? 43 : $taxRate.hashCode());
        final java.lang.Object $taxAmount = this.getTaxAmount();
        result = result * PRIME + ($taxAmount == null ? 43 : $taxAmount.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $buyerName = this.getBuyerName();
        result = result * PRIME + ($buyerName == null ? 43 : $buyerName.hashCode());
        final java.lang.Object $buyerTaxNo = this.getBuyerTaxNo();
        result = result * PRIME + ($buyerTaxNo == null ? 43 : $buyerTaxNo.hashCode());
        final java.lang.Object $sellerName = this.getSellerName();
        result = result * PRIME + ($sellerName == null ? 43 : $sellerName.hashCode());
        final java.lang.Object $sellerTaxNo = this.getSellerTaxNo();
        result = result * PRIME + ($sellerTaxNo == null ? 43 : $sellerTaxNo.hashCode());
        final java.lang.Object $remarks = this.getRemarks();
        result = result * PRIME + ($remarks == null ? 43 : $remarks.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ElectronicTrainTicket(id=" + this.getId() + ", tenantId=" + this.getTenantId() + ", voucherId=" + this.getVoucherId() + ", eTicketNumber=" + this.getETicketNumber() + ", electronicInvoiceNumber=" + this.getElectronicInvoiceNumber() + ", issueParty=" + this.getIssueParty() + ", issuePartyCode=" + this.getIssuePartyCode() + ", dateOfIssue=" + this.getDateOfIssue() + ", departureStation=" + this.getDepartureStation() + ", destinationStation=" + this.getDestinationStation() + ", trainNumber=" + this.getTrainNumber() + ", travelDate=" + this.getTravelDate() + ", departureTime=" + this.getDepartureTime() + ", seatLevel=" + this.getSeatLevel() + ", carriage=" + this.getCarriage() + ", seat=" + this.getSeat() + ", passengerName=" + this.getPassengerName() + ", idNumber=" + this.getIdNumber() + ", fare=" + this.getFare() + ", taxRate=" + this.getTaxRate() + ", taxAmount=" + this.getTaxAmount() + ", totalAmount=" + this.getTotalAmount() + ", buyerName=" + this.getBuyerName() + ", buyerTaxNo=" + this.getBuyerTaxNo() + ", sellerName=" + this.getSellerName() + ", sellerTaxNo=" + this.getSellerTaxNo() + ", remarks=" + this.getRemarks() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}
