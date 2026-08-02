/**
 * Excel 导入导出工具函数
 * 使用 xlsx 库处理 Excel 文件的读写操作
 */

import * as XLSX from 'xlsx'

// ============================================================
// 类型定义
// ============================================================

/** 列头定义 */
export interface ColumnHeader {
  key: string
  label: string
  width?: number
}

/** 导入结果 */
export interface ImportResult {
  success: number
  failed: number
  errors: Array<{ row: number; message: string }>
  data: Record<string, unknown>[]
}

/** 文件大小限制 (5MB) */
const MAX_FILE_SIZE = 5 * 1024 * 1024

/** 支持的文件类型 */
const ACCEPTED_TYPES = [
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', // .xlsx
  'application/vnd.ms-excel', // .xls
  'text/csv', // .csv
]

// ============================================================
// 导出功能
// ============================================================

/**
 * 将数据导出为 Excel 文件并自动下载
 * @param data - 要导出的数据数组
 * @param headers - 列头定义
 * @param filename - 文件名（不含扩展名）
 * @param sheetName - 工作表名称
 */
export function exportToExcel(
  data: Record<string, unknown>[],
  headers: ColumnHeader[],
  filename: string,
  sheetName: string = 'Sheet1'
): void {
  if (!data || data.length === 0) {
    throw new Error('没有数据可导出')
  }

  // 转换数据格式：将 key 转换为中文列名
  const headerLabels = headers.map(h => h.label)
  const headerKeys = headers.map(h => h.key)

  const rows = data.map(item =>
    headerKeys.map(key => {
      const value = item[key]
      // 处理特殊值
      if (value === null || value === undefined) return ''
      if (typeof value === 'object') return JSON.stringify(value)
      return value
    })
  )

  // 创建工作表数据（包含表头）
  const wsData = [headerLabels, ...rows]

  // 创建工作表
  const ws = XLSX.utils.aoa_to_sheet(wsData)

  // 设置列宽
  ws['!cols'] = headers.map(h => ({
    wch: h.width || Math.max(h.label.length + 2, 12),
  }))

  // 创建工作簿
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, sheetName)

  // 生成文件名（添加时间戳）
  const timestamp = new Date().toISOString().slice(0, 10)
  const fullFilename = `${filename}_${timestamp}.xlsx`

  // 导出下载
  XLSX.writeFile(wb, fullFilename)
}

/**
 * 导出多工作表 Excel 文件
 * @param sheets - 工作表配置数组
 * @param filename - 文件名（不含扩展名）
 */
export function exportMultiSheetExcel(
  sheets: Array<{
    data: Record<string, unknown>[]
    headers: ColumnHeader[]
    sheetName: string
  }>,
  filename: string
): void {
  const wb = XLSX.utils.book_new()

  for (const sheet of sheets) {
    if (!sheet.data || sheet.data.length === 0) continue

    const headerLabels = sheet.headers.map(h => h.label)
    const headerKeys = sheet.headers.map(h => h.key)

    const rows = sheet.data.map(item =>
      headerKeys.map(key => {
        const value = item[key]
        if (value === null || value === undefined) return ''
        if (typeof value === 'object') return JSON.stringify(value)
        return value
      })
    )

    const wsData = [headerLabels, ...rows]
    const ws = XLSX.utils.aoa_to_sheet(wsData)

    ws['!cols'] = sheet.headers.map(h => ({
      wch: h.width || Math.max(h.label.length + 2, 12),
    }))

    XLSX.utils.book_append_sheet(wb, ws, sheet.sheetName)
  }

  const timestamp = new Date().toISOString().slice(0, 10)
  const fullFilename = `${filename}_${timestamp}.xlsx`
  XLSX.writeFile(wb, fullFilename)
}

// ============================================================
// 导入功能
// ============================================================

/**
 * 解析上传的 Excel/CSV 文件
 * @param file - 上传的文件对象
 * @returns Promise<ImportResult> 解析结果
 */
export async function parseImportFile(
  file: File,
  options?: {
    requiredFields?: string[]
    headerMapping?: Record<string, string> // 将Excel列名映射到字段名
  }
): Promise<ImportResult> {
  // 验证文件类型
  if (!ACCEPTED_TYPES.includes(file.type)) {
    return {
      success: 0,
      failed: 0,
      errors: [{ row: 0, message: '不支持的文件格式，请使用 .xlsx、.xls 或 .csv 文件' }],
      data: [],
    }
  }

  // 验证文件大小
  if (file.size > MAX_FILE_SIZE) {
    return {
      success: 0,
      failed: 0,
      errors: [{ row: 0, message: `文件过大（${(file.size / 1024 / 1024).toFixed(2)}MB），最大支持5MB` }],
      data: [],
    }
  }

  try {
    // 使用 FileReader 读取文件
    const arrayBuffer = await readFileAsArrayBuffer(file)

    // 解析工作簿
    const workbook = XLSX.read(arrayBuffer, { type: 'array' })

    // 获取第一个工作表
    const firstSheetName = workbook.SheetNames[0]
    const worksheet = workbook.Sheets[firstSheetName]

    // 转换为 JSON 数据
    const jsonData = XLSX.utils.sheet_to_json<Record<string, unknown>>(worksheet, {
      defval: '', // 空单元格默认值为空字符串
    })

    if (!jsonData || jsonData.length === 0) {
      return {
        success: 0,
        failed: 0,
        errors: [{ row: 0, message: '文件中没有有效数据' }],
        data: [],
      }
    }

    // 字段映射转换
    let processedData = jsonData
    if (options?.headerMapping) {
      processedData = jsonData.map(row => {
        const newRow: Record<string, unknown> = {}
        for (const [excelKey, fieldKey] of Object.entries(options.headerMapping!)) {
          if (excelKey in row) {
            newRow[fieldKey] = row[excelKey]
          }
        }
        // 保留未映射的字段
        for (const [key, value] of Object.entries(row)) {
          if (!(key in (options.headerMapping || {}))) {
            newRow[key] = value
          }
        }
        return newRow
      })
    }

    // 验证必填字段
    const errors: Array<{ row: number; message: string }> = []
    const validData: Record<string, unknown>[] = []

    if (options?.requiredFields && options.requiredFields.length > 0) {
      processedData.forEach((row, index) => {
        const rowNum = index + 2 // Excel 行号从2开始（第1行是表头）
        const missingFields = options.requiredFields!.filter(
          field => !row[field] || String(row[field]).trim() === ''
        )

        if (missingFields.length > 0) {
          errors.push({
            row: rowNum,
            message: `缺少必填字段: ${missingFields.join(', ')}`,
          })
        } else {
          validData.push(row)
        }
      })
    } else {
      validData.push(...processedData)
    }

    return {
      success: validData.length,
      failed: errors.length,
      errors,
      data: validData,
    }
  } catch (error) {
    return {
      success: 0,
      failed: 0,
      errors: [{ row: 0, message: `文件解析失败: ${error instanceof Error ? error.message : '未知错误'}` }],
      data: [],
    }
  }
}

/**
 * 解析多工作表 Excel 文件（用于 BOM 导入等场景）
 * @param file - 上传的文件对象
 * @param sheetConfigs - 各工作表的解析配置
 * @returns Promise<Record<string, ImportResult>> 按工作表名称返回解析结果
 */
export async function parseMultiSheetFile(
  file: File,
  sheetConfigs: Array<{
    sheetName: string
    requiredFields?: string[]
    headerMapping?: Record<string, string>
  }>
): Promise<Record<string, ImportResult>> {
  // 验证文件类型
  if (!ACCEPTED_TYPES.includes(file.type)) {
    const errorResult: ImportResult = {
      success: 0,
      failed: 0,
      errors: [{ row: 0, message: '不支持的文件格式，请使用 .xlsx 或 .xls 文件' }],
      data: [],
    }
    return { default: errorResult }
  }

  // 验证文件大小
  if (file.size > MAX_FILE_SIZE) {
    const errorResult: ImportResult = {
      success: 0,
      failed: 0,
      errors: [{ row: 0, message: `文件过大（${(file.size / 1024 / 1024).toFixed(2)}MB），最大支持5MB` }],
      data: [],
    }
    return { default: errorResult }
  }

  try {
    const arrayBuffer = await readFileAsArrayBuffer(file)
    const workbook = XLSX.read(arrayBuffer, { type: 'array' })

    const results: Record<string, ImportResult> = {}

    for (const config of sheetConfigs) {
      const worksheet = workbook.Sheets[config.sheetName]

      if (!worksheet) {
        results[config.sheetName] = {
          success: 0,
          failed: 0,
          errors: [{ row: 0, message: `未找到工作表 "${config.sheetName}"` }],
          data: [],
        }
        continue
      }

      const jsonData = XLSX.utils.sheet_to_json<Record<string, unknown>>(worksheet, {
        defval: '',
      })

      if (!jsonData || jsonData.length === 0) {
        results[config.sheetName] = {
          success: 0,
          failed: 0,
          errors: [{ row: 0, message: `工作表 "${config.sheetName}" 中没有有效数据` }],
          data: [],
        }
        continue
      }

      // 字段映射
      let processedData = jsonData
      if (config.headerMapping) {
        processedData = jsonData.map(row => {
          const newRow: Record<string, unknown> = {}
          for (const [excelKey, fieldKey] of Object.entries(config.headerMapping!)) {
            if (excelKey in row) {
              newRow[fieldKey] = row[excelKey]
            }
          }
          for (const [key, value] of Object.entries(row)) {
            if (!(key in (config.headerMapping || {}))) {
              newRow[key] = value
            }
          }
          return newRow
        })
      }

      // 验证必填字段
      const errors: Array<{ row: number; message: string }> = []
      const validData: Record<string, unknown>[] = []

      if (config.requiredFields?.length) {
        processedData.forEach((row, index) => {
          const rowNum = index + 2
          const missingFields = config.requiredFields!.filter(
            field => !row[field] || String(row[field]).trim() === ''
          )
          if (missingFields.length > 0) {
            errors.push({ row: rowNum, message: `缺少必填字段: ${missingFields.join(', ')}` })
          } else {
            validData.push(row)
          }
        })
      } else {
        validData.push(...processedData)
      }

      results[config.sheetName] = {
        success: validData.length,
        failed: errors.length,
        errors,
        data: validData,
      }
    }

    return results
  } catch (error) {
    const errorResult: ImportResult = {
      success: 0,
      failed: 0,
      errors: [{ row: 0, message: `文件解析失败: ${error instanceof Error ? error.message : '未知错误'}` }],
      data: [],
    }
    return { default: errorResult }
  }
}

// ============================================================
// 辅助函数
// ============================================================

/**
 * 将文件读取为 ArrayBuffer
 */
function readFileAsArrayBuffer(file: File): Promise<ArrayBuffer> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as ArrayBuffer)
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.readAsArrayBuffer(file)
  })
}

/**
 * 格式化文件大小显示
 */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

export interface ExportTableConfig {
  filename: string
  tableData: Record<string, unknown>[]
  columnConfig: Array<{
    prop: string
    label: string
    formatter?: (value: unknown, row: Record<string, unknown>) => string
  }>
}

export function exportTableData(config: ExportTableConfig): void {
  const { filename, tableData, columnConfig } = config
  const headers: ColumnHeader[] = columnConfig.map(col => ({ key: col.prop, label: col.label }))
  const formattedData = tableData.map(row => {
    const formatted: Record<string, unknown> = {}
    for (const col of columnConfig) {
      const rawValue = row[col.prop]
      formatted[col.prop] = col.formatter ? col.formatter(rawValue, row) : rawValue
    }
    return formatted
  })
  exportToExcel(formattedData, headers, filename)
}

export function createStatusFormatter(statusMap: Record<string | number, string>): (value: unknown) => string {
  return (value: unknown): string => {
    const key = value as string | number
    return statusMap[key] ?? String(value)
  }
}
