import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { title: 'POS登录', requiresAuth: false, isPublic: true }
    },
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/Home.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/call-number',
      name: 'CallNumber',
      component: () => import('@/views/CallNumber.vue'),
      meta: { title: '叫号工作端', requiresAuth: true, standalone: true }
    },
    {
      path: '/order',
      name: 'Order',
      component: () => import('@/views/Order.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/payment',
      name: 'Payment',
      component: () => import('@/views/Payment.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/tables',
      name: 'TableManagement',
      component: () => import('@/views/TableManagement.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/customer/order',
      name: 'CustomerOrder',
      component: () => import('@/views/CustomerOrder.vue'),
      meta: { title: '顾客点餐', requiresAuth: false, isPublic: true }
    },
    {
      path: '/scan',
      name: 'Scan',
      component: () => import('@/views/Scan.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/kitchen',
      name: 'KitchenDisplay',
      component: () => import('@/views/kitchen/KitchenHome.vue'),
      meta: {
        title: '后厨工作板',
        requiresAuth: false,
        isPublic: true,
        icon: 'kitchen'
      }
    },
    {
      path: '/customer-display',
      name: 'CustomerDisplay',
      component: () => import('@/components/pos/CustomerOrderDisplay.vue'),
      meta: {
        title: '顾客订单显示屏',
        requiresAuth: false,
        isPublic: true,
        standalone: true
      }
    },
    {
      path: '/display',
      name: 'CallingDisplay',
      component: () => import('@/views/CallingDisplay.vue'),
      meta: {
        title: '叫号展示屏',
        requiresAuth: false,
        isPublic: true
      }
    }
  ]
})

/**
 * 白名单路径 - 不需要登录即可访问
 */
const WHITE_LIST = ['/login', '/kitchen', '/customer-display', '/display', '/customer/order']

/**
 * 验证Token格式有效性
 * - JWT 三段式格式：解析 payload 校验 exp 过期时间
 * - 非 JWT 格式（如后端 UUID token）：仅校验非空，过期由后端 401 响应处理
 * @param token - 待验证的token字符串
 * @returns token是否有效
 */
function isTokenValid(token: string | null): boolean {
  if (!token || typeof token !== 'string' || token.trim().length === 0) return false
  const parts = token.split('.')
  // JWT 三段式格式：解析 exp 字段校验过期
  if (parts.length === 3) {
    try {
      const payload = JSON.parse(atob(parts[1]))
      if (payload.exp && payload.exp * 1000 < Date.now()) return false
      return true
    } catch {
      // JWT 解析失败，但 token 存在，仍视为有效（后端会校验）
      return true
    }
  }
  // 非 JWT 格式（如 UUID token）：只要存在即视为有效，过期由后端 401 处理
  return true
}

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('pos-token')
  const userInfo = localStorage.getItem('pos-user')

  if (to.meta.isPublic || WHITE_LIST.includes(to.path)) {
    next()
    return
  }

  if (to.meta.requiresAuth !== false) {
    if (!isTokenValid(token)) {
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
      return
    }

    if (!userInfo) {
      next({
        path: '/login',
        query: { redirect: to.fullPath, reason: 'session' }
      })
      return
    }
  }

  if (to.path === '/login' && isTokenValid(token) && userInfo) {
    next({ path: '/' })
    return
  }

  next()
})

export default router