<script setup lang="ts">
/**
 * 电子合同 - 详情对话框（含5个Tab）
 *
 * Tab结构：
 * 1. 概览（基本信息）
 * 2. 起草内容（合同正文）
 * 3. 审批签署（审批流程 + 签署方）
 * 4. 签署存证（安全存证 + 签署记录）- 懒加载
 * 5. 归档（归档信息 + 操作留痕）
 *
 * 父组件传入详情数据和关联数据（signLogs/evidence），子组件负责展示
 * 操作按钮通过 emit('action', actionType, row) 委托给父组件处理
 */
import { ref, watch, computed } from 'vue'
import StatusTag from '@/components/core/StatusTag.vue'
import ContentCard from '@/components/core/ContentCard.vue'
import { electronicContractConverter } from '@/api/purchase/converters'
import type { ElectronicSignLog, ElectronicEvidence } from '@/api/purchase/electronic-contract'
import type { ElectronicContractInfo, ElectronicContractStatus } from '@/types/purchase-electronic-contract'

/** 详情动作类型 */
export type DetailAction =
  | 'initiateSigning'
  | 'confirmSigned'
  | 'generateLink'
  | 'archive'
  | 'terminate'

interface Props {
  /** 对话框可见性（v-model） */
  visible: boolean
  /** 详情数据 */
  detailData: ElectronicContractInfo | null
  /** 签署记录 */
  signLogs: ElectronicSignLog[]
  /** 签署记录加载中 */
  signLogsLoading: boolean
  /** 安全存证 */
  evidence: ElectronicEvidence | null
  /** 安全存证加载中 */
  evidenceLoading: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  /** Tab 切换（用于懒加载关联数据） */
  'tab-change': [tab: string]
  /** 操作动作（动作类型，目标合同） */
  'action': [action: DetailAction, target: ElectronicContractInfo]
}>()

/** 当前活动Tab */
const detailTab = ref('overview')

/** 签署流程步骤定义 */
const signSteps = [
  { title: '起草', description: '创建电子合同' },
  { title: '内部审批', description: '内部审批通过' },
  { title: '供应商确认', description: '供应商确认合同' },
  { title: '签署', description: '双方签署完成' },
  { title: '归档', description: '合同归档存储' },
]

/** 步骤索引 → Tab名称映射 */
const stepTabMap: Record<number, string> = {
  0: 'draft',
  1: 'approval',
  2: 'approval',
  3: 'signing',
  4: 'archive',
}

/** 监听 visible 变化，打开时重置Tab */
watch(() => props.visible, (open) => {
  if (open) {
    detailTab.value = 'overview'
  }
})

/** 监听Tab变化，通知父组件加载关联数据 */
watch(detailTab, (tab) => {
  emit('tab-change', tab)
})

/** 点击步骤跳转对应Tab */
function handleStepClick(stepIndex: number) {
  const tab = stepTabMap[stepIndex]
  if (tab) detailTab.value = tab
}

/** 根据状态计算当前步骤 */
function getActiveStep(status: ElectronicContractStatus): number {
  const stepMap: Record<ElectronicContractStatus, number> = {
    draft: 0,
    pending_sign: 2,
    signed: 4,
    expired: 4,
    cancelled: 0,
  }
  return stepMap[status] ?? 0
}

/** 签署记录动作映射为中文标签 */
function getActionLabel(action: ElectronicSignLog['action']): string {
  const map: Record<ElectronicSignLog['action'], string> = {
    initiate: '发起签署',
    company_sign: '公司签署',
    supplier_sign: '供应商签署',
    complete: '签署完成',
    reject: '签署驳回',
  }
  return map[action] || action
}

/** 签署记录类型映射为时间线颜色 */
function getLogType(action: ElectronicSignLog['action']): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<ElectronicSignLog['action'], 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    initiate: 'primary',
    company_sign: 'success',
    supplier_sign: 'success',
    complete: 'info',
    reject: 'danger',
  }
  return map[action] || 'info'
}

/** 格式化合同正文（将换行符转为HTML换行） */
function formatContractContent(content: string): string {
  if (!content) return ''
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
}

/** 关闭对话框 */
function closeDialog() {
  emit('update:visible', false)
}

/** 触发动作 */
function emitAction(action: DetailAction) {
  if (props.detailData) {
    emit('action', action, props.detailData)
  }
}

/** 是否显示归档Tab内容（仅 signed/terminated/expired 状态） */
const showArchiveTab = computed(() => {
  const status = props.detailData?.status
  return status === 'signed' || status === 'expired' || status === 'cancelled'
})
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="电子合同详情"
    width="1200px"
    class="fts-dialog--xl"
    destroy-on-close
    lock-scroll="false"
    @update:model-value="emit('update:visible', $event)"
  >
    <template v-if="detailData">
      <!-- 签署流程进度条（可点击跳转Tab） -->
      <div class="sign-steps-wrapper">
        <el-steps :active="getActiveStep(detailData.status)" finish-status="success" align-center class="lifecycle-steps">
          <el-step v-for="(step, idx) in signSteps" :key="step.title" :title="step.title" :description="step.description" @click="handleStepClick(idx)" />
        </el-steps>
        <!-- 步骤操作按钮区 -->
        <div class="step-actions">
          <el-button v-if="detailData.status === 'draft'" link type="primary" size="small" @click="emitAction('initiateSigning')">发起签署</el-button>
          <el-button v-if="detailData.status === 'pending_sign'" link type="primary" size="small" @click="emitAction('confirmSigned')">确认签署</el-button>
          <el-button v-if="detailData.status === 'pending_sign'" link type="success" size="small" @click="emitAction('generateLink')">发送供应商</el-button>
          <el-button v-if="detailData.status === 'signed' && !detailData.archived" link type="primary" size="small" @click="emitAction('archive')">归档合同</el-button>
        </div>
      </div>
      <el-divider />

      <el-tabs v-model="detailTab">
        <!-- Tab 1: 概览（基本信息） -->
        <el-tab-pane label="概览" name="overview">
          <ContentCard title="基本信息">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="电子合同编号">{{ detailData.eContractNo }}</el-descriptions-item>
              <el-descriptions-item label="关联采购合同">{{ detailData.contractNo || '无' }}</el-descriptions-item>
              <el-descriptions-item label="合同名称" :span="2">{{ detailData.contractName }}</el-descriptions-item>
              <el-descriptions-item label="供应商">{{ detailData.supplierName }}</el-descriptions-item>
              <el-descriptions-item label="合同金额">{{ electronicContractConverter.formatYuan(detailData.totalAmount) }} 元</el-descriptions-item>
              <el-descriptions-item label="签署日期">{{ detailData.signDate || '—' }}</el-descriptions-item>
              <el-descriptions-item label="到期日期">{{ detailData.expireDate }}</el-descriptions-item>
              <el-descriptions-item label="合同状态">
                <StatusTag :status="electronicContractConverter.toStatusTagStatus(detailData.status)" :label="electronicContractConverter.toStatusLabel(detailData.status)" size="small" />
              </el-descriptions-item>
              <el-descriptions-item label="备注">{{ detailData.remark || '—' }}</el-descriptions-item>
            </el-descriptions>
          </ContentCard>
        </el-tab-pane>

        <!-- Tab 2: 起草内容（合同正文） -->
        <el-tab-pane label="起草内容" name="draft">
          <ContentCard title="合同正文">
            <div class="contract-content-preview">
              <div v-if="detailData.contractContent" class="contract-text" v-html="formatContractContent(detailData.contractContent)"></div>
              <el-empty v-else description="暂无合同正文内容" />
            </div>
          </ContentCard>
        </el-tab-pane>

        <!-- Tab 3: 审批签署（审批流程 + 签署方） -->
        <el-tab-pane label="审批签署" name="approval">
          <ContentCard title="审批流程">
            <el-timeline>
              <el-timeline-item type="primary" :hollow="detailData.status === 'draft'">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span style="font-weight: 500;">提交审批</span>
                  <StatusTag :status="detailData.status === 'draft' ? 'pending' : 'success'" :label="detailData.status === 'draft' ? '待提交' : '已提交'" size="small" />
                </div>
              </el-timeline-item>
              <el-timeline-item :type="detailData.status === 'draft' ? 'info' : 'success'" :hollow="detailData.status === 'draft'">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span style="font-weight: 500;">审批通过</span>
                  <StatusTag :status="detailData.status === 'draft' ? 'pending' : 'success'" :label="detailData.status === 'draft' ? '待审批' : '已通过'" size="small" />
                </div>
              </el-timeline-item>
            </el-timeline>
          </ContentCard>
          <ContentCard title="签署方" style="margin-top: var(--fts-space-4);">
            <el-descriptions :column="1" border title="甲方（我方）">
              <el-descriptions-item label="企业名称">XX餐饮管理有限公司</el-descriptions-item>
              <el-descriptions-item label="法定代表人">张某某</el-descriptions-item>
              <el-descriptions-item label="认证状态">
                <StatusTag status="active" label="已认证" size="small" />
              </el-descriptions-item>
              <el-descriptions-item label="签署方式">企业电子签章</el-descriptions-item>
            </el-descriptions>
            <el-descriptions :column="1" border title="乙方（供应商）" style="margin-top: 16px;">
              <el-descriptions-item label="企业名称">{{ detailData.supplierName }}</el-descriptions-item>
              <el-descriptions-item label="法定代表人">待确认</el-descriptions-item>
              <el-descriptions-item label="认证状态">
                <StatusTag :status="detailData.status === 'signed' ? 'active' : 'warning'" :label="detailData.status === 'signed' ? '已认证' : '待认证'" size="small" />
              </el-descriptions-item>
              <el-descriptions-item label="签署方式">企业电子签章</el-descriptions-item>
            </el-descriptions>
          </ContentCard>
        </el-tab-pane>

        <!-- Tab 4: 签署存证（安全存证 + 签署记录） -->
        <el-tab-pane label="签署存证" name="signing">
          <ContentCard title="安全存证">
            <!-- 骨架屏加载态 -->
            <div v-if="evidenceLoading" class="detail-skeleton">
              <div class="skeleton-cell" style="width:80%;height:28px;" />
              <div class="skeleton-cell" style="width:60%;height:28px;" />
              <div class="skeleton-cell" style="width:90%;height:28px;" />
              <div class="skeleton-cell" style="width:70%;height:28px;" />
              <div class="skeleton-cell" style="width:50%;height:28px;" />
            </div>
            <!-- 数据内容 -->
            <div v-else class="detail-content-fadein">
              <el-descriptions :column="1" border>
                <el-descriptions-item label="数字证书">
                  <span v-if="evidence?.hasEvidence" class="security-info">CA数字证书已颁发（{{ evidence.caCertificate }}）</span>
                  <span v-else class="security-info pending">待签署后颁发</span>
                </el-descriptions-item>
                <el-descriptions-item label="时间戳">
                  <span v-if="evidence?.hasEvidence">{{ evidence.timestamp }}（国家授时中心）</span>
                  <span v-else class="security-info pending">待签署后生成</span>
                </el-descriptions-item>
                <el-descriptions-item label="存证哈希">
                  <span v-if="evidence?.hasEvidence" class="security-info hash">{{ evidence.evidenceHash }}</span>
                  <span v-else class="security-info pending">待签署后生成</span>
                </el-descriptions-item>
                <el-descriptions-item label="区块链存证">
                  <span v-if="evidence?.hasEvidence">
                    <StatusTag status="active" label="已上链" size="small" />
                    <span class="security-info hash" style="margin-left: 8px;">{{ evidence.blockchainAddress }}</span>
                  </span>
                  <span v-else><StatusTag status="inactive" label="未上链" size="small" /></span>
                </el-descriptions-item>
                <el-descriptions-item label="证据完整性">
                  <span v-if="evidence?.hasEvidence"><StatusTag status="active" label="完整" size="small" /></span>
                  <span v-else><StatusTag status="inactive" label="待签署" size="small" /></span>
                </el-descriptions-item>
              </el-descriptions>
              <el-alert v-if="evidence?.hasEvidence" type="success" :closable="false" style="margin-top: 16px;">
                <template #title>
                  合同已通过CA认证+时间戳+区块链三重存证，具备完整法律效力
                </template>
              </el-alert>
              <el-alert v-else type="info" :closable="false" style="margin-top: 16px;">
                <template #title>
                  合同签署后将自动生成数字证书、时间戳和区块链存证，确保合同不可篡改
                </template>
              </el-alert>
            </div>
          </ContentCard>
          <ContentCard title="签署记录" style="margin-top: var(--fts-space-4);">
            <!-- 骨架屏加载态 -->
            <div v-if="signLogsLoading" class="detail-skeleton">
              <div class="skeleton-timeline-item" />
              <div class="skeleton-timeline-item" />
              <div class="skeleton-timeline-item" />
            </div>
            <!-- 数据内容 -->
            <div v-else class="detail-content-fadein">
              <el-timeline v-if="signLogs.length">
                <el-timeline-item
                  v-for="(log, idx) in signLogs"
                  :key="log.logId"
                  :timestamp="log.actionTime"
                  :type="getLogType(log.action)"
                  placement="top"
                  class="sign-log-fadein"
                  :style="{ animationDelay: `${idx * 0.08}s` }"
                >
                  <div class="sign-log-item">
                    <span class="sign-log-action">{{ getActionLabel(log.action) }}</span>
                    <span class="sign-log-operator">{{ log.operator }}（{{ log.operatorRole }}）</span>
                  </div>
                  <div class="sign-log-desc">{{ log.description }}</div>
                  <div class="sign-log-ip">IP：{{ log.ipAddress }}</div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无签署记录" />
            </div>
          </ContentCard>
        </el-tab-pane>

        <!-- Tab 5: 归档 -->
        <el-tab-pane label="归档" name="archive">
          <ContentCard title="归档信息">
            <template v-if="showArchiveTab">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="归档状态">
                  <StatusTag :status="detailData?.archived ? 'active' : 'warning'" :label="detailData?.archived ? '已归档' : '待归档'" size="small" />
                </el-descriptions-item>
                <el-descriptions-item label="归档时间">{{ detailData?.archiveTime || '—' }}</el-descriptions-item>
                <el-descriptions-item label="归档人">{{ detailData?.archivedBy || '—' }}</el-descriptions-item>
                <el-descriptions-item label="归档位置">{{ detailData?.archiveLocation || '电子档案库' }}</el-descriptions-item>
              </el-descriptions>
              <el-divider content-position="left">操作留痕</el-divider>
              <el-timeline>
                <el-timeline-item :timestamp="detailData?.createTime || ''" type="primary">合同创建</el-timeline-item>
                <el-timeline-item v-if="detailData?.signDate" :timestamp="detailData.signDate + ' 15:00:00'" type="success">合同签署完成</el-timeline-item>
                <el-timeline-item v-if="detailData?.archived" :timestamp="detailData.archiveTime || ''" type="info">合同归档</el-timeline-item>
              </el-timeline>
              <div v-if="!detailData?.archived" style="margin-top: 16px;">
                <el-button type="primary" size="small" @click="emitAction('archive')">归档合同</el-button>
              </div>
            </template>
            <el-empty v-else description="合同签署后方可归档" />
          </ContentCard>
        </el-tab-pane>
      </el-tabs>
    </template>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeDialog">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
/* ===== 签署流程进度条样式 ===== */
.sign-steps-wrapper {
  padding: var(--fts-space-5) var(--fts-space-6);
  background: var(--fts-bg-card);
  border-radius: var(--fts-page-radius);
  border: 1px solid var(--fts-border-primary);
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: var(--fts-space-2);
  margin-top: var(--fts-space-4);
  padding-top: var(--fts-space-3);
  border-top: 1px solid var(--fts-border-secondary);
}

/* el-steps 深度样式定制 */
.lifecycle-steps {
  :deep(.el-step__head.is-finish) {
    .el-step__icon {
      background: var(--fts-primary);
      border-color: var(--fts-primary);
      box-shadow: 0 0 0 4px rgba(var(--fts-primary-rgb), 0.18);
      transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    }
  }

  :deep(.el-step__head.is-process) {
    .el-step__icon {
      background: var(--fts-primary);
      border-color: var(--fts-primary);
      color: var(--fts-text-inverse);
      animation: step-breathe 2s ease-in-out infinite;
      transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    }
  }

  :deep(.el-step__title.is-finish) {
    color: var(--fts-primary);
    font-weight: 600;
    transition: color 0.3s;
  }

  :deep(.el-step__title.is-process) {
    color: var(--fts-primary);
    font-weight: 700;
    transition: color 0.3s;
  }

  :deep(.el-step__head.is-finish .el-step__line) {
    background: linear-gradient(90deg, var(--fts-primary), rgba(var(--fts-primary-rgb), 0.4));
    transition: background 0.5s;
  }

  :deep(.el-step__description) {
    transition: color 0.3s;
  }

  :deep(.el-step__description.is-finish) {
    color: var(--fts-text-secondary);
  }

  :deep(.el-step__description.is-process) {
    color: var(--fts-text-regular);
  }

  :deep(.el-step) {
    cursor: pointer;
  }

  :deep(.el-step__head.is-wait) {
    .el-step__icon {
      transition: all 0.2s;
    }

    &:hover .el-step__icon {
      border-color: var(--fts-primary);
      color: var(--fts-primary);
    }
  }
}

@keyframes step-breathe {
  0%, 100% { box-shadow: 0 0 0 4px rgba(var(--fts-primary-rgb), 0.18); }
  50% { box-shadow: 0 0 0 8px rgba(var(--fts-primary-rgb), 0.30); }
}

/* ===== 骨架屏加载动画 ===== */
.detail-skeleton {
  padding: var(--fts-space-2) 0;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.skeleton-cell {
  border-radius: 4px;
  background: linear-gradient(90deg, var(--fts-fill-light) 25%, var(--fts-fill-lighter) 50%, var(--fts-fill-light) 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s ease-in-out infinite;
}

.skeleton-timeline-item {
  height: 48px;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--fts-fill-light) 25%, var(--fts-fill-lighter) 50%, var(--fts-fill-light) 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s ease-in-out infinite;
  margin-bottom: var(--fts-space-3);

  &:last-child { margin-bottom: 0; }
}

@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ===== 内容渐显动画 ===== */
.detail-content-fadein {
  animation: content-fadein 0.4s ease-out;
}

@keyframes content-fadein {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.sign-log-fadein {
  opacity: 0;
  animation: sign-log-enter 0.35s ease-out forwards;
}

@keyframes sign-log-enter {
  from { opacity: 0; transform: translateX(-12px); }
  to { opacity: 1; transform: translateX(0); }
}

.contract-content-preview {
  max-height: 400px;
  overflow-y: auto;
  padding: var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
}

.contract-text {
  line-height: 1.8;
  color: var(--fts-text-primary);
  font-size: var(--fts-font-size-base);
}

.security-info {
  font-family: var(--fts-font-family-mono, 'Courier New', monospace);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);

  &.pending {
    color: var(--fts-text-tertiary);
    font-family: inherit;
  }

  &.hash {
    word-break: break-all;
  }
}

.sign-log-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);

  .sign-log-action {
    font-weight: 500;
  }

  .sign-log-operator {
    color: var(--fts-text-tertiary);
    font-size: var(--fts-font-size-sm);
  }
}

.sign-log-desc {
  margin-top: var(--fts-space-1);
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
  line-height: 1.6;
}

.sign-log-ip {
  margin-top: var(--fts-space-1);
  color: var(--fts-text-tertiary);
  font-size: var(--fts-font-size-xs);
  font-family: var(--fts-font-family-mono, 'Courier New', monospace);
}
</style>
