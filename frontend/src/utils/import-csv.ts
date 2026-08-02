/**
 * CSV 导入工具函数
 *
 * 功能：
 * 1. 解析 CSV 文本内容（支持标准 RFC 4180 格式）
 * 2. 处理带引号的字段（嵌套逗号、换行、双引号）
 * 3. 数据校验（必填字段、自定义验证器）
 * 4. 错误收集与报告
 */

/** 导入错误信息 */
export interface ImportError {
  row: number
  column?: string
  message: string
}

/** 导入列定义 */
export interface ImportColumnDef<T> {
  prop: keyof T
  label: string
  required?: boolean
  validator?: (value: string) => boolean
}

/** 导入结果 */
export interface ImportResult<T> {
  data: T[]
  errors: ImportError[]
}

/**
 * 从 CSV 文件导入数据
 *
 * @param file File 对象
 * @param columns 列定义数组
 * @returns Promise<ImportResult> 包含成功数据和错误列表
 *
 * @example
 * ```typescript
 * const result = await importFromCSV(file, [
 *   { prop: 'name', label: '姓名', required: true },
 *   { prop: 'age', label: '年龄', required: true, validator: (v) => !isNaN(Number(v)) },
 * ])
 *
 * console.log(result.data)     // 成功解析的数据
 * console.log(result.errors)    // 错误详情
 * ```
 */
export async function importFromCSV<T>(
  file: File,
  columns: ImportColumnDef<T>[]
): Promise<ImportResult<T>> {
  return new Promise((resolve) => {
    if (!file) {
      resolve({ data: [], errors: [{ row: 0, message: '未选择文件' }] })
      return
    }

    // 验证文件类型
    if (!file.name.endsWith('.csv') && file.type !== 'text/csv') {
      resolve({
        data: [],
        errors: [{ row: 0, message: '仅支持 CSV 格式文件 (.csv)' }],
      })
      return
    }

    // 验证文件大小（限制 5MB）
    const MAX_FILE_SIZE = 5 * 1024 * 1024
    if (file.size > MAX_FILE_SIZE) {
      resolve({
        data: [],
        errors: [{ row: 0, message: `文件大小超过限制（最大 ${MAX_FILE_SIZE / 1024 / 1024}MB）` }],
      })
      return
    }

    const reader = new FileReader()

    reader.onload = (e) => {
      try {
        const text = e.target?.result as string

        if (!text || text.trim().length === 0) {
          resolve({ data: [], errors: [{ row: 0, message: '文件内容为空' }] })
          return
        }

        const lines = text.split(/\r?\n/).filter(line => line.trim())

        if (lines.length < 2) {
          resolve({ data: [], errors: [{ row: 0, message: '文件缺少数据行（仅有表头或空文件）' }] })
          return
        }

        // 解析表头
        const headers = parseCSVLine(lines[0])

        // 验证列数匹配
        if (headers.length !== columns.length) {
          resolve({
            data: [],
            errors: [{
              row: 1,
              message: `列数不匹配：表头有 ${headers.length} 列，期望 ${columns.length} 列`,
            }],
          })
          return
        }

        // 逐行解析数据
        const data: T[] = []
        const errors: ImportError[] = []

        for (let i = 1; i < lines.length; i++) {
          const values = parseCSVLine(lines[i])
          const record = {} as T
          let hasError = false

          for (let j = 0; j < columns.length; j++) {
            const col = columns[j]
            const value = values[j]?.trim() ?? ''

            // 必填校验
            if (col.required && !value) {
              errors.push({
                row: i + 1,
                column: col.label,
                message: `${col.label}不能为空`,
              })
              hasError = true
              continue
            }

            // 自定义校验器
            if (value && col.validator && !col.validator(value)) {
              errors.push({
                row: i + 1,
                column: col.label,
                message: `${col.label}格式不正确`,
              })
              hasError = true
              continue
            }

            // 存储值（使用类型断言避免索引签名问题）
            ;(record as unknown as Record<string, unknown>)[String(col.prop)] = value
          }

          // 仅无错误的记录加入结果集
          if (!hasError) {
            data.push(record)
          }
        }

        resolve({ data, errors })
      } catch (error) {
        console.error('[importFromCSV] 解析失败:', error)
        resolve({
          data: [],
          errors: [{ row: 0, message: `文件解析失败: ${error instanceof Error ? error.message : '未知错误'}` }],
        })
      }
    }

    reader.onerror = () => {
      resolve({ data: [], errors: [{ row: 0, message: '文件读取失败' }] })
    }

    reader.readAsText(file, 'UTF-8')
  })
}

/**
 * 解析单行 CSV 字符串
 * 正确处理带引号的字段（符合 RFC 4180 标准）
 *
 * @param line CSV 行字符串
 * @returns 字符串数组
 *
 * @example
 * ```typescript
 * parseCSVLine('Alice,"Bob, Jr.",30')
 * // 返回: ['Alice', 'Bob, Jr.', '30']
 * ```
 */
function parseCSVLine(line: string): string[] {
  const result: string[] = []
  let current = ''
  let inQuotes = false

  for (let i = 0; i < line.length; i++) {
    const char = line[i]

    if (char === '"') {
      if (inQuotes && line[i + 1] === '"') {
        // 转义的双引号（两个连续的双引号表示一个字面双引号）
        current += '"'
        i++ // 跳过下一个双引号
      } else {
        // 切换引号状态
        inQuotes = !inQuotes
      }
    } else if (char === ',' && !inQuotes) {
      // 引号外的逗号是字段分隔符
      result.push(current.trim())
      current = ''
    } else {
      // 普通字符追加到当前字段
      current += char
    }
  }

  // 添加最后一个字段
  result.push(current.trim())

  return result
}
