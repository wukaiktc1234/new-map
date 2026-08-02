package com.foodtraceability.service.impl;

import com.foodtraceability.dto.VoucherTypeDetectResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 凭证类型自动识别服务
 * 
 * 根据《电子凭证会计数据标准应用指南(推广应用版1.0)》
 * 自动识别上传文件的凭证类型
 */
@Service
public class VoucherTypeDetectService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VoucherTypeDetectService.class);
    private static final Map<String, String> XML_ROOT_NODE_MAP = new HashMap<>();
    private static final Map<String, String> MIME_TYPE_MAP = new HashMap<>();

    static {
        XML_ROOT_NODE_MAP.put("EInvoice", "invoice");
        XML_ROOT_NODE_MAP.put("FullElectronicInvoice", "full_electronic");
        XML_ROOT_NODE_MAP.put("EInvoiceData", "invoice");
        XML_ROOT_NODE_MAP.put("Invoice", "invoice");
        XML_ROOT_NODE_MAP.put("BankReceipt", "bank_receipt");
        XML_ROOT_NODE_MAP.put("ElectronicBankReceipt", "bank_receipt");
        XML_ROOT_NODE_MAP.put("TrainTicket", "train_ticket");
        XML_ROOT_NODE_MAP.put("ElectronicTrainTicket", "train_ticket");
        XML_ROOT_NODE_MAP.put("FlightTicket", "flight_ticket");
        XML_ROOT_NODE_MAP.put("ElectronicFlightTicket", "flight_ticket");
        XML_ROOT_NODE_MAP.put("FiscalReceipt", "fiscal_receipt");
        XML_ROOT_NODE_MAP.put("ElectronicFiscalReceipt", "fiscal_receipt");
        MIME_TYPE_MAP.put("application/pdf", "pdf");
        MIME_TYPE_MAP.put("application/xml", "xml");
        MIME_TYPE_MAP.put("text/xml", "xml");
        MIME_TYPE_MAP.put("application/ofd", "ofd");
        MIME_TYPE_MAP.put("image/jpeg", "image");
        MIME_TYPE_MAP.put("image/png", "image");
        MIME_TYPE_MAP.put("image/tiff", "image");
    }

    private static final List<String> INVOICE_TYPES = Arrays.asList("invoice", "full_electronic", "vat_special", "vat_general");

    /**
     * 自动检测凭证类型
     */
    public VoucherTypeDetectResult detectType(MultipartFile file) {
        log.info("[类型识别] 开始识别文件类型: {}", file.getOriginalFilename());
        VoucherTypeDetectResult result = new VoucherTypeDetectResult();
        result.setFileName(file.getOriginalFilename());
        result.setFileSize(file.getSize());
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "";
        }
        String extension = getFileExtension(originalFilename).toLowerCase();
        result.setFileExtension(extension);
        String mimeType = file.getContentType();
        result.setMimeType(mimeType);
        try {
            if ("xml".equals(extension) || "text/xml".equals(mimeType) || "application/xml".equals(mimeType)) {
                detectFromXml(file, result);
            } else if ("pdf".equals(extension) || "application/pdf".equals(mimeType)) {
                detectFromPdf(file, result);
            } else if ("ofd".equals(extension) || "application/ofd".equals(mimeType)) {
                detectFromOfd(file, result);
            } else if (isImageFile(extension, mimeType)) {
                detectFromImage(file, result);
            } else {
                result.setVoucherType("unknown");
                result.setConfidence(0.0);
                result.setMessage("无法识别的文件类型");
            }
        } catch (Exception e) {
            log.error("[类型识别] 识别失败: {}", e.getMessage());
            result.setVoucherType("unknown");
            result.setConfidence(0.0);
            result.setMessage("识别失败: " + e.getMessage());
        }
        log.info("[类型识别] 识别完成: 类型={}, 置信度={}", result.getVoucherType(), result.getConfidence());
        return result;
    }

    /**
     * 从XML内容检测凭证类型
     */
    private void detectFromXml(MultipartFile file, VoucherTypeDetectResult result) throws IOException {
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new IllegalArgumentException("文件大小超过20MB限制");
        }
        int maxRead = (int) Math.min(file.getSize(), 51200);
        byte[] buffer = new byte[maxRead];
        file.getInputStream().read(buffer);
        String content = new String(buffer, StandardCharsets.UTF_8);
        content = content.replaceAll("<!ENTITY[^>]*>", "");
        content = content.replaceAll("<!DOCTYPE[^>]*>", "");
        String rootNode = extractXmlRootNode(content);
        result.setXmlRootNode(rootNode);
        if (XML_ROOT_NODE_MAP.containsKey(rootNode)) {
            String voucherType = XML_ROOT_NODE_MAP.get(rootNode);
            result.setVoucherType(voucherType);
            result.setConfidence(0.95);
            result.setMessage("通过XML根节点识别");
            if (content.contains("<TaxBureauSignature>")) {
                result.setHasSignature(true);
                result.setSignatureType("TaxBureauSignature");
            } else if (content.contains("<ds:Signature>")) {
                result.setHasSignature(true);
                result.setSignatureType("XML-DSig");
            } else if (content.contains("<Signature>")) {
                result.setHasSignature(true);
                result.setSignatureType("XML-DSig");
            }
            if ("EInvoice".equals(rootNode)) {
                if (content.contains("<EInvoiceTag>SWEI")) {
                    result.setSubType("全电发票");
                } else {
                    result.setSubType("增值税电子发票");
                }
            }
        } else if (content.contains("<InvoiceCode>") || content.contains("<InvoiceNo>")) {
            result.setVoucherType("invoice");
            result.setConfidence(0.8);
            result.setMessage("通过发票字段识别");
        } else if (content.contains("<BuyerName>") && content.contains("<SellerName>")) {
            result.setVoucherType("invoice");
            result.setConfidence(0.7);
            result.setMessage("通过买卖双方字段识别");
        } else {
            result.setVoucherType("xml_document");
            result.setConfidence(0.5);
            result.setMessage("未知XML格式");
        }
        result.setParseMethod("XML解析");
    }

    /**
     * 从PDF检测凭证类型
     */
    private void detectFromPdf(MultipartFile file, VoucherTypeDetectResult result) {
        result.setVoucherType("invoice");
        result.setConfidence(0.6);
        result.setMessage("PDF文件，需要进一步解析");
        result.setParseMethod("PDF解析");
        result.setNeedOcr(true);
        String filename = file.getOriginalFilename();
        if (filename != null) {
            if (filename.contains("发票") || filename.toLowerCase().contains("invoice")) {
                result.setVoucherType("invoice");
                result.setConfidence(0.8);
            } else if (filename.contains("回单") || filename.toLowerCase().contains("receipt")) {
                result.setVoucherType("bank_receipt");
                result.setConfidence(0.8);
            } else if (filename.contains("火车") || filename.toLowerCase().contains("train")) {
                result.setVoucherType("train_ticket");
                result.setConfidence(0.8);
            } else if (filename.contains("机票") || filename.toLowerCase().contains("flight")) {
                result.setVoucherType("flight_ticket");
                result.setConfidence(0.8);
            }
        }
    }

    /**
     * 从OFD检测凭证类型
     */
    private void detectFromOfd(MultipartFile file, VoucherTypeDetectResult result) {
        result.setVoucherType("invoice");
        result.setConfidence(0.7);
        result.setMessage("OFD电子文件，需要进一步解析");
        result.setParseMethod("OFD解析");
        result.setHasSignature(true);
        result.setSignatureType("OFD签名");
    }

    /**
     * 从图片检测凭证类型
     */
    private void detectFromImage(MultipartFile file, VoucherTypeDetectResult result) {
        result.setVoucherType("invoice");
        result.setConfidence(0.5);
        result.setMessage("图片文件，需要OCR识别");
        result.setParseMethod("OCR识别");
        result.setNeedOcr(true);
        String filename = file.getOriginalFilename();
        if (filename != null) {
            if (filename.contains("发票") || filename.toLowerCase().contains("invoice")) {
                result.setVoucherType("invoice");
                result.setConfidence(0.7);
            } else if (filename.contains("回单")) {
                result.setVoucherType("bank_receipt");
                result.setConfidence(0.7);
            } else if (filename.contains("火车") || filename.contains("ticket")) {
                result.setVoucherType("train_ticket");
                result.setConfidence(0.7);
            }
        }
    }

    /**
     * 提取XML根节点名称
     */
    private String extractXmlRootNode(String xmlContent) {
        try {
            String trimmed = xmlContent.trim();
            int start = trimmed.indexOf("<");
            if (start < 0) {
                return null;
            }
            int end = trimmed.indexOf(">", start);
            if (end < 0) {
                return null;
            }
            String tag = trimmed.substring(start + 1, end);
            if (tag.startsWith("?xml")) {
                int nextStart = trimmed.indexOf("<", end + 1);
                if (nextStart >= 0) {
                    int nextEnd = trimmed.indexOf(">", nextStart);
                    if (nextEnd >= 0) {
                        tag = trimmed.substring(nextStart + 1, nextEnd);
                    }
                }
            }
            if (tag.contains(" ")) {
                tag = tag.substring(0, tag.indexOf(" "));
            }
            if (tag.startsWith("ns:") || tag.contains(":")) {
                tag = tag.substring(tag.indexOf(":") + 1);
            }
            return tag;
        } catch (Exception e) {
            log.warn("[类型识别] 提取XML根节点失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String extension, String mimeType) {
        if (mimeType != null && mimeType.startsWith("image/")) {
            return true;
        }
        return Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif").contains(extension.toLowerCase());
    }

    /**
     * 判断是否为发票类型
     */
    public boolean isInvoiceType(String voucherType) {
        return INVOICE_TYPES.contains(voucherType);
    }
}
