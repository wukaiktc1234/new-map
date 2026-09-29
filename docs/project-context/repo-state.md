# Repo 状态快照（repo-state.md）

> **性质声明**：本文档为**视图层快照，非权威来源**。与 git 实际状态不一致时，**以 git 为准**。
> 同步责任：PG-002（见同目录 `process-guards.md`）——每次卡片收口或 push 后由 developer 更新本文件。
> 快照时点：2026-09-28（S4c-2 clean commit `bf4f6b2` force-push 销项 + ENV-4 登记后）

## 当前状态

| 项 | 值 |
|----|------|
| 分支 | `master`（单分支工作流） |
| HEAD（快照时点） | `bf4f6b2` feat(location): M3-M4 S4c-2 absorb bypass writers (Q5=B executed) |
| 远程 | `origin` = https://github.com/wukaiktc1234/new-map.git |
| 远程同步 | **同步（ahead=0）**：S4c-2 clean commit `bf4f6b2` force-push 已推送（替代误吞 519 残留文件的 `cb8ee06`；push 前三项验证 A/B 全 PASS + Owner 批准，见 ENV-4） |
| 工作区 | **462 个已修改 tracked WIP**（canonical 迁移等，ENV-1/ENV-2 同源遗留债）+ 258 个 untracked（其中 576 个 ENV-4 残留项已由 `.gitignore` 专用段逐条排除，留盘不删除） |

## 已完成卡片

| Task ID | 终态 | 日期 | 备注 |
|---------|------|------|------|
| P1-ORDER-NUMBER-002（A1） | PASS_WITH_LIMITATION（本地放行 / Gate ALLOW_LOCAL） | 2026-09-23 | order_number 唯一性；限制 KL-069~073 |
| P1-POS-FOODID-MAP-001 | CLOSED_WITH_REGISTERED_LIMITATIONS | 2026-09-24 | POS food_id 映射；残余 KL-074~076 + ENV-1 |
| P1-COMBO-ORDER-001 | CLOSED_WITH_REGISTERED_LIMITATION | 2026-09-24 | 套餐下单 + KDS components[]；限制 KL-078~080 + ENV-2；生产均 PROVISIONAL |
| P1-INVENTORY-LOG-FILTER-001 | **CLOSED（Owner 活体验证收口）** | 2026-09-26 | 库存日志详情过滤；修复 `ece7e6e`（动态 WHERE）+ 4/4 集成测试 + MVN_EXIT=0；Owner 指令：活体截图 + 三调用方代码核对，不走 DS/QA 全流程；残留 1（selectConsumptionStats）→ 独立卡 P1-INVENTORY-CONSUMPTION-STATS-001 |

## 活动/待启动卡片

| Task ID | 状态 | 说明 |
|---------|------|------|
| P1-COMBO-LEGACY-CLEANUP-001 | **CLOSED_WITH_REGISTERED_LIMITATION（2026-09-25）** | 代码 `05d4404`；DS 5/5 + QA PWL（活体 4/4）+ REG-ORDER-012 转正（125→126）；实施记录 §6 收口；RESIDUALS L-01/L-02/OBS-3/OBS-4（OBS-1 → 独立卡已修复） |
| P1-POS-MENU-500-001 | IMPLEMENTED_QA_PENDING（2026-09-25） | OBS-1 升级卡：FoodCategory 实体对齐 Flyway（commit `872c874`），/menu 500→code=0；任务板 §24.2b |
| P0-SCHEMA-SINGLE-SOURCE-001 | CLOSED_WITH_REGISTERED_LIMITATION（2026-09-25） | Flyway 唯一真相源 + PG-003/PG-004；41 遗留 sql 归档；对齐基线 v2（KL-081=163 处）+ KL-082（47 黑箱表，应急快照已导出）；下游 P0-FLYWAY-COVERAGE-001（P0 预告） |
| P0-WORKSPACE-WIP-CONSOLIDATION-001 | PENDING（预告，任务板 §24.3） | 分五批入库工作区 WIP（canonical 迁移 / bug fix / 安全加固 / 新功能 / docs 287）；前置 = LEGACY-CLEANUP 收口 |
| P1-FOODS-STOCK-SEMANTICS-001 | 阶段 1 完成，待 Owner 审（2026-09-26） | foods.stock 语义诊断：报告 `docs/quality/foods-stock-semantics-diagnosis-001.md`（commit `22ccbdd`）；结论 = 当前人工计数器（Count Down），反推引擎已实现但孤立（StockForecast）；审过 → 阶段 2 |
| P1-POS-MENU-UNIFICATION-001 | 阶段 2 完成，待 Owner 审 + 4 项拍板（2026-09-26） | POS 菜单统一到 foods/food_categories：设计 `docs/design/pos-menu-unification-design-001.md`（commit `f3eeda4`）；拍板后 → 阶段 3（3a 切换/3b 观察/3c 下线/3d 废弃） |
| P1-INVENTORY-CONSUMPTION-STATS-001 | PENDING（2026-09-26 建卡，任务板 §24.3f） | selectConsumptionStats 列名错位（change_quantity vs change_amount）+ 三参数未落 WHERE；来源 = LOG-FILTER 残留 1 + Owner 活体截图 500 |
| P1-LOCATION-MODEL-001（M3-M4） | **进行中（2026-09-28，S4c-2 收口）** | 实施记录 `docs/architecture/03-review/p1-location-model-001-implementation-record-002-m3m4.md`；S1-S4c-2 ✅（S4c-2 = 5 旁路收编 Q5=B，commit `bf4f6b2` force-push 已销项）；S5 流水统一 + inventory_log 后门关闭进行中；ENV-4 登记（PG-001 首次违规，不冻结） |

## PurchaseOrderServiceImpl 双版本状态（2026-09-25 登记）

- **HEAD（已提交）** = 基线 + **P1-PURCHASE-SUPPLIER-BINDING-001 修复**（表头供应商优先，commit `0795665`）
- **工作树（未提交）** = HEAD + 修复 + **115 行 W1-EC 批次 WIP**（countByStatus 等 dashboard 查询辅助方法 + 接口声明，与修复零重叠，部分提交法隔离）
- ⚠ 后续 restoring/merge 该文件时：WIP 需在 P0-WORKSPACE-WIP-CONSOLIDATION-001 Batch 1 重新落库，注意与已提交修复区隔

## 已知环境问题（ENV 序列）

| 编号 | 内容 | 登记 |
|------|------|------|
| ENV-1 | `PosOrderCreateServiceImpl` 工作区 WIP（+681 行）导致 FOODID 卡 diff 不可隔离 | `p1-pos-foodid-map-001-implementation-record-001.md` §V9 |
| ENV-2 | commit `aec5c45` 内容混合（combo 卡 + P0 编译修复 + canonical WIP），已推送远程，diff 不可隔离 | `production-known-limitations.md` 主表 ENV-2 行 + 文末块 |
| ENV-3 | mvn 默认 JAVA_HOME 指向不存在的 `H:\fuwu\jdk-17.0.17+10`，编译须显式覆盖（实测 Temurin 25 可用）。**关联（2026-09-28 S5）**：JDK 25 下 bytebuddy 1.14.x 不识别 class file 69，跑单测须 `-DargLine=-Dnet.bytebuddy.experimental=true`（S8 同适用） | `production-known-limitations.md` 主表 ENV-3 行 + 文末追加块（2026-09-25 登记 / 2026-09-28 关联） |
| ENV-4 | commit `cb8ee06`（S4c-2）`git add .` 误吞 519 工作区残留文件；clean redo `bf4f6b2` + force-push 已销项（remote master = `bf4f6b2`）；残留留盘 + `.gitignore` 专用段 576 条目；PG-001 首次违规，Owner 裁决不冻结 | `production-known-limitations.md` 主表 ENV-4 行 + 文末块 + 实施记录 -002 §6 |
| ENV-5 | S7a 硬编码 `WHERE movement_type='OUT'` 静默违反 24.3f 已验证行为（无裁定/理由记录，无参全局场景丢失 IN 行）；S9a-2 已移除 + 恢复全动态 `<if>` + 重锚 6/6 绿（已销项；仅读路径无数据影响） | `production-known-limitations.md` 主表 ENV-5 行 + 文末块 + 实施记录 -002 §15 |
| ENV-7 | OrderManagementIntegrationTest 9 测试因无 @WithMockUser/登录 setup 全部 401/403 失败（既有设计缺口，V999 幂等化前被 Flyway 阻塞掩盖）；**排除 M3-M4 收口条件，归 M5 或独立卡**（Owner 裁决 2026-09-29） | `production-known-limitations.md` 主表 ENV-7 行 + 文末块 + 实施记录 -002 §15.3 |

## 待处理项

**无**。（push 批次 2026-09-25 已于 2026-09-25 销项：5 commit 全部推送成功，ahead=0）

## 最近 10 commit（快照时点）

| Hash | 消息 |
|------|------|
| bf4f6b2 | feat(location): M3-M4 S4c-2 absorb bypass writers (Q5=B executed) |
| 9e8aa29 | docs(location): S4c-1 unmapped-warehouse impact verification + test re-anchor nature |
| f30a515 | feat(location): M3-M4 S3/S4 core layer + rule-4 impersonation rewrite |
| 0c3126e | docs(quality): M3-M4 preflight pack + board status sync |
| 23aa2e1 | docs(index): add INDEX.md as docs entry map (independent commit per Owner) |
| 14b9639 | chore(ws): remove dead maxReconnectAttempts config (Q3) |
| c975a8f | fix(ws): app-level supervised reconnect for long outages |
| a956cd4 | feat(location): add locations model core layer (M1-M2) |
| abda72b | fix(ws): add Authorization frame header to STOMP CONNECT |
| b30dc3d | feat(pos): unify Order/CustomerOrder menu source |

## 已知遗留（不阻塞，独立决策）

- **KL-077**：git 历史含明文凭据（`dd33ee3` 基线 + 后续 commit 中的 keystore/JWT），HEAD 已清理 tracked 凭据（`883b639`/`fe3902f`/`0274549`/`c182e63`/`c3cbd3a`），**决定不重写历史**；若仓库公开/外泄须重评
- 生产侧证据缺口：KL-069 / KL-076 / KL-079 同族（PROVISIONAL_PENDING_PRODUCTION_EVIDENCE，仅阻断生产放行）
