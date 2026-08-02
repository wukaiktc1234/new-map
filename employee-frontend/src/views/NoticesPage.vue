<script setup lang="ts">
import { ref, computed } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { EmptyState, PageContainer } from '@/components/core'

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

const activeTab = ref<NoticeTab>('all')
const selectedNotice = ref<NoticeItem | null>(null)
const showDetail = ref(false)

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

const filteredNotices = computed(() => {
  if (activeTab.value === 'all') return notices.value
  if (activeTab.value === 'unread') return notices.value.filter(n => !n.read)
  if (activeTab.value === 'important') return notices.value.filter(n => n.important)
  return notices.value
})

const noticeColorMap: Record<string, string> = {
  通知: 'var(--fts-notice-info-bg)',
  活动: 'var(--fts-notice-success-bg)',
  培训: 'var(--fts-notice-warning-bg)',
  公告: 'var(--fts-notice-default-bg)',
}

function getTypeColor(type: string): string {
  return noticeColorMap[type] || 'var(--fts-notice-default-bg)'
}

function handleNoticeClick(notice: NoticeItem) {
  if (!notice.read) {
    notice.read = true
  }
  selectedNotice.value = notice
  showDetail.value = true
}

function markAllRead() {
  notices.value.forEach(n => { n.read = true })
  ElMessage.success('已全部标记为已读')
}
</script>

<template>
  <PageContainer title="通知消息">
    <template #headerActions>
      <button class="mark-read-btn" @click="markAllRead">全部标为已读</button>
    </template>

    <!-- 分类Tab -->
    <section class="page-section">
      <div class="notice-tabs">
        <button
          v-for="tab in noticeTabs"
          :key="tab.key"
          :class="['notice-tab', { 'notice-tab--active': activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <span v-if="tab.count > 0" class="notice-tab__count">{{ tab.count }}</span>
        </button>
      </div>
    </section>

    <!-- 通知列表 -->
    <section class="page-section">
      <div v-if="filteredNotices.length > 0" class="notice-list">
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
                :style="{ background: getTypeColor(notice.type) }"
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
      </div>

      <EmptyState
        v-else
        icon="📭"
        title="暂无通知"
        description="当前分类下没有通知消息"
      />
    </section>

    <!-- 通知详情弹窗 -->
    <el-dialog
      v-model="showDetail"
      :title="selectedNotice?.title"
      width="90%"
      class="notice-dialog"
      :style="{ '--dialog-max-width': '500px' }"
    >
      <div v-if="selectedNotice" class="notice-detail">
        <div class="notice-detail__meta">
          <span
            class="notice-tag"
            :style="{ background: getTypeColor(selectedNotice.type) }"
          >
            {{ selectedNotice.type }}
          </span>
          <span v-if="selectedNotice.important" class="notice-important">重要</span>
          <span class="notice-detail__dept">{{ selectedNotice.department }}</span>
          <span class="notice-detail__time">{{ selectedNotice.date }}</span>
        </div>
        <div class="notice-detail__content">{{ selectedNotice.content }}</div>
      </div>
    </el-dialog>
  </PageContainer>
</template>

<style scoped lang="scss">
.notices-page {
  padding-bottom: 0;
}

.page-section {
  margin-bottom: var(--fts-space-section-gap, 20px);

  &:last-child {
    margin-bottom: 0;
  }
}

// ========== 标记已读按钮 ==========
.mark-read-btn {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-primary);
  background: none;
  border: none;
  cursor: pointer;
  padding: var(--fts-space-2) var(--fts-space-3);
  border-radius: var(--fts-radius-md);
  transition: all var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    opacity: 0.75;
    background: rgba(var(--fts-primary-rgb), 0.06);
  }

  &:active {
    transform: scale(0.96);
  }
}

// ========== 分类Tab ==========
.notice-tabs {
  display: flex;
  gap: var(--fts-space-2);
}

.notice-tab {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-1);
  padding: var(--fts-space-2) var(--fts-space-4);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-lg);
  cursor: pointer;
  transition: all var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    color: var(--fts-primary);
    border-color: var(--fts-primary);
    background: rgba(var(--fts-primary-rgb), 0.04);
  }

  &--active {
    color: var(--fts-text-on-primary);
    background: var(--fts-primary);
    border-color: var(--fts-primary);

    &:hover {
      color: var(--fts-text-on-primary);
      background: var(--fts-primary);
      opacity: 0.9;
    }

    .notice-tab__count {
      background: var(--fts-overlay-medium);
      color: var(--fts-text-on-primary);
    }
  }

  &__count {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    font-size: var(--fts-font-size-xs);
    font-weight: 600;
    color: var(--fts-text-quaternary);
    background: var(--fts-bg-hover);
    border-radius: 9px;
  }
}

// ========== 通知列表 ==========
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
.notice-detail {
  &__meta {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    margin-bottom: var(--fts-space-4);
    flex-wrap: wrap;
  }

  &__dept {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
  }

  &__time {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-quaternary);
    margin-left: auto;
  }

  &__content {
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    line-height: 1.8;
    white-space: pre-wrap;
  }
}
</style>

<style lang="scss">
.notice-dialog {
  .el-dialog {
    max-width: var(--dialog-max-width, 500px);
  }
}
</style>
