/**
 * useStandardPage - 标准页面通用逻辑 Composable
 * ============================================================
 *
 * 【定位】
 * 与 StandardPage.vue 组件配套使用，提供标准页面的通用状态管理和方法。
 * 封装了标签切换、加载状态、分页、搜索重置等跨页面重复逻辑。
 *
 * 【设计原则】
 * - 轻量级：只管理纯 UI 状态，不耦合具体业务逻辑
 * - 可组合：与 useCrudTable / useSearchForm 等业务 Composable 配合使用
 * - 可扩展：通过泛型支持自定义搜索表单类型
 *
 * 【典型使用场景】
 * 1. 纯列表页（带筛选 + 分页）
 * 2. 带标签页的多视图页面
 * 3. 需要统一加载状态的页面
 *
 * 【与其他 Composable 的关系】
 * - useStandardPage → 页面骨架状态（标签、分页、加载、搜索/重置动作）
 * - useCrudTable     → 业务 CRUD 逻辑（数据加载、删除、状态变更）
 * - useSearchForm    → 搜索表单管理（表单数据、构建查询参数）
 *
 * 推荐组合方式：
 * ```
 * const { activeTab, loading, pagination, handleSearch, handleReset } = useStandardPage();
 * const { tableData, refresh, ... } = useCrudTable({ api, pageSize: pagination.size });
 * const { searchForm, buildQueryParams } = useSearchForm({ defaults: initialForm });
 * ```
 *
 * 【使用示例】
 * ```typescript
 * // 基础用法
 * const {
 *   activeTab,
 *   setActiveTab,
 *   loading,
 *   setLoading,
 *   pagination,
 *   resetFilters,
 *   handleSearch,
 *   handleReset,
 * } = useStandardPage();
 *
 * // 自定义初始值
 * const page = useStandardPage({
 *   defaultTab: 'all',
 *   defaultPageSize: 20,
 * });
 *
 * // 带搜索回调
 * const page = useStandardPage({
 *   onSearch: () => { refresh(); },
 *   onReset: () => { resetForm(); handleSearch(); },
 * });
 * ```
 */

import { ref, reactive, type Ref } from 'vue';

/** 分页状态接口 */
export interface StandardPagination {
  /** 当前页码（从1开始） */
  current: number;
  /** 每页条数 */
  size: number;
  /** 总记录数 */
  total: number;
}

/** UseStandardPage 配置选项 */
export interface UseStandardPageOptions {
  /** 默认激活的标签名 */
  defaultTab?: string;
  /** 默认每页条数 */
  defaultPageSize?: number;
  /** 搜索回调（点击"查询"按钮时触发） */
  onSearch?: () => void | Promise<void>;
  /** 重置回调（点击"重置"按钮时触发，在重置分页和标签后调用） */
  onReset?: () => void | Promise<void>;
}

/** UseStandardPage 返回值接口 */
export interface UseStandardPageReturn {
  /** 当前激活的标签名 */
  activeTab: Ref<string>;
  /** 切换标签并重置分页到第一页 */
  setActiveTab: (tab: string) => void;
  /** 全局加载状态 */
  loading: Ref<boolean>;
  /** 设置加载状态 */
  setLoading: (value: boolean) => void;
  /** 分页状态（响应式对象） */
  pagination: StandardPagination;
  /** 重置分页到第一页 */
  resetPagination: () => void;
  /** 重置所有过滤器（分页 + 标签） */
  resetFilters: () => void;
  /** 处理搜索操作（重置分页 + 触发 onSearch 回调） */
  handleSearch: () => Promise<void>;
  /** 处理重置操作（重置全部 + 触发 onReset 回调） */
  handleReset: () => Promise<void>;
}

/**
 * 标准页面通用逻辑 Composable
 *
 * 提供标准页面所需的通用状态管理：
 * - 标签切换（activeTab + setActiveTab）
 * - 加载状态（loading + setLoading）
 * - 分页管理（pagination + resetPagination）
 * - 搜索/重置动作（handleSearch + handleReset）
 *
 * @param options - 配置选项
 * @returns 标准页面状态和方法
 *
 * @example
 * ```vue
 * <script setup lang="ts">
 * import { useStandardPage } from '@/composables/useStandardPage';
 * import { useCrudTable } from '@/composables/useCrudTable';
 *
 * const { activeTab, loading, pagination, handleSearch, handleReset } = useStandardPage({
 *   defaultTab: 'all',
 *   defaultPageSize: 20,
 *   onSearch: async () => {
 *     await refresh();
 *   },
 * });
 *
 * const { tableData, refresh } = useCrudTable({
 *   api: employeeApi,
 *   pageSize: pagination.size,
 * });
 * </script>
 * ```
 */
export function useStandardPage(options: UseStandardPageOptions = {}): UseStandardPageReturn {
  const {
    defaultTab = '',
    defaultPageSize = 20,
    onSearch,
    onReset,
  } = options;

  /* ===== 标签状态 ===== */
  const activeTab = ref<string>(defaultTab);

  /**
   * 切换激活标签，同时重置分页到第一页
   * @param tab - 目标标签名
   */
  const setActiveTab = (tab: string): void => {
    activeTab.value = tab;
    pagination.current = 1;
  };

  /* ===== 加载状态 ===== */
  const loading = ref<boolean>(false);

  /**
   * 设置全局加载状态
   * @param value - 加载状态值
   */
  const setLoading = (value: boolean): void => {
    loading.value = value;
  };

  /* ===== 分页状态 ===== */
  const pagination = reactive<StandardPagination>({
    current: 1,
    size: defaultPageSize,
    total: 0,
  });

  /** 重置分页到第一页（保留 pageSize 和 total） */
  const resetPagination = (): void => {
    pagination.current = 1;
  };

  /* ===== 过滤器重置 ===== */

  /**
   * 重置所有过滤器：将分页恢复到第一页
   * 注意：不重置标签（标签切换由用户主动触发），不重置搜索表单（由 useSearchForm 管理）
   */
  const resetFilters = (): void => {
    pagination.current = 1;
  };

  /* ===== 搜索/重置动作 ===== */

  /**
   * 处理搜索操作
   * 1. 重置分页到第一页
   * 2. 调用 onSearch 回调（通常用于触发数据刷新）
   */
  const handleSearch = async (): Promise<void> => {
    resetPagination();
    await onSearch?.();
  };

  /**
   * 处理重置操作
   * 1. 重置分页到第一页
   * 2. 调用 onReset 回调（通常用于清空搜索表单 + 刷新数据）
   */
  const handleReset = async (): Promise<void> => {
    resetPagination();
    await onReset?.();
  };

  return {
    activeTab,
    setActiveTab,
    loading,
    setLoading,
    pagination,
    resetPagination,
    resetFilters,
    handleSearch,
    handleReset,
  };
}
