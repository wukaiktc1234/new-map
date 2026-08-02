<script setup lang="ts">
import { ref, computed } from 'vue'
import { WarningFilled, InfoFilled, CircleCheckFilled } from '@element-plus/icons-vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { usePermissionStore } from '@/stores/permission'

const permissionStore = usePermissionStore()

const loading = ref(false)
const searchKeyword = ref('')
const selectedEventType = ref('')
const selectedRiskLevel = ref('')

interface EventTypeInfo {
  key: string
  label: string
  icon: string
  color: string
}

const eventTypes: EventTypeInfo[] = [
  { key: 'TEMPLATE_APPLIED', label: '模板应用', icon: 'Document', color: 'primary' },
  { key: 'DOMAIN_PERMISSION_CHANGED', label: '域权限变更', icon: 'Grid', color: 'warning' },
  { key: 'USER_OVERRIDE_CREATED', label: '用户覆盖创建', icon: 'User', color: 'success' },
  { key: 'ROLE_UPDATED', label: '角色更新', icon: 'UserFilled', color: 'info' },
]

const riskLevels = [
  { key: '', label: '全部' },
  { key: 'HIGH', label: '高风险' },
  { key: 'MEDIUM', label: '中风险' },
  { key: 'LOW', label: '低风险' },
]

/** 从Store读取真实审计日志（F-006：替换静态mock数据） */
const filteredLogs = computed(() => {
  let result = [...permissionStore.auditLogs]

  if (selectedEventType.value) {
    result = result.filter(l => l.eventType === selectedEventType.value)
  }

  if (selectedRiskLevel.value) {
    result = result.filter(l => l.riskLevel === selectedRiskLevel.value)
  }

  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(l =>
      l.operatorName.toLowerCase().includes(keyword) ||
      l.eventName.toLowerCase().includes(keyword) ||
      l.targetRole?.toLowerCase().includes(keyword) ||
      l.targetUser?.toLowerCase().includes(keyword) ||
      l.detail.toLowerCase().includes(keyword)
    )
  }

  return result // 已按时间倒序（addAuditLog使用unshift）
})

const columns: DataTableColumn[] = [
  { prop: 'timestamp', label: '时间', minWidth: 170, fixed: 'left' as const, slot: 'time' as const },
  { prop: 'eventName', label: '事件', minWidth: 140, slot: 'event' as const },
  { prop: 'riskLevel', label: '风险等级', minWidth: 100, align: 'center', slot: 'risk' as const },
  { prop: 'operatorName', label: '操作人', minWidth: 120 },
  { prop: 'detail', label: '详情摘要', minWidth: 260, showOverflowTooltip: true },
  { prop: 'ipAddress', label: 'IP地址', minWidth: 130, slot: 'ip' as const },
]

function getEventIcon(eventType: string): string {
  return eventTypes.find(t => t.key === eventType)?.icon || 'Notebook'
}

function getEventColor(eventType: string): string {
  return eventTypes.find(t => t.key === eventType)?.color || 'info'
}

function getRiskType(level: string): string {
  const map: Record<string, string> = {
    HIGH: 'error',
    MEDIUM: 'warning',
    LOW: 'success',
  }
  return map[level] || 'info'
}

function getRiskLabel(level: string): string {
  const map: Record<string, string> = {
    HIGH: '高',
    MEDIUM: '中',
    LOW: '低',
  }
  return map[level] || level
}

function formatTime(date?: Date): string {
  if (!date) return '-'
  return new Date(date).toLocaleString()
}

// ========== 详情弹窗（F-006） ==========
const detailVisible = ref(false)
const detailData = ref<(typeof permissionStore.auditLogs)[number] | null>(null)

function showDetail(row: unknown) {
  detailData.value = row as NonNullable<typeof detailData.value>
  detailVisible.value = true
}
</script>

<template>
  <div class="audit-log">
    <div class="section-header">
      <div class="section-desc">记录所有权限相关的操作日志，支持按事件类型和风险等级筛选</div>
    </div>

    <div class="filter-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索操作人、目标对象..."
        clearable
        prefix-icon="Search"
        style="width: 260px"
      />
      
      <div class="event-filters">
        <span 
          v-for="type in eventTypes" 
          :key="type.key"
          :class="['event-chip', { 'event-chip--active': selectedEventType === type.key }]"
          @click="selectedEventType = selectedEventType === type.key ? '' : type.key"
        >
          <el-icon :size="12"><component :is="type.icon" /></el-icon>
          {{ type.label }}
        </span>
      </div>

      <div class="risk-filters">
        <span 
          v-for="level in riskLevels" 
          :key="level.key"
          :class="['risk-chip', { 
            'risk-chip--active': selectedRiskLevel === level.key,
            [`risk-chip--${level.key.toLowerCase()}`]: level.key,
          }]"
          @click="selectedRiskLevel = level.key"
        >
          {{ level.label }}
        </span>
      </div>
    </div>

    <div class="stats-summary">
      <div class="summary-item summary-item--high">
        <el-icon><WarningFilled /></el-icon>
        <span>{{ filteredLogs.filter(l => l.riskLevel === 'HIGH').length }}</span>
        <span>高风险</span>
      </div>
      <div class="summary-item summary-item--medium">
        <el-icon><InfoFilled /></el-icon>
        <span>{{ filteredLogs.filter(l => l.riskLevel === 'MEDIUM').length }}</span>
        <span>中风险</span>
      </div>
      <div class="summary-item summary-item--low">
        <el-icon><CircleCheckFilled /></el-icon>
        <span>{{ filteredLogs.filter(l => l.riskLevel === 'LOW').length }}</span>
        <span>低风险</span>
      </div>
    </div>

    <div class="table-section">
      <DataTable 
        :columns="columns" 
        :data="filteredLogs" 
        :loading="loading" 
        stripe
        @row-click="showDetail"
      >
        <template #time="{ row }">
          <div class="time-cell">
            <span class="time-value">{{ formatTime(row.timestamp) }}</span>
          </div>
        </template>
        <template #event="{ row }">
          <div class="event-cell">
            <StatusTag :status="getEventColor(row.eventType)" size="small">
              {{ row.eventName }}
            </StatusTag>
          </div>
        </template>
        <template #risk="{ row }">
          <StatusTag :status="getRiskType(row.riskLevel)" :label="getRiskLabel(row.riskLevel)" size="small" />
        </template>
        <template #ip="{ row }">
          <code class="ip-code">{{ row.ipAddress || '本地' }}</code>
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="showDetail(row)">详情</el-button>
        </template>
      </DataTable>

      <div class="table-footer">
        共 {{ filteredLogs.length }} 条记录
        <span v-if="permissionStore.auditLogs.length === 0" class="empty-hint">
          暂无操作日志。应用模板、修改域权限、同步菜单等操作会自动记录。
        </span>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="操作日志详情"
      width="800px"
      top="4vh"
      append-to-body
    >
      <div v-if="detailData" class="detail-content">
        <!-- 事件概览 -->
        <div class="detail-section">
          <div class="section-title">事件概览</div>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="field-label">事件类型</span>
              <StatusTag :status="getEventColor(detailData.eventType)" size="small">
                {{ detailData.eventName }}
              </StatusTag>
            </div>
            <div class="detail-field">
              <span class="field-label">风险等级</span>
              <StatusTag :status="getRiskType(detailData.riskLevel)" :label="getRiskLabel(detailData.riskLevel)" size="small" />
            </div>
            <div class="detail-field">
              <span class="field-label">操作时间</span>
              <span class="field-value">{{ formatTime(detailData.timestamp) }}</span>
            </div>
          </div>
        </div>

        <!-- 操作人信息 -->
        <div class="detail-section">
          <div class="section-title">操作人信息</div>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="field-label">操作人</span>
              <span class="field-value">{{ detailData.operatorName }}</span>
            </div>
            <div class="detail-field" v-if="detailData.targetRole">
              <span class="field-label">目标角色/模板</span>
              <span class="field-value">{{ detailData.targetRole }}</span>
            </div>
            <div class="detail-field field--full" v-if="detailData.targetUser">
              <span class="field-label">目标用户</span>
              <span class="field-value">{{ detailData.targetUser }}</span>
            </div>
          </div>
        </div>

        <!-- 来源信息（IP + 机器识别） -->
        <div class="detail-section">
          <div class="section-title">来源信息</div>
          <div class="detail-grid">
            <div class="detail-field">
              <span class="field-label">IP地址</span>
              <code class="ip-code">{{ detailData.ipAddress || '—' }}</code>
            </div>
            <div class="detail-field">
              <span class="field-label">机器识别</span>
              <span class="field-value field-value--mono">{{ detailData.userAgent || '—' }}</span>
            </div>
          </div>
        </div>

        <!-- 操作详情 -->
        <div class="detail-section">
          <div class="section-title">操作详情</div>
          <div class="reason-box">{{ detailData.detail }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.audit-log {
  width: 100%;
}

.section-header {
  margin-bottom: var(--fts-space-4);
}

.section-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
  flex-wrap: wrap;
}

.event-filters {
  display: flex;
  gap: var(--fts-space-2);
  flex-wrap: wrap;
}

.event-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-1);
  padding: var(--fts-space-1) var(--fts-space-3);
  border-radius: var(--fts-radius-full);
  background: var(--fts-bg-secondary);
  color: var(--fts-text-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
  user-select: none;
  font-size: var(--fts-font-size-sm);

  &:hover {
    background: var(--fts-bg-tertiary);
  }

  &--active {
    background: var(--fts-primary);
    color: var(--fts-text-inverse, #fff);
  }
}

.risk-filters {
  display: flex;
  gap: var(--fts-space-2);
}

.risk-chip {
  padding: var(--fts-space-1) var(--fts-space-3);
  border-radius: var(--fts-radius-full);
  background: var(--fts-bg-secondary);
  color: var(--fts-text-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
  user-select: none;
  font-size: var(--fts-font-size-sm);

  &:hover {
    background: var(--fts-bg-tertiary);
  }

  &--active {
    &.risk-chip--high {
      background: rgba(var(--fts-error-rgb, 245, 108, 108), 0.1);
      color: var(--fts-error);
    }
    &.risk-chip--medium {
      background: rgba(var(--fts-warning-rgb, 230, 162, 60), 0.1);
      color: var(--fts-warning);
    }
    &.risk-chip--low {
      background: rgba(var(--fts-success-rgb, 103, 194, 58), 0.1);
      color: var(--fts-success);
    }
  }
}

.table-section {
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-3);
}

.time-cell {
  .time-value {
    white-space: nowrap;
  }
}

.event-cell {
  display: inline-block;
}

.ip-code {
  font-family: monospace;
  font-size: var(--fts-font-size-xs);
  background: var(--fts-bg-tertiary);
  padding: 2px 6px;
  border-radius: var(--fts-radius-sm);
  color: var(--fts-text-secondary);
}

.table-footer {
  padding: var(--fts-space-3) var(--fts-space-4) 0;
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  text-align: right;
}

.stats-summary {
  display: flex;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
}

.summary-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-3);
  border-radius: var(--fts-radius-sm);
  font-size: var(--fts-font-size-sm);

  .el-icon {
    font-size: 16px;
  }

  span:nth-child(2) {
    font-weight: 600;
    font-size: var(--fts-font-size-lg);
    line-height: 1;
  }

  span:last-child {
    color: var(--fts-text-tertiary);
    font-size: var(--fts-font-size-xs);
  }

  &--high {
    background: rgba(var(--fts-error-rgb, 245, 108, 108), 0.08);
    color: var(--fts-error);
  }

  &--medium {
    background: rgba(var(--fts-warning-rgb, 230, 162, 60), 0.08);
    color: var(--fts-warning);
  }

  &--low {
    background: rgba(var(--fts-success-rgb, 103, 194, 58), 0.08);
    color: var(--fts-success);
  }
}

.empty-hint {
  margin-left: var(--fts-space-3);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);
}

// ========== 详情弹窗样式（专业布局） ==========
.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.detail-section {
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
}

.section-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-3);
  padding-bottom: var(--fts-space-2);
  border-bottom: 1px solid var(--fts-border-primary);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--fts-space-3) var(--fts-space-6);
}

.detail-field {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);

  &--full {
    grid-column: 1 / -1;
  }
}

.field-label {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.field-value {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  line-height: 1.5;

  &--mono {
    font-family: 'Cascadia Code', 'Fira Code', monospace;
    font-size: var(--fts-font-size-xs);
    word-break: break-all;
  }
}

.reason-box {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  line-height: 1.7;
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-sm);
  border-left: 3px solid var(--fts-warning);
}
</style>
