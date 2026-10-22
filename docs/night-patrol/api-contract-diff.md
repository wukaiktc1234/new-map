# API 契约静态比对报告（夜间任务 A）

- 仓库：`P:\my-new-project`　分支：`ai-night-chore-1`
- 方法：纯静态阅读 + 脚本化抽取（未连库、未启动后端、未发真实 HTTP、未修改任何源码）
- 生成时间：2026-10-23（Asia/Hong_Kong）
- 本次唯一写入文件：本报告

---

## 1. 摘要

| 指标 | 数值 |
| --- | --- |
| 后端 controller 文件 | 269（`backend/src/main/java/com/foodtraceability/controller/**`） |
| 后端 HTTP 端点（映射注解解析） | **2057** |
| 前端调用点位 | **944**（`frontend/src/api/**` 197 个 ts/js + `frontend/src/**/*.vue`，共 422 文件） |
| 路径级可配对调用 | 898（95.0%） |
| 前端调了后端不存在的路由 | **28** |
| 方法不匹配（脚本原始命中） | 18（人工复核后**真问题 10 条**，8 条为字符串拼接导致的脚本误报，判定见 6.4） |
| 人工逐字段深入比对端点 | **46 个**（清单见第 7 节） |
| 发现问题总数 | **58** |
| 高危 | **14** |
| 中低危 | **16** |
| 类型 / 精度专项条目 | **10**（含 20 处代码位置） |
| 待人工确认 | **8** |

**最需要立刻处理的 3 件事**

1. 日结对账（`/v1/store-management/settlements`）：后端已把金额转成「元」字符串，前端又按「分」除以 100 → 金额整体缩小 100 倍；状态后端是字符串，前端按数字映射 → 状态永远显示「待日结」。
2. 一批写操作端点后端根本不存在或方法不符（会员冻结/拉黑、HR 四类配置保存、优惠券发放/核销、薪资批次审发、POS 会员检索、排班引擎生成）→ 前端调用必然 404/405。
3. 采购入库创建在 `warehouseId` / `stockinDate` 为空时把 `undefined` / `null` 送去 `@NotNull` 字段 → 必 400，错误被前端 catch 吞成「创建失败」。

---

## 2. 高危问题表

| # | 端点 | 前端证据 | 后端证据 | 问题 | 影响 | 建议 |
| --- | --- | --- | --- | --- | --- | --- |
| H1 | GET `/v1/store-management/settlements` 及 `/{id}` | `frontend/src/api/store-ops/converters.ts:97-99`（`totalRevenue: storeDataConverter.fenToYuan(backend.totalRevenue)`）、`frontend/src/types/store-operation/settlement.ts:45-64`（注释「金额为分，状态为数字编码」） | `backend/src/main/java/com/foodtraceability/dto/store/operation/vo/DailySettlementVO.java:68-73`（totalRevenue 注释「单位：元」、类型 String）、`backend/src/main/java/com/foodtraceability/dataservice/impl/DailySettlementDataServiceImpl.java:188-192`（`return String.valueOf(fen / 100.0);`） | **金额单位二次换算**：后端 VO 已是「元」字符串，前端再 fenToYuan 除 100 | 日结列表/详情金额缩小 100 倍（15800.50 → 158.01）；对账、利润、导出全部错 | 前端 settlement 转换器去掉 fenToYuan，直接 `Number(backend.totalRevenue)`；同步修正 `SettlementBackend` 注释与字段语义 |
| H2 | 同上 | `frontend/src/api/store-ops/converters.ts:105,114-117`（`convertStatus(backend.status)` 按 0/1/2 数字映射）、`frontend/src/types/store-operation/settlement.ts:59`（`status: number`） | `.../vo/DailySettlementVO.java:101-109`（`private String status`，取值 draft/pending/approved/rejected/abnormal）、`.../DailySettlementDataServiceImpl.java:196-206`（`getStatusName(String)`） | **状态编码类型错位**：后端字符串，前端按数字映射 | `map["approved"]` 为 undefined → 永远回落 `'pending'`；列表全显示「待日结」，已确认单据仍可点「确认」，`pendingCount` 统计失真 | 前端改字符串直传，仅做 `SettlementStatus` 白名单校验 |
| H3 | GET `/v1/store-management/settlements/export` | `frontend/src/api/store-ops/settlement.ts:167-168`（`window.open('/v1/store-management/settlements/export?...')`） | `frontend/src/api/request.ts:26`（baseURL=`/api`）、`frontend/vite.config.ts:95-96`（仅代理 `/api`）、`backend/src/main/resources/application-dev.yml:69-70`（context-path: `/api`） | **绕过 axios baseURL**：window.open 直接开 `/v1/...`，缺 `/api` 前缀，dev 无代理、后端 context-path 也不匹配 | 导出 404 / 拿到 SPA index.html 损坏文件；而 `StoreDailySettlement.vue:263-268` 仍提示「导出成功」 | 改为 `window.open(API_BASE_URL + '/v1/...')`，或走 `responseType: 'blob'` 的 axios 下载 |
| H4 | PUT `/v1/members/{id}/freeze`、`/unfreeze`、`/blacklist`、`/preferences` | `frontend/src/api/marketing/member.ts:187-199, 225-227` | `backend/src/main/java/com/foodtraceability/controller/MarketingMemberController.java:21`（`@RequestMapping("/v1/members")`）；全类映射仅 `POST /`、`GET /{memberId}`、`GET /`、`GET /phone/{phone}`、`PUT /{memberId}`、`POST /{memberId}/visit`、`POST /{memberId}/upgrade-level`、`PUT /{memberId}/status`、`GET /statistics/summary`、`GET /stats/overview`（:31-137） | **路由不存在**：后端只有一个状态入口 `PUT /{memberId}/status` | 会员冻结/解冻/拉黑/偏好保存 100% 404；营销风控动作全部失效，报错文案是通用「接口不存在」 | 后端补 4 个端点；或前端改调 `PUT /{id}/status` + body `{status}` |
| H5 | PUT `/v1/hr/config/sick-pay`、`/deduction`、`/overtime`、`/tax` | `frontend/src/api/hr/config.ts:225, 237, 249, 261` | `backend/src/main/java/com/foodtraceability/controller/HrStubController.java:34, 55, 84, 103, 122`（四条路径均为 `@GetMapping`，整类无 PUT） | **方法不匹配**：前端 PUT 保存，后端只有 GET | 4 类 HR 配置无法保存（405）；前端把值写进内存变量（`config.ts:210-213`）制造「保存成功」假象，刷新即回滚 | 后端补 `@PutMapping`；或 UI 明确禁用并提示 |
| H6 | POST `/v1/sys-config/batch` | `frontend/src/api/system/sys-config.ts:73-75`（`post('/v1/sys-config/batch', items)`） | `backend/src/main/java/com/foodtraceability/controller/SysConfigController.java:140`（`@PutMapping("/batch")`） | **方法不匹配** | 批量改配置 405，系统参数批量维护不可用 | 前端改 put（最小改动），或后端加 `@PostMapping` 兼容 |
| H7 | POST `/v1/coupons/issue`、`/batch-issue`、GET `/v1/coupons/member/{id}`、POST `/v1/coupons/{id}/redeem` | `frontend/src/api/marketing/coupons.ts:27, 32, 37, 42` | `backend/src/main/java/com/foodtraceability/controller/CouponController.java:29`（`/v1/coupons`）；实际只有 `/templates`、`/templates/{templateId}`、`/page`、`/{couponId}`、`/receive`、`/member/{memberId}/available`、`/member/{memberId}/all`、`/{couponId}/use`、`/{couponId}/void`、`/expire`（:41-228） | **路由不存在 + 方法不匹配 + 语义错配**（redeem vs use） | 发券、批量发券、查会员券、核销券 4 个动作全失败；营销活动无法落地 | 后端补 issue/batch-issue；前端核销改 `/use`、会员券改 `/available`/`/all` |
| H8 | POST `/v1/salary/batches`、`/{id}/approve`、`/{id}/pay`、`/{id}/cancel`、`/{id}/sync`、GET `/{id}/records`、`/statistics` | `frontend/src/api/hr/salary.ts:389, 398, 407, 416, 425, 379, 434`（每处自带 TODO 注释，说明作者已知后端缺失） | `backend/src/main/java/com/foodtraceability/controller/SalaryBatchController.java:21`（`/v1/salary/batches`）；仅 `POST /generate`、`GET /`、`GET /{id}`（:43,57,78） | **端点族缺失 + 方法不匹配**：后端没有批次实体 CRUD，只有「按年月生成」 | 薪资账期创建/审核/发放/撤销/同步/明细/统计 7 个操作全不可用，薪资闭环断裂 | 后端补 SalaryBatch 控制器族；短期前端禁用入口，避免用户误以为可发薪 |
| H9 | POST `/v1/purchase/stockins` | `frontend/src/api/purchase/stockin.ts:244`（`warehouseId: form.deliveryWarehouse ? Number(form.deliveryWarehouse) : undefined`） | `backend/src/main/java/com/foodtraceability/dto/PurchaseStockinCreateDTO.java:29-31`（`@NotNull(message = "仓库ID不能为空") private Long warehouseId`） | **必填字段可能不传**：前端显式允许 undefined（注释说明「不再默认主仓库」） | 上游未带仓库 → 400；视图层把异常吞成「创建失败」，用户不知缺哪个字段 | 前端提交前硬校验并给字段级错误；后端 400 返回字段名 |
| H10 | 同上 | `frontend/src/api/purchase/stockin.ts:245`（`stockinDate: form.stockinDate || null`） | `.../dto/PurchaseStockinCreateDTO.java:36-39`（`@NotNull(message = "入库日期不能为空") private LocalDate stockinDate`） | 同上：空日期送 null | 表单未填日期即 400 | 前端默认今天或提交前校验 |
| H11 | GET `/v1/pos/members/search`、POST `/v1/pos/members/quick-register`、POST `/v1/pos/members/checkout` | `frontend/src/api/marketing/pos/members.ts:10, 20, 35` | `backend/src/main/java/com/foodtraceability/controller/pos/MemberController.java:40`（`/v1/pos/members`）；实际仅 `GET /`、`GET /{id}`、`GET /phone/{phone}`、`GET /member-no/{memberNo}`、`GET /top`、`/export`、`/stats`、`/overview`、`POST /`、`PUT /{id}`、`POST /{id}/recharge`、`POST /{id}/consume`、`DELETE /{id}` | **路由不存在** | POS 会员手机号检索、快速注册、积分结账三个高频动作全 404 | 前端改用 `GET /phone/{phone}` 与 `POST /{id}/consume`；后端补 search/quick-register/checkout |
| H12 | POST `/v1/schedule/engine/generate-and-save`、GET `/templates`、GET `/unavailable` | `frontend/src/api/schedule/engine.ts:48-51, 60-62, 77-80` | `backend/src/main/java/com/foodtraceability/controller/schedule/ScheduleEngineController.java:37`（`/v1/schedule/engine`）；实际仅 `POST /generate`、`GET /preview/{planId}`、`GET /progress/{planId}`、`GET /history/{planId}`（:51,68,86,103） | **路由不存在** | 一键自动排班、模板选择、员工不可用时段 3 个能力全 404，智能排班不可用 | 前端 `generate-and-save` → `/generate`（注意返回结构差异）；后端补 templates/unavailable |
| H13 | GET `/v1/roles/list`（对照 `/v1/users/list`） | `frontend/src/api/system/role.ts:81-94`（`status?: number | ''`）；同语义的 `user.ts:107-108` 已按字符串 | `backend/src/main/java/com/foodtraceability/controller/RoleController.java:150-155`（`@RequestParam String status`，注释「active-启用 / inactive-禁用」）、`UserController.java:86-96` | **同名字段类型语义不同**：角色状态后端要 `active/inactive`，前端传 `1/0` | 角色列表状态筛选静默失效（返回全量且不报错），管理员误判筛选生效；与用户列表实现不一致 | 前端 role.ts 改发字符串；或后端兼容数字入参 |
| H14 | GET `/v1/store-management/settlements`、`/{id}` | `frontend/src/views/store-ops/StoreDailySettlement.vue:96-99`（`r.refundAmount` / `r.totalRevenue` 直接参与 reduce）、`frontend/src/types/store-operation/settlement.ts:21,25-40`（要求 confirmTime / refundAmount / totalDiscountAmount / auditTime） | `.../vo/DailySettlementVO.java:50-207` 字段清单：**无** `confirmTime`、**无** `updateTime`、**无** `refundAmount`；只有 `auditTime`、`refundAmountDisplay`、`totalDiscountAmountDisplay`、`cancelledAmountDisplay` | **响应字段缺失 + 命名不一致** | 「日结时间」列永远空（`backend.confirmTime` 为 undefined）；退款/优惠统计恒 0（`todayRefund` 永远 0） | 后端 VO 增 confirmTime 或前端改读 auditTime / *Display 字段 |

---

## 3. 中低危问题表

| # | 端点 | 前端证据 | 后端证据 | 问题 | 影响 | 建议 |
| --- | --- | --- | --- | --- | --- | --- |
| M1 | POST `/v1/over-age-workers/{id}` | `frontend/src/api/hr/over-age-worker.ts:104` | `backend/src/main/java/com/foodtraceability/controller/OverAgeWorkerController.java:108`（`@PutMapping("/{id}")`；类映射 :25） | 方法不匹配 | 超龄劳动者记录无法更新（405） | 前端 post → put |
| M2 | POST `/v1/over-age-workers/{id}/delete` | `frontend/src/api/hr/over-age-worker.ts:109` | 同上，仅 `@DeleteMapping("/{id}")`（:141） | 路由不存在 | 删除 404 | 前端改 `del('/v1/over-age-workers/' + id)` |
| M3 | GET `/v1/finance/reports/balance-sheet` | `frontend/src/api/finance/report.ts:71-74`（已标 TODO P1） | `backend/src/main/java/com/foodtraceability/controller/finance/ReportController.java:21`（`/v1/finance/reports`）；仅 profit-statement、income-expense-summary、receivable-statistics、payable-statistics、aging-analysis、cost-structure、budget-execution（:31-77） | 路由不存在 | 资产负债表 404，报表页空 | 后端补；或前端隐藏入口 |
| M4 | GET `/v1/finance/reports/cash-flow` | `frontend/src/api/finance/report.ts:87-90`（已标 TODO P1） | 同上 | 路由不存在 | 现金流量表 404 | 同上 |
| M5 | GET `/v1/analytics/customer/segments` | `frontend/src/api/marketing/customer-analysis.ts:17` | `backend/src/main/java/com/foodtraceability/controller/CustomerAnalysisController.java:21`（`/v1/analytics/customer`）；仅 member-growth-trend、report、retention-cohort、rfm-distribution、segment-insight、clv-forecast、reports（:37-155） | 路由不存在 | 客群分析 404 | 前端改 `/segment-insight` |
| M6 | GET `/v1/analytics/customer/churn` | `frontend/src/api/marketing/customer-analysis.ts:27` | 同上 + `MarketingAnalysisController.java:57`（`/churn-risk`） | 路由不存在 | 流失分析 404 | 前端改 `/v1/marketing/analysis/churn-risk` |
| M7 | GET `/v1/marketing/analysis/campaigns`、`/channels`、`/trends` | `frontend/src/api/marketing/marketing-analysis.ts:17, 22, 27` | `backend/src/main/java/com/foodtraceability/controller/MarketingAnalysisController.java:19`；仅 overview、rfm/calculate、rfm/distribution、churn-risk、high-value、clv-ranking、**trend**（单数）、churn-warning/process（:29-98） | 路由不存在（复数 vs 单数） | 营销三张图全 404 | trends → trend；campaigns/channels 需后端补 |
| M8 | GET `/v1/recruitment-requirements/statistics` | `frontend/src/api/hr/recruitment.ts:375` | `backend/src/main/java/com/foodtraceability/controller/RecruitmentRequirementController.java:20`；仅 `GET /`、`/{id}`、`POST /`、`PUT /{id}`、`DELETE /{id}`、`POST /{id}/approve`、`/store/{storeId}`、`/department/{departmentId}`、`PUT /{id}/resubmit`（:35-121） | 路由不存在 | 招聘统计卡 404 | 后端补；或前端本地聚合 |
| M9 | GET `/v1/inventory/location/{id}` | `frontend/src/api/warehouse/inventory.ts:233` | `backend/src/main/java/com/foodtraceability/controller/InventoryLocationController.java:24`（`@RequestMapping("/v1/inventory-locations")`） | 路径前缀不一致（`/inventory/location` vs `/inventory-locations`） | 库位详情 404 | 前端改 `/v1/inventory-locations/{id}` |
| M10 | POST `/v1/store-management/settlements` | `frontend/src/views/store-ops/StoreDailySettlement.vue:188-192`（只发 storeId/settlementDate/remark）、`frontend/src/api/store-ops/settlement.ts:103`（`data: unknown`，零类型约束） | `backend/src/main/java/com/foodtraceability/dto/store/operation/DailySettlementCreateDTO.java:20-35`（`@NotBlank storeId`、`@NotNull settlementDate`、`@Positive String totalRevenue`） | **契约缺字段 + 校验注解类型用错**：`@Positive` 施加在 String 上，一旦补传该字段，Hibernate Validator 会抛 UnexpectedTypeException → 500 | 现在靠「不传」侥幸不炸；一旦补传即 500；`data: unknown` 让 TS 完全无法发现 | 改为 `@DecimalMin("0")` + `BigDecimal/String`；前端补 CreateForm 类型 |
| M11 | POST `/v1/store-management/settlements` | `StoreDailySettlement.vue:189`（`storeId: Number(settlementForm.storeId)`） | `.../DailySettlementCreateDTO.java:20-22`（`@NotBlank(message="门店ID不能为空") private String storeId`；`@Schema example = "store001"`） | 类型不匹配（number → String），且示例值本身含字母 | 纯数字仓库靠 Jackson 数字转字符串勉强通过；若门店编码是 `store001`（正是后端示例），`Number()` 直接 NaN → null → 400 | 前端直传字符串，去掉 Number() |
| M12 | POST `/v1/store-management/settlements/{id}/report-issue` | `frontend/src/api/store-ops/settlement.ts:143-147`（发 `{issueType, description}`） | `backend/src/main/java/com/foodtraceability/controller/StoreManagementSettlementController.java:116-124`（`@RequestBody @Valid Object dto`）+ `.../service/impl/DailySettlementServiceImpl.java:294-298`（`// TODO: 根据实际IssueReportDTO结构设置异常详情`） | **前端字段被整体丢弃**（多余字段静默忽略的典型） | 上报异常时原因/类型不入库，异常单据无说明，事后不可追溯；接口却返回 200 | 定义 `IssueReportDTO{issueType, description}` 并落 remark |
| M13 | PUT `/v1/purchase/stockins/{id}` | `frontend/src/api/purchase/stockin.ts:343-350`（只发 qualityRemark/status/stockinType/stockinDate） | `backend/src/main/java/com/foodtraceability/controller/PurchaseStockinController.java:133-158`（`@RequestBody Map<String,Object>` 手工取 4 个 key） | 弱契约（Map 传参，无 DTO 校验） | 多发/少发字段都不报错，后续新增字段极易静默丢失 | 抽 `PurchaseStockinUpdateDTO` + `@Valid` |
| M14 | GET 分页接口（`/v1/purchase/orders`、`/v1/purchase/stockins`、`/v1/store-management/settlements` 等） | `frontend/src/api/purchase/stockin.ts:76-82`（`total: number; current: number; size: number`）、`purchase/order.ts:333`、`store-ops/settlement.ts:72-75` | `backend/src/main/java/com/foodtraceability/config/JacksonConfig.java:51-54`（Long/long 全局 ToStringSerializer）+ controller 直接返回 `Page<T>`（如 `PurchaseStockinController.java:40-51`） | **分页元数据疑似被序列化为字符串**：MyBatis-Plus Page 的 total/size/current 是 long 基本类型，命中全局 Long→String | `res.total` 实为 `"123"`；el-pagination 拿到字符串 total 会页数计算异常（见 Q1，需运行确认） | 保留 Long→String（ID 需要），对 IPage 单独用 NumberSerializer 或返回 `PageVO{Number total}` |
| M15 | POST `/v1/purchase/orders` | `frontend/src/api/purchase/order.ts:255`（`unit: item.unit`，可能 undefined） | `backend/src/main/java/com/foodtraceability/dto/PurchaseOrderItemDTO.java:39-42`（`@NotBlank(message="单位不能为空") private String unit`） | 必填字段可能不传 | 明细未带单位整单 400 | 前端建单前校验，或后端从物料主数据补齐 |
| M16 | POST `/v1/hr/recruitment-requirements/{id}/approve`（前端用于 publish / close / cancel 三语义） | `frontend/src/api/hr/recruitment.ts:153-157, 160-164, 171-175` | `backend/src/main/java/com/foodtraceability/controller/RecruitmentRequirementController.java:86-93`（`@RequestParam String approvalStatus`，无枚举校验） | 语义复用 + 参数无白名单 | 传任何字符串都 200，状态机可能被写脏（如 close 传成 published） | 后端加枚举校验；前端补 cancel 端点后切回 |

---

## 4. 类型 / 精度专项

### 4.1 雪花 ID（19 位 Long）

**后端已做全局防护**：`backend/src/main/java/com/foodtraceability/config/JacksonConfig.java:51-54` 注册 `Long.class` 与 `Long.TYPE` 的 `ToStringSerializer`，因此**响应里的 Long 一律以字符串返回**，前端只要不做数值运算即安全。风险集中在「前端自己转 Number」和「TS 类型写成 number」。

| # | 位置 | 证据 | 风险 |
| --- | --- | --- | --- |
| T1 | 雪花实体清单 | `@TableId(... IdType.ASSIGN_ID)` 共 **58** 个（如 `entity/Order.java:20`、`entity/DailySettlement.java:23`、`entity/RefundRequest.java:26`、`entity/Invoice.java:18`、`entity/Employee.java:26`、`entity/ApprovalWorkflow.java:23`、`entity/Budget.java:21`）；全局默认 `id-type: ASSIGN_ID`（`backend/src/main/resources/application.yml:71`）；其余 287 个为 `IdType.AUTO` | 基准清单 |
| T2 | `Number(*Id)` 转换点 | 全仓 **101** 处（`frontend/src`，例：`api/warehouse/inventory.ts:118,140,151`、`api/asset/asset.ts:164,176`、`api/receipt-confirmation.ts:194,215`、`api/purchase/stockin.ts:228`） | 目标多为 AUTO 实体（`entity/PurchaseOrder.java:22`、`entity/Supplier.java:22`、`entity/PurchaseStockinItem.java:22`、`entity/Member.java:9`），**当前安全**；但任一实体切 ASSIGN_ID 即精度丢失，属定时炸弹 |
| T3 | POS 会员结账 | `frontend/src/api/marketing/pos/members.ts:33-36`（`checkout(data: { memberId: number; orderId: number; ... })`） | `entity/Order.java:20` 是 ASSIGN_ID，订单 ID 为 19 位。若调用方 `Number(orderId)`，末位被截断 → 结错单（见 Q4） |
| T4 | ID 类型声明与实现相反 | `frontend/src/types/finance.ts:1626,1632`（`payableId: number`、`bankAccountId: number`，注释「与后端 Long 保持一致」）；对照正确写法 `frontend/src/types/store-operation/settlement.ts:8,47`（`settlementId: string`） | 注释与事实相反（后端 Long 已序列化为 string）。声明 number 会让运行时字符串与 TS 类型不一致，`id === 123` / `id > 0` 类判断失真 |
| T5 | 单值 ID 出入口 | `frontend/src/api/marketing/member.ts:187`（`freeze(id: string)`，✔ 正确）；`frontend/src/api/marketing/pos/members.ts:14`（`getById(id: number)`，✘ member_id 实为 AUTO 小整数，属侥幸正确） | 建议统一约定：所有 `*Id` 入参用 string，展示层不做数值运算 |

### 4.2 金额单位（分 vs 元）

| # | 位置 | 证据 | 结论 |
| --- | --- | --- | --- |
| T6 | **三套单位并存** | 分：`PurchaseStockinItemBackend.unitPrice`（`frontend/src/api/purchase/stockin.ts:42-43`，注释「分」）、`backend/.../dto/finance/PaymentCreateDTO.java:25-27` + 前端注释 `api/finance/payment.ts:53`；元：`backend/.../vo/DailySettlementVO.java:68-73`（「单位：元」String）；写入口收元：`backend/.../dto/store/operation/DailySettlementCreateDTO.java:29-35` | 同一业务对象「读用元、写收元、库里存分」，前端到处撒 fenToYuan → H1 的二次换算正是这样产生的 |
| T7 | 报表用「分」 | `frontend/src/api/finance/report.ts:68, 85, 99`（注释「金额单位为分，View 层通过 fenToYuan 转换」） | 报表与日结两套单位，跨模块复用同一转换器必然出错 |
| T8 | 浮点串精度 | `backend/.../dataservice/impl/DailySettlementDataServiceImpl.java:192`（`String.valueOf(fen / 100.0)`） | 产出 `15800.5` / `0.30000000000000004` 类串，前端 `Number()` 后再格式化会抖动；建议 `BigDecimal.movePointLeft(2).setScale(2)` + `toPlainString()` |

### 4.3 日期 / 时间格式

| # | 位置 | 证据 | 结论 |
| --- | --- | --- | --- |
| T9 | 后端输出格式 | `backend/.../config/JacksonConfig.java:32-46`：LocalDateTime 统一 `yyyy-MM-dd HH:mm:ss`（**空格**分隔） | 与 VO 文档示例 `2026-05-12T01:00:00`（`DailySettlementVO.java:118, 163`）自相矛盾，前端按示例理解会解析失败 |
| T10 | 前端解析该格式 | `frontend/src/views/store-ops/StoreCertificate.vue:384, 519, 547, 574, 605, 637`（`new Date(b.updateTime).getTime()`）、`views/store-ops/StorePendingTasks.vue:163`、`views/hr/HRKnowledgeBase.vue:133` | 空格分隔在 Safari/iOS 下 `new Date()` 返回 Invalid Date → 排序失效/日期错乱；PC Chrome 正常，故长期未暴露 |

---

## 5. 待人工确认清单

> 静态阅读无法闭环（涉及运行时序列化 / 浏览器行为 / service 与 SQL 实现），按纪律不写入问题表。

| # | 事项 | 已掌握证据 | 为何需人工确认 |
| --- | --- | --- | --- |
| Q1 | `Page<T>` 的 total/size/current 是否真以字符串返回（决定 M14 严重级） | `JacksonConfig.java:51-54` + MyBatis-Plus Page 字段为 long 基本类型（库代码，不在本仓） | 需实抓一次 `GET /api/v1/purchase/orders` 响应体，确认是否 `"total": "37"` |
| Q2 | `@Positive` 施加于 `String totalRevenue` 是否 500 | `DailySettlementCreateDTO.java:33-35` | HV 对非 null String 会抛 HV000030，需运行确认；当前前端不传该字段故未触发 |
| Q3 | `PUT /v1/hr/config/*` 是否有父类/网关兜底 | 全仓 grep `/config/sick-pay` 仅命中 `HrStubController.java:55` | 需启动后 OPTIONS 探测确认无其它映射 |
| Q4 | POS 结账 `orderId` 是否被 `Number()` 处理 | `api/marketing/pos/members.ts:34` 仅声明类型，实际值来自视图层 | 需追 `views/pos/**` 调用点；若直传字符串则 T3 降级为低危 |
| Q5 | 日结列表 `records[]` 是否填充 shifts / paymentBreakdown | `DailySettlementVO.java:172-207` 有该字段，填充取决于 service | 影响 H14 中「支付明细为空」的判定 |
| Q6 | `approvalStatus` 允许值集合 | `RecruitmentRequirementController.java:86-93` 无枚举校验 | 需读 `RecruitmentRequirementServiceImpl.approveRequirement` 的字符串分支 |
| Q7 | 8 条脚本误报是否随脚本升级复发 | 判定依据见 6.4 | 若改用 AST 解析需回归确认（当前为字符串/正则启发式） |
| Q8 | `/v1/coupons/member/{id}` 与 `/member/{memberId}/all` 语义是否等价 | `CouponController.java:186-201` | 需产品确认「会员持券列表」语义，再决定是改前端还是补后端 |

---

## 6. 复现方法

所有结论均由以下可重复的静态扫描产生。扫描脚本为本次一次性临时产物，已清理（未留在仓库中）；按下列正则与片段可在 10 分钟内重建等价扫描器，产物 JSON 格式在每小节末尾注明。

### 6.1 后端端点抽取

- 范围：`backend/src/main/java/com/foodtraceability/controller/**/*.java`（269 文件）
- 类级前缀：正则 `@RequestMapping\s*(?:\(([^)]*)\))?`，取 `class \w+` 声明之前最后一个匹配
- 方法级：正则 `@(Get|Post|Put|Delete|Patch|Request)Mapping\s*(?:\(([^)]*)\))?`，跳过 classIdx 之前（类级）命中；`@RequestMapping` 再解析 `RequestMethod\.(\w+)`
- 路径归一：模板占位 ${...} 归一为 `{}`，路径占位 `{var}` 归一为 `{}`，去尾部 `/`
- 参数：签名括号内按顶层逗号切分（跟踪 `()<>[]` 深度），识别 `@RequestParam`（value|name 缺省取变量名，解析 defaultValue / required=false）、`@PathVariable`、`@RequestBody`（记录 DTO 类型与 `@Valid`）
- 产物：`tmp/_be_api.json`（2057 条，含 path/methods/file/line/params）

关键片段：

```js
const classIdx = src.search(/\b(public\s+)?(abstract\s+)?class\s+\w+/);
const cm = [...src.slice(0, classIdx).matchAll(/@RequestMapping\s*(?:\(([^)]*)\))?/g)];
const clsPrefix = (cm.length ? (cm[cm.length-1][1] || '').match(/"([^"]*)"/) || ['',''] : ['',''])[1];
const re = /@(Get|Post|Put|Delete|Patch|Request)Mapping\s*(?:\(([^)]*)\))?/g;   // 命中 index > classIdx 才计入方法
```

### 6.2 前端调用点抽取

- 范围：`frontend/src/api/**/*.ts|js` + `frontend/src/**/*.vue`，排除 `node_modules/dist/__tests__/mock/assets`
- 正则：`\b(silentGet|silentPost|silentPut|silentDel|silentPatch|get|post|put|del|patch|delete|request\.(get|post|put|delete)|axios\.(get|post|put|delete))\s*(?:<[^>()]*>)?\s*\(\s*([\x60'"])`
- 路径截到引号/反引号为止；模板占位 ${...} 归一为 `{}`；同文件 `const X = '/v1/...'` 会展开；查询串 `?query` 截断；`'/path/' + id` 形态补占位
- 丢弃注释区命中（`/** */` 与 `//`），避免把文档示例当真实调用（例：`api/hr/converters.ts:17` 的示例路径 `/v1/hr/employees/1` 因此被排除）
- 产物：`tmp/_fe_calls.json`（944 条）

### 6.3 配对与比对

```js
const segs = p => String(p).split('?')[0].split('/').filter(Boolean);
const norm = p => segs(p).map(s => /^\{.*\}$/.test(s) ? '{}' : s.toLowerCase()).join('/');
// 路径命中后，再要求后端 methods 数组包含前端 method（或 ALL），否则记为 method-mismatch
```

结果：匹配 898 / 路径缺失 28 / 方法不符 18。

分页专项（三套键并存，见 6.5）：

```js
const has = (e, n) => e.params.some(p => p.kind === 'query' && p.name === n);
// current/size 86 条、page/size 82 条、pageNum/pageSize 24 条
```

必填字段专项（**启发式，误报率高，未计入问题表**）：解析 `dto/**/*.java` 中带 `@NotBlank|@NotNull|@NotEmpty` 的 `private T name;` 字段，用词边界正则在调用文件全文里找；22 条候选逐条人工复核后全部排除（字段常声明在 `types/**` 而非 api 文件）。

### 6.4 脚本误报的判定规则（重要）

下列 8 条曾被脚本判为「方法不匹配」，人工确认为**误报**，根因是 `'/v1/x/' + id + '/sub'` 被截成父路径：

| 前端证据 | 实际全路径 | 后端存在性 |
| --- | --- | --- |
| `api/hr/recruitment.ts:299` | `PUT /v1/interviews/{id}/result` | `InterviewController.java:65` ✔ |
| `api/hr/recruitment.ts:316` | `POST /v1/interviews/{id}/hr-interview` | `InterviewController.java:122` ✔ |
| `api/hr/recruitment.ts:326` | `PUT /v1/interviews/{id}/hr-evaluation` | `InterviewController.java:141` ✔ |
| `api/hr/recruitment.ts:355` | `PUT /v1/job-offers/{id}/accept` | `JobOfferController.java:127` ✔ |
| `api/hr/recruitment.ts:358` | `PUT /v1/job-offers/{id}/reject` | `JobOfferController.java:146` ✔ |
| `api/hr/recruitment.ts:361` | `POST /v1/job-offers/{id}/convert-to-onboarding` | `JobOfferController.java:186` ✔ |
| `api/marketing/points.ts:54` | `PUT /v1/points/config/{ruleKey}` | `PointsController.java:113` ✔ |
| `api/marketing/member.ts:188` 等 `'/v1/x/' + id` 形态 | 归一路径后与后端一致 | — |

复核方法：对任一行执行

```bash
rg -n "@(Get|Post|Put|Delete)Mapping" backend/src/main/java/com/foodtraceability/controller/<Controller>.java
```

打印该类全部映射，再与前端拼接后的完整字符串逐段对比。

### 6.5 三套分页参数并存（未判为缺陷，建议统一）

| 约定 | 端点数量 | 示例 |
| --- | --- | --- |
| `current` + `size` | 26 / 86 | `GET /v1/purchase/stockins`（`PurchaseStockinController.java:41-42`）、`PurchaseOrderQueryDTO.java:78-84` |
| `page` + `size` | 82 | `GET /v1/recharge-records`（`controller/marketing/RechargeRecordController.java:108-109`）、`GET /v1/roles/list`（`RoleController.java:150-151`） |
| `pageNum` + `pageSize` | 24 | `GET /v1/users/list`（`UserController.java:86-87`）、`GET /v1/purchase/returns/page`、`GET /v1/device-status-history` |

结论：已配对的 898 个调用里，前端发送的分页键与后端接收键**是一致的**（分页键专项检测 0 例外），因此不构成线上故障；但契约层没有统一分页对象，新增端点极易写错。建议统一 `PageQuery{current,size}` 并保留旧名兼容。

---

## 7. 逐字段比对覆盖的 46 个端点

| 业务簇 | 端点 | 结论 |
| --- | --- | --- |
| 日结（7） | `GET /v1/store-management/settlements`、`/{id}`、`/{id}/shifts`、`POST /`、`PUT /{id}/confirm`、`POST /{id}/report-issue`、`GET /export` | H1 / H2 / H3 / H14 / M10 / M11 / M12 |
| 采购入库（9） | `GET /`、`GET /{id}`、`POST /`、`PUT /{id}`、`DELETE /{id}`、`PUT /{id}/start-inspection`、`/{id}/quality-check`、`/{id}/confirm`、`POST /{id}/void` | H9 / H10 / M13；质检 DTO 字段完全对齐（`PurchaseQualityCheckDTO.java:18-99` ↔ `stockin.ts:391-403`，11 个字段逐一命中，无多余/缺失） |
| 采购订单（14） | `GET /`、`GET /{id}`、`POST /`、`PUT /{id}`、`/{id}/submit`、`/approve`、`/confirm`、`/reject`、`/cancel`、`/terminate`、`/freeze`、`/unfreeze`、`DELETE /{id}`、`GET /statistics` | 路径 + 方法 + query 参数（`reason` / `approvalRemark`）全部对齐（`PurchaseOrderController.java:160,179,206,220,234` ↔ `order.ts:416,425,434,443,452`）；仅 M15 单位必填风险 |
| 权限（4） | `GET /v1/roles/list`、`GET /v1/roles/all`、`GET /v1/users/list`、`GET /v1/users/{id}` | H13（其余对齐） |
| POS 会员（6） | `GET /v1/pos/members/search`、`/{id}`、`/{id}/points`、`/{id}/coupons`、`POST /quick-register`、`/checkout` | H11 + T3 |
| 排班引擎（3） | `POST /generate-and-save`、`GET /templates`、`GET /unavailable` | H12 |
| 财务（5） | `GET /v1/finance/reports/profit-statement`、`/balance-sheet`、`/cash-flow`；`POST /v1/finance/payments`、`/receipts` | M3 / M4；付款/收款 DTO 字段对齐（`PaymentCreateDTO.java:21-42` ↔ `types/finance.ts:1624-1637`；`ReceiptCreateDTO` ↔ `types/finance.ts:1703-1716`） |
| 优惠券（7） | `GET /templates`、`POST /templates`、`PUT /templates/{id}`、`POST /issue`、`/batch-issue`、`GET /member/{id}`、`POST /{id}/redeem` | H7（前三条对齐） |

合计 46 个端点（含 4 个对照端点），覆盖订单 / 库存 / 采购 / 财务 / 日结 / 权限 / POS / 排班 8 个业务簇。

---

## 8. 本次未做（边界说明）

- 未启动后端、未连库、未发 HTTP：全部为静态推断；标注 Q1–Q8 的项需联调验证后才可定级。
- 未覆盖 `employee-frontend`、`frontend-pos`、`frontend-kitchen`、`frontend-design-*`、`miniprogram` 等子前端（任务范围限定 `frontend/`）。
- 未做鉴权矩阵比对（`@PreAuthorize` ↔ 前端路由/按钮权限），仅记录方法签名，可作任务 B 输入。
