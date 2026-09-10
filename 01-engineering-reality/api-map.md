# 食品溯源系统 API 地图

## API 概况

| 指标 | 数量 | 备注 |
|------|------|------|
| Controller 总数 | 154 | 系统所有 REST 控制器 |
| 子目录 | 18 | 按业务域划分的 API 分组 |
| 核心 API 链路 | 多条 | 见下方核心链路说明 |

---

## API 分类目录

### 1. 认证 / 用户 / 权限

| 路由前缀 | 用途 |
|-----------|------|
| `/v1/auth/*` | 登录、注册、Token 管理 |
| `/v1/users/*` | 用户 CRUD、角色分配 |
| `/v1/roles/*` | 角色管理 |
| `/v1/permissions/*` | 权限定义与分配 |

### 2. 产品

| 路由前缀 | 用途 |
|-----------|------|
| `/v1/product-center/foods` | 食品信息管理 |
| `/v1/food-categories` | 食品分类 |
| `/v1/dish-recipes` | 菜品配方 |
| `/v1/dish-combos` | 套餐组合 |

### 3. 订单

| 路由前缀 | 用途 |
|-----------|------|
| `/v1/orders` | 订单主表 |
| `/v1/order-items` | 订单明细 |
| `/v1/pos/orders/*` | POS 端订单 |

### 4. 库存

| 路由前缀 | 用途 |
|-----------|------|
| `/v1/inventory/*` | 中央库存 |
| `/v1/store-inventory/*` | 门店库存 |

### 5. 采购

| 路由前缀 | 用途 |
|-----------|------|
| `/v1/purchase/*` | 采购单管理 |
| `/v1/suppliers/*` | 供应商管理 |

### 6. 财务

| 路由前缀 | 用途 |
|-----------|------|
| `/v1/finance/payables` | 应付账款 |
| `/v1/finance/payments` | 付款记录 |
| `/v1/finance/receipts` | 收款记录 |
| `/v1/finance/vouchers` | 凭证 |
| `/v1/finance/accounting-subjects` | 会计科目 |
| `/v1/finance/fund-flows` | 资金流水 |
| `/v1/finance/tax-records` | 税务记录 |

### 7. HR

| 路由前缀 | 用途 |
|-----------|------|
| `/v1/employees/*` | 员工管理 |
| `/v1/departments/*` | 部门管理 |
| `/v1/positions/*` | 岗位管理 |
| `/v1/salary/*` | 薪资管理 |

### 8. 设备

| 路由前缀 | 用途 |
|-----------|------|
| `/v1/devices/*` | 设备管理 |
| `/v1/hardware/*` | 硬件接口 |

### 9. 通知

| 路由前缀 | 用途 |
|-----------|------|
| `/v1/notifications/*` | 消息通知 |

### 10. 任务

| 路由前缀 | 用途 |
|-----------|------|
| `/v1/tasks/*` | 任务调度与管理 |

### 11. 排班

| 路由前缀 | 用途 |
|-----------|------|
| `/v1/schedule/*` | 排班管理 |

### 12. 营销

| 路由前缀 | 用途 |
|-----------|------|
| `/v1/members/*` | 会员管理 |
| `/v1/coupons/*` | 优惠券 |
| `/v1/promotions/*` | 促销活动 |

### 13. 溯源

| 路由前缀 | 用途 |
|-----------|------|
| `/v1/traceability/*` | 食品溯源查询与上报 |

### 14. 资产

| 路由前缀 | 用途 |
|-----------|------|
| `/v1/assets/*` | 固定资产管理 |

### 15. 系统

| 路由前缀 | 用途 |
|-----------|------|
| `/v1/system/*` | 系统配置 |
| `/v1/audit-logs/*` | 审计日志 |

---

## 核心 API 链路

```
登录 → 认证鉴权 → 业务操作 → 审计日志
  ↓
用户/角色/权限 → 资源访问控制
  ↓
产品管理 → 订单 → 库存 → 财务
  ↓
溯源链路: 产品 → 生产 → 采购 → 库存 → 订单 → 溯源查询
```

---

## 零消费 / 断链 API

以下 API 端点存在但**无前端消费或无有效调用点**，需重点关注：

| 端点 | 方法数 | 状态说明 |
|------|--------|----------|
| `/v1/self-purchase/*` | 6 | 无前端消费，可能是废弃接口 |
| `/v1/tasks/*` | 12 | 无前端消费，可能是后端内部调度 |
| `/v1/plan-items/*` | 7 | 无前端消费 |
| `/v1/appeals/*` | 5 | 无前端消费 |
| `AutoVoucherService.generateSalesVoucher` | — | 零调用点 |
| `AutoVoucherService.generatePurchaseVoucher` | — | 零调用点 |
| `FinanceVoucherService`（根包） | — | no-op stub，空实现 |

### 零消费 API 清单详情

#### `/v1/self-purchase/*` (6 方法)
- 预计用途：自采管理
- 现状：前端未接入，建议评估是否保留

#### `/v1/tasks/*` (12 方法)
- 预计用途：后台定时任务管理
- 现状：可能由调度系统直接调用，无前端界面

#### `/v1/plan-items/*` (7 方法)
- 预计用途：计划明细
- 现状：无前端消费

#### `/v1/appeals/*` (5 方法)
- 预计用途：申诉处理
- 现状：无前端消费

#### Finance 相关零调用
- `AutoVoucherService.generateSalesVoucher` — 无任何调用点
- `AutoVoucherService.generatePurchaseVoucher` — 无任何调用点
- `FinanceVoucherService`（根包） — no-op stub，空实现，未接入业务流程

---

## 建议行动项

1. **清理零消费 API**：评估 `/v1/self-purchase/*`、`/v1/plan-items/*`、`/v1/appeals/*` 是否仍需要，无用则移除
2. **补全 tasks 接口文档**：12 个 task 方法无前端消费，需确认是否为后端内部接口
3. **修复财务自动凭证**：`AutoVoucherService` 两个方法零调用，检查是否应接入订单/采购流程
4. **移除 no-op stub**：`FinanceVoucherService` 根包为空实现，确认是否应删除或完成实现

---

*生成时间：2026-09-09*
*数据来源：代码库静态分析*