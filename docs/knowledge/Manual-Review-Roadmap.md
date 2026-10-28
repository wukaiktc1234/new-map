# 人工审查路线图（Manual Review Roadmap）

> 目的：给人类审查者一份**按优先级排序、可逐条复跑**的审查清单。
> 原则：**不信任任何结论，只信任你自己跑出来的输出。** 本文每条都给出「命令 + 期望输出 + 不符时怎么办」。
> 时间预算：完整审查约 90–120 分钟；若只有 30 分钟，只看 §2 与 §6。

---

## 0. 审查心法（先读这一节）

本阶段我一共给出过 **9 次被实测推翻的结论**（含我自己 4 次错、下游 5 次错）。所以审查的重点不是「找有没有 bug」，而是**抽查「结论是否有可复现证据」这个习惯有没有真的落地**。

三条判据：
1. **每条结论必须能追到「代码行」或「SQL 实测」**——只有文档描述没有证据的，一律降级为「未验证」；
2. **gate 是否真的会红**——任何 gate 都应能举出一个「它抓到问题」的实例；只见过它绿的 gate 等于没有；
3. **数据是否被不必要地改动**——测试造的数据应打标保留或冲正，不应删除。

---

## 1. 第一关：知识文档是否可信（10 分钟）

| 步骤 | 命令 | 期望 | 不符时 |
|---|---|---|---|
| 1.1 四份文档存在且非空壳 | `ls -l docs/knowledge/` | 4 个文件，各 > 8KB | 缺文件 = 知识沉淀未完成 |
| 1.2 README 挂了链接 | `grep -A6 知识资产 README.md` | 表格 4 行 + 三条铁律 | 链接缺失 = 交接入口断了 |
| 1.3 文档里的数字与代码一致 | 见 §3 的逐项核对 | 完全一致 | 文档数字过期 = 后续决策会基于错数字 |

**重点**：`System-Map.md` 的 §7「边界声明」必须存在——它列出了**未验证内容**（例如支付→扣库存顺序未插桩）。若没有这一节，说明地图在冒充圣经。

---

## 2. 第二关：gate 是否真的会红（30 分钟，最高优先级）

**这是整份路线图最重要的一节。** 一个只会绿的 gate 比没有 gate 更危险——它给人虚假安全感。

| 步骤 | 命令 | 期望 | 说明 |
|---|---|---|---|
| 2.1 全 gate 基线 | `python scripts/qa/preflight.py` | `PASS≥27 skipped≤3 FAIL=0` → `EXIT=0` | 记录三数字 |
| 2.2 审计工具自测 | `python scripts/qa/entity_column_audit.py --self-test` | `SELF-TEST PASS` | **同时打印「正确判据」与「旧错判据」结果** |
| 2.3 权限矩阵自测 | `python scripts/qa/permission_matrix_diff.py --self-test` | `stats={exact:1, compat:1, unresolved:1}` | 三路判定都要有 |
| 2.4 迁移版本 gate 自测 | `python scripts/qa/migration_version_gate.py --self-test` | 3 条全 PASS（冲突对/合法命名/_1 非三位） | 冲突对能被抓到是关键 |
| 2.5 新表体检自测 | `python scripts/qa/table_health_check.py --self-test` | 五项故意缺陷全部被抓 | 缺 deleted/缺索引/numeric 金额 |
| 2.6 **亲手制造一次红** | 临时把 `scripts/qa/runtime-exception-baseline.txt` 改成 482，再跑 `preflight.py` | `[BLOCK]` + `EXIT=2` | **改完必须改回 483**；不改回会污染基线 |

**如果 2.6 跑不出红，说明棘轮是摆设**——这是本阶段最需要人工确认的一条。

---

## 3. 第三关：核心数字与数据状态（20 分钟）

全部是可独立复跑的命令，**不要相信我的报告，直接看输出**。

### 3.1 数据库状态
```sql
-- 授权：期望 1734 条有效
SELECT count(*) FROM role_permissions WHERE deleted = 0;
-- 权限形态：期望 0 行（A44 口径）
SELECT count(*) FROM role_permissions rp LEFT JOIN permissions p
  ON p.permission_id_str = rp.permission_id
 WHERE p.permission_id_str IS NULL AND rp.deleted = 0;
-- 库存余额：期望合计 718.7000
SELECT sum(quantity) FROM inventories;
-- 库存流水：期望 159 行（冲正后只增不减）
SELECT count(*) FROM inventory_movement;
-- 四张夹具缴款单：每行 rcf 应等于其归属金额之和
SELECT s.slip_id, s.refund_component_fen FROM deposit_slips s WHERE s.slip_id IN (386,387,388,389);
```

### 3.2 部署缺口与迁移
```bash
python scripts/qa/deploy_gap_audit.py        # 期望：剩余 16 张（基线 33，已还 17）
ls backend/src/main/resources/db/migration/V*.sql | wc -l    # 期望：258
python scripts/qa/fen_shadow_check.py       # 期望：12 列全 NULL，EXIT=0
```

### 3.3 测试基线
```bash
cd backend && mvn -o -B clean test-compile      # 期望 BUILD SUCCESS
mvn -o -B test 2>&1 | tail -3                  # 期望 766 / 11F / 5E / 5S
```
**注意**：11F + 5E 是**已知历史欠账**（明细见 `Gate-Baseline-and-Health.md` §2）。**判断标准是「失败类名集合有没有变」，不是数字有没有变**——因为 `SystemSettings*` 依赖 Flyway 全链，Errors 会在 5~39 之间跳。

---

## 4. 第四关：核心改动逐项复核（30 分钟）

按「改动影响面 × 出错后果」排序，逐项给验证方法。

### 4.1 JWT 重构（影响全站鉴权，若错则全员 403）
| 验证点 | 命令/动作 | 期望 |
|---|---|---|
| token 变小 | 登录 4 个账号，解出 JWT 长度 | 498–530 字节（原 836–13892） |
| 不含权限清单 | 解 JWT payload | **无 `permissions` 字段**，只有 userId/roles/locationId/mfa |
| 撤权生效快 | 改一条 `role_permissions` 后立即调接口 | ≤5 秒内 403（实测 1.1 秒） |
| 请求路径不查 DB | 读 `UserPermissionCache.get()` | 只读本地 map；版本查询只在 `@Scheduled` 里 |

### 4.2 退款归属（影响账，若错则金额静默错误）
| 验证点 | 动作 | 期望 |
|---|---|---|
| 边界退款不计入 | 造一笔距交班 ±5 分钟内的退款，generate 缴款单 | `rcf=0` + 审计 `RECON_SHIFT_BOUNDARY_PENDING` |
| 人工归属生效 | `POST /deposit-slips/{id}/confirm-attribution` | 目标班次 `rcf` 增加 |
| **幂等** | 同 refund 同 shift 再提交一次 | **400 + 「该退款已完成归属确认，不可重复操作」** |
| **改归属不累加** | 同 refund 换 shift 提交两次 | `rcf` 仍 == 单笔金额（累加会翻倍） |
| 变更链可查 | 查 `finance_audit_logs` | 每次 old→new 一条 |

### 4.3 并发下单幂等（影响订单，若错则 500 或双写）
| 验证点 | 动作 | 期望 |
|---|---|---|
| 并发同 key | 2 线程 × 10 轮同 key 下单 | 每轮**恰好 1 行** `orders`，另一侧 200 + `reused=true`，**0 次 500** |
| **子表不多写** | 同上，查 `order_items` | 增量恒为 1（不能出现主表 1 行、子表 2 份） |
| 并发不同 key | 10 个不同 key 并发 | 全部写入，`reused` 全 0 |

### 4.4 权限改造（影响谁能用，若错则某角色静默失去权限）
| 验证点 | 动作 | 期望 |
|---|---|---|
| 三角验证 | 对 `/v1/orders/page` 分别用 admin / 有码角色 / 无码角色 | 200 / 200 / 403 |
| 矩阵逐格 | 改前改后快照 diff | `新增 N` 与 `移除 N` **一一成对**，无端点丢具体码 |
| star 计数 | `grep -c hasAuthority('*')` | 423（534 − 111） |
| 2 处 star-only 未误删 | 检查两个文件 | `UserPermissionOverrideController:221`、`HrMigrationController:33` 仍在 |

---

## 5. 第五关：数据改动是否有痕迹（10 分钟）

**重点：确认「测试造的数据没有被删除」。**

```sql
-- 夹具是否打标保留（不应被删）
SELECT slip_id, remark FROM deposit_slips WHERE slip_id IN (386,387,388,389);
-- 期望 remark 含 [E2E 夹具][勿删]

-- 授权行是否被物理删除过（应只有软删）
SELECT count(*) FROM role_permissions WHERE deleted = 1;   -- 允许 >0（软删）
-- 关键：有效的 1734 条是否都是「有业务意义」的，而不是补回来的空壳

-- 库存冲正记录是否留痕
SELECT count(*) FROM inventory_movement WHERE remark LIKE '[E2E:revert]%';   -- 期望 15
```

**审查判据**：如果发现「数据被 UPDATE 手改过」而非走产品 API / 重算逻辑，就把该改动降级为高风险——本阶段曾两次因手写 `DELETE` 误删他人数据。

---

## 6. 第六关：我最可能错的地方（诚实清单，15 分钟）

**如果时间有限，只看这一节。** 以下是我自己判断可信度最低的几条，请优先抽查：

| # | 我的结论 | 为什么可能错 | 怎么验 |
|---|---|---|---|
| 1 | `myself: 历史账绝不回溯` | 这是**业务决策**，不是技术结论；若有历史数据本来就错，不修会一直错 | 请财务抽查 3 张历史缴款单，人工重算比对 |
| 2 | `orders` 元/分混用「未出现问题」 | 我只做了静态清单（`money-unit-boundary.md`），**没有逐条运行时验证 5 条混用点** | 抽 1 条最危险的：缴款单净额是否取了 `orders.order_amount` |
| 3 | 那 2 处 `hasAuthority('*')` 保持不动 | 属「仅 admin 可用」，但**没有人验证过 admin 真的能进**（可能因目录缺码而 403） | 用 admin 调 `UserPermissionOverrideController:221` 对应接口 |
| 4 | 45 张缺表「约 28 张待方案」 | 是**三档粗分，未逐张裁定**（我在文档里注明了，但它仍是不完整结论） | 任取 3 张，核对写入点与前端调用 |
| 5 | `e2e 快速 52/2 属正常` | 这个「正常」是**从一次运行归纳的**，不是设计保证 | 连跑 3 次，确认稳定 52/2 |
| 6 | 权限 A46「317 码缺失 0」 | 只扫字面量，**不执行 `@PreAuthorize` 表达式**；动态拼接的码扫不到 | 若有 `@PreAuthorize` 拼接变量，逐个看 |
| 7 | 「测试造数据已冲正、无残留」 | 我核对过 `inventory` 与 `movement`，但**没有逐表核对所有被 e2e 碰过的表** | 按 tag 搜残留（`T-E2E`、`E2E-` 前缀） |

---

## 7. 最终结论怎么看（5 分钟）

### 可以放心的部分（有可复现证据）
- 从零建库可重放（Flyway `executed` 全绿，`failed=0`）；
- 并发下单不双写、不 500（10 轮逐轮 Δ 表）；
- JWT 变短且撤权即时（实测 1.1 秒）；
- 迁移版本冲突、权限形态错、实体列不一致三类问题**有 gate 能抓**（各有自测）。

### 需要人工继续判断的部分
- **45 张缺表的产品归属**（我做的是技术分类，不是产品决策）；
- **总账口径**（期初/期末/科目余额，无产品定义时我拒绝实现）；
- **423 处 `'*'` 的业务权限矩阵**（谁能看仓库、谁能改价格，需产品定）；
- **历史数据是否真的对**（口径变更不回溯，意味着历史误差被永久保留）。

### 一句话
> 这套东西能证明的是「**没有变差、且变差会被拦**」；
> 它不能证明「**当前是对的**」——那需要业务方按 §6 的清单自己看一遍。
