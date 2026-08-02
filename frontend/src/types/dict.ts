/**
 * 字典管理模块类型定义
 * @module types/dict
 */

/**
 * 字典类型接口
 */
export interface DictType {
  /** 字典ID */
  dictId: number;
  /** 字典名称 */
  dictName: string;
  /** 字典编码（唯一标识） */
  dictCode: string;
  /** 字典分组 */
  dictGroup: string;
  /** 字典描述 */
  description: string;
  /** 状态：0-禁用，1-启用 */
  status: number;
  /** 是否系统内置：0-否，1-是 */
  isSystem: number;
  /** 排序序号 */
  sortOrder: number;
  /** 创建人ID */
  createUserId: string;
  /** 创建人姓名 */
  createUsername: string;
  /** 更新人ID */
  updateUserId: string;
  /** 更新人姓名 */
  updateUsername: string;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
}

/**
 * 字典项明细接口
 */
export interface DictItem {
  /** 字典项ID */
  itemId: number;
  /** 关联的字典类型ID */
  dictId: number;
  /** 字典项标签（显示文本） */
  itemLabel: string;
  /** 字典项值（实际值） */
  itemValue: string;
  /** 排序序号 */
  sortOrder: number;
  /** CSS样式类 */
  cssClass: string;
  /** 列表样式类 */
  listClass: string;
  /** 颜色类型：primary/success/warning/danger/info */
  colorType: string;
  /** 是否默认项：0-否，1-是 */
  isDefault: number;
  /** 状态：0-禁用，1-启用 */
  status: number;
  /** 备注说明 */
  remark: string;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
}

/**
 * 字典类型创建表单数据
 */
export interface DictCreateFormData {
  dictName: string;
  dictCode: string;
  dictGroup: string;
  description?: string;
  sortOrder?: number;
}

/**
 * 字典类型更新表单数据
 */
export interface DictUpdateFormData {
  dictName: string;
  dictGroup?: string;
  description?: string;
  status?: number;
  sortOrder?: number;
}

/**
 * 字典项创建表单数据
 */
export interface DictItemCreateFormData {
  dictId: number;
  itemLabel: string;
  itemValue: string;
  sortOrder?: number;
  cssClass?: string;
  listClass?: string;
  colorType?: string;
  isDefault?: number;
  remark?: string;
}

/**
 * 字典项更新表单数据
 */
export interface DictItemUpdateFormData {
  itemLabel: string;
  itemValue: string;
  sortOrder?: number;
  cssClass?: string;
  listClass?: string;
  colorType?: string;
  isDefault?: number;
  status?: number;
  remark?: string;
}

/**
 * 字典查询参数
 */
export interface DictQueryParams {
  current?: number;
  size?: number;
  dictName?: string;
  dictCode?: string;
  dictGroup?: string;
  status?: number;
}

/**
 * 字典项查询参数
 */
export interface DictItemQueryParams {
  current?: number;
  size?: number;
  dictId: number;
  itemLabel?: string;
  status?: number;
}

/**
 * 分页响应数据
 */
export interface PageResponse<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

/**
 * 颜色类型枚举
 */
export type ColorType = 'primary' | 'success' | 'warning' | 'danger' | 'info' | '';

/** 
 * DictTag组件的Props
 */
export interface DictTagProps {
  /** 字典编码 */
  dictCode: string;
  /** 字典项值 */
  value: string | number;
  /** 是否显示为标签 */
  showTag?: boolean;
}

/**
 * DictSelect组件的Props
 */
export interface DictSelectProps {
  /** 字典编码 */
  dictCode: string;
  /** 绑定值 */
  modelValue?: string | number;
  /** 是否可清除 */
  clearable?: boolean;
  /** 是否禁用 */
  disabled?: boolean;
  /** 是否多选 */
  multiple?: boolean;
  /** 占位文本 */
  placeholder?: string;
}
