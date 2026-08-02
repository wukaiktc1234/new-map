package com.foodtraceability.dto;

/**
 * 标签元素DTO
 * 定义标签上的单个元素（文本、条码、二维码等）
 */
public class LabelElementDTO {
    /**
     * 元素ID
     */
    private String id;
    /**
     * 元素类型：TEXT-文本, BARCODE-条码, QRCODE-二维码, LINE-线条, BOX-矩形框
     */
    private ElementType type;
    /**
     * 元素位置配置
     */
    private PositionConfig position;
    /**
     * 文本配置（type=TEXT时有效）
     */
    private TextConfig text;
    /**
     * 条码配置（type=BARCODE时有效）
     */
    private BarcodeConfig barcode;
    /**
     * 二维码配置（type=QRCODE时有效）
     */
    private QRCodeConfig qrcode;
    /**
     * 线条配置（type=LINE时有效）
     */
    private LineConfig line;
    /**
     * 矩形框配置（type=BOX时有效）
     */
    private BoxConfig box;
    /**
     * 数据绑定配置
     */
    private DataBindingConfig dataBinding;
    /**
     * 是否可见
     */
    private Boolean visible = true;


    /**
     * 元素类型枚举
     */
    public enum ElementType {
        TEXT, BARCODE, QRCODE, LINE, BOX;
    }


    /**
     * 位置配置
     * 注意：前端传递的所有坐标和尺寸单位都是 mm（毫米）
     * 后端会根据打印机 DPI 自动转换为 dots
     */
    public static class PositionConfig {
        /**
         * X坐标（mm）
         */
        private Integer x;
        /**
         * Y坐标（mm）
         */
        private Integer y;
        /**
         * 宽度（mm，可选）
         */
        private Integer width;
        /**
         * 高度（mm，可选）
         */
        private Integer height;
        /**
         * 旋转角度（0, 90, 180, 270）
         */
        private Integer rotation = 0;
        /**
         * 对齐方式：left, center, right
         */
        private String align = "left";

        /**
         * 从毫米转换为点数
         */
        public static PositionConfig fromMm(double xMm, double yMm, int dpi) {
            PositionConfig pos = new PositionConfig();
            pos.x = (int) (xMm * dpi / 25.4);
            pos.y = (int) (yMm * dpi / 25.4);
            return pos;
        }

        public PositionConfig() {
        }

        /**
         * X坐标（mm）
         */
        public Integer getX() {
            return this.x;
        }

        /**
         * Y坐标（mm）
         */
        public Integer getY() {
            return this.y;
        }

        /**
         * 宽度（mm，可选）
         */
        public Integer getWidth() {
            return this.width;
        }

        /**
         * 高度（mm，可选）
         */
        public Integer getHeight() {
            return this.height;
        }

        /**
         * 旋转角度（0, 90, 180, 270）
         */
        public Integer getRotation() {
            return this.rotation;
        }

        /**
         * 对齐方式：left, center, right
         */
        public String getAlign() {
            return this.align;
        }

        /**
         * X坐标（mm）
         */
        public void setX(final Integer x) {
            this.x = x;
        }

        /**
         * Y坐标（mm）
         */
        public void setY(final Integer y) {
            this.y = y;
        }

        /**
         * 宽度（mm，可选）
         */
        public void setWidth(final Integer width) {
            this.width = width;
        }

        /**
         * 高度（mm，可选）
         */
        public void setHeight(final Integer height) {
            this.height = height;
        }

        /**
         * 旋转角度（0, 90, 180, 270）
         */
        public void setRotation(final Integer rotation) {
            this.rotation = rotation;
        }

        /**
         * 对齐方式：left, center, right
         */
        public void setAlign(final String align) {
            this.align = align;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof LabelElementDTO.PositionConfig)) return false;
            final LabelElementDTO.PositionConfig other = (LabelElementDTO.PositionConfig) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$x = this.getX();
            final java.lang.Object other$x = other.getX();
            if (this$x == null ? other$x != null : !this$x.equals(other$x)) return false;
            final java.lang.Object this$y = this.getY();
            final java.lang.Object other$y = other.getY();
            if (this$y == null ? other$y != null : !this$y.equals(other$y)) return false;
            final java.lang.Object this$width = this.getWidth();
            final java.lang.Object other$width = other.getWidth();
            if (this$width == null ? other$width != null : !this$width.equals(other$width)) return false;
            final java.lang.Object this$height = this.getHeight();
            final java.lang.Object other$height = other.getHeight();
            if (this$height == null ? other$height != null : !this$height.equals(other$height)) return false;
            final java.lang.Object this$rotation = this.getRotation();
            final java.lang.Object other$rotation = other.getRotation();
            if (this$rotation == null ? other$rotation != null : !this$rotation.equals(other$rotation)) return false;
            final java.lang.Object this$align = this.getAlign();
            final java.lang.Object other$align = other.getAlign();
            if (this$align == null ? other$align != null : !this$align.equals(other$align)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof LabelElementDTO.PositionConfig;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $x = this.getX();
            result = result * PRIME + ($x == null ? 43 : $x.hashCode());
            final java.lang.Object $y = this.getY();
            result = result * PRIME + ($y == null ? 43 : $y.hashCode());
            final java.lang.Object $width = this.getWidth();
            result = result * PRIME + ($width == null ? 43 : $width.hashCode());
            final java.lang.Object $height = this.getHeight();
            result = result * PRIME + ($height == null ? 43 : $height.hashCode());
            final java.lang.Object $rotation = this.getRotation();
            result = result * PRIME + ($rotation == null ? 43 : $rotation.hashCode());
            final java.lang.Object $align = this.getAlign();
            result = result * PRIME + ($align == null ? 43 : $align.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "LabelElementDTO.PositionConfig(x=" + this.getX() + ", y=" + this.getY() + ", width=" + this.getWidth() + ", height=" + this.getHeight() + ", rotation=" + this.getRotation() + ", align=" + this.getAlign() + ")";
        }
    }


    /**
     * 文本配置
     */
    public static class TextConfig {
        /**
         * 文本内容（固定文本或数据绑定字段名）
         */
        private String content;
        /**
         * 字体名称
         * TSS24.BF2 - 24点中文宋体
         * TSS20.BF2 - 20点中文宋体
         * TSS16.BF2 - 16点中文宋体
         * TSS12.BF2 - 12点中文宋体
         * 2 - 8点英文字体
         * 3 - 12点英文字体
         */
        private String font = "TSS24.BF2";
        /**
         * 字体大小倍数X（1-10）
         */
        private Integer scaleX = 1;
        /**
         * 字体大小倍数Y（1-10）
         */
        private Integer scaleY = 1;
        /**
         * 是否加粗
         */
        private Boolean bold = false;
        /**
         * 是否下划线
         */
        private Boolean underline = false;
        /**
         * 最大宽度（点数，用于自动换行）
         */
        private Integer maxWidth;
        /**
         * 行高（点数）
         */
        private Integer lineHeight;
        /**
         * 自动截断字符数（0表示不截断）
         */
        private Integer truncateChars = 0;

        public TextConfig() {
        }

        /**
         * 文本内容（固定文本或数据绑定字段名）
         */
        public String getContent() {
            return this.content;
        }

        /**
         * 字体名称
         * TSS24.BF2 - 24点中文宋体
         * TSS20.BF2 - 20点中文宋体
         * TSS16.BF2 - 16点中文宋体
         * TSS12.BF2 - 12点中文宋体
         * 2 - 8点英文字体
         * 3 - 12点英文字体
         */
        public String getFont() {
            return this.font;
        }

        /**
         * 字体大小倍数X（1-10）
         */
        public Integer getScaleX() {
            return this.scaleX;
        }

        /**
         * 字体大小倍数Y（1-10）
         */
        public Integer getScaleY() {
            return this.scaleY;
        }

        /**
         * 是否加粗
         */
        public Boolean getBold() {
            return this.bold;
        }

        /**
         * 是否下划线
         */
        public Boolean getUnderline() {
            return this.underline;
        }

        /**
         * 最大宽度（点数，用于自动换行）
         */
        public Integer getMaxWidth() {
            return this.maxWidth;
        }

        /**
         * 行高（点数）
         */
        public Integer getLineHeight() {
            return this.lineHeight;
        }

        /**
         * 自动截断字符数（0表示不截断）
         */
        public Integer getTruncateChars() {
            return this.truncateChars;
        }

        /**
         * 文本内容（固定文本或数据绑定字段名）
         */
        public void setContent(final String content) {
            this.content = content;
        }

        /**
         * 字体名称
         * TSS24.BF2 - 24点中文宋体
         * TSS20.BF2 - 20点中文宋体
         * TSS16.BF2 - 16点中文宋体
         * TSS12.BF2 - 12点中文宋体
         * 2 - 8点英文字体
         * 3 - 12点英文字体
         */
        public void setFont(final String font) {
            this.font = font;
        }

        /**
         * 字体大小倍数X（1-10）
         */
        public void setScaleX(final Integer scaleX) {
            this.scaleX = scaleX;
        }

        /**
         * 字体大小倍数Y（1-10）
         */
        public void setScaleY(final Integer scaleY) {
            this.scaleY = scaleY;
        }

        /**
         * 是否加粗
         */
        public void setBold(final Boolean bold) {
            this.bold = bold;
        }

        /**
         * 是否下划线
         */
        public void setUnderline(final Boolean underline) {
            this.underline = underline;
        }

        /**
         * 最大宽度（点数，用于自动换行）
         */
        public void setMaxWidth(final Integer maxWidth) {
            this.maxWidth = maxWidth;
        }

        /**
         * 行高（点数）
         */
        public void setLineHeight(final Integer lineHeight) {
            this.lineHeight = lineHeight;
        }

        /**
         * 自动截断字符数（0表示不截断）
         */
        public void setTruncateChars(final Integer truncateChars) {
            this.truncateChars = truncateChars;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof LabelElementDTO.TextConfig)) return false;
            final LabelElementDTO.TextConfig other = (LabelElementDTO.TextConfig) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$scaleX = this.getScaleX();
            final java.lang.Object other$scaleX = other.getScaleX();
            if (this$scaleX == null ? other$scaleX != null : !this$scaleX.equals(other$scaleX)) return false;
            final java.lang.Object this$scaleY = this.getScaleY();
            final java.lang.Object other$scaleY = other.getScaleY();
            if (this$scaleY == null ? other$scaleY != null : !this$scaleY.equals(other$scaleY)) return false;
            final java.lang.Object this$bold = this.getBold();
            final java.lang.Object other$bold = other.getBold();
            if (this$bold == null ? other$bold != null : !this$bold.equals(other$bold)) return false;
            final java.lang.Object this$underline = this.getUnderline();
            final java.lang.Object other$underline = other.getUnderline();
            if (this$underline == null ? other$underline != null : !this$underline.equals(other$underline)) return false;
            final java.lang.Object this$maxWidth = this.getMaxWidth();
            final java.lang.Object other$maxWidth = other.getMaxWidth();
            if (this$maxWidth == null ? other$maxWidth != null : !this$maxWidth.equals(other$maxWidth)) return false;
            final java.lang.Object this$lineHeight = this.getLineHeight();
            final java.lang.Object other$lineHeight = other.getLineHeight();
            if (this$lineHeight == null ? other$lineHeight != null : !this$lineHeight.equals(other$lineHeight)) return false;
            final java.lang.Object this$truncateChars = this.getTruncateChars();
            final java.lang.Object other$truncateChars = other.getTruncateChars();
            if (this$truncateChars == null ? other$truncateChars != null : !this$truncateChars.equals(other$truncateChars)) return false;
            final java.lang.Object this$content = this.getContent();
            final java.lang.Object other$content = other.getContent();
            if (this$content == null ? other$content != null : !this$content.equals(other$content)) return false;
            final java.lang.Object this$font = this.getFont();
            final java.lang.Object other$font = other.getFont();
            if (this$font == null ? other$font != null : !this$font.equals(other$font)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof LabelElementDTO.TextConfig;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $scaleX = this.getScaleX();
            result = result * PRIME + ($scaleX == null ? 43 : $scaleX.hashCode());
            final java.lang.Object $scaleY = this.getScaleY();
            result = result * PRIME + ($scaleY == null ? 43 : $scaleY.hashCode());
            final java.lang.Object $bold = this.getBold();
            result = result * PRIME + ($bold == null ? 43 : $bold.hashCode());
            final java.lang.Object $underline = this.getUnderline();
            result = result * PRIME + ($underline == null ? 43 : $underline.hashCode());
            final java.lang.Object $maxWidth = this.getMaxWidth();
            result = result * PRIME + ($maxWidth == null ? 43 : $maxWidth.hashCode());
            final java.lang.Object $lineHeight = this.getLineHeight();
            result = result * PRIME + ($lineHeight == null ? 43 : $lineHeight.hashCode());
            final java.lang.Object $truncateChars = this.getTruncateChars();
            result = result * PRIME + ($truncateChars == null ? 43 : $truncateChars.hashCode());
            final java.lang.Object $content = this.getContent();
            result = result * PRIME + ($content == null ? 43 : $content.hashCode());
            final java.lang.Object $font = this.getFont();
            result = result * PRIME + ($font == null ? 43 : $font.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "LabelElementDTO.TextConfig(content=" + this.getContent() + ", font=" + this.getFont() + ", scaleX=" + this.getScaleX() + ", scaleY=" + this.getScaleY() + ", bold=" + this.getBold() + ", underline=" + this.getUnderline() + ", maxWidth=" + this.getMaxWidth() + ", lineHeight=" + this.getLineHeight() + ", truncateChars=" + this.getTruncateChars() + ")";
        }
    }


    /**
     * 条码配置
     */
    public static class BarcodeConfig {
        /**
         * 条码类型
         * 128 - Code 128
         * 39 - Code 39
         * EAN13 - EAN-13
         * EAN8 - EAN-8
         * UPC-A - UPC-A
         * UPC-E - UPC-E
         */
        private String barcodeType = "128";
        /**
         * 条码内容
         */
        private String content;
        /**
         * 条码高度（点数）
         */
        private Integer height = 80;
        /**
         * 是否显示文本
         */
        private Boolean showText = true;
        /**
         * 文本位置：0-下方, 1-上方
         */
        private Integer textPosition = 0;
        /**
         * 窄条宽度
         */
        private Integer narrowWidth = 2;
        /**
         * 宽条宽度
         */
        private Integer wideWidth = 2;

        public BarcodeConfig() {
        }

        /**
         * 条码类型
         * 128 - Code 128
         * 39 - Code 39
         * EAN13 - EAN-13
         * EAN8 - EAN-8
         * UPC-A - UPC-A
         * UPC-E - UPC-E
         */
        public String getBarcodeType() {
            return this.barcodeType;
        }

        /**
         * 条码内容
         */
        public String getContent() {
            return this.content;
        }

        /**
         * 条码高度（点数）
         */
        public Integer getHeight() {
            return this.height;
        }

        /**
         * 是否显示文本
         */
        public Boolean getShowText() {
            return this.showText;
        }

        /**
         * 文本位置：0-下方, 1-上方
         */
        public Integer getTextPosition() {
            return this.textPosition;
        }

        /**
         * 窄条宽度
         */
        public Integer getNarrowWidth() {
            return this.narrowWidth;
        }

        /**
         * 宽条宽度
         */
        public Integer getWideWidth() {
            return this.wideWidth;
        }

        /**
         * 条码类型
         * 128 - Code 128
         * 39 - Code 39
         * EAN13 - EAN-13
         * EAN8 - EAN-8
         * UPC-A - UPC-A
         * UPC-E - UPC-E
         */
        public void setBarcodeType(final String barcodeType) {
            this.barcodeType = barcodeType;
        }

        /**
         * 条码内容
         */
        public void setContent(final String content) {
            this.content = content;
        }

        /**
         * 条码高度（点数）
         */
        public void setHeight(final Integer height) {
            this.height = height;
        }

        /**
         * 是否显示文本
         */
        public void setShowText(final Boolean showText) {
            this.showText = showText;
        }

        /**
         * 文本位置：0-下方, 1-上方
         */
        public void setTextPosition(final Integer textPosition) {
            this.textPosition = textPosition;
        }

        /**
         * 窄条宽度
         */
        public void setNarrowWidth(final Integer narrowWidth) {
            this.narrowWidth = narrowWidth;
        }

        /**
         * 宽条宽度
         */
        public void setWideWidth(final Integer wideWidth) {
            this.wideWidth = wideWidth;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof LabelElementDTO.BarcodeConfig)) return false;
            final LabelElementDTO.BarcodeConfig other = (LabelElementDTO.BarcodeConfig) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$height = this.getHeight();
            final java.lang.Object other$height = other.getHeight();
            if (this$height == null ? other$height != null : !this$height.equals(other$height)) return false;
            final java.lang.Object this$showText = this.getShowText();
            final java.lang.Object other$showText = other.getShowText();
            if (this$showText == null ? other$showText != null : !this$showText.equals(other$showText)) return false;
            final java.lang.Object this$textPosition = this.getTextPosition();
            final java.lang.Object other$textPosition = other.getTextPosition();
            if (this$textPosition == null ? other$textPosition != null : !this$textPosition.equals(other$textPosition)) return false;
            final java.lang.Object this$narrowWidth = this.getNarrowWidth();
            final java.lang.Object other$narrowWidth = other.getNarrowWidth();
            if (this$narrowWidth == null ? other$narrowWidth != null : !this$narrowWidth.equals(other$narrowWidth)) return false;
            final java.lang.Object this$wideWidth = this.getWideWidth();
            final java.lang.Object other$wideWidth = other.getWideWidth();
            if (this$wideWidth == null ? other$wideWidth != null : !this$wideWidth.equals(other$wideWidth)) return false;
            final java.lang.Object this$barcodeType = this.getBarcodeType();
            final java.lang.Object other$barcodeType = other.getBarcodeType();
            if (this$barcodeType == null ? other$barcodeType != null : !this$barcodeType.equals(other$barcodeType)) return false;
            final java.lang.Object this$content = this.getContent();
            final java.lang.Object other$content = other.getContent();
            if (this$content == null ? other$content != null : !this$content.equals(other$content)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof LabelElementDTO.BarcodeConfig;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $height = this.getHeight();
            result = result * PRIME + ($height == null ? 43 : $height.hashCode());
            final java.lang.Object $showText = this.getShowText();
            result = result * PRIME + ($showText == null ? 43 : $showText.hashCode());
            final java.lang.Object $textPosition = this.getTextPosition();
            result = result * PRIME + ($textPosition == null ? 43 : $textPosition.hashCode());
            final java.lang.Object $narrowWidth = this.getNarrowWidth();
            result = result * PRIME + ($narrowWidth == null ? 43 : $narrowWidth.hashCode());
            final java.lang.Object $wideWidth = this.getWideWidth();
            result = result * PRIME + ($wideWidth == null ? 43 : $wideWidth.hashCode());
            final java.lang.Object $barcodeType = this.getBarcodeType();
            result = result * PRIME + ($barcodeType == null ? 43 : $barcodeType.hashCode());
            final java.lang.Object $content = this.getContent();
            result = result * PRIME + ($content == null ? 43 : $content.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "LabelElementDTO.BarcodeConfig(barcodeType=" + this.getBarcodeType() + ", content=" + this.getContent() + ", height=" + this.getHeight() + ", showText=" + this.getShowText() + ", textPosition=" + this.getTextPosition() + ", narrowWidth=" + this.getNarrowWidth() + ", wideWidth=" + this.getWideWidth() + ")";
        }
    }


    /**
     * 二维码配置
     */
    public static class QRCodeConfig {
        /**
         * 二维码内容
         */
        private String content;
        /**
         * 模块宽度（1-10）
         */
        private Integer moduleSize = 4;
        /**
         * 纠错等级：L-7%, M-15%, Q-25%, H-30%
         */
        private String errorLevel = "M";
        /**
         * 编码模式：A-自动, M-混合, N-数字
         */
        private String encodeMode = "A";
        /**
         * 掩码模式：0-8, -1表示自动
         */
        private Integer maskMode = -1;
        /**
         * 二维码大小（点数，可选，自动计算）
         */
        private Integer size;

        public QRCodeConfig() {
        }

        /**
         * 二维码内容
         */
        public String getContent() {
            return this.content;
        }

        /**
         * 模块宽度（1-10）
         */
        public Integer getModuleSize() {
            return this.moduleSize;
        }

        /**
         * 纠错等级：L-7%, M-15%, Q-25%, H-30%
         */
        public String getErrorLevel() {
            return this.errorLevel;
        }

        /**
         * 编码模式：A-自动, M-混合, N-数字
         */
        public String getEncodeMode() {
            return this.encodeMode;
        }

        /**
         * 掩码模式：0-8, -1表示自动
         */
        public Integer getMaskMode() {
            return this.maskMode;
        }

        /**
         * 二维码大小（点数，可选，自动计算）
         */
        public Integer getSize() {
            return this.size;
        }

        /**
         * 二维码内容
         */
        public void setContent(final String content) {
            this.content = content;
        }

        /**
         * 模块宽度（1-10）
         */
        public void setModuleSize(final Integer moduleSize) {
            this.moduleSize = moduleSize;
        }

        /**
         * 纠错等级：L-7%, M-15%, Q-25%, H-30%
         */
        public void setErrorLevel(final String errorLevel) {
            this.errorLevel = errorLevel;
        }

        /**
         * 编码模式：A-自动, M-混合, N-数字
         */
        public void setEncodeMode(final String encodeMode) {
            this.encodeMode = encodeMode;
        }

        /**
         * 掩码模式：0-8, -1表示自动
         */
        public void setMaskMode(final Integer maskMode) {
            this.maskMode = maskMode;
        }

        /**
         * 二维码大小（点数，可选，自动计算）
         */
        public void setSize(final Integer size) {
            this.size = size;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof LabelElementDTO.QRCodeConfig)) return false;
            final LabelElementDTO.QRCodeConfig other = (LabelElementDTO.QRCodeConfig) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$moduleSize = this.getModuleSize();
            final java.lang.Object other$moduleSize = other.getModuleSize();
            if (this$moduleSize == null ? other$moduleSize != null : !this$moduleSize.equals(other$moduleSize)) return false;
            final java.lang.Object this$maskMode = this.getMaskMode();
            final java.lang.Object other$maskMode = other.getMaskMode();
            if (this$maskMode == null ? other$maskMode != null : !this$maskMode.equals(other$maskMode)) return false;
            final java.lang.Object this$size = this.getSize();
            final java.lang.Object other$size = other.getSize();
            if (this$size == null ? other$size != null : !this$size.equals(other$size)) return false;
            final java.lang.Object this$content = this.getContent();
            final java.lang.Object other$content = other.getContent();
            if (this$content == null ? other$content != null : !this$content.equals(other$content)) return false;
            final java.lang.Object this$errorLevel = this.getErrorLevel();
            final java.lang.Object other$errorLevel = other.getErrorLevel();
            if (this$errorLevel == null ? other$errorLevel != null : !this$errorLevel.equals(other$errorLevel)) return false;
            final java.lang.Object this$encodeMode = this.getEncodeMode();
            final java.lang.Object other$encodeMode = other.getEncodeMode();
            if (this$encodeMode == null ? other$encodeMode != null : !this$encodeMode.equals(other$encodeMode)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof LabelElementDTO.QRCodeConfig;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $moduleSize = this.getModuleSize();
            result = result * PRIME + ($moduleSize == null ? 43 : $moduleSize.hashCode());
            final java.lang.Object $maskMode = this.getMaskMode();
            result = result * PRIME + ($maskMode == null ? 43 : $maskMode.hashCode());
            final java.lang.Object $size = this.getSize();
            result = result * PRIME + ($size == null ? 43 : $size.hashCode());
            final java.lang.Object $content = this.getContent();
            result = result * PRIME + ($content == null ? 43 : $content.hashCode());
            final java.lang.Object $errorLevel = this.getErrorLevel();
            result = result * PRIME + ($errorLevel == null ? 43 : $errorLevel.hashCode());
            final java.lang.Object $encodeMode = this.getEncodeMode();
            result = result * PRIME + ($encodeMode == null ? 43 : $encodeMode.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "LabelElementDTO.QRCodeConfig(content=" + this.getContent() + ", moduleSize=" + this.getModuleSize() + ", errorLevel=" + this.getErrorLevel() + ", encodeMode=" + this.getEncodeMode() + ", maskMode=" + this.getMaskMode() + ", size=" + this.getSize() + ")";
        }
    }


    /**
     * 线条配置
     */
    public static class LineConfig {
        /**
         * 线条宽度（点数）
         */
        private Integer lineWidth = 2;
        /**
         * 线条长度（点数）
         */
        private Integer length;
        /**
         * 线条颜色（黑白打印机仅支持黑色）
         */
        private String color = "#000000";
        /**
         * 线条方向：horizontal, vertical
         */
        private String direction = "horizontal";

        public LineConfig() {
        }

        /**
         * 线条宽度（点数）
         */
        public Integer getLineWidth() {
            return this.lineWidth;
        }

        /**
         * 线条长度（点数）
         */
        public Integer getLength() {
            return this.length;
        }

        /**
         * 线条颜色（黑白打印机仅支持黑色）
         */
        public String getColor() {
            return this.color;
        }

        /**
         * 线条方向：horizontal, vertical
         */
        public String getDirection() {
            return this.direction;
        }

        /**
         * 线条宽度（点数）
         */
        public void setLineWidth(final Integer lineWidth) {
            this.lineWidth = lineWidth;
        }

        /**
         * 线条长度（点数）
         */
        public void setLength(final Integer length) {
            this.length = length;
        }

        /**
         * 线条颜色（黑白打印机仅支持黑色）
         */
        public void setColor(final String color) {
            this.color = color;
        }

        /**
         * 线条方向：horizontal, vertical
         */
        public void setDirection(final String direction) {
            this.direction = direction;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof LabelElementDTO.LineConfig)) return false;
            final LabelElementDTO.LineConfig other = (LabelElementDTO.LineConfig) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$lineWidth = this.getLineWidth();
            final java.lang.Object other$lineWidth = other.getLineWidth();
            if (this$lineWidth == null ? other$lineWidth != null : !this$lineWidth.equals(other$lineWidth)) return false;
            final java.lang.Object this$length = this.getLength();
            final java.lang.Object other$length = other.getLength();
            if (this$length == null ? other$length != null : !this$length.equals(other$length)) return false;
            final java.lang.Object this$color = this.getColor();
            final java.lang.Object other$color = other.getColor();
            if (this$color == null ? other$color != null : !this$color.equals(other$color)) return false;
            final java.lang.Object this$direction = this.getDirection();
            final java.lang.Object other$direction = other.getDirection();
            if (this$direction == null ? other$direction != null : !this$direction.equals(other$direction)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof LabelElementDTO.LineConfig;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $lineWidth = this.getLineWidth();
            result = result * PRIME + ($lineWidth == null ? 43 : $lineWidth.hashCode());
            final java.lang.Object $length = this.getLength();
            result = result * PRIME + ($length == null ? 43 : $length.hashCode());
            final java.lang.Object $color = this.getColor();
            result = result * PRIME + ($color == null ? 43 : $color.hashCode());
            final java.lang.Object $direction = this.getDirection();
            result = result * PRIME + ($direction == null ? 43 : $direction.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "LabelElementDTO.LineConfig(lineWidth=" + this.getLineWidth() + ", length=" + this.getLength() + ", color=" + this.getColor() + ", direction=" + this.getDirection() + ")";
        }
    }


    /**
     * 矩形框配置
     */
    public static class BoxConfig {
        /**
         * 线条宽度（点数）
         */
        private Integer lineWidth = 2;
        /**
         * 填充颜色（黑白打印机仅支持黑白）
         */
        private String fillColor;
        /**
         * 边框颜色
         */
        private String borderColor = "#000000";
        /**
         * 圆角半径（点数）
         */
        private Integer cornerRadius = 0;

        public BoxConfig() {
        }

        /**
         * 线条宽度（点数）
         */
        public Integer getLineWidth() {
            return this.lineWidth;
        }

        /**
         * 填充颜色（黑白打印机仅支持黑白）
         */
        public String getFillColor() {
            return this.fillColor;
        }

        /**
         * 边框颜色
         */
        public String getBorderColor() {
            return this.borderColor;
        }

        /**
         * 圆角半径（点数）
         */
        public Integer getCornerRadius() {
            return this.cornerRadius;
        }

        /**
         * 线条宽度（点数）
         */
        public void setLineWidth(final Integer lineWidth) {
            this.lineWidth = lineWidth;
        }

        /**
         * 填充颜色（黑白打印机仅支持黑白）
         */
        public void setFillColor(final String fillColor) {
            this.fillColor = fillColor;
        }

        /**
         * 边框颜色
         */
        public void setBorderColor(final String borderColor) {
            this.borderColor = borderColor;
        }

        /**
         * 圆角半径（点数）
         */
        public void setCornerRadius(final Integer cornerRadius) {
            this.cornerRadius = cornerRadius;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof LabelElementDTO.BoxConfig)) return false;
            final LabelElementDTO.BoxConfig other = (LabelElementDTO.BoxConfig) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$lineWidth = this.getLineWidth();
            final java.lang.Object other$lineWidth = other.getLineWidth();
            if (this$lineWidth == null ? other$lineWidth != null : !this$lineWidth.equals(other$lineWidth)) return false;
            final java.lang.Object this$cornerRadius = this.getCornerRadius();
            final java.lang.Object other$cornerRadius = other.getCornerRadius();
            if (this$cornerRadius == null ? other$cornerRadius != null : !this$cornerRadius.equals(other$cornerRadius)) return false;
            final java.lang.Object this$fillColor = this.getFillColor();
            final java.lang.Object other$fillColor = other.getFillColor();
            if (this$fillColor == null ? other$fillColor != null : !this$fillColor.equals(other$fillColor)) return false;
            final java.lang.Object this$borderColor = this.getBorderColor();
            final java.lang.Object other$borderColor = other.getBorderColor();
            if (this$borderColor == null ? other$borderColor != null : !this$borderColor.equals(other$borderColor)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof LabelElementDTO.BoxConfig;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $lineWidth = this.getLineWidth();
            result = result * PRIME + ($lineWidth == null ? 43 : $lineWidth.hashCode());
            final java.lang.Object $cornerRadius = this.getCornerRadius();
            result = result * PRIME + ($cornerRadius == null ? 43 : $cornerRadius.hashCode());
            final java.lang.Object $fillColor = this.getFillColor();
            result = result * PRIME + ($fillColor == null ? 43 : $fillColor.hashCode());
            final java.lang.Object $borderColor = this.getBorderColor();
            result = result * PRIME + ($borderColor == null ? 43 : $borderColor.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "LabelElementDTO.BoxConfig(lineWidth=" + this.getLineWidth() + ", fillColor=" + this.getFillColor() + ", borderColor=" + this.getBorderColor() + ", cornerRadius=" + this.getCornerRadius() + ")";
        }
    }


    /**
     * 数据绑定配置
     */
    public static class DataBindingConfig {
        /**
         * 数据字段名
         * 支持的字段：materialName, storeName, shelfLifeDays, generateTime,
         * expiryDate, supplierName, traceCode, productionDate, weight, weightUnit
         */
        private String field;
        /**
         * 数据格式化模式
         * 日期：yyyy-MM-dd, MM-dd, yyyy/MM/dd
         * 数字：#,##0.00
         */
        private String format;
        /**
         * 前缀文本
         */
        private String prefix;
        /**
         * 后缀文本
         */
        private String suffix;
        /**
         * 默认值（字段为空时使用）
         */
        private String defaultValue;
        /**
         * 空值时是否隐藏元素
         */
        private Boolean hideIfEmpty = false;

        public DataBindingConfig() {
        }

        /**
         * 数据字段名
         * 支持的字段：materialName, storeName, shelfLifeDays, generateTime,
         * expiryDate, supplierName, traceCode, productionDate, weight, weightUnit
         */
        public String getField() {
            return this.field;
        }

        /**
         * 数据格式化模式
         * 日期：yyyy-MM-dd, MM-dd, yyyy/MM/dd
         * 数字：#,##0.00
         */
        public String getFormat() {
            return this.format;
        }

        /**
         * 前缀文本
         */
        public String getPrefix() {
            return this.prefix;
        }

        /**
         * 后缀文本
         */
        public String getSuffix() {
            return this.suffix;
        }

        /**
         * 默认值（字段为空时使用）
         */
        public String getDefaultValue() {
            return this.defaultValue;
        }

        /**
         * 空值时是否隐藏元素
         */
        public Boolean getHideIfEmpty() {
            return this.hideIfEmpty;
        }

        /**
         * 数据字段名
         * 支持的字段：materialName, storeName, shelfLifeDays, generateTime,
         * expiryDate, supplierName, traceCode, productionDate, weight, weightUnit
         */
        public void setField(final String field) {
            this.field = field;
        }

        /**
         * 数据格式化模式
         * 日期：yyyy-MM-dd, MM-dd, yyyy/MM/dd
         * 数字：#,##0.00
         */
        public void setFormat(final String format) {
            this.format = format;
        }

        /**
         * 前缀文本
         */
        public void setPrefix(final String prefix) {
            this.prefix = prefix;
        }

        /**
         * 后缀文本
         */
        public void setSuffix(final String suffix) {
            this.suffix = suffix;
        }

        /**
         * 默认值（字段为空时使用）
         */
        public void setDefaultValue(final String defaultValue) {
            this.defaultValue = defaultValue;
        }

        /**
         * 空值时是否隐藏元素
         */
        public void setHideIfEmpty(final Boolean hideIfEmpty) {
            this.hideIfEmpty = hideIfEmpty;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof LabelElementDTO.DataBindingConfig)) return false;
            final LabelElementDTO.DataBindingConfig other = (LabelElementDTO.DataBindingConfig) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$hideIfEmpty = this.getHideIfEmpty();
            final java.lang.Object other$hideIfEmpty = other.getHideIfEmpty();
            if (this$hideIfEmpty == null ? other$hideIfEmpty != null : !this$hideIfEmpty.equals(other$hideIfEmpty)) return false;
            final java.lang.Object this$field = this.getField();
            final java.lang.Object other$field = other.getField();
            if (this$field == null ? other$field != null : !this$field.equals(other$field)) return false;
            final java.lang.Object this$format = this.getFormat();
            final java.lang.Object other$format = other.getFormat();
            if (this$format == null ? other$format != null : !this$format.equals(other$format)) return false;
            final java.lang.Object this$prefix = this.getPrefix();
            final java.lang.Object other$prefix = other.getPrefix();
            if (this$prefix == null ? other$prefix != null : !this$prefix.equals(other$prefix)) return false;
            final java.lang.Object this$suffix = this.getSuffix();
            final java.lang.Object other$suffix = other.getSuffix();
            if (this$suffix == null ? other$suffix != null : !this$suffix.equals(other$suffix)) return false;
            final java.lang.Object this$defaultValue = this.getDefaultValue();
            final java.lang.Object other$defaultValue = other.getDefaultValue();
            if (this$defaultValue == null ? other$defaultValue != null : !this$defaultValue.equals(other$defaultValue)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof LabelElementDTO.DataBindingConfig;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $hideIfEmpty = this.getHideIfEmpty();
            result = result * PRIME + ($hideIfEmpty == null ? 43 : $hideIfEmpty.hashCode());
            final java.lang.Object $field = this.getField();
            result = result * PRIME + ($field == null ? 43 : $field.hashCode());
            final java.lang.Object $format = this.getFormat();
            result = result * PRIME + ($format == null ? 43 : $format.hashCode());
            final java.lang.Object $prefix = this.getPrefix();
            result = result * PRIME + ($prefix == null ? 43 : $prefix.hashCode());
            final java.lang.Object $suffix = this.getSuffix();
            result = result * PRIME + ($suffix == null ? 43 : $suffix.hashCode());
            final java.lang.Object $defaultValue = this.getDefaultValue();
            result = result * PRIME + ($defaultValue == null ? 43 : $defaultValue.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "LabelElementDTO.DataBindingConfig(field=" + this.getField() + ", format=" + this.getFormat() + ", prefix=" + this.getPrefix() + ", suffix=" + this.getSuffix() + ", defaultValue=" + this.getDefaultValue() + ", hideIfEmpty=" + this.getHideIfEmpty() + ")";
        }
    }

    public LabelElementDTO() {
    }

    /**
     * 元素ID
     */
    public String getId() {
        return this.id;
    }

    /**
     * 元素类型：TEXT-文本, BARCODE-条码, QRCODE-二维码, LINE-线条, BOX-矩形框
     */
    public ElementType getType() {
        return this.type;
    }

    /**
     * 元素位置配置
     */
    public PositionConfig getPosition() {
        return this.position;
    }

    /**
     * 文本配置（type=TEXT时有效）
     */
    public TextConfig getText() {
        return this.text;
    }

    /**
     * 条码配置（type=BARCODE时有效）
     */
    public BarcodeConfig getBarcode() {
        return this.barcode;
    }

    /**
     * 二维码配置（type=QRCODE时有效）
     */
    public QRCodeConfig getQrcode() {
        return this.qrcode;
    }

    /**
     * 线条配置（type=LINE时有效）
     */
    public LineConfig getLine() {
        return this.line;
    }

    /**
     * 矩形框配置（type=BOX时有效）
     */
    public BoxConfig getBox() {
        return this.box;
    }

    /**
     * 数据绑定配置
     */
    public DataBindingConfig getDataBinding() {
        return this.dataBinding;
    }

    /**
     * 是否可见
     */
    public Boolean getVisible() {
        return this.visible;
    }

    /**
     * 元素ID
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * 元素类型：TEXT-文本, BARCODE-条码, QRCODE-二维码, LINE-线条, BOX-矩形框
     */
    public void setType(final ElementType type) {
        this.type = type;
    }

    /**
     * 元素位置配置
     */
    public void setPosition(final PositionConfig position) {
        this.position = position;
    }

    /**
     * 文本配置（type=TEXT时有效）
     */
    public void setText(final TextConfig text) {
        this.text = text;
    }

    /**
     * 条码配置（type=BARCODE时有效）
     */
    public void setBarcode(final BarcodeConfig barcode) {
        this.barcode = barcode;
    }

    /**
     * 二维码配置（type=QRCODE时有效）
     */
    public void setQrcode(final QRCodeConfig qrcode) {
        this.qrcode = qrcode;
    }

    /**
     * 线条配置（type=LINE时有效）
     */
    public void setLine(final LineConfig line) {
        this.line = line;
    }

    /**
     * 矩形框配置（type=BOX时有效）
     */
    public void setBox(final BoxConfig box) {
        this.box = box;
    }

    /**
     * 数据绑定配置
     */
    public void setDataBinding(final DataBindingConfig dataBinding) {
        this.dataBinding = dataBinding;
    }

    /**
     * 是否可见
     */
    public void setVisible(final Boolean visible) {
        this.visible = visible;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof LabelElementDTO)) return false;
        final LabelElementDTO other = (LabelElementDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$visible = this.getVisible();
        final java.lang.Object other$visible = other.getVisible();
        if (this$visible == null ? other$visible != null : !this$visible.equals(other$visible)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$type = this.getType();
        final java.lang.Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        final java.lang.Object this$position = this.getPosition();
        final java.lang.Object other$position = other.getPosition();
        if (this$position == null ? other$position != null : !this$position.equals(other$position)) return false;
        final java.lang.Object this$text = this.getText();
        final java.lang.Object other$text = other.getText();
        if (this$text == null ? other$text != null : !this$text.equals(other$text)) return false;
        final java.lang.Object this$barcode = this.getBarcode();
        final java.lang.Object other$barcode = other.getBarcode();
        if (this$barcode == null ? other$barcode != null : !this$barcode.equals(other$barcode)) return false;
        final java.lang.Object this$qrcode = this.getQrcode();
        final java.lang.Object other$qrcode = other.getQrcode();
        if (this$qrcode == null ? other$qrcode != null : !this$qrcode.equals(other$qrcode)) return false;
        final java.lang.Object this$line = this.getLine();
        final java.lang.Object other$line = other.getLine();
        if (this$line == null ? other$line != null : !this$line.equals(other$line)) return false;
        final java.lang.Object this$box = this.getBox();
        final java.lang.Object other$box = other.getBox();
        if (this$box == null ? other$box != null : !this$box.equals(other$box)) return false;
        final java.lang.Object this$dataBinding = this.getDataBinding();
        final java.lang.Object other$dataBinding = other.getDataBinding();
        if (this$dataBinding == null ? other$dataBinding != null : !this$dataBinding.equals(other$dataBinding)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LabelElementDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $visible = this.getVisible();
        result = result * PRIME + ($visible == null ? 43 : $visible.hashCode());
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final java.lang.Object $position = this.getPosition();
        result = result * PRIME + ($position == null ? 43 : $position.hashCode());
        final java.lang.Object $text = this.getText();
        result = result * PRIME + ($text == null ? 43 : $text.hashCode());
        final java.lang.Object $barcode = this.getBarcode();
        result = result * PRIME + ($barcode == null ? 43 : $barcode.hashCode());
        final java.lang.Object $qrcode = this.getQrcode();
        result = result * PRIME + ($qrcode == null ? 43 : $qrcode.hashCode());
        final java.lang.Object $line = this.getLine();
        result = result * PRIME + ($line == null ? 43 : $line.hashCode());
        final java.lang.Object $box = this.getBox();
        result = result * PRIME + ($box == null ? 43 : $box.hashCode());
        final java.lang.Object $dataBinding = this.getDataBinding();
        result = result * PRIME + ($dataBinding == null ? 43 : $dataBinding.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "LabelElementDTO(id=" + this.getId() + ", type=" + this.getType() + ", position=" + this.getPosition() + ", text=" + this.getText() + ", barcode=" + this.getBarcode() + ", qrcode=" + this.getQrcode() + ", line=" + this.getLine() + ", box=" + this.getBox() + ", dataBinding=" + this.getDataBinding() + ", visible=" + this.getVisible() + ")";
    }
}
