# bug-㉑ 大权限账号 token 超头（P1）—— 量化与重构方案

日期：2026-10-28｜Lead 已批准重构（1.5–2.0 人日）

## 1. 现状量化（改造前）

| 账号 | 角色 | 角色权限条数 | token 字节 | Tomcat 8KB 上限 |
|------|------|--------------|-----------|------------------|
| `qa_teller` | （收银/低权限） | — | 836 | ✔ |
| `vcashier01` | ROLE_CASHIER | 少量 | 1295 | ✔ |
| `qa_store_b` | ROLE_STORE_MANAGER | **120** | 4011 | ✔ 但已过半 |
| `qa_admin` | ROLE_FINANCE_DIRECTOR | **110** | 4218 | ✔ 但已过半 |
| （临时绑）`qa_admin` + ROLE_SUPER_ADMIN | ADD 418 | **418** | **13892** | ✘ **400 Bad Request** |

角色权限条数（`role_permissions` 实测 Top 8）：
```
ROLE_ADMIN 418 ｜ ROLE_OWNER 418 ｜ ROLE_SUPER_ADMIN 418
ROLE_STORE_MANAGER 120 ｜ ROLE_FINANCE_DIRECTOR 110 ｜ ROLE_HR_DIRECTOR 51
ROLE_PURCHASE_MANAGER 39 ｜ ROLE_FINANCE_MANAGER 36
```

**结论**：token 长度 ≈ `权限条数 × 33 字节`。
→ **418 条 = 13.8KB（炸）**；**120 条 = 4KB（正好是目标红线）**。
即：只要某个角色权限超过约 240 条，或用户叠加两个高权限角色，就一定超 8KB。**这不是极端场景**。

## 2. 根因（代码位置）

```
AuthServiceImpl:836   securityUser.setPermissions(new ArrayList<>(permissions))   ← 全部权限码放进 SecurityUser
SecurityUser:79-90    每个 permission → 一条 SimpleGrantedAuthority
JwtAuthenticationFilter / token 签发：authorities 随 SecurityUser 序列化进 token
```
代码里已有一次「认识到」但只堵了一半：
`AuthServiceImpl:826-833` 对 admin 只放 `'*'`（注释写明「150+ 权限码会让 token 超 9KB」），
**但只对 admin 生效** —— `ROLE_SUPER_ADMIN` / 任何绑了大量权限的自定义角色仍全量塞。

## 3. 重构方案

```
JWT payload 只保留：userId + roleIds[] + 少量标量（username、tenantId、storeId、tokenVersion）
权限：请求进入时按 roleIds 查 permission（Caffeine 缓存，key=roleIds 排序串，TTL 30–60s）
```

**灰度兼容**：签发新 token 时带上 `ver:2`；过滤器遇到 `ver` 缺失（老 token）走旧路径（读 token 里的 authorities），
至少保留一个发布版本，之后再删旧路径。

**预期长度**（国内典型 JWT）：userId(8) + roleIds(2–3 个 × 24 字符) + 标量 ≈ **400–600 字节**，远低于 4KB 目标。

## 4. 验收标准

1. 改造后 5 个角色 token **全部 < 4KB**（贴表对比）；
2. 老 token 仍可解析（灰度期），新老混合请求都 200；
3. 5 个角色账号各打 3 个接口 → 全 200；
4. 权限变更后 ≤60s 生效（缓存 TTL），并有一条「权限缓存刷新」日志。

## 5. 风险

- 权限解析从「读 token」变「查库+缓存」→ 首次请求延迟 +5–15ms（可接受），**缓存击穿**需加 single-flight；
- 改错会导致全员 403 → 必须灰度 + 保留旧路径；
- 缓存与 DB 不一致窗口 30–60s：**撤权延迟**，安全敏感操作（财务/审批）建议强制直查。
