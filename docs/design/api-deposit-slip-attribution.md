# 缴款单「待确认归属」接口契约（v2，2026-10-28 修订）

> 修订原因：pos-ux 对接发现 4 处缺口（缺字段 / 字段名不一致 / 新权限码未授权 / 无夹具）。

## 1. 待确认列表

```http
GET /v1/finance/deposit-slips/{slipId}/pending-refunds
权限：finance:deposit-slip:view
```

返回（`data`）：
```json
{
  "pendingRefunds": [
    {
      "refundRecordId": 161,
      "refundNo": "FF202610280064",      // ← 正名（新）
      "flowNo": "FF202610280064",        // ← 历史别名，同值，**下一版本删除**
      "amountFen": 2500,
      "refundTime": "2026-10-28T12:43:21",
      "orderId": "O1793129431011",
      "boundaryDeltaMinutes": 3,          // ← 新增：距边界分钟差（≤5 即边界退款）
      "boundaryKind": "END",              // ← 新增：START 贴近开班 / END 贴近交班
      "previousShift": { "shiftId": 429, "shiftNo": "S20261028001", "label": "morning #429",
                          "startTime": "…", "endTime": "…", "isPrevious": true },
      "currentShift":  { "shiftId": 430, "shiftNo": "S20261028002", "label": "morning #430",
                          "startTime": "…", "endTime": "…", "isPrevious": false }
    }
  ],
  "candidateShifts": [ { "shiftId": 429, "shiftNo": "…", "isPrevious": true, … } ],
  "missingRefundTimeCount": 0
}
```

**关键约定**：`isPrevious` / `previousShift` / `currentShift` 全部由**后端判定**，前端**不得**用数组下标推导。

### 关于 `shiftNo`
```text
PosShift 实体没有业务编号字段 → 后端返回 shiftNo = "S" + shiftId（如 S442）
**这是展示值，不是业务编号**：只用于界面显示与回传，**不得用于任何业务判断/对账匹配**。
若将来需要真实班次号，必须先由产品定义编码规则并加字段。
```

## 2. 提交归属

```http
POST /v1/finance/deposit-slips/{slipId}/confirm-attribution
body: { "refundRecordId": 161, "shiftId": 429, "reason": "归上一班" }
权限（新）：finance:deposit-slip:attribution:confirm   ← 已补行并授权 ROLE_FINANCE_DIRECTOR / ROLE_STORE_MANAGER
权限（旧）：finance:deposit-slip:confirm-attribution  ← 兼容保留，**下一版本移除**
```

## 3. 三种异常

| 情形 | HTTP | body.code | message |
|------|------|-----------|---------|
| 缺参 | 200 | 400 | 退款记录ID不能为空 / 目标班次ID不能为空 |
| 无权限 | 200 | 403 | 权限不足，无法访问该资源 |
| 已确认过 | 200 | 400 | 该退款已完成归属确认，不可重复操作 |

## 4. 字段/权限的清理计划（避免新旧长期并存）

| 项 | 现状 | 移除条件 |
|----|------|----------|
| `flowNo`（历史别名） | 与 `refundNo` 同值返回 | 前端切到 `refundNo` 后**下一版本**删除 |
| 旧权限码 `finance:deposit-slip:confirm-attribution` | 已绑 2 个角色 | 前端按钮改用新码后下一版本删除 |

> 两侧同时存在是**过渡态**，已在上表写明移除条件；不允许长期并存。
