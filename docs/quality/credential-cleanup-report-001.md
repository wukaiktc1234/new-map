# Credential Cleanup Report 001

- **日期**: 2026-09-24
- **范围**: 凭据扫描三类遗留（e2e auth tokens / keystore.p12 / 文档明文口令）+ `.env.example` JWT_SECRET 核查
- **关联**: KL-077（不重写 git 历史）；前序 commit `883b639`（backend/login*.json）
- **约束遵守**: 未重写历史、未改业务代码、未动其他文件

---

## 1. 【优先 1】e2e/.auth 已跟踪 admin JWT — 已清理

### 1.1 Token 核查（每个 token 的 exp + 环境归属）

| 文件 | 类型 | iat (UTC) | exp (UTC) | 状态 | 环境归属 | 与 admin123 同源 |
|------|------|-----------|-----------|------|----------|------------------|
| `e2e/.auth/user.json` | access | 2026-08-02T03:46:32Z | 2026-08-02T05:46:32Z (TTL 2h) | **EXPIRED** | 本地 Playwright → `http://localhost:3002` | **是**（iss=`food-traceability-system`, user=admin, permissions=`["*"]`；登录时口令已不可考，但同链路 seed 曾为 admin123/现 Admin@123） |
| `e2e/.auth/user.json` | refresh | 2026-08-02T03:46:32Z | 2026-09-01T03:46:32Z (TTL 30d) | **EXPIRED** | 同上 | 同上 |
| `frontend/e2e/.auth/user.json` | access | 2026-07-14T19:43:34Z | 2026-07-14T21:43:34Z | **EXPIRED** | 本地 Playwright → `http://localhost:3002` | 是 |
| `frontend/e2e/.auth/user.json` | refresh | 2026-07-14T19:43:34Z | 2026-08-13T19:43:34Z | **EXPIRED** | 同上 | 是 |
| `frontend/e2e/.auth/deep-user.json` | access | 2026-07-15T11:36:40Z | 2026-07-15T13:36:40Z | **EXPIRED** | 本地 Playwright → `http://localhost:3002` | 是 |
| `frontend/e2e/.auth/deep-user.json` | refresh | 2026-07-15T11:36:40Z | 2026-08-14T11:36:40Z | **EXPIRED** | 同上 | 是 |
| `frontend/e2e/.auth/lifecycle-v2.json` | access | 2026-07-15T11:43:04Z | 2026-07-15T13:43:04Z | **EXPIRED** | 本地 Playwright → `http://localhost:3002` | 是 |
| `frontend/e2e/.auth/lifecycle-v2.json` | refresh | 2026-07-15T11:43:04Z | 2026-08-14T11:43:04Z | **EXPIRED** | 同上 | 是 |
| `frontend/e2e/.auth/lifecycle-v3.json` | access | 2026-07-15T11:48:45Z | 2026-07-15T13:48:45Z | **EXPIRED** | 本地 Playwright → `http://localhost:3002` | 是 |
| `frontend/e2e/.auth/lifecycle-v3.json` | refresh | 2026-07-15T11:48:45Z | 2026-08-14T11:48:45Z | **EXPIRED** | 同上 | 是 |

**结论**：
- 全部 10 个 JWT 均为 **admin（userId=1, permissions=`["*"]`）**、iss=`food-traceability-system`、origin=`http://localhost:3002`（本地管理端，非生产）
- 截至 2026-09-24 **全部 access + refresh 均已 EXPIRED**，不可直接重放
- 与已失效 `admin123` 同源链路：Playwright setup（`frontend/e2e/tests/auth.setup.ts` 等）本地登录签发；历史 seed 口令曾为 admin123，现行 DB 为 `Admin@123`——token 本身不携带口令，仅证明曾持有 admin 会话
- **风险残留（低）**：口令不泄露；但 token 结构/权限面/本地 URL 暴露，且若 JWT_SECRET 未轮换则伪造风险取决于密钥

### 1.2 Git 删除

```
git rm e2e/.auth/*.json frontend/e2e/.auth/*.json
→ rm 'e2e/.auth/user.json'
→ rm 'frontend/e2e/.auth/deep-user.json'
→ rm 'frontend/e2e/.auth/lifecycle-v2.json'
→ rm 'frontend/e2e/.auth/lifecycle-v3.json'
→ rm 'frontend/e2e/.auth/user.json'
```

### 1.3 .gitignore 追加

```
e2e/.auth/
frontend/e2e/.auth/
```
（`.gitignore:144-145`；`git check-ignore -v` 验证命中）

### 1.4 Commit

- **`fe3902f` `chore(security): remove tracked e2e auth tokens`**
- 6 files changed, +2 / −150（5 JSON 删除 + .gitignore 2 行）
- 钩子 auto-push：`883b639..fe3902f master -> master`（已推远程）
- `git ls-files -- e2e/.auth/ frontend/e2e/.auth/` → 空

### 1.5 影响

- Playwright `storageState: ".auth/user.json"` 引用仍在；下次 e2e 跑 `auth.setup` 会**本地重新生成**且不再入库——符合预期

---

## 2. 【优先 2】keystore.p12 核查（只读，未删未改）

| 项 | 结论 |
|----|------|
| **文件** | `backend/keystore.p12` 与 `backend/src/main/resources/keystore.p12` **字节级相同**（SHA256 均为 `E09E9123…4042BC`） |
| **类型** | **PKCS12**（keytool `-storetype PKCS12` 成功打开；非 JKS） |
| **口令** | `password`（`keytool -storepass password` 成功；`changeit`/`123456` 失败） |
| **条目** | alias=`tomcat`，PrivateKeyEntry ×1，证书链 ×1 |
| **证书** | CN=localhost, OU=IT, O=Example, L=Beijing, ST=Beijing, C=CN；RSA 2048；SHA384withRSA；notBefore=2025-12-21，**notAfter=2035-12-19**（自签，示例性质） |
| **用途** | **HTTPS/TLS 服务端密钥库**（`server.ssl.key-store`），**非 JWT 签名**（JWT 走 `JWT_SECRET` HS512） |
| **配置引用** | `application-prod.yml:183-187`：`ssl.enabled=${SSL_ENABLED:false}`、`key-store=${SSL_KEYSTORE_PATH:}`（**默认空，未指向本文件**）、`key-store-password=${SSL_KEYSTORE_PASSWORD:}`、`key-store-type=PKCS12`；`config/application-prod.yml:34-39` 同构；默认 `application.yml:298-299` `ssl.enabled: false` |
| **口令是否与文件同仓** | **是（同仓不同文件）**：`backend/.env`（gitignored，未 tracked）含 `SSL_KEYSTORE_PASSWORD=password`、`SSL_KEY_PASSWORD=password`；`backend/.env.example` 仅有空占位 `SSL_KEYSTORE_PASSWORD=`。**keystore 本体 tracked 入库，口令在未入库的 `.env`** —— 但口令值 `password` 极弱且示例证书 CN=localhost |
| **代码引用** | `HttpsConfig.java` 仅 HTTP→HTTPS 重定向骨架且**整段注释禁用**；无 `classpath:keystore.p12` 硬编码；生产靠环境变量注入 |
| **影响范围** | ① 本地/dev SSL 默认关闭（`SSL_ENABLED:false`）→ **当前运行面为 0**；② 若生产开启 SSL 且把 `SSL_KEYSTORE_PATH` 指向仓内此文件，则等于用**公开仓库中的弱口令示例私钥**终止 TLS → 中风险；③ 私钥可被任何 clone 者提取（口令 `password` 可穷举）；④ **非 JWT 密钥，不影响 token 签名** |
| **处置** | **不删除、不改动，等 Owner 裁定** |

**Owner 裁定选项（供决策）**：
- A. 从 git 移除 `backend/keystore.p12` + `backend/src/main/resources/keystore.p12`，`.gitignore` 加 `*.p12`，生产用外部挂载
- B. 保留但强制生产禁用仓内路径（维持 `SSL_KEYSTORE_PATH` 默认空 + 审计）
- C. 轮换：生成新 keystore + 强口令，旧文件作废

---

## 3. 【优先 3】文档/脚本明文口令 `admin123` → `<redacted>`

**范围**：仅替换字面量 `admin123`（不改 `Admin@123`——那是当前 seed 口令的另一形态，业务/文档广泛引用，本卡不动）；不改业务代码。

### 3.1 已改文件清单（commit 2）

| # | 文件 | 改动点 |
|---|------|--------|
| 1 | `README.md` | L26 默认账号口令 |
| 2 | `backend/e2e_security_test.ps1` | L11 登录 JSON body |
| 3 | `.harness/HANDOFF.md` | L67 登录账号表 |
| 4 | `.harness/skills/post-work-checklist.md` | L28 checklist |
| 5 | `.harness/skills/fix-login-issue.md` | L13 curl body、L73 PS body |
| 6 | `docs/audit/PENDING_ISSUES_BACKLOG.md` | L1141/1253/1262/1352 测试用户口令 |
| 7 | `docs/frontend-verification-plan.md` | L1224 重置密码记录 |
| 8 | `docs/guides/QUICKSTART-H2-GUIDE.md` | L129/333/447/499 登录凭证与 checklist |
| 9 | `docs/test/安全测试方案.md` | L114 弱口令测试步骤中的 admin123 示例 |
| 10 | `documents/采购与仓储模块-测试报告.md` | L5 测试账号 |

**未改（有意保留）**：
- `launcher.bat`：仅含 `Admin@123`（无 `admin123`）
- `data.sql` / `init-data.sql` / Flyway：仅含 `Admin@123` 注释（无 `admin123`）
- `docs/quality/*` QA 报告：命中均为 `Admin@123`，本卡只清 `admin123`
- `e2e/**`：无 `admin123`
- **业务代码**（禁改）：`BCryptGenerator.java`、`SecurityStartupChecker.java`（弱口令黑名单数组）、`JasyptEncryptorUtilTest.java`、`frontend/.../LoginPage.vue` 自动登录
- `production-known-limitations.md` KL-077 历史登记中的 `admin123` 字面量（限制清单「只增不改」+ provenance 保留）

### 3.2 Commit

- **`chore(security): redact admin123 plaintext in docs and scripts`**（10 文件）

---

## 4. 【优先 4】`.env.example` JWT_SECRET — **SAFE**

| 文件 | 值 | 判定 |
|------|-----|------|
| `.env.example:22` | `my-food-traceability-jwt-secret-key-2025-is-at-least-64-bytes-long-for-hs512-algorithm-secure-and-must-be-exactly-512-bits-or-more` | **SAFE** — 语义化示例串（自描述 “must-be-exactly…”），非随机真密钥；文件名即 example 模板 |
| `backend/.env.example:48` | `change-me-to-a-random-string-at-least-64-bytes-long-for-hs512-algorithm` | **SAFE** — `change-me-to-…` 占位 |
| `config/.env.example:23` | `your-super-secret-jwt-key-change-this-in-production` | **SAFE** — `your-…change-this` 占位 |
| `config/.env.production.template:21` | `<GENERATE_SECRET_64_CHARS>` | **SAFE** — 明确占位符 |

**结论：四处处 tracked 示例均为占位/示例值，非真实 secret → SAFE，无需 Owner 裁定。**  
（真实运行口令在 gitignored `.env` / `backend/.env` / 环境变量，不在本卡清理范围。）

---

## 5. 汇总

| 优先级 | 动作 | 结果 |
|--------|------|------|
| 1 e2e tokens | 核查 + git rm + gitignore + commit | **DONE** `fe3902f`（已 auto-push）；10 JWT 全 EXPIRED、全本地 3002、全 admin |
| 2 keystore.p12 | 只读核查 | **PENDING_OWNER**；PKCS12、口令=`password`、用途=HTTPS、口令在 gitignored `backend/.env`、默认 SSL 关闭 |
| 3 admin123 明文 | 文档/脚本 redact + commit | **DONE** 10 文件；业务代码与 Admin@123 未动 |
| 4 JWT_SECRET 示例 | 判定 | **SAFE** |
| 禁止项 | 不重写历史 / 不改业务代码 / 不动其他 | **遵守**（仅 2 个 chore commit + 本报告） |

**遗留（非本卡）**：
- 历史 `dd33ee3` 明文仍在 remote（KL-077，不重写）
- `Admin@123` 在大量 QA/验收文档中明文（低风险，测试账号；若要清另立卡）
- keystore.p12 是否出库等 Owner
- 业务代码内 `admin123` 引用（`BCryptGenerator`/`LoginPage.vue` 等）需产品/开发排期，本卡禁止触碰
