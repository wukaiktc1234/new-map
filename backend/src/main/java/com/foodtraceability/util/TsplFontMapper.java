package com.foodtraceability.util;

import java.util.Arrays;
import java.util.List;

/**
 * TSPL 字体映射工具类
 * 统一设计界面、预览和打印的字体大小计算
 *
 * TSPL TSS24.BF2 是 24x24 点阵字体
 * 打印机 DPI = 300 (11.8 dots/mm * 25.4 mm/inch ≈ 300)
 *
 * 字体大小映射规则（与官方编辑器一致）：
 * 字号列表：4,5,6,7,8,9,10,11,12,13,14,16,18,20,24,28,32,48,56,72
 *
 * fontSize 直接对应点数，scale = ceil(fontSize / 24)
 */
public class TsplFontMapper {

    /** 打印机 DPI (11.8 dots/mm * 25.4 mm/inch ≈ 299.72) */
    public static final int PRINTER_DPI = 300;

    /** 屏幕 DPI (标准显示器的 DPI) */
    public static final int SCREEN_DPI = 96;

    public static final int BASE_FONT_DOTS = 24;

    /**
     * 官方编辑器支持的字号列表
     * 这些值对应实际打印的点数
     */
    public static final List<Integer> FONT_SIZES = Arrays.asList(
        4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 24, 28, 32, 48, 56, 72
    );

    /**
     * 根据设计界面的fontSize获取TSPL缩放级别
     * fontSize是点数，scale = ceil(fontSize / 24)
     */
    public static int fontSizeToScale(int fontSize) {
        if (fontSize < 4) fontSize = 4;
        if (fontSize > 72) fontSize = 72;
        return (int) Math.ceil(fontSize / 24.0);
    }

    /**
     * 获取预览图像中的字体像素大小
     * 使用fontSize直接作为点数，转换为屏幕像素
     * pixels = fontSize * previewDpi / PRINTER_DPI
     */
    public static int fontSizeToPixels(int fontSize, int previewDpi) {
        if (fontSize < 4) fontSize = 4;
        if (fontSize > 72) fontSize = 72;
        // fontSize直接就是点数，转换为像素
        return (int) Math.round(fontSize * previewDpi / (double) PRINTER_DPI);
    }

    /**
     * 获取预览图像中的字体像素大小 (使用默认屏幕DPI 96)
     */
    public static int fontSizeToPixels(int fontSize) {
        return fontSizeToPixels(fontSize, SCREEN_DPI);
    }

    /**
     * 获取实际打印的字体点数
     */
    public static int fontSizeToDots(int fontSize) {
        if (fontSize < 4) fontSize = 4;
        if (fontSize > 72) fontSize = 72;
        return fontSize;
    }

    /**
     * 获取fontSize对应的实际打印高度(mm)
     */
    public static double fontSizeToMm(int fontSize) {
        int dots = fontSizeToDots(fontSize);
        return dots / (double) PRINTER_DPI * 25.4;
    }

    /**
     * mm 转换为 dots
     */
    public static int mmToDots(double mm) {
        return (int) Math.round(mm * PRINTER_DPI / 25.4);
    }

    /**
     * dots 转换为 mm
     */
    public static double dotsToMm(int dots) {
        return dots * 25.4 / PRINTER_DPI;
    }

    /**
     * dots 转换为像素 (用于预览)
     */
    public static int dotsToPixels(int dots, int previewDpi) {
        return (int) Math.round(dots * previewDpi / (double) PRINTER_DPI);
    }

    /**
     * dots 转换为像素 (使用默认屏幕DPI)
     */
    public static int dotsToPixels(int dots) {
        return dotsToPixels(dots, SCREEN_DPI);
    }

    /**
     * 获取粗体偏移量
     */
    public static int getBoldOffsetDots() {
        return 1;
    }

    /**
     * 根据 scale 获取实际字体点数
     */
    public static int scaleToDots(int scale) {
        return BASE_FONT_DOTS * scale;
    }

    /**
     * 根据实际需要的字体大小 (dots) 计算 scale
     */
    public static int dotsToScale(int dots) {
        return (int) Math.ceil(dots / (double) BASE_FONT_DOTS);
    }
}
