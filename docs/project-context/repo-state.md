# Repo 状态快照（repo-state.md）

> **性质声明**：本文档为**视图层快照，非权威来源**。与 git 实际状态不一致时，**以 git 为准**。
> 同步责任：PG-002（见同目录 `process-guards.md`）——每次卡片收口或 push 后由 developer 更新本文件。
> 快照时点：2026-09-30（M3-M4 S5–S9c 收口 `b2cad36` → KL-084 `4ccf353` → ENV-8 + PG-001 规则6 `d86f0be` → PG-006 `1b3d7e3` → **P1-D 实现 `916c0b6`** → **M3-M4 正式收口 CLOSED_WITH_REGISTERED_LIMITATION（本 commit）**；local = 本收口 commit，**ahead=3 / behind=0，未 push**）

## 当前状态

| 项 | 值 |
|----|------|
| 分支 | `master`（单分支工作流） |
| HEAD（快照时点） | `916c0b6` **P1-D（P1-POS-CUSTOMER-PHONE-500-001）实现**：POS 电话 500 修复，`customer_phone`=脱敏 + `customer_phone_encrypted`=密文（Option A，读路径零改） |
| 远程 | `origin` = https://github.com/wukaiktc1234/new-map.git |
| 远程同步 | **local ahead=2 / behind=0**：local = `916c0b6`，origin/master = `d4c459f`（**未 push**，PG-001 规则6 禁静默 auto-push；`git rev-list --count origin/master..HEAD` 实测 2） |
| 工作区 | **545 个变更（status --short）/ 86 untracked**（WIP 留盘，未纳入提交；遵守 PG-001 精确清单，不 `git add .`；ENV-4 残留由 `.gitignore` 专用段逐条排除） |

## 已完成卡片

| Task ID | 终态 | 日期 | 备注 |
|---------|------|------|------|
| P1-ORDER-NUMBER-002（A1） | PASS_WITH_LIMITATION（本地放行 / Gate ALLOW_LOCAL） | 2026-09-23 | order_number 唯一性；限制 KL-069~073 |
| P1-POS-FOODID-MAP-001 | CLOSED_WITH_REGISTERED_LIMITATIONS | 2026-09-24 | POS food_id 映射；残余 KL-074~076 + ENV-1 |
| P1-COMBO-ORDER-001 | CLOSED_WITH_REGISTERED_LIMITATION | 2026-09-24 | 套餐下单 + KDS components[]；限制 KL-078~080 + ENV-2；生产均 PROVISIONAL |
| P1-INVENTORY-LOG-FILTER-001 | **CLOSED（Owner 活体验证收口）** | 2026-09-26 | 库存日志详情过滤；修复 `ece7e6e`（动态 WHERE）+ 4/4 集成测试 + MVN_EXIT=0；残留 1（selectConsumptionStats）→ 独立卡 P1-INVENTORY-CONSUMPTION-STATS-001 |
| **P1-LOCATION-MODEL-001（M3-M4）** | **CLOSED_WITH_REGISTERED_LIMITATION（正式收口 2026-09-30）** | **2026-09-30** | **库存统一账迁移 S1–S9c（`b2cad36`）；42/42 单测 + 32/32 E2E 全绿；收口门控 P1-D 已实现（`916c0b6`）；收口记录 = -002 实施记录 §18；KL-084 / ENV-7 / ENV-8 / ENV-9 已登记；M5 挂起（待产品定仓→店补货链路）** |
| **P1-POS-CUSTOMER-PHONE-500-001（P1-D）** | **COMMITTED（`916c0b6`，未 push）** | **2026-09-30** | **POS 下单带电话 500 修复（W1-EC-01）：Option A 脱敏+密文双列（`customer_phone` 脱敏 VARCHAR(20) + `customer_phone_encrypted` ENC_PHONE 密文 VARCHAR(255)），读路径零改（脱敏列天然安全）**；6 文件 +48/-7；4 受影响测试类 39/39 绿（DeductTest 28 / OrderNumberA1Test 3 / FoodIdMap 5 / EnsureLegacyRow 3，ENV-3 byte-buddy 开关下）；M3-M4 收口门控已放行；回拨另立"隐私分级"卡 |

## 活动/待启动卡片

| Task ID | 状态 | 说明 |
|---------|------|------|
| **P1-LOCATION-MODEL-001（M3-M4）** | **CLOSED_WITH_REGISTERED_LIMITATION（正式收口 2026-09-30）** | 收口记录 = 实施记录 -002 §18（本卡收口）；S1–S9c ✅（`b2cad36`，42/42 + 32/32 全绿）；**收口门控 P1-D 已实现（`916c0b6`）**；⚠ 收口时点 2 环境事实（非本卡回归）：① WIP 树致 ~10 无关单测类失败（**ENV-9**，42/42 基线在 WIP 树不可复现，属 P0-WORKSPACE-WIP-CONSOLIDATION-001）② 本机无 DB（8 @SpringBootTest + 32 E2E 不可运行，活体 E2E 已前序 32/32 验证）；**M5 挂起不现在开**（P0-C 仓→店补货 + status 不重算 + ENV-7，开卡条件 = 待产品定"仓→店补货链路"）；下一卡 = P1-USER-LOCATION-001（重档，待判档确认） |
| P1-COMBO-LEGACY-CLEANUP-001 | **CLOSED_WITH_REGISTERED_LIMITATION（2026-09-25）** | 代码 `05d4404`；DS 5/5 + QA PWL（活体 4/4）+ REG-ORDER-012 转正（125→126）；RESIDUALS L-01/L-02/OBS-3/OBS-4 |
| P1-POS-MENU-500-001 | IMPLEMENTED_QA_PENDING（2026-09-25） | OBS-1 升级卡：FoodCategory 实体对齐 Flyway（commit `872c874`），/menu 500→code=0；任务板 §24.2b |
| P0-SCHEMA-SINGLE-SOURCE-001 | CLOSED_WITH_REGISTERED_LIMITATION（2026-09-25） | Flyway 唯一真相源 + PG-003/PG-004；41 遗留 sql 归档；对齐基线 v2（KL-081=163 处）+ KL-082（47 黑箱表，应急快照已导出）；下游 P0-FLYWAY-COVERAGE-001（P0 预告） |
| P0-WORKSPACE-WIP-CONSOLIDATION-001 | PENDING（预告，任务板 §24.3） | 分五批入库工作区 WIP（canonical 迁移 / bug fix / 安全加固 / 新功能 / docs）；前置 = LEGACY-CLEANUP 收口；**⚠ 2026-09-30 实测：WIP 已致 ~10 个无关单测类失败（finance CostRecord/Payable/Payment/InvoiceReimbursement、foodcode、syssetting、position、sysdict），42/42 基线在当前 WIP 树上不可复现（与 P1-D 无关，P1-D 的 4 受影响测试类 39/39 全绿）** |
| P1-FOODS-STOCK-SEMANTICS-001 | 阶段 1 完成，待 Owner 审（2026-09-26） | foods.stock 语义诊断：报告 `docs/quality/foods-stock-semantics-diagnosis-001.md`（commit `22ccbdd`）；审过 → 阶段 2 |
| P1-POS-MENU-UNIFICATION-001 | 阶段 2 完成，待 Owner 审 + 4 项拍板（2026-09-26） | POS 菜单统一到 foods/food_categories：设计 `docs/design/pos-menu-unification-design-001.md`（commit `f3eeda4`）；拍板后 → 阶段 3（3a 切换/3b 观察/3c 下线/3d 废弃） |
| P1-INVENTORY-CONSUMPTION-STATS-001 | PENDING（2026-09-26 建卡，任务板 §24.3f） | selectConsumptionStats 列名错位（change_quantity vs change_amount）+ 三参数未落 WHERE；来源 = LOG-FILTER 残留 1 + Owner 活体截图 500 |

## PurchaseOrderServiceImpl 双版本状态（2026-09-25 登记，历史注记）

- **HEAD（已提交）** = 基线 + **P1-PURCHASE-SUPPLIER-BINDING-001 修复**（表头供应商优先，commit `0795665`）
- **工作树（未提交）** = HEAD + 修复 + **115 行 W1-EC 批次 WIP**（countByStatus 等 dashboard 查询辅助方法，与修复零重叠，部分提交法隔离）
- ⚠ 后续 restoring/merge 该文件时：WIP 需在 P0-WORKSPACE-WIP-CONSOLIDATION-001 Batch 1 重新落库，注意与已提交修复区隔

## 已知环境问题（ENV 序列）

| 编号 | 内容 | 登记 |
|------|------|------|
| ENV-1 | `PosOrderCreateServiceImpl` 工作区 WIP（+681 行）导致 FOODID 卡 diff 不可隔离 | `p1-pos-foodid-map-001-implementation-record-001.md` §V9 |
| ENV-2 | commit `aec5c45` 内容混合（combo 卡 + P0 编译修复 + canonical WIP），已推送远程，diff 不可隔离 | `production-known-limitations.md` 主表 ENV-2 行 + 文末块 |
| ENV-3 | mvn 默认 JAVA_HOME 指向不存在的 `H:\fuwu\jdk-17.0.17+10`，编译须显式覆盖（实测 Temurin 25 可用）。**关联（2026-09-28 S5 / 2026-09-30 P1-D 复现）**：JDK 25 下 bytebuddy 1.14.10 不识别 class file 69，跑 mockito 单测须 `-Dnet.bytebuddy.experimental=true`（否则 "Mockito cannot mock OrderWebSocketController"，P1-D 的 FoodIdMap/EnsureLegacyRow 即此因，非回归） | `production-known-limitations.md` 主表 ENV-3 行 |
| ENV-4 | commit `cb8ee06`（S4c-2）`git add .` 误吞 519 工作区残留文件；clean redo `bf4f6b2` + force-push 已销项；残留留盘 + `.gitignore` 专用段 576 条目；PG-001 首次违规，Owner 裁决不冻结 | `production-known-limitations.md` 主表 ENV-4 行 + 实施记录 -002 §6 |
| ENV-5 | S7a 硬编码 `WHERE movement_type='OUT'` 静默违反 24.3f 已验证行为；S9a-2 已移除 + 恢复全动态 `<if>` + 重锚 6/6 绿（已销项；仅读路径无数据影响） | `production-known-limitations.md` 主表 ENV-5 行 + 实施记录 -002 §15 |
| ENV-7 | OrderManagementIntegrationTest 9 测试因无 @WithMockUser/登录 setup 全部 401/403 失败（既有设计缺口）；**排除 M3-M4 收口条件，归 M5 或独立卡**（Owner 裁决 2026-09-29） | `production-known-limitations.md` 主表 ENV-7 行 + 实施记录 -002 §15.3 |
| **ENV-8** | **post-commit hook 静默 auto-push（每次 commit 自动 `git push origin master`，2026-09-10 加入）**：hook 加入后 97 个 commit（`7f40ab7`→`b2cad36`）均被自动推送至 origin；**已收口（2026-09-30：Owner 批准删除 hook + 审计确认无其它 auto-push 机制）+ PG-001 规则6（禁静默 auto-push 类 hook）+ 澄清"推送节奏 ≠ Owner 审阅节奏"** | `production-known-limitations.md` 主表 ENV-8 行 + `process-guards.md` PG-001 规则6 |

## 待处理项（挂起项集中登记）

| 项 | 归属 | 说明 |
|----|------|------|
| **P0-C** | **M5** | M3-M4 不处理，裁决归 M5 |
| **status 不重算** | **M5** | M3-M4 不处理，裁决归 M5 |
| **P1-D（密文方向）** | **✅ 已实现（`916c0b6`，未 push）** | 方向 A 落地：`customer_phone`=脱敏值（VARCHAR(20)）+ `customer_phone_encrypted`=ENC_PHONE 密文（VARCHAR(255)）；读路径零改（脱敏列天然安全）；**M3-M4 收口门控已放行**；回拨另立"隐私分级"卡 |
| **M3-M4 正式收口记录** | **待写** | P1-D 门控已放行；42/42 基线在 `b2cad36` 已达成；⚠ 当前 WIP 树致 ~10 无关单测类失败（P0-WORKSPACE-WIP-CONSOLIDATION-001 范畴）+ 本机无 DB（32/32 E2E 与 8 @SpringBootTest 不可跑）；收口记录需注明此两点 |

## 最近 10 commit（快照时点 `916c0b6`）

| Hash | 消息 |
|------|------|
| 916c0b6 | P1-POS-CUSTOMER-PHONE-500-001: store masked + encrypted customer phone on orders（Option A，读路径零改，6 文件 +48/-7） |
| 1b3d7e3 | docs(governance): PG-006 判档分流（重档/轻档）+ INDEX 指针 |
| d86f0be | 治理登记：ENV-8（post-commit auto-push）+ PG-001 规则6 + P1-D 修法A与M3-M4收口门控 |
| 4ccf353 | 治理登记：KL-084 门店日结确认后异步凭证生成失败（科目 5001 缺失 + 事件载荷 store=null） |
| b2cad36 | M3-M4 收口：库存统一账迁移 S5-S9c（26 文件 PG-001 显式清单） |
| ab7f483 | docs(location): ENV-7 OrderManagementIntegrationTest 401/403 登记（排除 M3-M4，归 M5） |
| 8b1178b | docs(location): 禁区6 InventoryLogMapper.xml 追认闭环（Owner 2026-09-29） |
| 2e541e4 | feat(location): M3-M4 S9a complete (S8 miss items + 4 fixture re-anchor green + V999 idempotent + ENV-5) |
| c11d0ae | M3-M4: impl record §14 observation period rules + S9 split adjudication |
| 3d0de97 | M3-M4 S8b: live DDL applied successfully (13/13 endpoints PASS) |

## 已知遗留（不阻塞，独立决策）

- **KL-084**（2026-09-30 登记）：门店日结确认后异步凭证生成失败（科目 5001 缺失 + 事件载荷 store=null），"假成功"形态；**观察**（财务域，随 P1-FIN-* 批次裁决）
- **KL-083**：多门店数据隔离未生效（Owner 已决策多门店模式，P1 必修，**高**）；修复卡 P0-ROLE-STORE-SCHEMA-001 → P1-ROLE-STORE-ISOLATION-001
- **P1-D**：✅ 已实现（`916c0b6`，2026-09-30）：Option A 脱敏+密文双列，读路径零改；M3-M4 收口门控已放行；回拨另立"隐私分级"卡
- **KL-077**：git 历史含明文凭据（`dd33ee3` 基线 + 后续 commit 中的 keystore/JWT），HEAD 已清理 tracked 凭据，**决定不重写历史**；若仓库公开/外泄须重评
- 生产侧证据缺口：KL-069 / KL-076 / KL-079 同族（PROVISIONAL_PENDING_PRODUCTION_EVIDENCE，仅阻断生产放行）
