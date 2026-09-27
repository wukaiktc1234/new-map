# docs/INDEX.md（当前事实源索引）

> 本文件是 docs 的入口地图：读文档前先看这里。
> 维护规则：新增/取代任何下述文件时，必须同步更新本索引。

## 当前主线（多门店隔离地基）
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

## Owner 决策与确认单（等批复/已批复）
- 一页三问（重连策略 / M3-M4 授权 / 死配置）：[quality/owner-decision-confirm-20260927-001.md](quality/owner-decision-confirm-20260927-001.md)（Q3 已执行 = commit `14b9639`）
- 四问是非题（流水失败语义 / 假流水 / 调拨成本 / 事务边界）：[quality/owner-decision-confirm-20260927-002.md](quality/owner-decision-confirm-20260927-002.md)（待批复，批复前按宪法 §四.6 现状迁移）

## 实施记录（Location 模型卡，详见任务池 §24.3g/h/i）
- M1-M2：[architecture/03-review/p1-location-model-001-implementation-record-001.md](architecture/03-review/p1-location-model-001-implementation-record-001.md)
- STOMP 重连：[architecture/03-review/p1-stomp-reconnect-001-implementation-record-001.md](architecture/03-review/p1-stomp-reconnect-001-implementation-record-001.md)
