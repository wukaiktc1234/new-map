/**
 * 合同智能分析逻辑
 * 提供到期提醒、续签推荐、风险识别等能力
 */
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { contractIntelligenceApi } from '@/api/contract-intelligence'
import type { ExpiringContract, ContractRisk, ExpiryStats, RenewalStats, RiskLevel, RenewRecommendation } from '@/types/contract-intelligence'
import { RenewRecommendationLabelMap, RiskLevelLabelMap } from '@/types/contract-intelligence'

export function useContractAnalysis() {
  /* ===== 数据状态 ===== */
  const analysisLoading = ref(false)
  const analysisTab = ref('expiry')
  const expiringContracts = ref<ExpiringContract[]>([])
  const contractRisks = ref<ContractRisk[]>([])
  const expiryStats = ref<ExpiryStats>({ expired: 0, urgent: 0, warning: 0, notice: 0 })
  const renewalStats = ref<RenewalStats>({ total: 0, strong: 0, normal: 0, cautious: 0, notRecommended: 0 })

  /* ===== 过滤 ===== */
  const expiryFilter = ref<'all' | 'expired' | 'urgent' | 'warning' | 'notice'>('all')
  const riskFilter = ref<'all' | RiskLevel>('all')

  const filteredExpiring = computed(() => {
    if (expiryFilter.value === 'all') return expiringContracts.value
    return expiringContracts.value.filter(c => c.alertLevel === expiryFilter.value)
  })

  const filteredRisks = computed(() => {
    if (riskFilter.value === 'all') return contractRisks.value
    return contractRisks.value.filter(r => r.riskLevel === riskFilter.value)
  })

  /** 智能分析统计卡片 */
  const analysisStats = computed(() => [
    { icon: 'AlarmClock', label: '已过期', value: expiryStats.value.expired, colorType: 'error' as const },
    { icon: 'Warning', label: '7天内到期', value: expiryStats.value.urgent, colorType: 'warning' as const },
    { icon: 'Calendar', label: '30天内到期', value: expiryStats.value.warning, colorType: 'primary' as const },
    { icon: 'Bell', label: '90天内到期', value: expiryStats.value.notice, colorType: 'info' as const },
  ])

  /** 续签推荐分布 */
  const renewalDistribution = computed(() => [
    { label: '强烈推荐', value: renewalStats.value.strong, colorType: 'success' as const },
    { label: '建议续签', value: renewalStats.value.normal, colorType: 'primary' as const },
    { label: '谨慎续签', value: renewalStats.value.cautious, colorType: 'warning' as const },
    { label: '不建议', value: renewalStats.value.notRecommended, colorType: 'error' as const },
  ])

  /* ===== 加载数据 ===== */
  async function loadAnalysisData() {
    analysisLoading.value = true
    try {
      const [expiring, risks, eStats, rStats] = await Promise.all([
        contractIntelligenceApi.getExpiringContracts(90),
        contractIntelligenceApi.getContractRisks(),
        contractIntelligenceApi.getExpiryStats(),
        contractIntelligenceApi.getRenewalStats(),
      ])
      expiringContracts.value = expiring
      contractRisks.value = risks
      expiryStats.value = eStats
      renewalStats.value = rStats
    } catch (error: unknown) {
      if (error instanceof Error) ElMessage.error(error.message || '加载分析数据失败')
    } finally {
      analysisLoading.value = false
    }
  }

  /* ===== 工具函数 ===== */
  function getAlertLevelStatus(level: string): string {
    return { expired: 'error', urgent: 'warning', warning: 'primary', notice: 'info' }[level] || 'info'
  }

  function getAlertLevelLabel(level: string): string {
    return { expired: '已过期', urgent: '紧急', warning: '预警', notice: '提醒' }[level] || level
  }

  function getRecommendationStatus(rec: RenewRecommendation): string {
    return { strong: 'success', normal: 'primary', cautious: 'warning', not_recommended: 'error' }[rec] || 'info'
  }

  function getRiskLevelStatus(level: RiskLevel): string {
    return { high: 'error', medium: 'warning', low: 'info' }[level] || 'info'
  }

  function getRiskTypeLabel(type: string): string {
    return {
      expired_unsigned: '过期未签', salary_anomaly: '薪资异常',
      missing_contract: '未签合同', type_mismatch: '类型不符', long_term_temp: '长期临时',
    }[type] || type
  }

  function formatRemainingDays(days: number): string {
    if (days < 0) return `已过期${Math.abs(days)}天`
    if (days === 0) return '今日到期'
    return `剩余${days}天`
  }

  return {
    analysisLoading,
    analysisTab,
    expiringContracts,
    contractRisks,
    expiryStats,
    renewalStats,
    expiryFilter,
    riskFilter,
    filteredExpiring,
    filteredRisks,
    analysisStats,
    renewalDistribution,
    loadAnalysisData,
    // 工具函数
    getAlertLevelStatus,
    getAlertLevelLabel,
    getRecommendationStatus,
    getRiskLevelStatus,
    getRiskTypeLabel,
    formatRemainingDays,
    // 常量
    RenewRecommendationLabelMap,
    RiskLevelLabelMap,
  }
}
