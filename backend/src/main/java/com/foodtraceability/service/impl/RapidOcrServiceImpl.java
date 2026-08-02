package com.foodtraceability.service.impl;

import com.benjaminwan.ocrlibrary.OcrResult;
import com.foodtraceability.dto.InvoiceGoodsItemDTO;
import com.foodtraceability.dto.InvoiceOcrResultDTO;
import com.foodtraceability.service.InvoiceOcrService;
import io.github.mymonstercat.Model;
import io.github.mymonstercat.ocr.config.ParamConfig;
import io.github.mymonstercat.ocr.InferenceEngine;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Primary
public class RapidOcrServiceImpl implements InvoiceOcrService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RapidOcrServiceImpl.class);
    private static final String ENGINE_NAME = "RapidOcr-Java";
    @Value("${ocr.rapid.enabled:true}")
    private boolean rapidOcrEnabled;
    private InferenceEngine inferenceEngine;
    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (!rapidOcrEnabled) {
            log.info("[RapidOCR] RapidOCR服务已禁用");
            return;
        }
        try {
            log.info("[RapidOCR] 正在初始化RapidOCR服务...");
            inferenceEngine = InferenceEngine.getInstance(Model.ONNX_PPOCR_V3);
            initialized = true;
            log.info("[RapidOCR] RapidOCR服务初始化成功，使用PP-OCRv3模型");
        } catch (Exception e) {
            log.error("[RapidOCR] 初始化失败: {}", e.getMessage(), e);
            initialized = false;
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("[RapidOCR] 关闭RapidOCR服务...");
        initialized = false;
    }

    @Override
    public InvoiceOcrResultDTO recognizeFromPdf(byte[] pdfContent) {
        log.info("[RapidOCR] 开始识别PDF, 大小: {} bytes", pdfContent.length);
        if (!initialized) {
            log.error("[RapidOCR] 服务未初始化!");
            return InvoiceOcrResultDTO.failed("RapidOCR服务未初始化");
        }
        try {
            List<String> allTextLines = new ArrayList<>();
            log.info("[RapidOCR] 正在加载PDF文档...");
            PDDocument document = PDDocument.load(pdfContent);
            PDFRenderer renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();
            log.info("[RapidOCR] PDF共 {} 页", totalPages);
            for (int i = 0; i < totalPages; i++) {
                log.info("[RapidOCR] 正在渲染第 {} 页...", i + 1);
                BufferedImage image = renderer.renderImage(i, 2.0F);
                log.info("[RapidOCR] 第 {} 页渲染完成, 尺寸: {}x{}", i + 1, image.getWidth(), image.getHeight());
                byte[] imageBytes = imageToBytes(image);
                List<String> pageTexts = recognizeImageBytes(imageBytes);
                log.info("[RapidOCR] 第 {} 页识别完成, 识别到 {} 行文本", i + 1, pageTexts.size());
                for (int j = 0; j < pageTexts.size(); j++) {
                    log.info("[RapidOCR] 第 {} 页第 {} 行: {}", i + 1, j + 1, pageTexts.get(j));
                }
                allTextLines.addAll(pageTexts);
            }
            document.close();
            log.info("[RapidOCR] PDF识别完成, 共识别到 {} 行文本", allTextLines.size());
            return extractInvoiceFieldsOptimized(allTextLines);
        } catch (Exception e) {
            log.error("[RapidOCR] PDF识别失败: {}", e.getMessage(), e);
            return InvoiceOcrResultDTO.failed("PDF识别失败: " + e.getMessage());
        }
    }

    @Override
    public InvoiceOcrResultDTO recognizeFromImage(BufferedImage image) {
        log.info("[RapidOCR] 开始识别图片, 尺寸: {}x{}", image.getWidth(), image.getHeight());
        if (!initialized) {
            return InvoiceOcrResultDTO.failed("RapidOCR服务未初始化");
        }
        try {
            byte[] imageBytes = imageToBytes(image);
            List<String> textLines = recognizeImageBytes(imageBytes);
            return extractInvoiceFieldsOptimized(textLines);
        } catch (Exception e) {
            log.error("[RapidOCR] 图片识别失败: {}", e.getMessage(), e);
            return InvoiceOcrResultDTO.failed("图片识别失败: " + e.getMessage());
        }
    }

    @Override
    public InvoiceOcrResultDTO recognizeFromImageBytes(byte[] imageData, String format) {
        log.info("[RapidOCR] 开始识别图片数据, 大小: {} bytes", imageData.length);
        if (!initialized) {
            return InvoiceOcrResultDTO.failed("RapidOCR服务未初始化");
        }
        try {
            List<String> textLines = recognizeImageBytes(imageData);
            return extractInvoiceFieldsOptimized(textLines);
        } catch (Exception e) {
            log.error("[RapidOCR] 图片识别失败: {}", e.getMessage(), e);
            return InvoiceOcrResultDTO.failed("图片识别失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return initialized && rapidOcrEnabled;
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    private List<String> recognizeImageBytes(byte[] imageData) {
        List<String> results = new ArrayList<>();
        java.io.File tempFile = null;
        try {
            tempFile = java.io.File.createTempFile("ocr_image_", ".png");
            java.nio.file.Files.write(tempFile.toPath(), imageData);
            log.info("[RapidOCR] 临时文件创建: {}", tempFile.getAbsolutePath());
            ParamConfig paramConfig = ParamConfig.getDefaultConfig();
            paramConfig.setDoAngle(true);
            paramConfig.setMostAngle(true);
            OcrResult ocrResult = inferenceEngine.runOcr(tempFile.getAbsolutePath(), paramConfig);
            if (ocrResult != null && ocrResult.getStrRes() != null) {
                String[] lines = ocrResult.getStrRes().split("\n");
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        results.add(trimmed);
                    }
                }
            }
            log.info("[RapidOCR] 识别到 {} 行文本", results.size());
        } catch (Exception e) {
            log.error("[RapidOCR] 识别失败: {}", e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
        return results;
    }

    private byte[] imageToBytes(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    private InvoiceOcrResultDTO extractInvoiceFieldsOptimized(List<String> textLines) {
        InvoiceOcrResultDTO result = InvoiceOcrResultDTO.success(ENGINE_NAME);
        result.setRawText(String.join("\n", textLines));
        String fullText = String.join("\n", textLines);
        result.setInvoiceCode(extractByPattern(fullText, "发票代码[：:\\s]*(\\d{10,12})"));
        result.setInvoiceNo(extractByPattern(fullText, "发票号码[：:\\s]*(\\d{8,20})"));
        result.setIssueDate(extractDate(fullText));
        result.setCheckCode(extractCheckCode(fullText));
        result.setMachineNo(extractMachineNo(textLines));
        extractBuyerInfoOptimized(textLines, result);
        extractSellerInfoOptimized(textLines, result);
        extractAmountInfoOptimized(textLines, result);
        extractGoodsInfoOptimized(textLines, result);
        log.info("[RapidOCR] 识别完成 - 发票代码: {}, 发票号码: {}, 机器编号: {}, 购买方: {}, 销售方: {}, 合计: {}, 税额: {}, 价税合计: {}, 货物名称: {}", result.getInvoiceCode(), result.getInvoiceNo(), result.getMachineNo(), result.getBuyerName(), result.getSellerName(), result.getAmountWithoutTax(), result.getTaxAmount(), result.getTotalAmount(), result.getGoodsName());
        return result;
    }

    private String extractMachineNo(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("机器编号")) {
                if (line.contains("：") || line.contains(":")) {
                    String machineNo = line.replaceAll(".*机器编号[：:]?\\s*", "").trim();
                    if (!machineNo.isEmpty() && machineNo.length() > 5) {
                        log.info("[RapidOCR] 机器编号: {}", machineNo);
                        return machineNo;
                    }
                }
                if (i + 1 < lines.size()) {
                    String nextLine = lines.get(i + 1).trim();
                    if (nextLine.matches("\\d{10,}")) {
                        log.info("[RapidOCR] 机器编号(下一行): {}", nextLine);
                        return nextLine;
                    }
                }
            }
            if (line.matches("\\d{12}") && i > 0 && lines.get(i - 1).contains("机器编号")) {
                log.info("[RapidOCR] 机器编号(纯数字行): {}", line);
                return line;
            }
        }
        return null;
    }

    private void extractBuyerInfoOptimized(List<String> lines, InvoiceOcrResultDTO result) {
        int buyerEndIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.equals("销") || (line.startsWith("销") && line.length() <= 3)) {
                buyerEndIndex = i;
                break;
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("名") && i + 1 < lines.size()) {
                String nextLine = lines.get(i + 1);
                if (nextLine.startsWith("称") || nextLine.contains("称：") || nextLine.contains("称:")) {
                    String nameValue = nextLine.replaceAll("称[：:]?\\s*", "").trim();
                    if (!nameValue.isEmpty() && !nameValue.matches(".*[\\*<>+/].*") && nameValue.length() > 1) {
                        result.setBuyerName(nameValue);
                        log.info("[RapidOCR] 购买方名称(竖排): {}", nameValue);
                        break;
                    }
                }
            }
            if (line.contains("称：") || line.contains("称:")) {
                String nameValue = line.replaceAll(".*称[：:]\\s*", "").trim();
                if (!nameValue.isEmpty() && !nameValue.matches(".*[\\*<>+/].*") && nameValue.length() > 1) {
                    result.setBuyerName(nameValue);
                    log.info("[RapidOCR] 购买方名称(横排): {}", nameValue);
                    break;
                }
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("纳税人识别号") && i + 1 < lines.size()) {
                String nextLine = lines.get(i + 1);
                String taxNo = extractTaxNo(nextLine);
                if (taxNo != null && !taxNo.contains("<") && !taxNo.contains(">")) {
                    result.setBuyerTaxNo(taxNo);
                    log.info("[RapidOCR] 购买方税号: {}", taxNo);
                    break;
                }
            }
            String taxNo = extractTaxNo(line);
            if (taxNo != null && !taxNo.contains("<") && !taxNo.contains(">") && line.contains("购")) {
                result.setBuyerTaxNo(taxNo);
                log.info("[RapidOCR] 购买方税号(同行): {}", taxNo);
                break;
            }
        }
        int buyerSectionEnd = buyerEndIndex > 0 ? buyerEndIndex : lines.size() / 2;
        for (int i = 0; i < buyerSectionEnd; i++) {
            String line = lines.get(i);
            if (line.contains("地址") && (line.contains("电话") || line.contains("：") || line.contains(":"))) {
                String addressPhone = line.replaceAll(".*地址[、，,：:]*\\s*", "").trim();
                addressPhone = addressPhone.replaceAll("^电话[：:]*\\s*", "").trim();
                if (addressPhone.length() > 5 && !addressPhone.matches(".*[\\*<>+/].*")) {
                    result.setBuyerAddressPhone(addressPhone);
                    log.info("[RapidOCR] 购买方地址电话: {}", addressPhone);
                    break;
                }
            }
        }
        for (int i = 0; i < buyerSectionEnd; i++) {
            String line = lines.get(i);
            if (line.contains("开户行") && (line.contains("账号") || line.contains("：") || line.contains(":"))) {
                String bankAccount = line.replaceAll(".*开户行[及和]?账号[：:]*\\s*", "").trim();
                if (bankAccount.length() > 5 && !bankAccount.matches(".*[\\*<>+/].*")) {
                    result.setBuyerBankAccount(bankAccount);
                    log.info("[RapidOCR] 购买方开户行账号: {}", bankAccount);
                    break;
                }
            }
        }
    }

    private void extractSellerInfoOptimized(List<String> lines, InvoiceOcrResultDTO result) {
        int sellerKeywordIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.equals("销") || (line.startsWith("销") && line.length() <= 3)) {
                sellerKeywordIndex = i;
                break;
            }
        }
        if (sellerKeywordIndex > 0) {
            for (int i = sellerKeywordIndex - 1; i >= Math.max(0, sellerKeywordIndex - 5); i--) {
                String line = lines.get(i);
                if (line.contains("称：") || line.contains("称:")) {
                    String nameValue = line.replaceAll(".*称[：:]\\s*", "").trim();
                    if (!nameValue.isEmpty() && nameValue.length() > 4 && !nameValue.matches(".*[\\*<>+/].*")) {
                        result.setSellerName(nameValue);
                        log.info("[RapidOCR] 销售方名称: {}", nameValue);
                        break;
                    }
                }
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("纳税人识别号") && line.contains("9") && line.length() > 20) {
                String taxNo = extractTaxNo(line);
                if (taxNo != null && taxNo.length() >= 18) {
                    result.setSellerTaxNo(taxNo);
                    log.info("[RapidOCR] 销售方税号: {}", taxNo);
                    break;
                }
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("地址") && (line.contains("电话") || line.contains("：") || line.contains(":"))) {
                String addressPhone = line.replaceAll(".*地址[、，,：:]*\\s*", "").trim();
                addressPhone = addressPhone.replaceAll("^电话[：:]*\\s*", "").trim();
                if (addressPhone.length() > 5) {
                    result.setSellerAddressPhone(addressPhone);
                    log.info("[RapidOCR] 销售方地址电话: {}", addressPhone);
                    break;
                }
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("开户行") && (line.contains("账号") || line.contains("：") || line.contains(":"))) {
                String bankAccount = line.replaceAll(".*开户行[及和]?账号[：:]*\\s*", "").trim();
                if (bankAccount.length() > 5) {
                    result.setSellerBankAccount(bankAccount);
                    log.info("[RapidOCR] 销售方开户行账号: {}", bankAccount);
                    break;
                }
            }
        }
    }

    private void extractAmountInfoOptimized(List<String> lines, InvoiceOcrResultDTO result) {
        BigDecimal amountWithoutTax = null;
        BigDecimal taxAmount = null;
        BigDecimal totalAmount = null;
        int hejiIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.equals("合") || line.equals("计")) {
                hejiIndex = i;
            }
        }
        if (hejiIndex > 0 && hejiIndex + 3 < lines.size()) {
            List<BigDecimal> amountsAfterHeji = new ArrayList<>();
            for (int i = hejiIndex + 1; i < Math.min(lines.size(), hejiIndex + 5); i++) {
                String line = lines.get(i);
                if (line.contains("￥") || line.contains("¥")) {
                    BigDecimal amt = extractMoney(line);
                    if (amt != null && amt.compareTo(BigDecimal.ZERO) > 0) {
                        amountsAfterHeji.add(amt);
                    }
                }
            }
            if (amountsAfterHeji.size() >= 2) {
                amountWithoutTax = amountsAfterHeji.get(0);
                taxAmount = amountsAfterHeji.get(1);
                log.info("[RapidOCR] 合计金额(合字后￥): {}", amountWithoutTax);
                log.info("[RapidOCR] 税额(合字后￥): {}", taxAmount);
            } else if (amountsAfterHeji.size() == 1) {
                amountWithoutTax = amountsAfterHeji.get(0);
                log.info("[RapidOCR] 合计金额(合字后￥): {}", amountWithoutTax);
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("价税合计") || line.contains("大写")) {
                if (i + 2 < lines.size()) {
                    String smallLine = lines.get(i + 2);
                    if (smallLine.contains("小写")) {
                        BigDecimal amt = extractMoney(smallLine);
                        if (amt != null && amt.compareTo(BigDecimal.ZERO) > 0) {
                            totalAmount = amt;
                            log.info("[RapidOCR] 价税合计: {}", amt);
                            break;
                        }
                    }
                }
                if (i + 1 < lines.size()) {
                    BigDecimal amt = extractMoney(lines.get(i + 1));
                    if (amt != null && amt.compareTo(BigDecimal.ZERO) > 0) {
                        totalAmount = amt;
                        log.info("[RapidOCR] 价税合计(大写后): {}", amt);
                        break;
                    }
                }
            }
        }
        if (amountWithoutTax != null) {
            result.setAmountWithoutTax(amountWithoutTax);
        }
        if (taxAmount != null) {
            result.setTaxAmount(taxAmount);
        }
        if (totalAmount != null) {
            result.setTotalAmount(totalAmount);
        }
        if (result.getTotalAmount() == null && result.getAmountWithoutTax() != null && result.getTaxAmount() != null) {
            result.setTotalAmount(result.getAmountWithoutTax().add(result.getTaxAmount()));
        }
    }

    private void extractGoodsInfoOptimized(List<String> lines, InvoiceOcrResultDTO result) {
        int goodsStartIndex = -1;
        int hejiStartIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("货物或应税劳务") || line.contains("服务名称") || line.contains("项目名称") || line.equals("货物名称") || line.equals("项目")) {
                goodsStartIndex = i;
            }
            if (line.equals("合") || line.equals("计") || line.equals("合计") || line.contains("价税合计")) {
                if (goodsStartIndex > 0 && hejiStartIndex < 0) {
                    hejiStartIndex = i;
                }
            }
        }
        if (goodsStartIndex < 0) {
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith("*") && line.contains("*") && !line.contains(">") && !line.contains("<")) {
                    goodsStartIndex = i;
                    break;
                }
            }
        }
        if (goodsStartIndex >= 0) {
            List<String> goodsSection = new ArrayList<>();
            int endIndex = hejiStartIndex > 0 ? hejiStartIndex : Math.min(lines.size(), goodsStartIndex + 20);
            for (int i = goodsStartIndex; i < endIndex; i++) {
                goodsSection.add(lines.get(i));
            }
            extractGoodsFromSectionV2(goodsSection, result);
        }
        extractPayeeInfo(lines, result);
    }

    private void extractPayeeInfo(List<String> lines, InvoiceOcrResultDTO result) {
        List<String> pendingNames = new ArrayList<>();
        boolean hasKaiPiaoRen = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.contains("收款人") || line.contains("收 款 人")) {
                String payee = extractValueAfterColon(line);
                if (payee != null && !payee.isEmpty()) {
                    result.setPayee(payee);
                    log.info("[RapidOCR] 收款人: {}", payee);
                }
            }
            if (line.contains("复核")) {
                String checker = extractValueAfterColon(line);
                if (checker != null && !checker.isEmpty()) {
                    result.setChecker(checker);
                    log.info("[RapidOCR] 复核: {}", checker);
                }
            }
            if (line.contains("开票人")) {
                hasKaiPiaoRen = true;
                String issuer = extractValueAfterColon(line);
                if (issuer != null && !issuer.isEmpty()) {
                    result.setIssuer(issuer);
                    log.info("[RapidOCR] 开票人: {}", issuer);
                }
            }
            if (line.matches("^[\\u4e00-\\u9fa5]{2,4}$") && !line.contains("收款") && !line.contains("复核") && !line.contains("开票")) {
                if (!isCommonNonNameWord(line)) {
                    pendingNames.add(line);
                    log.info("[RapidOCR] 待分配姓名: {}", line);
                }
            }
        }
        if (result.getPayee() == null || result.getChecker() == null || result.getIssuer() == null) {
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.matches("^[\\u4e00-\\u9fa5]{2,4}$") && !line.contains("收款") && !line.contains("复核") && !line.contains("开票")) {
                    if (i > 0) {
                        String prevLine = lines.get(i - 1).trim();
                        if ((prevLine.contains("收款人") || prevLine.contains("收 款 人")) && result.getPayee() == null) {
                            result.setPayee(line);
                            log.info("[RapidOCR] 收款人(下一行): {}", line);
                        } else if (prevLine.contains("复核") && result.getChecker() == null) {
                            result.setChecker(line);
                            log.info("[RapidOCR] 复核(下一行): {}", line);
                        } else if (prevLine.contains("开票人") && result.getIssuer() == null) {
                            result.setIssuer(line);
                            log.info("[RapidOCR] 开票人(下一行): {}", line);
                        }
                    }
                }
            }
        }
        if ((result.getPayee() == null || result.getChecker() == null || result.getIssuer() == null) && !pendingNames.isEmpty()) {
            List<String> unassigned = new ArrayList<>();
            for (String name : pendingNames) {
                if (!name.equals(result.getPayee()) && !name.equals(result.getChecker()) && !name.equals(result.getIssuer())) {
                    unassigned.add(name);
                }
            }
            if (hasKaiPiaoRen && result.getIssuer() == null && !unassigned.isEmpty()) {
                result.setIssuer(unassigned.get(unassigned.size() - 1));
                log.info("[RapidOCR] 开票人(最后一个未分配): {}", result.getIssuer());
                unassigned.remove(unassigned.size() - 1);
            }
            int nameIdx = 0;
            if (result.getPayee() == null && nameIdx < unassigned.size()) {
                result.setPayee(unassigned.get(nameIdx++));
                log.info("[RapidOCR] 收款人(按顺序): {}", result.getPayee());
            }
            if (result.getChecker() == null && nameIdx < unassigned.size()) {
                result.setChecker(unassigned.get(nameIdx++));
                log.info("[RapidOCR] 复核(按顺序): {}", result.getChecker());
            }
            if (result.getIssuer() == null && nameIdx < unassigned.size()) {
                result.setIssuer(unassigned.get(nameIdx++));
                log.info("[RapidOCR] 开票人(按顺序): {}", result.getIssuer());
            }
        }
    }

    private String extractValueAfterColon(String line) {
        String normalizedLine = line.replaceAll("\\s+", "");
        int colonIdx = normalizedLine.indexOf("：");
        if (colonIdx < 0) {
            colonIdx = normalizedLine.indexOf(":");
        }
        if (colonIdx >= 0) {
            String after = normalizedLine.substring(colonIdx + 1).trim();
            if (!after.isEmpty() && after.matches("^[\\u4e00-\\u9fa5]{2,4}$")) {
                return after;
            }
        }
        return null;
    }

    private boolean isCommonNonNameWord(String word) {
        String[] commonWords = {"单位", "名称", "地址", "电话", "账号", "开户", "银行", "税号", "合计", "小写", "大写", "金额", "税额", "税率", "数量", "单价", "规格", "型号", "备注", "日期", "发票", "代码", "号码", "校验", "机器", "编号", "购买", "销售", "纳税", "识别", "个人", "公司", "企业", "有限", "责任", "股份", "集团", "商店", "商场", "超市", "件", "个", "台", "套", "本", "张", "箱", "包", "千克", "公斤", "克", "吨", "米", "厘米", "毫米", "合计", "价税", "普通", "专用"};
        for (String common : commonWords) {
            if (word.equals(common) || word.contains(common)) {
                return true;
            }
        }
        return false;
    }

    private void extractGoodsFromSectionV2(List<String> goodsSection, InvoiceOcrResultDTO result) {
        List<InvoiceGoodsItemDTO> goodsItems = new ArrayList<>();
        log.info("[RapidOCR] 开始解析商品区域，共{}行", goodsSection.size());
        List<GoodsLineInfo> lineInfos = new ArrayList<>();
        GoodsLineInfo currentLine = null;
        List<BigDecimal> allNumbers = new ArrayList<>();
        List<BigDecimal> allNegativeNumbers = new ArrayList<>();
        List<BigDecimal> allTaxRates = new ArrayList<>();
        String lastUnit = null;
        for (int i = 0; i < goodsSection.size(); i++) {
            String line = goodsSection.get(i).trim();
            log.info("[RapidOCR] 商品区域第{}行: {}", i, line);
            if (line.equals("合") || line.equals("计") || line.equals("合计") || line.contains("价税合计") || line.contains("收款人") || line.contains("复核") || line.contains("开票人") || line.contains("货物或应税") || line.contains("服务名称") || line.contains("规格型号") || line.contains("单价") || line.contains("数量") || line.contains("金额") || line.contains("税率") || line.contains("税额")) {
                continue;
            }
            if (line.startsWith("*") && line.contains("*") && !line.contains(">") && !line.contains("<")) {
                if (currentLine != null && currentLine.hasGoodsName()) {
                    lineInfos.add(currentLine);
                }
                currentLine = new GoodsLineInfo();
                int firstStarIdx = line.indexOf("*");
                int lastStarIdx = line.lastIndexOf("*");
                if (firstStarIdx >= 0 && lastStarIdx > firstStarIdx) {
                    currentLine.goodsCode = line.substring(firstStarIdx + 1, lastStarIdx).trim();
                    String goodsName = line.substring(lastStarIdx + 1).trim();
                    if (!goodsName.isEmpty()) {
                        currentLine.goodsNameLines.add(goodsName);
                        log.info("[RapidOCR] 商品编码: {}, 名称: {}", currentLine.goodsCode, goodsName);
                    }
                }
                continue;
            }
            if (currentLine == null) {
                currentLine = new GoodsLineInfo();
            }
            if (line.matches("^-?\\d{1,3}%$")) {
                BigDecimal rate = new BigDecimal(line.replace("%", "")).divide(new BigDecimal("100"));
                allTaxRates.add(rate);
                log.info("[RapidOCR] 税率: {}%", line);
                continue;
            }
            if (line.matches("^-?\\d+\\.\\d{2,8}$")) {
                try {
                    BigDecimal num = new BigDecimal(line);
                    if (num.compareTo(BigDecimal.ZERO) < 0) {
                        allNegativeNumbers.add(num);
                        currentLine.hasNegativeNumber = true;
                    } else {
                        allNumbers.add(num);
                    }
                    currentLine.numbers.add(num);
                    log.info("[RapidOCR] 数值: {}", num);
                } catch (Exception ignored) {
                }
                continue;
            }
            if (line.matches("^￥?-?\\d+\\.\\d{2}$")) {
                try {
                    String numStr = line.replace("￥", "").replace("¥", "");
                    BigDecimal num = new BigDecimal(numStr);
                    if (num.compareTo(BigDecimal.ZERO) < 0) {
                        allNegativeNumbers.add(num);
                        currentLine.hasNegativeNumber = true;
                    } else {
                        allNumbers.add(num);
                    }
                    currentLine.numbers.add(num);
                    log.info("[RapidOCR] 金额: {}", num);
                } catch (Exception ignored) {
                }
                continue;
            }
            if (line.matches("^(件|个|台|套|本|张|箱|包|千克|公斤|克|吨|米|厘米|毫米|份|次)$")) {
                lastUnit = line;
                currentLine.unit = line;
                log.info("[RapidOCR] 单位: {}", line);
                continue;
            }
            if (line.matches("^\\d+$") && line.length() <= 4) {
                try {
                    BigDecimal num = new BigDecimal(line);
                    allNumbers.add(num);
                    currentLine.numbers.add(num);
                    log.info("[RapidOCR] 整数: {}", line);
                } catch (Exception ignored) {
                }
                continue;
            }
            if (line.matches(".*[gGkK][包装复印纸打印].*") || line.matches(".*\\d+[gGkK].*") || line.matches(".*[A-Z]\\d{1,2}.*") || line.contains("规格") || line.contains("型号") || (line.matches(".*\\d+.*") && line.length() < 20 && !line.matches("^-?\\d+\\.\\d+$"))) {
                currentLine.specLines.add(line);
                log.info("[RapidOCR] 规格行: {}", line);
                continue;
            }
            if (line.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9()（）\\-+]+$") && line.length() > 1 && line.length() < 60) {
                currentLine.goodsNameLines.add(line);
                log.info("[RapidOCR] 商品名称续行: {}", line);
            }
        }
        if (currentLine != null && currentLine.hasGoodsName()) {
            lineInfos.add(currentLine);
        }
        log.info("[RapidOCR] 收集到的数值: 正数={}, 负数={}, 税率={}", allNumbers, allNegativeNumbers, allTaxRates);
        BigDecimal amtWithoutTax = result.getAmountWithoutTax();
        BigDecimal taxAmt = result.getTaxAmount();
        BigDecimal unitPrice = null;
        BigDecimal quantity = null;
        BigDecimal goodsAmount = null;
        BigDecimal goodsTaxAmount = null;
        BigDecimal discountAmount = null;
        BigDecimal discountTaxAmount = null;
        for (BigDecimal num : allNumbers) {
            int scale = num.scale();
            if (scale >= 4 && num.compareTo(new BigDecimal("10000")) < 0 && unitPrice == null) {
                unitPrice = num;
                log.info("[RapidOCR] 识别单价(小数位多): {}", num);
            }
        }
        if (!allNegativeNumbers.isEmpty()) {
            discountAmount = allNegativeNumbers.get(0);
            log.info("[RapidOCR] 折扣金额: {}", discountAmount);
        }
        if (allNegativeNumbers.size() > 1) {
            discountTaxAmount = allNegativeNumbers.get(1);
            log.info("[RapidOCR] 折扣税额: {}", discountTaxAmount);
        }
        if (amtWithoutTax != null) {
            if (discountAmount != null) {
                goodsAmount = amtWithoutTax.subtract(discountAmount);
                log.info("[RapidOCR] 计算商品金额: {} - {} = {}", amtWithoutTax, discountAmount, goodsAmount);
            } else {
                goodsAmount = amtWithoutTax;
                log.info("[RapidOCR] 使用发票金额作为商品金额: {}", goodsAmount);
            }
        }
        if (taxAmt != null) {
            if (discountTaxAmount != null) {
                goodsTaxAmount = taxAmt.subtract(discountTaxAmount);
                log.info("[RapidOCR] 计算商品税额: {} - {} = {}", taxAmt, discountTaxAmount, goodsTaxAmount);
            } else {
                goodsTaxAmount = taxAmt;
                log.info("[RapidOCR] 使用发票税额作为商品税额: {}", goodsTaxAmount);
            }
        }
        if (unitPrice != null && goodsAmount != null && unitPrice.compareTo(BigDecimal.ZERO) > 0) {
            quantity = goodsAmount.divide(unitPrice, 4, RoundingMode.HALF_UP);
            if (quantity.compareTo(new BigDecimal("100")) > 0) {
                log.info("[RapidOCR] 计算数量过大({}), 重新识别", quantity);
                quantity = null;
            } else {
                log.info("[RapidOCR] 计算数量: {} / {} = {}", goodsAmount, unitPrice, quantity);
            }
        }
        if (quantity == null) {
            for (BigDecimal num : allNumbers) {
                if (num != unitPrice && num.stripTrailingZeros().scale() <= 2 && num.compareTo(BigDecimal.ONE) >= 0 && num.compareTo(new BigDecimal("100")) < 0 && (goodsAmount == null || num.compareTo(goodsAmount) != 0) && (goodsTaxAmount == null || num.compareTo(goodsTaxAmount) != 0) && (taxAmt == null || num.compareTo(taxAmt) != 0)) {
                    quantity = num;
                    log.info("[RapidOCR] 识别数量: {}", num);
                    break;
                }
            }
        }
        if (quantity == null) {
            quantity = BigDecimal.ONE;
            log.info("[RapidOCR] 默认数量: 1");
        }
        if (unitPrice == null && goodsAmount != null && quantity != null) {
            unitPrice = goodsAmount.divide(quantity, 4, RoundingMode.HALF_UP);
            log.info("[RapidOCR] 计算单价: {} / {} = {}", goodsAmount, quantity, unitPrice);
        }
        BigDecimal taxRate = allTaxRates.isEmpty() ? new BigDecimal("0.13") : allTaxRates.get(0);
        int itemNo = 1;
        for (GoodsLineInfo info : lineInfos) {
            StringBuilder nameBuilder = new StringBuilder();
            for (String line : info.goodsNameLines) {
                if (nameBuilder.length() > 0) {
                    nameBuilder.append(" ");
                }
                nameBuilder.append(line);
            }
            StringBuilder specBuilder = new StringBuilder();
            Set<String> uniqueSpecs = new LinkedHashSet<>(info.specLines);
            for (String spec : uniqueSpecs) {
                if (specBuilder.length() > 0) {
                    specBuilder.append(" ");
                }
                specBuilder.append(spec);
            }
            InvoiceGoodsItemDTO item = new InvoiceGoodsItemDTO();
            item.setItemNo(itemNo++);
            item.setGoodsName(nameBuilder.length() > 0 ? nameBuilder.toString().trim() : null);
            item.setSpecification(specBuilder.length() > 0 ? specBuilder.toString().trim() : null);
            item.setUnit(info.unit != null ? info.unit : lastUnit);
            item.setQuantity(quantity != null ? quantity : BigDecimal.ONE);
            item.setUnitPrice(unitPrice);
            item.setTaxRate(taxRate);
            if (info.hasNegativeNumber) {
                item.setAmount(discountAmount);
                item.setTaxAmount(discountTaxAmount);
                item.setDiscount(true);
            } else {
                item.setAmount(goodsAmount);
                item.setTaxAmount(goodsTaxAmount);
                item.setDiscount(false);
            }
            item.setGoodsCode(info.goodsCode);
            goodsItems.add(item);
            log.info("[RapidOCR] 构建商品项: itemNo={}, name={}, discount={}, amount={}", item.getItemNo(), item.getGoodsName(), item.isDiscount(), item.getAmount());
        }
        result.setGoodsItems(goodsItems);
        if (!goodsItems.isEmpty()) {
            StringBuilder allGoodsName = new StringBuilder();
            for (InvoiceGoodsItemDTO item : goodsItems) {
                if (!item.isDiscount() && item.getGoodsName() != null) {
                    if (allGoodsName.length() > 0) {
                        allGoodsName.append("; ");
                    }
                    allGoodsName.append(item.getGoodsName());
                }
            }
            result.setGoodsName(allGoodsName.toString());
            InvoiceGoodsItemDTO firstNonDiscountItem = null;
            for (InvoiceGoodsItemDTO item : goodsItems) {
                if (!item.isDiscount()) {
                    firstNonDiscountItem = item;
                    break;
                }
            }
            if (firstNonDiscountItem != null) {
                if (firstNonDiscountItem.getSpecification() != null) {
                    result.setGoodsSpec(firstNonDiscountItem.getSpecification());
                }
                if (firstNonDiscountItem.getUnit() != null) {
                    result.setGoodsUnit(firstNonDiscountItem.getUnit());
                }
                if (firstNonDiscountItem.getQuantity() != null) {
                    result.setGoodsQuantity(firstNonDiscountItem.getQuantity().stripTrailingZeros().toPlainString());
                }
                if (firstNonDiscountItem.getUnitPrice() != null) {
                    result.setGoodsPrice(firstNonDiscountItem.getUnitPrice().stripTrailingZeros().toPlainString());
                }
                if (firstNonDiscountItem.getTaxRate() != null) {
                    result.setTaxRate(firstNonDiscountItem.getTaxRate());
                }
            }
        }
        log.info("[RapidOCR] 解析完成，共{}个商品行", goodsItems.size());
    }


    private static class GoodsLineInfo {
        List<String> goodsNameLines = new ArrayList<>();
        List<String> specLines = new ArrayList<>();
        List<BigDecimal> numbers = new ArrayList<>();
        String unit;
        BigDecimal taxRate;
        String goodsCode;
        boolean hasNegativeNumber;

        boolean hasGoodsName() {
            return !goodsNameLines.isEmpty();
        }
    }

    private String extractPersonName(String line, String fieldName) {
        String[] patterns = {fieldName + "[：:]\\s*(\\S+)", fieldName + "[：:]\\s*(.+?)(?:\\s|$)", fieldName + "[：:](.+)"};
        for (String pattern : patterns) {
            try {
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(line);
                if (m.find()) {
                    String name = m.group(1).trim();
                    if (!name.isEmpty() && name.length() < 20 && !name.matches(".*[\\d\\*<>+/].*")) {
                        return name;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String extractPersonNameFromMultipleLines(String line1, String line2, String line3, String fieldName) {
        for (String line : new String[] {line1, line2, line3}) {
            if (line.contains(fieldName)) {
                String name = extractPersonName(line, fieldName);
                if (name != null) {
                    return name;
                }
                int idx = line.indexOf(fieldName);
                if (idx >= 0) {
                    String after = line.substring(idx + fieldName.length()).trim();
                    after = after.replaceAll("^[：:]+", "").trim();
                    if (!after.isEmpty() && after.length() < 20 && !after.matches(".*[\\d\\*<>+/].*")) {
                        return after;
                    }
                }
            }
        }
        return null;
    }

    private String extractTaxNo(String text) {
        Pattern p = Pattern.compile("([A-Za-z0-9]{15,20})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).toUpperCase();
        }
        return null;
    }

    private BigDecimal extractMoney(String text) {
        Pattern p = Pattern.compile("[￥¥]?\\s*([\\d,]+\\.\\d{2})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return new BigDecimal(m.group(1).replace(",", ""));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String extractCheckCode(String text) {
        Pattern p = Pattern.compile("校验码[：:\\s]*([\\d\\s]{10,30})");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).replaceAll("\\s+", "");
        }
        return null;
    }

    private String extractByPattern(String text, String pattern) {
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            if (m.find()) {
                return m.group(1).trim();
            }
        } catch (Exception e) {
            log.debug("[RapidOCR] 模式匹配失败: {}", pattern);
        }
        return null;
    }

    private String extractDate(String text) {
        String[] patterns = {"开票日期[：:\\s]*(\\d{4}年\\d{1,2}月\\d{1,2}日)", "开票日期[：:\\s]*(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2})", "日期[：:\\s]*(\\d{4}年\\d{1,2}月\\d{1,2}日)"};
        for (String pattern : patterns) {
            try {
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(text);
                if (m.find()) {
                    String date = m.group(1);
                    return date.replace("年", "-").replace("月", "-").replace("日", "").replace("/", "-");
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
