# E2E 边界退款夹具清单（**保留，勿删**）

> Lead 2026-10-28 决定：**不删夹具，改为「打标 + 保留」**。
> 理由：数据是干净的；且这些 slip 是现成夹具，删了下次还得再造一遍。
> 这也符合当天立的规矩：**能不动数据就不动，动数据只能因为它「脏了」**，不能因为「它是测试造的」。

## 夹具清单（`deposit_slips.remark` 已打标 `[E2E 夹具][勿删]`）

| slip | shift | refund | attribution | 能测什么 |
|------|-------|--------|-------------|----------|
| 386 | 440 | 170 | id=37 | **首次确认**（尚未归属 → 走 CONFIRMED 分支） |
| 387 | 441 | 171 | id=… | **无边界退款空态**（pending 为空的那张） |
| 388 | 442 | 172 | **id=38（version=3）** | **改归属**（442⇄441 已来回改过，是「改归属后只留 1 行」的活证据） |
| 389 | 443 | — | — | **纯空态**（无任何退款） |

## 关键证据（slip 388 / refund 172）

```text
refund_shift_attribution：仍只有 1 行（id=38）—— 改归属是 UPDATE，不是插新行
  version 从 1 → 3（两次改归属各 +1）
  shift_id 最终 = 442（改回本班）
slip 388 refund_component_fen = 8000（重算，未累加）
fund_flows：refund 172 仅 1 条（flow_id=5841）
```

## 完整变更链（`finance_audit_logs`，每次 old→new 一条）

```
log_id 2300  CONFIRMED  refund=172 -> shiftId=441  原因=边界退款测试：归上一班     13:11:41
log_id 2302  CHANGED    refund=172 oldShiftId=441 -> newShiftId=442              13:16:55
log_id 2303  CHANGED    refund=172 oldShiftId=442 -> newShiftId=441  原因=改归属到上一班测试  13:22:48
log_id 2304  CHANGED    refund=172 oldShiftId=441 -> newShiftId=442  原因=改回本班           13:22:48
```
→ 变更链完整可查（谁、何时、原归属、新归属、理由），且**没有漏一条**。

## 回滚/清理指引（将来真要清时）

```sql
-- 1) 先数（今天立的规矩）
SELECT count(*) FROM deposit_slips WHERE remark LIKE '[E2E 夹具]%';
-- 2) 优先软删 attribution，再处理 slip（不要 DELETE 授权/审计类数据）
-- 3) 只有确认是脏数据才物理删，且必须带具体 id
```

---

# 期望状态表（2026-10-28 ts-ops 补：**跑完 e2e 必须核对这张表**）

> 为什么需要这张表：full 跑实测把 `refund 171` 改成了 `shift=442/slip=388/ver=2`，
> 导致「跑一次 e2e 就改一次夹具」—— 夹具变成「每次被随机改写的共享状态」，那就不是夹具了。

## 1. 四个夹具的期望状态

| slip_id | shift_id | terminal_id | 场景标签 | rcf 期望（分） | 归属期望 |
|---|---|---|---|---|---|
| 386 | 440 | T-BND1793163964 | **首次确认场景**（尚未归属） | **0** | 无 attribution |
| 387 | 441 | T-BND1793163980 | 已归属（refund 171） | **8000** | `refund_record_id=171 → shift=441 / slip=387` |
| 388 | 442 | T-BND1793164182 | 已归属（refund 172） | **8000** | `refund_record_id=172 → shift=442 / slip=388` |
| 389 | 443 | T-CLN1793164206 | 纯空态（无退款） | **0** | 无 attribution |

## 2. 核对 SQL（一条命令看是否被污染）

```sql
SELECT s.slip_id, s.shift_id, COALESCE(s.refund_component_fen,0) AS rcf,
       COALESCE((SELECT sum(f.amount) FROM refund_shift_attribution a
                   JOIN fund_flows f ON f.refund_record_id = a.refund_record_id
                  WHERE a.shift_id = s.shift_id AND f.deleted = 0 AND f.status = 2
                    AND f.flow_category = 2), 0) AS attr_sum
  FROM deposit_slips s WHERE s.slip_id IN (386,387,388,389) ORDER BY s.slip_id;
```

判定：**每行 `rcf` 必须等于 `attr_sum`**，且数值与 §1 一致；不等 → 夹具被污染，按 §4 复原。

实测（2026-10-28 15:35，复原后）：
```
slip 386 shift 440 rcf=0     attr_sum=0
slip 387 shift 441 rcf=8000  attr_sum=8000
slip 388 shift 442 rcf=8000  attr_sum=8000
slip 389 shift 443 rcf=0     attr_sum=0
```

## 3. A39 分母为什么会在 4 / 6 / 8 之间变（**不是断言坏了**）

A39 分母 = 「班次窗口已结束 + `difference_net_fen` 非空」的缴款单数。

| 状态 | 分母 | 原因 |
|---|---|---|
| 干净基线 | **4** | 只有 386/387/388/389（T-BND*/T-CLN* 班次，窗口都已结束） |
| 刚跑完全量 e2e、未清理 | **6 ~ 8** | e2e 每段新开 `T-E2E-<tag>-*` 班次并 generate；班次一关，窗口就「已结束」→ 立刻进分母 |
| 按 tag 清理后 | 回到 **4** | `e2e_cleanup.py --tag <tag> --apply` 删该 tag 的班次/缴款单/退款/订单（含 fund_flows 同事务余额校准） |

⇒ 看到分母变大先别怀疑断言：先确认 e2e 数据是否还没清理。

## 4. 复原流程（只许走产品 API，禁止 UPDATE）

```bash
# 例：把 refund 171 的归属复原为 shift=441（既定状态）
POST /v1/finance/deposit-slips/387/confirm-attribution
     {"refundRecordId": 171, "shiftId": 441, "reason": "[fixture-restore] ..."}
```
产品走 CHANGED 分支重算旧/新两侧 slip 的 `refund_component_fen`；**不要** `UPDATE deposit_slips`。

## 5. 夹具隔离规则（Lead 2026-10-28 拍板）

1. 所有 section 的夹具必须 **tag 前缀隔离**（`T-E2E-<tag>-*`），不得引用非本 tag 的数据；
2. section 开始若目标数据不属于本 tag → **直接 `[NOT_READY]` 报错退出**，不许「就地修改」；
3. 三步① 前提是「全新 slip、rcf 必为 0」→ 必须新开班次造 slip，不许复用；
   同时保留 `generate 前后 rcf 增量 == 0` 断言（显式打前后值），防将来约束被绕过。

## e2e 快速模式（`--skip-boundary-wait`）**预期基线**

```text
PASS=52  FAIL=2  exit=1
INFO skipped_design_wait × 2（班次越界 / 退款距关班越界）
```

**两条 FAIL 是「跳过设计内等待」的直接后果，不是缺陷**：

| FAIL | 原因 |
|------|------|
| `AC-58-① 班次距 start 0 分钟（需 > 边界 10 分钟）` | 不等 16 分钟 → 班次仍在边界带内，① 退化为边界场景 |
| `AC-58-①（延后）refund_component_fen == 该笔退款` | 依赖上一条，退款还没被归属 → `refund_component=0` |

**判读规则（给以后看日志的人）**：
```text
看到 52 / 2  → 正常（快速模式基线），不用查
看到别的数字 → 才需要分析（多了 FAIL 或 PASS 数变了都算异常）
要「真正全绿」→ 必须跑全量（不 skip），那需要 ~16 分钟等待
```
