<script setup lang="ts">
/**
 * 标签模板管理页面
 *
 * 功能：
 * - 标签模板列表查询（按模板类型/启用状态筛选）
 * - 模板 CRUD 操作（编辑/删除/设为默认）
 * - 预览打印功能
 * - 统计卡片展示模板使用情况
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { labelTemplateApi } from '@/api/traceability'
import type { LabelTemplate } from '@/types/traceability'
import type { StatColorType } from '@/types/stat'

/* ===== 标准页面状态 ===== */
const { loading, handleSearch: stdSearch, handleReset: stdReset } = useStandardPage({
  onSearch: loadData,
  onReset: () => {
    resetSearchForm()
    loadData()
  },
})

/* ===== 搜索表单 ===== */
const searchForm = ref<{
  templateType: string
  enabled: boolean | undefined
}>({
  templateType: '',
  enabled: undefined,
})

function resetSearchForm(): void {
  searchForm.value = {
    templateType: '',
    enabled: undefined,
  }
}

/* ===== 表格数据 ===== */
const tableData = ref<LabelTemplate[]>([])

/** 加载列表数据 */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: { templateType?: string; enabled?: boolean } = {}
    if (searchForm.value.templateType) {
      params.templateType = searchForm.value.templateType
    }
    if (searchForm.value.enabled !== undefined) {
      params.enabled = searchForm.value.enabled
    }
    const res = await labelTemplateApi.getList(params)
    tableData.value = res.content
  } catch (error) {
    ElMessage.error('加载标签模板列表失败')
    tableData.value = []
  } finally {
    loading.value = false
  }
}

/* ===== 统计卡片（基于列表数据计算） ===== */
const statsCards = computed(() => {
  const list = tableData.value
  const total = list.length
  const active = list.filter((r) => r.enabled).length
  const archived = list.filter((r) => !r.enabled).length
  const defaultCount = list.filter((r) => r.isDefault).length
  return [
    {
      key: 'total',
      icon: 'CollectionTag',
      label: '标签模板',
      value: total,
      colorType: 'primary' as StatColorType,
    },
    {
      key: 'default',
      icon: 'Star',
      label: '默认模板',
      value: defaultCount,
      colorType: 'success' as StatColorType,
    },
    {
      key: 'active',
      icon: 'CircleCheck',
      label: '使用中',
      value: active,
      colorType: 'info' as StatColorType,
    },
    {
      key: 'archived',
      icon: 'Folder',
      label: '已停用',
      value: archived,
      colorType: 'warning' as StatColorType,
    },
  ]
})

/* ===== 表格列定义 ===== */
const columns: DataTableColumn[] = [
  { prop: 'templateName', label: '模板名称', minWidth: 200, showOverflowTooltip: true },
  { prop: 'templateCode', label: '模板编码', minWidth: 160, showOverflowTooltip: true },
  { prop: 'templateType', label: '模板类型', minWidth: 110, slot: 'templateType', align: 'center' },
  { prop: 'labelWidth', label: '宽度(mm)', minWidth: 90, align: 'right' },
  { prop: 'labelHeight', label: '高度(mm)', minWidth: 90, align: 'right' },
  { prop: 'dpi', label: 'DPI', minWidth: 80, align: 'center' },
  { prop: 'updateTime', label: '最后更新', minWidth: 160, slot: 'updateTime' },
  { prop: 'enabled', label: '状态', minWidth: 95, slot: 'enabled', align: 'center' },
]

/* ===== 状态展示工具方法 ===== */

/** 模板类型标签 */
function getTemplateTypeLabel(type: string): string {
  const map: Record<string, string> = {
    FOOD: '食品标签',
    MATERIAL: '原料标签',
    CUSTOM: '自定义',
  }
  return map[type] || type
}

/** 模板类型 StatusTag status */
function getTemplateTypeTagStatus(type: string): string {
  const map: Record<string, string> = {
    FOOD: 'primary',
    MATERIAL: 'success',
    CUSTOM: 'info',
  }
  return map[type] || 'default'
}

/** 格式化日期时间 */
function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  const normalized = dateStr.replace('T', ' ')
  return normalized.split('.')[0].slice(0, 19)
}

/* ===== 操作功能 ===== */

/** 编辑弹窗 */
const editVisible = ref(false)
const editForm = ref<LabelTemplate>({
  id: '',
  templateName: '',
  templateCode: '',
  templateType: 'FOOD',
  labelWidth: 40,
  labelHeight: 30,
  dpi: 300,
  gapSize: 2,
  printSpeed: 3,
  printDensity: 12,
  direction: 1,
  layoutConfig: '{}',
  dataMapping: '{}',
  enabled: true,
  isDefault: false,
  sortOrder: 1,
  remark: '',
  createTime: '',
  updateTime: '',
})
const editSaving = ref(false)

/** 编辑模板 */
function handleEdit(row: LabelTemplate): void {
  editForm.value = { ...row }
  editVisible.value = true
}

/** 保存编辑 */
async function handleSaveEdit(): Promise<void> {
  editSaving.value = true
  try {
    await labelTemplateApi.update(String(editForm.value.id), editForm.value)
    ElMessage.success('保存成功')
    editVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    editSaving.value = false
  }
}

/** 预览打印（按模板ID打印，使用模板已保存的布局配置）
 * 后端要求 request.labelDataList 或 request.labelData 非空，否则返回失败
 * 此处传入一个空数据对象作为占位，后端会使用模板的默认数据映射生成打印内容
 */
async function handlePreviewPrint(row: LabelTemplate): Promise<void> {
  try {
    const result = await labelTemplateApi.printByTemplateId(String(row.id), {
      labelDataList: [{}],
    })
    if (result.success) {
      ElMessage.success(result.message || '打印指令已发送')
    } else {
      ElMessage.warning(result.message || '打印失败')
    }
  } catch (error) {
    ElMessage.error('打印失败，请检查打印服务是否可用')
  }
}

/** 设为默认 */
async function handleSetDefault(row: LabelTemplate): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确认将「${row.templateName}」设为默认模板？`,
      '设为默认',
      { type: 'info' },
    )
    await labelTemplateApi.setDefault(String(row.id))
    ElMessage.success('已设为默认模板')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('设置默认模板失败')
    }
  }
}

/** 删除模板 */
async function handleDelete(row: LabelTemplate): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确认删除模板「${row.templateName}」？此操作不可恢复。`,
      '删除确认',
      { type: 'warning' },
    )
    await labelTemplateApi.delete(String(row.id))
    ElMessage.success('删除成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/* ===== 初始化 ===== */
onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="标签模板" description="追溯标签模板设计与打印管理" />

    <!-- 统计卡片 -->
    <div class="stats-section">
      <StatCard
        v-for="stat in statsCards"
        :key="stat.key"
        :icon="stat.icon"
        :label="stat.label"
        :value="stat.value"
        :color-type="stat.colorType"
        variant="bordered"
      />
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="toolbar-left">
        <el-select
          v-model="searchForm.templateType"
          placeholder="模板类型"
          clearable
          style="width: 140px"
        >
          <el-option label="食品标签" value="FOOD" />
          <el-option label="原料标签" value="MATERIAL" />
          <el-option label="自定义" value="CUSTOM" />
        </el-select>
        <el-select
          v-model="searchForm.enabled"
          placeholder="启用状态"
          clearable
          style="width: 130px"
        >
          <el-option label="启用" :value="true" />
          <el-option label="停用" :value="false" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="stdSearch">查询</el-button>
        <el-button @click="stdReset">重置</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-section">
      <DataTable :columns="columns" :data="tableData" :loading="loading" stripe>
        <!-- 模板类型 -->
        <template #templateType="{ row }">
          <StatusTag
            :status="getTemplateTypeTagStatus(row.templateType)"
            :label="getTemplateTypeLabel(row.templateType)"
            size="small"
          />
        </template>

        <!-- 最后更新 -->
        <template #updateTime="{ row }">
          {{ formatDateTime(row.updateTime) }}
        </template>

        <!-- 启用状态 -->
        <template #enabled="{ row }">
          <StatusTag
            :status="row.enabled ? 'success' : 'info'"
            :label="row.enabled ? '使用中' : '已停用'"
            size="small"
          />
        </template>

        <!-- 操作列 -->
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="primary" size="small" @click="handlePreviewPrint(row)">预览打印</el-button>
          <el-button
            v-if="!row.isDefault && row.enabled"
            link
            type="primary"
            size="small"
            @click="handleSetDefault(row)"
          >
            设为默认
          </el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </DataTable>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑标签模板" width="600px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="模板名称">
          <el-input v-model="editForm.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板编码">
          <el-input v-model="editForm.templateCode" placeholder="请输入模板编码" />
        </el-form-item>
        <el-form-item label="模板类型">
          <el-select v-model="editForm.templateType" :teleported="false" placeholder="请选择模板类型">
            <el-option label="食品标签" value="FOOD" />
            <el-option label="原料标签" value="MATERIAL" />
            <el-option label="自定义标签" value="CUSTOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签宽度">
          <el-input-number v-model="editForm.labelWidth" :min="10" :max="200" />
          <span style="margin-left: 8px; color: var(--fts-text-secondary)">mm</span>
        </el-form-item>
        <el-form-item label="标签高度">
          <el-input-number v-model="editForm.labelHeight" :min="10" :max="200" />
          <span style="margin-left: 8px; color: var(--fts-text-secondary)">mm</span>
        </el-form-item>
        <el-form-item label="DPI">
          <el-input-number v-model="editForm.dpi" :min="150" :max="600" :step="50" />
        </el-form-item>
        <el-form-item label="标签间隙">
          <el-input-number v-model="editForm.gapSize" :min="0" :max="10" />
          <span style="margin-left: 8px; color: var(--fts-text-secondary)">mm</span>
        </el-form-item>
        <el-form-item label="打印速度">
          <el-input-number v-model="editForm.printSpeed" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="打印浓度">
          <el-input-number v-model="editForm.printDensity" :min="1" :max="15" />
        </el-form-item>
        <el-form-item label="打印方向">
          <el-select v-model="editForm.direction" :teleported="false">
            <el-option label="正向" :value="1" />
            <el-option label="反向" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="editForm.enabled" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
  padding: var(--fts-space-4) 0;
}

.search-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-4);
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius) var(--fts-page-radius) 0 0;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
    flex-wrap: wrap;
  }

  .toolbar-right {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
  }
}

.table-section {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-top: none;
  border-radius: 0 0 var(--fts-page-radius) var(--fts-page-radius);
  padding: var(--fts-space-4);
}
</style>
