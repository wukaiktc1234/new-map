package com.foodtraceability.service;

import com.foodtraceability.dto.InvoiceOcrResultDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OCR引擎管理器
 * 
 * 负责协调多个OCR引擎，实现智能选择和降级策略
 */
@Service
public class OcrEngineManager {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OcrEngineManager.class);
    @Value("${ocr.primary-engine:rapid}")
    private String primaryEngine;
    @Value("${ocr.fallback-enabled:true}")
    private boolean fallbackEnabled;
    @Value("${ocr.paddle.enabled:false}")
    private boolean paddleEnabled;
    private final Map<String, InvoiceOcrService> ocrServices = new ConcurrentHashMap<>();

    public OcrEngineManager(List<InvoiceOcrService> services) {
        for (InvoiceOcrService service : services) {
            ocrServices.put(service.getEngineName(), service);
            log.info("[OcrEngineManager] 注册OCR引擎: {}", service.getEngineName());
        }
    }

    /**
     * 从PDF识别发票信息
     * 使用主引擎识别，失败时自动降级到备用引擎
     */
    public InvoiceOcrResultDTO recognizeFromPdf(byte[] pdfContent) {
        return recognizeWithFallback(() -> {
            InvoiceOcrService primary = getPrimaryService();
            if (primary != null && primary.isAvailable()) {
                return primary.recognizeFromPdf(pdfContent);
            }
            return null;
        }, "PDF识别");
    }

    /**
     * 从图片识别发票信息
     */
    public InvoiceOcrResultDTO recognizeFromImage(BufferedImage image) {
        return recognizeWithFallback(() -> {
            InvoiceOcrService primary = getPrimaryService();
            if (primary != null && primary.isAvailable()) {
                return primary.recognizeFromImage(image);
            }
            return null;
        }, "图片识别");
    }

    /**
     * 从图片字节数组识别发票信息
     */
    public InvoiceOcrResultDTO recognizeFromImageBytes(byte[] imageData, String format) {
        return recognizeWithFallback(() -> {
            InvoiceOcrService primary = getPrimaryService();
            if (primary != null && primary.isAvailable()) {
                return primary.recognizeFromImageBytes(imageData, format);
            }
            return null;
        }, "图片字节识别");
    }

    /**
     * 使用指定引擎识别
     */
    public InvoiceOcrResultDTO recognizeWithEngine(String engineName, byte[] imageData, String format) {
        InvoiceOcrService service = ocrServices.get(engineName);
        if (service == null) {
            log.warn("[OcrEngineManager] 未找到引擎: {}", engineName);
            return InvoiceOcrResultDTO.builder().success(false).errorMessage("未找到OCR引擎: " + engineName).build();
        }
        if (!service.isAvailable()) {
            log.warn("[OcrEngineManager] 引擎不可用: {}", engineName);
            return InvoiceOcrResultDTO.builder().success(false).errorMessage("OCR引擎不可用: " + engineName).engineName(engineName).build();
        }
        return service.recognizeFromImageBytes(imageData, format);
    }

    /**
     * 双引擎识别并比较结果
     * 返回置信度更高的结果
     */
    public InvoiceOcrResultDTO recognizeWithBothEngines(byte[] imageData, String format) {
        InvoiceOcrResultDTO rapidResult = null;
        InvoiceOcrResultDTO paddleResult = null;
        InvoiceOcrService rapidService = ocrServices.get("RapidOCR");
        if (rapidService != null && rapidService.isAvailable()) {
            try {
                rapidResult = rapidService.recognizeFromImageBytes(imageData, format);
                log.info("[OcrEngineManager] RapidOCR识别完成: success={}", rapidResult.isSuccess());
            } catch (Exception e) {
                log.error("[OcrEngineManager] RapidOCR识别失败: {}", e.getMessage());
            }
        }
        InvoiceOcrService paddleService = ocrServices.get("PaddleOCR-VL");
        if (paddleService != null && paddleService.isAvailable()) {
            try {
                paddleResult = paddleService.recognizeFromImageBytes(imageData, format);
                log.info("[OcrEngineManager] PaddleOCR-VL识别完成: success={}", paddleResult.isSuccess());
            } catch (Exception e) {
                log.error("[OcrEngineManager] PaddleOCR-VL识别失败: {}", e.getMessage());
            }
        }
        return selectBestResult(rapidResult, paddleResult);
    }

    /**
     * 选择最佳识别结果
     */
    private InvoiceOcrResultDTO selectBestResult(InvoiceOcrResultDTO rapidResult, InvoiceOcrResultDTO paddleResult) {
        if (rapidResult == null && paddleResult == null) {
            return InvoiceOcrResultDTO.builder().success(false).errorMessage("所有OCR引擎都不可用").build();
        }
        if (rapidResult == null) return paddleResult;
        if (paddleResult == null) return rapidResult;
        if (!rapidResult.isSuccess() && !paddleResult.isSuccess()) {
            return rapidResult;
        }
        if (!rapidResult.isSuccess()) return paddleResult;
        if (!paddleResult.isSuccess()) return rapidResult;
        int rapidScore = calculateResultScore(rapidResult);
        int paddleScore = calculateResultScore(paddleResult);
        log.info("[OcrEngineManager] 结果评分 - RapidOCR: {}, PaddleOCR-VL: {}", rapidScore, paddleScore);
        if (paddleScore > rapidScore) {
            log.info("[OcrEngineManager] 选择PaddleOCR-VL结果");
            return paddleResult;
        } else {
            log.info("[OcrEngineManager] 选择RapidOCR结果");
            return rapidResult;
        }
    }

    /**
     * 计算识别结果评分
     * 评分越高表示结果越完整
     */
    private int calculateResultScore(InvoiceOcrResultDTO result) {
        int score = 0;
        if (result.getInvoiceNo() != null) score += 10;
        if (result.getInvoiceCode() != null) score += 10;
        if (result.getIssueDate() != null) score += 10;
        if (result.getSellerName() != null) score += 15;
        if (result.getSellerTaxNo() != null) score += 10;
        if (result.getBuyerName() != null) score += 10;
        if (result.getTotalAmount() != null) score += 15;
        if (result.getAmountWithoutTax() != null) score += 10;
        if (result.getTaxAmount() != null) score += 10;
        if (result.getGoodsItems() != null && !result.getGoodsItems().isEmpty()) {
            score += 20;
            score += result.getGoodsItems().size() * 5;
        }
        if (result.getMachineNo() != null) score += 5;
        if (result.getPayee() != null) score += 5;
        if (result.getChecker() != null) score += 5;
        if (result.getIssuer() != null) score += 5;
        return score;
    }

    /**
     * 带降级策略的识别
     */
    private InvoiceOcrResultDTO recognizeWithFallback(RecognitionTask task, String taskName) {
        InvoiceOcrResultDTO result = null;
        try {
            result = task.execute();
            if (result != null && result.isSuccess()) {
                return result;
            }
            log.warn("[OcrEngineManager] {}主引擎失败，尝试降级", taskName);
        } catch (Exception e) {
            log.error("[OcrEngineManager] {}主引擎异常: {}", taskName, e.getMessage());
        }
        if (fallbackEnabled) {
            for (Map.Entry<String, InvoiceOcrService> entry : ocrServices.entrySet()) {
                if (entry.getKey().equals(primaryEngine)) continue;
                InvoiceOcrService fallbackService = entry.getValue();
                if (fallbackService.isAvailable()) {
                    log.info("[OcrEngineManager] 使用备用引擎: {}", fallbackService.getEngineName());
                    try {
                        result = task.executeWithFallback(fallbackService);
                        if (result != null && result.isSuccess()) {
                            return result;
                        }
                    } catch (Exception e) {
                        log.error("[OcrEngineManager] 备用引擎{}失败: {}", fallbackService.getEngineName(), e.getMessage());
                    }
                }
            }
        }
        if (result != null) {
            return result;
        }
        return InvoiceOcrResultDTO.builder().success(false).errorMessage("所有OCR引擎都无法完成识别").build();
    }

    private InvoiceOcrService getPrimaryService() {
        return ocrServices.get(primaryEngine);
    }

    /**
     * 获取所有可用的引擎名称
     */
    public java.util.Set<String> getAvailableEngines() {
        java.util.Set<String> available = new java.util.HashSet<>();
        for (Map.Entry<String, InvoiceOcrService> entry : ocrServices.entrySet()) {
            if (entry.getValue().isAvailable()) {
                available.add(entry.getKey());
            }
        }
        return available;
    }

    /**
     * 获取引擎状态
     */
    public Map<String, Boolean> getEngineStatus() {
        Map<String, Boolean> status = new ConcurrentHashMap<>();
        for (Map.Entry<String, InvoiceOcrService> entry : ocrServices.entrySet()) {
            status.put(entry.getKey(), entry.getValue().isAvailable());
        }
        return status;
    }


    @FunctionalInterface
    private interface RecognitionTask {
        InvoiceOcrResultDTO execute();

        default InvoiceOcrResultDTO executeWithFallback(InvoiceOcrService fallbackService) {
            return execute();
        }
    }
}
