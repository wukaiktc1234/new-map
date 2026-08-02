import type { MenuGroupConfig } from '@/types/permission'
import { UserRole } from '@/stores/permission'

export const financeMenu: MenuGroupConfig = {
  id: 'finance',
  title: '财务中心',
  icon: 'Money',
  path: '/finance',
  order: 80,
  category: 'finance',
  visibleRoles: [UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCE_DIRECTOR],
  children: [
    // ===== standard 及以上可见（单店/标准连锁/大型连锁） =====
    // 注：mini（单人店）整个财务中心菜单组不可见（mini-single模板domainMatrix不含finance域）
    { title: '财务总账', icon: 'Notebook', path: '/finance/ledger',
      scaleLevel: ['standard', 'chain-standard', 'chain-enterprise'] },
    { title: '会计科目', icon: 'List', path: '/finance/subject',
      scaleLevel: ['standard', 'chain-standard', 'chain-enterprise'] },
    { title: '会计期间', icon: 'Calendar', path: '/finance/period',
      scaleLevel: ['standard', 'chain-standard', 'chain-enterprise'] },
    { title: '应收账款', icon: 'Wallet', path: '/finance/receivable',
      scaleLevel: ['standard', 'chain-standard', 'chain-enterprise'] },
    { title: '应付账款', icon: 'Wallet', path: '/finance/payable',
      scaleLevel: ['standard', 'chain-standard', 'chain-enterprise'] },
    { title: '成本管理', icon: 'TrendCharts', path: '/finance/cost',
      scaleLevel: ['standard', 'chain-standard', 'chain-enterprise'] },
    { title: '税务管理', icon: 'Coin', path: '/finance/tax',
      scaleLevel: ['standard', 'chain-standard', 'chain-enterprise'] },
    { title: '发票报销', icon: 'Money', path: '/finance/invoice-reimbursement',
      scaleLevel: ['standard', 'chain-standard', 'chain-enterprise'] },
    { title: '财务报表', icon: 'Document', path: '/finance/report',
      scaleLevel: ['standard', 'chain-standard', 'chain-enterprise'] },
    // ===== chain-standard 及以上可见（标准连锁/大型连锁） =====
    { title: '预算管理', icon: 'Calendar', path: '/finance/budget',
      scaleLevel: ['chain-standard', 'chain-enterprise'] },
    { title: '资金管理', icon: 'Wallet', path: '/finance/fund',
      scaleLevel: ['chain-standard', 'chain-enterprise'] },
    { title: '财务审批', icon: 'Stamp', path: '/finance/approval',
      scaleLevel: ['chain-standard', 'chain-enterprise'] },
    // ===== chain-enterprise 可见（大型连锁） =====
    { title: '自动凭证管理', icon: 'Connection', path: '/finance/auto-voucher',
      scaleLevel: ['chain-enterprise'] },
  ],
}
