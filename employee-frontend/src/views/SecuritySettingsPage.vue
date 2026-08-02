<script setup lang="ts">
/**
 * SecuritySettingsPage - 安全设置导航页
 *
 * 作为安全功能入口枢纽，链接到各子页面：
 * - 修改密码 → PasswordChangePage
 * - 密码找回 → PasswordRecoveryPage
 * - 邮箱绑定 → EmailBindPage
 * - 登录设备 → DeviceListPage
 * - 登录日志 → LoginLogPage
 * - 安全锁定 → SecurityLockPage
 *
 * 注意：本页仅做导航，不内嵌任何表单/列表，
 * 避免与ProfilePage"安全中心"section产生功能重复。
 */
import { useRouter } from 'vue-router'
import {
  Lock, Key, Message, Monitor,
  Document, CircleCheck,
} from '@element-plus/icons-vue'
import { PageContainer } from '@/components/core'

const router = useRouter()

/** 安全设置菜单项 */
const menuItems = [
  {
    key: 'password',
    icon: Lock,
    label: '修改密码',
    desc: '更新登录密码',
    path: '/settings/security/password',
    color: 'var(--fts-warning)',
  },
  {
    key: 'recovery',
    icon: Key,
    label: '密码找回',
    desc: '通过已绑定方式重置密码',
    path: '/settings/security/recovery',
    color: 'var(--fts-warning)',
  },
  {
    key: 'email-bind',
    icon: Message,
    label: '邮箱绑定',
    desc: '绑定邮箱以使用密码找回',
    path: '/settings/security/email-bind',
    color: 'var(--fts-info)',
  },
  {
    key: 'devices',
    icon: Monitor,
    label: '登录设备',
    desc: '查看和管理已登录设备',
    path: '/settings/security/devices',
    color: 'var(--fts-info)',
  },
  {
    key: 'logs',
    icon: Document,
    label: '登录日志',
    desc: '查看历史登录记录',
    path: '/settings/security/logs',
    color: 'var(--fts-text-secondary)',
  },
  {
    key: 'lock',
    icon: CircleCheck,
    label: '安全锁定',
    desc: '锁定屏幕保护隐私',
    path: '/settings/security/lock',
    color: 'var(--fts-success)',
  },
]

function navigateTo(path: string) {
  router.push(path)
}
</script>

<template>
  <PageContainer title="安全设置">
    <div class="security-hub">
      <p class="page-desc">管理账号安全相关设置</p>

      <nav class="menu-list">
        <button
          v-for="item in menuItems"
          :key="item.key"
          class="menu-item"
          @click="navigateTo(item.path)"
        >
          <span class="menu-icon" :style="{ background: `${item.color}14`, color: item.color }">
            <el-icon :size="18"><component :is="item.icon" /></el-icon>
          </span>
          <div class="menu-body">
            <span class="menu-label">{{ item.label }}</span>
            <span class="menu-desc">{{ item.desc }}</span>
          </div>
          <span class="menu-arrow">›</span>
        </button>
      </nav>

      <!-- 安全建议 -->
      <section class="tip-card">
        <h4 class="tip-title">安全建议</h4>
        <ul class="tip-list">
          <li>定期更换密码，避免使用简单组合</li>
          <li>绑定邮箱或手机以便密码找回</li>
          <li>不要在公共设备上保持登录状态</li>
          <li>发现异常登录时立即移除未知设备</li>
        </ul>
      </section>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.security-hub {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.page-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  margin: 0;
  line-height: 1.5;
}

/* ===== 菜单列表 ===== */
.menu-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
  background-color: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  overflow: hidden;
  box-shadow: var(--fts-shadow-xs);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: transparent;
  border: none;
  border-bottom: 1px solid var(--fts-border-secondary);
  cursor: pointer;
  transition: background-color var(--fts-duration-fast) ease;
  width: 100%;
  text-align: left;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background-color: var(--fts-bg-tertiary);
  }

  &:active {
    background-color: var(--fts-bg-secondary);
  }
}

.menu-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--fts-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.menu-label {
  font-size: var(--fts-font-size-base);
  font-weight: 500;
  color: var(--fts-text-primary);
}

.menu-desc {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);
}

.menu-arrow {
  font-size: 20px;
  color: var(--fts-text-quaternary);
  flex-shrink: 0;
  line-height: 1;
}

/* ===== 提示卡片 ===== */
.tip-card {
  background: rgba(var(--fts-primary-rgb), 0.04);
  border: 1px solid rgba(var(--fts-primary-rgb), 0.1);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-3) var(--fts-space-4);
}

.tip-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-2);
}

.tip-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);

  li {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    padding-left: var(--fts-space-3);
    position: relative;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 9px;
      width: 4px;
      height: 4px;
      border-radius: 50%;
      background-color: var(--fts-primary);
    }
  }
}
</style>
