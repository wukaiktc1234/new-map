package com.foodtraceability.service;

import com.foodtraceability.dto.LabelLayoutConfigDTO;
import com.foodtraceability.dto.LabelElementDTO;

import java.util.Map;

/**
 * TSPL指令生成服务
 * 负责将标签布局配置转换为TSPL打印指令
 */
public interface TsplGeneratorService {

    /**
     * 根据布局配置生成TSPL指令
     *
     * @param layoutConfig 布局配置
     * @return TSPL指令字符串
     */
    String generateTspl(LabelLayoutConfigDTO layoutConfig);

    /**
     * 根据布局配置和数据生成TSPL指令
     *
     * @param layoutConfig 布局配置
     * @param data         标签数据
     * @return TSPL指令字符串
     */
    String generateTsplWithData(LabelLayoutConfigDTO layoutConfig, Map<String, Object> data);

    /**
     * 生成单个元素的TSPL指令
     *
     * @param element 元素配置
     * @param data    数据（可选）
     * @param dpi     DPI
     * @return TSPL指令字符串
     */
    String generateElementTspl(LabelElementDTO element, Map<String, Object> data, int dpi);

    /**
     * 验证布局配置
     *
     * @param layoutConfig 布局配置
     * @return 验证结果
     */
    ValidationResult validateLayout(LabelLayoutConfigDTO layoutConfig);

    /**
     * 计算元素的实际尺寸
     *
     * @param element 元素配置
     * @param data    数据（可选）
     * @param dpi     DPI
     * @return 尺寸信息 [width, height]
     */
    int[] calculateElementSize(LabelElementDTO element, Map<String, Object> data, int dpi);

    /**
     * 验证结果
     */
    class ValidationResult {
        private boolean valid;
        private String message;
        private String elementId;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public ValidationResult(boolean valid, String message, String elementId) {
            this.valid = valid;
            this.message = message;
            this.elementId = elementId;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public String getElementId() {
            return elementId;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, "验证通过");
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public static ValidationResult error(String message, String elementId) {
            return new ValidationResult(false, message, elementId);
        }
    }
}
