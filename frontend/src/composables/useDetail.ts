import { ref, type Ref } from 'vue';
import logger from '@/utils/logger';

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

/**
 * 详情页数据加载Composable
 * 提供根据ID加载详情数据、刷新等标准详情页操作
 * @template T - 详情数据类型
 */
export function useDetail<T>(options: UseDetailOptions<T>): UseDetailReturn<T> {
  const { api } = options;

  const data = ref<T | null>(null) as Ref<T | null>;
  const loading = ref(false);
  const error = ref<string | null>(null);
  let currentId: string | null = null;

  /** 加载详情数据 */
  const load = async (id: string): Promise<void> => {
    currentId = id;
    loading.value = true;
    error.value = null;
    try {
      data.value = await api(id);
    } catch (err: unknown) {
      const errorObj = err instanceof Error ? err : new Error(String(err));
      error.value = errorObj.message;
      logger.error('useDetail', '加载详情失败', errorObj);
    } finally {
      loading.value = false;
    }
  };

  /** 刷新详情数据 */
  const refresh = async (): Promise<void> => {
    if (currentId) {
      await load(currentId);
    }
  };

  return {
    data,
    loading,
    error,
    load,
    refresh,
  };
}

export type { UseDetailOptions, UseDetailReturn };
