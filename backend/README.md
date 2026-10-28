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