# P1-POS-CUSTOMER-PHONE-500-001 · 阶段2 设计（A1 专用字段·可还原）

> **判档**：重档（schema 变更）→ PG-005 三阶段。本文 = **阶段2 设计**，Owner 审后**才**进入阶段3 实现。
> **范围（Owner 锁定）**：只做存储层 —— 新增可还原加密字段 + `customer_phone` 存脱敏值；不碰展示层 UI；回拨能力另立"隐私分级"卡。

## 1. 诊断（已只读确认）

- **500 根因**：`PosOrderCreateServiceImpl:332` `orderNew.setCustomerPhone(sensitiveDataService.encryptPhone(...))` 把 AES 密文（`ENC_PHONE:<base64>`，~54 字符）写进 `orders.customer_phone` **`varchar(20)`** → `value too long` → 500。
- **两条写入点（都走 `OrderNew` 实体）**：
  - `PosOrderCreateServiceImpl:332`（POS）：存**密文** → 500（本卡目标）。
  - `OrderNewServiceImpl:1860`（`buildOrderEntity`）：存**明文** `createDTO.getCustomerPhone()`（不 500，但明文落库，且与 POS 不一致）。
- **读点**：`PosOrderQueryServiceImpl:81` `decryptPhone(customer_phone)`（**有 `ENC_PHONE:` 前缀才解密，无前缀原样透传**）；`KitchenOrderMapper:50/63/77`、`OrderNewServiceImpl:1989`、`SalesOrder` mapper 读 `customer_phone` raw。
- **现成能力**：`SensitiveDataServiceImpl` 已有 `encryptPhone` / `decryptPhone` / **`maskPhone`**（前3+`****`+后4，如 `138****8000`，11 字符 ≤ varchar(20)）。`maskPhone`/`encryptPhone` 对已加密/已脱敏输入均幂等安全。
- **加密实现**：`AESUtil` AES/CBC + 16B 随机 IV，`IV+密文` 再 Base64；11 位手机号 → **~54 字符**（含 `ENC_PHONE:` 前缀）。

## 2. 设计（改动清单）

**目标**：`customer_phone` 存脱敏值（varchar(20) 放得下，不再 500）；`customer_phone_encrypted` 存可还原密文（供未来"回拨"卡）；**读路径零改动**（`decryptPhone` 对脱敏值透传）。

| # | 改动 | 位置 |
|---|------|------|
| 1 | 新列 `orders.customer_phone_encrypted VARCHAR(255)`（实际 ~54 字符，留足余量杜绝"太长"） | 见 #2 |
| 2 | Flyway `V20260930_001__add_orders_customer_phone_encrypted.sql`：UP `ADD COLUMN IF NOT EXISTS …`；DOWN `DROP COLUMN IF EXISTS …`（PG-003 只增不改；版本号高于现有最高 `V20260927_001`，不撞号） | `backend/src/main/resources/db/migration/` |
| 3 | 实体加字段 `@TableField("customer_phone_encrypted") String customerPhoneEncrypted` + getter/setter（MyBatis-Plus 自动映射，insert 时落新列） | `entity/OrderNew.java` |
| 4 | POS 写入点：`setCustomerPhone(maskPhone(phone))` + `setCustomerPhoneEncrypted(encryptPhone(phone))` | `PosOrderCreateServiceImpl:332` |
| 5 | 一致写入点：同样拆两列（消除明文落库 + 与 POS 一致） | `OrderNewServiceImpl:1860` |
| 6 | **读路径：零改动**（`decryptPhone` 透传脱敏值 / raw 读点直接拿到脱敏值） | — |

**脱敏规则**（复用现成 `maskPhone`）：明文 → 前3+`****`+后4（`13800138000`→`138****8000`）；长度 <7 原样；空/null 原样。

## 3. 回滚（干净，无不可逆数据损失）

- 本卡**不回填、不覆盖存量** `customer_phone`（只改新单写入路径）→ 存量订单维持原样。
- 代码 revert（#3–#5）+ schema DOWN `DROP COLUMN IF EXISTS customer_phone_encrypted`。

## 4. 明确不做（Owner 圈定）

- 不重做展示层 UI（小票/后厨/配送代码不动）。
- 不实现"授权回拨 + 审计"（另立"隐私分级"卡；届时用 `decryptPhone(customer_phone_encrypted)` 还原全号）。
- 不回填存量数据。

## 5. 测试

- **单测**：`maskPhone("13800138000")`=`"138****8000"`；写入路径断言 `customer_phone`=脱敏、`customer_phone_encrypted`=`ENC_PHONE:...`；`decryptPhone(customer_phone_encrypted)`=原明文（**可还原**）；`decryptPhone(customer_phone)`=脱敏值（读路径透传不破）。
- **E2E**：POS `POST` 带 `contactPhone` → **200（不再 500）**，落库 `customer_phone` 脱敏 + `customer_phone_encrypted` 有值。

## 6. 待 Owner 拍板（1 个决策点）

**展示层显示语义** —— "customer_phone 存脱敏值" 与 "脱敏展示是未来卡" 存在张力，需你定：

- **A（推荐，纯存储层）**：`customer_phone`=脱敏，读路径零改 → 小票/后厨**自动显示脱敏** `138****8000`（基础脱敏展示随本卡落地，属隐私增强）；"授权回拨+审计"仍留未来卡。**改动面最小，最贴合"只做存储层 / 不碰展示层代码"**。
- **B（维持全号显示）**：读路径改为从 `customer_phone_encrypted` 解密显示全号 → 小票/后厨**维持全号**，"脱敏展示"留未来卡；**代价**：需改 `PosOrderQueryServiceImpl:81` + `Kitchen`/`VO`/`SalesOrder` 读点，**超出"只做存储层"**、动到展示相关读代码。

> 建议 **A**：最省、最合 scope，且"脱敏展示"本就是"隐私分级"卡的方向，以基础版提前落地无副作用；未来卡聚焦"授权回拨 + 审计"即可。
