<script setup lang="ts">
/**
 * NotificationSettingsPage - 通知偏好设置页面
 * 管理推送消息的接收方式和渠道偏好
 */
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Bell, ChatDotSquare, AlarmClock, Promotion } from '@element-plus/icons-vue'
import { PageContainer } from '@/components/core'

interface NotificationChannel {
  key: string
  label: string
  // eslint-disable-next-line @typescript-eslint/no-explicit-any -- Element Plus 图标组件类型（ICON_MAP 返回值）
  icon: any
  desc: string
  enabled: boolean
}

const loading = ref(false)
const saving = ref(false)

const channels = ref<NotificationChannel[]>([
  {
    key: 'approval',
    label: '审批通知',
    icon: Bell,
    desc: '审批申请、审批结果、催办提醒',
    enabled: true,
  },
  {
    key: 'message',
    label: '消息通知',
    icon: ChatDotSquare,
    desc: '系统消息、公告、团队通知',
    enabled: true,
  },
  {
    key: 'schedule',
    label: '排班提醒',
    icon: AlarmClock,
    desc: '排班变更、班次提醒、打卡提醒',
    enabled: true,
  },
  {
    key: 'activity',
    label: '活动推送',
    icon: Promotion,
    desc: '培训活动、公司活动、福利通知',
    enabled: false,
  },
])

async function fetchSettings() {
  loading.value = true
  try {
    // Mock: 从 localStorage 读取保存的设置
    const saved = localStorage.getItem('notification_prefs')
    if (saved) {
      const prefs = JSON.parse(saved) as Record<string, boolean>
      channels.value.forEach((ch) => {
        if (prefs[ch.key] !== undefined) {
          ch.enabled = prefs[ch.key]
        }
      })
    }
  } catch {
    // 使用默认设置
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  saving.value = true
  try {
    const prefs: Record<string, boolean> = {}
    channels.value.forEach((ch) => {
      prefs[ch.key] = ch.enabled
    })
    localStorage.setItem('notification_prefs', JSON.stringify(prefs))
    ElMessage.success('通知偏好已保存')
  } catch {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchSettings()
})
</script>

<template>
  <PageContainer title="通知偏好">
    <template #headerActions>
      <el-button type="primary" size="small" :loading="saving" @click="saveSettings">
        保存设置
      </el-button>
    </template>

    <div v-loading="loading" class="notify-settings">
      <p class="notify-settings__desc">
        选择您希望接收的通知类型，关闭后仍可在应用内查看对应消息
      </p>

      <div
        v-for="channel in channels"
        :key="channel.key"
        class="notify-settings__item"
      >
        <div class="notify-settings__item-left">
          <el-icon :size="20"><component :is="channel.icon" /></el-icon>
        </div>
        <div class="notify-settings__item-body">
          <span class="notify-settings__item-label">{{ channel.label }}</span>
          <span class="notify-settings__item-desc">{{ channel.desc }}</span>
        </div>
        <div class="notify-settings__item-right">
          <el-switch v-model="channel.enabled" />
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<style scoped>
.notify-settings {
  background: var(--fts-bg-card, #fff);
  border-radius: var(--fts-radius-lg, 12px);
  padding: var(--fts-space-4, 16px);
}

.notify-settings__desc {
  color: var(--fts-text-secondary);
  font-size: 13px;
  margin: 0 0 var(--fts-space-4, 16px);
  padding-bottom: var(--fts-space-4, 16px);
  border-bottom: 1px solid var(--fts-border-light);
}

.notify-settings__item {
  display: flex;
  align-items: center;
  padding: var(--fts-space-3, 12px) 0;
  border-bottom: 1px solid var(--fts-border-lighter);
}

.notify-settings__item:last-child {
  border-bottom: none;
}

.notify-settings__item-left {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--fts-radius-md, 8px);
  background: var(--fts-bg-secondary);
  color: var(--fts-primary);
  margin-right: var(--fts-space-3, 12px);
  flex-shrink: 0;
}

.notify-settings__item-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.notify-settings__item-label {
  font-size: 14px;
  color: var(--fts-text-primary);
  font-weight: 500;
}

.notify-settings__item-desc {
  font-size: 12px;
  color: var(--fts-text-tertiary);
}

.notify-settings__item-right {
  flex-shrink: 0;
  margin-left: var(--fts-space-3, 12px);
}
</style>