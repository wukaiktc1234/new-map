# 实体列名审计结果（bug-⑪ 复查，2026-10-28）

## 0. 先说脚本 Bug（已修）

`scripts/qa/entity_column_audit.py` 里判断「表是否存在」用错了元组下标：
```python
tables = {m[0] for m in mismatches if m[4] != '(表不存在)'}   # ✗ m[4] 是「相似列」，永远是列名
tables = {m[0] for m in mismatches if m[2] != '(表不存在)'}   # ✓ m[2] 才是判定列
```
后果：**所有 154 条都被打印成「B/表不存在 → 先判死代码」**，真实存在的列不匹配被完全掩盖（我上轮就是被这个误导的）。
修复后重新分类。

## 1. 分类结果

```
总不一致 154 条
  ├─ A/表不存在（部署缺口）        59 条
  └─ 表存在但列不匹配             95 条
        ├─ 空表（legacy，改动无效） 83 条
        └─ **表有数据（真 500 风险）12 条**
```

## 2. 12 条「表有数据但列缺失」——真实风险清单

| 表 | 行数 | 实体 | 实体列名 | 库里没有 | 影响 |
|----|------|------|----------|----------|------|
| `accounting_subject` | 102 | AccountingSubject.java | `create_by` / `update_by` | ✔ 都没有 | 会计科目查询/更新会 500 |
| `dish_combo` | 1 | DishCombo.java | `combo_name` | 真实列是 `name` | 套餐查询 500 |
| `employees` | 14 | Employee.java | `name` / `employee_code`（**写成大写**） | 真实列是小写 `name` / `employee_code` | 员工查询 500（大小写敏感） |
| `roles` | 24 | Role.java | `ROLE_CODE` / `ROLE_NAME` / `DESCRIPTION` / `STATUS`（**全大写**） | 真实列小写 | 角色查询 500 |
| `permission_assignment_log` | 27 | PermissionAssignmentLog.java | `deleted` | 无该列 | 删除过滤 SQL 500 |

**注意一类共性问题**：`employees` 与 `roles` 的实体把列名写成了**全大写**（`EMPLOYEE_NAME`、`ROLE_CODE`），
而 PostgreSQL 未加引号的标识符一律折叠成小写 → 只有加双引号时才区分大小写。
→ 这两处要么是「从 Oracle/DB2 迁过来的遗留」，要么是历史手误。**属批量可修的机械问题**。

## 3. 建议

1. **12 条先修**（改实体列名或补列，逐条判断哪边是真相；`roles`/`employees` 明显是实体侧错）；
2. 83 条空表不匹配：**不动**（空表 = 没在用，改了也没收益；等这些功能立项时一起做）；
3. 59 条表不存在：已在部署缺口棘轮里（33 张基线之外的部分已并入）；
4. **把本条自检接入门禁**：`entity_column_audit.py --only-existing-with-data`，要求 12 → 0 才允许提交。
