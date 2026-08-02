import { ref, type Ref } from 'vue';
import logger from '@/utils/logger';

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

/**
 * 弹窗状态管理Composable
 * 提供弹窗显示/隐藏、数据传递、确认/取消等标准弹窗操作
 * @template T - 弹窗携带的数据类型
 */
export function useDialog<T = undefined>(
  options: UseDialogOptions<T> = {},
): UseDialogReturn<T> {
  const { onOpen, onConfirm, onCancel } = options;

  const visible = ref(false);
  const loading = ref(false);
  const data = ref<T | undefined>(undefined) as Ref<T | undefined>;

  /** 打开弹窗 */
  const open = (openData?: T): void => {
    data.value = openData;
    visible.value = true;
    try {
      onOpen?.(openData);
    } catch (error: unknown) {
      const err = error instanceof Error ? error : new Error(String(error));
      logger.error('useDialog', '打开弹窗回调失败', err);
    }
  };

  /** 关闭弹窗 */
  const close = (): void => {
    visible.value = false;
    loading.value = false;
  };

  /** 确认操作 */
  const confirm = async (): Promise<void> => {
    if (!onConfirm) {
      close();
      return;
    }

    loading.value = true;
    try {
      await onConfirm();
      close();
    } catch (error: unknown) {
      const err = error instanceof Error ? error : new Error(String(error));
      logger.error('useDialog', '确认操作失败', err);
      loading.value = false;
    }
  };

  return {
    visible,
    loading,
    data,
    open,
    close,
    confirm,
  };
}

export type { UseDialogOptions, UseDialogReturn };
