import type { MenuGroupConfig } from '@/types/permission'
import { UserRole } from '@/stores/permission'

export const traceabilityMenu: MenuGroupConfig = {
  id: 'traceability',
  title: '食品追溯',
  icon: 'Location',
  path: '/traceability',
  order: 110,
  category: 'traceability',
  visibleRoles: [UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR, UserRole.STORE_MANAGER],
  children: [
    // ===== 全规模可见（mini/standard/chain-standard/chain-enterprise） =====
    // 临期预警前置：全规模最高频功能，单人店第一需求
    { title: '临期预警', icon: 'WarningFilled', path: '/traceability/expiry-warning' },
    { title: '追溯查询', icon: 'Search', path: '/traceability/query' },
    { title: '原料追溯', icon: 'Tickets', path: '/traceability/material-code' },
    // ===== standard 及以上可见（单店/标准连锁/大型连锁） =====
    { title: '食品追溯', icon: 'Food', path: '/traceability/food-trace-code',
      scaleLevel: ['standard', 'chain-standard', 'chain-enterprise'] },
    { title: '追溯链展示', icon: 'Link', path: '/traceability/chain',
      scaleLevel: ['standard', 'chain-standard', 'chain-enterprise'] },
    { title: '供应商追溯', icon: 'OfficeBuilding', path: '/traceability/supplier',
      scaleLevel: ['standard', 'chain-standard', 'chain-enterprise'] },
    { title: '检验记录', icon: 'Document', path: '/traceability/inspection',
      scaleLevel: ['standard', 'chain-standard', 'chain-enterprise'] },
    { title: '标签模板', icon: 'Postcard', path: '/traceability/label-template',
      scaleLevel: ['standard', 'chain-standard', 'chain-enterprise'] },
    // ===== chain-standard 及以上可见（标准连锁/大型连锁） =====
    { title: '召回管理', icon: 'AlarmClock', path: '/traceability/recall',
      scaleLevel: ['chain-standard', 'chain-enterprise'] },
    // ===== chain-enterprise 可见（大型连锁） =====
    { title: '质量追溯', icon: 'GoldMedal', path: '/traceability/quality',
      scaleLevel: ['chain-enterprise'] },
  ],
}
