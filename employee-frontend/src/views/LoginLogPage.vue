<script setup lang="ts">
/**
 * LoginLogPage - 登录日志页
 *
 * 显示近期登录记录，支持时间筛选
 * Mock数据实现，后续对接后端LoginLog API
 */
import { ref, computed } from 'vue'
import { View, CircleCloseFilled, CircleCheckFilled } from '@element-plus/icons-vue'
import { PageContainer, StatusTag, EmptyState } from '@/components/core'

/** 登录日志条目 */
interface LoginLogItem {
  id: string
  time: string           // 登录时间
  device: string         // 设备类型（Chrome/macOS等）
  ip: string             // IP地址
  location: string       // 地理位置
  status: 'success' | 'fail'  // 登录状态
  os: string             // 操作系统
}

/** 时间筛选类型 */
type TimeFilter = '7d' | '30d' | '90d'

const activeFilter = ref<TimeFilter>('7d')

// ========== Mock数据 ==========
const allLogs: LoginLogItem[] = [
  { id: '1', time: '2026-06-06 09:15:22', device: 'Chrome / Windows 11', ip: '192.168.1.105', location: '本机登录', status: 'success', os: 'Windows 11' },
  { id: '2', time: '2026-06-05 18:42:10', device: 'Chrome / Windows 11', ip: '192.168.1.105', location: '本机登录', status: 'success', os: 'Windows 11' },
  { id: '3', time: '2026-06-05 08:30:05', device: 'Safari / iOS 18', ip: '10.0.0.23', location: '北京市朝阳区', status: 'success', os: 'iOS' },
  { id: '4', time: '2026-06-04 23:15:44', device: 'Chrome / macOS 15', ip: '172.16.0.88', location: '上海市浦东新区', status: 'fail', os: 'macOS' },
  { id: '5', time: '2026-06-04 23:12:01', device: 'Chrome / macOS 15', ip: '172.16.0.88', location: '上海市浦东新区', status: 'fail', os: 'macOS' },
  { id: '6', time: '2026-06-04 14:20:33', device: 'Firefox / Android 14', ip: '10.8.0.45', location: '广州市天河区', status: 'success', os: 'Android' },
  { id: '7', time: '2026-06-03 09:00:18', device: 'Chrome / Windows 11', ip: '192.168.1.105', location: '本机登录', status: 'success', os: 'Windows 11' },
  { id: '8', time: '2026-06-02 17:45:55', device: 'Edge / Windows 10', ip: '10.0.0.102', location: '深圳市南山区', status: 'success', os: 'Windows 10' },
  { id: '9', time: '2026-06-01 08:12:40', device: '微信内置浏览器 / iOS 18', ip: '10.0.0.23', location: '北京市朝阳区', status: 'success', os: 'iOS' },
  { id: '10', time: '2026-05-28 20:33:21', device: 'Chrome / Windows 11', ip: '192.168.1.105', location: '本机登录', status: 'fail', os: 'Windows 11' },
  { id: '11', time: '2026-05-28 20:30:15', device: 'Chrome / Windows 11', ip: '192.168.1.105', location: '本机登录', status: 'fail', os: 'Windows 11' },
  { id: '12', time: '2026-05-25 11:07:48', device: 'Chrome / macOS 15', ip: '172.16.0.88', location: '上海市浦东新区', status: 'success', os: 'macOS' },
]

/** 根据筛选条件过滤日志 */
const filteredLogs = computed(() => {
  const now = new Date()
  let daysAgo: number

  switch (activeFilter.value) {
    case '7d': daysAgo = 7; break
    case '30d': daysAgo = 30; break
    case '90d': daysAgo = 90; break
    default: daysAgo = 7
  }

  const cutoff = new Date(now.getTime() - daysAgo * 24 * 60 * 60 * 1000)

  return allLogs.filter(log => new Date(log.time) >= cutoff)
})

/** 统计摘要 */
const stats = computed(() => ({
  total: filteredLogs.value.length,
  success: filteredLogs.value.filter(l => l.status === 'success').length,
  fail: filteredLogs.value.filter(l => l.status === 'fail').length,
}))

/** 时间筛选配置 */
const filterOptions: { key: TimeFilter; label: string }[] = [
  { key: '7d', label: '最近7天' },
  { key: '30d', label: '最近30天' },
  { key: '90d', label: '最近90天' },
]
</script>

<template>
  <PageContainer title="登录日志">
    <div class="login-log-page">
      <!-- 统计摘要 -->
      <section class="stats-bar">
        <div class="stat-item stat-item--total">
          <span class="stat-value">{{ stats.total }}</span>
          <span class="stat-label">总次数</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item stat-item--success">
          <span class="stat-dot stat-dot--success"></span>
          <span class="stat-value">{{ stats.success }}</span>
          <span class="stat-label">成功</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item stat-item--fail">
          <span class="stat-dot stat-dot--fail"></span>
          <span class="stat-value">{{ stats.fail }}</span>
          <span class="stat-label">失败</span>
        </div>
      </section>

      <!-- 时间筛选Tab -->
      <section class="filter-bar">
        <button
          v-for="opt in filterOptions"
          :key="opt.key"
          :class="['filter-tab', { 'filter-tab--active': activeFilter === opt.key }]"
          @click="activeFilter = opt.key"
        >
          {{ opt.label }}
        </button>
      </section>

      <!-- 空状态 -->
      <EmptyState
        v-if="filteredLogs.length === 0"
        title="暂无登录记录"
        description="所选时间段内没有登录记录"
      />

      <!-- 日志列表 -->
      <section v-else class="log-list-section">
        <ul class="log-list">
          <li
            v-for="log in filteredLogs"
            :key="log.id"
            class="log-item"
            :class="{ 'log-item--fail': log.status === 'fail' }"
          >
            <!-- 状态图标 -->
            <span class="log-status-icon">
              <el-icon v-if="log.status === 'success'" :size="16" style="color: var(--fts-success)">
                <CircleCheckFilled />
              </el-icon>
              <el-icon v-else :size="16" style="color: var(--fts-error)">
                <CircleCloseFilled />
              </el-icon>
            </span>

            <!-- 主要信息 -->
            <div class="log-main">
              <div class="log-header">
                <span class="log-device">{{ log.device }}</span>
                <StatusTag
                  :status="log.status === 'success' ? 'approved' : 'error'"
                  size="small"
                  variant="light"
                />
              </div>
              <div class="log-meta">
                <span class="log-time">{{ log.time }}</span>
                <span class="log-meta-sep">|</span>
                <span class="log-ip">{{ log.ip }}</span>
                <span class="log-meta-sep">|</span>
                <span class="log-location">{{ log.location }}</span>
              </div>
            </div>
          </li>
        </ul>
      </section>

      <!-- 安全警告（有失败记录时显示） -->
      <section v-if="stats.fail > 0" class="warning-section">
        <p class="warning-title">检测到异常登录</p>
        <p class="warning-desc">
          最近{{ activeFilter === '7d' ? '7' : activeFilter === '30d' ? '30' : '90' }}天内有
          <strong>{{ stats.fail }}</strong> 次失败登录记录，
          如非本人操作请尽快修改密码
        </p>
      </section>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.login-log-page {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

/* ===== 统计摘要栏 ===== */
.stats-bar {
  display: flex;
  align-items: center;
  padding: var(--fts-space-3) var(--fts-space-4);
  background-color: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 var(--fts-space-3);

  &--total {
    .stat-value {
      font-size: var(--fts-font-size-lg);
      font-weight: 700;
      color: var(--fts-text-primary);
    }
  }

  &--success,
  &--fail {
    .stat-value {
      font-size: var(--fts-font-size-base);
      font-weight: 600;
    }
  }

  &--success .stat-value { color: var(--fts-success); }
  &--fail .stat-value { color: var(--fts-error); }
}

.stat-label {
  font-size: var(--fts-font-size-2xs);
  color: var(--fts-text-quaternary);
}

.stat-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;

  &--success { background-color: var(--fts-success); }
  &--fail { background-color: var(--fts-error); }
}

.stat-divider {
  width: 1px;
  height: 20px;
  background-color: var(--fts-border-secondary);
}

/* ===== 筛选Tab ===== */
.filter-bar {
  display: flex;
  gap: var(--fts-space-1);
}

.filter-tab {
  padding: var(--fts-space-2) var(--fts-space-3);
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-secondary);
  background-color: var(--fts-bg-secondary);
  border: none;
  border-radius: var(--fts-radius-full);
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--fts-duration-fast) var(--fts-easing-default);

  &:hover:not(.filter-tab--active) {
    color: var(--fts-text-primary);
    background-color: var(--fts-bg-tertiary);
  }

  &--active {
    background-color: var(--fts-primary);
    color: var(--fts-text-on-primary);
  }

  &:active {
    transform: scale(0.97);
  }
}

/* ===== 日志列表 ===== */
.log-list-section {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-3) var(--fts-space-4);
  box-shadow: var(--fts-shadow-xs);
}

.log-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.log-item {
  display: flex;
  align-items: flex-start;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) 0;
  transition: background-color var(--fts-duration-fast) ease;
  border-radius: var(--fts-radius-sm);

  &:not(:last-child) {
    border-bottom: 1px solid var(--fts-border-secondary);
  }

  &:hover {
    background-color: var(--fts-bg-hover);
  }

  &--fail {
    background-color: rgba(var(--fts-error-rgb), 0.03);

    &:hover {
      background-color: rgba(var(--fts-error-rgb), 0.06);
    }
  }
}

.log-status-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.log-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.log-header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.log-device {
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-primary);
}

.log-meta {
  display: flex;
  align-items: center;
  gap: var(--fts-space-1);
  font-size: var(--fts-font-size-2xs);
  color: var(--fts-text-quaternary);
  flex-wrap: wrap;
}

.log-meta-sep {
  color: var(--fts-bg-tertiary);
}

/* ===== 安全警告 ===== */
.warning-section {
  background: rgba(var(--fts-error-rgb), 0.04);
  border: 1px solid rgba(var(--fts-error-rgb), 0.12);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-3) var(--fts-space-4);
}

.warning-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-error);
  margin: 0 0 var(--fts-space-1);
}

.warning-desc {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  margin: 0;
  line-height: 1.5;

  strong {
    color: var(--fts-error);
    font-weight: 700;
  }
}
</style>
