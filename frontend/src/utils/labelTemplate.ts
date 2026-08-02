/**
 * 标签模板工具函数
 * 用于标签排版编辑器的辅助计算
 */

import type { LabelElement, LabelSize, LabelData } from '@/types/label-template-editor';
import { TEXT_FIELD_CONFIGS } from '@/types/label-template-editor';

/**
 * 毫米转像素 (96 DPI)
 */
export function mmToPx(mm: number): number {
  return mm * 3.7795275591;
}

/**
 * 像素转毫米 (96 DPI)
 */
export function pxToMm(px: number): number {
  return px / 3.7795275591;
}

/**
 * 毫米转点 (1pt = 1/72英寸)
 */
export function mmToPt(mm: number): number {
  return mm * 2.8346456693;
}

/**
 * 点转毫米
 */
export function ptToMm(pt: number): number {
  return pt / 2.8346456693;
}

/**
 * 计算文本元素的实际高度
 */
export function calculateTextHeight(
  text: string,
  fontSize: number,
  fontFamily: string,
  maxWidth: number
): number {
  // 创建临时canvas计算文本高度
  const canvas = document.createElement('canvas');
  const ctx = canvas.getContext('2d');
  if (!ctx) return fontSize * 0.35;
  ctx.font = `${fontSize}pt ${fontFamily}`;
  const metrics = ctx.measureText(text);

  // 如果文本宽度超过最大宽度，需要换行
  if (metrics.width > mmToPx(maxWidth)) {
    const lines = Math.ceil(metrics.width / mmToPx(maxWidth));
    return lines * fontSize * 0.35;
  }

  return fontSize * 0.35;
}

/**
 * 检查元素是否在标签范围内
 */
export function isElementInBounds(
  element: LabelElement,
  labelSize: LabelSize
): boolean {
  return (
    element.x >= 0 &&
    element.y >= 0 &&
    element.x + element.width <= labelSize.width &&
    element.y + element.height <= labelSize.height
  );
}

/**
 * 调整元素位置使其保持在标签范围内
 */
export function constrainElementToBounds(
  element: LabelElement,
  labelSize: LabelSize
): LabelElement {
  const constrained = { ...element };

  // 确保元素不超出左边界和上边界
  constrained.x = Math.max(0, constrained.x);
  constrained.y = Math.max(0, constrained.y);

  // 确保元素不超出右边界和下边界
  if (constrained.x + constrained.width > labelSize.width) {
    constrained.x = labelSize.width - constrained.width;
  }
  if (constrained.y + constrained.height > labelSize.height) {
    constrained.y = labelSize.height - constrained.height;
  }

  return constrained;
}

/**
 * 检测两个元素是否重叠
 */
export function doElementsOverlap(
  elem1: LabelElement,
  elem2: LabelElement
): boolean {
  return !(
    elem1.x + elem1.width <= elem2.x ||
    elem2.x + elem2.width <= elem1.x ||
    elem1.y + elem1.height <= elem2.y ||
    elem2.y + elem2.height <= elem1.y
  );
}

/**
 * 获取元素的中心点
 */
export function getElementCenter(element: LabelElement): { x: number; y: number } {
  return {
    x: element.x + element.width / 2,
    y: element.y + element.height / 2
  };
}

/**
 * 计算元素之间的距离
 */
export function getDistanceBetweenElements(
  elem1: LabelElement,
  elem2: LabelElement
): number {
  const center1 = getElementCenter(elem1);
  const center2 = getElementCenter(elem2);
  return Math.sqrt(
    Math.pow(center2.x - center1.x, 2) +
    Math.pow(center2.y - center1.y, 2)
  );
}

/**
 * 对齐元素到网格
 */
export function snapToGrid(
  value: number,
  gridSize: number = 0.5
): number {
  return Math.round(value / gridSize) * gridSize;
}

/**
 * 获取元素的边界框
 */
export function getElementBounds(
  elements: LabelElement[]
): { minX: number; minY: number; maxX: number; maxY: number } | null {
  if (elements.length === 0) return null;
  let minX = Infinity;
  let minY = Infinity;
  let maxX = -Infinity;
  let maxY = -Infinity;
  elements.forEach(element => {
    minX = Math.min(minX, element.x);
    minY = Math.min(minY, element.y);
    maxX = Math.max(maxX, element.x + element.width);
    maxY = Math.max(maxY, element.y + element.height);
  });
  return { minX, minY, maxX, maxY };
}

/**
 * 获取元素的实际显示文本
 */
export function getElementDisplayText(
  element: LabelElement,
  data: LabelData
): string {
  if (element.type !== 'text') return '';
  if (element.textFieldType === 'custom') {
    return element.customText || '';
  }

  const field = element.textFieldType as keyof LabelData;
  return data[field] || '';
}

/**
 * 生成元素的唯一ID
 */
export function generateElementId(type: string): string {
  return `${type}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * 复制元素
 */
export function cloneElement(element: LabelElement): LabelElement {
  return {
    ...JSON.parse(JSON.stringify(element)),
    id: generateElementId(element.type)
  };
}

/**
 * 批量移动元素
 */
export function moveElements(
  elements: LabelElement[],
  dx: number,
  dy: number
): LabelElement[] {
  return elements.map(element => ({
    ...element,
    x: element.x + dx,
    y: element.y + dy
  }));
}

/**
 * 缩放元素
 */
export function scaleElement(
  element: LabelElement,
  scale: number
): LabelElement {
  return {
    ...element,
    x: element.x * scale,
    y: element.y * scale,
    width: element.width * scale,
    height: element.height * scale
  };
}

/**
 * 旋转元素位置（绕中心点）
 */
export function rotateElement(
  element: LabelElement,
  angle: number
): LabelElement {
  const center = getElementCenter(element);
  const radians = (angle * Math.PI) / 180;

  // 计算旋转后的位置
  const cos = Math.cos(radians);
  const sin = Math.sin(radians);

  // 元素四个角相对于中心点的位置
  const corners = [
    { x: -element.width / 2, y: -element.height / 2 },
    { x: element.width / 2, y: -element.height / 2 },
    { x: element.width / 2, y: element.height / 2 },
    { x: -element.width / 2, y: element.height / 2 }
  ];

  // 旋转后的角点
  const rotatedCorners = corners.map(corner => ({
    x: corner.x * cos - corner.y * sin + center.x,
    y: corner.x * sin + corner.y * cos + center.y
  }));

  // 计算新的边界
  const xs = rotatedCorners.map(c => c.x);
  const ys = rotatedCorners.map(c => c.y);
  const minX = Math.min(...xs);
  const maxX = Math.max(...xs);
  const minY = Math.min(...ys);
  const maxY = Math.max(...ys);
  return {
    ...element,
    rotation: (element.rotation + angle) % 360,
    x: minX,
    y: minY,
    width: maxX - minX,
    height: maxY - minY
  };
}

/**
 * 水平翻转元素
 */
export function flipElementHorizontal(
  element: LabelElement,
  labelWidth: number
): LabelElement {
  return {
    ...element,
    x: labelWidth - element.x - element.width
  };
}

/**
 * 垂直翻转元素
 */
export function flipElementVertical(
  element: LabelElement,
  labelHeight: number
): LabelElement {
  return {
    ...element,
    y: labelHeight - element.y - element.height
  };
}

/**
 * 分组元素（计算边界框）
 */
export function groupElements(
  elements: LabelElement[]
): { x: number; y: number; width: number; height: number } | null {
  const bounds = getElementBounds(elements);
  if (!bounds) return null;
  return {
    x: bounds.minX,
    y: bounds.minY,
    width: bounds.maxX - bounds.minX,
    height: bounds.maxY - bounds.minY
  };
}

/**
 * 等间距分布元素（水平方向）
 */
export function distributeElementsHorizontal(
  elements: LabelElement[]
): LabelElement[] {
  if (elements.length < 3) return elements;
  const sorted = [...elements].sort((a, b) => a.x - b.x);
  const leftmost = sorted[0].x;
  const rightmost = sorted[sorted.length - 1].x + sorted[sorted.length - 1].width;
  const totalWidth = sorted.reduce((sum, el) => sum + el.width, 0);
  const spacing = (rightmost - leftmost - totalWidth) / (sorted.length - 1);
  let currentX = leftmost;
  return sorted.map(element => {
    const newElement = { ...element, x: currentX };
    currentX += element.width + spacing;
    return newElement;
  });
}

/**
 * 等间距分布元素（垂直方向）
 */
export function distributeElementsVertical(
  elements: LabelElement[]
): LabelElement[] {
  if (elements.length < 3) return elements;
  const sorted = [...elements].sort((a, b) => a.y - b.y);
  const topmost = sorted[0].y;
  const bottommost = sorted[sorted.length - 1].y + sorted[sorted.length - 1].height;
  const totalHeight = sorted.reduce((sum, el) => sum + el.height, 0);
  const spacing = (bottommost - topmost - totalHeight) / (sorted.length - 1);
  let currentY = topmost;
  return sorted.map(element => {
    const newElement = { ...element, y: currentY };
    currentY += element.height + spacing;
    return newElement;
  });
}

/**
 * 验证模板
 */
export function validateTemplate(template: {
  size?: LabelSize;
  elements: LabelElement[];
}): { valid: boolean; errors: string[] } {
  const errors: string[] = [];

  // 检查标签尺寸
  const size = template.size;
  if (!size) {
    return { valid: false, errors: ['标签尺寸未设置'] };
  }

  if (size.width <= 0 || size.height <= 0) {
    errors.push('标签尺寸必须大于0');
  }

  if (size.width > 100 || size.height > 100) {
    errors.push('标签尺寸不能超过100mm');
  }

  // 检查元素
  template.elements.forEach((element, index) => {
    // 检查元素尺寸
    if (element.width <= 0 || element.height <= 0) {
      errors.push(`元素${index + 1}的尺寸必须大于0`);
    }

    // 检查元素是否在标签范围内
    if (!isElementInBounds(element, size)) {
      errors.push(`元素${index + 1}超出标签范围`);
    }

    // 检查文本元素
    if (element.type === 'text') {
      if (element.textFieldType === 'custom' && !element.customText) {
        errors.push(`文本元素${index + 1}的内容不能为空`);
      }
    }
  });
  return {
    valid: errors.length === 0,
    errors
  };
}

/**
 * 导出模板为JSON
 */
export function exportTemplateToJson(template: unknown): string {
  return JSON.stringify(template, null, 2);
}

/**
 * 从JSON导入模板
 */
export function importTemplateFromJson(json: string): unknown {
  try {
    return JSON.parse(json);
  } catch (error) {
    throw new Error('无效的JSON格式');
  }
}

/**
 * 计算文本字段的推荐字体大小
 */
export function getRecommendedFontSize(
  textFieldType: string,
  labelHeight: number
): number {
  // 根据标签高度和字段类型推荐字体大小
  const baseSize = Math.min(labelHeight * 0.3, 12);
  switch (textFieldType) {
    case 'materialName':
      return Math.min(baseSize * 1.2, 14);
    case 'traceCode':
      return Math.min(baseSize * 0.8, 10);
    default:
      return baseSize;
  }
}

/**
 * 自动布局元素
 */
export function autoLayoutElements(
  elements: LabelElement[],
  labelSize: LabelSize
): LabelElement[] {
  const padding = 2;
  const lineHeight = 4;
  let currentY = padding;
  return elements.map(element => {
    const newElement = {
      ...element,
      x: padding,
      y: currentY,
      width: labelSize.width - padding * 2
    };
    currentY += element.height + lineHeight;
    return newElement;
  });
}
