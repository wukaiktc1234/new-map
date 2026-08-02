/**
 * 标签元素类型
 */
export type LabelElementType = 'TEXT' | 'BARCODE' | 'QRCODE' | 'LINE' | 'BOX';

/**
 * 标签元素位置配置
 * 注意：所有坐标和尺寸单位都是 mm（毫米）
 * 后端会根据打印机 DPI 自动转换dots
 */
export interface LabelPositionConfig {
  /** X坐标（mm） */
  x: number;
  /** Y坐标（mm） */
  y: number;
  /** 宽度（mm，可选） */
  width?: number;
  /** 高度（mm，可选） */
  height?: number;
  /** 旋转角度（0, 90, 180, 270） */
  rotation?: number;
  /** 对齐方式
 */
  align?: 'left' | 'center' | 'right';
}

/**
 * 文本配置
 */
export interface LabelTextConfig {
  /** 文本内容
 */
  content: string;
  /** 字体名称
 */
  font?: string;
  /** 字体大小倍数X（1-10） */
  scaleX?: number;
  /** 字体大小倍数Y（1-10） */
  scaleY?: number;
  /** 是否加粗
 */
  bold?: boolean;
  /** 是否下划线 */
  underline?: boolean;
  /** 最大宽度（点数，用于自动换行） */
  maxWidth?: number;
  /** 行高（点数） */
  lineHeight?: number;
  /** 自动截断字符数（0表示不截断） */
  truncateChars?: number;
}

/**
 * 条码配置
 */
export interface LabelBarcodeConfig {
  /** 条码类型
 */
  barcodeType?: '128' | '39' | 'EAN13' | 'EAN8' | 'UPC-A' | 'UPC-E';
  /** 条码内容
 */
  content: string;
  /** 条码高度（点数） */
  height?: number;
  /** 是否显示文本
 */
  showText?: boolean;
  /** 文本位置（0-下方, 1-上方） */
  textPosition?: number;
  /** 窄条宽度
 */
  narrowWidth?: number;
  /** 宽条宽度
 */
  wideWidth?: number;
}

/**
 * 二维码配置
 */
export interface LabelQRCodeConfig {
  /** 二维码内容 */
  content: string;
  /** 模块宽度（1-10） */
  moduleSize?: number;
  /** 纠错等级
 */
  errorLevel?: 'L' | 'M' | 'Q' | 'H';
  /** 编码模式
 */
  encodeMode?: 'A' | 'M' | 'N';
  /** 掩码模式（0-8, -1表示自动） */
  maskMode?: number;
  /** 二维码大小（点数，可选） */
  size?: number;
}

/**
 * 线条配置
 */
export interface LabelLineConfig {
  /** 线条宽度（点数） */
  lineWidth?: number;
  /** 线条长度（点数） */
  length?: number;
  /** 线条颜色
 */
  color?: string;
  /** 线条方向
 */
  direction?: 'horizontal' | 'vertical';
}

/**
 * 矩形框配置
 */
export interface LabelBoxConfig {
  /** 线条宽度（点数） */
  lineWidth?: number;
  /** 填充颜色
 */
  fillColor?: string;
  /** 边框颜色
 */
  borderColor?: string;
  /** 圆角半径（点数） */
  cornerRadius?: number;
}

/**
 * 数据绑定配置
 */
export interface LabelDataBindingConfig {
  /** 数据字段
 */
  field?: string;
  /** 数据格式化模板 */
  format?: string;
  /** 前缀文本
 */
  prefix?: string;
  /** 后缀文本
 */
  suffix?: string;
  /** 默认值 */
  defaultValue?: string;
  /** 空值时是否隐藏元素
 */
  hideIfEmpty?: boolean;
}

/**
 * 标签元素
 */
export interface LabelElement {
  /** 元素ID
 */
  id: string;
  /** 元素类型
 */
  type: LabelElementType;
  /** 元素位置配置
 */
  position: LabelPositionConfig;
  /** 文本配置
 */
  text?: LabelTextConfig;
  /** 条码配置
 */
  barcode?: LabelBarcodeConfig;
  /** 二维码配置 */
  qrcode?: LabelQRCodeConfig;
  /** 线条配置
 */
  line?: LabelLineConfig;
  /** 矩形框配置 */
  box?: LabelBoxConfig;
  /** 数据绑定配置
 */
  dataBinding?: LabelDataBindingConfig;
  /** 是否可见
 */
  visible?: boolean;
}

/**
 * 标签尺寸配置
 */
export interface LabelSizeConfig {
  /** 宽度（mm） */
  width: number;
  /** 高度（mm） */
  height: number;
  /** DPI
 */
  dpi?: number;
  /** 间隙（mm） */
  gap?: number;
}

/**
 * 打印配置
 */
export interface LabelPrintConfig {
  /** 打印速度
 */
  speed?: number;
  /** 打印浓度
 */
  density?: number;
  /** 打印方向
 */
  direction?: number;
  /** 打印份数
 */
  copies?: number;
  /** 是否启用剥离模式
 */
  peelMode?: boolean;
  /** 剥离延迟（毫秒） */
  peelDelay?: number;
}

/**
 * 背景配置
 */
export interface LabelBackgroundConfig {
  /** 背景颜色
 */
  color?: string;
  /** 边框宽度
 */
  borderWidth?: number;
  /** 边框颜色
 */
  borderColor?: string;
}

/**
 * 标签布局配置
 */
export interface LabelLayoutConfig {
  /** 标签尺寸配置
 */
  size: LabelSizeConfig;
  /** 打印配置
 */
  print?: LabelPrintConfig;
  /** 元素列表
 */
  elements: LabelElement[];
  /** 背景设置
 */
  background?: LabelBackgroundConfig;
}

/**
 * 标签模板
 */
export interface LabelTemplate {
  /** 模板ID（UUID 字符串格式，与后端 LabelTemplate.id 类型一致）
 */
  id: string;
  /** 模板名称
 */
  templateName: string;
  /** 模板编码
 */
  templateCode: string;
  /** 模板类型
 */
  templateType: 'FOOD' | 'MATERIAL' | 'CUSTOM';
  /** 标签宽度（mm） */
  labelWidth: number;
  /** 标签高度（mm） */
  labelHeight: number;
  /** 打印机DPI
 */
  dpi: number;
  /** 标签间隙（mm） */
  gapSize: number;
  /** 打印速度
 */
  printSpeed: number;
  /** 打印浓度
 */
  printDensity: number;
  /** 打印方向
 */
  direction: number;
  /** 布局配置（JSON） */
  layoutConfig: string;
  /** 数据映射（JSON） */
  dataMapping: string;
  /** 是否启用
 */
  enabled: boolean;
  /** 是否为默认模板 */
  isDefault: boolean;
  /** 排序号
 */
  sortOrder: number;
  /** 备注
 */
  remark: string;
  /** 创建时间
 */
  createTime: string;
  /** 更新时间
 */
  updateTime: string;
}

/**
 * 预览元素
 */
export interface PreviewElement {
  /** 元素ID
 */
  id: string;
  /** 元素类型
 */
  type: string;
  /** X坐标（像素） */
  x: number;
  /** Y坐标（像素） */
  y: number;
  /** 宽度（像素） */
  width: number;
  /** 高度（像素） */
  height: number;
  /** 内容
 */
  content: string;
  /** 样式
 */
  style: Record<string, unknown>;
}

/**
 * 预览数据
 */
export interface PreviewData {
  /** 标签宽度（像素） */
  width: number;
  /** 标签高度（像素） */
  height: number;
  /** 缩放比例
 */
  scale: number;
  /** 元素列表
 */
  elements: PreviewElement[];
}

/**
 * 标签打印机数据
 */
export interface LabelPrintData {
  /** 追溯码 */
  traceCode?: string;
  /** 物料名称
 */
  materialName?: string;
  /** 门店名称
 */
  storeName?: string;
  /** 保质期（天数） */
  shelfLifeDays?: number | string;
  /** 入库日期
 */
  generateTime?: string;
  /** 到期日期
 */
  expiryDate?: string;
  /** 供应商名称 */
  supplierName?: string;
  /** 生产日期
 */
  productionDate?: string;
  /** 重量
 */
  weight?: number | string;
  /** 重量单位
 */
  weightUnit?: string;
  /** 存储条件
 */
  storageCondition?: string;
}

/**
 * 支持的数据字段
 */
export const LABEL_DATA_FIELDS: {
field: string;
label: string;
type: string }[] = [
{ field: 'traceCode',
label: '追溯',
type: 'string' },
{ field: 'materialName',
label: '物料名称',
type: 'string' },
{ field: 'storeName',
label: '门店名称',
type: 'string' },
{ field: 'shelfLifeDays',
label: '保质期（天数）',
type: 'number' },
{ field: 'generateTime',
label: '入库日期',
type: 'date' },
{ field: 'expiryDate',
label: '到期日期',
type: 'date' },
{ field: 'supplierName',
label: '供应商名称',
type: 'string' },
{ field: 'productionDate',
label: '生产日期',
type: 'date' },
{ field: 'weight',
label: '重量',
type: 'number' },
{ field: 'weightUnit',
label: '重量单位',
type: 'string' },
{ field: 'storageCondition',
label: '存储条件',
type: 'string' },
];

/**
 * 支持的字体列表
 */
export const LABEL_FONTS: {
value: string;
label: string;
size: number }[] = [
{ value: 'TSS24.BF2',
label: '中文宋体 24',
size: 24 },
{ value: 'TSS20.BF2',
label: '中文宋体 20',
size: 20 },
{ value: 'TSS16.BF2',
label: '中文宋体 16',
size: 16 },
{ value: 'TSS12.BF2',
label: '中文宋体 12',
size: 12 },
{ value: '2',
label: '英文 8',
size: 8 },
{ value: '3',
label: '英文 12',
size: 12 },
];

/**
 * 条码类型
 */
export const BARCODE_TYPES: {
value: string;
label: string }[] = [
{ value: '128',
label: 'Code 128' },
{ value: '39',
label: 'Code 39' },
{ value: 'EAN13',
label: 'EAN-13' },
{ value: 'EAN8',
label: 'EAN-8' },
{ value: 'UPC-A',
label: 'UPC-A' },
{ value: 'UPC-E',
label: 'UPC-E' },
];

/**
 * 二维码纠错等级
 */
export const QR_ERROR_LEVELS: {
value: string;
label: string }[] = [
{ value: 'L',
label: 'L(7%)' },
{ value: 'M',
label: 'M(15%)' },
{ value: 'Q',
label: 'Q(25%)' },
{ value: 'H',
label: 'H(30%)' },
];

/**
 * 默认标签布局配置
 */
export const DEFAULT_LABEL_CONFIG: LabelLayoutConfig = {
  size: {
    width: 40,
    height: 30,
    dpi: 300,
    gap: 2,
  },
  print: {
    speed: 3,
    density: 12,
    direction: 1,
    copies: 1,
    peelMode: false,
  },
  elements: [],
  background: {
    color: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#000000',
  },
};

/**
 * 创建默认食品标签模板
 */
export function createDefaultFoodLabelTemplate(): LabelLayoutConfig {
  const config: LabelLayoutConfig = {
    ...DEFAULT_LABEL_CONFIG,
    elements: [
      {
        id: 'material_name',
        type: 'TEXT',
position: {
x: 15,
y: 15,
align: 'left' },
        text: {
          content: '',
          font: 'TSS24.BF2',
          scaleX: 2,
          scaleY: 2,
          truncateChars: 4,
        },
        dataBinding: {
          field: 'materialName',
          hideIfEmpty: false,
        },
        visible: true,
      },
      {
        id: 'store_name',
        type: 'TEXT',
position: {
x: 15,
y: 55,
align: 'left' },
        text: {
          content: '',
          font: 'TSS24.BF2',
          scaleX: 1,
          scaleY: 1,
        },
        dataBinding: {
          field: 'storeName',
          prefix: '门店:',
          hideIfEmpty: false,
        },
        visible: true,
      },
      {
        id: 'shelf_life',
        type: 'TEXT',
position: {
x: 15,
y: 83,
align: 'left' },
        text: {
          content: '',
          font: 'TSS24.BF2',
          scaleX: 1,
          scaleY: 1,
        },
        dataBinding: {
          field: 'shelfLifeDays',
          prefix: '保质:',
          suffix: '天',
          hideIfEmpty: false,
        },
        visible: true,
      },
      {
        id: 'generate_time',
        type: 'TEXT',
position: {
x: 155,
y: 83,
align: 'left' },
        text: {
          content: '',
          font: 'TSS24.BF2',
          scaleX: 1,
          scaleY: 1,
        },
        dataBinding: {
          field: 'generateTime',
          prefix: '入库:',
          format: 'yyyy-MM-dd',
          hideIfEmpty: false,
        },
        visible: true,
      },
      {
        id: 'expiry_date',
        type: 'TEXT',
position: {
x: 15,
y: 111,
align: 'left' },
        text: {
          content: '',
          font: 'TSS24.BF2',
          scaleX: 1,
          scaleY: 1,
        },
        dataBinding: {
          field: 'expiryDate',
          prefix: '到期:',
          format: 'yyyy-MM-dd',
          hideIfEmpty: false,
        },
        visible: true,
      },
      {
        id: 'supplier_name',
        type: 'TEXT',
position: {
x: 155,
y: 111,
align: 'left' },
        text: {
          content: '',
          font: 'TSS24.BF2',
          scaleX: 1,
          scaleY: 1,
          truncateChars: 4,
        },
        dataBinding: {
          field: 'supplierName',
          prefix: '供应:',
          hideIfEmpty: false,
        },
        visible: true,
      },
      {
        id: 'qrcode',
        type: 'QRCODE',
position: {
x: 392,
y: 15,
align: 'left' },
        qrcode: {
          content: '',
          moduleSize: 4,
          errorLevel: 'M',
          encodeMode: 'A',
        },
        dataBinding: {
          field: 'traceCode',
          hideIfEmpty: false,
        },
        visible: true,
      },
      {
        id: 'trace_code',
        type: 'TEXT',
position: {
x: 15,
y: 315,
align: 'left' },
        text: {
          content: '',
          font: 'TSS24.BF2',
          scaleX: 1,
          scaleY: 1,
        },
        dataBinding: {
          field: 'traceCode',
          prefix: '',
          hideIfEmpty: false,
        },
        visible: true,
      },
    ],
  };

  return config;
}
