# STATUS.md — 单一真相源（轻档，只写文档）

> **本文件是唯一状态源。** 其他入口（`docs/INDEX.md` / `remediation-roadmap.md` / `production-remediation-task-board.md` / `production-known-limitations.md` / `docs/project-context/repo-state.md`）只放指向本文件的指针，不各自维护状态。
> **同步规则（PG-002 扩展）**：每次卡片收口或 push 后必改本文件；其他入口不改状态。
> **性质**：视图层快照，非权威；与 git 不一致时以 git 为准。快照时点：2026-10-13。

## 1. 当前 HEAD 与同步状态

| 项 | 值 |
|----|----|
| 分支 | `master`（单分支工作流） |
| HEAD | **上次同步时点：2026-10-13**（不写具体哈希——任何提交都会使其过时）→ 以 `git rev-parse HEAD` 为准 |
| 远程 | `origin` = https://github.com/wukaiktc1234/new-map.git；`origin/master` 以 `git rev-parse origin/master` 为准（上次同步时点 `671d87f`） |
| 同步 | 以 `git rev-list --count origin/master..HEAD` 实测为准（上次同步时点 ahead=57 / behind=0，未 push；push 由 Owner 执行）；push 仅由 Owner 显式指令触发；`.git/hooks` 实测无 `post-commit`（PG-001 规则 6） |
| 工作区 | **549 条目 = 456 M + 92 ?? + 1 D**（Owner WIP，留盘不暂存） |

## 2. 活跃卡（只列 5）

| 卡 | 状态 | 下一个动作 |
|----|------|-----------|
| **P1-APK-DEVICE-001** | 卡号/优先级已确认（Owner 2026-10-13）：**P1 / 整特性一次入库**；三项处置已入卡面范围（`apk-download/` → .gitignore 构建产物、`certs/` → .gitignore 敏感物含私钥、`android/` → 中间产物 .gitignore + 自定义原生代码入库） | Owner 放行后整特性入库，含 4 项卡面待补：guards.ts APK 四块 / request.ts X-Device-Id + FormData / index.ts 三条路由 / SecurityConfig 3 条 permitAll 与 `meta.requireAuth` 口径核对 |
| **§24.3s（N15 order:add）** | 实施 CLOSED（`68fb1d3` + rank 216 `success=t` + 58 F+E 集等） | Owner Live 验证：持角色 200 / 无角色 403 / UI 菜单（与批次 A/B/C 同批） |
| **P1-SEC-009** | QA 渗透验证未关闭（**发布门禁前必须关闭**） | 待 Owner 安排执行窗口 |
| **P0-SEC-001** | 第二阶段 71 Controller 审计，**暂缓**（Owner 2026-10-12） | Owner 放行后恢复 |
| **P0-WORKSPACE-WIP-CONSOLIDATION-001** | **暂缓**（GOV-003 分类表为其输入） | Owner 裁决五批入库 |

## 3. 挂起项（只列 5）

| 项 | 类型 | 说明 |
|----|------|------|
| Live 验证批次 A/B/C + N15 Live | 等 Owner 执行 | Agent 不跑 Live 验证 |
| **ENV-14** `flyway_schema_history` 卫生异常（20260922.001 checksum=NULL + 缺 V999） | 等 Owner 裁决 | 无阻断（Flyway 容忍）；**不 repair**；后续卡顺手修或另开卡 |
| **P1-SEC-002** | **部分收口（验收依赖 P1-APK-DEVICE-001）** | 卡面已提交（`eec8fb9`），但 `/verify-code`、`/device-activation` 路由条目属该卡簇 → 提交面无该路由，验收标准（未登录不重定向）不可执行；Live 验证须待该卡入库 |
| 15 个被忽略 `*.md`（非 production 面） | 等 Owner 裁决 | 是否同属 ENV-4 误伤；`production-*.md` 六个已清（`4c43418` + `8065e84`） |
| **Owner push**（commit 数以 `git rev-list --count origin/master..HEAD` 为准；上次同步时点 58） | 等外部 | 无 auto-push hook（PG-001 规则 6）；commit 与 push 分离；push 后按 PG-002 刷新本文件第 1 节 |

## 4. 禁区（不该碰）

- **Live 验证环境口径（Owner 选 C：混合快照，2026-10-13）**：= tracked-only worktree + **13 个必要未跟踪类**（最小可编译集）；纯 tracked 树编译不过（ENV-10）。结论须注明「混合快照（tracked + 13 WIP 类）」，**不得声称纯 tracked**。
- **Owner WIP**：456 M + 92 ?? + 1 D 不暂存；PG-001 精确 `git add <文件>`，禁 `git add .` / `-A` / 目录通配。
- **`flyway_schema_history`**：禁 `repair`（历史篡改需 Owner 逐次批准）。
- **`inventory_warning` / `inventory_warning_rule`（单数 legacy 表）**：不碰；现行是复数 `inventory_warning_records`（rank 214）。
- **治理文档历史面**：任务板 / KL / incidents / 实施记录 = **只增不改**。
- **KL-085 / KL-086 冻结项**：不解冻；`DEFAULT_STORE_ID` 8 行使用点维持冻结。
- **`.gitignore` ENV-4 残留专用段**：不批量改，逐条经 Owner 裁决。
- **未跟踪特性簇**（APK 设备 / P1-A 银行 / 原料扣减 / Receipt）：属 Owner WIP，不自行提交。
- **手写 schema 脚本**：禁止（PG-003）；表结构变更一律 Flyway（只增不改）。
- **含 schema/seed 的卡**：收口必须含 `flyway_schema_history` `success=t` 行，且迁移经**独立 Flyway runner**（`mvn test` 上下文不算，PG-001 v2 条款 2/3）。

## 5. 新会话第一步

1. 读本文件。
2. 读 `docs/project-context/process-guards.md`（PG-001 v2 / PG-002 / PG-003 / PG-006 判档）。
3. 读 `production-governance-incidents.md`（GOV-INCIDENT-001–004）。
4. 读任务板**尾部**最新追加段（当前 §24.3u / §24.3v）——只增不改，读尾部而非中部。
5. 跑 git 事实核对（HEAD / ahead-behind / status），不依赖本文件快照。
