<script setup lang="ts">
/**
 * AppealCreatePage - 申诉/举报创建页
 *
 * 支持两种模式：
 * - penalty: 处罚申诉
 * - complaint: 投诉举报
 *
 * 通过路由参数 :type 区分模式
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Warning, ChatDotSquare, Document, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { PageContainer, StatusTag } from '@/components/core'
import { appealApi, type AppealCreatePayload, type AppealType } from '@/api/appeal'

const route = useRoute()
const router = useRouter()

/** 当前申诉类型 */
const appealType = computed<AppealType>(() => {
  const type = route.params.type as AppealType
  if (type === 'penalty' || type === 'complaint') return type
  return 'penalty' // 默认
})

/** 页面标题和描述 */
const pageInfo = computed(() => {
  if (appealType.value === 'penalty') {
    return {
      title: '处罚申诉',
      desc: '对处罚决定有异议时，可在此提交申诉',
      icon: Warning,
      color: 'var(--fts-warning)',
    }
  }
  return {
    title: '投诉举报',
    desc: '发现违规行为或不公正待遇，可在此提交举报',
    icon: ChatDotSquare,
    color: 'var(--fts-error)',
  }
})

const submitting = ref(false)
const submitError = ref<string | null>(null)
const form = reactive({
  anonymousFlag: false as boolean,
  title: '',
  description: '',
  targetDecisionId: '',
  expectedResult: '',
})

/** 文件上传列表（Mock） */
const fileList = ref<string[]>([])

function handleFileChange() {
  // Mock：仅做UI展示，不实际上传
  fileList.value.push(`附件_${fileList.value.length + 1}.jpg`)
}

function removeFile(index: number) {
  fileList.value.splice(index, 1)
}

/** 表单校验 */
function validateForm(): boolean {
  if (!form.title.trim()) {
    ElMessage.warning('请输入标题')
    return false
  }
  if (form.title.length > 50) {
    ElMessage.warning('标题不超过50字')
    return false
  }
  if (!form.description.trim()) {
    ElMessage.warning('请输入详细描述')
    return false
  }
  if (form.description.length > 500) {
    ElMessage.warning('描述不超过500字')
    return false
  }
  if (appealType.value === 'penalty' && !form.targetDecisionId.trim()) {
    ElMessage.warning('请输入关联处罚单号')
    return false
  }
  return true
}

/** 提交申诉 */
async function handleSubmit() {
  if (!validateForm()) return

  submitting.value = true
  submitError.value = null
  try {
    const payload: AppealCreatePayload = {
      type: appealType.value,
      anonymousFlag: form.anonymousFlag,
      title: form.title.trim(),
      description: form.description.trim(),
      targetDecisionId: form.targetDecisionId.trim() || undefined,
      expectedResult: form.expectedResult.trim() || undefined,
    }

    await appealApi.create(payload)
    ElMessage.success('申诉已提交，可在"我的申诉"中查看进度')
    router.push('/appeals')
  } catch (e: unknown) {
    submitError.value = e instanceof Error ? e.message : '提交失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <PageContainer :title="pageInfo.title">
    <div class="appeal-create-page">
      <!-- 类型标识卡片 -->
      <section class="type-banner" :style="{ borderColor: pageInfo.color }">
        <div class="type-icon" :style="{ background: `${pageInfo.color}14`, color: pageInfo.color }">
          <el-icon :size="24"><component :is="pageInfo.icon" /></el-icon>
        </div>
        <div class="type-info">
          <h3 class="type-title">{{ pageInfo.title }}</h3>
          <p class="type-desc">{{ pageInfo.desc }}</p>
        </div>
      </section>

      <!-- 独立受理提示 -->
      <section class="independent-notice">
        <span class="notice-icon">i</span>
        <span class="notice-text">您的申诉将直接提交至独立受理部门，直属领导无法查看</span>
      </section>

      <!-- 匿名选项 -->
      <section class="form-section">
        <div class="form-row form-row--between">
          <label class="form-label">匿名提交</label>
          <el-switch v-model="form.anonymousFlag" active-text="是" inactive-text="否" />
        </div>
        <p class="form-hint">
          {{ form.anonymousFlag ? '开启后，审核人员将无法看到您的身份信息' : '关闭后，审核人员可看到您的姓名和职位' }}
        </p>
      </section>

      <!-- 标题 -->
      <section class="form-section">
        <label class="form-label required">申诉标题</label>
        <el-input
          v-model="form.title"
          placeholder="请简要概括申诉内容"
          maxlength="50"
          show-word-limit
          size="large"
        />
      </section>

      <!-- 详细描述 -->
      <section class="form-section">
        <label class="form-label required">详细描述</label>
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="5"
          placeholder="请详细描述申诉原因、经过和相关证据..."
          maxlength="500"
          show-word-limit
        />
      </section>

      <!-- 关联处罚单号（仅penalty模式） -->
      <section v-if="appealType === 'penalty'" class="form-section">
        <label class="form-label">关联处罚单号</label>
        <el-input
          v-model="form.targetDecisionId"
          placeholder="请输入关联的处罚单号（选填）"
        />
      </section>

      <!-- 期望结果 -->
      <section class="form-section">
        <label class="form-label">期望结果</label>
        <el-input
          v-model="form.expectedResult"
          type="textarea"
          :rows="2"
          placeholder="您希望如何处理此事？（选填）"
          maxlength="200"
          show-word-limit
        />
      </section>

      <!-- 附件上传 -->
      <section class="form-section">
        <label class="form-label">证据附件</label>
        <div class="upload-area">
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept=".jpg,.jpeg,.png,.pdf"
            @change="handleFileChange"
          >
            <div class="upload-trigger">
              <el-icon :size="24" style="color: var(--fts-text-quaternary)"><Plus /></el-icon>
              <span class="upload-text">点击上传图片或PDF</span>
              <span class="upload-hint">支持多文件，单个不超过10MB</span>
            </div>
          </el-upload>

          <!-- 已选文件列表 -->
          <div v-if="fileList.length > 0" class="file-list">
            <div v-for="(file, index) in fileList" :key="index" class="file-item">
              <el-icon :size="16" style="color: var(--fts-primary)"><Document /></el-icon>
              <span class="file-name">{{ file }}</span>
              <button class="file-remove" @click="removeFile(index)">移除</button>
            </div>
          </div>
        </div>
      </section>

      <!-- 提交错误提示 -->
      <div v-if="submitError" class="submit-error">
        <span class="submit-error__text">{{ submitError }}</span>
      </div>

      <!-- 提交按钮 -->
      <button
        class="submit-btn"
        :disabled="submitting"
        @click="handleSubmit"
      >
        {{ submitting ? '提交中...' : '提交申诉' }}
      </button>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.appeal-create-page {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

/* ===== 类型标识 ===== */
.type-banner {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-4) var(--fts-space-5);
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  border-left: 4px solid;
  box-shadow: var(--fts-shadow-xs);
}

.type-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--fts-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.type-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.type-title {
  font-size: var(--fts-font-size-lg);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0;
}

.type-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  margin: 0;
}

/* ===== 独立受理提示 ===== */
.independent-notice {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: rgba(var(--fts-primary-rgb), 0.04);
  border: 1px solid rgba(var(--fts-primary-rgb), 0.1);
  border-radius: var(--fts-radius-sm);
}

.notice-icon {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--fts-primary);
  color: var(--fts-text-on-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-style: italic;
  font-weight: 700;
  line-height: 1;
  flex-shrink: 0;
}

.notice-text {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-primary);
  font-weight: 500;
}

/* ===== 表单区域 ===== */
.form-section {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4) var(--fts-space-5);
  box-shadow: var(--fts-shadow-xs);
}

.form-label {
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-secondary);
  margin-bottom: var(--fts-space-2);
  display: block;

  &.required::before {
    content: '*';
    color: var(--fts-error);
    margin-right: 2px;
  }
}

.form-row {
  display: flex;
  align-items: center;

  &--between {
    justify-content: space-between;
  }
}

.form-hint {
  font-size: var(--fts-font-size-2xs);
  color: var(--fts-text-quaternary);
  margin: var(--fts-space-1) 0 0;
}

/* ===== 上传区域 ===== */
.upload-area {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-1);
  padding: var(--fts-space-6) var(--fts-space-4);
  border: 1.5px dashed var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all var(--fts-duration-fast) ease;

  &:hover {
    border-color: var(--fts-primary);
    background: rgba(var(--fts-primary-rgb), 0.02);
  }
}

.upload-text {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.upload-hint {
  font-size: var(--fts-font-size-2xs);
  color: var(--fts-text-quaternary);
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.file-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
}

.file-name {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-remove {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-error);
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px 8px;

  &:hover { opacity: 0.7; }
}

/* ===== 提交按钮 ===== */
.submit-btn {
  width: 100%;
  padding: var(--fts-space-3) 0;
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  color: var(--fts-text-on-primary);
  background: var(--fts-primary);
  border: none;
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: opacity var(--fts-duration-fast) ease;

  &:hover:not(:disabled) { opacity: 0.9; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
  &:active:not(:disabled) { transform: scale(0.98); }
}

/* ===== 提交错误提示 ===== */
.submit-error {
  padding: var(--fts-space-2) var(--fts-space-4);
  background-color: rgba(var(--fts-error-rgb), 0.06);
  border: 1px solid rgba(var(--fts-error-rgb), 0.15);
  border-radius: var(--fts-radius-md);

  &__text {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-error);
  }
}
</style>
