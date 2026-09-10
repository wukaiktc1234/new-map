# 销售/POS模型分析

## 1. 当前系统现实

### 1.1 核心表结构

```
orders (订单主表)
├── order_id (PK)
├── order_code (订单编号)
├── order_type (1堂食 2外卖 3自提 4打包)
├── order_source (1收银台 2小程序 3第三方平台)
├── table_id (桌台ID)
├── order_status (0待确认 1已确认 2已完成 3已取消)
├── payment_status (0未支付 1部分支付 2已支付 3已退款)
├── total_amount (订单总金额)
├── final_amount (实付金额)
└── ...

order_items (订单明细)
├── item_id (PK)
├── order_id (FK)
├── product_type (1单品 2套餐)  ← 关键：区分单品和套餐
├── food_id (单品ID)            ← 关键：关联的是 food
├── combo_id (套餐ID)
├── product_name (商品名称)
├── unit_price (单价)
├── quantity (数量)
├── amount (小计金额)
└── kitchen_status (0待制作 1制作中 2已完成 3已上菜 4已退款)
```

### 1.2 关键发现

1. **销售对象是 Food**：order_items 关联的是 food_id，不是 material_id
2. **Food 具有销售属性**：food 表包含 sale_price、cost_price、stock、min_stock 等
3. **Food 与 Material 的关系**：通过 dish_recipe 表连接，而不是继承

## 2. 统一对象分析

### 2.1 POS 销售的统一对象是什么？

**结论：POS 销售的统一对象是 `Food`（菜品/食品）**

理由：
- order_items 直接关联 food_id
- food 表包含完整的销售属性（售价、库存、图片、描述等）
- food 是顾客可见、可点的商品

### 2.2 Food 的业务语义

```
Food = 可销售的商品单元

在不同场景下的角色：
├── POS 收银：Food 是顾客点的菜品
├── 外卖平台：Food 是上架的商品
├── 库存管理：Food 是成品库存（如果启用库存跟踪）
└── 溯源系统：Food 是追溯的终端产品
```

### 2.3 为什么不是 Product？

虽然系统中有 `product` 表，但在订单系统中：
- order_items 使用的是 food_id，不是 product_id
- food 表有更丰富的餐饮行业属性（cooking_time、is_spicy 等）
- product 表更像是通用的商品主数据

**建议**：在餐饮业务上下文中，统一使用 `Food` 作为销售对象的术语。

## 3. Food 与 Material 的关系

### 3.1 当前关系模型

```
Food (销售对象)
    │
    │ dish_recipe (配方/BOM)
    │ ├── material_id
    │ ├── material_name
    │ ├── quantity
    │ └── unit
    │
    ▼
Material (库存对象)
```

### 3.2 关键设计决策

**Product/Material 之间是不是通过 Recipe 形成关系，而不是继承关系？**

**答案：是的，通过 Recipe 形成关系，而不是继承**

原因：
1. 同一个"可乐"在不同场景有不同身份：
   - POS 销售时：Food（可口可乐，售价 3 元）
   - 库存管理时：Material（可口可乐，库存 100 瓶）
   - Recipe 中：Ingredient（可口可乐，用量 1 瓶）

2. 这种设计是合理的，因为：
   - Food 和 Material 的属性集不同
   - 一个 Food 可能由多个 Material 组成（如汉堡 = 面包 + 肉饼 + 生菜）
   - 一个 Material 可能用于多个 Food（如可口可乐用于多道菜）

## 4. 统一术语建议

### 4.1 术语规范

| 术语 | 含义 | 使用场景 | 对应表 |
|------|------|----------|--------|
| **Food** | 可销售的菜品/食品 | POS、外卖、菜单 | foods |
| **Material** | 可库存的原料/物料 | 仓储、采购、库存 | material_trace_code, inventory |
| **Recipe** | 菜品的配方/BOM | 成本核算、库存扣减 | dish_recipe |
| **Ingredient** | 配方中的原料项 | Recipe 定义 | dish_recipe (每行) |

### 4.2 同一实体的不同视角

```
"可口可乐 330ml" 这个实体：

POS 视角 (Food):
├── food_id: FD001
├── food_name: 可口可乐
├── sale_price: 300 (分)
└── image_url: /images/cola.jpg

库存视角 (Material):
├── material_id: MAT001
├── material_name: 可口可乐 330ml
├── current_stock: 100
└── cost_price: 1.50

Recipe 视角 (Ingredient):
├── food_id: FD001 (关联 Food)
├── material_id: MAT001 (关联 Material)
├── quantity: 1
└── unit: 瓶
```

## 5. 设计建议

### 5.1 统一销售对象模型

```typescript
// 统一销售对象接口
interface SellableItem {
  id: string;           // food_id
  name: string;         // food_name
  type: 'food' | 'combo';
  price: number;        // sale_price
  image?: string;       // image_url
  description?: string;
}

// 订单明细
interface OrderItem {
  orderId: string;
  item: SellableItem;
  quantity: number;
  amount: number;
  kitchenStatus: KitchenStatus;
}
```

### 5.2 Food 的核心属性

```typescript
interface Food {
  // 标识
  foodId: string;
  foodCode: string;
  foodName: string;
  
  // 分类
  categoryId: number;
  categoryName?: string;
  
  // 价格
  salePrice: number;      // 售价（分）
  costPrice: number;      // 成本价（分）
  
  // 库存
  stock: number;          // 当前库存
  minStock: number;       // 最低库存预警
  
  // 展示
  imageUrl?: string;
  description?: string;
  specification?: string;
  
  // 属性
  cookingTime?: number;   // 制作时间（分钟）
  isRecommend: boolean;   // 是否推荐
  isSpicy: boolean;       // 是否辣
  
  // 状态
  status: 1 | 2 | 3;     // 1在售 2停售 3售罄
}
```

## 6. 数据流分析

### 6.1 POS 销售流程

```
1. 顾客点餐
   └── POS 选择 Food (food_id)
   
2. 创建订单
   └── order_items 插入 food_id, quantity, unit_price
   
3. 发送到后厨
   └── kitchen_order 创建，status = 'pending'
   
4. 后厨制作
   └── 根据 dish_recipe 查询需要的 Material
   └── 扣减 Material 库存
   └── kitchen_order.status = 'making'
   
5. 完成上菜
   └── kitchen_order.status = 'served'
   
6. 结账
   └── orders.payment_status = 2
```

### 6.2 库存扣减流程

```
kitchen_order.status = 'making'
    │
    ├── 1. 查询每个 food_id 对应的 dish_recipe
    │
    ├── 2. 计算实际用量 (quantity * 需求量)
    │
    ├── 3. 扣减 material 库存
    │   └── deductByRecipe / deductByTraceCode / deductByBatchCode
    │
    └── 4. 记录 material_consumption
```

## 7. 总结

| 问题 | 答案 |
|------|------|
| POS 销售的统一对象是什么？ | **Food**（菜品/食品） |
| Food 在库存中的对应是什么？ | **Material**（原料/物料） |
| Food 与 Material 的关系？ | 通过 **Recipe** 形成关系，不是继承 |
| 术语应该统一为？ | Food（销售）、Material（库存）、Recipe（配方） |

---

*分析时间：2026-09-09*
*基于系统当前实现*
