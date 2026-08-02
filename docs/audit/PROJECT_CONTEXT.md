# 食品溯源系统 — AI 可读项目上下文文档（V2 深度修订版）

> **文档定位**：面向 AI Agent 的项目认知文档。本版基于 2026-06-25 用户反馈深度修订，修正了模块划分错误，补充了财务专业缺失、人事全链路、门店联动等深层内容。
> **生成时间**：2026-06-25（V2）
> **审查范围**：除"系统管理"模块外的全部 14 个一级业务模块

---

## 0. 一句话项目摘要

食品溯源系统（Food Traceability System，简称 FTS）是一套面向**个体餐饮企业**（含连锁模式）的前后端分离管理系统，覆盖**门店运营、产品、订单、运营决策、采购、仓储、会员、财务、资产、人事、食品追溯、设备、电子签章**等 14 个一级业务域，技术栈为 **Spring Boot 3.2.0 + Vue 3.5 + TypeScript + PostgreSQL/H2 + Redis + RabbitMQ**，菜单按"规模档位"（mini/standard/chain-standard/chain-enterprise）分级显示。

---

## 1. 技术栈锁定版本

| 层 | 技术 | 版本 | 备注 |
|----|------|------|------|
| 后端框架 | Spring Boot | 3.2.0 | JDK 21 |
| ORM | MyBatis Plus | 3.5.5 | @TableLogic 软删除、@Version 乐观锁 |
| 数据库（生产） | PostgreSQL | 18 | snake_case 字段 |
| 数据库（开发） | H2 内存 | MODE=PostgreSQL | 兼容模式 |
| 缓存 | Redis | — | L2 缓存 |
| 消息队列 | RabbitMQ | — | durable + 死信队列 |
| 前端框架 | Vue | 3.5.x | Composition API + `<script setup>` |
| 类型系统 | TypeScript | 5.2.2 | strict 模式开启 |
| 构建工具 | Vite | 5.0.0 | |
| UI 库 | Element Plus | 2.13.x | 已封装 core 层组件 |
| 状态管理 | Pinia | 3.0.x | |
| 路由 | Vue Router | 5.0.x | |
| HTTP 客户端 | Axios | 1.6.2 | 已封装 `request` 实例 |

**关键决策**：
- Lombok 已禁用，手写 getter/setter
- 构造函数注入，禁止 `@Autowired` 字段注入
- 金额单位：后端/数据库以**分**为单位（Long），前端以**元**为单位（string/number）
- 状态字段：前端语义化字符串（`active/inactive/probation`），后端/数据库数字编码（`1/0/2`）
- 菜单不依赖后端 API，由前端 `src/modules/*/menu.ts` 模块化注册（后端 MenuServiceImpl 是遗留硬编码，已不同步）

---

## 2. 项目目录结构（核心）

### 2.1 后端

```
backend/src/main/java/com/example/demo/
├── controller/          # REST 控制器（部分按子包组织，如 finance/、approval/、schedule/）
├── service/             # Service 接口 + impl/ 实现
├── dataservice/         # 缓存+DB 双读层（DataService 接口及实现）
├── mapper/              # MyBatis Plus Mapper
├── entity/              # 数据库实体
├── dto/                 # CreateDTO / UpdateDTO / QueryDTO / BasicInfo / VO
├── config/              # 配置类（Redis/MQ/Security/MybatisPlus）
├── common/              # Result、ErrorCode、PageResponse
├── security/            # JWT、RBAC、MFA
├── exception/           # BusinessException、全局异常处理器
└── utils/               # 工具类
```

### 2.2 前端

```
frontend/src/
├── api/                 # API 封装（按模块分子目录，含 converters）
│   └── request.ts       # 统一 axios 实例（必用，禁止直接 axios）
├── components/
│   ├── core/            # 基础层：StatusTag/DataTable/PageHeader/StatCard/StandardPage...
│   ├── business/        # 业务层：跨模块共享业务组件（ApplicantDetailDrawer 等）
│   └── layout/          # MainLayout / SidebarMenu 等布局组件
├── composables/         # useCrudTable / useForm / useDialog / useStandardPage
├── config/              # baseURL 等配置
├── modules/             # ★ 菜单配置目录（每个一级模块一个 menu.ts）
├── router/              # 路由配置（扁平路由 + modules/schedule.ts）
├── stores/              # Pinia store（含 permission.ts 菜单注册）
├── styles/              # SCSS（含 _element-overrides.scss）
├── types/               # TypeScript 类型定义
├── utils/               # 工具函数（含 fenToYuanNumber / yuanToFen）
└── views/               # 页面视图（按模块分子目录，含 components/）
```

### 2.3 数据库迁移

```
backend/src/main/resources/db/migration/
├── V7.0.0__create_finance_tables.sql              # 财务核心 11 张表
├── V2026032103__create_electronic_voucher_tables.sql  # 电子凭证 9 张表
├── V20260405__auto_vouchering_foundation.sql      # 自动记账 4 张表 + 18 条规则
├── V20260513__create_report_redesign_tables.sql   # 报表重构 3 张表
├── V20260625_001__create_payment_table.sql
├── V20260625_002__create_receipt_table.sql
└── ...                  # Flyway 管理，禁止修改已执行脚本
```

---

## 3. 真实一级模块清单（基于 src/modules/ 注册顺序）

> ⚠️ **重要修正**：菜单由 `src/modules/*/menu.ts` 注册，共 **15 个一级模块**。之前文档错误地将"排班管理/印章管理/供应商门户/自助服务"当作一级模块，实际上：
> - 排班管理 = 门店管理下的二级菜单（`/store-management/shift`）
> - 印章管理 = 电子签章下的二级菜单（`/seal/management`）
> - 供应商门户 = 采购管理下的二级菜单"签署链接"（`/supplier-portal/links`）
> - 自助服务 = 孤立未接入的视图文件，无路由无菜单

| order | 一级模块 | 前端目录 | 后端包 | 可见角色 | 完成度 |
|-------|---------|---------|--------|---------|--------|
| 0 | 工作台 | `views/dashboard/` | — | 全员 | 30% |
| 10 | 产品中心 | `views/product/` | `controller/` 散落 | 全员 | 65% |
| 20 | 订单管理 | `views/order/` | `OrderNewController`（已建未对接） | 全员 | 40% |
| 30 | 运营中心 | `views/operations/` | `controller/operations/` | OWNER/ADMIN/OPS_DIRECTOR | 40% |
| 40 | 门店管理 | `views/store-ops/` | `controller/` 散落 | STORE_MANAGER/TEAM_LEADER/OWNER/ADMIN | 50% |
| 50 | 采购管理 | `views/purchase/` | `controller/purchase/` | 全员 | 35% |
| 60 | 仓储管理 | `views/warehouse/` | `controller/warehouse/` | 全员 | 75% |
| 70 | 会员管理 | `views/marketing/` | `controller/marketing/` | 全员 | 70% |
| 80 | 财务中心 | `views/finance/` | `controller/finance/` | OWNER/ADMIN/FINANCE_DIRECTOR | 70% |
| 90 | 资产管理 | `views/asset/` | `controller/asset/` | OWNER/ADMIN | 85% |
| 100 | 人事管理 | `views/hr/` | `controller/` 散落 | OWNER/ADMIN/HR_DIRECTOR | 50% |
| 110 | 食品追溯 | `views/traceability/` | `controller/traceability/` | 全员 | 90% |
| 120 | 设备管理 | `views/device/` | `controller/` 散落 | OWNER/ADMIN | 60% |
| 125 | 电子签章 | `views/seal/` | —（后端完全缺失） | OWNER/ADMIN/HR/FINANCE/OPS_DIRECTOR | 30% |
| 130 | 系统管理 | `views/system/` | `controller/system/` | OWNER/ADMIN | 75% |

### 3.1 规模档位过滤

二级菜单按 `scaleLevel` 过滤显示：
- `mini`：单店小规模（基础功能）
- `standard`：标准店（+财务报表、食品追溯等）
- `chain-standard`：连锁标准（+预算管理、资金管理、召回管理等）
- `chain-enterprise`：连锁企业（+自动凭证管理、质量追溯、合并报表等）

admin 用户始终可见全部菜单。

---

## 4. 关键架构模式

### 4.1 后端分层架构

```
Controller → Service 接口 → ServiceImpl → DataService(缓存) → Mapper → DB
   |           |                |               |                  |
请求路由    业务接口       业务实现+事务    缓存+DB双读         CRUD
参数校验    事务边界                       批量查询优化
返回 Result 消息发布                       缓存更新策略
```

**强制规则**：
- Controller 禁止编写业务逻辑、禁止直接注入 Mapper
- Service 层负责事务管理（`@Transactional(rollbackFor = Exception.class)`）
- DataService 层负责缓存（L1 Caffeine + L2 Redis）
- 所有业务核心数据使用逻辑删除（`@TableLogic`）

### 4.2 四账联动（财务核心模式）

付款/收款操作触发 4 张表同步写入（同一事务）：

```
Payment/Receipt 创建
   ├── 1. 更新 FinancePayable / FinanceReceivable（应付/应收余额）
   ├── 2. 更新 FinanceBankAccount（银行账户余额）
   ├── 3. 插入 FinanceFundFlow（资金流水）
   └── 4. 创建 FinanceVoucher（记账凭证，状态=暂存）
```

### 4.3 凭证状态机

```
                  审核                  过账
[暂存 0] ──────────→ [已审核 1] ──────────→ [已过账 2]
   ↑                     |                     |
   |     反审核          |                     |
   └─────────────────────┘                     |
   |              反过账                       |
   └───────────────────────────────────────────┘
                         ↓
                    [已作废 3]（仅暂存/已审核可作废，已过账需红字冲销）
```

### 4.4 自动记账规则引擎（已建表未充分使用）

数据库 `V20260405__auto_vouchering_foundation.sql` 已建 4 张表 + 18 条业务规则：
- `accounting_rule`：规则定义表
- `entry_template`：分录模板表
- `auto_voucher_log`：自动记账日志（审计轨迹）
- `voucher_idempotent`：幂等控制表

**18 条规则覆盖**：POS 堂食、美团/饿了么/抖音外卖、生鲜/调料/酒水采购、付供应商款、发工资、计提工资社保、平台结算、成本结转、付房租、付水电、收定金、固定资产购置、月末折旧、月末税费计提。

**问题**：前端 `AutoVoucher.vue` 仅展示凭证模板列表，**无规则配置 UI、无日志查看、无手工触发、无质量审核工作台**。

### 4.5 withMockFallback 模式（重要风险！）

```typescript
return withMockFallback(
  () => silentGet<T>(url, params),    // 先尝试真实后端
  mockData                            // 失败则返回 mock
)
```

**风险**：此模式会**掩盖前后端联调失败**，导致看似前端可用、实则后端未对接。**严重问题**：HR 招聘、订单管理、运营中心、数据看板等模块存在"前端 100% Mock + 后端表已建但未对接"的情况。商用前必须移除。

### 4.6 跨模块事件机制（已设计未实现）

`types/hr/cross-module.ts` 定义了 14 种跨模块事件类型：
- 门店→人事：`attendance_submission` / `recruitment_request` / `health_cert_expense`
- 人事→门店：`employee_onboard` / `employee_resign` / `health_cert_expiry_warning` / `training_completed`

**问题**：`PendingTask` 接口已定义 `sourceModule`/`eventType`/`routePath` 字段，但 `StorePendingTasks.vue` 实际只从 `mockDataCenter` 取数，**跨模块事件流未真正实现**。

---

## 5. API 规范

### 5.1 统一响应格式

```json
{
  "code": 0,           // 0=成功，非0=失败（业务状态码，非 HTTP）
  "message": "操作成功",
  "data": {},
  "timestamp": 1712345678901
}
```

### 5.2 前端请求规范（强制）

```typescript
// ✅ 正确：使用 request 实例，GET 直接传 params
request.get(url, params)
request.post(url, data)

// ❌ 禁止：直接 axios、嵌套 params、访问 response.data
axios.get(url)
request.get(url, { params })
response.data.records  // 响应拦截器已提取 data，直接用 response.records
```

### 5.3 API 路径约定

- 基础路径：`/api`（由 baseURL 配置）
- 模块路径：`/v1/{module}`（如 `/v1/vouchers`、`/v1/employees`）
- 门店管理特殊路径：`/api/v1/store-management/*`（注意前缀不同）
- 状态流转：`POST /v1/{module}/{id}/{action}`（如 `/v1/vouchers/123/approve`）

---

## 6. 前端组件体系（强制使用）

### 6.1 core 层组件清单

| 组件 | 用途 | 替代的 Element Plus |
|------|------|---------------------|
| `StatusTag` | 状态标签 | `el-tag`（显示状态时） |
| `DataTable` | 数据表格 | `el-table` |
| `PageHeader` | 页面头部 | 自定义标题区 |
| `StatCard` | 统计卡片 | 自定义指标卡 |
| `StandardPage` | 标准页面布局 | 自定义容器 |
| `Breadcrumb` | 面包屑 | `el-breadcrumb` |
| `EmptyState` | 空状态 | `el-empty` |
| `Skeleton` | 骨架屏 | `el-skeleton` |
| `Toolbar` | 工具栏 | 自定义工具区 |
| `TableActionBar` | 表格操作栏 | 自定义操作区 |
| `SearchPanel` | 搜索面板 | 自定义搜索区 |

### 6.2 强制规则

1. **状态显示必须用 StatusTag**，禁止 `el-tag` 或三元表达式
2. **操作列必须用 `el-button link`**，禁止 `text` 类型、禁止图标+文字组合
3. **颜色必须用 CSS 变量** `--fts-*`，禁止硬编码（如 `#409EFF`）
4. **禁止 `!important`**，通过选择器特异性解决
5. **Dialog 内的弹出组件必须 `:teleported="false"`**（el-select/el-date-picker/el-cascader 等）

### 6.3 抽屉组件现状（重要）

项目中已存在的 `el-drawer` 用法均为**详情抽屉**（非 AI 对话窗口）：
- `hr/components/ContractDashboardDrawer.vue` - 合同仪表盘抽屉
- `hr/components/ContractTemplateLibraryDrawer.vue` - 合同模板库抽屉
- `business/ApplicantDetailDrawer.vue` - 应聘者详情抽屉
- `order/components/OrderDetailDrawer.vue` / `RefundDetailDrawer.vue` / `ReservationDetailDrawer.vue`
- `operations/AlertCommandCenter.vue` - 预警指挥中心抽屉

**项目目前无抽屉式 AI 对话窗口**。仓储"智能补货建议" `SmartRestock.vue` 实际是**整页组件**（非抽屉），但其多模型对比、置信度展示、模型配置跳转的设计模式可复用为 AI 抽屉窗口的参考。

---

## 7. 数据转换规范

### 7.1 三层命名

| 层 | 风格 | 示例 |
|----|------|------|
| 数据库 | snake_case | `product_name`, `create_time` |
| Java 实体 | camelCase | `productName`, `createTime` |
| 前端 | camelCase | `productName`, `createTime` |

MyBatis-Plus 自动映射（`map-underscore-to-camel-case: true`）。

### 7.2 状态字段映射

| 业务含义 | 前端值 | 后端值 | 数据库值 |
|---------|--------|--------|----------|
| 启用/正常/在职 | `active` | 1 | 1 |
| 禁用/停用/离职 | `inactive` | 0 | 0 |
| 试用/审核中 | `probation` | 2 | 2 |

### 7.3 金额转换

```typescript
// 元转分
yuanToFen("123.45")  // → 12345

// 分转元（返回 number）
fenToYuanNumber(12345)  // → 123.45

// 禁止在组件中本地实现 formatFen，必须用 utils 统一函数
```

---

## 8. 财务模块专业缺失分析（用户重点关注）

### 8.1 财务模块当前已实现

**前端 13 个页面 + 7 个对话框组件**：
- 财务总账（凭证管理）、会计科目、会计期间
- 应收账款、应付账款、成本管理、税务管理（仅税率配置）
- 发票报销、财务报表（仅 3 张主表）、预算管理
- 资金管理（银行账户+流水）、财务审批流配置
- 自动凭证管理（仅规则列表展示）

**后端 21 个 Controller + 22 个 Service + 22 个实体**

**数据库基础设施已远超前端 UI 覆盖能力**：
- V7.0.0：11 张核心表 + 100 个标准会计科目
- V2026032103：9 张电子凭证表（XML/OFD/PDF、验签、时间戳、不相容职务分离）
- V20260405：4 张自动记账表 + 18 条业务规则
- V20260513：3 张报表重构表（report_configs / report_insight_rules / export_tasks）
- V2026033001：发票报销表

### 8.2 缺失的电子自动化方向（用户反馈 #1）

| 缺失项 | 数据库基础 | 前端 UI |
|--------|-----------|---------|
| 电子凭证上传/导入（XML/OFD/PDF 批量） | ✅ electronic_voucher 表已建 | ❌ 无 |
| 电子凭证解析预览（parsed_data 结构化） | ✅ 字段已就绪 | ❌ 无 |
| 验签/验真操作界面（手工+批量） | ✅ voucher_signature_log 表已建 | ❌ 无 |
| 电子凭证→记账凭证关联入账 | ✅ finance_voucher_id 字段已就绪 | ❌ 无 |
| 数电票接收/解析（财会〔2025〕9 号文） | ✅ electronic_invoice 表已建 | ❌ 无 |
| 数电票对接电子税务局 | ❌ | ❌ |
| 销项数电票开具（蓝字/红字冲销） | ❌ | ❌ |
| 银行流水导入（网银 Excel/CSV） | ❌ | ❌ |
| 银行对账单自动匹配 | ❌ | ❌ |
| 银行存款余额调节表 | ❌ | ❌ |
| 自动记账规则配置 UI | ✅ accounting_rule 表已建+18 条规则 | ❌ 仅展示 |
| 分录模板编辑器（金额表达式 `#{amount}*0.06`） | ✅ entry_template 表已建 | ❌ 无 |
| 自动记账日志查看 | ✅ auto_voucher_log 表已建 | ❌ 无 |
| 手工触发自动记账 | ❌ | ❌ |
| 质量评分审核工作台 | ✅ quality_score 字段已就绪 | ❌ 无 |
| OCR 发票识别 | ✅ OCR 模型文件已存在 | ❌ 财务无 OCR 入口 |
| 月末结账工作台（计提折旧/税费/结转损益） | ✅ 规则 E15-E18 已建 | ❌ 无 |
| 年末结账工作台（结转本年利润） | ❌ | ❌ |
| 凭证电子签章（出纳/审核/记账多人） | ✅ voucher_signature_log 表已建 | ❌ 无 |
| 财务报表电子签章（负责人+法人） | ❌ | ❌ |
| UKey/数字证书管理 | ❌ | ❌ |

### 8.3 缺失的报表类数据分析页面（用户反馈 #2）

当前 `FinanceReport.vue` **仅支持 3 张主表**（利润表/资产负债表/现金流量表），按"任意日期范围"查询。专业财务报表应分层级覆盖：

#### 8.3.1 法定财务报表（缺失）
- 所有者权益变动表（财政部要求四表一注的"第四表"）
- 财务报表附注（会计政策、税项、报表项目明细）
- 年度报表汇总页（12 个月利润趋势 + 年度四表）
- 季度报表对比页（同环比、预算对比）
- 月度报表归档（按月封存不可修改）

#### 8.3.2 管理利润表/内部管理报表（用户明确要求，全部缺失）
- 管理利润表（按门店/部门/渠道拆分，区别于法定合并利润表）
- 分门店利润表（chain-standard/chain-enterprise 多门店场景）
- 分渠道利润表（堂食/美团/饿了么/抖音各渠道毛利贡献）
- 单品盈利分析（菜品收入-食材成本-分摊人工=单品净利）
- 贡献毛益报表（区分固定成本和变动成本）
- 本量利分析报表（盈亏平衡点、安全边际）

#### 8.3.3 成本核算与成本报表（缺失）
- 成本核算方法选择 UI（先进先出/加权平均/个别计价）
- 标准成本差异分析表（standard_cost_card 表已有，差异分析 UI 缺失）
- 食材成本日报/月报（理论消耗 vs 实际消耗对比）
- 人工成本分摊报表（按工时/按营业额分摊到门店/菜品）
- 毛利率分析报表（按菜品分类、按时间段、按门店）

#### 8.3.4 资金与预算报表（部分缺失）
- 资金日报/月报
- 现金流量预测表（基于应收应付账期推算）
- 预算差异分析报表（按部门/科目多层钻取）
- 滚动预测报表（基于实际+预算生成 12 个月预测）

#### 8.3.5 应收应付与往来报表（部分缺失）
- 往来对账单（按客户/供应商生成对账函）
- 坏账准备测算表（账龄法计算应计提坏账）
- 应付账期分析（超期应付清单、账期分布）

#### 8.3.6 税务报表（缺失，FinanceTax.vue 仅配置税率）
- 增值税申报表（含附表一/二/三/四）
- 企业所得税季度预缴申报表
- 个人所得税申报表（含代扣代缴明细）
- 附加税申报表、印花税申报表
- 进项税抵扣明细表
- 税务风险评估报告（税负率异常预警）

#### 8.3.7 合并报表（连锁企业必备，全部缺失）
chain-enterprise 规模等级已支持多门店，但缺失：
- 合并资产负债表、合并利润表、合并现金流量表
- 内部交易抵消分录（门店间调拨、内部往来）
- 合并工作底稿

#### 8.3.8 报表可视化与导出（部分缺失）
- 报表图表可视化（ECharts 趋势图、饼图、柱状图）
- 报表智能洞察（report_insight_rules 表已建，但无 UI 触发）
- 报表订阅与定时推送（邮件/企业微信）

### 8.4 AI 接入预留（用户反馈 #3）

#### 8.4.1 项目中已有的 AI 基础设施
- `views/system/AIModelConfig.vue` - AI 模型配置页（`/system/ai-model-config`）
  - 字段：`modelType`(local/api)、`endpoint`、`apiKey`、`confidence`、`modelPath`、`timeout`
- `views/warehouse/SmartRestock.vue` - 智能补货建议（**整页组件，非抽屉**）
  - 多模型对比表格（动态列 + 最终建议量列）
  - 置信度展示（high/medium/low 三档）
  - 模型一致性判断（`isModelsAgree()` 最大差异 ≤ 20%）
  - 紧急程度映射（`calcUrgency()` 基于库存比例）
  - 顶部"已配置 N 个 AI 模型"提示 + 跳转配置入口
- `views/hr/KnowledgeIntelligence.vue` - 知识智能
- `views/operations/DecisionBoard.vue` - 经营决策看板（含 AI 洞察）
- `types/warehouse-warning.ts` - 含 `AIModelType` 类型定义

#### 8.4.2 AI 抽屉窗口接入建议

**推荐方案：全局 AI 财务助手抽屉**
- 在 `MainLayout.vue` 右下角添加悬浮按钮"AI 财务助手"
- 点击展开右侧抽屉（宽度 600px），类似 ChatGPT 风格的对话窗口
- 支持自然语言查询："上个月毛利率多少？"、"哪些供应商应付超期了？"
- 复用仓储 SmartRestock 的多模型对比 + 置信度展示模式
- 后端预留 `/v1/finance/ai-chat` 接口

**预留位置建议**：
- 前端通用组件：`frontend/src/components/business/AIAssistantDrawer.vue`（业务层通用 AI 抽屉）
- 财务专用：`frontend/src/views/finance/components/FinanceAIAssistant.vue`
- API 封装：`frontend/src/api/finance/ai-assistant.ts`
- 后端 Controller：`backend/.../controller/finance/FinanceAIController.java`
- 后端 Service：`backend/.../service/finance/FinanceAIService.java`
- 类型定义：在 `frontend/src/types/finance.ts` 添加 `FinanceAIChatMessage` / `FinanceAISuggestion` 类型
- 菜单不新增条目，通过悬浮按钮触发

**AI 应用场景**（财务专用）：
1. 报表 AI 解读：基于当前报表数据的异常提示、改进建议
2. 凭证智能填单：上传发票图片 → OCR 识别 → AI 自动生成凭证分录
3. 智能税务筹划：基于经营数据推荐税务优化方案
4. 预算 AI 预测：基于历史数据预测未来现金流
5. 异常检测：自动识别异常交易、关联交易、税务风险

### 8.5 其他专业财务缺失（需财务专家介入）

#### 8.5.1 会计核算方法配置（缺失）
- 存货计价方法配置（先进先出/月末加权平均/移动加权平均/个别计价）
- 折旧方法配置（年限平均法/工作量法/双倍余额递减法/年数总和法）
- 坏账准备计提方法（账龄分析法/余额百分比法/个别认定法）
- 所得税核算方法（应付税款法/资产负债表债务法）

#### 8.5.2 期末调整事项（缺失）
- 待摊费用摊销、预提费用计提
- 汇兑损益调整（660304 科目已建但无 UI）
- 存货跌价准备、资产减值损失

#### 8.5.3 外币业务（缺失）
- 外币凭证录入、期末汇率调整、外币报表折算

#### 8.5.4 会计档案管理（缺失）
- 凭证/账簿/报表按月归档
- 档案借阅管理
- 档案销毁（保管期满 30 年）

#### 8.5.5 审计与合规（部分缺失）
- 已有：`finance_operation_log` / `finance_audit_log` / `voucher_signature_log` / `incompatible_duty`
- 缺失：审计追踪报告、审计抽样工具、内部控制自我评价报告、关联交易披露

#### 8.5.6 财务共享中心（chain-enterprise 规模，缺失）
- 跨门店费用分摊（房租、营销费按营业额分摊到各门店）
- 内部资金池管理（连锁企业内部借贷）
- 财务共享工单（门店提交费用报销到总部审核）
- 统一支付中心（总部代付供应商款）

#### 8.5.7 财务预算与绩效（部分缺失）
- 已有：预算管理基础表（budgets，5 类预算，年/月两级）
- 缺失：预算编制工作流（自下而上申报 → 自上而下审批）、预算调整审批、预算考核报表、KPI 计分卡

#### 8.5.8 财务风险预警（缺失）
- 现金流预警（账户余额低于阈值）
- 应收账款超期预警（按客户分级）
- 毛利率异常预警（低于行业均值）
- 费用率异常预警（管理费用率/销售费用率突增）
- 税务风险预警（税负率异常）

#### 8.5.9 餐饮行业特色缺失
- 菜品成本卡管理（BOM 表：每道菜的标准食材消耗量）
- 菜品毛利率排行（按菜品分类、按厨师）
- 食材损耗率分析（理论消耗 vs 实际消耗）
- 外卖平台对账（平台结算单 vs 系统订单差异分析）
- 堂食/外卖/团购多渠道收入对账

---

## 9. 人事模块全链路分析（用户重点关注）

### 9.1 人事模块当前文件

**前端 20 个页面**（`views/hr/`）：
HREmployee / HROrganization / HRPosition / HRJobLevel / HRAttendance / HRRecruitment / HRTraining / HRContract / HRSalary / HRApproval / HRKnowledgeBase / HRAnalytics / EmployeeIntelligence / HRConfigCenter / HRHealthCertificate / HRInvitationCode / ContractDashboard / ContractTemplateLibrary / HRContractTemplate / KnowledgeIntelligence

**后端 Controller**（散落在根 `controller/` 下，无 `controller/hr/` 子目录）：
EmployeeController / DepartmentController / PositionController / AttendanceController / SalaryController / HealthCertificateController / RecruitmentRequirementController / ResumeController / InterviewController / OnboardingRecordController / OnboardingArchiveController / OnboardingInvitationController / EmployeeLaborContractController / ElectronicContractController 等

### 9.2 人事全链路状态

| 环节 | 后端 | 前端 | 联动状态 |
|------|------|------|---------|
| **招聘需求** | ✅ RecruitmentRequirementController（含 storeId/storeName，type=hr/store） | ❌ HRRecruitment.vue 全 Mock | ❌ 前端不调用后端 |
| **简历投递** | ✅ ResumeController /v1/resumes | ❌ 全 Mock | ❌ |
| **面试管理** | ✅ InterviewController /v1/interviews | ❌ 全 Mock | ❌ |
| **录用（发 Offer）** | ❌ 无 hires/offers 表 | ❌ hireApi 纯 Mock | ❌ 数据无法持久化 |
| **入职** | ✅ OnboardingRecordController /v1/onboarding-records（completeOnboarding 调用 createEmployeeProfile） | ❌ HRRecruitment.vue 的 handleOnboardHire() 仅更新 Mock 状态，未调用后端 | ❌ 前端入职操作与后端断链 |
| **试用/转正** | ⚠️ OnboardingRecord 有 probationStart/probationEnd/probationMonths 字段 | ⚠️ HREmployee.vue status='probation' 手动更新 | ❌ 无转正审批 API、无到期提醒、无转正薪酬调整 |
| **在职管理** | ✅ EmployeeController | ✅ HREmployee.vue | ✅ |
| **合同管理** | ✅ EmployeeLaborContractController + ElectronicContractController | ✅ HRContract.vue | ✅ 完整电子合同流程 |
| **薪资管理** | ✅ SalaryController + SalaryAdjustmentService | ✅ HRSalary.vue | ✅ |
| **考勤排班** | ✅ AttendanceController + controller/schedule/* | ✅ HRAttendance.vue | ⚠️ 与门店 ShiftManagement.vue 双系统割裂 |
| **培训发展** | ✅ | ✅ HRTraining.vue + HRKnowledgeBase.vue | ✅ |
| **健康证** | ✅ HealthCertificateController | ✅ HRHealthCertificate.vue | ⚠️ 与门店 StoreCertificate.vue 双系统割裂 |
| **异动管理** | ❌ 无 EmployeeTransfer/PositionChange 实体 | ❌ 仅 OrganizationChangeLog 记录组织变更 | ❌ 调岗/晋升/调薪无审批流程 |
| **离职流程** | ❌ 全后端无 resignation 实体 | ⚠️ HREmployee.vue 有 'resigning'/'resigned'/'retired' 枚举但无离职 API | ❌ 离职无闭环 |
| **档案归档** | ⚠️ EmployeeArchiveDetailController 存在 | ❌ 无离职档案归档、无离职员工档案库 | ❌ |

### 9.3 关键断链点

1. **HR 招聘前置链路 Mock 化**：`api/hr/recruitment.ts` 完全是 Mock 数据，无任何 `request.get/post` 调用后端。后端 `recruitment_requirements`/`resumes`/`interviews` 三张表存在但前端未消费。
2. **录用环节无后端持久化**：HR 前端 `hireApi` 操作 Mock `HireRecord`，但后端无 hires/offers 表。
3. **录用→入职不联动**：`HRRecruitment.vue` 的 `handleOnboardHire()` 仅调用 `hireApi.updateStatus(id, 'onboarded')`，未调用后端 `OnboardingRecordController`，也未通过 `OnboardingRecordService.completeOnboarding()` 创建 `Employee`。
4. **门店招聘→入职不联动**：`StoreRecruitment.vue` 走 `StoreManagementRecruitmentController` 三级审批，但审批通过后无自动生成 OnboardingRecord 或 Employee。
5. **入职后 storeId 丢失**：`OnboardingRecordServiceImpl.createEmployeeProfile()` 第 143-166 行**未设置 `Employee.storeId`**（即使 `OnboardingRecord` 有 storeId）。
6. **离职流程完全缺失**：无离职申请/审批/交接/离职证明 API。
7. **异动管理缺失**：无 EmployeeTransfer/PositionChange 实体。

---

## 10. 门店经营数据孤岛问题（用户重点关注）

### 10.1 门店管理功能清单

| 二级菜单 | 文件 | 完成度 | 联动状态 |
|---------|------|--------|---------|
| 待办事项 | StorePendingTasks.vue + StoreManagementTaskController | 中 | ❌ 未真正接收跨模块事件（仅 mockDataCenter） |
| 日结对账 | StoreDailySettlement.vue + StoreManagementSettlementController | 高 | ❌ 未流向财务凭证系统 |
| 证件管理 | StoreCertificate.vue + StoreHealthCertificateController | 高 | ⚠️ 与 HR 健康证双系统割裂 |
| 物资需求 | StoreMaterialRequest.vue | 中 | ❌ 与采购模块 MaterialRequest 分离 |
| 门店库存 | StoreInventory.vue | 中 | ⚠️ 与仓储模块联动，但补货未打通采购 |
| 门店招聘 | StoreRecruitment.vue + StoreManagementRecruitmentController | 高 | ❌ 与 HR 招聘割裂，审批通过后无入职闭环 |
| 桌台记录 | StoreTableUsage.vue + DiningTableController | 高 | ⚠️ 与订单通过 orderId 关联，但订单数据 Mock |
| 叫号记录 | StoreQueueHistory.vue + CallNumberController | 高 | ❌ 叫号→入座→下单流程断裂 |
| 排班管理 | ShiftManagement.vue + controller/schedule/* | 中 | ⚠️ 与 HR HRAttendance.vue 双系统割裂 |
| 门店总览 | StoreStatusOverview.vue | 中 | ❌ 与运营中心 OperationsDashboard 数据独立 |
| 操作日志 | StoreOperationLog.vue | 低 | ❌ 纯 Mock，未与系统审计日志打通 |

### 10.2 门店经营数据的 4 个严重孤岛

#### 孤岛 1：门店总览数据孤岛
- **数据源**：`StoreStatusOverview` 的营收/订单/翻台率数据
- **应流向**：运营中心 `OperationsDashboard` / `DecisionBoard` / `LiveMonitor`
- **实际**：运营中心 5 个页面全部硬编码 Mock（春熙路店、宽窄巷子店等 7 家门店的固定数据），不从 `StoreController` 取真实门店列表
- **影响**：运营决策无数据支撑

#### 孤岛 2：日结对账数据孤岛
- **数据源**：`StoreDailySettlement` 的营收/成本/利润数据
- **应流向**：财务模块凭证系统（自动生成营收凭证）、报表中心
- **实际**：日结数据未流向财务模块生成凭证，未流向报表中心
- **影响**：财务对账无真实日结依据

#### 孤岛 3：门店招聘审批孤岛
- **数据源**：`StoreManagementRecruitment` 三级审批数据
- **应流向**：HR 招聘 `HRRecruitment`、入职记录 `OnboardingRecord`、员工档案 `Employee`
- **实际**：审批通过后无自动生成 OnboardingRecord 或 Employee
- **影响**：招聘→入职→员工档案核心链路断裂

#### 孤岛 4：门店库存/物资孤岛
- **数据源**：`StoreInventory` + `StoreMaterialRequest`
- **应流向**：采购模块 `MaterialRequest` 审批流
- **实际**：前端注释明确说明"与采购模块的物资需求提报分离，仅展示本门店数据"
- **影响**：门店提报的物资需求未自动流转到采购审批

### 10.3 双系统割裂问题（3 处）

| 割裂点 | HR 系统 | 门店系统 | 问题 |
|--------|---------|---------|------|
| 招聘 | HRRecruitment.vue（Mock）+ RecruitmentPosition/Resume/InterviewRecord/HireRecord 类型 | StoreRecruitment.vue + RecruitmentApproval 实体（postId/applicantName/proposedSalary） | 字段不对齐，无数据同步，同一应聘者可能重复建档 |
| 考勤排班 | HRAttendance.vue + AttendanceController | ShiftManagement.vue + controller/schedule/* | 两套排班未共享数据 |
| 健康证 | HRHealthCertificate.vue + HealthCertificateController | StoreCertificate.vue + StoreHealthCertificateController | 两套独立 Controller，数据未共享 |

### 10.4 订单管理与门店经营的关系（完全割裂）

订单管理 4 个页面全部硬编码 Mock：
- `OrderQuery.vue` 第 19-35 行 `OrderRow` 类型有 `storeName`/`tableNo` 字段，但订单数据是硬编码 Mock
- `OrderStatistics.vue` 第 40-49 行 `dailyList` 是硬编码 Mock（旗舰店/一分店/二分店的固定数据）
- `OrderRefund.vue` 退款数据不流向门店日结的差异金额计算
- `OrderReservation.vue` 与桌台 `StoreTableUsage` 的 `reserved` 状态无联动

**订单数据未流向**：门店日结（应汇总订单金额）、桌台使用（应显示订单关联）、运营报表（应基于订单数据）。

---

## 11. 缓存规范

### 11.1 二级缓存

```
请求 → L1 Caffeine（本地） → L2 Redis → 数据库
```

### 11.2 缓存键与 TTL

| 数据类型 | 键格式 | TTL |
|---------|--------|-----|
| 员工基本信息 | `employee:basic:{id}` | 30 分钟 |
| 权限信息 | `permission:user:{id}` | 1 小时 |
| 部门信息 | `department:basic:{id}` | 24 小时 |
| 会计科目 | `accounting_subject:basic:{id}` | 24 小时 |
| 字典数据 | `dict:{type}` | 7 天 |

更新/删除时先清 L1 再清 L2，TTL 加随机偏移防雪崩。

---

## 12. 消息队列规范

### 12.1 队列命名

`{module}.{entity}.{action}`，如 `trace.food.create`

### 12.2 可靠性

- durable=true，deliveryMode=PERSISTENT
- publisher-confirm-type=correlated
- acknowledge-mode=manual
- 死信队列：消费失败转入 DLX

### 12.3 重试策略

| 参数 | 默认值 |
|------|--------|
| 最大重试 | 3 次 |
| 初始间隔 | 1000ms |
| 倍数 | 2.0 |
| 上限 | 10000ms |

---

## 13. 安全规范

- JWT + Refresh Token（Access 30min，Refresh 7d）
- RBAC 权限模型
- MFA 多因素认证（可选）
- 数据脱敏（手机号、身份证等）
- 禁止直接返回异常信息给前端
- 全局异常处理器 `@RestControllerAdvice`

---

## 14. 开发环境运行

### 14.1 后端

```bash
cd backend
mvn spring-boot:run -DskipTests
# 服务地址：http://localhost:8081/api
```

### 14.2 前端

```bash
cd frontend
npm install
npm run dev
# 服务地址：http://localhost:3000/
```

### 14.3 数据库

- 开发：H2 内存数据库（MODE=PostgreSQL），无需安装
- 生产：PostgreSQL 18 + Redis 哨兵 + RabbitMQ 镜像队列

---

## 15. 代码长度限制

| 文件类型 | 预警线 | 硬上限 |
|---------|--------|--------|
| Vue 组件 | 500 行 | 1500 行 |
| JS/TS | 500 行 | 1200 行 |
| SCSS/CSS | 400 行 | 800 行 |
| Java | 500 行 | 1500 行 |
| 函数/方法 | 80 行 | 150 行 |

---

## 16. 当前项目状态摘要（2026-06-25 V2 审查）

### 16.1 整体完成度（按真实 15 个一级模块）

- **可交付模块（≥85%）**：食品追溯(90%)、资产管理(85%)
- **基本可用模块（65-84%）**：仓储管理(75%)、系统管理(75%)、会员管理(70%)、财务中心(70%)、产品中心(65%)
- **半成品模块（40-64%）**：门店管理(50%)、人事管理(50%)、设备管理(60%)、订单管理(40%)、运营中心(40%)
- **未对接模块（<40%）**：工作台(30%)、采购管理(35%)、电子签章(30%，后端完全缺失)

### 16.2 主要风险

1. **withMockFallback 滥用**：HR 招聘、订单管理、运营中心、数据看板等模块前端可用、后端未对接
2. **跨模块数据孤岛**：门店经营数据无法流向运营中心/财务/订单；HR 招聘与门店招聘双系统割裂
3. **财务专业缺失**：电子凭证 UI、管理利润表、合并报表、税务申报、AI 接入等大量专业功能缺失
4. **人事全链路断裂**：招聘前置链路 Mock、录用无后端、入职不联动、离职流程缺失
5. **teleported 规则违反**：14+ 处 dialog 内弹出组件未设置 `:teleported="false"`
6. **金额转换不统一**：7+ 处本地 `formatFen` 函数

### 16.3 商用就绪度评估

**整体商用就绪度：约 50%（较之前评估下调）**

下调原因：
- 之前未发现 HR 招聘前置链路完全 Mock
- 之前未发现门店经营数据 4 个严重孤岛
- 之前未发现财务模块专业缺失的深度（电子凭证/报表/AI 等）
- 之前未发现双系统割裂问题（HR vs 门店的招聘/考勤/健康证）

详见 `DEVELOPMENT_STATUS.md`。

---

## 17. AI Agent 工作指引

### 17.1 接手任务时的第一步

1. 阅读本文件建立项目认知
2. 阅读 `.trae/rules/project_rules.md` 了解强制规则
3. 阅读 `docs/spec/` 下的规范文档（特别是 06-API、13-编码、16-数据转换器）
4. 查看目标模块的 `views/{module}/` 和 `controller/{module}/` 目录
5. **重要**：查看 `src/modules/{module}/menu.ts` 了解真实菜单结构，不要假设模块划分

### 17.2 修改代码时的检查清单

- [ ] 使用 `request` 实例，不直接 axios
- [ ] GET 请求直接传 params，不嵌套
- [ ] 响应数据不访问 `.data`
- [ ] 状态显示用 StatusTag
- [ ] 操作列用 `el-button link`
- [ ] 颜色用 `--fts-*` 变量
- [ ] Dialog 内弹出组件 `:teleported="false"`
- [ ] 金额转换用 `fenToYuanNumber` / `yuanToFen`
- [ ] 实体类有 `@TableLogic`
- [ ] 查询不手动加 `deleted = 0`
- [ ] 返回 `Result<T>` 统一格式
- [ ] 构造函数注入，无 `@Autowired`
- [ ] 事务注解 `rollbackFor = Exception.class`
- [ ] **不要新增 withMockFallback**（新代码应直接对接后端，或显式标注 TODO）

### 17.3 常见陷阱

1. **withMockFallback 误导**：看到 API "能用"不要假设后端已对接，需检查是否走 mock
2. **菜单结构误导**：`src/modules/*/menu.ts` 是真实菜单，后端 `MenuServiceImpl` 是遗留硬编码（已不同步）
3. **双系统割裂**：HR 与门店在招聘/考勤/健康证上有两套独立系统，修改时需确认数据源
4. **H2 与 PostgreSQL 差异**：开发环境 H2 兼容模式有局限，复杂 SQL 需在 PG 验证
5. **Element Plus 特殊伪类**：`el-tabs` 的 `:nth-child(2)` 和 `:last-child` 有特殊 padding 规则
6. **金额精度**：必须用 `Math.round(yuan * 100)` 避免浮点误差
7. **状态码混淆**：`code=0` 是成功（业务码），不是 HTTP 200
8. **门店管理路径特殊**：门店管理后端用 `/api/v1/store-management/*` 前缀，与其他模块 `/v1/*` 不同

---

## 18. 关键文件索引

### 18.1 规范文档

- `.trae/rules/project_rules.md` — 项目强制规则（最高优先级）
- `docs/spec/00-索引与总览.md` — 规范文档索引
- `docs/spec/06-API接口设计.md` — API 规范
- `docs/spec/13-编码规范.md` — 编码规范
- `docs/spec/16-数据转换器规范.md` — DataConverter 规范

### 18.2 核心配置

- `backend/src/main/resources/application.yml` — 后端配置
- `frontend/src/api/request.ts` — 前端请求实例
- `frontend/src/modules/index.ts` — ★ 菜单注册入口（真实菜单结构来源）
- `frontend/src/stores/permission.ts` — 权限+菜单 Store
- `frontend/src/styles/global/_element-overrides.scss` — Element Plus 样式覆盖
- `frontend/src/plugins/fixed-popper.ts` — Popper.js 全局插件

### 18.3 财务模块核心文件

- `backend/.../service/finance/impl/VoucherServiceImpl.java` — 凭证状态机
- `backend/.../service/finance/impl/PaymentServiceImpl.java` — 四账联动
- `backend/.../service/finance/impl/AutoVoucherServiceImpl.java` — 自动记账规则引擎
- `frontend/src/views/finance/FinanceLedger.vue` — 凭证列表（含反审核/反过账）
- `frontend/src/views/finance/AutoVoucher.vue` — 自动凭证管理（仅展示，需补 UI）
- `frontend/src/api/finance/voucher.ts` — 凭证 API

### 18.4 人事模块核心文件

- `backend/.../service/impl/OnboardingRecordServiceImpl.java` — 入职服务（第 143-166 行 createEmployeeProfile 未设置 storeId）
- `backend/.../entity/Employee.java` — 员工实体（第 106-108 行有 storeId）
- `backend/.../entity/RecruitmentRequirement.java` — 招聘需求实体（含 storeId/type=hr|store）
- `frontend/src/views/hr/HRRecruitment.vue` — 招聘管理页（全 Mock）
- `frontend/src/api/hr/recruitment.ts` — 招聘 API（全 Mock，未对接后端）
- `frontend/src/types/hr/cross-module.ts` — 跨模块事件类型定义（14 种，未实现）

### 18.5 门店管理核心文件

- `frontend/src/views/store-ops/StoreStatusOverview.vue` — 门店总览（数据孤岛）
- `frontend/src/views/store-ops/StoreDailySettlement.vue` — 日结对账（未流向财务）
- `frontend/src/views/store-ops/StoreRecruitment.vue` — 门店招聘（与 HR 割裂）
- `backend/.../controller/StoreManagementRecruitmentController.java` — 门店招聘三级审批
- `backend/.../service/impl/RecruitmentApprovalServiceImpl.java` — 门店招聘审批服务

### 18.6 运营中心核心文件

- `frontend/src/views/operations/OperationsDashboard.vue` — 运营总览（第 73-80 行硬编码 Mock）
- `frontend/src/views/operations/DecisionBoard.vue` — 经营分析（第 40-48 行硬编码 Mock）
- `frontend/src/views/warehouse/SmartRestock.vue` — 智能补货建议（AI 抽屉窗口参考模式）

### 18.7 本次审查输出

- `docs/audit/PROJECT_CONTEXT.md` — 本文件
- `docs/audit/DATA_LIFECYCLE.md` — 数据生命周期流转
- `docs/audit/DEVELOPMENT_STATUS.md` — 开发进度与商用完善清单

---

**文档结束。AI Agent 在接手任何任务前，请完整阅读本文件，特别注意第 8-10 章的财务专业缺失、人事全链路、门店孤岛问题。**
