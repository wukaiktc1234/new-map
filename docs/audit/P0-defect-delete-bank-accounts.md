# P0 缺陷：自测脚本含 `DELETE FROM bank_accounts`（生产风险，非测试问题）

**登记时间**：2026-10-29 ｜ **等级**：P0 ｜ **状态**：已加 DBGUARD 硬拦截（脚本层面），根因待改造

## 1. 缺陷

```text
scripts/qa/deposit_slip_balance_guard_selftest.py
  → DELETE FROM bank_accounts WHERE store_id=%d

任何人在共享库 food_traceability 跑一次这个 selftest，就可能删掉真实门店的资金账户
（连带其 fund_flows / deposit_slips / bank_transactions 的对账关系）。

2026-10-29 负向验证时实测：该脚本在 guard 生效前已经先建了夹具账户
（store=99001，account 76 BL4-TEST-CASH balance=34200 / account 77 BL4-TEST-BANK balance=0），
随后才被 guard 拦下 —— 证明「先建后删」的顺序本身就是风险。
已按 store 清理并复核 left=0；全局 bank_accounts 回到 11 行 / sum 153762。
```

## 2. 同类高危脚本（同一批）

| 脚本 | 危险语句 | 现行条件 |
|------|----------|----------|
| `deposit_slip_balance_guard_selftest.py` | `DELETE FROM bank_accounts WHERE store_id=%d` | store 参数（非 tag） |
| `bl7_bl8_selftest.py` | `DELETE ... WHERE account_id=24 AND business_date=DATE '2026-10-24'`、`slip_id=61` | 硬编码账户/日期 |
| `deposit_slip_bl4_regression.py` / `_reverse_concurrency_` / `_store_guard_` | `DELETE FROM deposit_slips/fund_flows WHERE store_id=...` | store 参数（非 tag） |
| `recon_time_diff_selftest.py` / `s3_time_diff_scenarios.py` | `DELETE FROM bank_transactions/fund_flows/recon_*` | 条件但非 tag |

## 3. 已采取的缓解（本轮）

```text
① 新增 scripts/qa/_dbguard.py：目标库白名单（food_traceability_test / ft_gate_* / ft_exc / ft_r4）；
   不在白名单且脚本含写操作 → 打印 [DBGUARD] 拒绝对 <db> 执行写操作 → exit 2；
② 14 个含 DELETE 的脚本全部插入 guard（位置：docstring 与 from __future__ 之后、任何业务代码之前）；
③ 负向验证：在共享库逐个执行 → 14/14 全部拒绝，bank_accounts 11 行 / sum 153762 前后一致。
```

## 4. 未完成（根因）

```text
❗ 删除条件仍是 store_id / 硬编码 id，不是 tag ——
   DBGUARD 只保证「不许在共享库跑」；一旦在测试库里多脚本并发或 store 参数写错，仍会互删夹具。
❗ 待改造：所有夹具 DELETE 一律改为 remark LIKE '%[<TAG>]%' 条件（与 e2e_cleanup / audit_nodes 一致）。
❗ 改造后需在隔离库逐一回归（见 docs/qa/test-isolation-plan.md）。
```

## 5. 三段：缓解 / 根治 / 时间盒（Lead 2026-10-29 要求）

### 5.1 缓解（已落地，本轮）

```text
DBGUARD：scripts/qa/_dbguard.py + 14 个含 DELETE 脚本硬拦截。
负向验证（共享库逐个跑）：14/14 REFUSED，bank_accounts 11 行 / sum 153762 **前后一致**。
→ 但 DBGUARD 只保证「不许在共享库跑」，不改变删除条件本身。
```

### 5.2 根治（未做，P0 待办）

```text
把三个高危脚本的夹具 DELETE 条件从 store_id / 硬编码 id 改为 tag：
  remark LIKE '%[SELFTEST:<tag>]%'（与 e2e_cleanup / audit_nodes_check 一致）
涉及：deposit_slip_balance_guard_selftest.py（删 bank_accounts）
      bl7_bl8_selftest.py（account_id=24 + business_date='2026-10-24'、slip_id=61）
      deposit_slip_bl4_regression.py / _reverse_concurrency_ / _store_guard_（按 store_id）
要求：改完在隔离库逐一回归，确认「只能删掉自己 tag 的行」。
```

### 5.3 时间盒（Lead 裁定）

```text
本轮：security_regression 暂停，preflight 打 [SKIP] 并打印原因（可见，不静默）。
下一轮 A：建 ft_gate_<ts> 独立库（Flyway 全量迁移 + seed_min.sql）+ 8082 独立后端实例；
          security_regression 指向 8082，跑完整 6 组 + 夹具 left=0；
          验收：共享库 bank_accounts 11 行 / sum 153762 / account20 82202 前后一致。
下一轮 B（= 8082 跑通后第一件事）：三个高危脚本 tag 化改造 + 隔离库回归。
下一轮 C：8082 跑通后把 8081 的 security_regression 从 preflight 移除（不留两套）。
```

## 6. 通用规则（Lead 2026-10-29，由本次实战得出）

```text
🔴 任何防呆检查必须是脚本的【第一条可执行语句】，晚一行就等于没装。

证据（2026-10-29 实测）：
  第一版把 guard 插在 docstring 之后、业务代码之前 →
  deposit_slip_balance_guard_selftest.py 在被拦下【之前】已经建了两个夹具账户：
    account 76 BL4-TEST-CASH balance=34200 / account 77 BL4-TEST-BANK balance=0（store 99001）
  全局 bank_accounts 从 11 行/153762 → 13 行/187962（已按 store 清理，复核 left=0）

正确插入点：shebang → coding → docstring → from __future__ → **guard** → 业务代码。
（guard 必须在 from __future__ 之后，否则 SyntaxError；但必须在任何 import 业务模块/建连接之前）
```
