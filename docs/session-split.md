# 会话拆分交接文档（2026-08-01）

> 目的：将当前项目工作拆分为**两个独立会话窗口**，避免上下文混淆。本文件是两边的公共交接信息。
> 背景：工作分为两件事——①**数据链修复**（P0 数据正确性，业务链打通）；②**复核数据链时发现的问题修复**（页面缺陷/体验/契约）。
> 本文档状态更新至：2026-08-01（批 2/批 3 完成）。

---

## 0. 公共环境（两个窗口共用）

| 项 | 值 |
|---|---|
| 前端 dev | `http://localhost:3002`（`frontend/logs/start-frontend.ps1`） |
| 后端 | `http://localhost:8081/api`，健康检查 `/api/actuator/health`（`backend/logs/start-new-backend.ps1`） |
| 登录 | admin / `Admin@123` |
| 数据库 | PostgreSQL 5432，库 `food_traceability`，postgres / 123456（psql 在 `P:\my-new-project\PostgreSQL\18\bin\psql.exe`，注意 SQL 写 UTF8 文件执行、输出重定向读文件避免编码乱码） |
| 编译/构建 | 后端 `backend/` 下 `mvn compile`（JDK21=`P:\my-new-project\JDK21`）；前端 `frontend/` 下 `npm run build` |
| 业务模式 | 集中式单店（centralized-single）；admin/owner 可见 采购+财务，店长/员工/运营不可见 |
| 关键坑 | 订单页路由是 **`/purchase/orders`（复数）**；psql 直接 -c 传中文会编码错，必须写文件 |
| Playwright 验证 | 脚本放 `C:\Users\Liberty\AppData\Local\Temp\opencode\*.cjs`，`NODE_PATH=frontend/node_modules` + `channel:'msedge'`，登录流程=填 admin/Admin@123 点登录 |

---

## 1. 窗口 A：数据链修复（P0 数据正确性）

### 1.1 已完成（会话批 1 + 本窗口延续项）
- F1-F6 资金链：应收状态统一 1未收/2部分收/3已核销/4逾期；报销单单一状态源；删除 Mock 总账控制器；未付清订单完成自动生成应收（`createForOrderByNo` 幂等）；采购结算付款联动应付/付款单（`linkPaymentToPayables`）；订单支付落资金流水
- SR-9：centralized-single 按角色开放 finance+purchase（V20260731_007 + permission.ts）
- T1-1：material_trace_code 三列 bigint→VARCHAR（V20260731_008）
- P2-1：store_inventory_log 全路径流水 + DECIMAL(12,3)（V20260731_009）
- 方案 D+/E：库存决策看板（`GET /{dishId}/forecast` + `/variance-analysis`，自校准实际每份用量）
- 直收移除：业务链收敛为 采购申请→计划→订单→到货→收货确认（无 arrivalId 请求被拒）
- 采购业务链衔接（批 3）：申请→订单跳转定位、计划→订单接口（幂等）、订单 requestNo 过滤
- 字典表 V20260731_010；菜单权限模板全局化（initFromToken + 路由守卫兜底）

### 1.2 待办（本窗口继续）
| 编号 | 任务 | 关键文件/方向 |
|---|---|---|
| M1 | 会员余额/消费金额 分→元 转换 | `api/marketing/member.ts` converter |
| M2 | 储值退款扣减会员余额 | `RefundServiceImpl.executeRefund` 补 `addBalance` 负数 |
| M3 | 会员统计接口真实聚合 | `MarketingMemberController.stats/overview` |
| M4 | 会员列表分页契约（后端返 IPage） | `MarketingMemberController` L57-60 |
| M5 | 会员状态/性别/渠道编码转换 | `api/marketing/converters.ts` 挂到 getList |
| M6 | 充值记录 paymentStatus 补 partial_refunded/success | `types/member.ts` + converter |
| T1 | 原料追溯码 materialId 类型/字段对齐 + 展示字段补齐 | `MaterialTraceCode` 实体/页面（inboundTime/unitPriceYuan 等） |
| T2 | 食品追溯码 JSON 结构细化 | `FoodTraceCode` + TS 类型 `Array<{traceCode;materialId;materialName;quantity;unit}>` |
| T3 | 供应商追溯召回 Tab 填充 recalls | `SupplierTraceServiceImpl` setRecalls |
| R1 | 召回状态三套表达统一 + 回写原料追溯码状态 | `RecallRecord` + converter + 召回执行 |
| R2 | 追溯模块 PUT body/query 统一（建议 body DTO） | 三处控制器 `@RequestParam`→DTO |
| D1 | 日结状态枚举对齐（后端 String↔前端 number） | `api/store-ops/converters.ts` L113-117 |
| D2 | 日结 refundAmount/confirmTime 补映射/补列 | converter + 迁移加 confirm_time |
| P2-2 | cost_record 关联订单字段 | CostRecordCreateDTO 增 bizId/orderNo |
| P1 | 菜品成本与库存成本联动重算 | 按需触发机制 |
| A4 | 预约到店转订单接口 | TableReservationNewController |

### 1.3 端到端验证清单（逐条跑，通过打勾）
见 `docs/data-chain-action-plan.md` 第七章 7.1~7.4（采购主闭环、销售闭环、追溯链、会员链）。

---

## 2. 窗口 B：复核发现问题修复（页面缺陷/体验/契约）

### 2.1 已完成（批 2/批 3）
- 商品档案：分类过滤（categoryId）、页大小持久化（localStorage `fts-table-page-size`）、加列 条码/产地/保质期/存储条件（V20260731_011）+ 移除 采购价/零售价/会员价/初始库存、序号列、翻页事件修复
- 明细下拉：采购申请/计划 = 商品档案 + 最新单价（latestPriceMap=门店库存 unit_cost）；**菜品管理保持门店库存（正确设计，勿改）**
- 套餐管理：openEdit 补 comboApi.getById（修复编辑空白）、明细加 单价/小计 列（converter 补 unitPrice/subtotal）、列宽固定、对话框 900px
- 定价历史：PricingBackend 字段对齐 oldSalePrice/newSalePrice（原用 oldPrice/newPrice 恒 undefined）
- 转单 500：purchase_request.store_id='624'（脏数据）→'1' + 后端 `resolveValidStoreId` 防御（注入 StoreNewMapper）
- 采购计划：生成计划（generate-from-stock）+ 导出（POI Excel）接口与前端按钮
- 申请/计划"生成订单"跳转订单列表定位（?requestNo=）
- 通用体验：明细表格 `resizable=false`、对话框加宽（申请/计划 1100、菜品 1000）、备注说明 form-section 对齐、onMounted 预加载物料选项
- 审批流程 tab：API 修复（silentGet/Promise.allSettled）、限流排除 /v1/approval/**
- 菜单模板：全局预加载（initFromToken + 路由守卫兜底），模式选择不重置

### 2.2 待办（本窗口继续）
| 任务 | 说明 |
|---|---|
| 按复验清单逐页复核 | 清单见 `docs/data-chain-fix-tracking.md` 批 1 的"需人工复验清单"（采购闭环/资金链/追溯/模式权限/字典/图片/编码/档案） |
| 会员链等页面问题 | 用户在复核中发现的 UI 缺陷继续修复（同本窗口方法论：先定位根因→修复→Playwright 端到端验证） |
| 单元字典（方案B） | 用户已认可但未落地：字典单位约束（V20260731_010 表已建，待接业务） |

### 2.3 验证方法
- 前端缺陷：改完 `npm run build` + Playwright（登录→目标页→断言），注意下拉/对话框用 `:popper-options="{ strategy:'fixed' }"` 与 teleported 规则
- 后端缺陷：`mvn compile` + 重启（Stop-Process 8081 → 起 start-new-backend.ps1 → health UP）→ Invoke-RestMethod 验证

---

## 3. 文档索引（两窗口共享）

- 主计划：`docs/data-chain-action-plan.md`（状态总览/全部 P0 列表/端到端验证清单 7.x）
- 修复流水：`docs/data-chain-fix-tracking.md`（批 1=资金链/决策看板；批 2=复核问题；批 3=业务链衔接）
- 业务链：`docs/data-business-chain.md`（采购→库存→成本→销售 数据流设计）
- 链路图：`docs/data-flow-map.md`
- 字段契约：`docs/frontend-verification-plan.md`
- 项目地图/启动：`docs/project-map.md`

## 4. 已知问题（待决策，两个窗口都可能碰到）

1. 孤儿测试应付：payable_no=AP+确认单ID 撞 7-28 遗留（AP0000000007/8/15/16/17），建议清理
2. 会员消费记录订单源（order_new vs POS）未定
3. 菜单权威源（后端 /v1/auth/menus vs 前端模板）——当前前端模板为准
4. 三个 100% Mock 页面（合同智能/员工画像/知识库智能）去留
5. 排班/叫号/资产调拨 模块归属未定
