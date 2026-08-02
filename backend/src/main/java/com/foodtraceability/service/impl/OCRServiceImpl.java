package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.OCRResultDTO;
import com.foodtraceability.entity.MaterialTemplate;
import com.foodtraceability.mapper.MaterialTemplateMapper;
import com.foodtraceability.service.OCRService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR识别服务实现
 * 支持本地OCR服务和智谱AI两种识别方式
 */
@Service
public class OCRServiceImpl implements OCRService {
    
    private static final Logger log = LoggerFactory.getLogger(OCRServiceImpl.class);
    
    private static final String ZHIPU_API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    

    public OCRServiceImpl(MaterialTemplateMapper materialTemplateMapper, @Nullable @Qualifier("ocrRestTemplate") RestTemplate restTemplate) {
        this.materialTemplateMapper = materialTemplateMapper;
        this.restTemplate = restTemplate;
    }

    /** 允许的私有IP地址范围 - 用于SSRF防护 */
    private static final List<String> BLOCKED_IP_PREFIXES = Arrays.asList(
        "127.", "0.", "10.", "192.168.", "172.16.", "172.17.", "172.18.", "172.19.",
        "172.20.", "172.21.", "172.22.", "172.23.", "172.24.", "172.25.", "172.26.",
        "172.27.", "172.28.", "172.29.", "172.30.", "172.31.", "169.254.", "localhost"
    );
    
    @Value("${ocr.service.url:http://localhost:9000/ocr/predict}")
    private String ocrServiceUrl;
    
    @Value("${ocr.service.enabled:true}")
    private boolean ocrEnabled;
    
    @Value("${ocr.zhipu.api-key:}")
    private String zhipuApiKey;
    
    @Value("${ocr.zhipu.model:glm-4v-flash}")
    private String zhipuModel;
    
    @Value("${ocr.service.allow-private-ip:false}")
    private boolean allowPrivateIp;
    
    @Value("${ocr.cache.enabled:true}")
    private boolean cacheEnabled;
    
    @Value("${ocr.cache.ttl-minutes:30}")
    private int cacheTtlMinutes;
    
    private static final String CACHE_KEY_PREFIX = "ocr:result:";
    
    private final MaterialTemplateMapper materialTemplateMapper;

    @Nullable
    private final RestTemplate restTemplate;

    /** OCR 结果本地缓存（带 TTL，替代 TwoLevelCacheManager） */
    private final ConcurrentHashMap<String, OcrCacheEntry> resultCache = new ConcurrentHashMap<>();

    /**
     * OCR 缓存条目，存储结果和过期时间（替代 TwoLevelCacheManager）
     */
    private static final class OcrCacheEntry {
        final OCRResultDTO value;
        final long expireAt;

        OcrCacheEntry(OCRResultDTO value, long ttlMinutes) {
            this.value = value;
            this.expireAt = System.currentTimeMillis() + ttlMinutes * 60 * 1000L;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OCRResultDTO recognizeImage(String imageBase64) {
        log.info("=== OCR识别请求 ===");
        log.debug("restTemplate: {}", restTemplate != null ? "存在" : "不存在");
        log.debug("zhipuApiKey: {}", StringUtils.hasText(zhipuApiKey) ? "已配置" : "未配置");
        
        // 参数验证
        if (!StringUtils.hasText(imageBase64)) {
            log.warn("图片数据为空");
            return createErrorResult("图片数据不能为空");
        }
        
        // 尝试从本地缓存获取结果
        if (cacheEnabled) {
            String cacheKey = generateCacheKey(imageBase64);
            OcrCacheEntry cached = resultCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                log.info("OCR缓存命中: {}", cacheKey.substring(0, Math.min(20, cacheKey.length())));
                return cached.value;
            }
            if (cached != null) {
                resultCache.remove(cacheKey, cached);
            }
        }
        
        // 执行OCR识别
        OCRResultDTO result = doRecognize(imageBase64);
        
        // 缓存成功结果
        if (cacheEnabled && result != null && result.getSuccess()) {
            String cacheKey = generateCacheKey(imageBase64);
            resultCache.put(cacheKey, new OcrCacheEntry(result, cacheTtlMinutes));
            log.debug("OCR结果已缓存: {}", cacheKey.substring(0, Math.min(20, cacheKey.length())));
        }
        
        return result;
    }
    
    /**
     * 生成缓存键
     * @param imageBase64 Base64编码的图片
     * @return 缓存键
     */
    private String generateCacheKey(String imageBase64) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(imageBase64.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(CACHE_KEY_PREFIX);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5不可用时，使用简单哈希
            return CACHE_KEY_PREFIX + imageBase64.hashCode();
        }
    }
    
    /**
     * 执行OCR识别
     * @param imageBase64 Base64编码的图片
     * @return 识别结果
     */
    private OCRResultDTO doRecognize(String imageBase64) {
        // 尝试调用本地OCR服务
        if (restTemplate != null && validateOcrServiceUrl()) {
            try {
                log.info("调用本地OCR服务: {}", ocrServiceUrl);
                OCRResultDTO result = recognizeWithLocalService(imageBase64);
                if (result != null && result.getSuccess()) {
                    return result;
                }
            } catch (Exception e) {
                log.error("本地OCR服务调用失败: {}", e.getMessage(), e);
            }
        } else {
            if (restTemplate == null) {
                log.warn("RestTemplate未注入，跳过本地OCR服务");
            }
        }
        
        // 使用智谱AI
        if (StringUtils.hasText(zhipuApiKey)) {
            log.info("使用智谱AI OCR服务");
            return recognizeWithZhipuAI(imageBase64);
        }
        
        // 返回错误结果，不使用模拟数据
        log.error("OCR识别失败，无可用服务");
        return createErrorResult("OCR识别失败，请检查图片是否清晰或联系管理员配置OCR服务");
    }
    
    /**
     * 验证OCR服务URL，防止SSRF攻击
     * @return URL是否安全
     */
    private boolean validateOcrServiceUrl() {
        if (!StringUtils.hasText(ocrServiceUrl)) {
            return false;
        }
        
        try {
            URI uri = new URI(ocrServiceUrl);
            String host = uri.getHost();
            
            if (host == null) {
                log.warn("OCR服务URL格式无效: {}", ocrServiceUrl);
                return false;
            }
            
            // 如果允许私有IP，直接返回true
            if (allowPrivateIp) {
                log.debug("允许私有IP访问，跳过SSRF检查");
                return true;
            }
            
            // 解析域名获取IP地址
            InetAddress address = InetAddress.getByName(host);
            String ip = address.getHostAddress();
            
            // 检查是否为私有IP
            for (String blockedPrefix : BLOCKED_IP_PREFIXES) {
                if (ip.startsWith(blockedPrefix) || host.equalsIgnoreCase(blockedPrefix.trim())) {
                    log.warn("SSRF防护: 禁止访问私有IP地址: {} -> {}", host, ip);
                    return false;
                }
            }
            
            log.debug("OCR服务URL验证通过: {} -> {}", host, ip);
            return true;
            
        } catch (Exception e) {
            log.warn("OCR服务URL验证失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 创建错误结果
     * @param message 错误信息
     * @return OCR结果DTO
     */
    private OCRResultDTO createErrorResult(String message) {
        OCRResultDTO result = new OCRResultDTO();
        result.setSuccess(false);
        result.setErrorMessage(message);
        return result;
    }
    
    private OCRResultDTO recognizeWithLocalService(String imageBase64) {
        try {
            log.info("调用本地OCR服务: {}", ocrServiceUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            String requestBody = "image=" + URLEncoder.encode(imageBase64, StandardCharsets.UTF_8);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    ocrServiceUrl, 
                    HttpMethod.POST, 
                    entity, 
                    String.class
            );
            
            log.info("本地OCR服务响应状态: {}", response.getStatusCode());
            log.debug("本地OCR服务响应内容: {}", response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                
                OCRResultDTO result = new OCRResultDTO();
                
                // 安全获取 success 字段
                JsonNode successNode = root.path("success");
                boolean success = successNode.isBoolean() ? successNode.asBoolean() : false;
                result.setSuccess(success);
                
                result.setRawText(getJsonText(root, "rawText"));
                result.setMaterialName(getJsonText(root, "materialName"));
                result.setBarcode(getJsonText(root, "barcode"));
                
                String prodDate = getJsonText(root, "productionDate");
                if (StringUtils.hasText(prodDate)) {
                    result.setProductionDate(parseDate(prodDate));
                }
                
                JsonNode shelfLifeNode = root.path("shelfLifeDays");
                if (!shelfLifeNode.isMissingNode() && !shelfLifeNode.isNull()) {
                    result.setShelfLifeDays(shelfLifeNode.asInt());
                }
                
                JsonNode weightNode = root.path("weight");
                if (!weightNode.isMissingNode() && !weightNode.isNull()) {
                    result.setWeight(BigDecimal.valueOf(weightNode.asDouble()));
                }
                
                result.setWeightUnit(getJsonText(root, "weightUnit"));
                result.setStorageCondition(getJsonText(root, "storageCondition"));
                result.setSupplierName(getJsonText(root, "supplierName"));
                
                // 新增字段
                result.setIngredients(getJsonText(root, "ingredients"));
                result.setManufacturer(getJsonText(root, "manufacturer"));
                result.setOrigin(getJsonText(root, "origin"));
                result.setProductType(getJsonText(root, "productType"));
                result.setBrand(getJsonText(root, "brand"));
                result.setSpecifications(getJsonText(root, "specifications"));
                result.setNutritionInfo(getJsonText(root, "nutritionInfo"));
                result.setAllergenInfo(getJsonText(root, "allergenInfo"));
                
                JsonNode confidenceNode = root.path("confidence");
                if (!confidenceNode.isMissingNode() && !confidenceNode.isNull()) {
                    result.setConfidence(confidenceNode.asDouble());
                } else {
                    result.setConfidence(0.9);
                }
                
                if (success && StringUtils.hasText(result.getMaterialName())) {
                    Long matchedId = matchExistingTemplate(result.getMaterialName(), result.getBarcode());
                    if (matchedId != null) {
                        result.setMatchedTemplateId(matchedId);
                        result.setIsNewProduct(false);
                    } else {
                        result.setIsNewProduct(true);
                    }
                    return result;
                }
                
                log.warn("本地OCR服务返回异常: success={}, materialName={}", success, result.getMaterialName());
                return result;
            }
            
            log.warn("本地OCR服务响应异常");
            return null;
            
        } catch (Exception e) {
            log.warn("本地OCR服务调用失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 安全获取JSON文本字段
     * @param node JSON节点
     * @param field 字段名
     * @return 字段值，无效时返回null
     */
    private String getJsonText(JsonNode node, String field) {
        JsonNode fieldNode = node.path(field);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }
        String text = fieldNode.asText();
        return (text == null || text.isEmpty() || "null".equals(text)) ? null : text;
    }
    
    private OCRResultDTO recognizeWithZhipuAI(String imageBase64) {
        try {
            log.info("调用智谱AI GLM-4V进行OCR识别");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + zhipuApiKey);
            
            String imageUrl = "data:image/jpeg;base64," + imageBase64;
            
            Map<String, Object> messageContent = new HashMap<>();
            messageContent.put("type", "text");
            messageContent.put("text", buildPrompt());
            
            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image_url");
            Map<String, String> imageUrlMap = new HashMap<>();
            imageUrlMap.put("url", imageUrl);
            imageContent.put("image_url", imageUrlMap);
            
            List<Map<String, Object>> contentList = new ArrayList<>();
            contentList.add(messageContent);
            contentList.add(imageContent);
            
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", contentList);
            
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(message);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", zhipuModel);
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 1024);
            
            String requestJson = objectMapper.writeValueAsString(requestBody);
            log.debug("请求体: {}", requestJson.substring(0, Math.min(500, requestJson.length())));
            
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    ZHIPU_API_URL, 
                    HttpMethod.POST, 
                    entity, 
                    String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                
                if (choices.isArray() && choices.size() > 0) {
                    String content = choices.get(0).path("message").path("content").asText();
                    log.info("智谱AI返回内容: {}", content);
                    
                    return parseAIResponse(content);
                }
            }
            
            log.warn("智谱AI返回异常");
            return createErrorResult("智谱AI识别失败");
            
        } catch (Exception e) {
            log.error("智谱AI OCR识别失败: {}", e.getMessage(), e);
            return createErrorResult("智谱AI识别失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建智谱AI提示词
     * @return 提示词字符串
     */
    private String buildPrompt() {
        return "请识别这张商品包装图片中的信息，提取以下字段并以JSON格式返回：\n" +
                "{\n" +
                "  \"materialName\": \"商品名称\",\n" +
                "  \"barcode\": \"条形码（如果有）\",\n" +
                "  \"productionDate\": \"生产日期（格式：YYYY-MM-DD）\",\n" +
                "  \"shelfLifeDays\": 保质期天数（数字）,\n" +
                "  \"weight\": 重量（数字）,\n" +
                "  \"weightUnit\": \"重量单位（kg/g/斤/L等）\",\n" +
                "  \"storageCondition\": \"存储条件（常温/冷藏/冷冻/阴凉干燥）\",\n" +
                "  \"supplierName\": \"供应商或生产商名称\"\n" +
                "}\n\n" +
                "如果某个字段无法识别，请填null。只返回JSON，不要其他文字。";
    }
    
    private OCRResultDTO parseAIResponse(String content) {
        OCRResultDTO result = new OCRResultDTO();
        result.setSuccess(true);
        result.setRawText(content);
        
        try {
            String jsonStr = extractJsonFromContent(content);
            JsonNode json = objectMapper.readTree(jsonStr);
            
            result.setMaterialName(getJsonString(json, "materialName"));
            result.setBarcode(getJsonString(json, "barcode"));
            
            String prodDate = getJsonString(json, "productionDate");
            if (prodDate != null && !"null".equals(prodDate)) {
                result.setProductionDate(parseDate(prodDate));
            }
            
            Integer shelfLife = getJsonInt(json, "shelfLifeDays");
            if (shelfLife != null && shelfLife > 0) {
                result.setShelfLifeDays(shelfLife);
                if (result.getProductionDate() != null) {
                    result.setExpiryDate(result.getProductionDate().plusDays(shelfLife));
                }
            }
            
            Double weight = getJsonDouble(json, "weight");
            if (weight != null && weight > 0) {
                result.setWeight(BigDecimal.valueOf(weight));
            }
            
            String unit = getJsonString(json, "weightUnit");
            if (unit != null && !"null".equals(unit)) {
                result.setWeightUnit(unit);
            }
            
            String storage = getJsonString(json, "storageCondition");
            if (storage != null && !"null".equals(storage)) {
                result.setStorageCondition(storage);
            }
            
            result.setSupplierName(getJsonString(json, "supplierName"));
            result.setConfidence(0.9);
            
        } catch (Exception e) {
            log.warn("解析AI响应失败，尝试正则提取: {}", e.getMessage());
            result = extractFromRawText(content);
        }
        
        Long matchedId = matchExistingTemplate(result.getMaterialName(), result.getBarcode());
        if (matchedId != null) {
            result.setMatchedTemplateId(matchedId);
            result.setIsNewProduct(false);
        } else {
            result.setIsNewProduct(true);
        }
        
        return result;
    }
    
    /**
     * 从内容中提取JSON字符串
     * @param content 原始内容
     * @return JSON字符串
     */
    private String extractJsonFromContent(String content) {
        String jsonStr = content;
        if (content.contains("```json")) {
            jsonStr = content.substring(content.indexOf("```json") + 7, content.lastIndexOf("```"));
        } else if (content.contains("```")) {
            jsonStr = content.substring(content.indexOf("```") + 3, content.lastIndexOf("```"));
        }
        return jsonStr.trim();
    }
    
    private OCRResultDTO extractFromRawText(String text) {
        OCRResultDTO result = new OCRResultDTO();
        result.setSuccess(true);
        result.setRawText(text);
        
        String[] patterns = {
            "品名[：:]*\\s*([^\\n\\r]+)",
            "商品名称[：:]*\\s*([^\\n\\r]+)",
            "名称[：:]*\\s*([^\\n\\r]+)"
        };
        
        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(text);
            if (matcher.find()) {
                result.setMaterialName(matcher.group(1).trim());
                break;
            }
        }
        
        Pattern barcodePattern = Pattern.compile("(\\d{8,14})");
        Matcher barcodeMatcher = barcodePattern.matcher(text);
        if (barcodeMatcher.find()) {
            result.setBarcode(barcodeMatcher.group(1));
        }
        
        return result;
    }
    
    private String getJsonString(JsonNode json, String field) {
        JsonNode node = json.path(field);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asText();
    }
    
    private Integer getJsonInt(JsonNode json, String field) {
        JsonNode node = json.path(field);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asInt();
    }
    
    private Double getJsonDouble(JsonNode json, String field) {
        JsonNode node = json.path(field);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asDouble();
    }
    
    private LocalDate parseDate(String dateStr) {
        String[] patterns = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyyMMdd", "yyyy年MM月dd日"};
        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(dateStr.replaceAll("[年月]", "-").replace("日", ""), formatter);
            } catch (Exception ignored) {}
        }
        return null;
    }

    @Override
    public OCRResultDTO extractProductInfo(String ocrText) {
        return extractFromRawText(ocrText);
    }

    @Override
    public Long matchExistingTemplate(String productName, String barcode) {
        // 优先通过条形码精确匹配
        if (StringUtils.hasText(barcode)) {
            LambdaQueryWrapper<MaterialTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MaterialTemplate::getBarcode, barcode)
                   .eq(MaterialTemplate::getStatus, "active");
            MaterialTemplate template = materialTemplateMapper.selectOne(wrapper);
            if (template != null) {
                return template.getId();
            }
        }
        
        // 通过商品名称模糊匹配
        if (StringUtils.hasText(productName)) {
            LambdaQueryWrapper<MaterialTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.like(MaterialTemplate::getMaterialName, productName)
                   .eq(MaterialTemplate::getStatus, "active");
            List<MaterialTemplate> templates = materialTemplateMapper.selectList(wrapper);
            if (!templates.isEmpty()) {
                return templates.get(0).getId();
            }
        }
        
        return null;
    }

    @Override
    public List<String> compareWithTemplate(Long templateId, OCRResultDTO ocrResult) {
        List<String> differences = new ArrayList<>();
        
        MaterialTemplate template = materialTemplateMapper.selectById(templateId);
        if (template == null) {
            return differences;
        }
        
        // 比较保质期
        if (ocrResult.getShelfLifeDays() != null && 
            template.getDefaultShelfLife() != null &&
            !ocrResult.getShelfLifeDays().equals(template.getDefaultShelfLife())) {
            differences.add("保质期: 档案=" + template.getDefaultShelfLife() + "天, 识别=" + ocrResult.getShelfLifeDays() + "天");
        }
        
        // 比较存储条件
        if (ocrResult.getStorageCondition() != null && 
            template.getStorageCondition() != null &&
            !ocrResult.getStorageCondition().equals(template.getStorageCondition())) {
            differences.add("存储条件: 档案=" + template.getStorageCondition() + ", 识别=" + ocrResult.getStorageCondition());
        }
        
        return differences;
    }
}
