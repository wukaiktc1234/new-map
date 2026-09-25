# P1-PROCUREMENT-BLOCKERS-001 — 实施记录

## 0. 状态

| 项 | 值 |
|----|------|
| Task ID | `P1-PROCUREMENT-BLOCKERS-001` |
| Stage | **IMPLEMENTED_VERIFIED（2026-09-26）**——A1/A2 修复 + 活体验证；是否补 DS/QA 待 Owner |
| 根因 | `docs/quality/procurement-chain-flow-audit-001.md` 异常清单 A1/A2 |

## 1. 修复内容（3 文件，+42/-6）

| 文件 | 修复 |
|------|------|
| `PayableServiceImpl.java`（A1） | payableNo 生成改「AP+日期+当日序号」（如 AP20260926001）；幂等键改按 **stockin_id 查重**（不靠 payableNo 编码）——旧 "AP"+0填充(stockinId) 与历史编号空间重叠（AP0000000001~0026） |
| `PurchaseArrivalServiceImpl.java`（A2 读侧） | ReceiverKey receiverType 大写归一（历史小写数据兼容） |
| `PurchaseOrderServiceImpl.java`（A2 写侧 + 议题6①） | saveOrderItems plannedReceiverType 大写归一；approveOrder 落 approval_user_id（议题6① 审批留痕） |

## 2. 隔离方式（PG-001 v2）

- PayableServiceImpl 开卡时零 WIP → 直接改
- PurchaseOrderServiceImpl（115 行 WIP）/ PurchaseArrivalServiceImpl（62 行 WIP）→ **部分提交法**（HEAD 打补丁提交 + 工作树保留 WIP+修复），修复区域与 WIP 零重叠（hunk 级核实）

## 3. 活体验证

| 项 | 结果 |
|----|------|
| A1 | stockin 20（修复前必撞 AP0000000020）→ quality-check + confirm **code=0**；payable **AP20260926001** 创建（stockin_id=20 幂等键）；store_inventory **127.1 → 177.1（+50 斤）** |
| A2 写侧 | 新订单 item plannedReceiverType='store'（入参小写）→ 落库 **STORE** |
| A2 读侧 | 模拟历史小写数据（SQL 置 'store'，测试数据操作）→ 到货创建 **code=0，receiver_type=STORE** |
| 议题6① | order 74 approve 后 **approval_user_id=1** |
| 数据核对 | stockinId≤26 未确认：仅历史测试单 stockin 1/2（建议人工处理或忽略） |

## 4. 残留

- Pricing 单侧改价 / OrderTimeoutTask 单侧回补（登记不修）
- 议题 1/2/3/5 的设计建议见 `docs/quality/procurement-design-review-001.md`（待 Owner 逐条裁决）
