# ADR-20261030：迁移安全（两次生产停机的复盘）

> **一句话结论**：迁移文件是唯一能「一次改动直接让生产起不来」的类型。两次停机都是**编辑既有文件时的低级错误**，
> 不是设计问题 —— 因此规则也必须是「编辑纪律 + 静态门禁」，而不是「更小心一点」。

## 1. 两次停机复盘

### 事故 1：单 `$` 定界符（2026-10-30 13:11）
```
文件：V20261030_012__add_missing_columns_order_items.sql
错误：DO $  … END $;   （应为 $$）
结果：Flyway 执行失败 → Tomcat 起不来 → **服务不可用约 40 分钟**
错误信息：org.postgresql.util.PSQLException: 错误: 语法错误 在 "$" 或附近的
损伤：PostgreSQL DDL 事务回滚 → 012 无记录、无部分修改（已实证）
```
### 事故 2：编码损坏吃掉闭合引号（2026-10-30 13:22）
```
文件：V20261030_014__add_missing_columns_pos_shifts.sql（第一版，含中文注释）
错误：中文字节被破坏 → RAISE EXCEPTION 的闭合单引号丢失 → 字符串未闭合
结果：第二次停机（012/013 已 success=t，014 失败）
附带错误：断言写死 `IF v_cnt <> 18`，而生产 pos_shifts 实际 16 列 → 即使语法正确也会失败
```

## 2. 五条规则（每条都有事故出处）

| # | 规则 | 出处 |
|---|---|---|
| 1 | **修改既有文件必须 read 全文 → edit 精确替换；禁止 str.replace / 正则批量插入**；一条改动涉及 3 处以上必须 write 全量 | 今晚 3 次：覆盖方法括号 / 注释插进字符串 / DO 块插错位置 |
| 2 | **迁移必须用 Flyway 本身双跑（空库/未打补丁库）**，psql -f 单跑不算验证 | 事故 1：psql 跑过一次「看起来 OK」 |
| 3 | **迁移文件必须过静态 lint**（`migration_sql_lint.py`，7 条 self-test） | 事故 1+2 的直接形态都已固化为反例 |
| 4 | **已 success=t 的历史迁移一个字都不许改**（改了 checksum 失配 → 启动失败） | V20260701_003 / 20261030_011 被误报时的处置 |
| 5 | **迁移的唯一真相来源是生产库**，不是代码里的 DDL 字符串 | TraceCodeSystemConfig 声明 numeric(10,3) / varchar(100)，生产实际 numeric(12,2) / varchar(200) |

## 3. `migration_sql_lint.py` 的 7 条 self-test

```
[PASS] bad: GBK bytes decoded as UTF-8              → BLOCK（规则1：严格 UTF-8）
[PASS] bad: unterminated string                     → BLOCK（规则2：字符串未闭合）
[PASS] good: English comment containing don't       → PASS （证明剥离生效，不误报）
[PASS] good: doubled quote escape ('it''s ok')      → PASS （'' 转义正确处理）
[PASS] good+warn: comment marker inside string      → PASS+WARN（规则5）
[PASS] good: balanced dollar block                  → PASS
[PASS] bad: single dollar DO                        → BLOCK（规则3：事故1形态）
self-test: 7/7 PASS ｜ 全量扫描 275 个文件 BLOCK=0
```
**为什么用状态机而不是正则**：正则无法区分「注释 / 字符串 / 双写引号」三者嵌套。
反例：`RAISE EXCEPTION 'a--b';`（`--` 在字符串里）、`RAISE EXCEPTION 'it''s';`（`''` 是转义）。

## 4. 门禁红了怎么办（今晚验证过的流程）

```
门禁红 → ① 判断是「判据过宽」还是「被测物真错」
        ② 只有在能证明判据过宽时，才收窄判据，且必须在注释里写明为什么收窄
        ③ 不许「放宽断言让它变绿」
实例：
  · migration_sql_lint 误报 V20260701_003（注释里含 $$）→ 修 lint（剥离注释），不改历史迁移
  · ConstantTimeLoginContractTest 误报「用户不存在」→ 收窄到 login 方法体（注释写明理由）
```

## 5. 待做

| 项 | 下一步动作 | 验收判据 |
|---|---|---|
| 接 preflight | `migration_sql_lint.py` 加进 preflight（BLOCK） | preflight 输出含一行 lint 结果 + exit code |
| 历史迁移非 ASCII | 270 个文件含中文注释（WARN，豁免不改正） | lint 输出汇总为 1 行「N 个历史迁移豁免」 |
