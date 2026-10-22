# 全仓注解权限字面量核对报告（N31 / N32）

> **扫描器**：`tools/scan_auth_annotations.py`（可复跑，见 §10 复现命令）
> **扫描面**：`backend/src/main/java` 全部 `.java`
> **比对基准**（活库 `food_traceability`，2026-10-15 时点）：`roles.role_code` ∪ `roles.role_id_str` → **19** 个归一化角色码；`permissions.permission_code` → **132** 个权限码；`AdminPermissions.ALL`（硬编码）→ **352** 个码
> **原始数据**：`docs/audit/auth-annotation-literal-audit-001-raw.md`（分类明细）、`auth-annotation-literal-audit-001.json`（逐条机器可读）

## 1. 摘要

| 指标 | 值 |
|---|---|
| 注解内字符串字面量总数 | **2526** |
| 有效（库中存在） | **1461**（57.8%） |
| A 类 角色幻码 | **12** 处 |
| B 类 权限码幻码 | **1031** 处（**286** 个不同码） |
| C 类 完全未定义 | **22** 处 |
| 其中「**无任何人可访问**」的硬死端点 | **67 处 / 34 个码** |

**一句话结论**：注解引用了 **372 个不同权限码**，而 `permissions` 目录只有 **132** 个 —— 其中只有 **86 个**能在库里对上，**286 个对不上**。这批失配里，**568 处**靠 `hasAuthority('*')` 兜底给 admin、**396 处**靠 admin 硬编码白名单兜底、**67 处两者都没有 → 连 admin 都进不去**。

## 2. 扫描口径

| 项 | 规则 |
|---|---|
| 覆盖注解 | `@PreAuthorize`（1878 处）、`@RequiresPermission`（56 处）；同时兼容 `@PostAuthorize` / `@Secured` / `@RolesAllowed` / `@RequiresPermissions`（当前全仓 0 处） |
| 取词范围 | `hasRole` / `hasAnyRole` / `hasAuthority` / `hasAnyAuthority` 的括号内实参；`@RequiresPermission` 取 `value()`（`action()`/`description()` 仅元数据，见 `PermissionAspect` L77） |
| 排除 | `'*'`、空串、含 `$` / `#{` 的表达式、含 `+` 的拼接、注释行里的示例注解 |
| 转义处理 | 正确区分源码里的 `\'` 写法（`@PreAuthorize("hasAnyRole(\'a\', \'b\')")`） |
| 角色归一 | 去 `ROLE_` 前缀 + 小写后比对（与 A 卡 `RoleCodeNormalizer` 同口径） |
| 分类判定 | A = 归一后能用 `difflib`（cutoff 0.6）在真实角色码里找到近邻；B = 含 `:` 但不在 `permissions`；C = 既无近邻角色也无该权限 |

## 3. 分类总览

| 分类 | 出现次数 | 不同字面量 | 说明 |
|---|---|---|---|
| VALID | 1461 | — | 库中存在，放行 |
| **A 角色幻码** | 12 | 1 | `ROLE_MANAGER`，近邻指向 `store_manager` / `region_manager` / `finance_manager` |
| **B 权限码幻码** | 1031 | 286 | `permissions` 表无此码 |
| **C 完全未定义** | 22 | 4 | `ROLE_CASHIER` / `ROLE_POS_OPERATOR` / `ROLE_CHEF` / `ROLE_KITCHEN` |

## 4. A 类：角色幻码（12 处，全部同一个码）

| 字面量 | 次数 | 近邻建议 | 位置 |
|---|---|---|---|
| `ROLE_MANAGER` | 12 | `store_manager`, `region_manager`, `finance_manager` | `kitchen/KitchenOrderController` L436,453,469,482,495；`pos/PosApiController` L26；`pos/PosCouponController` L29；`pos/PosOrderController` L29；`pos/YoloProductRecognitionController` L24；`service/impl/PosOrderPaymentServiceImpl` L104,280,699 |

全部出现在 `hasAnyAuthority(...)` 中（authority 位）。语义上「经理」在多处被使用，具体映射需按业务定（POS/厨房场景大概是 `store_manager`）。

## 5. B 类：权限码幻码（1031 处 / 286 个码）—— 按「能不能进」分三层

| 层 | 处数 | 判定依据 | 实际后果 |
|---|---|---|---|
| **B1 有 `'*'` 兜底** | **568** | 同一注解内含 `hasAuthority('*')` | admin 可进（`*`）；**其他角色一律 403** |
| **B2 无 `'*'` 但码在 `AdminPermissions.ALL`** | **396** | admin 硬编码白名单覆盖 | admin 可进；其他角色 403 |
| **B3 无 `'*'` 且不在 `AdminPermissions.ALL`** | **67** | 两条路都没有 | **任何人都进不去（含 admin）** |

### B3 死路清单（34 个码，67 处调用点）

```
admin:user:export               backup:config                  backup:create
backup:delete                   backup:download                backup:query
backup:restore                  file:query                     finance:receipt:delete
finance:receipt:edit            label:template:print           label:template:view
notification:deadletter:delete  notification:deadletter:query  notification:deadletter:retry
notification:event:test         notification:message:query     notification:message:send
notification:setting:edit       notification:setting:query     notification:statistics:query
notification:template:create    notification:template:delete   notification:template:edit
notification:template:query     product:analysis:export        product:analysis:view
scheduler:task:create           scheduler:task:delete          scheduler:task:edit
scheduler:task:query            scheduler:task:trigger         system:stats:view
warehouse:location:delete
```

原文抽验（确认不是扫描误判）：

```java
DataBackupController.java:90        @PreAuthorize("hasAuthority('backup:query')")
NotificationController.java:244     @PreAuthorize("hasAuthority('notification:message:send')")
ScheduledTaskController.java:273    @PreAuthorize("hasAuthority('scheduler:task:trigger')")
```

> 注：`label:template:print` 有两条调用点 —— L889 带 `or hasAuthority('*')`（admin 可进），L1039 不带（死路）。因此「34 个码」的口径是「至少有一处调用点无人可满足」。

### B 类的高频 TOP 10

| 码 | 次数 | 码 | 次数 |
|---|---|---|---|
| `trace:query` | 40 | `system:role:read` | 22 |
| `schedule:view:self` | 25 | `system:user:read` | 21 |
| `trace:update` | 21 | `system:permission:read` | 21 |
| `inventory:update` | 16 | `member:view` | 17 |
| `order:queue` | 15 | `system:permission:manage` | 16 |

## 6. C 类：完全未定义（22 处 / 4 个码）

| 字面量 | 次数 | 位置（全部在 `hasAnyAuthority`） |
|---|---|---|
| `ROLE_CASHIER` | 6 | `pos/PosApiController` L26、`pos/PosCouponController` L29、`pos/PosOrderController` L29、`pos/YoloProductRecognitionController` L24、`service/impl/PosOrderPaymentServiceImpl` L104,699 |
| `ROLE_POS_OPERATOR` | 6 | 同上（同 6 行） |
| `ROLE_CHEF` | 5 | `kitchen/KitchenOrderController` L436,453,469,482,495 |
| `ROLE_KITCHEN` | 5 | 同上（同 5 行） |

这 4 个码在 `roles` 表里没有任何对应角色（既非 `role_code` 也非 `role_id_str`），在 `permissions` 里也没有，是纯新生概念 → **必须由业务裁决**：补建角色，还是映射到现有角色（如 `store_manager`）。

## 7. 位置错配（A 卡归一化覆盖不到的那一类）

| 标记 | 次数 | 说明 |
|---|---|---|
| `ROLE_IN_AUTHORITY_POSITION` | **46** | `ROLE_` 前缀角色码写在 `hasAuthority/hasAnyAuthority`。前缀是对的，但角色必须真实存在 |
| `PERMISSION_IN_ROLE_POSITION` | **0** | 没有「权限码写进 `hasRole`」的情况 |
| `BARE_ROLE_IN_AUTHORITY_POSITION` | **0** | 没有「裸角色码写进 `hasAuthority`」的情况 |

46 处构成：`ROLE_ADMIN` 12（有效）+ `ROLE_MANAGER` 12（A）+ `ROLE_CASHIER` 6 + `ROLE_POS_OPERATOR` 6（C）+ `ROLE_CHEF` 5 + `ROLE_KITCHEN` 5（C）= 46。

**这就是 POS/厨房那批的完整口径**（含此前口头统计的「40 处」——差值是 `ROLE_ADMIN` 12 处属有效、不应计入幻码）。

## 8. 四象限缺口（目录 ↔ 注解 ↔ 白名单）

| 关系 | 数量 | 含义 |
|---|---|---|
| 注解引用、DB 目录缺失 | **286** 个码 | B 类来源；`role_permissions` 只能授目录内的码 → 无法授予 |
| `AdminPermissions.ALL` 有、DB 目录无 | **256** 个码 | admin 靠硬编码白名单放行，**与 DB 目录已严重分叉** |
| DB 目录有、注解从未引用 | **46** 个码 | 目录里存在但无端点使用（是否清理另议） |
| DB 目录有、但从未授予任何角色 | **2** 个码 | 已入目录却无人可持有 → 相关端点同样是死路 |
| `role_permissions` 孤儿授权（码不在目录） | **2** 行 | 授权面与目录面也不完全一致 |

## 9. `SecurityUser` authorities 组装分析（Owner 第 6 问）

代码事实（`security/model/SecurityUser.java`，A 卡改后）：

| 行号 | 行为 |
|---|---|
| L47–76 | 遍历 `roles`：`RoleCodeNormalizer.normalize()` → 加 `ROLE_<全大写>`（L60–61）+ `ROLE_<小写>` 变体（L62–65）；admin 角色额外加 `'*'`（L70–74） |
| L78–89 | 遍历 `permissions`：**原样**加入 authority（L84–88，跳过 `'*'` 本身） |
| L96–101 | `isAdmin` 时把 `AdminPermissions.ALL`（352 个码）全部展开进来 |

**答问**：

1. **`permissions` 和 `roles` 都装进去了** —— roles 以 `ROLE_<CODE>` 形式、permissions 以裸码形式。
2. **`hasAuthority` 不是「只找 permissions、漏了 roles 转换」** —— roles 确实被转换成 authority（`ROLE_<CODE>`）。真正约束是：`hasAuthority('X')` 里若写**裸角色码**（如 `store_manager`），authority 集合里只有 `ROLE_STORE_MANAGER`，字符串不等 → 必然不匹配。**本仓实测这种写法 0 处**。
3. 因此 POS/厨房那 46 处的失败**不是组装漏了**，而是**角色码在 DB 里根本不存在**（A/C 类）→ 没有任何用户的 authority 会出现 `ROLE_CASHIER` 等。
4. 两个结构性事实须一并记录：
   - admin 的权限实际来自**硬编码 352 码白名单**（`AdminPermissions.ALL`），不是 DB 目录（132 码）→ 已分叉 **256** 个码；
   - `hasAuthority('*')` 不是通配匹配，仅在注解**显式写了** `or hasAuthority('*')` 时才放行 admin。

## 10. 复现命令

```powershell
$env:PGPASSWORD="123456"
python tools/scan_auth_annotations.py `
  --root backend/src/main/java `
  --psql "P:\my-new-project\PostgreSQL\18\bin\psql.exe" `
  --dbname food_traceability --dbuser postgres `
  --md-out docs/audit/auth-annotation-literal-audit-001-raw.md `
  --json-out docs/audit/auth-annotation-literal-audit-001.json
```

离线模式：`--snapshot snapshot.json`（内含 `role_codes` / `permission_codes` 两个数组），便于无库环境或 CI 复跑。

## 11. 结论与建议动作（待 Owner 裁决，本轮未改任何代码）

1. **B 类是本报告最大体量问题**（1031 处 / 286 码）：286 个码在 `permissions` 目录缺失。两条路可组合：
   - ① 补 seed 迁移把缺码入目录并绑定角色（治本，工作量大）；
   - ② 把注解收敛到「目录内已有码」，多余端点改 `hasRole` 或 `isAuthenticated()`。
2. **B3 那 34 个死路码优先处理**（67 处）：当前**无论如何都进不去**（备份 / 通知 / 调度 / 标签模板 / 文件查询 / 仓库位置删除等）——补码入库，或至少补 `or hasAuthority('*')` 让 admin 可操作。
3. **A 类 12 处**：`ROLE_MANAGER` → 建议 `ROLE_STORE_MANAGER`（POS/厨房场景），需 Owner 确认。
4. **C 类 22 处**：`ROLE_CASHIER` / `ROLE_POS_OPERATOR` / `ROLE_CHEF` / `ROLE_KITCHEN` 需业务裁决：补建 4 个角色，还是并入 `store_manager` / `employee`。
5. **`AdminPermissions.ALL`（352）与 DB 目录（132）分叉 256 码** 是独立的结构性治理项，建议单列。
