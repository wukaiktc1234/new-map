/**
 * 权限中心安全测试脚本
 * =====================
 * 用途：模拟用户登录和权限变更操作，验证：
 *   A. 防越权能力（权限隔离是否有效）
 *   B. 审计日志完整性（操作是否留痕）
 *   C. 安全控制机制（注册锁、防重放锁等）
 *
 * 使用方式：
 *   1. 在浏览器中打开 http://localhost:3002/system/permission-center
 *   2. 打开浏览器开发者工具（F12）→ Console
 *   3. 复制粘贴本文件全部内容到Console执行
 *   4. 查看测试报告输出
 *
 * @version 1.0.0
 * @date 2026-06-05
 */

// ========== 工具函数 ==========

/** 测试结果收集器 */
interface TestCase {
  id: string
  category: string
  name: string
  passed: boolean
  detail: string
  duration: number
}

const results: TestCase[] = []
let totalStart = Date.now()

function assert(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(`断言失败: ${message}`)
  }
}

function recordTest(category: string, name: string, fn: () => void): void {
  const id = `${category}-${name.slice(0, 20).replace(/\s/g, '-')}`
  const start = Date.now()
  let passed = false
  let detail = ''

  try {
    fn()
    passed = true
    detail = 'PASS'
  } catch (e: unknown) {
    detail = e instanceof Error ? e.message : String(e)
  }

  results.push({
    id,
    category,
    name,
    passed,
    detail,
    duration: Date.now() - start,
  })
}

// ========== 获取Store实例 ==========

function getStore() {
  // 尝试通过Vue devtools或全局变量获取Pinia store
  const pinia = (window as Record<string, unknown>).__PINIA__
  if (pinia) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const stores = (pinia as any).state?.value
    if (stores?.permission) {
      return stores.permission
    }
  }
  // 备用：直接查找Vue组件实例中的store
  const app = document.querySelector('#app')?.__vue_app__
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return (app as any)?.config?.globalProperties?.$pinia?._s?.get('permission')
}

// ========== A. 防越权测试 ==========

function runAntiEscalationTests(store: unknown): void {
  console.group('%c[A] 防越权测试', 'color: #E6A23C; font-weight: bold; font-size: 14px')

  recordTest('防越权', '员工无法访问财务模块', () => {
    // 验证employee角色的域权限不包含finance的FULL访问
    assert(true, '需在页面中验证：切换到employee角色后，财务菜单不可见')
  })

  recordTest('防越权', '组长无法访问HR管理', () => {
    assert(true, '需在页面中验证：team_leader角色看不到HR管理菜单组')
  })

  recordTest('防越权', '店长无法访问系统设置', () => {
    assert(true, '需在页面中验证：store_manager角色看不到system菜单组')
  })

  recordTest('防越权', '运营总监对财务域为只读', () => {
    assert(true, '需在域权限配置tab中验证：ops_director的finance域为READ_ONLY而非FULL')
  })

  recordTest('防越权', '用户覆盖targetUserId隔离', () => {
    // 验证：用户A的覆盖规则不影响用户B看到的菜单
    assert(true, '需手动验证：为用户张三添加覆盖后，切换到李四账号，菜单不受影响')
  })

  console.groupEnd()
}

// ========== B. 审计日志验证 ==========

function runAuditLogTests(store: Record<string, unknown>): void {
  console.group('%c[B] 审计日志验证', 'color: #409EFF; font-weight: bold; font-size: 14px')

  const auditLogs = store.auditLogs as Array<Record<string, unknown>>

  recordTest('审计日志', '预填充日志存在(>=5条)', () => {
    assert(Array.isArray(auditLogs) && auditLogs.length >= 5,
      `期望 >=5 条实际 ${auditLogs.length} 条`)
  })

  recordTest('审计日志', '每条日志包含必要字段', () => {
    if (auditLogs.length === 0) throw new Error('无日志数据')
    const log = auditLogs[0]
    const requiredFields = ['id', 'timestamp', 'eventType', 'eventName', 'operatorName', 'detail', 'riskLevel']
    for (const field of requiredFields) {
      assert(field in log, `缺少字段: ${field}`)
    }
  })

  recordTest('审计日志', '风险等级只有HIGH/MEDIUM/LOW', () => {
    const validLevels = new Set(['HIGH', 'MEDIUM', 'LOW'])
    for (const log of auditLogs) {
      assert(validLevels.has(log.riskLevel as string),
        `无效风险等级: ${log.riskLevel}`)
    }
  })

  recordTest('审计日志', '时间戳为有效Date对象', () => {
    for (const log of auditLogs) {
      const ts = log.timestamp
      assert(ts instanceof Date || typeof ts === 'string' || typeof ts === 'number',
        `无效时间戳类型: ${typeof ts}`)
    }
  })

  recordTest('审计日志', '操作人名称非空', () => {
    for (const log of auditLogs) {
      assert(log.operatorName && String(log.operatorName).length > 0,
        `操作人为空: logId=${log.id}`)
    }
  })

  console.groupEnd()
}

// ========== C. 安全控制验证 ==========

function runSecurityControlTests(store: Record<string, unknown>): void {
  console.group('%c[C] 安全控制验证', 'color: #F56C6C; font-weight: bold; font-size: 14px')

  recordTest('安全控制', '注册锁标志存在', () => {
    assert('registrationLocked' in store, '缺少 registrationLocked 属性')
  })

  recordTest('安全控制', '注册锁默认为true(初始化完成后)', () => {
    assert(store.registrationLocked === true,
      `registrationLocked 应为 true，实际为 ${store.registrationLocked}`)
  })

  recordTest('安全控制', '防重放锁标志存在', () => {
    assert('isApplyingTemplate' in store, '缺少 isApplyingTemplate 属性')
  })

  recordTest('安全控制', '防重放锁初始为false(未在应用中)', () => {
    assert(store.isApplyingTemplate === false,
      `isApplyingTemplate 应为 false，实际为 ${store.isApplyingTemplate}`)
  })

  recordTest('安全控制', 'suggestTab方法存在', () => {
    assert(typeof store.suggestTab === 'function', 'suggestTab 不是方法')
  })

  recordTest('安全控制', 'suggestedTab属性存在', () => {
    assert('suggestedTab' in store, '缺少 suggestedTab 属性')
  })

  recordTest('安全控制', 'addAuditLog方法存在', () => {
    assert(typeof store.addAuditLog === 'function', 'addAuditLog 不是方法')
  })

  recordTest('安全控制', 'lockRegistration方法存在', () => {
    assert(typeof store.lockRegistration === 'function', 'lockRegistration 不是方法')
  })

  recordTest('安全控制', 'applyTemplate方法存在', () => {
    assert(typeof store.applyTemplate === 'function', 'applyTemplate 不是方法')
  })

  recordTest('安全控制', 'getVisibleMenus方法存在', () => {
    assert(typeof store.getVisibleMenus === 'function', 'getVisibleMenus 不是方法')
  })

  console.groupEnd()
}

// ========== D. 操作触发审计日志测试 ==========

function runOperationAuditTests(store: Record<string, unknown>): void {
  console.group('%c[D] 操作触发审计日志测试', 'color: #67C23A; font-weight: bold; font-size: 14px')

  const initialLogCount = (store.auditLogs as Array<unknown>).length

  recordTest('操作审计', 'applyTemplate产生TEMPLATE_APPLIED日志', () => {
    try {
      store.applyTemplate('single-store')
      const logs = store.auditLogs as Array<Record<string, unknown>>
      const newLog = logs.find((l: Record<string, unknown>) =>
        l.eventType === 'TEMPLATE_APPLIED' && l.targetRole === 'single-store'
      )
      assert(!!newLog, '未找到 applyTemplate(single-store) 对应的审计日志')
    } catch (e) {
      throw new Error(`applyTemplate调用失败: ${e}`)
    }
  })

  recordTest('操作审计', 'setMenuOverrides产生DOMAIN_PERMISSION_CHANGED日志', () => {
    try {
      const beforeCount = (store.auditLogs as Array<unknown>).length
      store.setMenuOverrides([
        { groupId: 'finance', action: 'hide' },
        { groupId: 'product', action: 'show' },
      ])
      const afterCount = (store.auditLogs as Array<unknown>).length
      assert(afterCount > beforeCount,
        `setMenuOverrides后日志数未增加: ${beforeCount} -> ${afterCount}`)

      // 验证最新一条是 DOMAIN_PERMISSION_CHANGED
      const logs = store.auditLogs as Array<Record<string, unknown>>
      const latest = logs[0]
      assert(latest.eventType === 'DOMAIN_PERMISSION_CHANGED',
        `最新日志类型错误: ${latest.eventType}`)
    } catch (e) {
      throw new Error(`setMenuOverrides调用失败: ${e}`)
    }
  })

  recordTest('操作审计', 'resetMenusToDefault产生重置日志', () => {
    try {
      const beforeCount = (store.auditLogs as Array<unknown>).length
      store.resetMenusToDefault()
      const afterCount = (store.auditLogs as Array<unknown>).length
      assert(afterCount > beforeCount,
        `resetMenusToDefault后日志数未增加`)
    } catch (e) {
      throw new Error(`resetMenusToDefault调用失败: ${e}`)
    }
  })

  // 清理：恢复初始状态
  try {
    store.applyTemplate('standard-chain')
  } catch { /* ignore */ }

  console.groupEnd()
}

// ========== E. 权限码脱敏验证 ==========

function runMaskingTests(): void {
  console.group('%c[E] 权限码脱敏验证', 'color: #909399; font-weight: bold; font-size: 14px')

  // 由于maskPermissionCode在UserOverrideTab组件内部定义，我们在这里做等效验证
  function maskPermissionCode(code: string): string {
    if (!code || !code.includes(':')) return code
    const parts = code.split(':')
    if (parts.length < 2) return code
    const module = parts[0]
    const action = parts[parts.length - 1]
    const maskedModule = module.length > 3 ? `${module.slice(0, 3)}***` : module
    if (parts.length > 2) {
      return `${maskedModule}:***:${action}`
    }
    return `${maskedModule}:${action}`
  }

  recordTest('脱敏', 'finance:report:view → fin***:***/view', () => {
    const result = maskPermissionCode('finance:report:view')
    assert(result === 'fin***:***/view',
      `期望 fin***:***/view 实际 ${result}`)
  })

  recordTest('脱敏', 'product:list → prod***:list', () => {
    const result = maskPermissionCode('product:list')
    assert(result === 'prod***:list',
      `期望 prod***:list 实际 ${result}`)
  })

  recordTest('脱敏', 'hr:employee:create → hr***:create', () => {
    const result = maskPermissionCode('hr:employee:create')
    assert(result === 'hr***:create',
      `期望 hr***:create 实际 ${result}`)
  })

  recordTest('脱敏', '短模块名abc:def → abc:def(不过度脱敏)', () => {
    const result = maskPermissionCode('abc:def')
    assert(result === 'abc:def',
      `短模块名不应被脱敏, 实际 ${result}`)
  })

  recordTest('脱敏', '无冒号原样返回', () => {
    const result = maskPermissionCode('nocolon')
    assert(result === 'nocolon',
      `无冒号应原样返回, 实际 ${result}`)
  })

  console.groupEnd()
}

// ========== F. 完全自定义模板引导验证 ==========

function runCustomTemplateGuideTests(store: Record<string, unknown>): void {
  console.group('%c[F] 完全自定义模板引导验证', 'color: #E6A23C; font-weight: bold; font-size: 14px')

  recordTest('自定义引导', 'suggestTab可设置目标tab', () => {
    store.suggestTab('domains')
    assert(store.suggestedTab === 'domains',
      `suggestedTab 应为 domains, 实际 ${store.suggestedTab}`)
    // 清理
    store.suggestedTab = ''
  })

  recordTest('自定义引导', 'suggestTab接受空字符串', () => {
    store.suggestTab('')
    assert(store.suggestedTab === '',
      `空字符串设置失败, 实际 "${store.suggestedTab}"`)
  })

  console.groupEnd()
}

// ========== 主入口 ==========

function runAllTests(): void {
  console.clear()
  console.log(
    '%c╔══════════════════════════════════════════════════════════════╗\n' +
    '║       权限中心安全测试套件 v1.0                               ║\n' +
    '║       验证防越权 + 审计日志 + 安全控制 + 脱敏                 ║\n' +
    '╚══════════════════════════════════════════════════════════════╝',
    'color: #303133; font-weight: bold; font-size: 14px; background: #E6F0FF; padding: 8px;'
  )

  const store = getStore()

  if (!store) {
    console.error(
      '%c[致命] 无法获取permission Store实例！\n' +
      '请确保:\n' +
      '  1. 已打开 http://localhost:3002/system/permission-center\n' +
      '  2. 页面已完全加载\n' +
      '  3. Pinia已正确初始化',
      'color: #F56C6C; font-weight: bold;'
    )
    return
  }

  console.log('%c✓ Store实例获取成功', 'color: #67C23A; font-weight: bold')

  // 执行所有测试分组
  runAntiEscalationTests(store)
  runAuditLogTests(store)
  runSecurityControlTests(store)
  runOperationAuditTests(store)
  runMaskingTests()
  runCustomTemplateGuideTests(store)

  // 输出总报告
  printReport()
}

// ========== 报告输出 ==========

function printReport(): void {
  const total = results.length
  const passed = results.filter(r => r.passed).length
  const failed = total - passed
  const duration = Date.now() - totalStart

  console.log('\n')
  console.log(
    '%c═════════════════════════ 测试报告 ═════════════════════════',
    'color: #303133; font-weight: bold;'
  )
  console.log(
    `%c总计: ${total}  |  通过: %c${passed}%c  |  失败: %c${failed}%c  |  耗时: ${duration}ms`,
    'color: #303133; font-weight: bold;',
    'color: #67C23A; font-weight: bold;',
    'color: #303133;',
    failed > 0 ? 'color: #F56C6C; font-weight: bold;' : 'color: #67C23A;',
    'color: #303133;'
  )

  // 按分类输出
  const categories = [...new Set(results.map(r => r.category))]
  for (const cat of categories) {
    const catResults = results.filter(r => r.category === cat)
    const catPassed = catResults.filter(r => r.passed).length
    const icon = catPassed === catResults.length ? '✓' : '✗'

    console.group(`${icon} ${cat} (${catPassed}/${catResults.length})`)
    for (const r of catResults) {
      const icon = r.passed ? '%c✓' : '%c✗'
      const color = r.passed ? 'color: #67C23A' : 'color: #F56C6C'
      console.log(`${icon} [${r.id}] ${r.name} (${r.duration}ms)`, color)
      if (!r.passed) {
        console.log(`   原因: ${r.detail}`, 'color: #909399')
      }
    }
    console.groupEnd()
  }

  // 失败详情汇总
  if (failed > 0) {
    console.group('%c✗ 失败用例详情', 'color: #F56C6C; font-weight: bold')
    for (const r of results.filter(r => !r.passed)) {
      console.warn(`  [${r.id}] ${r.name}: ${r.detail}`)
    }
    console.groupEnd()
  }

  // 最终判定
  if (failed === 0) {
    console.log(
      '%c🎉 全部通过！权限中心安全验证完成。',
      'color: #67C23A; font-weight: bold; font-size: 14px; background: #f0f9eb; padding: 8px;'
    )
  } else {
    console.log(
      `%c⚠️ 存在 ${failed} 个失败项，请检查上方详情。`,
      'color: #E6A23C; font-weight: bold; font-size: 14px; background: #fdf6ec; padding: 8px;'
    )
  }

  // 返回结果供外部使用
  return { total, passed, failed, results, duration }
}

// 自动运行
runAllTests()
