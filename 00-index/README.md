# 项目工程现实地图 (Project Master Map)

## 概述

本文档集是食品溯源系统（Food Traceability System）的工程现实地图，提供项目全景视图，用于快速理解系统规模、模块分布和技术架构。

本文档集包含两个阶段：
- **PROJECT-MASTER-MAP-001**：工程现实基线（系统有什么、在哪里、有哪些冲突）
- **PROJECT-MASTER-MAP-002**：业务地基 + 上下游 + 数据继承 + 数据流（为什么能工作、依赖哪些上游基础）

## 文档用途

- **新人入职**：快速了解项目全貌和代码分布
- **架构评审**：审视模块划分和职责边界
- **任务拆分**：基于地图定位改动范围
- **技术债务盘点**：识别重复代码和耦合热点
- **业务分析**：理解业务基础和上下游依赖关系
- **数据治理**：追踪数据继承和血缘关系

## 文档结构

### MAP-001 (工程现实基线)

| 文件 | 内容 |
|------|------|
| `project-master-map.md` | 主地图汇总：模块规模、技术栈、目录索引 |
| `frontend-map.md` | 前端地图：终端矩阵、技术栈、模块详细信息 |
| `business-object-map.md` | 业务对象地图：核心业务对象、遗留对象、关系图 |
| `db-reality-map.md` | 数据库现实地图：表结构、索引、约束 |
| `api-map.md` | API 地图：路由、端点、消费情况 |
| `event-job-map.md` | 事件/任务地图：领域事件、监听器、调度器 |
| `permission-data-scope-map.md` | 权限/数据范围地图：权限模型、数据范围 |
| `state-machine-map.md` | 状态机地图：订单、凭证、库存等状态机 |
| `truth-conflict-map.md` | 真相/冲突地图：真相源、冲突点、裁决 |
| `legacy-map.md` | 遗留代码地图：废弃表、服务、控制器 |
| `issue-lifecycle-map.md` | Issue 生命周期地图：审计、决策、开发、QA |

### MAP-002 (业务地基 + 上下游 + 数据继承)

| 文件 | 内容 |
|------|------|
| `foundation-map.md` | Foundation 对象地图：基础数据对象、依赖关系 |
| `upstream-dependency-map.md` | Upstream 依赖地图：业务能力追溯、依赖链路 |
| `downstream-impact-map.md` | Downstream 影响地图：基础数据停用影响分析 |
| `capability-readiness-map.md` | Capability 就绪地图：业务能力依赖分析 |
| `broken-chain-map.md` | 断链地图：识别 31 个断链问题 |
| `data-inheritance-map.md` | 数据继承地图：页面→Dialog→API→DB 继承关系 |
| `dialog-form-data-contract-map.md` | Dialog/Form 数据契约地图：20 个 Dialog 数据契约 |
| `master-data-source-map.md` | 主数据源地图：15 个主数据源、Reference/Snapshot/Derived 判定 |
| `business-identity-propagation-map.md` | 业务身份传播地图：ID 传播路径和问题 |
| `business-data-flow-map.md` | 业务数据流地图：采购/订单/财务/溯源链路 |
| `data-design-map.md` | 数据设计地图：表关系、字段映射、设计问题 |
| `data-lineage-map.md` | 数据血缘地图：数据创建到消费的完整血缘 |
| `cross-domain-coordination-matrix.md` | 跨域协调矩阵：8 个业务域的数据所有权 |

### MAP-REVIEW (系统治理评审)

| 文件 | 内容 |
|------|------|
| `review/system-fact-baseline.md` | 系统事实基线：44条已验证事实 |
| `review/business-requirements-baseline.md` | 业务需求基线：17个业务需求 |
| `review/architecture-principle-baseline.md` | 架构原则基线：16个架构原则 |
| `review/systemic-problem-register.md` | 系统问题注册表：13个Root Cause |
| `review/architecture-product-decision-register.yaml` | 决策注册表：14个待决策项 |
| `review/systemic-remediation-candidates.md` | 治理候选清单：17个候选 |
| `review/project-master-review-summary.md` | 评审总结：Executive Summary |

### 公共文件

| 文件 | 内容 |
|------|------|
| `project-master-map-summary.md` | 项目主地图摘要：核心统计、整改项、业务洞察 |
| `project-master-registry.yaml` | 项目主注册表：模块、业务对象、API 注册 |
| `project-master-map-conflict-registry.yaml` | 冲突注册表：16个一致性冲突 |
| `MAP-META-CONFLICT.md` | 地图一致性冲突报告 |
| `README.md` | 本文件：说明文档 |

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.0 |
| 前端框架 | Vue.js 3.5.x |
| 数据库 | PostgreSQL 18 |
| 缓存 | Redis |
| 消息队列 | RabbitMQ |

## 使用方式

### 工程现实分析 (MAP-001)

1. 阅读 `project-master-map.md` 获取项目全景
2. 根据任务类型定位对应模块
3. 使用目录索引快速跳转到相关代码区域

### 业务基础分析 (MAP-002)

4. 参考 `foundation-map.md` 了解基础数据对象
5. 参考 `upstream-dependency-map.md` 了解业务依赖关系
6. 参考 `downstream-impact-map.md` 了解基础数据停用影响
7. 参考 `capability-readiness-map.md` 了解业务能力就绪状态
8. 参考 `broken-chain-map.md` 了解断链问题

### 数据治理分析

9. 参考 `data-inheritance-map.md` 了解数据继承关系
10. 参考 `dialog-form-data-contract-map.md` 了解 Dialog 数据契约
11. 参考 `master-data-source-map.md` 了解主数据源
12. 参考 `business-data-flow-map.md` 了解业务数据流
13. 参考 `data-lineage-map.md` 了解数据血缘

## 维护约定

- 本地图应随项目重大变更同步更新
- 新增模块时需在主地图中登记
- 统计数据来源于静态分析脚本，定期刷新
- Foundation 对象和 Upstream 依赖需同步更新
- 数据继承和血缘关系需同步更新
