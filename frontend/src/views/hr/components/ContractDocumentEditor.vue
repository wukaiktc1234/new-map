<script setup lang="ts">
/**
 * 合同正文编辑器组件
 * 基于 WangEditor 实现合同正文的在线编辑
 * 支持来源信息展示和版本控制
 */
import { ref, shallowRef, onBeforeUnmount, watch } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'
import { ElMessage } from 'element-plus'
import StatusTag from '@/components/core/StatusTag.vue'
import type {
  ContractDocument,
  ContractDocumentEditForm,
  DocumentSourceType,
} from '@/types/hr/contract'
import {
  DocumentSourceTypeOptions,
  DocumentSourceTypeTagMap,
} from '@/types/hr/contract'

const props = defineProps<{
  /** 合同正文文档（编辑模式时传入） */
  document: ContractDocument | null
  /** 是否只读模式 */
  readonly?: boolean
}>()

const emit = defineEmits<{
  /** 保存合同正文 */
  (e: 'save', data: ContractDocumentEditForm): void
  /** 取消编辑 */
  (e: 'cancel'): void
}>()

/* ===== WangEditor 状态 ===== */
const editorRef = shallowRef<IDomEditor>()
const htmlContent = ref('')
const editRemark = ref('')

/* ===== 编辑器配置 ===== */
const toolbarConfig: Partial<IToolbarConfig> = {
  excludeKeys: [
    'group-video', // 排除视频上传
    'fullScreen',  // 排除全屏
  ],
}

const editorConfig: Partial<IEditorConfig> = {
  placeholder: '请输入合同正文内容...',
  MENU_CONF: {},
  readOnly: props.readonly || false,
}

/* ===== 来源信息标签 ===== */
function getSourceTypeLabel(type?: DocumentSourceType): string {
  if (!type) return '-'
  return DocumentSourceTypeOptions.find(o => o.value === type)?.label || type
}

/* ===== 编辑器事件处理 ===== */
function handleCreated(editor: IDomEditor) {
  editorRef.value = editor
}

function handleChange(editor: IDomEditor) {
  htmlContent.value = editor.getHtml()
}

function handleSave() {
  if (!htmlContent.value.trim()) {
    ElMessage.warning('合同正文不能为空')
    return
  }
  if (!props.document?.contractId) {
    ElMessage.error('缺少合同ID')
    return
  }
  emit('save', {
    contractId: props.document.contractId,
    htmlContent: htmlContent.value,
    editRemark: editRemark.value,
  })
}

function handleCancel() {
  emit('cancel')
}

/* ===== 监听 document 变化，初始化编辑器内容 ===== */
watch(
  () => props.document,
  (doc) => {
    if (doc) {
      htmlContent.value = doc.htmlContent || ''
      // 等待编辑器初始化后设置内容
      setTimeout(() => {
        if (editorRef.value) {
          editorRef.value.setHtml(htmlContent.value)
        }
      }, 100)
    }
  },
  { immediate: true }
)

/* ===== 组件卸载时销毁编辑器 ===== */
onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) {
    editor.destroy()
  }
})
</script>

<template>
  <div class="contract-document-editor">
    <!-- 来源信息展示 -->
    <div v-if="document" class="source-info">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="来源类型">
          <StatusTag
            v-if="document.sourceType"
            :status="DocumentSourceTypeTagMap[document.sourceType]"
            :label="getSourceTypeLabel(document.sourceType)"
            size="small"
          />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="来源描述">
          {{ document.sourceDesc || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="版本号">
          v{{ document.version || 1 }}
        </el-descriptions-item>
        <el-descriptions-item label="创建人">
          {{ document.createdBy || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ document.createTime || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="最后修改">
          {{ document.updatedBy || '-' }} {{ document.updateTime ? `· ${document.updateTime}` : '' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="document.templateId" label="模板来源">
          {{ document.templateId }}{{ document.templateVersion ? ` (v${document.templateVersion})` : '' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="document.documentHash" label="文档Hash">
          <span class="hash-text">{{ document.documentHash }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="document.editRemark" label="修改备注">
          {{ document.editRemark }}
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- 编辑器区域 -->
    <div class="editor-container" :class="{ 'editor-readonly': readonly }">
      <Toolbar
        v-if="!readonly"
        :editor="editorRef"
        :default-config="toolbarConfig"
        mode="default"
        class="editor-toolbar"
      />
      <Editor
        v-model="htmlContent"
        :default-config="editorConfig"
        mode="default"
        class="editor-content"
        @on-created="handleCreated"
        @on-change="handleChange"
      />
    </div>

    <!-- 操作按钮 -->
    <div v-if="!readonly" class="editor-footer">
      <div class="edit-remark-input">
        <el-input
          v-model="editRemark"
          placeholder="请输入修改备注（可选）"
          size="default"
          style="width: 300px;"
        />
      </div>
      <div class="editor-actions">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleSave">保存正文</el-button>
      </div>
    </div>
  </div>
</template>

<style src="@wangeditor/editor/dist/css/style.css"></style>

<style scoped lang="scss">
.contract-document-editor {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.source-info {
  margin-bottom: var(--fts-space-2);
}

.hash-text {
  font-family: monospace;
  font-size: 12px;
  color: var(--fts-text-secondary);
  word-break: break-all;
}

.editor-container {
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-base);
  overflow: hidden;

  &.editor-readonly {
    .editor-content {
      max-height: 600px;
      overflow-y: auto;
    }
  }
}

.editor-toolbar {
  border-bottom: 1px solid var(--fts-border-secondary);
  background: var(--fts-bg-card);
}

.editor-content {
  height: 500px;
  overflow-y: auto;
  background: var(--fts-bg-card);
}

.editor-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-3) 0;
  border-top: 1px solid var(--fts-border-secondary);
}

.editor-actions {
  display: flex;
  gap: var(--fts-space-3);
}
</style>
