package com.foodtraceability.service.impl;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.YoloRecognitionResultDTO;
import com.foodtraceability.service.YoloProductRecognitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * YOLO 商品识别服务默认实现
 * <p>
 * 默认未启用真实推理，提供两种模式：
 * <ul>
 *   <li>禁用模式（默认）：返回 disabled 状态，前端展示"未启用 YOLO"提示</li>
 *   <li>Mock 模式：通过配置 yolo.product.mock-enabled=true 启用，
 *       返回模拟识别结果，便于前端联调</li>
 * </ul>
 * </p>
 * <p>
 * 生产环境接入真实 YOLO 推理服务时：
 * <ol>
 *   <li>在 application.yml 配置 yolo.product.inference-url=http://inference-service:port/predict</li>
 *   <li>实现真实推理调用（HTTP/gRPC 调用推理服务）</li>
 *   <li>用 @Primary 或 @ConditionalOnProperty(name="yolo.product.inference-url") 切换实现</li>
 * </ol>
 * </p>
 */
@Service
public class DefaultYoloProductRecognitionService implements YoloProductRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(DefaultYoloProductRecognitionService.class);

    @Value("${yolo.product.enabled:false}")
    private boolean enabled;

    @Value("${yolo.product.mock-enabled:false}")
    private boolean mockEnabled;

    @Value("${yolo.product.inference-url:}")
    private String inferenceUrl;

    @Value("${yolo.product.confidence-threshold:0.5}")
    private double confidenceThreshold;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Result<YoloRecognitionResultDTO> recognizeProducts(String image, String storeId) {
        long start = System.currentTimeMillis();
        log.info("YOLO 商品识别请求: storeId={}, imageLength={}", storeId,
                image != null ? image.length() : 0);

        YoloRecognitionResultDTO result = new YoloRecognitionResultDTO();
        result.setEnabled(enabled);
        result.setProcessingTimeMs(System.currentTimeMillis() - start);

        // 服务未启用
        if (!enabled) {
            result.setSuccess(false);
            result.setErrorMessage("YOLO 商品识别服务未启用，请联系管理员配置 yolo.product.enabled=true");
            return Result.success(result, "服务未启用");
        }

        // Mock 模式（用于前端联调）
        if (mockEnabled) {
            log.info("YOLO Mock 模式启用，返回模拟识别结果");
            return Result.success(mockRecognize(image, storeId, start), "Mock 识别成功");
        }

        // 真实推理（未实现，需对接推理服务）
        if (inferenceUrl == null || inferenceUrl.isBlank()) {
            result.setSuccess(false);
            result.setErrorMessage("YOLO 推理服务 URL 未配置（yolo.product.inference-url）");
            return Result.error("推理服务未配置");
        }

        // TODO: 实现真实推理调用
        // 1. 调用推理服务（HTTP POST inferenceUrl，body: {image, storeId}）
        // 2. 解析推理服务返回的检测框列表
        // 3. 对每个检测框的 className 匹配本地 SKU（foods 表）
        // 4. 合并同类商品、统计数量
        // 5. 返回识别结果
        result.setSuccess(false);
        result.setErrorMessage("真实推理尚未实现，请配置 yolo.product.mock-enabled=true 使用 Mock 模式");
        return Result.error("真实推理未实现");
    }

    /**
     * Mock 识别（前端联调用）
     * 返回 2-3 个模拟识别商品，验证完整流程
     */
    private YoloRecognitionResultDTO mockRecognize(String image, String storeId, long startTime) {
        YoloRecognitionResultDTO result = new YoloRecognitionResultDTO();
        result.setEnabled(true);
        result.setSuccess(true);

        List<YoloRecognitionResultDTO.RecognizedProduct> products = new ArrayList<>();

        // 模拟商品 1
        YoloRecognitionResultDTO.RecognizedProduct p1 = new YoloRecognitionResultDTO.RecognizedProduct();
        p1.setFoodId("mock-food-001");
        p1.setFoodName("测试商品A");
        p1.setPrice(new java.math.BigDecimal("18.00"));
        p1.setQuantity(2);
        p1.setConfidence(0.92);
        p1.setBbox(new double[]{100, 100, 200, 200});
        products.add(p1);

        // 模拟商品 2
        YoloRecognitionResultDTO.RecognizedProduct p2 = new YoloRecognitionResultDTO.RecognizedProduct();
        p2.setFoodId("mock-food-002");
        p2.setFoodName("测试商品B");
        p2.setPrice(new java.math.BigDecimal("25.50"));
        p2.setQuantity(1);
        p2.setConfidence(0.88);
        p2.setBbox(new double[]{250, 100, 350, 200});
        products.add(p2);

        // 模拟未匹配检测框
        List<YoloRecognitionResultDTO.UnmatchedDetection> unmatched = new ArrayList<>();
        YoloRecognitionResultDTO.UnmatchedDetection u1 = new YoloRecognitionResultDTO.UnmatchedDetection();
        u1.setClassName("unknown_dish");
        u1.setConfidence(0.41);
        u1.setBbox(new double[]{400, 100, 500, 200});
        unmatched.add(u1);

        result.setProducts(products);
        result.setUnmatchedDetections(unmatched);
        result.setProcessingTimeMs(System.currentTimeMillis() - startTime);

        log.info("Mock 识别完成: 识别到 {} 个商品，{} 个未匹配检测框",
                products.size(), unmatched.size());
        return result;
    }
}
