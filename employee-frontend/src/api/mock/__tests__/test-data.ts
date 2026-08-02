/**
 * 员工端前端项目完整测试数据集
 *
 * 覆盖范围：
 * - 审批流程（正常/边界/异常）
 * - 工资条（正常/边界/异常）
 * - 排班（正常/边界）
 * - 认证（正常/边界/异常）
 *
 * 所有数据基于源码逻辑推导，用于代码审查和逻辑推演验证。
 */

// ============================================================
// 1. 审批流程测试数据
// ============================================================

/** 审批流程 - 正常场景 */
export const NORMAL_APPROVAL_TESTS = [
  // --- 创建审批 ---
  { name: '创建请假审批', type: 'leave' as const, urgency: 'normal' as const, expectedStatus: 'pending', expectedFlowNodeCount: 4 },
  { name: '创建加班审批', type: 'overtime' as const, urgency: 'normal' as const, expectedStatus: 'pending', expectedFlowNodeCount: 3 },
  { name: '创建报销审批', type: 'reimbursement' as const, urgency: 'normal' as const, expectedStatus: 'pending', expectedFlowNodeCount: 4 },
  { name: '创建调班审批', type: 'swap' as const, urgency: 'normal' as const, expectedStatus: 'pending', expectedFlowNodeCount: 2 },
  { name: '创建出差审批', type: 'travel' as const, urgency: 'normal' as const, expectedStatus: 'pending', expectedFlowNodeCount: 2 },
  { name: '创建领用审批', type: 'requisition' as const, urgency: 'normal' as const, expectedStatus: 'pending', expectedFlowNodeCount: 2 },
  { name: '创建紧急审批', type: 'leave' as const, urgency: 'urgent' as const, expectedStatus: 'pending' },

  // --- 审批全流程 ---
  // 请假流程：发起人(completed) -> 店长(current/pending) -> 人事(pending) -> 归档(pending)
  // 注意：create() 固定生成3个节点（发起人+店长+人事），不含归档节点
  { name: '审批全流程-请假通过', type: 'leave' as const, actions: ['approve', 'approve'] as const, expectedFinalStatus: 'approved' },
  { name: '审批全流程-请假驳回', type: 'leave' as const, actions: ['reject'] as const, expectedFinalStatus: 'rejected' },
  // 报销流程：发起人(completed) -> 店长(current) -> 财务(pending) -> 财务审核(pending)
  // 注意：create() 固定生成3个节点，报销实际需4节点但create()不区分类型
  { name: '审批全流程-报销通过', type: 'reimbursement' as const, actions: ['approve', 'approve'] as const, expectedFinalStatus: 'approved' },
  { name: '审批全流程-报销中间驳回', type: 'reimbursement' as const, actions: ['approve', 'reject'] as const, expectedFinalStatus: 'rejected' },

  // --- 撤回 ---
  { name: '发起人撤回-pending状态', applicantName: '张三', status: 'pending' as const, canWithdraw: true },
  { name: '发起人撤回-processing状态', applicantName: '张三', status: 'processing' as const, canWithdraw: true },
  { name: '发起人撤回-approved状态', applicantName: '张三', status: 'approved' as const, canWithdraw: false },
  { name: '发起人撤回-rejected状态', applicantName: '张三', status: 'rejected' as const, canWithdraw: false },
  { name: '发起人撤回-cancelled状态', applicantName: '张三', status: 'cancelled' as const, canWithdraw: false },
  { name: '非发起人撤回', applicantName: '他人', status: 'pending' as const, canWithdraw: false },
] as const

/** 审批流程 - 边界条件 */
export const BOUNDARY_APPROVAL_TESTS = [
  { name: '空标题审批', title: '', expected: '应被表单验证拦截（create()自动生成标题，空标题不会到达API层）' },
  { name: '超长标题审批', title: 'A'.repeat(200), expected: '应被表单验证拦截或截断（create()自动生成标题，超长标题不会到达API层）' },
  { name: '所有节点同时approve', expected: '应逐个推进，不应跳过（action()每次只推进一个pending节点为current）' },
  { name: '对已完成的审批再次approve', expected: '应报错或无操作（当前无current节点，action()中map不会修改任何节点）' },
  { name: '对已驳回的审批再次reject', expected: '应报错或无操作（当前无current节点，action()中map不会修改任何节点）' },
  { name: '审批ID冲突', expected: 'ap0001 已存在时setApproval()会覆盖（db.approvals[id] = detail 直接赋值）' },
  { name: '种子数据重复调用', expected: 'isDbEmpty()守卫确保只填充一次，重复调用无副作用' },
  { name: 'tombstone过滤', expected: 'getApprovalsByTab()会过滤tombstone中的ID，已删除审批不出现在列表' },
] as const

/** 审批流程 - 异常情况 */
export const ERROR_APPROVAL_TESTS = [
  { name: '查询不存在的审批', id: 'nonexistent', expected: '抛出错误"审批记录不存在"' },
  { name: '对不存在的审批操作', id: 'nonexistent', action: 'approve', expected: '抛出错误"审批记录不存在"' },
  { name: '撤回不存在的审批', id: 'nonexistent', expected: '抛出错误"审批记录不存在"' },
  { name: '非发起人撤回他人审批', id: 'ap0001', applicantName: '他人', expected: '抛出错误"只能撤回自己发起的审批"' },
  { name: '对已通过审批撤回', id: 'existing-approved', status: 'approved', expected: '抛出错误"当前状态不可撤回"' },
] as const

// ============================================================
// 2. 工资条测试数据
// ============================================================

/** 工资条 - 正常场景 */
export const NORMAL_SALARY_TESTS = [
  { name: '获取工资条列表', page: 1, size: 10, expectedTotal: 4 },
  { name: '获取已确认工资条详情', id: 'sal-001', expectedStatus: 'confirmed', hasDetail: true },
  { name: '确认draft工资条', id: 'sal-004', expected: '应报错：工资数据不完整，无法确认（detail为undefined且grossPay为0）' },
  { name: '对已确认工资条提出异议', id: 'sal-001', reason: '加班费计算有误', expectedStatus: 'disputed' },
] as const

/** 工资条 - 边界条件（金额一致性验证） */
export const BOUNDARY_SALARY_TESTS = [
  // sal-001: baseSalary(5000) + overtimePay(800) + bonus(300) + allowance(200) = 6300
  // deductions: 140+70+35+240+85 = 570
  // netPay = 6300 - 570 = 5730
  { name: '金额验证-sal-001', id: 'sal-001', expectedGross: 6300, expectedNet: 5730, expectedDeduction: 570 },
  // sal-002: baseSalary(5000) + overtimePay(1200) + bonus(200) + allowance(200) = 6600
  // deductions: 122+61+31+210+72 = 496
  // netPay = 6600 - 496 = 6104
  { name: '金额验证-sal-002', id: 'sal-002', expectedGross: 6600, expectedNet: 6104, expectedDeduction: 496 },
  // sal-003: baseSalary(4800) + overtimePay(600) + bonus(500) + allowance(200) = 6100
  // deductions: 107+54+27+183+55 = 426
  // netPay = 6100 - 426 = 5674
  { name: '金额验证-sal-003', id: 'sal-003', expectedGross: 6100, expectedNet: 5674, expectedDeduction: 426 },
  // sal-004: draft状态，无detail
  { name: 'draft工资条无detail', id: 'sal-004', expectedDetail: undefined },
  // 分页超出范围
  { name: '分页超出范围', page: 99, size: 10, expectedRecords: 0 },
] as const

/** 工资条 - 异常情况 */
export const ERROR_SALARY_TESTS = [
  { name: '查询不存在的工资条', id: 'nonexistent', expected: '抛出错误"工资条不存在"' },
  { name: '确认不存在的工资条', id: 'nonexistent', expected: '抛出错误"工资条不存在"' },
  { name: '对已确认工资条再次确认', id: 'sal-001', expected: '应报错：当前状态不可确认（status !== "draft"）' },
  { name: '对已异议工资条再次确认', id: 'sal-disputed', expected: '应报错：当前状态不可确认（status !== "draft"）' },
] as const

// ============================================================
// 3. 排班测试数据
// ============================================================

/** 排班 - 正常场景 */
export const SCHEDULE_TESTS = [
  { name: '获取当前周排班', expectedDays: 7 },
  { name: '工作日班次验证', expectedWorkdayShifts: ['morning', 'afternoon', 'evening'] },
  { name: '周末班次验证', expectedWeekendShift: 'rest' },
  { name: '过去日期状态', expectedStatus: 'completed' },
  { name: '未来日期状态', expectedStatus: 'upcoming' },
  { name: '今天日期状态', expectedStatus: 'today' },
  { name: '休息日状态', expectedStatus: 'rest' },
] as const

/** 排班 - 边界条件 */
export const BOUNDARY_SCHEDULE_TESTS = [
  { name: '周偏移为0-当前周', weekOffset: 0, expectedDays: 7 },
  { name: '周偏移为1-下周', weekOffset: 1, expectedDays: 7 },
  { name: '周偏移为-1-上周', weekOffset: -1, expectedDays: 7 },
  { name: '周一为起始日', expectedFirstDay: '周一' },
  { name: '周日为结束日', expectedLastDay: '周日' },
  { name: '工作日班次循环', expectedCycle: 'morning->afternoon->evening->morning->afternoon（5个工作日5种班次）' },
  { name: '统计数据一致性', expected: 'workDays + restDays === 7, morningCount + afternoonCount + eveningCount === workDays' },
] as const

// ============================================================
// 4. 认证测试数据
// ============================================================

/** 认证 - 正常场景 */
export const AUTH_TESTS = [
  { name: '演示模式登录', expectedToken: 'demo_token_', expectedRedirect: '/home' },
  { name: 'Token过期处理', expected: '401响应时清除token并跳转到/' },
  { name: 'Token刷新', expected: '使用refreshToken调用/v1/auth/refresh获取新token（authApi.refreshToken已实现）' },
  { name: '登出', expected: '清除token、重置userInfo、重置permission角色' },
] as const

/** 认证 - 边界条件 */
export const BOUNDARY_AUTH_TESTS = [
  { name: 'Token不存在时的请求', expected: '请求拦截器不添加Authorization header' },
  { name: 'Token存在时的请求', expected: '请求拦截器添加Bearer token到Authorization header' },
  { name: 'localStorage解析失败', expected: 'db.ts loadFromStorage()捕获JSON.parse异常，返回空数据库' },
  { name: 'localStorage存储失败', expected: 'db.ts saveToStorage()捕获localStorage.setItem异常，静默忽略' },
  { name: '业务状态码非0', expected: '响应拦截器显示ElMessage.error并reject' },
] as const

/** 认证 - 异常情况 */
export const ERROR_AUTH_TESTS = [
  { name: '密码登录失败', expected: 'catch块显示alert"登录失败，请检查账号或密码"' },
  { name: '网络错误', expected: '响应拦截器显示ElMessage.error"网络错误"' },
  { name: '403响应', expected: '响应拦截器显示ElMessage.error（非401走通用错误处理）' },
  { name: '500响应', expected: '响应拦截器显示ElMessage.error（非401走通用错误处理）' },
] as const

// ============================================================
// 5. 审批Tab过滤逻辑测试数据
// ============================================================

/** 审批Tab过滤 - 验证各tab的过滤条件 */
export const APPROVAL_TAB_FILTER_TESTS = [
  { name: 'pending tab', tab: 'pending' as const, expectedFilter: 'status === "pending"' },
  { name: 'initiated tab', tab: 'initiated' as const, expectedFilter: 'applicantName === "张三"' },
  { name: 'cc tab', tab: 'cc' as const, expectedFilter: 'applicantName !== "张三" && status !== "cancelled"' },
  { name: 'approved tab', tab: 'approved' as const, expectedFilter: 'status === "approved"' },
  { name: 'rejected tab', tab: 'rejected' as const, expectedFilter: 'status === "rejected"' },
  { name: 'completed tab', tab: 'completed' as const, expectedFilter: 'status === "approved" || status === "rejected" || status === "cancelled"' },
  { name: 'role tab', tab: 'role' as const, expectedFilter: '无过滤（返回全部）' },
] as const

// ============================================================
// 6. 审批action逻辑推演测试数据
// ============================================================

/** 审批action - 逐步推演 */
export const APPROVAL_ACTION_TRACE_TESTS = [
  {
    name: '请假审批完整推演',
    type: 'leave' as const,
    steps: [
      {
        action: 'create' as const,
        expectedNodes: [
          { id: 'fn1', status: 'completed', action: '提交申请' },
          { id: 'fn2', status: 'pending' },
          { id: 'fn3', status: 'pending' },
        ],
        expectedStatus: 'pending',
        note: 'create()生成3节点，首节点completed，其余pending；整体status=pending',
      },
      {
        action: 'approve' as const,
        expectedNodes: [
          { id: 'fn1', status: 'completed', action: '提交申请' },
          { id: 'fn2', status: 'completed', action: '同意' },
          { id: 'fn3', status: 'current' },
        ],
        expectedStatus: 'processing',
        note: 'current节点(fn2不存在，但pending节点fn2变为completed)→fn3从pending推进为current；hasPending=true→processing',
      },
      {
        action: 'approve' as const,
        expectedNodes: [
          { id: 'fn1', status: 'completed', action: '提交申请' },
          { id: 'fn2', status: 'completed', action: '同意' },
          { id: 'fn3', status: 'completed', action: '同意' },
        ],
        expectedStatus: 'approved',
        note: 'current节点fn3变为completed；无pending节点→hasPending=false→approved',
      },
    ],
  },
  {
    name: '请假审批驳回推演',
    type: 'leave' as const,
    steps: [
      {
        action: 'create' as const,
        expectedStatus: 'pending',
        note: '初始状态',
      },
      {
        action: 'reject' as const,
        expectedNodes: [
          { id: 'fn1', status: 'completed', action: '提交申请' },
          { id: 'fn2', status: 'completed', action: '驳回' },
          { id: 'fn3', status: 'completed', action: '无需处理' },
        ],
        expectedStatus: 'rejected',
        note: 'current节点变为completed(驳回)；pending节点变为completed(无需处理)；action=reject→直接rejected',
      },
    ],
  },
] as const

// ============================================================
// 7. 工资条金额一致性详细验证数据
// ============================================================

/** 工资条 - 金额分项验证 */
export const SALARY_AMOUNT_DETAIL_TESTS = [
  {
    id: 'sal-001',
    name: '2025年5月工资条',
    incomeItems: { baseSalary: 5000, overtimePay: 800, bonus: 300, allowance: 200 },
    deductionItems: { pension: 140, medical: 70, unemployment: 35, housingFund: 240, incomeTax: 85 },
    expectedGrossPay: 6300,   // 5000 + 800 + 300 + 200
    expectedTotalDeduction: 570, // 140 + 70 + 35 + 240 + 85
    expectedNetPay: 5730,     // 6300 - 570
  },
  {
    id: 'sal-002',
    name: '2025年4月工资条',
    incomeItems: { baseSalary: 5000, overtimePay: 1200, bonus: 200, allowance: 200 },
    deductionItems: { pension: 122, medical: 61, unemployment: 31, housingFund: 210, incomeTax: 72 },
    expectedGrossPay: 6600,   // 5000 + 1200 + 200 + 200
    expectedTotalDeduction: 496, // 122 + 61 + 31 + 210 + 72
    expectedNetPay: 6104,     // 6600 - 496
  },
  {
    id: 'sal-003',
    name: '2025年3月工资条',
    incomeItems: { baseSalary: 4800, overtimePay: 600, bonus: 500, allowance: 200 },
    deductionItems: { pension: 107, medical: 54, unemployment: 27, housingFund: 183, incomeTax: 55 },
    expectedGrossPay: 6100,   // 4800 + 600 + 500 + 200
    expectedTotalDeduction: 426, // 107 + 54 + 27 + 183 + 55
    expectedNetPay: 5674,     // 6100 - 426
  },
  {
    id: 'sal-004',
    name: '2025年6月工资条（draft）',
    incomeItems: null,
    deductionItems: null,
    expectedGrossPay: 0,
    expectedTotalDeduction: 0,
    expectedNetPay: 0,
    note: 'draft状态，grossAmount=0, netAmount=0, 无detail',
  },
] as const
