package com.foodtraceability.service.impl;

import com.foodtraceability.dto.InvoiceOcrResultDTO;
import com.foodtraceability.dto.InvoiceGoodsItemDTO;
import com.foodtraceability.service.InvoiceOcrService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * RapidOCR 发票识别服务实现（优化版）
 * 
 * 特性：
 * 1. 异步处理支持
 * 2. 连接池复用
 * 3. 结果缓存
 * 4. 批量处理支持
 * 5. 健康监控
 */
@Service
@ConditionalOnProperty(name = "ocr.paddle.enabled", havingValue = "true", matchIfMissing = false)
public class PaddleOcrServiceImpl implements InvoiceOcrService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaddleOcrServiceImpl.class);
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final int DEFAULT_READ_TIMEOUT = 120000;
    private static final int MAX_RETRY_COUNT = 3;
    private static final long RETRY_DELAY_MS = 1000;
    @Value("${ocr.paddle.base-url:http://localhost:8110}")
    private String baseUrl;
    @Value("${ocr.paddle.timeout:120000}")
    private int timeout;
    @Value("${ocr.paddle.max-file-size:52428800}")
    private long maxFileSize;
    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public PaddleOcrServiceImpl() {
        this.restTemplate = createRestTemplate();
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        factory.setReadTimeout(DEFAULT_READ_TIMEOUT);
        return new RestTemplate(factory);
    }

    @Override
    public InvoiceOcrResultDTO recognizeFromPdf(byte[] pdfContent) {
        log.info("[OCR] 开始识别PDF，大小: {} bytes", pdfContent.length);
        return recognizeWithRetry(pdfContent, true);
    }

    @Override
    public InvoiceOcrResultDTO recognizeFromImage(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return recognizeFromImageBytes(imageBytes, "png");
        } catch (Exception e) {
            log.error("[OCR] 图片转换失败: {}", e.getMessage());
            return InvoiceOcrResultDTO.builder().success(false).errorMessage("图片转换失败: " + e.getMessage()).engineName(getEngineName()).build();
        }
    }

    @Override
    public InvoiceOcrResultDTO recognizeFromImageBytes(byte[] imageData, String format) {
        log.info("[OCR] 开始识别图片，格式: {}, 大小: {} bytes", format, imageData.length);
        return recognizeWithRetry(imageData, false);
    }

    private InvoiceOcrResultDTO recognizeWithRetry(byte[] data, boolean isPdf) {
        int retryCount = 0;
        Exception lastException = null;
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                return doRecognize(data, isPdf);
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                log.warn("[OCR] 识别失败，第{}次重试: {}", retryCount, e.getMessage());
                if (retryCount < MAX_RETRY_COUNT) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("[OCR] 识别失败，已重试{}次: {}", MAX_RETRY_COUNT, lastException != null ? lastException.getMessage() : "未知错误");
        return InvoiceOcrResultDTO.builder().success(false).errorMessage("识别失败: " + (lastException != null ? lastException.getMessage() : "未知错误")).engineName(getEngineName()).build();
    }

    private InvoiceOcrResultDTO doRecognize(byte[] data, boolean isPdf) {
        long startTime = System.currentTimeMillis();
        if (data.length > maxFileSize) {
            return InvoiceOcrResultDTO.builder().success(false).errorMessage("文件大小超过限制: " + (maxFileSize / 1024 / 1024) + "MB").engineName(getEngineName()).build();
        }
        try {
            String base64Data = Base64.getEncoder().encodeToString(data);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setConnection("keep-alive");
            String requestBody = "image=" + java.net.URLEncoder.encode(base64Data, "UTF-8");
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            String endpoint = "/ocr/invoice";
            log.debug("[OCR] 发送请求到: {}{}", baseUrl, endpoint);
            ResponseEntity<String> response = restTemplate.exchange(baseUrl + endpoint, HttpMethod.POST, entity, String.class);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[OCR] 请求完成，耗时: {}ms", elapsed);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                InvoiceOcrResultDTO result = parseInvoiceResponse(response.getBody());
                if (result != null) {
                    result.setProcessingTimeMs((double) elapsed);
                }
                return result;
            } else {
                return InvoiceOcrResultDTO.builder().success(false).errorMessage("API请求失败: " + response.getStatusCode()).engineName(getEngineName()).build();
            }
        } catch (Exception e) {
            log.error("[OCR] 识别异常: {}", e.getMessage(), e);
            throw new RuntimeException("识别异常: " + e.getMessage(), e);
        }
    }

    public CompletableFuture<InvoiceOcrResultDTO> recognizeAsync(byte[] data, boolean isPdf) {
        return CompletableFuture.supplyAsync(() -> recognizeWithRetry(data, isPdf), executorService);
    }

    public List<InvoiceOcrResultDTO> recognizeBatch(List<byte[]> dataList) {
        List<CompletableFuture<InvoiceOcrResultDTO>> futures = new ArrayList<>();
        for (int i = 0; i < dataList.size(); i++) {
            byte[] data = dataList.get(i);
            boolean isPdf = data.length > 4 && new String(data, 0, 4).equals("%PDF");
            futures.add(recognizeAsync(data, isPdf));
        }
        List<InvoiceOcrResultDTO> results = new ArrayList<>();
        for (CompletableFuture<InvoiceOcrResultDTO> future : futures) {
            try {
                results.add(future.get(timeout, TimeUnit.MILLISECONDS));
            } catch (Exception e) {
                results.add(InvoiceOcrResultDTO.builder().success(false).errorMessage("批量处理失败: " + e.getMessage()).engineName(getEngineName()).build());
            }
        }
        return results;
    }

    private InvoiceOcrResultDTO parseInvoiceResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            boolean success = root.path("success").asBoolean(false);
            if (!success) {
                return InvoiceOcrResultDTO.builder().success(false).errorMessage("OCR识别失败").engineName(getEngineName()).rawText(getJsonText(root, "rawText")).build();
            }
            InvoiceOcrResultDTO result = InvoiceOcrResultDTO.builder().success(true).engineName(getEngineName()).rawText(getJsonText(root, "rawText")).invoiceCode(getJsonText(root, "invoiceCode")).invoiceNo(getJsonText(root, "invoiceNumber")).issueDate(getJsonText(root, "invoiceDate")).buyerName(getJsonText(root, "buyerName")).buyerTaxNo(getJsonText(root, "buyerTaxId")).sellerName(getJsonText(root, "sellerName")).sellerTaxNo(getJsonText(root, "sellerTaxId")).machineNo(getJsonText(root, "machineNumber")).payee(getJsonText(root, "payee")).checker(getJsonText(root, "checker")).issuer(getJsonText(root, "issuer")).build();
            if (root.has("totalAmount")) {
                result.setAmountWithoutTax(parseBigDecimal(root.get("totalAmount")));
            }
            if (root.has("totalTax")) {
                result.setTaxAmount(parseBigDecimal(root.get("totalTax")));
            }
            if (root.has("amountWithTax")) {
                result.setTotalAmount(parseBigDecimal(root.get("amountWithTax")));
            }
            if (root.has("goodsItems") && root.get("goodsItems").isArray()) {
                List<InvoiceGoodsItemDTO> goodsItems = new ArrayList<>();
                int itemNo = 1;
                for (JsonNode item : root.get("goodsItems")) {
                    InvoiceGoodsItemDTO goodsItem = InvoiceGoodsItemDTO.builder().itemNo(itemNo++).goodsName(getJsonText(item, "goodsName")).specification(getJsonText(item, "specification")).unit(getJsonText(item, "unit")).quantity(parseBigDecimal(item.get("quantity"))).unitPrice(parseBigDecimal(item.get("unitPrice"))).amount(parseBigDecimal(item.get("amount"))).taxRate(parseBigDecimal(item.get("taxRate"))).taxAmount(parseBigDecimal(item.get("taxAmount"))).discount(item.has("discount") && item.get("discount").asBoolean()).build();
                    goodsItems.add(goodsItem);
                }
                result.setGoodsItems(goodsItems);
                if (!goodsItems.isEmpty()) {
                    InvoiceGoodsItemDTO firstItem = goodsItems.get(0);
                    result.setGoodsName(firstItem.getGoodsName());
                    result.setGoodsSpec(firstItem.getSpecification());
                    result.setGoodsUnit(firstItem.getUnit());
                    if (firstItem.getQuantity() != null) {
                        result.setGoodsQuantity(firstItem.getQuantity().stripTrailingZeros().toPlainString());
                    }
                    if (firstItem.getUnitPrice() != null) {
                        result.setGoodsPrice(firstItem.getUnitPrice().stripTrailingZeros().toPlainString());
                    }
                    if (firstItem.getTaxRate() != null) {
                        result.setTaxRate(firstItem.getTaxRate());
                    }
                }
            }
            if (root.has("confidence")) {
                result.setConfidence(root.get("confidence").asDouble());
            }
            if (root.has("processingTime")) {
                result.setProcessingTimeMs(root.get("processingTime").asDouble());
            }
            log.info("[OCR] 解析成功: invoiceNo={}, sellerName={}, totalAmount={}, confidence={}, time={}ms", result.getInvoiceNo(), result.getSellerName(), result.getTotalAmount(), result.getConfidence(), result.getProcessingTimeMs());
            return result;
        } catch (Exception e) {
            log.error("[OCR] 解析响应失败: {}", e.getMessage());
            return InvoiceOcrResultDTO.builder().success(false).errorMessage("解析响应失败: " + e.getMessage()).engineName(getEngineName()).build();
        }
    }

    private String getJsonText(JsonNode json, String field) {
        if (json == null || !json.has(field)) return null;
        String value = json.get(field).asText();
        return (value == null || "null".equals(value) || value.isEmpty()) ? null : value;
    }

    private BigDecimal parseBigDecimal(JsonNode node) {
        if (node == null || node.isNull()) return null;
        try {
            String text = node.asText();
            if (text == null || text.isEmpty() || "null".equals(text)) return null;
            text = text.replace("%", "").replace("￥", "").replace("¥", "").replace(",", "").trim();
            return new BigDecimal(text);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/health", String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.debug("[OCR] 服务不可用: {}", e.getMessage());
            return false;
        }
    }

    public OcrServiceStats getServiceStats() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/stats", String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return new OcrServiceStats(root.path("totalRequests").asInt(), root.path("successfulRequests").asInt(), root.path("failedRequests").asInt(), root.path("avgProcessingTime").asDouble(), root.path("cacheHitRate").asDouble(), root.path("engineInitialized").asBoolean());
            }
        } catch (Exception e) {
            log.warn("[OCR] 获取服务统计失败: {}", e.getMessage());
        }
        return new OcrServiceStats(0, 0, 0, 0, 0, false);
    }

    @Override
    public String getEngineName() {
        return "RapidOCR";
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    public static class OcrServiceStats {
        private final int totalRequests;
        private final int successfulRequests;
        private final int failedRequests;
        private final double avgProcessingTime;
        private final double cacheHitRate;
        private final boolean engineInitialized;

        public OcrServiceStats(int totalRequests, int successfulRequests, int failedRequests, double avgProcessingTime, double cacheHitRate, boolean engineInitialized) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.failedRequests = failedRequests;
            this.avgProcessingTime = avgProcessingTime;
            this.cacheHitRate = cacheHitRate;
            this.engineInitialized = engineInitialized;
        }

        public int getTotalRequests() {
            return totalRequests;
        }

        public int getSuccessfulRequests() {
            return successfulRequests;
        }

        public int getFailedRequests() {
            return failedRequests;
        }

        public double getAvgProcessingTime() {
            return avgProcessingTime;
        }

        public double getCacheHitRate() {
            return cacheHitRate;
        }

        public boolean isEngineInitialized() {
            return engineInitialized;
        }
    }
}
