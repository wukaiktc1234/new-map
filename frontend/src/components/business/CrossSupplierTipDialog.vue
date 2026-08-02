<script setup lang="ts">
/**
 * 供应商一致性提示对话框（跨供应商选料警示）
 *
 * 当订单表头供应商与明细物料主供应商不一致且未开启跨供应商时弹出，
 * 说明系统已自动改回，并引导用户勾选「允许跨供应商选料」。
 */
import { ref } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'

const visible = ref(false)
const mismatchCount = ref(0)
const primarySupplierName = ref('')
const selectedSupplierName = ref('')

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

function open(params: { mismatchCount: number; primarySupplierName: string; selectedSupplierName: string }): void {
  mismatchCount.value = params.mismatchCount
  primarySupplierName.value = params.primarySupplierName
  selectedSupplierName.value = params.selectedSupplierName
  visible.value = true
}

defineExpose({ open })
</script>

<template>
  <el-dialog
    :model-value="visible"
    width="480px"
    class="cross-supplier-tip"
    :show-close="false"
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="tip-body">
      <div class="tip-icon">
        <el-icon :size="40"><WarningFilled /></el-icon>
      </div>
      <div class="tip-content">
        <div class="tip-title">供应商一致性提示</div>
        <div class="tip-desc">
          当前已选明细中有 <strong>{{ mismatchCount }}</strong> 项物料的主供应商为「{{ primarySupplierName }}」，
          与所选供应商「{{ selectedSupplierName }}」不一致。
        </div>
        <div class="tip-action">
          未开启「允许跨供应商选料」时，订单供应商已自动改回与明细一致；
          如需混用多家供应商，请先勾选「允许跨供应商选料」。
        </div>
      </div>
    </div>
    <template #footer>
      <el-button type="primary" @click="visible = false">知道了</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.tip-body {
  display: flex;
  gap: var(--fts-space-4);
  align-items: flex-start;
  padding: var(--fts-space-2) 0;
}

.tip-icon {
  flex-shrink: 0;
  color: var(--fts-warning);
  line-height: 1;
}

.tip-content {
  flex: 1;
}

.tip-title {
  font-size: var(--fts-font-size-lg);
  font-weight: var(--fts-font-weight-bold);
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-2);
}

.tip-desc {
  font-size: var(--fts-font-size-base);
  color: var(--fts-text-primary);
  line-height: 1.6;
}

.tip-action {
  margin-top: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-left: 3px solid var(--fts-warning);
  border-radius: var(--fts-radius-base);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  line-height: 1.6;
}
</style>
