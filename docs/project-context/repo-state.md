# Repo 状态快照（repo-state.md）

> **性质声明**：本文档为**视图层快照，非权威来源**。与 git 实际状态不一致时，**以 git 为准**。
> 同步责任：PG-002（见同目录 `process-guards.md`）——每次卡片收口或 push 后由 developer 更新本文件。
> 快照时点：2026-09-25（push 批次 2026-09-25 销项后、P1-COMBO-LEGACY-CLEANUP-001 启动前）

## 当前状态

| 项 | 值 |
|----|------|
| 分支 | `master`（单分支工作流） |
| HEAD（快照时点） | `7a802ca` docs(governance): register pending push batch 2026-09-25 |
| 远程 | `origin` = https://github.com/wukaiktc1234/new-map.git |
| 远程同步 | **同步（ahead=0）**：push 批次 2026-09-25（5 commit：`1a59f93`→`7a802ca`）已推送成功，`c3cbd3a..7a802ca master -> master` |
| 工作区 | **存在大量既有未提交 WIP（~1196 个已修改条目，canonical 迁移等，属 ENV-1/ENV-2 同源遗留债）**——P1-COMBO-LEGACY-CLEANUP-001 启动须先过 PG-001 门禁 |

## 已完成卡片

| Task ID | 终态 | 日期 | 备注 |
|---------|------|------|------|
| P1-ORDER-NUMBER-002（A1） | PASS_WITH_LIMITATION（本地放行 / Gate ALLOW_LOCAL） | 2026-09-23 | order_number 唯一性；限制 KL-069~073 |
| P1-POS-FOODID-MAP-001 | CLOSED_WITH_REGISTERED_LIMITATIONS | 2026-09-24 | POS food_id 映射；残余 KL-074~076 + ENV-1 |
| P1-COMBO-ORDER-001 | CLOSED_WITH_REGISTERED_LIMITATION | 2026-09-24 | 套餐下单 + KDS components[]；限制 KL-078~080 + ENV-2；生产均 PROVISIONAL |

## 活动/待启动卡片

| Task ID | 状态 | 说明 |
|---------|------|------|
| P1-COMBO-LEGACY-CLEANUP-001 | **启动中（2026-09-25）** | 废弃 combo_ingredient 旧表 / 切 getFullMenu 从 legacy dish_combo 到 dish_combos / 清理旧 POS 兼容代码 / KL-080 三文件归并 / DatabaseFixConfig 列名 bug；PG-001 门禁：工作区 WIP 处置为前置条件 |

## 已知环境问题（ENV 序列）

| 编号 | 内容 | 登记 |
|------|------|------|
| ENV-1 | `PosOrderCreateServiceImpl` 工作区 WIP（+681 行）导致 FOODID 卡 diff 不可隔离 | `p1-pos-foodid-map-001-implementation-record-001.md` §V9 |
| ENV-2 | commit `aec5c45` 内容混合（combo 卡 + P0 编译修复 + canonical WIP），已推送远程，diff 不可隔离 | `production-known-limitations.md` 主表 ENV-2 行 + 文末块 |

## 待处理项

**无**。（push 批次 2026-09-25 已于 2026-09-25 销项：5 commit 全部推送成功，ahead=0）

## 最近 10 commit（快照时点）

| Hash | 消息 |
|------|------|
| 7a802ca | docs(governance): register pending push batch 2026-09-25 |
| 0a7d963 | docs(governance): register PG-002 (repo state sync) |
| 040be94 | docs(project-context): add repo state snapshot |
| bbd52b0 | docs(governance): register ENV-2 and PG-001 |
| 1a59f93 | docs(governance): close out P1-COMBO-ORDER-001 (PWL) |
| c3cbd3a | docs(env): replace example JWT secret with placeholder |
| c182e63 | chore(security): remove tracked test keystore |
| 0274549 | chore(security): remove remaining nested e2e auth token |
| e7cbf22 | P1-COMBO-ORDER-001: regression REG-ORDER-008~011, task board §23, KL-078~080, roadmap |
| 24a1f60 | P1-COMBO-ORDER-001: QA PASS_WITH_LIMITATION report + status writeback |

## 已知遗留（不阻塞，独立决策）

- **KL-077**：git 历史含明文凭据（`dd33ee3` 基线 + 后续 commit 中的 keystore/JWT），HEAD 已清理 tracked 凭据（`883b639`/`fe3902f`/`0274549`/`c182e63`/`c3cbd3a`），**决定不重写历史**；若仓库公开/外泄须重评
- 生产侧证据缺口：KL-069 / KL-076 / KL-079 同族（PROVISIONAL_PENDING_PRODUCTION_EVIDENCE，仅阻断生产放行）
