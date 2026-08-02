/**
 * 树形数据扁平化工具函数
 * 将树形结构展平为 { value, label } 数组，常用于 el-select 下拉选项
 */

/** 扁平化后的选项结构 */
export interface TreeFlatOption {
  value: string
  label: string
}

/**
 * 将树形节点数组递归扁平化为 { value, label } 选项列表
 * @param nodes - 树形节点数组
 * @param idKey - ID字段名，默认 'id'
 * @param nameKey - 名称字段名，默认 'name'
 * @returns 扁平化后的选项数组
 */
export function flattenTree<T extends Record<string, unknown>>(
  nodes: T[],
  idKey = 'id',
  nameKey = 'name',
): TreeFlatOption[] {
  const result: TreeFlatOption[] = []
  for (const node of nodes) {
    const id = String(node[idKey] ?? '')
    const label = String(node[nameKey] ?? '')
    if (id) {
      result.push({ value: id, label })
    }
    const children = node.children as T[] | undefined
    if (children?.length) {
      result.push(...flattenTree(children, idKey, nameKey))
    }
  }
  return result
}
