<script setup lang="ts">
/**
 * 储值系统设置页面
 * 全局储值参数配置，包含充值限额、赠送规则、退款规则、风控、财务合规五大模块
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Wallet, Setting, Document, Bell, Money } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import { rechargeSettingsApi } from '@/api/marketing/recharge'
import { DefaultRechargeSettings, type RechargeSystemSettings } from '@/types/member-recharge'

// ==================== 页面状态 ====================
const settings = ref<RechargeSystemSettings>({ ...DefaultRechargeSettings })
const loading = ref(false)
const saving = ref(false)
/** 默认展开所有折叠面板 */
const activeNames = ref<string[]>(['limit', 'bonus', 'refund', 'risk', 'finance'])

// ==================== 选项常量 ====================
/** 赠送形式选项 */
const bonusTypeOptions = [
  { label: '余额', value: 'balance' }, { label: '优惠券', value: 'coupon' },
  { label: '积分', value: 'points' }, { label: '混合', value: 'mixed' },
] as const

/** 告警通知方式选项 */
const alertChannelOptions = [
  { label: '短信', value: 'sms' }, { label: '微信', value: 'wechat' },
  { label: '邮件', value: 'email' },
] as const

/** 财务系统类型选项 */
const financeSystemOptions = [
  { label: '未对接', value: 'none' }, { label: '金蝶', value: 'kingdee' },
  { label: '用友', value: 'yonyou' }, { label: '自定义', value: 'custom' },
] as const

/** 赠送处理方式选项 */
const bonusHandlingOptions = [
  { label: '清零', value: 'clear' }, { label: '按比例扣减', value: 'proportional' },
] as const

// ==================== 数据加载与保存 ====================
async function loadSettings() {
  loading.value = true
  try {
    settings.value = (await rechargeSettingsApi.get()) ?? { ...DefaultRechargeSettings }
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    await rechargeSettingsApi.update(settings.value)
    ElMessage.success('设置已保存')
  } finally {
    saving.value = false
  }
}

/** 更新协议版本号 */
async function handleUpdateVersion() {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的协议版本号', '更新协议版本', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: settings.value.currentAgreementVersion,
      inputPlaceholder: '如 v1.0.1',
    })
    if (value) {
      settings.value.currentAgreementVersion = value
      ElMessage.success('协议版本已更新，保存后生效')
    }
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  loadSettings()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="储值系统设置" description="全局储值参数配置，所有规则可随时调整">
      <el-button type="primary" :loading="saving" @click="handleSave">保存设置</el-button>
    </PageHeader>

    <div v-loading="loading" class="settings-container">
      <el-collapse v-model="activeNames">
        <!-- ==================== 充值限额配置 ==================== -->
        <el-collapse-item name="limit">
          <template #title>
            <div class="collapse-title">
              <el-icon><Wallet /></el-icon>
              <span>充值限额配置</span>
            </div>
          </template>
          <el-form label-width="160px" class="settings-form">
            <el-form-item label="单次充值上限">
              <el-input-number
                v-model="settings.singleRechargeLimit"
                :min="100"
                :step="100"
                controls-position="right"
              />
              <span class="form-unit">元</span>
            </el-form-item>
            <el-form-item label="单日累计充值上限">
              <el-input-number
                v-model="settings.dailyRechargeLimit"
                :min="0"
                :step="500"
                controls-position="right"
              />
              <span class="form-unit">元</span>
            </el-form-item>
            <el-form-item label="单月累计充值上限">
              <el-input-number
                v-model="settings.monthlyRechargeLimit"
                :min="0"
                :step="1000"
                controls-position="right"
              />
              <span class="form-unit">元</span>
            </el-form-item>
            <el-form-item label="单卡余额上限">
              <el-input-number
                v-model="settings.maxBalanceLimit"
                :min="0"
                :max="5000"
                controls-position="right"
              />
              <span class="form-unit">元</span>
              <span class="warning-text">法规要求不超过5000元</span>
            </el-form-item>
          </el-form>
          <div class="section-help">
            根据《单用途商业预付卡管理办法》，记名卡余额上限不超过5000元
          </div>
        </el-collapse-item>

        <!-- ==================== 赠送规则配置 ==================== -->
        <el-collapse-item name="bonus">
          <template #title>
            <div class="collapse-title">
              <el-icon><Setting /></el-icon>
              <span>赠送规则配置</span>
            </div>
          </template>
          <el-form label-width="160px" class="settings-form">
            <el-form-item label="赠送比例硬上限">
              <el-input-number
                v-model="settings.maxBonusRate"
                :min="0"
                :max="30"
                controls-position="right"
              />
              <span class="form-unit">%</span>
              <span class="warning-text">建议不超过20%</span>
            </el-form-item>
            <el-form-item label="赠送有效期默认天数">
              <el-input-number
                v-model="settings.defaultBonusValidityDays"
                :min="1"
                :step="30"
                controls-position="right"
              />
              <span class="form-unit">天</span>
            </el-form-item>
            <el-form-item label="赠送形式限制">
              <el-checkbox-group v-model="settings.allowedBonusTypes">
                <el-checkbox
                  v-for="opt in bonusTypeOptions"
                  :key="opt.value"
                  :value="opt.value"
                >
                  {{ opt.label }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-form>
          <div class="section-help">
            赠送比例超过20%可能被认定为高额返利，存在合规风险
          </div>
        </el-collapse-item>

        <!-- ==================== 退款规则配置 ==================== -->
        <el-collapse-item name="refund">
          <template #title>
            <div class="collapse-title">
              <el-icon><Money /></el-icon>
              <span>退款规则配置</span>
            </div>
          </template>
          <el-form label-width="160px" class="settings-form">
            <el-form-item label="免审批阈值">
              <el-input-number
                v-model="settings.noApprovalThreshold"
                :min="0"
                controls-position="right"
              />
              <span class="form-unit">元</span>
              <span class="form-help">低于此金额的退款无需审批</span>
            </el-form-item>
            <el-form-item label="店长审批阈值">
              <el-input-number
                v-model="settings.managerApprovalThreshold"
                :min="0"
                controls-position="right"
              />
              <span class="form-unit">元</span>
              <span class="form-help">高于免审批且低于此值需店长审批</span>
            </el-form-item>
            <el-form-item label="退款手续费比例">
              <el-input-number
                v-model="settings.refundFeeRate"
                :min="0"
                :max="10"
                controls-position="right"
              />
              <span class="form-unit">%</span>
            </el-form-item>
            <el-form-item label="充值后冷却期">
              <el-input-number
                v-model="settings.refundCooldownHours"
                :min="0"
                :step="6"
                controls-position="right"
              />
              <span class="form-unit">小时</span>
              <span class="form-help">充值后此时间内不可退款</span>
            </el-form-item>
            <el-form-item label="赠送处理方式">
              <el-radio-group v-model="settings.bonusHandlingOnRefund">
                <el-radio
                  v-for="opt in bonusHandlingOptions"
                  :key="opt.value"
                  :value="opt.value"
                >
                  {{ opt.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </el-collapse-item>

        <!-- ==================== 风控配置 ==================== -->
        <el-collapse-item name="risk">
          <template #title>
            <div class="collapse-title">
              <el-icon><Bell /></el-icon>
              <span>风控配置</span>
            </div>
          </template>
          <el-form label-width="160px" class="settings-form">
            <el-form-item label="风控总开关">
              <el-switch v-model="settings.riskControlEnabled" />
            </el-form-item>
            <el-form-item label="异常交易告警">
              <el-switch v-model="settings.anomalyAlertEnabled" />
            </el-form-item>
            <el-form-item label="告警通知方式">
              <el-checkbox-group v-model="settings.alertChannels">
                <el-checkbox
                  v-for="opt in alertChannelOptions"
                  :key="opt.value"
                  :value="opt.value"
                >
                  {{ opt.label }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-form>
          <div class="section-help">
            异常交易类型包括：频繁充值、充值后快速消费、高退款率、新会员高额充值、多设备充值、超限额交易等
          </div>
        </el-collapse-item>

        <!-- ==================== 财务与合规 ==================== -->
        <el-collapse-item name="finance">
          <template #title>
            <div class="collapse-title">
              <el-icon><Document /></el-icon>
              <span>财务与合规</span>
            </div>
          </template>

          <!-- 协议管理 -->
          <div class="sub-section">
            <div class="sub-section__title">协议管理</div>
            <el-form label-width="160px" class="settings-form">
              <el-form-item label="当前协议版本号">
                <el-input v-model="settings.currentAgreementVersion" readonly style="width: 200px" />
                <el-button link type="primary" @click="handleUpdateVersion">更新版本</el-button>
              </el-form-item>
              <el-form-item label="协议变更需重新确认">
                <el-switch v-model="settings.agreementReconfirmOnChange" />
                <span class="form-help">开启后协议变更时老用户需重新确认</span>
              </el-form-item>
            </el-form>
          </div>

          <!-- 财务系统对接 -->
          <div class="sub-section">
            <div class="sub-section__title">财务系统对接</div>
            <el-form label-width="160px" class="settings-form">
              <el-form-item label="财务系统对接">
                <el-switch v-model="settings.financeIntegrationEnabled" />
              </el-form-item>
              <el-form-item v-if="settings.financeIntegrationEnabled" label="财务系统类型">
                <el-select v-model="settings.financeSystemType" placeholder="请选择财务系统" style="width: 200px">
                  <el-option
                    v-for="opt in financeSystemOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item v-if="settings.financeIntegrationEnabled" label="自动生成凭证">
                <el-switch v-model="settings.autoVoucherGeneration" />
              </el-form-item>
            </el-form>
          </div>

          <!-- 资金存管（预留） -->
          <div class="sub-section">
            <div class="sub-section__title">
              资金存管
              <span class="sub-section__tag">预留</span>
            </div>
            <el-form label-width="160px" class="settings-form">
              <el-form-item label="资金存管">
                <el-switch v-model="settings.fundCustodyEnabled" />
                <span class="form-help">规模扩大后可开启银行存管</span>
              </el-form-item>
              <template v-if="settings.fundCustodyEnabled">
                <el-form-item label="存管银行">
                  <el-input v-model="settings.custodyBank" placeholder="请输入存管银行名称" style="width: 240px" />
                </el-form-item>
                <el-form-item label="存管比例">
                  <el-input-number v-model="settings.custodyRate" :min="0" :max="100" controls-position="right" />
                  <span class="form-unit">%</span>
                </el-form-item>
                <el-form-item label="存管账户">
                  <el-input v-model="settings.custodyAccount" placeholder="请输入存管账户" style="width: 240px" />
                </el-form-item>
              </template>
            </el-form>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<style scoped lang="scss">
.settings-container {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: 0 0 var(--fts-page-radius) var(--fts-page-radius);
  padding: var(--fts-space-4) var(--fts-space-6);
}
.collapse-title {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-md);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
}
.settings-form { padding: var(--fts-space-2) 0; }
.form-unit {
  margin-left: var(--fts-space-2);
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
}
.form-help,
.warning-text {
  margin-left: var(--fts-space-3);
  font-size: var(--fts-font-size-xs);
}
.form-help { color: var(--fts-text-tertiary); }
.warning-text { color: var(--fts-warning); }
.section-help {
  margin-top: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
  color: var(--fts-text-tertiary);
  font-size: var(--fts-font-size-xs);
  line-height: var(--fts-line-height-normal);
}
.sub-section {
  padding: var(--fts-space-4) 0;
  border-bottom: 1px solid var(--fts-border-secondary);
  &:last-child { border-bottom: none; }
  &__title {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
  }
  &__tag {
    padding: 2px var(--fts-space-2);
    background: var(--fts-bg-secondary);
    border-radius: var(--fts-radius-sm);
    color: var(--fts-text-tertiary);
    font-size: var(--fts-font-size-xs);
    font-weight: var(--fts-font-weight-normal);
  }
}
</style>
