# preflight 使用与门禁编排规则

## 1. 并发禁忌（2026-10-30 三次教训）

```text
❗ 跑 preflight 期间**不得**并发执行 `mvn clean` / `mvn package`。
   原因：preflight 自己会先跑 `mvn -o -B clean` 再 test-compile，
         并发的另一个构建会把 target 删掉 → 对方立刻 BUILD FAILURE（今天已发生）。
   措施：preflight 现在会创建 .preflight.lock（1 小时内存在则拒绝启动）。
```

## 2. 门禁编排规则（Lead 2026-10-30 定，写死）

```text
🔴 门禁编排必须【先跑完再汇总】，不许短路。
   修前：巡检 FAIL → 直接 return 2 → 排在后面的 permission_catalog / permission_403 gate 从未执行
        → 新 gate 在 BLOCK 场景下形同不存在（又一次「看起来装了」）。
   修后：所有 gate 的结果收集进 blocked[]，逐条打印 [BLOCK] 名字+原因，最后统一 exit 2。
   唯一的短路例外：环境不可用（连不上库/后端未起）—— 这种情况下继续跑没有意义。
```

## 3. 当前 gate 清单（--full）

| gate | 判据 | 退出码含义 |
|------|------|-----------|
| 迁移版本 gate | 版本号唯一 | 3=WARN |
| 权限常量 gate | 代码常量码 ⊆ 目录 | 2=BLOCK / 3=WARN(棘轮) |
| 错误响应体 gate | HTTP≥400 时 body 非空且含 code，且命中预期分支码 | 2=BLOCK / 3=SKIP |
| 权限目录 gate | parent_id 类型 / status=1 码数 / 含 pos:order:pay / 无空串 | 2=BLOCK / 3=ENV |
| 403 语义 gate | 无权限必须真 403；跨终端真阳性；SKIP 时 exit 3 | 2=BLOCK / 3=SKIP |
| 宽 catch 棘轮 | catch(Exception) 后返 500 的次数 ≤ 218 | 2=BLOCK |
| 巡检 | 27~28 条断言 | 2=BLOCK |
| 安全回归 | S1~S6 | 2=BLOCK / 3=未就绪 |
| 干净库迁移 | 从零迁移成功 | 2=BLOCK |

## 4. exit code 语义

```text
0 = 全绿
2 = BLOCK（有硬性失败）
3 = WARN / SKIP（有未验证项或棘轮提示，不阻断）
```
