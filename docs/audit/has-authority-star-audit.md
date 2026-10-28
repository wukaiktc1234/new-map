# `hasAuthority('*')` 全库审计（2026-10-28）

## 事实（2026-10-28 13:35 重新逐行统计 —— **这是权威版本**）

```text
含 hasAuthority('*') 的 @PreAuthorize 行：534
  ├─ **只有 '*'**（true star-only）：  **2 行**
  │    · POST /v1/admin/hr-migration/create-tables      (HrMigrationController.java:33)
  │    · POST /v1/user-permission-overrides/refresh-expired (UserPermissionOverrideController.java:221)
  └─ **'具体码' or '*'**：            **532 行**
       例：hasAuthority('asset:view') or hasAuthority('*')
```

端点级快照（1439 个端点）：**star-only 2 个 ｜ 码+admin兜底 534 条记录**。

## ⚠ 更正记录（我上一轮报错了，留痕）

| 我曾说 | 真实 | 影响 |
|--------|------|------|
| 「537 处 `hasAuthority('*')` 只有 admin 能进，属已知限制」 | **只有 2 处**是 star-only；其余 532 处 OR 了具体码，普通角色按码即可访问 | 该结论导致我们把「改造」误判成「放开 admin 专用接口」；实际是**收紧（拿掉 admin 兜底）** |
| 「加 `permission_code='*'` 就能让超管进来」 | `'*'` 不是通配符，Spring `hasAuthority` 精确匹配；目录里也从未有该码 | 若照此做，会把「通配语义」塞进数据，换库即崩 |

**为什么会错**：我只统计了「含 `'*'` 的行数」，**没有看同一行里是否还有具体码** —— 少看一步就得出反了的结论。

## 真实语义

```text
'*' 的来源：SecurityUser:73 仅给 admin 角色硬加 SimpleGrantedAuthority("*")；
permissions 目录里不存在 permission_code='*'（实测 count=0）。
因此 `码 or '*'` 的实际准入集合 = { 持有该码的角色 } ∪ { admin }。
→ 删掉 ` or hasAuthority('*')` 的效果是 **收紧**：admin 不再靠兜底，而是靠目录里的同名码。
```

## 处置方案（Lead 2026-10-28 批准）

1. **Top3 模块（资产 15 / 设备 49 / 订单 18）**：逐处删除 ` or hasAuthority('*')`，**保留具体码**，不新增码、不改方法语义；
2. **删前必须逐个核对：该具体码是否在 `permissions` 目录里**（admin 靠目录里的码进）——凡「只在硬编码 ALL、不在目录」的码，**先补目录再删**；
3. **那 2 处 star-only 本轮不动**，标注「已知限制：仅 admin 可调用」，列入**需产品定权限**清单（凭空定义「谁能改用户权限覆盖 / 谁能跑 HR 迁移」不合适）；
4. 验收三角：`admin → 200`、`有码角色 → 200`、`无码角色 → 403`，缺一不算通过。

## 需产品定权限的清单（当前仅 admin 可调用）

| 端点 | 文件 | 问题 |
|------|------|------|
| `POST /v1/admin/hr-migration/create-tables` | HrMigrationController.java | 谁能执行 HR 迁移建表？ |
| `POST /v1/user-permission-overrides/refresh-expired` | UserPermissionOverrideController.java | 谁能刷新过期的权限覆盖？ |
