# 采购链业务逻辑（申请 → 订单 → 到货 → 收货 → 库存）

> 验证状态：✅ 2026-09-25 活体全链（`business-chain-verification-20260925.md`）；2026-09-26 全流向排查（`docs/quality/procurement-chain-flow-audit-001.md`）发现 A1/A2 两个新阻断异常
> 文档性质：AI 理解稿，**待 Owner 审定**

## 应该做什么

门店/仓库缺货时：采购申请 → 审批 → 按供应商生成采购订单 → 确认后跟踪到货 → 质检收货入仓 → 总仓与门店库存增加。

## 数据流

```
申请  POST /v1/purchase/requests（items[].foodId/foodName/quantity/unit）
        → purchase_request(+item) 表，status: draft →(submit)→ →(approve)→ approved
订单  POST /v1/purchase/orders（supplierId + items[].materialId/quantity/unitPrice）
        → 【关键语义】表头供应商优先（F4 修复后）；未指定表头时按物料档案主供应商自动分组拆单
        → purchase_orders(+items)
        → 数字状态（order_status，真相源）：0 ─submit─▶ 1 ─approve─▶ 2 ─confirm─▶ 6
        → ⚠ 字符串 status 字段全程恒 'pending' 不动（双轨状态，勿读）
        → request_id 落库关联申请；request_no 不自动回填
        → items 的 planned_receiver_type/planned_store_id/planned_warehouse_id 原样透传落库（大小写不归一！）
到货  POST /v1/purchase/arrivals（orderId, shipmentStatus）
        → 【关键语义】receiver_type = item.planned_receiver_type **原样透传**（hasText 时），否则默认 'STORE'
        → ⚠ DB CHECK 仅允许 'STORE'/'WAREHOUSE'（大写）——item 存小写值时到货创建 500（异常 A2）
        → purchase_arrivals(+items)，status 0 起
收货  POST /v1/purchase/stockins（orderId, items[].orderItemId=**数值主键**/actualQuantity）
        → /quality-check（qualityCheckResult=1 + appearanceResult/odorResult=normal）→ /confirm
        → confirm 副作用：创建应付账款 payables（payableNo="AP"+0填充10位(stockinId)，确定性幂等设计）
        → ⚠ 应付创建失败会**回滚整个收货事务**（库存不入、stockin 卡 0）——payableNo 与历史编号空间
          重叠时必炸（异常 A1：历史已占 AP0000000001~0026，stockinId≤26 必撞）
        → 入库：总仓 inventory 增行/增库存；门店库存 store_inventory 增（按 item.planned_store_id 继承）
```

## 关键字段与约束

| 字段 | 约束 | 语义 |
|------|------|------|
| `PurchaseOrderCreateDTO.supplierId` | @NotNull Long | **表头优先（2026-09-25 修复 P1-PURCHASE-SUPPLIER-BINDING-001 后）**：指定表头供应商时全部 item 按表头落库，物料档案绑定不一致仅 log.warn 提示；未指定（防御分支）才按物料档案自动分组拆单 |
| `material_archives.supplier_id` | 物料 ↔ 供应商绑定 | 自动分组的数据源；**改绑定会改变后续所有采购单的落库供应商** |
| purchase 数量/单价 | quantity BigDecimal（业务单位）；unitPrice **Long 分** | total_amount 由系统按分计算 |
| 质检枚举 | qualityCheckResult=1（合格）、appearanceResult/odorResult=`normal|abnormal` | 缺一不可，confirm 前置 |
| 供应商创建 | settlementMethod=`monthly/immediate/prepaid/check/bank_transfer`；category=`原材料/包装/设备/其他`（中文） | 中英混排，前端对接注意 |

## 不变量

1. **订单供应商 = 表头 supplierId（优先）**；物料档案绑定不一致时 log.warn 提示但不阻断（P1-PURCHASE-SUPPLIER-BINDING-001 修复，commit 0795665；根因诊断 `docs/quality/f4-supplier-binding-diagnosis-001.md`）。仅当表头未指定时才回退到物料档案自动分组拆单
2. 收货必须先质检合格才能 confirm
3. 数量与金额：数量按业务单位（斤等），金额一律 Long 分
4. 单据编号：PR/PO/AR/SI + 日期 + 序号，系统生成

## 已知问题

- **A1（高，上线阻断，2026-09-26 排查）**：收货 confirm 500——payableNo="AP"+0填充(stockinId) 与历史编号空间重叠（历史已占 AP0000000001~0026）；stockinId≤26 必炸，事务整体回滚（库存不入、应付不建）
- **A2（高，上线阻断，2026-09-26 排查）**：到货创建 500——item.plannedReceiverType 大小写不归一透传，DB CHECK 仅允许大写 STORE/WAREHOUSE；前端传 'store' 的订单到货必炸
- A4（低）：order.request_no 不自动回填；A5（低）：字符串 status 双轨；A6（低）：申请 DTO.storeId 无落库列
- 全流向细节：`docs/quality/procurement-chain-flow-audit-001.md`（字段流向表 + 异常清单）
- **F4（高）→ 已修复（2026-09-25）**：P1-PURCHASE-SUPPLIER-BINDING-001 表头优先方案；活体验证：supplierId=11 订单落库 11（修复前落 1）+ 档案不一致 warn + 表头=档案回归无 warn；历史 6 条 supplier_id=1 经审计为当时档案绑定一致（正常，无需修数据）
- **F5（中）**：收货入总仓后 inventory 行 product_name/unit 为 NULL（未回填）
- 历史影响面：51 张采购单中 6 张 supplier_id=1，是否错绑待 Owner 逐单裁定

## 验证方式

1. 建申请→提交→审批，psql 断言 purchase_request.status='approved'
2. 建订单（已知供应商的物料），psql 断言 purchase_orders.supplier_id **等于预期主体**（表头 or 档案，按当时语义）
3. 到货+收货+质检+confirm，psql 断言 inventory.current_stock 增量 = actualQuantity，且 **product_name/unit 非空**（F5 修复后）
4. 到货断言（A2 回归）：item.planned_receiver_type 传小写 'store' 时，到货创建应成功（归一化后）或 400 结构化报错——**当前 500 即 A2 未修**
5. 收货断言（A1 回归）：confirm 后 payables 新增一行且 stockin.status 非 0——**stockin 卡 0 即 A1 未修**
