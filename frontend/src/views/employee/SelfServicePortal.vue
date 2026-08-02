<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  User, Calendar, Money, Document, Reading,
  Bell, Clock, Timer, ChatDotRound, Tickets,
  DataLine, TrendCharts, CircleCheck, Warning,
  Position, OfficeBuilding, Phone, Message,
  ShoppingCart, Coin, DataBoard, Setting,
} from '@element-plus/icons-vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { usePermissionStore, UserRole } from '@/stores/permission'

const permissionStore = usePermissionStore()

// ========== 内部Tab导航 ==========
type PortalTab = 'overview' | 'schedule' | 'salary' | 'leave' | 'training' | 'handover' | 'notices'

const activeTab = ref<PortalTab>('overview')

// ========== 员工类型判断 ==========
const isStoreStaff = computed(() => {
  return permissionStore.hasAnyRole([UserRole.EMPLOYEE, UserRole.TEAM_LEADER, UserRole.STORE_MANAGER])
})

const isHQStaff = computed(() => {
  return !isStoreStaff.value && !permissionStore.isAdmin
})

const userDepartmentType = computed<'store' | 'finance' | 'hr' | 'procurement' | 'operations' | 'unknown'>(() => {
  if (permissionStore.userDataScope === undefined) return 'unknown'
  const dept = permissionStore.userInfo?.departmentName?.toLowerCase() || ''
  if (dept.includes('财务') || dept.includes('finance')) return 'finance'
  if (dept.includes('人事') || dept.includes('hr') || dept.includes('人力')) return 'hr'
  if (dept.includes('采购') || dept.includes('procurement')) return 'procurement'
  if (dept.includes('运营') || dept.includes('operations') || dept.includes('营运')) return 'operations'
  if (isStoreStaff.value) return 'store'
  return 'unknown'
})

// ========== 员工信息（Mock，后续对接API）==========
const employeeProfile = computed(() => {
  const base = {
    name: permissionStore.userInfo?.fullName || permissionStore.userInfo?.username || '员工',
    employeeId: permissionStore.userInfo?.userId ? `EMP${permissionStore.userInfo.userId}` : 'EMP001',
    position: '待分配',
    storeName: permissionStore.userInfo?.storeName || '未分配门店',
    department: permissionStore.userInfo?.departmentName || '未分配部门',
    phone: '138****8888',
    entryDate: '2024-03-15',
    status: 'active' as const,
    roleLabel: '',
  }

  const roles = permissionStore.userInfo?.roles || []
  if (roles.includes(UserRole.EMPLOYEE)) { base.position = '员工'; base.roleLabel = '普通员工' }
  else if (roles.includes(UserRole.TEAM_LEADER)) { base.position = '组长'; base.roleLabel = '团队负责人' }
  else if (roles.includes(UserRole.STORE_MANAGER)) { base.position = '店长'; base.roleLabel = '门店经理' }

  return base
})

// ========== 根据角色动态生成快捷操作 ==========
const quickActions = computed<{ icon: any; label: string; desc: string; color: string; tab: PortalTab }[]>(() => {
  const actions = [
    { icon: Calendar, label: '我的排班', desc: '本周班次安排', color: 'warning', tab: 'schedule' as PortalTab },
    { icon: Money, label: '工资条', desc: '查看薪资明细', color: 'success', tab: 'salary' as PortalTab },
    { icon: Document, label: '请假申请', desc: '提交休假申请', color: 'primary', tab: 'leave' as PortalTab },
    { icon: Reading, label: '培训中心', desc: '在线学习课程', color: 'info', tab: 'training' as PortalTab },
  ]

  if (isStoreStaff.value) {
    actions.push({ icon: Tickets, label: '交接班', desc: '填写交接记录', color: '', tab: 'handover' as PortalTab })
  }

  if (userDepartmentType.value === 'finance') {
    actions.push({ icon: Coin, label: '报销申请', desc: '费用报销提交', color: '', tab: 'leave' as PortalTab })
    actions.push({ icon: DataLine, label: '审批队列', desc: '待我审批事项', color: '', tab: 'notices' as PortalTab })
  }

  if (userDepartmentType.value === 'hr') {
    actions.push({ icon: User, label: '面试安排', desc: '招聘面试日程', color: '', tab: 'schedule' as PortalTab })
  }

  actions.push({ icon: ChatDotRound, label: '意见反馈', desc: '提交建议与问题', color: '', tab: 'notices' as PortalTab })

  return actions
})

// ========== 排班数据 ==========
const thisWeekSchedule = ref([
  { day: '周一', date: '05-20', shift: '早班', time: '07:00-15:00', status: 'completed' as const },
  { day: '周二', date: '05-21', shift: '中班', time: '11:00-19:00', status: 'completed' as const },
  { day: '周三', date: '05-22', shift: '休息', time: '--', status: 'rest' as const },
  { day: '周四', date: '05-23', shift: '晚班', time: '16:00-24:00', status: 'today' as const },
  { day: '周五', date: '05-24', shift: '早班', time: '07:00-15:00', status: 'upcoming' as const },
  { day: '周六', date: '05-25', shift: '中班', time: '11:00-19:00', status: 'upcoming' as const },
  { day: '周日', date: '05-26', shift: '休息', time: '--', status: 'rest' as const },
])

// ========== 工资条数据 ==========
const recentPayslips = ref([
  { month: '2025年4月', baseSalary: 5000, overtime: 800, bonus: 300, deduction: 450, total: 5650, status: 'available' as const },
  { month: '2025年3月', baseSalary: 5000, overtime: 1200, bonus: 200, deduction: 380, total: 6020, status: 'viewed' as const },
  { month: '2025年2月', baseSalary: 4800, overtime: 600, bonus: 500, deduction: 320, total: 5580, status: 'viewed' as const },
])

// ========== 待办数据 ==========
const pendingTasks = ref([
  { id: 1, title: '完成食品安全培训课程', type: 'training' as const, deadline: '05-26', priority: 'high' as const },
  { id: 2, title: '更新健康证信息', type: 'certificate' as const, deadline: '05-30', priority: 'medium' as const },
  { id: 3, title: '确认本月排班计划', type: 'schedule' as const, deadline: '05-25', priority: 'low' as const },
])

// ========== 通知公告数据 ==========
const recentNotices = ref([
  { id: 1, title: '关于夏季营业时间调整的通知', date: '05-22', type: 'announcement' as const, read: false },
  { id: 2, title: '5月员工生日会活动安排', date: '05-20', type: 'activity' as const, read: false },
  { id: 3, title: '新菜品培训材料已上传', date: '05-18', type: 'training' as const, read: true },
])

// ========== 请假表单 ==========
const leaveFormVisible = ref(false)
const leaveForm = ref({
  type: '' as string,
  startDate: '',
  endDate: '',
  reason: '',
})
const leaveOptions = [
  { label: '事假', value: 'personal' },
  { label: '病假', value: 'sick' },
  { label: '年假', value: 'annual' },
  { label: '调休', value: 'compensatory' },
]

function openLeaveDialog() {
  leaveFormVisible.value = true
}

function submitLeave() {
  leaveFormVisible.value = false
}

// ========== 辅助函数 ==========
function getShiftStatusType(status: string): 'primary' | 'success' | 'warning' | 'info' | 'inactive' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'inactive'> = {
    completed: 'success',
    today: 'primary',
    upcoming: 'info',
    rest: 'inactive',
  }
  return map[status] || 'info'
}

function getShiftStatusLabel(status: string): string {
  const map: Record<string, string> = {
    completed: '已出勤',
    today: '今日',
    upcoming: '即将',
    rest: '休息',
  }
  return map[status] || status
}

function getPriorityTag(priority: string): 'error' | 'warning' | 'success' | 'info' {
  const map: Record<string, 'error' | 'warning' | 'success' | 'info'> = {
    high: 'error',
    medium: 'warning',
    low: 'success',
  }
  return map[priority] || 'info'
}

function switchTab(tab: PortalTab) {
  activeTab.value = tab
}
</script>

<template>
  <div class="self-service-portal">
    <!-- 页面标题栏 -->
    <div class="portal-header">
      <div class="portal-header-left">
        <h1 class="portal-title">
          <el-icon :size="24"><User /></el-icon>
          员工自助门户
        </h1>
        <span class="portal-subtitle">{{ employeeProfile.storeName }} · {{ employeeProfile.position }}</span>
      </div>
      <StatusTag :status="employeeProfile.status" :label="employeeProfile.roleLabel || '在职'" />
    </div>

    <!-- 内部导航 Tab -->
    <div class="portal-nav">
      <div 
        v-for="item in [
          { tab: 'overview', label: '首页概览', icon: 'DataBoard' },
          { tab: 'schedule', label: '我的排班', icon: 'Calendar' },
          { tab: 'salary', label: '工资条', icon: 'Money' },
          { tab: 'leave', label: '请假申请', icon: 'Document' },
          { tab: 'training', label: '培训中心', icon: 'Reading' },
          ...(isStoreStaff ? [{ tab: 'handover' as const, label: '交接班', icon: 'Tickets' }] : []),
          { tab: 'notices', label: '通知消息', icon: 'Bell' },
        ]"
        :key="item.tab"
        :class="['nav-item', { 'nav-item--active': activeTab === item.tab }]"
        @click="switchTab(item.tab as PortalTab)"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </div>
    </div>

    <!-- ====== Tab: 首页概览 ====== -->
    <div v-show="activeTab === 'overview'" class="portal-content">
      <!-- 个人信息卡片 + 快捷操作 -->
      <div class="portal-top-row">
        <div class="profile-card">
          <div class="profile-avatar">
            <el-icon :size="36"><User /></el-icon>
          </div>
          <div class="profile-info">
            <div class="profile-name-row">
              <h2 class="profile-name">{{ employeeProfile.name }}</h2>
              <StatusTag status="active" label="在职" size="small" />
            </div>
            <p class="profile-position">{{ employeeProfile.position }} · {{ employeeProfile.department }}</p>
            <div class="profile-meta">
              <span><el-icon><OfficeBuilding /></el-icon> {{ employeeProfile.storeName }}</span>
              <span><el-icon><Position /></el-icon> {{ employeeProfile.employeeId }}</span>
              <span><el-icon><Clock /></el-icon> 入职 {{ employeeProfile.entryDate }}</span>
            </div>
          </div>
        </div>

        <div class="quick-grid">
          <div 
            v-for="action in quickActions" 
            :key="action.label"
            class="quick-item"
            @click="switchTab(action.tab)"
          >
            <div class="quick-icon" :class="[`quick-icon--${action.color}`]">
              <el-icon :size="20"><component :is="action.icon" /></el-icon>
            </div>
            <div class="quick-text">
              <span class="quick-label">{{ action.label }}</span>
              <span class="quick-desc">{{ action.desc }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 三列布局 -->
      <div class="portal-main-grid">
        <!-- 本周排班 -->
        <div class="portal-card schedule-card">
          <div class="card-title-bar">
            <h3><el-icon><Calendar /></el-icon> 本周排班</h3>
            <el-button link type="primary" size="small" @click="switchTab('schedule')">查看完整排班 →</el-button>
          </div>
          <div class="schedule-list">
            <div 
              v-for="item in thisWeekSchedule" 
              :key="item.day"
              :class="['schedule-item', `schedule-item--${item.status}`]"
            >
              <div class="schedule-day">
                <span class="day-name">{{ item.day }}</span>
                <span class="day-date">{{ item.date }}</span>
              </div>
              <div class="schedule-detail">
                <StatusTag 
                  :status="getShiftStatusType(item.status)" 
                  :label="getShiftStatusLabel(item.status)"
                  size="small"
                />
                <span v-if="item.shift !== '休息'" class="shift-info">{{ item.shift }} {{ item.time }}</span>
                <span v-else class="shift-rest">休息日</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 待办事项 -->
        <div class="portal-card tasks-card">
          <div class="card-title-bar">
            <h3><el-icon><Bell /></el-icon> 待办事项</h3>
            <StatusTag status="error" size="small" :label="`${pendingTasks.length}项待处理`" />
          </div>
          <div class="task-list">
            <div v-for="task in pendingTasks" :key="task.id" class="task-item">
              <div class="task-icon" :class="[`task-icon--${task.type}`]">
                <el-icon v-if="task.type === 'training'"><Reading /></el-icon>
                <el-icon v-else-if="task.type === 'certificate'"><Document /></el-icon>
                <el-icon v-else><Calendar /></el-icon>
              </div>
              <div class="task-content">
                <span class="task-title">{{ task.title }}</span>
                <span class="task-deadline">截止: {{ task.deadline }}</span>
              </div>
              <StatusTag :status="getPriorityTag(task.priority)" size="small" />
            </div>
          </div>
        </div>

        <!-- 最近工资条 -->
        <div class="portal-card salary-card">
          <div class="card-title-bar">
            <h3><el-icon><Money /></el-icon> 最近工资条</h3>
            <el-button link type="primary" size="small" @click="switchTab('salary')">全部记录 →</el-button>
          </div>
          <div class="salary-list">
            <div v-for="slip in recentPayslips" :key="slip.month" class="salary-item">
              <div class="salary-month">{{ slip.month }}</div>
              <div class="salary-amount">
                <span class="amount-value">¥{{ slip.total.toLocaleString() }}</span>
                <span class="amount-label">实发</span>
              </div>
              <el-button link :type="slip.status === 'available' ? 'primary' : 'default'" size="small">
                {{ slip.status === 'available' ? '查看' : '已查' }}
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 通知公告 -->
      <div class="portal-bottom-row">
        <div class="portal-card notice-card">
          <div class="card-title-bar">
            <h3><el-icon><Message /></el-icon> 通知公告</h3>
            <el-button link type="primary" size="small" @click="switchTab('notices')">全部通知 →</el-button>
          </div>
          <div class="notice-list">
            <div 
              v-for="notice in recentNotices" 
              :key="notice.id" 
              :class="['notice-item', { 'notice-item--unread': !notice.read }]"
            >
              <div class="notice-dot" :class="{ 'notice-dot--unread': !notice.read }"></div>
              <div class="notice-content">
                <span class="notice-title">{{ notice.title }}</span>
                <span class="notice-date">{{ notice.date }}</span>
              </div>
              <StatusTag 
                :status="notice.type === 'announcement' ? 'primary' : notice.type === 'activity' ? 'success' : 'warning'" 
                :label="notice.type === 'announcement' ? '通知' : notice.type === 'activity' ? '活动' : '培训'"
                size="small"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ====== Tab: 我的排班（完整视图） ====== -->
    <div v-show="activeTab === 'schedule'" class="portal-content">
      <div class="portal-card">
        <div class="card-title-bar">
          <h3><el-icon><Calendar /></el-icon> 我的排班计划</h3>
          <el-radio-group size="small">
            <el-radio-button label="本周" />
            <el-radio-button label="下周" />
            <el-radio-button label="本月" />
          </el-radio-group>
        </div>
        <div class="schedule-full-list">
          <div 
            v-for="item in thisWeekSchedule" 
            :key="item.day"
            :class="['schedule-full-item', `schedule-full-item--${item.status}`]"
          >
            <div class="schedule-full-left">
              <div class="full-day-info">
                <span class="full-day-name">{{ item.day }}</span>
                <span class="full-day-date">{{ item.date }}</span>
              </div>
            </div>
            <div class="schedule-full-center">
              <StatusTag 
                v-if="item.shift !== '休息'"
                :status="getShiftStatusType(item.status)" 
                :label="item.shift"
                size="medium"
              />
              <StatusTag v-else status="inactive" label="休息" size="medium" />
              <span v-if="item.shift !== '休息'" class="full-time">{{ item.time }}</span>
            </div>
            <div class="schedule-full-right">
              <span v-if="item.status === 'upcoming'" class="full-hint">即将上班</span>
              <span v-else-if="item.status === 'today'" class="full-hint full-hint--today">今日班次</span>
              <span v-else-if="item.status === 'completed'" class="full-hint full-hint--done">已完成</span>
            </div>
          </div>
        </div>
        <div class="schedule-footer-note">
          <el-icon><InfoFilled /></el-icon>
          如需调班或换班，请联系您的店长或在「交接班」模块中提交申请
        </div>
      </div>
    </div>

    <!-- ====== Tab: 工资条 ====== -->
    <div v-show="activeTab === 'salary'" class="portal-content">
      <div class="portal-card">
        <div class="card-title-bar">
          <h3><el-icon><Money /></el-icon> 我的工资条</h3>
        </div>
        <div class="salary-full-list">
          <div v-for="slip in recentPayslips" :key="slip.month" class="salary-full-item">
            <div class="salary-full-header">
              <span class="salary-full-month">{{ slip.month }}</span>
              <StatusTag :status="slip.status === 'available' ? 'primary' : 'default'" :label="slip.status === 'available' ? '可查看' : '已查看'" size="small" />
            </div>
            <div class="salary-full-body">
              <div class="salary-detail-grid">
                <div class="detail-cell">
                  <span class="detail-label">基本工资</span>
                  <span class="detail-value">¥{{ slip.baseSalary.toLocaleString() }}</span>
                </div>
                <div class="detail-cell">
                  <span class="detail-label">加班费</span>
                  <span class="detail-value detail-value--plus">+¥{{ slip.overtime.toLocaleString() }}</span>
                </div>
                <div class="detail-cell">
                  <span class="detail-label">奖金</span>
                  <span class="detail-value detail-value--plus">+¥{{ slip.bonus.toLocaleString() }}</span>
                </div>
                <div class="detail-cell">
                  <span class="detail-label">扣除项</span>
                  <span class="detail-value detail-value--minus">-¥{{ slip.deduction.toLocaleString() }}</span>
                </div>
              </div>
              <div class="salary-total-row">
                <span class="total-label">实发工资</span>
                <span class="total-value">¥{{ slip.total.toLocaleString() }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="salary-footer-note">
          <el-icon><Warning /></el-icon>
          工资条信息仅供参考，实际金额以银行到账为准。如有疑问请联系财务部门。
        </div>
      </div>
    </div>

    <!-- ====== Tab: 请假申请 ====== -->
    <div v-show="activeTab === 'leave'" class="portal-content">
      <div class="portal-card">
        <div class="card-title-bar">
          <h3><el-icon><Document /></el-icon> 请假申请</h3>
        </div>
        <div class="leave-form-container">
          <el-form :model="leaveForm" label-width="80px" class="leave-form">
            <el-form-item label="请假类型">
              <el-select v-model="leaveForm.type" placeholder="请选择请假类型" style="width: 100%">
                <el-option v-for="opt in leaveOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="开始日期">
              <el-date-picker v-model="leaveForm.startDate" type="date" placeholder="选择开始日期" style="width: 100%" />
            </el-form-item>
            <el-form-item label="结束日期">
              <el-date-picker v-model="leaveForm.endDate" type="date" placeholder="选择结束日期" style="width: 100%" />
            </el-form-item>
            <el-form-item label="请假原因">
              <el-input v-model="leaveForm.reason" type="textarea" :rows="4" placeholder="请输入请假原因..." />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitLeave">提交申请</el-button>
              <el-button @click="leaveForm = { type: '', startDate: '', endDate: '', reason: '' }">重置</el-button>
            </el-form-item>
          </el-form>
          <div class="leave-history">
            <h4>最近请假记录</h4>
            <div class="history-empty">
              <el-icon :size="32"><Document /></el-icon>
              <span>暂无请假记录</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ====== Tab: 培训中心 ====== -->
    <div v-show="activeTab === 'training'" class="portal-content">
      <div class="portal-card">
        <div class="card-title-bar">
          <h3><el-icon><Reading /></el-icon> 培训中心</h3>
        </div>
        <div class="training-grid">
          <div class="training-course" v-for="i in 4" :key="i">
            <div class="course-cover">
              <el-icon :size="40"><Reading /></el-icon>
            </div>
            <div class="course-info">
              <h4>食品安全培训 · 第{{ i }}章节</h4>
              <p>必修课程 · 预计30分钟</p>
              <div class="course-progress">
                <el-progress :percentage="i === 1 ? 100 : i === 2 ? 60 : 0" :stroke-width="6" />
              </div>
            </div>
            <el-button :type="i === 1 ? 'success' : 'primary'" size="small">
              {{ i === 1 ? '已完成' : i === 2 ? '继续学习' : '开始学习' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- ====== Tab: 交接班（仅门店员工） ====== -->
    <div v-show="activeTab === 'handover'" class="portal-content">
      <div class="portal-card">
        <div class="card-title-bar">
          <h3><el-icon><Tickets /></el-icon> 交接班记录</h3>
          <el-button type="primary" size="small">新建交接记录</el-button>
        </div>
        <div class="handover-empty">
          <el-icon :size="48"><Tickets /></el-icon>
          <p>暂无交接班记录</p>
          <p class="empty-hint">每次下班前请填写交接班记录，确保工作顺利交接</p>
        </div>
      </div>
    </div>

    <!-- ====== Tab: 通知消息 ====== -->
    <div v-show="activeTab === 'notices'" class="portal-content">
      <div class="portal-card">
        <div class="card-title-bar">
          <h3><el-icon><Bell /></el-icon> 通知消息</h3>
          <el-button link type="primary" size="small">全部标为已读</el-button>
        </div>
        <div class="notice-full-list">
          <div 
            v-for="notice in recentNotices" 
            :key="notice.id" 
            :class="['notice-full-item', { 'notice-full-item--unread': !notice.read }]"
          >
            <div class="notice-full-dot" :class="{ 'notice-full-dot--unread': !notice.read }"></div>
            <div class="notice-full-content">
              <div class="notice-full-header">
                <StatusTag 
                  :status="notice.type === 'announcement' ? 'primary' : notice.type === 'activity' ? 'success' : 'warning'"
                  :label="notice.type === 'announcement' ? '通知' : notice.type === 'activity' ? '活动' : '培训'"
                  size="small"
                />
                <span class="notice-full-date">{{ notice.date }}</span>
              </div>
              <h4 class="notice-full-title">{{ notice.title }}</h4>
              <p class="notice-full-summary">点击查看详细内容...</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.self-service-portal {
  max-width: 1200px;
  margin: 0 auto;
}

// ========== 页面标题栏 ==========
.portal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-5);
}

.portal-header-left {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
}

.portal-title {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-xl);
  font-weight: 700;
  color: var(--fts-text-primary);
  margin: 0;
}

.portal-subtitle {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
}

// ========== 内部导航 Tab ==========
.portal-nav {
  display: flex;
  gap: var(--fts-space-1);
  padding: var(--fts-space-1);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-lg);
  margin-bottom: var(--fts-space-5);
  overflow-x: auto;

  &::-webkit-scrollbar { height: 0; }
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-1);
  padding: var(--fts-space-2) var(--fts-space-4);
  border-radius: var(--fts-radius-md);
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-secondary);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s ease;
  border: 1px solid transparent;

  &:hover {
    color: var(--fts-primary);
    background: var(--fts-bg-card);
  }

  &--active {
    color: var(--fts-primary);
    background: var(--fts-bg-card);
    border-color: var(--fts-primary);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
    font-weight: 600;
  }
}

// ========== 内容区域 ==========
.portal-content {
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

// ========== 顶部行：个人信息 + 快捷操作 ==========
.portal-top-row {
  display: grid;
  grid-template-columns: 340px 1fr;
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.profile-card {
  display: flex;
  gap: var(--fts-space-4);
  padding: var(--fts-space-5);
  background: linear-gradient(135deg, rgba(var(--fts-primary-rgb, 64, 158, 255), 0.06), rgba(var(--fts-primary-rgb, 64, 158, 255), 0.02));
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
}

.profile-avatar {
  width: 64px;
  height: 64px;
  border-radius: var(--fts-radius-lg);
  background: var(--fts-bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--fts-text-tertiary);
  flex-shrink: 0;
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-1);
}

.profile-name {
  font-size: var(--fts-font-size-lg);
  font-weight: 700;
  color: var(--fts-text-primary);
  margin: 0;
}

.profile-position {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  margin: 0 0 var(--fts-space-2);
}

.profile-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fts-space-2) var(--fts-space-4);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);

  span {
    display: flex;
    align-items: center;
    gap: 4px;
  }
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: var(--fts-space-3);
}

.quick-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--fts-primary);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    transform: translateY(-1px);
  }
}

.quick-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--fts-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &--primary { background: rgba(var(--fts-primary-rgb, 64, 158, 255), 0.1); color: var(--fts-primary); }
  &--success { background: rgba(var(--fts-success-rgb, 103, 194, 58), 0.1); color: var(--fts-success); }
  &--warning { background: rgba(var(--fts-warning-rgb, 230, 162, 60), 0.1); color: var(--fts-warning); }
  &--info { background: rgba(var(--fts-info-rgb, 144, 147, 153), 0.1); color: var(--fts-info); }

  &:not([class*='--']) {
    background: var(--fts-bg-tertiary);
    color: var(--fts-text-secondary);
  }
}

.quick-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.quick-label {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.quick-desc {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);
}

// ========== 三列网格 ==========
.portal-main-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr;
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

// 通用卡片样式
.portal-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-lg);
  overflow: hidden;
}

.card-title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-3) var(--fts-space-4);
  border-bottom: 1px solid var(--fts-border-secondary);

  h3 {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    font-size: var(--fts-font-size-base);
    font-weight: 600;
    color: var(--fts-text-primary);
    margin: 0;
  }
}

// 排班列表
.schedule-list {
  padding: var(--fts-space-2) var(--fts-space-4) var(--fts-space-3);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
}

.schedule-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-2) var(--fts-space-3);
  border-radius: var(--fts-radius-sm);

  &:nth-child(even) { background: var(--fts-bg-secondary); }
  &--today {
    background: rgba(var(--fts-primary-rgb, 64, 158, 255), 0.06);
    border-left: 3px solid var(--fts-primary);
  }
  &--rest { opacity: 0.55; }
}

.schedule-day {
  display: flex;
  flex-direction: column;
  min-width: 48px;
}

.day-name { font-size: var(--fts-font-size-sm); font-weight: 600; color: var(--fts-text-primary); }
.day-date { font-size: var(--fts-font-size-xs); color: var(--fts-text-quaternary); }

.schedule-detail {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex: 1;
  justify-content: flex-end;
}

.shift-info { font-size: var(--fts-font-size-xs); color: var(--fts-text-secondary); }
.shift-rest { font-size: var(--fts-font-size-xs); color: var(--fts-text-quaternary); }

// 待办列表
.task-list {
  padding: var(--fts-space-2) var(--fts-space-4) var(--fts-space-3);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.task-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
}

.task-icon {
  width: 32px; height: 32px;
  border-radius: var(--fts-radius-sm);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; font-size: 14px;
  &--training { background: rgba(var(--fts-warning-rgb, 230, 162, 60), 0.1); color: var(--fts-warning); }
  &--certificate { background: rgba(var(--fts-primary-rgb, 64, 158, 255), 0.1); color: var(--fts-primary); }
  &--schedule { background: rgba(var(--fts-success-rgb, 103, 194, 58), 0.1); color: var(--fts-success); }
}

.task-content {
  flex: 1; display: flex; flex-direction: column; gap: 2px; min-width: 0;
}
.task-title { font-size: var(--fts-font-size-sm); font-weight: 500; color: var(--fts-text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.task-deadline { font-size: var(--fts-font-size-xs); color: var(--fts-text-quaternary); }

// 工资条列表
.salary-list {
  padding: var(--fts-space-2) var(--fts-space-4) var(--fts-space-3);
  display: flex; flex-direction: column; gap: var(--fts-space-2);
}

.salary-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: var(--fts-space-2) var(--fts-space-3);
  &:not(:last-child) { border-bottom: 1px dashed var(--fts-border-secondary); }
}

.salary-month { font-size: var(--fts-font-size-sm); font-weight: 500; color: var(--fts-text-primary); }
.salary-amount { display: flex; align-items: baseline; gap: var(--fts-space-1); }
.amount-value { font-size: var(--fts-font-size-md); font-weight: 700; color: var(--fts-success); }
.amount-label { font-size: var(--fts-font-size-xs); color: var(--fts-text-quaternary); }

// 底部通知栏
.portal-bottom-row { }

.notice-list {
  padding: var(--fts-space-2) var(--fts-space-4) var(--fts-space-3);
  display: flex; flex-direction: column; gap: var(--fts-space-1);
}

.notice-item {
  display: flex; align-items: center; gap: var(--fts-space-3);
  padding: var(--fts-space-2) var(--fts-space-3);
  border-radius: var(--fts-radius-sm); cursor: pointer;
  transition: background-color 0.15s ease;
  &:hover { background: var(--fts-bg-secondary); }
  &--unread { background: rgba(var(--fts-primary-rgb, 64, 158, 255), 0.03); }
}

.notice-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--fts-border-primary); flex-shrink: 0;
  &--unread { background: var(--fts-primary); }
}

.notice-content {
  flex: 1; display: flex; justify-content: space-between;
  align-items: center; gap: var(--fts-space-2); min-width: 0;
}
.notice-title { font-size: var(--fts-font-size-sm); color: var(--fts-text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; .notice-item--unread & { font-weight: 600; } }
.notice-date { font-size: var(--fts-font-size-xs); color: var(--fts-text-quaternary); flex-shrink: 0; }

// ========== 完整排班视图 ==========
.schedule-full-list {
  padding: var(--fts-space-3) var(--fts-space-4);
}

.schedule-full-item {
  display: flex;
  align-items: center;
  padding: var(--fts-space-3) var(--fts-space-4);
  border-bottom: 1px solid var(--fts-border-secondary);

  &:last-child { border-bottom: none; }

  &--today { background: rgba(var(--fts-primary-rgb, 64, 158, 255), 0.04); }
  &--rest { opacity: 0.5; }
}

.schedule-full-left { min-width: 70px; }
.full-day-info { display: flex; flex-direction: column; }
.full-day-name { font-size: var(--fts-font-size-base); font-weight: 600; color: var(--fts-text-primary); }
.full-day-date { font-size: var(--fts-font-size-xs); color: var(--fts-text-quaternary); }

.schedule-full-center { flex: 1; display: flex; align-items: center; gap: var(--fts-space-3); }
.full-time { font-size: var(--fts-font-size-sm); color: var(--fts-text-secondary); }

.schedule-full-right { min-width: 80px; text-align: right; }
.full-hint { font-size: var(--fts-font-size-xs); color: var(--fts-text-quaternary); &--today { color: var(--fts-primary); font-weight: 600; } &--done { color: var(--fts-success); } }

.schedule-footer-note {
  display: flex; align-items: center; gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  font-size: var(--fts-font-size-xs); color: var(--fts-text-tertiary);
}

// ========== 完整工资条视图 ==========
.salary-full-list {
  padding: var(--fts-space-3) var(--fts-space-4);
  display: flex; flex-direction: column; gap: var(--fts-space-4);
}

.salary-full-item {
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  overflow: hidden;
}

.salary-full-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: var(--fts-space-2) var(--fts-space-4);
  background: var(--fts-bg-secondary);
}
.salary-full-month { font-weight: 600; color: var(--fts-text-primary); }

.salary-full-body { padding: var(--fts-space-3) var(--fts-space-4); }

.salary-detail-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-3);
}

.detail-cell {
  display: flex; flex-direction: column; gap: var(--fts-space-1);
}
.detail-label { font-size: var(--fts-font-size-xs); color: var(--fts-text-quaternary); }
.detail-value { font-size: var(--fts-font-size-sm); font-weight: 600; color: var(--fts-text-primary); &--plus { color: var(--fts-success); } &--minus { color: var(--fts-error); } }

.salary-total-row {
  display: flex; justify-content: space-between; align-items: center;
  padding-top: var(--fts-space-2);
  border-top: 2px solid var(--fts-border-primary);
}
.total-label { font-size: var(--fts-font-size-sm); color: var(--fts-text-secondary); font-weight: 500; }
.total-value { font-size: var(--fts-font-size-xl); font-weight: 700; color: var(--fts-success); }

.salary-footer-note {
  display: flex; align-items: center; gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: rgba(var(--fts-warning-rgb, 230, 162, 60), 0.06);
  font-size: var(--fts-font-size-xs); color: var(--fts-text-tertiary);
  margin-top: var(--fts-space-3);
}

// ========== 请假申请 ==========
.leave-form-container {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: var(--fts-space-5);
  padding: var(--fts-space-4);
}

.leave-form { max-width: 480px; }

.leave-history h4 { font-size: var(--fts-font-size-sm); color: var(--fts-text-secondary); margin: 0 0 var(--fts-space-3); }
.history-empty {
  display: flex; flex-direction: column; align-items: center; gap: var(--fts-space-2);
  padding: var(--fts-space-6) 0; color: var(--fts-text-quaternary); font-size: var(--fts-font-size-sm);
}

// ========== 培训中心 ==========
.training-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--fts-space-4);
  padding: var(--fts-space-4);
}

.training-course {
  display: flex; gap: var(--fts-space-3);
  padding: var(--fts-space-4);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  transition: border-color 0.15s ease;

  &:hover { border-color: var(--fts-primary); }
}

.course-cover {
  width: 64px; height: 64px;
  border-radius: var(--fts-radius-md);
  background: var(--fts-bg-tertiary);
  display: flex; align-items: center; justify-content: center;
  color: var(--fts-text-quaternary); flex-shrink: 0;
}

.course-info { flex: 1; display: flex; flex-direction: column; gap: var(--fts-space-1); }
.course-info h4 { font-size: var(--fts-font-size-sm); font-weight: 600; color: var(--fts-text-primary); margin: 0; }
.course-info p { font-size: var(--fts-font-size-xs); color: var(--fts-text-quaternary); margin: 0; }
.course-progress { margin-top: var(--fts-space-1); }

// ========== 交接班 ==========
.handover-empty {
  display: flex; flex-direction: column; align-items: center; gap: var(--fts-space-2);
  padding: var(--fts-space-10) var(--fts-space-4); color: var(--fts-text-quaternary);

  p { margin: 0; font-size: var(--fts-font-size-sm); }
  .empty-hint { font-size: var(--fts-font-size-xs); color: var(--fts-text-quaternary); opacity: 0.7; }
}

// ========== 通知完整列表 ==========
.notice-full-list {
  padding: var(--fts-space-3) var(--fts-space-4);
  display: flex; flex-direction: column; gap: var(--fts-space-2);
}

.notice-full-item {
  display: flex; gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  border-radius: var(--fts-radius-md);
  cursor: pointer; transition: background-color 0.15s ease;
  &:hover { background: var(--fts-bg-secondary); }
  &--unread { background: rgba(var(--fts-primary-rgb, 64, 158, 255), 0.03); border-left: 3px solid var(--fts-primary); }
}

.notice-full-dot {
  width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; margin-top: 4px;
  background: var(--fts-border-primary);
  &--unread { background: var(--fts-primary); }
}

.notice-full-content { flex: 1; min-width: 0; }
.notice-full-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--fts-space-1); }
.notice-full-date { font-size: var(--fts-font-size-xs); color: var(--fts-text-quaternary); }
.notice-full-title { font-size: var(--fts-font-size-sm); font-weight: 600; color: var(--fts-text-primary); margin: 0; .notice-full-item--unread & { font-weight: 700; } }
.notice-full-summary { font-size: var(--fts-font-size-xs); color: var(--fts-text-tertiary); margin: var(--fts-space-1) 0 0; }

// ========== 响应式适配 ==========
@media (max-width: 1024px) {
  .portal-top-row { grid-template-columns: 1fr; }
  .portal-main-grid { grid-template-columns: 1fr 1fr; }
  .schedule-card { grid-column: span 2; }
  .leave-form-container { grid-template-columns: 1fr; }
  .training-grid { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .portal-main-grid { grid-template-columns: 1fr; }
  .schedule-card { grid-column: auto; }
  .quick-grid { grid-template-columns: repeat(2, 1fr); }
  .salary-detail-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
