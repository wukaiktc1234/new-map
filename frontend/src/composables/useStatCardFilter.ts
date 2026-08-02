/**
 * useStatCardFilter - 统计卡片点击筛选 Composable
 * ============================================================
 *
 * 【定位】
 * 封装"统计卡片点击 → 自动筛选列表"的交互模式，统一 HR 模块所有页面
 * 的统计卡片点击行为，避免每个页面重复实现相同逻辑。
 *
 * 【交互模式：Toggle取消 + 多卡片互斥】
 * - 点击未激活的卡片：设置筛选条件 + 标记为激活 + 触发搜索
 * - 点击已激活的卡片：取消筛选条件 + 清除激活标记 + 触发搜索
 * - 多卡片互斥：同一时刻仅一张卡片处于激活状态
 *
 * 【设计原则】
 * - 与 useStandardPage 解耦：不依赖具体页面状态，由调用方传入 searchForm 和搜索回调
 * - 类型安全：通过泛型支持任意搜索表单类型
 * - 可组合：可与 useStandardPage / useCrudTable 等组合使用
 *
 * 【典型使用场景】
 * 1. 单 Tab 页面：员工管理、健康证管理等
 * 2. 多 Tab 页面：招聘管理（4 Tab）、薪资管理（多 Tab）等
 *    - 多 Tab 场景需在 Tab 切换时调用 clearActiveStat() 清除激活状态
 *
 * 【使用示例】
 * ```typescript
 * // 单 Tab 页面
 * const searchForm = ref({ status: '', departmentId: '', keyword: '' })
 * const { activeStatKey, handleStatClick, clearActiveStat } = useStatCardFilter({
 *   searchForm,
 *   onSearch: () => refresh(),
 * })
 *
 * // 模板中
 * <StatCard
 *   v-for="stat in statistics"
 *   :key="stat.key"
 *   :icon="stat.icon"
 *   :label="stat.label"
 *   :value="stat.value"
 *   :color-type="stat.colorType"
 *   :class="['stat-clickable', { 'stat-active': activeStatKey === stat.key }]"
 *   @click="handleStatClick(stat.key, 'status', stat.filterValue)"
 * />
 * ```
 */

import { ref, type Ref } from 'vue';

/** 统计卡片数据结构（推荐使用） */
export interface StatCardItem {
  /** 唯一标识，用于激活状态判断 */
  key: string;
  /** 图标名称 */
  icon: string;
  /** 标签文本 */
  label: string;
  /** 数值 */
  value: number | string;
  /** 颜色类型 */
  colorType: 'primary' | 'success' | 'warning' | 'error' | 'info';
  /** 点击时筛选的字段值（不传则该卡片不可点击筛选） */
  filterValue?: string | number;
  /** 点击时筛选的字段名（不传则使用 composable 配置的默认字段） */
  filterField?: string;
}

/** useStatCardFilter 配置选项 */
export interface UseStatCardFilterOptions<T extends Record<string, unknown>> {
  /** 搜索表单（响应式 ref） */
  searchForm: Ref<T>;
  /** 默认筛选字段名（如 'status'），可在 StatCardItem 中通过 filterField 覆盖 */
  defaultFilterField?: string;
  /** 触发搜索的回调（通常为 useStandardPage 的 handleSearch 或 useCrudTable 的 refresh） */
  onSearch: () => void | Promise<void>;
  /** 重置搜索表单的回调（可选，用于 Tab 切换时清理） */
  onReset?: () => void | Promise<void>;
}

/** useStatCardFilter 返回值接口 */
export interface UseStatCardFilterReturn {
  /** 当前激活的统计卡片 key（空字符串表示无激活） */
  activeStatKey: Ref<string>;
  /**
   * 处理统计卡片点击
   * - 点击未激活卡片：设置筛选条件 + 激活 + 触发搜索
   * - 点击已激活卡片：取消筛选 + 清除激活 + 触发搜索
   *
   * @param statKey - 卡片唯一标识
   * @param filterField - 筛选字段名（如 'status'）
   * @param filterValue - 筛选字段值（如 'active'）
   */
  handleStatClick: (
    statKey: string,
    filterField: string,
    filterValue: string | number,
  ) => Promise<void>;
  /** 清除当前激活状态（不触发搜索） */
  clearActiveStat: () => void;
  /** 重置：清除激活状态 + 清空筛选字段 + 触发搜索 */
  resetStatFilter: () => Promise<void>;
}

/**
 * 统计卡片点击筛选 Composable
 *
 * 封装"Toggle取消 + 多卡片互斥"交互模式，统一统计卡片点击行为。
 *
 * @param options - 配置选项
 * @returns 统计卡片筛选状态和方法
 *
 * @example
 * ```vue
 * <script setup lang="ts">
 * import { ref } from 'vue';
 * import { useStatCardFilter } from '@/composables/useStatCardFilter';
 *
 * const searchForm = ref({ status: '', keyword: '' });
 * const { activeStatKey, handleStatClick, clearActiveStat } = useStatCardFilter({
 *   searchForm,
 *   onSearch: async () => { await refresh(); },
 * });
 * </script>
 *
 * <template>
 *   <StatCard
 *     v-for="stat in statistics"
 *     :key="stat.key"
 *     :class="['stat-clickable', { 'stat-active': activeStatKey === stat.key }]"
 *     @click="handleStatClick(stat.key, 'status', stat.filterValue)"
 *   />
 * </template>
 * ```
 */
export function useStatCardFilter<T extends Record<string, unknown>>(
  options: UseStatCardFilterOptions<T>,
): UseStatCardFilterReturn {
  const { searchForm, onSearch, onReset } = options;

  /* ===== 激活状态 ===== */
  const activeStatKey = ref<string>('');

  /**
   * 处理统计卡片点击
   * 实现"Toggle取消 + 多卡片互斥"交互模式
   */
  const handleStatClick = async (
    statKey: string,
    filterField: string,
    filterValue: string | number,
  ): Promise<void> => {
    if (activeStatKey.value === statKey) {
      // 点击已激活卡片：取消筛选
      activeStatKey.value = '';
      (searchForm.value as Record<string, unknown>)[filterField] = '';
    } else {
      // 点击未激活卡片：设置筛选 + 激活（互斥：自动清除其他激活）
      activeStatKey.value = statKey;
      (searchForm.value as Record<string, unknown>)[filterField] = filterValue;
    }
    await onSearch();
  };

  /**
   * 清除当前激活状态（不触发搜索）
   * 用于 Tab 切换、重置等场景
   */
  const clearActiveStat = (): void => {
    activeStatKey.value = '';
  };

  /**
   * 重置统计筛选
   * 1. 清除激活状态
   * 2. 调用 onReset 回调（清空搜索表单 + 触发搜索）
   */
  const resetStatFilter = async (): Promise<void> => {
    clearActiveStat();
    await onReset?.();
  };

  return {
    activeStatKey,
    handleStatClick,
    clearActiveStat,
    resetStatFilter,
  };
}
