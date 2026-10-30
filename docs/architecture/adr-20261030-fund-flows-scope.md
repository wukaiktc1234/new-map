# ADR-20261030：fund_flows 口径与 balance_after 链（P0-A 诊断结论）

> **一句话结论**：P0-A 不是「after 写错」的问题，是三件事叠加 —— **口径未定义 + 我们自己的两次动作破坏链 + 测试脚本污染**。
> 修复路径 = 定义口径 + 补两条对冲流水 + 登记残留；**不做 after 回填**。

## 1. 口径定义（本轮确立）

```
fund_flows 只记【资金变动】，不记：
  · 期初余额（开业备用金 / 账户初始入金）
  · 任何不产生资金变动的动作
→ 因此每个账户的【首条流水的 balance_after】天然等于「期初偏移」，它【不是缺陷】。

⚠ 规则：opening_offset **不应被拆解**。
   它取决于首条流水 after，把它拆成「几笔不入流水的动作」是一次数值推算错误（今晚犯过：
   Lead 推算 20540，实测 31502）。
```

## 2. 五账户 opening_offset 实测值

| account | 账户名 | account_type | opening_offset | 链是否自洽 |
|---|---|---|---|---|
| 20 | 门店1现金账户 | 2（现金） | **31502** | 否（有 2 处可归因缺口） |
| 21 | — | — | **580** | ✅ 自洽 |
| 22 | — | — | **16800** | ✅ 自洽 |
| 24 | 门店1支付宝商户账户 | 1（银行） | **44700** | 否（s3-selftest 测试流水污染） |
| 25 | — | — | **5000** | ✅ 自洽 |

## 3. 14 断裂点 + 三段归因（account 20 主）

```
段 1（起点 → flow 6012）：偏移恒 +20540  → 口径（期初），非缺陷
段 2（flow 6019 起）：20540 → 24940（+4400） → **gate prime 污染**（走真 resolve 打到真实账户）
段 3（flow 6045 起）：24940 → 35940（+11000） → **A7 清理**（删 5 条 2200 夹具流水，未同步余额）
尾部（flow 6056）：category=8 TEST_WRITEOFF / amount=15400 / direction=2 / after=82202
                  local 正确（97602 − 15400 = 82202 = 当前 balance ✅）

account 24：
  flow 6061 | flow_no=**FFS3E21**（不符合 FF+日期 规范）| category=1 | dir=1 | amount=**45678** | after=44700
            | created_by=**s3-selftest** | remark="S3 sample" | business_date=**2026-10-24**（倒填，flow_id 更大）
  → 排序错位 → 该账户 after 链**不做修复**，登记为历史残留
  → ⚠ 文档必须写明：**account 24 余额 44700 含这笔测试流水影响**，将来对账会遇到
```

## 4. 修复路径

| 动作 | 对象 | 说明 |
|---|---|---|
| 定义口径（文档） | 全部账户 | opening_offset 不拆解、不计缺陷 |
| 补对冲流水 **4400** | account 20 | 方向与金额按归因；business_date 取当日；remark 标注来源 |
| 补对冲流水 **11000** | account 20 | 同上（A7 清理造成） |
| **不修** | account 24 | s3-selftest 残留，登记即可 |
| **不做** | 全部 | **after 回填**（会把 amount 链的错误固化） |

## 5. 待做

| 项 | 下一步动作 | 验收判据 |
|---|---|---|
| 对冲流水方案输出 | 脚本增加「建议对冲流水」段（direction/amount/remark/business_date，只打印） | 两条对冲流水的字段齐全，且金额与归因严格对应 |
| 脚本归因标签 | `gaps_by_attribution` 目前按差值字符串分组（可读性差），改为按归因标签 | 输出 `gate_prime:+4400 / a7_cleanup:+11000 / test_writeoff:…` |
| account 21/22/25 | 已自洽，无需动作 | 三数判定长期保持「相等」 |

## 6. 诊断脚本

`scripts/qa/balance_chain_diag.py` —— **只读 + dry-run，绝不写库**。

```
重放口径：ORDER BY (account_id, business_date, create_time, flow_id)
  -- business_date 必须参与排序：跨日同秒流水否则会错序
方向公式：expected_after = prev_after + (flow_direction == 1 ? amount : -amount)
  取证：PosSaleFinanceRecorder:53 FLOW_DIRECTION_INCOME = 1
        PosRefundFlowRecorder:123 setFlowDirection(2)  → 出账
输出：14 断裂点 + 五账户三段式（opening_offset / gaps_by_attribution / unexplained）
实测：所有账户 **unexplained = 0**（无不明缺口）
```
