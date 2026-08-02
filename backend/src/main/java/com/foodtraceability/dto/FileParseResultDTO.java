package com.foodtraceability.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件解析结果DTO
 */
public class FileParseResultDTO {
    /**
     * 解析是否成功
     */
    private boolean success;
    /**
     * 错误信息
     */
    private String errorMessage;
    /**
     * 文件类型：pdf, ofd, xml
     */
    private String fileType;
    /**
     * 凭证类型：invoice, train_ticket, flight_ticket, bank_receipt, other
     */
    private String voucherType;
    /**
     * 凭证类型描述
     */
    private String voucherTypeDesc;
    /**
     * 提取的原始XML内容
     */
    private String xmlContent;
    /**
     * 转换后的JSON数据
     */
    private String jsonData;
    /**
     * 发票号码（如果是发票类型）
     */
    private String invoiceNo;
    /**
     * 发票代码（如果是发票类型）
     */
    private String invoiceCode;
    /**
     * 发票类型代码
     */
    private String invoiceType;
    /**
     * 开票日期
     */
    private String issueDate;
    /**
     * 购买方名称
     */
    private String buyerName;
    /**
     * 购买方税号
     */
    private String buyerTaxNo;
    /**
     * 销售方名称
     */
    private String sellerName;
    /**
     * 销售方税号
     */
    private String sellerTaxNo;
    /**
     * 价税合计金额
     */
    private String totalAmount;
    /**
     * 金额（不含税）
     */
    private String amountWithoutTax;
    /**
     * 税额
     */
    private String taxAmount;
    /**
     * 机器编号
     */
    private String machineNo;
    /**
     * 购买方地址电话
     */
    private String buyerAddressPhone;
    /**
     * 购买方开户行账号
     */
    private String buyerBankAccount;
    /**
     * 销售方地址电话
     */
    private String sellerAddressPhone;
    /**
     * 销售方开户行账号
     */
    private String sellerBankAccount;
    /**
     * 校验码（发票校验码后6位）
     */
    private String checkCode;
    /**
     * 货物或应税劳务、服务名称
     */
    private String goodsName;
    /**
     * 规格型号
     */
    private String goodsSpec;
    /**
     * 单位
     */
    private String goodsUnit;
    /**
     * 数量
     */
    private String goodsQuantity;
    /**
     * 单价
     */
    private String goodsPrice;
    /**
     * 税率
     */
    private String taxRate;
    /**
     * 商品明细列表（支持多商品行）
     */
    private List<InvoiceGoodsItemDTO> goodsItems;
    /**
     * 收款人
     */
    private String payee;
    /**
     * 复核人
     */
    private String checker;
    /**
     * 开票人
     */
    private String issuer;
    /**
     * 备注
     */
    private String remarks;
    /**
     * 是否包含数字签名
     */
    private boolean hasSignature;
    /**
     * 签名验证结果
     */
    private Integer signatureStatus;
    /**
     * 签名验证描述
     */
    private String signatureStatusDesc;
    /**
     * 解析时间
     */
    private LocalDateTime parseTime;
    /**
     * OCR原始识别文本
     */
    private String rawText;

    /**
     * 创建成功结果
     */
    public static FileParseResultDTO success(String fileType, String voucherType, String xmlContent) {
        return FileParseResultDTO.builder().success(true).fileType(fileType).voucherType(voucherType).xmlContent(xmlContent).parseTime(LocalDateTime.now()).build();
    }

    /**
     * 创建失败结果
     */
    public static FileParseResultDTO failed(String errorMessage) {
        return FileParseResultDTO.builder().success(false).errorMessage(errorMessage).parseTime(LocalDateTime.now()).build();
    }

    /**
     * 获取JSON数据（将当前对象序列化为JSON）
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getJsonData() {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return null;
        }
    }


    public static class FileParseResultDTOBuilder {
        private boolean success;
        private String errorMessage;
        private String fileType;
        private String voucherType;
        private String voucherTypeDesc;
        private String xmlContent;
        private String jsonData;
        private String invoiceNo;
        private String invoiceCode;
        private String invoiceType;
        private String issueDate;
        private String buyerName;
        private String buyerTaxNo;
        private String sellerName;
        private String sellerTaxNo;
        private String totalAmount;
        private String amountWithoutTax;
        private String taxAmount;
        private String machineNo;
        private String buyerAddressPhone;
        private String buyerBankAccount;
        private String sellerAddressPhone;
        private String sellerBankAccount;
        private String checkCode;
        private String goodsName;
        private String goodsSpec;
        private String goodsUnit;
        private String goodsQuantity;
        private String goodsPrice;
        private String taxRate;
        private List<InvoiceGoodsItemDTO> goodsItems;
        private String payee;
        private String checker;
        private String issuer;
        private String remarks;
        private boolean hasSignature;
        private Integer signatureStatus;
        private String signatureStatusDesc;
        private LocalDateTime parseTime;
        private String rawText;

        FileParseResultDTOBuilder() {
        }

        /**
         * 解析是否成功
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder success(final boolean success) {
            this.success = success;
            return this;
        }

        /**
         * 错误信息
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder errorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        /**
         * 文件类型：pdf, ofd, xml
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder fileType(final String fileType) {
            this.fileType = fileType;
            return this;
        }

        /**
         * 凭证类型：invoice, train_ticket, flight_ticket, bank_receipt, other
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder voucherType(final String voucherType) {
            this.voucherType = voucherType;
            return this;
        }

        /**
         * 凭证类型描述
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder voucherTypeDesc(final String voucherTypeDesc) {
            this.voucherTypeDesc = voucherTypeDesc;
            return this;
        }

        /**
         * 提取的原始XML内容
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder xmlContent(final String xmlContent) {
            this.xmlContent = xmlContent;
            return this;
        }

        /**
         * 转换后的JSON数据
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder jsonData(final String jsonData) {
            this.jsonData = jsonData;
            return this;
        }

        /**
         * 发票号码（如果是发票类型）
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder invoiceNo(final String invoiceNo) {
            this.invoiceNo = invoiceNo;
            return this;
        }

        /**
         * 发票代码（如果是发票类型）
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder invoiceCode(final String invoiceCode) {
            this.invoiceCode = invoiceCode;
            return this;
        }

        /**
         * 发票类型代码
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder invoiceType(final String invoiceType) {
            this.invoiceType = invoiceType;
            return this;
        }

        /**
         * 开票日期
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder issueDate(final String issueDate) {
            this.issueDate = issueDate;
            return this;
        }

        /**
         * 购买方名称
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder buyerName(final String buyerName) {
            this.buyerName = buyerName;
            return this;
        }

        /**
         * 购买方税号
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder buyerTaxNo(final String buyerTaxNo) {
            this.buyerTaxNo = buyerTaxNo;
            return this;
        }

        /**
         * 销售方名称
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder sellerName(final String sellerName) {
            this.sellerName = sellerName;
            return this;
        }

        /**
         * 销售方税号
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder sellerTaxNo(final String sellerTaxNo) {
            this.sellerTaxNo = sellerTaxNo;
            return this;
        }

        /**
         * 价税合计金额
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder totalAmount(final String totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        /**
         * 金额（不含税）
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder amountWithoutTax(final String amountWithoutTax) {
            this.amountWithoutTax = amountWithoutTax;
            return this;
        }

        /**
         * 税额
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder taxAmount(final String taxAmount) {
            this.taxAmount = taxAmount;
            return this;
        }

        /**
         * 机器编号
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder machineNo(final String machineNo) {
            this.machineNo = machineNo;
            return this;
        }

        /**
         * 购买方地址电话
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder buyerAddressPhone(final String buyerAddressPhone) {
            this.buyerAddressPhone = buyerAddressPhone;
            return this;
        }

        /**
         * 购买方开户行账号
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder buyerBankAccount(final String buyerBankAccount) {
            this.buyerBankAccount = buyerBankAccount;
            return this;
        }

        /**
         * 销售方地址电话
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder sellerAddressPhone(final String sellerAddressPhone) {
            this.sellerAddressPhone = sellerAddressPhone;
            return this;
        }

        /**
         * 销售方开户行账号
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder sellerBankAccount(final String sellerBankAccount) {
            this.sellerBankAccount = sellerBankAccount;
            return this;
        }

        /**
         * 校验码（发票校验码后6位）
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder checkCode(final String checkCode) {
            this.checkCode = checkCode;
            return this;
        }

        /**
         * 货物或应税劳务、服务名称
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder goodsName(final String goodsName) {
            this.goodsName = goodsName;
            return this;
        }

        /**
         * 规格型号
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder goodsSpec(final String goodsSpec) {
            this.goodsSpec = goodsSpec;
            return this;
        }

        /**
         * 单位
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder goodsUnit(final String goodsUnit) {
            this.goodsUnit = goodsUnit;
            return this;
        }

        /**
         * 数量
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder goodsQuantity(final String goodsQuantity) {
            this.goodsQuantity = goodsQuantity;
            return this;
        }

        /**
         * 单价
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder goodsPrice(final String goodsPrice) {
            this.goodsPrice = goodsPrice;
            return this;
        }

        /**
         * 税率
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder taxRate(final String taxRate) {
            this.taxRate = taxRate;
            return this;
        }

        /**
         * 商品明细列表（支持多商品行）
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder goodsItems(final List<InvoiceGoodsItemDTO> goodsItems) {
            this.goodsItems = goodsItems;
            return this;
        }

        /**
         * 收款人
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder payee(final String payee) {
            this.payee = payee;
            return this;
        }

        /**
         * 复核人
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder checker(final String checker) {
            this.checker = checker;
            return this;
        }

        /**
         * 开票人
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder issuer(final String issuer) {
            this.issuer = issuer;
            return this;
        }

        /**
         * 备注
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder remarks(final String remarks) {
            this.remarks = remarks;
            return this;
        }

        /**
         * 是否包含数字签名
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder hasSignature(final boolean hasSignature) {
            this.hasSignature = hasSignature;
            return this;
        }

        /**
         * 签名验证结果
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder signatureStatus(final Integer signatureStatus) {
            this.signatureStatus = signatureStatus;
            return this;
        }

        /**
         * 签名验证描述
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder signatureStatusDesc(final String signatureStatusDesc) {
            this.signatureStatusDesc = signatureStatusDesc;
            return this;
        }

        /**
         * 解析时间
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder parseTime(final LocalDateTime parseTime) {
            this.parseTime = parseTime;
            return this;
        }

        /**
         * OCR原始识别文本
         * @return {@code this}.
         */
        public FileParseResultDTO.FileParseResultDTOBuilder rawText(final String rawText) {
            this.rawText = rawText;
            return this;
        }

        public FileParseResultDTO build() {
            return new FileParseResultDTO(this.success, this.errorMessage, this.fileType, this.voucherType, this.voucherTypeDesc, this.xmlContent, this.jsonData, this.invoiceNo, this.invoiceCode, this.invoiceType, this.issueDate, this.buyerName, this.buyerTaxNo, this.sellerName, this.sellerTaxNo, this.totalAmount, this.amountWithoutTax, this.taxAmount, this.machineNo, this.buyerAddressPhone, this.buyerBankAccount, this.sellerAddressPhone, this.sellerBankAccount, this.checkCode, this.goodsName, this.goodsSpec, this.goodsUnit, this.goodsQuantity, this.goodsPrice, this.taxRate, this.goodsItems, this.payee, this.checker, this.issuer, this.remarks, this.hasSignature, this.signatureStatus, this.signatureStatusDesc, this.parseTime, this.rawText);
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "FileParseResultDTO.FileParseResultDTOBuilder(success=" + this.success + ", errorMessage=" + this.errorMessage + ", fileType=" + this.fileType + ", voucherType=" + this.voucherType + ", voucherTypeDesc=" + this.voucherTypeDesc + ", xmlContent=" + this.xmlContent + ", jsonData=" + this.jsonData + ", invoiceNo=" + this.invoiceNo + ", invoiceCode=" + this.invoiceCode + ", invoiceType=" + this.invoiceType + ", issueDate=" + this.issueDate + ", buyerName=" + this.buyerName + ", buyerTaxNo=" + this.buyerTaxNo + ", sellerName=" + this.sellerName + ", sellerTaxNo=" + this.sellerTaxNo + ", totalAmount=" + this.totalAmount + ", amountWithoutTax=" + this.amountWithoutTax + ", taxAmount=" + this.taxAmount + ", machineNo=" + this.machineNo + ", buyerAddressPhone=" + this.buyerAddressPhone + ", buyerBankAccount=" + this.buyerBankAccount + ", sellerAddressPhone=" + this.sellerAddressPhone + ", sellerBankAccount=" + this.sellerBankAccount + ", checkCode=" + this.checkCode + ", goodsName=" + this.goodsName + ", goodsSpec=" + this.goodsSpec + ", goodsUnit=" + this.goodsUnit + ", goodsQuantity=" + this.goodsQuantity + ", goodsPrice=" + this.goodsPrice + ", taxRate=" + this.taxRate + ", goodsItems=" + this.goodsItems + ", payee=" + this.payee + ", checker=" + this.checker + ", issuer=" + this.issuer + ", remarks=" + this.remarks + ", hasSignature=" + this.hasSignature + ", signatureStatus=" + this.signatureStatus + ", signatureStatusDesc=" + this.signatureStatusDesc + ", parseTime=" + this.parseTime + ", rawText=" + this.rawText + ")";
        }
    }

    public static FileParseResultDTO.FileParseResultDTOBuilder builder() {
        return new FileParseResultDTO.FileParseResultDTOBuilder();
    }

    /**
     * 解析是否成功
     */
    public boolean isSuccess() {
        return this.success;
    }

    /**
     * 错误信息
     */
    public String getErrorMessage() {
        return this.errorMessage;
    }

    /**
     * 文件类型：pdf, ofd, xml
     */
    public String getFileType() {
        return this.fileType;
    }

    /**
     * 凭证类型：invoice, train_ticket, flight_ticket, bank_receipt, other
     */
    public String getVoucherType() {
        return this.voucherType;
    }

    /**
     * 凭证类型描述
     */
    public String getVoucherTypeDesc() {
        return this.voucherTypeDesc;
    }

    /**
     * 提取的原始XML内容
     */
    public String getXmlContent() {
        return this.xmlContent;
    }

    /**
     * 发票号码（如果是发票类型）
     */
    public String getInvoiceNo() {
        return this.invoiceNo;
    }

    /**
     * 发票代码（如果是发票类型）
     */
    public String getInvoiceCode() {
        return this.invoiceCode;
    }

    /**
     * 发票类型代码
     */
    public String getInvoiceType() {
        return this.invoiceType;
    }

    /**
     * 开票日期
     */
    public String getIssueDate() {
        return this.issueDate;
    }

    /**
     * 购买方名称
     */
    public String getBuyerName() {
        return this.buyerName;
    }

    /**
     * 购买方税号
     */
    public String getBuyerTaxNo() {
        return this.buyerTaxNo;
    }

    /**
     * 销售方名称
     */
    public String getSellerName() {
        return this.sellerName;
    }

    /**
     * 销售方税号
     */
    public String getSellerTaxNo() {
        return this.sellerTaxNo;
    }

    /**
     * 价税合计金额
     */
    public String getTotalAmount() {
        return this.totalAmount;
    }

    /**
     * 金额（不含税）
     */
    public String getAmountWithoutTax() {
        return this.amountWithoutTax;
    }

    /**
     * 税额
     */
    public String getTaxAmount() {
        return this.taxAmount;
    }

    /**
     * 机器编号
     */
    public String getMachineNo() {
        return this.machineNo;
    }

    /**
     * 购买方地址电话
     */
    public String getBuyerAddressPhone() {
        return this.buyerAddressPhone;
    }

    /**
     * 购买方开户行账号
     */
    public String getBuyerBankAccount() {
        return this.buyerBankAccount;
    }

    /**
     * 销售方地址电话
     */
    public String getSellerAddressPhone() {
        return this.sellerAddressPhone;
    }

    /**
     * 销售方开户行账号
     */
    public String getSellerBankAccount() {
        return this.sellerBankAccount;
    }

    /**
     * 校验码（发票校验码后6位）
     */
    public String getCheckCode() {
        return this.checkCode;
    }

    /**
     * 货物或应税劳务、服务名称
     */
    public String getGoodsName() {
        return this.goodsName;
    }

    /**
     * 规格型号
     */
    public String getGoodsSpec() {
        return this.goodsSpec;
    }

    /**
     * 单位
     */
    public String getGoodsUnit() {
        return this.goodsUnit;
    }

    /**
     * 数量
     */
    public String getGoodsQuantity() {
        return this.goodsQuantity;
    }

    /**
     * 单价
     */
    public String getGoodsPrice() {
        return this.goodsPrice;
    }

    /**
     * 税率
     */
    public String getTaxRate() {
        return this.taxRate;
    }

    /**
     * 商品明细列表（支持多商品行）
     */
    public List<InvoiceGoodsItemDTO> getGoodsItems() {
        return this.goodsItems;
    }

    /**
     * 收款人
     */
    public String getPayee() {
        return this.payee;
    }

    /**
     * 复核人
     */
    public String getChecker() {
        return this.checker;
    }

    /**
     * 开票人
     */
    public String getIssuer() {
        return this.issuer;
    }

    /**
     * 备注
     */
    public String getRemarks() {
        return this.remarks;
    }

    /**
     * 是否包含数字签名
     */
    public boolean isHasSignature() {
        return this.hasSignature;
    }

    /**
     * 签名验证结果
     */
    public Integer getSignatureStatus() {
        return this.signatureStatus;
    }

    /**
     * 签名验证描述
     */
    public String getSignatureStatusDesc() {
        return this.signatureStatusDesc;
    }

    /**
     * 解析时间
     */
    public LocalDateTime getParseTime() {
        return this.parseTime;
    }

    /**
     * OCR原始识别文本
     */
    public String getRawText() {
        return this.rawText;
    }

    /**
     * 解析是否成功
     */
    public void setSuccess(final boolean success) {
        this.success = success;
    }

    /**
     * 错误信息
     */
    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * 文件类型：pdf, ofd, xml
     */
    public void setFileType(final String fileType) {
        this.fileType = fileType;
    }

    /**
     * 凭证类型：invoice, train_ticket, flight_ticket, bank_receipt, other
     */
    public void setVoucherType(final String voucherType) {
        this.voucherType = voucherType;
    }

    /**
     * 凭证类型描述
     */
    public void setVoucherTypeDesc(final String voucherTypeDesc) {
        this.voucherTypeDesc = voucherTypeDesc;
    }

    /**
     * 提取的原始XML内容
     */
    public void setXmlContent(final String xmlContent) {
        this.xmlContent = xmlContent;
    }

    /**
     * 转换后的JSON数据
     */
    public void setJsonData(final String jsonData) {
        this.jsonData = jsonData;
    }

    /**
     * 发票号码（如果是发票类型）
     */
    public void setInvoiceNo(final String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    /**
     * 发票代码（如果是发票类型）
     */
    public void setInvoiceCode(final String invoiceCode) {
        this.invoiceCode = invoiceCode;
    }

    /**
     * 发票类型代码
     */
    public void setInvoiceType(final String invoiceType) {
        this.invoiceType = invoiceType;
    }

    /**
     * 开票日期
     */
    public void setIssueDate(final String issueDate) {
        this.issueDate = issueDate;
    }

    /**
     * 购买方名称
     */
    public void setBuyerName(final String buyerName) {
        this.buyerName = buyerName;
    }

    /**
     * 购买方税号
     */
    public void setBuyerTaxNo(final String buyerTaxNo) {
        this.buyerTaxNo = buyerTaxNo;
    }

    /**
     * 销售方名称
     */
    public void setSellerName(final String sellerName) {
        this.sellerName = sellerName;
    }

    /**
     * 销售方税号
     */
    public void setSellerTaxNo(final String sellerTaxNo) {
        this.sellerTaxNo = sellerTaxNo;
    }

    /**
     * 价税合计金额
     */
    public void setTotalAmount(final String totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * 金额（不含税）
     */
    public void setAmountWithoutTax(final String amountWithoutTax) {
        this.amountWithoutTax = amountWithoutTax;
    }

    /**
     * 税额
     */
    public void setTaxAmount(final String taxAmount) {
        this.taxAmount = taxAmount;
    }

    /**
     * 机器编号
     */
    public void setMachineNo(final String machineNo) {
        this.machineNo = machineNo;
    }

    /**
     * 购买方地址电话
     */
    public void setBuyerAddressPhone(final String buyerAddressPhone) {
        this.buyerAddressPhone = buyerAddressPhone;
    }

    /**
     * 购买方开户行账号
     */
    public void setBuyerBankAccount(final String buyerBankAccount) {
        this.buyerBankAccount = buyerBankAccount;
    }

    /**
     * 销售方地址电话
     */
    public void setSellerAddressPhone(final String sellerAddressPhone) {
        this.sellerAddressPhone = sellerAddressPhone;
    }

    /**
     * 销售方开户行账号
     */
    public void setSellerBankAccount(final String sellerBankAccount) {
        this.sellerBankAccount = sellerBankAccount;
    }

    /**
     * 校验码（发票校验码后6位）
     */
    public void setCheckCode(final String checkCode) {
        this.checkCode = checkCode;
    }

    /**
     * 货物或应税劳务、服务名称
     */
    public void setGoodsName(final String goodsName) {
        this.goodsName = goodsName;
    }

    /**
     * 规格型号
     */
    public void setGoodsSpec(final String goodsSpec) {
        this.goodsSpec = goodsSpec;
    }

    /**
     * 单位
     */
    public void setGoodsUnit(final String goodsUnit) {
        this.goodsUnit = goodsUnit;
    }

    /**
     * 数量
     */
    public void setGoodsQuantity(final String goodsQuantity) {
        this.goodsQuantity = goodsQuantity;
    }

    /**
     * 单价
     */
    public void setGoodsPrice(final String goodsPrice) {
        this.goodsPrice = goodsPrice;
    }

    /**
     * 税率
     */
    public void setTaxRate(final String taxRate) {
        this.taxRate = taxRate;
    }

    /**
     * 商品明细列表（支持多商品行）
     */
    public void setGoodsItems(final List<InvoiceGoodsItemDTO> goodsItems) {
        this.goodsItems = goodsItems;
    }

    /**
     * 收款人
     */
    public void setPayee(final String payee) {
        this.payee = payee;
    }

    /**
     * 复核人
     */
    public void setChecker(final String checker) {
        this.checker = checker;
    }

    /**
     * 开票人
     */
    public void setIssuer(final String issuer) {
        this.issuer = issuer;
    }

    /**
     * 备注
     */
    public void setRemarks(final String remarks) {
        this.remarks = remarks;
    }

    /**
     * 是否包含数字签名
     */
    public void setHasSignature(final boolean hasSignature) {
        this.hasSignature = hasSignature;
    }

    /**
     * 签名验证结果
     */
    public void setSignatureStatus(final Integer signatureStatus) {
        this.signatureStatus = signatureStatus;
    }

    /**
     * 签名验证描述
     */
    public void setSignatureStatusDesc(final String signatureStatusDesc) {
        this.signatureStatusDesc = signatureStatusDesc;
    }

    /**
     * 解析时间
     */
    public void setParseTime(final LocalDateTime parseTime) {
        this.parseTime = parseTime;
    }

    /**
     * OCR原始识别文本
     */
    public void setRawText(final String rawText) {
        this.rawText = rawText;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof FileParseResultDTO)) return false;
        final FileParseResultDTO other = (FileParseResultDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.isSuccess() != other.isSuccess()) return false;
        if (this.isHasSignature() != other.isHasSignature()) return false;
        final java.lang.Object this$signatureStatus = this.getSignatureStatus();
        final java.lang.Object other$signatureStatus = other.getSignatureStatus();
        if (this$signatureStatus == null ? other$signatureStatus != null : !this$signatureStatus.equals(other$signatureStatus)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$fileType = this.getFileType();
        final java.lang.Object other$fileType = other.getFileType();
        if (this$fileType == null ? other$fileType != null : !this$fileType.equals(other$fileType)) return false;
        final java.lang.Object this$voucherType = this.getVoucherType();
        final java.lang.Object other$voucherType = other.getVoucherType();
        if (this$voucherType == null ? other$voucherType != null : !this$voucherType.equals(other$voucherType)) return false;
        final java.lang.Object this$voucherTypeDesc = this.getVoucherTypeDesc();
        final java.lang.Object other$voucherTypeDesc = other.getVoucherTypeDesc();
        if (this$voucherTypeDesc == null ? other$voucherTypeDesc != null : !this$voucherTypeDesc.equals(other$voucherTypeDesc)) return false;
        final java.lang.Object this$xmlContent = this.getXmlContent();
        final java.lang.Object other$xmlContent = other.getXmlContent();
        if (this$xmlContent == null ? other$xmlContent != null : !this$xmlContent.equals(other$xmlContent)) return false;
        final java.lang.Object this$jsonData = this.getJsonData();
        final java.lang.Object other$jsonData = other.getJsonData();
        if (this$jsonData == null ? other$jsonData != null : !this$jsonData.equals(other$jsonData)) return false;
        final java.lang.Object this$invoiceNo = this.getInvoiceNo();
        final java.lang.Object other$invoiceNo = other.getInvoiceNo();
        if (this$invoiceNo == null ? other$invoiceNo != null : !this$invoiceNo.equals(other$invoiceNo)) return false;
        final java.lang.Object this$invoiceCode = this.getInvoiceCode();
        final java.lang.Object other$invoiceCode = other.getInvoiceCode();
        if (this$invoiceCode == null ? other$invoiceCode != null : !this$invoiceCode.equals(other$invoiceCode)) return false;
        final java.lang.Object this$invoiceType = this.getInvoiceType();
        final java.lang.Object other$invoiceType = other.getInvoiceType();
        if (this$invoiceType == null ? other$invoiceType != null : !this$invoiceType.equals(other$invoiceType)) return false;
        final java.lang.Object this$issueDate = this.getIssueDate();
        final java.lang.Object other$issueDate = other.getIssueDate();
        if (this$issueDate == null ? other$issueDate != null : !this$issueDate.equals(other$issueDate)) return false;
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
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$amountWithoutTax = this.getAmountWithoutTax();
        final java.lang.Object other$amountWithoutTax = other.getAmountWithoutTax();
        if (this$amountWithoutTax == null ? other$amountWithoutTax != null : !this$amountWithoutTax.equals(other$amountWithoutTax)) return false;
        final java.lang.Object this$taxAmount = this.getTaxAmount();
        final java.lang.Object other$taxAmount = other.getTaxAmount();
        if (this$taxAmount == null ? other$taxAmount != null : !this$taxAmount.equals(other$taxAmount)) return false;
        final java.lang.Object this$machineNo = this.getMachineNo();
        final java.lang.Object other$machineNo = other.getMachineNo();
        if (this$machineNo == null ? other$machineNo != null : !this$machineNo.equals(other$machineNo)) return false;
        final java.lang.Object this$buyerAddressPhone = this.getBuyerAddressPhone();
        final java.lang.Object other$buyerAddressPhone = other.getBuyerAddressPhone();
        if (this$buyerAddressPhone == null ? other$buyerAddressPhone != null : !this$buyerAddressPhone.equals(other$buyerAddressPhone)) return false;
        final java.lang.Object this$buyerBankAccount = this.getBuyerBankAccount();
        final java.lang.Object other$buyerBankAccount = other.getBuyerBankAccount();
        if (this$buyerBankAccount == null ? other$buyerBankAccount != null : !this$buyerBankAccount.equals(other$buyerBankAccount)) return false;
        final java.lang.Object this$sellerAddressPhone = this.getSellerAddressPhone();
        final java.lang.Object other$sellerAddressPhone = other.getSellerAddressPhone();
        if (this$sellerAddressPhone == null ? other$sellerAddressPhone != null : !this$sellerAddressPhone.equals(other$sellerAddressPhone)) return false;
        final java.lang.Object this$sellerBankAccount = this.getSellerBankAccount();
        final java.lang.Object other$sellerBankAccount = other.getSellerBankAccount();
        if (this$sellerBankAccount == null ? other$sellerBankAccount != null : !this$sellerBankAccount.equals(other$sellerBankAccount)) return false;
        final java.lang.Object this$checkCode = this.getCheckCode();
        final java.lang.Object other$checkCode = other.getCheckCode();
        if (this$checkCode == null ? other$checkCode != null : !this$checkCode.equals(other$checkCode)) return false;
        final java.lang.Object this$goodsName = this.getGoodsName();
        final java.lang.Object other$goodsName = other.getGoodsName();
        if (this$goodsName == null ? other$goodsName != null : !this$goodsName.equals(other$goodsName)) return false;
        final java.lang.Object this$goodsSpec = this.getGoodsSpec();
        final java.lang.Object other$goodsSpec = other.getGoodsSpec();
        if (this$goodsSpec == null ? other$goodsSpec != null : !this$goodsSpec.equals(other$goodsSpec)) return false;
        final java.lang.Object this$goodsUnit = this.getGoodsUnit();
        final java.lang.Object other$goodsUnit = other.getGoodsUnit();
        if (this$goodsUnit == null ? other$goodsUnit != null : !this$goodsUnit.equals(other$goodsUnit)) return false;
        final java.lang.Object this$goodsQuantity = this.getGoodsQuantity();
        final java.lang.Object other$goodsQuantity = other.getGoodsQuantity();
        if (this$goodsQuantity == null ? other$goodsQuantity != null : !this$goodsQuantity.equals(other$goodsQuantity)) return false;
        final java.lang.Object this$goodsPrice = this.getGoodsPrice();
        final java.lang.Object other$goodsPrice = other.getGoodsPrice();
        if (this$goodsPrice == null ? other$goodsPrice != null : !this$goodsPrice.equals(other$goodsPrice)) return false;
        final java.lang.Object this$taxRate = this.getTaxRate();
        final java.lang.Object other$taxRate = other.getTaxRate();
        if (this$taxRate == null ? other$taxRate != null : !this$taxRate.equals(other$taxRate)) return false;
        final java.lang.Object this$goodsItems = this.getGoodsItems();
        final java.lang.Object other$goodsItems = other.getGoodsItems();
        if (this$goodsItems == null ? other$goodsItems != null : !this$goodsItems.equals(other$goodsItems)) return false;
        final java.lang.Object this$payee = this.getPayee();
        final java.lang.Object other$payee = other.getPayee();
        if (this$payee == null ? other$payee != null : !this$payee.equals(other$payee)) return false;
        final java.lang.Object this$checker = this.getChecker();
        final java.lang.Object other$checker = other.getChecker();
        if (this$checker == null ? other$checker != null : !this$checker.equals(other$checker)) return false;
        final java.lang.Object this$issuer = this.getIssuer();
        final java.lang.Object other$issuer = other.getIssuer();
        if (this$issuer == null ? other$issuer != null : !this$issuer.equals(other$issuer)) return false;
        final java.lang.Object this$remarks = this.getRemarks();
        final java.lang.Object other$remarks = other.getRemarks();
        if (this$remarks == null ? other$remarks != null : !this$remarks.equals(other$remarks)) return false;
        final java.lang.Object this$signatureStatusDesc = this.getSignatureStatusDesc();
        final java.lang.Object other$signatureStatusDesc = other.getSignatureStatusDesc();
        if (this$signatureStatusDesc == null ? other$signatureStatusDesc != null : !this$signatureStatusDesc.equals(other$signatureStatusDesc)) return false;
        final java.lang.Object this$parseTime = this.getParseTime();
        final java.lang.Object other$parseTime = other.getParseTime();
        if (this$parseTime == null ? other$parseTime != null : !this$parseTime.equals(other$parseTime)) return false;
        final java.lang.Object this$rawText = this.getRawText();
        final java.lang.Object other$rawText = other.getRawText();
        if (this$rawText == null ? other$rawText != null : !this$rawText.equals(other$rawText)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FileParseResultDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isSuccess() ? 79 : 97);
        result = result * PRIME + (this.isHasSignature() ? 79 : 97);
        final java.lang.Object $signatureStatus = this.getSignatureStatus();
        result = result * PRIME + ($signatureStatus == null ? 43 : $signatureStatus.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $fileType = this.getFileType();
        result = result * PRIME + ($fileType == null ? 43 : $fileType.hashCode());
        final java.lang.Object $voucherType = this.getVoucherType();
        result = result * PRIME + ($voucherType == null ? 43 : $voucherType.hashCode());
        final java.lang.Object $voucherTypeDesc = this.getVoucherTypeDesc();
        result = result * PRIME + ($voucherTypeDesc == null ? 43 : $voucherTypeDesc.hashCode());
        final java.lang.Object $xmlContent = this.getXmlContent();
        result = result * PRIME + ($xmlContent == null ? 43 : $xmlContent.hashCode());
        final java.lang.Object $jsonData = this.getJsonData();
        result = result * PRIME + ($jsonData == null ? 43 : $jsonData.hashCode());
        final java.lang.Object $invoiceNo = this.getInvoiceNo();
        result = result * PRIME + ($invoiceNo == null ? 43 : $invoiceNo.hashCode());
        final java.lang.Object $invoiceCode = this.getInvoiceCode();
        result = result * PRIME + ($invoiceCode == null ? 43 : $invoiceCode.hashCode());
        final java.lang.Object $invoiceType = this.getInvoiceType();
        result = result * PRIME + ($invoiceType == null ? 43 : $invoiceType.hashCode());
        final java.lang.Object $issueDate = this.getIssueDate();
        result = result * PRIME + ($issueDate == null ? 43 : $issueDate.hashCode());
        final java.lang.Object $buyerName = this.getBuyerName();
        result = result * PRIME + ($buyerName == null ? 43 : $buyerName.hashCode());
        final java.lang.Object $buyerTaxNo = this.getBuyerTaxNo();
        result = result * PRIME + ($buyerTaxNo == null ? 43 : $buyerTaxNo.hashCode());
        final java.lang.Object $sellerName = this.getSellerName();
        result = result * PRIME + ($sellerName == null ? 43 : $sellerName.hashCode());
        final java.lang.Object $sellerTaxNo = this.getSellerTaxNo();
        result = result * PRIME + ($sellerTaxNo == null ? 43 : $sellerTaxNo.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $amountWithoutTax = this.getAmountWithoutTax();
        result = result * PRIME + ($amountWithoutTax == null ? 43 : $amountWithoutTax.hashCode());
        final java.lang.Object $taxAmount = this.getTaxAmount();
        result = result * PRIME + ($taxAmount == null ? 43 : $taxAmount.hashCode());
        final java.lang.Object $machineNo = this.getMachineNo();
        result = result * PRIME + ($machineNo == null ? 43 : $machineNo.hashCode());
        final java.lang.Object $buyerAddressPhone = this.getBuyerAddressPhone();
        result = result * PRIME + ($buyerAddressPhone == null ? 43 : $buyerAddressPhone.hashCode());
        final java.lang.Object $buyerBankAccount = this.getBuyerBankAccount();
        result = result * PRIME + ($buyerBankAccount == null ? 43 : $buyerBankAccount.hashCode());
        final java.lang.Object $sellerAddressPhone = this.getSellerAddressPhone();
        result = result * PRIME + ($sellerAddressPhone == null ? 43 : $sellerAddressPhone.hashCode());
        final java.lang.Object $sellerBankAccount = this.getSellerBankAccount();
        result = result * PRIME + ($sellerBankAccount == null ? 43 : $sellerBankAccount.hashCode());
        final java.lang.Object $checkCode = this.getCheckCode();
        result = result * PRIME + ($checkCode == null ? 43 : $checkCode.hashCode());
        final java.lang.Object $goodsName = this.getGoodsName();
        result = result * PRIME + ($goodsName == null ? 43 : $goodsName.hashCode());
        final java.lang.Object $goodsSpec = this.getGoodsSpec();
        result = result * PRIME + ($goodsSpec == null ? 43 : $goodsSpec.hashCode());
        final java.lang.Object $goodsUnit = this.getGoodsUnit();
        result = result * PRIME + ($goodsUnit == null ? 43 : $goodsUnit.hashCode());
        final java.lang.Object $goodsQuantity = this.getGoodsQuantity();
        result = result * PRIME + ($goodsQuantity == null ? 43 : $goodsQuantity.hashCode());
        final java.lang.Object $goodsPrice = this.getGoodsPrice();
        result = result * PRIME + ($goodsPrice == null ? 43 : $goodsPrice.hashCode());
        final java.lang.Object $taxRate = this.getTaxRate();
        result = result * PRIME + ($taxRate == null ? 43 : $taxRate.hashCode());
        final java.lang.Object $goodsItems = this.getGoodsItems();
        result = result * PRIME + ($goodsItems == null ? 43 : $goodsItems.hashCode());
        final java.lang.Object $payee = this.getPayee();
        result = result * PRIME + ($payee == null ? 43 : $payee.hashCode());
        final java.lang.Object $checker = this.getChecker();
        result = result * PRIME + ($checker == null ? 43 : $checker.hashCode());
        final java.lang.Object $issuer = this.getIssuer();
        result = result * PRIME + ($issuer == null ? 43 : $issuer.hashCode());
        final java.lang.Object $remarks = this.getRemarks();
        result = result * PRIME + ($remarks == null ? 43 : $remarks.hashCode());
        final java.lang.Object $signatureStatusDesc = this.getSignatureStatusDesc();
        result = result * PRIME + ($signatureStatusDesc == null ? 43 : $signatureStatusDesc.hashCode());
        final java.lang.Object $parseTime = this.getParseTime();
        result = result * PRIME + ($parseTime == null ? 43 : $parseTime.hashCode());
        final java.lang.Object $rawText = this.getRawText();
        result = result * PRIME + ($rawText == null ? 43 : $rawText.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "FileParseResultDTO(success=" + this.isSuccess() + ", errorMessage=" + this.getErrorMessage() + ", fileType=" + this.getFileType() + ", voucherType=" + this.getVoucherType() + ", voucherTypeDesc=" + this.getVoucherTypeDesc() + ", xmlContent=" + this.getXmlContent() + ", jsonData=" + this.getJsonData() + ", invoiceNo=" + this.getInvoiceNo() + ", invoiceCode=" + this.getInvoiceCode() + ", invoiceType=" + this.getInvoiceType() + ", issueDate=" + this.getIssueDate() + ", buyerName=" + this.getBuyerName() + ", buyerTaxNo=" + this.getBuyerTaxNo() + ", sellerName=" + this.getSellerName() + ", sellerTaxNo=" + this.getSellerTaxNo() + ", totalAmount=" + this.getTotalAmount() + ", amountWithoutTax=" + this.getAmountWithoutTax() + ", taxAmount=" + this.getTaxAmount() + ", machineNo=" + this.getMachineNo() + ", buyerAddressPhone=" + this.getBuyerAddressPhone() + ", buyerBankAccount=" + this.getBuyerBankAccount() + ", sellerAddressPhone=" + this.getSellerAddressPhone() + ", sellerBankAccount=" + this.getSellerBankAccount() + ", checkCode=" + this.getCheckCode() + ", goodsName=" + this.getGoodsName() + ", goodsSpec=" + this.getGoodsSpec() + ", goodsUnit=" + this.getGoodsUnit() + ", goodsQuantity=" + this.getGoodsQuantity() + ", goodsPrice=" + this.getGoodsPrice() + ", taxRate=" + this.getTaxRate() + ", goodsItems=" + this.getGoodsItems() + ", payee=" + this.getPayee() + ", checker=" + this.getChecker() + ", issuer=" + this.getIssuer() + ", remarks=" + this.getRemarks() + ", hasSignature=" + this.isHasSignature() + ", signatureStatus=" + this.getSignatureStatus() + ", signatureStatusDesc=" + this.getSignatureStatusDesc() + ", parseTime=" + this.getParseTime() + ", rawText=" + this.getRawText() + ")";
    }

    public FileParseResultDTO() {
    }

    /**
     * Creates a new {@code FileParseResultDTO} instance.
     *
     * @param success 解析是否成功
     * @param errorMessage 错误信息
     * @param fileType 文件类型：pdf, ofd, xml
     * @param voucherType 凭证类型：invoice, train_ticket, flight_ticket, bank_receipt, other
     * @param voucherTypeDesc 凭证类型描述
     * @param xmlContent 提取的原始XML内容
     * @param jsonData 转换后的JSON数据
     * @param invoiceNo 发票号码（如果是发票类型）
     * @param invoiceCode 发票代码（如果是发票类型）
     * @param invoiceType 发票类型代码
     * @param issueDate 开票日期
     * @param buyerName 购买方名称
     * @param buyerTaxNo 购买方税号
     * @param sellerName 销售方名称
     * @param sellerTaxNo 销售方税号
     * @param totalAmount 价税合计金额
     * @param amountWithoutTax 金额（不含税）
     * @param taxAmount 税额
     * @param machineNo 机器编号
     * @param buyerAddressPhone 购买方地址电话
     * @param buyerBankAccount 购买方开户行账号
     * @param sellerAddressPhone 销售方地址电话
     * @param sellerBankAccount 销售方开户行账号
     * @param checkCode 校验码（发票校验码后6位）
     * @param goodsName 货物或应税劳务、服务名称
     * @param goodsSpec 规格型号
     * @param goodsUnit 单位
     * @param goodsQuantity 数量
     * @param goodsPrice 单价
     * @param taxRate 税率
     * @param goodsItems 商品明细列表（支持多商品行）
     * @param payee 收款人
     * @param checker 复核人
     * @param issuer 开票人
     * @param remarks 备注
     * @param hasSignature 是否包含数字签名
     * @param signatureStatus 签名验证结果
     * @param signatureStatusDesc 签名验证描述
     * @param parseTime 解析时间
     * @param rawText OCR原始识别文本
     */
    public FileParseResultDTO(final boolean success, final String errorMessage, final String fileType, final String voucherType, final String voucherTypeDesc, final String xmlContent, final String jsonData, final String invoiceNo, final String invoiceCode, final String invoiceType, final String issueDate, final String buyerName, final String buyerTaxNo, final String sellerName, final String sellerTaxNo, final String totalAmount, final String amountWithoutTax, final String taxAmount, final String machineNo, final String buyerAddressPhone, final String buyerBankAccount, final String sellerAddressPhone, final String sellerBankAccount, final String checkCode, final String goodsName, final String goodsSpec, final String goodsUnit, final String goodsQuantity, final String goodsPrice, final String taxRate, final List<InvoiceGoodsItemDTO> goodsItems, final String payee, final String checker, final String issuer, final String remarks, final boolean hasSignature, final Integer signatureStatus, final String signatureStatusDesc, final LocalDateTime parseTime, final String rawText) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.fileType = fileType;
        this.voucherType = voucherType;
        this.voucherTypeDesc = voucherTypeDesc;
        this.xmlContent = xmlContent;
        this.jsonData = jsonData;
        this.invoiceNo = invoiceNo;
        this.invoiceCode = invoiceCode;
        this.invoiceType = invoiceType;
        this.issueDate = issueDate;
        this.buyerName = buyerName;
        this.buyerTaxNo = buyerTaxNo;
        this.sellerName = sellerName;
        this.sellerTaxNo = sellerTaxNo;
        this.totalAmount = totalAmount;
        this.amountWithoutTax = amountWithoutTax;
        this.taxAmount = taxAmount;
        this.machineNo = machineNo;
        this.buyerAddressPhone = buyerAddressPhone;
        this.buyerBankAccount = buyerBankAccount;
        this.sellerAddressPhone = sellerAddressPhone;
        this.sellerBankAccount = sellerBankAccount;
        this.checkCode = checkCode;
        this.goodsName = goodsName;
        this.goodsSpec = goodsSpec;
        this.goodsUnit = goodsUnit;
        this.goodsQuantity = goodsQuantity;
        this.goodsPrice = goodsPrice;
        this.taxRate = taxRate;
        this.goodsItems = goodsItems;
        this.payee = payee;
        this.checker = checker;
        this.issuer = issuer;
        this.remarks = remarks;
        this.hasSignature = hasSignature;
        this.signatureStatus = signatureStatus;
        this.signatureStatusDesc = signatureStatusDesc;
        this.parseTime = parseTime;
        this.rawText = rawText;
    }
}
