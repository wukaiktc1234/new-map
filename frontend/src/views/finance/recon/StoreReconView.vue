<script setup lang="ts">
/**
 * 门店当日对账视图（P3-2）
 *
 * 数据源：GET /v1/finance/store-recon/daily?storeId&date（金额单位：分）
 * 展示：预期 / 实交 / 差额三值卡片（差额为负标红并显示「短款」）+ 缴款单状态 + 备注。
 * 权限：店长只能看本店；接口返回 403 时显示「无权访问其他门店数据」（而不是空白页）。
 */
import { computed, onMounted, ref } from 'vue'
import { Lock } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import { getStoreReconDaily, ReconApiError, type StoreReconDailyVO } from '@/api/finance-store-recon'
import { usePermissionStore } from '@/stores/permission'
import { formatFen } from '@/types/finance-recon'

const permissionStore = usePermissionStore()

/** 当前登录账号归属门店（店长账号会带 storeId） */
const ownStoreId = computed<string>(() => {
  const raw = permissionStore.userInfo?.storeId
  return raw === undefined || raw === null ? '' : String(raw)
})

/** 查询条件 */
function todayString(): string {
  const now = new Date()
  const pad = (n: number) => (n < 10 ? '0' + n : String(n))
  return now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate())
}

const date = ref<string>(todayString())
const storeIdInput = ref<string>(ownStoreId.value)
const loading = ref(false)
const data = ref<StoreReconDailyVO | null>(null)
const errorText = ref('')
const forbidden = ref(false)
const loaded = ref(false)

/** 三值（分）；null 视为未知，展示「—」而不是假 0 */
const expectedFen = computed<number | null>(() => data.value?.expectedAmount ?? null)
const actualFen = computed<number | null>(() => data.value?.actualAmount ?? null)
const differenceFen = computed<number | null>(() => data.value?.differenceAmount ?? null)

const isShort = computed<boolean>(() => (differenceFen.value ?? 0) < 0)
const isOver = computed<boolean>(() => (differenceFen.value ?? 0) > 0)

function fenText(fen: number | null): string {
  return fen === null ? '—' : formatFen(fen)
}

/** 缴款单状态中文名 */
const SLIP_STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  CONFIRMED: '已确认',
  APPROVED: '已审核',
  REJECTED: '已驳回',
  VOID: '已作废',
}
const slipStatusText = computed<string>(() => {
  const status = data.value?.slipStatus
  if (!status) return '-'
  return SLIP_STATUS_LABELS[status] || status
})

async function load(): Promise<void> {
  loading.value = true
  errorText.value = ''
  forbidden.value = false
  try {
    const result = await getStoreReconDaily({
      storeId: storeIdInput.value.trim() || null,
      date: date.value,
    })
    data.value = result
    loaded.value = true
  } catch (error) {
    data.value = null
    loaded.value = false
    if (error instanceof ReconApiError && error.forbidden) {
      forbidden.value = true
    } else {
      errorText.value = error instanceof Error ? error.message : '门店对账加载失败'
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="modern-page store-recon">
    <PageHeader title="门店对账" description="门店当日预期缴款与实交金额勾稽 · 缴款单状态">
      <el-button type="primary" :loading="loading" @click="load">查询</el-button>
    </PageHeader>

    <section class="store-recon__filters">
      <el-date-picker
        v-model="date"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="业务日期"
        :clearable="false"
        style="width: 190px"
        @change="load"
      />
      <el-input
        v-model="storeIdInput"
        placeholder="门店ID（店长默认本店）"
        clearable
        style="width: 200px"
        @keyup.enter="load"
      />
      <span class="store-recon__hint">
        当前门店：
        <strong data-test="store-recon-store">{{ data?.storeId ?? (storeIdInput || ownStoreId || '-') }}</strong>
      </span>
    </section>

    <!-- 403：店长访问其他门店 -->
    <section v-if="forbidden" class="store-recon__forbidden" data-test="store-recon-forbidden">
      <el-icon :size="18"><Lock /></el-icon>
      <span>无权访问其他门店数据</span>
    </section>

    <section v-else-if="errorText" class="store-recon__error">{{ errorText }}</section>

    <section v-else class="store-recon__cards" v-loading="loading">
      <div class="store-recon__card">
        <div class="store-recon__label">预期缴款（元）</div>
        <div class="store-recon__value" data-test="store-recon-expected" :data-fen="expectedFen ?? ''">
          {{ fenText(expectedFen) }}
        </div>
        <div class="store-recon__hint">业务日期 {{ data?.businessDate || date }}</div>
      </div>

      <div class="store-recon__card">
        <div class="store-recon__label">实交金额（元）</div>
        <div class="store-recon__value" data-test="store-recon-actual" :data-fen="actualFen ?? ''">
          {{ fenText(actualFen) }}
        </div>
        <div class="store-recon__hint">来源：门店缴款单</div>
      </div>

      <div class="store-recon__card" :class="{ 'store-recon__card--short': isShort }">
        <div class="store-recon__label">差额（元）</div>
        <div
          class="store-recon__value"
          :class="{ 'is-short': isShort, 'is-over': isOver }"
          data-test="store-recon-difference"
          :data-fen="differenceFen ?? ''"
        >
          {{ fenText(differenceFen) }}
          <span v-if="isShort" class="store-recon__badge store-recon__badge--short">短款</span>
          <span v-else-if="isOver" class="store-recon__badge store-recon__badge--over">长款</span>
          <span v-else-if="differenceFen === 0" class="store-recon__badge">平账</span>
        </div>
        <div class="store-recon__hint">差额 = 实交 − 预期（负数即短款）</div>
      </div>

      <div class="store-recon__card">
        <div class="store-recon__label">缴款单</div>
        <div class="store-recon__value store-recon__value--sm">
          {{ data?.slipNo || (loaded ? '无缴款单' : '—') }}
        </div>
        <div class="store-recon__hint">
          状态：<span class="store-recon__status">{{ slipStatusText }}</span>
        </div>
      </div>
    </section>

    <section v-if="!forbidden && !errorText && data" class="store-recon__remark">
      <span class="store-recon__remark-label">备注</span>
      <span class="store-recon__remark-text">{{ data.remark || '（无）' }}</span>
    </section>
  </div>
</template>

<style scoped>
.store-recon {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
}

.store-recon__filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 12px 14px;
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
}

.store-recon__hint {
  font-size: 13px;
  color: var(--fts-text-secondary, #909399);
}

.store-recon__forbidden {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 18px 16px;
  border-radius: 12px;
  background: var(--el-color-warning-light-9, #fdf6ec);
  color: var(--el-color-warning-dark-2, #b88230);
  border: 1px solid var(--el-color-warning-light-5, #f5dab1);
  font-size: 14px;
  font-weight: 500;
}

.store-recon__error {
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger, #f56c6c);
  font-size: 13px;
}

.store-recon__cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.store-recon__card {
  padding: 14px 16px;
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
}

.store-recon__card--short {
  border-color: var(--el-color-danger-light-5, #fab6b6);
}

.store-recon__label {
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
  margin-bottom: 8px;
}

.store-recon__value {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  color: var(--fts-text-primary, #303133);
  font-variant-numeric: tabular-nums;
}

.store-recon__value--sm {
  font-size: 15px;
  font-weight: 500;
  word-break: break-all;
}

.store-recon__value.is-short {
  color: var(--el-color-danger, #f56c6c);
}

.store-recon__value.is-over {
  color: var(--el-color-success, #67c23a);
}

.store-recon__badge {
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
  background: var(--fts-bg-subtle, #f4f4f5);
  color: var(--fts-text-secondary, #909399);
}

.store-recon__badge--short {
  background: var(--el-color-danger-light-9, #fef0f0);
  color: var(--el-color-danger, #f56c6c);
}

.store-recon__badge--over {
  background: var(--el-color-success-light-9, #f0f9eb);
  color: var(--el-color-success, #67c23a);
}

.store-recon__hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
}

.store-recon__status {
  color: var(--fts-text-primary, #303133);
}

.store-recon__remark {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  background: var(--fts-bg-card, #fff);
  border: 1px solid var(--fts-border-secondary, #ebeef5);
  border-radius: 12px;
  font-size: 13px;
}

.store-recon__remark-label {
  flex: 0 0 auto;
  color: var(--fts-text-secondary, #909399);
}

.store-recon__remark-text {
  color: var(--fts-text-primary, #303133);
  word-break: break-all;
}

@media (max-width: 1200px) {
  .store-recon__cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
