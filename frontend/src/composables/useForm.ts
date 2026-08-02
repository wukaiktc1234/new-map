import { ref, type Ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import logger from '@/utils/logger';

interface UseFormOptions<T extends Record<string, unknown>> {
  initialValues: T;
  rules?: FormRules;
  onSubmit: (values: T) => Promise<void>;
}

interface UseFormReturn<T extends Record<string, unknown>> {
  form: Ref<T>;
  formRef: Ref<FormInstance | null>;
  loading: Ref<boolean>;
  submit: () => Promise<void>;
  reset: () => void;
  validate: () => Promise<boolean>;
  setFieldValue: <K extends keyof T>(key: K, value: T[K]) => void;
}

/**
 * 表单状态管理Composable
 * 提供表单数据、验证、提交、重置等标准表单操作
 * @template T - 表单数据类型
 */
export function useForm<T extends Record<string, unknown>>(
  options: UseFormOptions<T>,
): UseFormReturn<T> {
  const { initialValues, rules, onSubmit } = options;

  const form = ref<T>(structuredClone(initialValues)) as Ref<T>;
  const formRef = ref<FormInstance | null>(null);
  const loading = ref(false);
  const initialValuesSnapshot: T = structuredClone(initialValues) as T;

  /** 验证表单 */
  const validate = async (): Promise<boolean> => {
    if (!formRef.value) return false;
    try {
      await formRef.value.validate();
      return true;
    } catch {
      return false;
    }
  };

  /** 提交表单 */
  const submit = async (): Promise<void> => {
    const isValid = await validate();
    if (!isValid) return;

    loading.value = true;
    try {
      await onSubmit(form.value);
    } catch (error: unknown) {
      const err = error instanceof Error ? error : new Error(String(error));
      logger.error('useForm', '提交失败', err);
      throw err;
    } finally {
      loading.value = false;
    }
  };

  /** 重置表单 */
  const reset = (): void => {
    form.value = structuredClone(initialValuesSnapshot) as T;
    formRef.value?.resetFields();
  };

  /** 设置单个字段值 */
const setFieldValue = <K extends keyof T>(key: K, value: T[K]):
void => {
    form.value[key] = value;
  };

  return {
    form,
    formRef,
    loading,
    submit,
    reset,
    validate,
    setFieldValue,
  };
}

export type { UseFormOptions, UseFormReturn };
