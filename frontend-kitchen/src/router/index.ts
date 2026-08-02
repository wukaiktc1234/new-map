import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Home',
      component: () => import('@/views/Home.vue')
    },
    {
      path: '/scan',
      name: 'Scan',
      component: () => import('@/views/Scan.vue')
    },
    {
      path: '/station-print',
      name: 'StationPrint',
      component: () => import('@/views/StationPrint.vue')
    },
    {
      path: '/serve-window',
      name: 'ServeWindow',
      component: () => import('@/views/ServeWindow.vue')
    }
  ]
})

export default router
