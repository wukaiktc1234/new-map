<script setup lang="ts">
/**
 * 追溯链展示页面
 *
 * 功能：
 * - 输入追溯码查询完整追溯链（traceCodeApi.queryByTraceCode）
 * - 正向/反向追溯切换（正向：原料→成品；反向：成品→原料）
 * - el-timeline 可视化展示追溯链节点
 * - 节点点击查看详情弹窗
 * - 统计卡片基于 chainNodes 计算（节点数/操作人数/时间跨度/风险等级）
 *
 * 对接策略：真实后端优先 + Mock 降级
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, RefreshRight, Location, User, Box } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import EmptyState from '@/components/core/EmptyState.vue'
import { traceCodeApi } from '@/api/traceability'
import type { TraceCodeVO, TraceChainNodeVO } from '@/types/traceability'
import { ChainNodeTypeMap, TraceCodeStatusMap } from '@/types/traceability'
import type { StatColorType } from '@/types/stat'

const route = useRoute()

/** 追溯方向 */
type TraceDirection = 'forward' | 'reverse'

/* ===== 搜索状态 ===== */
const traceCodeInput = ref('')
const loading = ref(false)

/** 当前查询到的追溯码数据 */
const traceData = ref<TraceCodeVO | null>(null)

/** 追溯方向（正向：原料→成品；反向：成品→原料） */
const direction = ref<TraceDirection>('forward')

/** 是否已查询过（用于区分初始状态与查询后无数据） */
const hasQueried = ref(false)

/* ===== 节点详情弹窗 ===== */
const detailDialogVisible = ref(false)
const currentNode = ref<TraceChainNodeVO | null>(null)

/* ===== 排序后的节点列表（根据追溯方向） ===== */
const sortedNodes = computed<TraceChainNodeVO[]>(() => {
  const nodes = traceData.value?.chainNodes || []
  const arr = [...nodes]
  // 按操作时间升序排序
  arr.sort((a, b) => {
    const ta = a.operateTime ? new Date(a.operateTime).getTime() : 0
    const tb = b.operateTime ? new Date(b.operateTime).getTime() : 0
    return ta - tb
  })
  // 反向追溯：倒序展示（成品→原料）
  if (direction.value === 'reverse') {
    arr.reverse()
  }
  return arr
})

/* ===== 统计卡片（基于 chainNodes 计算） ===== */
const statsCards = computed(() => {
  const nodes = traceData.value?.chainNodes || []
  const nodeCount = nodes.length
  // 操作人数（去重，按 operatorName 去重，空值不计）
  const operatorSet = new Set<string>()
  nodes.forEach((n) => {
    if (n.operatorName) operatorSet.add(n.operatorName)
  })
  const operatorCount = operatorSet.size
  // 时间跨度（最早到最晚，单位：小时）
  let timeSpanHours = 0
  if (nodes.length >= 2) {
    const times = nodes
      .map((n) => (n.operateTime ? new Date(n.operateTime).getTime() : 0))
      .filter((t) => t > 0)
      .sort((a, b) => a - b)
    if (times.length >= 2) {
      timeSpanHours = Math.round((times[times.length - 1] - times[0]) / 3600000)
    }
  }
  // 风险等级
  const riskLevel = traceData.value?.riskLevel ?? 1
  const riskLabel = riskLevel === 1 ? '低风险' : riskLevel === 2 ? '中风险' : '高风险'
  return [
    {
      key: 'nodeCount',
      icon: 'Connection',
      label: '节点数量',
      value: nodeCount,
      colorType: 'primary' as StatColorType,
    },
    {
      key: 'operatorCount',
      icon: 'User',
      label: '操作人数',
      value: operatorCount,
      colorType: 'success' as StatColorType,
    },
    {
      key: 'timeSpan',
      icon: 'Clock',
      label: '时间跨度',
      value: timeSpanHours > 0 ? `${timeSpanHours}h` : '-',
      colorType: 'info' as StatColorType,
    },
    {
      key: 'riskLevel',
      icon: 'WarningFilled',
      label: '风险等级',
      value: riskLabel,
      colorType: (riskLevel === 1 ? 'success' : riskLevel === 2 ? 'warning' : 'error') as StatColorType,
    },
  ]
})

/* ===== 查询追溯链 ===== */
async function handleQuery(): Promise<void> {
  const code = traceCodeInput.value.trim()
  if (!code) {
    ElMessage.warning('请输入追溯码')
    return
  }
  loading.value = true
  hasQueried.value = true
  try {
    const result = await traceCodeApi.queryByTraceCode(code)
    traceData.value = result
    if (!result) {
      ElMessage.warning('未查询到该追溯码的追溯链信息')
    } else if (!result.chainNodes || result.chainNodes.length === 0) {
      ElMessage.info('该追溯码暂无追溯链节点数据')
    }
  } catch (error) {
    ElMessage.error('查询追溯链失败')
    traceData.value = null
  } finally {
    loading.value = false
  }
}

/* ===== 切换追溯方向 ===== */
function handleDirectionChange(val: TraceDirection): void {
  direction.value = val
}

/* ===== 节点点击查看详情 ===== */
function handleNodeClick(node: TraceChainNodeVO): void {
  currentNode.value = node
  detailDialogVisible.value = true
}

/* ===== 工具方法 ===== */

/** 获取节点类型 StatusTag status */
function getNodeTypeStatus(nodeType: number): string {
  return ChainNodeTypeMap[nodeType]?.type || 'default'
}

/** 获取节点类型标签文本 */
function getNodeTypeLabel(nodeType: number): string {
  return ChainNodeTypeMap[nodeType]?.label || `类型${nodeType}`
}

/** 获取节点图标（根据节点类型） */
function getNodeIcon(nodeType: number) {
  // 1原料/2入库/3领料/4投料/5成品/6出餐/7销售
  const iconMap: Record<number, typeof Box> = {
    1: Box,
    2: Box,
    3: RefreshRight,
    4: RefreshLeft,
    5: Box,
    6: RefreshRight,
    7: RefreshRight,
  }
  return iconMap[nodeType] || Box
}

/** 获取追溯码状态 StatusTag status */
function getTraceStatusTagStatus(status: string): string {
  const map: Record<string, string> = {
    active: 'success',
    recalled: 'danger',
    expired: 'info',
  }
  return map[status] || 'default'
}

/** 获取追溯码状态标签文本 */
function getTraceStatusLabel(status: string): string {
  const s = status as keyof typeof TraceCodeStatusMap
  return TraceCodeStatusMap[s]?.label || status
}

/** 格式化日期时间 */
function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  const normalized = dateStr.replace('T', ' ')
  return normalized.split('.')[0].slice(0, 19)
}

/** 解析详情 JSON */
function parseDetailJson(detailJson?: string): Record<string, unknown> | null {
  if (!detailJson) return null
  try {
    return JSON.parse(detailJson)
  } catch {
    return null
  }
}

/* ===== 初始化（从路由参数读取追溯码，无参数则显示初始空状态） ===== */
onMounted(() => {
  const codeFromRoute = route.query.code as string | undefined
  if (codeFromRoute) {
    traceCodeInput.value = codeFromRoute
    handleQuery()
  }
  // 无路由参数时不自动查询，显示初始空状态提示
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="追溯链展示" description="食品全链路追溯节点可视化" />

    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="toolbar-left">
        <el-input
          v-model="traceCodeInput"
          placeholder="请输入追溯码"
          clearable
          style="width: 320px"
          @keyup.enter="handleQuery"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" :loading="loading" @click="handleQuery">查询追溯链</el-button>
      </div>
      <div class="toolbar-right">
        <el-radio-group v-model="direction" @change="handleDirectionChange">
          <el-radio-button value="forward">正向追溯（原料→成品）</el-radio-button>
          <el-radio-button value="reverse">反向追溯（成品→原料）</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 查询结果区域 -->
    <template v-if="traceData">
      <!-- 追溯码基本信息卡片 -->
      <div class="trace-info-card">
        <div class="trace-info-header">
          <div class="trace-info-title">
            <span class="trace-code-label">追溯码：</span>
            <span class="trace-code-value">{{ traceData.traceCode }}</span>
            <StatusTag
              :status="getTraceStatusTagStatus(traceData.status)"
              :label="getTraceStatusLabel(traceData.status)"
              size="small"
            />
          </div>
          <div class="trace-info-meta">
            <span class="meta-item">产品：{{ traceData.productName }}</span>
            <span v-if="traceData.batchNumber" class="meta-item">批次：{{ traceData.batchNumber }}</span>
            <span v-if="traceData.supplierName" class="meta-item">供应商：{{ traceData.supplierName }}</span>
            <span v-if="traceData.scanCount !== undefined" class="meta-item">扫码次数：{{ traceData.scanCount }}</span>
          </div>
        </div>
      </div>

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

      <!-- 追溯链时间线 -->
      <div class="chain-section">
        <div class="chain-section-header">
          <span class="chain-section-title">追溯链节点</span>
          <span class="chain-section-count">共 {{ sortedNodes.length }} 个节点</span>
        </div>

        <div v-loading="loading" class="chain-timeline-wrapper">
          <el-timeline v-if="sortedNodes.length > 0">
            <el-timeline-item
              v-for="(node, index) in sortedNodes"
              :key="index"
              :timestamp="formatDateTime(node.operateTime)"
              placement="top"
              :type="getNodeTypeStatus(node.nodeType) === 'success' ? 'success' : getNodeTypeStatus(node.nodeType) === 'warning' ? 'warning' : getNodeTypeStatus(node.nodeType) === 'error' ? 'danger' : 'primary'"
            >
              <div class="chain-node-card" @click="handleNodeClick(node)">
                <div class="chain-node-header">
                  <div class="chain-node-title">
                    <el-icon class="chain-node-icon"><component :is="getNodeIcon(node.nodeType)" /></el-icon>
                    <span class="chain-node-name">{{ node.nodeName }}</span>
                  </div>
                  <StatusTag
                    :status="getNodeTypeStatus(node.nodeType)"
                    :label="getNodeTypeLabel(node.nodeType)"
                    size="small"
                  />
                </div>
                <div class="chain-node-body">
                  <div v-if="node.location" class="chain-node-info">
                    <el-icon><Location /></el-icon>
                    <span>{{ node.location }}</span>
                  </div>
                  <div v-if="node.operatorName" class="chain-node-info">
                    <el-icon><User /></el-icon>
                    <span>{{ node.operatorName }}</span>
                  </div>
                  <div v-if="node.quantity !== undefined && node.quantity !== null" class="chain-node-info">
                    <el-icon><Box /></el-icon>
                    <span>数量：{{ node.quantity }}</span>
                  </div>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>

          <!-- 空状态：追溯码存在但无节点数据 -->
          <EmptyState
            v-else-if="!loading"
            type="no-data"
            title="暂无追溯链节点"
            description="该追溯码尚未录入追溯链节点，请联系相关环节操作人员补录节点信息"
          />
        </div>
      </div>
    </template>

    <!-- 空状态：未查询到追溯码 -->
    <div v-else-if="hasQueried && !loading" class="empty-wrapper">
      <EmptyState
        type="no-data"
        title="未查询到追溯链"
        description="请确认追溯码是否正确，或联系管理员核查追溯码状态"
      />
    </div>

    <!-- 初始状态提示 -->
    <div v-else-if="!loading" class="empty-wrapper">
      <EmptyState
        type="no-data"
        title="请输入追溯码查询"
        description="输入追溯码后点击查询按钮，查看完整追溯链"
      />
    </div>

    <!-- 节点详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="追溯节点详情" width="520px">
      <template v-if="currentNode">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="节点名称">{{ currentNode.nodeName }}</el-descriptions-item>
          <el-descriptions-item label="节点类型">
            <StatusTag
              :status="getNodeTypeStatus(currentNode.nodeType)"
              :label="getNodeTypeLabel(currentNode.nodeType)"
              size="small"
            />
          </el-descriptions-item>
          <el-descriptions-item label="位置">{{ currentNode.location || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ currentNode.operatorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ formatDateTime(currentNode.operateTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentNode.quantity !== undefined" label="数量">
            {{ currentNode.quantity }}
          </el-descriptions-item>
          <el-descriptions-item v-if="parseDetailJson(currentNode.detailJson)" label="详情信息">
            <pre class="detail-json">{{ JSON.stringify(parseDetailJson(currentNode.detailJson), null, 2) }}</pre>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.search-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-4);
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  margin-bottom: var(--fts-space-4);

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

.trace-info-card {
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  margin-bottom: var(--fts-space-4);

  .trace-info-header {
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-2);
  }

  .trace-info-title {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
    flex-wrap: wrap;
  }

  .trace-code-label {
    font-size: 14px;
    color: var(--fts-text-secondary);
  }

  .trace-code-value {
    font-size: 16px;
    font-weight: 600;
    color: var(--fts-text-primary);
    font-family: 'Courier New', monospace;
  }

  .trace-info-meta {
    display: flex;
    align-items: center;
    gap: var(--fts-space-6);
    flex-wrap: wrap;
    font-size: 13px;
    color: var(--fts-text-secondary);

    .meta-item {
      display: inline-flex;
      align-items: center;
    }
  }
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.chain-section {
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);

  .chain-section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: var(--fts-space-3);
    margin-bottom: var(--fts-space-4);
    border-bottom: 1px solid var(--fts-border-primary);
  }

  .chain-section-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--fts-text-primary);
  }

  .chain-section-count {
    font-size: 13px;
    color: var(--fts-text-secondary);
  }

  .chain-timeline-wrapper {
    min-height: 200px;
    padding: var(--fts-space-2) 0;
  }
}

.chain-node-card {
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-base);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--fts-primary);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    transform: translateY(-1px);
  }

  .chain-node-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--fts-space-2);
  }

  .chain-node-title {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
  }

  .chain-node-icon {
    font-size: 16px;
    color: var(--fts-primary);
  }

  .chain-node-name {
    font-size: 14px;
    font-weight: 600;
    color: var(--fts-text-primary);
  }

  .chain-node-body {
    display: flex;
    align-items: center;
    gap: var(--fts-space-4);
    flex-wrap: wrap;
    font-size: 13px;
    color: var(--fts-text-secondary);
  }

  .chain-node-info {
    display: flex;
    align-items: center;
    gap: var(--fts-space-1);

    .el-icon {
      font-size: 13px;
    }
  }
}

.empty-wrapper {
  padding: var(--fts-space-8) 0;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
}

.detail-json {
  margin: 0;
  padding: var(--fts-space-2);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-base);
  font-size: 12px;
  color: var(--fts-text-primary);
  max-height: 200px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
