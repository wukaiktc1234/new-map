# 开发与排障避坑指南（跑通经验落袋）

> 用途：把 P1-USER-LOCATION-002 联调期（2026-10-13~15）实际踩通的经验固化，防止新会话重走弯路。
> 只写**已实测成立**的结论；未验证的一律标注「未验证」。
> 进度与裁决状态看 [STATUS.md](../STATUS.md) 与任务板，本文件不复述状态。

## 1. 联调生效法则

| 改了什么 | 生效方式 | 实测依据 |
|----|----|----|
| MyBatis mapper XML（`*.xml`） | **必须重启后端**才生效：XML 在启动时解析进 MybatisConfiguration，运行期改文件不生效 | `UserResultMap`（提交 `bd57473`）改后重启，`id`/`fullName` 才有值 |
| Java 实体 / Jackson 注解 | 重启后端（需重新编译） | `@JsonSerialize`、`@JsonProperty` 加后重启才见响应变化 |
| `application.yml` | 重启后端 | `map-underscore-to-camel-case`（L55–60）在启动时读取 |
| 前端 `.vue` / `.ts` | vite HMR 通常即时生效；**composable 与类型改动建议整页刷新** | 本轮三处显示修复在刷新后一致 |

**F12 抓包时机与判据顺序**（本轮 3 个显示 bug 全部靠这个顺序定位）：
1. 先开 Network 面板并勾 Preserve log，**再**触发操作；
2. 看**原始 JSON**（字段名、引号、类型），不要先看渲染后的 DOM；
3. 同一个 ID 在不同接口里类型可能不同 —— 实测：`departments/tree` 返回 `"id": "576"`（string），`users/list` 返回 `"departmentId": 576`（number）；
4. 确认请求**是否真的发出**：本轮编辑保存日志里没有 PUT，是前端 `editingId=null` 直接 `return`，不是后端问题。

**PowerShell 递归检索**：`Select-String -Path <单个文件>` 不递归；递归要 `Get-ChildItem -Recurse -Include *.java | Select-String -Pattern 'xxx'`。检索范围限定 `src/`，别扫 `node_modules`。优先用 harness 的 grep 工具（带行号、可 include 过滤）。

## 2. 雪花 ID（19 位 Long）全链路坑

- JS 安全整数上限 `2^53-1 = 9007199254740991`（16 位）。`IdType.ASSIGN_ID` 生成的雪花 ID 是 19 位（实测样本 `2110437068670001154`）→ `JSON.parse` 后**精度截断** → 前端拿到近似值 → 用近似值请求 `/v1/users/{id}` → 后端查不到 → 报「用户不存在」。
- 后端对策：实体字段级 `@JsonSerialize(using = ToStringSerializer.class)`，让 ID 以字符串进响应体。字段级注解被**任何** ObjectMapper 尊重，因此不受 `WebConfig` 与 `JacksonConfig` 两套 ObjectMapper 谁生效的争议影响（`User.id` 已加，提交 `1efdc32`；先例 `Employee.java` L27）。
- 全局方案（让 `WebConfig` 复用 `JacksonConfig` 的 ObjectMapper，1 个文件覆盖 293 个实体）尚未裁决；**哪个 ObjectMapper 在运行期生效仍需 Live 抓包确认**。
- 前端类型面未对齐：`UserItem.id: number`（`user.ts` L17）与运行时 string 不符；`editingId: ref<number | null>`（`UserManagementTab.vue` L83）同理。任何 `===` 比较或 `Number()` 转换都会踩坑。
- 判据：**API 级 PASS（响应里 ID 带引号）≠ UI 级 PASS**。V-A1 已 API 级 PASS，UI 级 FAIL，已记录于实施记录 §7.13。

## 3. 前端表单下拉回退显示裸 ID 的根因

**根因是类型不匹配，不是数据没加载。** 机制（读 `element-plus/es/components/tree-select/src/` 源码确认）：

1. `tree-select.mjs` L33：`key = props.nodeKey || props.valueKey || "value"`；
2. `tree.mjs` L20–27：`propsMap` 被 `props.props` 覆盖 → 本例 `value='id'`、`label='name'`；
3. `useOption.mjs` L23–24：`currentLabel = props.label ?? props.value`；
4. `useSelect.mjs` L222–245：`getOption(value)` 用**严格 `===`** 匹配；无匹配 → 回退 `currentLabel = value ?? ""` → **渲染裸 ID**（这就是编辑页显示 `576` 的直接原因）。

对策与排除项：
- 赋值时归一：`departmentId: row.departmentId ? String(row.departmentId) : ''`（提交时 Spring 自动转 Long，后端不受影响）；
- 显式 `node-key="id"`（搜索筛选 + 编辑表单两处）；
- 不要指望 label 兜底 —— label 只在匹配成功时生效；
- 已排除：`selectProps.persistent` 默认 true（`select.mjs` L190–194）→ `manuallyRenderSlots` 路径不走；`tree-node.mjs` L112 用 `v-show` → 所有节点都注册 option，不是懒渲染；
- 排查顺序：先看两个 API 的原始 JSON 类型 → 再看组件匹配逻辑 → 最后才怀疑时序（若类型已归一仍显示裸 ID，才是时序问题，需要 `await` tree 加载或 `cache-data`）。

## 4. 验证原则

- **API 级 PASS ≠ UI 级 PASS**。UI 受 JS 精度、渲染时序、组件匹配逻辑影响。
- 三层分开交验，每层单独出证据：**DB（psql）→ API（F12 原始 JSON）→ UI（DOM 渲染）**。三层结论不一致时，以能复现的那一层为准，并记录另一层为何不一致。
- 不猜只列证据；失败贴原始输出。
- Live 验证由 Owner 跑（GPT 不跑）。

## 5. 已实测的口径与命名陷阱（防再踩）

- `@TableField(exist=false)` 只影响 MyBatis-Plus 生成的 SQL，**不影响** XML `resultType` 自动映射。
- `resultType` 自动映射只看属性名，不看 `@TableId`/`@TableField` → `user_id`→`userId`（实体无此属性）→ `id=null`；`name`→`name`（实体属性是 `fullName`）→ `fullName=null`。
- `users.role_names` 是死列（全仓无写入点）→ 恒 NULL → UI 显示「未分配」。权威口径是 `GET /v1/users/{id}/role-names`。
- `getUserRoleNames` 读的是 `users.roles` JSON 列，**不是** `user_roles` 表（见任务板 §24.6a）。
- psql 实测：`departments` 主键是 `department_id`、名称列是 `dept_name`（不是 `id`/`name`）；`user_roles.user_id`/`role_id` 是 `character varying`（`varchar = bigint` 会报操作符错误）；中文查询需 `$env:PGCLIENTENCODING="UTF8"`。
- PowerShell：commit message 含 `"` 时用单引号变量，否则消息被截断、剩余部分被当 pathspec（本轮实际踩过，提交失败）。
- 行尾：仓库文件多为 CRLF，但 `UserMapper.xml`、实施记录、任务板、`frontend/src/api/system/user.ts` 为 LF。edit 前先确认行尾，否则 anchor 找不到。

## 6. 提交与交验纪律（与 PG-001 对齐）

- 精确 `git add <文件>`，禁 `git add .` / `-A` / 目录通配。
- 每片独立提交 + 独立交验，禁止合并提交，不改写历史。
- Owner WIP 永不入库。需要「只提交自己的 hunk」时，用重建 blob 的方式，不动工作区：
  1. `git cat-file blob HEAD:<path>` 取提交面原文；
  2. 在其上应用自己的改动（按文件行尾）；
  3. `git hash-object -w <temp>` → `git update-index --replace --cacheinfo 100644,<sha>,<path>`；
  4. 交叉校验：暂存面增删 + 工作区剩余增删 = 完整差异增删（本轮实例 39+42=81、13+23=36，通过）。
- 提交面与工作区不一致时，按 PG-001 条款 7 登记差异，并说明 Live 跑的是哪一侧。
