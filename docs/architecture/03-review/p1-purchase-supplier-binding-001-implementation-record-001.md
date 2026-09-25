# P1-PURCHASE-SUPPLIER-BINDING-001 — 实施记录

## 0. 状态

| 项 | 值 |
|----|------|
| Task ID | `P1-PURCHASE-SUPPLIER-BINDING-001` |
| Stage | **IMPLEMENTED_VERIFIED（2026-09-25；F4 修复卡，随修随验）** |
| 根因 | `docs/quality/f4-supplier-binding-diagnosis-001.md`（判定 E：物料档案主供应商自动分组设计 vs 表头语义冲突）


---

## 4. 实施与验证记录（2026-09-25，本卡随修随记）

| 项 | 值 |
|----|------|
| 修复方案 | **方案① 表头优先 + 档案不一致 log.warn 提示**（`PurchaseOrderServiceImpl.createOrder`；未指定表头时保留档案自动拆分） |
| commit | `0795665`（单文件 +26/-13；**部分提交法**——目标文件含 115 行无关 W1-EC 批次 WIP，按"HEAD 基线打补丁提交 + 工作树保留 WIP+修复"隔离，unstaged diff 复核 = 原样 +115/-2，零夹带） |
| 活体验证 | 修复前：supplierId=11 → 落库 1；修复后：PO202609260001 → **supplier_id=11** + warn 1 条；回归：supplierId=1（=档案绑定）→ 落库 1、无 warn |
| 历史数据 | 6 条 supplier_id=1 审计 = 当时档案绑定一致（档案 2026-07-23 创建后 update_time 从未变更）→ **全部正常，无需修数据** |
| 文档同步 | `docs/business-logic/01-procurement.md`（F4 → 已修复；不变量 1 改写为"表头优先"） |
