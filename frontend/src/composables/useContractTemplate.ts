/**
 * 合同模板库逻辑
 * 提供模板列表加载、筛选、详情查看、编辑、删除等能力
 */
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { contractIntelligenceApi } from '@/api/contract-intelligence'
import type { ContractTemplateInfo, ContractTemplateType, TemplateClause } from '@/types/contract-intelligence'
import { ContractTemplateTypeOptions, ContractTemplateTypeLabelMap } from '@/types/contract-intelligence'

export function useContractTemplate() {
  /* ===== 数据状态 ===== */
  const templateList = ref<ContractTemplateInfo[]>([])
  const templateLoading = ref(false)
  const templateTypeFilter = ref<ContractTemplateType | ''>('')

  const filteredTemplates = computed(() => {
    if (!templateTypeFilter.value) return templateList.value
    return templateList.value.filter(t => t.templateType === templateTypeFilter.value)
  })

  /** 模板库统计卡片 */
  const templateStats = computed(() => {
    const total = templateList.value.length
    const active = templateList.value.filter(t => t.status === 'active').length
    const inactive = total - active
    const clauses = templateList.value.reduce((sum, t) => sum + t.customClauses.length, 0)
    return [
      { icon: 'Document', label: '模板总数', value: total, colorType: 'primary' as const },
      { icon: 'CircleCheck', label: '启用模板', value: active, colorType: 'success' as const },
      { icon: 'CircleClose', label: '停用模板', value: inactive, colorType: 'error' as const },
      { icon: 'Edit', label: '可自定义条款', value: clauses, colorType: 'info' as const },
    ]
  })

  /* ===== 加载数据 ===== */
  async function loadTemplateData() {
    templateLoading.value = true
    try {
      templateList.value = await contractIntelligenceApi.getTemplates()
    } catch (error: unknown) {
      if (error instanceof Error) ElMessage.error(error.message || '加载模板失败')
    } finally {
      templateLoading.value = false
    }
  }

  /* ===== 模板详情对话框 ===== */
  const templateDetailVisible = ref(false)
  const templateDetail = ref<ContractTemplateInfo | null>(null)
  const templateDetailTab = ref('info')

  function handleViewTemplate(template: ContractTemplateInfo) {
    templateDetail.value = template
    templateDetailTab.value = 'info'
    templateDetailVisible.value = true
  }

  /* ===== 条款分类标签 ===== */
  const clauseCategoryLabels: Record<string, string> = {
    basic: '基本信息', salary: '薪资报酬', benefit: '福利保险',
    term: '合同期限', termination: '终止解除', confidentiality: '保密条款', other: '其他条款',
  }

  /** 按分类分组条款 */
  function groupClausesByCategory(clauses: TemplateClause[]): Record<string, TemplateClause[]> {
    const groups: Record<string, TemplateClause[]> = {}
    for (const clause of clauses) {
      if (!groups[clause.category]) groups[clause.category] = []
      groups[clause.category].push(clause)
    }
    return groups
  }

  /* ===== 编辑模板对话框 ===== */
  const templateEditVisible = ref(false)
  const templateEditForm = ref<Partial<ContractTemplateInfo>>({})
  const templateEditClauses = ref<TemplateClause[]>([])

  function handleEditTemplate(template?: ContractTemplateInfo) {
    if (template) {
      templateEditForm.value = { ...template }
      templateEditClauses.value = template.customClauses.map(c => ({ ...c }))
    } else {
      templateEditForm.value = { templateName: '', templateType: 'labor', description: '', content: '' }
      templateEditClauses.value = []
    }
    templateEditVisible.value = true
  }

  function handleAddClause() {
    templateEditClauses.value.push({
      clauseId: `C${Date.now()}`, clauseTitle: '新条款', clauseContent: '',
      required: false, editable: true, category: 'other',
    })
  }

  function handleRemoveClause(index: number) {
    templateEditClauses.value.splice(index, 1)
  }

  async function handleSaveTemplate() {
    if (!templateEditForm.value.templateName) {
      ElMessage.warning('请输入模板名称')
      return
    }
    try {
      await contractIntelligenceApi.saveTemplate({
        ...templateEditForm.value,
        customClauses: templateEditClauses.value,
      })
      ElMessage.success('保存成功')
      templateEditVisible.value = false
      loadTemplateData()
    } catch (error: unknown) {
      if (error instanceof Error) ElMessage.error(error.message || '保存失败')
    }
  }

  async function handleDeleteTemplate(template: ContractTemplateInfo) {
    try {
      await ElMessageBox.confirm(`确定删除模板「${template.templateName}」？`, '删除确认', {
        confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning',
      })
      await contractIntelligenceApi.deleteTemplate(template.templateId)
      ElMessage.success('删除成功')
      loadTemplateData()
    } catch { /* 用户取消 */ }
  }

  return {
    templateList,
    templateLoading,
    templateTypeFilter,
    filteredTemplates,
    templateStats,
    loadTemplateData,
    // 详情
    templateDetailVisible,
    templateDetail,
    templateDetailTab,
    handleViewTemplate,
    // 条款
    clauseCategoryLabels,
    groupClausesByCategory,
    // 编辑
    templateEditVisible,
    templateEditForm,
    templateEditClauses,
    handleEditTemplate,
    handleAddClause,
    handleRemoveClause,
    handleSaveTemplate,
    handleDeleteTemplate,
    // 常量
    ContractTemplateTypeOptions,
    ContractTemplateTypeLabelMap,
  }
}
