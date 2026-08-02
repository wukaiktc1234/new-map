// 健康证相关模板定义
// 模板类型枚举
export enum HealthCheckTemplateType {
  REASON = 'reason',
  NOTE = 'note',
  OPERATION_LOG = 'operation_log'
}

// 模板接口定义
export interface HealthCheckTemplate {
  id: string;
  type: HealthCheckTemplateType;
  name: string;
  content: string;
  description: string;
  isDefault: boolean;
  category?: string;
  createdAt: string;
  updatedAt: string;
}

// 预定义模板列表
export const HEALTH_CHECK_TEMPLATES: HealthCheckTemplate[] = [
  // 报销事由模板
  {
    id: 'reason-001',
    type: HealthCheckTemplateType.REASON,
    name: '新办健康证费用',
    content: '新办健康证费用',
    description: '用于新入职员工办理健康证的费用报销',
    isDefault: true,
    category: '新办',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: 'reason-002',
    type: HealthCheckTemplateType.REASON,
    name: '健康证续期费用',
    content: '健康证续期费用',
    description: '用于现有员工健康证到期续期的费用报销',
    isDefault: true,
    category: '续期',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: 'reason-003',
    type: HealthCheckTemplateType.REASON,
    name: '健康证补办费用',
    content: '健康证补办费用',
    description: '用于健康证丢失或损坏补办的费用报销',
    isDefault: false,
    category: '补办',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },

  // 备注模板
  {
    id: 'note-001',
    type: HealthCheckTemplateType.NOTE,
    name: '新入职员工首次办理',
    content: '该员工为新入职，首次办理健康证',
    description: '新员工健康证办理备注',
    isDefault: true,
    category: '新员工',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: 'note-002',
    type: HealthCheckTemplateType.NOTE,
    name: '续期办理有效期',
    content: '续期办理，有效期1年',
    description: '健康证续期备注',
    isDefault: true,
    category: '续期',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: 'note-003',
    type: HealthCheckTemplateType.NOTE,
    name: '加急办理',
    content: '加急办理，用于特殊岗位需求',
    description: '加急办理健康证备注',
    isDefault: false,
    category: '特殊情况',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },

  // 操作日志模板
  {
    id: 'log-001',
    type: HealthCheckTemplateType.OPERATION_LOG,
    name: '编辑健康证',
    content: '编辑健康证信息，健康证号：{certificateNumber}',
    description: '编辑健康证操作日志模板',
    isDefault: true,
    category: '编辑',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: 'log-002',
    type: HealthCheckTemplateType.OPERATION_LOG,
    name: '申请报销',
    content: '申请健康证费用报销，金额：{amount}元，健康证号：{certificateNumber}',
    description: '申请报销操作日志模板',
    isDefault: true,
    category: '报销',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  },
  {
    id: 'log-003',
    type: HealthCheckTemplateType.OPERATION_LOG,
    name: '审核通过',
    content: '审核通过健康证报销申请，金额：{amount}元，健康证号：{certificateNumber}',
    description: '审核通过操作日志模板',
    isDefault: true,
    category: '审核',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  }
];

// 获取模板列表
export const getHealthCheckTemplates = (type: HealthCheckTemplateType): HealthCheckTemplate[] => {
  return HEALTH_CHECK_TEMPLATES.filter(template => template.type === type);
};

// 获取默认模板
export const getDefaultTemplate = (type: HealthCheckTemplateType): HealthCheckTemplate | undefined => {
  return HEALTH_CHECK_TEMPLATES.find(template => template.type === type && template.isDefault);
};

// 获取指定ID的模板
export const getTemplateById = (id: string): HealthCheckTemplate | undefined => {
  return HEALTH_CHECK_TEMPLATES.find(template => template.id === id);
};

// 渲染模板内容（替换占位符）
export const renderTemplate = (template: HealthCheckTemplate, data: Record<string, unknown>): string => {
  let content = template.content;

  // 替换所有占位符
  Object.entries(data).forEach(([key, value]) => {
    const regex = new RegExp(`\\{${key}\\}`, 'g');
    content = content.replace(regex, String(value));
  });

  return content;
};

// 保存自定义模板
export const saveCustomTemplate = (template: Omit<HealthCheckTemplate, 'id' | 'createdAt' | 'updatedAt'>): HealthCheckTemplate => {
  const newTemplate: HealthCheckTemplate = {
    ...template,
    id: `${template.type}-${Date.now()}`,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };

  HEALTH_CHECK_TEMPLATES.push(newTemplate);
  return newTemplate;
};

// 更新模板
export const updateTemplate = (id: string, updates: Partial<HealthCheckTemplate>): HealthCheckTemplate | undefined => {
  const index = HEALTH_CHECK_TEMPLATES.findIndex(template => template.id === id);
  if (index !== -1) {
    HEALTH_CHECK_TEMPLATES[index] = {
      ...HEALTH_CHECK_TEMPLATES[index],
      ...updates,
      updatedAt: new Date().toISOString()
    };
    return HEALTH_CHECK_TEMPLATES[index];
  }
  return undefined;
};

// 删除模板
export const deleteTemplate = (id: string): boolean => {
  const index = HEALTH_CHECK_TEMPLATES.findIndex(template => template.id === id);
  if (index !== -1) {
    HEALTH_CHECK_TEMPLATES.splice(index, 1);
    return true;
  }
  return false;
};

// 按分类获取模板
export const getTemplatesByCategory = (type: HealthCheckTemplateType, category: string): HealthCheckTemplate[] => {
  return HEALTH_CHECK_TEMPLATES.filter(template => template.type === type && template.category === category);
};

// 搜索模板
export const searchTemplates = (type: HealthCheckTemplateType, keyword: string): HealthCheckTemplate[] => {
  const lowerKeyword = keyword.toLowerCase();
  return HEALTH_CHECK_TEMPLATES.filter(template =>
    template.type === type &&
    (template.name.toLowerCase().includes(lowerKeyword) ||
     template.content.toLowerCase().includes(lowerKeyword) ||
     template.description.toLowerCase().includes(lowerKeyword))
  );
};
