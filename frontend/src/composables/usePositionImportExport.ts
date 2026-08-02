import { ref } from 'vue'
import * as XLSX from 'xlsx'
import { ElMessage } from 'element-plus'
import { positionApi } from '@/api/hr/position'
import type { PositionFormData } from '@/api/hr/position'
import type { DepartmentDTO } from '@/types/hr'

const IMPORT_HEADERS = ['岗位编码', '岗位名称', '所属部门', '编制人数', '状态', '描述']

function flattenDepartments(tree: DepartmentDTO[]): DepartmentDTO[] {
  return tree.flatMap((d) => [d, ...(d.children ? flattenDepartments(d.children) : [])])
}

function statusValueByLabel(label: string): 'active' | 'inactive' {
  if (label === '启用' || label === 'active') return 'active'
  return 'inactive'
}

export function usePositionImportExport(
  departmentTreeRef: { value: DepartmentDTO[] },
  refresh: () => Promise<void>,
) {
  const importLoading = ref(false)
  const exportLoading = ref(false)

  /** 下载导入模板 */
  function handleDownloadTemplate(): void {
    const data = [
      IMPORT_HEADERS,
      ['', '示例岗位', '示例部门', 1, '启用', '请填写岗位描述'],
    ]
    const ws = XLSX.utils.aoa_to_sheet(data)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '岗位导入模板')
    XLSX.writeFile(wb, '岗位导入模板.xlsx')
  }

  /** 导出当前全部岗位 */
  async function handleExport(): Promise<void> {
    exportLoading.value = true
    try {
      const allPositions = await positionApi.getAll()
      const rows = allPositions.map((item) => ({
        岗位编码: item.positionCode,
        岗位名称: item.positionName,
        所属部门: item.departmentName,
        编制人数: item.maxCount,
        在职人数: item.currentCount,
        状态: item.status === 'active' ? '启用' : '停用',
        描述: item.description,
      }))
      const ws = XLSX.utils.json_to_sheet(rows)
      const wb = XLSX.utils.book_new()
      XLSX.utils.book_append_sheet(wb, ws, '岗位列表')
      XLSX.writeFile(wb, `岗位列表_${new Date().toISOString().slice(0, 10)}.xlsx`)
      ElMessage.success('导出成功')
    } catch (error: unknown) {
      ElMessage.error(error instanceof Error ? error.message : '导出失败')
    } finally {
      exportLoading.value = false
    }
  }

  /** 导入岗位 */
  async function handleImport(file: File): Promise<void> {
    if (!file) {
      ElMessage.error('请选择要导入的文件')
      return
    }
    importLoading.value = true
    try {
      const data = await file.arrayBuffer()
      const workbook = XLSX.read(data, { type: 'array' })
      const sheet = workbook.Sheets[workbook.SheetNames[0]]
      const jsonRows = XLSX.utils.sheet_to_json<Record<string, unknown>>(sheet, { header: 1 })
      if (jsonRows.length <= 1) {
        ElMessage.warning('导入文件为空或缺少数据行')
        return
      }
      const allDepartments = flattenDepartments(departmentTreeRef.value)
      const rows = jsonRows.slice(1) as unknown[][]
      let success = 0
      let fail = 0
      const errors: string[] = []

      for (let i = 0; i < rows.length; i++) {
        const row = rows[i]
        const name = String(row[1] ?? '').trim()
        if (!name) continue
        const deptName = String(row[2] ?? '').trim()
        const dept = allDepartments.find((d) => d.name === deptName)
        if (!dept?.id) {
          fail++
          errors.push(`第 ${i + 2} 行：未找到部门「${deptName}」`)
          continue
        }
        const headCountRaw = Number(row[3] ?? 1)
        const payload: PositionFormData = {
          positionCode: String(row[0] ?? '').trim(),
          positionName: name,
          departmentId: dept.id,
          maxCount: Number.isFinite(headCountRaw) && headCountRaw > 0 ? headCountRaw : 1,
          status: statusValueByLabel(String(row[4] ?? '启用')),
          description: String(row[5] ?? '').trim(),
        }
        try {
          await positionApi.create(payload)
          success++
        } catch (error: unknown) {
          fail++
          errors.push(`第 ${i + 2} 行：${error instanceof Error ? error.message : '创建失败'}`)
        }
      }

      if (success > 0) {
        ElMessage.success(`成功导入 ${success} 条岗位数据`)
        await refresh()
      }
      if (fail > 0) {
        ElMessage.warning(`导入失败 ${fail} 条，${errors.slice(0, 5).join('；')}`)
      }
      if (success === 0 && fail === 0) {
        ElMessage.info('未找到有效数据')
      }
    } catch (error: unknown) {
      ElMessage.error(error instanceof Error ? error.message : '导入失败，请检查文件格式')
    } finally {
      importLoading.value = false
    }
  }

  return {
    importLoading,
    exportLoading,
    handleDownloadTemplate,
    handleExport,
    handleImport,
  }
}
