# Production Data Snapshot（生产数据快照框架）

> - 文档编号：W1-PROD-SNAPSHOT-001
> - 日期：2026-09-09
> - 角色：架构总控（会话1）
> - **约束**：Agent 不得在开发环境伪造"生产结果"；实际结果由 DBA/运维在生产 DB 执行后回填。

---

## 一、开发/测试环境事实（代码层可确定）

| 维度 | 开发环境事实 | 生产环境判定 |
|---|---|---|
| orders count | 1（测试数据） | ⏸ 待执行 |
| orders_legacy count | **0** | ⏸ 待执行 |
| finance_records count | 0 | ⏸ 待执行 |
| Active legacy writer（代码层） | 0（OrderTimeoutTask 已迁移，PosOrderCreateServiceImpl line 705=DEAD CODE） | ⏸ 待生产确认 |
| Active legacy reader（代码层） | 1（OrderNewServiceImpl 已迁移=0；SalesAnalysis=POST-FREEZE） | ⏸ 待生产确认 |
| Hidden fallback（代码层） | 0（全仓 grep 确认） | ⏸ 待生产确认 |
| Finance order_code | EC-05 实施（order_code 字段+3 处 insert 统一） | ⏸ 待生产确认 |

> ⚠️ **开发环境 orders_legacy=0 不能代表生产环境**。生产扫描必须由 DBA 执行。

---

## 二、生产扫描执行状态

| 脚本 | 状态 | 执行者 | 结果位置 |
|---|---|---|---|
| 01-orders-production-scan.sql | ⏸ 待执行 | DBA/运维 | 待回填 |
| 02-legacy-consumer-classification.sql | ⏸ 待执行 | DBA/运维 | 待回填 |
| 03-finance-production-scan.sql | ⏸ 待执行 | DBA/运维 | 待回填 |
| 04-amount-unit-sampling.sql | ⏸ 待执行 | DBA/运维 | 待回填 |
| 05-legacy-consumer-production-confirm.sql | ⏸ 待执行 | DBA/运维 | 待回填 |

---

## 三、生产结果判定框架

### A. orders_legacy > 0

| 情况 | 判定 | 处置 |
|---|---|---|
| 全为历史合法数据，无活跃消费者 | D. POST-FREEZE CLEANUP | 保留历史数据，不冻结写入 |
| 有活跃消费者 | 需进一步分类 | 按消费者类型判定 |
| 存在不可转换记录 | 建立转换规则或豁免 | 产品决策 |
| 与 orders 有重复 order identity | **A. BLOCKER** | 必须解决重复 |

### B. orders_legacy active writer > 0

| 情况 | 判定 |
|---|---|
| 代码层确认 writer=0，生产层>0 | **A. BLOCKER**（定位来源） |
| 代码层 writer=0，生产层=0 | PASS |

### C. critical legacy reader > 0

| 情况 | 判定 |
|---|---|
| 代码层确认 reader=0（EC-04C 迁移后），生产层>0 | **B. PRE-GRAY REQUIRED**（定位来源） |
| 代码层 reader=0，生产层=0 | PASS |

### D. finance_record order_code unmatched > 0

| 情况 | 判定 |
|---|---|
| 全为历史数据（order_code=NULL，迁移前创建） | D. POST-FREEZE（数据迁移后可解决） |
| 有新增 unmatched（迁移后创建但无 order_code） | **A. BLOCKER**（EC-05 写入路径问题） |
| PO 编号被误识别 | **B. PRE-GRAY REQUIRED**（需确认识别逻辑） |

### E. Amount Unit 不一致

| 情况 | 判定 |
|---|---|
| orders/payment/refund 全为分 | PASS |
| 存在 yuan 混入 | **A. BLOCKER**（数据完整性） |
| finance_record 元（已知遗留） | D. POST-FREEZE（Batch0-方案2 批 C） |

---

## 四、Gray Readiness Decision 矩阵

| 条件 | PASS 标准 | 当前状态 |
|---|---|---|
| Production Data Scan | 全部脚本执行完成，无 A/B 级阻断 | ⏸ 待执行 |
| Production critical legacy reader = 0 | 生产环境无活跃 legacy 读取 | ⏸ 待确认 |
| Production active legacy writer = 0 | 生产环境无活跃 legacy 写入 | ⏸ 待确认 |
| orders_legacy 历史数据有明确处置方案 | 转换/保留/豁免已明确 | ⏸ 待确认 |
| finance_record order_code 关联通过 | unmatched=0 或全部有解释 | ⏸ 待确认 |
| Amount Unit PASS | 无 yuan/fen 混用 | ⏸ 待确认 |
| rollback plan PASS | 回滚方案可执行 | ✅ |
| monitoring PASS | 监控指标已定义 | ✅ |
| EC-04B-2 limitation 风险可接受 | 中等风险可接受 | ✅ |
| Product Owner approval | 最终确认 | ⏸ |

**Gray Release 启动条件 = 全部 PASS**。

---

## 五、执行说明

### 给 DBA/运维

1. 在生产 PostgreSQL 数据库上按 `README.md` 顺序执行 5 个 SQL 脚本
2. 所有脚本均为 **SELECT only**（只读，不修改任何数据）
3. 保存原始输出（文本文件）
4. 将结果回填到本文档的「二、生产扫描执行状态」表格
5. 执行完成后通知架构总控

### 给 QA

1. 收到 DBA 原始输出后，按「三、判定框架」逐项分类
2. 每项给出：Raw Result → Interpretation → Risk Classification → PASS/FAIL
3. 生成最终 Gray Readiness Decision

---

*本 Production Data Snapshot 为生产扫描框架，等待 DBA/运维执行生产扫描。*
*文档生成：架构总控（会话1）· 2026-09-09*
