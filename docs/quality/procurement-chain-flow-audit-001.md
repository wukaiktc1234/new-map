# 采购链全流向排查报告 001（procurement-chain-flow-audit-001）

- **日期**：2026-09-26
- **方式**：只读 + 活体（全程真实 HTTP + psql 逐步取证；未修代码、未修 DB、未启动修复卡）
- **测试主体**：采购申请 PR20260926002（request_id=3562c0d7…，AUDIT-采购申请-全字段，生菜 50 斤）→ 采购订单 72 / PO202609260005 → 到货（失败）→ 收货 20（SID=20，confirm 失败）
- **特殊说明响应**：用户报告"采购申请这步有问题"——实测申请状态机本身正常（见异常 A7），**真正的断裂在下游**：到货创建 500（A2）与收货确认 500（A1）。

## 一、完整流程图（实测）

```
申请  POST /v1/purchase/requests
      status: draft ──submit──▶ pending（待审批）──approve──▶ approved
      ✅ 状态机守卫正常：draft 直接 approve 被拒（code 8107）
      落库：purchase_request（title/type/dept/applicant/priority/expected/desc/total 全落）
            purchase_request_item（food_id/food_code/spec/qty/unit/est_price/subtotal/remark 全落）
      ⚠ DTO.storeId 无落库列（静默丢弃）；item 的 plannedReceiverType/plannedStoreId 无申请侧列（丢弃）
订单  POST /v1/purchase/orders（requestId 关联 + items 手工传入）
      → purchase_orders：request_id ✅ 关联；request_no ❌ 未自动回填（NULL）；store_id ❌ NULL（DTO 无字段）
      → purchase_order_items：planned_receiver_type / planned_store_id / planned_warehouse_id **原样透传落库**（大小写不归一）
      状态（order_status 数字）：0 ─submit─▶ 1 ─approve─▶ 2 ─confirm─▶ 6
      ⚠ 字符串 status 字段全程恒 'pending' 不动（双轨状态，数字为真相源）
到货  POST /v1/purchase/arrivals（orderId）
      → ReceiverKey：receiverType = item.planned_receiver_type（**hasText 则原样透传**）否则默认 'STORE'
      → DB CHECK：receiver_type ∈ {'STORE','WAREHOUSE'}
      ❌ item 值='store'（小写）→ **CHECK 违例 → 500，到货无法创建**（异常 A2）
收货  POST /v1/purchase/stockins（orderId + items[].orderItemId=数值主键）
      → /quality-check → /confirm
      confirm 副作用：创建应付账款 payables（payableNo = "AP" + 0填充10位(stockinId)，确定性幂等设计）
      ❌ payableNo 与历史编号空间重叠（AP0000000020 已被 2026-08 老数据占用）→ 唯一约束 500
         → **整个收货事务回滚**：库存不入、应付不建、stockin 停在 status=0（异常 A1）
```

## 二、字段流向表（输入 → 落库）

| 字段 | 申请侧 | 订单侧 | 到货/收货侧 | 判定 |
|------|--------|--------|-------------|------|
| 申请人 applicantId/Name | ✅ purchase_request 落库（响应回显"系统管理员"= 覆盖为登录人） | ❌ 订单表无该列 | — | 申请正常；不跨单据 |
| 部门 departmentId/Name | ✅ 落库 | ❌ 订单表无列（不随单） | — | 单据间部门链断裂（设计如此？待产品确认） |
| **供应商 supplierId** | —（申请无供应商） | ✅ 表头优先落库（F4 修复后） | ✅ arrival/stockin/payable 继承订单供应商 | **修复后正确**（审计单 72 → 12 ✅） |
| 物料 materialId/Name | 申请侧 food_id/food_name 落库；item.planned 字段无列（丢弃） | ✅ material_id/name 落库（手工传入，非自动从申请带出） | ✅ stockin 继承 | 映射依赖调用方手工传，**无申请→订单自动带出** |
| 数量/单位 quantity/unit | ✅ 落库 | ✅ 落库 | ✅ actualQuantity 落库 | 一致 |
| 单价 estimatedPrice/unitPrice | ✅ estimated_price 落库（100→subtotal 5000 分） | ✅ unit_price 落库（10000 分） | ✅ | 申请与订单价格独立计算，非强一致 |
| 金额（分） | ✅ total_amount=500000 | ✅ total_amount=500000（items 计算） | stockin.totalAmount=500000 | ✅ 全链分口径 |
| **status 流转** | draft→pending→approved（approved_by/time 落库） | order_status 0→1→2→6；字符串 status **恒 'pending' 不动** | arrival status=0；stockin 确认失败卡 0 | 数字状态正常；**字符串状态双轨异常**（A6） |
| 关联单据 | — | request_id ✅ / **request_no ❌ NULL**（未自动回填） | arrival.order_id ✅ / stockin.order_id ✅ | **半关联**（A4） |
| storeId（申请 DTO） | ❌ 无列，丢弃 | ❌ DTO 无字段，store_id=NULL | arrival.store_id=item.planned_store_id 继承 | **申请门店信息链路丢失**（A8） |
| item.plannedReceiverType | ❌ 申请 item 无列，丢弃 | ✅ **原样落库（含大小写）** | ❌→**到货 CHECK 违例炸点**（A2） | 大小写不归一 |
| item.plannedStoreId | ❌ 申请 item 无列，丢弃 | ✅ 落库（varchar '1'） | ✅ arrival.store_id 继承 | 订单侧正常 |

## 三、文档差异清单（vs 01-procurement.md）

| # | 类型 | 差异 |
|---|------|------|
| W1 | 文档缺 | 订单状态数字流转 0→1→2→6 与 submit/approve/confirm 的对应关系未写 |
| W2 | 文档缺 | **字符串 status 与 order_status 双轨**：字符串恒 'pending'，数字才是真相源 |
| W3 | 文档缺 | 到货 receiver_type 推导规则（item.planned_receiver_type 原样透传，缺省 STORE）与 **DB CHECK 大小写陷阱**（A2） |
| W4 | 文档缺 | 收货 confirm 的副作用链：创建应付账款（payableNo=AP+stockinId 填充）且**强一致回滚**（A1） |
| W5 | 文档缺 | 申请 DTO.storeId / item.planned* 字段在申请侧无落库列（仅订单 item 侧有 planned 三列） |
| W6 | 文档缺 | request→order 关联：request_id 落库但 request_no 不自动回填；订单 items 需手工传入（无自动从申请生成） |
| W7 | 文档过简 | 不变量 3"金额一律分"补充：申请/订单/收货三段 total 独立计算，相等依赖调用方传值一致 |

（以上 W1~W7 已同步更新进 01-procurement.md）

## 四、异常清单

| 编号 | 严重度 | 异常 | 实锤证据 |
|------|--------|------|----------|
| **A1** | **高（上线阻断）** | **收货 confirm 500：payableNo="AP"+0填充(stockinId) 与历史编号空间重叠**——历史 payables 已占用 AP0000000001~0026，stockinId≤26 的入库单 confirm 必撞唯一约束 → 事务整体回滚：库存不入、应付不建、stockin 卡 status=0。审计单 72（stockinId=20，撞 AP0000000020/PO20260804001）实锤；stockinId 18/19 恰好未占用故此前的单"看起来通" | `payables_payable_no_key` 唯一冲突 + payables 表 AP0000000020 行 + stockin 20 status=0、store_inventory 未增 |
| **A2** | **高（上线阻断）** | **到货创建 500：item.plannedReceiverType 大小写不归一**——订单 item 原样透传 'store'（小写），到货 ReceiverKey hasText 即用，DB CHECK 仅允许 'STORE'/'WAREHOUSE' → 违例。此前订单未传该字段（默认 STORE）掩盖了问题；**任何带小写 plannedReceiverType 的订单到货必炸** | audit order 72 item 落库 'store' → `purchase_arrivals_receiver_type_check` 违例 500 |
| A3 | 已修复 | 表头 supplierId 被档案覆盖（F4） | 本审计单 72 落库 supplier=12 ✅（修复生效） |
| A4 | 低 | order.request_no 未自动回填（requestId 关联存在） | purchase_orders 72: request_no NULL |
| A5 | 低 | 采购订单字符串 status 恒 'pending'（双轨状态） | 各流转节点实测 |
| A6 | 低 | 申请 DTO.storeId 无落库列（门店维度信息丢失） | purchase_request 无 store 列 |
| A7 | 信息 | 申请状态机正常：draft（创建）→ pending（submit）→ approved（approve）；draft 直接 approve 正确拒绝（8107）——**用户报的"申请这步有问题"实测不存在于状态机，断点在下游 A1/A2** | 实测全程 |
| A8 | 信息 | stockin 的 orderItemId 校验正常（我误用他单 item_id 被正确拒绝） | 非缺陷 |

## 五、结论

- 采购链**主干设计完整**（状态机守卫、字段大部分落库、金额分口径一致、F4 修复生效），
- 但存在 **2 个新阻断异常（A1 应付撞号 / A2 到货大小写违例）**——两者共同特征：**此前"看起来通"是因为测试数据恰好绕过**（未传 planned 字段 / stockinId 未撞号），与 F4 同族（静默踩坑）。
- 建议立卡优先级：A1、A2 均 **P1 上线阻断**（A1 使收货必败、A2 使带门店收货计划的订单到货必败）。
