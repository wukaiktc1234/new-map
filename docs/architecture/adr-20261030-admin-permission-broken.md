# ADR-20261030：Admin 角色权限管理自上线起从未生效（P0）

> **一句话结论**：Admin 的「角色权限」读写的是一个 `@TableField(exist = false)` 的死字段 —— 
> 读永远返回空、写不报错也无效、界面显示「成功」。这是今晚最贵的 P0：**功能在空转且无人察觉**。

## 1. 五层缺陷（同族：一个语义两套实现）

| 层 | 缺陷 | 证据 | 修复 |
|---|---|---|---|
| ① 读 | `getRolePermissions` 读 `roles.permissions`（死字段），永远返回空 | `Role.java` 注释原文「roles 表无 permissions 列，此字段不映射数据库」+ `@TableField(exist = false)`；接口实测返回 0 条（DB 实际 119/39 条） | 改为查 `role_permissions` **并 JOIN `permissions` 取 `permission_code`**（`PERM_*` 是内部 id，直接返回内容全错） |
| ② 写 | `assignPermissions` 写同一死字段，`updateById` 不报错但零改动 | `RoleServiceImpl` 原实现 `role.setPermissions(json); updateById(role);` | 改为「前置校验 → 软删目标 role → INSERT...SELECT upsert」，四条防护 |
| ③ 版本 | `permissionVersion()` = `COUNT(*) + MAX(create_time)`，**软删两项都不变** → 撤权永不刷新缓存 | `UserMapper:70` 原 SQL；注释却写「撤权立即生效」 | 加入「有效数」`SUM(CASE WHEN deleted=0 THEN 1 ELSE 0 END)` |
| ④ 代码提权 | `POS_BASE_PERMISSIONS` 无条件并入 6 个 pos 码 + 3 个 product 码给**所有 POS 登录者** | `PosAuthServiceImpl:196-210`；vtest01（ROLE_STORE_MANAGER，DB pos=0）实测 /me pos=**6** | 改为「仅当 DB 授权为空时兜底」并打 `[SECURITY] permission_empty_fallback` 告警 |
| ⑤ 数据缺口 | ROLE_STORE_MANAGER 的 119 条权限里 **0 个 pos 码**；ROLE_CASHIER 只有 5/10 | `role_permissions` 按角色统计 | 迁移 017（12 条授权）+ 018（修正组合：给 manager 补 pay、撤 yolo） |

## 2. 迁移 017 / 018 的目标集合

```
ROLE_CASHIER (10)      : member:consume/recharge/query, menu:query, order:create/pay/query/void,
                         shift:manage, yolo:recognize    ← 全部 pos 码
ROLE_STORE_MANAGER (7) : order:create/pay/query/void, shift:manage, member:query, menu:query
                         ← 不含 cashier-only：member:consume / member:recharge / yolo:recognize
```

**017 的组合错误由 018 修正**：017 给了 manager `pos:yolo:recognize` 却没给 `pos:order:pay` ——
按 Lead 的业务口径（店长要能收款、yolo 属收银员专属），018 补 pay、撤 yolo，条数仍为 7。
（017 已 success=true，按纪律不得修改，故用 018 纠正。）

## 3. 验收数据（全部实测）

```
读接口：GET /v1/roles/9/permissions   → 119 条（修复前 0），与 DB count 一致
        GET /v1/roles/4216/permissions → 39 条（修复前 0），其中 pos 码 5 个（修复中一度返回 PERM_* 内部 id）
        JOIN 前后 count 均 39 → 无孤儿行
版本：  1746-1746-… → 软删 → 1746-1745-… → 恢复 → 1746-1746-…（三段全部不同/复原）
        该 SQL EXPLAIN ANALYZE execution time = 0.279 ms（1746 行，1s 一次可接受）
缓存：  ft_gate 撤权 /me 10→9 用时 0.5s；恢复 9→10 用时 0.5s
        主库经 Admin 写接口撤权 /me 10→9 用时 0.5s；恢复 0.5s；快照逐条比对 44 条完全一致
写接口：组1 119→120(pos 0→1)；组2 回 119(pos 0) 且逐条比对快照一致；
        组3 ROLE_CASHIER 仍 39/5（只动目标 role_id）；
        组4 pos:fake:code → HTTP 400「部分权限码不存在，已拒绝（期望 120，有效 119）」，两角色条数完全不变
段 2：  删除无条件并入后 vcashier01 /me pos = 10（全来自 DB）｜vtest01 /me pos = 7（全来自 DB）
```

## 4. 写接口的四条防护

```
① 只动目标 role_id：UPDATE role_permissions SET deleted=1 WHERE role_id=? AND deleted=0
② 空列表保护：permissions 为空 → BusinessException(400)「不允许清空角色权限」
③ 变更前后对比日志：[PERM-CHANGE] role=… before=… after=… softDeleted=… upserted=…
④ 缓存失效：version 由「有效数」自动变化（0.5s 内生效）+ Controller 侧 permissionCacheInvalidator 双保险
⑤ 前置校验：countValidCodes 在软删之前执行 → 非法码直接被拒，不产生任何写入
   （因此 `upserted < n` 回滚分支当前不可达，保留作防御，已在代码注释说明）
```

## 5. 教训（今晚第二次「数量对上但内容错」）

```
「接口返回条数与 DB 计数一致」≠「返回内容正确」。
实例：修复中途返回 39 条（条数完全正确），但内容是 PERM_* 内部 id 而非 pos:* 业务码。
规则：任何数量断言都必须配一条【内容样例断言】（至少抽查 3 条具体值）。
```

## 6. 待做

| 项 | 下一步 | 验收判据 |
|---|---|---|
| `roles.permissions` 死字段清理 | 删除实体字段 + ObjectMapper 残留调用 | 全仓 grep `getPermissions()` on Role 为 0 |
| POS_BASE_PERMISSIONS 兜底告警监控 | 监控 `[SECURITY] permission_empty_fallback` 出现次数 | 生产长期为 0（否则说明有角色漏配） |
| ft_gate 的 017/018 未走 Flyway | 已在隔离库用 psql 验证两次，主库由 Flyway 应用 | ft_gate 的 flyway 历史终将补齐或重建（见 ft_verify 工程） |
