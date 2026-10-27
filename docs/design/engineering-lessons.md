# 工程教训（每轮开工前读一遍）

> 全部来自本项目实际事故，不是通用建议。每条附「怎么发现的」，便于判断是否还适用。

## 1. 断言写错比代码写错更危险

**它会把合法业务判成缺陷，逼人去删正常数据。**

- 事故：`A37` 旧判据要求 `end_time IS NOT NULL`，把「进行中班次」整条排除 → 营业时段内的合法退款（`refund 125`，有 active 班次 375）被判成「落不进任何窗口」。
- 危险点：当时的“修复方向”是删掉这笔退款 —— 删了就真把正常数据毁了。
- 规则：**断言报红时，先问「是数据错，还是断言口径错」，不许先删数据。**

## 2. 「没数据就通过」是最危险的假绿

- 事故：8 条断言（A1/A10/A27/A30/A31/A33/A42/A43）在分母=0 时报 `[PASS] rows=0`，等于「没数据 = 通过」。库清空后巡检显示 17 PASS，其中 12 条是 skipped 或假绿。
- 修复：分母=0 → `[INFO] skipped_no_data 分母=0`，不计入判定；判定项随数据量浮动（空库 16、有数据 28）。
- 规则：**分母=0 必须输出 skipped_no_data，不能 PASS；汇总行必须显示「判定项 N 条（其中 M 条 skipped_no_data）」。**

## 3. 静默失效只能靠断言抓

- 本会话 10 条静默失效中，**4 条是断言抓出来的**（A7 余额与流水链不一致、A37 口径错、A44 permission_id 形态、A45 引用可解析），人工复查一条都没抓到。
- 典型：418 行超管授权 `role_id='1'`（数字形态）→ join 丢弃 → 超管权限一直是空壳，A45 上线才发现。
- 规则：**新功能上线必须同时给出「能抓它的断言」；断言必须做过负向演练（证明会红）。**

## 4. 改公式必须查全部写入点

- 事故：`differenceNetOf`（`DepositSlipServiceImpl:945`）是 `difference_net_fen` 的唯一 helper，但加备用金时 `generate`/`receive` 各自内联改，helper 停在旧式 → `confirm-attribution` 重算漏 reserve（E2E ④ 用 `reserve=0` 单覆盖不到）。
- 同时 4 个 SQL 修复脚本仍是旧公式 —— 一旦「按老办法修数」就把 bug 写回库。
- 规则：**改金额公式时 grep 全部写入点（Java setter + SQL UPDATE + 修复脚本），逐个对齐，并补一条 `reserve>0` 的断言（E2E 第 29 条）。**

## 5. 表名相似对必须核对语义

- 事故：`food`/`foods`（E2E 改了历史表，库存永不恢复）、`dish_recipe`/`dish_recipes`（旧模型空表 + 缺列 → 扣料链路从未跑通）、`food_trace_codes`/`food_trace_code`（两代模型，前者从未有数据）。
- 规则：**看到单复数/近义表名，必须对比列结构与行数，判断是「两代模型」还是「误引用」；两代模型不能只改 `@TableName`（字段不兼容），要么切 Mapper，要么补 seed。**

## 6. 判断功能是否存在必须 grep 全类

- 事故：我据「只看到 `BankTransactionQueryService`」就下结论「银行流水无写入通道（P1 缺口）」，实际 `BankImportController` 两阶段 CSV 导入（`import` → `commit`）一直存在。ts-ops 只报了只读端点，我没追问也没 grep 全类。
- 规则：**判断某功能是否存在，必须 grep 全类/全项目，不能只看一个方法或一个端点；他人回报不完整时必须追问。结论里必须写明检索范围（命令 + 命中文件清单），否则视为未验证。**

## 7. 权限验证必须「真实账号 + 接口级」

- 事故：`role_permissions.permission_id` 用权限码插入（6 行孤儿）→ join 丢弃 → 店长 receive/deposit 一直 403；`submit` 只绑 CASHIER 而收银员进不了后台 → 单据永远停在 DRAFT（NEW-38 流程死锁）。
- 规则：**授权写入用 `CROSS JOIN permissions` 取 `permission_id_str`；验证只看 403/业务码，不看表；权限矩阵是验收项，不是文档装饰。**

## 8. 清理数据必须同事务内自洽

- 事故：删除流水但未回滚余额 → A7 红（发生 2 次）；`ON_ERROR_STOP` 以 `psql -c` 内嵌 `set` 的写法无效 → 整笔事务静默回滚、漏删 4 条流水 → A31 红。
- 规则：**删除 + 余额重算必须同一事务；psql 必须用 `-v ON_ERROR_STOP=1`；清理脚本必须有「apply 前锚点非空」与「apply 后残留为 0」双向护栏。**

## 9. 别假设项目用了 Lombok（本条来自 bug-③）

- 事故：bug-③ 给 `entity/OrderNew` 与 `dto/OrderResultDTO` 只加了字段，没写访问器 → `mvn compile` BUILD FAILURE，
  报「找不到符号 `getIdempotencyKey()` / `setReused()`」；我下意识以为「Spring 项目一般有 Lombok」。
- 实测统计（`backend/src/main/java`，2026-10-27）：**2888 个 Java 文件，使用 `@Data/@Getter/@Setter/@Builder` 的 = 0 个**（0.0%）；
  `entity/` 406 个文件、`dto/` 795 个文件，**同样 0 个**；`import lombok` 全仓 0 命中。
  抽查 `OrderNew`：手写 getter 38 个 / setter 38 个。→ **本项目零 Lombok，全手写访问器**。
  **记忆锚点：2888 : 0** —— 看到任何「加个字段就行」的念头，先想起这组数字。
- 因此本条不是「同模块混用」也不是「跨模块风格不同」（两者实测均不成立），而是**项目级惯例**：
  1) **加字段必须同时加手写 getter/setter**（`entity/`、`dto/` 无一例外）；
  2) 改实体/DTO 后必须立刻 `mvn -o -B -DskipTests compile` 验证，不能只靠「看起来对」；
  3) 遇到「找不到符号」先查该类**是否有访问器**，不要先怀疑 import 或依赖版本。
- 附带发现（同源，已修）：`mvn test` 曾因 `OrderNewServiceImplDeductTest` / `OrderNewServiceImplOrderNumberA1Test`
  构造器参数个数不匹配而**测试编译失败**（他人改构造器加 `PosRefundFlowRecorder` 未同步测试），
  导致 `spring-boot:run` 连带失败、本地只能 `-Dmaven.test.skip=true` —— 这本身就是「测试假装存在」的静默失效。
  已补 mock + 构造器参数，`mvn -o -B test-compile` BUILD SUCCESS，34 个用例全绿。

## 10. 表名相似对：单数 vs 复数必须核对 to_regclass

- 事故：判断「MarketingPromotion 声明 promotion_code，但表里没这列」——错在查了复数表名 marketing_promotions（不存在），实际表是单数 marketing_promotion 且该列一直存在。结论差点变成「给已有列补迁移」。
- 查法（唯一可靠）：SELECT to_regclass('public.<表名>') 逐个确认；不要凭命名习惯猜单复数，也不要用 information_schema.tables WHERE table_name LIKE 的模糊匹配下结论。
- 规则：表名相似对（x vs xs、x_new vs x）必须用 to_regclass + information_schema.columns 双向核对再下结论；与第 5 条（同名表核语义）合并执行。


## 11. 用子集结果代表全量

- 事故：只跑 `-Dtest=SystemSettingsDatabaseTest,InventoryDeductionServiceImplTest`（2 个类 27 用例）就报「SysSettingServiceTest 已转绿」，而全量清单里该类 4F **一直在**——它从未绿过。同一错误我（Lead）也犯过一次：据 16F 推断「14F/7E」，实测是 15F/8E。
- 危害：**局部绿 ≠ 整体绿**。若按这个结论继续开发，欠账会被永久掩盖，且在发版前才爆。
- 规则：**凡报「已修/已绿」，必须贴全量 `mvn test` 的最后两行（`Tests run: N, Failures: F, Errors: E, Skipped: S` 与 `BUILD SUCCESS/FAILURE`）**；不许用子集、单类、`-Dtest=` 的结果代表全量。推断必须标注「推断，未实测」。

