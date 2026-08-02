import type { MenuGroupConfig } from '@/types/permission'
import { UserRole } from '@/stores/permission'

export const deviceMenu: MenuGroupConfig = {
  id: 'device',
  title: '设备管理',
  icon: 'Monitor',
  path: '/device',
  order: 120,
  category: 'system',
  visibleRoles: [UserRole.OWNER, UserRole.ADMIN],
  children: [
    // ===== 设备列表（路由：/device/list → DeviceList.vue） =====
    { title: '设备列表', icon: 'Monitor', path: '/device/list' },
    // ===== 设备监控（路由：/device/monitor → DeviceMonitor.vue） =====
    { title: '设备监控', icon: 'View', path: '/device/monitor' },
    // ===== 设备告警（路由：/device/alerts → DeviceAlerts.vue） =====
    { title: '设备告警', icon: 'Bell', path: '/device/alerts' },
    // ===== 状态历史（路由：/device/status-history → DeviceStatusHistory.vue） =====
    { title: '状态历史', icon: 'Time', path: '/device/status-history' },
  ],
}

