# 全仓注解权限字面量核对报告（N32 扫描器产出）

> 扫描面：`backend/src/main/java`（全部 `.java`）
> 比对基准：活库 `roles.role_code` ∪ `roles.role_id_str` → 23 个归一化角色码；`permissions.permission_code` → 418 个权限码
> 分类：A=角色幻码（可近邻纠正）/ B=权限码幻码 / C=完全未定义 / VALID=库中存在

## 1. 总览

| 分类 | 出现次数 | 说明 |
|------|---------|------|
| **VALID** | 2536 | 有效（库中存在） |
| **A** | 0 | A 类：角色幻码（可用近邻匹配纠正标识） |
| **B** | 0 | B 类：权限码幻码（permissions 表无此码） |
| **C** | 0 | C 类：完全未定义（角色与权限均无，需设计决策） |
| **合计** | 2536 | |

## 2. 位置错配（A 卡归一化也覆盖不到的那一类）

| 标记 | 次数 | 含义 |
|------|------|------|
| `ROLE_IN_AUTHORITY_POSITION` | 54 | ROLE_ 前缀角色码写在 hasAuthority/hasAnyAuthority；前缀对，但角色必须真实存在 |

## 3. A 类：角色幻码（可用近邻匹配纠正标识）

无。

## 4. B 类：权限码幻码（permissions 表无此码）

无。

## 5. C 类：完全未定义（角色与权限均无，需设计决策）

无。

## 6. 有效字面量 TOP 20（确认扫描不是在空转）

| 字面量 | 次数 |
|--------|------|
| `admin` | 285 |
| `hr_director` | 203 |
| `store_manager` | 111 |
| `ADMIN` | 92 |
| `purchase_manager` | 63 |
| `inventory:query` | 57 |
| `OWNER` | 51 |
| `order:view` | 44 |
| `order:manage` | 43 |
| `trace:query` | 40 |
| `finance:view` | 27 |
| `kitchen:edit` | 26 |
| `schedule:view:self` | 25 |
| `finance:edit` | 24 |
| `finance_director` | 24 |
| `asset:view` | 23 |
| `hr:health-certificate:manage` | 22 |
| `system:role:read` | 22 |
| `system:user:read` | 22 |
| `trace:update` | 21 |
