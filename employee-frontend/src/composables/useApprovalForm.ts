/**
 * useApprovalForm - 审批发起表单通用逻辑 Composable
 *
 * 统一管理：
 * - 紧急程度状态
 * - 提交/取消流程
 * - 通用选项数据
 */
import { ref, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { approvalApi } from '@/api'
import type { ApprovalType, UrgencyLevel, AnyApprovalFormData } from '@/types/approval'
import { UrgencyOptions } from '@/types/approval'

/** 页面过渡动画时长（需与 App.vue page-slide-leave-duration 保持一致） */
const PAGE_LEAVE_DURATION = 200

/** 智能默认日期工具函数 */
function getDefaultDate(offsetDays: number): string {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  return date.toISOString().slice(0, 10)
}

export interface UseApprovalFormOptions {
  /** 当前审批类型 */
  type: ApprovalType
  /** 获取表单数据的函数（由各表单组件提供） */
  getFormData: () => AnyApprovalFormData
  /** 表单验证函数（由各表单组件提供） */
  validate: () => boolean
}

export function useApprovalForm(options: UseApprovalFormOptions) {
  const router = useRouter()

  // ========== 响应式状态 ==========
  const urgency = ref<UrgencyLevel>('normal')
  const submitting = ref(false)

  // ========== 智能默认日期 ==========
  const defaultDates = computed(() => ({
    /** 加班日期：今天 */
    today: getDefaultDate(0),
    /** 请假开始：明天 */
    tomorrow: getDefaultDate(1),
    /** 出差开始：后天 */
    dayAfterTomorrow: getDefaultDate(2),
    /** 领用期望：下周同一天 */
    nextWeek: getDefaultDate(7),
  }))

  // ========== 提交处理（等待页面离开动画完成后跳转） ==========
  async function handleSubmit(): Promise<boolean> {
    if (!options.validate()) {
      return false
    }
    submitting.value = true
    try {
      await approvalApi.create({
        type: options.type,
        urgency: urgency.value,
        fields: options.getFormData() as unknown as Record<string, unknown>,
      })

      ElMessage.success('提交成功')
      await navigateWithTransition('/approval')
      return true
    } catch {
      ElMessage.error('提交失败，请重试')
      return false
    } finally {
      submitting.value = false
    }
  }

  // ========== 取消处理（等待页面离开动画完成后跳转） ==========
  async function handleCancel(): Promise<void> {
    await navigateWithTransition('/approval')
  }

  /**
   * 延迟导航：等待当前帧渲染 + 页面离开动画完成后再跳转
   * 解决 mode="out-in" 下页面瞬间消失的问题
   */
  async function navigateWithTransition(path: string): Promise<void> {
    await nextTick()
    return new Promise((resolve) => {
      setTimeout(() => {
        router.push(path)
        resolve()
      }, PAGE_LEAVE_DURATION)
    })
  }

  return {
    /** 紧急程度 */
    urgency,
    /** 是否正在提交 */
    submitting,
    /** 紧急程度选项 */
    urgencyOptions: UrgencyOptions,
    /** 智能默认日期 */
    defaultDates,
    /** 提交处理 */
    handleSubmit,
    /** 取消处理 */
    handleCancel,
  }
}
