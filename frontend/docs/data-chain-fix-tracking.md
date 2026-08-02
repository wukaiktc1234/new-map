
---

## 2026-08-01 批 16：详情状态同步 + 申请审批时间 + checkbox + 审批对话框美化 + 表格拖拽

1. **详情内操作状态同步**：详情对话框内 提交审批/审批通过/驳回 后自动重新加载当前订单（reloadDetail），不再需要关闭重开；审批通过/驳回不再强制关闭详情
2. **申请审批时间/审批人**：后端 approve 补写 approvedTime/approvedBy（SecurityUtils 取当前用户）；申请详情显示审批时间（实测 approvedTime=2026-08-02 01:14:24 approvedBy=系统管理员）
3. **跨供应商 checkbox**：去掉 :disabled（未选供应商也可勾选）；删除"明细可混用多家供应商…"提示文本
4. **审批对话框美化**：节点信息卡片化（徽章+分隔线+审批人行）、意见区 header+提示、通过/驳回按钮带图标、宽度 520（去掉 fts-dialog--sm 覆盖）
5. **表格拖拽横滚**：新建 vTableDragScroll 指令（rAF 节流、仅可滚动时启用、交互元素不触发、拖动抑制误点），应用到 DataTable（全局所有表格生效）
6. **状态徽章主题**：确认 --fts-status-* 变量深浅两套已完备（tokens + dark-mode），无需改动

---

## 2026-08-01 批 17：拖拽滚动修复 + 徽章颜色根治 + 审批时间回填 + 审批对话框专业重设计

1. **拖拽横滚修复**：el-table 实际滚动容器是 .el-scrollbar__wrap（body-wrapper overflow hidden 不滚动）→ 指令优先选 scrollbar__wrap。实测拖动 0→100 ✓
2. **状态徽章颜色根治**：根因①scoped 样式不匹配动态 class（status-tag-- 无 data-v）→ 改 inline style 绑定主题变量（colorStyle/dotStyle）；根因②orderStatusMap 的 primary/danger 变量缺失 → tokens+dark 补齐 --fts-status-primary/danger（映射 active/error 色系）。实测：待审核黄、已完成绿、已下单蓝、已驳回红、草稿灰
3. **审批时间**：含义=审批人点击"确认通过"的时刻（后端 approve 写入）；历史 approved/completed 申请已回填 approved_time=update_time（29+0 条）
4. **审批对话框专业重设计**：审批上下文头部（节点徽章+业务类型+单据编号）、审批人信息 descriptions（与订单详情风格一致）、意见区、通过/驳回按钮带图标；540px；深浅主题自适应

---

## 2026-08-01 批 22：编辑重复 bug 根治 + 终止调研 + 对话框推广（上下游）

### 1. 采购申请编辑保存重复 bug（根治）
- **根因**：PurchaseRequestItemMapper.selectByRequestId SQL **无 deleted=0 过滤**——后端 update 用逻辑删除（deleted=1），旧明细仍被查回 → 每次编辑加载累积旧组+新组 → 保存再插新组 → 无限膨胀（PR20260801006 累积到 8 条）
- **修复**：SQL 加 AND deleted = 0（getById/update/generateOrder 全部受益）
- **存量清理**：PR20260801006 删除 6 条 deleted=1 冗余，保留最新 2 条（wawa/午餐肉）
- **验证**：编辑 2 行 → 保存 → 再编辑仍 2 行（不重复）✓

### 2. 对话框推广（采购订单上下游）
- ArrivalFormDialog（新增到货单/编辑物流）：960→1100（wide）
- ArrivalDetailDialog（到货单详情）：960→1100（wide）
- RequestDetailDialog（采购申请详情）：960→1100（wide）

### 3. 终止功能设计调研（见会话输出）
