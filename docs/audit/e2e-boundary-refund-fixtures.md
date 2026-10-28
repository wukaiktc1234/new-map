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
