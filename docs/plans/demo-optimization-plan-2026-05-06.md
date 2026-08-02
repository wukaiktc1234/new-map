# Demo应用全面优化计划（SDD流程）

> **文档版本**: v1.0
> **创建日期**: 2026-05-06
> **状态**: 待评审

---

## 📊 执行摘要

本计划基于对Demo应用的全面架构评估，共识别出 **33个问题**，涵盖7个评估维度。计划分为 **4个阶段** 执行，预计总周期 **6-8周**。

### 核心指标

| 指标 | 当前值 | 目标值 | 提升幅度 |
|------|--------|--------|---------|
| TypeScript any使用率 | 8处 (24%) | 0处 | -100% |
| CSS !important使用 | 72处 | 0处 | -100% |
| 组件代码重复 | ~600行 | <100行 | -83% |
| 可访问性覆盖率 | ~60% | >95% | +35% |

---

## 🔴 第一阶段：关键缺陷修复（第1-2周）

### 1.1 DataTable组件类型安全重构 [Critical]

**问题描述**：
- [DataTable.vue](src/components/core/DataTable.vue) 中6处使用 `any` 类型
- 污染整个项目的类型系统，降低代码可维护性

**SDD执行计划**：

#### Phase 1: Specify（规格定义）
```yaml
目标: 消除DataTable组件中的所有any类型
范围:
  - Props接口中的 data: any[]
  - emit事件中的 selection: any[], row: any
  - handleSelectionChange/handleRowClick函数参数
验收标准:
  - TypeScript编译无错误
  - 所有类型通过严格模式检查
  - 向后兼容现有使用方式
```

#### Phase 2: Plan（技术方案）
```typescript
// 方案：引入泛型 + 联合类型
interface DataTableProps<T = Record<string, unknown>> {
  data: T[]
  columns: Column<T>[]
}

interface Column<T = Record<string, unknown>> {
  prop: keyof T
  label: string
  // ...其他属性保持不变
}
```

#### Phase 3: Tasks（任务拆解）
1. 定义泛型接口 `Column<T>` 和 `DataTableProps<T>`
2. 重构 `handleSelectionChange` 函数签名
3. 重构 `handleRowClick` 函数签名
4. 更新所有使用DataTable的页面（ModernEmployee, DeviceList等5个文件）
5. 运行TypeScript编译验证
6. 更新ComponentGallery示例代码

#### Phase 4: Implement（实现）
- 预计工时：4小时
- 负责人：AI Assistant
- 依赖：无

#### Phase 5: Review（审查清单）
- [ ] 所有any类型已消除
- [ ] 泛型约束合理，不过度复杂
- [ ] 现有页面无需大幅修改即可适配
- [ ] 类型推导正确工作

#### Phase 6: Accept（验收标准）
- `npm run typecheck` 通过
- IDE智能提示正常工作
- 无运行时类型错误

---

### 1.2 CSS !important清理专项 [Critical]

**问题描述**：
- 全项目72处使用 `!important`（违反[项目规则#27](.trae/rules/project_rules.md)）
- 主要集中在 `_element-overrides.scss` 和业务页面样式中

**SDD执行计划**：

#### Phase 1: Specify
```yaml
目标: 消除所有!important使用，改用选择器特异性提升优先级
原则:
  - 保持样式覆盖效果不变
  - 不引入新的性能问题
  - 符合BEM命名规范
```

#### Phase 2: Plan
**技术方案**：
1. 使用更具体的选择器（如 `.data-table .el-table__row td.cell`）
2. 利用CSS层叠顺序（后定义覆盖前定义）
3. 对于Element Plus深层覆盖，使用 `:deep()` 嵌套选择器

#### Phase 3: Tasks
1. 统计所有!important位置（已完成：72处）
2. 分类处理：
   - Element Plus覆盖（~40处）→ 使用`:deep()`嵌套
   - 业务样式冲突（~20处）→ 提升选择器特异性
   - 第三方库强制覆盖（~12处）→ 保留并注释原因
3. 逐文件修改并测试
4. 视觉回归测试（浅色/深色主题）

#### Phase 4: Implement
- 预计工时：8小时
- 分批执行：每次处理10-15处

---

## 🟠 第二阶段：架构优化与组件复用（第3-4周）

### 2.1 CRUD列表页通用布局提取 [Major]

**问题描述**：
- DeviceList、AssetLedger、FinancePage 存在约200+行重复代码
- 共同模式：SearchPanel → StatCard → DataTable → Pagination

**解决方案**：创建 `CrudLayout.vue` 组合式组件

```vue
<!-- src/components/business/CrudLayout.vue -->
<template>
  <div class="crud-layout">
    <slot name="search" />
    <slot name="stats" />
    <slot name="table" />
    <slot name="pagination" />
  </div>
</template>
```

**预期收益**：
- 减少重复代码 60%+
- 统一布局行为（响应式、间距、主题适配）
- 新增CRUD页面开发效率提升 50%

---

### 2.2 StatusTag/el-tag统一迁移 [Major]

**当前状态**：
- ✅ 已完成：DeviceList、ModernEmployee、AssetLedger、EmployeeManagement
- ⏳ 待检查：OrganizationPage、FinancePage、TabPage、MasterDetailPage、ComponentGallery

**任务清单**：
1. 扫描剩余5个页面的el-tag使用情况
2. 统一替换为StatusTag或category模式
3. 为新发现的slot列添加 `ellipsis: false`
4. 更新ComponentGallery的el-tag示例为"已废弃"标记

---

## 🟡 第三阶段：性能与体验优化（第5-6周）

### 3.1 列表虚拟滚动实现 [Medium]

**适用场景**：
- 数据量 > 1000行的表格（如员工管理、财务报表）
- 当前方案：分页（每页10-50条）

**技术选型**：
- `@tanstack/vue-virtual` 或 `vue-virtual-scroller`
- 与现有DataTable集成（可选功能，通过props控制）

**预期效果**：
- 首屏渲染时间减少 40-60%
- 内存占用降低（仅渲染可视区域行）

---

### 3.2 图片/资源懒加载优化 [Medium]

**当前问题**：
- 员工头像（el-avatar）未使用懒加载
- 大型图标包（@element-plus/icons-vue）全量导入

**优化方案**：
1. 头像图片：使用 `loading="lazy"` 属性
2. 图标按需导入：将 `import { ... } from '@element-plus/icons-vue'` 改为按需
3. 路由级代码分割：确认所有动态导入配置正确

---

### 3.3 Pinia Store完善 [Minor]

**当前Store列表**：
- ✅ layout（主题、密度设置）
- ⏳ 缺失：用户偏好、表格状态持久化

**新增Store规划**：
```typescript
// stores/table-preferences.ts
interface TablePreferences {
  [tableName: string]: {
    pageSize: number
    defaultSort: SortOptions
    columnVisibility: Record<string, boolean>
    filters: FilterState
  }
}
```

---

## 🔵 第四阶段：质量保障与文档（第7-8周）

### 4.1 单元测试补充 [Minor]

**优先级排序**：
1. **StatusTag组件**（28种状态映射）
2. **DataConverter工具函数**（金额转换、状态映射）
3. **DataTable组件**（ellipsis逻辑、排序、选择）

**测试框架**：Vitest + Vue Test Utils

**覆盖率目标**：
- Core组件：> 80%
- Utils函数：> 90%
- 业务页面：> 60%（可选）

---

### 4.2 可访问性（A11y）审计与修复 [Minor]

**当前问题**（4处）：
1. 表格缺少 `<caption>` 或 `aria-label`
2. 操作按钮缺少 `aria-label`（纯文字按钮需语义化）
3. SearchPanel表单缺少 `<label>` 关联
4. Drawer关闭按钮键盘无法聚焦

**修复方案**：
```html
<!-- Before -->
<el-button link>编辑</el-button>

<!-- After -->
<el-button link aria-label="编辑 {{ row.name }}">编辑</el-button>
```

---

### 4.3 开发文档更新 [Continuous]

**待更新文档**：
1. [项目规则](.trae/rules/project_rules.md)
   - 添加 DataTable `ellipsis` 属性说明
   - 添加 StatusTag category 模式使用指南
   - 更新 CSS 变量规范（深色主题优化记录）

2. [ComponentGallery](src/views/Demo/ComponentGallery.vue)
   - 添加 CrudLayout 使用示例
   - 标记 el-tag 为"不推荐"

3. [API规范文档](docs/spec/06-API接口设计.md)
   - 补充前端组件API文档

---

## 📈 进度跟踪

### 总体进度

```
第一阶段 ████████████████████░░░░ 80% (2/2.5项完成)
第二阶段 ████░░░░░░░░░░░░░░░░░░░ 15% (规划中)
第三阶段 ░░░░░░░░░░░░░░░░░░░░░░░   0%
第四阶段 ░░░░░░░░░░░░░░░░░░░░░░░   0%
```

### 本轮已完成的改进（2026-05-06）

| # | 问题 | 状态 | 影响范围 |
|---|------|------|---------|
| 1 | 表格StatusTag省略号显示异常 | ✅ 已修复 | DataTable + 3个业务页 |
| 2 | SearchPanel展开/折叠按钮抖动 | ✅ 已修复 | SearchPanel + AdvancedSearchPanel |
| 3 | 深色主题标签背景过亮 | ✅ 已修复 | _dark-mode.scss (30+变量) |
| 4 | DataTable ellipsis架构缺陷 | ✅ 已修复 | Column接口新增属性 |

---

## 🎯 下一步行动

### 立即执行（本周内）

1. **启动 DataTable 泛型重构**
   - 文件：[DataTable.vue](src/components/core/DataTable.vue)
   - 预计时间：4小时
   - 依赖：无

2. **完成 el-tag → StatusTag 迁移扫描**
   - 文件：5个待检查页面
   - 预计时间：2小时
   - 输出：迁移清单 + 修复PR

3. **CSS !important 清理（第一批）**
   - 重点：`_element-overrides.scss`（40处）
   - 预计时间：3小时
   - 风险：中（需视觉回归测试）

### 短期规划（2周内）

4. **创建 CrudLayout 通用组件**
   - 文件：新建 `src/components/business/CrudLayout.vue`
   - 预计时间：6小时
   - 影响：3个列表页重构

5. **Pinia table-preferences Store**
   - 文件：新建 `src/stores/table-preferences.ts`
   - 预计时间：3小时
   - 功能：表格设置持久化

---

## 📚 参考资料

- [项目规则](.trae/rules/project_rules.md) - 完整规范体系
- [SDD Pipeline](.trae/skills/sdd-pipeline/SKILL.md) - 开发流程指引
- [架构评估报告](docs/reviews/architecture-review-2026-05-06.md) - 详细问题清单
- [TypeScript strict mode](docs/spec/13-编码规范.md) - 类型安全要求

---

## 📝 变更日志

| 日期 | 版本 | 变更内容 | 作者 |
|------|------|---------|------|
| 2026-05-06 | v1.0 | 初始版本，包含33个问题的系统性改进计划 | AI Assistant |

---

**文档结束**

> 💡 **提示**：本文档应作为持续改进的指导文件，每完成一个阶段后更新进度和状态。
