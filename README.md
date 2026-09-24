# 食品溯源系统 (Food Traceability System)

面向个体餐饮企业的全栈溯源管理平台。

## 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Spring Boot 3.2.0 + MyBatis Plus 3.5.5 + JDK 21 |
| 数据库 | **PostgreSQL 18** (待对接) / Redis / RabbitMQ |
| 前端 | Vue.js 3.3.8 + TypeScript 5.2.2 + Element Plus 2.4.4 |
| 构建 | Maven 3.9.x / Vite 5.0.0 / npm 9.x |

## 快速启动

```bash
# 后端
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

> 📌 **详细进度、模块清单、技术债务 → 见 [PROJECT_STATUS.md](./PROJECT_STATUS.md)**

### 已完成阶段

| 阶段 | 内容 | 状态 |
|------|------|------|
| Phase 0 | 基础设施(RabbitMQ 11队列 + 通用组件 + 统一类型) | ✅ |
| Phase 1 | 业务深化(通知SMTP发送 + 调度@Scheduled引擎 + 备份pg_dump) | ✅ |
| Phase 2 | 核心业务(溯源主链路 + 库存预警 + 召回 + 成本核算) | 🔴 待开始 |

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
