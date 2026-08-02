package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 电子发票VO
 */
@Schema(description = "电子发票VO")
public class ElectronicInvoiceVO {
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "发票类型")
    private String invoiceType;
    @Schema(description = "发票类型名称")
    private String invoiceTypeName;
    @Schema(description = "发票代码")
    private String invoiceCode;
    @Schema(description = "发票号码")
    private String invoiceNo;
    @Schema(description = "开票日期")
    private LocalDate issueDate;
    @Schema(description = "购买方名称")
    private String buyerName;
    @Schema(description = "购买方税号")
    private String buyerTaxNo;
    @Schema(description = "购买方地址电话")
    private String buyerAddress;
    @Schema(description = "购买方开户行账号")
    private String buyerBank;
    @Schema(description = "销售方名称")
    private String sellerName;
    @Schema(description = "销售方税号")
    private String sellerTaxNo;
    @Schema(description = "销售方地址电话")
    private String sellerAddress;
    @Schema(description = "销售方开户行账号")
    private String sellerBank;
    @Schema(description = "价税合计")
    private BigDecimal totalAmount;
    @Schema(description = "税额")
    private BigDecimal taxAmount;
    @Schema(description = "不含税金额")
    private BigDecimal amountWithoutTax;
    @Schema(description = "币种")
    private String currency;
    @Schema(description = "抵扣状态")
    private Integer deductibleStatus;
    @Schema(description = "可抵扣税额")
    private BigDecimal deductibleAmount;
    @Schema(description = "认证状态")
    private Integer certifyStatus;
    @Schema(description = "认证时间")
    private String certifyTime;
    @Schema(description = "认证所属期")
    private String certifyPeriod;
    @Schema(description = "进项税额转出金额")
    private BigDecimal transferOutAmount;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "校验码")
    private String checkCode;
    @Schema(description = "机器编号")
    private String machineNo;
    @Schema(description = "收款人")
    private String payee;
    @Schema(description = "复核人")
    private String checker;
    @Schema(description = "开票人")
    private String issuer;
    @Schema(description = "PDF文件路径")
    private String pdfUrl;
    @Schema(description = "OFD文件路径")
    private String ofdUrl;
    @Schema(description = "XML文件路径")
    private String xmlUrl;
    @Schema(description = "发票明细行项目")
    private List<ElectronicInvoiceItemVO> items;

    public ElectronicInvoiceVO() {
    }

    public Long getId() {
        return this.id;
    }

    public String getInvoiceType() {
        return this.invoiceType;
    }

    public String getInvoiceTypeName() {
        return this.invoiceTypeName;
    }

    public String getInvoiceCode() {
        return this.invoiceCode;
    }

    public String getInvoiceNo() {
        return this.invoiceNo;
    }

    public LocalDate getIssueDate() {
        return this.issueDate;
    }

    public String getBuyerName() {
        return this.buyerName;
    }

    public String getBuyerTaxNo() {
        return this.buyerTaxNo;
    }

    public String getBuyerAddress() {
        return this.buyerAddress;
    }

    public String getBuyerBank() {
        return this.buyerBank;
    }

    public String getSellerName() {
        return this.sellerName;
    }

    public String getSellerTaxNo() {
        return this.sellerTaxNo;
    }

    public String getSellerAddress() {
        return this.sellerAddress;
    }

    public String getSellerBank() {
        return this.sellerBank;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public BigDecimal getTaxAmount() {
        return this.taxAmount;
    }

    public BigDecimal getAmountWithoutTax() {
        return this.amountWithoutTax;
    }

    public String getCurrency() {
        return this.currency;
    }

    public Integer getDeductibleStatus() {
        return this.deductibleStatus;
    }

    public BigDecimal getDeductibleAmount() {
        return this.deductibleAmount;
    }

    public Integer getCertifyStatus() {
        return this.certifyStatus;
    }

    public String getCertifyTime() {
        return this.certifyTime;
    }

    public String getCertifyPeriod() {
        return this.certifyPeriod;
    }

    public BigDecimal getTransferOutAmount() {
        return this.transferOutAmount;
    }

    public String getRemark() {
        return this.remark;
    }

    public String getCheckCode() {
        return this.checkCode;
    }

    public String getMachineNo() {
        return this.machineNo;
    }

    public String getPayee() {
        return this.payee;
    }

    public String getChecker() {
        return this.checker;
    }

    public String getIssuer() {
        return this.issuer;
    }

    public String getPdfUrl() {
        return this.pdfUrl;
    }

    public String getOfdUrl() {
        return this.ofdUrl;
    }

    public String getXmlUrl() {
        return this.xmlUrl;
    }

    public List<ElectronicInvoiceItemVO> getItems() {
        return this.items;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setInvoiceType(final String invoiceType) {
        this.invoiceType = invoiceType;
    }

    public void setInvoiceTypeName(final String invoiceTypeName) {
        this.invoiceTypeName = invoiceTypeName;
    }

    public void setInvoiceCode(final String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    public void setInvoiceNo(final String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public void setIssueDate(final LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public void setBuyerName(final String buyerName) {
        this.buyerName = buyerName;
    }

    public void setBuyerTaxNo(final String buyerTaxNo) {
        this.buyerTaxNo = buyerTaxNo;
    }

    public void setBuyerAddress(final String buyerAddress) {
        this.buyerAddress = buyerAddress;
    }

    public void setBuyerBank(final String buyerBank) {
        this.buyerBank = buyerBank;
    }

    public void setSellerName(final String sellerName) {
        this.sellerName = sellerName;
    }

    public void setSellerTaxNo(final String sellerTaxNo) {
        this.sellerTaxNo = sellerTaxNo;
    }

    public void setSellerAddress(final String sellerAddress) {
        this.sellerAddress = sellerAddress;
    }

    public void setSellerBank(final String sellerBank) {
        this.sellerBank = sellerBank;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setTaxAmount(final BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public void setAmountWithoutTax(final BigDecimal amountWithoutTax) {
        this.amountWithoutTax = amountWithoutTax;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public void setDeductibleStatus(final Integer deductibleStatus) {
        this.deductibleStatus = deductibleStatus;
    }

    public void setDeductibleAmount(final BigDecimal deductibleAmount) {
        this.deductibleAmount = deductibleAmount;
    }

    public void setCertifyStatus(final Integer certifyStatus) {
        this.certifyStatus = certifyStatus;
    }

    public void setCertifyTime(final String certifyTime) {
        this.certifyTime = certifyTime;
    }

    public void setCertifyPeriod(final String certifyPeriod) {
        this.certifyPeriod = certifyPeriod;
    }

    public void setTransferOutAmount(final BigDecimal transferOutAmount) {
        this.transferOutAmount = transferOutAmount;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setCheckCode(final String checkCode) {
        this.checkCode = checkCode;
    }

    public void setMachineNo(final String machineNo) {
        this.machineNo = machineNo;
    }

    public void setPayee(final String payee) {
        this.payee = payee;
    }

    public void setChecker(final String checker) {
        this.checker = checker;
    }

    public void setIssuer(final String issuer) {
        this.issuer = issuer;
    }

    public void setPdfUrl(final String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public void setOfdUrl(final String ofdUrl) {
        this.ofdUrl = ofdUrl;
    }

    public void setXmlUrl(final String xmlUrl) {
        this.xmlUrl = xmlUrl;
    }

    public void setItems(final List<ElectronicInvoiceItemVO> items) {
        this.items = items;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ElectronicInvoiceVO)) return false;
        final ElectronicInvoiceVO other = (ElectronicInvoiceVO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$deductibleStatus = this.getDeductibleStatus();
        final java.lang.Object other$deductibleStatus = other.getDeductibleStatus();
        if (this$deductibleStatus == null ? other$deductibleStatus != null : !this$deductibleStatus.equals(other$deductibleStatus)) return false;
        final java.lang.Object this$certifyStatus = this.getCertifyStatus();
        final java.lang.Object other$certifyStatus = other.getCertifyStatus();
        if (this$certifyStatus == null ? other$certifyStatus != null : !this$certifyStatus.equals(other$certifyStatus)) return false;
        final java.lang.Object this$invoiceType = this.getInvoiceType();
        final java.lang.Object other$invoiceType = other.getInvoiceType();
        if (this$invoiceType == null ? other$invoiceType != null : !this$invoiceType.equals(other$invoiceType)) return false;
        final java.lang.Object this$invoiceTypeName = this.getInvoiceTypeName();
        final java.lang.Object other$invoiceTypeName = other.getInvoiceTypeName();
        if (this$invoiceTypeName == null ? other$invoiceTypeName != null : !this$invoiceTypeName.equals(other$invoiceTypeName)) return false;
        final java.lang.Object this$invoiceCode = this.getInvoiceCode();
        final java.lang.Object other$invoiceCode = other.getInvoiceCode();
        if (this$invoiceCode == null ? other$invoiceCode != null : !this$invoiceCode.equals(other$invoiceCode)) return false;
        final java.lang.Object this$invoiceNo = this.getInvoiceNo();
        final java.lang.Object other$invoiceNo = other.getInvoiceNo();
        if (this$invoiceNo == null ? other$invoiceNo != null : !this$invoiceNo.equals(other$invoiceNo)) return false;
        final java.lang.Object this$issueDate = this.getIssueDate();
        final java.lang.Object other$issueDate = other.getIssueDate();
        if (this$issueDate == null ? other$issueDate != null : !this$issueDate.equals(other$issueDate)) return false;
        final java.lang.Object this$buyerName = this.getBuyerName();
        final java.lang.Object other$buyerName = other.getBuyerName();
        if (this$buyerName == null ? other$buyerName != null : !this$buyerName.equals(other$buyerName)) return false;
        final java.lang.Object this$buyerTaxNo = this.getBuyerTaxNo();
        final java.lang.Object other$buyerTaxNo = other.getBuyerTaxNo();
        if (this$buyerTaxNo == null ? other$buyerTaxNo != null : !this$buyerTaxNo.equals(other$buyerTaxNo)) return false;
        final java.lang.Object this$buyerAddress = this.getBuyerAddress();
        final java.lang.Object other$buyerAddress = other.getBuyerAddress();
        if (this$buyerAddress == null ? other$buyerAddress != null : !this$buyerAddress.equals(other$buyerAddress)) return false;
        final java.lang.Object this$buyerBank = this.getBuyerBank();
        final java.lang.Object other$buyerBank = other.getBuyerBank();
        if (this$buyerBank == null ? other$buyerBank != null : !this$buyerBank.equals(other$buyerBank)) return false;
        final java.lang.Object this$sellerName = this.getSellerName();
        final java.lang.Object other$sellerName = other.getSellerName();
        if (this$sellerName == null ? other$sellerName != null : !this$sellerName.equals(other$sellerName)) return false;
        final java.lang.Object this$sellerTaxNo = this.getSellerTaxNo();
        final java.lang.Object other$sellerTaxNo = other.getSellerTaxNo();
        if (this$sellerTaxNo == null ? other$sellerTaxNo != null : !this$sellerTaxNo.equals(other$sellerTaxNo)) return false;
        final java.lang.Object this$sellerAddress = this.getSellerAddress();
        final java.lang.Object other$sellerAddress = other.getSellerAddress();
        if (this$sellerAddress == null ? other$sellerAddress != null : !this$sellerAddress.equals(other$sellerAddress)) return false;
        final java.lang.Object this$sellerBank = this.getSellerBank();
        final java.lang.Object other$sellerBank = other.getSellerBank();
        if (this$sellerBank == null ? other$sellerBank != null : !this$sellerBank.equals(other$sellerBank)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$taxAmount = this.getTaxAmount();
        final java.lang.Object other$taxAmount = other.getTaxAmount();
        if (this$taxAmount == null ? other$taxAmount != null : !this$taxAmount.equals(other$taxAmount)) return false;
        final java.lang.Object this$amountWithoutTax = this.getAmountWithoutTax();
        final java.lang.Object other$amountWithoutTax = other.getAmountWithoutTax();
        if (this$amountWithoutTax == null ? other$amountWithoutTax != null : !this$amountWithoutTax.equals(other$amountWithoutTax)) return false;
        final java.lang.Object this$currency = this.getCurrency();
        final java.lang.Object other$currency = other.getCurrency();
        if (this$currency == null ? other$currency != null : !this$currency.equals(other$currency)) return false;
        final java.lang.Object this$deductibleAmount = this.getDeductibleAmount();
        final java.lang.Object other$deductibleAmount = other.getDeductibleAmount();
        if (this$deductibleAmount == null ? other$deductibleAmount != null : !this$deductibleAmount.equals(other$deductibleAmount)) return false;
        final java.lang.Object this$certifyTime = this.getCertifyTime();
        final java.lang.Object other$certifyTime = other.getCertifyTime();
        if (this$certifyTime == null ? other$certifyTime != null : !this$certifyTime.equals(other$certifyTime)) return false;
        final java.lang.Object this$certifyPeriod = this.getCertifyPeriod();
        final java.lang.Object other$certifyPeriod = other.getCertifyPeriod();
        if (this$certifyPeriod == null ? other$certifyPeriod != null : !this$certifyPeriod.equals(other$certifyPeriod)) return false;
        final java.lang.Object this$transferOutAmount = this.getTransferOutAmount();
        final java.lang.Object other$transferOutAmount = other.getTransferOutAmount();
        if (this$transferOutAmount == null ? other$transferOutAmount != null : !this$transferOutAmount.equals(other$transferOutAmount)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$checkCode = this.getCheckCode();
        final java.lang.Object other$checkCode = other.getCheckCode();
        if (this$checkCode == null ? other$checkCode != null : !this$checkCode.equals(other$checkCode)) return false;
        final java.lang.Object this$machineNo = this.getMachineNo();
        final java.lang.Object other$machineNo = other.getMachineNo();
        if (this$machineNo == null ? other$machineNo != null : !this$machineNo.equals(other$machineNo)) return false;
        final java.lang.Object this$payee = this.getPayee();
        final java.lang.Object other$payee = other.getPayee();
        if (this$payee == null ? other$payee != null : !this$payee.equals(other$payee)) return false;
        final java.lang.Object this$checker = this.getChecker();
        final java.lang.Object other$checker = other.getChecker();
        if (this$checker == null ? other$checker != null : !this$checker.equals(other$checker)) return false;
        final java.lang.Object this$issuer = this.getIssuer();
        final java.lang.Object other$issuer = other.getIssuer();
        if (this$issuer == null ? other$issuer != null : !this$issuer.equals(other$issuer)) return false;
        final java.lang.Object this$pdfUrl = this.getPdfUrl();
        final java.lang.Object other$pdfUrl = other.getPdfUrl();
        if (this$pdfUrl == null ? other$pdfUrl != null : !this$pdfUrl.equals(other$pdfUrl)) return false;
        final java.lang.Object this$ofdUrl = this.getOfdUrl();
        final java.lang.Object other$ofdUrl = other.getOfdUrl();
        if (this$ofdUrl == null ? other$ofdUrl != null : !this$ofdUrl.equals(other$ofdUrl)) return false;
        final java.lang.Object this$xmlUrl = this.getXmlUrl();
        final java.lang.Object other$xmlUrl = other.getXmlUrl();
        if (this$xmlUrl == null ? other$xmlUrl != null : !this$xmlUrl.equals(other$xmlUrl)) return false;
        final java.lang.Object this$items = this.getItems();
        final java.lang.Object other$items = other.getItems();
        if (this$items == null ? other$items != null : !this$items.equals(other$items)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ElectronicInvoiceVO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $deductibleStatus = this.getDeductibleStatus();
        result = result * PRIME + ($deductibleStatus == null ? 43 : $deductibleStatus.hashCode());
        final java.lang.Object $certifyStatus = this.getCertifyStatus();
        result = result * PRIME + ($certifyStatus == null ? 43 : $certifyStatus.hashCode());
        final java.lang.Object $invoiceType = this.getInvoiceType();
        result = result * PRIME + ($invoiceType == null ? 43 : $invoiceType.hashCode());
        final java.lang.Object $invoiceTypeName = this.getInvoiceTypeName();
        result = result * PRIME + ($invoiceTypeName == null ? 43 : $invoiceTypeName.hashCode());
        final java.lang.Object $invoiceCode = this.getInvoiceCode();
        result = result * PRIME + ($invoiceCode == null ? 43 : $invoiceCode.hashCode());
        final java.lang.Object $invoiceNo = this.getInvoiceNo();
        result = result * PRIME + ($invoiceNo == null ? 43 : $invoiceNo.hashCode());
        final java.lang.Object $issueDate = this.getIssueDate();
        result = result * PRIME + ($issueDate == null ? 43 : $issueDate.hashCode());
        final java.lang.Object $buyerName = this.getBuyerName();
        result = result * PRIME + ($buyerName == null ? 43 : $buyerName.hashCode());
        final java.lang.Object $buyerTaxNo = this.getBuyerTaxNo();
        result = result * PRIME + ($buyerTaxNo == null ? 43 : $buyerTaxNo.hashCode());
        final java.lang.Object $buyerAddress = this.getBuyerAddress();
        result = result * PRIME + ($buyerAddress == null ? 43 : $buyerAddress.hashCode());
        final java.lang.Object $buyerBank = this.getBuyerBank();
        result = result * PRIME + ($buyerBank == null ? 43 : $buyerBank.hashCode());
        final java.lang.Object $sellerName = this.getSellerName();
        result = result * PRIME + ($sellerName == null ? 43 : $sellerName.hashCode());
        final java.lang.Object $sellerTaxNo = this.getSellerTaxNo();
        result = result * PRIME + ($sellerTaxNo == null ? 43 : $sellerTaxNo.hashCode());
        final java.lang.Object $sellerAddress = this.getSellerAddress();
        result = result * PRIME + ($sellerAddress == null ? 43 : $sellerAddress.hashCode());
        final java.lang.Object $sellerBank = this.getSellerBank();
        result = result * PRIME + ($sellerBank == null ? 43 : $sellerBank.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $taxAmount = this.getTaxAmount();
        result = result * PRIME + ($taxAmount == null ? 43 : $taxAmount.hashCode());
        final java.lang.Object $amountWithoutTax = this.getAmountWithoutTax();
        result = result * PRIME + ($amountWithoutTax == null ? 43 : $amountWithoutTax.hashCode());
        final java.lang.Object $currency = this.getCurrency();
        result = result * PRIME + ($currency == null ? 43 : $currency.hashCode());
        final java.lang.Object $deductibleAmount = this.getDeductibleAmount();
        result = result * PRIME + ($deductibleAmount == null ? 43 : $deductibleAmount.hashCode());
        final java.lang.Object $certifyTime = this.getCertifyTime();
        result = result * PRIME + ($certifyTime == null ? 43 : $certifyTime.hashCode());
        final java.lang.Object $certifyPeriod = this.getCertifyPeriod();
        result = result * PRIME + ($certifyPeriod == null ? 43 : $certifyPeriod.hashCode());
        final java.lang.Object $transferOutAmount = this.getTransferOutAmount();
        result = result * PRIME + ($transferOutAmount == null ? 43 : $transferOutAmount.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $checkCode = this.getCheckCode();
        result = result * PRIME + ($checkCode == null ? 43 : $checkCode.hashCode());
        final java.lang.Object $machineNo = this.getMachineNo();
        result = result * PRIME + ($machineNo == null ? 43 : $machineNo.hashCode());
        final java.lang.Object $payee = this.getPayee();
        result = result * PRIME + ($payee == null ? 43 : $payee.hashCode());
        final java.lang.Object $checker = this.getChecker();
        result = result * PRIME + ($checker == null ? 43 : $checker.hashCode());
        final java.lang.Object $issuer = this.getIssuer();
        result = result * PRIME + ($issuer == null ? 43 : $issuer.hashCode());
        final java.lang.Object $pdfUrl = this.getPdfUrl();
        result = result * PRIME + ($pdfUrl == null ? 43 : $pdfUrl.hashCode());
        final java.lang.Object $ofdUrl = this.getOfdUrl();
        result = result * PRIME + ($ofdUrl == null ? 43 : $ofdUrl.hashCode());
        final java.lang.Object $xmlUrl = this.getXmlUrl();
        result = result * PRIME + ($xmlUrl == null ? 43 : $xmlUrl.hashCode());
        final java.lang.Object $items = this.getItems();
        result = result * PRIME + ($items == null ? 43 : $items.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ElectronicInvoiceVO(id=" + this.getId() + ", invoiceType=" + this.getInvoiceType() + ", invoiceTypeName=" + this.getInvoiceTypeName() + ", invoiceCode=" + this.getInvoiceCode() + ", invoiceNo=" + this.getInvoiceNo() + ", issueDate=" + this.getIssueDate() + ", buyerName=" + this.getBuyerName() + ", buyerTaxNo=" + this.getBuyerTaxNo() + ", buyerAddress=" + this.getBuyerAddress() + ", buyerBank=" + this.getBuyerBank() + ", sellerName=" + this.getSellerName() + ", sellerTaxNo=" + this.getSellerTaxNo() + ", sellerAddress=" + this.getSellerAddress() + ", sellerBank=" + this.getSellerBank() + ", totalAmount=" + this.getTotalAmount() + ", taxAmount=" + this.getTaxAmount() + ", amountWithoutTax=" + this.getAmountWithoutTax() + ", currency=" + this.getCurrency() + ", deductibleStatus=" + this.getDeductibleStatus() + ", deductibleAmount=" + this.getDeductibleAmount() + ", certifyStatus=" + this.getCertifyStatus() + ", certifyTime=" + this.getCertifyTime() + ", certifyPeriod=" + this.getCertifyPeriod() + ", transferOutAmount=" + this.getTransferOutAmount() + ", remark=" + this.getRemark() + ", checkCode=" + this.getCheckCode() + ", machineNo=" + this.getMachineNo() + ", payee=" + this.getPayee() + ", checker=" + this.getChecker() + ", issuer=" + this.getIssuer() + ", pdfUrl=" + this.getPdfUrl() + ", ofdUrl=" + this.getOfdUrl() + ", xmlUrl=" + this.getXmlUrl() + ", items=" + this.getItems() + ")";
    }
}
