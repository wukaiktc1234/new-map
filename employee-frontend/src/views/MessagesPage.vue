<script setup lang="ts">
/**
 * MessagesPage - 统一消息中心
 *
 * 两级 Tab 架构，职责明确不重叠：
 *   消息 Tab    → 个人实时通知（审批结果、排班提醒、系统通知、@提及）
 *   通知公告 Tab → 企业级信息板（政策、培训、活动、节假日安排）
 * @last-modified 2026-06-04
 */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Bell, Check, Document, Clock, ChatDotRound, Warning } from '@element-plus/icons-vue'
import { ElMessage, ElDialog } from 'element-plus'
import { PageContainer, EmptyState, StatusTag, FilterTabs } from '@/components/core'
import type { FilterTabOption } from '@/components/core'
import { useMessageStore } from '@/stores/message'
import { useMessagePolling } from '@/composables/useMessagePolling'

const router = useRouter()
const route = useRoute()
const messageStore = useMessageStore()
const { isPolling, startPolling, stopPolling: stopPollingFn } = useMessagePolling(30000)

// ===== 主 Tab 分类（消息 vs 通知公告）=====
type MainTab = 'messages' | 'notices'
const activeMainTab = ref<MainTab>('messages')

/** FilterTabs 组件所需的主分类选项 */
const mainTabOptions = computed<FilterTabOption[]>(() => {
  const unreadNoticeCount = notices.value.filter(n => !n.read).length
  return [
    { value: 'messages', label: '消息' },
    { value: 'notices', label: '通知公告', count: unreadNoticeCount > 0 ? unreadNoticeCount : undefined },
  ]
})

// ===== 消息子 Tab =====
type MessageTab = 'all' | 'unread' | 'system'
const activeMessageTab = ref<MessageTab>('all')

/** 消息业务类型 */
type MessageType = 'approval' | 'schedule' | 'system' | 'mention'

interface EnhancedMessage {
  id: string
  category: string
  title: string
  content: string
  time: string
  isRead: boolean
  type?: string
  targetId?: string
  /** 业务类型：用于图标和样式区分 */
  messageType: MessageType
  /** 详情内容（可能比 summary 更完整） */
  fullContent?: string
  /** 关联操作文本，如"查看审批详情" */
  actionText?: string
  /** 关联跳转路径 */
  actionRoute?: string
}

const messageTabs = computed(() => [
  { key: 'all' as const, label: '全部', count: 0 },
  { key: 'unread' as const, label: '未读', count: messageStore.unreadCount },
  { key: 'system' as const, label: '系统通知', count: messageStore.systemNotifications.filter(m => !m.isRead).length },
])

/** FilterTabs 组件所需的消息子分类选项 */
const msgFilterOptions = computed<FilterTabOption[]>(() =>
  messageTabs.value.map(tab => ({ value: tab.key, label: tab.label, count: tab.count }))
)

/** 将 Store 消息增强为带业务类型的消息 */
const enhancedMessages = computed<EnhancedMessage[]>(() => {
  const list = messageStore.currentMessages
  return list.map(msg => enrichMessage(msg))
})

const filteredMessages = computed(() => {
  const list = enhancedMessages.value
  if (activeMessageTab.value === 'all') return list
  if (activeMessageTab.value === 'unread') return list.filter((m) => !m.isRead)
  return list
})

/**
 * 根据消息标题和内容推断业务类型
 */
function enrichMessage(msg: { id: string; category: string; title: string; content: string; time: string; isRead: boolean; type?: string; targetId?: string }): EnhancedMessage {
  const title = msg.title.toLowerCase()
  const content = (msg.content || '').toLowerCase()

  let messageType: MessageType = 'system'
  let actionText: string | undefined
  let actionRoute: string | undefined
  let fullContent: string | undefined

  // 审批类消息
  if (title.includes('审批') || title.includes('通过') || title.includes('驳回') || title.includes('请假') || title.includes('加班') || title.includes('换班') || title.includes('出差') || title.includes('报销')) {
    messageType = 'approval'
    actionText = '查看审批详情'
    if (msg.targetId) {
      actionRoute = `/approval/detail/${msg.targetId}`
    }
    fullContent = msg.content || `${msg.title}的详细内容请前往审批模块查看。`
  }
  // 排班类消息
  else if (title.includes('排班') || title.includes('班次') || title.includes('值班') || title.includes('调休') || content.includes('排班') || content.includes('班次')) {
    messageType = 'schedule'
    actionText = '查看排班'
    actionRoute = '/schedule'
    fullContent = msg.content || '您的排班信息已更新，请查看最新排班表。'
  }
  // @提及
  else if (title.includes('@') || msg.category === 'mention') {
    messageType = 'mention'
    fullContent = msg.content || msg.title
  }

  return {
    ...msg,
    messageType,
    fullContent: fullContent || msg.content,
    actionText,
    actionRoute,
  }
}

/** 获取消息类型对应的图标组件 */
function getMessageIcon(type: MessageType) {
  switch (type) {
    case 'approval': return Document
    case 'schedule': return Clock
    case 'mention': return ChatDotRound
    default: return Bell
  }
}

/** 获取消息类型对应的图标背景色 */
function getMessageIconBg(type: MessageType): string {
  switch (type) {
    case 'approval': return 'var(--fts-success)'
    case 'schedule': return 'var(--fts-warning)'
    case 'mention': return 'var(--fts-primary)'
    default: return 'var(--fts-text-quaternary)'
  }
}

// ===== 通知公告数据（从 NoticesPage 迁移）=====
interface NoticeItem {
  id: number
  title: string
  date: string
  type: string
  read: boolean
  important: boolean
  department: string
  content: string
}

type NoticeTab = 'all' | 'unread' | 'important'
const activeNoticeTab = ref<NoticeTab>('all')
const selectedNotice = ref<NoticeItem | null>(null)
const showNoticeDetail = ref(false)

// 通知公告数据（后续可替换为 API 调用）
const notices = ref<NoticeItem[]>([
  { id: 1, title: '关于夏季营业时间调整的通知', date: '05-23', type: '通知', read: false, important: true, department: '行政部', content: '根据季节变化，自6月1日起，门店营业时间调整为早9:00至晚10:00。请各位同事知悉并做好排班调整。' },
  { id: 2, title: '5月员工生日会活动安排', date: '05-22', type: '活动', read: false, important: false, department: '人事部', content: '5月员工生日会将于5月28日下午3点在会议室举行，届时将有蛋糕和互动游戏，欢迎5月寿星参加！' },
  { id: 3, title: '新菜品培训材料已上传', date: '05-20', type: '培训', read: true, important: false, department: '培训部', content: '本月新菜品培训材料已上传至内部学习平台，请所有后厨人员在本周内完成学习并通过考核。' },
  { id: 4, title: '门店卫生检查结果公示', date: '05-18', type: '公告', read: true, important: true, department: '品控部', content: '本周门店卫生检查评分：后厨区域92分，前厅区域88分。请针对扣分项进行整改，整改期限为3个工作日。' },
  { id: 5, title: '端午节放假安排通知', date: '05-15', type: '通知', read: true, important: true, department: '行政部', content: '端午节放假时间为5月31日至6月2日，共3天。请各部门提前安排好值班人员，确保门店正常运营。' },
])

const noticeTabs = computed(() => [
  { key: 'all' as NoticeTab, label: '全部', count: notices.value.length },
  { key: 'unread' as NoticeTab, label: '未读', count: notices.value.filter(n => !n.read).length },
  { key: 'important' as NoticeTab, label: '重要', count: notices.value.filter(n => n.important).length },
])

/** FilterTabs 组件所需的通知公告子分类选项 */
const noticeFilterOptions = computed<FilterTabOption[]>(() =>
  noticeTabs.value.map(tab => ({ value: tab.key, label: tab.label, count: tab.count }))
)

const filteredNotices = computed(() => {
  if (activeNoticeTab.value === 'all') return notices.value
  if (activeNoticeTab.value === 'unread') return notices.value.filter(n => !n.read)
  if (activeNoticeTab.value === 'important') return notices.value.filter(n => n.important)
  return notices.value
})

const noticeColorMap: Record<string, string> = {
  通知: 'var(--fts-notice-info-bg)',
  活动: 'var(--fts-notice-success-bg)',
  培训: 'var(--fts-notice-warning-bg)',
  公告: 'var(--fts-notice-default-bg)',
}

function getNoticeTypeColor(type: string): string {
  return noticeColorMap[type] || 'var(--fts-notice-default-bg)'
}

// ===== 下拉刷新 =====
const isRefreshing = ref(false)

async function handleRefresh() {
  if (isRefreshing.value) return
  isRefreshing.value = true
  try {
    await messageStore.fetchMessages()
  } finally {
    isRefreshing.value = false
  }
}

// ===== 消息详情弹窗 =====
const selectedMessage = ref<EnhancedMessage | null>(null)
const showMessageDetail = ref(false)

function handleMessageClick(msg: EnhancedMessage) {
  if (!msg.isRead) {
    messageStore.markRead(msg.id)
  }
  selectedMessage.value = msg
  showMessageDetail.value = true
}

function handleMessageAction(msg: EnhancedMessage) {
  showMessageDetail.value = false
  if (msg.actionRoute) {
    router.push(msg.actionRoute)
  }
}

function handleNoticeClick(notice: NoticeItem) {
  if (!notice.read) {
    notice.read = true
  }
  selectedNotice.value = notice
  showNoticeDetail.value = true
}

function handleMarkAllRead() {
  messageStore.markAllRead()
}

function markAllNoticesRead() {
  notices.value.forEach(n => { n.read = true })
  ElMessage.success('已全部标记为已读')
}

// ===== URL 同步（支持从 /notices 重定向过来）=====
watch(() => route.query.tab, (newTab) => {
  if (newTab === 'notices') {
    activeMainTab.value = 'notices'
  }
}, { immediate: true })

onMounted(() => {
  startPolling(() => messageStore.fetchMessages())
})

onUnmounted(() => {
  stopPollingFn()
})
</script>

<template>
  <PageContainer title="消息中心">
    <!-- 主 Tab：消息 / 通知公告 -->
    <FilterTabs v-model="activeMainTab" :options="mainTabOptions" variant="segment" size="large" class="main-tabs" />

    <!-- ========== 消息 Tab 内容 ========== -->
    <section v-show="activeMainTab === 'messages'" class="tab-content" aria-label="消息列表">
      <!-- 消息子分类 Tab + 操作栏 -->
      <div class="message-toolbar">
        <FilterTabs v-model="activeMessageTab" :options="msgFilterOptions" variant="pill" size="small" />
        <button
          v-if="filteredMessages.some(m => !m.isRead)"
          class="mark-all-read-btn"
          @click="handleMarkAllRead"
        >
          <el-icon :size="12"><Check /></el-icon>
          全部标为已读
        </button>
      </div>

      <!-- 下拉刷新指示器 -->
      <div v-if="isRefreshing" class="refresh-indicator">
        <span>正在刷新...</span>
      </div>

      <!-- 消息列表 -->
      <div v-if="filteredMessages.length > 0" class="message-list">
        <div
          v-for="msg in filteredMessages"
          :key="msg.id"
          :class="['message-item', { 'message-item--unread': !msg.isRead }]"
          tabindex="0"
          @click="handleMessageClick(msg)"
          @keydown.enter="handleMessageClick(msg)"
        >
          <!-- 未读蓝色竖线 -->
          <div v-if="!msg.isRead" class="message-indicator"></div>

          <!-- 类型图标 -->
          <div
            class="message-avatar"
            :style="{ background: getMessageIconBg(msg.messageType) }"
          >
            <el-icon :size="18" color="var(--fts-text-on-primary)">
              <component :is="getMessageIcon(msg.messageType)" />
            </el-icon>
          </div>

          <!-- 内容区 -->
          <div class="message-content">
            <div class="message-header">
              <span :class="['message-title', { 'message-title--unread': !msg.isRead }]">
                {{ msg.title }}
              </span>
              <span class="message-time">{{ msg.time }}</span>
            </div>
            <p v-if="msg.content" class="message-summary">{{ msg.content }}</p>
          </div>

          <!-- 未读蓝点 -->
          <div v-if="!msg.isRead" class="message-dot"></div>
        </div>
      </div>

      <!-- 空状态 -->
      <EmptyState
        v-else
        title="暂无消息"
        description="您目前没有任何消息通知"
        :action-text="activeMessageTab !== 'all' ? '查看全部消息' : ''"
        @action="activeMessageTab = 'all'"
      />
    </section>

    <!-- ========== 通知公告 Tab 内容 ========== -->
    <section v-show="activeMainTab === 'notices'" class="tab-content" aria-label="通知公告列表">
      <!-- 公告子分类 Tab -->
      <FilterTabs v-model="activeNoticeTab" :options="noticeFilterOptions" variant="pill" size="small" class="notice-tabs" />

      <!-- 通知列表 -->
      <section v-if="filteredNotices.length > 0" class="notice-list" aria-label="通知公告列表">
        <div
          v-for="notice in filteredNotices"
          :key="notice.id"
          :class="['notice-item', { 'notice-item--unread': !notice.read }]"
          tabindex="0"
          @click="handleNoticeClick(notice)"
          @keydown.enter="handleNoticeClick(notice)"
        >
          <div class="notice-dot" :class="{ 'notice-dot--unread': !notice.read }"></div>
          <div class="notice-body">
            <div class="notice-top">
              <span
                class="notice-tag"
                :style="{ background: getNoticeTypeColor(notice.type) }"
              >
                {{ notice.type }}
              </span>
              <span v-if="notice.important" class="notice-important">重要</span>
              <span class="notice-date">{{ notice.date }}</span>
            </div>
            <h4 class="notice-title">{{ notice.title }}</h4>
            <p class="notice-summary">{{ notice.content }}</p>
          </div>
        </div>
      </section>

      <!-- 空状态 -->
      <EmptyState
        v-else
        title="暂无通知"
        description="当前分类下没有通知消息"
      />
    </section>

    <!-- 通知详情弹窗 -->
    <Teleport to="body">
      <div v-if="showNoticeDetail && selectedNotice" class="notice-detail-overlay" @click.self="showNoticeDetail = false">
        <div class="notice-detail-dialog">
          <div class="notice-detail-header">
            <h3 class="notice-detail-title">{{ selectedNotice.title }}</h3>
            <button class="notice-detail-close" @click="showNoticeDetail = false" aria-label="关闭"></button>
          </div>
          <div class="notice-detail-meta">
            <span
              class="notice-tag"
              :style="{ background: getNoticeTypeColor(selectedNotice.type) }"
            >
              {{ selectedNotice.type }}
            </span>
            <span v-if="selectedNotice.important" class="notice-important">重要</span>
            <span class="notice-detail-dept">{{ selectedNotice.department }}</span>
            <span class="notice-detail-time">{{ selectedNotice.date }}</span>
          </div>
          <div class="notice-detail-content">{{ selectedNotice.content }}</div>
        </div>
      </div>

      <!-- 消息详情弹窗 -->
      <div v-if="showMessageDetail && selectedMessage" class="notice-detail-overlay" @click.self="showMessageDetail = false">
        <div class="notice-detail-dialog">
          <div class="notice-detail-header">
            <h3 class="notice-detail-title">{{ selectedMessage.title }}</h3>
            <button class="notice-detail-close" @click="showMessageDetail = false" aria-label="关闭"></button>
          </div>
          <div class="notice-detail-meta">
            <StatusTag
              :status="selectedMessage.messageType"
              :label="{
                approval: '审批通知',
                schedule: '排班提醒',
                mention: '@提及',
                system: '系统通知',
              }[selectedMessage.messageType]"
              variant="badge"
              size="small"
            />
            <span class="notice-detail-time">{{ selectedMessage.time }}</span>
          </div>
          <div class="notice-detail-content">{{ selectedMessage.fullContent || selectedMessage.content }}</div>
          <!-- 关联操作按钮 -->
          <div v-if="selectedMessage.actionText" class="notice-detail-actions">
            <button class="action-btn action-btn--primary" @click="handleMessageAction(selectedMessage)">
              {{ selectedMessage.actionText }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </PageContainer>
</template>

<style scoped lang="scss">
/* ================================================================
 * MessagesPage 样式
 *
 * 响应式断点规范（项目统一）：
 *   768px  — 移动端通用布局切换（平板竖屏/手机横屏）
 *   480px  — 小屏手机网格/字体缩减
 * ================================================================ */

// ========== 主 Tab 样式 ==========
.main-tabs {
  margin-bottom: var(--fts-space-4);
  display: flex;

  :deep(.ft) {
    flex: 1;
  }
}

// ========== 标记已读按钮 ==========
.message-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--fts-space-4);
}

.mark-all-read-btn {
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-primary);
  background: transparent;
  border: 1px solid var(--fts-border-hover);
  padding: var(--fts-space-2) var(--fts-space-4);
  border-radius: var(--fts-radius-full);
  cursor: pointer;
  transition: all var(--fts-duration-fast) ease;

  &:hover {
    background: rgba(var(--fts-primary-rgb), 0.06);
    border-color: var(--fts-primary);
  }

  &:active {
    transform: scale(0.96);
  }
}

// ========== 通知公告子分类 Tab ==========
.notice-tabs {
  margin-bottom: var(--fts-space-4);
}

// ========== 刷新指示器 ==========
.refresh-indicator {
  text-align: center;
  padding: var(--fts-space-2) 0;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

// ========== 消息列表 ==========
.message-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.message-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  position: relative;
  transition: all var(--fts-duration-fast) ease;

  &:hover {
    background: var(--fts-bg-hover);
    transform: translateX(2px);
  }

  &--unread {
    background: var(--fts-primary-lighter);

    &:hover { background: var(--fts-primary-light); }
  }
}

.message-indicator {
  width: 3px;
  height: calc(var(--fts-app-icon-size, 36px) - 4px);
  background: var(--fts-primary);
  border-radius: 0 2px 2px 0;
  flex-shrink: 0;
}

.message-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--fts-radius-full);
  background: var(--fts-primary);
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-1);
}

.message-title {
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &--unread { font-weight: var(--fts-font-weight-semibold); }
}

.message-time {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  flex-shrink: 0;
}

.message-summary {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--fts-primary);
  flex-shrink: 0;
  animation: dot-pulse 2s ease-in-out infinite;
}

@keyframes dot-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

// ========== 通知公告列表 ==========
.notice-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.notice-item {
  display: flex;
  gap: var(--fts-space-4);
  padding: 10px 14px;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: border-color var(--fts-duration-fast) var(--fts-easing-default),
              box-shadow var(--fts-duration-fast) var(--fts-easing-default),
              background-color var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    border-color: var(--fts-border-hover);
    box-shadow: var(--fts-shadow-xs);
    background: var(--fts-bg-hover);
  }

  &:focus-visible {
    outline: 2px solid var(--fts-primary);
    outline-offset: -2px;
  }

  &:active {
    transform: scale(0.99);
  }

  &--unread {
    border-color: rgba(var(--fts-primary-rgb), 0.25);
    background: rgba(var(--fts-primary-rgb), 0.03);

    &:hover {
      border-color: rgba(var(--fts-primary-rgb), 0.35);
    }
  }
}

.notice-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--fts-border-primary); flex-shrink: 0;
  margin-top: 5px;

  &--unread {
    width: 8px; height: 8px;
    background: var(--fts-primary);
    box-shadow: 0 0 4px rgba(var(--fts-primary-rgb), 0.35);
  }
}

.notice-body {
  flex: 1;
  min-width: 0;
}

.notice-top {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-2);
}

.notice-tag {
  font-size: var(--fts-font-size-xs); font-weight: 600;
  padding: 2px var(--fts-space-2);
  border-radius: var(--fts-radius-sm);
  flex-shrink: 0;
  color: var(--fts-text-on-primary);
}

.notice-important {
  font-size: var(--fts-font-size-xs);
  font-weight: 600;
  color: var(--fts-error);
  padding: 1px var(--fts-space-1);
  border-radius: var(--fts-radius-sm);
  background: rgba(var(--fts-error-rgb, 245, 108, 108), 0.1);
  flex-shrink: 0;
}

.notice-date {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);
  flex-shrink: 0;
  margin-left: auto;
}

.notice-title {
  font-size: var(--fts-font-size-base); font-weight: 600;
  color: var(--fts-text-primary); margin: 0 0 var(--fts-space-1);

  .notice-item--unread & {
    font-weight: 700;
  }
}

.notice-summary {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// ========== 通知详情弹窗 ==========
.notice-detail-overlay {
  position: fixed;
  inset: 0;
  background: var(--fts-overlay-backdrop);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: var(--fts-space-4);
  animation: overlay-fade-in 0.2s ease;
  backdrop-filter: blur(4px);
}

@keyframes overlay-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

.notice-detail-dialog {
  width: 100%;
  max-width: 520px;
  max-height: 80vh;
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-xl);
  box-shadow:
    0 20px 60px var(--fts-shadow-heavy),
    0 8px 24px var(--fts-shadow-medium);
  overflow-y: auto;
  overflow-x: hidden;
  animation: dialog-scale-in 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes dialog-scale-in {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.notice-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-5) var(--fts-space-5) var(--fts-space-4);
  border-bottom: 1px solid var(--fts-border-secondary);
  position: sticky;
  top: 0;
  background: var(--fts-bg-card);
  z-index: 1;
}

.notice-detail-title {
  margin: 0;
  font-size: var(--fts-font-size-lg);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
  flex: 1;
  margin-right: var(--fts-space-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-detail-close {
  width: 32px;
  height: 32px;
  border: none;
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-full);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--fts-duration-fast) ease;
  flex-shrink: 0;

  &::before {
    content: '';
    width: 14px;
    height: 2px;
    background: var(--fts-text-secondary);
    position: absolute;
    transform: rotate(45deg);
    transition: background var(--fts-duration-fast) ease;
  }

  &::after {
    content: '';
    width: 14px;
    height: 2px;
    background: var(--fts-text-secondary);
    position: absolute;
    transform: rotate(-45deg);
    transition: background var(--fts-duration-fast) ease;
  }

  &:hover {
    background: var(--fts-bg-hover);

    &::before,
    &::after {
      background: var(--fts-error);
    }
  }

  &:active {
    transform: scale(0.9);
  }
}

.notice-detail-meta {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-5);
  flex-wrap: wrap;
  border-bottom: 1px solid var(--fts-bg-fill);
}

.notice-detail-dept {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.notice-detail-time {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-quaternary);
  margin-left: auto;
}

.notice-detail-content {
  padding: var(--fts-space-5) var(--fts-space-5) var(--fts-space-6);
  font-size: var(--fts-font-size-base);
  color: var(--fts-text-primary);
  line-height: 1.8;
  white-space: pre-wrap;
}

// ========== 详情弹窗操作按钮 ==========
.notice-detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--fts-space-3);
  padding: 0 var(--fts-space-5) var(--fts-space-5);
  border-top: 1px solid var(--fts-bg-fill);
}

.action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--fts-space-2) var(--fts-space-5);
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all var(--fts-duration-fast) ease;
  border: 1px solid transparent;

  &--primary {
    color: var(--fts-text-on-primary);
    background: var(--fts-primary);

    &:hover {
      opacity: 0.9;
    }

    &:active {
      transform: scale(0.97);
    }
  }
}
</style>
