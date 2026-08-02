# 食品溯源系统 — P0 必做项实现设计规范（V1）

> **文档定位**：基于 DEVELOPMENT_STATUS.md Section 9「完整产品缺失审查」识别的 60 项缺失（其中 27 项 P0），为本轮 P0 必做项提供完整的实现前设计规范，确保实施过程不出现"孤岛"（前端不调用后端）和"摆件"（后端建好无人用）问题。
> **生成时间**：2026-06-25
> **配套文档**：
> - [DATA_LIFECYCLE.md](DATA_LIFECYCLE.md) V3 — 数据流转全景（含第 19 节站内通信、第 20 节基础设施支撑系统）
> - [DEVELOPMENT_STATUS.md](DEVELOPMENT_STATUS.md) V2 第二轮 — 完整产品缺失审查（Section 9）
> - [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) V2 — 项目上下文
> **强制执行**：本规范为 27 项 P0 必做项实施的**唯一权威设计依据**，所有实现必须严格遵循。

---

## 0. 文档说明

### 0.1 文档目的

本项目当前最大的工程问题是"建好了不用"——大量基础设施已建设但未与业务模块联动：

| 典型问题 | 后端状态 | 前端状态 | 后果 |
|---------|---------|---------|------|
| 打印任务系统 | PrintTaskController 11 端点完整 | 0 业务模块调用 | 后厨无工单、收银无小票 |
| 站内通信系统 | NotificationController 25 端点完整 | 20 类业务事件未触发通知 | 通知中心空空如也 |
| 数据导出系统 | ExportTask 表已建 | 仅 1 端点暴露 | 12 类导出场景缺失 |
| 订单管理后端 | OrderNewController 100% 完整 | 4 页 100% Mock | 前后端完全割裂 |
| 站内信渠道 | SiteNotificationService 完整 | 与 RabbitMQ SITE_MSG 双系统割裂 | 通知数据分散两套表 |

本规范通过 **5 项强制防护机制**，确保本次 27 项 P0 实施不再产生同类问题：

1. **前端调用点清单（强制）** — 每个 API 必须明确前端调用位置
2. **跨模块联动检查清单（强制）** — 每个功能必须列出上下游联动
3. **验收测试用例（强制）** — 每个功能必须有可测试的判断标准
4. **通知触发点（强制）** — 每个功能必须明确通知时机和接收人
5. **前端 Mock 移除检查清单（强制）** — 每个功能完成后必须清理 Mock

### 0.2 适用范围

本规范覆盖 DEVELOPMENT_STATUS.md Section 9.9 列出的 27 项 P0 必做项，分 4 批执行：

| 批次 | 内容 | 项数 | 详细设计章节 |
|------|------|------|------------|
| 第一批 | 业务流程完整性 + 入职注册联动 | 10 项 | 第 3 章 |
| 第二批 | 站内通信事件触发 | 5 项 | 第 4 章 |
| 第三批 | 基础设施对接 + 跨模块联动 | 7 项 | 第 5 章 |
| 第四批 | 财务专业 + 移动/连锁/合规 | 5 项 | 第 6 章 |

### 0.3 与其他文档的关系

```
┌─ PROJECT_CONTEXT.md（项目上下文，模块清单）
│
├─ DATA_LIFECYCLE.md（数据流转全景）
│   ├─ 第 1 节 人事域（招聘/入职/在职/离职/归档）
│   ├─ 第 19 节 站内通信/消息中心系统
│   └─ 第 20 节 基础设施支撑系统（文件/打印/导出）
│
├─ DEVELOPMENT_STATUS.md（开发进度与缺失审查）
│   ├─ Section 0.4 用户开发策略决策（工作台/系统设置延后）
│   ├─ Section 9 完整产品缺失审查（60 项缺失）
│   └─ Section 9.9 第二轮优先级排序（27 项 P0）
│
└─ IMPLEMENTATION_DESIGN.md（本文件，27 项 P0 实现设计规范）
    ├─ 第 1 章 防孤岛/防摆件设计原则
    ├─ 第 2 章 通用技术规范
    ├─ 第 3-6 章 4 批 27 项详细设计
    └─ 第 7-8 章 实施顺序 + 验收清单
```

### 0.4 修订记录

| 版本 | 日期 | 修订内容 | 修订人 |
|------|------|---------|--------|
| V1.0 | 2026-06-25 | 初始版本，覆盖 27 项 P0 全部详细设计 | — |

---

## 1. 防孤岛/防摆件设计原则（强制）

### 1.1 五大强制防护机制概述

每一项 P0 功能的详细设计**必须**包含以下 5 个章节，缺一不可：

| # | 防护机制 | 章节 | 目的 |
|---|---------|------|------|
| 1 | 前端调用点清单 | X.Y.5 | 防止后端 API 无人调用（孤岛） |
| 2 | 跨模块联动检查清单 | X.Y.6 | 防止功能自成闭环不联动（孤岛） |
| 3 | 通知触发点 | X.Y.7 | 防止业务事件无感知（摆件） |
| 4 | 验收测试用例 | X.Y.8 | 防止功能不可测试验证（摆件） |
| 5 | 前端 Mock 移除检查清单 | X.Y.9 | 防止 Mock 残留（孤岛） |

### 1.2 前端调用点清单规范

**目的**：确保每个后端 API 都有明确的前端调用方，避免"建好没人用"。

**强制要求**：
- 每个 API 端点必须列出**至少 1 个**前端调用点
- 调用点必须精确到：`页面文件路径` + `组件/方法名` + `触发时机`
- 无前端调用点的 API **不允许开发**
- 前端调用点变更时必须同步更新本清单

**模板**：
```markdown
#### X.Y.5 前端调用点清单【强制】

| API 端点 | HTTP | 页面文件 | 组件/方法 | 触发时机 |
|---------|------|---------|----------|---------|
| /v1/recruitment-quotas | POST | views/hr/HRRecruitment.vue | handleCreateQuota() | HR 点击"下发名额"按钮 |
| /v1/recruitment-quotas/{id}/confirm | PUT | views/store-management/StoreRecruitment.vue | handleConfirmQuota() | 门店收到名额通知后点击"确认" |
```

**审查要点**：
- [ ] 每个 API 至少 1 个前端调用点
- [ ] 调用点文件路径真实存在（或本次同步创建）
- [ ] 触发时机描述清晰
- [ ] 无"待对接""暂未使用"等模糊描述

### 1.3 跨模块联动检查清单规范

**目的**：确保每个功能与上下游模块正确联动，避免"自成闭环"。

**强制要求**：
- 每个功能必须列出**上游触发事件**（哪些模块的事件会触发本功能）
- 每个功能必须列出**下游影响事件**（本功能完成后会触发哪些下游）
- 联动通过 **Spring ApplicationEvent** 或 **RabbitMQ 消息**实现
- 未联动的必须说明原因（如"无下游"或"延后到第 X 批"）

**模板**：
```markdown
#### X.Y.6 跨模块联动检查清单【强制】

**上游触发事件**：
| 事件源 | 事件名 | 触发条件 | 监听方式 |
|--------|--------|---------|---------|
| HR 模块 | QuotaIssuedEvent | HR 下发名额 | @EventListener |

**下游影响事件**：
| 事件名 | 触发条件 | 接收方 | 实现方式 |
|--------|---------|--------|---------|
| QuotaConfirmedEvent | 门店确认名额 | HR 模块 | ApplicationEventPublisher |
| NotificationTriggerEvent | 名额下发完成 | 通知模块 | RabbitMQ msg.send.exchange |
```

**审查要点**：
- [ ] 上下游事件均已列出
- [ ] 事件实现方式明确（ApplicationEvent / RabbitMQ）
- [ ] 接收方模块明确
- [ ] 无"暂未实现""延后"等模糊描述（除非明确标注批次）

### 1.4 通知触发点规范

**目的**：确保每个业务事件都触发对应通知，避免"事件发生但无人知晓"。

**强制要求**：
- 每个状态变更（创建/审批/完成/拒绝等）必须评估是否需要通知
- 需要通知的必须明确：**时机** + **模板编码** + **接收人** + **渠道**
- 通知模板必须**先在 msg_template 表注册**，再在代码中引用
- 接收人必须明确（具体角色/具体用户/动态查询）

**模板**：
```markdown
#### X.Y.7 通知触发点【强制】

| 时机 | 模板编码 | 接收人 | 渠道 | 备注 |
|------|---------|--------|------|------|
| 名额下发完成 | recruitment.quota.issued | 门店店长 | SITE_MSG+EMAIL | 含名额数/岗位/有效期 |
| 门店确认名额 | recruitment.quota.confirmed | HR 专员 | SITE_MSG | 含确认时间/门店名 |
| 名额即将耗尽 | recruitment.quota.exhausting | 门店店长+HR | SITE_MSG | usedCount >= headcount*0.8 |
```

**通知模板注册规范**：
```sql
INSERT INTO msg_template (template_code, template_name, template_type, title_template, content_template, variables, enabled)
VALUES (
  'recruitment.quota.issued',
  '招聘名额下发通知',
  'SITE_MSG',
  '【招聘名额】您收到 {year} 年第 {quarter} 季度招聘名额',
  '门店：{storeName}\n岗位：{positionName}\n名额数：{headcount}\n有效期：{expireDate}\n请及时确认并提报招聘需求。',
  '["year","quarter","storeName","positionName","headcount","expireDate"]',
  TRUE
);
```

**审查要点**：
- [ ] 每个状态变更都评估了通知需求
- [ ] 通知模板已在 msg_template 表注册
- [ ] 接收人明确（非"相关人"等模糊描述）
- [ ] 渠道明确（SITE_MSG/EMAIL/SMS/WEBHOOK）

### 1.5 验收测试用例规范

**目的**：确保每个功能可测试、可验证，避免"看起来完成了实际不能用"。

**强制要求**：
- 每个功能至少 **3 个**验收用例（正常流程 + 异常流程 + 边界情况）
- 用例必须包含：**前置条件** + **操作步骤** + **预期结果** + **验证方式**
- 验证方式必须可执行（API 调用/数据库查询/UI 操作）
- 验收未通过的功能**不允许标记为完成**

**模板**：
```markdown
#### X.Y.8 验收测试用例【强制】

**TC-001：正常流程 — HR 下发名额到门店**
- 前置条件：HR 已登录，门店存在，岗位存在
- 操作步骤：
  1. 调用 POST /v1/recruitment-quotas 创建名额
  2. 查询 GET /v1/recruitment-quotas/{id}
- 预期结果：
  - 名额记录创建成功，status='issued'
  - 门店店长收到站内通知
  - notification 表新增 1 条记录
- 验证方式：
  - API：GET /v1/recruitment-quotas/{id} 返回 200
  - DB：SELECT * FROM notification WHERE business_type='recruitment_quota' AND business_id={id}
  - UI：门店店长登录后 NotificationCenter 铃铛显示未读 +1

**TC-002：异常流程 — 名额超限校验**
- 前置条件：名额 headcount=2，usedCount=2（已用完）
- 操作步骤：门店提报第 3 个招聘需求
- 预期结果：API 返回 422，错误码 3005「名额已用完」
- 验证方式：API 调用返回 422 + code=3005
```

**审查要点**：
- [ ] 至少 3 个用例（正常 + 异常 + 边界）
- [ ] 前置条件可准备
- [ ] 验证方式可执行
- [ ] 预期结果明确

### 1.6 前端 Mock 移除检查清单规范

**目的**：确保实现完成后前端 Mock 代码被清理，避免"Mock 残留导致看似可用实际假数据"。

**强制要求**：
- 实现前必须列出**所有需要移除的 Mock 代码位置**
- 实现后必须逐一确认移除
- Mock 移除必须**与后端对接同步进行**，不可先移除再对接（会导致页面空白）
- 移除后必须运行 `npm run build` 验证无 TypeScript 错误

**模板**：
```markdown
#### X.Y.9 前端 Mock 移除检查清单【强制】

| Mock 位置 | 文件 | 行号 | Mock 内容 | 替换为 | 状态 |
|----------|------|------|----------|--------|------|
| HRRecruitment.vue handleOnboardHire() | views/hr/HRRecruitment.vue | L234-256 | mockHireRecord.push() | POST /v1/onboarding-records | ☐ 待移除 |
| recruitment.ts fetchRequirements() | api/hr/recruitment.ts | L12-30 | return mockData | GET /v1/recruitment-requirements | ☐ 待移除 |

**移除顺序**：
1. 先完成后端 API 实现
2. 前端 API 封装层对接（api/hr/recruitment.ts）
3. 前端组件层对接（views/hr/HRRecruitment.vue）
4. 移除 Mock 代码
5. 运行 `npm run build` 验证
```

**审查要点**：
- [ ] 所有 Mock 位置已列出
- [ ] 替换目标明确
- [ ] 移除顺序合理（后端→API→组件→Mock）
- [ ] 验证步骤包含 `npm run build`

### 1.7 实施流程（设计→评审→实现→验收）

每一项 P0 功能的实施必须严格遵循以下流程：

```
┌─────────────────────────────────────────────────────────────────────┐
│                    P0 功能实施流程（强制）                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─[Step 1: 设计评审]                                               │
│  │   - 开发者阅读本规范对应章节                                      │
│  │   - 确认数据模型/状态机/API 契约/联动/通知/验收 全部理解          │
│  │   - 如有疑问，先与本规范维护者确认，不可自行修改设计              │
│  │                              ↓                                   │
│  │  ┌─[Step 2: 后端实现]                                            │
│  │  │   - 数据库迁移脚本（V{version}__{description}.sql）           │
│  │  │   - 实体类 + Mapper + DataService + Service + Controller     │
│  │  │   - 注册通知模板（msg_template 表 INSERT）                    │
│  │  │   - 实现跨模块联动（ApplicationEvent / RabbitMQ）             │
│  │  │   - 单元测试 + 集成测试                                       │
│  │  │   - mvn compile 通过                                          │
│  │  │                              ↓                                │
│  │  │  ┌─[Step 3: 前端对接]                                         │
│  │  │  │   - API 封装层（api/{module}/xxx.ts）                      │
│  │  │  │   - 组件层对接（views/{module}/xxx.vue）                   │
│  │  │  │   - 按 X.Y.5 前端调用点清单逐一对接                        │
│  │  │  │   - 按 X.Y.9 移除 Mock 代码                                │
│  │  │  │   - npm run build 通过                                     │
│  │  │  │                              ↓                             │
│  │  │  │  ┌─[Step 4: 联调验收]                                      │
│  │  │  │  │   - 按 X.Y.8 验收测试用例逐一执行                       │
│  │  │  │  │   - 确认跨模块联动（X.Y.6）真实发生                     │
│  │  │  │  │   - 确认通知触发（X.Y.7）真实推送                       │
│  │  │  │  │   - 确认 Mock 已移除（X.Y.9）                           │
│  │  │  │  │   - 全部用例 PASS 才可标记完成                          │
│  │  │  │  │                              ↓                          │
│  │  │  │  │  ┌─[Step 5: 文档更新]                                   │
│  │  │  │  │  │   - 更新 DATA_LIFECYCLE.md（标注已完成）             │
│  │  │  │  │  │   - 更新 DEVELOPMENT_STATUS.md（完成度上调）         │
│  │  │  │  │  │   - 更新本规范 X.Y 章节状态为 ✅                     │
└──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┘
```

**关键规则**：
- ❌ **禁止**跳过 Step 1 直接实现
- ❌ **禁止**先实现后端不实现前端对接（会导致孤岛）
- ❌ **禁止**实现完成后不移除 Mock（会导致摆件）
- ❌ **禁止**跳过验收测试用例直接标记完成
- ✅ **允许**在实现过程中发现设计问题，但必须先更新本规范再继续

---

## 2. 通用技术规范

### 2.1 命名规范补充

#### 2.1.1 数据库表命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 业务表 | 小写+下划线，复数 | `recruitment_quotas`, `job_offers` |
| 关联表 | {a}_{b}_rel | `recruitment_quota_store_rel` |
| 日志表 | {entity}_logs | `notification_logs` |
| 配置表 | {entity}_settings | `notification_settings` |

#### 2.1.2 主键命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 内部表 | `{table}_id` BIGINT GENERATED ALWAYS AS IDENTITY | `quota_id` |
| 对外暴露表 | `{table}_id` VARCHAR(32) 雪花算法 | `quota_id` |

**本次新增表主键策略**：

| 表 | 主键类型 | 理由 |
|----|---------|------|
| recruitment_quotas | BIGINT 自增 | 内部表，无外部引用 |
| recruitment_feedback | BIGINT 自增 | 内部表 |
| job_offers | BIGINT 自增 | 内部表 |
| certificate_reimbursements | BIGINT 自增 | 内部表 |
| resignations | BIGINT 自增 | 内部表 |
| position_default_roles | BIGINT 自增 | 配置表 |

#### 2.1.3 Java 类命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 实体 | {Entity} | `RecruitmentQuota` |
| Mapper | {Entity}Mapper | `RecruitmentQuotaMapper` |
| DataService | {Entity}DataService | `RecruitmentQuotaDataService` |
| Service | {Entity}Service | `RecruitmentQuotaService` |
| ServiceImpl | {Entity}ServiceImpl | `RecruitmentQuotaServiceImpl` |
| Controller | {Entity}Controller | `RecruitmentQuotaController` |
| CreateDTO | {Entity}CreateDTO | `RecruitmentQuotaCreateDTO` |
| UpdateDTO | {Entity}UpdateDTO | `RecruitmentQuotaUpdateDTO` |
| QueryDTO | {Entity}QueryDTO | `RecruitmentQuotaQueryDTO` |
| BasicInfo | {Entity}BasicInfo | `RecruitmentQuotaBasicInfo` |
| VO | {Entity}VO | `RecruitmentQuotaVO` |
| Event | {Entity}{Action}Event | `QuotaIssuedEvent` |

#### 2.1.4 API 路径命名

| 类型 | 规范 | 示例 |
|------|------|------|
| 资源集合 | /v1/{entity-kebab} | `/v1/recruitment-quotas` |
| 单资源 | /v1/{entity-kebab}/{id} | `/v1/recruitment-quotas/123` |
| 子动作 | /v1/{entity-kebab}/{id}/{action} | `/v1/recruitment-quotas/123/confirm` |
| 批量动作 | /v1/{entity-kebab}/batch/{action} | `/v1/recruitment-quotas/batch/issue` |

### 2.2 状态机设计规范

#### 2.2.1 状态字段类型

- **数据库**：VARCHAR(32)，存储状态码（如 `'issued'`）
- **Java 实体**：String
- **前端**：语义化字符串（如 `'issued'`）
- **禁止**：使用数字编码（与项目状态字段映射规范冲突）

#### 2.2.2 状态流转实现

```java
/**
 * 状态机工具类
 */
public class StateMachine<S extends Enum<S>> {
    private final Map<S, Set<S>> transitions;

    public StateMachine(Map<S, Set<S>> transitions) {
        this.transitions = transitions;
    }

    public void validateTransition(S from, S to) {
        Set<S> allowed = transitions.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                String.format("非法状态流转: %s → %s", from, to));
        }
    }
}
```

#### 2.2.3 状态机文档化

每个状态机必须在本规范的对应章节以图形方式文档化：

```
状态A ──[触发条件1]──→ 状态B
  │                      │
  └──[触发条件2]──→ 状态C ──[触发条件3]──→ 状态D
```

### 2.3 错误码分配

#### 2.3.1 错误码段位分配（本次新增）

| 模块 | 段位 | 已用 | 本次新增 |
|------|------|------|---------|
| 招聘名额 | 3000-3099 | 3000-3010 | 3011-3030 |
| 招聘反馈 | 3100-3199 | — | 3100-3120 |
| Offer | 3200-3299 | — | 3200-3220 |
| 入职注册 | 3300-3399 | — | 3300-3320 |
| 证件报销 | 3400-3499 | — | 3400-3420 |
| 离职 | 3500-3599 | — | 3500-3520 |
| 通知触发 | 3600-3699 | — | 3600-3620 |
| 打印联动 | 3700-3799 | — | 3700-3720 |
| 导出中心 | 3800-3899 | — | 3800-3820 |
| 财务报表 | 3900-3999 | — | 3900-3920 |

#### 2.3.2 错误码格式

```java
public enum ErrorCode {
    // 招聘名额模块 3011-3030
    QUOTA_NOT_FOUND(3011, "招聘名额不存在"),
    QUOTA_ALREADY_ISSUED(3012, "名额已下发，不可重复下发"),
    QUOTA_EXHAUSTED(3013, "名额已用完"),
    QUOTA_INVALID_TRANSITION(3014, "名额状态流转非法"),
    QUOTA_HEADCOUNT_INVALID(3015, "名额数必须大于 0"),
    QUOTA_EXPIRED(3016, "名额已过期"),
    // ... 3017-3030 预留
}
```

### 2.4 事务边界规范

#### 2.4.1 事务注解

```java
@Service
public class RecruitmentQuotaServiceImpl implements RecruitmentQuotaService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecruitmentQuotaVO issueQuota(RecruitmentQuotaCreateDTO dto) {
        // 1. 校验
        // 2. 创建名额记录
        // 3. 发布事件（在事务内）
        // 4. 触发通知（事务提交后异步）
    }
}
```

#### 2.4.2 事务边界规则

| 操作类型 | 事务范围 | 备注 |
|---------|---------|------|
| 单表 CRUD | @Transactional | 标准事务 |
| 多表写入 | @Transactional | 同一事务 |
| 跨模块联动 | 事务提交后 | @TransactionalEventListener |
| 通知发送 | 事务提交后 | RabbitMQ 异步 |
| 文件上传 | 无事务 | 文件先上传，元数据后入库 |

#### 2.4.3 跨模块事件发布

```java
/**
 * 事务提交后发布事件（避免事务回滚但事件已发出）
 */
@Component
public class EventPublisher {
    @Autowired
    private ApplicationEventPublisher publisher;

    public void publishAfterCommit(Object event) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.publishEvent(event);
                }
            }
        );
    }
}
```

### 2.5 缓存策略规范

#### 2.5.1 缓存键格式

```
{entity}:basic:{entityId}
```

示例：
- `recruitment_quota:basic:1234567890`
- `job_offer:basic:9876543210`

#### 2.5.2 缓存过期时间

| 数据类型 | 过期时间 | 说明 |
|---------|---------|------|
| 招聘名额 | 1 小时 | 变更频率中等 |
| Offer | 30 分钟 | 变更频率较高 |
| 证件报销 | 30 分钟 | 变更频率较高 |
| 离职记录 | 24 小时 | 变更频率低 |

#### 2.5.3 缓存更新策略

```java
@Service
public class RecruitmentQuotaDataServiceImpl implements RecruitmentQuotaDataService {

    @Override
    @CacheEvict(key = "#quotaId", value = "recruitmentQuota")
    public void clearRecruitmentQuotaCache(String quotaId) {
        // 先清 L1 Caffeine
        caffeineCache.invalidate(quotaId);
        // 再清 L2 Redis
        redisTemplate.delete("recruitment_quota:basic:" + quotaId);
    }
}
```

### 2.6 审计日志规范

#### 2.6.1 审计日志记录

所有 P0 功能的关键操作必须记录审计日志：

```java
@Aspect
@Component
public class AuditLogAspect {

    @AfterReturning(pointcut = "@annotation(auditLog)", returning = "result")
    public void recordAudit(JoinPoint joinPoint, AuditLog auditLog, Object result) {
        AuditLogRecord record = AuditLogRecord.builder()
            .module(auditLog.module())
            .action(auditLog.action())
            .operatorId(SecurityUtils.getCurrentUserId())
            .operatorName(SecurityUtils.getCurrentUserName())
            .targetId(extractTargetId(result))
            .requestParams(toJson(joinPoint.getArgs()))
            .result(toJson(result))
            .operationTime(LocalDateTime.now())
            .build();
        auditLogService.save(record);
    }
}
```

#### 2.6.2 审计日志表

```sql
CREATE TABLE audit_log (
    audit_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    module VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    operator_id BIGINT NOT NULL,
    operator_name VARCHAR(100) NOT NULL,
    target_id VARCHAR(64),
    target_type VARCHAR(50),
    request_params TEXT,
    result TEXT,
    ip_address VARCHAR(45),
    operation_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);
```

### 2.7 数据库迁移规范

#### 2.7.1 迁移脚本命名

```
V{YYYYMMDD}__create_{table_names}.sql
```

示例：
- `V20260626__create_recruitment_quotas_and_feedback.sql`
- `V20260627__create_job_offers_and_resignations.sql`

#### 2.7.2 迁移脚本规范

```sql
-- ============ 招聘名额系统 ============
-- 创建人：SYSTEM
-- 创建时间：2026-06-26
-- 说明：HR 向门店下发招聘名额

CREATE TABLE recruitment_quotas (
    quota_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    year INTEGER NOT NULL,
    quarter INTEGER NOT NULL CHECK (quarter IN (1, 2, 3, 4)),
    store_id BIGINT NOT NULL,
    store_name VARCHAR(100) NOT NULL,
    position_id BIGINT NOT NULL,
    position_name VARCHAR(100) NOT NULL,
    headcount INTEGER NOT NULL CHECK (headcount > 0),
    used_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    issued_by BIGINT,
    issued_time TIMESTAMP,
    confirmed_by BIGINT,
    confirmed_time TIMESTAMP,
    expire_date DATE,
    remark VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

-- 索引
CREATE INDEX idx_recruitment_quotas_store ON recruitment_quotas(store_id);
CREATE INDEX idx_recruitment_quotas_status ON recruitment_quotas(status);
CREATE INDEX idx_recruitment_quotas_year_quarter ON recruitment_quotas(year, quarter);
CREATE UNIQUE INDEX uk_recruitment_quotas_store_pos_year_quarter
    ON recruitment_quotas(store_id, position_id, year, quarter) WHERE deleted = 0;
```

#### 2.7.3 禁止事项

- ❌ **禁止**修改已执行的迁移脚本
- ❌ **禁止**在迁移脚本中使用 `DROP TABLE`（除非是回滚脚本）
- ❌ **禁止**在迁移脚本中写入业务数据（仅结构变更）
- ✅ **允许**在迁移脚本中插入配置类数据（如 msg_template 注册）

### 2.8 API 响应规范

#### 2.8.1 统一响应格式

```java
{
    "code": 0,           // 0=成功，非 0=失败
    "message": "操作成功",
    "data": { ... },
    "timestamp": 1712345678901
}
```

#### 2.8.2 分页响应格式

```java
{
    "code": 0,
    "message": "操作成功",
    "data": {
        "records": [ ... ],
        "total": 100,
        "current": 1,
        "size": 10,
        "pages": 10
    },
    "timestamp": 1712345678901
}
```

#### 2.8.3 错误响应格式

```java
{
    "code": 3013,
    "message": "名额已用完",
    "data": null,
    "timestamp": 1712345678901
}
```

#### 2.8.4 Controller 模板

```java
@Tag(name = "招聘名额管理", description = "HR 向门店下发招聘名额")
@RestController
@RequestMapping("/v1/recruitment-quotas")
@RequiredArgsConstructor
public class RecruitmentQuotaController {

    private final RecruitmentQuotaService quotaService;

    @Operation(summary = "创建招聘名额（HR 下发）")
    @PostMapping
    public Result<RecruitmentQuotaVO> create(
            @Valid @RequestBody RecruitmentQuotaCreateDTO dto) {
        return Result.success(quotaService.issueQuota(dto));
    }

    @Operation(summary = "门店确认名额")
    @PutMapping("/{quotaId}/confirm")
    public Result<RecruitmentQuotaVO> confirm(@PathVariable Long quotaId) {
        return Result.success(quotaService.confirmQuota(quotaId));
    }

    @Operation(summary = "分页查询名额")
    @GetMapping
    public Result<PageResponse<RecruitmentQuotaVO>> page(RecruitmentQuotaQueryDTO query) {
        return Result.success(quotaService.pageQuotas(query));
    }
}
```

---

## 3. 第一批：业务流程完整性（10 项）

> **批次目标**：补齐人事域全链路 6 个核心功能（招聘名额/协同反馈/Offer/入职注册/证件报销/离职），并完成对应的 4 项跨模块联动。
> **批次依赖**：本批次是后续批次的基础——站内通信事件触发（第二批）依赖本批次的业务事件源；基础设施对接（第三批）依赖本批次的附件场景。
> **预计工作量**：6 张新表 + 6 套 Controller/Service + 6 个前端页面 + 20+ 通知模板 + 30+ 验收用例

### 3.1 招聘名额系统（HR→门店名额下发）

#### 3.1.1 功能概述

| 项 | 内容 |
|----|------|
| **业务目标** | HR 总部按年度/季度向门店下发招聘名额，门店在名额范围内提报招聘需求，避免无限额约束 |
| **依赖项** | 门店表（stores）、岗位表（positions）、用户表（users）、通知系统（已有） |
| **优先级** | P0 |
| **关联文档** | DATA_LIFECYCLE.md 环节 1.1、DEVELOPMENT_STATUS.md 9.2 #1 |

#### 3.1.2 数据模型

**表：`recruitment_quotas`（招聘名额表）**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| quota_id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | 主键 |
| year | INTEGER | NOT NULL | 年度 |
| quarter | INTEGER | NOT NULL, CHECK IN (1,2,3,4) | 季度 |
| store_id | BIGINT | NOT NULL | 门店 ID |
| store_name | VARCHAR(100) | NOT NULL | 门店名称（冗余） |
| position_id | BIGINT | NOT NULL | 岗位 ID |
| position_name | VARCHAR(100) | NOT NULL | 岗位名称（冗余） |
| headcount | INTEGER | NOT NULL, CHECK > 0 | 名额数 |
| used_count | INTEGER | NOT NULL, DEFAULT 0 | 已用名额数 |
| status | VARCHAR(32) | NOT NULL, DEFAULT 'draft' | 状态 |
| issued_by | BIGINT | | 下发人 ID（HR） |
| issued_time | TIMESTAMP | | 下发时间 |
| confirmed_by | BIGINT | | 确认人 ID（门店店长） |
| confirmed_time | TIMESTAMP | | 确认时间 |
| expire_date | DATE | | 名额有效期 |
| remark | VARCHAR(500) | | 备注 |
| create_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 更新时间 |
| deleted | INTEGER | NOT NULL, DEFAULT 0 | 逻辑删除 |

**索引**：
- `idx_recruitment_quotas_store` (store_id)
- `idx_recruitment_quotas_status` (status)
- `idx_recruitment_quotas_year_quarter` (year, quarter)
- `uk_recruitment_quotas_store_pos_year_quarter` UNIQUE (store_id, position_id, year, quarter) WHERE deleted=0

**实体类**：`RecruitmentQuota`（com.example.demo.entity）
**Mapper**：`RecruitmentQuotaMapper` extends BaseMapper<RecruitmentQuota>

#### 3.1.3 状态机

```
                 ┌─────────────────────────────────────────────────┐
                 ↓                                                 │
  draft ──[HR下发]──→ issued ──[门店确认]──→ active ──[用完]──→ exhausted ──[HR关闭]──→ closed
                          │                       │
                          │                       └──[HR关闭]──→ closed
                          │
                          ├──[门店拒绝]──→ rejected
                          │
                          └──[门店申请追加]──→ adjustment_requested ──[HR审核通过]──→ active
                                                                  └──[HR驳回]──→ issued
```

**状态枚举**：

| 状态 | 说明 | 触发动作 |
|------|------|---------|
| draft | 草稿 | HR 创建名额未下发 |
| issued | 已下发 | HR 点击"下发" |
| active | 已生效 | 门店店长点击"确认接受" |
| exhausted | 已用完 | used_count >= headcount |
| closed | 已关闭 | HR 手动关闭或过期 |
| rejected | 已拒绝 | 门店店长点击"拒绝接受" |
| adjustment_requested | 申请追加 | 门店店长点击"申请追加" |

**状态流转校验**（StateMachine 配置）：

```java
Map<String, Set<String>> transitions = Map.of(
    "draft", Set.of("issued"),
    "issued", Set.of("active", "rejected", "adjustment_requested", "closed"),
    "adjustment_requested", Set.of("active", "issued"),
    "active", Set.of("exhausted", "closed"),
    "exhausted", Set.of("closed")
);
```

#### 3.1.4 API 契约

**Controller**：`RecruitmentQuotaController`（`/v1/recruitment-quotas`）

| # | 端点 | HTTP | 说明 | 请求体 | 响应 |
|---|------|------|------|--------|------|
| 1 | `/v1/recruitment-quotas` | POST | HR 创建并下发名额 | RecruitmentQuotaCreateDTO | RecruitmentQuotaVO |
| 2 | `/v1/recruitment-quotas/{quotaId}` | GET | 查询名额详情 | — | RecruitmentQuotaVO |
| 3 | `/v1/recruitment-quotas` | GET | 分页查询 | RecruitmentQuotaQueryDTO | PageResponse<RecruitmentQuotaVO> |
| 4 | `/v1/recruitment-quotas/{quotaId}/confirm` | PUT | 门店确认名额 | — | RecruitmentQuotaVO |
| 5 | `/v1/recruitment-quotas/{quotaId}/reject` | PUT | 门店拒绝名额 | {reason: String} | RecruitmentQuotaVO |
| 6 | `/v1/recruitment-quotas/{quotaId}/request-adjustment` | PUT | 门店申请追加 | {additionalCount: Integer, reason: String} | RecruitmentQuotaVO |
| 7 | `/v1/recruitment-quotas/{quotaId}/approve-adjustment` | PUT | HR 审核追加申请 | {approved: Boolean, remark: String} | RecruitmentQuotaVO |
| 8 | `/v1/recruitment-quotas/{quotaId}/close` | PUT | HR 关闭名额 | — | RecruitmentQuotaVO |
| 9 | `/v1/recruitment-quotas/store/{storeId}` | GET | 按门店查询可用名额 | — | List<RecruitmentQuotaVO> |
| 10 | `/v1/recruitment-quotas/{quotaId}/validate` | POST | 校验名额可用（提报招聘需求时调用） | {count: Integer} | {valid: Boolean, remaining: Integer} |

**CreateDTO**：
```java
public class RecruitmentQuotaCreateDTO {
    @NotNull private Integer year;
    @NotNull @Min(1) @Max(4) private Integer quarter;
    @NotNull private Long storeId;
    @NotNull private Long positionId;
    @NotNull @Min(1) private Integer headcount;
    private LocalDate expireDate;
    private String remark;
}
```

**VO**：包含所有字段 + `remaining`（剩余名额 = headcount - used_count）

#### 3.1.5 前端调用点清单【强制】

| API 端点 | HTTP | 页面文件 | 组件/方法 | 触发时机 |
|---------|------|---------|----------|---------|
| /v1/recruitment-quotas | POST | views/hr/RecruitmentQuotaManagement.vue（新建） | handleIssueQuota() | HR 点击"下发名额"按钮提交表单 |
| /v1/recruitment-quotas/{quotaId} | GET | views/hr/RecruitmentQuotaManagement.vue | handleViewDetail() | HR 点击某行"查看"按钮 |
| /v1/recruitment-quotas | GET | views/hr/RecruitmentQuotaManagement.vue | fetchQuotaList() | 页面加载/筛选/分页 |
| /v1/recruitment-quotas/{quotaId}/confirm | PUT | views/store-management/StoreRecruitment.vue | handleConfirmQuota() | 门店店长收到名额通知后点击"确认接受" |
| /v1/recruitment-quotas/{quotaId}/reject | PUT | views/store-management/StoreRecruitment.vue | handleRejectQuota() | 门店店长点击"拒绝接受" |
| /v1/recruitment-quotas/{quotaId}/request-adjustment | PUT | views/store-management/StoreRecruitment.vue | handleRequestAdjustment() | 门店店长点击"申请追加" |
| /v1/recruitment-quotas/{quotaId}/approve-adjustment | PUT | views/hr/RecruitmentQuotaManagement.vue | handleApproveAdjustment() | HR 审核追加申请 |
| /v1/recruitment-quotas/{quotaId}/close | PUT | views/hr/RecruitmentQuotaManagement.vue | handleCloseQuota() | HR 点击"关闭名额" |
| /v1/recruitment-quotas/store/{storeId} | GET | views/store-management/StoreRecruitment.vue | fetchAvailableQuotas() | 门店进入招聘需求提报页时加载可用名额 |
| /v1/recruitment-quotas/{quotaId}/validate | POST | views/store-management/StoreRecruitment.vue | validateQuotaBeforeSubmit() | 门店提报招聘需求前校验名额 |

**前端文件清单（新建）**：
- `frontend/src/api/hr/recruitment-quota.ts` — API 封装
- `frontend/src/types/hr/recruitment-quota.ts` — 类型定义
- `frontend/src/views/hr/RecruitmentQuotaManagement.vue` — HR 端名额管理页（新建）
- `frontend/src/views/store-management/StoreRecruitment.vue` — 门店端招聘页（已存在，需新增名额确认/拒绝/追加功能区块）

**菜单注册**：
- HR 模块新增二级菜单"招聘名额管理"，路由 `/hr/recruitment-quota`
- 门店模块的 `StoreRecruitment.vue` 增加"名额管理"Tab

#### 3.1.6 跨模块联动检查清单【强制】

**上游触发事件**：
| 事件源 | 事件名 | 触发条件 | 监听方式 |
|--------|--------|---------|---------|
| HR 模块 | — | HR 下发名额（本功能自身触发，无上游） | — |

**下游影响事件**：
| 事件名 | 触发条件 | 接收方 | 实现方式 |
|--------|---------|--------|---------|
| `QuotaIssuedEvent` | HR 下发名额（status→issued） | 通知模块（向门店店长发通知） | ApplicationEvent + @TransactionalEventListener(AFTER_COMMIT) |
| `QuotaConfirmedEvent` | 门店确认名额（status→active） | 通知模块（向 HR 发通知） | ApplicationEvent |
| `QuotaRejectedEvent` | 门店拒绝名额（status→rejected） | 通知模块（向 HR 发通知） | ApplicationEvent |
| `QuotaAdjustmentRequestedEvent` | 门店申请追加 | 通知模块（向 HR 发通知） | ApplicationEvent |
| `QuotaExhaustedEvent` | used_count >= headcount | 通知模块（向 HR + 门店发通知） | ApplicationEvent |
| `QuotaClosedEvent` | HR 关闭名额 | 通知模块（向门店发通知） | ApplicationEvent |

**与"门店招聘需求提报"的联动**：
- 门店提报招聘需求（POST /v1/recruitment-requirements）时，**必须先调用** `/v1/recruitment-quotas/{quotaId}/validate` 校验名额
- 名额校验通过后，招聘需求创建成功
- 招聘需求对应员工入职成功后，调用 `quotaService.incrementUsedCount(quotaId)` 使 used_count += 1
- used_count 达到 headcount 时触发 `QuotaExhaustedEvent`

**联动实现位置**：
- `RecruitmentRequirementServiceImpl.createRequirement()` 中新增名额校验调用
- `OnboardingRecordServiceImpl.completeOnboarding()` 中新增名额使用计数调用（依赖 3.4 入职注册联动）

#### 3.1.7 通知触发点【强制】

| 时机 | 模板编码 | 接收人 | 渠道 | 备注 |
|------|---------|--------|------|------|
| 名额下发完成 | `recruitment.quota.issued` | 门店店长（按 store_id 查询） | SITE_MSG + EMAIL | 含年度/季度/岗位/名额数/有效期 |
| 门店确认名额 | `recruitment.quota.confirmed` | 下发 HR（issued_by） | SITE_MSG | 含门店名/岗位/确认时间 |
| 门店拒绝名额 | `recruitment.quota.rejected` | 下发 HR | SITE_MSG | 含门店名/拒绝原因 |
| 门店申请追加 | `recruitment.quota.adjustment_requested` | 下发 HR | SITE_MSG + EMAIL | 含门店名/岗位/申请数量/原因 |
| HR 审核追加结果 | `recruitment.quota.adjustment_result` | 门店店长 | SITE_MSG | 含审核结果/备注 |
| 名额即将耗尽（80%） | `recruitment.quota.exhausting` | 门店店长 + HR | SITE_MSG | used_count >= headcount*0.8 时触发（定时任务） |
| 名额已用完 | `recruitment.quota.exhausted` | 门店店长 + HR | SITE_MSG | used_count >= headcount 时触发 |
| 名额已关闭 | `recruitment.quota.closed` | 门店店长 | SITE_MSG | 含关闭原因 |

**通知模板注册**（迁移脚本中 INSERT）：
- `recruitment.quota.issued` — 标题：「招聘名额」您收到 {year}年第{quarter}季度招聘名额
- `recruitment.quota.confirmed` — 标题：「招聘名额」{storeName} 已确认接收名额
- `recruitment.quota.rejected` — 标题：「招聘名额」{storeName} 拒绝接收名额
- `recruitment.quota.adjustment_requested` — 标题：「招聘名额」{storeName} 申请追加名额
- `recruitment.quota.adjustment_result` — 标题：「招聘名额」您的追加申请已审核
- `recruitment.quota.exhausting` — 标题：「招聘名额」{storeName} {positionName} 名额即将用完
- `recruitment.quota.exhausted` — 标题：「招聘名额」{storeName} {positionName} 名额已用完
- `recruitment.quota.closed` — 标题：「招聘名额」{storeName} {positionName} 名额已关闭

#### 3.1.8 验收测试用例【强制】

**TC-001：正常流程 — HR 下发名额到门店**
- 前置条件：HR 已登录（role=HR_MANAGER），门店"上海人民广场店"存在（store_id=1001），岗位"服务员"存在（position_id=2001）
- 操作步骤：
  1. 调用 `POST /v1/recruitment-quotas`，body: `{year:2026, quarter:3, storeId:1001, positionId:2001, headcount:5, expireDate:"2026-09-30"}`
  2. 调用 `GET /v1/recruitment-quotas/{quotaId}`
- 预期结果：
  - 步骤 1 返回 200，code=0，data.status='issued'，data.issuedBy=当前 HR 用户 ID
  - 步骤 2 返回 200，data.headcount=5，data.usedCount=0，data.remaining=5
  - notification 表新增 1 条记录，business_type='recruitment_quota'，business_id=quotaId
  - 门店店长（store_id=1001 的店长）的 NotificationCenter 未读数 +1
- 验证方式：
  - API：上述两个端点返回正确
  - DB：`SELECT * FROM notification WHERE business_type='recruitment_quota' AND business_id={quotaId}`
  - UI：门店店长登录后 NotificationCenter 铃铛显示未读 +1，点击查看有"招聘名额下发"通知

**TC-002：异常流程 — 名额超限校验**
- 前置条件：名额 quota_id=5001，headcount=2，used_count=2（已用完），status='exhausted'
- 操作步骤：
  1. 调用 `POST /v1/recruitment-quotas/5001/validate`，body: `{count:1}`
- 预期结果：返回 200，code=3013，data.valid=false，data.remaining=0
- 验证方式：API 返回 code=3013「名额已用完」

**TC-003：边界情况 — 门店申请追加名额**
- 前置条件：名额 quota_id=5002，status='active'，headcount=3，used_count=2
- 操作步骤：
  1. 门店店长调用 `PUT /v1/recruitment-quotas/5002/request-adjustment`，body: `{additionalCount:2, reason:"周末客流增加"}`
  2. HR 调用 `PUT /v1/recruitment-quotas/5002/approve-adjustment`，body: `{approved:true, remark:"同意追加"}`
- 预期结果：
  - 步骤 1 后 status='adjustment_requested'，notification 表新增追加申请通知
  - 步骤 2 后 status='active'，headcount=5（原 3 + 追加 2），notification 表新增审核结果通知
- 验证方式：
  - API：两次调用返回正确状态
  - DB：`SELECT headcount, status FROM recruitment_quotas WHERE quota_id=5002`
  - DB：`SELECT * FROM notification WHERE business_id=5002 ORDER BY create_time DESC LIMIT 2`

**TC-004：联动验证 — 招聘需求提报时名额校验**
- 前置条件：名额 quota_id=5003，status='active'，headcount=2，used_count=1
- 操作步骤：
  1. 门店调用 `POST /v1/recruitment-requirements` 提报招聘需求，body 中包含 quotaId=5003
- 预期结果：
  - 招聘需求创建成功
  - 名额 used_count 不变（提报时不增加，入职成功时才增加）
- 验证方式：
  - API：招聘需求创建返回 200
  - DB：`SELECT used_count FROM recruitment_quotas WHERE quota_id=5003` 仍为 1

**TC-005：联动验证 — 入职成功后名额计数增加**
- 前置条件：名额 quota_id=5003，used_count=1，关联的招聘需求 requirement_id=8001，候选人已完成入职
- 操作步骤：
  1. HR 调用 `POST /v1/onboarding-records/{id}/complete` 完成入职
- 预期结果：
  - 入职成功
  - 名额 used_count=2（增加 1）
  - 若 used_count 达到 headcount，触发 `QuotaExhaustedEvent`，notification 表新增"名额已用完"通知
- 验证方式：
  - DB：`SELECT used_count, status FROM recruitment_quotas WHERE quota_id=5003`
  - 若 exhausted：`SELECT * FROM notification WHERE business_type='recruitment_quota' AND template_code='recruitment.quota.exhausted'`

#### 3.1.9 前端 Mock 移除检查清单【强制】

| Mock 位置 | 文件 | 行号 | Mock 内容 | 替换为 | 状态 |
|----------|------|------|----------|--------|------|
| 无 | — | — | 本功能为全新功能，无既有 Mock | — | ☐ N/A |

**说明**：本功能为全新功能，前端 `RecruitmentQuotaManagement.vue` 为新建页面，无 Mock 需移除。但需确认：
- [ ] 新建 `api/hr/recruitment-quota.ts` 使用 `request` 实例（禁止 axios）
- [ ] 新建 `views/hr/RecruitmentQuotaManagement.vue` 使用 StandardPage 组件
- [ ] 新建 `types/hr/recruitment-quota.ts` 类型定义完整（无 any）
- [ ] 状态字段使用 StatusTag 组件展示（禁止 el-tag）
- [ ] 操作列使用 `el-button link` 纯文字按钮

---

### 3.2 门店↔HR 招聘协同反馈

#### 3.2.1 功能概述

| 项 | 内容 |
|----|------|
| **业务目标** | 解决门店招聘（type='store'）和 HR 招聘（type='hr'）双系统割裂，门店提报后 HR 可反馈（通过/修改/驳回），门店可协同面试 |
| **依赖项** | 招聘需求表（recruitment_requirements，已有）、面试表（interviews，已有）、招聘名额系统（3.1） |
| **优先级** | P0 |
| **关联文档** | DATA_LIFECYCLE.md 环节 1.2、DEVELOPMENT_STATUS.md 9.2 #2 |

#### 3.2.2 数据模型

**新增表：`recruitment_feedback`（招聘反馈表）**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| feedback_id | BIGINT | PK, GENERATED ALWAYS AS IDENTITY | 主键 |
| requirement_id | BIGINT | NOT NULL | 关联招聘需求 ID |
| feedback_type | VARCHAR(20) | NOT NULL, CHECK IN ('approve','feedback','reject') | 反馈类型 |
| content | TEXT | NOT NULL | 反馈内容 |
| reviewer_id | BIGINT | NOT NULL | 审核人 ID（HR） |
| reviewer_name | VARCHAR(100) | NOT NULL | 审核人姓名 |
| review_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 审核时间 |
| create_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| deleted | INTEGER | NOT NULL, DEFAULT 0 | |

**修改表：`interviews`（面试表，已有）增加门店协同字段**：

| 新增字段 | 类型 | 说明 |
|---------|------|------|
| store_interviewer_id | BIGINT | 门店面试官 ID |
| store_interviewer_name | VARCHAR(100) | 门店面试官姓名 |
| store_evaluation | TEXT | 门店面评 |
| store_evaluation_time | TIMESTAMP | 门店面评提交时间 |
| store_evaluation_score | INTEGER | 门店面评评分（1-5） |

**索引**：
- `idx_recruitment_feedback_requirement` (requirement_id)
- `idx_recruitment_feedback_reviewer` (reviewer_id)

#### 3.2.3 状态机

**招聘需求状态机扩展**（在现有 recruitment_requirements.status 基础上增加）：

```
门店提报 → submitted ──[HR approve]──→ approved ──[HR接管]──→ recruiting
                │                                                 │
                ├──[HR feedback]──→ feedback_given ──[门店修改重新提交]──→ submitted
                │
                └──[HR reject]──→ rejected
```

**新增状态**：

| 状态 | 说明 |
|------|------|
| feedback_given | HR 反馈需修改，等待门店重新提交 |

#### 3.2.4 API 契约

**Controller**：`RecruitmentFeedbackController`（`/v1/recruitment-feedback`）

| # | 端点 | HTTP | 说明 |
|---|------|------|------|
| 1 | `/v1/recruitment-feedback` | POST | HR 提交反馈（approve/feedback/reject） |
| 2 | `/v1/recruitment-feedback/requirement/{requirementId}` | GET | 按需求查询反馈历史 |
| 3 | `/v1/interviews/{interviewId}/store-evaluation` | PUT | 门店提交面评 |
| 4 | `/v1/recruitment-requirements/{requirementId}/resubmit` | PUT | 门店修改后重新提交 |

#### 3.2.5 前端调用点清单【强制】

| API 端点 | HTTP | 页面文件 | 组件/方法 | 触发时机 |
|---------|------|---------|----------|---------|
| /v1/recruitment-feedback | POST | views/hr/HRRecruitment.vue | handleSubmitFeedback() | HR 在招聘需求列表点击"审核"按钮提交反馈 |
| /v1/recruitment-feedback/requirement/{requirementId} | GET | views/hr/HRRecruitment.vue | handleViewFeedbackHistory() | HR 点击"反馈历史"查看 |
| /v1/interviews/{interviewId}/store-evaluation | PUT | views/store-management/StoreRecruitment.vue | handleSubmitStoreEvaluation() | 门店面试官面试后提交面评 |
| /v1/recruitment-requirements/{requirementId}/resubmit | PUT | views/store-management/StoreRecruitment.vue | handleResubmit() | 门店收到 feedback 后修改并重新提交 |

#### 3.2.6 跨模块联动检查清单【强制】

**上游触发事件**：
| 事件源 | 事件名 | 触发条件 | 监听方式 |
|--------|--------|---------|---------|
| 门店模块 | `StoreRecruitmentSubmittedEvent` | 门店提报招聘需求 | @EventListener（HR 模块监听） |

**下游影响事件**：
| 事件名 | 触发条件 | 接收方 | 实现方式 |
|--------|---------|--------|---------|
| `RecruitmentFeedbackEvent` | HR 提交反馈 | 通知模块（向门店发通知） | ApplicationEvent |
| `RecruitmentApprovedEvent` | HR 审核通过 | HR 招聘模块（接管招聘流程） | ApplicationEvent |
| `StoreEvaluationSubmittedEvent` | 门店提交面评 | HR 模块（更新面试记录） | ApplicationEvent |

#### 3.2.7 通知触发点【强制】

| 时机 | 模板编码 | 接收人 | 渠道 |
|------|---------|--------|------|
| 门店提报招聘需求 | `recruitment.requirement.submitted` | HR 审核员 | SITE_MSG + EMAIL |
| HR 反馈需修改 | `recruitment.feedback.given` | 门店店长 + 提报人 | SITE_MSG |
| HR 审核通过 | `recruitment.approved` | 门店店长 | SITE_MSG |
| HR 驳回 | `recruitment.rejected` | 门店店长 + 提报人 | SITE_MSG |
| 门店提交面评 | `interview.store_evaluation.submitted` | HR 招聘员 | SITE_MSG |
| 门店重新提交 | `recruitment.requirement.resubmitted` | HR 审核员 | SITE_MSG |

#### 3.2.8 验收测试用例【强制】

**TC-001：正常流程 — HR 审核通过**
- 前置：门店提报招聘需求 requirement_id=8001，status='submitted'
- 操作：HR 调用 `POST /v1/recruitment-feedback`，body: `{requirementId:8001, feedbackType:'approve', content:'符合需求'}`
- 预期：
  - recruitment_requirements.status → 'approved'
  - recruitment_feedback 新增 1 条记录
  - 门店店长收到通知
- 验证：DB 查询 status + notification 表

**TC-002：正常流程 — HR 反馈需修改**
- 前置：requirement_id=8002，status='submitted'
- 操作：HR 调用反馈 API，feedbackType='feedback'
- 预期：status → 'feedback_given'，门店收到通知，门店可调用 resubmit 重新提交

**TC-003：异常流程 — 非 submitted 状态不可反馈**
- 前置：requirement_id=8003，status='approved'
- 操作：HR 调用反馈 API
- 预期：返回 422，code=3101「当前状态不可反馈」

**TC-004：门店协同面试面评**
- 前置：interview_id=9001，store_interviewer_id 已设置
- 操作：门店面试官调用 `PUT /v1/interviews/9001/store-evaluation`，body: `{score:4, evaluation:"沟通能力好"}`
- 预期：interviews 表更新 store_evaluation 字段，HR 收到通知

#### 3.2.9 前端 Mock 移除检查清单【强制】

| Mock 位置 | 文件 | 行号 | Mock 内容 | 替换为 | 状态 |
|----------|------|------|----------|--------|------|
| HRRecruitment.vue 招聘需求列表 | views/hr/HRRecruitment.vue | 待确认 | mockRequirements 数据 | GET /v1/recruitment-requirements 真实接口 | ☐ 待移除 |
| HRRecruitment.vue 面试管理 | views/hr/HRRecruitment.vue | 待确认 | mockInterviews 数据 | GET /v1/interviews 真实接口 | ☐ 待移除 |
| recruitment.ts | api/hr/recruitment.ts | 待确认 | return mockData | 真实 API 调用 | ☐ 待移除 |

---

### 3.3 录用 Offer 持久化

#### 3.3.1 功能概述

| 项 | 内容 |
|----|------|
| **业务目标** | 将录用数据从 Mock 持久化到数据库，实现录用→入职联动 |
| **依赖项** | 招聘需求表（已有）、简历表（已有）、面试表（已有） |
| **优先级** | P0 |
| **关联文档** | DATA_LIFECYCLE.md 环节 4、DEVELOPMENT_STATUS.md 9.2 #3 |

#### 3.3.2 数据模型

**新增表：`job_offers`（录用 Offer 表）**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| offer_id | BIGINT | PK, IDENTITY | 主键 |
| requirement_id | BIGINT | NOT NULL | 招聘需求 ID |
| resume_id | BIGINT | NOT NULL | 简历 ID |
| interview_id | BIGINT | | 面试 ID |
| candidate_name | VARCHAR(100) | NOT NULL | 候选人姓名 |
| position_id | BIGINT | NOT NULL | 岗位 ID |
| position_name | VARCHAR(100) | NOT NULL | 岗位名称 |
| store_id | BIGINT | | 门店 ID |
| store_name | VARCHAR(100) | | 门店名称 |
| proposed_salary | BIGINT | NOT NULL | 拟定薪资（分） |
| probation_months | INTEGER | NOT NULL, DEFAULT 3 | 试用期月数 |
| probation_salary | BIGINT | | 试用期薪资（分） |
| status | VARCHAR(32) | NOT NULL, DEFAULT 'pending' | 状态 |
| send_time | TIMESTAMP | | 发送时间 |
| accept_time | TIMESTAMP | | 接受时间 |
| reject_time | TIMESTAMP | | 拒绝时间 |
| reject_reason | VARCHAR(500) | | 拒绝原因 |
| entry_date | DATE | | 预期入职日期 |
| remark | VARCHAR(500) | | 备注 |
| create_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| deleted | INTEGER | NOT NULL, DEFAULT 0 | |

#### 3.3.3 状态机

```
pending ──[发送 Offer]──→ sent ──[候选人接受]──→ accepted ──[办理入职]──→ onboarded
                    │                       │
                    │                       └──[候选人拒绝]──→ rejected
                    │
                    └──[HR 撤回]──→ withdrawn
```

#### 3.3.4 API 契约

**Controller**：`JobOfferController`（`/v1/job-offers`）

| # | 端点 | HTTP | 说明 |
|---|------|------|------|
| 1 | `/v1/job-offers` | POST | 创建 Offer |
| 2 | `/v1/job-offers/{offerId}` | GET | 查询详情 |
| 3 | `/v1/job-offers` | GET | 分页查询 |
| 4 | `/v1/job-offers/{offerId}/send` | PUT | 发送 Offer 给候选人 |
| 5 | `/v1/job-offers/{offerId}/accept` | PUT | 候选人接受 |
| 6 | `/v1/job-offers/{offerId}/reject` | PUT | 候选人拒绝 |
| 7 | `/v1/job-offers/{offerId}/withdraw` | PUT | HR 撤回 |
| 8 | `/v1/job-offers/{offerId}/convert-to-onboarding` | POST | 接受后转入职（生成 OnboardingRecord） |

#### 3.3.5 前端调用点清单【强制】

| API 端点 | HTTP | 页面文件 | 组件/方法 | 触发时机 |
|---------|------|---------|----------|---------|
| /v1/job-offers | POST | views/hr/HRRecruitment.vue | handleCreateOffer() | HR 在面试通过后点击"发 Offer" |
| /v1/job-offers/{offerId} | GET | views/hr/HRRecruitment.vue | handleViewOffer() | 查看详情 |
| /v1/job-offers | GET | views/hr/HRRecruitment.vue | fetchOfferList() | Offer 列表加载 |
| /v1/job-offers/{offerId}/send | PUT | views/hr/HRRecruitment.vue | handleSendOffer() | HR 点击"发送" |
| /v1/job-offers/{offerId}/accept | PUT | views/hr/HRRecruitment.vue | handleAcceptOffer() | HR 代候选人接受（或候选人自助） |
| /v1/job-offers/{offerId}/reject | PUT | views/hr/HRRecruitment.vue | handleRejectOffer() | 候选人拒绝 |
| /v1/job-offers/{offerId}/withdraw | PUT | views/hr/HRRecruitment.vue | handleWithdrawOffer() | HR 撤回 |
| /v1/job-offers/{offerId}/convert-to-onboarding | POST | views/hr/HRRecruitment.vue | handleConvertToOnboarding() | 接受后点击"办理入职" |

#### 3.3.6 跨模块联动检查清单【强制】

**上游**：
| 事件源 | 事件名 | 触发条件 |
|--------|--------|---------|
| 面试模块 | `InterviewPassedEvent` | 面试通过 → 触发创建 Offer |

**下游**：
| 事件名 | 触发条件 | 接收方 |
|--------|---------|--------|
| `OfferAcceptedEvent` | 候选人接受 | 入职模块（准备生成 OnboardingRecord） |
| `OfferSentEvent` | 发送 Offer | 通知模块（通知候选人） |
| `OfferRejectedEvent` | 候选人拒绝 | 通知模块（通知 HR） |

**关键联动**：`convert-to-onboarding` 端点调用 `OnboardingRecordService.createOnboardingRecord()`，将 Offer 数据转为入职记录。

#### 3.3.7 通知触发点【强制】

| 时机 | 模板编码 | 接收人 | 渠道 |
|------|---------|--------|------|
| Offer 发送 | `offer.sent` | 候选人（邮箱） | EMAIL |
| 候选人接受 | `offer.accepted` | HR | SITE_MSG |
| 候选人拒绝 | `offer.rejected` | HR | SITE_MSG |
| Offer 撤回 | `offer.withdrawn` | 候选人 | EMAIL |

#### 3.3.8 验收测试用例【强制】

**TC-001：完整流程 — 创建→发送→接受→转入职**
- 操作：创建 Offer → 发送 → 接受 → convert-to-onboarding
- 预期：最终生成 OnboardingRecord，包含候选人信息/岗位/薪资/试用期

**TC-002：异常 — 未发送不可接受**
- 前置：offer status='pending'
- 操作：调用 accept
- 预期：返回 422，code=3201

**TC-003：联动验证 — 转入职后数据完整**
- 操作：convert-to-onboarding 后查询 OnboardingRecord
- 预期：包含 offer 的 positionId/proposedSalary/probationMonths/storeId

#### 3.3.9 前端 Mock 移除检查清单【强制】

| Mock 位置 | 文件 | 行号 | Mock 内容 | 替换为 | 状态 |
|----------|------|------|----------|--------|------|
| HRRecruitment.vue handleOnboardHire() | views/hr/HRRecruitment.vue | L234-256 | mockHireRecord.push() | POST /v1/job-offers + convert-to-onboarding | ☐ 待移除 |
| hireApi | api/hr/hire.ts | 全文件 | mock HireRecord | GET/POST /v1/job-offers | ☐ 待移除（整个文件可能废弃） |

---

### 3.4 入职→注册账号联动

#### 3.4.1 功能概述

| 项 | 内容 |
|----|------|
| **业务目标** | 入职完成后自动创建登录账号、分配角色权限、发送激活通知，员工可首次登录改密 |
| **依赖项** | OnboardingRecordService（已有，需扩展）、UserService（已有）、RoleService（已有）、position_default_roles 表（新建） |
| **优先级** | P0 |
| **关联文档** | DATA_LIFECYCLE.md 环节 5、DEVELOPMENT_STATUS.md 9.2 #4 |

#### 3.4.2 数据模型

**新增表：`position_default_roles`（岗位默认角色配置表）**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, IDENTITY | 主键 |
| position_id | BIGINT | NOT NULL | 岗位 ID |
| role_id | BIGINT | NOT NULL | 角色 ID |
| role_name | VARCHAR(100) | NOT NULL | 角色名称（冗余） |
| is_primary | BOOLEAN | NOT NULL, DEFAULT FALSE | 是否主角色 |
| create_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| deleted | INTEGER | NOT NULL, DEFAULT 0 | |

**修改表：`users`（用户表，已有）增加状态字段**：

| 新增字段 | 类型 | 说明 |
|---------|------|------|
| account_status | VARCHAR(32) | 账号状态：active/pending_activation/disabled |
| activation_token | VARCHAR(64) | 激活令牌（首次登录改密用） |
| activation_expire_time | TIMESTAMP | 激活令牌过期时间 |
| first_login_changed_password | BOOLEAN, DEFAULT FALSE | 是否已首次改密 |
| employee_id | BIGINT | 关联员工 ID（已有，确认存在） |

**索引**：`uk_position_default_roles_pos_role` UNIQUE (position_id, role_id) WHERE deleted=0

#### 3.4.3 状态机（User.account_status）

```
[入职完成] → pending_activation ──[首次登录改密]──→ active ──[HR 停用]──→ disabled
                                                                  └──[HR 启用]──→ active
```

#### 3.4.4 API 契约

**扩展 OnboardingRecordController**：

| # | 端点 | HTTP | 说明 |
|---|------|------|------|
| 1 | `/v1/onboarding-records/{id}/complete` | POST | 完成入职（已有，扩展：调用 createUserAccount） |
| 2 | `/v1/auth/first-login-change-password` | PUT | 首次登录改密 |
| 3 | `/v1/users/{userId}/activate` | PUT | HR 手动激活账号 |
| 4 | `/v1/users/{userId}/disable` | PUT | HR 停用账号 |
| 5 | `/v1/system/positions/{positionId}/default-roles` | GET | 查询岗位默认角色 |
| 6 | `/v1/system/positions/{positionId}/default-roles` | POST | 配置岗位默认角色 |

**核心服务方法**：

```java
public interface UserAccountService {
    /**
     * 入职完成后创建用户账号（7 步流程）
     * 1. 校验员工已创建
     * 2. 创建 User 账号（username=employeeNo, status=pending_activation）
     * 3. 查询 position_default_roles 获取岗位默认角色
     * 4. 分配角色（user_role 关联表）
     * 5. 分配数据权限（按 storeId/departmentId）
     * 6. 生成 activation_token（UUID）+ 设置过期时间（7 天）
     * 7. 发送账号激活通知（站内信+邮件）
     */
    User createUserAccount(Long employeeId, Long positionId, Long storeId);

    /**
     * 首次登录强制改密
     */
    void changePasswordOnFirstLogin(Long userId, String newPassword);
}
```

#### 3.4.5 前端调用点清单【强制】

| API 端点 | HTTP | 页面文件 | 组件/方法 | 触发时机 |
|---------|------|---------|----------|---------|
| /v1/onboarding-records/{id}/complete | POST | views/hr/HREmployee.vue | handleCompleteOnboarding() | HR 点击"完成入职"（已有，扩展后端逻辑） |
| /v1/auth/first-login-change-password | PUT | views/auth/FirstLoginChangePassword.vue（新建） | handleSubmit() | 员工首次登录后强制改密页提交 |
| /v1/users/{userId}/activate | PUT | views/hr/HREmployee.vue | handleActivateAccount() | HR 点击"激活账号" |
| /v1/users/{userId}/disable | PUT | views/hr/HREmployee.vue | handleDisableAccount() | HR 点击"停用账号" |
| /v1/system/positions/{positionId}/default-roles | GET | views/system/PositionManagement.vue | handleViewDefaultRoles() | 系统管理查看岗位角色配置 |
| /v1/system/positions/{positionId}/default-roles | POST | views/system/PositionManagement.vue | handleConfigDefaultRoles() | 系统管理配置岗位角色 |

**新建页面**：
- `views/auth/FirstLoginChangePassword.vue` — 首次登录改密页（路由 `/auth/first-login`，无需登录拦截）

#### 3.4.6 跨模块联动检查清单【强制】

**上游**：
| 事件源 | 事件名 | 触发条件 |
|--------|--------|---------|
| 入职模块 | `OnboardingCompletedEvent` | 入职完成（completeOnboarding 调用后） |

**下游**：
| 事件名 | 触发条件 | 接收方 |
|--------|---------|--------|
| `UserAccountCreatedEvent` | 账号创建成功 | 通知模块（发激活通知给员工） |
| `UserAccountActivatedEvent` | 首次改密完成 | HR 模块（更新员工状态） |

**关键联动**：
- `OnboardingRecordServiceImpl.completeOnboarding()` 末尾调用 `userAccountService.createUserAccount(employeeId, positionId, storeId)`
- **修复已知 Bug**：`createEmployeeProfile()` L143-166 未设置 `Employee.storeId`，必须修复

#### 3.4.7 通知触发点【强制】

| 时机 | 模板编码 | 接收人 | 渠道 |
|------|---------|--------|------|
| 账号创建完成 | `user.account.created` | 新员工 | SITE_MSG + EMAIL |
| 账号已激活 | `user.account.activated` | HR | SITE_MSG |

#### 3.4.8 验收测试用例【强制】

**TC-001：完整流程 — 入职→账号创建→首次改密**
- 前置：完成入职流程
- 操作：调用 complete → 检查 User 创建 → 模拟首次登录改密
- 预期：
  - User 表新增记录，account_status='pending_activation'，employee_id 关联
  - user_role 表新增角色关联（按 position_default_roles 配置）
  - notification 表有激活通知
  - 改密后 account_status='active'，first_login_changed_password=TRUE

**TC-002：Bug 修复验证 — Employee.storeId 正确设置**
- 操作：完成入职后查询 Employee
- 预期：store_id 字段非空，与 OnboardingRecord.storeId 一致

**TC-003：异常 — 岗位未配置默认角色**
- 前置：position_id=9999 无 position_default_roles 配置
- 操作：完成入职
- 预期：抛出 BusinessException，code=3301「岗位未配置默认角色，无法创建账号」

#### 3.4.9 前端 Mock 移除检查清单【强制】

| Mock 位置 | 文件 | 行号 | Mock 内容 | 替换为 | 状态 |
|----------|------|------|----------|--------|------|
| HREmployee.vue 入职完成 | views/hr/HREmployee.vue | 待确认 | 仅更新本地状态 | 真实调用 POST /v1/onboarding-records/{id}/complete | ☐ 待移除 |

---

### 3.5 门店证件报销流程

#### 3.5.1 功能概述

| 项 | 内容 |
|----|------|
| **业务目标** | 员工证件（健康证/食品安全管理员证/营业执照/卫生许可证）报销申请→门店初审→HR 复审→财务报销→费用归集→通知员工 |
| **依赖项** | 员工表（已有）、门店表（已有）、财务凭证/成本记录（已有）、文件附件系统（已有） |
| **优先级** | P0 |
| **关联文档** | DATA_LIFECYCLE.md 6.5.1、DEVELOPMENT_STATUS.md 9.2 #5 |

#### 3.5.2 数据模型

**新增表：`certificate_reimbursements`（证件报销表）**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| reimbursement_id | BIGINT | PK, IDENTITY | 主键 |
| employee_id | BIGINT | NOT NULL | 申请员工 ID |
| employee_name | VARCHAR(100) | NOT NULL | 员工姓名 |
| store_id | BIGINT | NOT NULL | 门店 ID |
| store_name | VARCHAR(100) | NOT NULL | 门店名称 |
| certificate_type | VARCHAR(32) | NOT NULL, CHECK IN ('health_cert','food_safety_mgr','business_license','hygiene_license') | 证件类型 |
| certificate_name | VARCHAR(100) | NOT NULL | 证件名称 |
| amount | BIGINT | NOT NULL | 报销金额（分） |
| receipt_attachment_id | BIGINT | NOT NULL | 收据附件 ID（file_attachment） |
| certificate_attachment_id | BIGINT | | 证件附件 ID |
| status | VARCHAR(32) | NOT NULL, DEFAULT 'pending' | 状态 |
| store_approver_id | BIGINT | | 门店初审人 ID |
| store_approver_name | VARCHAR(100) | | 门店初审人姓名 |
| store_approve_time | TIMESTAMP | | 门店初审时间 |
| store_approve_remark | VARCHAR(500) | | 门店初审备注 |
| hr_approver_id | BIGINT | | HR 复审人 ID |
| hr_approver_name | VARCHAR(100) | | HR 复审人姓名 |
| hr_approve_time | TIMESTAMP | | HR 复审时间 |
| hr_approve_remark | VARCHAR(500) | | HR 复审备注 |
| finance_voucher_id | BIGINT | | 关联财务凭证 ID |
| reimburse_time | TIMESTAMP | | 报销到账时间 |
| create_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| deleted | INTEGER | NOT NULL, DEFAULT 0 | |

**索引**：`idx_cert_reimburse_employee`、`idx_cert_reimburse_store`、`idx_cert_reimburse_status`

#### 3.5.3 状态机

```
pending ──[门店初审通过]──→ store_approved ──[HR复审通过]──→ hr_approved ──[财务报销]──→ reimbursed ──[关闭]──→ closed
   │              │                    │
   │              └──[门店驳回]──→ rejected
   │
   └──[员工撤回]──→ withdrawn

任意阶段（pending/store_approved/hr_approved）──[HR 驳回]──→ rejected
```

#### 3.5.4 API 契约

**Controller**：`CertificateReimbursementController`（`/v1/certificate-reimbursements`）

| # | 端点 | HTTP | 说明 |
|---|------|------|------|
| 1 | `/v1/certificate-reimbursements` | POST | 员工提交报销申请 |
| 2 | `/v1/certificate-reimbursements/{id}` | GET | 查询详情 |
| 3 | `/v1/certificate-reimbursements` | GET | 分页查询（按角色过滤：员工看自己/门店看本店/HR 看全部） |
| 4 | `/v1/certificate-reimbursements/{id}/store-approve` | PUT | 门店初审通过 |
| 5 | `/v1/certificate-reimbursements/{id}/store-reject` | PUT | 门店初审驳回 |
| 6 | `/v1/certificate-reimbursements/{id}/hr-approve` | PUT | HR 复审通过 |
| 7 | `/v1/certificate-reimbursements/{id}/hr-reject` | PUT | HR 复审驳回 |
| 8 | `/v1/certificate-reimbursements/{id}/reimburse` | PUT | 财务报销（触发凭证生成） |
| 9 | `/v1/certificate-reimbursements/{id}/withdraw` | PUT | 员工撤回 |

#### 3.5.5 前端调用点清单【强制】

| API 端点 | HTTP | 页面文件 | 组件/方法 | 触发时机 |
|---------|------|---------|----------|---------|
| /v1/certificate-reimbursements | POST | views/store-management/StoreCertificate.vue | handleApplyReimburse() | 员工点击"申请报销" |
| /v1/certificate-reimbursements/{id} | GET | views/store-management/StoreCertificate.vue | handleViewDetail() | 查看详情 |
| /v1/certificate-reimbursements | GET | views/store-management/StoreCertificate.vue | fetchList() | 门店端列表加载 |
| /v1/certificate-reimbursements | GET | views/hr/HRCertificateReimbursement.vue（新建） | fetchList() | HR 端列表加载 |
| /v1/certificate-reimbursements/{id}/store-approve | PUT | views/store-management/StoreCertificate.vue | handleStoreApprove() | 门店店长点击"初审通过" |
| /v1/certificate-reimbursements/{id}/store-reject | PUT | views/store-management/StoreCertificate.vue | handleStoreReject() | 门店店长点击"初审驳回" |
| /v1/certificate-reimbursements/{id}/hr-approve | PUT | views/hr/HRCertificateReimbursement.vue | handleHrApprove() | HR 点击"复审通过" |
| /v1/certificate-reimbursements/{id}/hr-reject | PUT | views/hr/HRCertificateReimbursement.vue | handleHrReject() | HR 点击"复审驳回" |
| /v1/certificate-reimbursements/{id}/reimburse | PUT | views/finance/FinanceReimbursement.vue（新建） | handleReimburse() | 财务点击"报销" |
| /v1/certificate-reimbursements/{id}/withdraw | PUT | views/store-management/StoreCertificate.vue | handleWithdraw() | 员工点击"撤回" |

#### 3.5.6 跨模块联动检查清单【强制】

**上游**：
| 事件源 | 事件名 | 触发条件 |
|--------|--------|---------|
| 文件附件模块 | — | 员工上传收据/证件附件（调用 /v1/files/upload） |

**下游**：
| 事件名 | 触发条件 | 接收方 |
|--------|---------|--------|
| `ReimbursementSubmittedEvent` | 员工提交申请 | 通知模块（通知门店店长初审） |
| `ReimbursementStoreApprovedEvent` | 门店初审通过 | 通知模块（通知 HR 复审） |
| `ReimbursementHrApprovedEvent` | HR 复审通过 | 通知模块（通知财务报销） |
| `ReimbursementReimbursedEvent` | 财务报销完成 | 财务模块（生成凭证+成本记录）+ 通知模块（通知员工到账） |

**关键联动（财务）**：
- `reimburse` 端点执行时：
  1. 调用 `financeVoucherService.createVoucher()` 生成会计凭证（借：管理费用-福利费，贷：银行存款）
  2. 调用 `financeCostRecordService.addCostRecord()` 更新成本记录（按 storeId 归集到门店成本）
  3. 更新 `certificate_reimbursements.finance_voucher_id`
  4. 触发 `ReimbursementReimbursedEvent`

#### 3.5.7 通知触发点【强制】

| 时机 | 模板编码 | 接收人 | 渠道 |
|------|---------|--------|------|
| 员工提交申请 | `reimbursement.submitted` | 门店店长 | SITE_MSG |
| 门店初审通过 | `reimbursement.store_approved` | HR | SITE_MSG |
| HR 复审通过 | `reimbursement.hr_approved` | 财务 | SITE_MSG |
| 报销到账 | `reimbursement.reimbursed` | 申请员工 | SITE_MSG + EMAIL |
| 任一阶段驳回 | `reimbursement.rejected` | 申请员工 | SITE_MSG |

#### 3.5.8 验收测试用例【强制】

**TC-001：完整流程 — 申请→初审→复审→报销→通知**
- 操作：员工申请 → 门店初审 → HR 复审 → 财务报销
- 预期：
  - status 流转：pending → store_approved → hr_approved → reimbursed
  - finance_voucher 表新增凭证记录
  - finance_cost_record 表新增成本记录
  - 员工收到"报销到账"通知

**TC-002：联动验证 — 财务凭证生成**
- 操作：财务调用 reimburse
- 预期：
  - finance_voucher 新增凭证，摘要包含"证件报销"
  - certificate_reimbursements.finance_voucher_id 关联到凭证 ID

**TC-003：异常 — 非门店初审状态不可 HR 复审**
- 前置：status='pending'（未门店初审）
- 操作：HR 调用 hr-approve
- 预期：返回 422，code=3401

#### 3.5.9 前端 Mock 移除检查清单【强制】

| Mock 位置 | 文件 | 行号 | Mock 内容 | 替换为 | 状态 |
|----------|------|------|----------|--------|------|
| StoreCertificate.vue 报销功能 | views/store-management/StoreCertificate.vue | 待确认 | 无报销功能（需新增） | 新增报销申请功能区块 | ☐ N/A（新增功能） |

---

### 3.6 离职管理流程

#### 3.6.1 功能概述

| 项 | 内容 |
|----|------|
| **业务目标** | 实现离职申请→审批→工作交接→资产归还→离职证明→档案归档→账号停用完整闭环 |
| **依赖项** | 员工表（已有）、资产领用表（已有）、文件附件系统（已有）、UserAccountService（3.4） |
| **优先级** | P0 |
| **关联文档** | DATA_LIFECYCLE.md 环节 8、DEVELOPMENT_STATUS.md 9.2 #6 |

#### 3.6.2 数据模型

**新增表：`resignations`（离职申请表）**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| resignation_id | BIGINT | PK, IDENTITY | 主键 |
| employee_id | BIGINT | NOT NULL | 员工 ID |
| employee_name | VARCHAR(100) | NOT NULL | 员工姓名 |
| store_id | BIGINT | | 门店 ID |
| store_name | VARCHAR(100) | | 门店名称 |
| position_name | VARCHAR(100) | | 岗位名称 |
| resign_type | VARCHAR(32) | NOT NULL, CHECK IN ('voluntary','involuntary','retire','contract_expire') | 离职类型 |
| resign_reason | TEXT | NOT NULL | 离职原因 |
| planned_leave_date | DATE | NOT NULL | 计划离职日期 |
| actual_leave_date | DATE | | 实际离职日期 |
| status | VARCHAR(32) | NOT NULL, DEFAULT 'pending' | 状态 |
| approver_id | BIGINT | | 审批人 ID |
| approver_name | VARCHAR(100) | | 审批人姓名 |
| approve_time | TIMESTAMP | | 审批时间 |
| approve_remark | VARCHAR(500) | | 审批备注 |
| handover_to_id | BIGINT | | 交接接收人 ID |
| handover_to_name | VARCHAR(100) | | 交接接收人姓名 |
| handover_content | TEXT | | 交接内容 |
| handover_time | TIMESTAMP | | 交接完成时间 |
| asset_return_status | VARCHAR(32) | | 资产归还状态：pending/returned/lost |
| asset_return_time | TIMESTAMP | | 资产归还时间 |
| certificate_attachment_id | BIGINT | | 离职证明附件 ID |
| archive_status | VARCHAR(32) | DEFAULT 'not_archived' | 归档状态 |
| archive_time | TIMESTAMP | | 归档时间 |
| create_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| update_time | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | |
| deleted | INTEGER | NOT NULL, DEFAULT 0 | |

#### 3.6.3 状态机

```
pending ──[审批通过]──→ approved ──[工作交接]──→ handover_completed ──[资产归还]──→ assets_returned ──[开证明]──→ certified ──[归档]──→ archived
   │
   └──[审批驳回]──→ rejected
```

#### 3.6.4 API 契约

**Controller**：`ResignationController`（`/v1/resignations`）

| # | 端点 | HTTP | 说明 |
|---|------|------|------|
| 1 | `/v1/resignations` | POST | 员工/HR 提交离职申请 |
| 2 | `/v1/resignations/{id}` | GET | 查询详情 |
| 3 | `/v1/resignations` | GET | 分页查询 |
| 4 | `/v1/resignations/{id}/approve` | PUT | 审批通过 |
| 5 | `/v1/resignations/{id}/reject` | PUT | 审批驳回 |
| 6 | `/v1/resignations/{id}/handover` | PUT | 提交工作交接 |
| 7 | `/v1/resignations/{id}/return-assets` | PUT | 资产归还确认 |
| 8 | `/v1/resignations/{id}/certificate` | POST | 生成离职证明（PDF 附件） |
| 9 | `/v1/resignations/{id}/archive` | PUT | 档案归档（最终步骤） |

#### 3.6.5 前端调用点清单【强制】

| API 端点 | HTTP | 页面文件 | 组件/方法 | 触发时机 |
|---------|------|---------|----------|---------|
| /v1/resignations | POST | views/hr/HREmployee.vue | handleApplyResign() | HR/员工点击"申请离职" |
| /v1/resignations/{id} | GET | views/hr/HREmployee.vue | handleViewResign() | 查看离职详情 |
| /v1/resignations | GET | views/hr/HREmployee.vue | fetchResignList() | 离职列表加载 |
| /v1/resignations/{id}/approve | PUT | views/hr/HREmployee.vue | handleApproveResign() | HR/店长审批通过 |
| /v1/resignations/{id}/reject | PUT | views/hr/HREmployee.vue | handleRejectResign() | 审批驳回 |
| /v1/resignations/{id}/handover | PUT | views/hr/HREmployee.vue | handleHandover() | 提交工作交接 |
| /v1/resignations/{id}/return-assets | PUT | views/hr/HREmployee.vue | handleReturnAssets() | 确认资产归还 |
| /v1/resignations/{id}/certificate | POST | views/hr/HREmployee.vue | handleGenerateCertificate() | 生成离职证明 |
| /v1/resignations/{id}/archive | PUT | views/hr/HREmployee.vue | handleArchive() | 档案归档 |

#### 3.6.6 跨模块联动检查清单【强制】

**上游**：
| 事件源 | 事件名 | 触发条件 |
|--------|--------|---------|
| 员工/HR | — | 手动提交离职申请 |

**下游**：
| 事件名 | 触发条件 | 接收方 |
|--------|---------|--------|
| `ResignationApprovedEvent` | 审批通过 | 通知模块（通知员工+交接人）+ 资产模块（待归还资产清单） |
| `ResignationArchivedEvent` | 归档完成 | 员工模块（status→inactive）+ UserAccount 模块（停用账号） |

**关键联动**：
- `archive` 端点执行时（最终步骤）：
  1. 调用 `employeeService.updateStatus(employeeId, 'inactive')` 更新员工状态
  2. 调用 `userAccountService.disableAccount(userId)` 停用登录账号
  3. 调用 `employeeArchiveService.archive(employeeId)` 归档员工档案
  4. 触发 `ResignationArchivedEvent`

#### 3.6.7 通知触发点【强制】

| 时机 | 模板编码 | 接收人 | 渠道 |
|------|---------|--------|------|
| 离职申请提交 | `resignation.submitted` | HR + 门店店长 | SITE_MSG + EMAIL |
| 审批通过 | `resignation.approved` | 申请员工 + 交接接收人 | SITE_MSG |
| 审批驳回 | `resignation.rejected` | 申请员工 | SITE_MSG |
| 工作交接完成 | `resignation.handover_completed` | HR | SITE_MSG |
| 资产归还完成 | `resignation.assets_returned` | HR + 资产管理员 | SITE_MSG |
| 离职证明已开 | `resignation.certified` | 申请员工 | SITE_MSG + EMAIL |
| 档案归档完成 | `resignation.archived` | HR | SITE_MSG |

#### 3.6.8 验收测试用例【强制】

**TC-001：完整流程 — 申请→审批→交接→归还→证明→归档**
- 操作：完整走完 6 步
- 预期：
  - 最终 status='archived'
  - Employee.status='inactive'
  - User.account_status='disabled'
  - 员工档案归档

**TC-002：联动验证 — 归档触发账号停用**
- 操作：调用 archive
- 预期：User 表对应记录 account_status='disabled'

**TC-003：异常 — 未交接不可开证明**
- 前置：status='approved'（未交接）
- 操作：调用 certificate
- 预期：返回 422，code=3501

#### 3.6.9 前端 Mock 移除检查清单【强制】

| Mock 位置 | 文件 | 行号 | Mock 内容 | 替换为 | 状态 |
|----------|------|------|----------|--------|------|
| HREmployee.vue 离职枚举 | views/hr/HREmployee.vue | 待确认 | 'resigning'/'resigned'/'retired' 枚举无 API | 真实调用 /v1/resignations 系列 API | ☐ 待移除 |

---

### 3.7 跨模块联动总览（第一批 10 项）

第一批 6 个功能产生的跨模块联动汇总：

```
┌─────────────────────────────────────────────────────────────────────┐
│                第一批跨模块联动总览（10 项断链修复）                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─[3.1 招聘名额]                                                   │
│  │   ├─ 上游：无                                                    │
│  │   ├─ 下游：通知模块（8 个通知模板）                              │
│  │   ├─ 联动：门店招聘需求提报时校验名额（断链 #36 修复）           │
│  │   └─ 联动：入职成功时 used_count+=1（断链 #38 联动）             │
│  │                                                                  │
│  ├─[3.2 招聘协同反馈]                                               │
│  │   ├─ 上游：门店招聘需求提报                                      │
│  │   ├─ 下游：通知模块（6 个通知模板）                              │
│  │   └─ 联动：HR 反馈→门店通知（断链 #37 修复）                     │
│  │                                                                  │
│  ├─[3.3 Offer 持久化]                                               │
│  │   ├─ 上游：面试通过                                              │
│  │   ├─ 下游：入职模块（convert-to-onboarding）                     │
│  │   └─ 联动：Offer 接受→生成入职记录（修复录用→入职断链）          │
│  │                                                                  │
│  ├─[3.4 入职注册联动]                                               │
│  │   ├─ 上游：入职完成                                              │
│  │   ├─ 下游：通知模块（激活通知）                                  │
│  │   ├─ 联动：入职→账号创建（断链 #38 修复）                        │
│  │   └─ Bug 修复：Employee.storeId 设置（断链 #9 修复）             │
│  │                                                                  │
│  ├─[3.5 证件报销]                                                   │
│  │   ├─ 上游：文件附件上传                                          │
│  │   ├─ 下游：财务模块（凭证+成本）+ 通知模块（5 个通知）           │
│  │   ├─ 联动：报销→财务凭证（断链 #39 修复）                        │
│  │   └─ 联动：报销→成本归集（断链 #40 修复）                        │
│  │                                                                  │
│  └─[3.6 离职管理]                                                   │
│      ├─ 上游：员工/HR 手动申请                                      │
│      ├─ 下游：员工模块（状态变更）+ 账号模块（停用）+ 通知模块      │
│      └─ 联动：归档→账号停用+员工状态变更（修复离职闭环）            │
│                                                                     │
│  断链修复总览：                                                     │
│  - #36 HR→门店招聘名额下发 ✅                                       │
│  - #37 门店招聘→HR 反馈 ✅                                          │
│  - #38 入职→注册账号创建 ✅                                         │
│  - #39 证件报销→财务凭证 ✅                                         │
│  - #40 证件报销→成本归集 ✅                                         │
│  - #9 Employee.storeId 未设置 ✅                                    │
│  - 离职闭环缺失 ✅                                                  │
│  - Offer 持久化缺失 ✅                                              │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.8 第一批通知模板注册汇总

**迁移脚本需注册的通知模板（共 35 个）**：

| 模块 | 模板编码 | 数量 |
|------|---------|------|
| 招聘名额 | recruitment.quota.* | 8 |
| 招聘协同反馈 | recruitment.requirement.* / recruitment.feedback.* / recruitment.approved.* / recruitment.rejected.* / interview.store_evaluation.* | 6 |
| Offer | offer.* | 4 |
| 入职注册 | user.account.* | 2 |
| 证件报销 | reimbursement.* | 5 |
| 离职 | resignation.* | 7 |
| **合计** | — | **32** |

### 3.9 第一批新建数据库迁移脚本

| 脚本 | 内容 |
|------|------|
| `V20260626__create_recruitment_quotas_and_feedback.sql` | recruitment_quotas + recruitment_feedback + interviews 增字段 + 14 个通知模板 |
| `V20260627__create_job_offers.sql` | job_offers + 4 个通知模板 |
| `V20260628__create_position_default_roles_and_user_fields.sql` | position_default_roles + users 增字段 + 2 个通知模板 |
| `V20260629__create_certificate_reimbursements.sql` | certificate_reimbursements + 5 个通知模板 |
| `V20260630__create_resignations.sql` | resignations + 7 个通知模板 |

### 3.10 第一批验收检查清单（汇总）

实施完成后，必须逐一验证以下检查项：

**3.1 招聘名额**：
- [ ] TC-001 HR 下发名额成功 + 门店店长收到通知
- [ ] TC-002 名额超限校验返回正确错误码
- [ ] TC-003 门店申请追加流程完整
- [ ] TC-004 招聘需求提报时名额校验联动
- [ ] TC-005 入职成功后 used_count 增加

**3.2 招聘协同反馈**：
- [ ] TC-001 HR 审核通过流程
- [ ] TC-002 HR 反馈需修改流程
- [ ] TC-003 异常状态校验
- [ ] TC-004 门店协同面评

**3.3 Offer 持久化**：
- [ ] TC-001 完整流程（创建→发送→接受→转入职）
- [ ] TC-002 未发送不可接受
- [ ] TC-003 转入职后数据完整

**3.4 入职注册联动**：
- [ ] TC-001 完整流程（入职→账号→改密）
- [ ] TC-002 Employee.storeId 正确设置
- [ ] TC-003 岗位未配置角色异常

**3.5 证件报销**：
- [ ] TC-001 完整流程（申请→初审→复审→报销→通知）
- [ ] TC-002 财务凭证生成
- [ ] TC-003 异常状态校验

**3.6 离职管理**：
- [ ] TC-001 完整流程（6 步闭环）
- [ ] TC-002 归档触发账号停用
- [ ] TC-003 异常状态校验

**Mock 移除**：
- [ ] HRRecruitment.vue 招聘需求/面试/录用 Mock 全部移除
- [ ] api/hr/recruitment.ts、hire.ts Mock 全部移除
- [ ] HREmployee.vue 入职完成/离职 Mock 移除

**构建验证**：
- [ ] `mvn compile` 通过
- [ ] `npm run build` 通过
- [ ] 无 TypeScript 类型错误

---

## 4. 第二批：站内通信事件触发（5 项）

> **批次目标**：将已有的通知基础设施（NotificationController 25 端点 + 4 实体 + RabbitMQ 消费者）与第一批业务事件源打通，实现 20+ 类业务事件自动触发通知。
> **批次依赖**：依赖第一批的业务事件（QuotaIssuedEvent/OnboardingCompletedEvent/ReimbursementReimbursedEvent 等）已定义。
> **关键问题**：站内信双系统割裂（SiteNotificationService 直接落库 vs RabbitMQ SITE_MSG 抛异常），本批次必须整合。

### 4.1 统一站内信渠道（SiteNotificationService 与 RabbitMQ 整合）

#### 4.1.1 功能概述

| 项 | 内容 |
|----|------|
| **业务目标** | 消除站内信双系统割裂，统一为"SiteNotificationService 落库 + WebSocket 推送"单一机制，RabbitMQ SITE_MSG 渠道委托给 SiteNotificationService |
| **依赖项** | NotificationMessageConsumer（已有）、SiteNotificationService（已有） |
| **优先级** | P0 |
| **关联文档** | DATA_LIFECYCLE.md 19.2、DEVELOPMENT_STATUS.md 9.5 #1 |

#### 4.1.2 整合方案

**当前问题**：
```
┌─ 机制 A：SiteNotificationService.createNotification()  ─────────┐
│  - 直接落 notification 表                                        │
│  - WebSocket 推送                                                │
│  - 不走 RabbitMQ                                                 │
└──────────────────────────────────────────────────────────────────┘

┌─ 机制 B：NotificationMessageConsumer.handleSiteMessageChannel() ─┐
│  - 从 RabbitMQ 消费 SITE_MSG 类型消息                             │
│  - 抛 UnsupportedOperationException（未实现）                      │
│  - 死代码                                                         │
└──────────────────────────────────────────────────────────────────┘
```

**整合后**：
```
┌─ 统一机制 ──────────────────────────────────────────────────────┐
│  业务事件 → ApplicationEvent → NotificationEventBus             │
│       ↓                                                          │
│  NotificationEventBus 路由：                                     │
│   ├─ 站内信 → SiteNotificationService.createNotification()     │
│   │            → 落 notification 表 + WebSocket 推送            │
│   └─ 多渠道（EMAIL/SMS/WEBHOOK）→ RabbitMQ msg.send.exchange   │
│                → NotificationMessageConsumer 分发               │
└──────────────────────────────────────────────────────────────────┘
```

**修改点**：
1. `NotificationMessageConsumer.handleSiteMessageChannel()` 改为委托调用 `SiteNotificationService.createNotification()`
2. `SiteNotificationService` 保持不变（已是正确实现）
3. 移除 `notification` 表与 `msg_send_record` 表的双写逻辑（站内信只落 `notification` 表）

#### 4.1.3 数据模型

无新增表。修改 `msg_send_record` 表：当 channel='SITE_MSG' 时，`notification_id` 字段关联到 `notification` 表（已有字段，需确认）。

#### 4.1.4 API 契约

无新增 API。修改 `NotificationMessageConsumer` 内部实现。

#### 4.1.5 前端调用点清单【强制】

无前端变更。NotificationCenter.vue 已对接 SiteNotificationService 的 API，无需修改。

#### 4.1.6 跨模块联动检查清单【强制】

**上游**：RabbitMQ msg.send.exchange（channel='SITE_MSG'）
**下游**：notification 表 + WebSocket 推送

#### 4.1.7 通知触发点【强制】

本功能是通知基础设施整合，不新增通知触发点，但确保所有 SITE_MSG 类型消息能正确落库。

#### 4.1.8 验收测试用例【强制】

**TC-001：RabbitMQ SITE_MSG 消息能落库**
- 操作：向 msg.send.exchange 发送 channel='SITE_MSG' 消息
- 预期：notification 表新增记录，WebSocket 推送成功，msg_send_record.status='success'

**TC-002：站内信不双写**
- 操作：通过 NotificationEventBus 触发站内信
- 预期：notification 表新增 1 条（非 2 条），msg_send_record 表新增 1 条（status=success，关联 notification_id）

#### 4.1.9 前端 Mock 移除检查清单【强制】

无 Mock 移除。

---

### 4.2 创建 NotificationEventBus

#### 4.2.1 功能概述

| 项 | 内容 |
|----|------|
| **业务目标** | 基于 Spring ApplicationEvent 实现跨模块事件分发，业务模块发布事件→EventBus 路由到通知服务 |
| **依赖项** | Spring ApplicationEventPublisher、SiteNotificationService、NotificationService |
| **优先级** | P0 |
| **关联文档** | DATA_LIFECYCLE.md 19.4、DEVELOPMENT_STATUS.md 9.5 #2 |

#### 4.2.2 核心设计

**事件基类**：
```java
public abstract class BusinessEvent extends ApplicationEvent {
    private final String eventType;        // 事件类型（如 recruitment.quota.issued）
    private final Long operatorId;         // 操作人 ID
    private final Map<String, Object> variables;  // 模板变量
    private final List<Long> recipientUserIds;    // 接收人 ID 列表
    private final List<String> channels;   // 渠道（SITE_MSG/EMAIL/SMS）

    // 构造函数、getter
}
```

**EventBus 实现**：
```java
@Component
@RequiredArgsConstructor
public class NotificationEventBus {

    private final SiteNotificationService siteNotificationService;
    private final NotificationService notificationService;
    private final MsgTemplateService templateService;

    /**
     * 监听业务事件（事务提交后处理）
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBusinessEvent(BusinessEvent event) {
        // 1. 查询通知模板
        MsgTemplate template = templateService.getByCode(event.getEventType());
        if (template == null || !template.getEnabled()) {
            return;  // 模板不存在或未启用，跳过
        }

        // 2. 渲染模板
        String title = renderTemplate(template.getTitleTemplate(), event.getVariables());
        String content = renderTemplate(template.getContentTemplate(), event.getVariables());

        // 3. 按渠道分发
        for (String channel : event.getChannels()) {
            switch (channel) {
                case "SITE_MSG":
                    siteNotificationService.createNotification(
                        event.getRecipientUserIds(), title, content,
                        event.getEventType(), event.getSource().toString());
                    break;
                case "EMAIL":
                case "SMS":
                case "WEBHOOK":
                    // 走 RabbitMQ 多渠道分发
                    notificationService.sendMultiChannel(
                        event.getRecipientUserIds(), channel, template, event.getVariables());
                    break;
            }
        }
    }

    private String renderTemplate(String template, Map<String, Object> variables) {
        // 简单的 {variable} 占位符替换
        String result = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}",
                                    String.valueOf(entry.getValue()));
        }
        return result;
    }
}
```

**业务模块发布事件示例**：
```java
// RecruitmentQuotaServiceImpl 中
@Autowired private ApplicationEventPublisher eventPublisher;

public RecruitmentQuotaVO issueQuota(RecruitmentQuotaCreateDTO dto) {
    // ... 创建名额 ...

    // 发布事件（事务提交后触发通知）
    eventPublisher.publishEvent(new QuotaIssuedEvent(
        this,
        "recruitment.quota.issued",
        currentUserId,
        Map.of(
            "year", quota.getYear(),
            "quarter", quota.getQuarter(),
            "storeName", quota.getStoreName(),
            "positionName", quota.getPositionName(),
            "headcount", quota.getHeadcount(),
            "expireDate", quota.getExpireDate()
        ),
        recipientUserIds,  // 门店店长用户 ID 列表
        List.of("SITE_MSG", "EMAIL")
    ));

    return convertToVO(quota);
}
```

#### 4.2.3 前端调用点清单【强制】

无前端调用（纯后端基础设施）。

#### 4.2.4 跨模块联动检查清单【强制】

**上游**：所有业务模块（通过 ApplicationEventPublisher 发布 BusinessEvent）
**下游**：SiteNotificationService（站内信）+ NotificationService（多渠道）

#### 4.2.5 通知触发点【强制】

本功能是通知分发基础设施，不直接定义通知触发点，但承载所有业务事件的通知分发。

#### 4.2.6 验收测试用例【强制】

**TC-001：业务事件触发通知**
- 操作：业务模块发布 QuotaIssuedEvent
- 预期：
  - notification 表新增记录（SITE_MSG 渠道）
  - msg_send_record 表新增记录（EMAIL 渠道）
  - 接收人 NotificationCenter 未读 +1

**TC-002：模板未启用时跳过**
- 前置：msg_template.enabled=FALSE
- 操作：发布事件
- 预期：notification 表无新增

**TC-003：事务回滚不触发通知**
- 操作：业务方法抛异常导致事务回滚
- 预期：notification 表无新增（@TransactionalEventListener AFTER_COMMIT 保证）

#### 4.2.7 前端 Mock 移除检查清单【强制】

无 Mock 移除。

---

### 4.3 补充 20 类业务事件通知触发

#### 4.3.1 功能概述

| 项 | 内容 |
|----|------|
| **业务目标** | 在第一批 6 个功能中集成 NotificationEventBus，发布对应业务事件 |
| **依赖项** | NotificationEventBus（4.2）、第一批业务功能 |
| **优先级** | P0 |
| **关联文档** | DATA_LIFECYCLE.md 19.4 |

#### 4.3.2 业务事件清单（20 类）

| # | 事件类型 | 发布位置 | 接收人 | 渠道 |
|---|---------|---------|--------|------|
| 1 | recruitment.quota.issued | RecruitmentQuotaServiceImpl.issueQuota() | 门店店长 | SITE_MSG+EMAIL |
| 2 | recruitment.quota.confirmed | confirmQuota() | HR | SITE_MSG |
| 3 | recruitment.quota.rejected | rejectQuota() | HR | SITE_MSG |
| 4 | recruitment.quota.exhausted | incrementUsedCount() | 门店+HR | SITE_MSG |
| 5 | recruitment.requirement.submitted | RecruitmentRequirementServiceImpl.create() | HR | SITE_MSG+EMAIL |
| 6 | recruitment.feedback.given | RecruitmentFeedbackServiceImpl.submit() | 门店 | SITE_MSG |
| 7 | recruitment.approved | submit(approve) | 门店 | SITE_MSG |
| 8 | offer.sent | JobOfferServiceImpl.send() | 候选人 | EMAIL |
| 9 | offer.accepted | accept() | HR | SITE_MSG |
| 10 | onboarding.completed | OnboardingRecordServiceImpl.completeOnboarding() | 门店+新员工 | SITE_MSG |
| 11 | user.account.created | UserAccountServiceImpl.createUserAccount() | 新员工 | SITE_MSG+EMAIL |
| 12 | reimbursement.submitted | CertificateReimbursementServiceImpl.create() | 门店店长 | SITE_MSG |
| 13 | reimbursement.store_approved | storeApprove() | HR | SITE_MSG |
| 14 | reimbursement.hr_approved | hrApprove() | 财务 | SITE_MSG |
| 15 | reimbursement.reimbursed | reimburse() | 申请员工 | SITE_MSG+EMAIL |
| 16 | resignation.submitted | ResignationServiceImpl.create() | HR+店长 | SITE_MSG+EMAIL |
| 17 | resignation.approved | approve() | 员工+交接人 | SITE_MSG |
| 18 | resignation.archived | archive() | HR | SITE_MSG |
| 19 | interview.store_evaluation.submitted | submitStoreEvaluation() | HR | SITE_MSG |
| 20 | resignation.assets_returned | returnAssets() | HR+资产管理员 | SITE_MSG |

#### 4.3.3 前端调用点清单【强制】

无前端调用（事件在后端 Service 层发布）。

#### 4.3.4 跨模块联动检查清单【强制】

每个业务事件的发布位置即第一批功能的 ServiceImpl，事件接收方为通知模块。

#### 4.3.5 通知触发点【强制】

见 4.3.2 清单（20 类事件对应 20 个通知模板，已在第一批迁移脚本中注册）。

#### 4.3.6 验收测试用例【强制】

**TC-001：每个业务事件触发对应通知**
- 对 20 类事件逐一测试，验证 notification/msg_send_record 表新增记录

**TC-002：通知内容正确渲染**
- 操作：发布 QuotaIssuedEvent，variables={year:2026, quarter:3, storeName:"上海店", ...}
- 预期：notification.content 包含"2026年第3季度""上海店"等变量值

#### 4.3.7 前端 Mock 移除检查清单【强制】

无 Mock 移除。

---

### 4.4 邮件服务器配置

#### 4.4.1 功能概述

| 项 | 内容 |
|----|------|
| **业务目标** | 配置 SMTP 邮件服务器，使 EMAIL 渠道真实可用 |
| **依赖项** | spring-boot-starter-mail、application.yml |
| **优先级** | P0 |

#### 4.4.2 配置方案

**application.yml（开发环境）**：
```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 587
    username: ${MAIL_USERNAME:system@food-traceability.com}
    password: ${MAIL_PASSWORD:}
    protocol: smtp
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
```

**生产环境**：通过环境变量注入，使用企业邮箱（如阿里云企业邮箱）。

#### 4.4.3 前端调用点清单【强制】

无前端调用。系统管理可新增"邮件服务器配置"页面（可选，延后）。

#### 4.4.4 跨模块联动检查清单【强制】

**上游**：NotificationMessageConsumer.handleEmailChannel()
**下游**：SMTP 服务器

#### 4.4.5 通知触发点【强制】

本功能是邮件基础设施，不新增通知触发点。

#### 4.4.6 验收测试用例【强制】

**TC-001：邮件发送成功**
- 操作：通过 NotificationService.send() 发送 EMAIL 类型消息
- 预期：msg_send_record.status='success'，收件人邮箱收到邮件

#### 4.4.7 前端 Mock 移除检查清单【强制】

无 Mock 移除。

---

### 4.5 AlertNotificationServiceImpl Mock 替换

#### 4.5.1 功能概述

| 项 | 内容 |
|----|------|
| **业务目标** | 替换 AlertNotificationServiceImpl 中硬编码的邮箱/手机号，改为从 notification_setting 表读取配置 |
| **依赖项** | AlertNotificationServiceImpl（已有）、NotificationSettingService |
| **优先级** | P0 |
| **关联文档** | DATA_LIFECYCLE.md 19.1.2 |

#### 4.5.2 修改方案

**当前问题**：
```java
// AlertNotificationServiceImpl 中硬编码
private static final String ALERT_EMAIL = "admin@food-traceability.com";
private static final String ALERT_PHONE = "13800138000";
```

**修改后**：
```java
@Autowired
private NotificationSettingService settingService;

public void sendAlert(String alertType, String message) {
    // 从配置读取接收人
    NotificationSetting setting = settingService.getSetting("DEVICE_ALERT");
    List<String> recipients = setting.getEmailRecipients();  // 配置的邮箱列表

    // 走 NotificationEventBus 统一分发
    eventPublisher.publishEvent(new DeviceAlertEvent(
        this, "device.alert", currentUserId,
        Map.of("alertType", alertType, "message", message),
        recipientUserIds,
        List.of("SITE_MSG", "EMAIL")
    ));
}
```

#### 4.5.3-4.5.7 简略（参照通用规范）

- 前端调用点：系统管理-通知设置页面配置设备告警接收人
- 跨模块联动：设备模块 → NotificationEventBus → 通知模块
- 通知触发点：device.alert（新增模板）
- 验收：设备告警发生时，配置的接收人收到通知
- Mock 移除：移除 AlertNotificationServiceImpl 中硬编码常量

---

## 5. 第三批：基础设施对接 + 跨模块联动（7 项）

> **批次目标**：将已有的打印任务/数据导出/文件附件基础设施与业务模块对接，修复"建好没人用"问题。
> **批次依赖**：依赖第一批的附件场景（入职/报销/离职附件）。

### 5.1 打印任务对接订单模块

#### 5.1.1 功能概述

| 项 | 内容 |
|----|------|
| **业务目标** | 订单创建时触发厨房打印工单，订单支付完成时触发收银小票打印 |
| **依赖项** | PrintTaskController（已有 11 端点）、OrderNewController（已有） |
| **优先级** | P0 |
| **关联文档** | DATA_LIFECYCLE.md 20.2、DEVELOPMENT_STATUS.md 9.6 #1 |

#### 5.1.2 对接方案

**触发点 1：厨房工单打印**
- 位置：`OrderNewServiceImpl.createOrder()` 末尾
- 逻辑：订单创建成功后，调用 `printTaskService.createPrintTask()` 创建打印任务
- 打印内容：订单号/桌台号/菜品列表/数量/备注
- 打印机：后厨打印机（按 storeId 查询设备）

**触发点 2：收银小票打印**
- 位置：`OrderNewServiceImpl.payOrder()` 末尾
- 逻辑：支付成功后，创建收银小票打印任务
- 打印内容：订单号/支付方式/金额/找零/时间
- 打印机：收银台打印机

#### 5.1.3 API 契约

无新增 API。在现有 OrderNewServiceImpl 中注入 PrintTaskService 并调用。

#### 5.1.4 前端调用点清单【强制】

| API 端点 | HTTP | 页面文件 | 组件/方法 | 触发时机 |
|---------|------|---------|----------|---------|
| /api/v1/print-tasks | GET | views/device/PrintTaskList.vue（新建） | fetchPrintTasks() | 查看打印任务列表 |
| /api/v1/print-tasks/{taskId}/retry | POST | views/device/PrintTaskList.vue | handleRetry() | 重试失败任务 |

#### 5.1.5 跨模块联动检查清单【强制】

**上游**：
| 事件源 | 事件名 | 触发条件 |
|--------|--------|---------|
| 订单模块 | `OrderCreatedEvent` | 订单创建 |
| 订单模块 | `OrderPaidEvent` | 订单支付完成 |

**下游**：
| 事件名 | 触发条件 | 接收方 |
|--------|---------|--------|
| `PrintTaskCreatedEvent` | 打印任务创建 | 设备模块（执行打印） |

#### 5.1.6 通知触发点【强制】

| 时机 | 模板编码 | 接收人 | 渠道 |
|------|---------|--------|------|
| 打印失败（重试 3 次后） | `print.task.failed` | 门店店长 | SITE_MSG |

#### 5.1.7 验收测试用例【强制】

**TC-001：订单创建触发厨房打印**
- 操作：创建订单
- 预期：print_tasks 表新增 1 条记录，task_type='kitchen_order'，status='pending'

**TC-002：订单支付触发收银打印**
- 操作：支付订单
- 预期：print_tasks 表新增 1 条记录，task_type='receipt'

**TC-003：打印失败重试**
- 前置：打印机离线
- 操作：订单创建后打印失败
- 预期：自动重试 3 次，3 次失败后 status='failed'，通知门店店长

#### 5.1.8 前端 Mock 移除检查清单【强制】

无 Mock 移除（打印是后端自动触发）。

---

### 5.2 导出中心扩展为异步任务

#### 5.2.1 功能概述

| 项 | 内容 |
|----|------|
| **业务目标** | 将 DataExportController 从同步 1 端点扩展为异步任务模式（提交→后台处理→通知下载） |
| **依赖项** | DataExportController（已有）、ExportTask 实体（已有）、export_tasks 表（已有） |
| **优先级** | P0 |

#### 5.2.2 异步任务模式

```
用户提交导出请求 → 创建 export_task（status='pending'）→ 返回 taskId
                         ↓（异步）
                    后台worker 拉取 pending 任务
                         ↓
                    执行导出（生成 CSV/Excel 文件）
                         ↓
                    更新 status='completed'，记录 file_path
                         ↓
                    通知用户下载
```

#### 5.2.3 API 契约

| # | 端点 | HTTP | 说明 |
|---|------|------|------|
| 1 | `/v1/admin/export/tasks` | POST | 提交导出任务（参数：module/type/filter） |
| 2 | `/v1/admin/export/tasks` | GET | 查询任务列表（按用户/状态过滤） |
| 3 | `/v1/admin/export/tasks/{taskId}` | GET | 查询任务详情 |
| 4 | `/v1/admin/export/tasks/{taskId}/download` | GET | 下载导出文件 |
| 5 | `/v1/admin/export/tasks/{taskId}/cancel` | PUT | 取消任务 |
| 6 | `/v1/admin/export/tasks/{taskId}/retry` | POST | 重试失败任务 |

#### 5.2.4 前端调用点清单【强制】

| API 端点 | HTTP | 页面文件 | 组件/方法 | 触发时机 |
|---------|------|---------|----------|---------|
| /v1/admin/export/tasks | POST | 各模块列表页工具栏 | handleExport() | 用户点击"导出"按钮 |
| /v1/admin/export/tasks | GET | views/system/ExportTaskList.vue（新建） | fetchTasks() | 导出任务管理页加载 |
| /v1/admin/export/tasks/{taskId}/download | GET | views/system/ExportTaskList.vue | handleDownload() | 任务完成后点击"下载" |
| /v1/admin/export/tasks/{taskId}/cancel | PUT | views/system/ExportTaskList.vue | handleCancel() | 取消进行中任务 |

#### 5.2.5 跨模块联动检查清单【强制】

**上游**：各业务模块（HR/采购/订单/财务等）提交导出请求
**下游**：通知模块（导出完成通知用户下载）

#### 5.2.6 通知触发点【强制】

| 时机 | 模板编码 | 接收人 | 渠道 |
|------|---------|--------|------|
| 导出完成 | `export.completed` | 提交用户 | SITE_MSG |
| 导出失败 | `export.failed` | 提交用户 | SITE_MSG |

#### 5.2.7 验收测试用例【强制】

**TC-001：异步导出流程**
- 操作：提交导出任务 → 轮询状态 → 下载
- 预期：task status 流转 pending → processing → completed，文件可下载

#### 5.2.8 前端 Mock 移除检查清单【强制】

无 Mock 移除（新增功能）。

---

### 5.3 补充 12 个导出场景

#### 5.3.1 功能概述

在 5.2 异步导出框架基础上，补充 12 个导出场景的 worker 实现。

#### 5.3.2 导出场景清单

| # | 模块 | 导出类型 | 数据源 | 格式 |
|---|------|---------|--------|------|
| 1 | HR | 员工档案 | employees + 关联表 | Excel |
| 2 | HR | 考勤记录 | attendances | Excel |
| 3 | HR | 薪资单 | salaries | PDF |
| 4 | 采购 | 采购订单 | purchase_orders + items | Excel |
| 5 | 采购 | 采购收货单 | purchase_stockins | Excel |
| 6 | 订单 | 销售订单 | orders + items | Excel |
| 7 | 财务 | 财务凭证 | finance_vouchers | Excel |
| 8 | 财务 | 财务报表 | 聚合数据 | Excel |
| 9 | 仓储 | 库存台账 | warehouse_inventory | Excel |
| 10 | 溯源 | 溯源记录 | trace_records | Excel |
| 11 | 会员 | 会员列表 | members | Excel |
| 12 | 系统 | 审计日志 | audit_log | Excel |

#### 5.3.3-5.3.7 简略

每个导出场景实现一个 ExportWorker（实现统一接口），由导出任务调度器根据 module/type 路由到对应 worker。

---

### 5.4 入职/报销/离职附件对接

#### 5.4.1 功能概述

将 FileAttachmentService（已有 10 端点）与第一批的入职/报销/离职功能对接，支持附件上传与查询。

#### 5.4.2 对接场景

| 场景 | businessType | 上传时机 | 查询时机 |
|------|-------------|---------|---------|
| 入职档案附件 | onboarding | 入职办理时 | 查看员工档案 |
| 证件报销收据 | certificate_reimbursement | 提交报销申请时 | 审核报销 |
| 证件报销证件 | certificate_reimbursement | 提交报销申请时 | 审核报销 |
| 离职证明 | resignation | 生成离职证明时 | 查看离职记录 |
| 离职交接单 | resignation | 提交工作交接时 | 查看离职记录 |

#### 5.4.3-5.4.7 简略

参照 FileAttachmentController 已有 API，在第一批功能的前端页面集成附件上传组件。

---

### 5.5 业务事件→事件总线

#### 5.5.1 功能概述

本项是 4.2 NotificationEventBus 的延伸——确保所有业务模块（不仅第一批）都能通过 EventBus 发布事件。包括订单/采购/财务/会员等模块的事件接入。

#### 5.5.2 需接入事件总线的模块事件

| 模块 | 事件 | 说明 |
|------|------|------|
| 订单 | OrderCreatedEvent | 订单创建 |
| 订单 | OrderPaidEvent | 订单支付 |
| 订单 | OrderRefundedEvent | 订单退款 |
| 采购 | PurchaseOrderApprovedEvent | 采购订单审批 |
| 采购 | PurchaseStockinCompletedEvent | 采购收货完成 |
| 财务 | VoucherApprovedEvent | 凭证审核 |
| 财务 | VoucherRejectedEvent | 凭证退回 |
| 门店 | DailySettlementCompletedEvent | 日结完成 |
| 门店 | MaterialRequestSubmittedEvent | 物资请领 |
| 会员 | MemberRechargedEvent | 会员充值 |
| 会员 | MemberConsumedEvent | 会员消费 |
| 会员 | CouponExpiringEvent | 优惠券到期 |
| 资产 | AssetRepairedEvent | 资产维修完成 |
| 签章 | SealApplicationApprovedEvent | 用印申请审批 |

#### 5.5.3-5.5.7 简略

每个事件对应一个通知模板，在对应 ServiceImpl 中发布事件。

---

### 5.6 采购收货→打印

#### 5.6.1 功能概述

采购收货完成时触发收货单打印（参照 5.1 模式）。

**触发点**：`PurchaseStockinServiceImpl.completeStockin()` 末尾
**打印内容**：收货单号/供应商/商品列表/数量/批次/溯源码
**打印机**：仓储打印机

#### 5.6.2-5.6.7 简略

参照 5.1 模式实现。

---

### 5.7 各模块→异步导出

#### 5.7.1 功能概述

在 5.2 异步导出框架基础上，各业务模块列表页增加"导出"按钮，调用统一的 `/v1/admin/export/tasks` 端点。

#### 5.7.2 需增加导出按钮的页面

| 页面 | 导出类型 |
|------|---------|
| views/hr/HREmployee.vue | 员工档案 |
| views/hr/HRAttendance.vue | 考勤记录 |
| views/hr/HRSalary.vue | 薪资单 |
| views/purchase/PurchaseOrder.vue | 采购订单 |
| views/order/OrderList.vue | 销售订单 |
| views/finance/VoucherList.vue | 财务凭证 |
| views/warehouse/InventoryLedger.vue | 库存台账 |
| views/traceability/TraceList.vue | 溯源记录 |
| views/member/MemberList.vue | 会员列表 |
| views/system/AuditLog.vue | 审计日志 |

#### 5.7.3-5.7.7 简略

每个页面工具栏增加"导出"按钮，调用导出 API。

---

## Chapter 6 第四批：财务专业 + 移动/连锁/合规（5 项详细设计）

> **批次定位**：在业务流程、跨模块联动、基础设施三层完善后，补齐财务专业能力（三大报表/期末结账/多门店合并）、移动端能力（离线审批）、在线支付能力（微信/支付宝）。
> **目标**：从"能跑通业务"升级到"能对外商用"。

### 6.1 财务报表（资产负债表 / 利润表 / 现金流量表）

#### 6.1.1 表设计

新建 `finance_report_records` 表（报表生成记录主表）：

```sql
CREATE TABLE finance_report_records (
  report_id           VARCHAR(32) PRIMARY KEY,
  report_type         VARCHAR(32) NOT NULL,  -- balance_sheet / income_statement / cash_flow
  period_type         VARCHAR(16) NOT NULL,  -- month / quarter / year
  period_start        DATE NOT NULL,
  period_end          DATE NOT NULL,
  store_id            VARCHAR(32),           -- NULL 表示全公司合并
  store_name          VARCHAR(128),
  status              VARCHAR(32) NOT NULL DEFAULT 'draft', -- draft/generating/completed/failed
  generated_by        BIGINT NOT NULL,
  generated_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_time      TIMESTAMP,
  total_assets        BIGINT,                -- 资产总计（分）
  total_liabilities   BIGINT,                -- 负债总计（分）
  total_equity        BIGINT,                -- 所有者权益（分）
  total_revenue       BIGINT,                -- 营业收入（分）
  total_expense       BIGINT,                -- 营业成本费用（分）
  net_profit          BIGINT,                -- 净利润（分）
  operating_cf        BIGINT,                -- 经营活动现金流净额（分）
  investing_cf        BIGINT,                -- 投资活动现金流净额（分）
  financing_cf        BIGINT,                -- 筹资活动现金流净额（分）
  snapshot_data       JSONB,                 -- 报表明细快照（行项目）
  error_message       VARCHAR(512),
  create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_finance_report_records_type_period ON finance_report_records(report_type, period_start, period_end);
CREATE INDEX idx_finance_report_records_store ON finance_report_records(store_id);
```

新建 `finance_report_templates` 表（行项目模板，财政部标准科目）：

```sql
CREATE TABLE finance_report_templates (
  template_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  report_type         VARCHAR(32) NOT NULL,
  line_no             INTEGER NOT NULL,      -- 行次
  line_code           VARCHAR(32) NOT NULL,  -- 项目编码
  line_name           VARCHAR(128) NOT NULL, -- 项目名称
  line_category       VARCHAR(32),           -- 资产/负债/权益/收入/成本/费用/现金流
  calculation_formula VARCHAR(512),          -- 计算公式（科目余额聚合表达式）
  display_order       INTEGER NOT NULL,
  is_active           BOOLEAN NOT NULL DEFAULT TRUE,
  create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted             INTEGER NOT NULL DEFAULT 0,
  UNIQUE(report_type, line_code)
);
```

#### 6.1.2 报表生成公式（核心规则）

| 报表类型 | 关键行项目 | 取数公式（基于 accounting_subject 余额） |
|---------|-----------|----------------------------------------|
| 资产负债表 | 货币资金 | `SUM(余额 WHERE 科目 LIKE '1001%' OR '1002%' OR '1012%')` |
| 资产负债表 | 应收账款 | `SUM(余额 WHERE 科目 LIKE '1122%')` |
| 资产负债表 | 存货 | `SUM(余额 WHERE 科目 IN ('1403原材料','1405库存商品','1411周转材料'))` |
| 资产负债表 | 固定资产 | `SUM(余额 1601) - SUM(余额 1602 累计折旧)` |
| 资产负债表 | 应付账款 | `SUM(余额 WHERE 科目 LIKE '2202%')` |
| 资产负债表 | 实收资本 | `SUM(余额 WHERE 科目 LIKE '4001%')` |
| 利润表 | 营业收入 | `SUM(余额 WHERE 科目 LIKE '6001%')` |
| 利润表 | 营业成本 | `SUM(余额 WHERE 科目 LIKE '6401%')` |
| 利润表 | 期间费用 | `SUM(余额 6601销售费用 + 6602管理费用 + 6603研发费用 + 6604财务费用)` |
| 利润表 | 利润总额 | `营业利润 + 营业外收入(6301) - 营业外支出(6711)` |
| 现金流量表 | 经营活动现金流入 | `SUM(现金类科目借方发生额 WHERE 关联业务类型='经营')` |
| 现金流量表 | 投资活动现金流出 | `SUM(现金类科目贷方发生额 WHERE 关联业务类型='投资')` |

> **关键依赖**：必须在 Chapter 6.2「期末结账」完成后，`accounting_subject` 表的 `period_balance` 字段才正确。报表生成依赖期末结账。

#### 6.1.3 API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/v1/finance/reports/generate` | 触发生成报表（异步） |
| GET | `/v1/finance/reports` | 分页查询报表列表（支持类型/期间/门店筛选） |
| GET | `/v1/finance/reports/{reportId}` | 查询报表详情（含行项目明细） |
| GET | `/v1/finance/reports/{reportId}/export` | 导出 Excel/PDF |
| DELETE | `/v1/finance/reports/{reportId}` | 删除报表（草稿可删，已确认不可删） |
| POST | `/v1/finance/reports/{reportId}/confirm` | 确认报表（不可再改） |
| GET | `/v1/finance/reports/templates/{reportType}` | 查询行项目模板 |
| PUT | `/v1/finance/reports/templates/{reportType}` | 更新模板（财务专家维护） |
| GET | `/v1/finance/reports/comparison` | 多期对比分析 |

#### 6.1.4 状态机

```
draft → generating → completed → confirmed
                  → failed → draft（可重新生成）
```

#### 6.1.5 跨模块联动检查清单

- [ ] 报表生成前校验期末结账是否完成（period_end 在结账区间内）
- [ ] 报表生成依赖 accounting_subject.period_balance，需保证凭证已全部过账
- [ ] 多门店合并报表依赖 store_id=NULL + 子门店报表已确认
- [ ] 报表确认后触发"报表归档"事件 → 财务档案模块归档
- [ ] 报表生成失败时通知财务负责人

#### 6.1.6 通知触发点

```sql
-- 模板注册
INSERT INTO msg_template(template_code, template_name, channel, title_template, content_template) VALUES
('FINANCE_REPORT_GENERATED', '财务报表生成完成', 'SITE_MSG', '报表生成完成：{reportType}', '{periodType} {periodStart} 至 {periodEnd} 的{reportName}已生成完成，请前往财务报表模块查看。'),
('FINANCE_REPORT_FAILED', '财务报表生成失败', 'SITE_MSG', '报表生成失败：{reportType}', '{periodStart} 至 {periodEnd} 的{reportName}生成失败，原因：{errorMessage}。请检查期末结账状态后重试。');
```

触发位置：`FinanceReportServiceImpl.generateReport()` 完成后发布 `FinanceReportGeneratedEvent`，由 `NotificationEventBus` 路由。

#### 6.1.7 前端调用点清单

| 前端文件 | 调用端点 | Mock 状态 |
|---------|---------|----------|
| `views/finance/FinanceReport.vue`（新建） | POST /generate | 需新建 |
| 同上 | GET /reports | 需新建 |
| 同上 | GET /reports/{reportId} | 需新建 |
| 同上 | GET /export | 需新建 |
| `views/finance/FinanceReportBalanceSheet.vue`（新建） | GET /reports/{reportId} | 需新建 |
| `views/finance/FinanceReportIncomeStatement.vue`（新建） | GET /reports/{reportId} | 需新建 |
| `views/finance/FinanceReportCashFlow.vue`（新建） | GET /reports/{reportId} | 需新建 |
| `views/finance/FinanceReportComparison.vue`（新建） | GET /comparison | 需新建 |

#### 6.1.8 验收测试用例

| # | 用例 | 预期 |
|---|------|------|
| 1 | 期末结账完成后生成资产负债表 | status=completed，total_assets=科目余额聚合值 |
| 2 | 未结账直接生成报表 | status=failed，error_message="期间未结账" |
| 3 | 门店级报表（storeId 非空） | 仅聚合该门店凭证，不含其他门店 |
| 4 | 全公司合并报表（storeId=NULL） | 聚合所有已确认门店报表 |
| 5 | 已确认报表再次生成 | 抛 BizException("已确认报表不可重新生成") |
| 6 | 报表导出 PDF | 文件含表头/行项目/期间/制表人/制表日期 |

#### 6.1.9 前端 Mock 移除检查清单

- [ ] `api/finance/report.ts` 中是否含 `withMockFallback`？
- [ ] `views/finance/FinanceReport*.vue` 中是否硬编码示例数据？
- [ ] 报表详情页是否真实渲染 `snapshot_data` 字段而非假数据？

#### 6.1.10 错误码段位

- `33001` 报表类型不存在
- `33002` 期间重叠或非法
- `33003` 期末未结账
- `33004` 模板配置缺失
- `33005` 已确认报表不可重新生成
- `33006` 合并报表子门店未全部确认
- `33007` 报表生成超时

---

### 6.2 财务期末结账（损益结转 / 成本结转 / 年末封账）

#### 6.2.1 表设计

新建 `finance_period_closing` 表（结账记录）：

```sql
CREATE TABLE finance_period_closing (
  closing_id          VARCHAR(32) PRIMARY KEY,
  period_type         VARCHAR(16) NOT NULL,  -- month / quarter / year
  period_start        DATE NOT NULL,
  period_end          DATE NOT NULL,
  store_id            VARCHAR(32),
  closing_type        VARCHAR(32) NOT NULL,  -- income_summary / cost_carry / year_seal
  status              VARCHAR(32) NOT NULL DEFAULT 'pending', -- pending/processing/completed/reversed
  profit_loss_transferred  BOOLEAN NOT NULL DEFAULT FALSE,    -- 损益已结转
  cost_carried              BOOLEAN NOT NULL DEFAULT FALSE,    -- 成本已结转
  year_sealed               BOOLEAN NOT NULL DEFAULT FALSE,    -- 年末已封账
  voucher_ids         JSONB,                 -- 结转生成的凭证 ID 列表
  operated_by         BIGINT NOT NULL,
  operated_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reversed_by         BIGINT,
  reversed_time       TIMESTAMP,
  reverse_reason      VARCHAR(512),
  create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted             INTEGER NOT NULL DEFAULT 0,
  UNIQUE(period_type, period_start, period_end, store_id, closing_type)
);
```

新建 `accounting_subject_period_balance` 表（科目期间余额，结账后写入）：

```sql
CREATE TABLE accounting_subject_period_balance (
  balance_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  subject_code        VARCHAR(32) NOT NULL,
  period_start        DATE NOT NULL,
  period_end          DATE NOT NULL,
  store_id            VARCHAR(32),
  opening_balance     BIGINT NOT NULL DEFAULT 0,   -- 期初余额（分）
  debit_amount        BIGINT NOT NULL DEFAULT 0,   -- 本期借方发生额
  credit_amount       BIGINT NOT NULL DEFAULT 0,   -- 本期贷方发生额
  closing_balance     BIGINT NOT NULL DEFAULT 0,   -- 期末余额
  is_carry_forward    BOOLEAN NOT NULL DEFAULT FALSE,
  create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted             INTEGER NOT NULL DEFAULT 0,
  UNIQUE(subject_code, period_start, period_end, store_id)
);
CREATE INDEX idx_subject_period_balance_subject ON accounting_subject_period_balance(subject_code);
CREATE INDEX idx_subject_period_balance_period ON accounting_subject_period_balance(period_start, period_end);
```

#### 6.2.2 三类结账流程

**6.2.2.1 损益结转（月末）**：
```
1. 查询本期所有收入类科目余额（6001-60xx 贷方余额）
2. 查询本期所有成本费用类科目余额（6401-67xx 借方余额）
3. 计算本期利润 = 收入合计 - 成本费用合计
4. 生成结转凭证：
   借：各收入科目（冲平贷方余额）
       贷：本年利润 4103
   借：本年利润 4103
       贷：各成本费用科目（冲平借方余额）
5. 凭证自动过账 → 写入 accounting_subject_period_balance
6. 标记 profit_loss_transferred=TRUE
```

**6.2.2.2 成本结转（月末）**：
```
1. 查询本期销售出库的库存商品成本（warehouse_stock_out where type='sales')
2. 计算本期销售成本
3. 生成结转凭证：
   借：主营业务成本 6401
       贷：库存商品 1405
4. 凭证自动过账
5. 标记 cost_carried=TRUE
```

**6.2.2.3 年末封账（年末）**：
```
1. 校验 1-12 月所有月结账已完成
2. 计算本年累计净利润
3. 生成利润分配凭证：
   借：本年利润 4103
       贷：利润分配-未分配利润 4104
4. 凭证自动过账
5. 锁定本年度所有凭证（status='locked'，禁止修改/作废）
6. 标记 year_sealed=TRUE
```

#### 6.2.3 API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/v1/finance/closing/income-summary` | 执行损益结转 |
| POST | `/v1/finance/closing/cost-carry` | 执行成本结转 |
| POST | `/v1/finance/closing/year-seal` | 执行年末封账 |
| GET | `/v1/finance/closing` | 查询结账记录列表 |
| GET | `/v1/finance/closing/{closingId}` | 查询结账详情 |
| POST | `/v1/finance/closing/{closingId}/reverse` | 反结账（仅未封账期间） |
| GET | `/v1/finance/closing/status` | 查询某期间结账状态 |
| GET | `/v1/finance/closing/period-balance` | 查询科目期间余额 |

#### 6.2.4 状态机

```
pending → processing → completed
                     → reversed（反结账，回到 pending）
年末封账：completed → sealed（终态，不可反结账）
```

#### 6.2.5 跨模块联动检查清单

- [ ] 损益结转前校验本期所有凭证已过账（status='posted'）
- [ ] 成本结转依赖仓储模块销售出库数据
- [ ] 年末封账前校验 1-12 月月结全部 completed
- [ ] 年末封账后触发"凭证锁定"事件 → 凭证模块禁止修改/作废
- [ ] 结账完成后触发事件 → 财务报表可生成（解禁 Chapter 6.1 报表生成）
- [ ] 反结账时同步删除自动生成的结转凭证

#### 6.2.6 通知触发点

```sql
INSERT INTO msg_template(template_code, template_name, channel, title_template, content_template) VALUES
('FINANCE_CLOSING_COMPLETED', '财务结账完成', 'SITE_MSG', '{periodType}结账完成', '{periodStart} 至 {period_end} 的{closingTypeName}已执行完成，可生成财务报表。'),
('FINANCE_CLOSING_FAILED', '财务结账失败', 'SITE_MSG', '{periodType}结账失败', '{closingTypeName}执行失败，原因：{errorMessage}。'),
('FINANCE_YEAR_SEALED', '年末封账完成', 'SITE_MSG', '{year}年度已封账', '本年度所有凭证已锁定，不可修改。如需调整，请使用跨期调整凭证。');
```

#### 6.2.7 前端调用点清单

| 前端文件 | 调用端点 | Mock 状态 |
|---------|---------|----------|
| `views/finance/FinanceClosing.vue`（新建） | POST /income-summary | 需新建 |
| 同上 | POST /cost-carry | 需新建 |
| 同上 | POST /year-seal | 需新建 |
| 同上 | GET /closing | 需新建 |
| 同上 | GET /status | 需新建 |
| 同上 | POST /reverse | 需新建 |
| `views/finance/FinancePeriodBalance.vue`（新建） | GET /period-balance | 需新建 |

#### 6.2.8 验收测试用例

| # | 用例 | 预期 |
|---|------|------|
| 1 | 本期凭证全部过账后执行损益结转 | 生成结转凭证，profit_loss_transferred=TRUE |
| 2 | 存在未过账凭证时执行结转 | 抛 BizException("存在未过账凭证，无法结账") |
| 3 | 反结账 | status=reversed，结转凭证被删除 |
| 4 | 年末封账 | status=sealed，本年度凭证全部 status='locked' |
| 5 | 已封账期间反结账 | 抛 BizException("已封账期间不可反结账") |
| 6 | 重复执行损益结转 | 抛 BizException("本期损益已结转") |

#### 6.2.9 前端 Mock 移除检查清单

- [ ] `api/finance/closing.ts` 中是否含 `withMockFallback`？
- [ ] 结账详情页是否真实渲染 voucher_ids 而非假凭证号？
- [ ] 反结账按钮是否真实调用后端而非前端状态切换？

#### 6.2.10 错误码段位

- `34001` 存在未过账凭证
- `34002` 本期已结转
- `34003` 已封账期间不可反结账
- `34004` 年末封账前需完成所有月结
- `34005` 成本结转依赖仓储数据缺失
- `34006` 结转凭证生成失败

---

### 6.3 财务共享服务（多门店合并报表 + 集中核算 + 集中收付）

#### 6.3.1 表设计

新建 `finance_shared_center` 表（共享中心配置）：

```sql
CREATE TABLE finance_shared_center (
  center_id           VARCHAR(32) PRIMARY KEY,
  center_name         VARCHAR(128) NOT NULL,   -- 共享中心名称
  center_code         VARCHAR(32) NOT NULL UNIQUE,
  parent_center_id    VARCHAR(32),             -- 上级中心（支持多级）
  center_type         VARCHAR(32) NOT NULL,    -- group / region / store
  store_ids           JSONB,                   -- 归属门店 ID 列表
  consolidation_rule  VARCHAR(32),             -- 合并规则：full / proportional
  is_active           BOOLEAN NOT NULL DEFAULT TRUE,
  create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted             INTEGER NOT NULL DEFAULT 0
);
```

新建 `finance_consolidation_tasks` 表（合并任务）：

```sql
CREATE TABLE finance_consolidation_tasks (
  task_id             VARCHAR(32) PRIMARY KEY,
  center_id           VARCHAR(32) NOT NULL,
  period_type         VARCHAR(16) NOT NULL,
  period_start        DATE NOT NULL,
  period_end          DATE NOT NULL,
  status              VARCHAR(32) NOT NULL DEFAULT 'pending',
  child_report_ids    JSONB,                   -- 子门店报表 ID
  consolidated_report_id VARCHAR(32),          -- 合并后报表 ID
  elimination_entries JSONB,                   -- 内部交易抵销分录
  operated_by         BIGINT NOT NULL,
  operated_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted             INTEGER NOT NULL DEFAULT 0
);
```

新建 `finance_internal_transactions` 表（内部交易记录，用于抵销）：

```sql
CREATE TABLE finance_internal_transactions (
  transaction_id      VARCHAR(32) PRIMARY KEY,
  from_store_id       VARCHAR(32) NOT NULL,
  to_store_id         VARCHAR(32) NOT NULL,
  transaction_type    VARCHAR(32) NOT NULL,    -- inter_sale / inter_transfer / inter_loan
  amount              BIGINT NOT NULL,         -- 金额（分）
  business_id         VARCHAR(32),             -- 关联业务单据 ID
  business_type       VARCHAR(32),
  period_date         DATE NOT NULL,
  is_eliminated       BOOLEAN NOT NULL DEFAULT FALSE,
  elimination_voucher_id VARCHAR(32),          -- 抵销凭证 ID
  create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted             INTEGER NOT NULL DEFAULT 0,
  CHECK (from_store_id <> to_store_id)
);
CREATE INDEX idx_internal_transactions_period ON finance_internal_transactions(period_date, from_store_id, to_store_id);
```

#### 6.3.2 合并报表流程

```
1. 财务共享中心发起合并任务（指定中心/期间）
2. 系统查询所有子门店已确认的同期间同类型报表
3. 校验所有子门店报表状态='confirmed'
4. 聚合子门店报表数据（资产/负债/权益/收入/成本/现金流）
5. 识别内部交易（finance_internal_transactions）
6. 生成抵销分录：
   借：内部应收
       贷：内部应付
   借：内部销售收入
       贷：内部销售成本
7. 应用抵销分录生成合并报表
8. 合并报表自动确认（status='confirmed'）
9. 任务完成通知财务负责人
```

#### 6.3.3 API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/v1/finance/shared/centers` | 查询共享中心列表 |
| POST | `/v1/finance/shared/centers` | 创建共享中心 |
| PUT | `/v1/finance/shared/centers/{centerId}` | 更新共享中心 |
| DELETE | `/v1/finance/shared/centers/{centerId}` | 删除共享中心 |
| POST | `/v1/finance/shared/consolidation` | 发起合并任务（异步） |
| GET | `/v1/finance/shared/consolidation` | 查询合并任务列表 |
| GET | `/v1/finance/shared/consolidation/{taskId}` | 查询合并任务详情 |
| GET | `/v1/finance/shared/internal-transactions` | 查询内部交易记录 |
| POST | `/v1/finance/shared/internal-transactions/{transactionId}/eliminate` | 手动标记抵销 |
| GET | `/v1/finance/shared/center-payment` | 查询集中收付记录 |
| POST | `/v1/finance/shared/center-payment` | 发起集中收付 |

#### 6.3.4 状态机

合并任务：`pending → processing → completed → failed`

#### 6.3.5 跨模块联动检查清单

- [ ] 共享中心配置依赖 store 表（store_ids 必须为有效门店）
- [ ] 合并任务依赖 Chapter 6.1 报表已确认（子门店报表 status='confirmed'）
- [ ] 合并任务依赖 Chapter 6.2 期末结账已完成
- [ ] 内部交易记录依赖采购/调拨模块（自动识别门店间业务）
- [ ] 集中收付依赖 finance_payment / finance_receipt 模块
- [ ] 抵销凭证自动生成 → 凭证模块自动过账

#### 6.3.6 通知触发点

```sql
INSERT INTO msg_template(template_code, template_name, channel, title_template, content_template) VALUES
('FINANCE_CONSOLIDATION_COMPLETED', '财务合并完成', 'SITE_MSG', '合并报表生成完成：{centerName}', '{periodType} {periodStart} 至 {periodEnd} 的合并报表已生成，包含 {storeCount} 家门店。'),
('FINANCE_CONSOLIDATION_FAILED', '财务合并失败', 'SITE_MSG', '合并报表生成失败：{centerName}', '失败原因：{errorMessage}。常见原因：子门店报表未确认、期末未结账、内部交易未识别。');
```

#### 6.3.7 前端调用点清单

| 前端文件 | 调用端点 | Mock 状态 |
|---------|---------|----------|
| `views/finance/FinanceSharedCenter.vue`（新建） | GET/POST/PUT/DELETE /centers | 需新建 |
| `views/finance/FinanceConsolidation.vue`（新建） | POST /consolidation | 需新建 |
| 同上 | GET /consolidation | 需新建 |
| `views/finance/FinanceInternalTransactions.vue`（新建） | GET /internal-transactions | 需新建 |
| `views/finance/FinanceCenterPayment.vue`（新建） | GET/POST /center-payment | 需新建 |

#### 6.3.8 验收测试用例

| # | 用例 | 预期 |
|---|------|------|
| 1 | 3 家门店报表全部确认后合并 | 合并报表生成，total_assets=Σ门店 total_assets - 内部交易抵销 |
| 2 | 某门店报表未确认时合并 | 抛 BizException("门店 {name} 报表未确认") |
| 3 | 门店间调拨 1 万元，合并时识别为内部交易 | 抵销分录生成，elimination_entries 含 1 万元抵销 |
| 4 | 重复发起同期间合并 | 抛 BizException("本期间已存在合并任务") |
| 5 | 集中收付 | 资金从中心账户流出 → 各门店账户入账 + 凭证自动生成 |

#### 6.3.9 前端 Mock 移除检查清单

- [ ] `api/finance/shared.ts` 中是否含 `withMockFallback`？
- [ ] 合并详情页是否真实展示 child_report_ids 而非假门店名？
- [ ] 内部交易记录页是否真实查询而非硬编码示例？

#### 6.3.10 错误码段位

- `35001` 子门店报表未确认
- `35002` 本期间已存在合并任务
- `35003` 内部交易识别失败
- `35004` 抵销凭证生成失败
- `35005` 共享中心配置无效
- `35006` 集中收付账户余额不足

---

### 6.4 移动端审批（店长/HR/财务离线审批）

#### 6.4.1 设计原则

> 移动端**不**新建独立前端项目，而是基于现有 Vue 3 + Element Plus 的响应式布局适配移动端，并增加 PWA 离线能力。审批入口嵌入到各业务模块，通过统一的 ApprovalCenter 集中展示待办。

#### 6.4.2 表设计

新建 `approval_tasks` 表（统一审批任务表）：

```sql
CREATE TABLE approval_tasks (
  task_id             VARCHAR(32) PRIMARY KEY,
  business_type       VARCHAR(32) NOT NULL,   -- recruitment / leave / expense / voucher / purchase / closing
  business_id         VARCHAR(32) NOT NULL,
  business_title      VARCHAR(256) NOT NULL,
  applicant_id        BIGINT NOT NULL,
  applicant_name      VARCHAR(64) NOT NULL,
  current_node        VARCHAR(32) NOT NULL,   -- 当前审批节点
  current_approver_id BIGINT NOT NULL,
  current_approver_name VARCHAR(64) NOT NULL,
  status              VARCHAR(32) NOT NULL DEFAULT 'pending', -- pending/approved/rejected/withdrawn/transferred
  priority            VARCHAR(16) NOT NULL DEFAULT 'normal', -- urgent/high/normal/low
  submitted_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  due_time            TIMESTAMP,              -- 截止时间（超时自动升级）
  processed_time      TIMESTAMP,
  processed_by        BIGINT,
  processed_comment   VARCHAR(512),
  transferred_to      BIGINT,                 -- 转交目标用户 ID
  snapshot_data       JSONB,                  -- 业务单据快照（用于离线查看）
  offline_status      VARCHAR(32) DEFAULT 'synced', -- synced/pending_sync/conflict
  create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted             INTEGER NOT NULL DEFAULT 0,
  UNIQUE(business_type, business_id, current_node)
);
CREATE INDEX idx_approval_tasks_approver ON approval_tasks(current_approver_id, status);
CREATE INDEX idx_approval_tasks_business ON approval_tasks(business_type, business_id);
```

新建 `approval_logs` 表（审批历史）：

```sql
CREATE TABLE approval_logs (
  log_id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  task_id             VARCHAR(32) NOT NULL,
  node_name           VARCHAR(32) NOT NULL,
  approver_id         BIGINT NOT NULL,
  approver_name       VARCHAR(64) NOT NULL,
  action              VARCHAR(32) NOT NULL,   -- submit/approve/reject/transfer/withdraw
  comment             VARCHAR(512),
  operated_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_approval_logs_task ON approval_logs(task_id, operated_time);
```

#### 6.4.3 统一审批流引擎

**6.4.3.1 流程定义**（基于业务类型路由）：

| business_type | 审批节点（顺序） | 适用模块 |
|---------------|----------------|---------|
| recruitment_quota | 区域经理 → 总部HR → HR总监 | HR招聘名额 |
| store_recruitment | 区域经理 → 总部HR | 门店招聘需求 |
| leave | 直属主管 → HR | HR请假 |
| expense | 直属主管 → 财务 → 总经理 | 财务报销 |
| voucher | 制单人 → 审核 → 财务主管 | 财务凭证审核 |
| purchase | 采购主管 → 财务 → 总经理 | 采购订单 |
| closing | 财务主管 → 总经理 | 期末结账 |
| resignation | 直属主管 → HR → 总经理 | HR离职 |

**6.4.3.2 审批动作**：
- `approve`：通过，流转至下一节点
- `reject`：驳回，回到申请人（业务单据状态回退）
- `transfer`：转交，指定新审批人
- `withdraw`：撤回（仅申请人，且当前未审批时）

#### 6.4.4 离线审批 PWA 方案

```
移动端浏览器访问 → Service Worker 缓存审批列表页
       ↓
断网时 → IndexedDB 缓存待审批任务（snapshot_data）
       ↓
离线审批操作 → 写入 IndexedDB pending_actions 队列
       ↓
恢复网络 → Service Worker 后台同步 → 调用后端审批 API
       ↓
同步结果 → 更新 offline_status='synced'，冲突时标记 'conflict' 提示用户
```

#### 6.4.5 API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/v1/approval/tasks` | 查询待审批任务列表（支持 businessType/status 筛选） |
| GET | `/v1/approval/tasks/{taskId}` | 查询任务详情（含 snapshot_data） |
| POST | `/v1/approval/tasks/{taskId}/approve` | 通过审批 |
| POST | `/v1/approval/tasks/{taskId}/reject` | 驳回审批 |
| POST | `/v1/approval/tasks/{taskId}/transfer` | 转交审批 |
| POST | `/v1/approval/tasks/{taskId}/withdraw` | 撤回审批 |
| GET | `/v1/approval/logs/{taskId}` | 查询审批历史 |
| GET | `/v1/approval/statistics` | 查询审批统计（待办数/已办数） |
| POST | `/v1/approval/sync` | 离线审批同步（批量） |
| GET | `/v1/approval/my-submitted` | 查询我提交的审批 |

#### 6.4.6 状态机

```
pending → approved（自动流转下一节点或最终通过）
       → rejected（业务单据状态回退）
       → transferred（任务转移给新审批人）
       → withdrawn（申请人撤回）
```

#### 6.4.7 跨模块联动检查清单

- [ ] 各业务模块（招聘/请假/报销/凭证/采购/结账/离职）发起审批时调用 ApprovalService.createTask()
- [ ] 审批通过后回调业务模块的 onApproved() 方法（业务单据状态推进）
- [ ] 审批驳回后回调业务模块的 onRejected() 方法（业务单据状态回退）
- [ ] 离线审批同步时校验任务状态（已审批的任务不可重复审批）
- [ ] 超时未审批自动升级（定时任务扫描 due_time < NOW() AND status='pending'）
- [ ] 审批通过触发通知申请人

#### 6.4.8 通知触发点

```sql
INSERT INTO msg_template(template_code, template_name, channel, title_template, content_template) VALUES
('APPROVAL_NEW_TASK', '新审批任务', 'SITE_MSG', '您有新的审批任务：{businessTitle}', '{applicantName} 提交的{businessTypeName}需要您审批，请及时处理。截止时间：{dueTime}。'),
('APPROVAL_APPROVED', '审批已通过', 'SITE_MSG', '您的申请已通过：{businessTitle}', '您提交的{businessTypeName}已通过{approverName}的审批。'),
('APPROVAL_REJECTED', '审批已驳回', 'SITE_MSG', '您的申请已驳回：{businessTitle}', '您提交的{businessTypeName}已被{approverName}驳回。驳回原因：{comment}。'),
('APPROVAL_TRANSFERRED', '审批已转交', 'SITE_MSG', '审批任务已转交给您：{businessTitle}', '{originalApproverName} 将审批任务转交给您，请及时处理。'),
('APPROVAL_TIMEOUT_WARN', '审批即将超时', 'SITE_MSG', '审批即将超时：{businessTitle}', '您有审批任务将在 {hours} 小时后超时，请尽快处理。'),
('APPROVAL_TIMEOUT_ESCALATE', '审批已升级', 'SITE_MSG', '审批已升级处理：{businessTitle}', '原审批人 {originalApproverName} 超时未处理，已自动升级至 {escalatedApproverName}。');
```

#### 6.4.9 前端调用点清单

| 前端文件 | 调用端点 | Mock 状态 |
|---------|---------|----------|
| `views/approval/ApprovalCenter.vue`（新建，移动端响应式） | GET /tasks | 需新建 |
| 同上 | GET /tasks/{taskId} | 需新建 |
| 同上 | POST /approve | 需新建 |
| 同上 | POST /reject | 需新建 |
| 同上 | POST /transfer | 需新建 |
| 同上 | GET /statistics | 需新建 |
| `views/approval/ApprovalHistory.vue`（新建） | GET /logs/{taskId} | 需新建 |
| `views/approval/MySubmitted.vue`（新建） | GET /my-submitted | 需新建 |
| `composables/useApproval.ts`（新建） | 全部端点 | 需新建 |
| `service-worker.ts`（新建，PWA 离线缓存） | - | 需新建 |

**移动端响应式适配清单**：
- [ ] 审批列表页：单列卡片布局（< 768px）
- [ ] 审批详情页：垂直折叠面板（业务快照在上方，操作按钮固定底部）
- [ ] 操作按钮：触摸友好（最小高度 44px）
- [ ] 字体：移动端字号 ≥ 14px

#### 6.4.10 验收测试用例

| # | 用例 | 预期 |
|---|------|------|
| 1 | 招聘名额三级审批全流程通过 | 任务从 区域经理 → 总部HR → HR总监，全部通过后回调招聘模块 status='approved' |
| 2 | 中间节点驳回 | 任务状态='rejected'，招聘名额 status 回退到 'submitted'，通知申请人 |
| 3 | 转交审批 | current_approver_id 变更，通知新审批人 |
| 4 | 离线审批 5 个任务后联网同步 | 5 个任务状态正确同步，offline_status='synced' |
| 5 | 离线审批期间任务已被他人审批 | 同步时标记 'conflict'，提示用户该任务已处理 |
| 6 | 审批超时 24 小时自动升级 | due_time + 24h 后定时任务自动 transfer 给上级 |

#### 6.4.11 前端 Mock 移除检查清单

- [ ] `api/approval.ts` 中是否含 `withMockFallback`？
- [ ] 审批列表是否真实查询而非硬编码待办？
- [ ] 审批通过后是否真实回调业务模块状态更新？
- [ ] PWA Service Worker 是否真实注册？

#### 6.4.12 错误码段位

- `36001` 任务不存在
- `36002` 非当前审批人
- `36003` 任务已被处理
- `36004` 超时不可处理
- `36005` 离线同步冲突
- `36006` 转交目标用户无效
- `36007` 业务回调失败（业务单据状态推进失败）

---

### 6.5 在线支付对接（微信 / 支付宝）

#### 6.5.1 表设计

新建 `payment_channels` 表（支付渠道配置）：

```sql
CREATE TABLE payment_channels (
  channel_id          VARCHAR(32) PRIMARY KEY,
  channel_code        VARCHAR(32) NOT NULL UNIQUE,  -- wechat / alipay / unionpay
  channel_name        VARCHAR(64) NOT NULL,
  app_id              VARCHAR(128) NOT NULL,
  merchant_id         VARCHAR(128) NOT NULL,
  api_key             VARCHAR(512) NOT NULL,        -- 加密存储
  public_key          TEXT,
  private_key         TEXT,
  callback_url        VARCHAR(256),
  notify_url          VARCHAR(256),
  is_active           BOOLEAN NOT NULL DEFAULT TRUE,
  is_sandbox          BOOLEAN NOT NULL DEFAULT FALSE, -- 沙箱模式
  create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted             INTEGER NOT NULL DEFAULT 0
);
```

新建 `payment_orders` 表（支付订单，关联业务订单）：

```sql
CREATE TABLE payment_orders (
  payment_id          VARCHAR(32) PRIMARY KEY,
  business_type       VARCHAR(32) NOT NULL,    -- sales_order / member_recharge / purchase_payment
  business_id         VARCHAR(32) NOT NULL,
  business_title      VARCHAR(256),
  payer_id            BIGINT,                  -- 付款人用户 ID（会员付款时）
  payer_name          VARCHAR(64),
  channel_code        VARCHAR(32) NOT NULL,    -- wechat / alipay
  channel_order_no    VARCHAR(64),             -- 第三方订单号
  channel_transaction_no VARCHAR(64),          -- 第三方交易流水号
  amount              BIGINT NOT NULL,         -- 应付金额（分）
  paid_amount         BIGINT DEFAULT 0,        -- 实付金额（分）
  refund_amount       BIGINT DEFAULT 0,        -- 已退款金额（分）
  status              VARCHAR(32) NOT NULL DEFAULT 'pending', -- pending/paying/paid/refunded/closed/failed
  pay_url             VARCHAR(512),            -- 支付链接（扫码/跳转）
  qr_code             TEXT,                    -- 二维码 base64
  expired_time        TIMESTAMP,               -- 支付过期时间
  paid_time           TIMESTAMP,
  closed_time         TIMESTAMP,
  error_code          VARCHAR(32),
  error_message       VARCHAR(512),
  extra_data          JSONB,                   -- 扩展字段（如 openid）
  create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted             INTEGER NOT NULL DEFAULT 0,
  UNIQUE(business_type, business_id, channel_code)
);
CREATE INDEX idx_payment_orders_channel ON payment_orders(channel_code, channel_order_no);
CREATE INDEX idx_payment_orders_status ON payment_orders(status, expired_time);
```

新建 `payment_refunds` 表（退款记录）：

```sql
CREATE TABLE payment_refunds (
  refund_id           VARCHAR(32) PRIMARY KEY,
  payment_id          VARCHAR(32) NOT NULL,
  refund_amount       BIGINT NOT NULL,
  refund_reason       VARCHAR(256),
  channel_refund_no   VARCHAR(64),
  status              VARCHAR(32) NOT NULL DEFAULT 'pending', -- pending/processing/success/failed
  applied_by          BIGINT NOT NULL,
  applied_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_time      TIMESTAMP,
  error_message       VARCHAR(512),
  create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted             INTEGER NOT NULL DEFAULT 0
);
```

#### 6.5.2 支付流程（统一下单 + 异步回调）

**6.5.2.1 下单支付流程**：
```
1. 业务模块（订单/会员充值/采购付款）调用 PaymentService.createPayment()
2. 校验业务单据状态（订单未支付、金额>0）
3. 根据渠道生成支付订单（payment_orders.status='pending'）
4. 调用第三方支付 API（微信 Native / 支付宝电脑网站）
5. 获取支付链接/二维码 → 写入 pay_url / qr_code
6. 设置过期时间（默认 30 分钟）
7. 返回支付信息给前端
8. 前端展示二维码或跳转支付页
```

**6.5.2.2 异步回调流程**：
```
1. 第三方支付成功 → 回调 notify_url
2. PaymentNotifyController 接收回调
3. 验签（签名校验 + 防重放）
4. 更新 payment_orders.status='paid', paid_amount, paid_time
5. 发布 PaymentPaidEvent
6. PaymentPaidEventListener 处理：
   - 销售订单：更新订单状态为已支付 → 触发打印任务 → 触发会员积分累计
   - 会员充值：更新会员余额 → 触发充值通知
   - 采购付款：更新应付账款 → 触发四账联动
7. 返回 success 给第三方
```

**6.5.2.3 退款流程**：
```
1. 业务模块发起退款 PaymentService.refund()
2. 校验原支付订单状态='paid' 且 refund_amount + 新退款额 <= paid_amount
3. 创建退款记录（payment_refunds.status='pending'）
4. 调用第三方退款 API
5. 第三方异步回调 → 更新退款状态
6. 发布 PaymentRefundedEvent
7. 业务模块处理退款回调（订单状态回退、会员积分扣减等）
```

#### 6.5.3 API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/v1/payment/channels` | 查询可用支付渠道 |
| POST | `/v1/payment/create` | 创建支付订单（业务模块调用） |
| GET | `/v1/payment/{paymentId}` | 查询支付订单状态 |
| POST | `/v1/payment/{paymentId}/query` | 主动查询第三方支付状态 |
| POST | `/v1/payment/notify/wechat` | 微信支付回调（第三方调用） |
| POST | `/v1/payment/notify/alipay` | 支付宝支付回调（第三方调用） |
| POST | `/v1/payment/{paymentId}/refund` | 发起退款 |
| GET | `/v1/payment/refunds/{refundId}` | 查询退款状态 |
| POST | `/v1/payment/refunds/notify/wechat` | 微信退款回调 |
| POST | `/v1/payment/refunds/notify/alipay` | 支付宝退款回调 |
| GET | `/v1/payment/orders` | 查询支付订单列表 |
| POST | `/v1/payment/{paymentId}/close` | 关闭支付订单（手动） |

#### 6.5.4 状态机

支付订单：`pending → paying → paid → refunded（部分/全部）`
退款：`pending → processing → success / failed`

#### 6.5.5 跨模块联动检查清单

- [ ] 销售订单创建后调用 PaymentService.createPayment()
- [ ] 支付成功回调更新订单状态 → 触发 Chapter 5.1 打印任务
- [ ] 支付成功回调会员充值 → 触发余额更新
- [ ] 支付成功回调采购付款 → 触发四账联动（应付/银行/资金流水/凭证）
- [ ] 退款成功回调订单 → 订单状态回退 + 库存回补
- [ ] 退款成功回调会员 → 积分扣减
- [ ] 支付超时（30 分钟）定时任务自动关闭订单
- [ ] 第三方回调验签失败时记录告警日志

#### 6.5.6 通知触发点

```sql
INSERT INTO msg_template(template_code, template_name, channel, title_template, content_template) VALUES
('PAYMENT_SUCCESS', '支付成功', 'SITE_MSG', '支付成功：{businessTitle}', '您的{businessTypeName}已支付成功，金额：{amount} 元。'),
('PAYMENT_FAILED', '支付失败', 'SITE_MSG', '支付失败：{businessTitle}', '您的{businessTypeName}支付失败，原因：{errorMessage}。'),
('PAYMENT_REFUND_SUCCESS', '退款成功', 'SITE_MSG', '退款已到账：{businessTitle}', '您的退款 {refundAmount} 元已原路退回，请注意查收。'),
('PAYMENT_TIMEOUT', '支付超时', 'SITE_MSG', '支付已超时关闭：{businessTitle}', '您的{businessTypeName}支付订单已超时关闭，请重新发起。'),
('PAYMENT_ADMIN_NOTIFY', '收到一笔新支付', 'SITE_MSG', '收到支付：{amount} 元', '订单 {businessId} 已支付 {amount} 元，请及时处理。');
```

#### 6.5.7 前端调用点清单

| 前端文件 | 调用端点 | Mock 状态 |
|---------|---------|----------|
| `views/payment/PaymentCheckout.vue`（新建） | POST /create | 需新建 |
| 同上 | GET /{paymentId} | 需新建 |
| `views/payment/PaymentQrCode.vue`（新建，扫码支付） | GET /{paymentId} | 需新建 |
| `views/payment/PaymentResult.vue`（新建） | GET /{paymentId} | 需新建 |
| `views/payment/PaymentRefund.vue`（新建） | POST /{paymentId}/refund | 需新建 |
| `views/payment/PaymentOrders.vue`（新建，管理后台） | GET /orders | 需新建 |
| `views/system/PaymentChannels.vue`（新建，渠道配置） | GET/POST /channels | 需新建 |
| `views/order/OrderList.vue`（订单页集成支付按钮） | POST /create | 需对接 |
| `views/member/MemberRecharge.vue`（充值集成支付） | POST /create | 需对接 |
| `api/payment.ts`（新建） | 全部端点 | 需新建 |

#### 6.5.8 验收测试用例

| # | 用例 | 预期 |
|---|------|------|
| 1 | 微信 Native 扫码支付完整流程 | 二维码生成 → 扫码 → 回调 → 订单状态更新 + 打印任务触发 |
| 2 | 支付宝电脑网站支付完整流程 | 跳转支付宝 → 支付完成 → 回调 → 订单状态更新 |
| 3 | 支付超时 30 分钟未支付 | 定时任务关闭订单，status='closed'，通知用户 |
| 4 | 退款流程（部分退款） | refund_amount 累加，订单状态保持 'paid'，金额累计校验 |
| 5 | 退款流程（全额退款） | status='refunded'，订单状态回退，库存回补 |
| 6 | 第三方回调验签失败 | 返回 fail，记录告警日志，payment_orders 状态不变 |
| 7 | 重复回调 | 幂等处理，不重复更新业务订单状态 |
| 8 | 会员充值支付成功 | 会员余额 += paid_amount，触发充值通知 |

#### 6.5.9 前端 Mock 移除检查清单

- [ ] `api/payment.ts` 中是否含 `withMockFallback`？
- [ ] 订单支付按钮是否真实调用后端而非假弹窗？
- [ ] 二维码是否真实从后端返回的 qr_code 渲染？
- [ ] 支付结果页是否真实轮询 payment_orders 状态？
- [ ] 退款按钮是否真实调用后端而非前端状态切换？

#### 6.5.10 错误码段位

- `37001` 支付渠道未启用
- `37002` 业务订单状态不允许支付
- `37003` 支付金额非法
- `37004` 第三方支付下单失败
- `37005` 回调验签失败
- `37006` 重复回调
- `37007` 退款金额超过已付金额
- `37008` 退款失败
- `37009` 支付超时已关闭
- `37010` 渠道配置缺失

---

### 6.6 第四批跨模块联动总览

| # | 联动点 | 触发 | 影响 |
|---|--------|------|------|
| 1 | 期末结账 → 财务报表 | FinanceClosingCompletedEvent | 报表可生成 |
| 2 | 财务报表确认 → 共享中心合并 | FinanceReportConfirmedEvent | 合并任务可发起 |
| 3 | 共享中心合并 → 抵销凭证 | ConsolidationCompletedEvent | 凭证自动生成过账 |
| 4 | 业务模块 → 审批任务 | ApprovalCreateEvent | 统一审批中心展示 |
| 5 | 审批通过 → 业务模块状态推进 | ApprovalApprovedEvent | 业务单据状态机推进 |
| 6 | 订单支付 → 打印任务 | PaymentPaidEvent | 厨房工单 + 收银小票打印 |
| 7 | 订单支付 → 会员积分 | PaymentPaidEvent | 积分累计 |
| 8 | 会员充值支付 → 会员余额 | PaymentPaidEvent | 余额更新 |
| 9 | 采购付款支付 → 四账联动 | PaymentPaidEvent | 应付/银行/资金流水/凭证 |
| 10 | 退款 → 订单回退 + 库存回补 | PaymentRefundedEvent | 业务回滚 |

### 6.7 第四批通知模板注册汇总

| 模板编码 | 模板名称 | 渠道 |
|---------|---------|------|
| FINANCE_REPORT_GENERATED | 财务报表生成完成 | SITE_MSG |
| FINANCE_REPORT_FAILED | 财务报表生成失败 | SITE_MSG |
| FINANCE_CLOSING_COMPLETED | 财务结账完成 | SITE_MSG |
| FINANCE_CLOSING_FAILED | 财务结账失败 | SITE_MSG |
| FINANCE_YEAR_SEALED | 年末封账完成 | SITE_MSG |
| FINANCE_CONSOLIDATION_COMPLETED | 财务合并完成 | SITE_MSG |
| FINANCE_CONSOLIDATION_FAILED | 财务合并失败 | SITE_MSG |
| APPROVAL_NEW_TASK | 新审批任务 | SITE_MSG |
| APPROVAL_APPROVED | 审批已通过 | SITE_MSG |
| APPROVAL_REJECTED | 审批已驳回 | SITE_MSG |
| APPROVAL_TRANSFERRED | 审批已转交 | SITE_MSG |
| APPROVAL_TIMEOUT_WARN | 审批即将超时 | SITE_MSG |
| APPROVAL_TIMEOUT_ESCALATE | 审批已升级 | SITE_MSG |
| PAYMENT_SUCCESS | 支付成功 | SITE_MSG |
| PAYMENT_FAILED | 支付失败 | SITE_MSG |
| PAYMENT_REFUND_SUCCESS | 退款成功 | SITE_MSG |
| PAYMENT_TIMEOUT | 支付超时 | SITE_MSG |
| PAYMENT_ADMIN_NOTIFY | 收到一笔新支付 | SITE_MSG |

**合计 18 个模板**，需在 V2026062502 迁移脚本中批量注册。

### 6.8 第四批迁移脚本清单

| 脚本版本 | 内容 |
|---------|------|
| V2026062502__create_finance_report_tables.sql | finance_report_records + finance_report_templates |
| V2026062503__create_finance_closing_tables.sql | finance_period_closing + accounting_subject_period_balance |
| V2026062504__create_finance_shared_tables.sql | finance_shared_center + finance_consolidation_tasks + finance_internal_transactions |
| V2026062505__create_approval_tables.sql | approval_tasks + approval_logs |
| V2026062506__create_payment_tables.sql | payment_channels + payment_orders + payment_refunds |
| V2026062507__insert_batch4_msg_templates.sql | 18 个通知模板批量注册 |
| V2026062508__init_finance_report_templates.sql | 财政部标准报表模板初始化 |

### 6.9 第四批验收检查清单汇总

#### 财务报表（6.1）
- [ ] 三大报表生成公式正确（与科目余额一致）
- [ ] 期末未结账时禁止生成报表
- [ ] 多门店合并报表正确聚合
- [ ] 报表导出 Excel/PDF 格式合规
- [ ] 已确认报表不可重新生成

#### 财务期末结账（6.2）
- [ ] 损益结转凭证自动生成并过账
- [ ] 成本结转依赖仓储数据
- [ ] 年末封账锁定本年度凭证
- [ ] 反结账删除结转凭证
- [ ] 已封账期间不可反结账

#### 财务共享服务（6.3）
- [ ] 合并任务依赖子门店报表全部确认
- [ ] 内部交易自动识别并抵销
- [ ] 抵销凭证自动生成过账
- [ ] 集中收付资金流转正确

#### 移动端审批（6.4）
- [ ] 8 类业务审批流路由正确
- [ ] 审批通过回调业务模块状态推进
- [ ] 离线审批 PWA 同步正确
- [ ] 超时自动升级
- [ ] 移动端响应式适配（< 768px）

#### 在线支付（6.5）
- [ ] 微信 Native 扫码支付完整流程
- [ ] 支付宝电脑网站支付完整流程
- [ ] 异步回调验签 + 幂等处理
- [ ] 退款流程（部分/全额）
- [ ] 支付超时自动关闭
- [ ] 支付成功联动打印任务 + 会员积分

---

## Chapter 7 实施顺序与依赖关系

> **核心目标**：基于 27 项 P0 的依赖关系，给出科学的实施顺序，避免"前置未完成导致后置返工"。
> **总原则**：基础设施先行 → 业务流程闭环 → 跨模块联动 → 财务专业能力 → 移动/支付能力。

### 7.1 全局依赖关系图

```
                      ┌─────────────────────────────────────┐
                      │  Chapter 2 通用技术规范（最先完成）   │
                      │  - 状态机/事件总线/缓存/审计/迁移    │
                      └────────────────┬────────────────────┘
                                       │
              ┌────────────────────────┴────────────────────────┐
              │                                                  │
              ▼                                                  ▼
    ┌──────────────────────┐                       ┌──────────────────────┐
    │  Chapter 4 站内通信  │                       │  Chapter 3 业务流程  │
    │  (Batch 2, 5 项)     │                       │  (Batch 1, 10 项)    │
    │                      │                       │                      │
    │  4.1 站内信整合      │  ←─── 依赖 ────       │  3.1 招聘名额        │
    │  4.2 EventBus        │                       │  3.2 招聘协同反馈    │
    │  4.3 20类事件触发    │                       │  3.3 Offer持久化     │
    │  4.4 邮件配置        │                       │  3.4 入职→注册联动   │
    │  4.5 AlertMock替换   │                       │  3.5 证件报销        │
    └──────────┬───────────┘                       │  3.6 离职管理        │
               │                                   └──────────┬───────────┘
               │                                              │
               │  ┌───────────────────────────────────────────┘
               │  │
               ▼  ▼
    ┌─────────────────────────────────────┐
    │  Chapter 5 基础设施对接（Batch 3, 7项）│
    │  5.1 打印任务对接                    │
    │  5.2 异步导出框架                    │
    │  5.3-5.7 各模块导出/打印             │
    └────────────────┬────────────────────┘
                     │
                     ▼
    ┌─────────────────────────────────────┐
    │  Chapter 6 财务专业 + 移动/支付      │
    │  (Batch 4, 5 项)                    │
    │                                     │
    │  6.2 期末结账 ──→ 6.1 财务报表       │
    │              ──→ 6.3 共享服务        │
    │  6.4 移动端审批（独立，可并行）       │
    │  6.5 在线支付（独立，可并行）         │
    └─────────────────────────────────────┘
```

### 7.2 依赖关系矩阵（精确依赖）

| 上游 → 下游 | 依赖原因 | 强/弱依赖 |
|------------|---------|----------|
| Ch2 通用规范 → 所有 Chapter | 命名/状态机/事件总线/缓存/审计/迁移统一标准 | 强 |
| Ch4.2 EventBus → Ch4.3 20类事件触发 | 事件触发依赖 EventBus 基础设施 | 强 |
| Ch4.2 EventBus → Ch3.1-3.6 业务流程 | 业务流程中的通知依赖 EventBus | 弱（可先 Mock） |
| Ch3.4 入职→注册联动 → Ch3.6 离职管理 | 离职闭环依赖账号存在 | 强 |
| Ch3.1 招聘名额 → Ch3.2 招聘协同反馈 | 反馈依赖招聘需求存在 | 强 |
| Ch3.3 Offer 持久化 → Ch3.4 入职联动 | 入职依赖 Offer 数据 | 强 |
| Ch5.1 打印任务对接 → Ch6.5 在线支付 | 支付成功触发打印 | 弱（可先无支付打印） |
| Ch6.2 期末结账 → Ch6.1 财务报表 | 报表取数依赖科目期间余额 | 强 |
| Ch6.1 财务报表 → Ch6.3 共享服务 | 合并报表依赖子门店报表 | 强 |
| Ch6.2 期末结账 → Ch6.3 共享服务 | 合并前需各门店结账完成 | 强 |
| Ch3.5 证件报销 → Ch6.2 期末结账 | 报销凭证需结账前过账 | 弱 |
| Ch4.1 站内信整合 → 所有事件触发 | 事件触发需站内信渠道可用 | 强 |

### 7.3 关键路径（最长依赖链）

```
关键路径（决定整体工期）：

Ch2 通用规范
    ↓
Ch4.1 站内信整合 + Ch4.2 EventBus
    ↓
Ch3.1 招聘名额
    ↓
Ch3.2 招聘协同反馈
    ↓
Ch3.3 Offer 持久化
    ↓
Ch3.4 入职→注册联动
    ↓
Ch3.6 离职管理
    ↓
Ch5.1 打印任务对接（基础设施层）
    ↓
Ch6.2 期末结账
    ↓
Ch6.1 财务报表
    ↓
Ch6.3 共享服务
```

**关键路径共 11 个节点**，任何一个节点延期都会影响整体进度。

### 7.4 可并行项（缩短工期）

| 并行组 | 可同时进行的项 | 前置条件 |
|--------|--------------|---------|
| 并行组 A | Ch3.5 证件报销 + Ch3.6 离职管理 | Ch3.4 入职联动完成 |
| 并行组 B | Ch5.2 异步导出 + Ch5.4 附件对接 | Ch2 完成 |
| 并行组 C | Ch6.4 移动端审批 + Ch6.5 在线支付 | Ch2 + Ch4 完成 |
| 并行组 D | Ch4.4 邮件配置 + Ch4.5 AlertMock 替换 | Ch4.1 完成 |

### 7.5 实施顺序建议（按 Sprint）

| Sprint | 周期 | 完成内容 | 验收门禁 |
|--------|------|---------|---------|
| Sprint 1 | 第 1-2 周 | Ch2 通用规范 + Ch4.1 站内信整合 + Ch4.2 EventBus | EventBus 单元测试通过 + 站内信可发 |
| Sprint 2 | 第 3-4 周 | Ch3.1 招聘名额 + Ch3.2 招聘协同反馈 + Ch3.3 Offer 持久化 | 招聘链路可走通 |
| Sprint 3 | 第 5-6 周 | Ch3.4 入职联动 + Ch3.5 证件报销 + Ch3.6 离职管理 | HR 全链路闭环 |
| Sprint 4 | 第 7-8 周 | Ch4.3 20类事件触发 + Ch4.4 邮件配置 + Ch4.5 AlertMock 替换 | 通知体系完整 |
| Sprint 5 | 第 9-10 周 | Ch5.1 打印对接 + Ch5.2 异步导出 + Ch5.3-5.7 各模块对接 | 基础设施全部利用 |
| Sprint 6 | 第 11-12 周 | Ch6.2 期末结账 + Ch6.1 财务报表 | 财务核算闭环 |
| Sprint 7 | 第 13-14 周 | Ch6.3 共享服务 + Ch6.4 移动端审批 | 连锁 + 移动能力 |
| Sprint 8 | 第 15-16 周 | Ch6.5 在线支付 + 整体联调 + 验收 | 商用就绪 |

**总工期约 16 周**（4 个月），可按并行组缩短至 12-14 周。

### 7.6 风险点与缓解策略

| 风险 | 等级 | 影响 | 缓解策略 |
|------|------|------|---------|
| 财务专业公式错误（三大报表取数） | 🔴 高 | 报表数据失真，决策失误 | 财务专家评审公式 + 与手工账核对 |
| 第三方支付集成复杂（微信/支付宝） | 🟠 中 | 支付链路不可用 | 先用沙箱环境，逐渠道集成 |
| PWA 离线同步冲突处理复杂 | 🟠 中 | 离线审批数据丢失 | 优先保证在线审批可用，离线作为增强 |
| Eventual Consistency 导致状态不一致 | 🟠 中 | 业务单据与审批状态不同步 | 所有事件用 @TransactionalEventListener(AFTER_COMMIT) |
| 期末结账幂等性（重复结账） | 🟠 中 | 数据重复结转 | 数据库 UNIQUE 约束 + 业务校验双重保障 |
| 合并报表内部交易识别遗漏 | 🟠 中 | 合并数据虚增 | 财务专家定义识别规则 + 手动标记兜底 |
| 招聘名额跨年余额结转 | 🟡 低 | 跨年名额管理混乱 | 年末定时任务结转 + 人工审核 |
| 通知模板变量未填充 | 🟡 低 | 通知内容异常 | 单元测试覆盖所有模板变量 |
| 迁移脚本执行失败回滚 | 🟡 低 | 数据库结构不一致 | 每个脚本单独事务 + 失败自动回滚 |
| 前端 Mock 残留导致孤岛 | 🟠 中 | 后端建好无人用 | 强制 Mock 移除检查清单 + CI 校验 |

### 7.7 实施流程关键规则（强制）

1. **每个 Sprint 必须有验收门禁**：Sprint 结束前由独立审查 Agent 跑一遍验收检查清单
2. **跨模块联动必须在 Sprint 内闭环**：不允许"后端做好等下个 Sprint 再对接前端"
3. **通知模板注册必须先于代码合并**：避免代码引用未注册的模板
4. **迁移脚本必须可回滚**：每个 V 脚本配套 U 脚本（Undo）
5. **前端 Mock 移除必须随对应后端 API 一同提交**：禁止"先合并后端，前端 Mock 留下个 PR"
6. **关键路径节点延期超 3 天必须升级**：触发风险评审会议
7. **每个 P0 项必须完成 Chapter 1 五大防护检查**：缺一不可合并

---

## Chapter 8 验收检查清单汇总

> **本章汇总 27 项 P0 的全部验收用例，作为最终上线前的统一门禁**。
> **使用方式**：每个 P0 项实施完成后，逐项打勾确认；全部完成后由独立审查 Agent 进行最终验收。

### 8.1 验收门禁分级

| 等级 | 含义 | 通过标准 |
|------|------|---------|
| L1 单元测试 | 单个功能点验证 | 覆盖率 ≥ 80% |
| L2 集成测试 | 跨模块联动验证 | 所有联动检查清单通过 |
| L3 验收测试 | 端到端业务流程 | 全部用例通过 |
| L4 防孤岛检查 | 防止前端不调用后端 | Mock 移除 + 调用点清单完整 |
| L5 商用就绪 | 可对外发布 | 全部 L1-L4 通过 + 性能/安全达标 |

### 8.2 第一批验收清单（业务流程完整性 10 项）

#### 8.2.1 招聘名额系统（3.1）
- [ ] L3：HR 创建年度招聘名额 → 下发到门店 → 门店确认 → 名额生效
- [ ] L3：门店招聘需求提报时名额校验（usedCount < headcount）
- [ ] L3：入职成功时 usedCount += 1
- [ ] L3：名额追加申请流程（adjustment_requested 状态）
- [ ] L4：HRRecruitment.vue 中是否有真实名额查询调用
- [ ] L4：StoreRecruitment.vue 中是否校验名额余量

#### 8.2.2 门店↔HR 招聘协同反馈（3.2）
- [ ] L3：门店提报 → HR 反馈 approve → HR 接管
- [ ] L3：HR 反馈 feedback → 门店修改后重新提交
- [ ] L3：HR 反馈 reject → 门店看到驳回原因
- [ ] L3：门店负责人参与终面 + 门店面评
- [ ] L4：recruitment_feedback 表中数据流转完整

#### 8.2.3 录用 Offer 持久化（3.3）
- [ ] L3：HR 发送 Offer → 候选人接受 → 入职触发
- [ ] L3：候选人拒绝 Offer → 状态变更 + 通知 HR
- [ ] L3：Offer 过期自动失效
- [ ] L4：HRRecruitment.vue 中 Offer 数据真实持久化

#### 8.2.4 入职→注册账号联动（3.4）
- [ ] L3：completeOnboarding() 触发 createUserAccount()
- [ ] L3：根据职位自动分配默认角色
- [ ] L3：分配数据权限（限本门店）
- [ ] L3：生成激活令牌 + 发送激活通知
- [ ] L3：首次登录强制改密
- [ ] L4：Employee.storeId 在 createEmployeeProfile 中正确设置

#### 8.2.5 门店证件报销流程（3.5）
- [ ] L3：员工提交报销 → 门店审核 → HR 审核 → 财务报销 → 闭单
- [ ] L3：财务报销触发凭证生成
- [ ] L3：驳回流程（任一节点可驳回）
- [ ] L3：报销金额联动 finance_cost_record
- [ ] L4：报销附件对接 FileAttachment 模块

#### 8.2.6 离职管理流程（3.6）
- [ ] L3：离职申请 → 审批 → 交接 → 资产归还 → 离职证明 → 归档
- [ ] L3：归档触发账号停用（users.status='inactive'）
- [ ] L3：归档触发员工状态变更（employees.status='inactive'）
- [ ] L3：离职证明 PDF 生成
- [ ] L4：HREmployee.vue 中离职按钮真实调用 ResignationController

#### 8.2.7 HR→门店招聘名额下发联动（3.7）
- [ ] L3：HR 名额下发 → 门店收到通知
- [ ] L3：门店确认/拒绝名额
- [ ] L4：notification 表中存在名额下发通知记录

#### 8.2.8 门店招聘→HR 反馈联动（3.8）
- [ ] L3：门店提交 → HR 收到待办
- [ ] L3：HR 反馈 → 门店收到通知
- [ ] L4：approval_tasks 表中存在对应任务

#### 8.2.9 入职→注册账号创建联动（3.9）
- [ ] L3：入职完成 → 用户账号自动创建
- [ ] L3：用户账号默认停用状态，需激活
- [ ] L4：users 表与 onboarding_records 表关联

#### 8.2.10 证件报销→财务凭证/成本归集联动（3.10）
- [ ] L3：报销完成 → finance_voucher 自动生成
- [ ] L3：finance_cost_record 自动写入
- [ ] L3：凭证自动过账

### 8.3 第二批验收清单（站内通信 5 项）

#### 8.3.1 统一站内信渠道（4.1）
- [ ] L3：NotificationMessageConsumer.SITE_MSG 委托 SiteNotificationService
- [ ] L3：站内信落库 + WebSocket 推送
- [ ] L4：NotificationCenter.vue 真实展示新通知

#### 8.3.2 NotificationEventBus（4.2）
- [ ] L1：EventBus 单元测试通过
- [ ] L3：业务事件发布 → 监听器接收到
- [ ] L3：AFTER_COMMIT 事务边界正确

#### 8.3.3 20 类业务事件通知触发（4.3）
- [ ] L3：逐项验证 20 类事件触发（招聘/入职/离职/报销/订单/采购/仓储/会员/财务/支付）
- [ ] L4：notification 表中存在各类事件对应记录

#### 8.3.4 邮件服务器配置（4.4）
- [ ] L3：邮件发送测试通过
- [ ] L3：邮件模板渲染正确

#### 8.3.5 AlertNotificationServiceImpl Mock 替换（4.5）
- [ ] L3：设备告警 → 真实邮件发送（非硬编码邮箱）
- [ ] L4：AlertNotificationServiceImpl 中无硬编码邮箱/手机号

### 8.4 第三批验收清单（基础设施对接 7 项）

#### 8.4.1 打印任务对接订单模块（5.1）
- [ ] L3：订单创建 → 厨房工单打印
- [ ] L3：订单支付 → 收银小票打印
- [ ] L3：打印失败重试

#### 8.4.2 导出中心扩展为异步任务（5.2）
- [ ] L3：提交导出任务 → 后台处理 → 通知下载
- [ ] L3：状态流转 pending → processing → completed

#### 8.4.3 补充 12 个导出场景（5.3）
- [ ] L3：HR/采购/订单/财务/仓储/溯源/会员/系统 8 类导出全部可用
- [ ] L4：10 个页面工具栏均有导出按钮

#### 8.4.4 入职/报销/离职附件对接（5.4）
- [ ] L3：入职档案上传附件
- [ ] L3：报销单上传发票附件
- [ ] L3：离职交接单上传交接清单

#### 8.4.5 业务事件→事件总线（5.5）
- [ ] L3：14 类模块事件接入 EventBus
- [ ] L4：每类事件有对应监听器

#### 8.4.6 采购收货→打印（5.6）
- [ ] L3：采购收货完成 → 收货单打印

#### 8.4.7 各模块→异步导出（5.7）
- [ ] L3：10 个页面导出按钮真实调用后端

### 8.5 第四批验收清单（财务专业 + 移动/支付 5 项）

#### 8.5.1 财务报表（6.1）
- [ ] L3：三大报表生成公式正确
- [ ] L3：期末未结账时禁止生成
- [ ] L3：多门店合并报表正确
- [ ] L3：报表导出 Excel/PDF
- [ ] L4：FinanceReport*.vue 无 Mock

#### 8.5.2 财务期末结账（6.2）
- [ ] L3：损益结转凭证自动生成
- [ ] L3：成本结转依赖仓储数据
- [ ] L3：年末封账锁定凭证
- [ ] L3：反结账删除结转凭证
- [ ] L3：已封账期间不可反结账

#### 8.5.3 财务共享服务（6.3）
- [ ] L3：合并任务依赖子门店报表全部确认
- [ ] L3：内部交易自动识别并抵销
- [ ] L3：抵销凭证自动生成
- [ ] L3：集中收付资金流转正确

#### 8.5.4 移动端审批（6.4）
- [ ] L3：8 类业务审批流路由正确
- [ ] L3：审批通过回调业务模块状态推进
- [ ] L3：离线审批 PWA 同步正确
- [ ] L3：超时自动升级
- [ ] L3：移动端响应式适配（< 768px）
- [ ] L4：ApprovalCenter.vue 无 Mock

#### 8.5.5 在线支付（6.5）
- [ ] L3：微信 Native 扫码支付完整流程
- [ ] L3：支付宝电脑网站支付完整流程
- [ ] L3：异步回调验签 + 幂等
- [ ] L3：退款流程（部分/全额）
- [ ] L3：支付超时自动关闭
- [ ] L3：支付成功联动打印 + 会员积分
- [ ] L4：PaymentCheckout.vue 真实调用后端

### 8.6 全局验收门禁

#### 8.6.1 L1 单元测试门禁
- [ ] 后端单元测试覆盖率 ≥ 80%
- [ ] 前端单元测试覆盖率 ≥ 70%
- [ ] 所有 P0 项的 Service 层有单元测试
- [ ] 所有状态机有单元测试

#### 8.6.2 L2 集成测试门禁
- [ ] 跨模块联动全部通过（详见各 Chapter 联动检查清单）
- [ ] 事件总线发布/订阅测试通过
- [ ] 缓存层（L1+L2）测试通过
- [ ] RabbitMQ 消息发送/消费测试通过

#### 8.6.3 L3 验收测试门禁
- [ ] 端到端业务流程测试通过（详见 8.2-8.5）
- [ ] 用户角色权限测试通过（RBAC）
- [ ] 多门店数据隔离测试通过
- [ ] 数据迁移脚本执行成功

#### 8.6.4 L4 防孤岛检查门禁
- [ ] **全局 withMockFallback 全部移除**（grep 校验）
- [ ] 所有前端 API 调用点有对应后端端点
- [ ] 所有新建 Vue 页面无硬编码示例数据
- [ ] 所有 console.log/info/debug 已清理
- [ ] 所有 P0 项的 Mock 移除检查清单完成

#### 8.6.5 L5 商用就绪门禁
- [ ] 性能测试通过（接口响应 < 3s，慢查询 < 5s）
- [ ] 安全测试通过（SQL 注入/XSS/CSRF/越权）
- [ ] 日志/监控/告警配置完成
- [ ] 数据库备份恢复测试通过
- [ ] 生产环境配置（PostgreSQL/Redis/RabbitMQ）就绪
- [ ] 文档更新完成（API 文档 + 操作手册）

### 8.7 全局 Mock 移除最终扫描清单

> **此清单在所有 Sprint 完成后执行一次全局扫描，确保无残留 Mock。**

```bash
# 1. 扫描前端 withMockFallback
grep -r "withMockFallback" frontend/src/api/ --include="*.ts"
# 预期：无匹配

# 2. 扫描前端 console.log/info/debug
grep -rE "console\.(log|info|debug)" frontend/src/ --include="*.vue" --include="*.ts"
# 预期：无匹配（console.error/warn 允许）

# 3. 扫描前端硬编码 Mock 数据
grep -rE "mock|Mock|MOCK" frontend/src/views/ --include="*.vue" | grep -v "//" | grep -v "node_modules"
# 预期：仅注释或变量名包含 mock 字样

# 4. 扫描后端 TODO/FIXME
grep -rE "TODO|FIXME|XXX" backend/src/main/java/ --include="*.java"
# 预期：无未完成标记

# 5. 扫描硬编码邮箱/手机号
grep -rE "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}" backend/src/main/java/ --include="*.java" | grep -v "test\|Test"
# 预期：仅配置文件中存在

# 6. 扫描硬编码颜色值
grep -rE "#[0-9a-fA-F]{6}" frontend/src/views/ --include="*.vue" | grep -v "//\|/\*"
# 预期：无匹配（必须使用 CSS 变量）

# 7. 扫描 teleported 违规（dialog 内的 el-select/el-date-picker 等）
grep -rE "<el-(select|date-picker|time-picker|cascader)" frontend/src/views/ --include="*.vue" -A 2 | grep -v "teleported=\"false\"" | grep "el-\|teleported"
# 预期：dialog 内的弹出组件均有 teleported="false"
```

### 8.8 27 项 P0 验收统计模板

| 批次 | 项数 | L1 通过 | L2 通过 | L3 通过 | L4 通过 | L5 通过 | 完成率 |
|------|------|--------|--------|--------|--------|--------|--------|
| 第一批 业务流程 | 10 | __/10 | __/10 | __/10 | __/10 | __/10 | __% |
| 第二批 站内通信 | 5 | __/5 | __/5 | __/5 | __/5 | __/5 | __% |
| 第三批 基础设施 | 7 | __/7 | __/7 | __/7 | __/7 | __/7 | __% |
| 第四批 财务/移动/支付 | 5 | __/5 | __/5 | __/5 | __/5 | __/5 | __% |
| **合计** | **27** | **__/27** | **__/27** | **__/27** | **__/27** | **__/27** | **__%** |

**商用就绪标准**：27/27 全部 L5 通过 + 全局 Mock 扫描无残留 → 可对外发布。

---

## 附录 A：文档变更记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|---------|------|
| V1.0 | 2026-06-25 | 初版创建，覆盖 Chapters 0-8，27 项 P0 全部详细设计完成 | AI Architect Agent |

## 附录 B：与其他文档的关系

```
┌─────────────────────────────────────────────────────────────┐
│  docs/audit/IMPLEMENTATION_DESIGN.md（本文档）              │
│  ─────────────────────────────────────────────────────────  │
│  作用：27 项 P0 的详细设计与防护规范（防孤岛/防摆件）        │
│  位置：实施前的设计评审依据 + 实施中的对照清单 + 上线前的门禁│
└─────────────────────────────────────────────────────────────┘
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
   ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
   │ PROJECT_     │ │ DATA_        │ │ DEVELOPMENT_ │
   │ CONTEXT.md   │ │ LIFECYCLE.md │ │ STATUS.md    │
   │              │ │              │ │              │
   │ 项目背景     │ │ 数据流转     │ │ 进度评估     │
   │ 业务上下文   │ │ V3 第19节    │ │ V2 Section 9 │
   │              │ │ 站内通信     │ │ 60 项缺失    │
   │              │ │ V3 第20节    │ │ 27 项 P0     │
   │              │ │ 基础设施     │ │ 4 批排序     │
   └──────────────┘ └──────────────┘ └──────────────┘
```

**文档使用顺序**：
1. **设计阶段**：先读本文档 Chapter 1-2（防护原则 + 通用规范），再读对应批次 Chapter
2. **实施阶段**：参照对应 Chapter 的表设计/API/状态机/联动清单
3. **验收阶段**：参照 Chapter 8 验收检查清单逐项打勾
4. **追溯阶段**：DATA_LIFECYCLE.md 提供数据流转细节，DEVELOPMENT_STATUS.md 提供整体进度

## 附录 C：术语表

| 术语 | 含义 |
|------|------|
| 孤岛 | 后端建好但前端不调用，导致功能无法使用 |
| 摆件 | 前端有 UI 但调用的是 Mock 数据，后端实际未对接 |
| Mock 移除检查清单 | 防护机制之一，强制前端不再使用 Mock 数据 |
| 调用点清单 | 防护机制之一，明确前端每个调用点对应的后端端点 |
| 联动检查清单 | 防护机制之一，明确跨模块事件的触发与影响 |
| 通知触发点 | 防护机制之一，明确业务事件对应的通知模板与接收人 |
| 验收测试用例 | 防护机制之一，至少 3 个用例（正常+异常+边界） |
| EventBus | 基于 Spring ApplicationEvent 的事件总线 |
| AFTER_COMMIT | 事务提交后触发的事件监听器，确保数据已落库 |
| 状态机 | 业务对象的状态流转定义，VARCHAR(32) 编码 |
| 抵销分录 | 合并报表时消除内部交易的会计分录 |
| PWA | Progressive Web App，支持离线能力的 Web 应用 |

---

**文档结束。**

> 本文档覆盖 27 项 P0 必做项的完整设计细节与技术指导规范，包含 5 大防护机制（防孤岛/防摆件）、4 批 27 项详细设计、实施顺序与依赖关系、全局验收门禁。所有实施工作必须严格遵循本规范，确保最终交付的产品无孤岛、无摆件、可商用。


