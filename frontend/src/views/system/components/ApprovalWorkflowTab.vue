<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight } from '@element-plus/icons-vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { approvalWorkflowApi } from '@/api/approval'
import { approvalWorkflowConverter } from '@/api/approval/converters'
import { usePermissionStore } from '@/stores/permission'
import type {
  ApprovalWorkflow,
  ApprovalNode,
  ApprovalCondition,
  BusinessType,
  TemplateCode,
  ApproverType,
  ApproveMode,
  TimeoutAction,
  ConditionField,
  ConditionOperator,
} from '@/types/approval-workflow'
import {
  BusinessTypeOptions,
  TemplateCodeOptions,
  ApproverTypeOptions,
  ApproveModeOptions,
  TimeoutActionOptions,
  ConditionFieldOptions,
  ConditionOperatorOptions,
} from '@/types/approval-workflow'

const permissionStore = usePermissionStore()

// ============================================================
// 状态
// ============================================================

const loading = ref(false)
const saving = ref(false)

/** 当前选中的业务类型 */
const activeBusinessType = ref<BusinessType>('purchase_request')

/** 各业务类型的审批流程缓存 */
const workflowMap = ref<Map<BusinessType, ApprovalWorkflow | null>>(new Map())

/** 当前编辑中的审批流程（深拷贝，避免直接修改缓存） */
const editingWorkflow = ref<ApprovalWorkflow | null>(null)

/** 当前选中的权限模板（用于加载默认流程） */
const selectedTemplateCode = ref<TemplateCode>('centralized-single')

// ============================================================
// 计算属性
// ============================================================

/** 当前业务类型的审批流程 */
const currentWorkflow = computed(() => editingWorkflow.value)

/** 当前流程的节点列表 */
const currentNodes = computed(() => currentWorkflow.value?.nodes || [])

/** 当前流程的条件列表 */
const currentConditions = computed(() => currentWorkflow.value?.conditions || [])

/** 当前业务类型的中文名称 */
const activeBusinessTypeLabel = computed(() =>
  approvalWorkflowConverter.toBusinessTypeLabel(activeBusinessType.value),
)

/** 节点选项（用于条件跳转目标选择） */
const nodeOptions = computed(() =>
  currentNodes.value.map(n => ({ value: n.nodeId, label: n.nodeName })),
)

// ============================================================
// 数据加载
// ============================================================

/** 加载指定业务类型的审批流程 */
async function loadWorkflow(businessType: BusinessType) {
  loading.value = true
  try {
    const templateCode = selectedTemplateCode.value
    const workflow = await approvalWorkflowApi.getDefaultWorkflow(businessType, templateCode)
    workflowMap.value.set(businessType, workflow)
    // 深拷贝到编辑区
    editingWorkflow.value = workflow ? JSON.parse(JSON.stringify(workflow)) : createEmptyWorkflow(businessType)
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '加载审批流程失败')
    }
    editingWorkflow.value = createEmptyWorkflow(businessType)
  } finally {
    loading.value = false
  }
}

/** 创建空的审批流程 */
function createEmptyWorkflow(businessType: BusinessType): ApprovalWorkflow {
  return {
    workflowId: '',
    workflowName: `${approvalWorkflowConverter.toBusinessTypeLabel(businessType)}审批`,
    businessType,
    templateCode: selectedTemplateCode.value,
    enabled: false,
    nodes: [],
    conditions: [],
    createTime: '',
    updateTime: '',
  }
}

/** 加载所有业务类型的审批流程（容错：单项失败不影响整体，静默不弹提示） */
async function loadAllWorkflows() {
  const templateCode = permissionStore.currentTemplate as TemplateCode || 'centralized-single'
  selectedTemplateCode.value = templateCode

  // 并行加载所有业务类型的审批流程（allSettled：任一失败不中断整体）
  const promises = BusinessTypeOptions.map(async (bt) => {
    const workflow = await approvalWorkflowApi.getDefaultWorkflow(bt.value, templateCode).catch(() => null)
    return { key: bt.value, workflow }
  })
  const results = await Promise.allSettled(promises)

  // 批量设置，减少响应式更新次数
  const newMap = new Map<BusinessType, ApprovalWorkflow | null>()
  results.forEach((result) => {
    if (result.status === 'fulfilled' && result.value) {
      newMap.set(result.value.key, result.value.workflow)
    }
  })
  workflowMap.value = newMap

  // 加载当前选中的业务类型
  await loadWorkflow(activeBusinessType.value)
}

// ============================================================
// 业务类型切换
// ============================================================

function handleBusinessTypeChange(businessType: BusinessType) {
  activeBusinessType.value = businessType
  loadWorkflow(businessType)
}

// ============================================================
// 权限模板切换
// ============================================================

async function handleTemplateChange(templateCode: TemplateCode) {
  selectedTemplateCode.value = templateCode
  await loadWorkflow(activeBusinessType.value)
}

/** 重置为默认流程 */
async function handleResetToDefault() {
  try {
    await ElMessageBox.confirm(
      `将「${activeBusinessTypeLabel.value}」的审批流程重置为「${approvalWorkflowConverter.toTemplateCodeLabel(selectedTemplateCode.value)}」模板的默认配置，当前修改将丢失。`,
      '确认重置',
      { confirmButtonText: '确认重置', cancelButtonText: '取消', type: 'warning' },
    )
    await loadWorkflow(activeBusinessType.value)
    ElMessage.success('已重置为默认配置')
  } catch {
    // 用户取消
  }
}

// ============================================================
// 启用开关
// ============================================================

function handleEnabledChange(val: boolean) {
  if (editingWorkflow.value) {
    editingWorkflow.value.enabled = val
  }
}

// ============================================================
// 节点操作
// ============================================================

/** 生成唯一节点ID */
function generateNodeId(): string {
  return `N${Date.now().toString(36).toUpperCase()}`
}

/** 添加审批节点 */
function addNode() {
  if (!editingWorkflow.value) return
  const order = editingWorkflow.value.nodes.length + 1
  const newNode: ApprovalNode = {
    nodeId: generateNodeId(),
    nodeName: `审批节点${order}`,
    nodeOrder: order,
    approverType: 'role' as ApproverType,
    approverValue: '',
    approveMode: 'or' as ApproveMode,
    timeoutHours: 24,
    timeoutAction: 'escalate' as TimeoutAction,
  }
  editingWorkflow.value.nodes.push(newNode)
}

/** 删除审批节点 */
function removeNode(index: number) {
  if (!editingWorkflow.value) return
  editingWorkflow.value.nodes.splice(index, 1)
  // 重新排序
  editingWorkflow.value.nodes.forEach((n, i) => {
    n.nodeOrder = i + 1
  })
}

/** 上移节点 */
function moveNodeUp(index: number) {
  if (!editingWorkflow.value || index <= 0) return
  const nodes = editingWorkflow.value.nodes
  const temp = nodes[index]
  nodes[index] = nodes[index - 1]
  nodes[index - 1] = temp
  nodes.forEach((n, i) => { n.nodeOrder = i + 1 })
}

/** 下移节点 */
function moveNodeDown(index: number) {
  if (!editingWorkflow.value || index >= editingWorkflow.value.nodes.length - 1) return
  const nodes = editingWorkflow.value.nodes
  const temp = nodes[index]
  nodes[index] = nodes[index + 1]
  nodes[index + 1] = temp
  nodes.forEach((n, i) => { n.nodeOrder = i + 1 })
}

// ============================================================
// 条件操作
// ============================================================

/** 生成唯一条件ID */
function generateConditionId(): string {
  return `C${Date.now().toString(36).toUpperCase()}`
}

/** 添加条件 */
function addCondition() {
  if (!editingWorkflow.value) return
  const newCondition: ApprovalCondition = {
    conditionId: generateConditionId(),
    field: 'totalAmount' as ConditionField,
    operator: 'gte' as ConditionOperator,
    value: 0,
    targetNodeIds: [],
  }
  editingWorkflow.value.conditions.push(newCondition)
}

/** 删除条件 */
function removeCondition(index: number) {
  if (!editingWorkflow.value) return
  editingWorkflow.value.conditions.splice(index, 1)
}

// ============================================================
// 保存
// ============================================================

async function handleSave() {
  if (!editingWorkflow.value) return

  // 基础校验
  if (editingWorkflow.value.nodes.length === 0) {
    ElMessage.warning('请至少添加一个审批节点')
    return
  }
  for (const node of editingWorkflow.value.nodes) {
    if (!node.nodeName.trim()) {
      ElMessage.warning(`第${node.nodeOrder}个节点名称不能为空`)
      return
    }
    if (!node.approverValue.trim()) {
      ElMessage.warning(`节点「${node.nodeName}」的审批人值不能为空`)
      return
    }
  }

  saving.value = true
  try {
    const formData = {
      workflowName: editingWorkflow.value.workflowName,
      businessType: editingWorkflow.value.businessType,
      templateCode: editingWorkflow.value.templateCode,
      enabled: editingWorkflow.value.enabled,
      nodes: editingWorkflow.value.nodes,
      conditions: editingWorkflow.value.conditions,
    }

    if (editingWorkflow.value.workflowId) {
      await approvalWorkflowApi.update(editingWorkflow.value.workflowId, formData)
    } else {
      const result = await approvalWorkflowApi.create(formData)
      editingWorkflow.value.workflowId = result.workflowId
    }

    // 更新缓存
    workflowMap.value.set(activeBusinessType.value, JSON.parse(JSON.stringify(editingWorkflow.value)))
    ElMessage.success('保存成功')
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

// ============================================================
// 生命周期
// ============================================================

/** 监听权限模板变化，自动重新加载审批流程 */
watch(() => permissionStore.currentTemplate, () => {
  loadAllWorkflows()
})

onMounted(() => {
  loadAllWorkflows()
})
</script>

<template>
  <div class="approval-workflow-config">
    <div class="section-header">
      <div class="section-desc">配置各业务类型的审批流程节点、审批人和条件分支</div>
      <StatusTag
        v-if="currentWorkflow"
        :status="currentWorkflow.enabled ? 'active' : 'inactive'"
        :label="currentWorkflow.enabled ? '已启用' : '未启用'"
        size="small"
      />
    </div>

    <div class="workflow-layout">
      <!-- 左侧：业务类型列表 -->
      <aside class="business-type-panel">
        <div class="panel-title">业务类型</div>
        <el-menu
          :default-active="activeBusinessType"
          class="business-type-menu"
          @select="handleBusinessTypeChange"
        >
          <el-menu-item
            v-for="item in BusinessTypeOptions"
            :key="item.value"
            :index="item.value"
          >
            <span class="menu-item-label">{{ item.label }}</span>
            <StatusTag
              v-if="workflowMap.get(item.value)?.enabled"
              status="active"
              label="启用"
              size="small"
            />
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 右侧：审批流程编辑器 -->
      <main class="workflow-editor" v-loading="loading">
        <template v-if="currentWorkflow">
          <!-- 顶部：基本信息 -->
          <div class="editor-header">
            <div class="header-left">
              <h3 class="editor-title">{{ activeBusinessTypeLabel }}审批流程</h3>
              <el-select
                v-model="selectedTemplateCode"
                placeholder="选择权限模板"
                size="small"
                class="template-select"
                @change="handleTemplateChange"
              >
                <el-option
                  v-for="opt in TemplateCodeOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </div>
            <div class="header-right">
              <span class="switch-label">启用流程</span>
              <el-switch
                :model-value="currentWorkflow.enabled"
                @change="handleEnabledChange"
              />
            </div>
          </div>

          <!-- 中部：审批节点列表 -->
          <div class="editor-section">
            <div class="section-bar">
              <span class="section-title">审批节点</span>
              <el-button type="primary" size="small" :icon="Plus" @click="addNode">添加节点</el-button>
            </div>

            <div v-if="currentNodes.length === 0" class="empty-hint">
              暂无审批节点，请点击「添加节点」创建
            </div>

            <div v-else class="node-list">
              <div
                v-for="(node, index) in currentNodes"
                :key="node.nodeId"
                class="node-card"
              >
                <div class="node-header">
                  <span class="node-order">{{ index + 1 }}</span>
                  <el-input
                    v-model="node.nodeName"
                    placeholder="节点名称"
                    size="small"
                    class="node-name-input"
                  />
                  <div class="node-actions">
                    <el-button
                      link
                      size="small"
                      :disabled="index === 0"
                      @click="moveNodeUp(index)"
                    >
                      上移
                    </el-button>
                    <el-button
                      link
                      size="small"
                      :disabled="index === currentNodes.length - 1"
                      @click="moveNodeDown(index)"
                    >
                      下移
                    </el-button>
                    <el-button
                      link
                      type="danger"
                      size="small"
                      @click="removeNode(index)"
                    >
                      删除
                    </el-button>
                  </div>
                </div>

                <div class="node-fields">
                  <div class="field-row">
                    <div class="field-item">
                      <label class="field-label">审批人类型</label>
                      <el-select v-model="node.approverType" size="small" class="field-select">
                        <el-option
                          v-for="opt in ApproverTypeOptions"
                          :key="opt.value"
                          :label="opt.label"
                          :value="opt.value"
                        />
                      </el-select>
                    </div>
                    <div class="field-item field-item--grow">
                      <label class="field-label">审批人值</label>
                      <el-input
                        v-model="node.approverValue"
                        placeholder="角色编码/上级编码/用户ID"
                        size="small"
                      />
                    </div>
                  </div>
                  <div class="field-row">
                    <div class="field-item">
                      <label class="field-label">审批模式</label>
                      <el-select v-model="node.approveMode" size="small" class="field-select">
                        <el-option
                          v-for="opt in ApproveModeOptions"
                          :key="opt.value"
                          :label="opt.label"
                          :value="opt.value"
                        />
                      </el-select>
                    </div>
                    <div class="field-item">
                      <label class="field-label">超时时间(小时)</label>
                      <el-input-number
                        v-model="node.timeoutHours"
                        :min="1"
                        :max="720"
                        size="small"
                        class="field-number"
                      />
                    </div>
                    <div class="field-item">
                      <label class="field-label">超时处理</label>
                      <el-select v-model="node.timeoutAction" size="small" class="field-select">
                        <el-option
                          v-for="opt in TimeoutActionOptions"
                          :key="opt.value"
                          :label="opt.label"
                          :value="opt.value"
                        />
                      </el-select>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 底部：条件分支配置 -->
          <div class="editor-section">
            <div class="section-bar">
              <span class="section-title">条件分支</span>
              <el-button size="small" :icon="Plus" @click="addCondition">添加条件</el-button>
            </div>

            <div v-if="currentConditions.length === 0" class="empty-hint">
              暂无条件分支，满足不同条件时走不同的审批路径
            </div>

            <div v-else class="condition-list">
              <div
                v-for="(cond, index) in currentConditions"
                :key="cond.conditionId"
                class="condition-card"
              >
                <div class="condition-fields">
                  <div class="field-item">
                    <label class="field-label">条件字段</label>
                    <el-select v-model="cond.field" size="small" class="field-select">
                      <el-option
                        v-for="opt in ConditionFieldOptions"
                        :key="opt.value"
                        :label="opt.label"
                        :value="opt.value"
                      />
                    </el-select>
                  </div>
                  <div class="field-item">
                    <label class="field-label">操作符</label>
                    <el-select v-model="cond.operator" size="small" class="field-select">
                      <el-option
                        v-for="opt in ConditionOperatorOptions"
                        :key="opt.value"
                        :label="opt.label"
                        :value="opt.value"
                      />
                    </el-select>
                  </div>
                  <div class="field-item field-item--grow">
                    <label class="field-label">条件值</label>
                    <el-input
                      v-model="cond.value"
                      placeholder="输入条件值"
                      size="small"
                    />
                  </div>
                  <div class="field-item field-item--grow">
                    <label class="field-label">跳转节点</label>
                    <el-select
                      v-model="cond.targetNodeIds"
                      multiple
                      size="small"
                      class="field-select"
                      placeholder="选择跳转节点"
                    >
                      <el-option
                        v-for="opt in nodeOptions"
                        :key="opt.value"
                        :label="opt.label"
                        :value="opt.value"
                      />
                    </el-select>
                  </div>
                </div>
                <el-button
                  link
                  type="danger"
                  size="small"
                  class="condition-delete"
                  @click="removeCondition(index)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="editor-footer">
            <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
            <el-button :icon="RefreshRight" @click="handleResetToDefault">重置为默认</el-button>
          </div>
        </template>
      </main>
    </div>
  </div>
</template>

<style scoped lang="scss">
.approval-workflow-config {
  width: 100%;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-4);
  flex-wrap: wrap;
  gap: var(--fts-space-3);
}

.section-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
}

// ========== 左右布局 ==========
.workflow-layout {
  display: flex;
  gap: var(--fts-space-4);
  min-height: 500px;
}

// ========== 左侧面板 ==========
.business-type-panel {
  width: 200px;
  flex-shrink: 0;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  overflow: hidden;
}

.panel-title {
  padding: var(--fts-space-3) var(--fts-space-4);
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
  border-bottom: 1px solid var(--fts-border-primary);
}

.business-type-menu {
  border-right: none;

  .el-menu-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 48px;
    line-height: 48px;
    padding: 0 var(--fts-space-4);

    .menu-item-label {
      font-size: var(--fts-font-size-sm);
    }
  }
}

// ========== 右侧编辑器 ==========
.workflow-editor {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

// ========== 编辑器头部 ==========
.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-4);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  flex-wrap: wrap;
  gap: var(--fts-space-3);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-4);
}

.editor-title {
  font-size: var(--fts-font-size-lg);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0;
}

.template-select {
  width: 160px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.switch-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

// ========== 编辑区域 ==========
.editor-section {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4);
}

.section-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-3);
}

.section-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.empty-hint {
  padding: var(--fts-space-6) var(--fts-space-4);
  text-align: center;
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-quaternary);
}

// ========== 节点卡片 ==========
.node-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.node-card {
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  transition: border-color 0.2s;

  &:hover {
    border-color: var(--fts-primary);
  }
}

.node-header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-3);
}

.node-order {
  width: 24px;
  height: 24px;
  border-radius: var(--fts-radius-full);
  background: var(--fts-primary);
  color: var(--fts-text-inverse, #fff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fts-font-size-xs);
  font-weight: 600;
  flex-shrink: 0;
}

.node-name-input {
  flex: 1;
  max-width: 240px;
}

.node-actions {
  display: flex;
  gap: var(--fts-space-1);
  margin-left: auto;
}

.node-fields {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.field-row {
  display: flex;
  gap: var(--fts-space-3);
  flex-wrap: wrap;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
  min-width: 140px;

  &--grow {
    flex: 1;
    min-width: 160px;
  }
}

.field-label {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.field-select {
  width: 100%;
}

.field-number {
  width: 120px;
}

// ========== 条件卡片 ==========
.condition-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.condition-card {
  display: flex;
  align-items: flex-end;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  transition: border-color 0.2s;

  &:hover {
    border-color: var(--fts-warning);
  }
}

.condition-fields {
  display: flex;
  gap: var(--fts-space-3);
  flex: 1;
  flex-wrap: wrap;
}

.condition-delete {
  flex-shrink: 0;
  margin-bottom: 1px;
}

// ========== 底部按钮 ==========
.editor-footer {
  display: flex;
  gap: var(--fts-space-3);
  padding-top: var(--fts-space-2);
}
</style>
