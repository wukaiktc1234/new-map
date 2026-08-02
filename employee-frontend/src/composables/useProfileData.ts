import { computed } from 'vue'
import type { Component } from 'vue'
import { useRouter } from 'vue-router'
import {
  Edit, Bell, Switch, Lock, QuestionFilled,
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

export interface ServiceItem {
  id: string
  label: string
  icon: Component
  route?: string
  action?: () => void
  badge?: number | string
  desc?: string   // 简短描述，说明该项的独特价值
}

/**
 * 个人中心服务项设计原则：
 * - 不重复已有导航入口（审批/排班/消息等已在底部Tab栏或侧边栏）
 * - 每项提供"仅在此处可用"的个人化功能或快捷操作
 * - 分为三类：个人资料、工作偏好、账户工具
 */
const MOCK_SERVICE_ITEMS: ServiceItem[] = [
  // --- 个人资料（快捷编辑，无需跳转完整设置页） ---
  {
    id: 'profile-edit', label: '个人资料', icon: Edit,
    desc: '修改头像、姓名、联系方式',
    route: '/settings/account',
  },
  // --- 工作偏好（员工端独有，非导航重复） ---
  {
    id: 'notify-pref', label: '通知偏好', icon: Bell,
    desc: '管理推送消息的接收方式',
    route: '/settings/notifications',
  },
  {
    id: 'shift-pref', label: '班次偏好', icon: Switch,
    desc: '设置可接受的班次类型',
    route: '/schedule',
  },
]

const MOCK_OTHER_ITEMS: ServiceItem[] = [
  { id: 'security', label: '安全设置', icon: Lock, route: '/settings/security' },
  { id: 'help', label: '帮助与反馈', icon: QuestionFilled, route: '/help' },
]

export function useProfileData() {
  const router = useRouter()

  const serviceItems = computed<ServiceItem[]>(() => [...MOCK_SERVICE_ITEMS])
  const otherItems = computed<ServiceItem[]>(() => [...MOCK_OTHER_ITEMS])

  function handleServiceClick(item: ServiceItem) {
    if (item.action) {
      item.action()
      return
    }
    if (item.route) {
      router.push(item.route)
      return
    }
    ElMessage.info(`「${item.label}」功能开发中`)
  }

  return {
    serviceItems,
    otherItems,
    handleServiceClick,
  }
}
