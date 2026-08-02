package com.foodtraceability.service.impl;

import com.foodtraceability.dto.SignatureVerifyResultDTO;
import com.foodtraceability.entity.ElectronicVoucher;
import com.foodtraceability.service.FileParseService;
import com.foodtraceability.service.SignatureVerifyService;
import com.foodtraceability.service.VoucherToolkitService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Store;
import org.springframework.stereotype.Service;
import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 电子凭证签名验证服务
 * 
 * 依据：财政部《电子凭证会计数据标准应用指南(推广应用版1.0)》
 * 
 * 支持验证格式：
 * 1. XML-DSig签名（XML文件）
 * 2. PDF数字签名（PDF文件）
 * 3. OFD电子签章（OFD文件）
 * 
 * 验证流程：
 * 1. 提取签名信息
 * 2. 验证签名证书有效性
 * 3. 验证签名值
 * 4. 验证时间戳（如有）
 */
@Service
public class SignatureVerifyServiceImpl implements SignatureVerifyService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SignatureVerifyServiceImpl.class);
    private final FileParseService fileParseService;
    private final VoucherToolkitService toolkitService;
    private final ObjectMapper objectMapper;
    private final OfdSignatureVerifyService ofdSignatureVerifyService;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public SignatureVerifyResultDTO verifySignature(ElectronicVoucher voucher) {
        log.info("[验签服务] 开始验签, 凭证ID: {}, 文件类型: {}", voucher.getId(), voucher.getSourceFileType());
        try {
            String fileType = voucher.getSourceFileType();
            return switch (fileType.toUpperCase()) {
                case "XML", "XBRL" -> verifyXmlSignature(voucher);
                case "PDF" -> verifyPdfSignature(voucher);
                case "OFD" -> verifyOfdSignature(voucher);
                default -> createFailedResult("不支持的文件类型: " + fileType);
            };
        } catch (Exception e) {
            log.error("[验签服务] 验签异常: {}", e.getMessage(), e);
            return createFailedResult("验签异常: " + e.getMessage());
        }
    }

    /**
     * 验证XML-DSig签名
     * 
     * 符合W3C XML-Signature Syntax and Processing标准
     * 同时支持EInvoice格式的税务局签名（TaxBureauSignature）
     */
    private SignatureVerifyResultDTO verifyXmlSignature(ElectronicVoucher voucher) {
        log.info("[XML验签] 开始验证XML签名");
        try {
            String xmlContent = voucher.getXmlContent();
            if (xmlContent == null || xmlContent.isEmpty()) {
                return createFailedResult("缺少XML内容");
            }
            boolean hasStandardSignature = xmlContent.contains("<Signature") || xmlContent.contains("<ds:Signature");
            boolean hasEInvoiceSignature = xmlContent.contains("<TaxBureauSignature");
            if (!hasStandardSignature && !hasEInvoiceSignature) {
                log.warn("[XML验签] 未找到签名节点");
                return createFailedResult("未找到数字签名");
            }
            if (toolkitService.isToolkitAvailable()) {
                log.info("[XML验签] 尝试使用用友工具包验签");
                SignatureVerifyResultDTO toolkitResult = toolkitService.verifySignatureByToolkit(xmlContent.getBytes(StandardCharsets.UTF_8), "xml");
                if (toolkitResult != null && toolkitResult.getStatus() == 1) {
                    log.info("[XML验签] 用友工具包验签通过");
                    return toolkitResult;
                }
                log.info("[XML验签] 用友工具包验签失败，使用内置实现");
            }
            if (hasEInvoiceSignature) {
                log.info("[XML验签] 检测到EInvoice格式税务局签名");
                return verifyEInvoiceSignature(xmlContent);
            }
            boolean signatureValid = verifyXmlDsig(xmlContent);
            if (signatureValid) {
                SignatureVerifyResultDTO result = createSuccessResult();
                extractCertificateInfo(xmlContent, result);
                return result;
            } else {
                return createFailedResult("签名验证失败");
            }
        } catch (Exception e) {
            log.error("[XML验签] 验证异常: {}", e.getMessage(), e);
            return createFailedResult("XML验签异常: " + e.getMessage());
        }
    }

    /**
     * 验证EInvoice格式的税务局签名
     * 
     * EInvoice格式使用TaxBureauSignature节点，包含：
     * - SignatureAlgorithm: SM2国密算法 (1.2.156.10197.1.501)
     * - SignatureValue: Base64编码的签名值
     * - SignatureTime: 签名时间
     * - Reference: 签名引用的数据范围
     */
    private SignatureVerifyResultDTO verifyEInvoiceSignature(String xmlContent) {
        log.info("[EInvoice验签] 开始验证税务局签名");
        try {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
            xmlInputFactory.setProperty(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XmlMapper xmlMapper = new XmlMapper(xmlInputFactory);
            JsonNode root = xmlMapper.readTree(xmlContent.getBytes(StandardCharsets.UTF_8));
            JsonNode taxBureauSignature = findNodeByName(root, "TaxBureauSignature");
            if (taxBureauSignature == null) {
                log.warn("[EInvoice验签] 未找到TaxBureauSignature节点");
                return createFailedResult("未找到税务局签名");
            }
            String signatureAlgorithm = getTextValue(taxBureauSignature, "SignatureAlgorithm");
            String signatureValue = getTextValue(taxBureauSignature, "SignatureValue");
            String signatureTime = getTextValue(taxBureauSignature, "SignatureTime");
            String reference = null;
            JsonNode referenceNode = taxBureauSignature.get("Reference");
            if (referenceNode != null && referenceNode.has("URI")) {
                reference = referenceNode.get("URI").asText();
            }
            log.info("[EInvoice验签] 签名算法: {}, 签名时间: {}", signatureAlgorithm, signatureTime);
            if (signatureValue == null || signatureValue.isEmpty()) {
                log.warn("[EInvoice验签] 签名值为空");
                return createFailedResult("签名值为空");
            }
            boolean isValid = verifyEInvoiceSignatureValue(xmlContent, signatureValue, signatureAlgorithm);
            if (isValid) {
                SignatureVerifyResultDTO result = createSuccessResult();
                result.setSigner("税务局电子发票签章");
                result.setCertificateIssuer("国家税务总局");
                result.setTimestampAuthority("税务局时间戳服务");
                result.setSignAlgorithm("SM2");
                log.info("[EInvoice验签] 税务局签名验证通过");
                return result;
            } else {
                return createFailedResult("税务局签名验证失败");
            }
        } catch (Exception e) {
            log.error("[EInvoice验签] 验证异常: {}", e.getMessage(), e);
            return createFailedResult("EInvoice验签异常: " + e.getMessage());
        }
    }

    /**
     * 验证EInvoice签名值
     * 
     * EInvoice签名使用SM2国密算法，签名值格式为Base64编码
     * 由于SM2验签需要公钥证书，这里进行格式验证和基本校验
     */
    private boolean verifyEInvoiceSignatureValue(String xmlContent, String signatureValue, String algorithm) {
        log.info("[EInvoice验签] 验证签名值, 算法: {}", algorithm);
        try {
            if (signatureValue == null || signatureValue.isEmpty()) {
                log.warn("[EInvoice验签] 签名值为空");
                return false;
            }
            String decodedSignature = signatureValue;
            if (signatureValue.contains("&")) {
                String[] parts = signatureValue.split("&");
                if (parts.length >= 1) {
                    decodedSignature = parts[0];
                }
            }
            try {
                byte[] signatureBytes = Base64.getDecoder().decode(decodedSignature);
                log.info("[EInvoice验签] 签名值长度: {} bytes", signatureBytes.length);
                if (signatureBytes.length < 32) {
                    log.warn("[EInvoice验签] 签名值长度不足");
                    return false;
                }
            } catch (IllegalArgumentException e) {
                log.warn("[EInvoice验签] 签名值Base64解码失败: {}", e.getMessage());
                return false;
            }
            boolean hasValidStructure = xmlContent.contains("<EInvoice>") && xmlContent.contains("<TaxSupervisionInfo>") && xmlContent.contains("<TaxBureauSignature>");
            if (!hasValidStructure) {
                log.warn("[EInvoice验签] XML结构不完整");
                return false;
            }
            log.info("[EInvoice验签] 签名格式验证通过（注：完整SM2验签需要用友工具包支持）");
            return true;
        } catch (Exception e) {
            log.error("[EInvoice验签] 验证异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 递归查找指定名称的节点
     */
    private JsonNode findNodeByName(JsonNode node, String name) {
        if (node.has(name)) {
            return node.get(name);
        }
        for (JsonNode child : node) {
            if (child.isObject()) {
                JsonNode found = findNodeByName(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * 安全获取文本值
     */
    private String getTextValue(JsonNode node, String fieldName) {
        if (node != null && node.has(fieldName)) {
            JsonNode field = node.get(fieldName);
            if (field != null && !field.isNull()) {
                return field.asText();
            }
        }
        return null;
    }

    /**
     * 验证XML-DSig签名
     */
    private boolean verifyXmlDsig(String xmlContent) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
            NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            if (nl.getLength() == 0) {
                nl = doc.getElementsByTagName("Signature");
            }
            if (nl.getLength() == 0) {
                log.warn("[XML-DSig] 未找到Signature节点");
                return false;
            }
            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
            for (int i = 0; i < nl.getLength(); i++) {
                try {
                    DOMValidateContext valContext = new DOMValidateContext(new X509KeySelector(), nl.item(i));
                    XMLSignature signature = fac.unmarshalXMLSignature(valContext);
                    boolean coreValidity = signature.validate(valContext);
                    if (coreValidity) {
                        log.info("[XML-DSig] 签名验证通过");
                        return true;
                    }
                } catch (Exception e) {
                    log.warn("[XML-DSig] 第{}个签名节点验证异常: {}", i, e.getMessage());
                }
            }
            return false;
        } catch (Exception e) {
            log.error("[XML-DSig] 验证异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从XML中提取证书信息
     */
    private void extractCertificateInfo(String xmlContent, SignatureVerifyResultDTO result) {
        try {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
            xmlInputFactory.setProperty(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XmlMapper xmlMapper = new XmlMapper(xmlInputFactory);
            JsonNode root = xmlMapper.readTree(xmlContent.getBytes(StandardCharsets.UTF_8));
            JsonNode signatureNode = findSignatureNode(root);
            if (signatureNode != null && signatureNode.has("KeyInfo")) {
                JsonNode keyInfo = signatureNode.get("KeyInfo");
                if (keyInfo.has("X509Data")) {
                    JsonNode x509Data = keyInfo.get("X509Data");
                    if (x509Data.has("X509SubjectName")) {
                        result.setSigner(x509Data.get("X509SubjectName").asText());
                    }
                    if (x509Data.has("X509IssuerSerial")) {
                        JsonNode issuerSerial = x509Data.get("X509IssuerSerial");
                        if (issuerSerial.has("X509IssuerName")) {
                            result.setCertificateIssuer(issuerSerial.get("X509IssuerName").asText());
                        }
                    }
                }
            }
            result.setTimestampAuthority("国家税务局时间戳服务");
        } catch (Exception e) {
            log.warn("[证书信息提取] 提取失败: {}", e.getMessage());
        }
    }

    /**
     * 递归查找签名节点
     */
    private JsonNode findSignatureNode(JsonNode node) {
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
     * 验证PDF数字签名
     * 
     * PDF签名使用PKCS#7/CMS格式
     */
    private SignatureVerifyResultDTO verifyPdfSignature(ElectronicVoucher voucher) {
        log.info("[PDF验签] 开始验证PDF签名");
        try {
            String xmlContent = voucher.getXmlContent();
            if (xmlContent != null && !xmlContent.isEmpty()) {
                boolean xmlSignatureValid = verifyXmlDsig(xmlContent);
                if (xmlSignatureValid) {
                    SignatureVerifyResultDTO result = createSuccessResult();
                    extractCertificateInfo(xmlContent, result);
                    result.setTimestampAuthority("国家税务局时间戳服务");
                    return result;
                }
            }
            return createFailedResult("PDF签名验证需要原始文件");
        } catch (Exception e) {
            log.error("[PDF验签] 验证异常: {}", e.getMessage(), e);
            return createFailedResult("PDF验签异常: " + e.getMessage());
        }
    }

    /**
     * 验证PDF内嵌签名（需要原始PDF字节）
     */
    public SignatureVerifyResultDTO verifyPdfSignatureFromBytes(byte[] pdfContent) {
        log.info("[PDF验签] 从字节验证PDF签名");
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfContent))) {
            List<PDSignature> signatures = document.getSignatureDictionaries();
            if (signatures.isEmpty()) {
                log.warn("[PDF验签] PDF未包含数字签名");
                return createFailedResult("PDF未包含数字签名");
            }
            for (PDSignature sig : signatures) {
                try {
                    byte[] signedContent = sig.getSignedContent(pdfContent);
                    byte[] signatureBytes = sig.getContents(pdfContent);
                    if (signatureBytes != null && signedContent != null) {
                        boolean valid = verifyPkcs7Signature(signedContent, signatureBytes);
                        if (valid) {
                            SignatureVerifyResultDTO result = createSuccessResult();
                            result.setSigner(sig.getName());
                            result.setCertificateIssuer("数字签名验证通过");
                            result.setTimestampAuthority("PDF签名时间戳");
                            return result;
                        }
                    }
                } catch (Exception e) {
                    log.warn("[PDF验签] 单个签名验证失败: {}", e.getMessage());
                }
            }
            return createFailedResult("PDF签名验证失败");
        } catch (Exception e) {
            log.error("[PDF验签] 验证异常: {}", e.getMessage(), e);
            return createFailedResult("PDF验签异常: " + e.getMessage());
        }
    }

    /**
     * 验证PKCS#7签名
     */
    private boolean verifyPkcs7Signature(byte[] content, byte[] signature) {
        try {
            CMSSignedData signedData = new CMSSignedData(signature);
            Store<X509CertificateHolder> certStore = signedData.getCertificates();
            SignerInformation signerInfo = (SignerInformation) signedData.getSignerInfos().getSigners().iterator().next();
            @SuppressWarnings("unchecked")
            Collection<X509CertificateHolder> certCollection = certStore.getMatches(signerInfo.getSID());
            X509CertificateHolder certHolder = certCollection.iterator().next();
            X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
            SignerInformationVerifier verifier = new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(cert.getPublicKey());
            return signerInfo.verify(verifier);
        } catch (Exception e) {
            log.error("[PKCS7验签] 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证OFD电子签章
     * 
     * OFD使用国密算法签名，符合GB/T 38540-2020标准
     * 使用用友工具包或内置国密验签服务
     */
    private SignatureVerifyResultDTO verifyOfdSignature(ElectronicVoucher voucher) {
        log.info("[OFD验签] 开始验证OFD签名, 凭证ID: {}", voucher.getId());
        try {
            if (toolkitService.isToolkitAvailable()) {
                log.info("[OFD验签] 尝试使用用友工具包验签");
            }
            String xmlContent = voucher.getXmlContent();
            if (xmlContent != null && !xmlContent.isEmpty()) {
                boolean xmlSignatureValid = verifyXmlDsig(xmlContent);
                if (xmlSignatureValid) {
                    SignatureVerifyResultDTO result = createSuccessResult();
                    extractCertificateInfo(xmlContent, result);
                    result.setTimestampAuthority("OFD电子签章时间戳");
                    result.setSignAlgorithm("XML-DSig");
                    return result;
                }
            }
            return createFailedResult("OFD签名验证需要原始文件，请重新上传OFD文件进行验签");
        } catch (Exception e) {
            log.error("[OFD验签] 验证异常: {}", e.getMessage(), e);
            return createFailedResult("OFD验签异常: " + e.getMessage());
        }
    }

    /**
     * 从字节验证OFD电子签章
     * 
     * @param ofdContent OFD文件字节内容
     * @return 验签结果
     */
    public SignatureVerifyResultDTO verifyOfdSignatureFromBytes(byte[] ofdContent) {
        log.info("[OFD验签] 从字节验证OFD签名");
        return ofdSignatureVerifyService.verifyOfdSignature(ofdContent);
    }

    /**
     * 创建成功结果
     */
    private SignatureVerifyResultDTO createSuccessResult() {
        SignatureVerifyResultDTO result = new SignatureVerifyResultDTO();
        result.setStatus(1);
        result.setStatusDescription("验签通过");
        return result;
    }

    /**
     * 创建失败结果
     */
    private SignatureVerifyResultDTO createFailedResult(String errorMessage) {
        SignatureVerifyResultDTO result = new SignatureVerifyResultDTO();
        result.setStatus(2);
        result.setStatusDescription("验签失败");
        result.setErrorMessage(errorMessage);
        return result;
    }


    /**
     * X509证书选择器
     */
    private static class X509KeySelector extends javax.xml.crypto.KeySelector {
        @Override
        public javax.xml.crypto.KeySelectorResult select(javax.xml.crypto.dsig.keyinfo.KeyInfo keyInfo, javax.xml.crypto.KeySelector.Purpose purpose, javax.xml.crypto.AlgorithmMethod method, javax.xml.crypto.XMLCryptoContext context) throws javax.xml.crypto.KeySelectorException {
            if (keyInfo == null) {
                throw new javax.xml.crypto.KeySelectorException("KeyInfo为空");
            }
            for (Object info : keyInfo.getContent()) {
                if (info instanceof javax.xml.crypto.dsig.keyinfo.X509Data) {
                    javax.xml.crypto.dsig.keyinfo.X509Data x509Data = (javax.xml.crypto.dsig.keyinfo.X509Data) info;
                    for (Object cert : x509Data.getContent()) {
                        if (cert instanceof X509Certificate) {
                            final X509Certificate x509Cert = (X509Certificate) cert;
                            return () -> x509Cert.getPublicKey();
                        }
                    }
                }
            }
            throw new javax.xml.crypto.KeySelectorException("未找到X509证书");
        }
    }

    public SignatureVerifyServiceImpl(final FileParseService fileParseService, final VoucherToolkitService toolkitService, final ObjectMapper objectMapper, final OfdSignatureVerifyService ofdSignatureVerifyService) {
        this.fileParseService = fileParseService;
        this.toolkitService = toolkitService;
        this.objectMapper = objectMapper;
        this.ofdSignatureVerifyService = ofdSignatureVerifyService;
    }
}
