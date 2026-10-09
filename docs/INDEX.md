# docs/INDEX.md（当前事实源索引）

> 本文件是 docs 的入口地图：读文档前先看这里。
> 维护规则：新增/取代任何下述文件时，必须同步更新本索引。

## 新会话第一步（开工前必读顺序）

> 目标：任何新会话进来，几分钟内知道「进度到哪、从哪开始、什么不能动」，而不是翻到一份早已过时的旧交接摘要。

1. **读本文件（INDEX）** → 锁定当前主线与活文档清单（下方各节）
2. **读任务板 §0** → [../production-remediation-task-board.md](../production-remediation-task-board.md) 顶部"0.1 验收状态速查" → 当前已完成 / 挂起
3. **读 KL 主表** → [../production-known-limitations.md](../production-known-limitations.md) §1 → 已知限制 + ENV 序列（ENV-1~8）
4. **读最新实施记录** → [architecture/03-review/p1-location-model-001-implementation-record-002-m3m4.md](architecture/03-review/p1-location-model-001-implementation-record-002-m3m4.md)（当前主线 M3-M4 细节）
5. **需要 git / 仓库状态** → [project-context/repo-state.md](project-context/repo-state.md)；**需要流程规矩** → [project-context/process-guards.md](project-context/process-guards.md)（PG-001/002…）
6. **进度总览** → [../remediation-roadmap.md](../remediation-roadmap.md) 顶部"当前进度总览（2026-09-30）"

> 🎚️ **开卡前先判档（PG-006）**：GPT 一句问"这是重档还是轻档？"，Owner 回一个字。**重档**=完整流程（PG-005 三阶段 + 实施记录 + 更新 INDEX/roadmap）；**轻档**=诊断→改→测→提交（一句 commit message 说清证据，不写实施记录、不更 INDEX/roadmap）。
>
> ⚠️ 旧交接摘要（如 [GPT总结/CURRENT.md](GPT总结/CURRENT.md)）已标注 `SUPERSEDED_BY_INDEX.md`，仅作历史，**勿据此开工**。

## 当前主线（多门店隔离地基）
> 主线现状（S9c 收口 / P0-A·P0-B 修复 / P1-D 门控 / KL-084 / ENV-8）→ **单一事实源** [../remediation-roadmap.md](../remediation-roadmap.md) 顶部「当前进度总览（2026-09-30）」；INDEX 只列治理文档、不复述状态。
- 设计依据：[design/location-organization-separation-design-002.md](design/location-organization-separation-design-002.md)
- 用户归属：[design/user-store-assignment-design-002.md](design/user-store-assignment-design-002.md)
- 实施纪律：[quality/m3m4-preflight/implementation-constitution-001.md](quality/m3m4-preflight/implementation-constitution-001.md)
- 考古矩阵：[quality/m3m4-preflight/inventory-chain-archaeology-matrix-001.md](quality/m3m4-preflight/inventory-chain-archaeology-matrix-001.md)
- 行为基线：[quality/m3m4-preflight/inventory-chain-snapshot-20260927-001.json](quality/m3m4-preflight/inventory-chain-snapshot-20260927-001.json)

## 活文档（需要持续维护；均位于项目根目录）
- 任务池：[../production-remediation-task-board.md](../production-remediation-task-board.md)（§ 编号建卡，当前 §24.x 系列）
- KL 列表：[../production-known-limitations.md](../production-known-limitations.md)（KL-xxx 全局编号）
- 路线图：[../remediation-roadmap.md](../remediation-roadmap.md)
- 治理规则：[project-context/process-guards.md](project-context/process-guards.md)
- schema 治理：[project-context/schema-governance.md](project-context/schema-governance.md)

## 业务逻辑（长期有效）
- [business-logic/README.md](business-logic/README.md) + 01~04（采购 / POS 点单 / 食材与配方 / 单位与换算约定）

## 历史（有效但已被取代，仅供追溯）
- `*-001.md` 被 `*-002.md` 取代的设计（如 design/ 下的 location / user-store 系列）
- 更早的设计（见 design/ 与 archive/）

## 证据（完成后不再更新，保留审计链）
- 实施记录 `*-implementation-record-*.md`（主要在 architecture/03-review/）
- QA 报告 `*-qa-report.md`（主要在 quality/）
- 审计 `*-audit-*.md`（主要在 quality/ 与 audit/）

## 数据源索引（字段入口清单，做字段类卡前先查这里）
> 用途：任何涉及 `location_id / store_id / warehouse_id` 的卡片，**先引用本清单对应行**，不要每卡各自 grep、结果不归档（P1-USER-LOCATION-001 漏扫前端的根因，见 [architecture/03-review/p1-user-location-001-implementation-record-001.md](architecture/03-review/p1-user-location-001-implementation-record-001.md) §7）。
- 字段读写点 + UI 入口清单（36 行入口 / 4 个 ID 空间 / 已知 6 项 + 新发现 12 项不一致）：[quality/field-entry-inventory-001.md](quality/field-entry-inventory-001.md)
- POS / KDS 客户端 storeId 端点**下游断点清单**（会断 7 / 不会断但语义错 6 / 已断 7 组；轻档扫描卡 N11 产物）：[quality/p1-pos-kitchen-storeid-downstream-scan-001.md](quality/p1-pos-kitchen-storeid-downstream-scan-001.md)
- P1-USER-LOCATION-002 **阶段 2 设计**（D1–D9 + 19 文件改动清单 + 回滚 + 验收基准定稿 + 未决项 **U1–U8 已全部裁决**）：[design/p1-user-location-002-design-001.md](design/p1-user-location-002-design-001.md)；阶段 1 诊断 = [quality/p1-user-location-002-diagnosis-001.md](quality/p1-user-location-002-diagnosis-001.md)
- P1-USER-LOCATION-002 **阶段 3 实施记录**（C1 `aff2568` / C2 `ff4cba3` / C2b `acb9aa8` / C3 `ddb7d08` / C4 `e97db65` / C5 `563c3e8` 已提交，C6–C10 未开始；实施期新发现 F-1/F-2/F-3 **已裁 = 设计 U9/U10**）：[architecture/03-review/p1-user-location-002-implementation-record-001.md](architecture/03-review/p1-user-location-002-implementation-record-001.md)

## Owner 决策与确认单（等批复/已批复）
> 批复现状 → **单一事实源** [../remediation-roadmap.md](../remediation-roadmap.md)「当前进度总览」；INDEX 只列文件、不复述状态。
- 一页三问（重连策略 / M3-M4 授权 / 死配置）：[quality/owner-decision-confirm-20260927-001.md](quality/owner-decision-confirm-20260927-001.md)（Q3 已执行 = commit `14b9639`）
- 四问是非题（流水失败语义 / 假流水 / 调拨成本 / 事务边界）：[quality/owner-decision-confirm-20260927-002.md](quality/owner-decision-confirm-20260927-002.md)（**执行中：四问按宪法 §四.6 现状迁移**；批复现状见 roadmap「当前进度总览」）

## 实施记录（Location 模型卡，详见任务池 §24.3g/h/i）
- M1-M2：[architecture/03-review/p1-location-model-001-implementation-record-001.md](architecture/03-review/p1-location-model-001-implementation-record-001.md)
- M3-M4：[architecture/03-review/p1-location-model-001-implementation-record-002-m3m4.md](architecture/03-review/p1-location-model-001-implementation-record-002-m3m4.md)
- STOMP 重连：[architecture/03-review/p1-stomp-reconnect-001-implementation-record-001.md](architecture/03-review/p1-stomp-reconnect-001-implementation-record-001.md)
