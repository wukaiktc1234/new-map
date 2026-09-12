# D-IG — Business Capability Set Establishment Charter 001

> **状态**: ACTIVE_REFERENCE — METHOD CHARTER
> **Decision ID**: D-IG
> **性质**: Pre-Screen input establishment method
> **前置边界**: D-IG = OPEN；A1/A2 = CONFIRMED；Production Gate-0 = BLOCKED
> **禁止事项**: 本 Charter 不定义 Identity、不决定 H1/H2、不授权 Schema/Migration。

## 1. Purpose

本 Charter 定义 Business Capability Set 如何建立，使 Identity Necessity Pre-Screen 的输入可追溯、可复核、不可由工程团队自行补全为“业务需求”。

Business Capability Set 的目标不是枚举所有餐饮行业可能能力，而是在 D-IG 当前证据边界内建立一个可审计的能力集合，并明确哪些能力已经得到证据支持、哪些由 BUSINESS_OWNER 明确声明、哪些仍未知。

## 2. Capability Source Boundary

Capability Set 只允许三类来源状态：

- `IMPLEMENTED`: 当前工程证据能够定位到实际业务能力路径；
- `DECLARED`: BUSINESS_OWNER 已明确记录的业务能力，必须存在可定位 Owner 记录；
- `UNKNOWN`: 可能存在但尚未由 Owner 或可靠证据确认的能力。

以下内容不得直接升级为 `DECLARED`：

- 行业惯例；
- 工程团队推测；
- “未来可能需要”的设计想象；
- 单一示例反推的全局需求；
- 仅由表名、字段名或 endpoint 名称推断的业务能力。

`UNKNOWN` 可以作为缺口/待确认状态被记录，但不得作为 Pre-Screen 排除候选的依据。

## 3. Establishment Owner and Roles

### 3.1 Execution Owner

`ENGINEERING_TEAM` 负责建立初版 Capability Set，职责限于：

1. 从现有 evidence package 中提取可定位的 `IMPLEMENTED` 能力；
2. 登记已存在的 Owner-declared capability 记录；
3. 标记无法确认的 capability boundary 为 `UNKNOWN`，但不得自行发明其具体业务定义；
4. 为每项能力建立 evidence/reference 链。

如果 Owner-declared capability 记录不存在，ENGINEERING_TEAM 必须在 metadata 记录 `REQUIREMENT_DRIVEN_INCOMPLETE`，不得以工程推测填充 `DECLARED`。

### 3.2 Completeness Reviewer

`BUSINESS_OWNER` 负责：

1. 确认 Capability Set 的业务边界是否遗漏明显已知能力；
2. 对新增/补充的 `DECLARED` 能力提供可定位记录；
3. 对关键 `UNKNOWN` 边界作出确认、否定或保持未知的裁定。

BUSINESS_OWNER 的确认是 Capability Set 进入 Pre-Screen 的完成门槛，不等于确认任何 Identity。

### 3.3 Adversarial Reviewer

独立 Reviewer（例如 DeepSeek）负责检查：

- capability 是否由证据支持；
- `IMPLEMENTED` / `DECLARED` / `UNKNOWN` 是否误用；
- 是否存在把工程结构误当业务能力的情况；
- 是否有能力被单一案例或行业惯例过度泛化。

Reviewer 不代替 BUSINESS_OWNER 做业务确认。

## 4. Establishment Method

Capability Set 按以下顺序建立：

### Step 1 — Evidence Inventory

从当前 evidence package 中收集所有与已实现业务行为直接相关的证据来源，包括但不限于：

- current baseline / business-object map；
- engineering reality / code-path evidence；
- database reality / data evidence；
- 已确认的 decision / requirement records；
- 现有业务流程或接口证据。

工程团队必须记录每项能力的 `source_reference`。无法定位来源的能力不得标记为 `IMPLEMENTED`。

### Step 2 — Capability Extraction

从证据中提取“业务能力”，而不是表、API、Service 或技术组件名称。

能力描述应表达业务行为，例如“创建采购申请”“接收入库”“记录库存变动”，而不是“purchase_request 表存在”“某 Service 有某方法”。

技术证据只能作为能力存在的证据，不直接成为 Capability 名称。

能力证据的最低要求是：至少存在一条**业务行为路径**的证据，而不是仅仅存在表、字段、endpoint 或 Service。

“表 X 存在”不是能力证据；“系统可以通过某路径完成某业务动作”才可作为已实现能力的证据。字段或 endpoint 只能在能够与实际业务行为路径建立可定位联系时作为辅助证据。

### Step 3 — Deduplication and Boundary Check

合并仅在两个记录表达同一业务能力且没有不同生命周期、不同业务责任或不同业务结果时允许。

判断两条记录是否为同一业务能力，必须逐项检查：

1. 业务动作是否相同（动词 + 宾语）；
2. 触发条件是否相同；
3. 业务结果是否相同；
4. 责任人是否相同；
5. 生命周期是否相同。

五项全部相同才允许合并；任一项不同则保持分离，除非有更高层、可定位业务证据证明两者本质上是同一能力。

不得为了减少数量而合并不同能力，也不得因为不同表/endpoint 就自动拆成不同能力。

### Step 4 — Requirement Declaration Intake

从已有可定位的 BUSINESS_OWNER 记录中登记 `DECLARED` 能力。

如果 Owner 记录不可得，应明确记录：

`REQUIREMENT_DRIVEN_INCOMPLETE`

不得以“当前没有实现”替代 Owner 声明。

### Step 5 — Unknown Boundary Register

对证据无法确认、且可能影响 Identity Necessity 判断的能力边界，登记为 `UNKNOWN`。

UNKNOWN 的登记应描述“为什么当前无法确认”，而不是虚构一个完整能力定义。

### Step 6 — Completeness Review

BUSINESS_OWNER 必须逐项确认：

1. 当前 `IMPLEMENTED` 能力是否有明显遗漏；
2. 当前 `DECLARED` 能力是否均有真实 Owner 来源；
3. `UNKNOWN` 是否错误承载了本应确认的已知能力；
4. 是否存在会实质改变 Pre-Screen 结果的未确认能力边界。

## 5. Required Output

Capability Set 正式产出文件：

`03-review/d-ig-business-capability-set-001.yaml`

每项 capability 至少包含：

- `capability_id`；
- `capability_name`；
- `source_status` (`IMPLEMENTED` / `DECLARED` / `UNKNOWN`)；
- `source_reference`；
- `business_description`；
- `evidence_class`；
- `owner_review_status`；
- `production_dependency`；
- `notes`。

`owner_review_status` 取值：`PENDING` / `CONFIRMED` / `REJECTED` / `NOT_APPLICABLE`。

- `PENDING`：尚未完成 Owner review；
- `CONFIRMED`：BUSINESS_OWNER 已确认该能力记录；
- `REJECTED`：BUSINESS_OWNER 明确否定该记录；
- `NOT_APPLICABLE`：该能力来自已验证工程行为或当前阶段不要求 Owner 对其逐项作业务声明确认；但该取值不得掩盖来源缺失。

Capability Set metadata 必须同时记录：

- `requirement_driven_status`；
- `production_evidence_status`；
- `establishment_status`；
- `completeness_review_status`；
- `pre_screen_authorization`。

`pre_screen_authorization` 取值：`READY` / `BLOCKED` / `NOT_EVALUATED`。

- `NOT_EVALUATED`：Capability Set 建立中，尚未完成 Completion Gate；
- `BLOCKED`：Completion Gate 未通过；
- `READY`：Completion Gate 全部通过，可进入 Pre-Screen。

## 6. Completion Gate

Capability Set 只有同时满足以下条件，才可进入 Pre-Screen：

1. 所有 `IMPLEMENTED` 能力均有可定位 evidence reference；
2. 所有 `DECLARED` 能力均有可定位 BUSINESS_OWNER reference；
3. 没有无来源的 capability；
4. 明显重复能力已完成边界审查；
5. `UNKNOWN` 边界已显式登记，且未被当作排除依据；
6. BUSINESS_OWNER 已确认当前 Capability Set 的业务边界完整性；
7. 若 Requirement-driven sources 不完整，状态仍明确保持 `REQUIREMENT_DRIVEN_INCOMPLETE`；
8. 若生产证据仍不可得，生产依赖项必须保持 `BLOCKED/PROVISIONAL`，不得伪装为生产事实。

### 6.1 Meaning of “Completeness” in Condition 6

Condition 6 的“完整性”仅指：

1. 相对当前 evidence package：所有能定位到的已实现业务行为都已提取为 `IMPLEMENTED` 能力；
2. 相对 Owner 已记录材料：所有 Owner 已声明的业务能力都已登记为 `DECLARED`；
3. 不宣称对“业务世界所有可能的业务能力”完整。

BUSINESS_OWNER 的确认因此是“在已声明来源与当前 evidence package 边界内的完整性确认”，不是对现实业务世界全部能力的穷尽性确认。

完成后：

`pre_screen_authorization = READY`

否则：

`pre_screen_authorization = BLOCKED`

在 Completion Gate 尚未完成评估前：

`pre_screen_authorization = NOT_EVALUATED`

## 7. Relationship to Pre-Screen

Capability Set 是 Pre-Screen 的输入，不是 Pre-Screen 结果。

Pre-Screen 只能使用 `IMPLEMENTED` 与 `DECLARED` 作为“候选不需要进一步 Identity testing”的证据基础；`UNKNOWN` 只能产生 `PENDING_CAPABILITY_CONFIRMATION`，不得产生 `OBSERVED_ONLY`。

Capability Set 不得提前把某个候选定义为 Identity，也不得为了让 Pre-Screen 更容易通过而裁剪业务能力。

## 8. Governance State

本 Charter 建立后：

- D-IG = OPEN；
- Business Capability Set = METHOD_DEFINED;
- Capability Set v1 = NOT_YET_ESTABLISHED;
- BUSINESS_OWNER completeness review = PENDING;
- Pre-Screen Rule 001 = ESTABLISHED;
- Charter Amendment 001 = ACTIVE_REFERENCE;
- Pre-Screen execution = NOT_STARTED;
- E1–E4 = BLOCKED_PENDING_PRE_SCREEN;
- H1/H2 = NOT_MADE;
- Schema = BLOCKED;
- Migration = BLOCKED。

## 9. Freeze Boundary

本 Charter 的建立方法与执行契约完成后，除发现新的结构性矛盾或与 A1/A2、D-IG Charter 发生不可消解的接口冲突外，不再新增方法论层面的规则。后续工作直接进入 Capability Set v1 建立、BUSINESS_OWNER 完整性审核与 Pre-Screen 执行。
