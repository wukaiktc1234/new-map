<script setup lang="ts">
/**
 * 合同起草对话框（多步骤）
 * 流程：选择类型与模板 → 填写业务表单 → 预览正式合同正文 → 提交创建
 *
 * 设计参考：法大大/e签宝等专业电子合同平台
 * - 模板+变量填充 → 生成正式合同 → 审批 → 签署 → 归档
 * - 起草阶段即生成完整正文，避免"只有条款没有正文"的空洞
 */
import { ref, computed, watch, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { contractApi } from '@/api/hr/contract'
import { contractTemplateApi } from '@/api/hr/contract-template'
import type {
  EmployeeContract,
  ContractType,
  ContractFormData,
  ContractCarrier,
  ContractTemplate,
} from '@/types/hr/contract'
import {
  ContractTypeOptions,
  ContractTypeTagMap,
  LaborContractTermOptions,
  ContractCarrierOptions,
  ContractCarrierTagMap,
} from '@/types/hr/contract'
import type { Employee } from '@/types/hr'
import { fenToYuan, yuanToFen } from '@/utils/finance-utils'
import StatusTag from '@/components/core/StatusTag.vue'
import ContentCard from '@/components/core/ContentCard.vue'

interface Props {
  /** 对话框显隐 */
  visible: boolean
  /** 员工选项 */
  employeeOptions: Employee[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  /** 更新显隐 */
  (e: 'update:visible', val: boolean): void
  /** 创建成功 */
  (e: 'success', contract: EmployeeContract): void
}>()

/* ===== 步骤控制 ===== */
const currentStep = ref(0)
const steps = [
  { title: '选择类型与模板', description: '选择合同类型和模板' },
  { title: '填写业务表单', description: '录入合同要素' },
  { title: '预览合同正文', description: '确认正式正文' },
  { title: '完成创建', description: '提交保存' },
]

/** 重置步骤 */
function resetSteps() {
  currentStep.value = 0
  templateList.value = []
  selectedTemplateId.value = ''
  previewHtml.value = ''
  previewLoading.value = false
  generating.value = false
}

/* ===== 步骤1：选择类型与模板 ===== */
const contractForm = reactive<ContractFormData>(defaultFormData())
const selectedTemplateId = ref<string>('')
const templateList = ref<ContractTemplate[]>([])
const templateLoading = ref(false)

function defaultFormData(): ContractFormData {
  return {
    employeeId: '',
    contractType: 'labor',
    contractNo: '',
    termType: undefined,
    startDate: '',
    endDate: '',
    probationEndDate: '',
    signDate: '',
    carrier: 'electronic',
    paperArchiveNo: undefined,
    paperArchiveLocation: undefined,
    electronicAllowPrint: true,
    dispatchCompanyName: undefined,
    dispatchCompanyCode: undefined,
    dispatchPeriod: undefined,
    dispatchPositionCategory: undefined,
    dispatchManagementFeeRate: undefined,
    outsourcingCompanyName: undefined,
    outsourcingCompanyCode: undefined,
    outsourcingServiceScope: undefined,
    outsourcingSettlementMethod: undefined,
    insurancePolicyNo: undefined,
    insuranceExpiryDate: undefined,
    medicalExamDate: undefined,
    medicalExamResult: undefined,
    retirementDate: undefined,
    pensionLocation: undefined,
    positionAdaptationNote: undefined,
    hourlyRate: undefined,
    weeklyHours: undefined,
    dailyMaxHours: undefined,
    settlementCycle: undefined,
    internSchool: undefined,
    internMajor: undefined,
    internPeriod: undefined,
    internSubsidy: undefined,
    internSchoolContact: undefined,
    confidentialityScope: undefined,
    confidentialityPeriod: undefined,
    confidentialityFee: undefined,
    confidentialityPenalty: undefined,
    nonCompeteCompensation: undefined,
    nonCompetePeriod: undefined,
    nonCompeteScope: undefined,
    nonCompetePenalty: undefined,
    templateId: undefined,
    remark: '',
  }
}

/** 加载指定合同类型的可用模板 */
async function loadTemplates(contractType: ContractType) {
  templateLoading.value = true
  templateList.value = []
  selectedTemplateId.value = ''
  try {
    const list = await contractTemplateApi.getActiveTemplates(contractType)
    templateList.value = list || []
    // 自动选中第一个模板
    if (templateList.value.length > 0) {
      selectedTemplateId.value = templateList.value[0].id
      contractForm.templateId = selectedTemplateId.value
    }
  } catch {
    templateList.value = []
  } finally {
    templateLoading.value = false
  }
}

/** 切换合同类型时重新加载模板 */
watch(() => contractForm.contractType, (newType) => {
  if (newType) loadTemplates(newType)
})

/** 选择模板 */
function handleSelectTemplate(templateId: string) {
  selectedTemplateId.value = templateId
  contractForm.templateId = templateId
}

/** 步骤1下一步校验 */
function handleStep1Next() {
  if (!contractForm.contractType) {
    ElMessage.warning('请选择合同类型')
    return
  }
  if (!selectedTemplateId.value) {
    ElMessage.warning('请选择合同模板')
    return
  }
  currentStep.value = 1
}

/* ===== 步骤2：业务表单 ===== */
const formRef = ref()
const formRules = {
  employeeId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  contractNo: [{ required: true, message: '请输入合同编号', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  signDate: [{ required: true, message: '请选择签订日期', trigger: 'change' }],
  carrier: [{ required: true, message: '请选择合同载体', trigger: 'change' }],
}

/** 兼职时薪（元输入 ↔ 分存储） */
const hourlyRateYuan = computed<number | undefined>({
  get: () => {
    const fen = contractForm.hourlyRate
    return fen === undefined || fen === null ? undefined : fenToYuan(fen)
  },
  set: (val) => {
    contractForm.hourlyRate = val === undefined || val === null ? undefined : yuanToFen(val)
  },
})

/** 竞业限制补偿金（元/月 ↔ 分/月） */
const nonCompeteCompensationYuan = computed<number | undefined>({
  get: () => {
    const fen = contractForm.nonCompeteCompensation
    return fen === undefined || fen === null ? undefined : fenToYuan(fen)
  },
  set: (val) => {
    contractForm.nonCompeteCompensation = val === undefined || val === null ? undefined : yuanToFen(val)
  },
})

/** 保密费（元/月 ↔ 分/月） */
const confidentialityFeeYuan = computed<number | undefined>({
  get: () => {
    const fen = contractForm.confidentialityFee
    return fen === undefined || fen === null ? undefined : fenToYuan(fen)
  },
  set: (val) => {
    contractForm.confidentialityFee = val === undefined || val === null ? undefined : yuanToFen(val)
  },
})

/** 保密违约金（元 ↔ 分） */
const confidentialityPenaltyYuan = computed<number | undefined>({
  get: () => {
    const fen = contractForm.confidentialityPenalty
    return fen === undefined || fen === null ? undefined : fenToYuan(fen)
  },
  set: (val) => {
    contractForm.confidentialityPenalty = val === undefined || val === null ? undefined : yuanToFen(val)
  },
})

/** 竞业违约金（元 ↔ 分） */
const nonCompetePenaltyYuan = computed<number | undefined>({
  get: () => {
    const fen = contractForm.nonCompetePenalty
    return fen === undefined || fen === null ? undefined : fenToYuan(fen)
  },
  set: (val) => {
    contractForm.nonCompetePenalty = val === undefined || val === null ? undefined : yuanToFen(val)
  },
})

/** 实习补贴（元/月 ↔ 分/月） */
const internSubsidyYuan = computed<number | undefined>({
  get: () => {
    const fen = contractForm.internSubsidy
    return fen === undefined || fen === null ? undefined : fenToYuan(fen)
  },
  set: (val) => {
    contractForm.internSubsidy = val === undefined || val === null ? undefined : yuanToFen(val)
  },
})

/** 步骤2下一步校验 */
async function handleStep2Next() {
  try {
    await formRef.value?.validate()
  } catch {
    ElMessage.warning('请完善必填项')
    return
  }
  // 进入步骤3，自动生成预览
  currentStep.value = 2
  await generatePreview()
}

/* ===== 步骤3：预览合同正文 ===== */
const previewHtml = ref('')
const previewLoading = ref(false)
const generating = ref(false)

/** 从表单数据构建模板变量 */
function buildVariablesFromForm(): Record<string, string | number> {
  const emp = props.employeeOptions.find(e => e.id === contractForm.employeeId)
  return {
    employeeName: emp?.employeeName || '',
    contractNo: contractForm.contractNo || '',
    startDate: contractForm.startDate || '',
    endDate: contractForm.endDate || '无固定期限',
    departmentName: emp?.departmentName || '',
    positionName: emp?.positionName || '',
    signDate: contractForm.signDate || new Date().toISOString().slice(0, 10),
    companyName: 'XX餐饮管理有限公司',
    // 类型相关变量
    ...(contractForm.contractType === 'part_time' ? {
      hourlyRate: hourlyRateYuan.value || 0,
      weeklyHours: contractForm.weeklyHours || 0,
      dailyMaxHours: contractForm.dailyMaxHours || 0,
      settlementCycle: contractForm.settlementCycle || 0,
    } : {}),
    ...(contractForm.contractType === 'confidentiality' ? {
      confidentialityScope: contractForm.confidentialityScope || '',
      confidentialityPeriod: contractForm.confidentialityPeriod || 0,
      confidentialityFeeYuan: confidentialityFeeYuan.value || 0,
      confidentialityPenaltyYuan: confidentialityPenaltyYuan.value || 0,
    } : {}),
    ...(contractForm.contractType === 'non_compete' ? {
      nonCompeteCompensationYuan: nonCompeteCompensationYuan.value || 0,
      nonCompetePeriod: contractForm.nonCompetePeriod || 0,
      nonCompeteScope: contractForm.nonCompeteScope || '',
      nonCompetePenaltyYuan: nonCompetePenaltyYuan.value || 0,
    } : {}),
    ...(contractForm.contractType === 'intern' ? {
      internSchool: contractForm.internSchool || '',
      internMajor: contractForm.internMajor || '',
      internPeriod: contractForm.internPeriod || 0,
      internSubsidyYuan: internSubsidyYuan.value || 0,
    } : {}),
    ...(contractForm.contractType === 'dispatch' ? {
      dispatchCompanyName: contractForm.dispatchCompanyName || '',
      dispatchCompanyCode: contractForm.dispatchCompanyCode || '',
      dispatchPeriod: contractForm.dispatchPeriod || 0,
      dispatchPositionCategory: contractForm.dispatchPositionCategory || '',
    } : {}),
    ...(contractForm.contractType === 'outsourcing' ? {
      outsourcingCompanyName: contractForm.outsourcingCompanyName || '',
      outsourcingCompanyCode: contractForm.outsourcingCompanyCode || '',
      outsourcingServiceScope: contractForm.outsourcingServiceScope || '',
      outsourcingSettlementMethod: contractForm.outsourcingSettlementMethod || '',
    } : {}),
    ...(contractForm.contractType === 'over_age' ? {
      retirementDate: contractForm.retirementDate || '',
      pensionLocation: contractForm.pensionLocation || '',
      insurancePolicyNo: contractForm.insurancePolicyNo || '',
      insuranceExpiryDate: contractForm.insuranceExpiryDate || '',
      medicalExamDate: contractForm.medicalExamDate || '',
      medicalExamResult: contractForm.medicalExamResult || '',
      positionAdaptationNote: contractForm.positionAdaptationNote || '',
    } : {}),
  }
}

/** 生成合同正文预览（调用模板preview API进行变量替换） */
async function generatePreview() {
  if (!selectedTemplateId.value) {
    ElMessage.warning('请先选择合同模板')
    return
  }
  previewLoading.value = true
  previewHtml.value = ''
  try {
    const variables = buildVariablesFromForm()
    const html = await contractTemplateApi.preview({
      templateId: selectedTemplateId.value,
      variables,
    })
    previewHtml.value = html
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '预览生成失败')
  } finally {
    previewLoading.value = false
  }
}

/** 重新生成预览 */
async function handleRegeneratePreview() {
  await generatePreview()
}

/** 步骤3下一步 */
function handleStep3Next() {
  currentStep.value = 3
}

/* ===== 步骤4：提交创建 ===== */
/** 提交创建合同（同时生成正式正文） */
async function handleSubmit() {
  generating.value = true
  try {
    // 1. 创建合同
    const contract = await contractApi.create(contractForm)
    // 2. 自动生成正式合同正文（模板变量填充）
    if (selectedTemplateId.value) {
      try {
        await contractApi.generateDocument({
          contractId: contract.id,
          templateId: selectedTemplateId.value,
          variables: buildVariablesFromForm(),
        })
      } catch {
        // 正文生成失败不阻断创建流程，但提示用户
        ElMessage.warning('合同已创建，但正文生成失败，请稍后在详情页手动生成')
      }
    }
    ElMessage.success('合同起草完成，已生成正式正文，可提交审批')
    emit('success', contract)
    handleClose()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '创建失败')
  } finally {
    generating.value = false
  }
}

/* ===== 对话框控制 ===== */
const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
})

/** 打开对话框时初始化 */
watch(() => props.visible, (val) => {
  if (val) {
    Object.assign(contractForm, defaultFormData())
    resetSteps()
    // 加载默认类型的模板
    loadTemplates(contractForm.contractType)
  }
})

/** 关闭对话框 */
function handleClose() {
  dialogVisible.value = false
}

/** 上一步 */
function handlePrev() {
  if (currentStep.value > 0) currentStep.value -= 1
}

/** 获取合同类型标签 */
function getContractTypeLabel(type: ContractType): string {
  return ContractTypeOptions.find(o => o.value === type)?.label || type
}

/** 获取载体标签 */
function getCarrierLabel(carrier?: ContractCarrier): string {
  if (!carrier) return '电子'
  return ContractCarrierOptions.find(o => o.value === carrier)?.label || '电子'
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="起草合同"
    width="1200px"
    class="fts-dialog--xl"
    destroy-on-close
    lock-scroll="false"
    :close-on-click-modal="false"
  >
    <!-- 步骤条 -->
    <el-steps :active="currentStep" finish-status="success" align-center class="create-steps">
      <el-step v-for="(step, idx) in steps" :key="step.title" :title="step.title" :description="step.description" />
    </el-steps>

    <div class="step-content">
      <!-- 步骤1：选择类型与模板 -->
      <div v-if="currentStep === 0" class="step-pane">
        <ContentCard title="选择合同类型">
          <el-radio-group v-model="contractForm.contractType" class="contract-type-group">
            <el-radio-button v-for="opt in ContractTypeOptions" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </el-radio-button>
          </el-radio-group>
        </ContentCard>

        <ContentCard title="选择合同模板" style="margin-top: var(--fts-space-3);">
          <div v-loading="templateLoading" class="template-list">
            <el-empty v-if="!templateLoading && templateList.length === 0" description="该合同类型暂无可用模板" :image-size="60" />
            <div
              v-for="tpl in templateList"
              :key="tpl.id"
              :class="['template-card', { 'template-card--active': selectedTemplateId === tpl.id }]"
              @click="handleSelectTemplate(tpl.id)"
            >
              <div class="template-card__header">
                <span class="template-card__name">{{ tpl.templateName }}</span>
                <StatusTag :status="ContractTypeTagMap[tpl.contractType]" :label="getContractTypeLabel(tpl.contractType)" size="small" />
              </div>
              <div class="template-card__meta">
                <span>版本：v{{ tpl.version }}</span>
                <span>变量：{{ tpl.templateVariables?.length || 0 }} 个</span>
              </div>
              <div v-if="tpl.description" class="template-card__desc">{{ tpl.description }}</div>
            </div>
          </div>
        </ContentCard>
      </div>

      <!-- 步骤2：填写业务表单 -->
      <div v-else-if="currentStep === 1" class="step-pane">
        <el-form ref="formRef" :model="contractForm" :rules="formRules" label-width="120px">
          <el-divider content-position="left">基础信息</el-divider>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="员工" prop="employeeId">
                <el-select v-model="contractForm.employeeId" placeholder="请选择员工" :teleported="false" filterable style="width:100%">
                  <el-option v-for="emp in employeeOptions" :key="emp.id" :label="`${emp.employeeName}（${emp.employeeCode}）`" :value="emp.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="合同编号" prop="contractNo">
                <el-input v-model="contractForm.contractNo" placeholder="请输入合同编号" />
              </el-form-item>
            </el-col>
            <el-col v-if="contractForm.contractType === 'labor'" :span="12">
              <el-form-item label="合同期限类型">
                <el-select v-model="contractForm.termType" placeholder="请选择期限类型" :teleported="false" style="width:100%">
                  <el-option v-for="opt in LaborContractTermOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="合同载体" prop="carrier">
                <el-radio-group v-model="contractForm.carrier">
                  <el-radio v-for="opt in ContractCarrierOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 纸质合同专属字段 -->
          <el-row v-if="contractForm.carrier === 'paper'" :gutter="16">
            <el-col :span="12">
              <el-form-item label="档案编号">
                <el-input v-model="contractForm.paperArchiveNo" placeholder="如 HR-2024-001" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="存放位置">
                <el-input v-model="contractForm.paperArchiveLocation" placeholder="如 档案柜A-3-02" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-divider content-position="left">期限信息</el-divider>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="开始日期" prop="startDate">
                <el-date-picker v-model="contractForm.startDate" type="date" placeholder="选择开始日期" :teleported="false" value-format="YYYY-MM-DD" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="结束日期" :required="contractForm.termType !== 'unfixed'">
                <el-date-picker v-model="contractForm.endDate" type="date" placeholder="选择结束日期" :teleported="false" value-format="YYYY-MM-DD" style="width:100%" :disabled="contractForm.termType === 'unfixed'" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="签订日期" prop="signDate">
                <el-date-picker v-model="contractForm.signDate" type="date" placeholder="选择签订日期" :teleported="false" value-format="YYYY-MM-DD" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col v-if="contractForm.contractType === 'labor'" :span="12">
              <el-form-item label="试用期结束日期">
                <el-date-picker v-model="contractForm.probationEndDate" type="date" placeholder="选择试用期结束日期" :teleported="false" value-format="YYYY-MM-DD" style="width:100%" />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 兼职专属字段 -->
          <template v-if="contractForm.contractType === 'part_time'">
            <el-divider content-position="left">兼职专属信息</el-divider>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="兼职时薪(元)">
                  <el-input-number v-model="hourlyRateYuan" :min="0" :precision="2" :step="1" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="每周工作时长">
                  <el-input-number v-model="contractForm.weeklyHours" :min="0" :max="24" :precision="0" :step="1" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="每日最大工时">
                  <el-input-number v-model="contractForm.dailyMaxHours" :min="0" :max="4" :precision="0" :step="1" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="结算周期(天)">
                  <el-input-number v-model="contractForm.settlementCycle" :min="1" :max="15" :precision="0" :step="1" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <!-- 实习专属字段 -->
          <template v-if="contractForm.contractType === 'intern'">
            <el-divider content-position="left">实习专属信息</el-divider>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="实习学校">
                  <el-input v-model="contractForm.internSchool" placeholder="请输入实习学校" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="实习专业">
                  <el-input v-model="contractForm.internMajor" placeholder="请输入实习专业" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="实习期限(月)">
                  <el-input-number v-model="contractForm.internPeriod" :min="1" :max="12" :precision="0" :step="1" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="实习补贴(元/月)">
                  <el-input-number v-model="internSubsidyYuan" :min="0" :precision="2" :step="100" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="学校联系人">
                  <el-input v-model="contractForm.internSchoolContact" placeholder="如：王老师 13800138000" />
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <!-- 劳务派遣专属字段 -->
          <template v-if="contractForm.contractType === 'dispatch'">
            <el-divider content-position="left">劳务派遣专属信息</el-divider>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="派遣公司名称">
                  <el-input v-model="contractForm.dispatchCompanyName" placeholder="请输入派遣公司名称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="派遣公司信用代码">
                  <el-input v-model="contractForm.dispatchCompanyCode" placeholder="统一社会信用代码" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="派遣期限(月)">
                  <el-input-number v-model="contractForm.dispatchPeriod" :min="1" :max="36" :precision="0" :step="1" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="派遣岗位类别">
                  <el-select v-model="contractForm.dispatchPositionCategory" placeholder="请选择岗位类别" :teleported="false" style="width:100%">
                    <el-option label="临时性" value="临时性" />
                    <el-option label="辅助性" value="辅助性" />
                    <el-option label="替代性" value="替代性" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <!-- 劳务外包专属字段 -->
          <template v-if="contractForm.contractType === 'outsourcing'">
            <el-divider content-position="left">劳务外包专属信息</el-divider>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="外包公司名称">
                  <el-input v-model="contractForm.outsourcingCompanyName" placeholder="请输入外包公司名称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="外包公司信用代码">
                  <el-input v-model="contractForm.outsourcingCompanyCode" placeholder="统一社会信用代码" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="服务范围">
                  <el-input v-model="contractForm.outsourcingServiceScope" type="textarea" :rows="2" placeholder="请输入服务范围" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="结算方式">
                  <el-select v-model="contractForm.outsourcingSettlementMethod" placeholder="请选择结算方式" :teleported="false" style="width:100%">
                    <el-option label="按月结算" value="按月结算" />
                    <el-option label="按季度结算" value="按季度结算" />
                    <el-option label="按项目结算" value="按项目结算" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <!-- 超龄用工专属字段 -->
          <template v-if="contractForm.contractType === 'over_age'">
            <el-divider content-position="left">超龄用工专属信息</el-divider>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="退休日期">
                  <el-date-picker v-model="contractForm.retirementDate" type="date" placeholder="选择退休日期" :teleported="false" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="养老金发放地">
                  <el-input v-model="contractForm.pensionLocation" placeholder="请输入养老金发放地" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="意外伤害保险单号">
                  <el-input v-model="contractForm.insurancePolicyNo" placeholder="请输入保险单号" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="保险有效期至">
                  <el-date-picker v-model="contractForm.insuranceExpiryDate" type="date" placeholder="选择保险有效期" :teleported="false" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="体检日期">
                  <el-date-picker v-model="contractForm.medicalExamDate" type="date" placeholder="选择体检日期" :teleported="false" value-format="YYYY-MM-DD" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="体检结果">
                  <el-input v-model="contractForm.medicalExamResult" placeholder="请输入体检结果" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="岗位适配说明">
                  <el-input v-model="contractForm.positionAdaptationNote" type="textarea" :rows="2" placeholder="说明岗位适配情况" />
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <!-- 保密协议专属字段 -->
          <template v-if="contractForm.contractType === 'confidentiality'">
            <el-divider content-position="left">保密协议专属信息</el-divider>
            <el-row :gutter="16">
              <el-col :span="24">
                <el-form-item label="保密范围">
                  <el-input v-model="contractForm.confidentialityScope" type="textarea" :rows="2" placeholder="如：技术信息、经营信息、客户资料等" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="保密期限(月)">
                  <el-input-number v-model="contractForm.confidentialityPeriod" :min="1" :max="60" :precision="0" :step="1" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="保密费(元/月)">
                  <el-input-number v-model="confidentialityFeeYuan" :min="0" :precision="2" :step="100" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="违约金(元)">
                  <el-input-number v-model="confidentialityPenaltyYuan" :min="0" :precision="2" :step="10000" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <!-- 竞业限制专属字段 -->
          <template v-if="contractForm.contractType === 'non_compete'">
            <el-divider content-position="left">竞业限制专属信息</el-divider>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="竞业补偿金(元/月)">
                  <el-input-number v-model="nonCompeteCompensationYuan" :min="0" :precision="2" :step="100" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="竞业限制期限(月)">
                  <el-input-number v-model="contractForm.nonCompetePeriod" :min="0" :max="24" :precision="0" :step="1" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="限制范围">
                  <el-input v-model="contractForm.nonCompeteScope" type="textarea" :rows="2" placeholder="竞争对手清单、地域范围、业务领域" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="违约金(元)">
                  <el-input-number v-model="nonCompetePenaltyYuan" :min="0" :precision="2" :step="10000" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <el-divider content-position="left">其他信息</el-divider>
          <el-row :gutter="16">
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="contractForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <!-- 步骤3：预览合同正文 -->
      <div v-else-if="currentStep === 2" class="step-pane">
        <ContentCard title="合同正文预览">
          <template #actions>
            <el-button size="small" :loading="previewLoading" @click="handleRegeneratePreview">重新生成</el-button>
          </template>
          <el-alert type="info" :closable="false" show-icon style="margin-bottom: var(--fts-space-3);">
            <template #title>合同正文由模板 + 业务表单变量自动填充生成</template>
            <div style="line-height: 1.6; margin-top: var(--fts-space-1);">
              此正文为正式合同内容，提交后将作为合同正文保存。如需修改，可返回上一步调整表单后重新生成，或在合同详情中编辑正文。
            </div>
          </el-alert>
          <div v-loading="previewLoading" class="preview-wrapper">
            <div v-if="previewHtml" class="contract-document-preview" v-html="previewHtml"></div>
            <el-empty v-else-if="!previewLoading" description="暂无预览内容" :image-size="60" />
          </div>
        </ContentCard>
      </div>

      <!-- 步骤4：完成创建 -->
      <div v-else-if="currentStep === 3" class="step-pane">
        <ContentCard title="确认创建">
          <el-alert type="success" :closable="false" show-icon style="margin-bottom: var(--fts-space-3);">
            <template #title>即将创建合同并生成正式正文</template>
            <div style="line-height: 1.6; margin-top: var(--fts-space-1);">
              合同创建后状态为"草稿"，可在合同详情中提交审批，审批通过后进入签署流程。
            </div>
          </el-alert>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="合同类型">
              <StatusTag :status="ContractTypeTagMap[contractForm.contractType]" :label="getContractTypeLabel(contractForm.contractType)" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="合同载体">
              <StatusTag :status="ContractCarrierTagMap[contractForm.carrier || 'electronic']" :label="getCarrierLabel(contractForm.carrier)" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="合同编号">{{ contractForm.contractNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="员工">
              {{ employeeOptions.find(e => e.id === contractForm.employeeId)?.employeeName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="合同期限">
              {{ contractForm.startDate }} ~ {{ contractForm.endDate || '无固定期限' }}
            </el-descriptions-item>
            <el-descriptions-item label="签订日期">{{ contractForm.signDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="使用模板" :span="2">
              {{ templateList.find(t => t.id === selectedTemplateId)?.templateName || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </ContentCard>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button v-if="currentStep > 0 && currentStep < 3" @click="handlePrev">上一步</el-button>
        <el-button v-if="currentStep === 0" type="primary" @click="handleStep1Next">下一步</el-button>
        <el-button v-if="currentStep === 1" type="primary" @click="handleStep2Next">下一步</el-button>
        <el-button v-if="currentStep === 2" type="primary" @click="handleStep3Next">下一步</el-button>
        <el-button v-if="currentStep === 3" type="primary" :loading="generating" @click="handleSubmit">确认创建</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.create-steps {
  margin-bottom: var(--fts-space-5);
}

.step-content {
  min-height: 400px;
}

.step-pane {
  animation: step-fadein 0.3s ease-out;
}

@keyframes step-fadein {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 合同类型选择 */
.contract-type-group {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fts-space-2);
}

/* 模板列表 */
.template-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--fts-space-3);
  min-height: 80px;
}

.template-card {
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-card);
  border: 2px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: var(--fts-primary);
    box-shadow: 0 2px 8px rgba(var(--fts-primary-rgb), 0.1);
  }

  &--active {
    border-color: var(--fts-primary);
    background: rgba(var(--fts-primary-rgb), 0.04);
    box-shadow: 0 2px 8px rgba(var(--fts-primary-rgb), 0.15);
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--fts-space-2);
  }

  &__name {
    font-weight: 600;
    color: var(--fts-text-primary);
    font-size: 14px;
  }

  &__meta {
    display: flex;
    gap: var(--fts-space-3);
    color: var(--fts-text-secondary);
    font-size: 12px;
    margin-bottom: var(--fts-space-1);
  }

  &__desc {
    color: var(--fts-text-regular);
    font-size: 12px;
    line-height: 1.5;
  }
}

/* 合同正文预览 */
.preview-wrapper {
  min-height: 300px;
}

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
</style>
