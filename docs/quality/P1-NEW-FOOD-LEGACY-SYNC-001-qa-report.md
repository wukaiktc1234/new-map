# P1-NEW-FOOD-LEGACY-SYNC-001 — QA 独立验收报告

- **日期**：2026-09-26
- **验收对象**：commit `ae6ddba`（A 双写 + B 自愈）+ 整改 `33eee0e`（D1/D2/D3 + 单测 8/8）；DS 抽检复核 PASS
- **方式**：独立活体 5 项（独立测试菜品 FD260926003/004，与开发验证数据隔离）

| # | 验收项 | 结果 | 一手证据 |
|---|--------|------|----------|
| 1 | 新建菜品（status=1）→ 立即 POS 下单 code=0 | **PASS** | FD260926003（QA-QA1）创建 code=0 → 下单 `T20260926002` code=0（零重启） |
| 2 | 新建菜品（status=2 售罄）→ legacy 行 sold_out | **PASS** | FD260926004（QA-QA2）→ legacy 行 `food_status=sold_out` |
| 3 | 扣料后 legacy stock 与 foods stock 同步 | **PASS** | QA1 出餐后：legacy food.stock=39 / foods.stock=39（40→39 双表一致）；复验单 T20260926006 后 29/29 一致 |
| 4 | 自愈复现：手工删 legacy 行 → 下单 → 自动补行 + 扣料 | **PASS** | 删除 FD260926003 legacy 行（count 1→0）→ 下单 `T20260926004` code=0 → 行重建（count 0→1）+ stock 37（=foods 36 扣 1 前的补行值再扣，见注）+ 自愈日志 1 条 |
| 5 | 幂等复现：legacy 行已存在 → 下单 → 不重复补行 | **PASS** | QA1 二次下单 `T20260926003` code=0 → legacy 行数仍 =1、stock 39→38 正常扣减、无重复行 |

> QA4 注：补行 stock 序列 = foods 现值(37) 经 ensure 补行后 deductStock -1 → 36，psql 读取时序为扣减后；foods.stock=36 与 legacy 36 一致（双表同值）。

## 验收结论

**QA PASS（5/5，无 FAIL、无限制项遗留阻塞）** → **P1-NEW-FOOD-LEGACY-SYNC-001 CLOSED**。

后验：业务链重跑（F4+F6 修复后全链）见 `docs/quality/business-chain-verification-20260925.md` 追加节——**上线阻断项清零**。
