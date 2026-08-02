<template>
  <div class="approval-tabs">
    <el-tabs :key="tabsKey" v-model="activeTabValue" @tab-change="handleTabChange">
      <el-tab-pane
        v-for="tab in tabList"
        :key="tab.value"
        :label="tab.label"
        :name="tab.value"
      />
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
/**
 * ApprovalTabs - 审批中心Tab栏组件
 *
 * 根据用户层级（userLevel）动态渲染不同的Tab选项：
 * - L1(员工): 只有 "我发起的" + "已完成"
 * - L2+(主管): "我发起的" + "待我审批" + "已完成"
 * - L3+(经理): 额外增加 "部门汇总" Tab (只读统计)
 */
import { computed, watch } from 'vue'
import type { UserLevel } from '@/types/permission'

interface TabOption {
  value: string
  label: string
}

/** 所有可用Tab定义 */
const ALL_TABS: TabOption[] = [
  { value: 'pending', label: '需要我确认' },
  { value: 'initiated', label: '我发起的' },
  { value: 'completed', label: '已完成' },
]

/** 部门汇总Tab（仅L3+可见） */
const SUMMARY_TAB: TabOption = { value: 'summary', label: '部门汇总' }

/** 无审批权限时的Tab列表（L1员工） */
const STAFF_ONLY_TABS: TabOption[] = [
  { value: 'initiated', label: '我发起的' },
  { value: 'completed', label: '已完成' },
]

interface Props {
  /** 当前激活的Tab */
  activeTab: string
  /** 是否有审批权限（L2+） */
  canApprove?: boolean
  /** 是否显示部门汇总Tab（L3+） */
  showSummary?: boolean
  /** 用户层级（用于日志和未来扩展） */
  userLevel?: number
}

const props = withDefaults(defineProps<Props>(), {
  canApprove: false,
  showSummary: false,
  userLevel: 1,
})

const emit = defineEmits<{
  (e: 'update:activeTab', tab: string): void
}>()

/** 根据权限和层级过滤后的Tab列表 */
const tabList = computed<TabOption[]>(() => {
  // L1 员工：只有"我发起的"和"已完成"
  if (!props.canApprove) {
    return STAFF_ONLY_TABS
  }
  
  // L2+ 有审批权限的用户
  const tabs = [...ALL_TABS]
  
  // L3+ 额外显示"部门汇总"
  if (props.showSummary) {
    tabs.push(SUMMARY_TAB)
  }
  
  return tabs
})

/** 双向绑定的当前Tab值 */
const activeTabValue = computed({
  get: () => props.activeTab,
  set: (val: string) => emit('update:activeTab', val),
})

/** el-tabs 的 key：当 tab 结构变化时强制重建，避免 Element Plus 内部 panes 数组不一致 */
const tabsKey = computed(() => tabList.value.map(t => t.value).join(':'))

/** 当 canApprove 或 showSummary 变化导致 tabList 缩减时，若 activeTab 不存在于新列表则切到第一个可用 tab */
watch(tabList, (list) => {
  const exists = list.some(tab => tab.value === props.activeTab)
  if (!exists && list.length > 0) {
    emit('update:activeTab', list[0].value)
  }
})

function handleTabChange(name: string): void {
  emit('update:activeTab', name)
}
</script>

<style scoped lang="scss">
.approval-tabs {
  // el-tabs 样式覆盖已迁移至 src/styles/index.scss 全局样式文件
}
</style>
