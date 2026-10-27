# 缴款单权限矩阵（验收基线）

> 2026-10-27 定稿。**这张表是验收项，不是文档装饰**：任何一处错位都会造成
> 「单看能跑、合起来死锁」（关班的人不能出单 / 出单的人不能提交 / 提交的人进不了后台）。

## 一、矩阵（10 权限 × 7 角色）

| 权限 | 语义 | 绑定角色 | 状态 |
|---|---|---|---|
| `finance:deposit-slip:view` | 查看缴款单 | AUDITOR, FIN_DIRECTOR, FIN_MANAGER, STORE_MANAGER, TELLER | 既有 |
| `finance:deposit-slip:create` | 生成缴款单（关班后） | FIN_DIRECTOR, **STORE_MANAGER**, TELLER | **NEW-38③ 补齐** |
| `finance:deposit-slip:submit` | 提交（DRAFT→SUBMITTED） | CASHIER, **STORE_MANAGER**, **FIN_DIRECTOR** | **NEW-38① 补齐** |
| `finance:deposit-slip:receive` | 清点收款（SUBMITTED→RECEIVED） | FIN_DIRECTOR, **STORE_MANAGER**, TELLER | **NEW-38 前置补齐** |
| `finance:deposit-slip:deposit` | 存行（RECEIVED→DEPOSITED） | FIN_DIRECTOR, **STORE_MANAGER**, TELLER | **NEW-38 前置补齐** |
| `finance:deposit-slip:verify` | 财务核对 | FIN_DIRECTOR | 既有（反审属财务） |
| `finance:deposit-slip:close` | 关单归档 | FIN_DIRECTOR | 既有 |
| `finance:deposit-slip:reverse` | 冲正 | FIN_DIRECTOR | 既有（冲正属财务） |
| `finance:deposit-slip:reject` | 驳回 | FIN_DIRECTOR, **FIN_MANAGER**, TELLER | **NEW-38④ 补齐**（财务经理为驳回主力；出纳现场可退回不合规单据，属既有设计） |
| `finance:deposit-slip:confirm-attribution` | 确认退款归属 | FIN_DIRECTOR, STORE_MANAGER | 既有 |

## 二、为什么这么分（业务依据）

- **create / submit / receive / deposit 四件必须落在「实际关班的人」手里**：真实门店由店长或收银主管关班出单、清点、存行；出纳只做清点接收（receive），财务只做核对与归档（verify/close/reverse）。此前 create 只给 FIN_DIRECTOR+TELLER、submit 只给 CASHIER，造成 **NEW-38 流程死锁**。
- **reverse / close / verify 只给 FIN_DIRECTOR**：涉及账务不可逆动作与最终归档，不做下沉。
- **reject 给 FIN_MANAGER**：驳回是财务日常动作，不必等总监。
- **CASHIER 保留 submit**：POS 侧收银员在自己班里提交，是合理路径；但不能因此让后台角色缺 submit。

## 三、授权写入的硬规则（两次事故的教训）

1. `role_permissions.permission_id` **必须等于** `permissions.permission_id_str`（`PERM_*` 形态）。
   用权限码字符串插入 → `UserMapper.getUserRolePermissions` 的 join 丢弃 → **静默失效、接口 403**。
2. `role_permissions.role_id` **必须等于** `roles.role_id_str`。
   （2026-10-27 发现 418 行 `role_id='1'` 数字形态 → 超管授权全部被丢弃。）
3. 所有授权脚本统一写 `CROSS JOIN permissions p … rp.permission_id = p.permission_id_str`，并幂等。
4. 守护断言：**A44**（`permission_id` 形态自洽，分母 = role_permissions 有效行）、**A45**（引用可解析：permission 侧 + role 侧）。

## 四、验收方式（不接受「查表看着对」）

对每个角色用**真实账号**调对应端点，检查 **HTTP 状态 + 业务码**：

- 应为 200 或业务码（400/404/409）；
- **出现 403 即为无效授权**；
- 例外按设计：`qa_teller → submit` 应 403（出纳不提交）。

矩阵 + 接口级验证两者缺一不可：只查表会漏「形态错」，只调接口会漏「本该有却没有」。
