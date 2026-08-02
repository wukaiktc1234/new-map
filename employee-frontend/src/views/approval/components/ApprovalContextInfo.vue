<script setup lang="ts">
/**
 * ApprovalContextInfo - 审批参考信息区域
 * 根据审批类型动态展示：HR风控预警、假期余额、费用明细、加班统计、换班确认、出差政策、领用库存等
 */
import PolicyStandardCard from '@/components/business/PolicyStandardCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import type { ContextData, RiskWarning } from '@/types/approval'

defineProps<{
  contextData: ContextData
}>()
</script>

<template>
  <section class="detail-section context-section">
    <h3 class="ds-title ctx-header">
      <svg class="ctx-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
      审批参考信息
    </h3>

    <!-- ====== HR风控预警（最高优先级展示） ====== -->
    <div
      v-if="contextData.riskWarning"
      class="hr-risk-warning"
      :class="[`hr-risk-warning--${contextData.riskWarning.level}`]"
    >
      <div class="hrw-header">
        <div class="hrw-level-badge" :class="[`hrw-level-badge--${contextData.riskWarning.level}`]">
          <template v-if="contextData.riskWarning.level === 'prohibited'">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 019.9-1"/></svg>
            HR禁止性建议
          </template>
          <template v-else-if="contextData.riskWarning.level === 'danger'">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            健康风险警告
          </template>
          <template v-else-if="contextData.riskWarning.level === 'warning'">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            注意事项
          </template>
          <template v-else>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
            HR建议
          </template>
        </div>
        <span class="hrw-source">
          {{ contextData.riskWarning.source === 'hr_system_auto' ? 'HR系统自动检测' : contextData.riskWarning.source === 'hr_manual_review' ? 'HR人工审核' : '政策引擎' }}
          · {{ contextData.riskWarning.generatedAt?.slice(5, 16).replace('T', ' ') }}
        </span>
      </div>

      <div class="hrw-title">{{ contextData.riskWarning.title }}</div>

      <div class="hrw-body">
        <pre class="hrw-description">{{ contextData.riskWarning.description }}</pre>
      </div>

      <div class="hrw-evidence">
        <span class="hrw-evidence-label">数据依据</span>
        <span class="hrw-evidence-value">{{ contextData.riskWarning.evidence }}</span>
      </div>

      <div class="hrw-suggestion">
        <div class="hrw-suggestion-label">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          HR建议操作
        </div>
        <p class="hrw-suggestion-text">{{ contextData.riskWarning.suggestion }}</p>
      </div>

      <div v-if="contextData.riskWarning.regulationRef" class="hrw-regulation">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
        {{ contextData.riskWarning.regulationRef }}
      </div>
    </div>

    <!-- 请假类型：假期余额 + 年度统计 -->
    <template v-if="contextData.type === 'leave'">
      <div class="ctx-grid ctx-grid--2">
        <div class="ctx-card leave-balance-card">
          <div class="ctx-card__label">年假余额</div>
          <div class="ctx-card__headline">
            <span class="ctx-card__value leave-balance-num">{{ contextData.data.annualRemaining }}</span>
            <span class="ctx-card__unit">天 / 总{{ contextData.data.annualTotal }}天</span>
          </div>
          <div class="ctx-progress">
            <div class="ctx-progress__bar" :style="{ width: `${(contextData.data.annualUsed / contextData.data.annualTotal) * 100}%` }"></div>
          </div>
          <div class="ctx-card__sub">已用 {{ contextData.data.annualUsed }} 天</div>
        </div>
        <PolicyStandardCard
          title="本年度统计"
          :columns="2"
          :items="[
            { label: '请假次数', value: String(contextData.data.yearLeaveCount) },
            ...(contextData.data.sickBalance !== undefined ? [{ label: '病假余额', value: `${contextData.data.sickBalance}天` }] : []),
            ...(contextData.data.personalBalance !== undefined ? [{ label: '事假余额', value: `${contextData.data.personalBalance}天` }] : []),
            ...(contextData.data.compensatoryBalance !== undefined ? [{ label: '调休余额', value: `${contextData.data.compensatoryBalance}时` }] : []),
          ]"
          :summary="{
            prefix: '本月已请',
            value: contextData.data.monthLeaveDays || 0,
            suffix: '天',
          }"
        />
      </div>
      <div v-if="contextData.data.consecutiveDaysWarning" class="ctx-alert ctx-alert--warning">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
        本次连续请假超过3天，请注意工作交接安排
      </div>
      <div v-if="contextData.data.leaveTrend?.length" class="ctx-trend">
        <div class="ctx-trend__title">近期请假记录</div>
        <div class="ctx-trend__list">
          <div v-for="(item, ti) in contextData.data.leaveTrend" :key="ti" class="ctx-trend__item">
            <span class="ctx-trend__month">{{ item.month }}</span>
            <span class="ctx-trend__days">{{ item.days }}天</span>
            <span class="ctx-trend__type">{{ item.type }}</span>
          </div>
        </div>
      </div>

    <!-- 报销类型：费用明细 + 预算使用率 + 关联出差 -->
    </template>
    <template v-else-if="contextData.type === 'reimbursement'">
      <div v-if="contextData.data.relatedTravel" class="ctx-linked-task">
        <div class="clt-badge">关联出差任务</div>
        <div class="clt-info">
          <span class="clt-title">{{ contextData.data.relatedTravel.title }}</span>
          <span class="clt-meta">{{ contextData.data.relatedTravel.source }} · {{ contextData.data.relatedTravel.issueDate.slice(5,10) }} 指派 · {{ contextData.data.relatedTravel.issuerName }}</span>
        </div>
        <StatusTag :status="contextData.data.relatedTravel.status === 'completed' ? 'active' : 'processing'" size="small" />
      </div>
      <div class="ctx-expense-table">
        <div class="cet-header">
          <span>费用明细（共 {{ contextData.data.items.length }} 项）</span>
          <span class="cet-total">合计: {{ contextData.data.items.reduce((s, i) => s + parseFloat(i.amount.replace(/[¥,]/g, '')), 0).toFixed(2) }} 元</span>
        </div>
        <div class="cet-list">
          <div v-for="(item, ei) in contextData.data.items" :key="ei" class="cet-row">
            <span class="cet-cat">{{ item.category }}</span>
            <span class="cet-desc">{{ item.description }}</span>
            <span class="cet-amount">{{ item.amount }}</span>
          </div>
        </div>
      </div>
      <!-- 报销类型：预算概况 -->
      <PolicyStandardCard
        title="费用概况"
        :columns="2"
        :items="[
          {
            label: '本月累计',
            value: contextData.data.monthlyTotal,
            highlight: contextData.data.monthlyUsagePercent > 80,
            tag: contextData.data.monthlyUsagePercent > 80 ? { status: 'warning', label: `${contextData.data.monthlyUsagePercent}%` } : undefined,
          },
          { label: '月度预算', value: contextData.data.monthlyBudget },
          { label: '本年度累计', value: contextData.data.yearTotal },
          { label: '年度预算', value: contextData.data.yearBudget },
          { label: '票据数量', value: `${contextData.data.receiptCount}张` },
        ]"
      />
      <div v-if="contextData.data.policyNotes" class="ctx-policy-note">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
        {{ contextData.data.policyNotes }}
      </div>
    </template>

    <!-- 加班类型：政策标准 + 补偿方式 + 近期记录 -->
    <template v-else-if="contextData.type === 'overtime'">
      <PolicyStandardCard
        title="加班统计"
        :columns="3"
        :items="[
          {
            label: '本月加班',
            value: `${contextData.data.monthHours}h`,
            highlight: contextData.data.monthUsagePercent > 80,
          },
          { label: '本季度累计', value: `${contextData.data.quarterHours}h` },
          { label: '部门平均', value: `${contextData.data.deptAvgHours}h` },
        ]"
      />
      <div class="ctx-compensate-type">
        <span class="cct-label">补偿方式：</span>
        <StatusTag :status="contextData.data.compensateType === '调休' ? 'active' : contextData.data.compensateType === '加班费' ? 'success' : 'processing'" :label="contextData.data.compensateType" size="small" />
        <span v-if="contextData.data.compensatoryBalance !== undefined" class="cct-balance">调休余额: {{ contextData.data.compensatoryBalance }}小时</span>
      </div>
      <div v-if="contextData.data.recentRecords?.length" class="ctx-recent-list">
        <div class="crl-title">近期加班记录</div>
        <div class="crl-items">
          <div v-for="(rec, ri) in contextData.data.recentRecords" :key="ri" class="crl-item">
            <span class="crl-date">{{ rec.date }}</span>
            <span class="crl-hours">{{ rec.hours }}h</span>
            <span class="crl-type">{{ rec.type }}</span>
          </div>
        </div>
      </div>
    </template>

    <!-- 换班类型：确认状态 + 排班覆盖方案 -->
    <template v-else-if="contextData.type === 'swap'">
      <div class="ctx-swap-status" :class="{ 'ctx-swap-status--confirmed': contextData.data.partnerConfirmed }">
        <div class="css-icon">
          <svg v-if="contextData.data.partnerConfirmed" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6L9 17l-5-5"/></svg>
          <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        </div>
        <div class="css-text">
          <span class="css-main">{{ contextData.data.partnerConfirmed ? '换班对象已确认' : '等待换班对象确认' }}</span>
          <span class="css-sub">{{ contextData.data.partnerName }}{{ contextData.data.partnerConfirmed ? ` · 确认于 ${contextData.data.confirmTime}` : '' }}</span>
        </div>
      </div>
      <div class="ctx-swap-detail">
        <div class="csd-row">
          <div class="csd-shift">
            <span class="csd-label">原班次</span>
            <span class="csd-value csd-value--out">{{ contextData.data.originalShiftDetail?.date || '--' }} {{ contextData.data.originalShiftDetail?.time || '' }}</span>
            <span v-if="contextData.data.originalShiftDetail?.position" class="csd-pos">{{ contextData.data.originalShiftDetail.position }}</span>
          </div>
          <div class="csd-arrow">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
          </div>
          <div class="csd-shift">
            <span class="csd-label">目标班次</span>
            <span class="csd-value csd-value--in">{{ contextData.data.targetShiftDetail?.date || '--' }} {{ contextData.data.targetShiftDetail?.time || '' }}</span>
            <span v-if="contextData.data.targetShiftDetail?.position" class="csd-pos">{{ contextData.data.targetShiftDetail.position }}</span>
          </div>
        </div>
      </div>
      <div class="ctx-coverage-plan">
        <div class="ccp-label">排班覆盖方案</div>
        <div class="ccp-content">{{ contextData.data.coveragePlan }}</div>
      </div>
      <PolicyStandardCard
        title="换班统计"
        :columns="2"
        :items="[
          {
            label: '近30天换班',
            value: `${contextData.data.recentSwapCount} / ${contextData.data.swapLimit} 次`,
            highlight: contextData.data.recentSwapCount >= contextData.data.swapLimit - 1,
            tag: contextData.data.recentSwapCount >= contextData.data.swapLimit - 1 ? { status: 'error', label: '接近上限' } : undefined,
          },
        ]"
      />
    </template>

    <!-- 出差类型：关联任务指令 + 政策标准 -->
    <template v-else-if="contextData.type === 'travel'">
      <div v-if="contextData.data.relatedTask" class="ctx-linked-task ctx-linked-task--travel">
        <div class="clt-badge clt-badge--task">关联任务指令</div>
        <div class="clt-body">
          <div class="clt-title-row">
            <span class="clt-title">{{ contextData.data.relatedTask.title }}</span>
            <StatusTag :status="contextData.data.relatedTask.status === 'active' ? 'processing' : 'active'" :label="contextData.data.relatedTask.status === 'active' ? '执行中' : '已完成'" size="small" />
          </div>
          <div class="clt-meta-row">
            <span>{{ contextData.data.relatedTask.source }}</span>
            <span>指派人: {{ contextData.data.relatedTask.issuerName }}</span>
            <span>{{ contextData.data.relatedTask.issueDate.slice(5, 10) }}</span>
            <span class="clt-id">编号: {{ contextData.data.relatedTask.id }}</span>
          </div>
        </div>
      </div>
      <PolicyStandardCard
        title="差旅政策标准"
        :items="[
          { label: '政策限额', value: contextData.data.policyLimit },
          { label: '预估费用', value: contextData.data.estimatedActual },
          { label: '住宿标准', value: contextData.data.hotelStandard },
          { label: '交通标准', value: contextData.data.transportAllowance },
          { label: '日补标准', value: contextData.data.dailyAllowance },
          { label: '预支金额', value: contextData.data.advancePayment },
        ]"
        :summary="{
          prefix: '本年度已出差',
          value: contextData.data.yearTripCount,
          suffix: '次',
          children: contextData.data.similarTrips?.map(t => ({ text: `${t.destination} (${t.dates}) · ${t.amount}` })),
        }"
      />
    </template>

    <!-- 领用类型：库存与预算概况 -->
    <template v-else-if="contextData.type === 'requisition'">
      <PolicyStandardCard
        title="库存与预算"
        :columns="2"
        :items="[
          {
            label: '当前库存',
            value: `${contextData.data.stockQuantity}${contextData.data.stockUnit}`,
            highlight: contextData.data.stockQuantity < 10,
          },
          { label: '月均用量', value: `${contextData.data.monthlyAvg}${contextData.data.stockUnit}` },
          { label: '部门预算', value: contextData.data.departmentBudget },
          { label: '已使用', value: contextData.data.departmentUsed },
          {
            label: '到货周期',
            value: `${contextData.data.leadTimeDays}天`,
            tag: contextData.data.leadTimeDays > 7 ? { status: 'warning', label: '较长' } : undefined,
          },
        ]"
      />
        <div v-if="contextData.data.supplierInfo" class="ctx-supplier">
          <span class="cs-label">供应商:</span>
          <span class="cs-name">{{ contextData.data.supplierInfo }}</span>
        </div>
        <div v-if="contextData.data.stockQuantity < contextData.data.monthlyAvg * 2" class="ctx-alert ctx-alert--warning">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          库存低于2个月均用量，建议及时补充
        </div>
    </template>
  </section>
</template>

<style scoped lang="scss">
/* ================================================================
 * ApprovalContextInfo 样式 - 审批参考信息区域
 *
 * 包含：
 *   Part 1 - 通用详情区样式（复用 detail-section）
 *   Part 2 - 审批参考信息区域（ctx-*）
 *   Part 3 - HR风控预警样式（hrw-*）
 * ================================================================ */

// ========== 通用详情区 ==========
.detail-section {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.ds-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-3);
  padding-bottom: var(--fts-space-2);
  border-bottom: 1px solid var(--fts-border-secondary);
}

// ========== 审批参考信息区域样式 ==========
.context-section {
  border-left: 3px solid var(--fts-primary);
}

.ctx-header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  color: var(--fts-primary);
}

.ctx-icon {
  flex-shrink: 0;
  opacity: 0.8;
}

// 网格布局
.ctx-grid {
  display: grid;
  gap: var(--fts-space-3);

  &--2 { grid-template-columns: 1fr 1fr; }
  &--3 { grid-template-columns: repeat(3, 1fr); }

  @media (max-width: 480px) {
    &--2, &--3 { grid-template-columns: 1fr; }
  }
}

// 请假 - 年假余额卡片
.ctx-card {
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);

  &__label {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  &__value {
    font-size: 36px;
    font-weight: 700;
    line-height: 1.1;
    color: var(--fts-text-primary);
  }

  &__unit {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }

  &__sub {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-quaternary);
  }
}

.leave-balance-card {
  background: linear-gradient(135deg, rgba(var(--fts-warning-rgb), 0.08), rgba(var(--fts-warning-rgb), 0.03));
  border: 1px solid rgba(var(--fts-warning-rgb), 0.15);
}

.leave-balance-num {
  color: var(--fts-warning);
}

// 数值+单位同行显示（用于年假余额等需要同行展示的卡片）
.ctx-card__headline {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: var(--fts-space-2);
}

// 进度条
.ctx-progress {
  height: 6px;
  background: var(--fts-bg-tertiary);
  border-radius: 3px;
  overflow: hidden;

  &__bar {
    height: 100%;
    background: var(--fts-warning);
    border-radius: 3px;
    transition: width 0.5s ease;
  }
}

// 统计数据
.ctx-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--fts-space-3);
}

.ctx-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;

  &__num {
    font-size: 20px;
    font-weight: 700;
    color: var(--fts-text-primary);
  }

  &__label {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    text-align: center;
  }
}

// 预警提示
.ctx-alert {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-4);
  border-radius: var(--fts-radius-sm);
  font-size: var(--fts-font-size-sm);
  margin-top: var(--fts-space-3);

  &--warning {
    background: rgba(var(--fts-warning-rgb), 0.06);
    color: var(--fts-warning);
    border: 1px solid rgba(var(--fts-warning-rgb), 0.15);
  }

  svg { flex-shrink: 0; }
}

// 请假趋势
.ctx-trend {
  margin-top: var(--fts-space-3);
  padding-top: var(--fts-space-3);
  border-top: 1px solid var(--fts-border-secondary);

  &__title {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    margin-bottom: var(--fts-space-2);
  }

  &__list {
    display: flex;
    flex-wrap: wrap;
    gap: var(--fts-space-2);
  }

  &__item {
    display: inline-flex;
    align-items: center;
    gap: var(--fts-space-2);
    padding: 4px var(--fts-space-3);
    background: var(--fts-bg-secondary);
    border-radius: var(--fts-radius-sm);
    font-size: var(--fts-font-size-xs);
  }

  &__month { color: var(--fts-text-secondary); font-weight: 600; }
  &__days { color: var(--fts-primary); font-weight: 600; }
  &__type { color: var(--fts-text-tertiary); }
}

// 关联任务/出差
.ctx-linked-task {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: linear-gradient(135deg, rgba(var(--fts-primary-rgb), 0.05), rgba(var(--fts-success-rgb), 0.03));
  border: 1px solid rgba(var(--fts-primary-rgb), 0.12);
  border-radius: var(--fts-radius-md);
  margin-bottom: var(--fts-space-3);

  &--travel {
    flex-direction: column;
    align-items: stretch;
    gap: var(--fts-space-2);
  }

  .clt-badge {
    padding: 2px 8px;
    border-radius: var(--fts-radius-xs);
    font-size: var(--fts-font-size-xs);
    font-weight: 600;
    white-space: nowrap;
    background: var(--fts-primary);
    color: var(--fts-text-on-primary);

    &--task {
      background: var(--fts-success);
      color: var(--fts-text-on-primary);
    }
  }

  .clt-info, .clt-body {
    flex: 1;
    min-width: 0;
  }

  .clt-title {
    display: block;
    font-size: var(--fts-font-size-sm);
    font-weight: 600;
    color: var(--fts-text-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .clt-meta {
    display: block;
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-secondary);
    margin-top: 2px;
  }

  .clt-title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--fts-space-2);
  }

  .clt-meta-row {
    display: flex;
    flex-wrap: wrap;
    gap: var(--fts-space-3);
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-secondary);
  }

  .clt-id {
    font-family: monospace;
    color: var(--fts-text-tertiary);
  }
}

// 报销费用明细表
.ctx-expense-table {
  margin-bottom: var(--fts-space-3);

  .cet-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: var(--fts-font-size-sm);
    font-weight: 600;
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-2);
  }

  .cet-total {
    color: var(--fts-error);
    font-weight: 700;
  }

  .cet-list {
    display: flex;
    flex-direction: column;
    border: 1px solid var(--fts-border-secondary);
    border-radius: var(--fts-radius-sm);
    overflow: hidden;
  }

  .cet-row {
    display: grid;
    grid-template-columns: 80px 1fr 90px;
    gap: var(--fts-space-2);
    padding: var(--fts-space-2) var(--fts-space-3);
    align-items: center;
    font-size: var(--fts-font-size-xs);
    background: var(--fts-bg-card);

    + .cet-row { border-top: 1px solid var(--fts-border-secondary); }

    &:hover { background: var(--fts-bg-secondary); }
  }

  .cet-cat {
    font-weight: 600;
    color: var(--fts-primary);
    white-space: nowrap;
  }

  .cet-desc {
    color: var(--fts-text-secondary);
    word-break: break-word;
  }

  .cet-amount {
    font-weight: 600;
    color: var(--fts-error);
    text-align: right;
    white-space: nowrap;
  }
}

// 迷你统计卡片（报销/加班/领用共用）
.ctx-mini-card {
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-3) var(--fts-space-4);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cmc-label {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  font-weight: 500;
}

.cmc-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--fts-text-primary);
  line-height: 1.2;

  small {
    font-size: var(--fts-font-size-xs);
    font-weight: 400;
    color: var(--fts-text-tertiary);
  }

  &--num { color: var(--fts-primary); }
  &--warn { color: var(--fts-warning); }
}

.cmc-sub {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);
}

.cmc-bar {
  height: 4px;
  background: var(--fts-bg-tertiary);
  border-radius: 2px;
  overflow: hidden;
  margin-top: 4px;

  &__fill {
    height: 100%;
    background: var(--fts-primary);
    border-radius: 2px;
    transition: width 0.5s ease;

    &--warn { background: var(--fts-warning); }
  }
}

// ============================================================
//  宽版布局：ctx-wide-row（报销/出差等金额较长数据）
// ============================================================
.ctx-wide-row {
  display: flex;
  align-items: stretch;
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-3) var(--fts-space-4);
}

// 左侧主区 — 占比适中，含进度条
.cwr-main {
  flex: 1.15;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 0;
  min-width: 0;
}

// 右侧副区 — 上下两行堆叠
.cwr-side {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 0;
  min-width: 0;

  &__row {
    display: flex;
    flex-direction: column;
    gap: 0;
  }

  // 右侧内部行分隔线
  &__sep {
    height: 1px;
    background: var(--fts-border-secondary);
    margin: var(--fts-space-2) 0;
    flex-shrink: 0;
  }
}

// 宽版分隔符（竖线，全高）
.cwr-divider {
  width: 1px;
  background: var(--fts-border-secondary);
  align-self: stretch;
  margin: 0 var(--fts-space-3);
  flex-shrink: 0;
}

// 政策备注
.ctx-policy-note {
  display: flex;
  align-items: flex-start;
  gap: var(--fts-space-2);
  margin-top: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: rgba(var(--fts-info-rgb), 0.04);
  border: 1px solid rgba(var(--fts-info-rgb), 0.10);
  border-radius: var(--fts-radius-sm);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  line-height: 1.6;

  svg { flex-shrink: 0; margin-top: 2px; color: var(--fts-text-tertiary); }
}

// ============================================================
//  统一紧凑布局：ctx-compact-row（报销/加班/领用 共用）
// ============================================================
.ctx-compact-row {
  display: flex;
  align-items: stretch;
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-2) var(--fts-space-3);
  overflow: hidden;
}

// 单个格子 — 纵向列布局
.cc-cell {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 0;
  min-width: 0;
  overflow: hidden;

  // 含进度条的格子
  &--bar { padding-bottom: 2px; }
}

.cc-label {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  font-weight: 500;
  line-height: 1.2;
  white-space: nowrap;
}

.cc-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--fts-text-primary);
  line-height: 1.2;
  white-space: nowrap;

  small {
    font-size: var(--fts-font-size-xs);
    font-weight: 400;
    color: var(--fts-text-tertiary);
    margin-left: 1px;
  }

  &--warn { color: var(--fts-warning); }
  &--muted { color: var(--fts-primary); font-size: 16px; font-weight: 600; }
}

// 副文本 — 次要信息，允许截断
.cc-sub {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  font-weight: 500;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

// 竖线分隔符
.cc-divider {
  width: 1px;
  background: var(--fts-border-secondary);
  align-self: stretch;
  margin: var(--fts-space-2) var(--fts-space-2);
  flex-shrink: 0;
}

// 进度条（仅在 cc-cell--bar 内出现）
.cc-bar {
  width: 100%;
  height: 4px;
  background: var(--fts-bg-tertiary);
  border-radius: 2px;
  overflow: hidden;
  margin-top: var(--fts-space-1);

  &__fill {
    height: 100%;
    background: var(--fts-primary);
    border-radius: 2px;
    transition: width 0.5s ease;

    &--warn { background: var(--fts-warning); }
  }
}

// 加班补偿方式
.ctx-compensate-type {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-top: var(--fts-space-3);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  font-size: var(--fts-font-size-sm);
}

.cct-label { color: var(--fts-text-secondary); }
.cct-balance {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  margin-left: auto;
}

// 近期记录列表
.ctx-recent-list {
  margin-top: var(--fts-space-3);
}

.crl-title {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  margin-bottom: var(--fts-space-2);
}

.crl-items {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.crl-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-2) var(--fts-space-3);
  font-size: var(--fts-font-size-xs);
  border-radius: var(--fts-radius-xs);
  background: var(--fts-bg-secondary);

  &:hover { background: var(--fts-bg-tertiary); }
}

.crl-date { color: var(--fts-text-secondary); font-weight: 500; min-width: 45px; }
.crl-hours { color: var(--fts-primary); font-weight: 700; min-width: 35px; }
.crl-type { color: var(--fts-text-tertiary); }

// 换班确认状态
.ctx-swap-status {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  border-radius: var(--fts-radius-md);
  margin-bottom: var(--fts-space-3);
  background: rgba(var(--fts-text-quaternary-rgb), 0.06);
  border: 1px solid var(--fts-border-secondary);

  &--confirmed {
    background: rgba(var(--fts-success-rgb), 0.06);
    border-color: rgba(var(--fts-success-rgb), 0.15);

    .css-icon { color: var(--fts-success); }
    .css-main { color: var(--fts-success); }
  }

  .css-icon {
    color: var(--fts-warning);
    flex-shrink: 0;
  }

  .css-text {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .css-main {
    font-size: var(--fts-font-size-sm);
    font-weight: 600;
    color: var(--fts-warning);
  }

  .css-sub {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }
}

// 换班详情
.ctx-swap-detail {
  margin-bottom: var(--fts-space-3);
}

.csd-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-3);
}

.csd-shift {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex: 1;
  max-width: 180px;
}

.csd-label {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.csd-value {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  padding: 4px 12px;
  border-radius: var(--fts-radius-sm);

  &--out {
    color: var(--fts-error);
    background: rgba(var(--fts-error-rgb), 0.06);
  }

  &--in {
    color: var(--fts-success);
    background: rgba(var(--fts-success-rgb), 0.06);
  }
}

.csd-pos {
  font-size: 10px;
  color: var(--fts-text-quaternary);
}

.csd-arrow {
  color: var(--fts-text-quaternary);
  flex-shrink: 0;
}

// 排班覆盖方案
.ctx-coverage-plan {
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  margin-bottom: var(--fts-space-3);
}

.ccp-label {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  margin-bottom: 4px;
}

.ccp-content {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  line-height: 1.5;
}

// 换班频率
.ctx-swap-freq {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);

  strong { color: var(--fts-text-primary); }
}

// 出差政策标准
.ctx-travel-policy {
  margin-bottom: var(--fts-space-3);
}

.ctp-title {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  margin-bottom: var(--fts-space-2);
  font-weight: 600;
}

.ctp-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--fts-space-2);

  @media (max-width: 480px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.ctp-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
}

.ctp-label {
  font-size: 10px;
  color: var(--fts-text-quaternary);
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.ctp-value {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
}

// 本年度出差次数
.ctx-year-trips {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);

  strong { color: var(--fts-primary); }
}

.cyt-list {
  margin-top: var(--fts-space-2);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.cyt-item {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  padding: 2px 0;
  padding-left: var(--fts-space-2);
  border-left: 2px solid var(--fts-border-primary);
}

// 领用供应商信息
.ctx-supplier {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-top: var(--fts-space-3);
  font-size: var(--fts-font-size-xs);
}

.cs-label { color: var(--fts-text-tertiary); }
.cs-name { color: var(--fts-primary); font-weight: 500; }

// ========== HR风控预警样式 ==========
.hr-risk-warning {
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
  border: 1px solid;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
  }

  // 等级变体
  &--info {
    background: rgba(var(--fts-info-rgb), 0.03);
    border-color: rgba(var(--fts-info-rgb), 0.12);
    &::before { background: var(--fts-info); }
  }

  &--warning {
    background: rgba(var(--fts-warning-rgb), 0.04);
    border-color: rgba(var(--fts-warning-rgb), 0.18);
    &::before { background: var(--fts-warning); }
  }

  &--danger {
    background: rgba(var(--fts-error-rgb), 0.04);
    border-color: rgba(var(--fts-error-rgb), 0.18);
    &::before { background: var(--fts-error); }
  }

  &--prohibited {
    background: rgba(var(--fts-error-rgb), 0.05);
    border-color: rgba(var(--fts-error-rgb), 0.25);
    &::before { background: var(--fts-error); }
  }
}

// 预警头部
.hrw-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-3);
}

.hrw-level-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: 4px 12px;
  border-radius: var(--fts-radius-sm);
  font-size: var(--fts-font-size-xs);
  font-weight: 700;
  letter-spacing: 0.5px;

  svg { flex-shrink: 0; }

  &--info {
    background: rgba(var(--fts-info-rgb), 0.10);
    color: var(--fts-text-secondary);
  }

  &--warning {
    background: rgba(var(--fts-warning-rgb), 0.12);
    color: var(--fts-warning-dark);
  }

  &--danger {
    background: rgba(var(--fts-error-rgb), 0.12);
    color: var(--fts-error);
  }

  &--prohibited {
    background: rgba(var(--fts-error-rgb), 0.12);
    color: var(--fts-error);
  }
}

.hrw-source {
  font-size: 10px;
  color: var(--fts-text-tertiary);
  white-space: nowrap;
}

// 标题（深色模式增强可读性）
.hrw-title {
  font-size: var(--fts-font-size-base);
  font-weight: 700;
  line-height: 1.4;
  margin-bottom: var(--fts-space-3);

  .hr-risk-warning--info & { color: var(--fts-text-primary); }
  .hr-risk-warning--warning & {
    color: var(--fts-warning-dark);
  }
  .hr-risk-warning--danger & {
    color: var(--fts-error);
  }
  .hr-risk-warning--prohibited & {
    color: var(--fts-error);
  }
}

// 详细描述
.hrw-body {
  margin-bottom: var(--fts-space-3);
}

.hrw-description {
  margin: 0;
  font-family: inherit;
  font-size: var(--fts-font-size-sm);
  line-height: 1.7;
  color: var(--fts-text-secondary);
  white-space: pre-wrap;
  word-wrap: break-word;
}

// 数据依据
.hrw-evidence {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-xs);
  font-size: var(--fts-font-size-xs);
  margin-bottom: var(--fts-space-3);
  border: 1px solid var(--fts-border-secondary);
}

.hrw-evidence-label {
  font-weight: 600;
  color: var(--fts-text-secondary);
  white-space: nowrap;
  flex-shrink: 0;
}

.hrw-evidence-value {
  color: var(--fts-primary);
  font-family: monospace;
  font-size: 11px;
  font-weight: 500;
}

// 建议操作
.hrw-suggestion {
  border-radius: var(--fts-radius-sm);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-card);
  border: 1px dashed var(--fts-border-secondary);

  .hr-risk-warning--danger &, .hr-risk-warning--prohibited & {
    border-style: solid;
    border-color: rgba(var(--fts-error-rgb), 0.15);
  }
}

.hrw-suggestion-label {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-xs);
  font-weight: 600;
  color: var(--fts-success);
  margin-bottom: var(--fts-space-2);

  svg { flex-shrink: 0; }
}

.hrw-suggestion-text {
  margin: 0;
  font-size: var(--fts-font-size-sm);
  line-height: 1.6;
  color: var(--fts-text-secondary);
}

// 法规引用
.hrw-regulation {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-top: var(--fts-space-3);
  padding-top: var(--fts-space-3);
  border-top: 1px solid var(--fts-border-secondary);
  font-size: 10px;
  color: var(--fts-text-quaternary);
  font-style: italic;

  svg { flex-shrink: 0; opacity: 0.6; }
}
</style>
