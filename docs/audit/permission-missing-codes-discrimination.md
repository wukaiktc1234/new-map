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
