import { ref, reactive, onMounted, type Ref } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import type { PageResponse, PageParams, TableColumnConfig } from '@/types';
import logger from '@/utils/logger';

interface CrudApi<T, Q = Record<string, unknown>> {
  getList: (params: Q & PageParams) => Promise<PageResponse<T>>;
  delete?: (id: string) => Promise<void>;
  batchDelete?: (ids: string[]) => Promise<void>;
  updateStatus?: (id: string, status: string) => Promise<void>;
}

interface UseCrudTableOptions<T, Q = Record<string, unknown>> {
  api: CrudApi<T, Q>;
  columns?: TableColumnConfig<T>[];
  queryForm?: Ref<Q>;
  autoLoad?: boolean;
  pageSize?: number;
  immediate?: boolean;
  deleteConfirmText?: string;
  batchDeleteConfirmText?: string;
  confirmBeforeDelete?: boolean;
  /** 状态变更前是否弹出二次确认（默认 true，避免误操作启用/禁用） */
  confirmBeforeStatusChange?: boolean;
  /** 状态变更二次确认文案，默认 "确定要变更此记录的状态吗？" */
  statusChangeConfirmText?: string;
  onDeleteSuccess?: () => void;
  onBatchDeleteSuccess?: () => void;
  onStatusChangeSuccess?: (id: string, status: string) => void;
  onError?: (action: string, error: Error) => void;
}

interface UseCrudTableReturn<T, Q = Record<string, unknown>> {
  tableData: Ref<T[]>;
  loading: Ref<boolean>;
  pagination: {
    current: number;
    pageSize: number;
    total: number;
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

/**
 * CRUD表格通用Composable
 * 提供列表数据加载、分页、搜索重置、删除、批量删除、状态切换等标准CRUD操作
 *
 * queryForm 支持传入 Ref<Q>，此时会直接使用外部 ref（页面模板和 useCrudTable 共享同一份表单数据）
 *
 * @template T - 数据项类型
 * @template Q - 查询参数类型
 */
export function useCrudTable<T, Q = Record<string, unknown>>(
  options: UseCrudTableOptions<T, Q>,
): UseCrudTableReturn<T, Q> {
  const {
    api,
    queryForm: externalQueryForm,
    autoLoad = true,
    pageSize: defaultPageSize = 20,
    immediate = true,
    deleteConfirmText = '确定要删除这条记录吗？',
    batchDeleteConfirmText = '确定要删除选中的记录吗？',
    confirmBeforeDelete = true,
    confirmBeforeStatusChange = true,
    statusChangeConfirmText = '确定要变更此记录的状态吗？',
    onDeleteSuccess,
    onBatchDeleteSuccess,
    onStatusChangeSuccess,
    onError,
  } = options;

  /** 分页大小偏好持久化：用户选择的每页条数保存在 localStorage，刷新后保持 */
  const PAGE_SIZE_KEY = 'fts-table-page-size'
  const persistedSize = Number(localStorage.getItem(PAGE_SIZE_KEY))
  const initialPageSize = persistedSize > 0 ? persistedSize : defaultPageSize

  /** 统一错误处理：若使用方未传 onError，则默认弹出 ElMessage 提示 */
  const handleError = (action: string, error: Error, fallbackMsg = '操作失败，请稍后重试'): void => {
    logger.error('useCrudTable', `${action}失败`, error);
    if (onError) {
      onError(action, error);
    } else {
      ElMessage.error(error.message || fallbackMsg);
    }
  };

  const tableData = ref<T[]>([]) as Ref<T[]>;
  const loading = ref(autoLoad && immediate);

  // 如果外部传入了 Ref，直接使用；否则创建内部 Ref
  const queryForm = externalQueryForm || ref({} as Q) as Ref<Q>;

  // 安全的深拷贝函数，防止JSON.parse(undefined)错误
  const safeClone = <T>(value: T | undefined): T => {
    if (value === undefined || value === null) {
      return {} as T;
    }
    try {
      return JSON.parse(JSON.stringify(value)) as T;
    } catch {
      return {} as T;
    }
  };

  // 保存初始快照用于重置
  const initialQueryFormSnapshot: Q = safeClone(queryForm.value);

  const pagination = reactive({
    current: 1,
    pageSize: initialPageSize,
    total: 0,
  });

  /** 加载列表数据 */
  const refresh = async (): Promise<void> => {
    loading.value = true;
    try {
      const params = {
        ...queryForm.value,
        page: pagination.current,
        size: pagination.pageSize,
      } as Q & PageParams;

      const response = await api.getList(params);
      tableData.value = response.records || [];
      pagination.total = response.total || 0;
    } catch (error: unknown) {
      const err = error instanceof Error ? error : new Error(String(error));
      handleError('load', err, '加载数据失败，请稍后重试');
    } finally {
      loading.value = false;
    }
  };

  /** 重置查询条件并刷新 */
  const resetQuery = async (): Promise<void> => {
    queryForm.value = safeClone(initialQueryFormSnapshot) as Q;
    pagination.current = 1;
    await refresh();
  };

  /** 删除单条记录 */
  const handleDelete = async (id: string): Promise<void> => {
    if (!api.delete) {
      ElMessage.warning('当前未配置删除接口，无法删除');
      return;
    }
    try {
      if (confirmBeforeDelete) {
        await ElMessageBox.confirm(deleteConfirmText, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        });
      }
      await api.delete(id);
      onDeleteSuccess?.();
      if (tableData.value.length <= 1 && pagination.current > 1) {
        pagination.current--;
      }
      await refresh();
    } catch (error: unknown) {
      // 用户主动取消不视为错误，静默返回
      if (error === 'cancel' || (error instanceof Error && error.message === 'cancel')) return;
      const err = error instanceof Error ? error : new Error(String(error));
      handleError('delete', err, '删除失败，请稍后重试');
    }
  };

  /** 批量删除 */
  const handleBatchDelete = async (ids: string[]): Promise<void> => {
    if (!ids || ids.length === 0) {
      ElMessage.warning('请先勾选要删除的记录');
      return;
    }
    if (!api.batchDelete && !api.delete) {
      ElMessage.warning('当前未配置删除接口，无法批量删除');
      return;
    }
    try {
      if (confirmBeforeDelete) {
        await ElMessageBox.confirm(
          `${batchDeleteConfirmText}（共${ids.length}条）`,
          '批量删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          },
        );
      }
      if (api.batchDelete) {
        await api.batchDelete(ids);
      } else if (api.delete) {
        await Promise.all(ids.map((id) => api.delete!(id)));
      }
      onBatchDeleteSuccess?.();
      await refresh();
    } catch (error: unknown) {
      if (error === 'cancel' || (error instanceof Error && error.message === 'cancel')) return;
      const err = error instanceof Error ? error : new Error(String(error));
      handleError('batchDelete', err, '批量删除失败，请稍后重试');
    }
  };

  /** 状态变更 */
  const handleStatusChange = async (id: string, status: string): Promise<void> => {
    if (!api.updateStatus) {
      ElMessage.warning('当前未配置状态变更接口，无法变更状态');
      return;
    }
    try {
      if (confirmBeforeStatusChange) {
        await ElMessageBox.confirm(statusChangeConfirmText, '状态变更', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        });
      }
      await api.updateStatus(id, status);
      onStatusChangeSuccess?.(id, status);
      await refresh();
    } catch (error: unknown) {
      if (error === 'cancel' || (error instanceof Error && error.message === 'cancel')) return;
      const err = error instanceof Error ? error : new Error(String(error));
      handleError('statusChange', err, '状态变更失败，请稍后重试');
    }
  };

  /** 页码变化 */
  const handlePageChange = (page: number): void => {
    pagination.current = page;
    refresh();
  };

  /** 每页条数变化（持久化用户偏好，刷新后保持） */
  const handleSizeChange = (size: number): void => {
    pagination.pageSize = size;
    pagination.current = 1;
    localStorage.setItem(PAGE_SIZE_KEY, String(size));
    refresh();
  };

  if (autoLoad && immediate) {
    onMounted(() => {
      refresh();
    });
  }

  return {
    tableData,
    loading,
    pagination,
    queryForm,
    refresh,
    resetQuery,
    handleDelete,
    handleBatchDelete,
    handleStatusChange,
    handlePageChange,
    handleSizeChange,
  };
}

export type {
  UseCrudTableOptions,
  UseCrudTableReturn,
  CrudApi,
};