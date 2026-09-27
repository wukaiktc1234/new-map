# M3-M4 实施宪法（一页 · Implementation Constitution）

- **日期**：2026-09-27 · 效力范围：P1-LOCATION-MODEL-001 M3-M4 及后续批次（M5-M7）全程
- **性质**：实施纪律文档。与 -001/-002 设计冲突时，以 Owner 拍板 + 本文裁决规则为准。
- **前置**：§24.3f Owner 活体验证收口后 M3-M4 方可动工（当前：实施完成、验证中）。

---

## 一、19 项拍板（原样固化，实施中不得重新讨论）

**D 系列（Location 模型，-001）**
1. D-1 locations 一期不加 parent_location_id 层级
2. D-2 departments 挂接用单列 location_id（映射表后置）
3. D-3 批次维度一期单行化（batch_no 可空列、不进唯一键）
4. D-4 门店账全清后起账（不做流水重放回填）
5. D-5 出餐扣料缺行 = 报错 + 提示调拨（禁止自动建账）
6. D-6 不做多模型，单 location_type 枚举
7. D-7 旧表退役（M7）独立观察窗口，不随一期

**U 系列（用户归属，-001 任务 2）**
8. U-1 总部职能用户 store_id/location_id 保持 NULL
9. U-2 分配入口 = 用户管理页加门店控件（方案 A）
10. U-3 positions 不加门店维度
11. U-4 employees 加 user_id 外键
12. U-5 finqa×3 测试账号建议停用（Owner 定）
13. U-6 设备心跳折中【已废弃】→ 被附加拍板"设备注册绑 location_id"替代（见 #17）
14. U-7 批量分配接口一期做

**附加拍板（-002）**
15. 附加 2：users.store_id → users.location_id 全链改名 + 兼容视图一个观察期
16. 附加 3：admin 读走 data_scope=all 分支；写侧三级位置上下文解析链（显式带 → 对象推导 → NoLocationContext）
17. 附加 4：JWT/SecurityUser 统一写 locationId 单字段；设备注册必填 location_id、心跳自带并校验
18. 务实简化 3 项：批次一期单行化 / 总部用户 NULL+dataScope / 设备注册绑 location_id
19. 全清决策：8 个测试仓不迁移；store_inventory '584'/'9901' 及仓库语义行清除；material_id=999999 虚账清除

## 二、6 条 ID 空间规则（-002 §7，逐字固化）

1. location_id 是库存、调拨、员工归属的唯一外键
2. store_id 保留为 STORE 型 location 的别名，不新增独立 ID 空间
3. store_id ↔ location_id 的映射由 location_id_map 提供
4. 所有跨表 JOIN 若混合使用 store_id 和 location_id，必须显式经 location_id_map
5. 新代码优先使用 location_id；旧代码在观察期内保留 store_id 兼容视图
6. users.store_id 统一改为 users.location_id；store_id 保留兼容视图一个观察期

## 三、禁区（触碰即停）

1. **users.store_id / employees 归属列 / JWT / SecurityUser / SecurityUtils** —— 属 P1-USER-LOCATION-001，M3-M4 一律不碰
2. **经营链 store_id 列名**（orders/日结/资产/设备/排班/招聘等 ~250 文件）—— 保留不动，仅允许注释标注
3. **store_id 与 stores/stores_new/location 的 JOIN** —— 必须经 location_id_map，禁止数值假设相等（R-04 数值巧合为前车之鉴）
4. **任何"默认门店/仓库 1"式兜底** —— 一律拒绝或显式报错（NoLocationAssignedException / NoLocationContext）
5. **历史 Flyway 脚本** —— 只新增不改；旧表 M7 前不 DROP
6. **topic 命名 / /ws/device / 24.3f 文件（InventoryLogMapper.xml）** —— 本卡范围外
7. **inventory_log 后门** —— `InventoryConsumptionController:100/117` 允许 update/delete 流水（矩阵 §6-9）：M3-M4 合并流水后**必须关闭**该写后改删入口；实施期间禁止新增任何"流水改/删"能力的调用方

## 四、裁决规则（实施中遇到分歧时）

1. **与拍板冲突的发现**（如又一处 ID 语义漂移、类型不一致）：停手 → 记录 file:line → 报告 Owner，不自行裁定
2. **设计未覆盖的实现细节**（列序、索引命名、异常文案）：按 -002 口径 + 仓库既有惯例就近选择，实施记录中登记即可
3. **行为兼容问题**：以行为快照（`docs/quality/m3m4-preflight/inventory-chain-snapshot-20260927-001.json`）为 diff 基线，合并后重放同组端点，除"伪门店账消失"这一预期差异外不得有其他行为变化
4. **DDL 红线**：新表唯一索引/非空约束以 -001 §1.3/§1.4 为准；source_type + source_ref NOTNULL 不得放松
5. **停机窗口**：M3-M4 发布须冻结写操作；先 pg_dump 快照，回滚 = DOWN 脚本 + 快照恢复（演练后执行）
6. **行为敏感点以 Owner 批复为准**：考古矩阵 §6 的 9 处敏感点中，4 处（流水失败语义 / 假流水 / 调拨成本 / 事务边界）已提交 Owner 是非题（`owner-decision-confirm-20260927-002.md`）；**未批复前一律按现状行为迁移**（最小行为变化原则），禁止顺手"修正"；其余 5 处按"现状迁移 + 全清清单"处理

---

*本页为 M3-M4 实施唯一纪律来源；-001/-002 提供结构与字段细节，本文提供边界与裁决。*
