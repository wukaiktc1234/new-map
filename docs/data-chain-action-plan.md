# 数据流转打通：可执行修复 + 验证清单

> 编制时间：2026-07-31
> 依据：对 `frontend/src`、`backend/src`、`docs/` 的全量静态核查（已复核关键结论），配合 `data-chain-fix-tracking.md`（L 级修复源）与 `data-flow-map.md`（L1 链路图）使用。
> **契约权威：前端展示需求为准**——前端展示/使用的字段，后端或数据库缺失即视为待修复项，方向是后端补齐，而非删减前端。
> **目标业务模式：集中式单店**（单仓单店，流程从简，优先打通钱货闭环）。
> 验证方式：先按本清单完成静态修复，再按「第七章 端到端验证」实跑后端+前端逐项验证。

---

## 一、各链打通状态总览（静态核查结论）

| 链路/模块 | 状态 | 核心问题 | 关联编号 |
|---|---|---|---|
| A. 采购链（申请→计划→订单→收货→入库→结算→应付→付款回写） | ✅ 打通 | 业务链衔接已补齐：申请/计划→订单跳转定位、计划→订单接口、requestNo 过滤（2026-08-01 批 3） | F5 / 批3 |
| B. 库存/成本链（入库→库存→流水→成本→汇总→订单扣减→成本结转） | ✅ 打通 | cost_record 无订单关联；门店扣减不写流水 | F7 / P2-1 |
| C. 订单/销售链（预约→下单→桌台→支付→退款→日结） | ✅ 打通 | 预约转单无 API；支付不落资金流水；退款无真实支付渠道 | A4 / F6 |
| D. 资金链（收入→应收→收款→应付→付款→流水→凭证→总账） | ✅ 打通 | F1-F6 已全部修复（2026-07-31 批 1） | F1-F4 |
| 追溯链 | ⚠️ 部分 | #1 ID 类型、#12 JSON 未细化、#3 召回状态、PUT body/query 不匹配 | T1-T3 / R1-R2 |
| 会员链 | ⚠️ 部分 | #2 金额单位、分页断裂、消费记录无端点、退款不扣余额 | M1-M6 / A1-A2 |
| 产品/BOM 链 | ✅ 打通 | 菜品成本为保存时快照，不自动联动库存成本 | P1 |
| 门店运营链 | ⚠️ 部分 | 日结 confirmTime/退款/状态三处展示断裂；叫号/证件续期/招聘缺后端 | D1-D2 / A5-A7 |
| 系统权限链 | ⚠️ 部分 | 权限中心操作日志假数据；前端不消费后端菜单接口 | O1-O3 |
| 人事/排班/薪资链 | ⚠️ 部分 | 排班前后端脱节；薪资缺接口+结构错配；3 个 100% Mock 页面 | HR-1~HR-5 |
| 资产链 | ⚠️ 部分 | 资产调拨无后端；CANCELLED 无后端语义 | A3 / C1 |
| 设备链 | ✅ 打通 | 历史 storeId 硬编码 1L；趋势/维护记录 TODO | D3 |
| 供应商门户/电子签章 | ✅ 打通 | 短信网关未接真实 | E1 |
| 运营中心 | ✅ 打通 | 个别字段空返回/塞 description hack | O4 |

**备注**：`data-flow-map.md`（L1）中标记的断点大部分已被 `data-chain-fix-tracking.md` 修复（见第五章对齐表），仅追溯/会员/日结相关断点仍真实存在。

---

## 二、P0 修复项（数据正确性断裂，直接导致显示错/空/失真）

### F1 应收账款"已核销"状态码冲突（资金链）
- **现象**：同一核销动作，`ReceivableServiceImpl.confirmPayment` 置 `status=3`（坏账/核销），`ReceiptServiceImpl.registerReceipt` 置 `status=4`（已核销）。前端 converters 映射 `3=settled / 4=overdue`，经收款单核销的应收在前端显示为「逾期」。
- **证据**：`backend/.../service/finance/impl/ReceivableServiceImpl.java:139,152`；`.../finance/impl/ReceiptServiceImpl.java:124`；`frontend/src/api/finance/converters.ts:148-161`
- **修复**：统一应收状态编码（建议 `1正常/2逾期/3已核销`），`ReceiptServiceImpl` 改为置 3；同步核对前端映射。
- **验证**：创建应收→建收款单核销→应收列表该条应显示「已核销」而非「逾期」。

### F2 报销单 status 与 paymentStatus 双状态并行，且不进应付/付款链（资金链） ✅ 已修复
- **现象**：`invoice_reimbursement` 同时有 `status(0草稿/1审批/2付款/3取消/4拒绝)` 与 `payment_status(0/1)`，`pay()` 同时置两个字段；审批通过不创建应付账款。
- **证据**：`backend/.../service/finance/impl/InvoiceReimbursementServiceImpl.java:396-399,310-331`；实体 `InvoiceReimbursement.java:74-106`
- **修复**：合并为单一状态机；审批通过时调用 `payableService.createFor...` 进入应付→付款主链。
- **验证**：报销单审批通过→应付账款列表出现对应记录；付款后报销单状态与付款状态一致。

### F3 总账模块纯 Mock，无真实数据（资金链） ✅ 已修复
- **现象**：`LedgerController` 科目/凭证/试算平衡/期末结转全部硬编码，无 Mapper/Service 调用。
- **证据**：`backend/.../controller/finance/LedgerController.java:20-59,82-163`
- **修复**：以真实 `finance_voucher`/`accounting_subject` 落库实现总账查询（或确认前端 `FinanceLedger.vue` 已改走 voucherApi 后删除 Mock 控制器）。
- **验证**：过账凭证后总账页应显示对应科目余额；期末结转更新科目余额。

### F4 核心订单完成不生成应收账款（资金链） ✅ 已修复
- **现象**：`OrderNew` 完成仅写 `finance_record`，不调 `ReceivableService.createForOrder`（该调用只存在于 legacy `SalesOrder` 链路）。
- **证据**：`backend/.../service/impl/OrderNewServiceImpl.java:455-474`（persistOrderCost）；`backend/.../service/finance/impl/ReceivableServiceImpl.java:167-212`；`.../service/impl/SalesOrderServiceImpl.java:159-222`
- **修复**：订单完成事件中调用 `receivableService.createForOrder` 生成应收。
- **验证**：订单完成后应收账款列表出现对应应收记录（金额=订单应收额，分）。

### F5 采购结算单与应付/付款双轨，互不联动（采购/资金链） ✅ 已修复
- **现象**：`PurchaseSettlementServiceImpl.executePay` 仅更新结算单自身金额/状态，不创建付款单、不回写应付/订单；同一条采购在「结算单」与「应付/付款单」两条线上金额可能不一致。
- **证据**：`backend/.../service/purchase/impl/PurchaseSettlementServiceImpl.java:212-289`
- **修复**：结算执行时复用 `paymentService` 落付款单并回写应付，或明确结算单仅作对账视图、移除其支付动作。
- **验证**：对采购结算执行支付→应付账款/付款单出现对应记录且金额一致。

### F6 订单支付不落资金流水 fund_flow（订单/资金链） ✅ 已修复
- **现象**：`payOrder`/POS 支付仅写 `finance_record`/`order_payment_record_new`，不创建 `fund_flow`，资金流水页缺订单收入。
- **证据**：`backend/.../service/impl/OrderNewServiceImpl.java:180-230`；`.../service/impl/PosOrderPaymentServiceImpl.java:197,725`
- **修复**：支付成功时调用 `fundFlowService.create`（或确认现有事件链路补录）。
- **验证**：订单支付后资金流水列表出现该笔收入，银行余额正确变化。

### M1 会员余额/消费金额 分→元 未转换（会员链 #2）
- **现象**：后端 `MarketingMember.balance` 为分(Long)，前端 `MemberInfo.balance` 声明为元字符串，`member.ts getList` 未做分→元转换，`MemberList.vue` 直接 `parseFloat` 显示 → 余额按"分"当"元"显示。
- **证据**：`backend/.../entity/marketing/MarketingMember.java:79-80`；`frontend/src/types/member.ts:103-104`；`frontend/src/api/marketing/member.ts:70-72`；`frontend/src/views/marketing/MemberList.vue:507-511,712`
- **修复**：在 `member.ts` converter 统一分→元；累计消费/储值字段同规则。
- **验证**：会员列表余额、详情页余额/累计消费显示为正确元数值。

### M2 储值退款不扣会员余额（会员链）
- **现象**：`RefundServiceImpl.executeRefund` 只更新充值记录 `refund_amount/payment_status/refund_status` 并写财务流水，**未调用 `marketingMemberMapper.addBalance` 扣减余额** → 退款后会员 balance 不减少。
- **证据**：`backend/.../service/impl/marketing/RefundServiceImpl.java:167-221`（对照 `RechargeRecordServiceImpl.java:145` 充值时有 addBalance）
- **修复**：退款通过时扣减会员余额（并考虑赠送金规则）。
- **验证**：会员 A 余额 100 元→退款 30 元→余额应为 70 元。

### M3 会员统计接口占位（会员链）
- **现象**：`GET /v1/members/stats/overview` 大量字段（newThisMonth/activeMembers/.../levelDistribution 等）硬编码 0 或不返回。
- **证据**：`backend/.../controller/marketing/MarketingMemberController.java:137-155`
- **修复**：按 `member` 表真实聚合（本月新增、活跃/沉睡、充值/消费合计、等级分布等）。
- **验证**：会员概览页各统计卡与真实数据一致，非恒 0。

### M4 会员列表分页契约断裂（会员链）
- **现象**：前端期望 `{records,total}`，后端返回 `Result<List<MemberVO>>` 纯数组 → `records` 恒 undefined，列表空。
- **证据**：`frontend/src/api/marketing/member.ts:70-72`；`backend/.../controller/marketing/MarketingMemberController.java:57-60`
- **修复**：后端改返回 `IPage<MemberVO>` 或前端按数组适配。
- **验证**：会员列表分页加载、翻页正常。

### M5 会员状态/性别/渠道编码未转换（会员链 #3）
- **现象**：后端 Integer 编码（status 1-4/gender 0-2/registerChannel 1-6），前端语义字符串；converter 已定义但 getList 未调用。
- **证据**：`frontend/src/api/marketing/member.ts`；`frontend/src/api/marketing/converters.ts:50-91`
- **修复**：getList 链路上调用 converter 做编码↔语义转换。
- **验证**：会员列表状态/性别/来源渠道显示正确文案。

### M6 会员页充值记录 paymentStatus 缺 partial_refunded/success（会员链 #3）
- **现象**：`MemberRechargeRecord.paymentStatus` 仅 `pending/paid/refunded/failed`，`member.ts` 将 `partial_refunded→refunded`、`success→paid` 折叠，部分退款信息丢失。
- **证据**：`frontend/src/types/member.ts:304`；`frontend/src/api/marketing/member.ts:43-61`
- **修复**：类型补充 `partial_refunded/success`，converter 停止折叠。
- **验证**：部分退款的储值记录显示"部分退款"而非"已退款"。

### T1 原料追溯码 materialId 类型/字段名断裂（追溯链 #1）
- **现象**：前端 `productId?: number`，后端 `materialId String`，查询/回显均无法匹配；页面渲染的 `inboundTime/unitPriceYuan/totalPriceYuan/purchaseOrderNo/productCode/warehouseName/...` 后端实体均无。
- **证据**：`frontend/src/types/traceability/material-trace-code.ts:16,82,105`；`backend/.../entity/MaterialTraceCode.java:22-24`；`MaterialTraceCodeController.java:65`；`frontend/src/views/traceability/MaterialTraceCode.vue:170,537,758,759,771`
- **修复**：前端类型/API 对齐后端 `materialId(String)`；后端实体补齐页面展示字段（含分→元转换）；新增表列或 join 采购/仓储。
- **验证**：原料追溯列表/详情页各列有值、按物料可查询、溯源链可点击跳转。

### T2 食品追溯码 materialTraceCodes/materialDetails JSON 未细化（追溯链 #12）
- **现象**：前后端均以 `String` 存 JSON，TS 无结构定义，页面不解析展示。
- **证据**：`frontend/src/types/traceability/food-trace-code.ts:26-27`；`backend/.../entity/FoodTraceCode.java:59-64`
- **修复**：在 TS 类型中定义 `Array<{traceCode;materialId;materialName;quantity;unit}>` 结构并解析展示。
- **验证**：食品追溯码详情展开原料明细列表。

### R1 召回状态三套表达不一致（追溯链 #3）
- **现象**：`RecallRecord.status Integer(1/2/3)`；前端 `RecallRecord` 用语义字符串 `pending/processing/completed/cancelled`；同文件 `AffectedTraceCode.status` 用 number。
- **证据**：`backend/.../entity/RecallRecord.java:64-66`；`frontend/src/types/traceability/recall.ts:155,166,61`
- **修复**：统一映射（建议后端 Integer 权威，converter 双向映射）；召回执行时同步回写原料追溯码状态（现仅回写 food_trace_codes）。
- **验证**：召回记录状态显示正确；受影响追溯码列表有值。

### R2 追溯模块 PUT 接口 body/query 参数不匹配（追溯链）
- **现象**：前端发 body，后端 `@RequestParam` 收参 → 状态更新/上传均失败。
- **证据**：`frontend/src/api/traceability/material-trace-code.ts:118-121` ↔ `MaterialTraceCodeController.java:106-108`；`food-trace-code.ts:103-107` ↔ `FoodTraceCodeController.java:118-119`；`inspection.ts:84-85` ↔ `InspectionController.java:113`
- **修复**：统一为 body 或统一为 query（建议 body DTO）。
- **验证**：原料追溯码状态更新、食品追溯码制作状态更新、检验报告上传均成功。

### D1 日结对账状态枚举不匹配（门店运营）
- **现象**：后端 `DailySettlementVO.status String('draft/pending/approved/rejected')`，前端 `number` + `convertStatus` 按数字 0/1/2 映射 → 状态恒显示「待处理」。
- **证据**：`backend/.../dto/store/DailySettlementVO.java:101-104`；`frontend/src/types/store-operation/settlement.ts:59`；`frontend/src/api/store-ops/converters.ts:113-117`
- **修复**：前端 converter 改为字符串→语义状态映射。
- **验证**：日结列表状态列显示正确的 草稿/待处理/已审核/已拒绝。

### D2 日结 refundAmount/confirmTime 展示断裂（门店运营 #11）
- **现象**：`refund_amount` 列已存在但后端返回 `refundAmountDisplay`（元），前端 converter 未映射 → 退款列恒空；`confirm_time` 数据库/实体无此列，前端读 `confirmTime` 恒空。
- **证据**：`V20260514__add_settlement_payment_breakdown.sql:16`；`DailySettlementVO.java:193-199`；`frontend/src/api/store-ops/converters.ts:91-111`；`frontend/src/views/store-ops/StoreDailySettlement.vue:98,123,127,461`
- **修复**：converter 映射 `refundAmountDisplay`（或后端直接返回分）；新增 `confirm_time` 列并回写。
- **验证**：日结列表退款金额、确认时间有值。

### T3 供应商追溯召回 Tab 恒空（追溯链）
- **现象**：`SupplierTraceServiceImpl.getSupplierTrace` 只 `setTotalRecalls` 从不 `setRecalls`；前端 `recalls` 类型声明为 `RecallRecord[]` 但后端 VO 为 `RecallQueryResultVO`。
- **证据**：`backend/.../service/trace/impl/SupplierTraceServiceImpl.java:86-90`；`dto/trace/SupplierTraceVO.java:48`；`frontend/src/views/traceability/SupplierTrace.vue:70`
- **修复**：后端填充 `recalls` 列表，前端类型对齐 VO。
- **验证**：供应商追溯页召回 Tab 显示该供应商相关召回记录。

---

## 三、P1 修复项（功能缺失/接口不存在）

| 编号 | 模块 | 现象 | 证据（文件:行号） | 修复方向 |
|---|---|---|---|---|
| A1 | 会员链 | 消费记录无后端端点，前端硬编码空数组 | `frontend/src/api/marketing/member.ts:110-115` | 后端提供消费记录接口（关联订单） |
| A2 | 会员链 | RFM 前端 API 与后端端点不匹配且未被页面引用 | `frontend/src/api/marketing/customer-analysis.ts:9-33` ↔ `CustomerAnalysisController.java:21-163` | 对齐端点或删除死代码 |
| A3 | 资产 | 资产调拨整体无后端 Controller | `frontend/src/api/asset/transfer.ts:1-50` | 补后端或移除前端入口 |
| A4 | 订单链 | 预约"到店转订单"无后端接口，arrive 仅改状态 | `backend/.../controller/TableReservationNewController.java:129-138` | 提供 arrive→createOrder 转单接口 |
| A5 | 门店运营 | 叫号仅查询/统计，无取号/叫号/过号写接口 | `CallNumberQueueManagementController.java:48-78` | 补写接口（或确认由 POS/小程序承担） |
| A6 | 门店运营 | 证件续期历史无后端；renewalRecordId 前端伪造 | `views/store-ops/StoreCertificate.vue:801-802`；`components/CertificateRenewalDialog.vue:213` | 后端补续期历史接口与关联列 |
| A7 | 门店运营 | 招聘仅审批流，岗位/简历/面试缺失 | `frontend/src/api/store-ops/recruitment.ts:8-12` | 补接口或移除占位 |
| HR-1 | 人事/排班 | 排班前后端脱节：后端引擎占位、前端无消费页面、hr 侧 scheduleApi 为 Mock | `ScheduleEngineServiceImpl.java:48-74,116-133`；`frontend/src/api/hr/attendance.ts:348-444`；`views/Schedule/` 空目录 | 接后端真实排班接口或明确排班页面归属 |
| HR-2 | 人事/薪资 | 薪资 pay/reject/batchPay/账期 approve/pay/cancel/sync 均 throw；账期接口返回结构错配 | `frontend/src/api/hr/salary.ts:276-424`；`SalaryBatchController.java:59-70` | 补后端接口，对齐 SalaryBatch 结构 |
| HR-3 | 人事/薪资 | 薪资生成基于 users+默认值兜底，不读考勤/排班；金额 BigDecimal 元 vs 前端分 | `SalaryServiceImpl.java:85-131`；`salary.ts:10,22` | 改为按员工+考勤计算，金额统一分 |
| HR-4 | 人事 | 3 个页面 100% Mock：合同智能/员工画像/知识库智能 | `frontend/src/api/contract-intelligence.ts:21-131`；`employee-intelligence.ts:35+`；`knowledge-intelligence.ts:18+` | 补后端或标注"规划中"下线入口 |
| HR-5 | 人事/合同 | 合同 18 个方法后端未实现（续签/变更/签任务/验签/PDF 下载等） | `frontend/src/api/hr/contract.ts:27-35` | 按业务优先级补接口 |
| C1 | 资产 | 盘点 CANCELLED 前端映射保留值 4，后端仅认 0-3，无"已取消"语义 | `api/asset/inventory.ts:95-101`；`AssetInventoryController.java:44` | 后端补"已取消"状态码 |
| D3 | 设备 | 状态历史 storeId 硬编码 1L | `DeviceStatusHistoryServiceImpl.java:64` | 从 JWT 上下文取门店 |
| E1 | 供应商门户 | 短信验证码未接真实网关 | `supplier-portal.ts:133-135` | 接网关或标注受限 |
| O1 | 系统权限 | 权限中心操作日志为前端假数据（预填 AL-INIT，addAuditLog 仅内存） | `stores/permission.ts:194-243,1148-1163` | 接真实审计接口或移除假数据 |
| O2 | 系统权限 | 前端不消费后端 `/v1/auth/menus` 菜单接口 | `AuthController.java:100`；`stores/permission.ts` | 明确菜单权威源（后端 or 前端模板） |
| O3 | 系统权限 | 操作审计今日操作数/登录次数恒 0 | `OperationAudit.vue:63-69,187-190`；`AuditLogServiceImpl.java:190` | 前端类型映射 `todayCount` |
| O4 | 运营中心 | 策略字段塞 description JSON hack；告警 ignored/type 靠推断 | `api/operations/strategy.ts:145-162`；`alert.ts:107-158` | 后端补列或前端删除 |
| P1 | 产品/BOM | 菜品成本保存时快照，库存成本变化不联动重算 | `FoodServiceImpl.java:704-732`；`FoodManagement.vue:243-249,413` | 按需引入"重算菜品成本"触发机制 |

---

## 四、P2 修复项（一致性优化）

| 编号 | 现象 | 证据 | 方向 |
|---|---|---|---|
| P2-1 | ~~订单完成扣门店库存时不写 `store_inventory_log`~~ ✅ 已修复（2026-07-31）：`increaseStock/decreaseStock` 内部统一写流水（含变动类型+来源备注），覆盖采购入库/到货确认/销售出库/调拨/退款回补/采购退货/入库作废/调整全路径；`store_inventory_log` 数量列改 DECIMAL(12,3) 支持小数 | `StoreInventoryServiceImpl.java`、`V20260731_009__fix_store_inventory_log_quantity_precision.sql`、各调用方 | 实跑验证：调整+2 生成流水（20.000→22.000，operator=admin） |
| P2-2 | `cost_record` 无订单关联字段，无法按订单追溯成本 | `OrderNewServiceImpl.java:461-468` | CostRecordCreateDTO 增 `bizId/orderNo` |
| P2-3 | 会员链时间字段 createdAt/updateTime 混用（#4） | `types/member.ts:133,298` vs `MemberVO.java:123` | 统一 createTime/updateTime |
| P2-4 | 采购订单 createByName 后端仅 userId | `api/purchase/order.ts:217-218` | 后端 join users 返回姓名 |
| P2-5 | 印章图片 Base64 存入 URL 字段，语义混用 | `seal.ts:33-34,79-94` | 区分图片地址/Base64 存储 |
| P2-6 | 门店库存 specification/category 数据库无列（#11） | `StoreInventory.vue` 列表 | 增列或 join 物料档案 |
| P2-7 | 采购订单 updateBy/deletedBy/deletedTime 前端兜底空串 | `api/purchase/order.ts:89-93,220-223` | 降级可选或后端补字段 |

---

## 五、L1 断点 #1-15 现状对齐表（与 data-flow-map.md 对照）

> data-flow-map.md 中的红色虚线断点按本次核查结果更新状态。已修复的断点在原图中应改为实线/绿色。

| 编号 | 问题 | 现状（2026-07-31 核查） | 待办 |
|---|---|---|---|
| #1 | 主键命名/类型不一致 | ⚠️ 仍存在：追溯链 materialId(number↔String) | T1 |
| #2 | 金额单位不统一 | ⚠️ 采购/财务/库存已统一为分；**会员链仍断裂** | M1/M2 |
| #3 | 状态字段表达不统一 | ⚠️ 部分修复（盘点 CANCELLED 已修）；仍存：召回状态、会员 paymentStatus、日结状态 | R1/M6/D1 |
| #4 | 时间字段命名混用 | ⚠️ 会员链仍存 | P2-3 |
| #5 | 物料字段命名混用 | ✅ 采购链路已统一 materialId（converter）；采购订单仍用 productId 为已知残留 | 保持现状 |
| #7 | 审批字段命名不统一 | ✅ 已统一 createByName/approvedByName | — |
| #8 | 关联单据号字段命名 | ⚠️ 部分修复 | 按 `{entity}No` 规范抽查 |
| #11 | 数据库字段缺失 | ⚠️ 部分：日结 refund_amount 已补、confirm_time 仍缺；门店库存 specification/category 仍缺 | D2 / P2-6 |
| #12 | JSON 字段结构未细化 | ⚠️ 仍存在 | T2 |
| #14 | 字段无回写 API | ✅ LK-FINANCE-02 已修复（付款回写已核实） | — |
| #15 | TS 类型缺失但页面已用 | ✅ 设备链 online/ip 已补齐（断点消除） | — |
| 追加 | 采购结算双轨 / 应收状态码冲突 / 总账 Mock / 订单不生成应收 | ⚠️ 本次新发现 | F1-F5 |

---

## 六、修复执行记录（回填用）

| 日期 | 编号 | 修改文件 | 验证结果 | 状态 |
|---|---|---|---|---|
| 2026-07-31 | （静态核查） | 全量核查，结论见第二章 | — | 待修复 |
| 2026-07-31 | SR-1 | `backend/src/main/resources/db/migration/V20260731_006__direct_receipt_and_single_store_finance.sql` | `mvn compile -q` 通过 | 已完成 |
| 2026-07-31 | SR-2 | `dto/ReceiptConfirmationCreateDTO.java`（receiptSource/storeId/supplier/明细字段）、`entity/ReceiptConfirmation.java`（receiptSource）、`service/impl/ReceiptConfirmationServiceImpl.java`（createDirectConfirmation 分支） | `mvn compile -q` 通过 | 已完成 |
| 2026-07-31 | SR-3 | `service/impl/ReceiptConfirmationServiceImpl.java`（createDirectPayable） | `mvn compile -q` 通过 | 已完成 |
| 2026-07-31 | SR-4 | `types/purchase-arrival.ts`、`api/receipt-confirmation.ts`（converter/toCreateDTO）、新增 `components/business/receipt-confirmation/DirectReceiptFormDialog.vue`、`composables/useReceiptPage.ts`、`views/store-ops/StoreReceiving.vue`（新增直收按钮） | `npm run build` 通过（exit 0） | 已完成 |
| 2026-07-31 | SR-5 | 后端迁移（centralized-single role_config 加 finance）+ `stores/permission.ts` `getFallbackTemplates` CORE_DOMAINS 加 finance | `mvn compile -q` + `npm run build` 通过 | 已完成 |
| 2026-07-31 | SR-6 | `stores/permission.ts` `syncRoleDomainMatrix` 移除 SUPER_ROLES 强制全显 | `npm run build` 通过 | 已完成 |
| 2026-07-31 | SR-7 | `event/ReceiptConfirmationCompletedEvent.java`、`event/listener/ReceiptConfirmationEventListener.java`（新增）、`event/EventPublisher.java`、`service/impl/ReceiptConfirmationServiceImpl.java` | `mvn compile -q` 通过 | 已完成 |
| 2026-07-31 | SR-8 | `views/store-ops/StoreReceiving.vue`（新增直收按钮按 `centralized-single` 模式显隐） | `npm run build` 通过 | 已完成 |
| 2026-07-31 | SR-9 | `db/migration/V20260731_007__single_store_role_based_finance_purchase.sql`、`stores/permission.ts`（fallback 模板 OWNER_DOMAINS + SR-6 注释） | `mvn compile` + `npm run build` 通过；DB role_config 已确认 | 已完成 |
| 2026-07-31 | T1-1 | `db/migration/V20260731_008__fix_material_trace_code_id_types.sql` | `mvn compile` 通过；实跑验证追溯码生成成功 | 已完成 |
| 2026-07-31 | 实跑验证 | 登录→直收建单→门店库存→追溯码→应付（`direct-receipt-test.ps1`） | 建单/库存/追溯码 PASS；应付代码逻辑正确但幂等跳过（见已知问题） | 见已知问题 |
| 2026-07-31 | 移除直收（D1 回退） | 后端：`ReceiptConfirmationServiceImpl`（去 direct 分支/`createDirectPayable`/常量）、`dto/ReceiptConfirmationCreateDTO`（回退原状+恢复 @NotNull）；前端：删除 `DirectReceiptFormDialog.vue`、`StoreReceiving.vue`/`useReceiptPage.ts` 去直收、`receipt-confirmation.ts`/`types/purchase-arrival.ts` 回退 | `mvn compile` + `npm run build` 通过；实跑验证无 arrivalId 请求被拒绝（HTTP 200 code=400 "到货单ID不能为空"） | ✅ 已完成 |
| 2026-07-31 | P2-1 门店库存流水全路径打通 | `StoreInventoryServiceImpl`（increaseStock/decreaseStock 重载 + writeLog + 注入 StoreInventoryLogService）、`StoreInventoryController.adjust`（去重复流水、改用重载）、各调用方（调拨/订单/退货/入库/到货）、`entity/StoreInventoryLog`（数量 BigDecimal）、`MaterialTraceCodeServiceImpl`（2 处 BigDecimal）、`db/migration/V20260731_009__fix_store_inventory_log_quantity_precision.sql` | `mvn compile` 通过；实跑验证调整+2 生成流水（20.000→22.000，operator=admin） | ✅ 已完成 |
| 2026-07-31 | 数据修复：门店库存/仓库库存名称乱码回填 | 根因：`store_inventory`/`inventory` 的 `material_name`/`unit` 副本被早期编码修复污染（如 material_id=1 应"娃娃菜"实为"???"），主数据 `material_archives` 干净。已用 `UPDATE ... FROM material_archives` 回填干净名称/单位（store_inventory 6 行、inventory 11 行） | 直接 SQL 修复（非迁移） | 无头浏览器验证：原料明细下拉 14 项，名称已正常 | |
| 2026-07-31 | 数据清理：移除测试物料 | 删除 `store_inventory`（8 行）与 `inventory`（17 行）的测试库存行（material_id 22-34：Test Cucumber/E2E Potato/Test Tomato/E2E Material/UI测试物料B/测试采购物料）；`material_archives` 软删 13 条测试物料（deleted=1，保留采购历史引用） | 直接 SQL | 下拉仅剩 娃娃菜/菠菜/生菜/虾滑/蟹棒 | |
| 2026-07-31 | 方案D+/E：库存决策看板（自校准实际每份用量 + 损耗差异分析） | 后端：`StockForecastMapper`（订单销量→BOM→库存流水聚合，cast 修复 food_id 类型）、`StockForecastServiceImpl`（实际每份用量=近N天实际出库÷售出份数，含损耗；可做份数/售罄时间/补货量）、`StockForecastVO`/`StockForecastItemVO`/`VarianceItemVO`、`BomCheckController` 新增 `GET /{dishId}/forecast` + `GET /variance-analysis`；前端：`bom-check.ts` 新增 forecast/variance API、新增 `StockForecastDialog.vue`（决策看板：可做份数/理论/售罄/补货 + 原料明细 + 损耗差异 tab）、`FoodManagement.handleCheckStock` 改调 forecast | `mvn compile` + `npm run build` 通过；实跑验证 forecast 返回（暂无销量回退理论，积累后自校准） | ✅ 已完成 |
| 2026-07-31 | F1-F6 资金链 P0 修复 | F1 应收状态统一 1未收/2部分收/3已核销/4逾期（`ReceivableServiceImpl.confirmPayment` 部分收置2、`ReceiptServiceImpl` 核销4→3、getStatusName、自动逾期联动）；F2 报销单单状态源（`InvoiceReimbursementServiceImpl.pay` 不再双写 payment_status）；F3 删除 Mock `LedgerController`（前端已用 voucherApi）；F4 订单完成未付清自动生成应收（新增 `ReceivableService.createForOrderByNo` + `OrderCompletedEventListener.recordReceivableIfUnpaid`）；F5 采购结算付款联动应付/付款单（`PurchaseSettlementServiceImpl.linkPaymentToPayables`，优先 registerPayment 四账联动，降级 confirmPayment）；F6 订单支付落资金流水（`OrderNewServiceImpl.recordPaymentFundFlow`，收入/销售收款） | `mvn compile` 通过；后端已重启 | ✅ 已完成 |
| 2026-07-31 | 毛细管修复：菜品原料下拉空白（表格内下拉被裁剪） | 根因：`el-select` 在 `el-table` 单元格内且带 `:teleported="false"` → 下拉内联渲染被表格 `overflow:hidden` 裁剪（DOM 有选项但不可见）。修复：表格内下拉改用 `teleported=true` + `:popper-options="{ strategy:'fixed' }"`（锚点正确、滚动不追锚、不被裁剪）；下拉选项标签简化为仅"物料名称"（移除价格与单位，单价/单位选择后自动回填）。涉及：`FoodManagement.vue`、`DishComboDialog.vue`、`BatchPricingDialog.vue` | 前端修复 | 无头浏览器 `topElementIsOption=true` 验证可见且锚点正确；`npm run build` 通过 | |
| 2026-08-01 | 批 2：复核问题修复（采购申请/计划/菜品/套餐/定价/转单 500） | ① 分类过滤（控制器补 categoryId 绑定）；② 页大小持久化（useCrudTable localStorage）；③ 商品档案加列（barcode/origin/shelf_life/storage_condition，V20260731_011）+ 移除假字段 + 序号列；④ 明细下拉数据源：采购申请/计划=商品档案+最新价（latestPriceMap）、菜品管理**保持门店库存**（用户澄清正确设计）；⑤ 套餐管理：openEdit 补 getById（修复编辑空白）、明细加单价/小计列（converter 补 unitPrice/subtotal）；⑥ 定价历史：PricingBackend 字段对齐 oldSalePrice/newSalePrice；⑦ 转单 500：purchase_request.store_id="624" 脏数据修复（→1）+ `resolveValidStoreId` 防御（注入 StoreNewMapper）；⑧ 采购计划：新增生成计划（generate-from-stock，扫描低库存）+ 导出（POI Excel）接口并接入前端；⑨ 备注对齐/列宽固定 resizable=false/对话框加宽（申请1100/计划1100/菜品1000/套餐900） | 后端+前端 | `mvn compile` + `npm run build` 通过；UI 实测：分类过滤 5 条、下拉 21 项、套餐编辑明细带出、定价历史 0.50→0.80、页大小刷新保持 | ✅ 已完成 |
| 2026-08-01 | 批 3：采购业务链衔接（用户确认方案） | ① 申请页"生成订单"成功后跳转 `/purchase/orders?requestNo=`（原不跳转）；② 计划→订单：新增 `POST /v1/purchase/plans/{id}/generate-order`（仅 approved/executing、幂等返回既有单、草稿、requestNo=planNo、sourceType=purchase_plan）+ 计划页"生成订单"按钮；③ 订单查询 requestNo 过滤（后端 DTO+前端 order.ts/queryForm）；④ 修正路由坑：订单页为 `/purchase/orders`（复数） | 后端+前端 | 实测：PR20260729003→跳转定位 1 行；PL20260801001→PO202608010004 幂等通过；订单状态草稿(0) | ✅ 已完成 |

**已知问题（待决策）**：应付幂等键 `payable_no = "AP" + 确认单ID`，当前库中 7-28 遗留测试应付（AP0000000007/8/15/16/17，供应商 "E2E Supplier"/null，对应的确认单已删除）与今日确认单 ID 复用冲突，导致这些 ID 的直收应付被幂等跳过。建议清理孤儿测试应付或改进幂等键。

> 每完成一项，在此追加一行并同步更新 `data-chain-fix-tracking.md`、`frontend-verification-plan.md` 与 `data-flow-map.md`。

---

## 七、端到端验证清单（实跑）

> 启动方式见 `docs/project-map.md` 与 `DEPLOY.md`。以下验证以"集中式单店"模式、前端展示为准逐条执行，通过后打勾。

### 7.1 采购→库存→成本（钱货主闭环）
- [ ] 1. 建采购申请→审批通过→转采购计划→计划执行→生成采购订单（订单回显来源申请号 requestNo、来源类型、优先级、计划号）
- [ ] 2. 采购订单→生成到货单（回写实收数量，不增加库存）
- [ ] 3. 到货质检通过→确认入库→仓库库存 + 门店库存增加（unit_cost 按最新入库价覆盖、total_cost 正确）
- [ ] 4. 确认入库自动生成应付账款（payableNo 幂等，重复确认不重复生成）
- [ ] 5. 库存汇总页：仓库库存 + 门店库存 + 总库存 + 总价值正确（UNION ALL）
- [ ] 6. 财务付款→采购订单 paidAmount/paymentStatus 自动回写（0未付/1部分/2已付）
- [ ] 7. 采购结算执行支付→应付/付款单联动金额一致（F5 修复后验证）

### 7.2 订单→扣库存→成本结转→资金（销售闭环）
- [ ] 1. 新建订单（含套餐）→支付→订单完成→按 BOM 扣减门店库存、生成 cost_record、解锁桌台
- [ ] 2. 成本管理/成本分析按期间汇总食材成本，且可按订单追溯（P2-2 修复后）
- [ ] 3. 订单完成/支付后应收账款出现对应记录（F4 修复后）；资金流水出现该笔收入（F6 修复后）
- [ ] 4. 全额退款审批通过→门店库存回补、会员余额扣减（M2 修复后）、退款状态正确
- [ ] 5. 日结生成→确认→状态/退款金额/确认时间正确显示（D1/D2 修复后）

### 7.3 追溯链
- [ ] 1. 入库批次→生成原料追溯码（materialId 查询正常，T1 修复后）
- [ ] 2. 原料追溯码→食品追溯码（materialTraceCodes 明细可展开，T2 修复后）
- [ ] 3. 质检不合格→触发召回→受影响追溯码回写（food + material，R1 修复后）
- [ ] 4. 供应商追溯页召回 Tab 有数据（T3 修复后）

### 7.4 会员链
- [ ] 1. 会员列表分页、余额/累计消费按元显示（M1/M4/M5 修复后）
- [ ] 2. 充值→余额增加；退款→余额扣减（M2 修复后）；部分退款显示"部分退款"（M6）
- [ ] 3. 会员概览统计卡非恒 0（M3 修复后）

### 7.5 模块冒烟
- [ ] 1. 排班页可查看/生成计划（HR-1 修复后）；薪资账期/发放流程可走通（HR-2/3）
- [ ] 2. 资产调拨可操作（A3 修复后）；权限中心操作日志真实（O1）
- [ ] 3. 合同智能/员工画像/知识库智能 页面有真实数据源（HR-4）

---

## 八、待确认事项

1. **排班模块归属**：后端已实现 10 个 schedule 接口但前端无页面，排班是否计划在管理端落地？还是仅由 POS/小程序承担？
2. **叫号写接口归属**：管理端是否需要取号/叫号操作，还是只读展示历史？
3. **资产调拨**：确认是否需要管理端提供调拨功能，还是移除入口？
4. **三个 100% Mock 页面**（合同智能/员工画像/知识库智能）：是规划中功能待后端支持，还是应下线？
5. **总账 Mock**：确认前端 FinanceLedger.vue 是否已改走 voucherApi，若是则建议删除 Mock 控制器，避免误导。
6. **会员消费记录**：消费记录应取自哪个订单源（order_new / POS），以哪个为准？
7. **菜单权威源**：后端 /v1/auth/menus 与前端模板本地菜单，最终以哪个为准？
8. ~~**admin/owner 是否遵循模式**~~ ✅ 已决策（2026-07-31）：**按角色开放**——单店下 admin/owner 显示 finance+purchase，店长/员工/运营不显示（SR-9）
9. ~~**单店直收是否生成原料追溯码**~~ ✅ 已决策：**直收已移除**；到货确认统一生成原料追溯码（SR-7 保留）

---

## 九、门店收货 / 单店直收血缘专项（2026-07-31 决策）

> 背景：`centralized-single` 模板核心域仅 9 个（`permission.ts:1062-1065`），**不含 purchase/warehouse**。单店模式下无法建采购订单/到货单，而 `ReceiptConfirmationServiceImpl.createConfirmation()`（`:100-102`）强制 `arrivalId` → **单店门店收货无单可收，门店库存与菜品成本链在单店模式下断裂**。

### 9.1 决策记录（2026-07-31 已确认）

| 编号 | 决策 | 结论 |
|---|---|---|
| D1 | 单店"无单直收"是否支持 | ~~支持~~ **已移除（2026-07-31）**：改为开放采购模块（admin/owner 可见），门店收货统一走「采购订单→到货单→收货确认」，链路完整可溯源 |
| D2 | centralized-single 是否隐藏财务 | **不隐藏，按角色开放**：admin/owner（老板/管理员）显示「财务+采购」，店长及以下不显示（避免员工掌控财务与采购业务） |
| D3 | 单店直收是否生成应付 | **随 D1 移除**：不再有直收；应付统一由到货确认生成 |
| D4 | admin/owner 是否遵循模式 | **按角色细化**：模式内 admin/owner 开放 finance+purchase；其余角色（店长/员工/运营等）不显示——由模板角色域矩阵 + 菜单 `visibleRoles` 双重过滤 |
| D5 | 直收是否生成原料追溯码 | **随 D1 移除**；到货确认流程仍统一生成原料追溯码（SR-7 保留，修正原采购入库追溯码断裂） |

### 9.2 域矩阵修订（centralized-single，按角色）

```
admin/owner（老板/管理员）：workspace/store-ops/product/order/operations/member/traceability/device/system + finance + purchase
其余角色（店长/员工/运营/HR）：不含 finance / purchase（finance_director 除外，见财务角色）
仍隐藏：warehouse / hr / asset
说明：采购/财务菜单全局显隐由模板 admin 域列表派生；店长/员工再经 visibleRoles 过滤不可见。
```

### 9.3 统一血缘模型（最终）

```
【单店（admin/owner 开放采购）】
  采购模块：商品档案/供应商/采购申请→采购计划→采购订单（admin/owner 维护）
    → 到货单(receiverType=STORE) → 门店收货（确认入库）
    → store_inventory.unit_cost（最新入库价覆盖）+ store_inventory_log
    → 应付账款 payable（财务模块 admin/owner 可见）
    → 原料追溯码（SR-7：确认后异步生成）
    → 门店库存 → 菜品原料明细(unitCost 元/单位) → 菜品成本
【连锁】 采购订单 → 到货单(receiverType=STORE/WAREHOUSE) → 收货确认 → 仓库/门店库存
【调拨】 调拨单 → store_inventory.increaseStock
所有入账统一 → store_inventory.unit_cost → 菜品原料明细(unitCost) → 菜品成本
```
> 门店收货（StoreReceiving）统一基于采购到货单确认，**不再有无单直收入口**。

### 9.4 实施项（新增，优先级建议）

| 编号 | 改动 | 位置 | 状态 |
|---|---|---|---|
| ~~SR-1~SR-4~~ | ~~直收 DB 迁移 / DTO / createConfirmation direct 分支 / 直收 UI~~ | 已回退 | ❌ 已移除（2026-07-31） |
| ~~SR-8~~ | ~~直收入口模式开关~~ | 已回退 | ❌ 已移除 |
| SR-5 | 权限模板 finance 域 | `stores/permission.ts` + 后端迁移 | ✅ 由 SR-9 覆盖 |
| SR-6 | 移除超管强制全显，域矩阵按模板角色列表 | `stores/permission.ts` | ✅ 已完成 |
| SR-7 | 到货确认统一生成原料追溯码：`ReceiptConfirmationCompletedEvent` + `ReceiptConfirmationEventListener`（AFTER_COMMIT 异步） | `event/ReceiptConfirmationCompletedEvent.java`、`event/listener/ReceiptConfirmationEventListener.java`、`event/EventPublisher.java`、`service/impl/ReceiptConfirmationServiceImpl.java` | ✅ 保留（仅到货流程触发） |
| SR-9 | centralized-single 按角色开放 finance+purchase（admin/owner） | `db/migration/V20260731_007__single_store_role_based_finance_purchase.sql`、`stores/permission.ts` | ✅ 已完成 |
| T1-1 | 追溯码列类型对齐（断点#1）：`material_trace_code` 三列 bigint→VARCHAR，删除不兼容 FK | `db/migration/V20260731_008__fix_material_trace_code_id_types.sql` | ✅ 已完成（实跑验证） |

> 说明：`receipt_confirmations` 的 `receipt_source` 列与可空约束已保留（历史直收数据引用，默认 `arrival`），直收创建能力已从后端移除（无 arrivalId 请求被校验拒绝）。

### 9.5 关键约束（标准化原则）

1. **受控链路**：门店收货仅基于采购到货单（采购订单→到货单→收货确认），**无无单直收**。
2. **主数据必引用**：物料/供应商必须来自主数据（商品档案/供应商档案），禁止自由文本，保证可追溯。
3. **最新入库价规则不变**：确认入库后 `unit_cost` 按最新入库价格法覆盖（LK-WAREHOUSE-03 规则）。
4. **追溯证据统一**：到货确认后统一生成原料追溯码（SR-7），采购入库与到货确认两条来源均可追溯。

---

## 十、相关文档索引

- 链路图：`docs/data-flow-map.md`（L1）
- 字段契约：`docs/frontend-verification-plan.md`（L2，4.2 全模块字段表、4.3 共性汇总）
- 修复进度源：`docs/data-chain-fix-tracking.md`
- 交接结论：`docs/data-chain-fix-handover.md`（决策 9.1-9.3）
- 业务链设计：`docs/data-business-chain.md`
- 项目地图：`docs/project-map.md`
- 数据转换器规范：`docs/spec/16-数据转换器规范.md`
