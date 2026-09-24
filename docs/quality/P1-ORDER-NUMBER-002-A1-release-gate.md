# Release Gate 报告 — P1-ORDER-NUMBER-002 / A1（订单号唯一性专项）

- **角色**：regression（回归测试 / 发布门禁 Agent，会话6）
- **日期**：2026-09-23
- **批次**：Batch ORDER-A1（P1-ORDER-NUMBER-002 / A1，发布批次）
- **执行方式**：QA 已验收结论复核 + 回归独立抽验复跑（单测 / 活体 API / psql / git diff）
- **环境**：backend `http://127.0.0.1:8081/api`（health UP）；DB `food_traceability`（psql 18）；登录 admin/Admin@123 code=0
- **权限声明**：仅写 `production-regression-test.md` 与本报告，未修改任何业务/测试代码
- **QA 输入**：`docs/quality/P1-ORDER-NUMBER-002-A1-qa-report.md`（PASS_WITH_LIMITATION，2026-09-23）
- **Scope**：`docs/architecture/03-review/p1-order-number-scope-002.md`（FROZEN）

---

## 〇、执行摘要

| 项目 | 结果 |
|---|---|
| 回归基线 | **121 条**（114 + REG-ORDER-001~007 七条新增） |
| 本批回归执行 | REG-ORDER-001~007：**PASS 6 / PASS_WITH_LIMITATION 1 / FAIL 0** |
| QA 验收 | PASS_WITH_LIMITATION（H-01~H-06 6/6 PASS；FAIL=0；无 -R） |
| 回归独立抽验 | 单测 3/3；H-01~H-06 全 PASS（含 H-06 托盘全链 material_consumed=1） |
| 既有失败 | 仅范围外既有失败（OBS-02），A1 未引入；CC-6 33/33 保持 |
| **Regression Result** | **PASS** |
| **FAIL 数** | **0** |
| **是否允许放行** | **是（本地放行）**；生产仍 `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`（L-01） |

```text
Regression Result: PASS
FAIL: 0
Release: ALLOW_LOCAL（生产 PROVISIONAL — L-01 生产不可访问，待生产证据补验）
```

---

## 一、本批基线回归结果（REG-ORDER-001~007）

| 编号 | 状态 | 执行方式 | 关键证据 |
|---|---|---|---|
| REG-ORDER-001 | PASS | 活体+DB | `ORD202609231729170001`；psql eq=t；全表 total=36 null_on=0 distinct_on=36 |
| REG-ORDER-002 | PASS | 活体+DB | `T20260923004`；order_code=order_number=T* |
| REG-ORDER-003 | PASS | 活体+DB | `ORD202609231729170002`；eq=t |
| REG-ORDER-004 | PASS | DB | INSERT 重复 → `orders_order_number_key` EXIT=1 |
| REG-ORDER-005 | PASS | 活体 | KDS list 命中 T 码 + KO* pending |
| REG-ORDER-006 | PASS_WITH_LIMITATION | 活体+DB | 全链 serve code=0；material_consumed=1；log 8.800→8.700；限制=L-03（非 A1） |
| REG-ORDER-007 | PASS | 代码+单测 | diff 2 文件 +206；resources 零改动；单测 3/3；git 三链一致 |
| **FAIL** | **0** | | |

---

## 二、QA PASS_WITH_LIMITATION 限制项逐条判定（L-01~L-05）

| 限制 | 内容 | 是否影响本地放行 | 判定 |
|---|---|---|---|
| **L-01** | 生产不可访问，`PROVISIONAL_PENDING_PRODUCTION_EVIDENCE` | 否（本地证据充分） | **非阻断**；Gate 标注「本地放行 / 生产 PROVISIONAL」，生产上线前须补生产证据 |
| **L-02** | CC-7 回滚窗口无 `POST /v1/orders` API 探针 | 否 | **非阻断**；回滚窗口已由 git 三提交链 + 证据文件时间戳核验，不放大回滚风险 |
| **L-03** | POS 数字 id → food_id 默认 null（既有） | 否 | **非阻断 GATE**；NOT_CAUSED_BY_A1；foodCode 路径 H-06 本批独立 PASS；已登记 P1-POS-FOODID-MAP-001（planner 池） |
| **L-04** | Scope-002/实施记录 untracked | 否 | **非阻断**；FROZEN 文档不可 git 化，内容核对与 A1 实施一致 |
| **L-05** | OrderVO 无 orderNumber；scan-serve materialConsumed 短路 | 否 | **非阻断**；以 DB SQL/`material_consumed=1` 为准，展示层问题登记 |

**结论**：5 项 PWL 均不阻断本地放行；无任何限制项升级为 FAIL。

---

## 三、非 A1 引入的既有失败（OBS-02）

| 现象 | 归因 | 与 A1 关系 |
|---|---|---|
| `DeductMaterialsForServeConcurrencyTest` 2 errors | 既有并发/测试库问题 | 非 A1；CC-6 范围外套件；A1 diff 未触碰扣料核心 |
| `OrderManagementIntegrationTest` 等既有 errors | 既有 context/测试库/Flyway | 早于 A1；resources 零改动 |
| 工作区 `OrderNewServiceImpl.java` 相对 HEAD 大量未提交改动 | P0-KDS-DEDUCT / W1-EC-04C 等既有多批次 | 非 A1 引入（A1 提交仅 +1 行业务赋值） |

A1 有效证据：CC-6 33/33 PASS；A1 单测 3/3 PASS；`git diff 7f40ab7^..0acf99b` 仅 2 文件。

---

## 四、Release Gate

```text
Regression Result: PASS
FAIL: 0
允许放行: 是（本地）
生产状态: PROVISIONAL_PENDING_PRODUCTION_EVIDENCE（L-01）
基线: 114 → 121（REG-ORDER-001~007）
QA: PASS_WITH_LIMITATION（H 6/6、FAIL 0、无 -R）
PWL 阻断项: 0 / 5
```

**关键结论**：
1. 本批 7 条回归全部无 FAIL（6 PASS + 1 PWL），**FAIL=0 → 允许本地放行**。
2. H-06 托盘出餐链由回归角色独立复跑 **零数据补丁 PASS**（material_consumed=1，库存真实扣减），非仅采信 QA。
3. L-01~L-05 逐条确认均不阻断本地放行；生产放行仍待生产证据（L-01）。
4. 既有失败（OBS-02）与工作区未提交改动均 **NOT_CAUSED_BY_A1**（diff 仅 2 文件、CC-6 33/33、resources 零改动）。
5. 回归库已更新：`production-regression-test.md` 新增 REG-ORDER-001~007 + Batch ORDER-A1 门禁，正式基线 **121 条**。