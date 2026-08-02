import * as XLSX from 'xlsx'

/** 班次类型定义 */
export interface ShiftTypeDef {
  code: string
  name: string
  startTime: string
  endTime: string
}

/** 排班导入列定义 */
export interface ScheduleImportColumn {
  key: string
  label: string
  width: number
  required: boolean
}

/** 排班导入行数据 */
export interface ScheduleImportRow {
  date: string
  employeeName: string
  shiftCode: string
  startTime: string
  endTime: string
  remark: string
}

/** 排班导入错误 */
export interface ScheduleImportError {
  row: number
  field: string
  message: string
  value: string
}

/** 排班导入结果 */
export interface ScheduleImportResult {
  successCount: number
  failCount: number
  data: ScheduleImportRow[]
  errors: ScheduleImportError[]
}

/** 导入列定义 */
export const SCHEDULE_IMPORT_COLUMNS: ScheduleImportColumn[] = [
  { key: 'date', label: '日期', width: 14, required: true },
  { key: 'employeeName', label: '员工姓名', width: 14, required: true },
  { key: 'shiftCode', label: '班次编码', width: 14, required: true },
  { key: 'startTime', label: '上班时间', width: 10, required: false },
  { key: 'endTime', label: '下班时间', width: 10, required: false },
  { key: 'remark', label: '备注', width: 20, required: false },
]

/** 默认班次类型对照表 */
export const DEFAULT_SHIFT_TYPES: ShiftTypeDef[] = [
  { code: 'morning', name: '早班', startTime: '06:00', endTime: '14:00' },
  { code: 'noon', name: '中班', startTime: '11:00', endTime: '19:00' },
  { code: 'evening', name: '晚班', startTime: '16:00', endTime: '24:00' },
  { code: 'full', name: '全天班', startTime: '06:00', endTime: '22:00' },
  { code: 'off', name: '休息', startTime: '-', endTime: '-' },
]

/** 最大导入行数 */
const MAX_IMPORT_ROWS = 500

/** 日期正则 YYYY-MM-DD */
const DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/

/** 时间正则 HH:mm */
const TIME_REGEX = /^\d{2}:\d{2}$/

/**
 * 生成排班导入模板Excel文件并下载
 * 模板格式与排班表格视图一致：员工×日期矩阵
 * @param shiftTypes 可选的自定义班次类型列表
 * @param yearMonth 可选的年月，格式 YYYY-MM，默认当前月
 * @param employeeNames 可选的员工姓名列表
 */
export function downloadScheduleTemplate(
  shiftTypes?: ShiftTypeDef[],
  yearMonth?: string,
  employeeNames?: string[],
): void {
  const types = shiftTypes || DEFAULT_SHIFT_TYPES

  // 解析年月
  const now = new Date()
  const [year, month] = yearMonth
    ? yearMonth.split('-').map(Number)
    : [now.getFullYear(), now.getMonth() + 1]
  const daysInMonth = new Date(year, month, 0).getDate()
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  const monthLabel = `${year}年${String(month).padStart(2, '0')}月`

  // 员工列表（默认5个示例员工）
  const employees = employeeNames?.length
    ? employeeNames
    : ['员工1', '员工2', '员工3', '员工4', '员工5']

  const workbook = XLSX.utils.book_new()

  // ==================== Sheet 1: 排班数据（矩阵格式） ====================
  // 第一行：月份标题（合并单元格）
  const titleRow = [`${monthLabel}排班表`, ...Array(daysInMonth).fill('')]

  // 第二行：日期表头
  const headerRow = ['员工 \\ 日期']
  for (let d = 1; d <= daysInMonth; d++) {
    const date = new Date(year, month - 1, d)
    const weekday = weekdays[date.getDay()]
    headerRow.push(`${d}日/${weekday}`)
  }

  // 数据行：每个员工一行，用缩略班次标签填充示例
  const shortLabelMap: Record<string, string> = {
    morning: '早', noon: '中', evening: '晚', full: '全', off: '休',
  }

  const dataRows = employees.map((emp, empIdx) => {
    const row = [emp]
    for (let d = 1; d <= daysInMonth; d++) {
      const date = new Date(year, month - 1, d)
      const dayOfWeek = date.getDay()
      // 示例数据：周末休息，工作日轮班
      if (dayOfWeek === 0 || dayOfWeek === 6) {
        row.push('休')
      } else {
        const shiftIdx = empIdx % types.length
        const code = types[shiftIdx].code
        row.push(shortLabelMap[code] || types[shiftIdx].name)
      }
    }
    return row
  })

  const allRows = [titleRow, headerRow, ...dataRows]
  const ws = XLSX.utils.aoa_to_sheet(allRows)

  // 设置列宽
  ws['!cols'] = [
    { wch: 12 }, // 员工名列
    ...Array(daysInMonth).fill({ wch: 6 }), // 日期列
  ]

  // 合并标题行
  ws['!merges'] = [
    { s: { r: 0, c: 0 }, e: { r: 0, c: daysInMonth } },
  ]

  XLSX.utils.book_append_sheet(workbook, ws, '排班数据')

  // ==================== Sheet 2: 班次对照表 ====================
  const shiftHeaders = ['编码', '缩写', '班次名称', '默认上班时间', '默认下班时间']
  const shiftRows = [
    shiftHeaders,
    ...types.map((t) => [t.code, shortLabelMap[t.code] || t.name.charAt(0), t.name, t.startTime, t.endTime]),
  ]

  const shiftSheet = XLSX.utils.aoa_to_sheet(shiftRows)
  shiftSheet['!cols'] = [
    { wch: 12 },
    { wch: 8 },
    { wch: 12 },
    { wch: 14 },
    { wch: 14 },
  ]
  XLSX.utils.book_append_sheet(workbook, shiftSheet, '班次对照表')

  // ==================== Sheet 3: 填写说明 ====================
  const instructions = [
    ['排班模板填写说明'],
    [''],
    ['【填写步骤】'],
    ['1. 在"排班数据"工作表中填写排班信息'],
    ['2. 第一列为员工姓名，后续列为对应日期的班次'],
    ['3. 班次填写方式：使用缩写（早/中/晚/全/休）或编码（morning/noon/evening/full/off）均可'],
    ['4. 填写完成后保存文件，在系统中选择导入'],
    [''],
    ['【班次填写对照】'],
    ['缩写', '编码', '班次名称', '时间段'],
    ...types.map((t) => [shortLabelMap[t.code] || t.name.charAt(0), t.code, t.name, `${t.startTime}-${t.endTime}`]),
    [''],
    ['【注意事项】'],
    ['1. 员工姓名必须与系统中已注册的员工一致'],
    ['2. 日期列顺序与日历一致，请勿调整列顺序'],
    ['3. 空白单元格表示未排班'],
    ['4. 法定节假日列已标注星期，请注意安排休息'],
    ['5. 可新增员工行，但请勿修改表头行'],
    ['6. 导入时系统会自动识别缩写和编码两种格式'],
  ]

  const instructionSheet = XLSX.utils.aoa_to_sheet(instructions)
  instructionSheet['!cols'] = [
    { wch: 10 },
    { wch: 14 },
    { wch: 14 },
    { wch: 20 },
  ]
  XLSX.utils.book_append_sheet(workbook, instructionSheet, '填写说明')

  XLSX.writeFile(workbook, `排班模板_${year}-${String(month).padStart(2, '0')}.xlsx`)
}

/**
 * 解析导入的排班Excel文件
 * 支持两种格式：
 * 1. 矩阵格式（员工×日期）：与下载模板和表格视图一致
 * 2. 长格式（日期/员工/班次编码）：兼容旧格式
 * @param file Excel文件
 * @param shiftTypes 有效的班次类型列表（用于校验班次编码）
 * @param yearMonth 当前选中的年月，格式 YYYY-MM，用于矩阵格式日期解析
 * @returns 导入结果
 */
export async function parseScheduleExcel(
  file: File,
  shiftTypes?: ShiftTypeDef[],
  yearMonth?: string,
): Promise<ScheduleImportResult> {
  const types = shiftTypes || DEFAULT_SHIFT_TYPES
  const validCodes = new Set(types.map((t) => t.code))

  // 构建班次名称/缩写到编码的映射
  const shiftNameToCode: Record<string, string> = {}
  const shortLabelMap: Record<string, string> = {
    morning: '早', noon: '中', evening: '晚', full: '全', off: '休',
  }
  for (const t of types) {
    shiftNameToCode[t.code.toLowerCase()] = t.code
    shiftNameToCode[t.name] = t.code
    shiftNameToCode[shortLabelMap[t.code]] = t.code
    // 也支持"早班"→morning
    shiftNameToCode[t.name.replace('班', '')] = t.code
  }

  const data: ScheduleImportRow[] = []
  const errors: ScheduleImportError[] = []

  return new Promise((resolve) => {
    const reader = new FileReader()

    reader.onload = (e) => {
      try {
        const arrayBuffer = e.target?.result
        const workbook = XLSX.read(arrayBuffer, { type: 'array' })

        // 查找排班数据工作表
        const sheetName =
          workbook.SheetNames.find(
            (name) => name === '排班数据' || name.includes('排班'),
          ) || workbook.SheetNames[0]

        if (!sheetName) {
          errors.push({ row: 0, field: '文件', message: 'Excel文件中没有工作表', value: '' })
          resolve({ successCount: 0, failCount: errors.length, data, errors })
          return
        }

        const worksheet = workbook.Sheets[sheetName]
        if (!worksheet) {
          errors.push({ row: 0, field: '文件', message: `无法读取工作表: ${sheetName}`, value: '' })
          resolve({ successCount: 0, failCount: errors.length, data, errors })
          return
        }

        const jsonData = XLSX.utils.sheet_to_json<unknown[]>(worksheet, {
          header: 1,
          defval: '',
        })

        if (jsonData.length < 2) {
          errors.push({ row: 0, field: '文件', message: '工作表没有数据', value: '' })
          resolve({ successCount: 0, failCount: errors.length, data, errors })
          return
        }

        // 检测格式：矩阵格式 vs 长格式
        const firstRow = (Array.isArray(jsonData[0]) ? jsonData[0] : []) as string[]
        const secondRow = jsonData.length > 1 ? (Array.isArray(jsonData[1]) ? jsonData[1] : []) as string[] : []

        const isMatrixFormat = detectMatrixFormat(firstRow, secondRow)

        if (isMatrixFormat) {
          // 矩阵格式解析
          parseMatrixFormat(jsonData as string[][], types, validCodes, shiftNameToCode, yearMonth, data, errors)
        } else {
          // 长格式解析（兼容旧格式）
          parseLongFormat(jsonData as string[][], types, validCodes, data, errors)
        }

        resolve({ successCount: data.length, failCount: errors.length, data, errors })
      } catch (error) {
        errors.push({
          row: 0,
          field: '文件',
          message: `Excel文件解析失败: ${error instanceof Error ? error.message : '未知错误'}`,
          value: '',
        })
        resolve({ successCount: 0, failCount: errors.length, data, errors })
      }
    }

    reader.onerror = () => {
      errors.push({ row: 0, field: '文件', message: '文件读取失败', value: '' })
      resolve({ successCount: 0, failCount: errors.length, data, errors })
    }

    reader.readAsArrayBuffer(file)
  })
}

/**
 * 检测是否为矩阵格式（员工×日期）
 * 特征：表头包含"1日/"或"日期"等日期列标识
 */
function detectMatrixFormat(firstRow: string[], secondRow: string[]): boolean {
  const allCells = [...firstRow, ...secondRow]
  // 检查是否有"X日/"格式的列头
  return allCells.some(cell => /(\d{1,2})日\/[一二三四五六日]/.test(String(cell)))
}

/**
 * 解析矩阵格式（员工×日期）
 * 第1行：月份标题（可选）
 * 第2行：表头（员工\日期, 1日/一, 2日/二, ...）
 * 第3行起：数据行（员工名, 班次, 班次, ...）
 */
function parseMatrixFormat(
  rows: string[][],
  types: ShiftTypeDef[],
  validCodes: Set<string>,
  shiftNameToCode: Record<string, string>,
  yearMonth: string | undefined,
  data: ScheduleImportRow[],
  errors: ScheduleImportError[],
): void {
  // 确定表头行（跳过标题行）
  let headerRowIndex = 0
  for (let i = 0; i < Math.min(rows.length, 3); i++) {
    const row = rows[i]
    if (row && row.some(cell => /(\d{1,2})日/.test(String(cell)))) {
      headerRowIndex = i
      break
    }
  }

  const headerRow = rows[headerRowIndex]
  if (!headerRow) {
    errors.push({ row: 0, field: '表头', message: '无法找到日期表头行', value: '' })
    return
  }

  // 解析年月
  const now = new Date()
  let year = now.getFullYear()
  let month = now.getMonth() + 1
  if (yearMonth) {
    const parts = yearMonth.split('-').map(Number)
    year = parts[0]
    month = parts[1]
  } else {
    // 尝试从标题行解析
    const titleRow = rows[0]?.[0] || ''
    const titleMatch = String(titleRow).match(/(\d{4})年(\d{1,2})月/)
    if (titleMatch) {
      year = parseInt(titleMatch[1], 10)
      month = parseInt(titleMatch[2], 10)
    }
  }

  // 解析日期列
  const dateColumns: Array<{ colIndex: number; date: string }> = []
  for (let c = 1; c < headerRow.length; c++) {
    const cellText = String(headerRow[c] || '').trim()
    const dayMatch = cellText.match(/(\d{1,2})日/)
    if (dayMatch) {
      const day = parseInt(dayMatch[1], 10)
      const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
      dateColumns.push({ colIndex: c, date: dateStr })
    }
  }

  if (dateColumns.length === 0) {
    errors.push({ row: headerRowIndex + 1, field: '表头', message: '无法解析日期列', value: '' })
    return
  }

  // 解析数据行
  for (let r = headerRowIndex + 1; r < rows.length; r++) {
    const row = rows[r]
    if (!row || row.length < 2) continue

    const employeeName = String(row[0] || '').trim()
    if (!employeeName) continue

    for (const col of dateColumns) {
      const cellValue = String(row[col.colIndex] || '').trim()
      if (!cellValue) continue // 空白=未排班

      // 解析班次：支持缩写、编码、全名
      const shiftCode = shiftNameToCode[cellValue] || shiftNameToCode[cellValue.toLowerCase()]
      if (!shiftCode) {
        errors.push({
          row: r + 1,
          field: `${employeeName}-${col.date}`,
          message: `无法识别班次: "${cellValue}"`,
          value: cellValue,
        })
        continue
      }

      const shiftDef = types.find(t => t.code === shiftCode)
      data.push({
        date: col.date,
        employeeName,
        shiftCode,
        startTime: shiftDef?.startTime || '',
        endTime: shiftDef?.endTime || '',
        remark: '',
      })
    }
  }
}

/**
 * 解析长格式（日期/员工/班次编码）
 * 兼容旧版模板
 */
function parseLongFormat(
  rows: string[][],
  types: ShiftTypeDef[],
  validCodes: Set<string>,
  data: ScheduleImportRow[],
  errors: ScheduleImportError[],
): void {
  const headerRow = rows[0]
  const headerValidation = validateHeaders(headerRow as string[])
  if (headerValidation.length > 0) {
    errors.push(...headerValidation)
    return
  }

  for (let i = 1; i < rows.length; i++) {
    const row = rows[i]
    if (!row) continue
    const excelRowNum = i + 1

    if (i > MAX_IMPORT_ROWS) {
      errors.push({ row: excelRowNum, field: '全部', message: `超过最大导入限制(${MAX_IMPORT_ROWS}条)`, value: '' })
      break
    }

    if (row.every((cell) => cell === '' || cell == null)) continue

    const rowErrors = validateAndParseRow(row as unknown[], excelRowNum, validCodes)
    if (rowErrors.errors.length > 0) {
      errors.push(...rowErrors.errors)
    } else if (rowErrors.data) {
      data.push(rowErrors.data)
    }
  }
}

/**
 * 验证表头是否完整
 * @param headerRow 表头行数据
 * @returns 错误列表
 */
function validateHeaders(headerRow: string[]): ScheduleImportError[] {
  const errors: ScheduleImportError[] = []
  const cleanHeaders = headerRow.map((h) => String(h).replace('*', '').trim())

  for (const col of SCHEDULE_IMPORT_COLUMNS) {
    const found = cleanHeaders.some((h) => h === col.label)
    if (col.required && !found) {
      errors.push({
        row: 1,
        field: '表头',
        message: `缺少必填列: ${col.label}`,
        value: cleanHeaders.join(', '),
      })
    }
  }

  return errors
}

/**
 * 校验并解析单行数据
 * @param row 行数据
 * @param rowNum 行号（Excel行号）
 * @param validCodes 有效班次编码集合
 * @returns 解析结果
 */
function validateAndParseRow(
  row: unknown[],
  rowNum: number,
  validCodes: Set<string>,
): { data?: ScheduleImportRow; errors: ScheduleImportError[] } {
  const errors: ScheduleImportError[] = []

  const getCell = (index: number): string => String(row[index] ?? '').trim()

  const date = getCell(0)
  const employeeName = getCell(1)
  const shiftCode = getCell(2)
  const startTime = getCell(3)
  const endTime = getCell(4)
  const remark = getCell(5)

  // 校验日期（必填）
  if (!date) {
    errors.push({
      row: rowNum,
      field: '日期',
      message: '日期不能为空',
      value: date,
    })
  } else if (!DATE_REGEX.test(date)) {
    errors.push({
      row: rowNum,
      field: '日期',
      message: '日期格式不正确，应为 YYYY-MM-DD',
      value: date,
    })
  }

  // 校验员工姓名（必填）
  if (!employeeName) {
    errors.push({
      row: rowNum,
      field: '员工姓名',
      message: '员工姓名不能为空',
      value: employeeName,
    })
  } else if (employeeName.length > 50) {
    errors.push({
      row: rowNum,
      field: '员工姓名',
      message: '员工姓名长度不能超过50个字符',
      value: employeeName,
    })
  }

  // 校验班次编码（必填）
  if (!shiftCode) {
    errors.push({
      row: rowNum,
      field: '班次编码',
      message: '班次编码不能为空',
      value: shiftCode,
    })
  } else if (!validCodes.has(shiftCode)) {
    errors.push({
      row: rowNum,
      field: '班次编码',
      message: `无效的班次编码: ${shiftCode}，有效值: ${[...validCodes].join(', ')}`,
      value: shiftCode,
    })
  }

  // 校验上班时间（选填）
  if (startTime && !TIME_REGEX.test(startTime)) {
    errors.push({
      row: rowNum,
      field: '上班时间',
      message: '时间格式不正确，应为 HH:mm',
      value: startTime,
    })
  }

  // 校验下班时间（选填）
  if (endTime && !TIME_REGEX.test(endTime)) {
    errors.push({
      row: rowNum,
      field: '下班时间',
      message: '时间格式不正确，应为 HH:mm',
      value: endTime,
    })
  }

  // 校验备注（选填）
  if (remark && remark.length > 200) {
    errors.push({
      row: rowNum,
      field: '备注',
      message: '备注长度不能超过200个字符',
      value: `${remark.substring(0, 50)}...`,
    })
  }

  if (errors.length > 0) {
    return { errors }
  }

  return {
    data: { date, employeeName, shiftCode, startTime, endTime, remark },
    errors,
  }
}