# ADR 与防雷指南

> 面向：接手本项目的人或 AI 会话。读完这一份，能避开本阶段踩过的全部重大坑。
> 来源：本阶段 10 个 bug、9 次「结论被实测推翻」、15 条工程教训。

---

## 一、财务红线（违反会直接造成账务错误，且不报错）

### R1 历史数据绝不回溯重算
- **规则**：任何口径变更（如退款边界从 10 分钟改为 ±5 分钟）**只对上线后的新数据生效**；历史缴款单一律不动。
- **理由**：一份已对过账的账，即使算法有微小误差，也比「按新算法重算一遍」更可信——后者没人能证明重算结果是对的。
- **允许的两种处理**：① 当期盘点差异挂账；② 人工挂账说明。
- **禁止**：写「重算历史缴款单」脚本；批量 UPDATE 历史 refund_component_fen / difference_net_fen。

### R2 退款归属按「物理钱箱 + 边界」处理
- **归属逻辑**：按退款时间与缴款单班次窗口匹配——**钱从哪个班次出，就算哪个班次的扣减**。
- **边界缓冲**：距交班（start/end）**±5 分钟内**标记「边界退款」，生成时**不计入任何班次**，写审计 RECON_SHIFT_BOUNDARY_PENDING，等财务/店长人工指定归属。
- **人工归属优先级高于 as-of 截断**：自动窗口分支受 refund_time <= slip.create_time 约束；**人工归属分支不受约束**。若给人工归属加截断，则「财务确认了，系统却说『不算』」，边界功能就废了。
- **权威口径**：docs/design/api-deposit-slip-attribution.md §7。**改一侧不改另一侧 = 口径撕裂**。

### R3 结账不能双计数
- 缴款单 refund_component_fen 一律**重算**（从 refund_shift_attribution 现值推导），**绝不累加**。
- 验证方式：连改归属两次后 rcf 仍等于单笔金额（若实现成累加会是 2 倍）。
- **反面案例**：曾计划「/adjust 成功后补写一条日志」——实际库存变更**已自动写一条 inventory_movement**，再补会双计数、流水合计翻倍。正确修法是「透传 movementType，只写一条」。

### R4 记账系统里「冲正」永远优于「删除」
- 测试产生的流水**用反向流水冲正**（remark='[E2E:revert] …'），**不 DELETE**。效果：余额回基线、流水行数**只增不减**、审计链完整。
- **已发生的事故**：① 删「重复授权行」实际删掉了唯一有效授权（inventory:query 丢 2 个角色）；② 批量撤回临时绑定时 DELETE 条件过宽，误删 2 条既有 user_roles。
- **铁律**：
  > SELECT 错了只是看到错数据，DELETE 错了是**静默删掉别人的数据**，不报错、不留痕。
  > 授权行/审计行**永远不用 DELETE，只用 deleted=1**；去重前必须核对「另一条同权限授权是 deleted=0」——deleted=1 的行**不算数**；任何 DELETE 先 SELECT count(*) 确认命中行数。

### R5 金额单位：元/分混用是本系统最大隐性风险
- orders **表内部就混用两种单位**：numeric(元) 的 order_amount/discount_amount/actual_amount/refund_amount + bigint(分) 的 total_amount/final_amount/paid_amount/coupon_amount/points_amount。
- 唯一合法换算实现：MoneyYuanToFen.toFen(BigDecimal)（service/finance/recon/importing/MoneyYuanToFen.java:39）。**禁止自行 ×100 / ÷100**。
- 5 条最危险的跨表混用点见 docs/audit/money-unit-boundary.md。

---

## 二、代码陷阱

### C1 禁止用 INSERT ... ON CONFLICT 重写长列 SQL
- 场景：并发同 idempotency_key 下单，唯一键冲突后 PG 把**外层事务标记 aborted**（SQLState 25P02），后续 SQL 全废——500 的真因是这个，不是「没捕获异常」。
- **错误修法**：ON CONFLICT DO NOTHING RETURNING —— 要在 XML 里重写 orders 的 **50+ 列 INSERT**，等于手工维护第二份列清单，会与实体双向漂移（本阶段刚因此修了 12 处列不一致）。
- **采用修法**：Propagation.NESTED **保存点** —— 撞唯一键只回滚保存点，外层事务不中止，达到同一关键性质，改动集中在一个类（OrderInsertAttempt.insertWithinSavepoint()）。
- **若将来要换**：只替换该类；但**先补测试**：并发同 key × N 轮「主表 1 行 + 子表不多行」。

### C2 禁止前端调不存在的接口后兜底假数据
- 事故：财务总账页显示「凭证金额 0.00 / 状态未知」，原因是 formatAmount(null) 返回 '0.00'，把「无值」画成了「零」。财务会读成「本期无发生额」。
- **规则**：无值显示 —；未知枚举显示 未知状态(原始码)；接口不存在时显示「该功能未上线」并跳转真实可用接口，**绝不用 0/空列表兜底**。
- **另一类**：4 个 404 端点曾被误判为「前端在调不存在的接口」，实际是**前端根本没调**——**跨层结论必须两层都取证**（后端 404 ≠ 有调用方）。

### C3 验证必须看重启后的 [BOOT] 日志
- [BOOT] build=<git短哈希> at <构建时间> 由 ApplicationRunner 打印（BuildInfoLogger）。
- **事故**：改完代码没重启就测，看到 5 条全是 IN，误判「实现失败」；重启后一次通过。
- **规则**：验证前先看 [BOOT] 的哈希是不是当前 HEAD；比比对文件时间戳可靠（增量编译会蒙时间戳）。
- **同族陷阱**：mvn compile ≠ mvn test-compile（只编主源会漏掉测试源编译错误）。

### C4 跨层调用：单层局部都正常，问题只在链上暴露
四种同族事故：
1. service return Result.error(String)（默认 500）→ controller catch(RuntimeException) 吞掉 → 业务码丢失；
2. 代码要 hr:contract-template:view，permissions 目录没有 → 除超管外全 403，**不报错**；
3. POST /v1/kitchen/scan 经 service 写表，controller 无 mapper → 覆盖率扫描判成「无入口」，**漏标**；
4. 表在迁移里缺失，但活库靠手工建表「看起来正常」。
- **规则**：凡跨层判断（覆盖率、异常码、权限、表依赖），**必须沿调用链验证**；只验一层时的「0 问题」结论不可信。

### C5 名字 ≠ 实际（本阶段同一个坑踩了 5 次）
| 我以为 | 实际 |
|---|---|
| marketing_promotions 表不存在 | 实际表是单数 marketing_promotion，列一直存在 |
| inventory_log 被读取 | StoreInventoryLogMapper.xml 已改读 inventory_movement |
| operation_logs 缺表 | 实体查的是 sys_operation_logs（已存在） |
| food_trace 有写入 | foodTraceMapper 全仓 0 处调用，是死实体 |
| /v1/assets、/v1/order 等路径 | 实际是 /v1/asset、/v1/orders |
- **规则**：表名用 SELECT to_regclass('public.<表名>') 核对；**判「有写入」只认 Mapper 实例调用**（xxxMapper.insert/update），不认「文件里出现实体类名」；接口路径从 @RequestMapping 精确取，**不许凭经验拼**。

---

## 三、验证陷阱

### V1 验证 Flyway 不能用 psql 裸跑
- 用 psql -f 按**文件名字典序**应用 254 个 V*.sql → failed=25，差点得出「迁移历史无法从零重放」。
- 改用 FlywayMigrateRunner（Flyway API）→ ok=true executed=254 failed=0。
- 三点差异：① **字典序 ≠ Flyway 版本语义序**（V1.0.0.400 / V4.0.0 / V9.0.0 与 V20261028_014 先后完全不同，Flyway 按段解析并补 0）；② psql 遇错即停，Flyway 把「已存在」当 WARN 跳过；③ psql 没有 schema_history / validate / outOfOrder。
- **规则**：验证一个由工具管理的对象，必须用该工具本身。详见 scripts/qa/db/README.md。

### V2 测试必须跑全量，不能用子集代表整体
- 事故：只跑 -Dtest=A,B（2 个类 27 用例）就报「SysSettingServiceTest 已转绿」，而全量清单里它 4F **一直在**——它从未绿过。
- **规则**：凡报「已修/已绿」，必须贴**全量** mvn test 的最后两行（Tests run: 与 BUILD）；推断必须标注「推断，未实测」。

### V3 空库断言必须标 skipped，不能直接 PASS
- **规则**：分母=0 时必须输出 skipped_no_data，**既不算 PASS 也不算 FAIL**，且必须打印。
- 反面案例：AC-49d 曾因 seed 只有一条缴款单而假绿；A37 曾因 end_time IS NULL 排除在飞班次而漏判合法退款。
- **推论**：任何 gate 输出都要带三个数字（PASS / skipped / FAIL），skipped 占比 >50% 时提示「分母不足，PASS 不可信」。

### V4 审计工具自身必须有 --self-test
本阶段 3 个工具因自身 bug 给出错误结论：
- entity_column_audit 下标用错（m[4] 当 m[2]）→ 154 条全部误判「表不存在」，**掩盖 95 条真实问题**；
- permission_matrix_diff join 用错列（permission_id::text 而非 permission_id_str）→ 1732 条正常授权被判「未解析」；
- psql 裸跑（见 V1）。
- **规则**：审计脚本必须喂**已知答案**的样本自测；自测最好同时打印「正确判据」与「曾经的错误判据」的结果，**这样有人改回旧逻辑时自测立刻红**。

### V5 「假象冒充真问题」的四种形态
| 假象 | 真因 |
|---|---|
| 「测试已绿」 | 只跑了子集 |
| 「实现失败」 | 旧进程没重启 |
| 「进程卡死」 | print 块缓冲没 flush（python -u / flush=True） |
| 「编译通过」 | 跑的是 compile 不是 test-compile |

### V6 长跑必须能自证活着
- e2e 全量含 **16 分钟设计内等待**（等班次越过边界）。没有心跳时会被误判为挂起。
- 已实现：[BEAT +Xs] stage 心跳 + 独立 daemon 看门狗（threading.Timer + os._exit(3) + faulthandler.dump_traceback），并用「指向黑洞地址」验证过它**真的会杀**。
- **规则**：超时退出码必须非 0；等待循环里必须打「这是设计内等待，不是卡死」。

---

## 四、15 条工程教训 → 7 条核心铁律

| # | 铁律 | 出处 |
|---|---|---|
| 1 | **先验证再动手**：改代码前先用 SQL/接口证明问题存在与形态 | bug-④ jsonb 规范化（没先验证会埋「假篡改告警」） |
| 2 | **断言必须能证明会红**：改测试前先问「产品改坏，这条断言会不会红」 | AC-49d 假绿、AC-58 三步断言加负向演练 |
| 3 | **局部绿 ≠ 整体绿**：全量测试、全量 e2e、全链验证 | lesson 11 |
| 4 | **跨层必验全链**：入口→服务→数据，缺一层则「0 问题」不可信 | lesson 12 + 四种同族事故 |
| 5 | **名字 ≠ 实际**：表名 to_regclass、写入看 Mapper 调用、路径读 @RequestMapping | lesson 10/13 + 5 次踩坑 |
| 6 | **审计工具必须自证**（--self-test）；**验证工具管理的对象必须用该工具** | lesson 15 + 3 个工具 bug |
| 7 | **动数据只能因为它脏了**，不能因为「它是测试造的」；**冲正优于删除** | R4 + 两次 DELETE 事故 |

---

## 五、事故速查表（按症状找原因）

| 症状 | 首要怀疑 |
|---|---|
| 应用起不来 / 集成测试全 ERROR | 迁移版本重复（V20261028_01 vs _001）或 target/classes 残留（先 mvn clean） |
| 某角色突然 403 | 授权行形态错（非 PERM_*）或 role_id 写成数字；跑 A44/A45 |
| 页面显示 0.00 / 空列表 | 前端兜底假数据；查真实接口是否 404 |
| 库存数量虚高/对不上 | 有人绕开 InventoryService 直接写表漏流水；或测试未冲正 |
| 缴款单净额差 100 倍 | 元/分混用（见 R5） |
| 接口 500 但业务码本该 400/404 | service 直接 Result.error(String) 或裸 RuntimeException（见 C4） |
| 表存在但查询报「列不存在」 | 实体列名错（含全大写、单复数、旧列名）；跑 entity_column_audit |
| 新环境/CI 起不来 | 部署缺口（deploy_gap_audit）或 from-zero 迁移失败 |
