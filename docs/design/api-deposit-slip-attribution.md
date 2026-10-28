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

## 6. ⛔ 历史账绝不回溯（Owner 2026-10-28 裁决，**明文规则**）

> **不写任何重算历史缴款单的脚本。**
> **±5 分钟边界规则仅对上线后的新退款生效。**
> 历史误差由**当期盘点**或**人工挂账**解决，**不做数据回溯**。

### 为什么（写给未来想「顺手补一下」的人）

```text
1. 历史缴款单已经过财务签认/入账，重算 = 改动已确认的账，审计链直接失真；
2. 边界规则从 10 分钟改到 5 分钟，属于口径变更，口径变更只向未来生效（会计惯例）；
3. 回溯脚本一旦跑错，影响面是「所有历史缴款单」，且无法区分哪些是「本来对的」——
   这比历史误差本身危险得多。
```

### 允许的处理方式（按优先级）

| 方式 | 适用 | 是否留痕 |
|------|------|----------|
| 当期盘点时消化 | 误差金额小、可并入当月盘点差异 | 盘点表留痕 |
| 人工挂账（红字调整） | 误差金额大、需单独说明 | 冲销凭证留痕 |
| **重算历史缴款单** | **禁止** | — |

## 7. 权威口径：as-of 截断 vs 人工归属（A39 必须与此一致）

> **自动窗口计入受 as-of 约束；人工归属不受 as-of 约束，且优先级更高。**
> 这是 A39 与 `recomputeSlipRefundComponent` 人工归属分支的**权威口径**，二者必须一致。

### 具体规定

| 场景 | 是否看 `refund_time <= slip.create_time` | 说明 |
|------|------------------------------------------|------|
| **自动窗口计入**（退款落在班次窗口内、且不在边界带） | **看**（受 as-of 约束） | 缴款单生成后发生的退款不应自动回填进已生成的单，避免「生成后悄悄变数」 |
| **人工归属**（`confirm-attribution`） | **不看** | 见下 |

### 为什么人工归属不看 as-of（产品裁定，2026-10-28）

```text
人工归属就是**边界情况的逃生口**。
如果财务已经确认「这笔退款归这个班次」，系统却因为「它发生在缴款单生成之后」而拒绝计入，
那这个功能就失去了意义 —— 用户点了确认，系统说「不算」，这比不提供该功能更糟。
```

### 实现约束

- **不要**给人工归属分支加 as-of 截断；
- A39（自动窗口）**保持** as-of 约束不变；
- 若将来有人想让二者统一，必须同时改这两处并更新本节 —— 单边修改会造成「同一笔退款，自动算不进、人工也算不进」或反之的口径撕裂。
