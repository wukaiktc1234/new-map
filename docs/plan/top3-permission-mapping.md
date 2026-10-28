# Top 3 模块（资产/设备/订单）权限码对照表（2026-10-28）

目的：改 `hasAuthority('*')` 前先核对**是否已有等价权限码**，避免造重复码。
数据源：`SELECT permission_code FROM permissions WHERE permission_code LIKE '<模块>:%'`（实测）。

## 1. 资产（`AssetController`，15 处 `'*'`）

```
已有：asset:view ｜ asset:manage ｜ asset:delete ｜ asset:finance ｜ asset:inventory
→ **够用，无需补码**。改法：hasAuthority('*') → hasAnyAuthority('asset:view','asset:manage')（读接口）
                                       写接口 → hasAuthority('asset:manage')
                                       删除 → hasAuthority('asset:delete')
```

## 2. 设备（4 个 Controller 共 49 处 `'*'`）

```
已有：device:alert:view/manage ｜ device:communication:view/manage ｜ device:locker:view/manage
      device:manage:view/manage ｜ device:management:view/manage ｜ device:print-task:view/manage
      device:print-template:view/manage ｜ device:scan:view/manage ｜ device:sdk-printer:view/manage
      device:simulator:view/manage
→ **覆盖已很全，无需补码**（每个子域都有 view/manage 对）
```

## 3. 订单（`OrderNewController`，18 处 `'*'`）

```
已有：order:view ｜ order:add ｜ order:create ｜ order:manage ｜ order:pay ｜ order:queue ｜ order:refund ｜ order:refund:initiate
→ **够用，无需补码**
```

## 4. 真正的缺码（本批需补）

| 模块 | 缺失码 | 依据 |
|------|--------|------|
| 供应商评估 | `supplier:evaluation:view` / `supplier:evaluation:manage` | 实测 `permissions` 里 `supplier:%` **一条都没有**，而 `/v1/supplier-evaluations/*` 在用 |

## 结论

**Top 3 无需新增权限码**（已有码覆盖），只需把注解换成真实码 + 给角色绑定；
**唯一缺码的模块是 supplier**（连 `supplier:view` 都没有），本批补两条并绑采购/财务角色。

### 改造风险（必须带负向验证）

```
每模块改完必须实测两条：
  ① 有权限账号 → 200（防止改完仍是 403）
  ② 无权限账号 → 403（防止改完变成「谁都能进」）
```
