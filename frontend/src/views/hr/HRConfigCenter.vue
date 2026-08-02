<script setup lang="ts">
/**
 * HR配置中心页面
 *
 * 四个Tab配置：
 * 1. 病假扣薪：按地区配置，不同地区政策不同
 * 2. 扣薪规则：按实际出勤计酬（劳动法合规）
 * 3. 加班调休：可自定义，部分地方标准高于基本法
 * 4. 个税计算：可自定义，防止政策调整
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, DocumentChecked, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { hrConfigApi } from '@/api/hr/config'
import type {
  SickPayConfig,
  SickPayRegionConfig,
  DeductionRuleConfig,
  OvertimeConfig,
  TaxConfig,
  SickPayCalcMethod,
  DeductionType,
  OvertimeType,
} from '@/types/hr/config'
import {
  SickPayCalcMethodOptions,
  DeductionTypeOptions,
  OvertimeTypeOptions,
  OvertimeCompensationOptions,
  SpecialDeductionTypeOptions,
} from '@/types/hr/config'

const activeTab = ref<'sick_pay' | 'deduction' | 'overtime' | 'tax'>('sick_pay')
const loading = ref(false)
const saving = ref(false)

/* ===== 病假扣薪配置 ===== */
const sickPayConfig = ref<SickPayConfig | null>(null)

/* ===== 扣薪规则配置 ===== */
const deductionConfig = ref<DeductionRuleConfig | null>(null)

/* ===== 加班调休配置 ===== */
const overtimeConfig = ref<OvertimeConfig | null>(null)

/* ===== 个税计算配置 ===== */
const taxConfig = ref<TaxConfig | null>(null)

/* ===== 预览计算 ===== */
const sickPayPreview = reactive({
  dailyWage: 200,
  sickDays: 3,
  serviceYears: 2,
  regionCode: 'default',
  result: 0,
  detail: '',
})
const taxPreview = reactive({
  grossSalary: 8000,
  insuranceDeduction: 800,
  specialDeduction: 2000,
  result: 0,
  detail: '',
})

/* ===== 加载配置 ===== */
async function loadAllConfigs() {
  loading.value = true
  try {
    const [sickPay, deduction, overtime, tax] = await Promise.all([
      hrConfigApi.getSickPayConfig(),
      hrConfigApi.getDeductionConfig(),
      hrConfigApi.getOvertimeConfig(),
      hrConfigApi.getTaxConfig(),
    ])
    sickPayConfig.value = sickPay
    deductionConfig.value = deduction
    overtimeConfig.value = overtime
    taxConfig.value = tax
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载配置失败')
  } finally {
    loading.value = false
  }
}

/* ===== 保存配置 ===== */
async function handleSaveSickPay() {
  if (!sickPayConfig.value) return
  saving.value = true
  try {
    await hrConfigApi.saveSickPayConfig(sickPayConfig.value)
    ElMessage.success('病假扣薪配置保存成功')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSaveDeduction() {
  if (!deductionConfig.value) return
  saving.value = true
  try {
    await hrConfigApi.saveDeductionConfig(deductionConfig.value)
    ElMessage.success('扣薪规则配置保存成功')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSaveOvertime() {
  if (!overtimeConfig.value) return
  saving.value = true
  try {
    await hrConfigApi.saveOvertimeConfig(overtimeConfig.value)
    ElMessage.success('加班调休配置保存成功')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSaveTax() {
  if (!taxConfig.value) return
  saving.value = true
  try {
    await hrConfigApi.saveTaxConfig(taxConfig.value)
    ElMessage.success('个税计算配置保存成功')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

/* ===== 病假扣薪：地区管理 ===== */
function handleAddRegion() {
  if (!sickPayConfig.value) return
  sickPayConfig.value.regions.push({
    regionCode: `region_${Date.now()}`,
    regionName: '新地区',
    calcMethod: 'fixed_ratio',
    fixedRatio: 0.6,
    minSickPay: 2000,
    enabled: true,
  })
}

function handleRemoveRegion(index: number) {
  if (!sickPayConfig.value) return
  sickPayConfig.value.regions.splice(index, 1)
}

function handleAddProgressiveItem(region: SickPayRegionConfig) {
  if (!region.progressiveItems) region.progressiveItems = []
  region.progressiveItems.push({ fromDays: 1, toDays: 7, payRatio: 0.8 })
}

function handleAddServiceYearItem(region: SickPayRegionConfig) {
  if (!region.serviceYearItems) region.serviceYearItems = []
  region.serviceYearItems.push({ fromYears: 0, toYears: 2, payRatio: 0.6 })
}

/* ===== 预览计算 ===== */
async function handlePreviewSickPay() {
  try {
    const res = await hrConfigApi.previewSickPay({
      dailyWage: sickPayPreview.dailyWage,
      sickDays: sickPayPreview.sickDays,
      serviceYears: sickPayPreview.serviceYears,
      regionCode: sickPayPreview.regionCode,
    })
    sickPayPreview.result = res.sickPay
    sickPayPreview.detail = res.calcDetail
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '计算失败')
  }
}

async function handlePreviewTax() {
  try {
    const res = await hrConfigApi.previewTax({
      grossSalary: taxPreview.grossSalary,
      insuranceDeduction: taxPreview.insuranceDeduction,
      specialDeduction: taxPreview.specialDeduction,
    })
    taxPreview.result = res.taxAmount
    taxPreview.detail = res.calcDetail
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '计算失败')
  }
}

/* ===== 辅助函数 ===== */
function getSickPayCalcMethodLabel(method: SickPayCalcMethod): string {
  return SickPayCalcMethodOptions.find(o => o.value === method)?.label || method
}

function getDeductionTypeLabel(type: DeductionType): string {
  return DeductionTypeOptions.find(o => o.value === type)?.label || type
}

function getOvertimeTypeLabel(type: OvertimeType): string {
  return OvertimeTypeOptions.find(o => o.value === type)?.label || type
}

function getSpecialDeductionLabel(type: string): string {
  return SpecialDeductionTypeOptions.find(o => o.value === type)?.label || type
}

onMounted(() => {
  loadAllConfigs()
})
</script>

<template>
  <div class="hr-config-center">
    <PageHeader title="配置中心" :icon="Setting">
      <template #description>
        管理病假扣薪、扣薪规则、加班调休、个税计算等薪资相关配置，支持自定义以适配不同地区政策和法规调整
      </template>
    </PageHeader>

    <el-tabs v-model="activeTab" class="page-tabs" v-loading="loading">
      <!-- ===== Tab 1: 病假扣薪配置 ===== -->
      <el-tab-pane label="病假扣薪" name="sick_pay">
        <div v-if="sickPayConfig" class="config-content">
          <!-- 基础信息 -->
          <el-card shadow="never" class="config-card">
            <template #header>
              <div class="card-header">
                <span>基础信息</span>
                <el-button type="primary" :icon="DocumentChecked" :loading="saving" @click="handleSaveSickPay">保存配置</el-button>
              </div>
            </template>
            <el-form label-width="140px" label-position="right">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="配置名称">
                    <el-input v-model="sickPayConfig.name" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="最低病假工资">
                    <el-input-number v-model="sickPayConfig.defaultRegion.minSickPay" :min="0" :step="100" style="width: 100%" />
                    <div class="form-tip">不低于当地最低工资的80%</div>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-card>

          <!-- 默认地区配置 -->
          <el-card shadow="never" class="config-card">
            <template #header>
              <span>默认地区配置（未匹配到特定地区时使用）</span>
            </template>
            <el-form label-width="140px" label-position="right">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="地区名称">
                    <el-input v-model="sickPayConfig.defaultRegion.regionName" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="计算方式">
                    <el-select v-model="sickPayConfig.defaultRegion.calcMethod" style="width: 100%">
                      <el-option v-for="opt in SickPayCalcMethodOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>

              <!-- 固定比例 -->
              <el-form-item v-if="sickPayConfig.defaultRegion.calcMethod === 'fixed_ratio'" label="发放比例">
                <el-slider v-model="sickPayConfig.defaultRegion.fixedRatio" :min="0" :max="1" :step="0.05" show-input :format-tooltip="(val: number) => `${(val * 100).toFixed(0)}%`" />
              </el-form-item>

              <!-- 阶梯比例 -->
              <template v-if="sickPayConfig.defaultRegion.calcMethod === 'progressive'">
                <el-form-item label="阶梯比例配置">
                  <el-table :data="sickPayConfig.defaultRegion.progressiveItems" border size="small">
                    <el-table-column label="起始天数" width="120">
                      <template #default="{ row }">
                        <el-input-number v-model="row.fromDays" :min="1" size="small" controls-position="right" />
                      </template>
                    </el-table-column>
                    <el-table-column label="结束天数" width="120">
                      <template #default="{ row }">
                        <el-input-number v-model="row.toDays" :min="0" size="small" controls-position="right" placeholder="空=无上限" />
                      </template>
                    </el-table-column>
                    <el-table-column label="发放比例">
                      <template #default="{ row }">
                        <el-slider v-model="row.payRatio" :min="0" :max="1" :step="0.05" show-input />
                      </template>
                    </el-table-column>
                  </el-table>
                </el-form-item>
              </template>

              <!-- 按工龄 -->
              <template v-if="sickPayConfig.defaultRegion.calcMethod === 'by_service_years'">
                <el-form-item label="工龄比例配置">
                  <el-table :data="sickPayConfig.defaultRegion.serviceYearItems" border size="small">
                    <el-table-column label="最低工龄" width="120">
                      <template #default="{ row }">
                        <el-input-number v-model="row.fromYears" :min="0" size="small" controls-position="right" />
                      </template>
                    </el-table-column>
                    <el-table-column label="最高工龄" width="120">
                      <template #default="{ row }">
                        <el-input-number v-model="row.toYears" :min="0" size="small" controls-position="right" placeholder="空=无上限" />
                      </template>
                    </el-table-column>
                    <el-table-column label="发放比例">
                      <template #default="{ row }">
                        <el-slider v-model="row.payRatio" :min="0" :max="1" :step="0.05" show-input />
                      </template>
                    </el-table-column>
                  </el-table>
                </el-form-item>
              </template>

              <el-form-item label="备注">
                <el-input v-model="sickPayConfig.defaultRegion.remark" type="textarea" :rows="2" />
              </el-form-item>
            </el-form>
          </el-card>

          <!-- 各地区配置 -->
          <el-card shadow="never" class="config-card">
            <template #header>
              <div class="card-header">
                <span>各地区配置（{{ sickPayConfig.regions.length }}个地区）</span>
                <el-button type="primary" size="small" @click="handleAddRegion">新增地区</el-button>
              </div>
            </template>
            <el-table :data="sickPayConfig.regions" border size="small">
              <el-table-column label="地区编码" prop="regionCode" width="120" />
              <el-table-column label="地区名称" width="120">
                <template #default="{ row }">
                  <el-input v-model="row.regionName" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="计算方式" width="140">
                <template #default="{ row }">
                  <el-select v-model="row.calcMethod" size="small" style="width: 100%">
                    <el-option v-for="opt in SickPayCalcMethodOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="发放比例" width="180">
                <template #default="{ row }">
                  <template v-if="row.calcMethod === 'fixed_ratio'">
                    <el-slider v-model="row.fixedRatio" :min="0" :max="1" :step="0.05" show-input />
                  </template>
                  <template v-else>
                    <span class="text-muted">{{ getSickPayCalcMethodLabel(row.calcMethod) }}</span>
                  </template>
                </template>
              </el-table-column>
              <el-table-column label="最低病假工资" width="130">
                <template #default="{ row }">
                  <el-input-number v-model="row.minSickPay" :min="0" :step="100" size="small" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="启用" width="80">
                <template #default="{ row }">
                  <el-switch v-model="row.enabled" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" fixed="right">
                <template #default="{ $index }">
                  <el-button link type="danger" size="small" @click="handleRemoveRegion($index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <!-- 预览计算 -->
          <el-card shadow="never" class="config-card">
            <template #header><span>病假工资计算预览</span></template>
            <el-form label-width="100px" label-position="right" inline>
              <el-form-item label="日工资">
                <el-input-number v-model="sickPayPreview.dailyWage" :min="0" :step="10" size="small" />
              </el-form-item>
              <el-form-item label="病假天数">
                <el-input-number v-model="sickPayPreview.sickDays" :min="1" :max="365" size="small" />
              </el-form-item>
              <el-form-item label="工龄">
                <el-input-number v-model="sickPayPreview.serviceYears" :min="0" :max="50" size="small" />
              </el-form-item>
              <el-form-item label="地区">
                <el-select v-model="sickPayPreview.regionCode" size="small" style="width: 120px">
                  <el-option label="默认" value="default" />
                  <el-option v-for="r in sickPayConfig.regions" :key="r.regionCode" :label="r.regionName" :value="r.regionCode" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" size="small" @click="handlePreviewSickPay">计算</el-button>
              </el-form-item>
            </el-form>
            <el-alert v-if="sickPayPreview.detail" :title="`病假工资：${sickPayPreview.result}元`" :description="sickPayPreview.detail" type="success" show-icon :closable="false" />
          </el-card>
        </div>
      </el-tab-pane>

      <!-- ===== Tab 2: 扣薪规则配置 ===== -->
      <el-tab-pane label="扣薪规则" name="deduction">
        <div v-if="deductionConfig" class="config-content">
          <el-card shadow="never" class="config-card">
            <template #header>
              <div class="card-header">
                <span>扣薪规则配置（按实际出勤计酬）</span>
                <el-button type="primary" :icon="DocumentChecked" :loading="saving" @click="handleSaveDeduction">保存配置</el-button>
              </div>
            </template>
            <el-alert type="info" :closable="false" show-icon class="config-alert">
              <template #title>
                根据劳动法相关规定，用人单位不得无故克扣、扣发工资。出现迟到/早退/旷工/请假时，应按实际出勤给予报酬。
                应当出勤8小时，实际只有6小时，就按6小时算。满勤奖在出现迟到/早退/旷工时取消。
              </template>
            </el-alert>
            <el-form label-width="160px" label-position="right">
              <el-row :gutter="20">
                <el-col :span="8">
                  <el-form-item label="标准日工作时长">
                    <el-input-number v-model="deductionConfig.standardWorkHours" :min="1" :max="12" :step="0.5" />
                    <span class="form-suffix">小时</span>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="标准月工作天数">
                    <el-input-number v-model="deductionConfig.standardWorkDays" :min="1" :max="31" :step="0.25" />
                    <span class="form-suffix">天</span>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="满勤奖金额">
                    <el-input-number v-model="deductionConfig.fullAttendanceBonus" :min="0" :step="50" />
                    <span class="form-suffix">元</span>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-card>

          <el-card shadow="never" class="config-card">
            <template #header><span>各扣薪类型规则</span></template>
            <el-table :data="deductionConfig.rules" border size="small">
              <el-table-column label="类型" width="100">
                <template #default="{ row }">
                  <StatusTag :status="row.type === 'absent' ? 'error' : row.type === 'annual_leave' ? 'success' : 'warning'" :label="getDeductionTypeLabel(row.type)" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="计算说明" min-width="300">
                <template #default="{ row }">
                  <el-input v-model="row.calcDescription" size="small" type="textarea" :rows="2" />
                </template>
              </el-table-column>
              <el-table-column label="宽限时间" width="120">
                <template #default="{ row }">
                  <el-input-number v-if="row.type === 'late' || row.type === 'early_leave'" v-model="row.graceMinutes" :min="0" :max="60" size="small" controls-position="right" />
                  <span v-else class="text-muted">-</span>
                </template>
              </el-table-column>
              <el-table-column label="扣除间隔" width="120">
                <template #default="{ row }">
                  <el-input-number v-if="row.type === 'late' || row.type === 'early_leave'" v-model="row.deductPerMinutes" :min="1" :max="120" size="small" controls-position="right" />
                  <span v-else-if="row.type === 'absent'">
                    <el-input-number v-model="row.absentDeductRatio" :min="0" :max="1" :step="0.1" size="small" controls-position="right" :precision="1" />
                  </span>
                  <span v-else class="text-muted">-</span>
                </template>
              </el-table-column>
              <el-table-column label="影响满勤奖" width="100">
                <template #default="{ row }">
                  <el-switch v-model="row.affectFullAttendance" />
                </template>
              </el-table-column>
              <el-table-column label="启用" width="80">
                <template #default="{ row }">
                  <el-switch v-model="row.enabled" />
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- ===== Tab 3: 加班调休配置 ===== -->
      <el-tab-pane label="加班调休" name="overtime">
        <div v-if="overtimeConfig" class="config-content">
          <el-card shadow="never" class="config-card">
            <template #header>
              <div class="card-header">
                <span>加班调休配置</span>
                <el-button type="primary" :icon="DocumentChecked" :loading="saving" @click="handleSaveOvertime">保存配置</el-button>
              </div>
            </template>
            <el-form label-width="160px" label-position="right">
              <el-row :gutter="20">
                <el-col :span="8">
                  <el-form-item label="日加班上限">
                    <el-input-number v-model="overtimeConfig.dailyOvertimeLimit" :min="0" :max="8" />
                    <span class="form-suffix">小时</span>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="月加班上限">
                    <el-input-number v-model="overtimeConfig.monthlyOvertimeLimit" :min="0" :max="100" />
                    <span class="form-suffix">小时</span>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="加班需审批">
                    <el-switch v-model="overtimeConfig.approvalRequired" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="高于法定标准">
                    <el-switch v-model="overtimeConfig.aboveLegalStandard" />
                    <span class="form-tip">开启后表示本企业标准高于法定标准</span>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="自定义说明">
                    <el-input v-model="overtimeConfig.customRemark" placeholder="如：本企业休息日加班按2.5倍发放" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-card>

          <el-card shadow="never" class="config-card">
            <template #header><span>各加班类型规则</span></template>
            <el-table :data="overtimeConfig.rules" border size="small">
              <el-table-column label="加班类型" width="140">
                <template #default="{ row }">
                  <StatusTag :status="row.type === 'holiday' ? 'error' : row.type === 'weekend' ? 'warning' : 'info'" :label="getOvertimeTypeLabel(row.type)" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="补偿方式" width="140">
                <template #default="{ row }">
                  <el-select v-model="row.compensation" size="small" style="width: 100%">
                    <el-option v-for="opt in OvertimeCompensationOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="加班费倍率" width="160">
                <template #default="{ row }">
                  <el-input-number v-model="row.payMultiplier" :min="1" :max="5" :step="0.5" size="small" controls-position="right" :precision="1" />
                </template>
              </el-table-column>
              <el-table-column label="调休倍率" width="160">
                <template #default="{ row }">
                  <el-input-number v-model="row.compTimeMultiplier" :min="1" :max="5" :step="0.5" size="small" controls-position="right" :precision="1" :disabled="!row.allowCompTime" />
                </template>
              </el-table-column>
              <el-table-column label="调休有效期" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.compTimeExpiryDays" :min="0" :max="365" size="small" controls-position="right" :disabled="!row.allowCompTime" />
                </template>
              </el-table-column>
              <el-table-column label="允许调休" width="80">
                <template #default="{ row }">
                  <el-switch v-model="row.allowCompTime" />
                </template>
              </el-table-column>
              <el-table-column label="启用" width="80">
                <template #default="{ row }">
                  <el-switch v-model="row.enabled" />
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- ===== Tab 4: 个税计算配置 ===== -->
      <el-tab-pane label="个税计算" name="tax">
        <div v-if="taxConfig" class="config-content">
          <el-card shadow="never" class="config-card">
            <template #header>
              <div class="card-header">
                <span>个税计算配置</span>
                <el-button type="primary" :icon="DocumentChecked" :loading="saving" @click="handleSaveTax">保存配置</el-button>
              </div>
            </template>
            <el-alert type="info" :closable="false" show-icon class="config-alert">
              <template #title>{{ taxConfig.calcDescription }}</template>
            </el-alert>
            <el-form label-width="160px" label-position="right">
              <el-row :gutter="20">
                <el-col :span="8">
                  <el-form-item label="起征点">
                    <el-input-number v-model="taxConfig.taxThreshold" :min="0" :step="500" />
                    <span class="form-suffix">元/月</span>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="社保公积金税前扣除">
                    <el-switch v-model="taxConfig.insurancePreTax" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="使用自定义税率表">
                    <el-switch v-model="taxConfig.customTaxBrackets" />
                    <span class="form-tip">关闭则使用法定标准</span>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item v-if="taxConfig.customTaxBrackets" label="自定义说明">
                <el-input v-model="taxConfig.customRemark" placeholder="如：2026年新政策调整后的税率表" />
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never" class="config-card">
            <template #header><span>个税税率表（七级超额累进）</span></template>
            <el-table :data="taxConfig.taxBrackets" border size="small">
              <el-table-column label="级数" prop="level" width="60" align="center" />
              <el-table-column label="应纳税所得额下限" width="160">
                <template #default="{ row }">
                  <el-input-number v-model="row.fromAmount" :min="0" :step="500" size="small" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="应纳税所得额上限" width="160">
                <template #default="{ row }">
                  <el-input-number v-model="row.toAmount" :min="0" :step="500" size="small" controls-position="right" placeholder="空=无上限" />
                </template>
              </el-table-column>
              <el-table-column label="税率(%)" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.rate" :min="0" :max="100" size="small" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="速算扣除数" width="160">
                <template #default="{ row }">
                  <el-input-number v-model="row.quickDeduction" :min="0" :step="10" size="small" controls-position="right" />
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <el-card shadow="never" class="config-card">
            <template #header><span>专项附加扣除配置</span></template>
            <el-table :data="taxConfig.specialDeductions" border size="small">
              <el-table-column label="扣除类型" width="180">
                <template #default="{ row }">
                  {{ getSpecialDeductionLabel(row.type) }}
                </template>
              </el-table-column>
              <el-table-column label="标准扣除金额" width="200">
                <template #default="{ row }">
                  <el-input-number v-model="row.standardAmount" :min="0" :step="100" size="small" controls-position="right" />
                  <span class="form-suffix">元/月</span>
                </template>
              </el-table-column>
              <el-table-column label="启用" width="80">
                <template #default="{ row }">
                  <el-switch v-model="row.enabled" />
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <!-- 预览计算 -->
          <el-card shadow="never" class="config-card">
            <template #header><span>个税计算预览</span></template>
            <el-form label-width="120px" label-position="right" inline>
              <el-form-item label="税前工资">
                <el-input-number v-model="taxPreview.grossSalary" :min="0" :step="500" size="small" />
              </el-form-item>
              <el-form-item label="社保公积金">
                <el-input-number v-model="taxPreview.insuranceDeduction" :min="0" :step="100" size="small" />
              </el-form-item>
              <el-form-item label="专项附加扣除">
                <el-input-number v-model="taxPreview.specialDeduction" :min="0" :step="100" size="small" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :icon="Search" size="small" @click="handlePreviewTax">计算</el-button>
              </el-form-item>
            </el-form>
            <el-alert v-if="taxPreview.detail" :title="`应缴个税：${taxPreview.result}元`" :description="taxPreview.detail" type="success" show-icon :closable="false" />
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped lang="scss">
.hr-config-center {
  .config-content {
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-4);
  }

  .config-card {
    :deep(.el-card__header) {
      padding: var(--fts-space-3) var(--fts-space-4);
      font-weight: 600;
    }

    :deep(.el-card__body) {
      padding: var(--fts-space-4);
    }
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .config-alert {
    margin-bottom: var(--fts-space-4);
  }

  .form-tip {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-color-secondary);
    margin-top: var(--fts-space-1);
  }

  .form-suffix {
    margin-left: var(--fts-space-2);
    color: var(--fts-text-color-secondary);
  }

  .text-muted {
    color: var(--fts-text-color-secondary);
  }
}
</style>
