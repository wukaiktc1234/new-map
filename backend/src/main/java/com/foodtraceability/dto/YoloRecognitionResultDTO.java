package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * YOLO 商品识别结果 DTO
 */
@Schema(description = "YOLO 商品识别结果")
public class YoloRecognitionResultDTO {
    @Schema(description = "是否启用 YOLO 服务")
    private boolean enabled;

    @Schema(description = "识别是否成功")
    private boolean success;

    @Schema(description = "处理耗时（毫秒）")
    private long processingTimeMs;

    @Schema(description = "识别到的商品列表")
    private List<RecognizedProduct> products;

    @Schema(description = "未匹配 SKU 的检测框列表（需人工匹配）")
    private List<UnmatchedDetection> unmatchedDetections;

    @Schema(description = "错误信息（识别失败时返回）")
    private String errorMessage;

    public YoloRecognitionResultDTO() {
    }

    @Schema(description = "识别到的单个商品")
    public static class RecognizedProduct {
        @Schema(description = "商品 ID（匹配到 foods 表的 id）")
        private String foodId;
        @Schema(description = "商品名称")
        private String foodName;
        @Schema(description = "单价（元）")
        private java.math.BigDecimal price;
        @Schema(description = "数量")
        private int quantity;
        @Schema(description = "置信度（0-1）")
        private double confidence;
        @Schema(description = "检测框坐标 [x1, y1, x2, y2]")
        private double[] bbox;

        public RecognizedProduct() {
        }

        public String getFoodId() { return this.foodId; }
        public void setFoodId(String foodId) { this.foodId = foodId; }
        public String getFoodName() { return this.foodName; }
        public void setFoodName(String foodName) { this.foodName = foodName; }
        public java.math.BigDecimal getPrice() { return this.price; }
        public void setPrice(java.math.BigDecimal price) { this.price = price; }
        public int getQuantity() { return this.quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getConfidence() { return this.confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public double[] getBbox() { return this.bbox; }
        public void setBbox(double[] bbox) { this.bbox = bbox; }
    }

    @Schema(description = "未匹配 SKU 的检测框")
    public static class UnmatchedDetection {
        @Schema(description = "YOLO 原始类别名（训练时的标签）")
        private String className;
        @Schema(description = "置信度")
        private double confidence;
        @Schema(description = "检测框坐标")
        private double[] bbox;

        public UnmatchedDetection() {
        }

        public String getClassName() { return this.className; }
        public void setClassName(String className) { this.className = className; }
        public double getConfidence() { return this.confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public double[] getBbox() { return this.bbox; }
        public void setBbox(double[] bbox) { this.bbox = bbox; }
    }

    public boolean isEnabled() { return this.enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isSuccess() { return this.success; }
    public void setSuccess(boolean success) { this.success = success; }
    public long getProcessingTimeMs() { return this.processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    public List<RecognizedProduct> getProducts() { return this.products; }
    public void setProducts(List<RecognizedProduct> products) { this.products = products; }
    public List<UnmatchedDetection> getUnmatchedDetections() { return this.unmatchedDetections; }
    public void setUnmatchedDetections(List<UnmatchedDetection> unmatchedDetections) { this.unmatchedDetections = unmatchedDetections; }
    public String getErrorMessage() { return this.errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
