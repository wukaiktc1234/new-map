# 路线图与遗留问题（Roadmap & Known Issues）

> 状态：**2026-10-28 核心业务暂停开发，进入维护与响应模式。**
> 用途：新接手者判断「哪些能改、哪些要等产品、试运行前还缺什么」。

---

## 一、缺表缺口：59 → 45（分批结论）

### 已建 14 张（本阶段完成，全部通过六项体检 + 三列对照）

| 批次 | 表 |
|---|---|
| 批 1 | contract_document ｜ file_attachment ｜ finance_report ｜ hardware_config ｜ hardware_config_version |
| self_purchase | self_purchase ｜ self_purchase_item（Owner 拍板「单店模式必须功能」，Java 侧已有故只建表） |
| 批 2/3 | inventory_code ｜ plan_item ｜ receivable ｜ requisition_requests ｜ swap_request_details ｜ travel_requests ｜ weighing_record |

### skip 16 张（**不建**，逐张有理由）

**A. registry 登记 8 张**（判据：生产无表 + 无 Controller 入口 + 无调度；或重复设计/空壳）

| 表 | 理由 |
|---|---|
| finance_account | 与 bank_accounts / period_balance 语义重叠 |
| finance_approval | 有写入代码但无 Controller 入口、无调度 → 半成品 |
| finance_report_detail | 同上（有入口的是 finance_report） |
| department_permissions | 重复设计：已由 role_permissions + role_departments 表达 |
| user_sessions | 会话走 JWT + 内存，无需落表 |
| hardware_operation_log | Mapper insert/select 调用皆 0 → 纯空壳 |
| registration_code_log | 同上 |
| data_change_history | 已有 finance_audit_logs / operation_logs |

**B. 本阶段新判定 8 张**（建了会让死链路变活，或产生第二份真相）

| 表 | 理由 |
|---|---|
| operation_logs | 实际查 sys_operation_logs（已存在），实体指向错，属误标已修 |
| scan_device ｜ weighing_device | 只读配置表，写入方在仓库外（外部写入假设） |
| dish_ingredient | 食品安全召回链（原料→菜品→订单）一环，**有活引用**；生产从未建过 → 建表=让死链路变活，需产品先定方案 |
| role_departments ｜ role_stores | 数据权限（可见门店/部门）落地表，被 5 个报表/看板/采购服务调用，**有活引用**；同上 |
| sales_order ｜ sales_order_detail | 已确认是 orders / order_items 的旧名，建了就是**第二份真相** |

### 剩余 45 张（**暂停推进，等产品统一梳理**）

约 8 张属未上线功能（无入口/无调度，不建）；约 9 张属废弃/重复映射（建议删实体，不建表）；约 28 张待产品/方案确认。

> 这 45 张**未逐张裁定**，`docs/audit/missing-tables-triage.md` 只做了三档粗分。产品梳理时以 `docs/audit/missing-tables-delta.md` 为工作底稿。

**例外**：前端交互必须用到的表 → 一句话即可按既有标准开建（Flyway + 实体/Mapper + 六项体检 + 三列对照）。

---

## 二、已知妥协设计（**不是 bug，但必须知道**）

### 1. `*_fen` 影子列：已建列但**未双写**

| 项 | 内容 |
|---|---|
| 现状 | 12 个 `*_fen` 列（bigint 分）已建，但双写未实现 → 全部恒为 NULL |
| 风险 | 看 schema 会以为已有分单位数据；直接 `SELECT *_fen` 对账会得到全 NULL（静默无数据） |
| 防护 ① | 列注释已写「当前未启用（双写未上线）」进 DDL（`\d+` 可见） |
| 防护 ② | `scripts/qa/fen_shadow_check.py` 定期校验，非空 > 0 则 `EXIT=1` |

**启用触发条件**（任一成立即必须先完成双写）：① 任何报表/对账/导出开始 `SELECT *_fen`（含前端接口返回该字段）；② `fen_shadow_check` 报任一列非空 > 0；③ 要把旧 `numeric(元)` 列标记废弃或删除；④ 财务/审计要求分精度数据。

### 2. 时间列统一 `timestamp without time zone`（偏离新标准）

| 项 | 内容 |
|---|---|
| 现状 | 全库 500+ 表时间列均为 `timestamp`；实体统一 `LocalDateTime`；DB/JVM 同区 Asia/Shanghai（无夏令时） |
| 偏离 | Owner 新表标准要求 `timestamptz`，本阶段裁定沿用 `timestamp`（一致性与前端序列化兼容） |
| 迁移触发器 | 多时区部署 / 引入夏令时地区 / 与外部系统做带时区时间对齐 |

详见 `docs/audit/tech-debt-schema.md` P0-1。

### 3. `hasAuthority('*')` 仍存 423 处

本阶段已删除 111 处（Top3：资产/设备/订单），剩余 423 处仍是「具体码 or `*`」。这 423 个接口普通角色按具体码就能进，`*` 只是 admin 的兜底逃生口。仅 2 处是 star-only（`UserPermissionOverrideController:221`、`HrMigrationController:33`）→ 仅 admin 可调用，需产品定权限。

### 4. 既有核心表不符合新标准（老账，已登记不修）

| 表 | 问题 |
|---|---|
| `orders` | `user_id/transaction_id/merchant_id/deliveryman_id/cashier_user_id` 无索引；4 个金额列 `numeric(元)` |
| `deposit_slips` | 6 个 `_id` 列无索引 |
| `fund_flows` | 2 个 `_id` 列无索引 |
| `pos_shifts` | `total_amount` 是 `numeric(元)` |

见 `docs/audit/tech-debt-schema.md`。改这些是牵动全系统的大手术，不做。

### 5. 实体指向空表（登记，不修）

`Order → orders_legacy(0行)`、`DishRecipe`、`BackupRestoreLog`、`ApprovalRecord`、`ComboInventory/DishInventory`、`VoucherHeader/VoucherLine/VoucherSignatureLog`、`food_trace`。

**原则**：不修比修了安全 —— 改了可能让死代码变成活代码，在无测试覆盖下开始写生产数据。

### 6. `inventory_code` 表比实体多三列

`create_time / update_time / deleted` 已按标准建，但实体无这些字段 → 查询不带 `deleted` 过滤。**不要为了对齐给实体加字段**（会改变 Mapper 行为）。

---

## 三、遗留缺陷清单（已知、未修、有据可查）

| 编号 | 问题 | 优先级 | 说明 |
|---|---|---|---|
| bug-⑰ | `v_inventory_summary` 疑视图，未建 | P3 | 反推视图会「能跑但数字错」，需产品给聚合口径 |
| bug-㉓ | `attribution.slip_id` 历史行可能等于入口 slip | P3 | 写入逻辑已修；历史行不批量修，**以 `shift_id` 为准** |
| mvn test | 11 Failures + 5 Errors（已知历史欠账） | P3 | 明细见 `Gate-Baseline-and-Health.md`；**新增失败才是问题** |
| 部署缺口 | 剩余 45 张表 | 冻结 | 等产品统一梳理 |
| `orders` 元/分混用 | 5 条最危险跨表混用点 | P2 | 见 `money-unit-boundary.md`；不改 numeric，走双写路线 |

---

## 四、试运行前 Checklist（下个月要跑起来，还差这些）

### 必须（缺一不可）

- [ ] **产品确认 45 张缺表的处置**（至少确认「前端会调到的那几张」）
- [ ] **`*_fen` 双写决策**：要么实现双写（含等值校验），要么把影子列在文档/UI 明确标「未启用」
- [ ] **财务总账口径确认**（期初/期末/科目余额）→ 决定是否实现 4 个 404 端点
- [ ] **权限矩阵审查**：423 处 `'*'` 中「业务上应该谁能用」需产品定；2 处 star-only 必须给具体码
- [ ] **历史 mvn test 11F/5E 清零或明确豁免**（发版前 `preflight --full` 必须 `EXIT=0`）
- [ ] **对象存储选型落地**（开发 MinIO / 生产 OSS，一套 `FileStorage` 接口两实现；硬禁 DB Blob 与本地磁盘）
- [ ] **`file_attachment` 上传链路端到端验证**（表已建，但上传/下载接口未验收）

### 建议（影响体验或运维）

- [ ] `print_format_template` 模板 JSON + 硬编码兜底（缺兜底会整单不出票）
- [ ] 既有核心表补索引（`orders.*_user_id` 等，高频查询会全表扫描）
- [ ] `PositionSortingTest` 分页装配（唯一剩下的环境类失败）
- [ ] 每周回归巡检结果归档（定时任务已建：每周一 09:00）

### 上线当天

- [ ] `preflight.py --full` 全绿（含 from-zero executed 达最新）
- [ ] 全量 e2e `PASS=56 / FAIL=0`
- [ ] 巡检 `FAIL=0`，记下 PASS/skipped 三数字
- [ ] 备份 `food_traceability` 库 + 记录 `flyway_schema_history` 版本
- [ ] 确认 `[BOOT] build=<当前 HEAD 短哈希>`

---

## 五、待命响应：前端重构可能需要的只读聚合接口

| 前端诉求 | 能否立刻提供 | 数据来源 |
|---|---|---|
| 交班核算的微信/支付宝收入 | 能（按支付方式分组聚合） | `fund_flows` + `orders` 支付渠道 |
| 异常单拦截（未支付/超时/退款中） | 能（只读列表 + 计数） | `orders.status` 状态机 |
| 日/班次营收对账 | 能 | `deposit_slips` 净额 + `daily_settlements` |
| 菜品销量/毛利 | 能 | `order_items` + 菜品成本 |
| 库存实时可用量 | 能 | `inventories` + `inventory_movement` 汇总 |

**约束**：新增接口一律**只读**、不改底层逻辑、不改表结构；若口径不匹配（如元/分混用），**先摊开口径再加接口**。
