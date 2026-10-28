# 干净库迁移闸门用法（**不要用 psql 裸跑**）

## 为什么不能用 `psql -f` 逐个应用 `V*.sql`

2026-10-28 实测对比（同一个干净库、同一批 254 个迁移文件）：

| 方法 | 结果 | 原因 |
|------|------|------|
| `psql -f` 按文件名**字典序**逐个跑 | `failed=25` | ① 字典序 ≠ Flyway 版本语义序（`V1.0.0.400` vs `V20261028_014` 顺序不同）；② psql 遇错即停，Flyway 会把「列/表已存在」当 WARN 跳过；③ 没有 `flyway_schema_history` 与 validate |
| `FlywayMigrateRunner`（Flyway API） | **`ok=true executed=254`，`failed=0`** | 与生产启动时的迁移机制完全一致 |

**结论**：判定「迁移链是否可从零重放」**只能用 FlywayMigrateRunner**；用 psql 裸跑得出的失败结论无效。

## 用法

```powershell
cd P:\my-new-project
$env:PGPASSWORD="123456"
# 1) 造干净库
& ".\PostgreSQL\18\bin\psql.exe" -h localhost -p 5432 -U postgres -c "DROP DATABASE IF EXISTS ft_zero2"
& ".\PostgreSQL\18\bin\psql.exe" -h localhost -p 5432 -U postgres -c "CREATE DATABASE ft_zero2"

# 2) 生成 classpath（一次即可）
cd backend
& "H:\fuwu\apache-maven-3.9.11\bin\mvn.cmd" -o -B dependency:build-classpath "-Dmdep.outputFile=..\.verify-tmp\cp.txt" -q
cd ..

# 3) 编译 runner
$cp = (Get-Content ".verify-tmp\cp.txt" -Raw).Trim()
$full = "backend\target\classes;$cp;scripts\qa\db"
& "P:\my-new-project\JDK21\bin\javac.exe" -encoding UTF-8 -cp $full -d ".verify-tmp\runner" "scripts\qa\db\FlywayMigrateRunner.java"

# 4) 跑迁移（输出 FLYWAY_RESULT 供解析）
& "P:\my-new-project\JDK21\bin\java.exe" -cp ".verify-tmp\runner;$full" FlywayMigrateRunner ft_zero2 "backend/src/main/resources/db/migration"
```

期望输出：

```text
FLYWAY_RESULT ok=true executed=254 target=2026022701
```

## 结果解读

```text
ok=true          → 迁移链可从零重放
executed=N       → 本次实际执行条数
target=…         → 迁移后的 schema 版本（注意：这是 Flyway 的『最后执行的那条』，
                   并不等于「最大版本号」——本次实测 target=2026022701，而库里 max(version)=9.0.0，
                   因为 outOfOrder=true 且存在 V9.0.0 这类低版本号文件在后期才加入）
```
