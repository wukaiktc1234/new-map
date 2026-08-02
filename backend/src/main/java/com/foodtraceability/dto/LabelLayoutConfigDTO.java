package com.foodtraceability.dto;

import java.util.List;

/**
 * 标签布局配置DTO
 * 用于定义标签的完整布局结构
 */
public class LabelLayoutConfigDTO {
    /**
     * 标签尺寸配置
     */
    private LabelSizeConfig size;
    /**
     * 打印配置
     */
    private PrintConfig print;
    /**
     * 元素列表
     */
    private List<LabelElementDTO> elements;
    /**
     * 背景设置
     */
    private BackgroundConfig background;


    /**
     * 标签尺寸配置
     */
    public static class LabelSizeConfig {
        /**
         * 宽度（mm）
         */
        private Integer width;
        /**
         * 高度（mm）
         */
        private Integer height;
        /**
         * DPI
         */
        private Integer dpi;
        /**
         * 间隙（mm）
         */
        private Integer gap;

        /**
         * 获取宽度点数
         */
        public Integer getWidthDots() {
            return width != null && dpi != null ? (int) (width * dpi / 25.4) : 472;
        }

        /**
         * 获取高度点数
         */
        public Integer getHeightDots() {
            return height != null && dpi != null ? (int) (height * dpi / 25.4) : 354;
        }

        /**
         * 获取间隙点数
         */
        public Integer getGapDots() {
            return gap != null && dpi != null ? (int) (gap * dpi / 25.4) : 24;
        }

        public LabelSizeConfig() {
        }

        /**
         * 宽度（mm）
         */
        public Integer getWidth() {
            return this.width;
        }

        /**
         * 高度（mm）
         */
        public Integer getHeight() {
            return this.height;
        }

        /**
         * DPI
         */
        public Integer getDpi() {
            return this.dpi;
        }

        /**
         * 间隙（mm）
         */
        public Integer getGap() {
            return this.gap;
        }

        /**
         * 宽度（mm）
         */
        public void setWidth(final Integer width) {
            this.width = width;
        }

        /**
         * 高度（mm）
         */
        public void setHeight(final Integer height) {
            this.height = height;
        }

        /**
         * DPI
         */
        public void setDpi(final Integer dpi) {
            this.dpi = dpi;
        }

        /**
         * 间隙（mm）
         */
        public void setGap(final Integer gap) {
            this.gap = gap;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof LabelLayoutConfigDTO.LabelSizeConfig)) return false;
            final LabelLayoutConfigDTO.LabelSizeConfig other = (LabelLayoutConfigDTO.LabelSizeConfig) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$width = this.getWidth();
            final java.lang.Object other$width = other.getWidth();
            if (this$width == null ? other$width != null : !this$width.equals(other$width)) return false;
            final java.lang.Object this$height = this.getHeight();
            final java.lang.Object other$height = other.getHeight();
            if (this$height == null ? other$height != null : !this$height.equals(other$height)) return false;
            final java.lang.Object this$dpi = this.getDpi();
            final java.lang.Object other$dpi = other.getDpi();
            if (this$dpi == null ? other$dpi != null : !this$dpi.equals(other$dpi)) return false;
            final java.lang.Object this$gap = this.getGap();
            final java.lang.Object other$gap = other.getGap();
            if (this$gap == null ? other$gap != null : !this$gap.equals(other$gap)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof LabelLayoutConfigDTO.LabelSizeConfig;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $width = this.getWidth();
            result = result * PRIME + ($width == null ? 43 : $width.hashCode());
            final java.lang.Object $height = this.getHeight();
            result = result * PRIME + ($height == null ? 43 : $height.hashCode());
            final java.lang.Object $dpi = this.getDpi();
            result = result * PRIME + ($dpi == null ? 43 : $dpi.hashCode());
            final java.lang.Object $gap = this.getGap();
            result = result * PRIME + ($gap == null ? 43 : $gap.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "LabelLayoutConfigDTO.LabelSizeConfig(width=" + this.getWidth() + ", height=" + this.getHeight() + ", dpi=" + this.getDpi() + ", gap=" + this.getGap() + ")";
        }
    }


    /**
     * 打印配置
     */
    public static class PrintConfig {
        /**
         * 打印速度
         */
        private Integer speed = 3;
        /**
         * 打印浓度
         */
        private Integer density = 12;
        /**
         * 打印方向
         */
        private Integer direction = 1;
        /**
         * 打印份数
         */
        private Integer copies = 1;
        /**
         * 是否启用剥离模式
         */
        private Boolean peelMode = false;
        /**
         * 剥离延迟（毫秒）
         */
        private Integer peelDelay = 500;

        public PrintConfig() {
        }

        /**
         * 打印速度
         */
        public Integer getSpeed() {
            return this.speed;
        }

        /**
         * 打印浓度
         */
        public Integer getDensity() {
            return this.density;
        }

        /**
         * 打印方向
         */
        public Integer getDirection() {
            return this.direction;
        }

        /**
         * 打印份数
         */
        public Integer getCopies() {
            return this.copies;
        }

        /**
         * 是否启用剥离模式
         */
        public Boolean getPeelMode() {
            return this.peelMode;
        }

        /**
         * 剥离延迟（毫秒）
         */
        public Integer getPeelDelay() {
            return this.peelDelay;
        }

        /**
         * 打印速度
         */
        public void setSpeed(final Integer speed) {
            this.speed = speed;
        }

        /**
         * 打印浓度
         */
        public void setDensity(final Integer density) {
            this.density = density;
        }

        /**
         * 打印方向
         */
        public void setDirection(final Integer direction) {
            this.direction = direction;
        }

        /**
         * 打印份数
         */
        public void setCopies(final Integer copies) {
            this.copies = copies;
        }

        /**
         * 是否启用剥离模式
         */
        public void setPeelMode(final Boolean peelMode) {
            this.peelMode = peelMode;
        }

        /**
         * 剥离延迟（毫秒）
         */
        public void setPeelDelay(final Integer peelDelay) {
            this.peelDelay = peelDelay;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof LabelLayoutConfigDTO.PrintConfig)) return false;
            final LabelLayoutConfigDTO.PrintConfig other = (LabelLayoutConfigDTO.PrintConfig) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$speed = this.getSpeed();
            final java.lang.Object other$speed = other.getSpeed();
            if (this$speed == null ? other$speed != null : !this$speed.equals(other$speed)) return false;
            final java.lang.Object this$density = this.getDensity();
            final java.lang.Object other$density = other.getDensity();
            if (this$density == null ? other$density != null : !this$density.equals(other$density)) return false;
            final java.lang.Object this$direction = this.getDirection();
            final java.lang.Object other$direction = other.getDirection();
            if (this$direction == null ? other$direction != null : !this$direction.equals(other$direction)) return false;
            final java.lang.Object this$copies = this.getCopies();
            final java.lang.Object other$copies = other.getCopies();
            if (this$copies == null ? other$copies != null : !this$copies.equals(other$copies)) return false;
            final java.lang.Object this$peelMode = this.getPeelMode();
            final java.lang.Object other$peelMode = other.getPeelMode();
            if (this$peelMode == null ? other$peelMode != null : !this$peelMode.equals(other$peelMode)) return false;
            final java.lang.Object this$peelDelay = this.getPeelDelay();
            final java.lang.Object other$peelDelay = other.getPeelDelay();
            if (this$peelDelay == null ? other$peelDelay != null : !this$peelDelay.equals(other$peelDelay)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof LabelLayoutConfigDTO.PrintConfig;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $speed = this.getSpeed();
            result = result * PRIME + ($speed == null ? 43 : $speed.hashCode());
            final java.lang.Object $density = this.getDensity();
            result = result * PRIME + ($density == null ? 43 : $density.hashCode());
            final java.lang.Object $direction = this.getDirection();
            result = result * PRIME + ($direction == null ? 43 : $direction.hashCode());
            final java.lang.Object $copies = this.getCopies();
            result = result * PRIME + ($copies == null ? 43 : $copies.hashCode());
            final java.lang.Object $peelMode = this.getPeelMode();
            result = result * PRIME + ($peelMode == null ? 43 : $peelMode.hashCode());
            final java.lang.Object $peelDelay = this.getPeelDelay();
            result = result * PRIME + ($peelDelay == null ? 43 : $peelDelay.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "LabelLayoutConfigDTO.PrintConfig(speed=" + this.getSpeed() + ", density=" + this.getDensity() + ", direction=" + this.getDirection() + ", copies=" + this.getCopies() + ", peelMode=" + this.getPeelMode() + ", peelDelay=" + this.getPeelDelay() + ")";
        }
    }


    /**
     * 背景配置
     */
    public static class BackgroundConfig {
        /**
         * 背景颜色（仅预览用）
         */
        private String color = "#FFFFFF";
        /**
         * 边框宽度
         */
        private Integer borderWidth = 1;
        /**
         * 边框颜色
         */
        private String borderColor = "#000000";

        public BackgroundConfig() {
        }

        /**
         * 背景颜色（仅预览用）
         */
        public String getColor() {
            return this.color;
        }

        /**
         * 边框宽度
         */
        public Integer getBorderWidth() {
            return this.borderWidth;
        }

        /**
         * 边框颜色
         */
        public String getBorderColor() {
            return this.borderColor;
        }

        /**
         * 背景颜色（仅预览用）
         */
        public void setColor(final String color) {
            this.color = color;
        }

        /**
         * 边框宽度
         */
        public void setBorderWidth(final Integer borderWidth) {
            this.borderWidth = borderWidth;
        }

        /**
         * 边框颜色
         */
        public void setBorderColor(final String borderColor) {
            this.borderColor = borderColor;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof LabelLayoutConfigDTO.BackgroundConfig)) return false;
            final LabelLayoutConfigDTO.BackgroundConfig other = (LabelLayoutConfigDTO.BackgroundConfig) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$borderWidth = this.getBorderWidth();
            final java.lang.Object other$borderWidth = other.getBorderWidth();
            if (this$borderWidth == null ? other$borderWidth != null : !this$borderWidth.equals(other$borderWidth)) return false;
            final java.lang.Object this$color = this.getColor();
            final java.lang.Object other$color = other.getColor();
            if (this$color == null ? other$color != null : !this$color.equals(other$color)) return false;
            final java.lang.Object this$borderColor = this.getBorderColor();
            final java.lang.Object other$borderColor = other.getBorderColor();
            if (this$borderColor == null ? other$borderColor != null : !this$borderColor.equals(other$borderColor)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof LabelLayoutConfigDTO.BackgroundConfig;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $borderWidth = this.getBorderWidth();
            result = result * PRIME + ($borderWidth == null ? 43 : $borderWidth.hashCode());
            final java.lang.Object $color = this.getColor();
            result = result * PRIME + ($color == null ? 43 : $color.hashCode());
            final java.lang.Object $borderColor = this.getBorderColor();
            result = result * PRIME + ($borderColor == null ? 43 : $borderColor.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "LabelLayoutConfigDTO.BackgroundConfig(color=" + this.getColor() + ", borderWidth=" + this.getBorderWidth() + ", borderColor=" + this.getBorderColor() + ")";
        }
    }

    public LabelLayoutConfigDTO() {
    }

    /**
     * 标签尺寸配置
     */
    public LabelSizeConfig getSize() {
        return this.size;
    }

    /**
     * 打印配置
     */
    public PrintConfig getPrint() {
        return this.print;
    }

    /**
     * 元素列表
     */
    public List<LabelElementDTO> getElements() {
        return this.elements;
    }

    /**
     * 背景设置
     */
    public BackgroundConfig getBackground() {
        return this.background;
    }

    /**
     * 标签尺寸配置
     */
    public void setSize(final LabelSizeConfig size) {
        this.size = size;
    }

    /**
     * 打印配置
     */
    public void setPrint(final PrintConfig print) {
        this.print = print;
    }

    /**
     * 元素列表
     */
    public void setElements(final List<LabelElementDTO> elements) {
        this.elements = elements;
    }

    /**
     * 背景设置
     */
    public void setBackground(final BackgroundConfig background) {
        this.background = background;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof LabelLayoutConfigDTO)) return false;
        final LabelLayoutConfigDTO other = (LabelLayoutConfigDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$size = this.getSize();
        final java.lang.Object other$size = other.getSize();
        if (this$size == null ? other$size != null : !this$size.equals(other$size)) return false;
        final java.lang.Object this$print = this.getPrint();
        final java.lang.Object other$print = other.getPrint();
        if (this$print == null ? other$print != null : !this$print.equals(other$print)) return false;
        final java.lang.Object this$elements = this.getElements();
        final java.lang.Object other$elements = other.getElements();
        if (this$elements == null ? other$elements != null : !this$elements.equals(other$elements)) return false;
        final java.lang.Object this$background = this.getBackground();
        final java.lang.Object other$background = other.getBackground();
        if (this$background == null ? other$background != null : !this$background.equals(other$background)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LabelLayoutConfigDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $size = this.getSize();
        result = result * PRIME + ($size == null ? 43 : $size.hashCode());
        final java.lang.Object $print = this.getPrint();
        result = result * PRIME + ($print == null ? 43 : $print.hashCode());
        final java.lang.Object $elements = this.getElements();
        result = result * PRIME + ($elements == null ? 43 : $elements.hashCode());
        final java.lang.Object $background = this.getBackground();
        result = result * PRIME + ($background == null ? 43 : $background.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "LabelLayoutConfigDTO(size=" + this.getSize() + ", print=" + this.getPrint() + ", elements=" + this.getElements() + ", background=" + this.getBackground() + ")";
    }
}
