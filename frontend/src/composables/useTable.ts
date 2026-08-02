import { ref, reactive, onMounted, type Ref } from 'vue';
import type { PageResponse, PageParams } from '@/types';
import logger from '@/utils/logger';

interface UseTableOptions<T> {
  api: (params: PageParams) => Promise<PageResponse<T>>;
  pageSize?: number;
  autoLoad?: boolean;
}

interface UseTableReturn<T> {
  tableData: Ref<T[]>;
  loading: Ref<boolean>;
  pagination: {
    current: number;
    pageSize: number;
    total: number;
  };
  refresh: () => Promise<void>;
  handlePageChange: (page: number) => void;
  handleSizeChange: (size: number) => void;
}

/**
 * 纯表格数据加载Composable（无CRUD操作）
 * 适用于只读数据展示页，如日志查看、报表展示等
 * @template T - 数据项类型
 */
export function useTable<T>(options: UseTableOptions<T>): UseTableReturn<T> {
  const {
    api,
    pageSize: defaultPageSize = 20,
    autoLoad = true,
  } = options;

  const tableData = ref<T[]>([]) as Ref<T[]>;
  const loading = ref(false);

  const pagination = reactive({
    current: 1,
    pageSize: defaultPageSize,
    total: 0,
  });

  const refresh = async (): Promise<void> => {
    loading.value = true;
    try {
      const params: PageParams = {
        page: pagination.current,
        size: pagination.pageSize,
      };
      const response = await api(params);
      tableData.value = response.records || [];
      pagination.total = response.total || 0;
    } catch (error: unknown) {
      const err = error instanceof Error ? error : new Error(String(error));
      logger.error('useTable', '加载数据失败', err);
    } finally {
      loading.value = false;
    }
  };

  const handlePageChange = (page: number): void => {
    pagination.current = page;
    refresh();
  };

  const handleSizeChange = (size: number): void => {
    pagination.pageSize = size;
    pagination.current = 1;
    refresh();
  };

  if (autoLoad) {
    onMounted(() => {
      refresh();
    });
  }

  return {
    tableData,
    loading,
    pagination,
    refresh,
    handlePageChange,
    handleSizeChange,
  };
}

export type { UseTableOptions, UseTableReturn };
