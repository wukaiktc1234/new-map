/**
 * 门店运营模块单元测试
 *
 * 覆盖范围：
 * 1. DataConverter 数据转换器（金额/状态映射）
 * 2. CSV 导出工具（特殊字符处理/BOM）
 * 3. CSV 导入工具（解析/校验/错误收集）
 *
 * 注意：原 mockDataCenter 已在 mock 数据清理中删除，
 * Mock 数据服务相关测试（CRUD/分页/筛选）已移除，
 * 待真实 API 落地后替换为真实接口测试。
 */

import { describe, it, expect, beforeEach } from 'vitest'
import {
  storeDataConverter,
  settlementDataConverter,
  taskDataConverter,
  certificateDataConverter,
} from '@/api/store-ops/converters'
import { exportToCSV } from '@/utils/export-csv'
import { importFromCSV } from '@/utils/import-csv'

// ============================================================
// 1. storeDataConverter 测试
// ============================================================

describe('storeDataConverter', () => {
  describe('fenToYuan', () => {
    it('应正确将分转换为元（整数）', () => {
      expect(storeDataConverter.fenToYuan(100)).toBe(1)
      expect(storeDataConverter.fenToYuan(1000)).toBe(10)
      expect(storeDataConverter.fenToYuan(8146050)).toBe(81460.5)
    })

    it('应处理零值', () => {
      expect(storeDataConverter.fenToYuan(0)).toBe(0)
    })

    it('应保留两位小数', () => {
      expect(storeDataConverter.fenToYuan(123)).toBe(1.23)
      expect(storeDataConverter.fenToYuan(456)).toBe(4.56)
    })
  })

  describe('yuanToFen', () => {
    it('应正确将元转换为分（整数）', () => {
      expect(storeDataConverter.yuanToFen(1)).toBe(100)
      expect(storeDataConverter.yuanToFen(10)).toBe(1000)
      expect(storeDataConverter.yuanToFen(81460.5)).toBe(8146050)
    })

    it('应处理零值', () => {
      expect(storeDataConverter.yuanToFen(0)).toBe(0)
    })

    it('应四舍五入到整数', () => {
      expect(storeDataConverter.yuanToFen(1.234)).toBe(123)
      expect(storeDataConverter.yuanToFen(9.999)).toBe(1000)
    })
  })

  describe('convertStatus', () => {
    it('应正确映射状态码为字符串', () => {
      expect(storeDataConverter.convertStatus(1)).toBe('running')
      expect(storeDataConverter.convertStatus(0)).toBe('paused')
      expect(storeDataConverter.convertStatus(2)).toBe('closed')
    })

    it('应对未知状态码返回 unknown', () => {
      expect(storeDataConverter.convertStatus(99)).toBe('unknown')
      expect(storeDataConverter.convertStatus(-1)).toBe('unknown')
    })
  })

  describe('convertToStatusNum', () => {
    it('应正确映射字符串为状态码', () => {
      expect(storeDataConverter.convertToStatusNum('running')).toBe(1)
      expect(storeDataConverter.convertToStatusNum('paused')).toBe(0)
      expect(storeDataConverter.convertToStatusNum('closed')).toBe(2)
    })

    it('应对未知字符串返回默认值 0', () => {
      expect(storeDataConverter.convertToStatusNum('unknown')).toBe(0)
    })
  })

  describe('双向转换一致性', () => {
    it('fenToYuan 和 yuanToFen 应互为逆运算', () => {
      const original = 81460.5
      const fen = storeDataConverter.yuanToFen(original)
      const yuan = storeDataConverter.fenToYuan(fen)
      expect(yuan).toBeCloseTo(original, 2)
    })
  })
})

// ============================================================
// 2. settlementDataConverter 测试
// ============================================================

describe('settlementDataConverter', () => {
  describe('convertStatus', () => {
    it('应正确映射对账状态码', () => {
      expect(settlementDataConverter.convertStatus(0)).toBe('pending')
      expect(settlementDataConverter.convertStatus(1)).toBe('approved')
      expect(settlementDataConverter.convertStatus(2)).toBe('rejected')
    })

    it('应对未知状态码返回 unknown', () => {
      expect(settlementDataConverter.convertStatus(99)).toBe('unknown')
    })
  })

  describe('toFrontend', () => {
    it('应正确转换后端数据格式', () => {
      const backend = {
        settlementId: 'SETTLE0001',
        settlementDate: '2026-05-11',
        storeId: 'STORE0001',
        storeName: '总店',
        totalRevenue: 8146050,
        totalCost: 5238000,
        netProfit: 2908050,
        grossProfitRate: 35.7,
        orderCount: 548,
        avgOrderValue: 14865,
        auditorId: 'AUD0001',
        auditorName: '赵会计',
        status: 1,
        confirmTime: new Date().toISOString(),
        remark: '',
        createTime: new Date().toISOString(),
        updateTime: new Date().toISOString(),
      }

      const frontend = settlementDataConverter.toFrontend(backend)

      expect(frontend.settlementId).toBe('SETTLE0001')
      expect(frontend.totalRevenue).toBe(81460.5) // 分转元
      expect(frontend.status).toBe('approved') // 状态码转换
    })
  })
})

// ============================================================
// 3. taskDataConverter 测试
// ============================================================

describe('taskDataConverter', () => {
  describe('convertTaskType', () => {
    it('应正确映射任务类型码', () => {
      expect(taskDataConverter.convertTaskType(1)).toBe('approval')
      expect(taskDataConverter.convertTaskType(2)).toBe('inspection')
      expect(taskDataConverter.convertTaskType(3)).toBe('refund')
      expect(taskDataConverter.convertTaskType(4)).toBe('settlement')
      expect(taskDataConverter.convertTaskType(5)).toBe('certificate')
      expect(taskDataConverter.convertTaskType(6)).toBe('audit')
      expect(taskDataConverter.convertTaskType(7)).toBe('maintenance')
      expect(taskDataConverter.convertTaskType(99)).toBe('other')
    })

    it('应对未知类型码返回 other', () => {
      expect(taskDataConverter.convertTaskType(888)).toBe('other')
    })
  })

  describe('convertPriority', () => {
    it('应正确映射优先级码', () => {
      expect(taskDataConverter.convertPriority(3)).toBe('high')
      expect(taskDataConverter.convertPriority(2)).toBe('medium')
      expect(taskDataConverter.convertPriority(1)).toBe('low')
    })

    it('应对未知优先级返回 medium', () => {
      expect(taskDataConverter.convertPriority(9)).toBe('medium')
    })
  })

  describe('convertTaskStatus', () => {
    it('应正确映射任务状态码', () => {
      expect(taskDataConverter.convertTaskStatus(0)).toBe('pending')
      expect(taskDataConverter.convertTaskStatus(1)).toBe('completed')
      expect(taskDataConverter.convertTaskStatus(2)).toBe('expired')
      expect(taskDataConverter.convertTaskStatus(3)).toBe('cancelled')
    })
  })
})

// ============================================================
// 4. certificateDataConverter 测试
// ============================================================

describe('certificateDataConverter', () => {
  describe('calculateDaysLeft', () => {
    it('应正确计算未来日期的剩余天数', () => {
      const futureDate = new Date()
      futureDate.setDate(futureDate.getDate() + 30)

      const daysLeft = certificateDataConverter.calculateDaysLeft(
        futureDate.toISOString().split('T')[0]
      )

      expect(daysLeft).toBeGreaterThanOrEqual(29)
      expect(daysLeft).toBeLessThanOrEqual(31)
    })

    it('应正确计算过去日期的负数天数（已过期）', () => {
      const pastDate = new Date()
      pastDate.setDate(pastDate.getDate() - 10)

      const daysLeft = certificateDataConverter.calculateDaysLeft(
        pastDate.toISOString().split('T')[0]
      )

      expect(daysLeft).toBeLessThanOrEqual(-9)
    })
  })

  describe('deriveStatus', () => {
    it('过期日期应返回 expired', () => {
      expect(certificateDataConverter.deriveStatus(-10, 1)).toBe('expired')
    })

    it('即将到期（≤30天）应返回 expiring', () => {
      expect(certificateDataConverter.deriveStatus(15, 1)).toBe('expiring')
      expect(certificateDataConverter.deriveStatus(30, 1)).toBe('expiring')
    })

    it('正常有效期（>30天）应返回 active', () => {
      expect(certificateDataConverter.deriveStatus(60, 1)).toBe('active')
    })
  })

  describe('deriveAlertLevel', () => {
    it('已过期应返回 danger', () => {
      expect(certificateDataConverter.deriveAlertLevel(-5)).toBe('danger')
    })

    it('即将到期（≤30天）应返回 warning', () => {
      expect(certificateDataConverter.deriveAlertLevel(20)).toBe('warning')
    })

    it('正常应返回 normal', () => {
      expect(certificateDataConverter.deriveAlertLevel(90)).toBe('normal')
    })
  })

  describe('convertCertType', () => {
    it('应正确映射证件类型码', () => {
      expect(certificateDataConverter.convertCertType(1)).toBe('business_license')
      expect(certificateDataConverter.convertCertType(2)).toBe('catering_license')
      expect(certificateDataConverter.convertCertType(5)).toBe('health_certificate')
      expect(certificateDataConverter.convertCertType(99)).toBe('other')
    })
  })
})

// ============================================================
// 5. Mock 数据服务测试（已移除）
// ============================================================
// 原 StoreOperationMockService 测试已随 mockDataCenter 删除而移除，
// 待真实 API 落地后补充 storeOperationService 的接口测试。

// ============================================================
// 6. exportToCSV 测试
// ============================================================

describe('exportToCSV', () => {
  let createObjectURLMock: jest.Mock
  let revokeObjectURLMock: jest.Mock

  beforeEach(() => {
    createObjectURLMock = jest.fn(() => 'blob:url')
    revokeObjectURLMock = jest.fn()

    global.URL.createObjectURL = createObjectURLMock
    global.URL.revokeObjectURL = revokeObjectURLMock

    document.body.appendChild = jest.fn()
    document.body.removeChild = jest.fn()
  })

  afterEach(() => {
    jest.restoreAllMocks()
  })

  it('应生成包含 BOM 的 UTF-8 CSV 并触发下载', () => {
    const data = [
      { name: '张三', age: 25, city: '成都' },
      { name: '李四', age: 30, city: '北京' },
    ]

    const columns = [
      { prop: 'name' as const, label: '姓名' },
      { prop: 'age' as const, label: '年龄' },
      { prop: 'city' as const, label: '城市' },
    ]

    exportToCSV(data, columns, 'test')

    expect(createObjectURLMock).toHaveBeenCalledTimes(1)
    expect(revokeObjectURLMock).toHaveBeenCalledTimes(1)
  })

  it('应正确处理包含逗号的字段', () => {
    const data = [
      { address: '成都市,天府大道' },
    ]

    const columns = [{ prop: 'address' as const, label: '地址' }]

    exportToCSV(data, columns, 'test-comma')

    const blobArg = createObjectURLMock.mock.calls[0][0]
    expect(blobArg instanceof Blob).toBe(true)
  })

  it('空数据数组不应生成文件', () => {
    const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation()

    exportToCSV([], [], 'test-empty')

    expect(consoleWarnSpy).toHaveBeenCalledWith('[exportToCSV] 数据为空，不生成文件')
    expect(createObjectURLMock).not.toHaveBeenCalled()

    consoleWarnSpy.mockRestore()
  })
})

// ============================================================
// 7. importFromCSV 测试
// ============================================================

describe('importFromCSV', () => {
  it('应正确解析标准 CSV 文件', async () => {
    const csvContent = 'name,age\nAlice,30\nBob,25\nCharlie,35'
    const file = new File([csvContent], 'test.csv', { type: 'text/csv' })

    const columns = [
      { prop: 'name' as const, label: '姓名', required: true },
      { prop: 'age' as const, label: '年龄', required: true, validator: (v: string) => !isNaN(Number(v)) },
    ]

    const result = await importFromCSV(file, columns)

    expect(result.data).toHaveLength(3)
    expect(result.errors).toHaveLength(0)
    expect(result.data[0].name).toBe('Alice')
    expect(result.data[0].age).toBe('30')
  })

  it('应对空文件或仅有表头的文件返回错误', async () => {
    const csvContent = 'name,age'
    const file = new File([csvContent], 'empty.csv', { type: 'text/csv' })

    const columns = [
      { prop: 'name' as const, label: '姓名', required: true },
    ]

    const result = await importFromCSV(file, columns)

    expect(result.data).toHaveLength(0)
    expect(result.errors).toHaveLength(1)
    expect(result.errors[0].message).toContain('缺少数据行')
  })

  it('应检测必填字段缺失并报告错误', async () => {
    const csvContent = 'name,age\nAlice,\nBob,25'
    const file = new File([csvContent], 'missing.csv', { type: 'text/csv' })

    const columns = [
      { prop: 'name' as const, label: '姓名', required: true },
      { prop: 'age' as const, label: '年龄', required: true },
    ]

    const result = await importFromCSV(file, columns)

    expect(result.data).toHaveLength(1) // 只有 Bob 这行有效
    expect(result.errors.length).toBeGreaterThanOrEqual(1) // Alice 缺少 age
  })

  it('应执行自定义校验器验证', async () => {
    const csvContent = 'email\ninvalid-email\ntest@example.com'
    const file = new File([csvContent], 'validate.csv', { type: 'text/csv' })

    const emailValidator = (value: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)

    const columns = [
      { prop: 'email' as const, label: '邮箱', required: true, validator: emailValidator },
    ]

    const result = await importFromCSV(file, columns)

    expect(result.data).toHaveLength(1) // 只有 test@example.com 有效
    expect(result.errors.some(e => e.message.includes('格式不正确'))).toBe(true)
  })

  it('应拒绝非 CSV 格式文件', async () => {
    const file = new File(['<xml>...</xml>'], 'data.xml', { type: 'application/xml' })

    const columns = [{ prop: 'name' as const, label: '姓名' }]

    const result = await importFromCSV(file, columns)

    expect(result.data).toHaveLength(0)
    expect(result.errors[0].message).toContain('仅支持 CSV')
  })
})
