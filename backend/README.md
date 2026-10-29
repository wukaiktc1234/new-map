# backend（Spring Boot 3.2 / Java 21）

## 启动前必做：先增量编译

```bash
cd backend
mvn -o -B -DskipTests compile
```

然后再启动（Windows）：

```bat
start-backend.cmd          :: 等价于 mvn -o -B spring-boot:run -Dspring-boot.run.profiles=pg
```

### 为什么必须编译

直接 `mvn spring-boot:run` 而 `target/classes` 落后于源码时，会撞：

```
java.lang.NoClassDefFoundError: com/foodtraceability/entity/InventoryCheckAsset
  Caused by: java.lang.ClassNotFoundException: com.foodtraceability.entity.InventoryCheckAsset
```

**这不是代码坏了，是构建产物旧了**（2026-10-28 实际踩过：别人更新了实体/控制器，本地只跑了 test-compile）。
建议把 `mvn -o -B -DskipTests compile` 写进 `start-backend.cmd` 首行，避免每个人拉新代码都误判。

## JDK

仓库自带 `JDK21/`（`P:\my-new-project\JDK21`）。机器上的 `JAVA_HOME` 可能指向不存在的路径，
启动/测试前请显式指定，例如：

```powershell
$env:JAVA_HOME='P:\my-new-project\JDK21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
```

（`scripts/qa/preflight.py` 已自动做这件事：优先用仓库 JDK21，其次校验 `JAVA_HOME`。）

## 测试

```bash
mvn -o -B test-compile     # 快：只证明测试代码能编译，不等于用例通过
mvn -o -B test             # 慢：真跑用例
```

集成测试需要独立测试库 `food_traceability_test`（Flyway 负责迁移，`application-test.yml` 指向它，
测试类用 `@ActiveProfiles("test")`）。
## ⚠ 迁移文件改动与 `mvn clean`

> 改/重命名 `src/main/resources/db/migration/` 下的文件后，**必须 `mvn -o -B clean`** 再 test/run。
> 否则 `target/classes/db/migration/` 残留旧副本，Flyway 会扫到重复版本号，
> 报 `Found more than one migration with version ...` 并使 **应用启动失败**（单测里不会暴露）。
>
> 另：提交前请跑 `python scripts/qa/migration_version_gate.py`（EXIT=2 阻断）——
> 它会把 `V20261028_001` / `V20261028_01` 这种肉眼看不出的重复版本号拦下来。

## 启动前必读：先全杀 java，再核验 `[BOOT]`（2026-10-29 踩坑）

```powershell
# 1) 全杀（不要只杀 8081 的 owner —— 可能有两个 java 同时活着）
Get-Process java -ErrorAction SilentlyContinue | ForEach-Object { Stop-Process -Id $_.Id -Force }
Start-Sleep -Seconds 6
Write-Output (-not [bool](Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue))  # 应为 True
# 2) 启动
mvn -o -B -DskipTests spring-boot:run
# 3) 核验：能打出 [BOOT] 才说明新进程真的起来了
#    期望格式：[BOOT] build=<hash>[-dirty] at <ts> | classesDirMtime=<ts>
```

**为什么**：曾出现两个 java 进程并存，旧进程占着 8081 → 新进程启动时报
`Web server failed to start. Port 8081 was already in use.`，但 `mvn` 的日志很容易被忽略，
于是「以为重启成功、实际还是旧代码」。**`[BOOT]` 行是唯一可靠的启动证据**；
`classesDirMtime` 用来证明「这次确实重新构建过」（git hash 在未提交时不变）。

## 用 git worktree 跑测试前，必须补 `backend/lib`（2026-10-29 踩坑）

`backend/lib` 里的本地 system-scope jar（`pdf-xml-extractor` / `xbrl-json` / `DJFileUtil`）**被 gitignore**，
新建的 worktree 里没有它们 → `mvn test` 直接报：

```text
Could not find artifact com.bosssoft.tool:pdf-xml-extractor:jar:1.0 at ...\backend\lib\pdf-xml-extractor-1.0.jar
```

**做法**：
```powershell
git worktree add .verify-tmp/baseline HEAD
Copy-Item backend\lib\* .verify-tmp\baseline\backend\lib\ -Force -Recurse
cd .verify-tmp\baseline\backend ; mvn -o -B test -Dtest=SomeTest
cd P:\my-new-project ; git worktree remove .verify-tmp/baseline --force
```

**为什么用 worktree 而不是 `git stash`**：当前工作区常年有几十个未提交改动，
`stash/pop` 一旦出错就丢工作；worktree 在独立目录检出 HEAD，**对当前树零影响**。

## 覆盖 Spring 配置：优先环境变量，`mvn -D` 不可靠（2026-10-29 踩坑）

```powershell
# ❌ 不生效：-D 被 Maven 自己的 JVM 吃掉，不会传给 spring-boot:run 派生出的应用进程
mvn -o -B -Dorder.pending.timeout.minutes=1 spring-boot:run
#    启动日志仍打印 [TTL] timeout minutes=15  ← 你会以为改了，其实没改

# ✅ 生效：环境变量（Spring relaxed binding）
$env:ORDER_PENDING_TIMEOUT_MINUTES=1
mvn -o -B spring-boot:run
#    启动日志打印 [TTL] timeout minutes=1
```

**为什么记这条**：这正是「假信号」的一种 —— **你以为配置改了，实际跑的还是默认值**。
因此凡是「有默认值的配置项」，**必须在启动日志里打印生效值**（参考 `TtlOrderCloseJob` 的 `[TTL] timeout minutes=…`）。
