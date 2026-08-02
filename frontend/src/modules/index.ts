/**
 * 模块注册统一入口
 *
 * 导入所有模块的菜单配置（纯数据，无副作用）
 * 通过 registerAllMenus() 在 Pinia 安装后统一注册到 Store
 * 新增模块时只需在此文件添加两行：import + push 到数组
 */

import type { MenuGroupConfig } from '@/types/permission'
import { usePermissionStore } from '@/stores/permission'

import { workspaceMenu } from '@/modules/workspace/menu'
import { requestCenterMenu } from '@/modules/request-center/menu'
import { productMenu } from '@/modules/product/menu'
import { orderMenu } from '@/modules/order/menu'
import { operationsMenu } from '@/modules/operations/menu'
import { storeManagementMenu } from '@/modules/store-management/menu'
import { purchaseMenu } from '@/modules/purchase/menu'
import { warehouseMenu } from '@/modules/warehouse/menu'
import { memberMenu } from '@/modules/member/menu'
import { financeMenu } from '@/modules/finance/menu'
import { assetMenu } from '@/modules/asset/menu'
import { hrMenu } from '@/modules/hr/menu'
import { traceabilityMenu } from '@/modules/traceability/menu'
import { deviceMenu } from '@/modules/device/menu'
import { sealMenu } from '@/modules/seal/menu'
import { systemMenu } from '@/modules/system/menu'

/**
 * 所有模块的菜单配置（纯数据声明，不触发 Store 调用）
 *
 * 【架构原则】管理端仅承载管理后台功能，不集成 POS端/后厨端 业务页面：
 * - POS端：独立部署，订单收款成功后弹出托盘绑定界面（含绑定/解除、使用记录、状态查询）
 * - 后厨端：独立部署，作为状态展示页（订单来源明细 + 托盘出餐状态）
 * - 管理端：仅在订单详情中展示托盘使用数据，不执行任何业务步骤
 */
const allMenuConfigs: MenuGroupConfig[] = [
  requestCenterMenu,
  workspaceMenu,
  productMenu,
  orderMenu,
  operationsMenu,
  storeManagementMenu,
  purchaseMenu,
  warehouseMenu,
  memberMenu,
  financeMenu,
  assetMenu,
  hrMenu,
  traceabilityMenu,
  deviceMenu,
  sealMenu,
  systemMenu,
]

/**
 * 将所有模块的菜单配置注册到 PermissionStore
 * 必须在 app.use(pinia) 之后调用（此函数内部才会执行 usePermissionStore()）
 */
export function registerAllMenus(): void {
  const store = usePermissionStore()
  for (const config of allMenuConfigs) {
    store.registerMenuGroup(config)
  }
  // F-008: 注册完成后锁定，防止运行时动态注入菜单
  store.lockRegistration()
}
