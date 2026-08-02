package com.foodtraceability.service.impl;

import com.foodtraceability.dto.FileParseResultDTO;
import com.foodtraceability.dto.InvoiceGoodsItemDTO;
import com.foodtraceability.dto.InvoiceOcrResultDTO;
import com.foodtraceability.dto.InvoiceQrInfoDTO;
import com.foodtraceability.service.FileParseService;
import com.foodtraceability.service.InvoiceOcrService;
import com.foodtraceability.service.InvoiceQrCodeService;
import com.foodtraceability.service.VoucherToolkitService;
import com.foodtraceability.utils.XmlParseUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 电子凭证文件解析服务实现
 * 
 * 依据：财政部《电子凭证会计数据标准应用指南(推广应用版1.0)》
 * 
 * 实现原理：
 * 1. PDF解析：使用PDFBox提取内嵌XML附件
 * 2. OFD解析：OFD本质是ZIP压缩包，解压后提取XML
 * 3. XML解析：直接解析XBRL格式数据
 * 4. 二维码+OCR：二维码提取基本信息，OCR补充完整信息
 * 
 * 重要说明：
 * - 原始XML内容保留用于验签（验签必须使用原始XML）
 * - JSON转换时进行预处理移除命名空间（仅用于数据提取）
 */
@Service
public class FileParseServiceImpl implements FileParseService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileParseServiceImpl.class);
    private final ObjectMapper objectMapper;
    private final VoucherToolkitService toolkitService;
    private final InvoiceQrCodeService qrCodeService;
    private final InvoiceOcrService ocrService;
    private static final String PDF_MAGIC = "%PDF";
    private static final String OFD_MAGIC = "PK";
    private static final String XML_MAGIC = "<?xml";

    @Override
    public FileParseResultDTO parseFile(MultipartFile file) {
        log.info("[文件解析] 开始解析文件: {}, 大小: {} bytes", XmlParseUtils.maskInvoiceNo(file.getOriginalFilename()), file.getSize());
        try {
            byte[] fileContent = file.getBytes();
            String fileName = file.getOriginalFilename();
            String fileType = detectFileType(fileContent, fileName);
            log.info("[文件解析] 检测到文件类型: {}", fileType);
            return switch (fileType.toLowerCase()) {
                case "pdf" -> parsePdf(fileContent);
                case "ofd" -> parseOfd(fileContent);
                case "xml" -> parseXml(fileContent);
                default -> FileParseResultDTO.failed("不支持的文件类型: " + fileType);
            };
        } catch (Exception e) {
            log.error("[文件解析] 解析失败: {}", e.getMessage());
            return FileParseResultDTO.failed("文件解析失败，请检查文件格式");
        }
    }

    @Override
    public FileParseResultDTO parsePdf(byte[] fileContent) {
        log.info("[PDF解析] 开始解析PDF文件, 大小: {} bytes", fileContent.length);
        try {
            String xmlContent = null;
            if (toolkitService.isToolkitAvailable()) {
                log.info("[PDF解析] 尝试使用官方工具包提取XML");
                xmlContent = toolkitService.extractXmlFromPdfByToolkit(fileContent);
            }
            if (xmlContent == null || xmlContent.isEmpty()) {
                log.info("[PDF解析] 使用内置PDFBox提取XML");
                xmlContent = extractXmlFromPdf(fileContent);
            }
            FileParseResultDTO xmlResult = null;
            if (xmlContent != null && !xmlContent.isEmpty()) {
                log.info("[PDF解析] 成功提取内嵌XML, 长度: {}", xmlContent.length());
                xmlResult = parseXml(xmlContent.getBytes(StandardCharsets.UTF_8));
                xmlResult.setFileType("pdf");
                // 检查XML解析结果是否包含关键信息
                boolean hasBuyerInfo = xmlResult.getBuyerName() != null || xmlResult.getBuyerTaxNo() != null;
                boolean hasSellerInfo = xmlResult.getSellerName() != null || xmlResult.getSellerTaxNo() != null;
                if (hasBuyerInfo && hasSellerInfo) {
                    log.info("[PDF解析] XML解析成功，已包含购买方和销售方信息");
                    return xmlResult;
                }
                log.info("[PDF解析] XML解析成功，但缺少购买方或销售方信息，尝试OCR补充");
            }
            // 执行二维码提取和OCR识别
            log.info("[PDF解析] 步骤1: 开始提取二维码...");
            InvoiceQrInfoDTO qrInfo = qrCodeService.extractQrCodeFromPdf(fileContent);
            log.info("[PDF解析] 二维码提取结果: success={}, invoiceCode={}, invoiceNo={}", qrInfo != null && qrInfo.isSuccess(), qrInfo != null ? qrInfo.getInvoiceCode() : null, qrInfo != null ? qrInfo.getInvoiceNo() : null);
            log.info("[PDF解析] 步骤2: 开始OCR识别...");
            InvoiceOcrResultDTO ocrResult = ocrService.recognizeFromPdf(fileContent);
            log.info("[PDF解析] OCR识别结果: success={}, buyerName={}, sellerName={}, buyerTaxNo={}, sellerTaxNo={}", ocrResult != null && ocrResult.isSuccess(), ocrResult != null ? ocrResult.getBuyerName() : null, ocrResult != null ? ocrResult.getSellerName() : null, ocrResult != null ? ocrResult.getBuyerTaxNo() : null, ocrResult != null ? ocrResult.getSellerTaxNo() : null);
            if (ocrResult != null && ocrResult.getRawText() != null) {
                log.info("[PDF解析] OCR原始文本(前500字符): {}", ocrResult.getRawText().length() > 500 ? ocrResult.getRawText().substring(0, 500) : ocrResult.getRawText());
            }
            // 合并结果：优先使用XML结果，用OCR补充缺失信息
            FileParseResultDTO result;
            if (xmlResult != null && xmlResult.isSuccess()) {
                // XML结果存在，用OCR补充缺失信息
                result = xmlResult;
                if (ocrResult != null && ocrResult.isSuccess()) {
                    log.info("[PDF解析] 使用OCR补充缺失信息");
                    // 补充购买方信息
                    if (result.getBuyerName() == null && ocrResult.getBuyerName() != null) {
                        result.setBuyerName(ocrResult.getBuyerName());
                    }
                    if (result.getBuyerTaxNo() == null && ocrResult.getBuyerTaxNo() != null) {
                        result.setBuyerTaxNo(ocrResult.getBuyerTaxNo());
                    }
                    // 补充销售方信息
                    if (result.getSellerName() == null && ocrResult.getSellerName() != null) {
                        result.setSellerName(ocrResult.getSellerName());
                    }
                    if (result.getSellerTaxNo() == null && ocrResult.getSellerTaxNo() != null) {
                        result.setSellerTaxNo(ocrResult.getSellerTaxNo());
                    }
                    // 补充其他可能缺失的信息
                    if (result.getInvoiceCode() == null && ocrResult.getInvoiceCode() != null) {
                        result.setInvoiceCode(ocrResult.getInvoiceCode());
                    }
                    if (result.getInvoiceNo() == null && ocrResult.getInvoiceNo() != null) {
                        result.setInvoiceNo(ocrResult.getInvoiceNo());
                    }
                    if (result.getIssueDate() == null && ocrResult.getIssueDate() != null) {
                        result.setIssueDate(ocrResult.getIssueDate());
                    }
                    if (result.getCheckCode() == null && ocrResult.getCheckCode() != null) {
                        result.setCheckCode(ocrResult.getCheckCode());
                    }
                    if (result.getTotalAmount() == null && ocrResult.getTotalAmount() != null) {
                        result.setTotalAmount(ocrResult.getTotalAmount().toString());
                    }
                }
                // 如果二维码有信息，也补充
                if (qrInfo != null && qrInfo.isSuccess()) {
                    if (result.getInvoiceCode() == null && qrInfo.getInvoiceCode() != null) {
                        result.setInvoiceCode(qrInfo.getInvoiceCode());
                    }
                    if (result.getInvoiceNo() == null && qrInfo.getInvoiceNo() != null) {
                        result.setInvoiceNo(qrInfo.getInvoiceNo());
                    }
                    if (result.getIssueDate() == null && qrInfo.getIssueDate() != null) {
                        result.setIssueDate(qrInfo.getIssueDate());
                    }
                    if (result.getCheckCode() == null && qrInfo.getCheckCode() != null) {
                        result.setCheckCode(qrInfo.getCheckCode());
                    }
                }
            } else {
                // 没有XML结果，合并二维码和OCR结果
                result = mergeQrAndOcrResults(qrInfo, ocrResult);
            }
            if (result != null) {
                result.setFileType("pdf");
                return result;
            }
            log.warn("[PDF解析] 未找到内嵌XML、二维码和OCR识别结果");
            return FileParseResultDTO.failed("PDF文件无法自动识别，请确认文件为有效发票");
        } catch (Exception e) {
            log.error("[PDF解析] 解析失败: {}", e.getMessage());
            return FileParseResultDTO.failed("PDF解析失败，请检查文件格式");
        }
    }

    /**
     * 合并二维码和OCR识别结果
     * 二维码提供准确的发票代码、号码、金额等结构化数据
     * OCR补充购买方、销售方名称和税号等详细信息
     */
    private FileParseResultDTO mergeQrAndOcrResults(InvoiceQrInfoDTO qrInfo, InvoiceOcrResultDTO ocrResult) {
        boolean qrSuccess = qrInfo != null && qrInfo.isSuccess();
        boolean ocrSuccess = ocrResult != null && ocrResult.isSuccess();
        if (!qrSuccess && !ocrSuccess) {
            return null;
        }
        FileParseResultDTO result = FileParseResultDTO.success("pdf", "invoice", null);
        result.setHasSignature(false);
        if (qrSuccess) {
            log.info("[PDF解析] 二维码提取成功: 发票代码={}, 发票号码={}", qrInfo.getInvoiceCode(), qrInfo.getInvoiceNo());
            result.setInvoiceCode(qrInfo.getInvoiceCode());
            result.setInvoiceNo(qrInfo.getInvoiceNo());
            result.setInvoiceType(qrInfo.getInvoiceType());
            result.setIssueDate(qrInfo.getIssueDate());
            result.setAmountWithoutTax(qrInfo.getAmountWithoutTax() != null ? qrInfo.getAmountWithoutTax().toString() : null);
            result.setTaxAmount(qrInfo.getTaxAmount() != null ? qrInfo.getTaxAmount().toString() : null);
            result.setCheckCode(qrInfo.getCheckCode());
            result.setBuyerName(qrInfo.getBuyerName());
            result.setSellerName(qrInfo.getSellerName());
            if (qrInfo.getInvoiceTypeName() != null) {
                result.setVoucherTypeDesc(qrInfo.getInvoiceTypeName());
            }
        }
        if (ocrSuccess) {
            log.info("[PDF解析] OCR识别成功，补充详细信息");
            if (result.getInvoiceCode() == null && ocrResult.getInvoiceCode() != null) {
                result.setInvoiceCode(ocrResult.getInvoiceCode());
            }
            if (result.getInvoiceNo() == null && ocrResult.getInvoiceNo() != null) {
                result.setInvoiceNo(ocrResult.getInvoiceNo());
            }
            if (result.getInvoiceType() == null && ocrResult.getInvoiceType() != null) {
                result.setInvoiceType(ocrResult.getInvoiceType());
            }
            if (result.getIssueDate() == null && ocrResult.getIssueDate() != null) {
                result.setIssueDate(ocrResult.getIssueDate());
            }
            if (result.getCheckCode() == null && ocrResult.getCheckCode() != null) {
                result.setCheckCode(ocrResult.getCheckCode());
            }
            if (ocrResult.getBuyerName() != null) {
                result.setBuyerName(ocrResult.getBuyerName());
            }
            if (ocrResult.getBuyerTaxNo() != null) {
                result.setBuyerTaxNo(ocrResult.getBuyerTaxNo());
            }
            if (ocrResult.getSellerName() != null) {
                result.setSellerName(ocrResult.getSellerName());
            }
            if (ocrResult.getSellerTaxNo() != null) {
                result.setSellerTaxNo(ocrResult.getSellerTaxNo());
            }
            if (result.getAmountWithoutTax() == null && ocrResult.getAmountWithoutTax() != null) {
                result.setAmountWithoutTax(ocrResult.getAmountWithoutTax().toString());
            }
            if (result.getTaxAmount() == null && ocrResult.getTaxAmount() != null) {
                result.setTaxAmount(ocrResult.getTaxAmount().toString());
            }
            if (ocrResult.getTotalAmount() != null) {
                result.setTotalAmount(ocrResult.getTotalAmount().toString());
            }
            if (ocrResult.getMachineNo() != null) {
                result.setMachineNo(ocrResult.getMachineNo());
            }
            if (ocrResult.getSellerAddressPhone() != null) {
                result.setSellerAddressPhone(ocrResult.getSellerAddressPhone());
            }
            if (ocrResult.getSellerBankAccount() != null) {
                result.setSellerBankAccount(ocrResult.getSellerBankAccount());
            }
            if (ocrResult.getBuyerAddressPhone() != null) {
                result.setBuyerAddressPhone(ocrResult.getBuyerAddressPhone());
            }
            if (ocrResult.getBuyerBankAccount() != null) {
                result.setBuyerBankAccount(ocrResult.getBuyerBankAccount());
            }
            if (ocrResult.getGoodsName() != null) {
                result.setGoodsName(ocrResult.getGoodsName());
            }
            if (ocrResult.getGoodsSpec() != null) {
                result.setGoodsSpec(ocrResult.getGoodsSpec());
            }
            if (ocrResult.getGoodsUnit() != null) {
                result.setGoodsUnit(ocrResult.getGoodsUnit());
            }
            if (ocrResult.getGoodsQuantity() != null) {
                result.setGoodsQuantity(ocrResult.getGoodsQuantity());
            }
            if (ocrResult.getGoodsPrice() != null) {
                result.setGoodsPrice(ocrResult.getGoodsPrice());
            }
            if (ocrResult.getTaxRate() != null) {
                result.setTaxRate(ocrResult.getTaxRate().toString());
            }
            if (ocrResult.getRemarks() != null) {
                result.setRemarks(ocrResult.getRemarks());
            }
            if (result.getVoucherTypeDesc() == null && ocrResult.getInvoiceTypeName() != null) {
                result.setVoucherTypeDesc(ocrResult.getInvoiceTypeName());
            }
            if (ocrResult.getPayee() != null) {
                result.setPayee(ocrResult.getPayee());
            }
            if (ocrResult.getChecker() != null) {
                result.setChecker(ocrResult.getChecker());
            }
            if (ocrResult.getIssuer() != null) {
                result.setIssuer(ocrResult.getIssuer());
            }
            if (ocrResult.getGoodsItems() != null && !ocrResult.getGoodsItems().isEmpty()) {
                result.setGoodsItems(ocrResult.getGoodsItems());
            }
            if (ocrResult.getRawText() != null) {
                result.setRawText(ocrResult.getRawText());
                log.info("[PDF解析] OCR原始文本长度: {}", ocrResult.getRawText().length());
            }
        }
        if (result.getInvoiceCode() == null && result.getInvoiceNo() == null) {
            log.warn("[PDF解析] 无法获取发票基本信息");
            return null;
        }
        return result;
    }

    @Override
    public FileParseResultDTO parseOfd(byte[] fileContent) {
        log.info("[OFD解析] 开始解析OFD文件, 大小: {} bytes", fileContent.length);
        try {
            String xmlContent = extractXmlFromOfd(fileContent);
            if (xmlContent != null && !xmlContent.isEmpty()) {
                log.info("[OFD解析] 成功提取XML, 长度: {}", xmlContent.length());
                FileParseResultDTO result = parseXml(xmlContent.getBytes(StandardCharsets.UTF_8));
                result.setFileType("ofd");
                return result;
            }
            log.warn("[OFD解析] 未找到有效XML数据");
            return FileParseResultDTO.failed("OFD文件未包含有效XML数据");
        } catch (Exception e) {
            log.error("[OFD解析] 解析失败: {}", e.getMessage());
            return FileParseResultDTO.failed("OFD解析失败，请检查文件格式");
        }
    }

    @Override
    public FileParseResultDTO parseXml(byte[] fileContent) {
        log.info("[XML解析] 开始解析XML文件, 大小: {} bytes", fileContent.length);
        try {
            String xmlContent = new String(fileContent, StandardCharsets.UTF_8);
            if (xmlContent.contains("<xbrli:xbrl") || xmlContent.contains("<xbrl")) {
                log.info("[XML解析] 检测到XBRL格式");
                return parseXbrl(xmlContent);
            }
            if (xmlContent.contains("<EInvoice") || xmlContent.contains("<Einvoice")) {
                log.info("[XML解析] 检测到EInvoice格式（中国电子发票标准）");
                return parseEInvoice(xmlContent);
            }
            log.warn("[XML解析] 未识别的XML格式");
            return FileParseResultDTO.failed("未识别的XML格式");
        } catch (Exception e) {
            log.error("[XML解析] 解析失败: {}", e.getMessage());
            return FileParseResultDTO.failed("XML解析失败");
        }
    }

    public String detectFileType(byte[] content, String fileName) {
        if (content == null || content.length < 4) {
            return "unknown";
        }
        String header = new String(content, 0, Math.min(10, content.length), StandardCharsets.ISO_8859_1);
        if (header.startsWith(PDF_MAGIC)) {
            return "pdf";
        }
        if (header.startsWith(OFD_MAGIC)) {
            return "ofd";
        }
        if (header.trim().startsWith(XML_MAGIC)) {
            return "xml";
        }
        if (fileName != null) {
            String ext = fileName.toLowerCase();
            if (ext.endsWith(".pdf")) return "pdf";
            if (ext.endsWith(".ofd")) return "ofd";
            if (ext.endsWith(".xml")) return "xml";
        }
        return "unknown";
    }

    public String extractXmlFromPdf(byte[] pdfContent) {
        try (PDDocument document = PDDocument.load(pdfContent)) {
            PDDocumentNameDictionary names = new PDDocumentNameDictionary(document.getDocumentCatalog());
            PDEmbeddedFilesNameTreeNode embeddedFiles = names.getEmbeddedFiles();
            if (embeddedFiles == null) {
                log.debug("[PDF解析] 未找到内嵌文件");
                return null;
            }
            Map<String, PDComplexFileSpecification> files = getEmbeddedFiles(embeddedFiles);
            for (Map.Entry<String, PDComplexFileSpecification> entry : files.entrySet()) {
                String name = entry.getKey().toLowerCase();
                PDComplexFileSpecification spec = entry.getValue();
                PDEmbeddedFile embeddedFile = spec.getEmbeddedFile();
                if (embeddedFile != null && (name.contains(".xml") || name.contains("invoice"))) {
                    log.info("[PDF解析] 找到内嵌XML文件: {}", entry.getKey());
                    return new String(embeddedFile.toByteArray(), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            log.error("[PDF解析] 提取XML失败: {}", e.getMessage());
        }
        return null;
    }

    private Map<String, PDComplexFileSpecification> getEmbeddedFiles(PDEmbeddedFilesNameTreeNode node) {
        Map<String, PDComplexFileSpecification> result = new HashMap<>();
        try {
            Map<String, PDComplexFileSpecification> names = node.getNames();
            if (names != null) {
                result.putAll(names);
            }
            List<PDNameTreeNode<PDComplexFileSpecification>> kids = node.getKids();
            if (kids != null) {
                for (PDNameTreeNode<PDComplexFileSpecification> kid : kids) {
                    result.putAll(getEmbeddedFiles((PDEmbeddedFilesNameTreeNode) kid));
                }
            }
        } catch (IOException e) {
            log.error("[PDF解析] 获取内嵌文件列表失败: {}", e.getMessage());
        }
        return result;
    }

    public String extractXmlFromOfd(byte[] ofdContent) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(ofdContent))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase();
                if (name.contains("invoice.xml") || name.contains("einvoice.xml")) {
                    log.info("[OFD解析] 找到发票XML: {}", entry.getName());
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    return baos.toString(StandardCharsets.UTF_8.name());
                }
            }
        } catch (Exception e) {
            log.error("[OFD解析] 提取XML失败: {}", e.getMessage());
        }
        return null;
    }

    private XmlMapper createXmlMapper() {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XmlMapper xmlMapper = new XmlMapper(xmlInputFactory);
        xmlMapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
        return xmlMapper;
    }

    private FileParseResultDTO parseXbrl(String xmlContent) {
        try {
            String cleanedXml = preprocessXml(xmlContent);
            XmlMapper xmlMapper = createXmlMapper();
            JsonNode root = xmlMapper.readTree(cleanedXml.getBytes(StandardCharsets.UTF_8));
            FileParseResultDTO result = FileParseResultDTO.success("xml", "invoice", xmlContent);
            extractInvoiceFields(root, result);
            log.info("[XML解析] 解析成功: 发票号码={}", result.getInvoiceNo());
            return result;
        } catch (Exception e) {
            log.error("[XML解析] XBRL解析失败: {}", e.getMessage());
            return FileParseResultDTO.failed("XBRL解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析中国电子发票标准格式（EInvoice）
     * 依据：GB/T 34851-2017 电子发票基础信息规范
     */
    private FileParseResultDTO parseEInvoice(String xmlContent) {
        try {
            XmlMapper xmlMapper = createXmlMapper();
            JsonNode root = xmlMapper.readTree(xmlContent.getBytes(StandardCharsets.UTF_8));
            FileParseResultDTO result = FileParseResultDTO.success("xml", "invoice", xmlContent);
            result.setHasSignature(true);
            JsonNode header = root.path("Header");
            JsonNode eInvoiceData = root.path("EInvoiceData");
            JsonNode taxSupervisionInfo = root.path("TaxSupervisionInfo");
            String invoiceNumber = getTextValue(taxSupervisionInfo, "InvoiceNumber");
            if (invoiceNumber == null) {
                invoiceNumber = getTextValue(header, "EIid");
            }
            result.setInvoiceNo(invoiceNumber);
            String issueTime = getTextValue(taxSupervisionInfo, "IssueTime");
            result.setIssueDate(issueTime);
            JsonNode sellerInfo = eInvoiceData.path("SellerInformation");
            result.setSellerName(getTextValue(sellerInfo, "SellerName"));
            result.setSellerTaxNo(getTextValue(sellerInfo, "SellerIdNum"));
            result.setSellerAddressPhone(getTextValue(sellerInfo, "SellerAddr") + " " + getTextValue(sellerInfo, "SellerTelNum"));
            result.setSellerBankAccount(getTextValue(sellerInfo, "SellerBankName") + " " + getTextValue(sellerInfo, "SellerBankAccNum"));
            JsonNode buyerInfo = eInvoiceData.path("BuyerInformation");
            result.setBuyerName(getTextValue(buyerInfo, "BuyerName"));
            result.setBuyerTaxNo(getTextValue(buyerInfo, "BuyerIdNum"));
            result.setBuyerAddressPhone(getTextValue(buyerInfo, "BuyerAddr") + " " + getTextValue(buyerInfo, "BuyerTelNum"));
            result.setBuyerBankAccount(getTextValue(buyerInfo, "BuyerBankName") + " " + getTextValue(buyerInfo, "BuyerBankAccNum"));
            JsonNode basicInfo = eInvoiceData.path("BasicInformation");
            result.setAmountWithoutTax(getTextValue(basicInfo, "TotalAmWithoutTax"));
            result.setTaxAmount(getTextValue(basicInfo, "TotalTaxAm"));
            result.setTotalAmount(getTextValue(basicInfo, "TotalTax-includedAmount"));
            String totalAmountChinese = getTextValue(basicInfo, "TotalTax-includedAmountInChinese");
            result.setRemarks("大写金额: " + totalAmountChinese);
            result.setPayee(getTextValue(basicInfo, "Drawer"));
            JsonNode issuItemInfo = eInvoiceData.path("IssuItemInformation");
            if (issuItemInfo != null && !issuItemInfo.isMissingNode()) {
                result.setGoodsName(getTextValue(issuItemInfo, "ItemName"));
                result.setGoodsSpec(getTextValue(issuItemInfo, "SpecMod"));
                result.setGoodsUnit(getTextValue(issuItemInfo, "MeaUnits"));
                result.setGoodsQuantity(getTextValue(issuItemInfo, "Quantity"));
                result.setGoodsPrice(getTextValue(issuItemInfo, "UnPrice"));
                result.setTaxRate(getTextValue(issuItemInfo, "TaxRate"));
                List<InvoiceGoodsItemDTO> goodsItems = new ArrayList<>();
                InvoiceGoodsItemDTO item = new InvoiceGoodsItemDTO();
                item.setGoodsName(getTextValue(issuItemInfo, "ItemName"));
                item.setSpecification(getTextValue(issuItemInfo, "SpecMod"));
                item.setUnit(getTextValue(issuItemInfo, "MeaUnits"));
                try {
                    item.setQuantity(new BigDecimal(getTextValue(issuItemInfo, "Quantity")));
                    item.setUnitPrice(new BigDecimal(getTextValue(issuItemInfo, "UnPrice")));
                    item.setAmount(new BigDecimal(getTextValue(issuItemInfo, "Amount")));
                    item.setTaxRate(new BigDecimal(getTextValue(issuItemInfo, "TaxRate")));
                    item.setTaxAmount(new BigDecimal(getTextValue(issuItemInfo, "ComTaxAm")));
                } catch (Exception e) {
                    log.warn("[XML解析] 商品金额转换失败: {}", e.getMessage());
                }
                item.setGoodsCode(getTextValue(issuItemInfo, "TaxClassificationCode"));
                goodsItems.add(item);
                result.setGoodsItems(goodsItems);
            }
            JsonNode inherentLabel = header.path("InherentLabel");
            if (inherentLabel != null && !inherentLabel.isMissingNode()) {
                JsonNode eInvoiceType = inherentLabel.path("EInvoiceType");
                String invoiceTypeName = getTextValue(eInvoiceType, "LabelName");
                result.setVoucherTypeDesc(invoiceTypeName != null ? invoiceTypeName : "电子发票");
            }
            String taxBureauName = getTextValue(taxSupervisionInfo, "TaxBureauName");
            result.setRemarks(result.getRemarks() != null ? result.getRemarks() + "; 主管税务机关: " + taxBureauName : "主管税务机关: " + taxBureauName);
            log.info("[XML解析] EInvoice解析成功: 发票号码={}, 销方={}, 金额={}", result.getInvoiceNo(), result.getSellerName(), result.getTotalAmount());
            return result;
        } catch (Exception e) {
            log.error("[XML解析] EInvoice解析失败: {}", e.getMessage(), e);
            return FileParseResultDTO.failed("EInvoice解析失败: " + e.getMessage());
        }
    }

    private String preprocessXml(String xml) {
        return xml.replaceAll("xbrli:", "").replaceAll("xbrldt:", "").replaceAll("link:", "").replaceAll("xmlns[^\"]*\"[^\"]*\"", "").replaceAll("<![^>]*>", "").trim();
    }

    private void extractInvoiceFields(JsonNode root, FileParseResultDTO result) {
        JsonNode invoice = findInvoiceNode(root);
        if (invoice == null) {
            invoice = root;
        }
        result.setInvoiceCode(getTextValue(invoice, "发票代码", "InvoiceCode", "发票类型代码", "invoiceCode"));
        result.setInvoiceNo(getTextValue(invoice, "发票号码", "InvoiceNumber", "发票号码文本", "invoiceNo"));
        result.setIssueDate(getTextValue(invoice, "开票日期", "IssueDate", "开票日期文本", "issueDate"));
        result.setCheckCode(getTextValue(invoice, "校验码", "CheckCode", "校验码文本", "checkCode"));
        result.setBuyerName(getTextValue(invoice, "购买方名称", "BuyerName", "购方名称", "buyerName"));
        result.setBuyerTaxNo(getTextValue(invoice, "购买方纳税人识别号", "BuyerTaxID", "购方纳税人识别号", "buyerTaxNo"));
        result.setSellerName(getTextValue(invoice, "销售方名称", "SellerName", "销方名称", "sellerName"));
        result.setSellerTaxNo(getTextValue(invoice, "销售方纳税人识别号", "SellerTaxID", "销方纳税人识别号", "sellerTaxNo"));
        result.setAmountWithoutTax(getTextValue(invoice, "金额", "AmountWithoutTax", "合计金额", "amountWithoutTax"));
        result.setTaxAmount(getTextValue(invoice, "税额", "TaxAmount", "合计税额", "taxAmount"));
        result.setTotalAmount(getTextValue(invoice, "价税合计", "TotalAmount", "价税合计金额", "totalAmount"));
        extractGoodsInfoFromXml(invoice, result);
        result.setPayee(getTextValue(invoice, "收款人", "Payee", "收款人名称", "payee"));
        result.setChecker(getTextValue(invoice, "复核人", "Checker", "复核人名称", "checker"));
        result.setIssuer(getTextValue(invoice, "开票人", "Issuer", "开票人名称", "issuer"));
        result.setMachineNo(getTextValue(invoice, "机器编号", "MachineNo", "机器号码", "machineNo"));
        result.setHasSignature(true);
    }

    private void extractGoodsInfoFromXml(JsonNode invoice, FileParseResultDTO result) {
        JsonNode goodsDetails = null;
        String[] goodsArrayNames = {"货物或应税劳务服务名称", "GoodsDetail", "货物明细", "商品明细", "GoodsDetails", "goodsDetails"};
        for (String name : goodsArrayNames) {
            if (invoice.has(name)) {
                goodsDetails = invoice.get(name);
                break;
            }
        }
        if (goodsDetails == null) {
            for (JsonNode child : invoice) {
                if (child.isArray()) {
                    goodsDetails = child;
                    break;
                }
            }
        }
        if (goodsDetails != null && goodsDetails.isArray() && goodsDetails.size() > 0) {
            JsonNode firstGoods = goodsDetails.get(0);
            result.setGoodsName(getTextValue(firstGoods, "货物或应税劳务服务名称", "GoodsName", "商品名称", "goodsName"));
            result.setGoodsSpec(getTextValue(firstGoods, "规格型号", "Specification", "规格", "goodsSpec"));
            result.setGoodsUnit(getTextValue(firstGoods, "单位", "Unit", "计量单位", "goodsUnit"));
            result.setGoodsQuantity(getTextValue(firstGoods, "数量", "Quantity", "货物数量", "goodsQuantity"));
            result.setGoodsPrice(getTextValue(firstGoods, "单价", "UnitPrice", "货物单价", "goodsPrice"));
            result.setAmountWithoutTax(getTextValue(firstGoods, "金额", "Amount", "货物金额", "amount"));
            result.setTaxRate(getTextValue(firstGoods, "税率", "TaxRate", "货物税率", "taxRate"));
            log.info("[XML解析] 提取商品信息: name={}, spec={}, unit={}, qty={}, price={}", result.getGoodsName(), result.getGoodsSpec(), result.getGoodsUnit(), result.getGoodsQuantity(), result.getGoodsPrice());
        }
    }

    private JsonNode findInvoiceNode(JsonNode root) {
        if (root.has("发票")) return root.get("发票");
        if (root.has("Invoice")) return root.get("Invoice");
        if (root.has("电子发票")) return root.get("电子发票");
        if (root.has("EInvoice")) return root.get("EInvoice");
        for (JsonNode child : root) {
            if (child.isObject()) {
                JsonNode found = findInvoiceNode(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private String getTextValue(JsonNode node, String... fieldNames) {
        for (String name : fieldNames) {
            if (node.has(name)) {
                JsonNode field = node.get(name);
                if (field.isTextual()) {
                    return field.asText();
                } else if (field.isObject() && field.has("value")) {
                    return field.get("value").asText();
                } else if (field.isObject()) {
                    for (JsonNode child : field) {
                        if (child.isTextual()) {
                            return child.asText();
                        }
                    }
                }
            }
        }
        return null;
    }

    public FileParseServiceImpl(final ObjectMapper objectMapper, final VoucherToolkitService toolkitService, final InvoiceQrCodeService qrCodeService, final InvoiceOcrService ocrService) {
        this.objectMapper = objectMapper;
        this.toolkitService = toolkitService;
        this.qrCodeService = qrCodeService;
        this.ocrService = ocrService;
    }
}
