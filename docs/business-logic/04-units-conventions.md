# 单位与金额约定（units & conventions）

> 验证状态：✅ 2026-09-25 实测（业务链活体走查全程换算核对）
> 文档性质：AI 理解稿，**待 Owner 审定**——本篇是跨模块"度量衡宪法"，改动需全链回归

## 重量单位

| 场景 | 单位 | 说明 |
|------|------|------|
| store_inventory.current_stock / 原料库存 | **斤** | 1 斤 = 500g；系统内所有原料库存的存储与展示单位 |
| dish_recipes.required_quantity | **斤** | 配方消耗量：1 份菜品的原料消耗（实测 0.4 斤 → 扣 0.4） |
| store_inventory_log.change_quantity | 斤 | before/after/change 三列同单位，可交叉验证 |
| 采购单 items.quantity | 业务单位（跟单带 unit 字符串） | 收货 actualQuantity 按单单位落库存 |
| "g/kg" 表达 | **换算后录入** | 200g = 0.4 斤；100kg = 200 斤——系统不存 g/kg，需求里出现必须先换算 |

**核心不变量**：`扣减量(斤) = required_quantity(斤/份) × 份数`，实测 0.4×1=0.4（=200g）精确成立。

## 金额单位

| 场景 | 单位 | 说明 |
|------|------|------|
| foods.salePrice/costPrice、dish_combos.combo_price、purchase unit_price、finance_records.amount | **Long 分** | 一切落库金额 = 分 |
| 菜单出口（PosComboDTO.price 等） | 元（BigDecimal，scale 2） | `BigDecimal.valueOf(分, 2)`，1990 → 19.90 |
| POS 下单请求价 | **元**（请求侧） | 后端 yuanToFen 落库；与 DB 价差异仅 warn 不阻断 |
| 总额 | 分 | total_amount/final_amount 均 Long 分 |

**核心不变量**：`落库金额 = 分`；出口展示 = 元（2 位小数）。元↔分换算点只有两处（出口 DTO / 下单入口），不要在中间层二次换算（防 double-conversion）。

## 其他约定

- 单据编号：PR/PO/AR/SI（采购）、T（订单）、O（订单 ID 前缀）、KO（厨单）——均为"前缀+日期+序号"，系统生成，不手填
- 双表并存窗口：foods↔food、dish_combos↔dish_combo、combo_ingredients↔combo_ingredient——**新表为真相源**，legacy 由 DatabaseFixConfig 启动时同步（采购/POS 写路径以新表为准，POS 库存扣减例外：food 表仍参与）
- 状态枚举中英混排（如供应商 category=中文、结算方式=英文、质检=数字+normal/abnormal）——对接时以各 DTO 为准

## 已知歧义（待 Owner 裁定后更新本档）

1. 采购单 items.unit 与库存单位不一致时（如单填 kg、库存是斤）**不做换算**——需求方必须按斤下单，或立卡引入单位换算层
2. finance_records 存在 amount=0 的伴随行（F7），语义未确认
