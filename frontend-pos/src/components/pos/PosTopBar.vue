<template>
  <div class="pos-top-bar">
    <div class="store-info">
      <div class="pos-logo">
        <div class="pos-logo-icon">
          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M3 9L12 2L21 9V20C21 20.5304 20.7893 21.0391 20.4142 21.4142C20.0391 21.7893 19.5304 22 19 22H5C4.46957 22 3.96086 21.7893 3.58579 21.4142C3.21071 21.0391 3 20.5304 3 20V9Z" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M9 22V12H15V22" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="pos-logo-text">
          <h1>快餐收银系统</h1>
        </div>
      </div>
      <div class="time-display">
        <span class="time">{{ currentTime.split(' ')[1] }}</span>
        <span class="date-sep">|</span>
        <span class="date">{{ currentTime.split(' ')[0] }}</span>
      </div>
    </div>

    <div class="right-section">
      <div class="quick-actions">
        <!-- 收银核心操作 -->
        <button class="action-btn hold-order-btn" title="挂单" @click="$emit('hold')">
          <el-icon><Document /></el-icon>
          <span>挂单</span>
        </button>

        <el-badge :value="heldCount" :hidden="heldCount === 0" :max="99">
          <button class="action-btn retrieve-order-btn" title="取单" @click="$emit('retrieve')">
            <el-icon><FolderOpened /></el-icon>
            <span>取单</span>
          </button>
        </el-badge>

        <!-- 叫号系统入口 -->
        <el-badge :value="callNumberPendingCount" :hidden="callNumberPendingCount === 0" type="danger" :max="99">
          <button class="action-btn call-number-btn" title="叫号" @click="$emit('callNumber')">
            <el-icon><Bell /></el-icon>
            <span>叫号</span>
          </button>
        </el-badge>

        <!-- 高频辅助操作 -->
        <button class="action-btn coupon-btn" title="券码核销" @click="$emit('coupon')">
          <el-icon><Ticket /></el-icon>
          <span>券码</span>
        </button>

        <button class="action-btn query-btn" title="订单查询" @click="$emit('orderQuery')">
          <el-icon><Search /></el-icon>
          <span>订单</span>
        </button>

        <!-- 桌台管理 -->
        <button class="action-btn table-btn" title="桌台管理" @click="$emit('tables')">
          <el-icon><Grid /></el-icon>
          <span>桌台</span>
        </button>

        <!-- YOLO 辅助收银（多商品快速结算） -->
        <button class="action-btn yolo-btn" @click="$emit('yolo')" title="YOLO 视觉识别辅助收银">
          <el-icon><View /></el-icon>
          <span>视觉识别</span>
        </button>

        <!-- 交班 -->
        <button class="action-btn shift-handover-btn" title="交班日结" @click="$emit('shiftHandover')">
          <el-icon><SwitchButton /></el-icon>
          <span>交班</span>
        </button>

        <!-- 主题切换 -->
        <button class="action-btn theme-btn" :title="isDark ? '切换浅色' : '切换深色'" @click="$emit('toggleTheme')">
          <el-icon v-if="isDark"><Sunny /></el-icon>
          <el-icon v-else><Moon /></el-icon>
        </button>
      </div>

      <!-- 当前登录账号：头像即入口，点开是下拉菜单（用户信息 / 修改密码 / 偏好设置 / 退出登录） -->
      <div class="account-area">
        <el-popover
          v-model:visible="accountMenuVisible"
          placement="bottom-end"
          :width="300"
          trigger="click"
          :show-arrow="false"
          :offset="10"
          :hide-after="0"
          popper-class="pos-account-popper"
          data-test="account-popover"
        >
          <template #reference>
            <button
              type="button"
              class="account-chip"
              :class="{ 'is-open': accountMenuVisible }"
              :title="accountTooltip"
              aria-haspopup="menu"
              :aria-expanded="accountMenuVisible"
              data-test="account-chip"
            >
              <span class="account-avatar">{{ avatarText }}</span>
              <span class="account-meta">
                <span class="account-name">{{ accountName }}</span>
                <span v-if="accountCode" class="account-code">{{ accountCode }}</span>
              </span>
              <el-icon class="account-caret" :class="{ 'is-open': accountMenuVisible }"><ArrowDown /></el-icon>
            </button>
          </template>

          <div class="account-menu" role="menu" data-test="account-menu">
            <!-- 用户信息 -->
            <div class="account-menu__head">
              <span class="account-menu__avatar">{{ avatarText }}</span>
              <span class="account-menu__identity">
                <span class="account-menu__name">{{ accountName }}</span>
                <span class="account-menu__sub">
                  {{ accountCode || '未绑定工号' }}
                  <template v-if="accountRoleText"> · {{ accountRoleText.replace('角色：', '') }}</template>
                </span>
                <span class="account-menu__terminal" :title="terminalTip">
                  <el-icon><Monitor /></el-icon>
                  {{ terminalName }}
                </span>
              </span>
            </div>

            <div class="account-menu__divider" />

            <!-- 构建标记：刷新后据此确认是否拿到最新前端包（排查"改了没生效"用） -->
            <div class="account-menu__build" data-test="build-stamp" :title="BUILD_STAMP">
              前端版本 {{ BUILD_STAMP }}
            </div>

            <button
              type="button"
              class="account-menu__item"
              role="menuitem"
              data-test="account-menu-password"
              @click="openChangePassword"
            >
              <el-icon><Lock /></el-icon>
              <span>修改密码</span>
            </button>

            <button
              type="button"
              class="account-menu__item"
              role="menuitem"
              data-test="account-menu-preferences"
              @click="openPreferences"
            >
              <el-icon><Setting /></el-icon>
              <span>偏好设置</span>
            </button>

            <div class="account-menu__divider" />

            <button
              type="button"
              class="account-menu__item account-menu__item--danger"
              role="menuitem"
              :disabled="loggingOut"
              data-test="account-menu-logout"
              @click="handleLogout"
            >
              <el-icon v-if="!loggingOut"><SwitchButton /></el-icon>
              <el-icon v-else class="is-loading"><Loading /></el-icon>
              <span>{{ loggingOut ? '退出中…' : '退出登录' }}</span>
            </button>
          </div>
        </el-popover>
      </div>
    </div>

    <!-- 修改密码 -->
    <ChangePasswordDialog v-model="showChangePassword" @success="handlePasswordChanged" />
    <!-- 偏好设置 -->
    <PreferenceDialog v-model="showPreferences" :is-dark="isDark" @toggle-theme="$emit('toggleTheme')" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clearCapabilities } from '@/composables/usePermission'
import axios from 'axios'
import {
  Document,
  FolderOpened,
  Search,
  Ticket,
  Bell,
  Grid,
  SwitchButton,
  Sunny,
  Moon,
  View,
  Loading,
  ArrowDown,
  Lock,
  Setting,
  Monitor
} from '@element-plus/icons-vue'
import ChangePasswordDialog from '@/components/pos/dialogs/ChangePasswordDialog.vue'
import PreferenceDialog from '@/components/pos/dialogs/PreferenceDialog.vue'
import { terminalDisplayName, terminalTooltip } from '@/utils/terminal'
import { clearPosRuntimeCache } from '@/utils/posCache'

interface Props {
  currentTime: string
  heldCount: number
  callNumberPendingCount: number
  isDark: boolean
}

defineProps<Props>()

defineEmits<{
  (e: 'hold'): void
  (e: 'retrieve'): void
  (e: 'orderQuery'): void
  (e: 'coupon'): void
  (e: 'callNumber'): void
  (e: 'tables'): void
  (e: 'yolo'): void
  (e: 'shiftHandover'): void
  (e: 'toggleTheme'): void
}>()

/**
 * 后端 PosAuthServiceImpl.login 实际返回的 userInfo 字段：
 *   userId / employeeId / employeeName / terminalId
 * 另外兼容历史与其它环境可能出现的 fullName / name / username / roleName / roles。
 */
interface PosUserInfo {
  userId?: string | number
  employeeId?: string
  employeeName?: string
  fullName?: string
  name?: string
  username?: string
  roleName?: string
  roles?: string[] | string
  terminalId?: string
}

/** 构建标记：每次前端改动递增，用于确认浏览器加载到的是最新包 */
const BUILD_STAMP = 'r12.2026-10-28'
console.info('[POS] 前端版本', BUILD_STAMP)

const router = useRouter()
const posUser = ref<PosUserInfo | null>(null)
const loggingOut = ref(false)
const accountMenuVisible = ref(false)
const showChangePassword = ref(false)
const showPreferences = ref(false)
/** 终端展示名（脱敏）：「前台收银机A」，不展示 pos-localhost-1793xxx */
const terminalName = ref('未绑定终端')
const terminalTip = ref('')

/** 从 localStorage 读取当前登录账号（Login.vue 登录成功时写入 pos-user） */
function readPosUser(): void {
  try {
    const raw = localStorage.getItem('pos-user')
    posUser.value = raw ? (JSON.parse(raw) as PosUserInfo) : null
  } catch {
    posUser.value = null
  }
  const terminal = localStorage.getItem('pos-terminal') || posUser.value?.terminalId || ''
  terminalName.value = terminalDisplayName(terminal)
  terminalTip.value = terminalTooltip(terminal)
}

/** 兜底顺序：姓名 → 工号 → 用户名 */
const accountName = computed<string>(() => {
  const user = posUser.value
  if (!user) return '未登录'
  const candidates = [user.employeeName, user.fullName, user.name, user.employeeId, user.username]
  for (const candidate of candidates) {
    const text = typeof candidate === 'string' ? candidate.trim() : ''
    if (text) return text
  }
  return '未登录'
})

/** 小字工号（与主名称重复时不展示） */
const accountCode = computed<string>(() => {
  const user = posUser.value
  if (!user) return ''
  const code = (user.employeeId || user.username || '').trim()
  if (!code || code === accountName.value) return ''
  return `工号 ${code}`
})

/** 角色文本（hover / 菜单头部展示；后端 PosAuthController 当前未返回角色，缺失时不显示占位） */
const accountRoleText = computed<string>(() => {
  const user = posUser.value
  if (!user) return '未登录或登录信息已失效'
  const roles = user.roles
  const roleText = user.roleName
    ? user.roleName
    : Array.isArray(roles)
      ? roles.join(' / ')
      : (roles || '')
  return roleText ? `角色：${roleText}` : ''
})

const accountTooltip = computed<string>(() => {
  if (!posUser.value) return '未登录或登录信息已失效'
  return [accountName.value, accountRoleText.value, accountCode.value].filter(Boolean).join(' · ')
})

/** 头像圆点文字：姓名首字 */
const avatarText = computed<string>(() => {
  const name = accountName.value
  if (!name || name === '未登录') return '?'
  return name.slice(0, 1).toUpperCase()
})

/** 打开修改密码（先收起菜单，避免弹窗与菜单层级打架） */
function openChangePassword(): void {
  accountMenuVisible.value = false
  showChangePassword.value = true
}

/** 打开偏好设置 */
function openPreferences(): void {
  accountMenuVisible.value = false
  showPreferences.value = true
}

/**
 * 退出登录：二次确认 → 清理本班次缓存 → 尽力调用后端登出 → 回登录页
 * 二次确认不可跳过（收银场景误触代价太高）：误点退出会中断当前购物车与班次归属。
 */
async function handleLogout(): Promise<void> {
  if (loggingOut.value) return
  accountMenuVisible.value = false

  try {
    await ElMessageBox.confirm(
      '退出后将清理本机登录凭证与当前购物车草稿（挂单会保留），需重新登录才能继续收银。',
      '确认退出登录？',
      {
        confirmButtonText: '退出登录',
        cancelButtonText: '继续收银',
        type: 'warning',
        distinguishCancelAndClose: true,
        closeOnClickModal: false
      }
    )
  } catch {
    // 用户取消 / 关闭弹窗
    return
  }

  loggingOut.value = true
  try {
    const token = localStorage.getItem('pos-token')
    if (token) {
      // 真实端点：POST /v1/auth/logout（**不在 /v1/pos/auth 下**）。
      // 历史误传的 /v1/pos/auth/logout 后端无此 mapping，会返回 HTTP 200 + body.code=404「资源不存在」，
      // 前端若按 HTTP 状态判成功就会误以为已登出。本文件自始至终只调 /v1/auth/logout。
      // 失败也继续本地清理（离线/后端未启动时同样能退出）。
      try {
        await axios.post('/api/v1/auth/logout', null, {
          headers: { Authorization: `Bearer ${token}` },
          timeout: 5000
        })
      } catch {
        // 忽略后端登出失败
      }
    }
  } finally {
    // 清理凭证 + 开班标记 + 购物车/客显等运行态缓存（挂单与个人偏好保留）
    clearPosRuntimeCache()
    clearCapabilities()
    posUser.value = null
    loggingOut.value = false
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}

/** 修改密码成功：旧 token 已不可信，确认后强制重新登录 */
async function handlePasswordChanged(): Promise<void> {
  try {
    await ElMessageBox.confirm('密码已修改，需要用新密码重新登录。是否立即重新登录？', '修改成功', {
      confirmButtonText: '立即重新登录',
      cancelButtonText: '稍后',
      type: 'success'
    })
  } catch {
    return
  }
  clearPosRuntimeCache()
  router.push('/login')
}

/** 其它标签页登录/登出后同步账号信息 */
function handleStorageChange(event: StorageEvent): void {
  if (!event.key || event.key === 'pos-user' || event.key === 'pos-terminal') {
    readPosUser()
  }
}

onMounted(() => {
  readPosUser()
  window.addEventListener('storage', handleStorageChange)
})

onUnmounted(() => {
  window.removeEventListener('storage', handleStorageChange)
})
</script>

<style scoped>
.pos-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--pos-spacing-sm, 16px);
  padding: var(--pos-spacing-xs, 8px) var(--pos-spacing-md, 24px);
  min-height: 64px;
  background: var(--pos-glass-bg, rgba(255, 255, 255, 0.8));
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--pos-border-color, #e5e7eb);
}

.store-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.pos-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pos-logo-icon {
  width: 40px;
  height: 40px;
  background: var(--pos-primary-gradient, linear-gradient(135deg, #e3651c, #ef7f3a));
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.pos-logo-icon svg {
  width: 24px;
  height: 24px;
}

.pos-logo-text h1 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.3px;
  white-space: nowrap;
  color: var(--pos-text-primary, #1f2937);
}

.time-display {
  display: flex;
  align-items: center;
  gap: var(--pos-spacing-xs, 8px);
  padding-left: var(--pos-spacing-sm, 16px);
  border-left: 1px solid var(--pos-border-color, #e5e7eb);
  white-space: nowrap;
}

.time-display .time {
  font-size: 18px;
  font-weight: 600;
  color: var(--pos-text-primary, #1f2937);
  font-family: 'Courier New', monospace;
  letter-spacing: 0.5px;
}

.time-display .date-sep {
  color: #d4d4d8;
  font-size: 14px;
}

.time-display .date {
  font-size: 13px;
  color: var(--pos-text-muted, #6b7280);
  white-space: nowrap;
}

.right-section {
  display: flex;
  align-items: center;
  gap: var(--pos-spacing-sm, 16px);
  min-width: 0;
  flex: 1 1 auto;
  justify-content: flex-end;
}

.quick-actions {
  display: flex;
  align-items: center;
  gap: var(--pos-spacing-xs, 8px);
  flex-wrap: nowrap;
  min-width: 0;
  /* 允许按钮区被压缩而不是把账号区挤出屏幕 */
  flex: 0 1 auto;
  /* 不能 hidden：会把 el-badge 的角标（悬在按钮右上角）裁掉一半 */
  overflow: visible;
}

/* 角标改为"按钮内右上角"：既不会被顶栏上边缘裁断，也不占额外宽度 */
.quick-actions .el-badge {
  display: inline-flex;
  position: relative;
}

.quick-actions .el-badge__content.is-fixed {
  top: 5px;
  right: 6px;
  transform: none;
  border: none;
  box-shadow: 0 0 0 2px var(--pos-bg-primary, #ffffff);
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 44px;
  padding: 0 var(--pos-spacing-sm, 16px);
  border-radius: var(--pos-radius-sm, 8px);
  border: 1px solid var(--pos-border-color, #e5e7eb);
  background: var(--pos-bg-primary, #ffffff);
  color: var(--pos-text-secondary, #64748b);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color var(--pos-transition-fast, 150ms ease),
    border-color var(--pos-transition-fast, 150ms ease),
    color var(--pos-transition-fast, 150ms ease),
    transform var(--pos-transition-fast, 150ms ease);
  white-space: nowrap;
  user-select: none;
}

.action-btn:hover {
  background: var(--pos-primary-soft, #fbf1ea);
  border-color: var(--pos-primary, #c9622a);
  color: var(--pos-primary, #c9622a) !important;
}

.action-btn:active {
  transform: scale(0.97);
}

.action-btn:disabled {
  cursor: not-allowed;
  opacity: 0.7;
  transform: none;
}

.action-btn .el-icon {
  font-size: 16px;
}

/* 顶栏统一"中性按钮 + 单一主行动色"：
 * 之前 8 个按钮各有蓝/青/绿/紫/黄描边，颜色越多越显廉价；
 * 现在只有「叫号」是填充主色（最高频、需要最醒目），其余走中性描边。
 */
.call-number-btn {
  background: var(--pos-primary, #c9622a) !important;
  border-color: var(--pos-primary, #c9622a) !important;
  color: #ffffff !important;
  box-shadow: 0 4px 12px rgba(201, 98, 42, 0.24);
}

.call-number-btn:hover {
  background: var(--pos-primary-dark, #a94f1f) !important;
  border-color: var(--pos-primary-dark, #a94f1f) !important;
  color: #ffffff !important;
  box-shadow: 0 6px 16px rgba(201, 98, 42, 0.3) !important;
}

.theme-btn {
  width: 44px;
  padding: 0;
  min-width: 44px;
}

.theme-btn .el-icon {
  font-size: 18px;
}

/* ====== 当前账号（头像 = 下拉菜单入口） ====== */
.account-area {
  display: flex;
  align-items: center;
  gap: var(--pos-spacing-xs, 8px);
  padding-left: var(--pos-spacing-sm, 16px);
  border-left: 1px solid var(--pos-border-color, #e5e7eb);
  flex-shrink: 0;
}

.account-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 220px;
  min-height: 44px;
  padding: 4px 10px 4px 6px;
  border-radius: var(--pos-radius-md, 12px);
  border: 1px solid var(--pos-border-color, #e5e7eb);
  background: var(--pos-bg-secondary, #f9fafb);
  cursor: pointer;
  transition: all var(--pos-transition-fast, 150ms ease);
}

.account-chip:hover,
.account-chip.is-open {
  border-color: var(--pos-primary, #c9622a);
  background: var(--pos-primary-soft, #fbf1ea);
}

.account-avatar {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: var(--pos-text-inverse, #ffffff);
  background: var(--pos-primary-gradient, linear-gradient(135deg, #e3651c, #ef7f3a));
}

.account-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.2;
  align-items: flex-start;
}

.account-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--pos-text-primary, #1f2937);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}

.account-code {
  font-size: 11px;
  color: var(--pos-text-muted, #6b7280);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}

.account-caret {
  font-size: 12px;
  color: var(--pos-text-muted, #94a3b8);
  transition: transform var(--pos-transition-fast, 150ms ease);
}

.account-caret.is-open {
  transform: rotate(180deg);
  color: var(--pos-primary, #e3651c);
}

/* ====== 下拉菜单（Popover 内容区） ====== */
.account-menu {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.account-menu__head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 4px 4px 10px;
}

.account-menu__avatar {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
  background: var(--pos-primary-gradient, linear-gradient(135deg, #e3651c, #ef7f3a));
}

.account-menu__identity {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.account-menu__name {
  font-size: 15px;
  font-weight: 700;
  color: var(--pos-text-primary, #1f2937);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.account-menu__sub {
  font-size: 12px;
  color: var(--pos-text-secondary, #64748b);
}

.account-menu__terminal {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
  padding: 2px 8px;
  align-self: flex-start;
  border-radius: var(--pos-radius-full, 9999px);
  background: var(--pos-bg-tertiary, #f1f5f9);
  font-size: 11px;
  font-weight: 600;
  color: var(--pos-text-secondary, #64748b);
  cursor: help;
}

.account-menu__divider {
  height: 1px;
  margin: 4px 0;
  background: var(--pos-border-light, #f1f5f9);
}

.account-menu__item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 0 10px;
  border: none;
  border-radius: var(--pos-radius-sm, 8px);
  background: transparent;
  color: var(--pos-text-primary, #1f2937);
  font-size: 14px;
  font-weight: 500;
  text-align: left;
  cursor: pointer;
  transition: background-color var(--pos-transition-fast, 150ms ease);
}

.account-menu__item:hover {
  background: var(--pos-bg-tertiary, #f1f5f9);
  color: var(--pos-primary, #e3651c);
}

.account-menu__item .el-icon {
  font-size: 16px;
}

.account-menu__item--danger {
  color: var(--pos-danger, #dc2626);
}

.account-menu__item--danger:hover {
  background: #fef2f2;
  color: #b91c1c;
}

.account-menu__item:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.account-menu__build {
  padding: 4px 14px 6px;
  font-size: 11px;
  color: var(--pos-text-muted, #94a3b8);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.2px;
}

/* ========== 响应式：顶栏必须"永不被裁" ==========
 * 110% 缩放会让可用宽度缩到 1/1.1，1280 窗口只剩 1164px；
 * 旧版所有按钮都带文字，总宽 1370px → 右侧账号区被裁掉、菜单被截断。
 * 断点策略：① 先隐藏按钮文字（保留图标 + title 提示）② 再隐藏低频入口。
 */
@media (max-width: 1780px) {
  .account-chip {
    max-width: 150px;
  }

  .action-btn {
    padding: 0 10px;
    min-width: 44px;
    gap: 0;
  }

  .action-btn span:not(.el-icon) {
    display: none;
  }

  .quick-actions {
    gap: 6px;
  }
}

@media (max-width: 1120px) {
  .account-chip__meta {
    display: none;
  }

  /* 低频入口在窄屏收起，保证核心操作（挂单/取单/叫号/订单/结账）不被挤掉 */
  .table-btn,
  .yolo-btn,
  .coupon-btn {
    display: none !important;
  }
}

@media (max-width: 1200px) {
  .quick-actions {
    gap: 6px;
  }

  .action-btn {
    padding: 0 10px;
    font-size: 13px;
  }

  .action-btn span:not(.el-icon) {
    display: none;
  }

  .hold-order-btn span:not(.el-icon),
  .retrieve-order-btn span:not(.el-icon),
  .call-number-btn span:not(.el-icon),
  .coupon-btn span:not(.el-icon),
  .query-btn span:not(.el-icon) {
    display: inline;
  }

  .right-section {
    gap: 10px;
  }

  .account-area {
    padding-left: 10px;
  }

  .account-chip {
    padding: 4px;
  }

  .account-meta,
  .account-caret {
    display: none;
  }
}
</style>
