package com.foodtraceability.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * XML安全解析工具类
 * 
 * 提供安全的XML解析方法，防止XXE攻击
 * 符合财政部《电子凭证会计数据标准应用指南》要求
 */
public final class XmlParseUtils {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(XmlParseUtils.class);

    private XmlParseUtils() {
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 安全解析XML为JsonNode
     * 
     * 防止XXE攻击的安全配置：
     * - 禁用外部实体解析
     * - 禁用DTD
     * - 启用安全处理模式
     * 
     * @param xmlContent XML内容
     * @return JsonNode或null（解析失败时）
     */
    public static JsonNode parseXmlSecurely(String xmlContent) {
        try {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
            xmlInputFactory.setProperty(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            xmlInputFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            XmlMapper xmlMapper = new XmlMapper(xmlInputFactory);
            return xmlMapper.readTree(xmlContent.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("[XML安全解析] 解析异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 预处理XML，移除命名空间前缀以便JSON转换
     * 
     * 注意：此方法会改变XML内容，仅用于JSON转换，
     * 验签时必须使用原始XML内容
     * 
     * @param xml 原始XML
     * @return 预处理后的XML
     */
    public static String preprocessXmlForJson(String xml) {
        if (xml == null || xml.isEmpty()) {
            return xml;
        }
        String processed = xml;
        processed = processed.replaceAll("xmlns\\s*=\\s*\"[^\"]*\"", "");
        processed = processed.replaceAll("xmlns:\\w+\\s*=\\s*\"[^\"]*\"", "");
        processed = processed.replaceAll("<([a-zA-Z0-9]+):([a-zA-Z0-9]+)", "<$2");
        processed = processed.replaceAll("</([a-zA-Z0-9]+):([a-zA-Z0-9]+)", "</$2");
        return processed;
    }

    /**
     * 解析预处理后的XML为JsonNode（用于数据提取）
     */
    public static JsonNode parseXmlForDataExtraction(String xmlContent) {
        try {
            String processedXml = preprocessXmlForJson(xmlContent);
            return parseXmlSecurely(processedXml);
        } catch (Exception e) {
            log.error("[XML数据提取] 解析异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 递归查找签名节点
     */
    public static JsonNode findSignatureNode(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.has("Signature")) {
            return node.get("Signature");
        }
        if (node.has("ds:Signature")) {
            return node.get("ds:Signature");
        }
        for (JsonNode child : node) {
            if (child.isObject()) {
                JsonNode found = findSignatureNode(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * 检查是否包含数字签名
     */
    public static boolean hasSignature(String xmlContent) {
        if (xmlContent == null || xmlContent.isEmpty()) {
            return false;
        }
        return xmlContent.contains("<Signature") || xmlContent.contains("<ds:Signature") || xmlContent.contains("SignatureValue");
    }

    /**
     * 从JsonNode获取文本值
     */
    public static String getTextValue(JsonNode node, String... fieldNames) {
        if (node == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            if (node.has(fieldName) && !node.get(fieldName).isNull()) {
                return node.get(fieldName).asText();
            }
        }
        return null;
    }

    /**
     * 从JsonNode获取BigDecimal值
     */
    public static BigDecimal getBigDecimalValue(JsonNode node, String... fieldNames) {
        if (node == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            if (node.has(fieldName) && !node.get(fieldName).isNull()) {
                try {
                    return new BigDecimal(node.get(fieldName).asText());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * 查找数据节点
     */
    public static JsonNode findDataNode(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (root.has("EInvoiceData")) {
            return root.get("EInvoiceData");
        }
        if (root.has("Signature") && root.get("Signature").has("EInvoiceData")) {
            return root.get("Signature").get("EInvoiceData");
        }
        if (root.has("EInvoice") && root.get("EInvoice").has("EInvoiceData")) {
            return root.get("EInvoice").get("EInvoiceData");
        }
        return root;
    }

    /**
     * 获取XBRL命名空间字段值
     */
    public static String getXbrlValue(JsonNode root, String namespace, String fieldName) {
        if (root == null) {
            return null;
        }
        String fullFieldName = namespace + ":" + fieldName;
        if (root.has(fullFieldName)) {
            return root.get(fullFieldName).asText();
        }
        return null;
    }

    /**
     * 获取XBRL命名空间BigDecimal值
     */
    public static BigDecimal getXbrlDecimalValue(JsonNode root, String namespace, String fieldName) {
        String value = getXbrlValue(root, namespace, fieldName);
        if (value != null) {
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 检测凭证类型
     */
    public static String detectVoucherType(JsonNode root, String xmlContent) {
        if (root == null || xmlContent == null) {
            return "other";
        }
        String rootFieldName = root.fieldNames().hasNext() ? root.fieldNames().next() : "";
        if (xmlContent.contains("http://xbrl.mof.gov.cn/taxonomy/2023-12-31/rai") || rootFieldName.contains("rai:")) {
            return "train_ticket";
        }
        if (xmlContent.contains("http://xbrl.mof.gov.cn/taxonomy/2023-12-31/atr") || rootFieldName.contains("atr:")) {
            return "flight_ticket";
        }
        if (xmlContent.contains("http://xbrl.mof.gov.cn/taxonomy/2023-12-31/bker") || rootFieldName.contains("bker:")) {
            return "bank_receipt";
        }
        if (root.has("TrainTicket")) {
            return "train_ticket";
        }
        if (root.has("FlightTicket")) {
            return "flight_ticket";
        }
        if (root.has("BankReceipt")) {
            return "bank_receipt";
        }
        if (root.has("EInvoiceData") || root.has("InvoiceData") || root.has("InvoiceType") || root.has("InvoiceNo") || root.has("TypeOfInvoice") || root.has("NumberOfInvoice") || root.has("Buyer") || root.has("Seller")) {
            return "invoice";
        }
        if (root.has("Signature")) {
            JsonNode sig = root.get("Signature");
            if (sig.has("EInvoiceData") || sig.has("InvoiceData")) {
                return "invoice";
            }
        }
        return "other";
    }

    /**
     * 脱敏发票号码（日志使用）
     */
    public static String maskInvoiceNo(String invoiceNo) {
        if (invoiceNo == null || invoiceNo.length() < 8) {
            return invoiceNo;
        }
        return invoiceNo.substring(0, 4) + "****" + invoiceNo.substring(invoiceNo.length() - 4);
    }

    /**
     * 脱敏金额（日志使用）
     */
    public static String maskAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return "***";
    }
}
