# 35 个「代码常量有、权限目录无」的码：甄别表（2026-10-29）

**生成方式**：`scripts/qa/permission_constant_gate.py` 扫描 `*PERMISSIONS` 集合常量 → 与 `permissions` 目录比对 →
对每个缺失码用正则搜 `hasAuthority('码')` / `hasAnyAuthority(…码…)` / `@PreAuthorize(…码…)` 的**实际引用**。

**判定规则（写死，后续照此执行）**：

| 情况 | 处置 |
|------|------|
| 有 `hasAuthority` / `@PreAuthorize` 引用 | **登记进目录**（否则除超管外全 403） |
| 只在常量表/注释/历史清单出现，引用为 0 | **从代码删除**（先贴引用为 0 的 grep 证据） |
| 拿不准 | 单列「待产品确认」，**不登记不删除**，进 gate 白名单（带理由注释） |

## 实测结果：**35 个码，注解引用全部为 0**

```text
REFERENCED(登记): 0
NO-REF(删/待确认): 35
```

**为什么一律判「待产品确认」而不是直接删**：这 35 个码全部集中在三张**历史硬编码权限清单**里
（`AuthenticationServiceImpl:45-58`、`TokenServiceImpl:41-54`、`UserDetailsServiceImpl:54-66`），
看起来是早期「为用户一次性写入全量权限码」的遗留表。它们**可能**存在两种情况：
1. 对应功能**还没做**（未来要用这些码）→ 删了以后要重建；
2. 对应功能**已废弃**（码永远不会被引用）→ 该删。
而我在没有产品确认前**无法区分**，所以按规则落「待确认」，不擅自删。

## 甄别表

| 码 | 出现在哪些文件:行 | 是否被 hasAuthority/@PreAuthorize 实际引用 | 判定 |
|----|------------------|------------------------------------------|------|
| `finance:budget:approve` | AuthenticationServiceImpl:45-46 / TokenServiceImpl:41-42 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `finance:budget:delete` | AuthenticationServiceImpl:45-46 / TokenServiceImpl:41-42 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `finance:cost:approve` | AuthenticationServiceImpl:45-46 / TokenServiceImpl:41-42 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `finance:cost:delete` | AuthenticationServiceImpl:45-46 / TokenServiceImpl:41-42 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:contract:approve` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:contract:create` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:contract:delete` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:contract:edit` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:contract:view` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:material-request:approve` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:material-request:create` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:material-request:delete` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:material-request:edit` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:material-request:view` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:order:approve` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:order:delete` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:order:edit` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:plan:approve` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:plan:create` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:plan:delete` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:plan:edit` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:plan:view` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:settlement:approve` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:settlement:create` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:settlement:delete` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:settlement:edit` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:settlement:view` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:stockin:create` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:stockin:delete` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:stockin:edit` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:supplier:create` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:supplier:delete` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:supplier:edit` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `purchase:supplier:view` | UserDetailsServiceImpl:59-66 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |
| `trace:export` | AuthenticationServiceImpl:58 / TokenServiceImpl:54 / UserDetailsServiceImpl:54 | **0** | **待产品确认**（不登记不删除，进 gate 白名单） |

## 后续动作

1. 产品确认这批码对应的功能是「未做」还是「已废弃」；
2. 未做 → 写迁移登记进目录（像 `V20261029_005` 那样带断言）；
3. 已废弃 → 从三张常量表删除，并用 gate 验证缺失数下降；
4. 每完成一批，`scripts/qa/permission-constant-baseline.txt` 相应缩小（见下）。

## 补查（Lead 2026-10-29 要求）：三张常量表所在的类**本身是否还活着**

### 证据 1：谁注入/调用这三个类

| 类 | 注入/调用情况 | 结论 |
|----|--------------|------|
| `TokenServiceImpl` | `AuthServiceImpl:68 private final TokenService tokenService;` + 实际调用 `AuthServiceImpl:545 tokenService.refreshToken(token)`、`AuthenticationServiceImpl:103/180/247/252` | **活代码** |
| `UserDetailsServiceImpl` | `SecurityConfig:61 private final UserDetailsService userDetailsService;` + `SecurityConfig:205 authProvider.setUserDetailsService(...)` | **活代码（Spring Security 认证链）** |
| `AuthenticationServiceImpl` | 全仓搜 `AuthenticationService`（排除 Impl 自身）：**只有接口定义 `AuthenticationService.java:14` 和 Impl 的 import 第 8 行**，**无任何类注入它、无 Controller 使用** | **疑似死代码** |

### 证据 2：常量数组有没有读取点

```text
AuthenticationServiceImpl:29  private static final Set<String> ALL_PERMISSIONS = Set.of( ... 35 码里的一部分 ... )
AuthenticationServiceImpl:163 permissions.addAll(ALL_PERMISSIONS);    ← 有读取点，但整类无人调用 → 读了也没用

TokenServiceImpl:25          private static final Set<String> ALL_PERMISSIONS = Set.of( ... )
TokenServiceImpl:159         permissions.addAll(ALL_PERMISSIONS);    ← 有读取点，且类活着 → **这部分码会真的进 token**

UserDetailsServiceImpl:54-66 同类常量（无 ALL_PERMISSIONS 命名，直接写在装配逻辑里）→ 类活着（Spring Security 用）
```

### 判定（更新）

| 常量表 | 类是否活 | 处理建议 |
|--------|----------|----------|
| `AuthenticationServiceImpl:29-58`（ALL_PERMISSIONS，含 finance:*） | **疑似死**（无注入方） | **先确认再删**：需 ① 确认没有 `@Autowired AuthenticationService`（已确认无）② 确认没有反射/SPI 按名取 bean ③ 删除后跑全量测试。若确认，**整类可删**，其中的码随之消失 |
| `TokenServiceImpl:25-54`（ALL_PERMISSIONS，含 finance:*） | **活** | 这些码**真的会被写进 token** → 必须**登记进目录**，否则「token 里有权、注解里查不到」，属隐形授权 |
| `UserDetailsServiceImpl:54-66`（purchase:*） | **活** | 同上：登记进目录 |

**因此 35 个码不能一刀切**：
- `finance:*`（在 AuthenticationServiceImpl + TokenServiceImpl 两处都有）→ 因 TokenServiceImpl 活着，**至少这部分要登记**；
- `purchase:*` / `trace:export`（UserDetailsServiceImpl 活着）→ **要登记**；
- 只有当 `AuthenticationServiceImpl` 独有的码，才可能随整类删除而消失。

**下一步（需你批）**：
1. 我先做一次「`AuthenticationServiceImpl` 是否真死」的验证：启动日志看有没有它的 Bean、`/actuator/beans`（若开）或加一条临时日志；
2. 确认后：`finance:*` / `purchase:*` / `trace:export` 写一个迁移**批量登记**（带 DO 断言 35 码全在）；
3. 若 `AuthenticationServiceImpl` 确认死 → 单独一轮删整类（不在本批混做）。
