import type { MenuGroupConfig } from '@/types/permission'
import { UserRole } from '@/stores/permission'

export const productMenu: MenuGroupConfig = {
  id: 'product',
  title: '产品中心',
  icon: 'Goods',
  path: '/product',
  order: 10,
  category: 'management',
  visibleRoles: [UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR, UserRole.STORE_MANAGER],
  children: [
    { title: '菜品管理', icon: 'Food', path: '/product/food' },
    { title: '菜品分类', icon: 'Folder', path: '/product/category' },
    { title: '套餐管理', icon: 'Grid', path: '/product/combo' },
    // 配方管理(BOM)已废弃
    { title: '菜品定价', icon: 'Money', path: '/product/pricing' },
    { title: '菜品成本分析', icon: 'DataAnalysis', path: '/product/cost-analysis' },
  ],
}

