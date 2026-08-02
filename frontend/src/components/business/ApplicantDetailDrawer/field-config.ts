/**
 * 应聘者详情抽屉 - 字段配置
 *
 * 支持通过配置文件自定义显示字段，
 * 后期可根据业务需求灵活调整
 */

export interface FieldConfig {
  key: string               // 数据字段名
  label: string              // 显示标签
  type: 'text' | 'phone' | 'date' | 'status' | 'tag' | 'custom'
  span?: number             // 占据列数 (1 或 2)
  visible?: boolean         // 是否可见 (默认 true)
  formatter?: (value: any, data: any) => string  // 自定义格式化函数
  render?: (value: any, data: any) => any        // 自定义渲染插槽名
}

export interface SectionConfig {
  key: string                // 区块标识
  title: string              // 区块标题
  icon?: string              // 图标名称
  fields: FieldConfig[]      // 字段列表
  visible?: boolean          // 是否可见 (默认 true)
}

/** 默认字段配置 */
export const DEFAULT_APPLICANT_FIELDS_CONFIG: SectionConfig[] = [
  {
    key: 'basic',
    title: '基本信息',
    icon: 'User',
    visible: true,
    fields: [
      { key: 'name', label: '姓名', type: 'text', span: 1 },
      { key: 'gender', label: '性别', type: 'text', span: 1 },
      { key: 'phone', label: '联系电话', type: 'phone', span: 2 },
      { key: 'submitTime', label: '应聘日期', type: 'date', span: 2 },
      { key: 'status', label: '当前状态', type: 'status', span: 2 }
    ]
  },
  {
    key: 'position',
    title: '应聘岗位',
    icon: 'Briefcase',
    visible: true,
    fields: [
      { key: 'positionName', label: '目标岗位', type: 'text', span: 2 },
      { key: 'expectedSalary', label: '期望薪资', type: 'text', span: 1 },
      { key: 'availableTime', label: '到岗时间', type: 'text', span: 1 }
    ]
  },
  {
    key: 'experience',
    title: '工作经验',
    icon: 'Medal',
    visible: true,
    fields: [
      { key: 'totalExperience', label: '总工作年限', type: 'text', span: 2 },
      { key: 'introduction', label: '自我介绍', type: 'custom', span: 2 }
    ]
  }
]

/**
 * 根据配置获取可见区块
 */
export function getVisibleSections(config: SectionConfig[]): SectionConfig[] {
  return config.filter(section => section.visible !== false)
}

/**
 * 根据配置获取可见字段
 */
export function getVisibleFields(fields: FieldConfig[]): FieldConfig[] {
  return fields.filter(field => field.visible !== false)
}

/**
 * 格式化字段值
 */
export function formatFieldValue(
  value: any,
  field: FieldConfig,
  data: any
): string {
  if (field.formatter) {
    return field.formatter(value, data)
  }

  switch (field.type) {
    case 'phone':
      return value ? `${String(value).slice(0, 3)}****${String(value).slice(-4)}` : '-'
    case 'date':
      if (!value) return '-'
      try {
        return new Date(value).toLocaleDateString('zh-CN')
      } catch {
        return String(value)
      }
    default:
      return value ?? '-'
  }
}
