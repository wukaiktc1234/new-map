<script setup lang="ts">
/**
 * DeviceListPage - 设备管理页
 *
 * 从 SecuritySettingsPage 提取的设备管理功能
 * 展示登录设备列表，支持移除非当前设备
 */
import { ref, onMounted } from 'vue'
import { Monitor } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { PageContainer, EmptyState } from '@/components/core'

/** 登录设备信息 */
interface LoginDevice {
  id: string
  device: string       // 浏览器/操作系统描述
  location: string     // IP归属地
  ip: string           // IP地址
  time: string         // 登录时间
  current: boolean     // 是否当前设备
}

const loginDevices = ref<LoginDevice[]>([])

/** 从浏览器环境采集当前设备信息 */
function detectCurrentDevice(): string {
  const ua = navigator.userAgent
  let browser = '未知浏览器'
  let os = '未知系统'

  // 浏览器检测
  if (ua.includes('Chrome') && !ua.includes('Edg')) browser = 'Chrome'
  else if (ua.includes('Edg')) browser = 'Edge'
  else if (ua.includes('Firefox')) browser = 'Firefox'
  else if (ua.includes('Safari') && !ua.includes('Chrome')) browser = 'Safari'
  else if (ua.includes('MicroMessenger')) browser = '微信内置浏览器'

  // 操作系统检测
  if (ua.includes('Windows NT 10')) os = 'Windows 10/11'
  else if (ua.includes('Windows NT 6.3')) os = 'Windows 8.1'
  else if (ua.includes('Windows NT 6.1')) os = 'Windows 7'
  else if (ua.includes('Mac OS X')) os = 'macOS'
  else if (ua.includes('Android')) os = 'Android'
  else if (ua.includes('iPhone') || ua.includes('iPad')) os = 'iOS'

  return `${browser} / ${os}`
}

/** 格式化当前时间 */
function formatNow(): string {
  return new Date().toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: undefined,
  })
}

/** 初始化设备列表：从localStorage加载历史 + 添加当前设备 */
onMounted(() => {
  const stored = localStorage.getItem('fts_login_devices')
  const historyDevices: LoginDevice[] = stored ? JSON.parse(stored) : []

  // 采集当前设备信息
  const currentDeviceStr = detectCurrentDevice()
  const now = formatNow()

  // 查找是否已有当前设备记录
  const existingIndex = historyDevices.findIndex(d => d.device === currentDeviceStr && d.current)

  if (existingIndex >= 0) {
    // 更新已有设备的登录时间
    historyDevices[existingIndex].time = now
  } else {
    // 将之前的current标记取消
    historyDevices.forEach(d => { d.current = false })
    // 添加新设备
    historyDevices.unshift({
      id: `device_${Date.now()}`,
      device: currentDeviceStr,
      location: '本机登录',
      ip: '本地网络',
      time: now,
      current: true,
    })
  }

  // 只保留最近10条记录
  loginDevices.value = historyDevices.slice(0, 10)
  // 持久化到localStorage
  localStorage.setItem('fts_login_devices', JSON.stringify(loginDevices.value))
})

/** 移除设备（带二次确认） */
async function handleRemoveDevice(device: LoginDevice) {
  try {
    await ElMessageBox.confirm(
      `确定要移除 ${device.device} 吗？移除后该设备需要重新登录`,
      '确认移除',
      {
        confirmButtonText: '移除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    loginDevices.value = loginDevices.value.filter(d => d.id !== device.id)
    // 同步更新localStorage
    localStorage.setItem('fts_login_devices', JSON.stringify(loginDevices.value))
    ElMessage.success('已移除该设备')
  } catch {
    // 用户取消
  }
}
</script>

<template>
  <PageContainer title="常用设备">
    <div class="device-list-page">
      <!-- 说明文字 -->
      <p class="page-desc">以下设备曾登录过您的账号，如发现异常请及时移除</p>

      <!-- 空状态 -->
      <EmptyState
        v-if="loginDevices.length === 0"
        title="暂无设备记录"
        description="登录后会在此显示设备信息"
      />

      <!-- 设备列表 -->
      <section v-else class="device-section">
        <div class="device-count">
          共 {{ loginDevices.length }} 台设备
        </div>
        <ul class="device-list">
          <li
            v-for="device in loginDevices"
            :key="device.id"
            class="device-item"
          >
            <div class="device-left">
              <span class="device-icon-wrap" :class="{ 'device-icon-wrap--current': device.current }">
                <el-icon :size="16" :color="device.current ? 'var(--fts-primary)' : 'var(--fts-text-tertiary)'">
                  <Monitor />
                </el-icon>
              </span>
              <div class="device-info">
                <span class="device-name">{{ device.device }}</span>
                <span class="device-meta">
                  {{ device.location }} · {{ device.ip }} · {{ device.time }}
                </span>
              </div>
            </div>
            <div class="device-right">
              <span v-if="device.current" class="device-current-tag">当前设备</span>
              <button
                v-else
                class="device-remove-btn"
                @click="handleRemoveDevice(device)"
              >
                移除
              </button>
            </div>
          </li>
        </ul>
      </section>

      <!-- 安全提示 -->
      <section class="tips-section">
        <p class="tips-title">安全提醒</p>
        <ul class="tips-list">
          <li>如果发现未知设备，请立即修改密码并移除该设备</li>
          <li>建议定期检查登录设备列表，确保账号安全</li>
        </ul>
      </section>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.device-list-page {
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

/* ===== 设备列表区 ===== */
.device-section {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4) var(--fts-space-5);
  box-shadow: var(--fts-shadow-xs);
}

.device-count {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);
  margin-bottom: var(--fts-space-3);
}

.device-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.device-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-3) 0;
  transition: background-color var(--fts-duration-fast) ease;

  &:not(:last-child) {
    border-bottom: 1px solid var(--fts-border-secondary);
  }

  &:active {
    background-color: var(--fts-bg-hover);
    border-radius: var(--fts-radius-sm);
  }
}

.device-left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex: 1;
  min-width: 0;
}

.device-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: var(--fts-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--fts-bg-secondary);
  flex-shrink: 0;

  &--current {
    background: rgba(var(--fts-primary-rgb), 0.1);
  }
}

.device-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.device-name {
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-meta {
  font-size: var(--fts-font-size-2xs);
  color: var(--fts-text-quaternary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-right {
  flex-shrink: 0;
  margin-left: var(--fts-space-3);
}

.device-current-tag {
  font-size: var(--fts-font-size-2xs);
  color: var(--fts-primary);
  font-weight: 600;
  padding: 2px 10px;
  background: rgba(var(--fts-primary-rgb), 0.08);
  border-radius: var(--fts-radius-full);
}

.device-remove-btn {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-error);
  background: transparent;
  border: 1px solid var(--fts-error-lighter);
  padding: 4px 12px;
  border-radius: var(--fts-radius-sm);
  cursor: pointer;
  transition: all var(--fts-duration-fast) ease;

  &:hover {
    background: var(--fts-error-lighter);
  }

  &:active {
    transform: scale(0.96);
  }
}

/* ===== 安全提示 ===== */
.tips-section {
  background: rgba(var(--fts-primary-rgb), 0.04);
  border: 1px solid rgba(var(--fts-primary-rgb), 0.1);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4) var(--fts-space-5);
}

.tips-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-primary);
  margin: 0 0 var(--fts-space-2);
}

.tips-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
}

.tips-list li {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  padding-left: var(--fts-space-3);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 7px;
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background-color: var(--fts-primary);
  }
}
</style>
