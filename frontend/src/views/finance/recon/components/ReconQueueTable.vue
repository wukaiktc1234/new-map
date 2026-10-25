<script setup lang="ts">
/**
 * 对账工作台 - 差异队列（P3-1）
 *
 * 职责：筛选区常显（status / type / runId）+ 原生表格 + 分页。
 * 时间窗（默认 7 天）由父组件计算后直接传给 R8，**前端不做二次过滤**。
 *
 * 每行带 `data-test="queue-row"`（E2E 依赖），并附 `data-difference-id` / `data-status`。
 */
import { computed } from 'vue'
import {
  RECON_STATUS_LABELS,
  RECON_TYPE_LABELS,
  formatFen,
} from '@/types/finance-recon'
import type { ReconDifferenceVO } from '@/types/finance-recon'

interface Props {
  rows: ReconDifferenceVO[]
  total: number
  current: number
  size: number
  loading?: boolean
  statusFilter: string
  typeFilter: string
  runIdFilter: string
  /** 口径 B：默认只显示当前跑批；true 表示用户显式切到「全部批次」 */
  scopeAllBatches?: boolean
  currentRunId?: number | null
  selectedId: number | null
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  scopeAllBatches: false,
  currentRunId: null,
})

const emit = defineEmits<{
  (e: 'update:statusFilter', value: string): void
  (e: 'update:typeFilter', value: string): void
  (e: 'update:runIdFilter', value: string): void
  (e: 'update:scopeAllBatches', value: boolean): void
  (e: 'search'): void
  (e: 'reset'): void
  (e: 'page-change', current: number): void
  (e: 'select', row: ReconDifferenceVO): void
}>()

const statusOptions = [
  { value: 'OPEN', label: '待认领' },
  { value: 'ASSIGNED', label: '已认领' },
  { value: 'RESOLVING', label: '处理中' },
  { value: 'CLOSED', label: '已核销' },
  { value: 'EXEMPT', label: '已豁免' },
  { value: 'PENDING_P2_REVERSE', label: '待冲正(P2)' },
]

const typeOptions = [
  { value: 'AMOUNT_DIFF', label: '金额差异' },
  { value: 'TIME_DIFF', label: '时间差异' },
  { value: 'ONE_SIDED_INTERNAL', label: '单边-内部' },
  { value: 'ONE_SIDED_EXTERNAL', label: '单边-外部' },
  { value: 'DUPLICATE', label: '重复入账' },
  { value: 'CHANNEL_FEE', label: '渠道手续费' },
  { value: 'AMBIGUOUS', label: '存疑' },
]

const pageCount = computed<number>(() => {
  if (!props.size) return 1
  return Math.max(1, Math.ceil((props.total || 0) / props.size))
})

function statusText(status: string): string {
  return RECON_STATUS_LABELS[status] || status || '-'
}

function typeText(type: string): string {
  return RECON_TYPE_LABELS[type] || type || '-'
}

function statusClass(status: string): string {
  const map: Record<string, string> = {
    OPEN: 'st--open',
    ASSIGNED: 'st--assigned',
    RESOLVING: 'st--resolving',
    CLOSED: 'st--closed',
    EXEMPT: 'st--exempt',
    PENDING_P2_REVERSE: 'st--pending',
  }
  return map[status] || 'st--open'
}

function onRowClick(row: ReconDifferenceVO): void {
  emit('select', row)
}
</script>

<template>
  <section class="recon-queue">
    <!-- 筛选区：常显（AC：筛选条件全部交给后端 R8，前端不过滤） -->
    <div class="recon-queue__filters">
      <el-select
        :model-value="statusFilter"
        placeholder="差异状态"
        clearable
        style="width: 150px"
        @update:model-value="(v: string) => emit('update:statusFilter', v ?? '')"
      >
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>

      <el-select
        :model-value="typeFilter"
        placeholder="差异类型"
        clearable
        style="width: 160px"
        @update:model-value="(v: string) => emit('update:typeFilter', v ?? '')"
      >
        <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>

      <el-input
        :model-value="runIdFilter"
        placeholder="批次ID(runId)"
        clearable
        style="width: 150px"
        @update:model-value="(v: string) => emit('update:runIdFilter', v ?? '')"
        @keyup.enter="emit('search')"
      />

      <el-button type="primary" @click="emit('search')">查询</el-button>
      <el-button @click="emit('reset')">重置</el-button>
      <!-- 口径开关：默认只看当前跑批（与摘要 differenceCount 同集合）；显式切到全部批次才跨 run -->
      <el-checkbox
        :model-value="scopeAllBatches"
        data-test="scope-all-batches"
        @update:model-value="(v: any) => emit('update:scopeAllBatches', Boolean(v))"
      >全部批次</el-checkbox>
      <span class="recon-queue__window" data-test="queue-scope-hint">
        {{ scopeAllBatches ? '范围：全部批次（与摘要不可直接比较）' : `范围：当前批次 #${currentRunId ?? '-'}（与摘要一致）` }}
        ｜时间窗：近 7 天（服务端过滤）
      </span>
    </div>

    <!-- 表格：原生 table，便于给每行挂 data-test -->
    <div class="recon-queue__table-wrap" v-loading="loading">
      <table class="recon-table">
        <thead>
          <tr>
            <th style="width: 88px">差异ID</th>
            <th style="width: 120px">类型</th>
            <th>内部凭证</th>
            <th>外部凭证</th>
            <th style="width: 110px" class="num">内部(元)</th>
            <th style="width: 110px" class="num">外部(元)</th>
            <th style="width: 110px" class="num">差额(元)</th>
            <th style="width: 110px">状态</th>
            <th style="width: 160px">创建时间</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in rows"
            :key="row.differenceId"
            data-test="queue-row"
            :data-difference-id="row.differenceId"
            :data-status="row.status"
            class="recon-table__row"
            :class="{ 'is-selected': selectedId === row.differenceId }"
            @click="onRowClick(row)"
          >
            <td class="mono">{{ row.differenceId }}</td>
            <td>{{ typeText(row.differenceType) }}</td>
            <td class="mono ellipsis" :title="row.internalRef || ''">{{ row.internalRef || '-' }}</td>
            <td class="mono ellipsis" :title="row.externalRef || ''">{{ row.externalRef || '-' }}</td>
            <td class="num">{{ formatFen(row.amountInternal) }}</td>
            <td class="num">{{ formatFen(row.amountExternal) }}</td>
            <td class="num" :data-diff-amount-fen="row.diffAmount ?? 0">{{ formatFen(row.diffAmount) }}</td>
            <td>
              <span class="st" :class="statusClass(String(row.status))">{{ statusText(String(row.status)) }}</span>
            </td>
            <td class="mono">{{ row.createTime || '-' }}</td>
          </tr>
          <tr v-if="!loading && rows.length === 0">
            <td colspan="9" class="recon-table__empty">近 7 天窗口内没有匹配的差异</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="recon-queue__pager">
      <el-pagination
        :current-page="current"
        :page-size="size"
        :total="total"
        layout="total,prev,pager,next"
        background
        @current-change="(p: number) => emit('page-change', p)"
      />
      <span class="recon-queue__count">共 {{ total }} 条 / {{ pageCount }} 页</span>
    </div>
  </section>
</template>

<style scoped>
.recon-queue {
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
  padding: 12px;
}

.recon-queue__filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.recon-queue__window {
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
}

.recon-queue__table-wrap {
  min-height: 160px;
  overflow: auto;
}

.recon-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.recon-table th,
.recon-table td {
  padding: 8px 10px;
  border-bottom: 1px solid var(--fts-border-secondary, #f0f2f5);
  text-align: left;
  white-space: nowrap;
}

.recon-table th {
  font-weight: 600;
  color: var(--fts-text-secondary, #606266);
  background: var(--fts-bg-subtle, #fafafa);
  position: sticky;
  top: 0;
  z-index: 1;
}

.recon-table__row {
  cursor: pointer;
  transition: background 0.15s ease;
}

.recon-table__row:hover {
  background: var(--el-fill-color-light, #f5f7fa);
}

.recon-table__row.is-selected {
  background: var(--el-color-primary-light-9, #ecf5ff);
}

.recon-table__empty {
  text-align: center;
  color: var(--fts-text-secondary, #909399);
  padding: 24px 0;
}

.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.ellipsis {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.st {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 12px;
  line-height: 18px;
}

.st--open { background: #fdf6ec; color: #e6a23c; }
.st--assigned { background: #ecf5ff; color: #409eff; }
.st--resolving { background: #f4f4f5; color: #909399; }
.st--closed { background: #f0f9eb; color: #67c23a; }
.st--exempt { background: #f3f0ff; color: #7c3aed; }
.st--pending { background: #fef0f0; color: #f56c6c; }

.recon-queue__pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 10px;
}

.recon-queue__count {
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
}
</style>
