<script setup lang="ts">
// 【保留 · 暂未接入路由】P1-SEC-009 防伪码查验页待接线（规划 /verify-code/{code} 扫码直达），
// 当前 frontend/src 内无任何引用，保留勿删。
/**
 * 防伪码查验页
 * 输入 16 位防伪码查询收货确认单基本信息与完整打印历史
 * 支持 URL 参数预填（/verify-code/{code}，二维码扫码直达）
 */
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { receiptConfirmationApi, type ReceiptVerifyResult } from '@/api/receipt-confirmation'
import { formatters } from '@/utils/formatters'

const route = useRoute()
const router = useRouter()

const codeInput = ref('')
const loading = ref(false)
const result = ref<ReceiptVerifyResult | null>(null)
const errorMsg = ref('')

function formatMoney(v: number): string {
  return `¥${(v ?? 0).toFixed(2)}`
}

async function doVerify(code: string): Promise<void> {
  const trimmed = code.trim().toUpperCase()
  if (!trimmed) {
    ElMessage.warning('请输入防伪码')
    return
  }
  if (!/^[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{16}$/.test(trimmed)) {
    ElMessage.warning('防伪码格式不正确（应为 16 位字母数字）')
    return
  }
  loading.value = true
  errorMsg.value = ''
  result.value = null
  try {
    result.value = await receiptConfirmationApi.verifyByCode(trimmed)
  } catch (err) {
    const msg = (err as { message?: string })?.message || ''
    errorMsg.value = msg.includes('登录') ? '请先登录后查验' : `查验失败：${msg || '请稍后重试'}`
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  void doVerify(codeInput.value)
}

onMounted(() => {
  const prefill = route.params.code as string | undefined
  if (prefill) {
    codeInput.value = prefill
    void doVerify(prefill)
  }
})
</script>

<template>
  <div class="verify-page">
    <div class="verify-card">
      <h2 class="page-title">收货确认单防伪查验</h2>
      <p class="page-sub">输入单据上的 16 位防伪码，查验真伪与打印记录</p>

      <div class="search-row">
        <el-input
          v-model="codeInput"
          placeholder="请输入 16 位防伪码"
          maxlength="16"
          size="large"
          style="text-transform: uppercase"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" size="large" :loading="loading" @click="handleSearch">查验</el-button>
      </div>

      <el-alert
        v-if="errorMsg"
        :title="errorMsg"
        type="error"
        show-icon
        :closable="false"
        class="result-alert"
      >
        <template v-if="errorMsg === '请先登录后查验'" #default>
          <el-button size="small" type="primary" link @click="router.push('/login')">前往登录</el-button>
        </template>
      </el-alert>

      <div v-if="result" class="result-box">
        <el-alert
          :title="result.valid ? '查验通过：该防伪码真实有效' : '查验失败'"
          :type="result.valid ? 'success' : 'error'"
          show-icon
          :closable="false"
        />
        <template v-if="result.valid">
          <el-descriptions :column="2" border class="result-desc">
            <el-descriptions-item label="确认单号">{{ result.confirmationCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="防伪码">
              <span class="code-text">{{ result.verifyCode || '-' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="供应商">{{ result.supplierName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="确认时间">{{ result.confirmTime ? formatters.datetime(result.confirmTime) : '-' }}</el-descriptions-item>
            <el-descriptions-item label="实收数量">{{ result.totalQuantity ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="确认金额">{{ formatMoney(result.totalAmount ?? 0) }}</el-descriptions-item>
            <el-descriptions-item label="打印次数">
              {{ result.printCount }} 次{{ result.lastPrintTime ? `（上次 ${formatters.datetime(result.lastPrintTime)}）` : '' }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="print-history">
            <h3>打印历史（{{ result.printLogs.length }} 条）</h3>
            <el-table :data="result.printLogs" border size="small">
              <el-table-column prop="printId" label="序号" width="80">
                <template #default="{ $index }">{{ $index + 1 }}</template>
              </el-table-column>
              <el-table-column label="打印时间" min-width="170">
                <template #default="{ row }">{{ formatters.datetime(row.printTime) }}</template>
              </el-table-column>
              <el-table-column prop="operatorName" label="操作人" min-width="120">
                <template #default="{ row }">{{ row.operatorName || '-' }}</template>
              </el-table-column>
              <el-table-column prop="sourceIp" label="来源 IP" min-width="140">
                <template #default="{ row }">{{ row.sourceIp || '-' }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!result.printLogs.length" description="暂无打印记录" :image-size="60" />
          </div>
        </template>
        <el-alert v-else :title="result.message || '防伪码不存在'" type="error" show-icon :closable="false" class="result-alert" />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.verify-page {
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 60px 16px;
  background: #f5f7fa;
}

.verify-card {
  width: 100%;
  max-width: 760px;
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a5fb4;
  margin-bottom: 4px;
}

.page-sub {
  color: #6b7a88;
  font-size: 13px;
  margin-bottom: 20px;
}

.search-row {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.result-alert {
  margin-bottom: 16px;
}

.result-desc {
  margin-top: 16px;
}

.code-text {
  font-family: Consolas, Menlo, monospace;
  font-weight: 700;
  letter-spacing: 1px;
  color: #1a5fb4;
}

.print-history {
  margin-top: 20px;

  h3 {
    font-size: 15px;
    margin-bottom: 10px;
    color: #1f3a5f;
  }
}
</style>
