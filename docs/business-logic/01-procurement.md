# 采购链业务逻辑（申请 → 订单 → 到货 → 收货 → 库存）

> 验证状态：✅ 2026-09-25 活体全链（证据 `docs/quality/business-chain-verification-20260925.md`）
> 文档性质：AI 理解稿，**待 Owner 审定**

## 应该做什么

门店/仓库缺货时：采购申请 → 审批 → 按供应商生成采购订单 → 确认后跟踪到货 → 质检收货入仓 → 总仓与门店库存增加。

## 数据流

```
申请  POST /v1/purchase/requests（items[].foodId/foodName/quantity/unit）
        → purchase_request(+item) 表，status: draft →(submit)→ →(approve)→ approved
订单  POST /v1/purchase/orders（supplierId + items[].materialId/quantity/unitPrice）
        → 【关键语义】按物料档案主供应商自动分组，多供应商自动拆成多张单
        → purchase_orders(+items)，status 0(draft) →(submit)→ →(approve)→ →(confirm)→ 6
到货  POST /v1/purchase/arrivals（orderId, shipmentStatus）
        → purchase_arrivals(+items)，status 0 起
收货  POST /v1/purchase/stockins（orderId, items[].orderItemId/actualQuantity）
        → /quality-check（qualityCheckResult=1 + appearanceResult=normal）→ /confirm
        → 入库：总仓 inventory 增行/增库存；门店库存 store_inventory 增（plannedStoreId）
```

## 关键字段与约束

| 字段 | 约束 | 语义 |
|------|------|------|
| `PurchaseOrderCreateDTO.supplierId` | @NotNull Long | **⚠ 仅 fallback**：item 的物料在 `material_archives` 有主供应商绑定时，按档案分组（表头被忽略）；无绑定才用表头。见"不变量" |
| `material_archives.supplier_id` | 物料 ↔ 供应商绑定 | 自动分组的数据源；**改绑定会改变后续所有采购单的落库供应商** |
| purchase 数量/单价 | quantity BigDecimal（业务单位）；unitPrice **Long 分** | total_amount 由系统按分计算 |
| 质检枚举 | qualityCheckResult=1（合格）、appearanceResult/odorResult=`normal|abnormal` | 缺一不可，confirm 前置 |
| 供应商创建 | settlementMethod=`monthly/immediate/prepaid/check/bank_transfer`；category=`原材料/包装/设备/其他`（中文） | 中英混排，前端对接注意 |

## 不变量

1. **订单供应商 = 物料档案主供应商（有绑定时）**——表头 supplierId 不能覆盖。⚠ 与"表头为准"直觉冲突，是 F4 的根因（根因诊断 `docs/quality/f4-supplier-binding-diagnosis-001.md`）
2. 收货必须先质检合格才能 confirm
3. 数量与金额：数量按业务单位（斤等），金额一律 Long 分
4. 单据编号：PR/PO/AR/SI + 日期 + 序号，系统生成

## 已知问题

- **F4（高，上线阻断）**：表头 supplierId 语义陷阱——采购已绑主供应商的物料时，新建供应商通过表头传参不生效
- **F5（中）**：收货入总仓后 inventory 行 product_name/unit 为 NULL（未回填）
- 历史影响面：51 张采购单中 6 张 supplier_id=1，是否错绑待 Owner 逐单裁定

## 验证方式

1. 建申请→提交→审批，psql 断言 purchase_request.status='approved'
2. 建订单（已知供应商的物料），psql 断言 purchase_orders.supplier_id **等于预期主体**（表头 or 档案，按当时语义）
3. 到货+收货+质检+confirm，psql 断言 inventory.current_stock 增量 = actualQuantity，且 **product_name/unit 非空**（F5 修复后）
