/**
 * CSV 导出工具函数
 *
 * 功能：
 * 1. 导出数据为 UTF-8 BOM 格式的 CSV 文件
 * 2. 自动处理特殊字符（逗号、双引号、换行）
 * 3. 动态文件名生成（含时间戳）
 */

/**
 * 将数据导出为 CSV 文件并触发浏览器下载
 *
 * @param data 数据数组
 * @param columns 列定义 [{ prop, label }]
 * @param filename 文件名（不含扩展名和日期）
 *
 * @example
 * ```typescript
 * exportToCSV(
 *   [{ name: '张三', age: 25 }],
 *   [{ prop: 'name', label: '姓名' }, { prop: 'age', label: '年龄' }],
 *   '用户列表'
 * )
 * // 下载文件名: 用户列表_2026-05-11_143052.csv
 * ```
 */
export function exportToCSV<T>(
  data: T[],
  columns: { prop: keyof T; label: string }[],
  filename: string
): void {
  if (!data || data.length === 0) {
    console.warn('[exportToCSV] 数据为空，不生成文件')
    return
  }

  // 1. 构建 BOM + 表头（UTF-8 BOM 确保 Excel 正确识别中文）
  const BOM = '\uFEFF'
  const header = columns.map(col => col.label).join(',')

  // 2. 构建数据行（处理特殊字符）
  const rows = data.map(row =>
    columns.map(col => {
      const value = row[col.prop]
      const strValue = String(value ?? '')

      // 处理包含逗号、换行、双引号的字段（CSV 规范要求用双引号包裹）
      if (
        strValue.includes(',') ||
        strValue.includes('\n') ||
        strValue.includes('"') ||
        strValue.includes('\r')
      ) {
        return `"${strValue.replace(/"/g, '""')}"`
      }

      return strValue
    }).join(',')
  )

  // 3. 组装完整 CSV 内容
  const csvContent = BOM + header + '\n' + rows.join('\n')

  // 4. 创建 Blob 并触发下载
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.href = url
  link.download = `${filename}_${formatDate(new Date(), 'YYYY-MM-DD_HHmmss')}.csv`

  // 必须将 link 添加到 DOM 才能在某些浏览器中正常工作
  document.body.appendChild(link)
  link.click()

  // 清理 DOM 和释放内存
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/**
 * 格式化日期为指定模式
 *
 * @param date 日期对象
 * @param pattern 格式模式（支持 YYYY-MM-DD HH:mm:ss）
 * @returns 格式化后的日期字符串
 */
function formatDate(date: Date, pattern: string): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return pattern
    .replace('YYYY', String(year))
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}
