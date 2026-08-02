<script setup lang="ts">
/**
 * CreateApprovalDialog - 发起申请选择弹窗
 * 
 * 现代风格的申请类型选择器，支持分类和更好的交互反馈。
 */
import { computed } from 'vue'
import { getIconComponent, usePermissionStore } from '@/stores/permission'
import { useRouter } from 'vue-router'

interface Props {
  modelValue: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const router = useRouter()
const permission = usePermissionStore()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const approvalTypes = computed(() => permission.getVisibleApprovalTypes())

const APPROVAL_ROUTE_MAP: Record<string, string> = {
  leave: '/approval/create/leave',
  overtime: '/approval/create/overtime',
  shift_swap: '/approval/create/swap',
  reimbursement: '/approval/create/reimbursement',
  business_trip: '/approval/create/travel',
  requisition: '/approval/create/requisition',
}

function handleSelect(typeId: string) {
  visible.value = false
  const path = APPROVAL_ROUTE_MAP[typeId]
  if (path) router.push(path)
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="发起申请"
    width="min(440px, 92vw)"
    :show-close="true"
    align-center
    class="fts-modern-dialog"
  >
    <div class="approval-type-grid">
      <button
        v-for="atype in approvalTypes"
        :key="atype.id"
        class="approval-type-item"
        @click="handleSelect(atype.id)"
      >
        <div class="icon-box" :style="{ '--theme-color': atype.color }">
          <el-icon :size="24">
            <component :is="getIconComponent(atype.icon)" />
          </el-icon>
        </div>
        <span class="label">{{ atype.label }}</span>
      </button>
    </div>
    
    <template #footer>
      <div class="dialog-footer-hint">
        根据您的岗位权限，已为您展示可发起的申请类型
      </div>
    </template>
  </el-dialog>
</template>

<style lang="scss">
/* 全局样式覆盖（不使用 scoped 确保 el-dialog 内部生效） */
.fts-modern-dialog {
  border-radius: var(--fts-radius-xl) !important;
  overflow: hidden;
  
  .el-dialog__header {
    padding: var(--fts-space-5) var(--fts-space-6) var(--fts-space-2);
    margin-right: 0;
    text-align: center;
    
    .el-dialog__title {
      font-size: var(--fts-font-size-lg);
      font-weight: 700;
      color: var(--fts-text-primary);
    }
  }
  
  .el-dialog__body {
    padding: var(--fts-space-4) var(--fts-space-6) var(--fts-space-6);
  }
  
  .el-dialog__footer {
    padding: 0 var(--fts-space-6) var(--fts-space-5);
    border-top: none;
  }
}
</style>

<style scoped lang="scss">
.approval-type-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--fts-space-4);
}

.approval-type-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: var(--fts-transition-all);
  border-radius: var(--fts-radius-lg);

  &:hover {
    background: var(--fts-bg-secondary);
    transform: translateY(-2px);
    
    .icon-box {
      transform: scale(1.1);
      box-shadow: 0 8px 16px rgba(var(--fts-primary-rgb), 0.1);
    }
  }

  &:active {
    transform: scale(0.96);
  }
}

.icon-box {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--fts-radius-md);
  background: color-mix(in srgb, var(--theme-color) 12%, transparent);
  color: var(--theme-color);
  transition: var(--fts-transition-transform);
}

.label {
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-primary);
}

.dialog-footer-hint {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  text-align: center;
}

@media (max-width: 480px) {
  .approval-type-grid {
    gap: var(--fts-space-3);
  }
  
  .icon-box {
    width: 48px;
    height: 48px;
  }
}
</style>
