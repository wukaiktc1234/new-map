<script setup lang="ts">
/**
 * 工作台首页 - 基于角色的差异化展示
 *
 * 设计原理：
 * 1. 通过 useRoleDashboard composable 读取当前用户角色
 * 2. 根据 RoleProfile 配置渲染差异化的统计卡片/快捷入口/预警
 * 3. 所有数据来源于真实后端 API（/v1/dashboard/*）
 * 4. 后端未实现的指标展示 0 + "待实现"角标（不伪造数据）
 * 5. 待办事项暂无统一聚合接口，展示空状态 + 说明（不戛然而止）
 *
 * 6 个角色组：admin_owner / ops / finance / hr / store / employee
 * 角色配置见 @/views/dashboard/role-profiles.ts
 */
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Document, Warning } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import { useRoleDashboard } from '@/composables/useRoleDashboard'
import { usePermissionStore } from '@/stores/permission'

const router = useRouter()
const permissionStore = usePermissionStore()
const { profile, greeting, statsCards, alerts, loading, errorMessage, refresh } =
  useRoleDashboard()

/**
 * 根据权限过滤快捷入口
 * - 未配置 requiredPermission 的入口默认可见
 * - 配置 requiredPermission 的入口需用户拥有对应权限才可见
 */
const filteredQuickActions = computed(() =>
  profile.value.quickActions.filter(
    action => !action.requiredPermission || permissionStore.hasPermission(action.requiredPermission),
  ),
)

function navigateTo(path: string): void {
  router.push(path)
}

onMounted(() => {
  void refresh()
})
</script>

<template>
  <div class="dashboard-page">
    <PageHeader :title="`工作台 · ${profile.displayName}`" :description="greeting">
      <el-button size="default" @click="refresh" :loading="loading">
        <el-icon :size="16"><Refresh /></el-icon>
        刷新
      </el-button>
    </PageHeader>

    <!-- 角色描述条 -->
    <div class="role-banner">
      <span class="role-banner__label">当前角色：</span>
      <span class="role-banner__name">{{ profile.displayName }}</span>
      <span class="role-banner__desc">{{ profile.description }}</span>
    </div>

    <!-- 加载错误提示 -->
    <el-alert
      v-if="errorMessage"
      :title="`数据加载失败：${errorMessage}`"
      type="error"
      :closable="false"
      show-icon
      class="error-alert"
    />

    <!-- 统计卡片区域 -->
    <section class="stats-section" v-loading="loading">
      <div v-for="card in statsCards" :key="card.key" class="stat-card-wrapper">
        <StatCard
          :icon="card.icon"
          :label="card.label"
          :value="card.displayValue"
          :color-type="card.colorType"
          :trend="card.trend"
          variant="bordered"
        />
        <span
          v-if="card.showTodoBadge"
          class="todo-badge"
          title="该指标后端待实现，当前展示为占位值"
        >
          待实现
        </span>
      </div>
    </section>

    <!-- 主体内容区 -->
    <div class="dashboard-grid">
      <div class="grid-left">
        <!-- 快捷入口 -->
        <section class="panel quick-actions-panel">
          <div class="panel__header">
            <h3 class="panel__title">快捷入口</h3>
            <span class="panel__subtitle">{{ profile.displayName }}常用功能</span>
          </div>
          <div class="quick-actions">
            <button
              v-for="action in filteredQuickActions"
              :key="action.path"
              class="quick-action-btn"
              @click="navigateTo(action.path)"
            >
              <span class="quick-action-btn__icon" :style="{ color: action.color }">
                <el-icon :size="22"><component :is="action.icon" /></el-icon>
              </span>
              <span class="quick-action-btn__label">{{ action.label }}</span>
            </button>
          </div>
        </section>

        <!-- 我的待办 -->
        <section class="panel pending-panel">
          <div class="panel__header">
            <h3 class="panel__title">我的待办</h3>
            <span class="panel__badge">0 项</span>
          </div>
          <!--
            空状态说明：
            后端目前未提供统一的待办聚合接口（如 /v1/dashboard/my-pending）。
            此处展示空状态 + 角色对应的待办类别说明，避免"戛然而止"。
            后续后端实现后，替换为真实待办列表。
          -->
          <div class="empty-state">
            <el-icon :size="48" class="empty-state__icon"><Document /></el-icon>
            <p class="empty-state__title">暂无待办事项</p>
            <p class="empty-state__hint">{{ profile.pendingTasksHint }}</p>
          </div>
        </section>
      </div>

      <div class="grid-right">
        <!-- 预警中心 -->
        <section class="panel alert-panel">
          <div class="panel__header">
            <h3 class="panel__title">预警中心</h3>
            <span class="panel__subtitle">需要关注的风险</span>
          </div>
          <div class="alert-list" v-loading="loading">
            <div
              v-for="alert in alerts"
              :key="alert.module"
              class="alert-item"
              :class="[`alert-item--${alert.type}`, { 'alert-item--clickable': alert.link }]"
              @click="alert.link && navigateTo(alert.link)"
            >
              <div class="alert-item__header">
                <span class="alert-item__module">{{ alert.module }}</span>
                <span class="alert-item__count">{{ alert.count }} 项</span>
              </div>
              <p class="alert-item__content">{{ alert.content }}</p>
            </div>
            <!--
              空状态说明：
              当库存预警/缺货/积压都为 0 时展示。
              后端扩展更多预警源（财务/合规/设备）后，此处会自动填充。
            -->
            <div v-if="alerts.length === 0 && !loading" class="empty-state">
              <el-icon :size="48" class="empty-state__icon"><Warning /></el-icon>
              <p class="empty-state__title">暂无预警信息</p>
              <p class="empty-state__hint">{{ profile.alertsHint }}</p>
            </div>
          </div>
        </section>

        <!-- 数据来源说明 -->
        <section class="panel data-source-panel">
          <div class="panel__header">
            <h3 class="panel__title">数据来源</h3>
          </div>
          <ul class="data-source-list">
            <li>
              <span class="data-source-list__label">今日销售/订单：</span>
              <span class="data-source-list__value">GET /v1/dashboard/today-sales</span>
            </li>
            <li>
              <span class="data-source-list__label">库存预警：</span>
              <span class="data-source-list__value">GET /v1/dashboard/inventory-alerts</span>
            </li>
            <li>
              <span class="data-source-list__label">会员增长：</span>
              <span class="data-source-list__value">GET /v1/dashboard/member-growth?days=30</span>
            </li>
            <li>
              <span class="data-source-list__label">财务概况：</span>
              <span class="data-source-list__value">GET /v1/dashboard/finance-summary</span>
            </li>
          </ul>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.dashboard-page {
  min-height: 100vh;
  background: var(--fts-bg-page);
}

// 角色描述条
.role-banner {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
  background: var(--fts-primary-light);
  border-radius: var(--fts-radius-md);
  font-size: var(--fts-font-size-sm);

  &__label {
    color: var(--fts-text-tertiary);
  }

  &__name {
    color: var(--fts-primary);
    font-weight: var(--fts-font-weight-semibold);
  }

  &__desc {
    color: var(--fts-text-secondary);
    margin-left: var(--fts-space-2);

    &::before {
      content: '·';
      margin-right: var(--fts-space-2);
      color: var(--fts-text-tertiary);
    }
  }
}

.error-alert {
  margin-bottom: var(--fts-space-4);
}

// 统计卡片区域
.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-6);
  min-height: 100px;

  @media (max-width: 1280px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.stat-card-wrapper {
  position: relative;
}

.todo-badge {
  position: absolute;
  top: var(--fts-space-2);
  right: var(--fts-space-2);
  padding: 2px var(--fts-space-2);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-warning);
  background: var(--fts-warning-bg);
  border-radius: var(--fts-radius-sm);
  font-weight: var(--fts-font-weight-medium);
  pointer-events: none;
}

// 主体内容网格
.dashboard-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: var(--fts-space-4);
  min-height: 360px;

  @media (max-width: 1100px) {
    grid-template-columns: 1fr;
  }
}

.grid-left,
.grid-right {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

// 面板通用样式
.panel {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-5) var(--fts-space-6);

  &__header {
    display: flex;
    align-items: baseline;
    gap: var(--fts-space-3);
    margin-bottom: var(--fts-space-4);
  }

  &__title {
    margin: 0;
    font-size: var(--fts-font-size-lg);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
  }

  &__subtitle {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-tertiary);
  }

  &__badge {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-primary);
    background: var(--fts-primary-light);
    padding: 2px var(--fts-space-2);
    border-radius: var(--fts-radius-full);
    margin-left: auto;
  }
}

// 快捷入口
.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-3);

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.quick-action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-4) var(--fts-space-2);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  background: var(--fts-bg-card);
  cursor: pointer;
  transition: all var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    border-color: var(--fts-primary);
    background: var(--fts-bg-hover);
    transform: translateY(-1px);
    box-shadow: var(--fts-shadow-sm);
  }

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    border-radius: var(--fts-radius-md);
    background: var(--fts-bg-secondary);
  }

  &__label {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    font-weight: var(--fts-font-weight-medium);
  }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--fts-space-8);
  text-align: center;

  &__icon {
    color: var(--fts-text-quaternary);
    margin-bottom: var(--fts-space-3);
  }

  &__title {
    margin: 0 0 var(--fts-space-2);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-secondary);
    font-weight: var(--fts-font-weight-medium);
  }

  &__hint {
    margin: 0;
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    max-width: 320px;
    line-height: var(--fts-line-height-normal);
  }
}

// 预警列表
.alert-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.alert-item {
  padding: var(--fts-space-3) var(--fts-space-4);
  border-radius: var(--fts-radius-md);
  border-left: 3px solid var(--fts-border-hover);
  background: var(--fts-bg-secondary);
  transition: all var(--fts-duration-fast) var(--fts-easing-default);

  &--warning {
    border-left-color: var(--fts-warning);
    background: var(--fts-warning-bg);
  }

  &--error {
    border-left-color: var(--fts-error);
    background: var(--fts-error-bg);
  }

  &--clickable {
    cursor: pointer;

    &:hover {
      transform: translateX(2px);
      box-shadow: var(--fts-shadow-sm);
    }
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--fts-space-1);
  }

  &__module {
    font-size: var(--fts-font-size-sm);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
  }

  &__count {
    font-size: var(--fts-font-size-xs);
    font-weight: var(--fts-font-weight-semibold);
    padding: 2px var(--fts-space-2);
    border-radius: var(--fts-radius-sm);
    background: var(--fts-bg-card);
    font-variant-numeric: tabular-nums;
  }

  &__content {
    margin: 0;
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    line-height: var(--fts-line-height-normal);
  }
}

// 数据来源说明
.data-source-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);

  li {
    display: flex;
    align-items: baseline;
    gap: var(--fts-space-2);
    font-size: var(--fts-font-size-xs);
    line-height: var(--fts-line-height-normal);
  }

  &__label {
    color: var(--fts-text-tertiary);
    flex-shrink: 0;
    min-width: 100px;
  }

  &__value {
    color: var(--fts-text-secondary);
    font-family: var(--fts-font-mono, 'SF Mono', Monaco, Menlo, monospace);
    word-break: break-all;
  }
}
</style>
