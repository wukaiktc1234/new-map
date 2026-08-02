/**
 * 部门下拉选项 Composable
 *
 * 提供统一的部门下拉数据加载能力，避免各页面硬编码部门列表。
 * 后端真实 API：/v1/departments/page、/v1/departments/tree、/v1/departments/{id}/children
 *
 * 使用方式：
 * ```ts
 * const { departmentOptions, loading, loadDepartments, getDepartmentName } = useDepartmentOptions()
 * await loadDepartments()
 * ```
 */
import { ref, type Ref } from 'vue'
import { departmentApi } from '@/api/hr/department'
import type { DepartmentDTO } from '@/types/hr'

/** 部门下拉选项（扁平结构，便于 el-select 直接使用） */
export interface DepartmentOption {
  /** 部门 ID */
  id: string
  /** 部门名称 */
  name: string
  /** 部门编码 */
  code?: string
  /** 父部门 ID */
  parentId?: string | null
  /** 部门层级 */
  level?: number
  /** 部门类型（company/department/store/team/office/group） */
  type?: string
  /** 显示文本（含层级缩进，如 "├ 前厅部"） */
  label: string
  /** 原始节点 */
  raw: DepartmentDTO
}

/** 将树结构扁平化为下拉选项列表 */
function flattenTree(
  tree: DepartmentDTO[],
  result: DepartmentOption[] = [],
  depth = 0,
  parentPrefix = '',
): DepartmentOption[] {
  const indent = '├ '.repeat(depth)
  tree.forEach((node, idx) => {
    const isLast = idx === tree.length - 1
    const prefix = depth === 0 ? '' : (isLast ? '└ ' : '├ ')
    const label = `${parentPrefix}${prefix}${node.name}`.trim()
    result.push({
      id: String(node.id ?? ''),
      name: node.name,
      code: node.code,
      parentId: node.parentId,
      level: node.level,
      type: node.type,
      label: label || node.name,
      raw: node,
    })
    if (node.children && node.children.length > 0) {
      const childPrefix = depth === 0 ? '' : '  '.repeat(depth)
      flattenTree(node.children, result, depth + 1, childPrefix)
    }
  })
  return result
}

/**
 * 部门下拉选项 Composable
 *
 * @param autoLoad 是否在创建时自动加载，默认为 false（由调用方控制加载时机）
 */
export function useDepartmentOptions(autoLoad = false): {
  /** 部门选项列表（扁平） */
  departmentOptions: Ref<DepartmentOption[]>
  /** 部门树（原始结构，用于树形控件） */
  departmentTree: Ref<DepartmentDTO[]>
  /** 加载状态 */
  loading: Ref<boolean>
  /** 错误信息 */
  error: Ref<string | null>
  /** 加载部门数据（优先调用 /tree 接口获取完整树，失败时降级到 /page） */
  loadDepartments: () => Promise<void>
  /** 根据 ID 获取部门名称（同步查询，需先调用 loadDepartments） */
  getDepartmentName: (id: string | number | null | undefined) => string
  /** 根据 ID 获取部门选项对象 */
  getDepartmentById: (id: string | number | null | undefined) => DepartmentOption | undefined
} {
  const departmentOptions = ref<DepartmentOption[]>([])
  const departmentTree = ref<DepartmentDTO[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  /** 加载部门数据 */
  async function loadDepartments(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      // 优先调用 tree 接口，便于保留父子层级
      const tree = await departmentApi.getDepartmentTree()
      departmentTree.value = tree || []
      departmentOptions.value = flattenTree(tree || [])
    } catch (err) {
      // tree 失败时降级到 page 接口
      try {
        const list = await departmentApi.getDepartments()
        const flatList: DepartmentOption[] = (list || []).map((d) => ({
          id: String(d.id ?? ''),
          name: d.name,
          code: d.code,
          parentId: d.parentId,
          level: d.level,
          type: d.type,
          label: d.name,
          raw: d,
        }))
        departmentOptions.value = flatList
        departmentTree.value = list || []
      } catch (innerErr) {
        // 两个接口都失败时清空，避免显示陈旧数据
        departmentOptions.value = []
        departmentTree.value = []
        error.value = err instanceof Error ? err.message : '加载部门列表失败'
        // eslint-disable-next-line no-console
        console.error('[useDepartmentOptions] 加载部门数据失败:', innerErr)
      }
    } finally {
      loading.value = false
    }
  }

  /** 根据 ID 获取部门名称 */
  function getDepartmentName(id: string | number | null | undefined): string {
    if (id === null || id === undefined || id === '') return '-'
    const found = departmentOptions.value.find((d) => d.id === String(id))
    return found?.name ?? '-'
  }

  /** 根据 ID 获取部门选项 */
  function getDepartmentById(id: string | number | null | undefined): DepartmentOption | undefined {
    if (id === null || id === undefined || id === '') return undefined
    return departmentOptions.value.find((d) => d.id === String(id))
  }

  if (autoLoad) {
    loadDepartments()
  }

  return {
    departmentOptions,
    departmentTree,
    loading,
    error,
    loadDepartments,
    getDepartmentName,
    getDepartmentById,
  }
}
