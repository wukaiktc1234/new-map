import type { MenuGroupConfig } from '@/types/permission'
import { UserRole } from '@/stores/permission'

export const warehouseMenu: MenuGroupConfig = {
  id: 'warehouse',
  title: '仓储管理',
  icon: 'Box',
  path: '/warehouse',
  order: 40,
  category: 'management',
  visibleRoles: [UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR, UserRole.WAREHOUSE_MANAGER, UserRole.STORE_MANAGER],
  children: [
    { title: '库存概览', icon: 'DataBoard', path: '/warehouse/overview' },
    { title: '库存管理', icon: 'Box', path: '/warehouse/inventory' },
    { title: '库存入库', icon: 'Download', path: '/warehouse/inventory-stockin' },
    { title: '库存出库', icon: 'Sell', path: '/warehouse/outbound' },
    { title: '库存调拨', icon: 'Connection', path: '/warehouse/transfer' },
    { title: '库存调整', icon: 'Edit', path: '/warehouse/adjust' },
    { title: '库存报损', icon: 'Delete', path: '/warehouse/inventory-loss' },
    { title: '库存盘点', icon: 'Check', path: '/warehouse/check' },
    { title: '库存预警', icon: 'Warning', path: '/warehouse/warning' },
    { title: '库位管理', icon: 'Grid', path: '/warehouse/location' },
    { title: '门店库存查看', icon: 'Shop', path: '/warehouse/store-inventory' },
    { title: '库存报表', icon: 'Document', path: '/warehouse/report' },
    { title: '智能补货建议', icon: 'MagicStick', path: '/warehouse/smart-restock' },
  ],
}

