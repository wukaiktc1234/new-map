/**
 * 标签模板类型定义
 * 用于食品溯源系统的标签排版编辑器
 */

/**
 * 标签元素类型
 */
export type LabelElementType = 'text' | 'qrcode' | 'barcode' | 'line' | 'rect';

/**
 * 文本元素数据字段类型
 */
export type TextFieldType =
  | 'materialName'      // 物料名称
  | 'storeName'         // 门店名称
  | 'shelfLife'         // 保质期
  | 'expiryDate'        // 到期日期
  | 'supplierName'      // 供应商名称
  | 'inboundDate'       // 入库日期
  | 'traceCode'         // 追溯码
  | 'printDate'         // 打印日期
  | 'custom';           // 自定义文本

/**
 * 字体样式
 */
export interface FontStyle {
  bold?: boolean;
  italic?: boolean;
  underline: boolean;
}

/**
 * 标签元素基础接口
 */
export interface LabelElementBase {
  id?: string;
  type?: LabelElementType;
  x: number;              // X坐标 (mm)
  y: number;              // Y坐标 (mm)
  width: number;          // 宽度 (mm)
  height: number;         // 高度 (mm)
  rotation: number;       // 旋转角度 (0-360)
  zIndex: number;         // 层级
  locked: boolean;        // 是否锁定
  visible: boolean;       // 是否可见
}

/**
 * 文本元素
 */
export interface TextLabelElement extends LabelElementBase {
  type: 'text';
  textFieldType?: TextFieldType;
  customText?: string;
  fontFamily?: string;
  fontSize: number;       // 字体大小 (pt)
  fontColor?: string;
  fontStyle?: FontStyle;
  textAlign: 'left' | 'center' | 'right';
  verticalAlign: 'top' | 'middle' | 'bottom';
  autoSize: boolean;      // 是否自动调整大小
  maxWidth: number;
}

/**
 * 二维码元素
 */
export interface QRCodeLabelElement extends LabelElementBase {
  type: 'qrcode';
  dataField: 'traceCode' | 'customUrl';
  customData?: string;
  margin: number;         // 边距 (mm)
}

/**
 * 条码元素
 */
export interface BarcodeLabelElement extends LabelElementBase {
  type: 'barcode';
  dataField: 'traceCode' | 'batchNumber' | 'custom';
  customData?: string;
  barcodeType: 'CODE128' | 'CODE39' | 'EAN13' | 'EAN8';
  showText: boolean;      // 是否显示文本
  textPosition: 'top' | 'bottom';
  textFont?: string;
  textSize: number;
}

/**
 * 线条元素
 */
export interface LineLabelElement extends LabelElementBase {
  type: 'line';
  borderColor?: string;
  borderWidth?: number;
  borderStyle: 'solid' | 'dashed' | 'dotted';
}

/**
 * 矩形元素
 */
export interface RectLabelElement extends LabelElementBase {
  type: 'rect';
  fillColor?: string;
  borderColor?: string;
  borderWidth?: number;
  borderRadius: number;   // 圆角半径 (mm)
}

/**
 * 标签元素联合类型
 */
export type LabelElement =
  | TextLabelElement
  | QRCodeLabelElement
  | BarcodeLabelElement
  | LineLabelElement
  | RectLabelElement;

/**
 * 标签尺寸
 */
export interface LabelSize {
  width: number;          // 宽度 (mm)
  height: number;         // 高度 (mm)
}

/**
 * 标签模板
 */
export interface LabelTemplate {
  id?: string;
  name?: string;
  templateName?: string;
  templateCode?: string;
  templateType?: string;
  description?: string;
  category?: string;
  labelWidth?: number;
  labelHeight?: number;
  dpi?: number;
  gapSize?: number;
  printSpeed?: number;
  printDensity?: number;
  direction?: number;
  sortOrder?: number;
  elements?: LabelElement[];
  layoutConfig?: Record<string, unknown>;
  size?: LabelSize;
  backgroundColor?: string;
  enabled?: boolean;
  isDefault?: boolean;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
  // 兼容前端字段
  createTime?: string;
  updateTime?: string;
  createBy?: string;
  updateBy?: string;
  remark?: string;
}

/**
 * 标签数据
 */
export interface LabelData {
  materialName?: string;
  storeName?: string;
  shelfLife?: string;
  inboundDate?: string;
  expiryDate?: string;
  supplierName?: string;
  traceCode?: string;
  printDate?: string;
  qrCodeUrl?: string;
  batchNumber?: string;
}

/**
 * 元素位置调整
 */
export interface ElementPosition {
  x?: number;
  y?: number;
  width?: number;
  height: number;
}

/**
 * 对齐方式
 */
export type AlignmentType =
  | 'left'
  | 'center'
  | 'right'
  | 'top'
  | 'middle'
  | 'bottom'
  | 'distribute-horizontal'
  | 'distribute-vertical';

/**
 * 文本字段配置
 */
export interface TextFieldConfig {
  type?: TextFieldType;
  label?: string;
  description?: string;
  icon?: string;
  defaultValue: string;
}

/**
 * 文本字段配置列表
 */
export const TEXT_FIELD_CONFIGS: TextFieldConfig[] = [
  {
    type: 'materialName',
    label: '物料名称',
    description: '显示物料/产品名称',
    icon: 'Box',
    defaultValue: '示例物料名称',
  },
  {
    type: 'storeName',
    label: '门店名称',
    description: '显示门店/仓库名称',
    icon: 'Shop',
    defaultValue: '示例门店',
  },
  {
    type: 'shelfLife',
    label: '保质期',
    description: '显示保质期天数',
    icon: 'Timer',
    defaultValue: '365',
  },
  {
    type: 'inboundDate',
    label: '入库日期',
    description: '显示入库日期',
    icon: 'Calendar',
    defaultValue: '2024-01-01',
  },
  {
    type: 'expiryDate',
    label: '到期日期',
    description: '显示到期日期',
    icon: 'Clock',
    defaultValue: '2025-01-31',
  },
  {
    type: 'supplierName',
    label: '供应商名称',
    description: '显示供应商名称',
    icon: 'User',
    defaultValue: '示例供应商',
  },
  {
    type: 'traceCode',
    label: '追溯码',
    description: '显示追溯码',
    icon: 'Link',
    defaultValue: 'TRACE-001',
  },
  {
    type: 'printDate',
    label: '打印日期',
    description: '自动生成打印时的日期',
    icon: 'Calendar',
    defaultValue: '2024-01-01',
  },
  {
    type: 'custom',
    label: '自定义文本',
    description: '自定义显示文本',
    icon: 'Edit',
    defaultValue: '自定义内容',
  },
];

/**
 * 默认标签尺寸 (40mm x 30mm)
 */
export const DEFAULT_LABEL_SIZE: LabelSize = {
  width: 40,
  height: 30,
};

/**
 * 预设标签尺寸列表
 */
export const PRESET_LABEL_SIZES: LabelSize[] = [
  { width: 40, height: 30 },
  { width: 50, height: 30 },
  { width: 60, height: 40 },
  { width: 80, height: 50 },
  { width: 100, height: 60 },
];

/**
 * 字体列表
 */
export const FONT_LIST = [
  { value: 'SimSun', label: '宋体' },
  { value: 'SimHei', label: '黑体' },
  { value: 'Microsoft YaHei', label: '微软雅黑' },
  { value: 'KaiTi', label: '楷体' },
  { value: 'FangSong', label: '仿宋' },
  { value: 'Arial', label: 'Arial' },
  { value: 'Times New Roman', label: 'Times New Roman' },
];

/**
 * 字体大小列表
 */
export const FONT_SIZES = [4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 24, 28, 32, 48, 56, 72] as const;

/**
 * 创建默认文本元素
 */
export function createDefaultTextElement(
  textFieldType: TextFieldType,
  x: number = 2,
  y: number = 2
): TextLabelElement {
  const config = TEXT_FIELD_CONFIGS.find(c => c.type === textFieldType);
  const fontSize = textFieldType === 'materialName' ? 24 : 12;
  return {
    id: `text-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    type: 'text',
    textFieldType,
    customText: '',
    fontFamily: 'Microsoft YaHei',
    fontSize,
    fontColor: '#000000',
    fontStyle: {
      bold: textFieldType === 'materialName',
      italic: false,
      underline: false,
    },
    textAlign: 'left',
    verticalAlign: 'top',
    autoSize: true,
    maxWidth: 36,
    x,
    y,
    width: 36,
    height: Math.max(6, fontSize * 0.35),
    rotation: 0,
    zIndex: 1,
    locked: false,
    visible: true,
  };
}

/**
 * 创建默认二维码元素
 */
export function createDefaultQRCodeElement(
  x: number = 28,
  y: number = 18
): QRCodeLabelElement {
  return {
    id: `qrcode-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    type: 'qrcode',
    dataField: 'traceCode',
    customData: '',
    margin: 1,
    x,
    y,
    width: 10,
    height: 10,
    rotation: 0,
    zIndex: 1,
    locked: false,
    visible: true,
  };
}

/**
 * 创建默认条码元素
 */
export function createDefaultBarcodeElement(
  x: number = 2,
  y: number = 20
): BarcodeLabelElement {
  return {
    id: `barcode-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    type: 'barcode',
    dataField: 'traceCode',
    customData: '',
    barcodeType: 'CODE128',
    showText: true,
    textPosition: 'bottom',
    textFont: 'Microsoft YaHei',
    textSize: 6,
    x,
    y,
    width: 36,
    height: 8,
    rotation: 0,
    zIndex: 1,
    locked: false,
    visible: true,
  };
}

/**
 * 创建默认线条元素
 */
export function createDefaultLineElement(
  x: number = 2,
  y: number = 15
): LineLabelElement {
  return {
    id: `line-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    type: 'line',
    borderColor: '#000000',
    borderWidth: 0.5,
    borderStyle: 'solid',
    x,
    y,
    width: 36,
    height: 0.5,
    rotation: 0,
    zIndex: 1,
    locked: false,
    visible: true,
  };
}

/**
 * 创建默认矩形元素
 */
export function createDefaultRectElement(
  x: number = 0,
  y: number = 0
): RectLabelElement {
  return {
    id: `rect-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    type: 'rect',
    fillColor: 'transparent',
    borderColor: '#000000',
    borderWidth: 0.5,
    borderRadius: 0,
    x,
    y,
    width: 40,
    height: 30,
    rotation: 0,
    zIndex: 0,
    locked: false,
    visible: true,
  };
}

/**
 * 创建默认标签模板
 */
export function createDefaultLabelTemplate(): LabelTemplate {
  const now = new Date().toISOString();
  return {
    id: `template-${Date.now()}`,
    name: '新建标签模板',
    description: '自定义标签模板',
    createdAt: now,
    updatedAt: now,
  };
}
