# 权限模型（写死，不许再变）

> 2026-10-28 定稿。本文档的作用是**防止第三次踩同一个坑**：当天在权限这块连续错了两次，
> 两次都源自分不清 `permission_id_str` 与 `permission_code` 的职责，以及用「推断」代替「读代码」。
> 后续任何涉及权限的改动，**先读本文件再动手**；若与本文件冲突，以本文件为准并回来改代码。

## 1. 两套形态，职责不同（核心）

| 列 | 形态示例 | 用途 | 谁依赖它 |
|----|----------|------|----------|
| `permissions.permission_id_str` | `PERM_HR_CONTRACT_TEMPLATE_VIEW` | **join 键** | `role_permissions.permission_id` / `user_permissions.permission_id` 必须**等于**它 |
| `permissions.permission_code` | `hr:contract-template:view` | **授予给 Spring Security 的 authority 字符串** | `@PreAuthorize("hasAuthority('hr:contract-template:view')")` 匹配的是它 |

**一句话**：`permission_id_str` 管「绑得上」，`permission_code` 管「对得上」。两者不能互换。

## 2. 授权链（实际 SQL，见 `backend/src/main/resources/mapper/UserMapper.xml`）

```sql
-- getUserRolePermissions（登录时装载 authority）
SELECT DISTINCT p.PERMISSION_CODE              -- ← authority 取自这里
  FROM user_roles ur
  JOIN roles r            ON ur.ROLE_ID      = r.role_id_str
  JOIN role_permissions rp ON r.role_id_str  = rp.ROLE_ID
  JOIN permissions p      ON rp.PERMISSION_ID = p.permission_id_str  -- ← join 用 id_str
 WHERE ur.USER_ID::bigint = #{userId} AND ur.DELETED = 0 AND r.DELETED = 0 AND rp.DELETED = 0;
```

链路文字版：
```
user ──user_roles.role_id──▶ roles.role_id_str
                                  │
                                  └─role_permissions.role_id ──▶ role_permissions.permission_id
                                                                        │ 必须 = permissions.permission_id_str
                                                                        ▼
                                                            权限行 → 交给 Spring Security 的是 permission_code
```

## 3. 三条断言各查什么（不许混用）

| 断言 | 查什么 | 反例 |
|------|--------|------|
| **A44** | `role_permissions.permission_id` 必须是某个 `permissions.permission_id_str`（否则 join 丢弃，授权静默消失） | 用冒号形态插 `role_permissions` → 6 行孤儿，用户拿不到权限、也不报错 |
| **A45** | 引用可解析：permission 侧（`role_permissions.permission_id` → `permissions`）与 role 侧（`user_roles.role_id` → `roles.role_id_str`）都存在 | `role_id` 写数字 `1` → 超管授权 418 行被 join 丢弃 |
| **A46** | **代码 ↔ 目录对应**：`hasAuthority('X')` 的 X 必须等于某个 `permission_code`；`hasRole('Y')`/`hasAnyRole(...)` 的 Y（补 `ROLE_` 前缀后）必须等于某个 `roles.role_id_str` 或 `role_code` | 把 `permission_id_str` 当 authority 比对 → 311 条假缺失 |

A46 的实现：`scripts/qa/permission_code_audit.py`，已接入 `preflight` 作为固定 gate（缺失 ≠ 0 → EXIT=2）。
扫描前**必须剥注释**（javadoc 里常有 `hasAuthority('xxx')` 示例），并**剥掉 Java 源码里的 `\'` 转义**（`hasAnyRole(\'admin\')` 这种写法存在）。

## 4. 三个反例（2026-10-28 当天全踩过）

### 反例 1：用冒号形态插 `role_permissions` → join 丢弃（6 行孤儿）
```sql
-- 错
INSERT INTO role_permissions(role_id, permission_id) VALUES ('ROLE_ADMIN','hr:contract-template:view');
-- 对（permission_id 必须是 permission_id_str）
INSERT INTO role_permissions(role_id, permission_id)
SELECT 'ROLE_ADMIN', p.permission_id_str FROM permissions p WHERE p.permission_code='hr:contract-template:view';
```
后果：`getUserRolePermissions` 的 `rp.PERMISSION_ID = p.permission_id_str` 匹配不上 → 权限静默丢失，接口只回 403，无任何日志。

### 反例 2：`role_id` 写数字形态 → 超管授权 418 行被丢
```sql
-- 错：user_roles.role_id='1'
-- 对：user_roles.role_id='ROLE_ADMIN'（= roles.role_id_str）
```

### 反例 3：把 `permission_id_str` 当 authority 比对 → 311 条假缺失
A46 第一版用 `permission_id_str` 比对 `@PreAuthorize` 里的冒号码，直接报 311 条「目录缺失」，
而实际上这些权限全部存在（在 `permission_code` 列里）。**A46 最终口径 = 比 `permission_code`**，写死。

## 5. 查询陷阱（两个方向各自错过一次）

| 你想确认 | 该查哪列 | 今天错在哪 |
|----------|----------|------------|
| 「这条权限存不存在？」 | `permissions.permission_code`（冒号形态在这列） | 查了 `permission_id_str LIKE '%contract%'` → 0 行 → 误判「目录缺条目」，进而插了重复行（已用 V20261028_008 回滚） |
| 「这条绑定能不能 join 上？」 | `permissions.permission_id_str`（PERM_* 形态） | 直接把冒号形态插进 `role_permissions` → join 丢弃 |

**记忆口诀**：
```
冒号码（a:b:c） → 只在 permission_code 找；插 role_permissions 必须先换成 permission_id_str
PERM_ 码        → 只在 permission_id_str 找；它就是 join 键
```

## 6. 改权限时的检查清单

1. 新增权限：`INSERT INTO permissions(permission_id_str='PERM_XXX', permission_code='a:b:c', …)` —— 两列都要填对形态；
2. 绑定角色：`role_permissions.permission_id` 用 `permission_id_str`（`SELECT … WHERE permission_code='a:b:c'` 取）；
3. 绑定用户：`user_roles.role_id` 用 `roles.role_id_str`（`ROLE_*`）；
4. 改完跑 `python scripts/qa/permission_code_audit.py`（A46）与巡检 A44/A45；
5. 真调一次目标接口确认 200/业务码（**403 与 500 都要看日志**，权限/缺表都会伪装成别的错）。


## 历史上出现过的「脏形态」清单（每出现一种就丢一批授权）

> 规律：`role_permissions.permission_id` 本应**只**存 `permissions.permission_id_str`（`PERM_*` 形态）。
> 但历史上不同人/不同脚本往同一列塞进了 5 种形态，**每混进一种，就有一批授权静默失效**（A44/A45 报 rows=1 之类的现象）。

| # | 脏形态 | 表现 | 识别 SQL | 处置 |
|---|--------|------|----------|------|
| 1 | **冒号形态权限码**写进 `permission_id` | 授权行永远匹配不上 `permission_id_str`（`PERM_*`），权限静默丢失；A44/A45 FAIL | `SELECT * FROM role_permissions WHERE permission_id NOT LIKE 'PERM_%';` | 按 `permission_code` 映射回 `permission_id_str`（**只改授权行**） |
| 2 | **数字 role_id** 写进 `role_id` | `user_roles.role_id` 是 `ROLE_*` 字符串，数字形态导致角色绑定不上（418 行历史事故） | `SELECT * FROM role_permissions WHERE role_id ~ '^[0-9]+$';` | 按 `roles.role_id` 与 `role_id_str` 映射表归一 |
| 3 | **`permission_id_str` 当 Spring authority 用** | 注解写 `hasAuthority('PERM_XXX')`，而运行时 authority 是 `permission_code`（`a:b:c`）→ 311 处假缺失 | `grep -rn "hasAuthority('PERM_" backend/src` | 注解一律改 `permission_code` 形态 |
| 4 | **`hasAuthority('*')`** | `'*'` 不是通配符（精确匹配），且目录里无该行 → 只有 admin 能进（537 处，已知限制） | `grep -rn "hasAuthority('\\*')" backend/src` | 改真实权限码，或明确 `hasRole('SUPER_ADMIN')` |
| 5 | **目录自身** `permission_id_str` 是冒号形态 | 目录 4 行（`inventory:query` 等）id_str 与 code 相同 → 授权行跟着「合法但形态脏」 | `SELECT permission_id_str FROM permissions WHERE permission_id_str NOT LIKE 'PERM_%';` | **需 Owner 批准后一次性改目录**（本次未动） |

### 2026-10-28 实测结果

```sql
-- A44/A45 分母 1733，修复前 FAIL rows=1
UPDATE role_permissions rp SET permission_id = p.permission_id_str
  FROM permissions p
 WHERE p.permission_code = rp.permission_id AND rp.permission_id NOT LIKE 'PERM_%';
-- 结果：a44/a45 失败 0 ｜ 仍有 16 行 permission_id 非 PERM_（因其指向的是「目录里 id_str 本身就是冒号形态」的 4 条）
```

软删除悬空行（`deleted=1` 且 `permission_id` 无对应目录行）**不动**：不参与判定，动了反而破坏审计。
