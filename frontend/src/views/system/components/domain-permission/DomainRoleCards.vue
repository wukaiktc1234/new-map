<script setup lang="ts">
/**
 * 角色卡片对比面板
 *
 * 职责：
 * - 摘要统计栏：所有非 admin 角色的权限分布点阵
 * - 对比模式选择器：选择对比目标角色
 * - 角色卡片网格：点击切换选中角色，对比模式下显示双行点阵
 * - 对比差异详情：列出当前角色与对比角色的域权限差异
 */
import { DataLine, Position } from '@element-plus/icons-vue'
import StatusTag from '@/components/core/StatusTag.vue'
import type {
  BusinessDomain,
  DomainAccessLevel,
  DomainAccessLevelMetaMap,
  DomainCompareDiff,
  RoleOverviewStat,
  RoleDomainMatrix,
} from '@/types/domain-permission'

interface RoleItem {
  code: string
  name: string
}

interface Props {
  /** 业务域列表 */
  businessDomains: BusinessDomain[]
  /** 非 admin 角色列表 */
  nonAdminRoles: RoleItem[]
  /** 全部角色列表（用于查找角色名） */
  roles: RoleItem[]
  /** 当前选中角色 */
  selectedRole: string
  /** 对比模式开关 */
  compareMode: boolean
  /** 对比目标角色 */
  compareRole: string
  /** 对比差异列表（compareMode 关闭时为 null） */
  compareDiff: DomainCompareDiff[] | null
  /** 角色概览统计（摘要栏） */
  roleOverviewStats: RoleOverviewStat[]
  /** 总域数量 */
  totalDomainCount: number
  /** 获取单元格访问级别（响应式） */
  getLevel: (domainCode: string, roleCode: string) => DomainAccessLevel
  /** 获取角色可见域列表 */
  getRoleDomains: (roleCode: string) => string[]
  /** 获取完整矩阵 */
  getFullRoleDomainMatrix: () => RoleDomainMatrix
  /** 访问级别元数据 */
  levelMeta: DomainAccessLevelMetaMap
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:selectedRole', value: string): void
  (e: 'update:compareMode', value: boolean): void
  (e: 'update:compareRole', value: string): void
}>()

/** 切换对比模式 */
function toggleCompareMode(): void {
  emit('update:compareMode', !props.compareMode)
}

/** 选中角色 */
function selectRole(code: string): void {
  emit('update:selectedRole', code)
}

/** 查找角色名 */
function findRoleName(code: string): string {
  return props.roles.find(r => r.code === code)?.name || code
}

/** 获取角色完全访问域数 */
function getFullCount(roleCode: string): number {
  const matrix = props.getFullRoleDomainMatrix()[roleCode] || {}
  return Object.values(matrix).filter(l => l === 'FULL').length
}

/** 获取角色只读域数 */
function getReadOnlyCount(roleCode: string): number {
  const matrix = props.getFullRoleDomainMatrix()[roleCode] || {}
  return Object.values(matrix).filter(l => l === 'READ_ONLY').length
}

/** 判断域是否在对比差异中 */
function isDiffDomain(domainCode: string): boolean {
  return !!props.compareDiff?.some(d => d.domainCode === domainCode)
}

/** 将访问级别映射为 StatusTag 的 status 值 */
function levelToTagStatus(level: DomainAccessLevel): string {
  if (level === 'FULL') return 'primary'
  if (level === 'READ_ONLY') return 'success'
  if (level === 'LIMITED') return 'warning'
  return 'inactive'
}
</script>

<template>
  <div class="quick-actions">
    <div class="qa-header">
      <div class="qa-header-left">
        <h4 class="action-title">典型角色权限分布对比</h4>
        <span class="qa-hint">点击角色卡片快速切换并查看详细分析</span>
      </div>
      <div class="qa-header-actions">
        <el-button
          :type="compareMode ? 'primary' : 'default'"
          size="small"
          @click="toggleCompareMode"
        >
          <el-icon><DataLine /></el-icon>
          {{ compareMode ? '退出对比' : '对比模式' }}
        </el-button>
      </div>
    </div>

    <!-- 摘要统计栏 -->
    <div class="qa-summary-bar">
      <div class="summary-item" v-for="stat in roleOverviewStats" :key="stat.code">
        <span class="summary-name" :class="{ 'summary-name--active': selectedRole === stat.code }">{{ stat.name }}</span>
        <div class="summary-dots">
          <span class="summary-dot summary-dot--full" :title="`完全访问: ${stat.full}个域`" :style="{ width: `${(stat.full / totalDomainCount) * 100}%` }"></span>
          <span class="summary-dot summary-dot--readonly" :title="`只读: ${stat.readOnly}个域`" :style="{ width: `${(stat.readOnly / totalDomainCount) * 100}%` }"></span>
          <span class="summary-dot summary-dot--limited" :title="`受限: ${stat.limited}个域`" :style="{ width: `${(stat.limited / totalDomainCount) * 100}%` }"></span>
          <span class="summary-dot summary-dot--hidden" :title="`隐藏: ${stat.hidden}个域`" :style="{ width: `${(stat.hidden / totalDomainCount) * 100}%` }"></span>
        </div>
        <span class="summary-label">{{ stat.full + stat.readOnly }}域可见</span>
      </div>
    </div>

    <!-- 对比模式选择器 -->
    <div class="compare-selector" v-if="compareMode">
      <div class="compare-line">
        <span class="compare-label">对比目标：</span>
        <el-select
          :model-value="compareRole"
          size="small"
          style="width: 140px"
          @update:model-value="(v: string) => emit('update:compareRole', v)"
        >
          <el-option
            v-for="role in nonAdminRoles.filter(r => r.code !== selectedRole)"
            :key="role.code"
            :label="role.name"
            :value="role.code"
          />
        </el-select>
        <span class="compare-vs">VS</span>
        <strong>{{ findRoleName(selectedRole) }}</strong>
        <StatusTag v-if="compareDiff" status="info" :label="`${compareDiff.length}项差异`" size="small" />
      </div>
    </div>

    <div class="role-cards">
      <div
        v-for="role in nonAdminRoles"
        :key="role.code"
        :class="['role-card', {
          'role-card--active': selectedRole === role.code,
          'role-card--compare': compareMode && compareRole === role.code,
        }]"
        @click="selectRole(role.code)"
      >
        <div class="card-header">
          <span class="card-role-name">{{ role.name }}</span>
          <div class="card-badges">
            <StatusTag
              v-if="selectedRole === role.code"
              status="primary"
              label="当前选中"
              size="small"
            />
            <StatusTag
              v-if="compareMode && compareRole === role.code && selectedRole !== role.code"
              status="warning"
              label="对比目标"
              size="small"
            />
          </div>
        </div>

        <!-- 对比模式：双行点阵 -->
        <div class="card-domains" v-if="!compareMode || selectedRole === role.code || compareRole === role.code">
          <template v-if="compareMode && selectedRole === role.code && compareRole !== role.code">
            <span
              v-for="domain in businessDomains"
              :key="domain.domainCode"
              :class="['domain-dot', `dot--${getLevel(domain.domainCode, role.code).toLowerCase()}`, {
                'dot--diff': isDiffDomain(domain.domainCode)
              }]"
              :title="`${domain.domainName}: ${levelMeta[getLevel(domain.domainCode, role.code)].label}${isDiffDomain(domain.domainCode) ? ' (与' + findRoleName(compareRole) + '不同)' : ''}`"
            ></span>
          </template>
          <template v-else>
            <span
              v-for="domain in businessDomains"
              :key="domain.domainCode"
              :class="['domain-dot', `dot--${getLevel(domain.domainCode, role.code).toLowerCase()}`]"
              :title="`${domain.domainName}: ${levelMeta[getLevel(domain.domainCode, role.code)].label}`"
            ></span>
          </template>
        </div>

        <!-- 对比模式：显示差异箭头 -->
        <div class="card-compare-row" v-if="compareMode && selectedRole === role.code && compareRole !== role.code">
          <span class="compare-arrow">↓ 对比 ↓</span>
        </div>
        <div class="card-domains card-domains--compare" v-if="compareMode && selectedRole === role.code && compareRole !== role.code">
          <span
            v-for="domain in businessDomains"
            :key="'cmp-' + domain.domainCode"
            :class="['domain-dot', `dot--${getLevel(domain.domainCode, compareRole).toLowerCase()}`, {
              'dot--diff': isDiffDomain(domain.domainCode)
            }]"
            :title="`${domain.domainName} (${findRoleName(compareRole)}): ${levelMeta[getLevel(domain.domainCode, compareRole)].label}`"
          ></span>
        </div>

        <div class="card-stats">
          <span><b>{{ getRoleDomains(role.code).length }}</b>域可见</span>
          <span class="stat-divider">|</span>
          <span><b>{{ getFullCount(role.code) }}</b>完全</span>
          <span class="stat-divider">|</span>
          <span><b>{{ getReadOnlyCount(role.code) }}</b>只读</span>
        </div>
      </div>
    </div>

    <!-- 对比差异详情 -->
    <div class="compare-detail" v-if="compareMode && compareDiff && compareDiff.length > 0">
      <div class="compare-detail-header">
        <el-icon><Position /></el-icon>
        <span>权限差异详情（{{ findRoleName(selectedRole) }} VS {{ findRoleName(compareRole) }}）</span>
      </div>
      <div class="compare-diff-list">
        <div
          v-for="diff in compareDiff"
          :key="diff.domainCode"
          class="diff-item"
        >
          <span class="diff-domain">{{ diff.domainName }}</span>
          <div class="diff-values">
            <StatusTag
              :status="levelToTagStatus(diff.current)"
              :label="levelMeta[diff.current].label"
              size="small"
            />
            <span class="diff-arrow">→</span>
            <StatusTag
              :status="levelToTagStatus(diff.compare)"
              :label="levelMeta[diff.compare].label"
              size="small"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.quick-actions {
  margin-top: var(--fts-space-4);
  padding: var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
}

.qa-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-4);
}

.qa-header-left {
  display: flex;
  align-items: baseline;
  gap: var(--fts-space-3);
}

.qa-header-actions {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.action-title {
  margin: 0;
  font-size: var(--fts-font-size-md);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.qa-hint {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);
}

.qa-summary-bar {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  margin-bottom: var(--fts-space-4);
}

.summary-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
}

.summary-name {
  font-size: var(--fts-font-size-xs);
  font-weight: 500;
  color: var(--fts-text-secondary);
  min-width: 72px;
  text-align: right;
  transition: color 0.15s ease;

  &--active {
    color: var(--fts-primary);
    font-weight: 600;
  }
}

.summary-dots {
  flex: 1;
  display: flex;
  height: 8px;
  border-radius: var(--fts-radius-full);
  overflow: hidden;
  background: var(--fts-bg-tertiary);
}

.summary-dot {
  height: 100%;
  transition: width 0.3s ease;

  &--full { background: var(--fts-primary); }
  &--readonly { background: var(--fts-success); }
  &--limited { background: var(--fts-warning); }
  &--hidden { background: var(--fts-border-primary); opacity: 0.3; }
}

.summary-label {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  min-width: 56px;
}

.compare-selector {
  padding: var(--fts-space-2) var(--fts-space-4);
  background: rgba(var(--fts-warning-rgb, 230, 162, 60), 0.06);
  border: 1px dashed var(--fts-warning);
  border-radius: var(--fts-radius-md);
  margin-bottom: var(--fts-space-3);
}

.compare-line {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-sm);
}

.compare-label {
  color: var(--fts-text-secondary);
  font-weight: 500;
}

.compare-vs {
  color: var(--fts-warning);
  font-weight: 700;
  font-size: var(--fts-font-size-xs);
}

.role-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-4);
}

.role-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-3) var(--fts-space-4);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--fts-primary);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    transform: translateY(-1px);
  }

  &--active {
    border-color: var(--fts-primary);
    background: linear-gradient(135deg, rgba(var(--fts-primary-rgb, 64, 158, 255), 0.04), transparent);
    box-shadow: 0 0 0 1px var(--fts-primary);
  }

  &--compare {
    border-color: var(--fts-warning);
    background: linear-gradient(135deg, rgba(var(--fts-warning-rgb, 230, 162, 60), 0.04), transparent);
    box-shadow: 0 0 0 1px var(--fts-warning);

    &:hover {
      border-color: var(--fts-warning);
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-2);
}

.card-badges {
  display: flex;
  gap: var(--fts-space-1);
}

.card-role-name {
  font-size: var(--fts-font-size-md);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.card-domains {
  display: flex;
  gap: 3px;
  flex-wrap: wrap;
  margin-bottom: var(--fts-space-2);
}

.domain-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  transition: background-color 0.15s ease;

  &--full { background: var(--fts-primary); }
  &--read_only { background: var(--fts-success); }
  &--limited { background: var(--fts-warning); }
  &--hidden { background: var(--fts-border-primary); opacity: 0.3; }

  &--diff {
    box-shadow: 0 0 0 2px var(--fts-warning);
    animation: dot-pulse 1.5s ease-in-out infinite;
  }
}

@keyframes dot-pulse {
  0%, 100% { box-shadow: 0 0 0 2px rgba(var(--fts-warning-rgb, 230, 162, 60), 0.4); }
  50% { box-shadow: 0 0 0 4px rgba(var(--fts-warning-rgb, 230, 162, 60), 0.15); }
}

.card-compare-row {
  display: flex;
  justify-content: center;
  padding: var(--fts-space-1) 0;
}

.compare-arrow {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-warning);
  font-weight: 600;
  letter-spacing: 2px;
}

.card-stats {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);

  b {
    color: var(--fts-text-primary);
    font-weight: 500;
  }

  .stat-divider {
    color: var(--fts-border-primary);
  }
}

.compare-detail {
  margin-top: var(--fts-space-3);
  border: 1px solid var(--fts-warning);
  border-radius: var(--fts-radius-md);
  overflow: hidden;
}

.compare-detail-header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-4);
  background: rgba(var(--fts-warning-rgb, 230, 162, 60), 0.08);
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.compare-diff-list {
  padding: var(--fts-space-2) var(--fts-space-4);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
  background: var(--fts-bg-card);
}

.diff-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-1) var(--fts-space-2);
  border-radius: var(--fts-radius-sm);

  &:nth-child(even) {
    background: var(--fts-bg-secondary);
  }
}

.diff-domain {
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-primary);
  min-width: 80px;
}

.diff-values {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.diff-arrow {
  color: var(--fts-text-quaternary);
  font-weight: 600;
}
</style>
