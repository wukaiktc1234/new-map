# `hasAuthority('*')` 全库审计（2026-10-28）

## 事实

```text
全库 @PreAuthorize("hasAuthority('*')") 共 537 处，分布在 82 个 Controller
permissions 表里 permission_code='*' 的行数 = 0  （SELECT count(*) 实测）
```

**结论（Lead 2026-10-28 裁定）**：Spring Security 的 `hasAuthority` 是**精确字符串匹配**，`'*'` 从来不是通配符。
这条注解在数据库层面**永远无法满足** —— 不是「数据缺失」，是**代码理解错误**。
唯一能产生 `*` authority 的路径是 `SecurityUser.getAuthorities():73`（admin 角色硬编码添加），
即 `hasAuthority('*')` 实际等价于「只有 admin 角色能进」，但写成了数据权限的样子。

## 清单（按 Controller 聚合，前 25）

| Controller | 处数 |
|------------|------|
| `OrderNew` | 18 |
| `Asset` | 15 |
| `DeviceAlert` | 15 |
| `Coupon` | 14 |
| `HardwareConfig` | 14 |
| `Member` | 13 |
| `Device` | 12 |
| `EmployeeArchiveDetail` | 12 |
| `PrintTask` | 11 |
| `DeviceManagement` | 11 |
| `TakeoutLocker` | 11 |
| `DeviceCommunicationTest` | 11 |
| `OnboardingArchive` | 11 |
| `AIModelConfig` | 10 |
| `ContractTemplate` | 10 |
| `LabelPrinter` | 10 |
| `MarketingMember` | 10 |
| `Training` | 10 |
| `Tray` | 10 |
| `Approval` | 9 |
| `PrintTemplate` | 9 |
| `KitchenOrder` | 9 |
| `Points` | 9 |
| `Seal` | 9 |
| `UserPermissionOverride` | 9 |

## 处置方案（Lead 裁定：方案②，不选「加一条 permission_code='*'」）

1. 逐模块替换为**真实权限码**（`模块:资源:动作`），并在 `permissions` 补码 + 绑角色；
2. 若该 Controller 确实只给超管用 → 改 `hasRole('SUPER_ADMIN')`（Spring 会自动补 `ROLE_` 前缀）；
3. **不要**往 `permissions` 插 `'*'`：那会把「通配语义」塞进数据，换库即崩。

## 已处理

| 位置 | 改动 | 状态 |
|------|------|------|
| `OverAgeWorkerController`（6 处） | → `hasAnyAuthority('hr:over-age-worker:view','hr:over-age-worker:manage')`；`V20261028_011` 补两码并绑 `ROLE_HR_DIRECTOR` | 已改，待 200 验收 |
