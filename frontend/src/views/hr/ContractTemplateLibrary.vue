<script setup lang="ts">
/**
 * 合同模板库（增强版）
 * 解决用户反思：合同条款不应浮于表面，需要模板化+可自定义格式条款
 *
 * 核心能力：
 * 1. 按合同类型分类展示模板（劳动合同/劳务协议/用工协议/实习协议/保密协议）
 * 2. 模板详情包含可自定义条款（TemplateClause）
 * 3. 条款按分类组织（基本信息/薪资/福利/期限/终止/保密/其他）
 * 4. 支持条款编辑和预览
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { contractIntelligenceApi } from '@/api/contract-intelligence'
import {
  type ContractTemplateInfo,
  type ContractTemplateType,
  type TemplateClause,
  ContractTemplateTypeOptions,
  ContractTemplateTypeLabelMap,
} from '@/types/contract-intelligence'

/* ===== 数据状态 ===== */
const loading = ref(false)
const templates = ref<ContractTemplateInfo[]>([])
const currentTypeFilter = ref<ContractTemplateType | ''>('')

const filteredTemplates = computed(() => {
  if (!currentTypeFilter.value) return templates.value
  return templates.value.filter(t => t.templateType === currentTypeFilter.value)
})

/* ===== 统计 ===== */
const stats = computed(() => {
  const byType: Record<string, number> = {}
  for (const opt of ContractTemplateTypeOptions) {
    byType[opt.value] = templates.value.filter(t => t.templateType === opt.value).length
  }
  return { total: templates.value.length, byType }
})

/* ===== 加载数据 ===== */
async function loadData() {
  loading.value = true
  try {
    templates.value = await contractIntelligenceApi.getTemplates()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

/* ===== 模板详情对话框 ===== */
const detailDialogVisible = ref(false)
const detailTemplate = ref<ContractTemplateInfo | null>(null)
const detailTab = ref('info')

function handleViewDetail(template: ContractTemplateInfo) {
  detailTemplate.value = template
  detailTab.value = 'info'
  detailDialogVisible.value = true
}

/* ===== 条款分类标签 ===== */
const clauseCategoryLabels: Record<string, string> = {
  basic: '基本信息',
  salary: '薪资报酬',
  benefit: '福利保险',
  term: '合同期限',
  termination: '终止解除',
  confidentiality: '保密条款',
  other: '其他条款',
}

function getClauseCategoryLabel(category: string): string {
  return clauseCategoryLabels[category] || category
}

/** 按分类分组条款 */
function groupClausesByCategory(clauses: TemplateClause[]): Record<string, TemplateClause[]> {
  const groups: Record<string, TemplateClause[]> = {}
  for (const clause of clauses) {
    const cat = clause.category
    if (!groups[cat]) groups[cat] = []
    groups[cat].push(clause)
  }
  return groups
}

/* ===== 编辑模板对话框 ===== */
const editDialogVisible = ref(false)
const editTemplate = ref<Partial<ContractTemplateInfo>>({})
const editingClauses = ref<TemplateClause[]>([])

function handleEdit(template?: ContractTemplateInfo) {
  if (template) {
    editTemplate.value = { ...template }
    editingClauses.value = template.customClauses.map(c => ({ ...c }))
  } else {
    editTemplate.value = {
      templateName: '',
      templateType: 'labor',
      description: '',
      content: '',
    }
    editingClauses.value = []
  }
  editDialogVisible.value = true
}

/** 添加条款 */
function handleAddClause() {
  editingClauses.value.push({
    clauseId: `C${Date.now()}`,
    clauseTitle: '新条款',
    clauseContent: '',
    required: false,
    editable: true,
    category: 'other',
  })
}

/** 删除条款 */
function handleRemoveClause(index: number) {
  editingClauses.value.splice(index, 1)
}

/** 保存模板 */
async function handleSaveTemplate() {
  if (!editTemplate.value.templateName) {
    ElMessage.warning('请输入模板名称')
    return
  }
  try {
    await contractIntelligenceApi.saveTemplate({
      ...editTemplate.value,
      customClauses: editingClauses.value,
    })
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '保存失败')
  }
}

/** 删除模板 */
async function handleDelete(template: ContractTemplateInfo) {
  try {
    await ElMessageBox.confirm(`确定删除模板「${template.templateName}」？`, '删除确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning',
    })
    await contractIntelligenceApi.deleteTemplate(template.templateId)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* 用户取消 */ }
}

onMounted(() => loadData())
</script>

<template>
  <div class="modern-page">
    <PageHeader title="合同模板库" description="按合同类型管理标准模板与可自定义格式条款">
      <el-button type="primary" @click="handleEdit()">
        <el-icon><Plus /></el-icon>新增模板
      </el-button>
    </PageHeader>

    <!-- 类型筛选 -->
    <div class="type-filter-bar">
      <el-radio-group v-model="currentTypeFilter">
        <el-radio-button value="">全部 ({{ stats.total }})</el-radio-button>
        <el-radio-button v-for="opt in ContractTemplateTypeOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }} ({{ stats.byType[opt.value] || 0 }})
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- 模板卡片网格 -->
    <div v-loading="loading" class="template-grid">
      <div v-for="template in filteredTemplates" :key="template.templateId" class="template-card">
        <div class="template-card__header">
          <div class="template-card__title">{{ template.templateName }}</div>
          <StatusTag :status="template.status === 'active' ? 'success' : 'inactive'" :label="template.status === 'active' ? '启用' : '停用'" size="small" />
        </div>
        <div class="template-card__type">
          <StatusTag status="primary" :label="ContractTemplateTypeLabelMap[template.templateType]" size="small" />
          <span class="template-card__version">v{{ template.version }}</span>
        </div>
        <div class="template-card__desc">{{ template.description }}</div>
        <div class="template-card__clauses">
          <el-icon><Document /></el-icon>
          <span>{{ template.customClauses.length }} 个可自定义条款</span>
        </div>
        <div class="template-card__footer">
          <span class="template-card__time">更新于 {{ template.updateTime }}</span>
          <div class="template-card__actions">
            <el-button link type="primary" size="small" @click="handleViewDetail(template)">查看</el-button>
            <el-button link type="primary" size="small" @click="handleEdit(template)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(template)">删除</el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && filteredTemplates.length === 0" description="暂无模板" />
    </div>

    <!-- 模板详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="模板详情" width="960px" class="fts-dialog--lg" destroy-on-close lock-scroll="false" :teleported="false">
      <template v-if="detailTemplate">
        <el-tabs v-model="detailTab">
          <!-- 基本信息 Tab -->
          <el-tab-pane label="基本信息" name="info">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="模板名称">{{ detailTemplate.templateName }}</el-descriptions-item>
              <el-descriptions-item label="模板类型">
                <StatusTag status="primary" :label="ContractTemplateTypeLabelMap[detailTemplate.templateType]" size="small" />
              </el-descriptions-item>
              <el-descriptions-item label="版本">v{{ detailTemplate.version }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <StatusTag :status="detailTemplate.status === 'active' ? 'success' : 'inactive'" :label="detailTemplate.status === 'active' ? '启用' : '停用'" size="small" />
              </el-descriptions-item>
              <el-descriptions-item label="创建人">{{ detailTemplate.createBy }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ detailTemplate.updateTime }}</el-descriptions-item>
              <el-descriptions-item label="模板描述" :span="2">{{ detailTemplate.description }}</el-descriptions-item>
            </el-descriptions>

            <el-divider content-position="left">合同正文</el-divider>
            <div class="template-content-preview">{{ detailTemplate.content }}</div>
          </el-tab-pane>

          <!-- 可自定义条款 Tab -->
          <el-tab-pane :label="`可自定义条款 (${detailTemplate.customClauses.length})`" name="clauses">
            <div class="clauses-list">
              <template v-for="(clauses, category) in groupClausesByCategory(detailTemplate.customClauses)" :key="category">
                <div class="clause-group">
                  <div class="clause-group__title">{{ getClauseCategoryLabel(category) }}</div>
                  <div v-for="clause in clauses" :key="clause.clauseId" class="clause-item">
                    <div class="clause-item__header">
                      <span class="clause-item__title">{{ clause.clauseTitle }}</span>
                      <div class="clause-item__tags">
                        <StatusTag v-if="clause.required" status="error" label="必选" size="small" />
                        <StatusTag v-if="clause.editable" status="success" label="可编辑" size="small" />
                      </div>
                    </div>
                    <div class="clause-item__content">{{ clause.clauseContent }}</div>
                  </div>
                </div>
              </template>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑模板对话框 -->
    <el-dialog v-model="editDialogVisible" :title="editTemplate.templateId ? '编辑模板' : '新增模板'" width="960px" class="fts-dialog--lg" destroy-on-close lock-scroll="false" :teleported="false">
      <el-form label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模板名称" required>
              <el-input v-model="editTemplate.templateName" placeholder="如：标准劳动合同" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板类型" required>
              <el-select v-model="editTemplate.templateType" style="width:100%" :teleported="false">
                <el-option v-for="opt in ContractTemplateTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="模板描述">
          <el-input v-model="editTemplate.description" type="textarea" :rows="2" placeholder="模板用途说明" />
        </el-form-item>
        <el-form-item label="合同正文">
          <el-input v-model="editTemplate.content" type="textarea" :rows="6" placeholder="合同正文模板，使用__作为变量占位符" />
        </el-form-item>

        <el-divider content-position="left">
          <span>可自定义条款</span>
          <el-button link type="primary" size="small" @click="handleAddClause" style="margin-left:8px;">
            <el-icon><Plus /></el-icon>添加条款
          </el-button>
        </el-divider>

        <div class="edit-clauses">
          <div v-for="(clause, index) in editingClauses" :key="clause.clauseId" class="edit-clause-item">
            <div class="edit-clause-row">
              <el-input v-model="clause.clauseTitle" placeholder="条款标题" style="flex:1;" />
              <el-select v-model="clause.category" placeholder="分类" style="width:120px;" :teleported="false">
                <el-option v-for="(label, key) in clauseCategoryLabels" :key="key" :label="label" :value="key" />
              </el-select>
              <el-checkbox v-model="clause.required">必选</el-checkbox>
              <el-checkbox v-model="clause.editable">可编辑</el-checkbox>
              <el-button link type="danger" size="small" @click="handleRemoveClause(index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-input v-model="clause.clauseContent" type="textarea" :rows="2" placeholder="条款内容，使用__作为变量占位符" />
          </div>
          <el-empty v-if="editingClauses.length === 0" description="暂无条款，点击上方添加条款按钮" :image-size="60" />
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveTemplate">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.type-filter-bar {
  margin-bottom: var(--fts-space-4);
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: var(--fts-space-4);
}

.template-card {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  box-shadow: var(--fts-shadow-sm);
  transition: box-shadow 0.2s;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);

  &:hover {
    box-shadow: var(--fts-shadow);
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: var(--fts-space-2);
  }

  &__title {
    font-size: var(--fts-font-size-base);
    font-weight: 600;
    color: var(--fts-text-primary);
  }

  &__type {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
  }

  &__version {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }

  &__desc {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    line-height: 1.5;
    min-height: 42px;
  }

  &__clauses {
    display: flex;
    align-items: center;
    gap: var(--fts-space-1);
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-tertiary);
  }

  &__footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: var(--fts-space-2);
    border-top: 1px solid var(--fts-border-color);
  }

  &__time {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }

  &__actions {
    display: flex;
    gap: var(--fts-space-1);
  }
}

.template-content-preview {
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  padding: var(--fts-space-3);
  font-size: var(--fts-font-size-sm);
  line-height: 1.8;
  color: var(--fts-text-secondary);
  white-space: pre-wrap;
  max-height: 300px;
  overflow-y: auto;
}

.clauses-list {
  max-height: 500px;
  overflow-y: auto;
}

.clause-group {
  margin-bottom: var(--fts-space-4);

  &__title {
    font-size: var(--fts-font-size-sm);
    font-weight: 600;
    color: var(--fts-primary);
    margin-bottom: var(--fts-space-2);
    padding-bottom: var(--fts-space-1);
    border-bottom: 1px solid var(--fts-border-color);
  }
}

.clause-item {
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  padding: var(--fts-space-3);
  margin-bottom: var(--fts-space-2);

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--fts-space-1);
  }

  &__title {
    font-weight: 500;
    color: var(--fts-text-primary);
  }

  &__tags {
    display: flex;
    gap: 4px;
  }

  &__content {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    line-height: 1.6;
  }
}

.edit-clauses {
  max-height: 400px;
  overflow-y: auto;
}

.edit-clause-item {
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  padding: var(--fts-space-2);
  margin-bottom: var(--fts-space-2);
}

.edit-clause-row {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-2);
}
</style>
