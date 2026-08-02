<script setup lang="ts">
/**
 * ApprovalComments - 审批意见列表
 * 展示审批过程中的评论和意见
 */
import type { Comment } from '@/types/approval'

defineProps<{
  comments: Comment[]
}>()
</script>

<template>
  <section v-if="comments && comments.length > 0" class="detail-section">
    <h3 class="ds-title">审批意见</h3>
    <div class="comments-list">
      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="comment-header">
          <span class="comment-user">{{ comment.userName }}</span>
          <span class="comment-time">{{ comment.createdAt.slice(5, 16).replace('T', ' ') }}</span>
        </div>
        <p class="comment-content">{{ comment.content }}</p>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
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

// ========== 审批意见 ==========
.comments-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.comment-item {
  padding: var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-1);
}

.comment-user {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.comment-time {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.comment-content {
  margin: 0;
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  line-height: 1.6;
}
</style>
