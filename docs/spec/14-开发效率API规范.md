# 食品溯源系统 - 开发效率API规范

> 本文档定义了前端开发效率API（Composable）的标准接口、参数类型、返回值类型和组合规则，确保各模块页面开发的一致性和高效性。

## 文档版本

| 版本 | 日期 | 说明 |
|------|------|------|
| v1.0 | 2026-04-13 | 初始版本 |

## 第一章 总则

### 1.1 目标

- 减少CRUD页面样板代码至3-5行
- 统一表格/表单/弹窗/详情页的开发模式
- 确保所有模块页面遵循相同的数据加载和状态管理模式

### 1.2 适用范围【强制】

- 所有新增业务页面必须使用本规范定义的Composable
- 已有页面逐步迁移，优先迁移高频修改的页面

### 1.3 核心原则

- **约定优于配置**：提供合理默认值，减少必填参数
- **渐进式增强**：简单场景3行代码，复杂场景可扩展
- **类型安全**：所有接口使用泛型，禁止any
- **单一职责**：每个Composable只负责一个关注点

## 第二章 核心Composable清单

| Composable | 职责 | 使用场景 |
|-----------|------|---------|
| `useCrudTable` | CRUD表格页完整逻辑 | 列表页（占页面总数60%+） |
| `useTable` | 纯表格数据加载（无CRUD） | 只读数据展示页 |
| `useForm` | 表单状态/验证/提交 | 新增/编辑表单 |
| `useDialog` | 弹窗状态管理 | 新增/编辑/详情弹窗 |
| `useDetail` | 详情页数据加载 | 详情查看页 |
| `useSearchForm` | 搜索筛选逻辑 | 配合useCrudTable使用 |

## 第三章 useCrudTable 规范【强制】

### 3.1 标准接口

```typescript
interface UseCrudTableOptions<T, Q = Record<string, unknown>> {
  api: {
    getList: (params: Q & PageParams) => Promise<PageResponse<T>>;
    delete?: (id: string) => Promise<void>;
    batchDelete?: (ids: string[]) => Promise<void>;
    updateStatus?: (id: string, status: string) => Promise<void>;
  };
  columns: TableColumn<T>[];
  queryForm?: Q;
  autoLoad?: boolean;
  pageSize?: number;
  immediate?: boolean;
}

interface UseCrudTableReturn<T, Q = Record<string, unknown>> {
  tableData: Ref<T[]>;
  loading: Ref<boolean>;
  pagination: {
    current: Ref<number>;
    pageSize: Ref<number>;
    total: Ref<number>;
  };
  queryForm: Ref<Q>;
  refresh: () => Promise<void>;
  resetQuery: () => Promise<void>;
  handleDelete: (id: string) => Promise<void>;
  handleBatchDelete: (ids: string[]) => Promise<void>;
  handleStatusChange: (id: string, status: string) => Promise<void>;
  handlePageChange: (page: number) => void;
  handleSizeChange: (size: number) => void;
}

function useCrudTable<T, Q = Record<string, unknown>>(
  options: UseCrudTableOptions<T, Q>
): UseCrudTableReturn<T, Q>;
```

### 3.2 最简使用示例

```vue
<template>
  <StandardPage title="员工管理">
    <template #filter>
      <SearchPanel :form="queryForm" :columns="searchColumns" @search="refresh" @reset="resetQuery" />
    </template>
    <DataTable :data="tableData" :columns="columns" :loading="loading" :pagination="pagination"
      @page-change="handlePageChange" @size-change="handleSizeChange" />
  </StandardPage>
</template>

<script setup lang="ts">
import { useCrudTable } from '@/composables/useCrudTable';
import { employeeApi } from '@/api/hr';
import type { Employee } from '@/types/hr';

const { tableData, loading, pagination, queryForm, refresh, resetQuery, handlePageChange, handleSizeChange } = useCrudTable<Employee>({
  api: employeeApi,
  columns: employeeColumns,
  autoLoad: true
});
</script>
```

### 3.3 扩展使用示例

```typescript
const { tableData, loading, pagination, queryForm, refresh, resetQuery,
  handleDelete, handleBatchDelete, handleStatusChange } = useCrudTable<Employee, EmployeeQuery>({
  api: {
    getList: employeeApi.getList,
    delete: employeeApi.delete,
    batchDelete: employeeApi.batchDelete,
    updateStatus: employeeApi.updateStatus
  },
  columns: employeeColumns,
  queryForm: { status: 'active', department: '' },
  pageSize: 20,
  autoLoad: true
});
```

## 第四章 useTable 规范

### 4.1 标准接口

```typescript
interface UseTableOptions<T> {
  api: (params: PageParams) => Promise<PageResponse<T>>;
  pageSize?: number;
  autoLoad?: boolean;
}

interface UseTableReturn<T> {
  tableData: Ref<T[]>;
  loading: Ref<boolean>;
  pagination: {
    current: Ref<number>;
    pageSize: Ref<number>;
    total: Ref<number>;
  };
  refresh: () => Promise<void>;
  handlePageChange: (page: number) => void;
  handleSizeChange: (size: number) => void;
}

function useTable<T>(options: UseTableOptions<T>): UseTableReturn<T>;
```

### 4.2 使用场景

只读数据展示页，如日志查看、报表展示等不需要增删改的页面。

## 第五章 useForm 规范

### 5.1 标准接口

```typescript
interface UseFormOptions<T> {
  initialValues: T;
  rules?: FormRules<T>;
  onSubmit: (values: T) => Promise<void>;
}

interface UseFormReturn<T> {
  form: Ref<T>;
  formRef: Ref<FormInstance | null>;
  loading: Ref<boolean>;
  submit: () => Promise<void>;
  reset: () => void;
  validate: () => Promise<boolean>;
  setFieldValue: <K extends keyof T>(key: K, value: T[K]) => void;
}

function useForm<T extends Record<string, unknown>>(options: UseFormOptions<T>): UseFormReturn<T>;
```

### 5.2 使用示例

```typescript
const { form, loading, submit, reset, validate } = useForm<EmployeeFormData>({
  initialValues: { name: '', department: '', status: 'active' },
  rules: employeeFormRules,
  onSubmit: async (values) => {
    await employeeApi.create(values);
    ElMessage.success('创建成功');
  }
});
```

## 第六章 useDialog 规范

### 6.1 标准接口

```typescript
interface UseDialogOptions<T = undefined> {
  onOpen?: (data?: T) => void | Promise<void>;
  onConfirm?: () => void | Promise<void>;
  onCancel?: () => void;
}

interface UseDialogReturn<T = undefined> {
  visible: Ref<boolean>;
  loading: Ref<boolean>;
  data: Ref<T | undefined>;
  open: (data?: T) => void;
  close: () => void;
  confirm: () => Promise<void>;
}

function useDialog<T = undefined>(options?: UseDialogOptions<T>): UseDialogReturn<T>;
```

## 第七章 useDetail 规范

### 7.1 标准接口

```typescript
interface UseDetailOptions<T> {
  api: (id: string) => Promise<T>;
}

interface UseDetailReturn<T> {
  data: Ref<T | null>;
  loading: Ref<boolean>;
  error: Ref<string | null>;
  load: (id: string) => Promise<void>;
  refresh: () => Promise<void>;
}

function useDetail<T>(options: UseDetailOptions<T>): UseDetailReturn<T>;
```

## 第八章 Composable组合规则【强制】

### 8.1 组合原则

- `useCrudTable` 内部可组合 `useTable` + `useSearchForm`
- `useForm` 可独立使用，也可被 `useDialog` 组合
- `useDialog` 可组合 `useForm` 实现表单弹窗
- 禁止Composable之间产生循环依赖

### 8.2 禁止事项

- 禁止在Composable中直接操作DOM
- 禁止在Composable中使用any类型
- 禁止在Composable中硬编码API路径
- 禁止在Composable中直接使用ElMessage（应通过回调参数传递）

## 第九章 类型定义规范

### 9.1 通用类型

```typescript
interface PageParams {
  page: number;
  size: number;
}

interface PageResponse<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

interface TableColumn<T = Record<string, unknown>> {
  prop: keyof T & string;
  label: string;
  width?: number | string;
  minWidth?: number | string;
  fixed?: 'left' | 'right';
  sortable?: boolean | 'custom';
  formatter?: (row: T, column: TableColumn<T>, cellValue: unknown, index: number) => string;
  slot?: string;
}
```

### 9.2 命名规范

| 类型 | 命名格式 | 示例 |
|------|---------|------|
| 查询参数 | `{Entity}Query` | `EmployeeQuery` |
| 表单数据 | `{Entity}FormData` | `EmployeeFormData` |
| 列配置 | `{entity}Columns` | `employeeColumns` |
| 搜索配置 | `{entity}SearchColumns` | `employeeSearchColumns` |
