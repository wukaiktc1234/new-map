# 会话交接文档 — Handoff for Next Session

> 交接时间：2026-04-07
> 交接原因：v0.13.1 安全加固全闭环 + P1全清(5项) + Critical#1 CAS锁E2E验证通过(9.8/10) + 厨房端完整实现 + 收银端架构重构
> 项目版本：**v0.13.1**（P0全清+**P1全清(5项新增)**+P2 17/19+E2E 6/6 + **DTO验证4轮PASS(98.75)** + **CAS锁E2E 9.8/10** + **[C]Controller拆分940→114+N+1优化95%+5支付方式** + **🍳厨房端10文件完整实现** + **独立审查验收工作流建立**）
> 工作模式：**Leader-Agent 单会话内多智能体协调模式 + 独立审查验收流程（已验证）**

> **📋 新窗口启动指令 → 复制 `.harness/STARTUP-COMMAND.md` 中的内容粘贴给AI即可**

---

## 一、你是谁？你应该做什么？

**你的角色 = 持久主控协调员（Persistent Coordinator Leader）**

你不是来做一个具体任务的。你是**项目经理 + 技术负责人 + 代码审查员**的合体。

### 核心工作循环（已验证）

```
① 用户给你指令 → 你理解需求并拆解任务
② 用 Task 工具派出专业 Sub-Agent 并行执行
③ Agent 返回结果 → 你审查质量
④ 验收通过 → 更新进度文档 → 向用户汇报
⑤ 发现问题 → 打回修复或自己介入
⑥ 每轮结束必须：审查验收 + 更新文档 + 制定下一步计划 ← ⚠️ 本次教训
⑦ 等待用户下一个指令 → 回到①
```

### 可用 Agent 类型

| Agent 类型 | 用途 | 已验证 |
|-----------|------|--------|
| `web-interaction-expert` | 前端交互/UI完善、CRUD验证 | ✅ 本轮大量使用 |
| `code-review-expert` | 代码质量审查、回溯性验收 | ✅ 本轮使用 |
| `code-quality-expert` | 代码重构优化、规范修复 | ✅ 本轮使用 |
| `java-expert` | Java语言专项、编译修复 | ✅ 本轮使用 |
| `backend-architect` | 后端架构、ISP重构等 | ✅ 本轮使用 |
| `search` | 代码搜索与研究 | ✅ 可用 |
| `postgresql-expert` | 数据库专项 | ✅ 可用 |
| `bug-testing-repair` | Bug发现与修复 | ✅ 可用 |

**派出方式**：
```typescript
Task({ subagent_type: "web-interaction-expert", query: "任务描述", response_language: "zh-CN" })
// 多个Agent可并行派出，各自独立返回结果
```

---

## 二、项目是什么？

### 基本信息

| 属性 | 值 |
|------|-----|
| 名称 | 食品溯源系统 (Food Traceability System) |
| 定位 | 个体餐饮企业的自动化办公+追溯系统 |
| 架构 | 前后端分离，星型架构（1个后端+N个前端） |
| 后端 | Spring Boot 3.2.0 + MyBatis Plus 3.5.5 + PostgreSQL 18(生产)/H2(开发, profile=h2) |
| 前端主站 | Vue.js 3.3.8 + Element Plus 2.4.4 + TypeScript 5.2.2 + Vite 5.0 |
| 收银端 | Vue 3 (`frontend-pos/`) |
| 后厨端 | Vue 3 (`frontend-kitchen/`) |
| 小程序 | 微信原生 (`miniprogram/`) |
| OCR服务 | Python FastAPI + PaddleOCR (`ocr-service/`) |
| 服务地址 | 后端 http://localhost:8081/api, 前端 http://localhost:3000 |
| 登录账号 | admin / &lt;redacted&gt; |
| 当前版本 | **v0.13.0** |

### 项目生态全景（v0.10.1 更新）

```
p:\my-new-project\
├── backend/              ← Spring Boot API（所有终端共用）
│   └── src/main/java/com/example/demo/
│       ├── hardware/     ← 硬件驱动层（ISP重构后3层接口体系）
│       │   ├── HardwareDriver.java          ← 基础接口(10方法)
│       │   ├── TextBasedDriver.java         ← 文本协议子接口(+4方法)
│       │   ├── BinaryDriver.java            ← 二进制协议子接口(+3方法)
│       │   ├── AbstractHardwareDriver.java  ← 抽象基类(AtomicLong线程安全)
│       │   ├── DriverRegistry.java         ← 注册中心(inferConnectionType推断)
│       │   ├── utils/HexUtils.java         ← 🆕 十六进制工具类
│       │   └── driver/
│       │       ├── SerialPortDriver.java    ← implements TextBasedDriver + BinaryDriver
│       │       ├── NetworkDriver.java      ← implements TextBasedDriver + BinaryDriver (Hex编码)
│       │       └── UsbDeviceDriver.java    ← implements HardwareDriver only (零桩代码)
│       ├── controller/   ← REST API层
│       ├── service/      ← 业务逻辑层
│       ├── mapper/       ← MyBatis Plus数据访问
│       ├── entity/       ← 实体类
│       ├── dto/          ← 数据传输对象
│       ├── config/       ← 配置类
│       └── security/     ← 安全认证（JWT+RBAC）
│
├── frontend/             ← 管理后台（当前主力）
│   └── src/
│       ├── api/
│       │   ├── request.ts              ← 统一请求实例（强制使用）
│       │   ├── electronicVoucher.ts    ← 电子凭证API（含20+接口定义）
│       │   ├── auditLog.ts             ← 🆕 审计日志API
│       │   ├── permission.ts           ← 兼容层Facade(@deprecated)
│       │   └── permission/             ← 权限API新模块(user.ts/role.ts等)
│       ├── composables/
│       │   ├── useElectronicVoucher.ts  ← 门面Composable(172行)
│       │   └── voucher/                ← 🆕 5个子composable(List/Detail/Upload/Accounting/Reimburse)
│       ├── constants/
│       │   └── voucher.ts              ← 🆕 凭证状态枚举常量(VoucherStatus/SignatureStatus/VerifyStatus)
│       ├── views/SystemSettings/
│       │   ├── PermissionCenter/       ← 权限中心6页(DataScopeConfig props已修复)
│       │   ├── UserCenter/             ← 个人中心3页(Security:真实API+绑定对话框)
│       │   └── OrganizationSettings.vue ← 🆕 组织架构(左右分栏+el-tree+部门CRUD, 618行)
│       ├── views/Finance/
│       │   ├── components/
│       │   │   ├── ElectronicVoucher/   ← 已拆分为12文件
│       │   │   ├── VoucherPreviewTab.vue      ← 🆕 预览Tab子组件
│       │   │   ├── VoucherParsedInfoTab.vue   ← 🆕 解析信息Tab子组件
│       │   │   ├── VoucherXmlTab.vue          ← 🆕 XML源文件Tab子组件
│       │   │   └── ReimbursementForm.vue      ← 魔术数字已枚举化+API规范化
│       │   └── ...
│       └── components/Voucher/
│           └── InvoiceRenderer.vue     ← 类型已统一(复用InvoiceVO)
│
├── frontend-pos/         ← 收银端（基础框架+TS修复完成）
├── frontend-kitchen/     ← 后厨端（基础框架+TS修复完成）
├── miniprogram/          ← 微信小程序（24页+6组件）
├── ocr-service/          ← OCR识别服务
├── .harness/             ← 协调基础设施 ⬅️ 必读！
└── PROJECT_PROGRESS.md   ← 进度追踪 ⬅️ 每次必读！
```

---

## 三、之前发生了什么？（完整时间线）

### Phase 0: 项目初始化（历史）
- 项目骨架搭建、数据库设计、认证体系(JWT+RBAC)、基础CRUD代码生成
- 财务模块自动记账架构设计与实现
- Lombok全量Delombok展开（1382文件）

### Phase 1: 基础设施大修（v0.3.0 ~ v0.9.0）
- 致命Bug修复（AnomalyAccessLogFilter空响应体、DatabaseInitConfig初始化数据错误、auth.ts权限同步缺失）
- 系统设置路由重构（29页→11页MVP）
- Harness工程体系建立（PROTOCOL/MULTI_SESSION/SESSION_ASSIGNMENT/skills）
- 三会话并行执行（Session A/B/C）：冒烟测试24页通过 + API规范化15文件 + EV拆分12文件 + POS/Kitchen/TS修复 + 硬件驱动6文件 + PG迁移脚本 + prod配置

### 🔥 v0.10.0 — Leader-Agent模式首秀（本轮核心工作）

**启动**: 读取 HANDOFF.md → 启动服务(h2 profile) → 用户确认方向"前端深度功能验证"

**阶段一：三Agent并行深度验证**
| Agent | 范围 | 结果 |
|-------|------|------|
| web-interaction-expert ×2 | 权限中心6页 + 个人中心4页 | CONDITIONAL PASS 88/100 |
| code-review-expert ×1 | hardware 6文件 + EV 14文件 | Conditional Pass (3致命+8严重) |

**阶段二：P0 致命修复（5项）**
| # | 问题 | 方案 | Agent |
|---|------|------|-------|
| P0#1 | RoleManagement DataScopeConfig缺props | 补充storeOptions/departmentTree数据加载 | Frontend |
| P0#2 | OrganizationSettings缺组织架构树 | 重写618行(左右分栏+el-tree+部门CRUD) | Frontend |
| P0#3 | lastActivityTime线程竞态 | long→AtomicLong | Backend |
| P0#4 | USB disconnect资源泄漏 | finally块清理+桩方法改抛异常 | Backend+Leader补修 |
| P0#5 | useElectronicVoucher硬编码API | 迁移至electronicVoucherApi | Frontend |

**阶段三：P1 严重修复（6项）**
| # | 问题 | 方案 |
|---|------|------|
| P1#1 | user.ts 4处unknown类型 | UserBasicInfo/UserListQuery接口 |
| P1#2 | permission.ts旧版API残留 | 兼容层Facade(@deprecated)，15引用零改动 |
| P1#3 | Security安全日志模拟数据 | 新建auditLogApi对接后端 |
| P1#4 | 手机/邮箱绑定按钮无事件 | 完整对话框+表单验证+倒计时 |
| P1#5 | HardwareDriver ISP违反 | 3层接口体系(HardwareDriver+TextBased+Binary) |
| P1#6 | Composable 976行超限 | 拆分5个子composable+172行门面 |

**阶段四：风格审查修复**
- 发现 false() 运行时Bug → 已修复
- 发现 response.data 违规访问 → 已修复(2处)

### 🔥 v0.10.1 — P2质量优化 + 双轨并行

**P2-A 后端优化（3项）**
- Javadoc 6文件全覆盖（HardwareDriver/TextBased/Binary/Abstract/SerialPort/Network）
- HexUtils 工具类提取 + NetworkDriver Base64→Hex 编码统一
- DriverRegistry inferConnectionType 智能推断 + 重复注册WARN检测

**P2-B 前端优化（5项）**
- constants/voucher.ts 枚举常量（VoucherStatus/SignatureStatus/VerifyStatus）
- 路由去重（index.ts 删除4条UserCenter重复路由）
- loading 分离（verifyingSignature / verifyingInvoice 独立状态）
- InvoiceRenderer 类型统一（删除59行本地接口，复用InvoiceVO）
- DetailDialog 子Tab组件化（424行→195行+3子组件）

**Leader 额外修复**（审查发现遗漏）
- ReimbursementForm.vue: 4处魔术数字 + 3处硬编码API + 直接import request违规

**另一窗口 F1-F4 并行推进**（无冲突）
- F1: CSS设计令牌统一(_tokens.scss 416行)
- F2: TS核心类型补全(Product/Order/Inventory等10个)
- F3: formatters.ts统一格式化(414行)
- F4: Store文档化+useCrudTable/useSearchForm composable

### 🔥 v0.11.0 — 无人值守模式（阶段三验证 + P2收尾）

**启动**: 用户授权全权无人值守模式(~3小时)，三域全覆盖

**Batch 1: 后端API补全**
- 覆盖率分析：收银端100% + 后厨端95.2%（仅scan-serve缺失）
- 新增 `POST /v1/kitchen/pre-make/scan-serve` 接口（75行，预制作食品扫码出餐）
- `mvn compile` ✅ BUILD SUCCESS

**Batch 2: 前后端联调验证**
- 8个核心API全部PASS：登录/商品(8分类)/分类/餐桌/后厨订单(待制作+制作中)/用户/角色
- 验证数据完整性：CRUD操作、分页查询、状态流转均正常

**Batch 3: P2收尾4项**
- VoucherParsedInfoTab魔术数字 → SignatureStatus/VerifyStatus枚举替换
- 3个Driver类log字段遮蔽修复（移除private log声明）
- Security.vue 6处TODO添加版本号标记 `TODO(v0.11.0)`
- NetworkDriver/SerialPortDriver未使用导入清理（StandardCharsets/Base64）
- 双编译验证：`mvn compile` ✅ + `npm run build` ✅

**Batch 4: 文档更新至v0.11.0**

### 🔥 v0.12.0 — 无人值守P2收尾 + 数据库审计

**启动**: 用户选择 🟢P2收尾+数据库验证 方案，授权无人值守模式

**Batch 0: 文档同步 + 环境基线**
- HANDOFF.md 从 v0.10.1 同步至 v0.11.0（4处更新：版本号/状态表/下一步/版本线）
- 后端编译基线确认 BUILD SUCCESS
- console.error 审计：100行/100文件，其中60处活跃调用需处理

**Batch 1: P2代码质量（双Agent并行）**
- **Agent1 console.error统一**: 24文件/50处替换为LogUtil logger（权限中心+用户中心+工具层+CRUD全覆盖）
- **Agent2 分号+日期**:
  - 分号策略：扫描692文件(237TS+455Vue)，277混合文件超安全阈值→暂不改，推荐ESLint规则
  - 日期去重：utils/index.ts re-export(-47行) + CostAnalysis替换 + EmployeeHelpers重构，2处合理差异保留注释
- 双构建验证通过（53s + 58s）

**Batch 2: P2功能完善 + 审计（双Agent并行）**
- **Security.vue 6个TODO全部实现**:
  - 密码修改→authStore.logout()跳转登录 | 日志→router.push('/system/log')
  - 手机验证码→复用sendVerificationCode(PHONE) | 邮箱验证码→复用sendEmailCode
  - 手机/邮箱绑定→确认已有userApi.update()真实实现
- **permission.ts兼容层文档化**: 20个引用模块完整清单+迁移指南+移除计划v0.13.0
- **系统设置19页面审计**: 全部空壳(~30行)或半成品(60-65行)，0个有完整功能

**Batch 3: 数据库层验证**
- 19个核心实体 vs H2 DDL 一致性审计
- 发现 **5致命 + 7严重 + 7轻微** 问题（最高优：SysPermission表名不匹配、UserRole.userId类型错误）
- PG迁移脚本发现 **1项跨环境重大风险**（主键列名与Entity @TableId不匹配）
- ⚠️ Schema修复属高风险操作，已记录待用户回归后确认

**Batch 4: 最终审查 + 文档v0.12.0**
- 双编译最终验证：后端BUILD SUCCESS + 前端build成功(5413kb gzip)
- PROJECT_PROGRESS.md + HANDOFF.md 同步更新至 v0.12.0

---

## 四、当前状态快照（v0.13.1）

### 编译与运行状态
| 项目 | 状态 | 最后验证时间 |
|------|------|------------|
| 后端编译 (mvn compile -q) | ✅ BUILD SUCCESS, exit code 0 | 本次会话(Round2) |
| 前端构建 (npm run build) | ✅ 0 errors, ~8.75s | 本次会话 |
| 后端服务 (8081, MySQL) | ✅ 运行中 PID 30324 | 本次会话 |
| E2E功能测试 (V7+V6+CAS) | ✅ **98.75/100 PASS** | 本次会话(重启后验证) |

### 任务完成度总览
| 优先级 | 总数 | 已完成 | 剩余 |
|--------|------|--------|------|
| **P0 紧急** | 8 | **8/8 ✅** | **0** |
| **P1 重要** | **19 (原14+5新增)** | **19/19 ✅** | **0** |
| **P2 改进** | ~19 | **17/19 ✅** | ~2 |
| **Critical安全** | 3 | **3/3 ✅** | **0** |
| **E2E验证** | 9项 | **9/9 ✅** | 0 |

---

## 四-B. v0.13.1 核心变更详情

### A. 安全加固与对抗性审查（Round 1-4，4轮验证）
- **FATAL-00 身份系统混淆**: SecurityUtils.getCurrentUserId()返回Long但order.getUserId()存String → 已分析并规避
- **DTO输入验证框架修复**(🎯关键发现): 嵌套`List<OrderItemDTO> items`缺少`@Valid`导致所有约束(@Min/@NotBlank等)静默失效 → 添加`@Valid`(2行代码)，4轮验证: FAIL(1.5)→FAIL→CONDITIONAL→**PASS(98.75/100)**
- **BUG#1 IDEMPOTENCY_KEY列缺失**: SchemaFixMigration.java @PostConstruct自动DDL
- **BUG#2 payOrder 403**: SecurityUser.java `toLowerCase()`→`toUpperCase()`
- **BUG#3/#4 Order实体**: delombok混乱(743行)→手动60个getter/setter(446行干净版)

### B. [C] 收银端架构重构（6专家并行交付）
| 专家 | 任务 | 成果 |
|------|------|------|
| [C]-1 | Controller拆分 | PosOrderController **940→114行**(薄壳) + PosOrderService **961行**(业务逻辑) |
| [C]-2 | N+1优化+安全 | selectById循环→selectBatchIds批量(**95%查询减少**) + 6安全漏洞修复 |
| [C]-3 | 支付UI增强 | **5种支付方式**(微信/支付宝/现金/银行卡/余额) + **挂单/取单**功能 + npm build✅ |

### C. 🍳 后厨端完整实现（6专家并行交付）
| 专家 | 任务 | 成果 |
|------|------|------|
| 🍳-1 | 主页面 | KitchenHome.vue **680行**三栏看板(pending/making/completed) + 统计面板 + WebSocket |
| 🍳-2 | 后端API | **5新API**(sorted/stats/overdue/batch/grouped-by-food) + **4个DTO** + @PreAuthorize |
| 🍳-3 | 交互增强 | BatchOperationPanel + **12键盘快捷键** + 触摸优化CSS + Web Audio音效 |

### D. P1 问题修复（独立审查驱动，Round 1+2）
| # | 问题 | 根因 | 修复 | 验证 |
|---|------|------|------|------|
| P1-SQL | PostgreSQL语法在MySQL报错 | EXTRACT/HOUR是PG专属 | 4处→TIMESTAMPDIFF/HOUR | ✅ E2E PASS |
| P1-金额 | totalAmount=0.00 | DB price=0覆盖前端price | 条件增加>0+三层防御回退 | ✅ E2E **101.50** |
| P1-API404 | kitchen/stats返回404 | 端点在kitchen-order路径下 | KitchenScanController补映射 | ✅ E2E PASS |
| M1 | createTableOrder缺防御 | 同类Bug复现风险 | 添加金额验证(与createOrder一致) | ✅ 编译 |
| M2 | 裸RuntimeException | 绕过全局异常处理器 | →BusinessException(400/500) ×3处 | ✅ 编译 |

### E. Critical#1 CAS锁 — 最终验证（🏆首次完整通过！）
```
之前状态: ⚠️ 机制正确但被金额Bug阻断无法E2E
现在状态: ✅ 9.8/10 完美通过
```
| 测试 | 内容 | 结果 |
|------|------|------|
| CAS-1 | 完整支付流程 | ✅ 创建(38.00)→支付成功→status **0→1** |
| CAS-2 | 重复支付防护 | ✅ 二次支付返回"订单已支付，请勿重复操作" |
| CAS-3 | 数据一致性 | ✅ DB字段全部正确 |

### F. 独立审查验收工作流（本次建立并验证的流程）
```
专家交付 → 独立code-review-expert审查 → 独立api-testing-expert验收 → 
独立maven-build-expert编译验证 → Leader汇总 → 发现问题→再派专家修复 → 
再独立审查 → 再验收 → ✅ PASS
```
**经验教训**: 必须在专家交付后立即执行独立审查和验收，不能仅作为建议。

---

### v0.10.0 回溯性审查结果
| 维度 | 分数 | 说明 |
|------|------|------|
| 功能正确性 | 9/10 | 核心功能完整，发现setInterval内存泄漏已修复 |
| 规范合规 | 9/10 | InvoiceVerifyResult缺字段已补充 |
| 代码质量 | 9/10 | 架构设计优秀(ISP/门面模式/AtomicLong) |
| 潜在风险 | 8/10 | 2个遗留问题已全部修复 |

### 认证体系状态
| 功能 | 状态 | 备注 |
|------|------|------|
| 登录 | ✅ 正常 | BCrypt + JWT HS512 + RBAC |
| JWT双令牌 | ✅ 正常 | AccessToken 2h + RefreshToken 30d |
| 图形验证码 | ✅ 正常 | Kaptcha |
| 菜单系统 | ✅ 正常 | admin有*权限→全部路由可见 |

### 已知技术债务
| # | 债务 | 状态 |
|---|------|------|
| T1 | Delombok后1382文件手动维护getter/setter | 开放 |
| T2 | H2与PostgreSQL语法差异硬编码 | PG迁移脚本已写(T1缓解) |
| T3 | 部分Vue组件接近1000行上限 | 大部分已拆分 |
| T4 | 前端部分页面为空壳/占位符 | P2剩余项 |

---

## 五、下一步应该做什么？（优先级排序）

### ✅ v0.12.2 E2E全链路验证通过（已完成）

**POS → Kitchen 端到端完整链路 6/6 全部通过：**
```
✅ Step 1: POS Create Order (POST /v1/pos/order)     → code=0
✅ Step 2: Kitchen Pending Orders                    → 1 order received
✅ Step 3: Receive (接单 POST /receive)              → code=0, status=received
✅ Step 4: Start Making (制作中 POST /start-make)    → code=0, status=making
✅ Step 5: Complete Make (完成制作 /complete-make)   → code=0, status=ready  ← v0.12.2修复!
✅ Step 6: Serve (上菜 POST /serve)                  → code=0, status=served
```

**🔧 v0.12.2 修复的4个关键问题：**

| # | 问题 | 根因 | 修复 |
|---|------|------|------|
| 1 | **Complete-Make回滚** | Controller重复调用generateFoodTraceCodes + 三重嵌套@Transactional + eventPublisher在事务内 | 移除Controller重复调用 + 去掉3处嵌套@Transactional + Controller层直接publishEvent |
| 2 | **eventPublisher污染事务(v0.12.1遗留)** | createOrder内publishEvent触发@EventListener注册同步标记rollback | TransactionSynchronization.afterCommit()延迟发布 |
| 3 | **OperationLogAspect污染事务** | createLog()默认REQUIRED传播级别 | REQUIRES_NEW |
| 4 | **H2 Schema缺失(11处)** | 7张表缺少共100+列 | DatabaseInitConfig新增7个fix方法 |

### ✅ v0.12.2 独立代码审查（25项全修复）

**审查范围**：4月5日-6日期间所有快速调试变更（8个核心文件）

| 类别 | 数量 | 关键项 |
|------|------|--------|
| 🔴 调试残留 | 9项 | debug_steps/目录(9文件删除)、[STEP]标记(15处)、=== ===分隔符(8处)、System.out.println(2处) |
| 🟠 安全/事务 | 4项 | refundOrder缺@Transactional、clearTestData无权限控制(@Profile dev)、OperationLogAspect传播级别 |
| ✅ 验证结果 | | 编译零错误 + E2E回归6/6全通过 |

### ✅ v0.13.0 — Phase 2 业务闭环修复（本次完成）

**问题背景**：
> POS收银端(`frontend-pos`)的结算页面 `Payment.vue` 中，用户点「确认支付」按钮后，
> **只调用了 `createOrder()` 创建订单，从未调用 `payOrder()` 完成支付**。
> 导致订单永远停留在 status=0(待支付) 状态，支付记录为空，KitchenOrder 未同步为 paid。

**修复内容（6个文件）：**

| # | 文件 | 修改 | 说明 |
|---|------|------|------|
| 1 | [PosOrderController.java](backend/src/main/java/com/example/demo/controller/PosOrderController.java) | payOrder L704 | 修复未定义变量`orderNumber`→`request.getOrderId()`，增加订单查询日志 |
| 2 | [PosOrderController.java](backend/src/main/java/com/example/demo/controller/PosOrderController.java) | createOrder L194 | `orderSource`硬编码1(小程序)→**4(POS终端)** |
| 3 | [PosOrderController.java](backend/src/main/java/com/example/demo/controller/PosOrderController.java) | createTableOrder L630 | 补充缺失的 `orderSource = 4` |
| 4 | [Order.java](backend/src/main/java/com/example/demo/entity/Order.java) | L113 | orderSource Schema注释扩展添加"4-POS终端" |
| 5 | [posApi.ts](frontend-pos/src/api/posApi.ts) | 新增 | PayRequest接口 + OrderResultItem接口 + OrderResult增强 + **payOrder() API方法** |
| 6 | [Payment.vue](frontend-pos/src/views/Payment.vue) | **核心重构** | confirmPayment从单步(createOrder)改为**两步流程**(create→pay)，步骤指示器动态化 |

**流程对比：**
```
❌ Before: 点"确认支付" → createOrder() → status=0(待支付) → 结束(从未支付!)
✅ After:  点"确认支付" → Step1:createOrder(status=0) → Step2:payOrder(status=0→1) → 完成
```

**orderSource 枚举扩展：**
```
0-APP / 1-小程序 / 2-公众号 / 3-网页 / 🆕 4-POS终端
```

**验证结果：后端 mvn compile ✅ + 前端 npm run build ✅**

**📄 新增产出（v0.12.2遗留）：**
- [postgres-schema-v0.12.sql](backend/src/main/resources/sql/postgres-schema-v0.12.sql) — 65表正式PG DDL脚本（99.8KB）

### 🟡 剩余P2收尾项（~2项，低优先级）

| # | 任务 | 预估工时 | 说明 |
|---|------|---------|------|
| 1 | 系统设置空壳页面填充 | 4-8h | 19个空壳页需逐个开发（建议按需启用） |
| 2 | 前端分号策略统一执行 | 2h | 277个混合文件，需加ESLint规则后逐步迁移 |

### 🔴 数据库Schema修复（⚠️ 高优先级 — 审计发现的问题）

| # | 严重度 | 问题 | 建议 |
|---|--------|------|------|
| 1 | 🔴 致命 | SysPermission表名Entity与DDL不匹配 | 统一表名 |
| 2 | 🔴 致命 | UserRole.userId类型错误(Long vs String) | 修正类型 |
| 3 | 🔴 致命 | Role主键混乱 | 统一主键策略 |
| 4 | 🔴 致命 | Food/FoodTraceCode大量字段缺失 | 补齐DDL或Entity |
| 5 | 🔴 致命 | (第5项) | 待详细确认 |
| 6-12 | 🟠严重 | AuditLog缺7列/KitchenOrder缺字段/等 | 逐项补齐 |
| 13 | ⚠️ PG跨环境风险 | 迁移脚本主键列名与@TableId不匹配 | 统一PG DDL |

> **⚠️ 以上数据库问题需要用户回归后逐一确认再修复！**

### 🚀 Phase 2 业务闭环（✅ 已完成 v0.13.0）

**Phase 2.1 — 收银端业务逻辑 ✅**
- ✅ 商品分类展示 / 商品搜索 / 购物车管理
- ✅ **下单结算流程（createOrder→payOrder 两步闭环）** ← v0.13.0修复
- ✅ **支付集成模拟（微信/支付宝/现金/余额）** ← v0.13.0修复
- ⏳ 小票打印（调用硬件驱动层SerialPortDriver）
- ⏳ 会员积分/折扣计算

**Phase 2.2 — 后厨端业务逻辑**
- ✅ 订单看板（WebSocket实时推送）
- ✅ 菜品状态切换（待制作→制作中→已完成→已上菜）6/6全通过
- ⏳ 超时订单提醒
- ⏳ 出餐确认/打印

**Phase 2.3 — 全链路模拟测试**
- ✅ POS下单 → 后厨接单 → 状态流转 → E2E验证6/6
- ✅ **多渠道orderSource枚举扩展（新增POS终端=4）**

---

## 六、启动必做清单（下一个窗口必须先做这些）

### 第一步：加载上下文（按顺序读取）
1. **`PROJECT_PROGRESS.md`** — 全局进度和待办事项（**每次必读！**）
2. **`.harness/PROTOCOL.md`** — Agent工作协议和规范
3. **`.harness/SESSION_ASSIGNMENT.md`** — 任务分派方案
4. **`.harness/skills/post-work-checklist.md`** — 工作结束检查标准
5. **本文档（HANDOFF.md）** — 当前交接内容

### 第二步：确认环境状态
```powershell
# 启动后端（必须使用 h2 profile！）
cd p:\my-new-project\backend; $env:SPRING_PROFILES_ACTIVE="h2"; mvn spring-boot:run -DskipTests
# 启动前端（另一个终端）
cd p:\my-new-project\frontend; npm run dev
```
> ⚠️ 注意：默认 application.yml 配置的是 MySQL，开发环境必须传 h2 profile！

### 第三步：建立 Todo 追踪
根据上面的"下一步"规划建立 todo list。

### 第四步：向用户确认方向
在开始执行前，跟用户确认下一步要做什么。

### 第五步：每轮工作结束后必须做三件事
> ⚠️ **本次教训：之前遗漏了这三步**

1. **审查验收本轮工作**（派 code-review-expert 或自行审查）
2. **更新 PROJECT_PROGRESS.md 和 HANDOFF.md**（版本号+变更内容+状态）
3. **制定下一步推进计划**（明确优先级和预估工时）

---

## 七、关键文件速查索引

### 后端核心（修改频率高）
| 文件 | 用途 | v0.10.1 变更 |
|------|------|-------------|
| `hardware/HardwareDriver.java` | 基础驱动接口 | ISP重构为10方法基础接口 |
| `hardware/TextBasedDriver.java` | 🆕 文本协议子接口 | 新建 |
| `hardware/BinaryDriver.java` | 🆕 二进制协议子接口 | 新建(Hex编码规范) |
| `hardware/AbstractHardwareDriver.java` | 抽象基类 | AtomicLong线程安全+Javadoc全覆盖 |
| `hardware/DriverRegistry.java` | 驱动注册中心 | inferConnectionType推断+重复检测 |
| `hardware/utils/HexUtils.java` | 🆕 十六进制工具类 | 新建(从SerialPortDriver提取) |
| `hardware/driver/SerialPortDriver.java` | 串口驱动 | implements TextBased+Binary, 用HexUtils |
| `hardware/driver/NetworkDriver.java` | 网络驱动 | implements TextBased+Binary, Base64→Hex |
| `hardware/driver/UsbDeviceDriver.java` | USB驱动 | 仅实现HardwareDriver, disconnect资源清理 |
| `config/DatabaseInitConfig.java` | H2初始化 | 改表结构必须同步这里 |
| `security/filter/AnomalyAccessLogFilter.java` | 异常日志过滤器 | **必须保留 copyBodyToResponse()** |

### 前端核心（修改频率高）
| 文件 | 用途 | v0.10.1 变更 |
|------|------|-------------|
| `api/request.ts` | 统一请求实例 | **禁止绕过！** |
| `api/electronicVoucher.ts` | 电子凭证API | +manualInput/reimburse方法+InvoiceVerifyResult补字段 |
| `api/auditLog.ts` | 🆕 审计日志API | 新建 |
| `api/permission/user.ts` | 用户API | unknown→具体类型 |
| `api/permission.ts` | 权限API兼容层 | @deprecated Facade模式 |
| `constants/voucher.ts` | 🆕 凭证枚举常量 | 新建(VoucherStatus/SignatureStatus/VerifyStatus) |
| `composables/useElectronicVoucher.ts` | 凭证门面 | 976行→172行门面 |
| `composables/voucher/*.ts` | 🆕 5个子composable | 新建(List/Detail/Upload/Accounting/Reimburse) |
| `views/.../RoleManagement.vue` | 角色管理 | DataScopeConfig props修复 |
| `views/.../OrganizationSettings.vue` | 组织架构 | 重写618行(树形展示+部门CRUD) |
| `views/.../Security.vue` | 账号安全 | 真实API+绑定对话框+定时器清理 |
| `views/Finance/components/VoucherDetailDialog.vue` | 凭证详情 | 424行→195行+3子Tab组件 |
| `components/Voucher/InvoiceRenderer.vue` | 发票渲染器 | 删除本地接口, 复用InvoiceVO |
| `router/index.ts` | 全局路由 | 删除4条UserCenter重复路由 |
| `stores/auth.ts` | 认证状态 | login/getUserInfo后必须sync permissionStore |

---

## 八、常见陷阱（前人踩过的坑 + 新增）

| # | 陷阱 | 影响 | 解决方案 |
|---|------|------|---------|
| 1 | AnomalyAccessLogFilter 忘记 copyBodyToResponse() | **所有API返回空body** | 已修复，勿移除finally中的调用 |
| 2 | auth.ts 未同步 permissionStore | **登录后菜单消失** | login()/getUserInfo()后加 initFromAuthStore() |
| 3 | 直接使用 axios 而非 request 实例 | **Token不注入/拦截器失效** | 强制规则，禁止绕过 |
| 4 | 响应数据再访问 .data 属性 | **undefined 错误** | 响应拦截器已提取data，直接用返回值 |
| 5 | 物理删除（DELETE语句） | **数据永久丢失** | 必须用 @TableLogic 逻辑删除 |
| 6 | TypeScript 使用 any/unknown 类型 | **类型安全丢失** | unknown已在v0.10.0清零 |
| 7 | Vue组件超过1000行 | **维护困难** | EV已拆分12文件, OrgSettings 618行 |
| 8 | **ref<boolean>.value = false()** | **TypeError: false is not a function** | 🆕 v0.10.0发现并修复，注意不要把false当函数调用 |
| 9 | **setInterval 未在 onBeforeUnmount 清理** | **内存泄漏** | 🆕 v0.10.1回溯审查发现并修复 |
| 10 | **后端启动必须传 h2 profile** | **MySQL连接失败** | `$env:SPRING_PROFILES_ACTIVE="h2"` |
| 11 | **每轮工作结束不更新文档** | **上下文断裂** | 🆕 **本次教训：必须审查+更新文档+制定计划** |
| 12 | **UsbProtocol 无 disconnect 方法** | **编译失败** | Leader已修复：移除不存在的方法调用 |
| 13 | **嵌套List/@Valid缺失** | **DTO约束全部失效** | 集合字段必须加@Valid才能级联验证内元素(Spring Boot 3.x Jakarta Validation) |
| 14 | **Lombok在此项目已禁用** | **@Data编译失败** | pom.xml为lombok_DISABLED，必须手写getter/setter(项目有1382文件Delombok展开) |
| 15 | **DB价格为0覆盖前端价格** | **订单金额totalAmount=0** | 仅当`dbFood.getPrice()!=null && dbFood.getPrice().compareTo(ZERO)>0`时才用DB价格覆盖 |
| 16 | **专家交付后不执行独立审查** | **问题延迟发现** | 必须执行: 专家交付→独立审查→独立验收→Leader汇总，不能仅作为建议 |

---

## 九、项目规范摘要

### 后端强制规范
- 逻辑删除：`@TableLogic` + `deleted` 字段
- 统一返回：`Result<T>` 对象
- DTO接收参数：`@Valid` 校验
- 禁止物理删除：用 `removeById()` / `removeByIds()`
- Javadoc：公共接口/方法必须有中文注释
- 线程安全：多线程共享变量用 AtomicXxx

### 前端强制规范
- API请求：只用 `request` 实例（`request.get(url, params)` 格式）
- 禁止 axios/fetch 直接使用
- 禁止 any/unknown 类型
- 禁止 console.log（调试代码）
- 响应数据直接用（不访问 .data）
- Vue组件 <1000行, TS文件 <800行, CSS <600行
- 魔术数字：必须提取为枚举常量（见 constants/voucher.ts）

### 工作流强制规范
- **每轮结束必须三件事**：审查验收 + 文档更新 + 下一步计划
- **每个P0/P1修复后必须编译验证**：mvn compile + npm run build
- **跨窗口协调**：检查 .harness/inbox/ 是否有其他窗口消息

---

## 十、版本线

| 版本 | 日期 | 内容 |
|------|------|------|
| v0.1.0 ~ v0.8.0 | 04-03~04-05 | 初始化→财务模块→Delombok→认证修复→Harness→多会话→冒烟测试→后端双轨 |
| v0.9.0 | 04-05 | Session B前端矩阵 + Phase 1收官 |
| v0.9.1 | 04-05 | P1收尾: SMTP邮件 + Redis连接 |
| **v0.10.0** | **04-05** | **Leader-Agent首秀: 深度验证(3Agent) + P0修复5项 + P1修复6项 + 风格修复 + ISP重构 + Composable拆分** |
| **v0.10.1** | **04-05** | **P2优化8项(Javadoc+HexUtils+Registry+枚举+路由+loading+类型+拆分) + 回溯审查(v0.10.0遗留2项修复) + 双轨F1-F4并行** |
| **v0.11.0** | **04-05** | **无人值守: 阶段三API验证(覆盖率100%/95%+scan-serve补齐) + 8/8联调PASS + P2收尾4项(魔术数字/log遮蔽/TODO/导入清理)** |
| **v0.12.0** | **04-05** | **无人值守P2收尾+数据库审计: console.error统一(24文件50处) + 日期去重(3处) + Security.vue 6TODO实现 + permission.ts文档化(20引用) + 系统设置19页审计 + 分号策略分析(692文件) + 数据库19实体审计(5致命/7严重/7轻微)** |
| **v0.12.1** | **04-06** | **E2E端到端联调: POS下单→Kitchen接收验证通过 + @Transactional rollback根因修复(eventPublisher→TransactionSynchronization) + H2 Schema修复11处(Food/Orders/KitchenOrder/FoodTraceCode/CallRecord/SysOperationLogs) + OperationLogAspect.REQUIRES_NEW + Kitchen状态流转3/4通过** |
| **v0.12.2** | **04-06** | **Complete-Make修复+全链路6/6通过: 移除嵌套@Transactional(3处) + Controller重复调用移除 + eventPublisher直接发布(非事务上下文) + FoodTraceCodeService.generate()去事务化 + PG正式DDL脚本生成(65表99.8KB) + E2E验证POS→Receive→Make→Complete→Serve全部code=0** |
| **v0.13.0** | **04-06** | **独立审查25项全修复(debug_steps删除/诊断日志清理/System.out修复/@Transactional补充/@Profile安全) + Phase 2业务闭环: POS支付流程两步修复(create→pay) + orderSource多渠道扩展(POS终端=4) + payOrder未定义变量修复 + 前后端编译双通过** |
| **v0.13.1** | **04-07** | **安全加固全闭环: DTO验证@Valid嵌套修复(4轮PASS 98.75) + CAS锁E2E 9.8/10首次完整验证 + P1全清5项(SQL方言/金额=0/API404/M1/M2) + 收银端Controller拆分(940→114+Service 961) + N+1优化95%查询减少 + 5支付方式+挂单 + 厨房端完整实现(680行看板+5API+批量操作+12快捷键+触摸CSS+音效) + 6专家并行交付+独立审查验收工作流建立** |

> **下一版本目标：v0.14.0 — 数据库Schema修复 + permission.ts废弃层移除 + 小票打印/会员积分等POS增强功能**

---

*文档结束。下一窗口请从「五、下一步应该做什么」开始与用户确认方向。*
*⚠️ 记住：每轮工作结束必须完成「审查验收 + 文档更新 + 下一步计划」三件事！*
