# N11 扫描卡：`frontend-pos` / `frontend-kitchen` 的 storeId 端点下游清单

> **卡号**：P1-POS-KITCHEN-STOREID-DOWNSTREAM-SCAN-001（来源 = [field-entry-inventory-001.md](field-entry-inventory-001.md) §6.2 **N11**）
> **判档 = 轻档**（Owner 2026-09-30）：只读扫描 + 落盘清单；不写实施记录、不更 INDEX/roadmap、不改代码、不修任何发现。
> **目的**：给「后端做 store→location 收编」提供一份**已登记的下游客户端清单**——收编时哪些客户端调用会断、哪些不会断但语义会错、哪些其实早就断了。
> **独立性**：本卡与 P1-USER-LOCATION-002 并行，**不是 002 的前置也不是 002 的范围**；002 不读本卡结论即可继续。

---

## 0. 口径与传输链事实（先固定，否则行号会误读）

| 事实 | 证据 |
|---|---|
| 两客户端 axios `baseURL: '/api'` | [frontend-pos/src/api/request.ts:5](../../frontend-pos/src/api/request.ts#L5)、[frontend-kitchen/src/api/request.ts:16](../../frontend-kitchen/src/api/request.ts#L16) |
| 后端 `server.servlet.context-path: /api` | [application-dev.yml:70](../../backend/src/main/resources/application-dev.yml#L70)、[application-pg.yml:72](../../backend/src/main/resources/application-pg.yml#L72)、application-prod.yml:107 |
| ⇒ 客户端里写的 `/v1/...` 实际打到 `/api/v1/...`；本文件所有后端 handler 行号按 **context-path 相对路径**登记 | — |
| vite 代理不改写路径（只转发 `/api` → `:8081`） | [frontend-pos/vite.config.ts:19-23](../../frontend-pos/vite.config.ts#L19)、[frontend-kitchen/vite.config.ts:19-23](../../frontend-kitchen/vite.config.ts#L19) |
| ID 空间口径沿用 [field-entry-inventory-001.md §0](field-entry-inventory-001.md)（LOC / STORE / WH / CODE） | — |
| 扫描范围 = 仅 `frontend-pos/src` + `frontend-kitchen/src`（不含 `dist/`、`node_modules/`、`security-test/`） | — |

**身份链事实（决定「会不会断」的前提）**

| 事实 | 证据 |
|---|---|
| POS 登录发 JWT，**claim 含 `locationId`** | [PosAuthController.java:39](../../backend/src/main/java/com/foodtraceability/controller/pos/PosAuthController.java#L39) → [PosAuthServiceImpl.java:113](../../backend/src/main/java/com/foodtraceability/service/impl/PosAuthServiceImpl.java#L113) → [JwtUtils.java:162](../../backend/src/main/java/com/foodtraceability/security/utils/JwtUtils.java#L162)（写入）/ :375-379（解析） |
| POS 下单的 storeId **不来自客户端**，来自 JWT 经桥反查 | [PosOrderCreateServiceImpl.java:311-317](../../backend/src/main/java/com/foodtraceability/service/pos/impl/PosOrderCreateServiceImpl.java#L311)：请求无 storeId → `SecurityUtils.getCurrentUserStoreId()` → 仍 null 则抛 `NoLocationContextException`（`"1"` 兜底已在 -001 #11 删除） |
| POS 客户端下单载荷**没有 storeId 字段** | [posApi.ts:214-220](../../frontend-pos/src/api/posApi.ts#L214) `OrderRequest`；[Payment.vue:649-662](../../frontend-pos/src/views/Payment.vue#L649) 组装的 orderData |
| `frontend-kitchen` **没有登录**（4 个视图，无 login 调用） | `frontend-kitchen/src/views/` = Home / Scan / ServeWindow / StationPrint；全库 grep `login` 0 命中 |
| ⇒ kitchen 的调用落在**公开路径白名单**内，无身份、无 store 上下文 | [JwtAuthenticationFilter.java:45-46](../../backend/src/main/java/com/foodtraceability/security/filter/JwtAuthenticationFilter.java#L45)（`/v1/kitchen/`、`/v1/kitchen-order/`）；[SecurityConfig.java:107-108](../../backend/src/main/java/com/foodtraceability/security/config/SecurityConfig.java#L107) |

---

## 1. 表 A：**会断**的下游（依赖 store 空间 / 身份桥，收编动作直接触及）

| # | 客户端调用 file:line | 端点 | 后端 handler file:line | storeId 从哪来 | 收编时怎么断 | 严重度 |
|---|---|---|---|---|---|---|
| A1 | [posApi.ts:423](../../frontend-pos/src/api/posApi.ts#L423)（调用点 [Payment.vue:662](../../frontend-pos/src/views/Payment.vue#L662)） | `POST /v1/pos/orders/order` | [PosOrderController.java:63](../../backend/src/main/java/com/foodtraceability/controller/pos/PosOrderController.java#L63) → [PosOrderCreateServiceImpl.java:311-317](../../backend/src/main/java/com/foodtraceability/service/pos/impl/PosOrderCreateServiceImpl.java#L311) | JWT `locationId` 经 `LocationIdBridge` 反查 `stores_new.store_id` | 用户 `users.location_id` 未分配 → `NoLocationContextException`，**POS 下单直接失败**（无兜底） | **高（主链路）** |
| A2 | [posApi.ts:428](../../frontend-pos/src/api/posApi.ts#L428)（3 个调用点） | `POST /v1/pos/orders/order/pay` | [PosOrderController.java:124](../../backend/src/main/java/com/foodtraceability/controller/pos/PosOrderController.java#L124) | 沿用 A1 落库的订单归属 | 随 A1 连锁（A1 断则无单可付） | 高（从属） |
| A3 | [CustomerOrder.vue:437](../../frontend-pos/src/views/CustomerOrder.vue#L437) | `POST /v1/pos/orders/order/table` | [PosOrderController.java:111](../../backend/src/main/java/com/foodtraceability/controller/pos/PosOrderController.java#L111) | `TableOrderDTO` **无 storeId 字段** → 服务端推导 | 同 A1 链；扫码点餐主链路 | **高** |
| A4 | [api/kitchen.ts:60,63](../../frontend-pos/src/api/kitchen.ts#L60) | `GET /v1/kitchen/orders/list?storeId=` | [KitchenOrderController.java:71](../../backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java#L71)（`wrapper.eq(KitchenOrder::getStoreId, storeId)`，:79-80） | 客户端参数——**声明了但没有任何调用点传值** | 参数名/类型随 `kitchen_orders` 收编而变；当前形态是「声明了却恒不传」→ 恒全量查询，收编后仍恒全量但**看起来像已过滤** | 中 |
| A5 | [api/kitchen.ts:140-141](../../frontend-pos/src/api/kitchen.ts#L140) | `GET /v1/kitchen/orders/statistics/{storeId}` | [KitchenOrderController.java:402-410](../../backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java#L402)（`countByStatusAndStore`） | **无调用点**（死包装） | 改签名**没有回归信号**（无调用点 = 无测试覆盖） | 低（死代码） |
| A6 | [posApi.ts:559-563](../../frontend-pos/src/api/posApi.ts#L559) | `POST /v1/pos/yolo/recognize` body `{image, storeId}` | [YoloProductRecognitionController.java:43](../../backend/src/main/java/com/foodtraceability/controller/pos/YoloProductRecognitionController.java#L43) | 调用点 [YoloCheckoutDialog.vue:268,306](../../frontend-pos/src/components/pos/YoloCheckoutDialog.vue#L268) **只传 image** → `storeId` 恒 `undefined` | 若收编后按 location 选模型/库存口径，恒 null → 走默认分支 | 中 |
| A7 | [types/kitchen.ts:62](../../frontend-pos/src/types/kitchen.ts#L62)；[kitchenOrderApi.ts:13-14,46-47,65-66](../../frontend-kitchen/src/api/kitchenOrderApi.ts#L13) | 响应契约 `storeId` / `storeName` | `KitchenOrder` 实体出参 | 后端返回 | 出参字段改名/换空间 → 客户端类型与展示层同时断（与 inventory §6.2 **N8** 同源，客户端侧下游） | 中 |

---

## 2. 表 B：**不会断但语义错**的活端点（无门店作用域，收编后仍跨店可见）

| # | 客户端调用 file:line | 端点族 | 后端 handler file:line | 作用域现状 |
|---|---|---|---|---|
| B1 | [TableManagement.vue:392,393,442,445,476,495,518,560,589](../../frontend-pos/src/views/TableManagement.vue#L392) | `/v1/pos/tables/**`（查/建/改/删/占用/释放/转台/重生成二维码） | [DiningTableController.java:11-143](../../backend/src/main/java/com/foodtraceability/controller/pos/DiningTableController.java#L11) —— **整类无 `storeId` / `locationId` 出现** | 跨门店桌台可见可改 |
| B2 | [CallNumber.vue:390,403,487,503,527,592,643](../../frontend-pos/src/views/CallNumber.vue#L390) | `/v1/call-number/**` | [CallNumberController.java:35-135](../../backend/src/main/java/com/foodtraceability/controller/pos/CallNumberController.java#L35)（`getPendingOrders()` 无参无过滤） | 跨门店叫号。**对照**：管理端同一域走 [CallNumberQueueManagementController.java:56,81](../../backend/src/main/java/com/foodtraceability/controller/CallNumberQueueManagementController.java#L56)（`getCurrentStoreId()` → `listByStoreId` / `getQueueStatsByStoreId`）——**两套 API，POS 用的是无作用域那套** |
| B3 | [posApi.ts:460](../../frontend-pos/src/api/posApi.ts#L460)（6 个调用点） | `GET /v1/pos/orders` | [PosOrderController.java:42-48](../../backend/src/main/java/com/foodtraceability/controller/pos/PosOrderController.java#L42)（仅 `startDate/endDate/keyword`） | 跨门店订单列表 |
| B4 | [Home.vue:347,348](../../frontend-kitchen/src/views/Home.vue#L347) | `GET /v1/kitchen/scan/pending-orders` / `making-orders` | [KitchenScanController.java:156-168](../../backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java#L156)（仅按 status 过滤） | **无身份 + 无 store 过滤** → 全门店后厨订单可见 |
| B5 | [Home.vue:381,403,441](../../frontend-kitchen/src/views/Home.vue#L381)、[Scan.vue:148](../../frontend-kitchen/src/views/Scan.vue#L148) | `GET /v1/tray/idle`、`POST /v1/tray/bind-order`、`POST /v1/tray/scan-serve` | [TrayController.java:34,90,148](../../backend/src/main/java/com/foodtraceability/controller/TrayController.java#L34) | `bind-order` 内的 `KdsPrincipal` store 校验位于**未提交 WIP**：`TrayController.java` 状态 ` M`、`KdsTokenFilter.java` 状态 `??`（`git status` 实测）→ **已提交树无 store 校验**。与 **KL-085（冻结）** 同一根因，本行只登记客户端侧下游 |
| B6 | [Home.vue:477](../../frontend-kitchen/src/views/Home.vue#L477) | `POST /v1/kitchen/orders/{id}/receive?chefId=1&chefName=后厨` | [KitchenOrderController.java:208](../../backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java#L208) | 操作员身份**硬编码 `chefId=1`**；文件内注释自述已登记为 **KIT-DATA-003（只登记不修）** → 属已知项，本卡不重复登记 |

---

## 3. 表 C：**其实已经断**的调用（无 handler / 路径错 / 参数没发出）

> 这些与收编无关，**先于收编存在**。登记价值：收编时若有人「顺手验证 POS/KDS 是否正常」，会在这里得到假阴性/假阳性。

| # | 客户端调用 file:line | 客户端写的端点 | 后端实况 |
|---|---|---|---|
| C1 | [kitchenOrderApi.ts:58,68,80,84,88,92,96,100,104,125,129,133](../../frontend-kitchen/src/api/kitchenOrderApi.ts#L58) | `/v1/kitchen-order/**` 共 **12 个端点** | **后端无 handler**。全库 `kitchen-order` 只出现在 [SecurityConfig.java:107-108](../../backend/src/main/java/com/foodtraceability/security/config/SecurityConfig.java#L107) 与 [JwtAuthenticationFilter.java:46](../../backend/src/main/java/com/foodtraceability/security/filter/JwtAuthenticationFilter.java#L46)（白名单）→ **整条 API 层 404，且白名单为不存在的路径让路** |
| C2 | [kitchenOrderApi.ts:71-76](../../frontend-kitchen/src/api/kitchenOrderApi.ts#L71) | `/v1/kitchen/pending-orders`、`/v1/kitchen/making-orders` | 真实路径是 `/v1/kitchen/scan/*`（[KitchenScanController.java:156,163](../../backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java#L156)）。另：`request.get(url, storeId ? { storeId } : undefined)` 把 `{storeId}` 当 **axios config** 传（应为 `{ params: { storeId } }`）→ 即使路径正确，**storeId 也不会发出** |
| C3 | [kitchenOrderApi.ts:121](../../frontend-kitchen/src/api/kitchenOrderApi.ts#L121) | `/v1/kitchen/order-requirements/{id}` | 真实 `/v1/kitchen/scan/order-requirements/{id}`（[KitchenScanController.java:170](../../backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java#L170)） |
| C4 | [Scan.vue:169](../../frontend-kitchen/src/views/Scan.vue#L169) | `/v1/kitchen/scan-food-trace-code` | 真实 `/v1/kitchen/scan/scan-food-trace-code`（[KitchenScanController.java:78](../../backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java#L78)）；同客户端 [ServeWindow.vue:310](../../frontend-kitchen/src/views/ServeWindow.vue#L310) 用的是**正确**路径 → 同文件族内路径口径不一致 |
| C5 | [ServeWindow.vue:233,264](../../frontend-kitchen/src/views/ServeWindow.vue#L233) | `GET /v1/kitchen/completed-orders`、`POST /v1/kitchen/serve-order/{id}` | 无 handler（真实：`/v1/kitchen/orders/{id}/serve`，[KitchenOrderController.java:322](../../backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenOrderController.java#L322)） |
| C6 | [Scan.vue:151,153,155](../../frontend-pos/src/views/Scan.vue#L151) | `/v1/orders/verify`、`/v1/coupons/verify`、`/v1/members/identify` | 三个扫码分支全部无 handler。真实：`/v1/pos/coupons/verify`（[PosCouponController.java:49](../../backend/src/main/java/com/foodtraceability/controller/pos/PosCouponController.java#L49)）；`/v1/pos/members`（[MemberController.java:40-188](../../backend/src/main/java/com/foodtraceability/controller/pos/MemberController.java#L40)）**无 `/identify`** |
| C7 | [CallingDisplay.vue:162](../../frontend-pos/src/views/CallingDisplay.vue#L162) | `GET /api/v1/call-number/pending` | `baseURL` 已是 `/api` → 实际请求 `/api/api/v1/call-number/pending` → 404。同文件其余调用写 `/v1/...` 正常 |

---

## 4. 判定汇总

| 判定 | 条目 | 对「后端收编」的含义 |
|---|---|---|
| **收编会打断** | A1 / A2 / A3（身份桥主链路）、A4 / A6 / A7（参数与出参契约） | 收编前须先保证 `users.location_id` 已分配到位，否则 **POS 下单直接失败**（A1 无兜底）；A7 出参改名须与客户端类型同步改 |
| **收编不会打断，但收编后仍然错** | B1 / B2 / B3 / B4 / B5 | 这些端点**没有 store 作用域**，收编只换 ID 空间、不会补上隔离；多门店隔离在这两个客户端上**不成立**，须单独卡处理（不在本卡范围） |
| **与收编无关，已经断** | C1–C7 | 收编验收若跑 POS/KDS 冒烟，**不能**以这些调用作为「正常」证据 |
| **已知项，不重复登记** | B6（KIT-DATA-003）、B5（KL-085 冻结）、A1 的兜底删除（-001 #11） | 沿用既有登记，本卡只补客户端侧下游事实 |

**与既有登记的关系**：本卡是 [field-entry-inventory-001.md](field-entry-inventory-001.md) §5「范围外客户端登记表」的**展开**（那里只给了命中计数，这里给到端点级 + handler 级 + 断点判定）。§5 的「风险提示」段落由本卡接管，后续引用应指向本文件。

---

## 5. 复现方法（只读）

```bash
# 1) 客户端端点与字段抽取（脚本已留在 gitignored tmp/，不进版本库）
python3 tmp/scan001/n11.py            # → tmp/scan001/n11.json（pos 66 行 / kitchen 40 行）
python3 tmp/scan001/n11_backend2.py   # → tmp/scan001/n11_backend.json（后端 2054 个 mapping）
python3 tmp/scan001/n11_match.py      # → 82 个客户端端点交叉匹配，22 个无 handler

# 2) 关键事实单点复核
grep -n "context-path" backend/src/main/resources/application-dev.yml
grep -n "kitchen-order" backend/src/main/java -r --include=*.java
grep -n "locationId" backend/src/main/java/com/foodtraceability/security/utils/JwtUtils.java
git status --short backend/src/main/java/com/foodtraceability/controller/TrayController.java \
                   backend/src/main/java/com/foodtraceability/security/filter/KdsTokenFilter.java
```

## 6. 边界声明

- 全程只读：**未改任何代码**，未改 DB，未修表 C 的任何断链。
- 未扩到 `employee-frontend` / `frontend-design-demo` / `frontend-design-v3`（仍按 inventory §5 计数登记）。
- 未新开 KL/ENV 条目（轻档不更 INDEX/roadmap）；本文件即登记载体。
- 本卡结论**不进入** P1-USER-LOCATION-002 范围（Owner 2026-09-30 明确：不把 N11 塞进 002）。
