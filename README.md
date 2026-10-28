# 食品溯源系统 (Food Traceability System)

面向个体餐饮企业的全栈溯源管理平台。

## 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Spring Boot 3.2.0 + MyBatis Plus 3.5.5 + JDK 21 |
| 数据库 | **PostgreSQL 18** (待对接) / Redis / RabbitMQ |
| 前端 | Vue.js 3.3.8 + TypeScript 5.2.2 + Element Plus 2.4.4 |
| 构建 | Maven 3.9.x / Vite 5.0.0 / npm 9.x |

## 环境前置（必读）

本机 JDK **不在 PATH 上**，也没有预设 `JAVA_HOME`；`where java` 查不到属正常现象，**不代表没有 JDK**。

```powershell
# 本仓库自带的 JDK 21（唯一正确路径）
$env:JAVA_HOME = "P:\my-new-project\JDK21"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
java -version   # 期望 21.x
```

> 历史坑：`H:\fuwu\jdk-17...` 目录已不存在，指向它的 JAVA_HOME 会让 `mvn` 报
> `The JAVA_HOME environment variable is not defined correctly`，并被误判成「本机没有 JDK」。
> 详见 `NIGHT_PATROL_LOG.md` 的 JAVA_HOME 记录。

## 快速启动

```bash
# 后端（必须先按上面设置 JAVA_HOME）
cd backend && mvn spring-boot:run -DskipTests

# 前端（新终端）
cd frontend && npm install && npm run dev
```

- 后端: http://localhost:8081/api
- 前端: http://localhost:3000/
- 默认账号: admin / &lt;redacted&gt;

## 项目结构

```
├── backend/src/main/java/com/example/demo/
│   ├── controller/    # REST API 控制器
│   ├── service/       # 业务逻辑层
│   ├── mapper/        # MyBatis 数据访问
│   ├── entity/        # 实体类 (@TableLogic + @Version)
│   ├── dto/           # 数据传输对象 (@Valid)
│   ├── config/        # 配置类 (含RabbitMQ 11队列)
│   ├── common/mq/     # MQ基础设施 (Producer/Consumer基类)
│   ├── scheduler/     # @Scheduled 调度引擎
│   └── security/      # JWT认证 + RBAC权限
├── frontend/src/
│   ├── views/         # 页面视图 (~100页)
│   ├── components/    # 通用组件 (SearchForm/ProTable等)
│   ├── api/           # API封装 (request实例)
│   ├── types/         # TypeScript类型定义
│   └── stores/        # Pinia状态管理
└── docs/archive/      # 历史文档归档 (157个文件)
```

## 当前状态

> 📌 **详细进度、模块清单、技术债务 → 见 [docs/INDEX.md](./docs/INDEX.md)（当前事实源索引）**

### 已完成阶段

| 阶段 | 内容 | 状态 |
|------|------|------|
| Phase 0 | 基础设施(RabbitMQ 11队列 + 通用组件 + 统一类型) | ✅ |
| Phase 1 | 业务深化(通知SMTP发送 + 调度@Scheduled引擎 + 备份pg_dump) | ✅ |
| Phase 2 | 核心业务(溯源主链路 + 库存预警 + 召回 + 成本核算) | ✅ 已实现，进度以 [INDEX](./docs/INDEX.md) 为准 |

### 已完成模块 (16个)

安全认证 · 审计日志 · 文件管理 · 系统配置 · 定时任务 · 消息通知 · 数据备份 · 用户权限 · HR人事 · 产品菜品 · 采购管理 · 仓储库存 · 订单厨房 · 财务管理 · 设备管理 · 门店管理

**总计**: 230+ Java文件, 300+ REST API, 100+ Vue页面

## 开发规范

- 编码规则: [`.trae/rules/project_rules.md`](./.trae/rules/project_rules.md) (**强制遵守**)
- 工作流: [`.trae/skills/enterprise-module-dev/SKILL.md`](./.trae/skills/enterprise-module-dev/SKILL.md)
- 部署: [`DEPLOY.md`](./DEPLOY.md)

## 关键约束速查

- `@TableId(type = IdType.AUTO)` — 不是 IDENTITY
- Controller 禁止注入 Mapper — 必须走 Service
- DTO 字段必须有 `@Valid` 校验注解
- Java布尔值小写: `false` 不是 `False`
- 写操作必须 `@Transactional(rollbackFor = Exception.class)`
- Entity 必须有 `@TableLogic` + `Integer deleted`
- 前端禁止 `any` 类型，必须使用 `request` 实例发请求

---

## 📚 知识资产（Knowledge Base）

> 新接手者 / 新 AI 会话的入职手册。四项，按需查阅。

| 文档 | 内容 | 什么时候看 |
|---|---|---|
| [System-Map.md](docs/knowledge/System-Map.md) | 系统全景：业务时序图（POS→支付→扣库存→资金流水→交班→对账）、模块边界与**写入权归属**、核心表 ER 图（**金额列标元/分**）、9 条可直接跑的验证 SQL | 第一次接触这个项目时 |
| [Gate-Baseline-and-Health.md](docs/knowledge/Gate-Baseline-and-Health.md) | 全部质量门禁清单、当前健康数字、**提交前必跑命令**、数字判读表（含「看到 52/2 算正常」） | 每次提交 / 发版前 |
| [ADR-and-Danger-Zones.md](docs/knowledge/ADR-and-Danger-Zones.md) | **防雷指南**：财务红线 R1–R5、代码陷阱 C1–C5、验证陷阱 V1–V6、7 条核心铁律、按症状查原因的事故速查表 | 动手改任何东西之前 |
| [Roadmap-and-Known-Issues.md](docs/knowledge/Roadmap-and-Known-Issues.md) | 缺表 59→45 的分批结论（含 skip 逐张理由）、6 项已知妥协设计、遗留缺陷、**试运行前 Checklist** | 排期 / 判断「能不能上线」时 |

**三条最先要记住的**：
1. **历史账绝不回溯重算**（口径变更只对新数据生效）；
2. **验证必须用管理该对象的工具**（Flyway 迁移不能用 `psql -f` 裸跑；全量测试不能用子集代表）；
3. **能不动数据就不动**；动数据只能因为它脏了，且**冲正优于删除**。
