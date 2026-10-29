# 宽 catch 吞语义 —— 扫描清单（Lead 2026-10-30 P0）

> **规则**：宽 `catch (Exception)` 之前，必须先声明「哪些异常必须透传」；
> 否则等于把**状态码语义**吞掉（403 变 500、业务 400 变 500、权限失败变「空集/默认值」）。

## 1. 扫描口径与规模

```text
① 全仓 controller + service 的 `catch (Exception` / `catch (Throwable`：**1915 处**（不含 test）
② 高风险子集（catch 后 6 行内 return Result.error(500 / INTERNAL_SERVER_ERROR）：**212 处**
   —— 这些会把「鉴权拒绝」「业务校验失败」降级成 500，前端无法区分
```

## 2. 已修（本轮之前的实战触发）

| 位置 | 被吞异常 | 吞后返回 | 修法 |
|------|----------|----------|------|
| `PosShiftController#stats` | `AccessDeniedException` | `500 统计失败`（HTTP 200 + code=500） | 显式 `catch (AccessDeniedException ade) { throw ade; }` 透传 → **真 403** |
| `PosShiftController#exceptions` | 同上 | 同上 | 同上 |
| `AdminPermissionCatalog#reload` | 任意 DB 异常 | `WARN` + catalog 空（静默降级） | 改为 `[METRIC] admin_permission_catalog_load_failed` + 状态 `loadedOk=false` + 门禁断言 |
| `PosExceptionController`（不存在，实际在 GlobalExceptionHandler） | `NoHandlerFound`/`NoResourceFound` | `HTTP 200 + code=404` | 改 `ResponseEntity` 真 404 |

## 3. 高风险清单（212 处，按文件聚合 top 12 → 逐文件展开时按下表判定）

| 文件 | 高风险处数 | 典型被吞异常 | 吞后返回 | 是否应透传 |
|------|-----------|--------------|----------|------------|
| `RoleController.java` | 17 | `BusinessException`（角色名校验/重复） | `Result.error(500,…)` | **是** |
| `KnowledgeBaseController.java` | 16 | IO / 文件不存在 | `Result.error(500,…)` | 部分（IO 可降级，需注释） |
| `PermissionController.java` | 16 | `BusinessException`（权限码非法） | `Result.error(500,…)` | **是** |
| `TrainingController.java` | 10 | `BusinessException` | `Result.error(500,…)` | **是** |
| `UserPermissionOverrideController.java` | 9 | `BusinessException` / 鉴权 | `Result.error(500,…)` | **是** |
| `InventoryWarningController.java` | 9 | 查询/校验 | `Result.error(500,…)` | 视情况 |
| `InventoryStatsController.java` | 8 | 同上 | `Result.error(500,…)` | 视情况 |
| `InventoryCheckController.java` | 8 | `BusinessException`（盘点状态机） | `Result.error(500,…)` | **是** |
| `InventoryController.java` | 8 | 同上 | `Result.error(500,…)` | **是** |
| `InventoryUnitController.java` | 7 | 同上 | `Result.error(500,…)` | 视情况 |
| `InventoryAdjustController.java` | 7 | `BusinessException` | `Result.error(500,…)` | **是** |
| `WarehouseController.java` | 7 | `BusinessException` | `Result.error(500,…)` | **是** |

## 4. 统一修法（白名单透传，不逐个加 catch）

```java
// ❌ 修前：鉴权/业务异常被吞成 500
try {
    return svc.doIt();
} catch (Exception e) {
    log.error(…);
    return Result.error(500, "失败：" + e.getMessage());
}

// ✅ 修后：先声明必透传类型，再兜底
try {
    return svc.doIt();
} catch (AccessDeniedException | BusinessException e) {
    throw e;                       // 状态码语义必须保留（403 / 业务码）
} catch (Exception e) {
    log.error(…);
    return Result.error(500, "失败：" + e.getMessage());
}
```

**为什么不是「加一个全局 @ExceptionHandler」就够**：异常在**内层已被 catch 吃掉**，
全局处理器根本收不到 —— 所以必须在内层显式透传。

## 5. 分级与排期

| 优先级 | 范围 | 理由 |
|--------|------|------|
| **P0 已完成** | POS 域 3 处（stats / exceptions / catalog） | 今天真实踩到（403→500、catalog 静默空） |
| P1 | 上表 12 个文件（≈145 处） | 含业务状态机与权限域，错误码被吞影响可诊断性 |
| P2 | 其余 ≈1767 处 | 多为 IO/第三方降级，需逐处判断是否合理，且多数已有注释 |

**不做的代价**：前端只能显示「系统错误」，用户/客服无法区分「没权限」「参数错」「真的崩了」——
这正是今天 403、404、错误体为空、建单 200+400 四个问题的共同根源。
