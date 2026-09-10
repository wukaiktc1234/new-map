# Decision Baseline Recovery: 纯工程执行项 (ENGINEERING-READY)

> **生成时间**: 2026-09-09
> **验证状态**: 仅包含已有明确方案、可直接工程化的项
> **排除项**: 需要产品决策或架构变更的项

---

## 概述

本文档列出所有已经过验证、具有明确实现方案、可直接进入工程执行阶段的决策项。每个项包含完整的工程化信息，包括决策内容、推荐方案、代码一致性状态、执行步骤、风险评估和依赖关系。

---

## ENGINEERING_READY 决策项

### DEC-002: Unit 管理 (A_enum - 保持枚举)

#### 决策内容
系统内所有计量单位（Unit）采用枚举类型管理，而非自由文本输入。确保数据一致性和查询效率。

#### 推荐方案
1. **数据库层**: 在 `ingredients`、`material_archives`、`foods` 等表中使用 `VARCHAR(20)` 存储单位代码
2. **代码层**: 定义 `UnitEnum` 枚举类，包含常用单位（kg、g、ml、L、件、个等）
3. **校验层**: 在 Service 层添加单位校验逻辑，确保输入值在枚举范围内
4. **前端层**: 使用下拉选择框，禁止自由输入

#### 与代码一致性
- **已实现**: 
  - `ingredients.unit` 字段使用 VARCHAR(20) 存储
  - `material_archives.unit` 字段使用 VARCHAR(20) 存储
  - `foods.unit` 字段使用 VARCHAR(20) 存储
- **待完善**: 
  - 缺少统一的 `UnitEnum` 枚举定义
  - 部分表单位字段未设置 CHECK 约束
  - 前端未完全实现下拉选择

#### 执行步骤
1. **Step 1**: 创建 `UnitEnum` 枚举类
   ```java
   public enum UnitEnum {
       KG("kg"), G("g"), ML("ml"), L("L"), 
       PIECE("件"), PIECE2("个"), BOX("箱"), BAG("袋")
   }
   ```
2. **Step 2**: 为现有表添加 CHECK 约束
   ```sql
   ALTER TABLE ingredients ADD CONSTRAINT chk_unit 
   CHECK (unit IN ('kg', 'g', 'ml', 'L', '件', '个', '箱', '袋'));
   ```
3. **Step 3**: 更新 Service 层校验逻辑
4. **Step 4**: 更新前端组件为下拉选择
5. **Step 5**: 数据迁移（如有非标准单位值）

#### 风险评估
- **低风险**: 单位枚举相对稳定，变更频率低
- **数据迁移风险**: 历史数据中可能存在非标准单位值，需清洗
- **前端影响**: 需更新所有涉及单位输入的表单组件

#### 依赖关系
- **前置依赖**: 无
- **并行依赖**: DEC-005 (Price 管理)
- **后续依赖**: 所有涉及物料、菜品、库存的模块

---

### DEC-003: PaymentMethod 管理 (A_enum - 保持枚举)

#### 决策内容
系统内所有支付方式（PaymentMethod）采用枚举类型管理，确保支付逻辑的一致性和可维护性。

#### 推荐方案
1. **数据库层**: 使用 `INTEGER` 或 `VARCHAR(20)` 存储支付方式代码
2. **代码层**: 定义 `PaymentMethodEnum` 枚举类
3. **统一编码**: 制定全系统统一的支付方式编码标准
4. **前端层**: 使用下拉选择框，禁止自由输入

#### 与代码一致性
- **已实现**: 
  - `orders.payment_method` 使用 INTEGER 存储
  - `payment.payment_method` 使用 VARCHAR(20) 存储
  - `recharge_record.payment_method` 使用 INTEGER 存储
- **不一致**: 
  - 部分表使用 INTEGER，部分使用 VARCHAR
  - 支付方式编码不统一（0-微信、1-支付宝 vs wechat、alipay）
  - 缺少统一的枚举定义

#### 执行步骤
1. **Step 1**: 创建 `PaymentMethodEnum` 枚举类
   ```java
   public enum PaymentMethodEnum {
       WECHAT(0, "wechat", "微信支付"),
       ALIPAY(1, "alipay", "支付宝"),
       CASH(2, "cash", "现金"),
       BANK_CARD(3, "bank_card", "银行卡"),
       BALANCE(4, "balance", "余额支付")
   }
   ```
2. **Step 2**: 统一数据库字段类型（推荐 VARCHAR(20)）
3. **Step 3**: 执行数据迁移，统一编码格式
4. **Step 4**: 更新所有支付相关 Service 的校验逻辑
5. **Step 5**: 更新前端支付方式选择组件

#### 风险评估
- **中风险**: 支付方式变更可能影响支付流程
- **数据迁移风险**: 需要统一 INTEGER 和 VARCHAR 编码
- **兼容性风险**: 需确保新旧编码格式的兼容

#### 依赖关系
- **前置依赖**: 无
- **并行依赖**: DEC-002 (Unit 管理)
- **后续依赖**: 支付模块、订单模块、财务模块

---

### DEC-005: Price 管理 (A_inline - 价格内联)

#### 决策内容
价格信息直接内联存储在业务表中，而非通过独立的价格表管理。适用于价格相对稳定的场景。

#### 推荐方案
1. **数据库层**: 在 `foods`、`material_archives` 等表中直接存储价格字段
2. **字段设计**: 
   - `sale_price`: 销售价格（INTEGER，单位：分）
   - `cost_price`: 成本价格（INTEGER，单位：分）
   - `reference_price`: 参考价格（INTEGER，单位：分）
3. **代码层**: 在实体类中直接定义价格字段
4. **校验层**: 添加价格合理性校验（非负、范围检查）

#### 与代码一致性
- **已实现**: 
  - `foods.sale_price`、`foods.cost_price` 使用 INTEGER 存储（单位：分）
  - `material_archives.reference_price` 使用 INTEGER 存储（单位：分）
  - `ingredients.cost_price` 使用 INTEGER 存储（单位：分）
- **待完善**: 
  - 部分旧表仍使用 DECIMAL 存储（单位：元）
  - 缺少统一的价格校验工具类
  - 价格变更历史未完整记录

#### 执行步骤
1. **Step 1**: 创建价格工具类
   ```java
   public class PriceUtil {
       public static void validatePrice(Integer price) {
           if (price == null || price < 0) {
               throw new BusinessException("价格不能为负数");
           }
       }
   }
   ```
2. **Step 2**: 为新表添加价格字段约束
   ```sql
   ALTER TABLE foods ADD CONSTRAINT chk_sale_price CHECK (sale_price >= 0);
   ```
3. **Step 3**: 迁移旧表价格字段（DECIMAL -> INTEGER）
4. **Step 4**: 更新所有价格计算逻辑，确保使用整数运算
5. **Step 5**: 添加价格变更审计日志

#### 风险评估
- **低风险**: 价格内联模式简单直接，维护成本低
- **数据迁移风险**: 旧表价格迁移需注意精度转换（元 -> 分）
- **性能影响**: 价格查询无需 JOIN，性能较好

#### 依赖关系
- **前置依赖**: 无
- **并行依赖**: DEC-002 (Unit 管理)
- **后续依赖**: 订单模块、财务模块、报表模块

---

### DEC-007: Order State Machine (B_完整状态机)

#### 决策内容
订单状态流转采用完整的状态机模式，明确定义状态转换规则，确保业务流程的正确性。

#### 推荐方案
1. **状态定义**: 定义完整的订单状态枚举
2. **状态转换表**: 使用 Map 或配置文件定义合法的状态转换
3. **状态机工具**: 使用 `StateMachineUtil` 工具类验证状态转换
4. **事件驱动**: 状态变更触发相应事件（WebSocket 推送、异步任务）

#### 与代码一致性
- **已实现**: 
  - `StateMachineUtil` 工具类已存在
  - `RecruitmentRequirementStateMachine`、`RecruitmentQuotaStateMachine`、`JobOfferStateMachine` 已实现
  - `FinanceVoucherStateMachine` 已实现
- **待完善**: 
  - 缺少订单状态机 (`OrderStateMachine`)
  - 缺少采购订单状态机 (`PurchaseOrderStateMachine`)
  - 缺少库存状态机 (`InventoryStateMachine`)

#### 执行步骤
1. **Step 1**: 定义订单状态枚举
   ```java
   public enum OrderStatusEnum {
       PENDING(0, "待支付"),
       PAID(1, "已支付"),
       PREPARING(2, "准备中"),
       READY(3, "待取餐"),
       COMPLETED(4, "已完成"),
       CANCELLED(5, "已取消"),
       REFUNDING(6, "退款中"),
       REFUNDED(7, "已退款")
   }
   ```
2. **Step 2**: 创建 `OrderStateMachine` 类
   ```java
   public class OrderStateMachine {
       private static final Map<OrderStatusEnum, Set<OrderStatusEnum>> TRANSITIONS = Map.of(
           OrderStatusEnum.PENDING, Set.of(OrderStatusEnum.PAID, OrderStatusEnum.CANCELLED),
           OrderStatusEnum.PAID, Set.of(OrderStatusEnum.PREPARING, OrderStatusEnum.REFUNDING),
           // ... 其他转换规则
       );
   }
   ```
3. **Step 3**: 在 Service 层集成状态机验证
4. **Step 4**: 添加状态变更事件发布
5. **Step 5**: 编写状态机单元测试

#### 风险评估
- **中风险**: 状态机逻辑复杂，需全面测试
- **业务影响**: 状态转换错误可能导致业务流程中断
- **性能影响**: 状态验证增加少量计算开销

#### 依赖关系
- **前置依赖**: 无
- **并行依赖**: 无
- **后续依赖**: 订单模块、支付模块、通知模块

---

### DEC-009: Data Ownership (B_业务域所有)

#### 决策内容
数据所有权遵循业务域划分原则，每个业务域负责维护其核心数据的完整性和一致性。

#### 推荐方案
1. **域划分**: 明确各业务域边界
   - 订单域: 订单、订单项、支付记录
   - 库存域: 库存、库位、库存变动
   - 采购域: 采购单、供应商、采购结算
   - 财务域: 财务记录、凭证、报表
2. **数据归属**: 每个数据表明确归属业务域
3. **访问控制**: 跨域数据访问通过 API 或事件机制
4. **数据同步**: 使用 Event Outbox 模式实现跨域数据同步

#### 与代码一致性
- **已实现**: 
  - 各业务域有独立的 Service 和 Mapper
  - 使用 Event Outbox 进行跨域数据同步
  - 数据权限控制（`DataPermissionService`）
- **待完善**: 
  - 部分表缺乏明确的域归属标记
  - 跨域查询逻辑分散在多个 Service 中
  - 数据同步机制不够完善

#### 执行步骤
1. **Step 1**: 维护数据归属矩阵
   ```yaml
   order_domain:
     tables: [orders, order_items, order_payments]
     owner: OrderService
   inventory_domain:
     tables: [inventory, inventory_locations, inventory_transactions]
     owner: InventoryService
   ```
2. **Step 2**: 为数据表添加域标记字段（可选）
3. **Step 3**: 规范化跨域数据访问接口
4. **Step 4**: 完善 Event Outbox 事件定义
5. **Step 5**: 编写域边界检查测试

#### 风险评估
- **低风险**: 数据归属划分主要是架构层面的规范
- **重构风险**: 跨域逻辑重构可能影响现有功能
- **一致性风险**: 数据同步机制需确保最终一致性

#### 依赖关系
- **前置依赖**: 无
- **并行依赖**: 无
- **后续依赖**: 所有业务模块

---

### DEC-012: Inventory Location (B_多级位置)

#### 决策内容
库存位置采用多级结构管理，支持仓库-库位-货架等多级位置体系。

#### 推荐方案
1. **表结构设计**: 
   - `warehouses`: 仓库表
   - `inventory_locations`: 库位表（关联仓库）
   - `inventory_shelves`: 货架表（关联库位，可选）
2. **位置编码**: 采用层级编码格式（如 `WH01-LOC01-SH01`）
3. **容量管理**: 支持最大容量、当前数量等容量属性
4. **状态管理**: 支持启用/停用状态

#### 与代码一致性
- **已实现**: 
  - `inventory_locations` 表已创建
  - `InventoryLocation` 实体类已定义
  - `InventoryLocationService` 服务已实现
  - `InventoryLocationController` 控制器已实现
- **待完善**: 
  - 缺少仓库表（`warehouses`）
  - 缺少货架表（`inventory_shelves`）
  - 位置编码规则未统一

#### 执行步骤
1. **Step 1**: 创建 `warehouses` 表
   ```sql
   CREATE TABLE warehouses (
       id BIGSERIAL PRIMARY KEY,
       warehouse_code VARCHAR(20) UNIQUE NOT NULL,
       warehouse_name VARCHAR(100) NOT NULL,
       address VARCHAR(200),
       status INTEGER DEFAULT 1,
       create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       deleted INTEGER DEFAULT 0
   );
   ```
2. **Step 2**: 更新 `inventory_locations` 表，添加仓库关联
3. **Step 3**: 定义位置编码生成规则
4. **Step 4**: 更新库存查询逻辑，支持多级位置
5. **Step 5**: 更新前端位置选择组件

#### 风险评估
- **低风险**: 多级位置是常见的库存管理模式
- **数据迁移风险**: 需要为现有库位数据分配仓库
- **前端影响**: 位置选择组件需支持多级联动

#### 依赖关系
- **前置依赖**: 无
- **并行依赖**: 无
- **后续依赖**: 库存模块、采购模块、生产模块

---

### DEC-013: Authentication Strategy (B_JWT)

#### 决策内容
系统认证采用 JWT（JSON Web Token）策略，支持无状态认证、令牌刷新、多设备登录等功能。

#### 推荐方案
1. **令牌类型**: 
   - Access Token: 短期访问令牌（默认 8 小时）
   - Refresh Token: 长期刷新令牌（默认 7 天）
2. **令牌存储**: 客户端存储在 localStorage 或内存中
3. **令牌验证**: API 网关统一验证 JWT 签名和有效期
4. **密钥管理**: 使用 HS512 算法，密钥通过环境变量注入

#### 与代码一致性
- **已实现**: 
  - JWT 配置已定义（`application.yml`）
  - `JwtUtil` 工具类已实现
  - JWT 验证中间件已实现
  - 登录/刷新 API 已实现
- **待完善**: 
  - 密钥轮换机制未实现
  - 多设备登录管理未完善
  - 令牌黑名单机制未实现

#### 执行步骤
1. **Step 1**: 完善 JWT 配置
   ```yaml
   jwt:
     secret: ${JWT_SECRET}
     access-token-expiration: 28800000  # 8小时
     refresh-token-expiration: 604800000  # 7天
     refresh-window: 3600000  # 1小时
   ```
2. **Step 2**: 实现密钥轮换机制
3. **Step 3**: 实现多设备登录管理
4. **Step 4**: 实现令牌黑名单（可选）
5. **Step 5**: 编写安全测试用例

#### 风险评估
- **中风险**: 认证是系统安全的核心
- **密钥泄露风险**: 需确保密钥安全存储
- **令牌劫持风险**: 需实施 HTTPS 和令牌绑定

#### 依赖关系
- **前置依赖**: 无
- **并行依赖**: DEC-014 (Authorization Model)
- **后续依赖**: 所有需要认证的 API

---

### DEC-014: Authorization Model (A_RBAC)

#### 决策内容
系统授权采用 RBAC（Role-Based Access Control）模型，基于角色进行权限控制。

#### 推荐方案
1. **权限模型**: 
   - 用户（User） -> 角色（Role） -> 权限（Permission）
   - 支持用户直接分配权限（可选）
2. **权限粒度**: 
   - 菜单权限: 控制前端菜单显示
   - 按钮权限: 控制前端按钮显示
   - API 权限: 控制后端接口访问
   - 数据权限: 控制数据访问范围
3. **权限校验**: 
   - 前端: 路由守卫 + 指令（v-permission）
   - 后端: 注解（@RequiresPermission） + 拦截器

#### 与代码一致性
- **已实现**: 
  - `roles`、`permissions`、`role_permissions`、`user_roles` 表已创建
  - `PermissionVerifyService` 服务已实现
  - `PermissionAspect` 切面已实现
  - `PermissionInterceptor` 拦截器已实现
  - `FieldPermissionService` 字段级权限已实现
- **待完善**: 
  - 数据权限范围控制未完善
  - 权限缓存机制未实现
  - 权限变更实时生效机制未完善

#### 执行步骤
1. **Step 1**: 完善数据权限模型
   ```java
   public enum DataScopeEnum {
       ALL(1, "全部数据"),
       DEPARTMENT(2, "本部门数据"),
       DEPARTMENT_AND_CHILD(3, "本部门及子部门数据"),
       SELF(4, "仅本人数据"),
       CUSTOM(5, "自定义数据")
   }
   ```
2. **Step 2**: 实现权限缓存（Redis）
3. **Step 3**: 实现权限变更事件通知
4. **Step 4**: 完善数据权限 SQL 拦截器
5. **Step 5**: 编写权限测试用例

#### 风险评估
- **高风险**: 权限控制是系统安全的核心
- **性能影响**: 权限校验可能影响接口性能
- **缓存一致性**: 权限变更后缓存需及时更新

#### 依赖关系
- **前置依赖**: DEC-013 (Authentication Strategy)
- **并行依赖**: 无
- **后续依赖**: 所有需要权限控制的模块

---

## 执行优先级

### 高优先级（立即执行）
1. **DEC-002**: Unit 管理 - 基础数据规范
2. **DEC-003**: PaymentMethod 管理 - 支付基础
3. **DEC-005**: Price 管理 - 价格基础

### 中优先级（1-2 周内执行）
4. **DEC-007**: Order State Machine - 订单核心逻辑
5. **DEC-013**: Authentication Strategy - 安全基础
6. **DEC-014**: Authorization Model - 权限基础

### 低优先级（2-4 周内执行）
7. **DEC-009**: Data Ownership - 架构规范
8. **DEC-012**: Inventory Location - 库存扩展

---

## 相关文档

- [已验证锁定决策](./verified-locked-decisions.md)
- [开放架构决策](./open-architecture-decisions.md)
- [开放产品决策](./open-product-decisions.md)
- [决策溯源矩阵](./decision-provenance-matrix.md)

---

## 变更记录

| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|--------|
| 1.0 | 2026-09-09 | 初始版本 | System |
