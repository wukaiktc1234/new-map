# 业务链技术验证报告（最小可上线链活体走查）

- **日期**：2026-09-25
- **方式**：活体 HTTP 调用 + psql DB 断言，只核实不修复
- **环境**：本地 dev（工作区构建——如实披露：运行实例编译自工作树 HEAD+WIP，含未提交 canonical WIP 代码）；PostgreSQL 18 @ food_traceability
- **测试主体**：原料模板 QA-TPL-001 / 菜品 QA-红烧土豆（FD260925001，food_id=2）/ 供应商 QA-供应商-绿源蔬菜（supplier_id=11）/ 采购单 PO20260925001（order_id=67）/ 订单 T20260925003

## 步骤 1：新建商品（原料 + 菜品 + 配方）

| 项 | 请求 | 响应 | DB | 判定 |
|----|------|------|-----|------|
| 原料 | `POST /api/v1/material-templates`（QA-TPL-001） | 首发仪式外**500**（`template_code` NOT NULL，接口不自动生成，载荷缺字段即炸且报"系统繁忙"）；补 `templateCode` 后 **code=0**，id=2 | material_template 行存在 | **通（有摩擦）**——发现 F1 |
| 菜品 | `POST /api/v1/product-center/foods`（QA-红烧土豆，categoryId=745，salePrice=1800 分） | **code=0**，foodId=2 / FD260925001 | foods 行 status=1 | **通** |
| 配方 | 随菜品创建（`recipes[]`：materialId=3 生菜，quantity=0.4 斤） | code=0 | dish_recipes recipe_id=90，required_quantity=0.400 斤 | **通**——发现 F2：**无独立配方管理端点**，配方只能随菜品创建/或 SQL |

## 步骤 2：新建供应商

- 请求：`POST /api/v1/suppliers`（QA-供应商-绿源蔬菜）
- 响应：两次 400 枚举校验（结算方式必须是 monthly/immediate/prepaid/check/bank_transfer；分类必须是「原材料/包装/设备/其他」中文枚举）→ 修正后 **code=0**，supplierId=11 / SUP20260925001
- DB：suppliers 行 supplier_id=11, status=1
- **判定：通（有摩擦）**——发现 F3：错误信息为中文枚举白名单，前端可感知，不算静默

## 步骤 3：采购申请 → 审批 → 采购订单 → 到货 → 收货

| 段 | 请求 | 响应 | DB | 判定 |
|----|------|------|-----|------|
| 采购申请 | `POST /v1/purchase/requests`（生菜 100 斤）→ `/{rid}/submit` → `/{rid}/approve` | 3× code=0 | purchase_request PR20260925001 status=**approved**, total_amount=1000000 分 | **通** |
| 采购订单 | `POST /v1/purchase/orders`（supplierId=**11**, item: material 3 × 100 斤 @10000 分）→ submit → approve → confirm | 4× code=0 | purchase_orders id=67 **supplier_id=1（≠11！）**；purchase_order_items item 88 material 3 qty=100 | **断（发现 F4：供应商绑定错位）**——请求 supplierId=11 被忽略，落库为 1（鲜蔬源），响应亦显示错绑；订单其余字段正确 |
| 到货 | `POST /v1/purchase/arrivals`（orderId=67） | code=0 | purchase_arrivals id=22 AR20260925001 | **通** |
| 收货 | `POST /v1/purchase/stockins`（item 88, actualQuantity=100）→ quality-check（qualityCheckResult=1 + appearanceResult=normal 枚举）→ confirm | 质检/确认均 code=0（质检 400 两次为枚举命名摩擦） | purchase_stockins id=18 SI20260925001 totalQuantity=100 | **通** |

## 步骤 4：库存 +100 核查

- **store_inventory**（store 1, material_id=3 生菜）：8.300 → **108.300 = +100，单位斤** ✅
- **inventory（总仓）**：新增行 inventory_id=38 current_stock=**100** ✅——但 **product_name 与 unit 为 NULL**（发现 F5：收货入仓未回填商品名/单位，静默数据缺陷）
- 单位口径：本链全部为**斤**；若要求字面 "+100kg" 需按 200 斤下单。实测增量与下单数量（100）一致、单位语义一致
- **判定：通（数量正确；口径为斤；F5 静默缺陷）**

## 步骤 5：POS 下单 → 支付 → KDS 出餐 → 扣料

| 段 | 请求 | 响应 | 判定 |
|----|------|------|------|
| 下单 | `POST /v1/pos/orders/order`（id=FD260925001, single, ¥18） | 首发 **500「菜品库存不足」——误导性错误**：根因是 legacy `food` 表无该行（count=0），`foodMapper.deductStock` 返 0；新建菜品只写 foods 新表，**legacy 同步仅在启动时执行**（发现 F6）。**重启后端触发同步后 → code=0**，O1790324882333 / T20260925003 | **断后通（F6）** |
| 支付 | `POST /v1/pos/orders/order/pay`（orderId, 现金 ¥18） | code=0（400 一次为字段名 orderId 非 orderNumber） | **通** |
| KDS 拉单 | `GET /v1/kitchen/orders/recent/full` | code=0，KO1790324882332（id=110） | **通** |
| 出餐 | tray create→bind→kitchen-in→kitchen-out→serve（QATRAY02） | 5× code=0，kitchen_order id=110 status=**served** | **通** |

## 步骤 6：支付/财务记录核查

- **order_payment_records**：payment_id=47，order_id=O1790324882333，payment_method=2（现金），payment_amount=**1800 分**，transaction_no=CSH1790325104125 ✅
- **finance_records**：2 行——record 19（type=1，amount=**1800**，order_code=T20260925003）✅；record 20（type=1，**amount=0**，同订单）——**发现 F7**：伴随一条 0 元流水行，性质待查（优惠/找零占位？），无说明
- **判定：通（F7 记录在案）**

## 步骤 7：扣料 -200g 核查

- store_inventory_log：**id=67，生菜 108.300 → 107.900，change=0.400 斤，remark=KDS出餐扣料 - 订单:T20260925003** ✅
- **0.4 斤 × 500g/斤 = 正好 -200g**，与配方 required_quantity=0.4 精确一致
- store_inventory 现值 107.900 ✅；foods.stock 100 → 99（菜品份数扣减）✅
- **判定：通（-200g 精确达成）**

## 发现汇总（F1~F7，均未修复，按指令只核实）

| # | 严重度 | 发现 |
|---|--------|------|
| F1 | 低 | 原料模板创建：`template_code` NOT NULL 但接口不自动生成且 500 报"系统繁忙"（应 400 带字段提示） |
| F2 | 中 | **配方无独立管理端点**——只能随菜品创建（recipes[]）或 SQL；已有配方的编辑/删除路径未验证 |
| F3 | 低 | 供应商/质检等枚举为中文/魔法值混排（原材料 vs monthly vs qualityCheckResult=1 + appearanceResult=normal），前端对接成本高 |
| **F4** | **高** | **采购订单供应商绑定错位**：请求 supplierId=11，落库与响应均为 supplier_id=1——采购对账主体错误 |
| **F5** | 中 | 收货入仓后总仓 inventory 行 product_name/unit 为 NULL（静默数据缺陷） |
| **F6** | **高** | **新建菜品不能立即 POS 下单**：legacy food 表不同步（仅启动时同步），报误导性"库存不足"；需重启才恢复 |
| F7 | 低 | 支付产生两条 finance_records，其一 amount=0，语义不明 |
| — | 信息 | 本链单位口径为**斤**（1 斤=500g）；"100kg/200g" 类需求需按斤换算下量 |

## 结论

**最小可上线链整体可走通**：原料→菜品+配方→供应商→采购→到货→收货→库存+100→下单→支付→KDS→出餐→扣料 -200g，**七步全部以真实 DB 证据闭环**。但存在 **2 个高危断点（F4 供应商绑定错位 / F6 新菜品下单断链）**与多项中低摩擦——**上线前建议将 F4/F5/F6 建卡修复**。


---

## 复跑（2026-09-26，F4+F6 修复后全链贯通）

| 步骤 | 结果 | 证据 |
|------|------|------|
| 新供应商（supplier_id=12）+ 采购单 | ✅ | PO202609260003 **supplier_id=12**（F4 修复前会错绑 1）|
| 审批→确认→到货→收货 | ✅ | SI202609260001，20 斤 |
| 库存 +20 | ✅ | store_inventory 107.5 → **127.5**（+20 斤） |
| 新菜品（创建即下单）| ✅ | FD260926005 → T20260926006 code=0（F6 修复） |
| 支付→出餐→扣料 | ✅ | served + material_consumed=1；store_inventory **127.5→127.1（-0.4 斤=-200g）**；log change=0.400 |
| 支付/财务记录 | ✅ | order_payment_records 1 行；finance_records 2 行 |

**复跑结论：F4/F6 修复后全链贯通，上线阻断项清零**（F1/F2/F3/F5/F7 为非阻断体验/数据质量问题，维持登记）。
