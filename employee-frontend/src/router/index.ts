import { createRouter, createWebHistory, createWebHashHistory } from 'vue-router'

// Capacitor 原生环境使用 Hash 模式（file:// 协议不支持 HTML5 History）
const isCapacitor = (window as any).Capacitor?.isNativePlatform?.() ?? false
const historyMode = isCapacitor ? createWebHashHistory() : createWebHistory()

const router = createRouter({
  history: historyMode,
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginPage.vue'),
      meta: { title: '登录', public: true },
    },
    {
      path: '/',
      redirect: '/home',
    },
    {
      path: '/home',
      name: 'Home',
      component: () => import('@/views/HomePage.vue'),
      meta: { title: '首页', tab: 'home' },
    },
    {
      path: '/office',
      name: 'Office',
      component: () => import('@/views/OfficePage.vue'),
      meta: { title: '办公中心', tab: 'office' },
    },
    {
      path: '/messages',
      name: 'Messages',
      component: () => import('@/views/MessagesPage.vue'),
      meta: { title: '消息', tab: 'messages' },
    },
    {
      path: '/schedule',
      name: 'Schedule',
      component: () => import('@/views/SchedulePage.vue'),
      meta: { title: '我的排班' },
    },
    {
      path: '/attendance',
      name: 'Attendance',
      component: () => import('@/views/AttendancePage.vue'),
      meta: { title: '我的考勤' },
    },
    {
      path: '/salary',
      name: 'Salary',
      component: () => import('@/views/SalaryPage.vue'),
      meta: { title: '我的收入' },
    },
    {
      path: '/leave',
      name: 'Leave',
      component: () => import('@/views/LeavePage.vue'),
      meta: { title: '请假管理' },
    },
    {
      path: '/training',
      name: 'Training',
      component: () => import('@/views/TrainingPage.vue'),
      meta: { title: '培训记录' },
    },
    {
      path: '/knowledge',
      name: 'Knowledge',
      component: () => import('@/views/KnowledgePage.vue'),
      meta: { title: '知识库' },
    },
    {
      path: '/tasks',
      name: 'Tasks',
      component: () => import('@/views/TaskPage.vue'),
      meta: { title: '工作任务' },
    },
    {
      path: '/tasks/publish',
      name: 'TaskPublish',
      component: () => import('@/views/TaskPublishPage.vue'),
      meta: { title: '发布任务' },
    },
    {
      path: '/notices',
      redirect: '/messages?tab=notices',
    },
    {
      path: '/reports',
      name: 'Reports',
      component: () => import('@/views/ReportsPage.vue'),
      meta: { title: '数据报表' },
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/ProfilePage.vue'),
      meta: { title: '个人中心', tab: 'profile' },
    },
    {
      path: '/settings/account',
      name: 'AccountSettings',
      component: () => import('@/views/AccountSettingsPage.vue'),
      meta: { title: '账号设置' },
    },
    {
      path: '/settings/security',
      name: 'SecuritySettings',
      component: () => import('@/views/SecuritySettingsPage.vue'),
      meta: { title: '安全设置' },
    },
    {
      path: '/settings/security/password',
      name: 'PasswordChange',
      component: () => import('@/views/PasswordChangePage.vue'),
      meta: { title: '修改密码' },
    },
    {
      path: '/settings/security/devices',
      name: 'DeviceList',
      component: () => import('@/views/DeviceListPage.vue'),
      meta: { title: '常用设备' },
    },
    {
      path: '/settings/security/logs',
      name: 'LoginLog',
      component: () => import('@/views/LoginLogPage.vue'),
      meta: { title: '登录日志' },
    },
    {
      path: '/settings/security/recovery',
      name: 'PasswordRecovery',
      component: () => import('@/views/PasswordRecoveryPage.vue'),
      meta: { title: '密码找回' },
    },
    {
      path: '/settings/security/email-bind',
      name: 'EmailBind',
      component: () => import('@/views/EmailBindPage.vue'),
      meta: { title: '邮箱绑定' },
    },
    {
      path: '/settings/security/lock',
      name: 'SecurityLock',
      component: () => import('@/views/SecurityLockPage.vue'),
      meta: { title: '安全锁定' },
    },
    {
      path: '/settings/notifications',
      name: 'NotificationSettings',
      component: () => import('@/views/NotificationSettingsPage.vue'),
      meta: { title: '通知偏好' },
    },
    {
      path: '/help',
      name: 'HelpFeedback',
      component: () => import('@/views/HelpFeedbackPage.vue'),
      meta: { title: '帮助与反馈' },
    },
    {
      path: '/approval',
      name: 'Approval',
      component: () => import('@/views/ApprovalPage.vue'),
      meta: { title: '审批中心' },
    },
    {
      path: '/approval/detail/:id',
      name: 'ApprovalDetail',
      component: () => import('@/views/approval/ApprovalDetailPage.vue'),
      meta: { title: '审批详情' },
    },
    {
      path: '/approval/create/:type',
      name: 'ApprovalCreate',
      component: () => import('@/views/approval/ApprovalCreatePage.vue'),
      meta: { title: '发起申请' },
    },
    {
      path: '/review',
      name: 'Review',
      component: () => import('@/views/ReviewPage.vue'),
      meta: { title: '绩效考核' },
    },
    {
      path: '/review/detail/:id',
      name: 'ReviewDetail',
      component: () => import('@/views/review/ReviewDetailPage.vue'),
      meta: { title: '考核详情' },
    },
    {
      path: '/appeals/create/:type',
      name: 'AppealCreate',
      component: () => import('@/views/AppealCreatePage.vue'),
      meta: { title: '创建申诉' },
    },
    {
      path: '/appeals',
      name: 'AppealList',
      component: () => import('@/views/AppealListPage.vue'),
      meta: { title: '我的申诉' },
    },
    {
      path: '/appeals/:id',
      name: 'AppealDetail',
      component: () => import('@/views/AppealDetailPage.vue'),
      meta: { title: '申诉详情' },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFoundPage.vue'),
      meta: { title: '页面不存在', public: true },
    },
  ],
  /**
   * 滚动行为配置
   *
   * - 返回导航（popState）：恢复之前保存的滚动位置
   * - 前进导航（push）：滚动到顶部
   * - 锚点跳转：滚动到锚点位置
   */
  scrollBehavior(to, from, savedPosition) {
    // 有保存的位置（浏览器后退/前进）→ 恢复
    if (savedPosition) {
      return savedPosition
    }
    // 锚点跳转
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }
    // 默认：新导航滚到顶部
    return { top: 0, left: 0 }
  },
})

/** 检查 Token 是否已过期 */
function isTokenExpired(): boolean {
  const expiresAt = localStorage.getItem('tokenExpiresAt')
  if (!expiresAt) return true
  return Date.now() > Number(expiresAt)
}

router.beforeEach((to, _from, next) => {
  // 【修复】移除每次路由切换时的 SW 异步检测
  // 原因：每次路由切换都执行 navigator.serviceWorker.getRegistrations() 是不必要的
  // SW 卸载已在 main.ts 中完成（APP 模式启动时一次性清理）
  // 如果仍检测到残留 SW，说明 main.ts 的清理未生效，此处重复检测也无法解决

  const title = to.meta.title as string
  if (title) {
    document.title = `${title} - 员工门户`
  }

  const hasToken = !!localStorage.getItem('token')
  const isLoggedIn = hasToken && !isTokenExpired()

  // Token 存在但已过期：清除过期凭据，视为未登录
  if (hasToken && !isLoggedIn) {
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('tokenExpiresAt')
  }

  if (!to.meta.public && !isLoggedIn) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }

  if (to.path === '/login' && isLoggedIn) {
    return next('/home')
  }

  next()
})

export default router
