<script setup lang="ts">
/**
 * 系统配置页面
 *
 * 四 Tab 结构：
 * - Tab 1: 系统配置（/v1/sys-config）全局扁平配置管理，支持分组、编辑、重置、启停、历史
 * - Tab 2: 系统设置（/v1/settings）分层 scope 配置管理，支持 global/store/user 三层作用域
 * - Tab 3: 字典管理（/v1/dict）字典类型与字典项管理，支持分组、启停、缓存清理
 * - Tab 4: 安全维护（运维操作：重置登录失败计数等）
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { sysConfigApi } from '@/api/system/sys-config'
import { sysSettingApi, type SysSettingItem, type SettingScopeType } from '@/api/system/sys-setting'
import { authApi } from '@/api/auth'
import DictManagementTab from './components/DictManagementTab.vue'
import {
  ConfigValueType,
  type SysConfigItem,
  type ConfigGroupInfo,
  type ConfigHistoryRecord,
  type SysConfigStats,
} from '@/types/sys-config'

/* ===== Tab 状态 ===== */
const route = useRoute()
/** 支持通过路由 query.tab 指定初始 Tab（如菜单"字典管理"跳转 ?tab=dict） */
type SystemTabKey = 'config' | 'setting' | 'maintenance' | 'dict'
const validTabKeys: SystemTabKey[] = ['config', 'setting', 'maintenance', 'dict']
const initialTab = ((): SystemTabKey => {
  const q = route.query.tab
  if (typeof q === 'string' && validTabKeys.includes(q as SystemTabKey)) {
    return q as SystemTabKey
  }
  return 'config'
})()
const activeTab = ref<SystemTabKey>(initialTab)

/** 监听路由 query.tab 变化（支持从菜单"字典管理"在已打开页面内切换 Tab） */
watch(() => route.query.tab, (val) => {
  if (typeof val === 'string' && validTabKeys.includes(val as SystemTabKey)) {
    activeTab.value = val as SystemTabKey
  }
})

/* ===== 统计信息 ===== */
const stats = ref<SysConfigStats>({
  totalConfigs: 0,
  enabledConfigs: 0,
  disabledConfigs: 0,
  sensitiveConfigs: 0,
  readonlyConfigs: 0,
  editableConfigs: 0,
  groups: [],
  totalHistoryRecords: 0,
})

const statsCards = computed(() => [
  { icon: 'Setting', label: '配置总数', value: stats.value.totalConfigs, colorType: 'primary' as const },
  { icon: 'CircleCheck', label: '已启用', value: stats.value.enabledConfigs, colorType: 'success' as const },
  { icon: 'Lock', label: '只读配置', value: stats.value.readonlyConfigs, colorType: 'info' as const },
  { icon: 'Warning', label: '敏感配置', value: stats.value.sensitiveConfigs, colorType: 'warning' as const },
])

/* ===== Tab 1: 系统配置 ===== */
const configLoading = ref(false)
const configGroups = ref<ConfigGroupInfo[]>([])
const selectedGroup = ref<string>('')
const configList = ref<SysConfigItem[]>([])

const configColumns: DataTableColumn[] = [
  { prop: 'configName', label: '配置名', minWidth: 140, showOverflowTooltip: true },
  { prop: 'configKey', label: '配置键', minWidth: 200, showOverflowTooltip: true },
  { prop: 'configValue', label: '配置值', minWidth: 160, slot: 'configValue', showOverflowTooltip: true },
  { prop: 'valueType', label: '值类型', minWidth: 90, slot: 'valueType' },
  { prop: 'isEnabled', label: '启用', minWidth: 70, align: 'center', slot: 'isEnabled' },
  { prop: 'isReadonly', label: '只读', minWidth: 70, align: 'center', slot: 'isReadonly' },
  { prop: 'sortOrder', label: '排序', minWidth: 70, align: 'center' },
]

async function loadStats(): Promise<void> {
  try {
    stats.value = await sysConfigApi.getStats()
  } catch {
    // 统计加载失败不影响主流程
  }
}

async function loadConfigGroups(): Promise<void> {
  try {
    const groups = await sysConfigApi.getGroups()
    configGroups.value = groups || []
    if (configGroups.value.length > 0 && !selectedGroup.value) {
      selectedGroup.value = configGroups.value[0].groupName
      await loadConfigList()
    }
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载配置分组失败')
  }
}

async function loadConfigList(): Promise<void> {
  if (!selectedGroup.value) {
    configList.value = []
    return
  }
  configLoading.value = true
  try {
    const res = await sysConfigApi.getList({ configGroup: selectedGroup.value, size: 200 })
    configList.value = res?.records || []
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载配置列表失败')
    configList.value = []
  } finally {
    configLoading.value = false
  }
}

async function handleGroupClick(groupName: string): Promise<void> {
  selectedGroup.value = groupName
  await loadConfigList()
}

/* ===== 配置编辑对话框 ===== */
const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance>()
const editSubmitLoading = ref(false)
const editForm = ref<{
  configKey: string
  configName: string
  configValue: string
  valueType: ConfigValueType
  isSensitive: number
  isReadonly: number
  description: string
}>({
  configKey: '',
  configName: '',
  configValue: '',
  valueType: ConfigValueType.STRING,
  isSensitive: 0,
  isReadonly: 0,
  description: '',
})

const editRules: FormRules = {
  configValue: [{ required: true, message: '请输入配置值', trigger: 'blur' }],
}

/** 数值类型配置值的双向转换（el-input-number 需要 number 类型） */
const editNumericValue = computed({
  get: () => {
    const num = Number(editForm.value.configValue)
    return Number.isNaN(num) ? 0 : num
  },
  set: (val: number | undefined) => {
    editForm.value.configValue = String(val ?? 0)
  },
})

function openEditDialog(row: SysConfigItem): void {
  if (row.isReadonly === 1) {
    ElMessage.warning('只读配置不允许编辑')
    return
  }
  editForm.value = {
    configKey: row.configKey,
    configName: row.configName,
    configValue: row.configValue || '',
    valueType: row.valueType,
    isSensitive: row.isSensitive,
    isReadonly: row.isReadonly,
    description: row.description || '',
  }
  editDialogVisible.value = true
}

async function submitEdit(): Promise<void> {
  if (!editFormRef.value) return
  try {
    await editFormRef.value.validate()
  } catch {
    return
  }
  editSubmitLoading.value = true
  try {
    await sysConfigApi.updateValue(editForm.value.configKey, editForm.value.configValue)
    ElMessage.success('配置已更新')
    editDialogVisible.value = false
    await loadConfigList()
    await loadStats()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '更新失败')
  } finally {
    editSubmitLoading.value = false
  }
}

/* ===== 重置默认值 ===== */
async function handleReset(row: SysConfigItem): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定将配置「${row.configName}」重置为默认值？`,
      '重置确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
    await sysConfigApi.resetToDefault(row.configKey)
    ElMessage.success('已重置为默认值')
    await loadConfigList()
  } catch (error: unknown) {
    if (error === 'cancel' || error === 'close') return
    if (error instanceof Error) ElMessage.error(error.message || '重置失败')
  }
}

/* ===== 启用/禁用 ===== */
async function handleToggleEnabled(row: SysConfigItem): Promise<void> {
  const action = row.isEnabled === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定${action}配置「${row.configName}」？`,
      `${action}确认`,
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
    )
    if (row.isEnabled === 1) {
      await sysConfigApi.disable(row.configKey)
    } else {
      await sysConfigApi.enable(row.configKey)
    }
    ElMessage.success(`已${action}`)
    await loadConfigList()
    await loadStats()
  } catch (error: unknown) {
    if (error === 'cancel' || error === 'close') return
    if (error instanceof Error) ElMessage.error(error.message || '操作失败')
  }
}

/* ===== 历史记录对话框 ===== */
const historyDialogVisible = ref(false)
const historyLoading = ref(false)
const historyList = ref<ConfigHistoryRecord[]>([])
const historyConfigName = ref('')

const historyColumns: DataTableColumn[] = [
  { prop: 'createdAt', label: '变更时间', minWidth: 160 },
  { prop: 'oldValue', label: '旧值', minWidth: 140, showOverflowTooltip: true },
  { prop: 'newValue', label: '新值', minWidth: 140, showOverflowTooltip: true },
  { prop: 'changeType', label: '变更类型', minWidth: 100, slot: 'changeType' },
  { prop: 'operatorName', label: '操作人', minWidth: 100 },
]

async function handleViewHistory(row: SysConfigItem): Promise<void> {
  historyConfigName.value = row.configName
  historyDialogVisible.value = true
  historyLoading.value = true
  try {
    const res = await sysConfigApi.getHistory({ configId: row.configId, size: 50 })
    historyList.value = res?.records || []
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载历史记录失败')
    historyList.value = []
  } finally {
    historyLoading.value = false
  }
}

/* ===== Tab 2: 系统设置 ===== */
const settingLoading = ref(false)
const settingGroup = ref<string>('')
const settingList = ref<SysSettingItem[]>([])
const settingGroupOptions = ref<string[]>(['system', 'notification', 'security', 'business'])

const settingColumns: DataTableColumn[] = [
  { prop: 'settingName', label: '设置名', minWidth: 140, showOverflowTooltip: true },
  { prop: 'settingKey', label: '设置键', minWidth: 200, showOverflowTooltip: true },
  { prop: 'settingValue', label: '值', minWidth: 160, showOverflowTooltip: true },
  { prop: 'scopeType', label: '作用域', minWidth: 90, slot: 'scopeType' },
  { prop: 'isEnabled', label: '启用', minWidth: 70, align: 'center', slot: 'settingEnabled' },
]

async function loadSettingList(): Promise<void> {
  if (!settingGroup.value) {
    settingList.value = []
    return
  }
  settingLoading.value = true
  try {
    settingList.value = await sysSettingApi.getGroupSettings(settingGroup.value)
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载设置列表失败')
    settingList.value = []
  } finally {
    settingLoading.value = false
  }
}

async function handleSettingGroupChange(): Promise<void> {
  await loadSettingList()
}

/* ===== 设置编辑对话框 ===== */
const settingEditDialogVisible = ref(false)
const settingEditFormRef = ref<FormInstance>()
const settingSubmitLoading = ref(false)
const settingEditForm = ref<{
  settingKey: string
  settingName: string
  scopeType: SettingScopeType
  scopeId: string
  settingValue: string
}>({
  settingKey: '',
  settingName: '',
  scopeType: 'global',
  scopeId: '',
  settingValue: '',
})

const settingEditRules: FormRules = {
  scopeType: [{ required: true, message: '请选择作用域', trigger: 'change' }],
  settingValue: [{ required: true, message: '请输入设置值', trigger: 'blur' }],
  scopeId: [{
    validator: (_rule, value, callback) => {
      if (settingEditForm.value.scopeType !== 'global' && !value) {
        return callback(new Error('门店/用户作用域必须填写ID'))
      }
      callback()
    },
    trigger: 'blur',
  }],
}

function openSettingEditDialog(row: SysSettingItem): void {
  settingEditForm.value = {
    settingKey: row.settingKey,
    settingName: row.settingName,
    scopeType: row.scopeType || 'global',
    scopeId: row.scopeId || '',
    settingValue: row.settingValue || '',
  }
  settingEditDialogVisible.value = true
}

async function submitSettingEdit(): Promise<void> {
  if (!settingEditFormRef.value) return
  try {
    await settingEditFormRef.value.validate()
  } catch {
    return
  }
  settingSubmitLoading.value = true
  try {
    const { settingKey, scopeType, scopeId, settingValue } = settingEditForm.value
    if (scopeType === 'global') {
      await sysSettingApi.setGlobal(settingKey, settingValue)
    } else if (scopeType === 'store') {
      await sysSettingApi.setStore(settingKey, settingValue, scopeId)
    } else {
      await sysSettingApi.setUser(settingKey, settingValue, scopeId)
    }
    ElMessage.success('设置已更新')
    settingEditDialogVisible.value = false
    await loadSettingList()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '更新失败')
  } finally {
    settingSubmitLoading.value = false
  }
}

/* ===== 辅助函数 ===== */
/** 值类型 → StatusTag 状态映射 */
function valueTypeToStatus(vt: ConfigValueType): string {
  const map: Record<ConfigValueType, string> = {
    [ConfigValueType.STRING]: 'info',
    [ConfigValueType.INTEGER]: 'primary',
    [ConfigValueType.LONG]: 'primary',
    [ConfigValueType.DOUBLE]: 'warning',
    [ConfigValueType.BOOLEAN]: 'success',
    [ConfigValueType.JSON]: 'warning',
    [ConfigValueType.TEXT]: 'info',
  }
  return map[vt] || 'info'
}

/** 作用域 → StatusTag 状态映射 */
function scopeToStatus(scope: SettingScopeType): string {
  const map: Record<SettingScopeType, string> = {
    global: 'primary',
    store: 'warning',
    user: 'success',
  }
  return map[scope] || 'info'
}

/** 变更类型 → StatusTag 状态映射 */
function changeTypeToStatus(ct: string): string {
  const map: Record<string, string> = {
    CREATE: 'success',
    UPDATE: 'warning',
    RESET: 'info',
    ENABLE: 'success',
    DISABLE: 'error',
  }
  return map[ct] || 'info'
}

/** 脱敏显示配置值（敏感配置显示 ******） */
function maskValue(row: SysConfigItem): string {
  if (row.isSensitive === 1) return '******'
  return row.configValue || '-'
}

/* ===== Tab 3: 安全维护 ===== */
/** 重置登录失败计数 - 输入的用户名 */
const resetFailCountUsername = ref('')
/** 重置登录失败计数 - 提交中 */
const resetFailCountLoading = ref(false)

/**
 * 重置指定用户的登录失败计数并解锁账号
 * 调用后端 POST /v1/auth/reset-fail-count/{username}
 */
async function handleResetLoginFailCount(): Promise<void> {
  const username = resetFailCountUsername.value.trim()
  if (!username) {
    ElMessage.warning('请输入需要重置的用户名')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认重置用户「${username}」的登录失败计数并解锁账号？此操作不可撤销。`,
      '重置确认',
      { type: 'warning', confirmButtonText: '确认重置', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  resetFailCountLoading.value = true
  try {
    await authApi.resetFailCount(username)
    ElMessage.success(`用户「${username}」的登录失败计数已重置`)
    resetFailCountUsername.value = ''
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '重置失败'
    ElMessage.error(errorMessage)
  } finally {
    resetFailCountLoading.value = false
  }
}

/* ===== 初始化 ===== */
onMounted(async () => {
  await loadStats()
  await loadConfigGroups()
  if (!settingGroup.value && settingGroupOptions.value.length > 0) {
    settingGroup.value = settingGroupOptions.value[0]
  }
  await loadSettingList()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="系统配置" description="系统参数配置、分层设置管理" />

    <!-- 统计卡片 -->
    <section class="stats-section">
      <StatCard
        v-for="stat in statsCards"
        :key="stat.label"
        :icon="stat.icon"
        :label="stat.label"
        :value="String(stat.value)"
        :color-type="stat.colorType"
        variant="bordered"
      />
    </section>

    <!-- Tab 区域 -->
    <div class="system-tabs">
      <div class="tabs-header">
        <div
          v-for="tab in [{ key: 'config', label: '系统配置', icon: 'Setting' }, { key: 'setting', label: '系统设置', icon: 'Tools' }, { key: 'dict', label: '字典管理', icon: 'Collection' }, { key: 'maintenance', label: '安全维护', icon: 'Lock' }]"
          :key="tab.key"
          :class="['tab-item', { 'tab-item--active': activeTab === tab.key }]"
          @click="activeTab = tab.key as SystemTabKey"
        >
          <el-icon :size="16" style="margin-right:6px"><component :is="tab.icon" /></el-icon>
          {{ tab.label }}
        </div>
      </div>

      <!-- Tab 1: 系统配置 -->
      <div v-show="activeTab === 'config'" class="tab-content">
        <div class="config-layout">
          <!-- 左侧分组列表 -->
          <div class="group-list">
            <div class="group-list__header">配置分组</div>
            <div
              v-for="group in configGroups"
              :key="group.groupName"
              :class="['group-item', { 'group-item--active': selectedGroup === group.groupName }]"
              @click="handleGroupClick(group.groupName)"
            >
              <span class="group-item__name">{{ group.groupName }}</span>
              <span class="group-item__count">{{ group.count }}</span>
            </div>
            <el-empty v-if="configGroups.length === 0" description="暂无分组" :image-size="60" />
          </div>

          <!-- 右侧配置列表 -->
          <div class="config-table">
            <DataTable
              :columns="configColumns"
              :data="configList"
              :loading="configLoading"
              :actions-width="220"
              stripe
            >
              <template #configValue="{ row }">
                <span>{{ maskValue(row) }}</span>
              </template>
              <template #valueType="{ row }">
                <StatusTag :status="valueTypeToStatus(row.valueType)" :label="row.valueType" size="small" />
              </template>
              <template #isEnabled="{ row }">
                <StatusTag :status="row.isEnabled === 1 ? 'success' : 'error'" :label="row.isEnabled === 1 ? '启用' : '禁用'" size="small" />
              </template>
              <template #isReadonly="{ row }">
                <StatusTag :status="row.isReadonly === 1 ? 'warning' : 'info'" :label="row.isReadonly === 1 ? '是' : '否'" size="small" />
              </template>
              <template #actions="{ row }">
                <el-button link type="primary" size="small" :disabled="row.isReadonly === 1" @click="openEditDialog(row)">编辑</el-button>
                <el-button link type="primary" size="small" @click="handleReset(row)">重置</el-button>
                <el-button link type="primary" size="small" @click="handleToggleEnabled(row)">{{ row.isEnabled === 1 ? '禁用' : '启用' }}</el-button>
                <el-button link type="primary" size="small" @click="handleViewHistory(row)">历史</el-button>
              </template>
            </DataTable>
          </div>
        </div>
      </div>

      <!-- Tab 2: 系统设置 -->
      <div v-show="activeTab === 'setting'" class="tab-content">
        <div class="setting-toolbar">
          <div class="toolbar-left">
            <el-select
              v-model="settingGroup"
              placeholder="选择分组"
              style="width: 180px"
              size="default"
              :teleported="false"
              @change="handleSettingGroupChange"
            >
              <el-option v-for="g in settingGroupOptions" :key="g" :label="g" :value="g" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-button size="default" @click="loadSettingList">刷新</el-button>
          </div>
        </div>
        <div class="table-section">
          <DataTable
            :columns="settingColumns"
            :data="settingList"
            :loading="settingLoading"
            :actions-width="100"
            stripe
          >
            <template #scopeType="{ row }">
              <StatusTag :status="scopeToStatus(row.scopeType)" :label="row.scopeType" size="small" />
            </template>
            <template #settingEnabled="{ row }">
              <StatusTag :status="row.isEnabled === 1 ? 'success' : 'error'" :label="row.isEnabled === 1 ? '启用' : '禁用'" size="small" />
            </template>
            <template #actions="{ row }">
              <el-button link type="primary" size="small" @click="openSettingEditDialog(row)">编辑</el-button>
            </template>
          </DataTable>
        </div>
      </div>

      <!-- Tab 3: 字典管理 -->
      <div v-show="activeTab === 'dict'" class="tab-content">
        <DictManagementTab />
      </div>

      <!-- Tab 4: 安全维护 -->
      <div v-show="activeTab === 'maintenance'" class="tab-content">
        <div class="maintenance-section">
          <!-- 重置登录失败计数卡片 -->
          <div class="maintenance-card">
            <div class="maintenance-card__header">
              <el-icon :size="20" color="var(--fts-warning)"><Unlock /></el-icon>
              <span class="maintenance-card__title">重置登录失败计数</span>
            </div>
            <div class="maintenance-card__body">
              <p class="maintenance-card__desc">
                当用户因密码错误次数过多被锁定（5次锁定30分钟）或被要求输入验证码（3次）时，
                管理员可通过此功能重置该用户的失败计数并解锁账号。
              </p>
              <div class="maintenance-card__form">
                <el-input
                  v-model="resetFailCountUsername"
                  placeholder="请输入需要重置的用户名"
                  style="width: 240px"
                  clearable
                  @keyup.enter="handleResetLoginFailCount"
                />
                <el-button
                  type="warning"
                  :loading="resetFailCountLoading"
                  @click="handleResetLoginFailCount"
                >
                  重置失败计数
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 配置编辑对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑配置" width="560px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="配置名">
          <span>{{ editForm.configName }}</span>
        </el-form-item>
        <el-form-item label="配置键">
          <span class="config-key-text">{{ editForm.configKey }}</span>
        </el-form-item>
        <el-form-item v-if="editForm.description" label="说明">
          <span class="desc-text">{{ editForm.description }}</span>
        </el-form-item>
        <el-form-item v-if="editForm.isSensitive === 1" label="提示">
          <el-alert title="该配置为敏感配置，请谨慎修改" type="warning" :closable="false" show-icon />
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <!-- 根据值类型显示不同编辑器 -->
          <el-switch
            v-if="editForm.valueType === 'BOOLEAN'"
            v-model="editForm.configValue"
            active-value="true"
            inactive-value="false"
          />
          <el-input-number
            v-else-if="editForm.valueType === 'INTEGER' || editForm.valueType === 'LONG'"
            v-model="editNumericValue"
            :step="1"
            style="width: 100%"
          />
          <el-input-number
            v-else-if="editForm.valueType === 'DOUBLE'"
            v-model="editNumericValue"
            :step="0.1"
            :precision="4"
            style="width: 100%"
          />
          <el-input
            v-else-if="editForm.valueType === 'JSON' || editForm.valueType === 'TEXT'"
            v-model="editForm.configValue"
            type="textarea"
            :rows="4"
            placeholder="请输入配置值"
          />
          <el-input
            v-else
            v-model="editForm.configValue"
            placeholder="请输入配置值"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitLoading" @click="submitEdit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 历史记录对话框 -->
    <el-dialog v-model="historyDialogVisible" :title="`变更历史 - ${historyConfigName}`" width="780px" destroy-on-close>
      <DataTable :columns="historyColumns" :data="historyList" :loading="historyLoading" stripe>
        <template #changeType="{ row }">
          <StatusTag :status="changeTypeToStatus(row.changeType)" :label="row.changeType" size="small" />
        </template>
      </DataTable>
    </el-dialog>

    <!-- 设置编辑对话框 -->
    <el-dialog v-model="settingEditDialogVisible" title="编辑设置" width="520px" destroy-on-close>
      <el-form ref="settingEditFormRef" :model="settingEditForm" :rules="settingEditRules" label-width="100px">
        <el-form-item label="设置名">
          <span>{{ settingEditForm.settingName }}</span>
        </el-form-item>
        <el-form-item label="设置键">
          <span class="config-key-text">{{ settingEditForm.settingKey }}</span>
        </el-form-item>
        <el-form-item label="作用域" prop="scopeType">
          <el-select v-model="settingEditForm.scopeType" style="width: 100%" :teleported="false">
            <el-option label="全局" value="global" />
            <el-option label="门店" value="store" />
            <el-option label="用户" value="user" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="settingEditForm.scopeType !== 'global'" :label="settingEditForm.scopeType === 'store' ? '门店ID' : '用户ID'" prop="scopeId">
          <el-input v-model="settingEditForm.scopeId" :placeholder="`请输入${settingEditForm.scopeType === 'store' ? '门店' : '用户'}ID`" />
        </el-form-item>
        <el-form-item label="设置值" prop="settingValue">
          <el-input v-model="settingEditForm.settingValue" type="textarea" :rows="3" placeholder="请输入设置值" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="settingEditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="settingSubmitLoading" @click="submitSettingEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.system-tabs { background: var(--fts-bg-card); border: 1px solid var(--fts-border-primary); border-radius: var(--fts-radius-lg); margin-top: var(--fts-space-4); }
.tabs-header { display: flex; border-bottom: 1px solid var(--fts-border-primary); padding: 0 var(--fts-space-4); }
.tab-item {
  display: flex; align-items: center; padding: var(--fts-space-3) var(--fts-space-4);
  font-size: var(--fts-font-size-md); color: var(--fts-text-tertiary); cursor: pointer;
  border-bottom: 2px solid transparent; transition: all var(--fts-transition-fast); user-select: none;
}
.tab-item:hover { color: var(--fts-text-primary); background: var(--fts-bg-secondary); }
.tab-item--active { color: var(--fts-primary); border-bottom-color: var(--fts-primary); font-weight: 500; }
.tab-item--active:hover { color: var(--fts-primary); background: transparent; }
.tab-content { padding: var(--fts-space-4); }

/* 配置布局：左侧分组 + 右侧表格 */
.config-layout { display: flex; gap: var(--fts-space-4); min-height: 400px; }
.group-list {
  width: 220px; flex-shrink: 0; background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md); padding: var(--fts-space-2); height: fit-content;
}
.group-list__header { font-size: var(--fts-font-size-sm); color: var(--fts-text-tertiary); padding: var(--fts-space-2) var(--fts-space-3); font-weight: 500; }
.group-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: var(--fts-space-2) var(--fts-space-3); border-radius: var(--fts-radius-sm);
  cursor: pointer; transition: all var(--fts-transition-fast); margin-bottom: var(--fts-space-1);
}
.group-item:hover { background: var(--fts-bg-card); }
.group-item--active { background: var(--fts-bg-card); color: var(--fts-primary); font-weight: 500; }
.group-item__name { font-size: var(--fts-font-size-md); }
.group-item__count { font-size: var(--fts-font-size-sm); color: var(--fts-text-tertiary); }
.config-table { flex: 1; min-width: 0; }

/* 设置工具栏 */
.setting-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--fts-space-3); }
.toolbar-left, .toolbar-right { display: flex; align-items: center; gap: var(--fts-space-3); }

/* 对话框辅助样式 */
.config-key-text { font-family: monospace; font-size: var(--fts-font-size-sm); color: var(--fts-text-secondary); word-break: break-all; }
.desc-text { color: var(--fts-text-tertiary); font-size: var(--fts-font-size-sm); }

/* 安全维护 */
.maintenance-section { max-width: 720px; }
.maintenance-card {
  background: var(--fts-bg-secondary); border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md); padding: var(--fts-space-5); margin-bottom: var(--fts-space-4);
}
.maintenance-card__header { display: flex; align-items: center; gap: var(--fts-space-2); margin-bottom: var(--fts-space-3); }
.maintenance-card__title { font-size: var(--fts-font-size-lg); font-weight: 600; color: var(--fts-text-primary); }
.maintenance-card__body { padding-left: var(--fts-space-1); }
.maintenance-card__desc { color: var(--fts-text-tertiary); font-size: var(--fts-font-size-sm); line-height: 1.6; margin-bottom: var(--fts-space-4); }
.maintenance-card__form { display: flex; gap: var(--fts-space-3); align-items: center; }
</style>
