<script setup lang="ts">
/**
 * 合同详情对话框（流程化设计）
 * 5个Tab按合同生命周期阶段动态显示：
 * 概览(overview) / 正文(document) / 签署(signing) / 变更(changes) / 归档(archive)
 */
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '@/components/core/StatusTag.vue'
import ContentCard from '@/components/core/ContentCard.vue'
import CompanySignDialog from './CompanySignDialog.vue'
import PaperScanUploadDialog from './PaperScanUploadDialog.vue'
import ContractDocumentEditor from './ContractDocumentEditor.vue'
import { contractApi } from '@/api/hr/contract'
import { contractIntelligenceApi } from '@/api/contract-intelligence'
import type { ExpiringContract, ContractRisk } from '@/types/contract-intelligence'
import { RiskLevelLabelMap, RiskTypeLabelMap } from '@/types/contract-intelligence'
import type {
  EmployeeContract,
  ContractType,
  ContractStatus,
  ContractArchiveStatus,
  LaborContractTerm,
  ContractClause,
  ContractArchiveFormData,
  LegalWarning,
  ContractDocument,
  ContractDocumentEditForm,
  DocumentSourceType,
  ContractCarrier,
  SignTask,
  SignTaskStatus,
  CreateSignTaskForm,
  SignatureRecord,
  PaperArchiveInfo,
  ContractChangeRecord,
  ContractChangeType,
  ContractChangeStatus,
} from '@/types/hr/contract'
import {
  ContractTypeOptions,
  ContractTypeTagMap,
  ContractStatusOptions,
  ContractStatusTagMap,
  LaborContractTermOptions,
  LegalWarningMap,
  ContractArchiveStatusOptions,
  ContractArchiveStatusTagMap,
  ClauseTypeOptions,
  DocumentSourceTypeOptions,
  DocumentSourceTypeTagMap,
  ContractCarrierOptions,
  ContractCarrierTagMap,
  SignTaskStatusTagMap,
  ContractChangeTypeOptions,
  ContractChangeTypeTagMap,
  ContractChangeStatusOptions,
  ContractChangeStatusTagMap,
} from '@/types/hr/contract'
import type { Employee } from '@/types/hr'

const props = defineProps<{
  visible: boolean
  contract: EmployeeContract | null
  employeeOptions: Employee[]
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'refresh': []
  'change-contract': [contractId: string, changeType: ContractChangeType]
  'renew': [contract: EmployeeContract]
  'terminate': [contract: EmployeeContract]
}>()

/* ===== Tab状态 ===== */
const detailTab = ref('overview')
const loadedDetailTabs = ref<Set<string>>(new Set())

/* ===== 生命周期步骤定义（4步） ===== */
const lifecycleSteps = [
  { title: '起草', description: '生成合同' },
  { title: '签署', description: '双方签署' },
  { title: '履行', description: '合同生效' },
  { title: '归档', description: '合同归档' },
]

/** 步骤索引 → Tab名称映射（点击步骤跳转到对应Tab） */
const stepTabMap: Record<number, string> = {
  0: 'document',
  1: 'signing',
  2: 'overview',
  3: 'archive',
}

/** 根据合同状态计算当前生命周期步骤 */
function getLifecycleStep(status: ContractStatus, archiveStatus?: ContractArchiveStatus): number {
  if (archiveStatus === 'archived' || archiveStatus === 'destroyed') return 3
  const map: Record<ContractStatus, number> = {
    draft: 0,
    pending_approval: 1,
    pending_sign: 1,
    company_signed: 1,
    signed: 2,
    active: 2,
    expiring: 2,
    expired: 2,
    terminated: 2,
    renewed: 2,
    archived: 3,
  }
  return map[status] ?? 0
}

/** 点击生命周期步骤跳转到对应Tab */
function handleStepClick(stepIndex: number) {
  const tab = stepTabMap[stepIndex]
  if (tab) detailTab.value = tab
}

/* ===== 动态Tab显示逻辑 ===== */
const visibleTabs = computed(() => {
  if (!props.contract) return ['overview', 'document']
  const status = props.contract.status
  const tabs = ['overview', 'document']
  // 签署Tab：非草稿时显示（审批中即开始签署流程）
  if (status !== 'draft') tabs.push('signing')
  // 变更Tab：active及之后显示
  const changeStatuses: ContractStatus[] = ['active', 'expiring', 'expired', 'terminated', 'renewed', 'archived']
  if (changeStatuses.includes(status)) tabs.push('changes')
  // 归档Tab：signed及之后显示
  const archiveStatuses: ContractStatus[] = ['signed', 'active', 'expiring', 'expired', 'terminated', 'renewed', 'archived']
  if (archiveStatuses.includes(status)) tabs.push('archive')
  return tabs
})

/* ===== 当前步骤可执行的操作按钮 ===== */
const currentStepActions = computed(() => {
  if (!props.contract) return []
  const status = props.contract.status
  const actions: Array<{ label: string; type: 'primary' | 'success' | 'warning' | 'danger'; command: string }> = []
  if (status === 'draft') {
    actions.push({ label: '编辑合同', type: 'primary', command: 'edit' })
    actions.push({ label: '提交审批', type: 'success', command: 'submitApproval' })
  } else if (status === 'pending_sign' || status === 'company_signed') {
    if ((props.contract.carrier || 'electronic') === 'electronic') {
      actions.push({ label: '公司签署', type: 'primary', command: 'companySign' })
    }
    actions.push({ label: '推送员工签署', type: 'success', command: 'pushSignTask' })
  } else if ((status === 'terminated' || status === 'expired' || status === 'renewed') && props.contract.archiveStatus !== 'archived') {
    actions.push({ label: '归档', type: 'primary', command: 'archive' })
  }
  return actions
})

/* ===== 法律风险提示 ===== */
const currentLegalWarning = computed<LegalWarning | null>(() => {
  const type = props.contract?.contractType
  if (!type) return null
  return LegalWarningMap[type] || null
})

/* ===== 签署进度（3步） ===== */
const signingStep = computed(() => {
  if (!props.contract) return 0
  const c = props.contract
  if (c.employeeSignTime) return 2 // 签署完成
  if (c.companySignTime) return 1 // 员工签署
  return 0 // 公司签章
})

const signingSteps = [
  { title: '公司签章', description: '公司方签署' },
  { title: '员工签署', description: '员工方签署' },
  { title: '签署完成', description: '双方签署完成' },
]

/* ===== 签署记录 ===== */
const signatureRecords = ref<SignatureRecord[]>([])
const signatureLoading = ref(false)

async function loadSignatureRecords() {
  if (!props.contract) return
  signatureLoading.value = true
  signatureRecords.value = []
  try {
    const records = await contractApi.getSignatures(props.contract.id)
    signatureRecords.value = records || []
  } catch {
    signatureRecords.value = []
  } finally {
    signatureLoading.value = false
  }
}

/* ===== 合同正文 ===== */
const documentHtml = ref('')
const documentLoading = ref(false)
const currentDocument = ref<ContractDocument | null>(null)

async function loadContractDocument() {
  if (!props.contract) return
  documentLoading.value = true
  documentHtml.value = ''
  currentDocument.value = null
  try {
    const doc = await contractApi.getContractDocument(props.contract.id)
    documentHtml.value = doc.htmlContent
    currentDocument.value = doc
  } catch {
    documentHtml.value = ''
    currentDocument.value = null
  } finally {
    documentLoading.value = false
  }
}

/* 合同正文编辑 */
const documentEditDialogVisible = ref(false)
const documentEditLoading = ref(false)

function handleEditDocument() {
  if (!currentDocument.value || !props.contract) return
  documentEditDialogVisible.value = true
}

async function handleSaveDocument(data: ContractDocumentEditForm) {
  documentEditLoading.value = true
  try {
    const saved = await contractApi.saveContractDocument(data)
    currentDocument.value = saved
    documentHtml.value = saved.htmlContent
    documentEditDialogVisible.value = false
    ElMessage.success('合同正文保存成功')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '保存合同正文失败')
  } finally {
    documentEditLoading.value = false
  }
}

/* 合同正文版本历史 */
const documentHistoryDialogVisible = ref(false)
const documentHistoryLoading = ref(false)
const documentHistoryList = ref<ContractDocument[]>([])

async function handleViewDocumentHistory() {
  if (!props.contract) return
  documentHistoryDialogVisible.value = true
  documentHistoryLoading.value = true
  documentHistoryList.value = []
  try {
    const history = await contractApi.getDocumentHistory(props.contract.id)
    documentHistoryList.value = history
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载版本历史失败')
  } finally {
    documentHistoryLoading.value = false
  }
}

async function handleViewVersion(version: number) {
  if (!props.contract) return
  try {
    const doc = await contractApi.getDocumentByVersion(props.contract.id, version)
    currentDocument.value = doc
    documentHtml.value = doc.htmlContent
    documentHistoryDialogVisible.value = false
    ElMessage.info(`已切换到 v${version} 版本`)
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载版本失败')
  }
}

/* ===== 智能分析数据（概览Tab到期提醒） ===== */
const detailExpiring = ref<ExpiringContract | null>(null)
const detailRisks = ref<ContractRisk[]>([])
const detailIntelligenceLoading = ref(false)

async function loadDetailIntelligence() {
  if (!props.contract) return
  detailIntelligenceLoading.value = true
  try {
    const [expiring, risks] = await Promise.all([
      contractIntelligenceApi.getExpiringContracts(365),
      contractIntelligenceApi.getContractRisks(),
    ])
    const contractNo = props.contract.contractNo
    detailExpiring.value = expiring.find(e => e.contractNo === contractNo) || null
    detailRisks.value = risks.filter(r => r.employeeName === props.contract!.employeeName)
  } catch {
    detailExpiring.value = null
    detailRisks.value = []
  } finally {
    detailIntelligenceLoading.value = false
  }
}

function getIntelligenceRiskStatus(level: string): string {
  const map: Record<string, string> = { high: 'error', medium: 'warning', low: 'info' }
  return map[level] || 'info'
}

/* ===== 变更记录 ===== */
const changeRecords = ref<ContractChangeRecord[]>([])
const changeRecordsLoading = ref(false)

async function loadChangeRecords() {
  if (!props.contract) return
  changeRecordsLoading.value = true
  try {
    changeRecords.value = await contractApi.getChangeRecords(props.contract.id)
  } catch {
    changeRecords.value = []
  } finally {
    changeRecordsLoading.value = false
  }
}

function getChangeTypeLabel(type: ContractChangeType): string {
  return ContractChangeTypeOptions.find(o => o.value === type)?.label || type
}

function getChangeStatusLabel(status: ContractChangeStatus): string {
  return ContractChangeStatusOptions.find(o => o.value === status)?.label || status
}

function handleChangeContract(changeType: ContractChangeType) {
  if (!props.contract) return
  emit('change-contract', props.contract.id, changeType)
}

function handleViewChangeDetail(record: ContractChangeRecord) {
  ElMessageBox.alert(
    `<div style="line-height:1.8;">
      <p><strong>变更标题：</strong>${record.title}</p>
      <p><strong>变更原因：</strong>${record.reason}</p>
      <p><strong>变更前：</strong>${record.beforeSummary}</p>
      <p><strong>变更后：</strong>${record.afterSummary}</p>
      <p><strong>生效日期：</strong>${record.effectiveDate || '-'}</p>
    </div>`,
    '变更详情',
    { dangerouslyUseHTMLString: true, confirmButtonText: '关闭' }
  )
}

async function handleSubmitChange(changeId: string) {
  try {
    await contractApi.submitChangeApproval(changeId)
    ElMessage.success('变更审批已通过')
    await loadChangeRecords()
    emit('refresh')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '提交失败')
  }
}

async function handleActivateChange(changeId: string) {
  try {
    await ElMessageBox.confirm('确认变更生效后将更新原合同信息，是否继续？', '确认生效')
    await contractApi.activateChange(changeId)
    ElMessage.success('变更已生效，原合同信息已更新')
    await loadChangeRecords()
    emit('refresh')
  } catch (error: unknown) {
    // ElMessageBox取消时抛出字符串'cancel'，不是Error实例
    if (error !== 'cancel' && error instanceof Error) {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

/* ===== 公司方签署对话框 ===== */
const companySignDialogVisible = ref(false)

function handleCompanySign() {
  if (!props.contract) return
  if ((props.contract.carrier || 'electronic') !== 'electronic') {
    ElMessage.warning('仅电子合同支持公司方电子签署，纸质合同请上传扫描件')
    return
  }
  companySignDialogVisible.value = true
}

async function onCompanySignSuccess() {
  companySignDialogVisible.value = false
  ElMessage.success('公司方签署成功，等待员工在员工端完成签署')
  emit('refresh')
}

/* ===== 纸质扫描件上传 ===== */
const paperScanDialogVisible = ref(false)

function handleUploadScan() {
  if (!props.contract) return
  paperScanDialogVisible.value = true
}

async function onPaperScanSuccess() {
  paperScanDialogVisible.value = false
  ElMessage.success('纸质合同扫描件上传成功')
  emit('refresh')
}

/* ===== 推送员工签署任务 ===== */
async function handlePushSignTask() {
  if (!props.contract) return
  try {
    await ElMessageBox.confirm(
      `确认向员工「${props.contract.employeeName}」推送合同「${props.contract.contractNo}」的签署任务吗？推送后员工可在员工端完成签署。`,
      '推送签署任务',
      { type: 'info', confirmButtonText: '确认推送', cancelButtonText: '取消' }
    )
  } catch { return }
  try {
    const expireDate = new Date()
    expireDate.setDate(expireDate.getDate() + 7)
    await contractApi.createSignTask(props.contract.id, {
      employeeId: props.contract.employeeId,
      taskType: 'sign',
      expireAt: expireDate.toISOString().slice(0, 19).replace('T', ' '),
      notifyChannels: ['app', 'sms'],
    })
    ElMessage.success('签署任务已推送到员工端')
    emit('refresh')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '推送签署任务失败')
  }
}

/* ===== 下载PDF ===== */
async function handleDownloadPdf() {
  if (!props.contract) return
  if ((props.contract.carrier || 'electronic') !== 'electronic') {
    ElMessage.warning('仅电子合同支持下载 PDF')
    return
  }
  try {
    const blob = await contractApi.downloadPdf(props.contract.id)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const safeContractNo = props.contract.contractNo.replace(/[\\/:*?"<>|]/g, '_')
    link.download = `合同_${safeContractNo}.pdf`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('PDF 下载已开始')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '下载失败')
  }
}

/* ===== 归档操作 ===== */
const archiveForm = ref<ContractArchiveFormData>({
  archiveDate: '',
  archiveLocation: '',
  archiveNo: '',
  retentionPeriod: 5,
  remark: '',
})
const archiveFormRef = ref()
const archiveFormRules = {
  archiveDate: [{ required: true, message: '请选择归档日期', trigger: 'change' }],
  archiveLocation: [{ required: true, message: '请输入归档位置', trigger: 'blur' }],
  retentionPeriod: [{ required: true, message: '请输入保管期限', trigger: 'blur' }],
}
const archiveDialogVisible = ref(false)

function handleArchive() {
  if (!props.contract) return
  archiveForm.value = {
    archiveDate: new Date().toISOString().slice(0, 10),
    archiveLocation: '',
    archiveNo: '',
    retentionPeriod: 5,
    remark: '',
  }
  archiveDialogVisible.value = true
}

async function handleArchiveSubmit() {
  try {
    await archiveFormRef.value?.validate()
  } catch { return }
  if (!props.contract) return
  try {
    await contractApi.archive(props.contract.id, archiveForm.value)
    ElMessage.success('归档成功')
    archiveDialogVisible.value = false
    emit('refresh')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '归档失败')
  }
}

/* ===== 续签/终止（从变更Tab触发，关闭详情后交由父组件处理） ===== */
function handleRenew() {
  if (!props.contract) return
  const contract = props.contract
  emit('update:visible', false)
  emit('renew', contract)
}

function handleTerminate() {
  if (!props.contract) return
  const contract = props.contract
  emit('update:visible', false)
  emit('terminate', contract)
}

/* ===== 操作按钮分发 ===== */
function handleActionCommand(command: string) {
  switch (command) {
    case 'edit': detailTab.value = 'document'; break
    case 'submitApproval': emit('refresh'); break
    case 'companySign': handleCompanySign(); break
    case 'pushSignTask': handlePushSignTask(); break
    case 'archive': handleArchive(); break
    default: break
  }
}

/* ===== 辅助方法 ===== */
function getContractTypeLabel(type: ContractType): string {
  return ContractTypeOptions.find(o => o.value === type)?.label || type
}

function getContractStatusLabel(status: ContractStatus): string {
  return ContractStatusOptions.find(o => o.value === status)?.label || status
}

function getArchiveStatusLabel(status: ContractArchiveStatus): string {
  return ContractArchiveStatusOptions.find(o => o.value === status)?.label || status
}

function getSignMethodLabel(method?: string): string {
  if (!method) return '-'
  const map: Record<string, string> = { electronic: '电子签署', paper: '纸质签署' }
  return map[method] || method
}

function getTriggerSourceLabel(source?: string): string {
  if (!source) return '-'
  const map: Record<string, string> = {
    onboarding: '入职触发',
    renewal: '续签触发',
    transfer: '内部调整触发',
    resignation: '离职触发',
    manual: '手动创建',
  }
  return map[source] || source
}

function getTermTypeLabel(termType?: LaborContractTerm): string {
  if (!termType) return '-'
  return LaborContractTermOptions.find(o => o.value === termType)?.label || termType
}

function getCarrierLabel(carrier?: ContractCarrier): string {
  if (!carrier) return '电子'
  return ContractCarrierOptions.find(o => o.value === carrier)?.label || '电子'
}

function getSourceTypeLabel(type?: DocumentSourceType): string {
  if (!type) return '-'
  return DocumentSourceTypeOptions.find(o => o.value === type)?.label || type
}

function getClauseTypeLabel(clauseType: string): string {
  return ClauseTypeOptions.find(o => o.value === clauseType)?.label || clauseType
}

function getClauseTypeTagStatus(clauseType: string): string {
  if (clauseType === 'essential') return 'error'
  if (clauseType === 'optional') return 'info'
  return 'warning'
}

function getVerifyMethodLabel(method?: string): string {
  if (!method) return '-'
  const map: Record<string, string> = { sms: '短信验证', email: '邮箱验证', face: '人脸识别' }
  return map[method] || method
}

function getSignatoryTypeLabel(type?: string): string {
  if (!type) return '-'
  const map: Record<string, string> = { company: '公司方', employee: '员工方', supplier: '供应商' }
  return map[type] || type
}

function getSignStatusLabels(status?: string): string {
  if (!status) return '-'
  const map: Record<string, string> = {
    pending: '待签署',
    signed: '已签署',
    rejected: '已驳回',
    expired: '已过期',
    cancelled: '已取消',
  }
  return map[status] || status
}

function handlePreviewScan(paperArchive: PaperArchiveInfo) {
  if (paperArchive.scanFileUrl) {
    window.open(paperArchive.scanFileUrl, '_blank')
  } else {
    ElMessage.warning('扫描件文件地址不存在')
  }
}

/* ===== 查看原合同（续签链路跳转） ===== */
async function handleViewParentContract(parentId: string) {
  try {
    const detail = await contractApi.getById(parentId)
    if (detail) {
      ElMessage.info(`已切换到原合同：${detail.contractNo}`)
    }
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载原合同失败')
  }
}

/* ===== Tab切换时按需加载关联数据 ===== */
watch(detailTab, (tab) => {
  loadedDetailTabs.value.add(tab)
  if (tab === 'signing' && props.contract) loadSignatureRecords()
  if (tab === 'changes' && props.contract) loadChangeRecords()
  if (tab === 'document' && props.contract) loadContractDocument()
})

/* ===== 对话框打开时重置 ===== */
watch(() => props.visible, (val) => {
  if (val) {
    detailTab.value = 'overview'
    loadedDetailTabs.value.clear()
    detailExpiring.value = null
    detailRisks.value = []
    documentHtml.value = ''
    currentDocument.value = null
    signatureRecords.value = []
    changeRecords.value = []
    // 概览Tab立即加载智能分析数据
    loadDetailIntelligence()
  }
})

/* ===== 关闭对话框 ===== */
function handleClose() {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="合同详情"
    width="1200px"
    class="fts-dialog--xl"
    destroy-on-close
    lock-scroll="false"
    @update:model-value="handleClose"
  >
    <template v-if="contract">
      <!-- 生命周期进度条（可点击跳转对应Tab） -->
      <div class="lifecycle-steps-wrapper">
        <el-steps :active="getLifecycleStep(contract.status, contract.archiveStatus)" finish-status="success" align-center class="lifecycle-steps">
          <el-step v-for="(step, idx) in lifecycleSteps" :key="step.title" :title="step.title" :description="step.description" @click="handleStepClick(idx)" />
        </el-steps>
      </div>

      <el-divider style="margin: var(--fts-space-3) 0;" />

      <el-alert v-if="contract.status === 'expiring'" type="warning" :closable="false" style="margin-bottom: var(--fts-space-3);">
        <template #title>该合同即将到期，请及时处理续签或终止</template>
      </el-alert>
      <el-alert v-if="contract.status === 'expired'" type="error" :closable="false" style="margin-bottom: var(--fts-space-3);">
        <template #title>该合同已过期</template>
      </el-alert>

      <el-tabs v-model="detailTab">
        <!-- ===== Tab 1: 概览 ===== -->
        <el-tab-pane v-if="visibleTabs.includes('overview')" label="概览" name="overview">
          <!-- 操作按钮区 -->
          <div v-if="currentStepActions.length" class="overview-actions">
            <el-button v-for="act in currentStepActions" :key="act.command" :type="act.type" size="small" @click="handleActionCommand(act.command)">
              {{ act.label }}
            </el-button>
          </div>

          <el-descriptions :column="2" border title="基本信息">
            <el-descriptions-item label="合同编号">{{ contract.contractNo }}</el-descriptions-item>
            <el-descriptions-item label="员工姓名">{{ contract.employeeName }}</el-descriptions-item>
            <el-descriptions-item label="合同类型">
              <StatusTag :status="ContractTypeTagMap[contract.contractType] || 'info'" :label="getContractTypeLabel(contract.contractType)" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="合同状态">
              <StatusTag :status="ContractStatusTagMap[contract.status]" :label="getContractStatusLabel(contract.status)" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="合同期限">{{ contract.startDate }} ~ {{ contract.endDate || '无固定期限' }}</el-descriptions-item>
            <el-descriptions-item label="签署方式">
              <StatusTag v-if="contract.signMethod" :status="contract.signMethod === 'electronic' ? 'active' : 'info'" :label="getSignMethodLabel(contract.signMethod)" size="small" />
              <span v-else>-</span>
            </el-descriptions-item>
            <el-descriptions-item label="部门">{{ contract.departmentName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="职位">{{ contract.positionName || '-' }}</el-descriptions-item>
          </el-descriptions>
          <el-descriptions v-if="contract.contractType === 'labor'" :column="2" border size="small" style="margin-top: var(--fts-space-3);" title="用工信息">
            <el-descriptions-item label="用工形式">{{ contract.employmentForm || '-' }}</el-descriptions-item>
            <el-descriptions-item label="工作地点">{{ contract.workLocation || '-' }}</el-descriptions-item>
            <el-descriptions-item label="试用期">{{ contract.probationPeriod ? `${contract.probationPeriod} 个月` : '无' }}</el-descriptions-item>
            <el-descriptions-item label="试用期薪资">{{ contract.probationSalary ? `${contract.probationSalary} 元/月` : '-' }}</el-descriptions-item>
          </el-descriptions>
          <el-descriptions :column="2" border size="small" style="margin-top: var(--fts-space-3);" title="薪资信息">
            <el-descriptions-item label="基本薪资">{{ contract.baseSalary ? `${contract.baseSalary} 元/月` : '-' }}</el-descriptions-item>
            <el-descriptions-item label="薪资结构">{{ contract.salaryStructure || '-' }}</el-descriptions-item>
          </el-descriptions>

          <!-- 到期提醒（从旧"履行"Tab移入） -->
          <ContentCard title="到期提醒" style="margin-top: var(--fts-space-3);">
            <div v-if="detailIntelligenceLoading" class="intelligence-skeleton">
              <div class="intelligence-skeleton-section">
                <div class="skeleton-title-bar" />
                <div class="skeleton-grid">
                  <div class="skeleton-cell" />
                  <div class="skeleton-cell" />
                  <div class="skeleton-cell wide" />
                </div>
              </div>
            </div>
            <div v-else class="intelligence-content">
              <template v-if="detailExpiring">
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="剩余天数">
                    <span :class="['remaining-days', `remaining-${detailExpiring.alertLevel}`]">
                      {{ detailExpiring.remainingDays < 0 ? `已过期${Math.abs(detailExpiring.remainingDays)}天` : detailExpiring.remainingDays === 0 ? '今日到期' : `剩余${detailExpiring.remainingDays}天` }}
                    </span>
                  </el-descriptions-item>
                  <el-descriptions-item label="提醒级别">
                    <StatusTag :status="detailExpiring.alertLevel === 'expired' ? 'error' : detailExpiring.alertLevel === 'urgent' ? 'warning' : 'primary'" :label="{ expired: '已过期', urgent: '紧急', warning: '预警', notice: '提醒' }[detailExpiring.alertLevel] || '提醒'" size="small" />
                  </el-descriptions-item>
                  <el-descriptions-item label="续签推荐" :span="2">
                    <StatusTag :status="detailExpiring.recommendation === 'strong' ? 'success' : detailExpiring.recommendation === 'normal' ? 'primary' : detailExpiring.recommendation === 'cautious' ? 'warning' : 'error'" :label="{ strong: '强烈推荐续签', normal: '建议续签', cautious: '谨慎续签', not_recommended: '不建议续签' }[detailExpiring.recommendation] || '建议续签'" size="small" />
                  </el-descriptions-item>
                  <el-descriptions-item label="推荐理由" :span="2">{{ detailExpiring.recommendationReason || '-' }}</el-descriptions-item>
                </el-descriptions>
              </template>
              <el-alert v-else type="success" :closable="false">
                <template #title>该合同暂无到期预警</template>
              </el-alert>
            </div>
          </ContentCard>

          <!-- 风险提示 -->
          <ContentCard v-if="currentLegalWarning" title="风险提示" style="margin-top: var(--fts-space-3);">
            <el-alert
              :type="currentLegalWarning.level === 'error' ? 'error' : currentLegalWarning.level === 'warning' ? 'warning' : 'info'"
              :closable="false"
              show-icon
            >
              <template #title>{{ currentLegalWarning.title }}</template>
              <div style="line-height: 1.6; margin-top: var(--fts-space-1);">{{ currentLegalWarning.content }}</div>
            </el-alert>
          </ContentCard>

          <!-- 风险识别（从旧"履行"Tab移入） -->
          <ContentCard v-if="detailRisks.length > 0" title="风险识别" style="margin-top: var(--fts-space-3);">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item v-for="(risk, idx) in detailRisks" :key="risk.riskId" :label="`风险 ${idx + 1}`">
                <div class="risk-card-fadein" :style="{ animationDelay: `${idx * 0.1}s` }">
                  <div style="display: flex; align-items: center; gap: var(--fts-space-2); margin-bottom: var(--fts-space-2);">
                    <StatusTag :status="getIntelligenceRiskStatus(risk.riskLevel)" :label="RiskLevelLabelMap[risk.riskLevel as keyof typeof RiskLevelLabelMap]" size="small" />
                    <StatusTag status="warning" :label="RiskTypeLabelMap[risk.riskType as keyof typeof RiskTypeLabelMap]" size="small" variant="outline" />
                  </div>
                  <div style="color: var(--fts-text-regular); line-height: 1.6; margin-bottom: var(--fts-space-1);">{{ risk.description }}</div>
                  <div style="color: var(--fts-text-secondary); font-size: var(--fts-font-size-sm); padding-top: var(--fts-space-1); border-top: 1px dashed var(--fts-border-primary);">{{ risk.suggestion }}</div>
                </div>
              </el-descriptions-item>
            </el-descriptions>
          </ContentCard>

          <!-- 续签链路 -->
          <ContentCard v-if="contract.parentContractId || contract.lifecycleLink?.triggerSource === 'renewal'" title="续签链路" style="margin-top: var(--fts-space-3);">
            <el-alert type="success" :closable="false" show-icon style="margin-bottom: var(--fts-space-3);">
              <template #title>该合同为续签合同，与原合同形成链路关系</template>
              <div style="line-height: 1.6; margin-top: var(--fts-space-1);">
                续签链路确保合同管理的连续性：原合同状态变为"已续签"，新合同继承原合同的关键信息（合同类型、载体、模板、专属字段等），并基于原合同模板自动生成正式正文。
              </div>
            </el-alert>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="原合同ID">
                <el-link type="primary" :underline="false" @click="handleViewParentContract(contract.parentContractId!)">
                  {{ contract.parentContractId }}
                </el-link>
              </el-descriptions-item>
              <el-descriptions-item label="续签次数">第 {{ contract.renewCount }} 次续签</el-descriptions-item>
              <el-descriptions-item v-if="contract.lifecycleLink" label="触发来源">
                {{ getTriggerSourceLabel(contract.lifecycleLink.triggerSource) }}
              </el-descriptions-item>
              <el-descriptions-item v-if="contract.lifecycleLink" label="来源描述">
                {{ contract.lifecycleLink.triggerSourceDesc || '-' }}
              </el-descriptions-item>
              <el-descriptions-item v-if="contract.templateId" label="继承模板">
                {{ contract.templateId }}
                <span style="color: var(--fts-text-secondary); font-size: 12px; margin-left: var(--fts-space-1);">（从原合同继承）</span>
              </el-descriptions-item>
            </el-descriptions>
          </ContentCard>
        </el-tab-pane>

        <!-- ===== Tab 2: 正文 ===== -->
        <el-tab-pane v-if="visibleTabs.includes('document')" label="正文" name="document">
          <ContentCard title="合同正文">
            <el-descriptions v-if="currentDocument" :column="3" border size="small" style="margin-bottom: var(--fts-space-3);">
              <el-descriptions-item label="来源类型">
                <StatusTag v-if="currentDocument.sourceType" :status="DocumentSourceTypeTagMap[currentDocument.sourceType]" :label="getSourceTypeLabel(currentDocument.sourceType)" size="small" />
                <span v-else>-</span>
              </el-descriptions-item>
              <el-descriptions-item label="版本号">v{{ currentDocument.version || 1 }}</el-descriptions-item>
              <el-descriptions-item label="创建人">{{ currentDocument.createdBy || '-' }}</el-descriptions-item>
            </el-descriptions>
            <div v-loading="documentLoading" style="min-height: 120px;">
              <div v-if="documentHtml" class="contract-document-preview" v-html="documentHtml"></div>
              <el-empty v-else-if="!documentLoading" description="暂无合同正文" :image-size="60" />
            </div>
            <template v-if="currentDocument" #footer>
              <el-button size="small" @click="handleViewDocumentHistory">版本历史</el-button>
              <el-button v-if="contract.status === 'draft'" type="primary" size="small" @click="handleEditDocument">编辑正文</el-button>
            </template>
          </ContentCard>
        </el-tab-pane>

        <!-- ===== Tab 3: 签署（合并审批+签署） ===== -->
        <el-tab-pane v-if="visibleTabs.includes('signing')" label="签署" name="signing">
          <!-- 审批流程 -->
          <ContentCard title="审批流程">
            <el-timeline>
              <el-timeline-item type="primary" :hollow="contract.status === 'draft'">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span style="font-weight: 500;">提交审批</span>
                  <StatusTag :status="contract.status === 'draft' ? 'pending' : 'success'" :label="contract.status === 'draft' ? '待提交' : '已提交'" size="small" />
                </div>
                <div v-if="contract.status !== 'draft'" style="color: var(--fts-text-secondary); font-size: var(--fts-font-size-sm); margin-top: var(--fts-space-1);">
                  提交人：{{ contract.createdBy || '系统' }} | 提交时间：{{ contract.createTime || '-' }}
                </div>
              </el-timeline-item>
              <el-timeline-item :type="contract.status === 'draft' ? 'info' : 'success'" :hollow="contract.status === 'draft'">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span style="font-weight: 500;">审批通过</span>
                  <StatusTag :status="contract.status === 'draft' ? 'pending' : 'success'" :label="contract.status === 'draft' ? '待审批' : '已通过'" size="small" />
                </div>
                <div v-if="contract.status !== 'draft'" style="color: var(--fts-text-secondary); font-size: var(--fts-font-size-sm); margin-top: var(--fts-space-1);">
                  审批人：{{ contract.approvedBy || 'HR主管' }} | 审批时间：{{ contract.approvalTime || '-' }}
                </div>
              </el-timeline-item>
            </el-timeline>
          </ContentCard>

          <!-- 签署进度条 -->
          <ContentCard title="签署进度" style="margin-top: var(--fts-space-3);">
            <el-steps :active="signingStep" finish-status="success" align-center>
              <el-step v-for="step in signingSteps" :key="step.title" :title="step.title" :description="step.description" />
            </el-steps>
          </ContentCard>

          <!-- 签署双方信息 -->
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: var(--fts-space-3); margin-top: var(--fts-space-3);">
            <!-- 公司方签署 -->
            <ContentCard title="公司方签署">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="签署状态">
                  <StatusTag
                    :status="contract.companySignTime ? 'active' : 'pending'"
                    :label="contract.companySignTime ? '已签署' : '待签署'"
                    size="small"
                  />
                </el-descriptions-item>
                <el-descriptions-item v-if="contract.companySignTime" label="签署时间">{{ contract.companySignTime }}</el-descriptions-item>
                <el-descriptions-item label="签署方式">
                  <StatusTag
                    :status="(contract.carrier || 'electronic') === 'electronic' ? 'active' : 'info'"
                    :label="getCarrierLabel(contract.carrier)"
                    size="small"
                  />
                </el-descriptions-item>
              </el-descriptions>
              <template v-if="!contract.companySignTime && (contract.carrier || 'electronic') === 'electronic'" #footer>
                <el-button type="primary" size="small" @click="handleCompanySign">公司签署</el-button>
              </template>
            </ContentCard>

            <!-- 员工方签署 -->
            <ContentCard title="员工方签署">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="签署状态">
                  <StatusTag
                    :status="contract.employeeSignTime ? 'active' : 'pending'"
                    :label="contract.employeeSignTime ? '已签署' : (contract.companySignTime ? '待推送' : '待公司签署')"
                    size="small"
                  />
                </el-descriptions-item>
                <el-descriptions-item label="签署人">{{ contract.employeeName }}</el-descriptions-item>
                <el-descriptions-item v-if="contract.employeeSignTime" label="签署时间">{{ contract.employeeSignTime }}</el-descriptions-item>
              </el-descriptions>
              <template v-if="(contract.status === 'pending_sign' || contract.status === 'company_signed') && !contract.employeeSignTime" #footer>
                <el-button type="success" size="small" @click="handlePushSignTask">推送签署</el-button>
              </template>
            </ContentCard>
          </div>

          <!-- 签署记录表格 -->
          <ContentCard title="签署记录" style="margin-top: var(--fts-space-3);">
            <el-table :data="signatureRecords" v-loading="signatureLoading" border size="small" style="width: 100%;">
              <el-table-column prop="signatoryType" label="签署方" width="100">
                <template #default="{ row }">
                  <StatusTag :status="row.signatoryType === 'company' ? 'primary' : row.signatoryType === 'employee' ? 'success' : 'info'" :label="getSignatoryTypeLabel(row.signatoryType)" size="small" />
                </template>
              </el-table-column>
              <el-table-column prop="signatoryName" label="签署人" width="100" />
              <el-table-column prop="signMethod" label="签署方式" width="100">
                <template #default="{ row }">{{ getSignMethodLabel(row.signMethod) }}</template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="90">
                <template #default="{ row }: { row: SignatureRecord }">
                  <StatusTag :status="SignTaskStatusTagMap[row.status as SignTaskStatus] || 'info'" :label="getSignStatusLabels(row.status)" size="small" />
                </template>
              </el-table-column>
              <el-table-column prop="signTime" label="签署时间" width="160" />
              <el-table-column prop="failReason" label="备注" min-width="120">
                <template #default="{ row }">{{ row.failReason || row.signDevice || '-' }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!signatureLoading && signatureRecords.length === 0" description="暂无签署记录" :image-size="60" />
          </ContentCard>

          <!-- 安全验证信息 -->
          <ContentCard v-if="contract.securityVerification" title="安全验证" style="margin-top: var(--fts-space-3);">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="验证方式">
                <StatusTag :status="contract.securityVerification.method === 'sms' ? 'active' : contract.securityVerification.method === 'email' ? 'info' : 'warning'" :label="getVerifyMethodLabel(contract.securityVerification.method)" size="small" />
              </el-descriptions-item>
              <el-descriptions-item label="验证时间">{{ contract.securityVerification.verifiedAt || '-' }}</el-descriptions-item>
              <el-descriptions-item label="验证人">{{ contract.securityVerification.verifiedBy || '-' }}</el-descriptions-item>
              <el-descriptions-item label="IP地址">{{ contract.securityVerification.ipAddress || '-' }}</el-descriptions-item>
            </el-descriptions>
          </ContentCard>
        </el-tab-pane>

        <!-- ===== Tab 4: 变更 ===== -->
        <el-tab-pane v-if="visibleTabs.includes('changes')" label="变更" name="changes">
          <!-- 操作按钮 -->
          <div v-if="contract.status === 'active' || contract.status === 'expiring'" class="change-actions">
            <el-button size="small" @click="handleChangeContract('position_change')">岗位/部门变更</el-button>
            <el-button size="small" @click="handleChangeContract('term_change')">期限变更</el-button>
            <el-button size="small" @click="handleChangeContract('supplementary')">补充协议</el-button>
            <el-button size="small" @click="handleRenew">续签</el-button>
            <el-button v-if="contract.status === 'active'" size="small" @click="handleTerminate">终止</el-button>
          </div>

          <!-- 变更记录列表 -->
          <el-table :data="changeRecords" v-loading="changeRecordsLoading" border size="small" style="width: 100%;">
            <el-table-column prop="title" label="变更标题" min-width="150" />
            <el-table-column prop="changeType" label="变更类型" width="120">
              <template #default="{ row }: { row: ContractChangeRecord }">
                <StatusTag :status="ContractChangeTypeTagMap[row.changeType]" :label="getChangeTypeLabel(row.changeType)" size="small" />
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }: { row: ContractChangeRecord }">
                <StatusTag :status="ContractChangeStatusTagMap[row.status]" :label="getChangeStatusLabel(row.status)" size="small" />
              </template>
            </el-table-column>
            <el-table-column prop="effectiveDate" label="生效日期" width="110" />
            <el-table-column prop="createTime" label="创建时间" width="160" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }: { row: ContractChangeRecord }">
                <el-button link type="primary" size="small" @click="handleViewChangeDetail(row)">查看</el-button>
                <el-button v-if="row.status === 'draft'" link type="success" size="small" @click="handleSubmitChange(row.id)">提交审批</el-button>
                <el-button v-if="row.status === 'approved'" link type="primary" size="small" @click="handleActivateChange(row.id)">确认生效</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!changeRecordsLoading && changeRecords.length === 0" description="暂无变更记录" :image-size="60" />
        </el-tab-pane>

        <!-- ===== Tab 5: 归档 ===== -->
        <el-tab-pane v-if="visibleTabs.includes('archive')" label="归档" name="archive">
          <ContentCard title="归档信息">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="归档状态">
                <StatusTag v-if="contract.archiveStatus && contract.archiveStatus !== 'active'" :status="ContractArchiveStatusTagMap[contract.archiveStatus]" :label="getArchiveStatusLabel(contract.archiveStatus)" size="small" />
                <span v-else>未归档</span>
              </el-descriptions-item>
              <el-descriptions-item label="归档日期">{{ contract.archiveDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="归档位置">{{ contract.archiveLocation || '-' }}</el-descriptions-item>
              <el-descriptions-item label="归档编号">{{ contract.archiveNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="保管期限">{{ contract.retentionPeriod ? `${contract.retentionPeriod} 年` : '-' }}</el-descriptions-item>
              <el-descriptions-item label="保管到期日">{{ contract.retentionEndDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="归档人" :span="2">{{ contract.archivedBy || '-' }}</el-descriptions-item>
            </el-descriptions>
            <template v-if="!contract.archiveDate && (contract.status === 'terminated' || contract.status === 'expired' || contract.status === 'renewed')" #footer>
              <el-button type="primary" size="small" @click="handleArchive">归档合同</el-button>
            </template>
          </ContentCard>
          <!-- 纸质合同档案 -->
          <ContentCard v-if="(contract.carrier || 'electronic') === 'paper' && contract.paperArchive" title="纸质合同档案" style="margin-top: var(--fts-space-3);">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="档案编号">{{ contract.paperArchive?.archiveNo || contract.paperArchiveNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="存放位置">{{ contract.paperArchive?.archiveLocation || contract.paperArchiveLocation || '-' }}</el-descriptions-item>
              <el-descriptions-item v-if="contract.paperArchive?.scanFileName" label="扫描件" :span="2">
                <el-link type="primary" :underline="false" @click="handlePreviewScan(contract.paperArchive)">{{ contract.paperArchive.scanFileName }}</el-link>
              </el-descriptions-item>
            </el-descriptions>
          </ContentCard>
          <!-- 电子合同档案 -->
          <ContentCard v-if="(contract.carrier || 'electronic') === 'electronic' && contract.electronicArchive" title="电子合同档案" style="margin-top: var(--fts-space-3);">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item v-if="contract.electronicArchive.signedPdfUrl" label="已签署PDF" :span="2">
                <el-link type="primary" :underline="false" @click="handleDownloadPdf">下载已签署PDF</el-link>
              </el-descriptions-item>
              <el-descriptions-item v-if="contract.electronicArchive.downloadCount !== undefined" label="下载次数">{{ contract.electronicArchive.downloadCount }} 次</el-descriptions-item>
              <el-descriptions-item v-if="contract.electronicArchive.lastDownloadTime" label="最后下载时间">{{ contract.electronicArchive.lastDownloadTime }}</el-descriptions-item>
              <el-descriptions-item v-if="contract.electronicArchive.allowPrint !== undefined" label="是否允许打印">
                <StatusTag :status="contract.electronicArchive.allowPrint ? 'active' : 'inactive'" :label="contract.electronicArchive.allowPrint ? '允许' : '禁止'" size="small" />
              </el-descriptions-item>
            </el-descriptions>
          </ContentCard>
        </el-tab-pane>
      </el-tabs>
    </template>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
      </div>
    </template>

    <!-- 公司方签署对话框（嵌套） -->
    <CompanySignDialog
      v-model:visible="companySignDialogVisible"
      :contract="contract"
      @success="onCompanySignSuccess"
    />

    <!-- 纸质合同扫描件上传对话框 -->
    <PaperScanUploadDialog
      v-model:visible="paperScanDialogVisible"
      :contract="contract"
      @success="onPaperScanSuccess"
    />

    <!-- 合同正文编辑对话框 -->
    <el-dialog v-model="documentEditDialogVisible" title="编辑合同正文" width="960px" class="fts-dialog--lg" destroy-on-close lock-scroll="false" :close-on-click-modal="false" :teleported="false">
      <div v-loading="documentEditLoading">
        <ContractDocumentEditor
          :document="currentDocument"
          @save="handleSaveDocument"
          @cancel="documentEditDialogVisible = false"
        />
      </div>
    </el-dialog>

    <!-- 合同正文版本历史对话框 -->
    <el-dialog v-model="documentHistoryDialogVisible" title="合同正文版本历史" width="960px" class="fts-dialog--lg" destroy-on-close lock-scroll="false" :teleported="false">
      <div v-loading="documentHistoryLoading">
        <el-table v-if="documentHistoryList.length" :data="documentHistoryList" border stripe>
          <el-table-column prop="version" label="版本" width="80">
            <template #default="{ row }">
              <span>v{{ row.version }}</span>
              <StatusTag v-if="row.isCurrent" status="active" label="当前" size="small" />
            </template>
          </el-table-column>
          <el-table-column prop="sourceType" label="来源" width="100">
            <template #default="{ row }">
              <StatusTag
                v-if="row.sourceType"
                :status="DocumentSourceTypeTagMap[row.sourceType as DocumentSourceType]"
                :label="getSourceTypeLabel(row.sourceType)"
                size="small"
              />
            </template>
          </el-table-column>
          <el-table-column prop="sourceDesc" label="来源描述" min-width="150" />
          <el-table-column prop="createdBy" label="操作人" width="100" />
          <el-table-column prop="updateTime" label="修改时间" width="160" />
          <el-table-column prop="editRemark" label="修改备注" min-width="120" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handleViewVersion(row.version)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else-if="!documentHistoryLoading" description="暂无版本历史" />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="documentHistoryDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 归档对话框 -->
    <el-dialog v-model="archiveDialogVisible" title="合同归档" width="680px" class="fts-dialog--md" destroy-on-close lock-scroll="false" :teleported="false">
      <template v-if="contract">
        <el-descriptions :column="1" border style="margin-bottom: var(--fts-space-4);">
          <el-descriptions-item label="合同编号">{{ contract.contractNo }}</el-descriptions-item>
          <el-descriptions-item label="员工姓名">{{ contract.employeeName }}</el-descriptions-item>
          <el-descriptions-item label="合同类型">
            <StatusTag :status="ContractTypeTagMap[contract.contractType]" :label="getContractTypeLabel(contract.contractType)" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="合同状态">
            <StatusTag :status="ContractStatusTagMap[contract.status]" :label="getContractStatusLabel(contract.status)" size="small" />
          </el-descriptions-item>
        </el-descriptions>
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: var(--fts-space-4);">
          <template #title>归档说明</template>
          <div style="line-height: 1.6; margin-top: var(--fts-space-1);">
            合同终止/到期后应于30日内归档，保管期限默认5年（涉密合同10年）。保管期满后方可按程序销毁。
          </div>
        </el-alert>
      </template>
      <el-form ref="archiveFormRef" :model="archiveForm" :rules="archiveFormRules" label-width="100px">
        <el-form-item label="归档日期" prop="archiveDate">
          <el-date-picker v-model="archiveForm.archiveDate" type="date" placeholder="选择归档日期" :teleported="false" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="归档位置" prop="archiveLocation">
          <el-input v-model="archiveForm.archiveLocation" placeholder="如：档案柜A-01-03" />
        </el-form-item>
        <el-form-item label="归档编号" prop="archiveNo">
          <el-input v-model="archiveForm.archiveNo" placeholder="如：ARC-2026-001" />
        </el-form-item>
        <el-form-item label="保管期限" prop="retentionPeriod">
          <el-input-number v-model="archiveForm.retentionPeriod" :min="1" :max="30" :step="1" placeholder="年" style="width:100%" />
          <span style="margin-left: var(--fts-space-2); color: var(--fts-text-secondary); font-size: 12px;">年（普通合同5年，涉密合同10年）</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="archiveForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="archiveDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleArchiveSubmit">确认归档</el-button>
        </div>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<style scoped lang="scss">
/* 生命周期进度条 */
.lifecycle-steps-wrapper {
  padding: var(--fts-space-5) var(--fts-space-6);
  background: var(--fts-bg-card);
  border-radius: var(--fts-page-radius);
  border: 1px solid var(--fts-border-primary);
}

/* 概览Tab操作按钮区 */
.overview-actions {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-3);
}

/* 变更Tab操作按钮区 */
.change-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-3);
}

/* el-steps 深度样式定制 */
.lifecycle-steps {
  /* 已完成步骤圆圈 - 带脉冲光晕 */
  :deep(.el-step__head.is-finish) {
    .el-step__icon {
      background: var(--fts-primary);
      border-color: var(--fts-primary);
      box-shadow: 0 0 0 4px rgba(var(--fts-primary-rgb), 0.18);
      transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    }
  }

  /* 当前活跃步骤 - 呼吸光效 */
  :deep(.el-step__head.is-process) {
    .el-step__icon {
      background: var(--fts-primary);
      border-color: var(--fts-primary);
      color: var(--fts-text-inverse);
      animation: step-breathe 2s ease-in-out infinite;
      transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    }
  }

  /* 已完成步骤标题 */
  :deep(.el-step__title.is-finish) {
    color: var(--fts-primary);
    font-weight: 600;
    transition: color 0.3s;
  }

  /* 当前步骤标题 */
  :deep(.el-step__title.is-process) {
    color: var(--fts-primary);
    font-weight: 700;
    transition: color 0.3s;
  }

  /* 已完成连线 - 渐变色 */
  :deep(.el-step__head.is-finish .el-step__line) {
    background: linear-gradient(90deg, var(--fts-primary), rgba(var(--fts-primary-rgb), 0.4));
    transition: background 0.5s;
  }

  /* 步骤描述 */
  :deep(.el-step__description) {
    transition: color 0.3s;
  }

  :deep(.el-step__description.is-finish) {
    color: var(--fts-text-secondary);
  }

  :deep(.el-step__description.is-process) {
    color: var(--fts-text-regular);
  }

  /* 步骤可点击样式 */
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

/* 当前步骤呼吸动画 */
@keyframes step-breathe {
  0%, 100% { box-shadow: 0 0 0 4px rgba(var(--fts-primary-rgb), 0.18); }
  50% { box-shadow: 0 0 0 8px rgba(var(--fts-primary-rgb), 0.30); }
}

/* 合同正文预览 */
.contract-document-preview {
  padding: var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  max-height: 500px;
  overflow-y: auto;
  line-height: 1.8;

  :deep(h1) { font-size: 20px; }
  :deep(p) { margin: 8px 0; }
}

/* ===== 骨架屏加载动画 ===== */
.intelligence-skeleton {
  padding: var(--fts-space-2) 0;
}

.intelligence-skeleton-section {
  margin-bottom: var(--fts-space-5);

  &:last-child { margin-bottom: 0; }
}

.skeleton-title-bar {
  width: 100px;
  height: 18px;
  border-radius: 4px;
  margin-bottom: var(--fts-space-3);
  background: linear-gradient(90deg, var(--fts-fill-light) 25%, var(--fts-fill-lighter) 50%, var(--fts-fill-light) 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s ease-in-out infinite;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--fts-space-3);
}

.skeleton-cell {
  height: 32px;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--fts-fill-light) 25%, var(--fts-fill-lighter) 50%, var(--fts-fill-light) 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s ease-in-out infinite;

  &.wide { grid-column: span 2; }
}

.skeleton-card {
  height: 72px;
  border-radius: var(--fts-page-radius);
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

/* ===== 智能分析内容渐显动画 ===== */
.intelligence-content {
  animation: intelligence-fadein 0.4s ease-out;
}

@keyframes intelligence-fadein {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 风险卡片逐项渐显 */
.risk-card-fadein {
  opacity: 0;
  animation: risk-card-enter 0.35s ease-out forwards;
}

@keyframes risk-card-enter {
  from { opacity: 0; transform: translateX(-12px); }
  to { opacity: 1; transform: translateX(0); }
}

.remaining-days {
  font-weight: 600;

  &.remaining-expired { color: var(--fts-error); }
  &.remaining-urgent { color: var(--fts-warning); }
  &.remaining-warning { color: var(--fts-warning); }
  &.remaining-notice { color: var(--fts-primary); }
}
</style>
