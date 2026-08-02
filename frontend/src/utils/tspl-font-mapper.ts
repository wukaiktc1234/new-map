/**
 * TSPL 字体映射工具
 * 完全基于 dots 单位，与追溯码标签打印机保持一致
 *
 * 核心原则:
 * 1. 所有坐标使用 dots (1/300英寸为单位)
 * 2. 打印DPI = 300 (11.8 dots/mm)
 * 3. 字体固定使用 TSS24.BF2，通过 scale 控制大小
 * 4. 设计界面显示 mm，但内部存储 dots
 *
 * 转换公式:
 * - mm 转 dots: dots = mm * 300 / 25.4
 * - dots 转 mm: mm = dots * 25.4 / 300
 */

/** 打印DPI (11.8 dots/mm * 25.4 mm/inch ≈ 299.72) */
export const PRINTER_DPI = 300;

/** 屏幕 DPI (标准显示器的 DPI) */
export const SCREEN_DPI = 96;

/** 基础字体大小 (点阵) - TSS24.BF2 */
export const BASE_FONT_DOTS = 24;

/** 标准字体名称 */
export const STANDARD_FONT = 'TSS24.BF2';

/**
 * 字号列表 - TSPL 官方编辑器一致
 * 这些值对应 fontSize (点数)，会映射到 scale
 */
export const FONT_SIZES = [4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 24, 28, 32, 48, 56, 72] as const;
export type FontSize = typeof FONT_SIZES[number];

/**
 * 字号到 scale 的映射表
 * scale = ceil(fontSize / 24)
 */
export const FONT_SIZE_TO_SCALE: Record<number, number> = {
  4: 1,
  5: 1,
  6: 1,
  7: 1,
  8: 1,
  9: 1,
  10: 1,
  11: 1,
  12: 1,
  13: 1,
  14: 1,
  16: 1,
  18: 1,
  20: 1,
  24: 1,
  28: 2,
  32: 2,
  48: 2,
  56: 3,
  72: 3
};

/**
 * 推荐的字号级别(对应实际打印效果)
 */
export const FONT_SIZE_LEVELS = [
  { size: 8, label: '超小', scale: 1, dots: 24, mm: 2.03 },
  { size: 12, label: '小', scale: 1, dots: 24, mm: 2.03 },
  { size: 16, label: '中小', scale: 1, dots: 24, mm: 2.03 },
  { size: 20, label: '中', scale: 1, dots: 24, mm: 2.03 },
  { size: 24, label: '中大', scale: 1, dots: 24, mm: 2.03 },
  { size: 28, label: '大', scale: 2, dots: 48, mm: 4.06 },
  { size: 32, label: '特大', scale: 2, dots: 48, mm: 4.06 },
  { size: 48, label: '超大', scale: 2, dots: 48, mm: 4.06 },
  { size: 56, label: '巨大', scale: 3, dots: 72, mm: 6.10 },
  { size: 72, label: '超巨', scale: 3, dots: 72, mm: 6.10 }
] as const;

/**
 * mm 转换 dots
 * @param mm - 毫米
 * @returns dots
 */
export function mmToDots(mm: number): number {
  return Math.round(mm * PRINTER_DPI / 25.4);
}

/**
 * dots 转换 mm
 * @param dots - dots
 * @returns 毫米
 */
export function dotsToMm(dots: number): number {
  return dots * 25.4 / PRINTER_DPI;
}

/**
 * 根据 scale 获取实际字体点数
 */
export function scaleToDots(scale: number): number {
  return BASE_FONT_DOTS * scale;
}

/**
 * 根据实际需要的字体大小 (dots) 计算 scale
 */
export function dotsToScale(dots: number): number {
  return Math.ceil(dots / BASE_FONT_DOTS);
}

/**
 * 根据 fontSize 获取 TSPL scale
 * fontSize 是点数，scale = ceil(fontSize / 24)
 */
export function fontSizeToScale(fontSize: number): number {
  if (fontSize < 4) fontSize = 4;
  if (fontSize > 72) fontSize = 72;
  return Math.ceil(fontSize / 24);
}

/**
 * 获取预览图像中的字体像素大小
 * @param fontSize - 设计界面的字体大小(点数)
 * @param previewDpi - 预览 DPI (屏幕通常使用 96)
 */
export function fontSizeToPixels(fontSize: number, previewDpi: number = SCREEN_DPI): number {
  if (fontSize < 4) fontSize = 4;
  if (fontSize > 72) fontSize = 72;
  // fontSize 直接就是点数，转换为像素
  return Math.round(fontSize * previewDpi / PRINTER_DPI);
}

/**
 * 获取实际打印的字体点数
 */
export function fontSizeToDots(fontSize: number): number {
  if (fontSize < 4) fontSize = 4;
  if (fontSize > 72) fontSize = 72;
  return fontSize;
}

/**
 * 获取 fontSize 对应的实际打印高度(mm)
 */
export function fontSizeToMm(fontSize: number): number {
  const dots = fontSizeToDots(fontSize);
  return dots / PRINTER_DPI * 25.4;
}

/**
 * dots 转换为像素(用于预览)
 * @param dots - 点数
 * @param previewDpi - 预览 DPI
 */
export function dotsToPixels(dots: number, previewDpi: number = SCREEN_DPI): number {
  return Math.round(dots * previewDpi / PRINTER_DPI);
}

/**
 * 获取预览图像中的位置像素
 * @param dots - 位置 dots
 * @param previewDpi - 预览 DPI
 */
export function positionToPixels(dots: number, previewDpi: number = SCREEN_DPI): number {
  return Math.round(dots * previewDpi / PRINTER_DPI);
}

/**
 * 获取预览图像中的位置 (mm)
 * @param mm - 毫米
 * @param previewDpi - 预览 DPI
 */
export function mmToPixels(mm: number, previewDpi: number = SCREEN_DPI): number {
  const dots = mmToDots(mm);
  return positionToPixels(dots, previewDpi);
}

/**
 * 获取粗体偏移
 */
export function getBoldOffsetDots(): number {
  return 1;
}
