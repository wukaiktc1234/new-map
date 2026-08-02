package com.foodtraceability.service;

import com.foodtraceability.dto.LabelLayoutConfigDTO;

import java.util.Map;

/**
 * 标签预览服务
 * 生成与打印完全一致的HTML预览
 */
public interface LabelPreviewService {

    /**
     * 生成标签HTML预览
     *
     * @param layoutConfig 布局配置
     * @param data         标签数据
     * @return HTML内容
     */
    String generateHtmlPreview(LabelLayoutConfigDTO layoutConfig, Map<String, Object> data);

    /**
     * 生成标签预览数据（用于前端Canvas渲染）
     *
     * @param layoutConfig 布局配置
     * @param data         标签数据
     * @return 预览数据（JSON格式）
     */
    PreviewData generatePreviewData(LabelLayoutConfigDTO layoutConfig, Map<String, Object> data);

    /**
     * 生成缩略图（Base64图片）
     *
     * @param layoutConfig 布局配置
     * @param data         标签数据
     * @param maxWidth     最大宽度（像素）
     * @param maxHeight    最大高度（像素）
     * @return Base64编码的PNG图片
     */
    String generateThumbnail(LabelLayoutConfigDTO layoutConfig, Map<String, Object> data,
                            int maxWidth, int maxHeight);

    /**
     * 预览数据结构
     */
    class PreviewData {
        /**
         * 标签宽度（像素）
         */
        private int width;

        /**
         * 标签高度（像素）
         */
        private int height;

        /**
         * 缩放比例
         */
        private double scale;

        /**
         * 元素列表
         */
        private java.util.List<PreviewElement> elements;

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public double getScale() {
            return scale;
        }

        public void setScale(double scale) {
            this.scale = scale;
        }

        public java.util.List<PreviewElement> getElements() {
            return elements;
        }

        public void setElements(java.util.List<PreviewElement> elements) {
            this.elements = elements;
        }
    }

    /**
     * 预览元素
     */
    class PreviewElement {
        /**
         * 元素ID
         */
        private String id;

        /**
         * 元素类型
         */
        private String type;

        /**
         * X坐标（像素）
         */
        private int x;

        /**
         * Y坐标（像素）
         */
        private int y;

        /**
         * 宽度（像素）
         */
        private int width;

        /**
         * 高度（像素）
         */
        private int height;

        /**
         * 内容
         */
        private String content;

        /**
         * 样式
         */
        private Map<String, Object> style;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Map<String, Object> getStyle() {
            return style;
        }

        public void setStyle(Map<String, Object> style) {
            this.style = style;
        }
    }
}
