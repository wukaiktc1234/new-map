import { onUnmounted, ref, watch } from 'vue'

/**
 * 表单草稿：把用户输入实时存到 sessionStorage（F5/崩溃回来不丢）
 *
 * 设计约束：
 *   · 作用域是**当前标签页会话**：sessionStorage 关标签即清，不污染下一个人的班次；
 *   · key 支持传函数：弹窗常驻挂载（order 后到）也能按当前订单切换草稿；
 *   · 提交成功由调用方 clear()，失败/取消保留；
 *   · 反序列化失败（脏数据）自动清空，绝不让脏 storage 把弹窗炸掉。
 */
const PREFIX = 'pos-draft:'

function read<T>(key: string): T | null {
  try {
    const raw = sessionStorage.getItem(PREFIX + key)
    return raw ? (JSON.parse(raw) as T) : null
  } catch {
    sessionStorage.removeItem(PREFIX + key)
    return null
  }
}

export function useDraft<T extends Record<string, unknown>>(
  keyInput: string | (() => string),
  initial: T
) {
  const resolveKey = (): string => (typeof keyInput === 'function' ? keyInput() : keyInput)
  let activeKey = resolveKey()
  const draft = ref<T>({ ...initial, ...(read<Partial<T>>(activeKey) || {}) } as T)

  const persist = (): void => {
    try {
      sessionStorage.setItem(PREFIX + activeKey, JSON.stringify(draft.value))
    } catch {
      /* 存满/被禁用时静默失败：草稿是锦上添花，不能影响主流程 */
    }
  }

  const stopPersist = watch(draft, persist, { deep: true, flush: 'post' })

  // 订单变了（弹窗常驻挂载）→ 切到对应草稿
  const stopKey = watch(
    () => resolveKey(),
    (nextKey, prevKey) => {
      if (nextKey === prevKey) return
      persist()
      activeKey = nextKey
      draft.value = { ...initial, ...(read<Partial<T>>(nextKey) || {}) } as T
    }
  )

  onUnmounted(() => {
    stopPersist()
    stopKey()
  })

  /** 清空草稿（提交成功后调用） */
  const clear = (): void => {
    sessionStorage.removeItem(PREFIX + activeKey)
    draft.value = { ...initial } as T
  }

  const reset = (): void => {
    draft.value = { ...initial } as T
  }

  return { draft, clear, reset, key: () => activeKey }
}
