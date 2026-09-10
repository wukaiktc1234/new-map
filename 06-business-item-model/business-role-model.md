# 餐饮ERP系统业务角色模型分析

## 业务角色定义

### 1. SELLABLE（可销售）
**定义**：可以直接销售给客户的商品或服务。
**所需数据**：
- 基本信息：名称、编码、描述
- 销售属性：销售价格、单位、规格
- 分类信息：商品类别、品牌
- 关联信息：图片、备注
**生命周期**：
1. 创建（上架）→ 启用 → 停用 → 下架
**依赖关系**：
- 上游：PRODUCIBLE（可生产）或 PACKAGED（预包装）或 SERVICE（服务）
- 下游：CONSUMABLE（可消耗）
**消费者**：销售订单、POS系统、线上平台
**所在系统表**：foods（菜品/食品）

### 2. PURCHASABLE（可采购）
**定义**：可以从供应商采购的商品或物料。
**所需数据**：
- 基本信息：名称、编码、描述
- 采购属性：采购价格、最小起订量、采购单位
- 供应商信息：供应商编码、供应商名称
- 物流信息：交货周期、运输方式
**生命周期**：
1. 创建 → 评估 → 试用 → 正式采购 → 停用
**依赖关系**：
- 上游：无（独立角色）
- 下游：STOCKABLE（可存储）、CONSUMABLE（可消耗）
**消费者**：采购订单、采购计划、库存管理
**所在系统表**：product（产品/物料基础信息）

### 3. STOCKABLE（可存储）
**定义**：可以存储在仓库中的商品或物料。
**所需数据**：
- 基本信息：名称、编码、描述
- 存储属性：存储条件、保质期、仓库位置
- 库存管理：安全库存、最大库存、库存预警
- 批次信息：生产日期、批次号、序列号
**生命周期**：
1. 创建 → 入库 → 在库 → 出库 → 报废
**依赖关系**：
- 上游：PURCHASABLE（可采购）或 PRODUCIBLE（可生产）
- 下游：SELLABLE（可销售）、CONSUMABLE（可消耗）
**消费者**：库存管理、仓库管理、盘点系统
**所在系统表**：material_archives（商品档案）、inventory（库存管理）

### 4. INGREDIENT（原料/配方成分）
**定义**：用于制作菜品或产品的原料或成分。
**所需数据**：
- 基本信息：名称、编码、描述
- 配方属性：用量单位、用量比例、成本
- 营养信息：热量、蛋白质、脂肪等
- 质量标准：等级、规格、验收标准
**生命周期**：
1. 创建 → 评估 → 试用 → 正式使用 → 停用
**依赖关系**：
- 上游：PURCHASABLE（可采购）、STOCKABLE（可存储）
- 下游：PRODUCIBLE（可生产）、CONSUMABLE（可消耗）
**消费者**：菜品配方、生产配方、成本核算
**所在系统表**：product（产品/物料基础信息）、material_archives（商品档案）

### 5. CONSUMABLE（可消耗）
**定义**：在生产、销售或服务过程中被消耗的商品或物料。
**所需数据**：
- 基本信息：名称、编码、描述
- 消耗属性：消耗单位、消耗定额、消耗频率
- 成本信息：单位成本、消耗成本
- 追溯信息：批次号、序列号
**生命周期**：
1. 创建 → 入库 → 消耗 → 核算
**依赖关系**：
- 上游：INGREDIENT（原料/配方成分）、STOCKABLE（可存储）
- 下游：无（消耗后结束）
**消费者**：生产过程、销售过程、成本核算
**所在系统表**：inventory（库存管理）、dish_recipe（菜品配方）

### 6. PRODUCIBLE（可生产）
**定义**：可以通过生产过程制造出来的商品。
**所需数据**：
- 基本信息：名称、编码、描述
- 生产属性：生产配方、生产流程、生产周期
- 成本信息：生产成本、直接材料、直接人工
- 质量标准：成品标准、检验标准
**生命周期**：
1. 创建 → 试产 → 量产 → 停产
**依赖关系**：
- 上游：INGREDIENT（原料/配方成分）
- 下游：SELLABLE（可销售）、STOCKABLE（可存储）
**消费者**：生产计划、生产订单、成本核算
**所在系统表**：foods（菜品/食品）、dish_recipe（菜品配方）

### 7. PACKAGED（预包装）
**定义**：预先包装好的商品，可直接销售。
**所需数据**：
- 基本信息：名称、编码、描述
- 包装属性：包装规格、包装材料、包装成本
- 标签信息：生产日期、保质期、成分表
- 物流信息：重量、体积、存储条件
**生命周期**：
1. 创建 → 包装 → 入库 → 销售 → 过期处理
**依赖关系**：
- 上游：PRODUCIBLE（可生产）、INGREDIENT（原料/配方成分）
- 下游：SELLABLE（可销售）
**消费者**：包装生产线、仓储管理、销售系统
**所在系统表**：foods（菜品/食品）、material_archives（商品档案）

### 8. SERVICE（服务）
**定义**：提供的无形服务，非实体商品。
**所需数据**：
- 基本信息：名称、编码、描述
- 服务属性：服务时长、服务内容、服务标准
- 定价信息：服务价格、计价方式
- 人员信息：服务人员、技能要求
**生命周期**：
1. 创建 → 上线 → 服务 → 结束 → 下线
**依赖关系**：
- 上游：无（独立角色）
- 下游：SELLABLE（可销售）
**消费者**：销售订单、服务管理、客户评价
**所在系统表**：foods（菜品/食品，服务类菜品）

## 业务角色关系分析

### 角色独立性分析
1. **独立角色**：
   - PURCHASABLE（可采购）：独立角色，可直接创建
   - SERVICE（服务）：独立角色，可直接创建

2. **依赖角色**：
   - SELLABLE（可销售）：依赖于 PRODUCIBLE 或 PACKAGED 或 SERVICE
   - STOCKABLE（可存储）：依赖于 PURCHASABLE 或 PRODUCIBLE
   - INGREDIENT（原料/配方成分）：依赖于 PURCHASABLE 和 STOCKABLE
   - CONSUMABLE（可消耗）：依赖于 INGREDIENT 和 STOCKABLE
   - PRODUCIBLE（可生产）：依赖于 INGREDIENT
   - PACKAGED（预包装）：依赖于 PRODUCIBLE 和 INGREDIENT

### 角色多重性分析
一个对象可以同时拥有多个角色：
1. **product（产品）**：
   - 可以同时是 PURCHASABLE + STOCKABLE + INGREDIENT
   - 例如：面粉既是采购物料，也是库存物料，还是面点的原料

2. **material_archives（商品档案）**：
   - 可以同时是 STOCKABLE + INGREDIENT + CONSUMABLE
   - 例如：鸡蛋既是库存物料，也是蛋糕的原料，也是消耗品

3. **foods（菜品/食品）**：
   - 可以同时是 SELLABLE + PRODUCIBLE + PACKAGED
   - 例如：红烧肉既是销售菜品，也是可生产的菜品，也是预包装食品

### 角色转换关系
1. **采购→存储**：PURCHASABLE → STOCKABLE（采购入库）
2. **存储→消耗**：STOCKABLE → CONSUMABLE（生产领料）
3. **原料→生产**：INGREDIENT → PRODUCIBLE（生产过程）
4. **生产→销售**：PRODUCIBLE → SELLABLE（成品销售）
5. **生产→包装**：PRODUCIBLE → PACKAGED（包装过程）
6. **包装→销售**：PACKAGED → SELLABLE（包装食品销售）

## 系统表与角色映射

| 系统表 | 对应业务角色 | 说明 |
|--------|-------------|------|
| foods | SELLABLE, PRODUCIBLE, PACKAGED, SERVICE | 菜品/食品，可销售、可生产、预包装、服务 |
| product | PURCHASABLE, INGREDIENT | 产品/物料，可采购、可作为原料 |
| material_archives | STOCKABLE, INGREDIENT, CONSUMABLE | 商品档案，可存储、可作为原料、可消耗 |
| inventory | STOCKABLE, CONSUMABLE | 库存管理，存储管理、消耗记录 |
| dish_recipe | INGREDIENT, PRODUCIBLE | 菜品配方，原料关系、生产配方 |
| purchase_order_items | PURCHASABLE | 采购订单明细，采购对象 |
| order_items | SELLABLE | 订单明细，销售对象 |

## 业务流程中的角色流转

### 采购流程
```
PURCHASABLE (product) → STOCKABLE (material_archives) → INGREDIENT (product/material_archives)
```

### 生产流程
```
INGREDIENT (product/material_archives) → CONSUMABLE (inventory) → PRODUCIBLE (foods) → SELLABLE (foods)
```

### 销售流程
```
SELLABLE (foods) → order_items → 客户
```

### 库存流程
```
STOCKABLE (material_archives) → CONSUMABLE (inventory) → 消耗核算
```

## 角色依赖关系图

```
PURCHASABLE (独立)
    ↓
STOCKABLE (依赖PURCHASABLE)
    ↓
INGREDIENT (依赖PURCHASABLE和STOCKABLE)
    ↓
CONSUMABLE (依赖INGREDIENT和STOCKABLE)
    
PRODUCIBLE (依赖INGREDIENT)
    ↓
PACKAGED (依赖PRODUCIBLE和INGREDIENT)
    ↓
SELLABLE (依赖PRODUCIBLE或PACKAGED或SERVICE)

SERVICE (独立)
    ↓
SELLABLE
```

## 总结

1. **角色独立性**：PURCHASABLE和SERVICE是独立角色，其他角色都有依赖关系。
2. **角色多重性**：一个业务对象可以同时拥有多个角色，这在实际业务中很常见。
3. **角色转换**：业务角色在业务流程中会发生转换，形成完整的业务链路。
4. **数据映射**：不同的系统表对应不同的业务角色，需要建立清晰的映射关系。
5. **依赖管理**：需要管理好角色之间的依赖关系，确保业务流程的完整性。

通过建立清晰的业务角色模型，可以更好地理解系统数据结构，优化业务流程，提高系统可维护性和扩展性。