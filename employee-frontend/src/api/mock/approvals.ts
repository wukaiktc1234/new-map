/**
 * 审批Mock数据生成器（确定性版本）
 *
 * 预定义28条真实场景审批数据，覆盖：
 * - 全部6种审批类型：leave / overtime / swap / travel / reimbursement / requisition
 * - 全部5种状态：pending(4) / processing(3) / approved(8) / rejected(3) / cancelled(2)
 * - 完整角色链路：发起人 → L2主管 → L3店长/L4区域经理 → 归档
 * - 7条当前用户("我")发起的申请，分布各状态
 * - 真实中文评论、表单字段、时间戳
 * - 层级化权限体系：每条数据包含 applicantLevel 字段
 */
import type { ApprovalDetail, ApprovalType, ApprovalStatus, FlowNode, Comment, FormField, UrgencyLevel } from '@/types/approval'
import { UserLevel } from '@/types/permission'
import { isDbEmpty, setApproval } from './db'

function daysAgo(days: number, hour = 9, minute = 0): string {
  const d = new Date()
  d.setDate(d.getDate() - days)
  d.setHours(hour, minute, 0, 0)
  return d.toISOString()
}

function fmt(iso: string): string {
  return iso.slice(5, 16).replace('T', ' ')
}

/** Mock 原始审批数据类型（applicantLevel 可选，部分场景由后端填充） */
type RawApproval = Omit<ApprovalDetail, 'attachments'> & { applicantLevel?: number }

const MANAGER_NAME = '王店长'
/** 当前登录用户姓名（模拟数据中使用真实姓名，避免审批流程中出现"我"） */
const CURRENT_USER_NAME = '张三'

/** Mock 审批数据（类型断言：Mock 字段可能与 RawApproval 存在细微差异，使用时由转换器归一化） */
// eslint-disable-next-line @typescript-eslint/no-explicit any
const DATA = [
  // ================================================================
  //  PENDING — 待审批（4条）
  // ================================================================

  {
    id: 'ap0001',
    type: 'overtime',
    title: '加班申请',
    applicantName: '李四',
    summary: '周末加班 · 4小时',
    status: 'pending',
    createdAt: daysAgo(0, 10, 30),
    urgency: 'urgent' as UrgencyLevel,
    approvalNo: 'AP20260602001',
    applicantLevel: UserLevel.STAFF,
    contextData: {
      type: 'overtime',
      data: {
        monthHours: 12,
        monthLimit: 36,
        monthUsagePercent: 33,
        quarterHours: 28,
        quarterLimit: 100,
        deptAvgHours: 8.5,
        compensateType: '加班费',
        recentRecords: [
          { date: '05-15', hours: 3, type: '工作日加班' },
          { date: '05-24', hours: 5, type: '周末加班' },
          { date: '05-28', hours: 4, type: '工作日加班' },
        ],
      },
      riskWarning: {
        level: 'warning',
        category: 'health',
        title: '加班频率偏高，请注意员工休息保障',
        description: '该员工本月已有3次加班记录（2次工作日+1次周末），累计12小时。\n本次申请为周末加班4小时，批准后本月将达到16小时（上限44%）。\n\n虽然当前未超限，但需注意：工作日加班(05-15/28)间隔仅13天，建议关注员工疲劳状态。',
        evidence: '本月已12h + 本次4h = 16h(44%) | 工作日加班2次 | 部门平均8.5h',
        suggestion: '可正常审批。建议在评论中提醒员工注意劳逸结合，如后续仍有加班需求，建议优先安排调休补偿而非连续加班费结算。',
        source: 'hr_system_auto',
        generatedAt: daysAgo(0, 10, 30),
        regulationRef: undefined,
      },
    },
    formFields: [
      { label: '加班类型', value: '周末加班', type: 'select' },
      { label: '加班日期', value: '2026-06-07', type: 'date' },
      { label: '加班时段', value: '18:00 - 22:00', type: 'text' },
      { label: '加班时长', value: 4, type: 'number' },
      { label: '加班原因', value: '项目紧急上线，需周末配合技术团队完成系统部署和测试验证', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '李四', status: 'completed', action: '提交申请', time: fmt(daysAgo(0, 10, 30)) },
      { id: 'fn2', role: 'L2主管', userName: '王建国', status: 'current' },
      { id: 'fn3', role: 'L3经理（HR备案）', userName: '陈美华', status: 'pending' },
    ],
    comments: [
      { id: 'c1', userId: 'u-lisi', userName: '李四', content: '本周六有紧急上线任务，需要加班配合后端联调，申请4小时加班补贴。', createdAt: daysAgo(0, 10, 30) },
    ],
  },

  {
    id: 'ap0002',
    type: 'requisition',
    title: '物品领用',
    applicantName: '赵敏',
    summary: '厨房用品 · 5件',
    status: 'pending',
    createdAt: daysAgo(1, 14, 15),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260601002',
    applicantLevel: UserLevel.STAFF,
    contextData: {
      type: 'requisition',
      data: {
        stockQuantity: 24,
        stockUnit: '箱',
        monthlyAvg: 8,
        lastRequisitionDate: '2026-05-10',
        lastRequisitionQuantity: 10,
        departmentBudget: '¥2,000/月',
        departmentUsed: '¥1,280/月',
        supplierInfo: '餐饮物资供应商（月结）',
        leadTimeDays: 3,
      },
    },
    formFields: [
      { label: '物品类别', value: '厨房用品', type: 'select' },
      { label: '物品名称', value: '一次性手套(M号)', type: 'text' },
      { label: '数量', value: 5, type: 'number' },
      { label: '用途说明', value: '后厨月度常规消耗补充，现有库存仅剩2箱', type: 'textarea' },
      { label: '期望日期', value: '2026-06-05', type: 'date' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '赵敏', status: 'completed', action: '提交申请', time: fmt(daysAgo(1, 14, 15)) },
      { id: 'fn2', role: 'L2主管', userName: '王建国', status: 'current' },
    ],
    comments: [
      { id: 'c1', userId: 'u-zhaomin', userName: '赵敏', content: '厨房一次性手套库存不足，需要领用5箱补充，请店长审批。', createdAt: daysAgo(1, 14, 15) },
    ],
  },

  {
    id: 'ap0003',
    type: 'travel',
    title: '出差申请',
    applicantName: CURRENT_USER_NAME,
    summary: '上海 · 3天 · 客户拜访与合同签约',
    status: 'pending',
    createdAt: daysAgo(1, 9, 0),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260601003',
    applicantLevel: UserLevel.SUPERVISOR,
    contextData: {
      type: 'travel',
      data: {
        policyLimit: '¥4,500.00',
        estimatedActual: '¥3,200.00',
        yearTripCount: 2,
        advancePayment: '¥0 (实报实销)',
        relatedTask: {
          id: 'TK20260528001',
          title: '上海客户年度供应合同续签拜访',
          type: 'client_meeting',
          status: 'active',
          source: '运营部-客户管理组',
          issueDate: '2026-05-28',
          issuerName: '刘志远',
        },
        hotelStandard: '≤500元/晚',
        transportAllowance: '高铁二等座',
        dailyAllowance: '¥200/天',
        similarTrips: [
          { destination: '广州', dates: '05/12-05/14', amount: '¥2,156' },
        ],
      },
    },
    formFields: [
      { label: '目的地', value: '上海', type: 'text' },
      { label: '开始日期', value: '2026-06-10', type: 'date' },
      { label: '结束日期', value: '2026-06-12', type: 'date' },
      { label: '出差天数', value: 3, type: 'number' },
      { label: '预算金额', value: '¥3,500.00', type: 'text' },
      { label: '出差事由', value: '前往上海拜访重要客户，推进年度供应合同续签事宜', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: CURRENT_USER_NAME, status: 'completed', action: '提交申请', time: fmt(daysAgo(1, 9, 0)) },
      { id: 'fn2', role: 'L3经理', userName: '刘志远', status: 'current' },
    ],
    comments: [
      { id: 'c1', userId: 'u-me', userName: CURRENT_USER_NAME, content: '上海客户年度续约谈判，预计3天行程，已提前确认对方时间，请批准出差。', createdAt: daysAgo(1, 9, 0) },
    ],
  },

  {
    id: 'ap0004',
    type: 'leave',
    title: '请假申请',
    applicantName: '孙强',
    summary: '事假 · 2天',
    status: 'pending',
    createdAt: daysAgo(2, 11, 20),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260531004',
    applicantLevel: UserLevel.STAFF,
    contextData: {
      type: 'leave',
      data: {
        annualTotal: 10,
        annualUsed: 3,
        annualRemaining: 7,
        sickBalance: 5,
        personalBalance: 5,
        yearLeaveCount: 2,
        consecutiveDaysWarning: false,
        lastLeaveDate: '2026-04-10',
        leaveTrend: [
          { month: '3月', days: 1, type: '事假' },
          { month: '4月', days: 2, type: '病假' },
        ],
      },
    },
    formFields: [
      { label: '请假类型', value: '事假', type: 'select' },
      { label: '开始时间', value: '2026-06-09 09:00', type: 'date' },
      { label: '结束时间', value: '2026-06-10 18:00', type: 'date' },
      { label: '请假时长', value: 2, type: 'number' },
      { label: '请假原因', value: '家中老人住院陪护，需请假2天前往医院照料', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '孙强', status: 'completed', action: '提交申请', time: fmt(daysAgo(2, 11, 20)) },
      { id: 'fn2', role: 'L2主管', userName: '刘志远', status: 'current' },
      { id: 'fn3', role: 'L3经理（HR备案）', userName: '陈美华', status: 'pending' },
      { id: 'fn4', role: '归档', userName: '系统', status: 'pending' },
    ],
    comments: [
      { id: 'c1', userId: 'u-sunqiang', userName: '孙强', content: '家父突发身体不适需住院观察，特请事假2天陪护，工作已与同事做好交接。', createdAt: daysAgo(2, 11, 20) },
    ],
  },

  // ================================================================
  //  PROCESSING — 审批中（3条）
  // ================================================================

  {
    id: 'ap0005',
    type: 'leave',
    title: '年假申请',
    applicantName: CURRENT_USER_NAME,
    summary: '年假 · 5天 · 家庭事务需处理',
    status: 'processing',
    createdAt: daysAgo(3, 8, 45),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260530005',
    contextData: {
      type: 'leave',
      data: {
        annualTotal: 13,
        annualUsed: 5,
        annualRemaining: 8,
        sickBalance: 10,
        personalBalance: 5,
        compensatoryBalance: 16,
        yearLeaveCount: 1,
        consecutiveDaysWarning: true,
        lastLeaveDate: '2026-03-20',
        leaveTrend: [
          { month: '3月', days: 3, type: '年假' },
          { month: '2月', days: 2, type: '调休' },
        ],
      },
      riskWarning: {
        level: 'info',
        category: 'schedule',
        title: '连续5天年假，请注意工作交接安排',
        description: '本次申请为连续5个工作日（06-15至06-19）的年假。\n\n该时间段跨越一个完整工作周，需确保：\n· 已与直接上级确认工作交接人及交接事项清单\n· 紧急联系人信息已更新且可随时联系\n· 如有进行中的项目/任务，已明确暂停或转交安排\n\n年假余额充足（剩余8天），本次扣除后仍余3天。',
        evidence: '请假天数: 5天(连续工作日) | 年假余额: 8天 → 扣除后3天 | 本年度第1次年假申请',
        suggestion: '年假余额充足，可正常审批。建议店长在审批意见中确认交接安排已完成。',
        source: 'hr_system_auto',
        generatedAt: daysAgo(3, 8, 50),
        regulationRef: undefined,
      },
    },
    formFields: [
      { label: '请假类型', value: '年假', type: 'select' },
      { label: '开始时间', value: '2026-06-15 09:00', type: 'date' },
      { label: '结束时间', value: '2026-06-19 18:00', type: 'date' },
      { label: '请假时长', value: 5, type: 'number' },
      { label: '请假原因', value: '年假余额充足，计划回老家探亲并处理房屋装修事宜', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: CURRENT_USER_NAME, status: 'completed', action: '提交申请', time: fmt(daysAgo(3, 8, 45)) },
      { id: 'fn2', role: '门店店长', userName: '王建国', status: 'completed', action: '同意', time: fmt(daysAgo(2, 16, 30)) },
      { id: 'fn3', role: '人事审批', userName: '陈美华', status: 'current' },
      { id: 'fn4', role: '归档', userName: '系统', status: 'pending' },
    ],
    comments: [
      { id: 'c1', userId: 'u-zhangsan', userName: CURRENT_USER_NAME, content: '年假余额还有8天，计划6月中旬回老家一趟，请领导审批。', createdAt: daysAgo(3, 8, 45) },
      { id: 'c2', userId: 'u-wangjianguo', userName: '王建国', content: '张三近期表现优秀，年假申请合理，工作已安排好交接，同意。HR请核对假期余额。', createdAt: daysAgo(2, 16, 30) },
    ],
  },

  {
    id: 'ap0006',
    type: 'reimbursement',
    title: '费用报销',
    applicantName: '周丽',
    summary: '差旅费 · ¥1,280.00',
    status: 'processing',
    createdAt: daysAgo(4, 10, 0),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260529006',
    contextData: {
      type: 'reimbursement',
      data: {
        items: [
          { date: '2026-05-19', category: '高铁票', description: '杭州东→北京南（二等座）', amount: '¥553.00', receiptNo: 'HT20260519001' },
          { date: '2026-05-20', category: '高铁票', description: '北京南→杭州东（二等座）', amount: '¥553.00', receiptNo: 'HT20260520001' },
          { date: '2026-05-19~21', category: '住宿费', description: '杭州西湖智选假日酒店 × 2晚', amount: '¥174.00', receiptNo: 'JD20260519001' },
        ],
        monthlyTotal: '¥1,280.00',
        monthlyBudget: '¥5,000.00',
        monthlyUsagePercent: 26,
        yearTotal: '¥4,580.00',
        yearBudget: '¥30,000.00',
        receiptCount: 3,
        relatedTravel: {
          id: 'TK20260515003',
          title: '杭州供应商培训参访',
          type: 'training',
          status: 'completed',
          source: '运营部-培训组',
          issueDate: '2026-05-15',
          issuerName: '刘志远',
        },
        policyNotes: '差旅费用需在返回后15个工作日内提交报销，超期需说明原因',
      },
      riskWarning: {
        level: 'warning',
        category: 'budget',
        title: '月度差旅预算使用率偏高',
        description: '该员工本月差旅类报销已达¥1,280，占月度预算(¥5,000)的26%。\n\n加上本次申请后，本月累计将达：\n· 若仅此一笔：¥1,280（26%）— 正常范围\n· 但本季度已累计¥4,580，按当前节奏Q2可能超支\n\n建议关注后续出差计划的必要性评估。',
        evidence: '本月: ¥1,280/¥5,000(26%) | 本年度: ¥4,580/¥30,000(15.3%) | 票据3张',
        suggestion: '金额在合理范围内可正常审批。建议财务审核时关注本季度整体趋势，如后续仍有大额差旅报销建议提前进行预算调整。',
        source: 'hr_system_auto',
        generatedAt: daysAgo(4, 10, 10),
        regulationRef: undefined,
      },
    },
    formFields: [
      { label: '报销类型', value: '差旅费', type: 'select' },
      { label: '报销金额', value: '¥1,280.00', type: 'text' },
      { label: '费用日期', value: '2026-05-20', type: 'date' },
      { label: '费用说明', value: '赴杭州参加供应商培训，含高铁往返及2晚住宿费用', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '周丽', status: 'completed', action: '提交报销', time: fmt(daysAgo(4, 10, 0)) },
      { id: 'fn2', role: '门店店长', userName: '刘志远', status: 'completed', action: '同意', time: fmt(daysAgo(3, 14, 20)) },
      { id: 'fn3', role: '财务审核', userName: '郑晓峰', status: 'current' },
      { id: 'fn4', role: '财务审核', userName: '吴芳华', status: 'pending' },
    ],
    comments: [
      { id: 'c1', userId: 'u-zhouli', userName: '周丽', content: '杭州培训差旅费票据已全部整理上传，包含高铁票2张、酒店发票1张，共计1280元。', createdAt: daysAgo(4, 10, 0) },
      { id: 'c2', userId: 'u-liuzhiyuan', userName: '刘志远', content: '培训费用属于合理业务支出，票据齐全，同意报销。财务请审核金额准确性。', createdAt: daysAgo(3, 14, 20) },
    ],
  },

  {
    id: 'ap0007',
    type: 'reimbursement',
    title: '费用报销',
    applicantName: CURRENT_USER_NAME,
    summary: '交通餐饮 · ¥686.50',
    status: 'processing',
    createdAt: daysAgo(5, 15, 10),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260528007',
    contextData: {
      type: 'reimbursement',
      data: {
        items: [
          { date: '2026-05-25', category: '交通费', description: '打车：公司→朝阳门店', amount: '¥42.50', receiptNo: 'TX20260525001' },
          { date: '2026-05-25', category: '交通费', description: '打车：朝阳门店→国贸门店', amount: '¥38.00', receiptNo: 'TX20260525002' },
          { date: '2026-05-25', category: '交通费', description: '打车：国贸门店→望京门店', amount: '¥45.00', receiptNo: 'TX20260525003' },
          { date: '2026-05-25', category: '交通费', description: '打车：望京门店→公司', amount: '¥51.00', receiptNo: 'TX20260525004' },
          { date: '2026-05-25', category: '午餐补贴', description: '巡检午餐补助（3家）', amount: '¥200.00', receiptNo: 'WC20260525001' },
          { date: '2026-05-25', category: '晚餐补贴', description: '巡检晚餐补助（1家）', amount: '¥100.00', receiptNo: 'WC20260525002' },
          { date: '2026-05-25', category: '交通费', description: '打车：返回住处', amount: '¥210.00', receiptNo: 'TX20260525005' },
        ],
        monthlyTotal: '¥686.50',
        monthlyBudget: '¥3,000.00',
        monthlyUsagePercent: 23,
        yearTotal: '¥8,240.00',
        yearBudget: '¥20,000.00',
        receiptCount: 7,
        policyNotes: '市内交通报销需附行程说明，单笔超过100元需备注起止地点',
      },
    },
    formFields: [
      { label: '报销类型', value: '交通费', type: 'select' },
      { label: '报销金额', value: '¥686.50', type: 'text' },
      { label: '费用日期', value: '2026-05-25', type: 'date' },
      { label: '费用说明', value: '市区三家门店巡检交通费（打车）及午餐补助', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: CURRENT_USER_NAME, status: 'completed', action: '提交报销', time: fmt(daysAgo(5, 15, 10)) },
      { id: 'fn2', role: '门店店长', userName: '王建国', status: 'completed', action: '同意', time: fmt(daysAgo(4, 11, 0)) },
      { id: 'fn3', role: '财务审核', userName: '郑晓峰', status: 'current' },
      { id: 'fn4', role: '财务审核', userName: '吴芳华', status: 'pending' },
    ],
    comments: [
      { id: 'c1', userId: 'u-me', userName: CURRENT_USER_NAME, content: '25日巡检朝阳、国贸、望京三家门店，打车费共486.5元，午餐补贴200元，发票已上传。', createdAt: daysAgo(5, 15, 10) },
      { id: 'c2', userId: 'u-wangjianguo', userName: '王建国', content: '巡检工作属正常业务范畴，费用明细清晰，同意。', createdAt: daysAgo(4, 11, 0) },
    ],
  },

  // ================================================================
  //  APPROVED — 已通过（8条）
  // ================================================================

  {
    id: 'ap0008',
    type: 'swap',
    title: '调班申请',
    applicantName: '王芳',
    summary: '换班申请',
    status: 'approved',
    createdAt: daysAgo(6, 9, 30),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260527008',
    contextData: {
      type: 'swap',
      data: {
        partnerConfirmed: true,
        partnerName: '刘海燕',
        confirmTime: '2026-05-27 08:15',
        coveragePlan: '06-02晚班由刘海燕顶替，06-04早班由王芳顶替，排班表已同步更新',
        recentSwapCount: 1,
        swapLimit: 4,
        originalShiftDetail: { date: '06-02', time: '16:00-24:00', position: '前厅服务员A岗' },
        targetShiftDetail: { date: '06-04', time: '08:00-16:00', position: '前厅服务员B岗' },
      },
    },
    formFields: [
      { label: '调班类型', value: '交换班次', type: 'select' },
      { label: '原班次', value: '06-02 晚班(16:00-24:00)', type: 'text' },
      { label: '目标班次', value: '06-04 早班(08:00-16:00)', type: 'text' },
      { label: '换班对象', value: '刘海燕', type: 'text' },
      { label: '换班原因', value: '孩子学校家长会，需周四上午参加，已与海燕协商一致', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '王芳', status: 'completed', action: '提交申请', time: fmt(daysAgo(6, 9, 30)) },
      { id: 'fn2', role: '门店店长', userName: '王建国', status: 'completed', action: '同意', time: fmt(daysAgo(5, 10, 15) )},
    ],
    comments: [
      { id: 'c1', userId: 'u-wangfang', userName: '王芳', content: '因孩子学校临时通知开家长会，需要和海燕调换一下班次，双方已确认。', createdAt: daysAgo(6, 9, 30) },
      { id: 'c2', userId: 'u-wangjianguo', userName: '王建国', content: '双方确认无误，排班调整不影响运营，同意换班。', createdAt: daysAgo(5, 10, 15) },
    ],
  },

  {
    id: 'ap0009',
    type: 'travel',
    title: '出差申请',
    applicantName: '吴刚',
    summary: '深圳 · 2天 · 新店选址考察',
    status: 'approved',
    createdAt: daysAgo(7, 8, 0),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260526009',
    contextData: {
      type: 'travel',
      data: {
        policyLimit: '¥3,500.00',
        estimatedActual: '¥2,800.00',
        yearTripCount: 4,
        advancePayment: '¥0 (实报实销)',
        relatedTask: {
          id: 'TK20260518004',
          title: '深圳南山片区新店选址考察（3个候选点位）',
          type: 'project_task',
          status: 'active',
          source: '运营部-拓展组',
          issueDate: '2026-05-18',
          issuerName: '刘志远',
        },
        hotelStandard: '≤400元/晚',
        transportAllowance: '高铁二等座',
        dailyAllowance: '¥180/天',
        similarTrips: [
          { destination: '广州', dates: '05/08-05/11', amount: '¥2,156' },
          { destination: '杭州', dates: '04/20-04/22', amount: '¥1,680' },
        ],
      },
    },
    formFields: [
      { label: '目的地', value: '深圳', type: 'text' },
      { label: '开始日期', value: '2026-05-20', type: 'date' },
      { label: '结束日期', value: '2026-05-21', type: 'date' },
      { label: '出差天数', value: 2, type: 'number' },
      { label: '预算金额', value: '¥2,800.00', type: 'text' },
      { label: '出差事由', value: '深圳南山片区新店选址实地考察，评估商圈客流和租金水平', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '吴刚', status: 'completed', action: '提交申请', time: fmt(daysAgo(7, 8, 0)) },
      { id: 'fn2', role: '门店店长', userName: '刘志远', status: 'completed', action: '同意', time: fmt(daysAgo(6, 17, 0)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-wugang', userName: '吴刚', content: '深圳新店项目进入选址阶段，需实地考察3个候选点位，预算控制在2800以内。', createdAt: daysAgo(7, 8, 0) },
      { id: 'c2', userId: 'u-liuzhiyuan', userName: '刘志远', content: '新店拓展是本季度重点工作，考察行程安排合理，批准出差。注意收集竞品信息。', createdAt: daysAgo(6, 17, 0) },
    ],
  },

  {
    id: 'ap0010',
    type: 'leave',
    title: '病假申请',
    applicantName: CURRENT_USER_NAME,
    summary: '病假 · 1天 · 发烧就医',
    status: 'approved',
    createdAt: daysAgo(8, 7, 0),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260525010',
    contextData: {
      type: 'leave',
      data: {
        annualTotal: 13,
        annualUsed: 3,
        annualRemaining: 10,
        sickBalance: 9,
        personalBalance: 5,
        yearLeaveCount: 2,
        consecutiveDaysWarning: false,
        lastLeaveDate: '2026-05-27',
        leaveTrend: [
          { month: '4月', days: 1, type: '事假' },
          { month: '5月', days: 1, type: '病假' },
        ],
      },
    },
    formFields: [
      { label: '请假类型', value: '病假', type: 'select' },
      { label: '开始时间', value: '2026-05-27 09:00', type: 'date' },
      { label: '结束时间', value: '2026-05-27 18:00', type: 'date' },
      { label: '请假时长', value: 1, type: 'number' },
      { label: '请假原因', value: '突发高烧38.5°C，需前往医院就诊', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: CURRENT_USER_NAME, status: 'completed', action: '提交申请', time: fmt(daysAgo(8, 7, 0)) },
      { id: 'fn2', role: '门店店长', userName: '王建国', status: 'completed', action: '同意', time: fmt(daysAgo(8, 9, 30)) },
      { id: 'fn3', role: '人事审批', userName: '陈美华', status: 'completed', action: '同意', time: fmt(daysAgo(7, 14, 0)) },
      { id: 'fn4', role: '归档', userName: '系统', status: 'completed', action: '已归档', time: fmt(daysAgo(7, 14, 1)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-me', userName: CURRENT_USER_NAME, content: '今早起床发烧到38.5度，已预约社区医院门诊，申请病假1天。', createdAt: daysAgo(8, 7, 0) },
      { id: 'c2', userId: 'u-wangjianguo', userName: '王建国', content: '注意休息，早日康复。病假申请批准。', createdAt: daysAgo(8, 9, 30) },
      { id: 'c3', userId: 'u-chenmeihua', userName: '陈美华', content: '病假手续完备，假期余额充足，核准通过。', createdAt: daysAgo(7, 14, 0) },
    ],
  },

  {
    id: 'ap0011',
    type: 'overtime',
    title: '加班申请',
    applicantName: '陈伟',
    summary: '节假日加班 · 6小时',
    status: 'approved',
    createdAt: daysAgo(9, 18, 0),
    urgency: 'urgent' as UrgencyLevel,
    approvalNo: 'AP20260524011',
    contextData: {
      type: 'overtime',
      data: {
        monthHours: 16,
        monthLimit: 36,
        monthUsagePercent: 44,
        quarterHours: 35,
        quarterLimit: 100,
        deptAvgHours: 10.2,
        compensateType: '加班费',
        recentRecords: [
          { date: '05-03', hours: 6, type: '节假日加班' },
          { date: '04-30', hours: 4, type: '工作日加班' },
          { date: '04-20', hours: 6, type: '周末加班' },
        ],
      },
    },
    formFields: [
      { label: '加班类型', value: '节假日加班', type: 'select' },
      { label: '加班日期', value: '2026-05-17', type: 'date' },
      { label: '加班时段', value: '10:00 - 16:00', type: 'text' },
      { label: '加班时长', value: 6, type: 'number' },
      { label: '加班原因', value: '五一期间门店促销活动筹备，需提前到岗布置现场和准备物料', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '陈伟', status: 'completed', action: '提交申请', time: fmt(daysAgo(9, 18, 0)) },
      { id: 'fn2', role: '门店店长', userName: '王建国', status: 'completed', action: '同意', time: fmt(daysAgo(8, 10, 0)) },
      { id: 'fn3', role: 'HR备案', userName: '陈美华', status: 'completed', action: '已备案', time: fmt(daysAgo(7, 16, 0)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-chenwei', userName: '陈伟', content: '五一促销活动需要提前一天到场布置，申请节假日加班6小时。', createdAt: daysAgo(9, 18, 0) },
      { id: 'c2', userId: 'u-wangjianguo', userName: '王建国', content: '活动期间加班属实，按公司规定给予三倍工资补偿，同意。', createdAt: daysAgo(8, 10, 0) },
      { id: 'c3', userId: 'u-chenmeihua', userName: '陈美华', content: '已备案，考勤系统中登记节假日加班工时。', createdAt: daysAgo(7, 16, 0) },
    ],
  },

  {
    id: 'ap0012',
    type: 'reimbursement',
    title: '费用报销',
    applicantName: '林涛',
    summary: '办公用品 · ¥458.00',
    status: 'approved',
    createdAt: daysAgo(10, 13, 0),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260523012',
    contextData: {
      type: 'reimbursement',
      data: {
        items: [
          { date: '2026-05-18', category: '办公用品', description: 'A4打印纸 × 2箱（500张/包×8包/箱）', amount: '¥240.00', receiptNo: 'BG20260518001' },
          { date: '2026-05-18', category: '办公用品', description: '文件夹（A4/双夹）× 50个', amount: '¥98.00', receiptNo: 'BG20260518002' },
          { date: '2026-05-18', category: '办公用品', description: '中性签字笔（0.5mm黑色）× 2盒（12支/盒）', amount: '¥120.00', receiptNo: 'BG20260518003' },
        ],
        monthlyTotal: '¥458.00',
        monthlyBudget: '¥2,000.00',
        monthlyUsagePercent: 23,
        yearTotal: '¥1,850.00',
        yearBudget: '¥10,000.00',
        receiptCount: 3,
        policyNotes: '办公用品采购需走行政物资领用流程，单次超过300元需部门负责人审批',
      },
    },
    formFields: [
      { label: '报销类型', value: '办公用品', type: 'select' },
      { label: '报销金额', value: '¥458.00', type: 'text' },
      { label: '费用日期', value: '2026-05-18', type: 'date' },
      { label: '费用说明', value: '采购A4打印纸2箱、文件夹50个、签字笔2盒', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '林涛', status: 'completed', action: '提交报销', time: fmt(daysAgo(10, 13, 0)) },
      { id: 'fn2', role: '门店店长', userName: '刘志远', status: 'completed', action: '同意', time: fmt(daysAgo(9, 11, 30)) },
      { id: 'fn3', role: '财务审核', userName: '郑晓峰', status: 'completed', action: '审核通过', time: fmt(daysAgo(8, 15, 0)) },
      { id: 'fn4', role: '财务审核', userName: '吴芳华', status: 'completed', action: '批准', time: fmt(daysAgo(7, 10, 0)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-lintao', userName: '林涛', content: '办公室耗材采购清单和发票已上传，共458元整。', createdAt: daysAgo(10, 13, 0) },
      { id: 'c2', userId: 'u-liuzhiyuan', userName: '刘志远', content: '办公物资采购属正常需求，单价合理，同意。', createdAt: daysAgo(9, 11, 30) },
      { id: 'c3', userId: 'u-zhengxiaofeng', userName: '郑晓峰', content: '发票查验通过，金额与采购清单一致，审核无误。', createdAt: daysAgo(8, 15, 0) },
    ],
  },

  {
    id: 'ap0013',
    type: 'requisition',
    title: '物品领用',
    applicantName: '黄磊',
    summary: '清洁用品 · 10件',
    status: 'approved',
    createdAt: daysAgo(11, 9, 0),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260522013',
    contextData: {
      type: 'requisition',
      data: {
        stockQuantity: 15,
        stockUnit: '桶',
        monthlyAvg: 12,
        lastRequisitionDate: '2026-04-20',
        lastRequisitionQuantity: 8,
        departmentBudget: '¥800/月',
        departmentUsed: '¥520/月',
        supplierInfo: '清洁用品供应商（月结）',
        leadTimeDays: 2,
      },
    },
    formFields: [
      { label: '物品类别', value: '清洁用品', type: 'select' },
      { label: '物品名称', value: '洗洁精(大桶装)', type: 'text' },
      { label: '数量', value: 10, type: 'number' },
      { label: '用途说明', value: '月度清洁用品补充，后厨洗碗区用量较大', type: 'textarea' },
      { label: '期望日期', value: '2026-05-25', type: 'date' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '黄磊', status: 'completed', action: '提交申请', time: fmt(daysAgo(11, 9, 0)) },
      { id: 'fn2', role: '门店店长', userName: '王建国', status: 'completed', action: '同意', time: fmt(daysAgo(10, 16, 0)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-huanglei', userName: '黄磊', content: '洗碗区洗洁精库存告急，需领用10桶补充，已核实用量。', createdAt: daysAgo(11, 9, 0) },
      { id: 'c2', userId: 'u-wangjianguo', userName: '王建国', content: '清洁物资属日常消耗品，用量合理，批准领用。', createdAt: daysAgo(10, 16, 0) },
    ],
  },

  {
    id: 'ap0014',
    type: 'leave',
    title: '请假申请',
    applicantName: '徐静',
    summary: '调休 · 1天',
    status: 'approved',
    createdAt: daysAgo(12, 8, 30),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260521014',
    contextData: {
      type: 'leave',
      data: {
        annualTotal: 10,
        annualUsed: 6,
        annualRemaining: 4,
        sickBalance: 7,
        personalBalance: 5,
        compensatoryBalance: 8,
        yearLeaveCount: 3,
        consecutiveDaysWarning: false,
        lastLeaveDate: '2026-05-08',
        leaveTrend: [
          { month: '3月', days: 1, type: '调休' },
          { month: '4月', days: 2, type: '事假' },
          { month: '5月', days: 1, type: '病假' },
        ],
      },
    },
    formFields: [
      { label: '请假类型', value: '调休', type: 'select' },
      { label: '开始时间', value: '2026-05-19 09:00', type: 'date' },
      { label: '结束时间', value: '2026-05-19 18:00', type: 'date' },
      { label: '请假时长', value: 1, type: 'number' },
      { label: '请假原因', value: '上月周末加班累计8小时，申请调休1天', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '徐静', status: 'completed', action: '提交申请', time: fmt(daysAgo(12, 8, 30)) },
      { id: 'fn2', role: '门店店长', userName: '刘志远', status: 'completed', action: '同意', time: fmt(daysAgo(11, 10, 0)) },
      { id: 'fn3', role: '人事审批', userName: '陈美华', status: 'completed', action: '同意', time: fmt(daysAgo(10, 14, 30)) },
      { id: 'fn4', role: '归档', userName: '系统', status: 'completed', action: '已归档', time: fmt(daysAgo(10, 14, 31)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-xujing', userName: '徐静', content: '上个月连续两个周末加班，累计8小时调休余额，申请周一调休1天。', createdAt: daysAgo(12, 8, 30) },
      { id: 'c2', userId: 'u-liuzhiyuan', userName: '刘志远', content: '调休余额核实无误，同意调休。', createdAt: daysAgo(11, 10, 0) },
      { id: 'c3', userId: 'u-chenmeihua', userName: '陈美华', content: '调休假扣减已录入系统，核准通过。', createdAt: daysAgo(10, 14, 30) },
    ],
  },

  {
    id: 'ap0015',
    type: 'reimbursement',
    title: '差旅报销',
    applicantName: CURRENT_USER_NAME,
    summary: '差旅费 · ¥2,156.00',
    status: 'approved',
    createdAt: daysAgo(14, 10, 0),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260519015',
    contextData: {
      type: 'reimbursement',
      data: {
        items: [
          { date: '2026-05-12', category: '高铁票', description: '北京南→广州南（二等座）', amount: '¥876.00', receiptNo: 'HT20260512001' },
          { date: '2026-05-14', category: '高铁票', description: '广州南→北京南（二等座）', amount: '¥876.00', receiptNo: 'HT20260514001' },
          { date: '2026-05-12~14', category: '住宿费', description: '广州天河希尔顿欢朋 × 2晚', amount: '¥980.00', receiptNo: 'JD20260512001' },
          { date: '2026-05-13', category: '市内交通', description: '地铁+打车（客户拜访行程）', amount: '¥300.00', receiptNo: 'TX20260513001' },
        ],
        monthlyTotal: '¥3,032.00',
        monthlyBudget: '¥8,000.00',
        monthlyUsagePercent: 38,
        yearTotal: '¥12,480.00',
        yearBudget: '¥30,000.00',
        receiptCount: 4,
        relatedTravel: {
          id: 'TK20260508002',
          title: '广州客户拜访：年度供应合同续签谈判',
          type: 'client_meeting',
          status: 'completed',
          source: '运营部-销售组',
          issueDate: '2026-05-08',
          issuerName: '刘志远',
        },
        policyNotes: '住宿标准一线城市≤500元/晚，本次使用协议酒店优惠价490元/晚',
      },
    },
    formFields: [
      { label: '报销类型', value: '差旅费', type: 'select' },
      { label: '报销金额', value: '¥2,156.00', type: 'text' },
      { label: '费用日期', value: '2026-05-12', type: 'date' },
      { label: '费用说明', value: '广州客户拜访：高铁往返876元 + 酒店2晚980元 + 市内交通300元', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: CURRENT_USER_NAME, status: 'completed', action: '提交报销', time: fmt(daysAgo(14, 10, 0)) },
      { id: 'fn2', role: '门店店长', userName: '王建国', status: 'completed', action: '同意', time: fmt(daysAgo(13, 11, 0)) },
      { id: 'fn3', role: '财务审核', userName: '郑晓峰', status: 'completed', action: '审核通过', time: fmt(daysAgo(12, 16, 0)) },
      { id: 'fn4', role: '财务审核', userName: '吴芳华', status: 'completed', action: '批准', time: fmt(daysAgo(11, 9, 30)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-me', userName: CURRENT_USER_NAME, content: '12-14日赴广州拜访客户，所有票据和行程说明已附上，总计2156元。', createdAt: daysAgo(14, 10, 0) },
      { id: 'c2', userId: 'u-wangjianguo', userName: '王建国', content: '此次拜访为重要客户维护工作，费用在预算范围内，同意报销。', createdAt: daysAgo(13, 11, 0) },
      { id: 'c3', userId: 'u-zhengxiaofeng', userName: '郑晓峰', content: '发票验真通过，住宿标准符合公司规定（不超过500元/晚），审核通过。', createdAt: daysAgo(12, 16, 0) },
    ],
  },

  // ================================================================
  //  REJECTED — 已驳回（3条）
  // ================================================================

  {
    id: 'ap0016',
    type: 'leave',
    title: '请假申请',
    applicantName: '马超',
    summary: '事假 · 3天',
    status: 'rejected',
    createdAt: daysAgo(5, 9, 0),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260528016',
    contextData: {
      type: 'leave',
      data: {
        annualTotal: 10,
        annualUsed: 7,
        annualRemaining: 3,
        sickBalance: 5,
        personalBalance: 3,
        yearLeaveCount: 4,
        consecutiveDaysWarning: true,
        lastLeaveDate: '2026-05-20',
        leaveTrend: [
          { month: '3月', days: 2, type: '事假' },
          { month: '4月', days: 1, type: '事假' },
          { month: '5月', days: 3, type: '年假' },
          { month: '5月', days: 1, type: '事假' },
        ],
      },
    },
    formFields: [
      { label: '请假类型', value: '事假', type: 'select' },
      { label: '开始时间', value: '2026-06-05 09:00', type: 'date' },
      { label: '结束时间', value: '2026-06-07 18:00', type: 'date' },
      { label: '请假时长', value: 3, type: 'number' },
      { label: '请假原因', value: '朋友婚礼需要参加，在外地举办', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '马超', status: 'completed', action: '提交申请', time: fmt(daysAgo(5, 9, 0)) },
      { id: 'fn2', role: '门店店长', userName: '王建国', status: 'completed', action: '驳回', time: fmt(daysAgo(4, 15, 30)) },
      { id: 'fn3', role: '人事审批', userName: '陈美华', status: 'completed', action: '无需处理', time: fmt(daysAgo(4, 15, 31)) },
      { id: 'fn4', role: '归档', userName: '系统', status: 'completed', action: '未归档', time: fmt(daysAgo(4, 15, 32)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-machao', userName: '马超', content: '大学好友6月初结婚，需要请假3天去外地参加婚礼。', createdAt: daysAgo(5, 9, 0) },
      { id: 'c2', userId: 'u-wangjianguo', userName: '王建国', content: '驳回：下周正值月末盘点关键期，前厅人手紧张，建议改期或缩短天数。可协调调休替代。', createdAt: daysAgo(4, 15, 30) },
    ],
  },

  {
    id: 'ap0017',
    type: 'reimbursement',
    title: '费用报销',
    applicantName: '何婷',
    summary: '餐饮费 · ¥892.00',
    status: 'rejected',
    createdAt: daysAgo(8, 14, 0),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260525017',
    contextData: {
      type: 'reimbursement',
      data: {
        items: [
          { date: '2026-05-20', category: '餐饮费', description: '团队聚餐：庆祝项目上线（10人）', amount: '¥892.00', receiptNo: 'CY20260520001' },
        ],
        monthlyTotal: '¥892.00',
        monthlyBudget: '¥1,500.00',
        monthlyUsagePercent: 59,
        yearTotal: '¥2,340.00',
        yearBudget: '¥8,000.00',
        receiptCount: 1,
        policyNotes: '餐饮费报销仅限业务招待和因公外出就餐，团队建设活动需走团建经费',
      },
    },
    formFields: [
      { label: '报销类型', value: '餐饮费', type: 'select' },
      { label: '报销金额', value: '¥892.00', type: 'text' },
      { label: '费用日期', value: '2026-05-20', type: 'date' },
      { label: '费用说明', value: '团队聚餐庆祝项目完成', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '何婷', status: 'completed', action: '提交报销', time: fmt(daysAgo(8, 14, 0)) },
      { id: 'fn2', role: '门店店长', userName: '刘志远', status: 'completed', action: '同意', time: fmt(daysAgo(7, 10, 0)) },
      { id: 'fn3', role: '财务审核', userName: '郑晓峰', status: 'completed', action: '驳回', time: fmt(daysAgo(6, 11, 0)) },
      { id: 'fn4', role: '财务审核', userName: '吴芳华', status: 'completed', action: '无需处理', time: fmt(daysAgo(6, 11, 1)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-heting', userName: '何婷', content: '项目顺利上线，团队聚餐庆祝，人均约100元，共892元。', createdAt: daysAgo(8, 14, 0) },
      { id: 'c2', userId: 'u-liuzhiyuan', userName: '刘志远', content: '团队建设活动，金额合理范围内，同意。', createdAt: daysAgo(7, 10, 0) },
      { id: 'c3', userId: 'u-zhengxiaofeng', userName: '郑晓峰', content: '驳回：团队聚餐不属于可报销范围，需走部门团建经费或个人承担。根据《费用报销管理办法》第7条，餐饮类非业务招待费用不予报销。', createdAt: daysAgo(6, 11, 0) },
    ],
  },

  {
    id: 'ap0018',
    type: 'overtime',
    title: '加班申请',
    applicantName: CURRENT_USER_NAME,
    summary: '工作日加班 · 3小时',
    status: 'rejected',
    createdAt: daysAgo(10, 17, 30),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260523018',
    contextData: {
      type: 'overtime',
      data: {
        monthHours: 6,
        monthLimit: 36,
        monthUsagePercent: 17,
        quarterHours: 22,
        quarterLimit: 100,
        deptAvgHours: 9.8,
        compensateType: '调休',
        recentRecords: [
          { date: '05-15', hours: 3, type: '工作日加班' },
          { date: '05-08', hours: 4, type: '周末加班' },
          { date: '04-25', hours: 3, type: '工作日加班' },
        ],
      },
    },
    formFields: [
      { label: '加班类型', value: '工作日加班', type: 'select' },
      { label: '加班日期', value: '2026-05-22', type: 'date' },
      { label: '加班时段', value: '18:30 - 21:30', type: 'text' },
      { label: '加班时长', value: 3, type: 'number' },
      { label: '加班原因', value: '当日报表数据未按时完成，需加班补录', type: 'textarea' },
    { label: '是否调休', value: '否', type: 'text' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: CURRENT_USER_NAME, status: 'completed', action: '提交申请', time: fmt(daysAgo(10, 17, 30)) },
      { id: 'fn2', role: '门店店长', userName: '王建国', status: 'completed', action: '驳回', time: fmt(daysAgo(9, 9, 15)) },
      { id: 'fn3', role: 'HR备案', userName: '陈美华', status: 'completed', action: '无需处理', time: fmt(daysAgo(9, 9, 16)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-me', userName: CURRENT_USER_NAME, content: '22日下午系统故障导致报表延迟，晚上补录数据到21:30，申请加班3小时。', createdAt: daysAgo(10, 17, 30) },
      { id: 'c2', userId: 'u-wangjianguo', userName: '王建国', content: '驳回：经核查，当日数据延迟系个人操作疏忽导致（未按时保存），非突发客观原因。此类情况不纳入加班认定范围。请注意工作时间管理。', createdAt: daysAgo(9, 9, 15) },
    ],
  },

  // ================================================================
  //  CANCELLED — 已撤回（2条）
  // ================================================================

  {
    id: 'ap0019',
    type: 'leave',
    title: '请假申请',
    applicantName: '朱杰',
    summary: '年假 · 2天',
    status: 'cancelled',
    createdAt: daysAgo(3, 16, 0),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260530019',
    contextData: {
      type: 'leave',
      data: {
        annualTotal: 12,
        annualUsed: 4,
        annualRemaining: 8,
        sickBalance: 10,
        personalBalance: 5,
        yearLeaveCount: 2,
        consecutiveDaysWarning: false,
        lastLeaveDate: '2026-04-15',
        leaveTrend: [
          { month: '2月', days: 2, type: '年假' },
          { month: '4月', days: 2, type: '年假' },
        ],
      },
    },
    formFields: [
      { label: '请假类型', value: '年假', type: 'select' },
      { label: '开始时间', value: '2026-06-06 09:00', type: 'date' },
      { label: '结束时间', value: '2026-06-07 18:00', type: 'date' },
      { label: '请假时长', value: 2, type: 'number' },
      { label: '请假原因', value: '计划带家人周边游', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '朱杰', status: 'completed', action: '提交申请', time: fmt(daysAgo(3, 16, 0)) },
      { id: 'fn2', role: '门店店长', userName: '刘志远', status: 'completed', action: '已撤回', time: fmt(daysAgo(2, 10, 0)) },
      { id: 'fn3', role: '人事审批', userName: '陈美华', status: 'completed', action: '已撤回', time: fmt(daysAgo(2, 10, 1)) },
      { id: 'fn4', role: '归档', userName: '系统', status: 'completed', action: '已撤回', time: fmt(daysAgo(2, 10, 2)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-zhujie', userName: '朱杰', content: '计划6月初请两天年假带家人出去走走。', createdAt: daysAgo(3, 16, 0) },
    ],
  },

  {
    id: 'ap0020',
    type: 'reimbursement',
    title: '交通费报销',
    applicantName: CURRENT_USER_NAME,
    summary: '交通费 · ¥156.00',
    status: 'cancelled',
    createdAt: daysAgo(6, 11, 0),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260527020',
    contextData: {
      type: 'reimbursement',
      data: {
        items: [
          { date: '2026-05-24', category: '交通费', description: '打车：公司→客户处（朝阳区建国路）', amount: '¥86.00', receiptNo: 'TX20260524001' },
          { date: '2026-05-24', category: '交通费', description: '打车：客户处→返回公司', amount: '¥70.00', receiptNo: 'TX20260524002' },
        ],
        monthlyTotal: '¥156.00',
        monthlyBudget: '¥1,000.00',
        monthlyUsagePercent: 16,
        yearTotal: '¥1,890.00',
        yearBudget: '¥8,000.00',
        receiptCount: 2,
        policyNotes: '市内交通报销需附行程说明，单笔超过100元需备注起止地点',
      },
    },
    formFields: [
      { label: '报销类型', value: '交通费', type: 'select' },
      { label: '报销金额', value: '¥156.00', type: 'text' },
      { label: '费用日期', value: '2026-05-24', type: 'date' },
      { label: '费用说明', value: '外出办事打车费用', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: CURRENT_USER_NAME, status: 'completed', action: '提交报销', time: fmt(daysAgo(6, 11, 0)) },
      { id: 'fn2', role: '门店店长', userName: '王建国', status: 'completed', action: '已撤回', time: fmt(daysAgo(5, 9, 0)) },
      { id: 'fn3', role: '财务审核', userName: '郑晓峰', status: 'completed', action: '已撤回', time: fmt(daysAgo(5, 9, 1)) },
      { id: 'fn4', role: '财务审核', userName: '吴芳华', status: 'completed', action: '已撤回', time: fmt(daysAgo(5, 9, 2)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-me', userName: CURRENT_USER_NAME, content: '24日外出办事打车费用156元，发票已上传。', createdAt: daysAgo(6, 11, 0) },
    ],
  },

  // ================================================================
  //  补充数据 — 覆盖更多类型和状态组合（8条）
  // ================================================================

  {
    id: 'ap0021',
    type: 'leave',
    title: '请假申请',
    applicantName: '韩雪梅',
    summary: '病假 · 3天 · 身体不适需休息',
    status: 'approved',
    createdAt: daysAgo(16, 9, 0),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260517021',
    contextData: {
      type: 'leave',
      data: {
        annualTotal: 12,
        annualUsed: 3,
        annualRemaining: 9,
        sickBalance: 10,
        personalBalance: 5,
        yearLeaveCount: 1,
        consecutiveDaysWarning: true,
        lastLeaveDate: '2026-05-10',
        leaveTrend: [
          { month: '5月', days: 3, type: '病假' },
        ],
      },
    },
    formFields: [
      { label: '请假类型', value: '病假', type: 'select' },
      { label: '开始时间', value: '2026-05-10 09:00', type: 'date' },
      { label: '结束时间', value: '2026-05-12 18:00', type: 'date' },
      { label: '请假时长', value: 3, type: 'number' },
      { label: '请假原因', value: '急性肠胃炎，医嘱建议休息3天', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '韩雪梅', status: 'completed', action: '提交申请', time: fmt(daysAgo(16, 9, 0)) },
      { id: 'fn2', role: '门店店长', userName: '王建国', status: 'completed', action: '同意', time: fmt(daysAgo(15, 10, 30)) },
      { id: 'fn3', role: '人事审批', userName: '陈美华', status: 'completed', action: '同意', time: fmt(daysAgo(14, 14, 0)) },
      { id: 'fn4', role: '归档', userName: '系统', status: 'completed', action: '已归档', time: fmt(daysAgo(14, 14, 1)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-hanxuemei', userName: '韩雪梅', content: '突发急性肠胃炎，医院开了诊断证明，需卧床休息3天。', createdAt: daysAgo(16, 9, 0) },
      { id: 'c2', userId: 'u-wangjianguo', userName: '王建国', content: '诊断证明已查阅，病情属实，准予病假。祝早日康复！', createdAt: daysAgo(15, 10, 30) },
      { id: 'c3', userId: 'u-chenmeihua', userName: '陈美华', content: '病假材料齐全，医疗证明有效，核准通过。', createdAt: daysAgo(14, 14, 0) },
    ],
  },

  {
    id: 'ap0022',
    type: 'overtime',
    title: '加班申请',
    applicantName: '高峰',
    summary: '周末加班 · 5小时',
    status: 'approved',
    createdAt: daysAgo(18, 18, 20),
    urgency: 'urgent' as UrgencyLevel,
    approvalNo: 'AP20260515022',
    contextData: {
      type: 'overtime',
      data: {
        monthHours: 20,
        monthLimit: 36,
        monthUsagePercent: 56,
        quarterHours: 48,
        quarterLimit: 100,
        deptAvgHours: 11.0,
        compensateType: '调休',
        recentRecords: [
          { date: '05-10', hours: 5, type: '周末加班' },
          { date: '05-03', hours: 6, type: '节假日加班' },
          { date: '04-25', hours: 4, type: '工作日加班' },
          { date: '04-15', hours: 5, type: '周末加班' },
        ],
      },
    },
    formFields: [
      { label: '加班类型', value: '周末加班', type: 'select' },
      { label: '加班日期', value: '2026-05-10', type: 'date' },
      { label: '加班时段', value: '14:00 - 19:00', type: 'text' },
      { label: '加班时长', value: 5, type: 'number' },
      { label: '加班原因', value: '季度末库存大盘点，全店参与', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '高峰', status: 'completed', action: '提交申请', time: fmt(daysAgo(18, 18, 20)) },
      { id: 'fn2', role: '门店店长', userName: '刘志远', status: 'completed', action: '同意', time: fmt(daysAgo(17, 10, 0)) },
      { id: 'fn3', role: 'HR备案', userName: '陈美华', status: 'completed', action: '已备案', time: fmt(daysAgo(16, 11, 30)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-gaofeng', userName: '高峰', content: 'Q2季度末大盘点，后厨需全员参与，申请周六下午加班5小时。', createdAt: daysAgo(18, 18, 20) },
      { id: 'c2', userId: 'u-liuzhiyuan', userName: '刘志远', content: '季度盘点属必要工作，加班事实清楚，同意并安排调休补偿。', createdAt: daysAgo(17, 10, 0) },
      { id: 'c3', userId: 'u-chenmeihua', userName: '陈美华', content: '已备案，加班工时计入本周期调休池。', createdAt: daysAgo(16, 11, 30) },
    ],
  },

  {
    id: 'ap0023',
    type: 'swap',
    title: '调班申请',
    applicantName: CURRENT_USER_NAME,
    summary: '换班申请',
    status: 'approved',
    createdAt: daysAgo(20, 8, 45),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260513023',
    contextData: {
      type: 'swap',
      data: {
        partnerConfirmed: true,
        partnerName: '李四',
        confirmTime: '2026-05-13 08:30',
        coveragePlan: '05-15早班由李四顶替，05-17晚班由我顶替，排班表已同步更新',
        recentSwapCount: 2,
        swapLimit: 4,
        originalShiftDetail: { date: '05-15', time: '08:00-16:00', position: '运营专员A岗' },
        targetShiftDetail: { date: '05-17', time: '16:00-24:00', position: '运营专员B岗' },
      },
    },
    formFields: [
      { label: '调班类型', value: '交换班次', type: 'select' },
      { label: '原班次', value: '05-15 早班(08:00-16:00)', type: 'text' },
      { label: '目标班次', value: '05-17 晚班(16:00-24:00)', type: 'text' },
      { label: '换班对象', value: '李四', type: 'text' },
      { label: '换班原因', value: '15日上午需参加职业技能培训考试', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: CURRENT_USER_NAME, status: 'completed', action: '提交申请', time: fmt(daysAgo(20, 8, 45)) },
      { id: 'fn2', role: '门店店长', userName: '王建国', status: 'completed', action: '同意', time: fmt(daysAgo(19, 9, 30)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-me', userName: CURRENT_USER_NAME, content: '15日上午有职业资格证考试，需要和李四换一下班次，已沟通好。', createdAt: daysAgo(20, 8, 45) },
      { id: 'c2', userId: 'u-wangjianguo', userName: '王建国', content: '职业提升值得支持，双方已确认，同意调换。加油考试！', createdAt: daysAgo(19, 9, 30) },
    ],
  },

  {
    id: 'ap0024',
    type: 'travel',
    title: '出差申请',
    applicantName: '郭明',
    summary: '成都 · 4天 · 行业展会参加',
    status: 'approved',
    createdAt: daysAgo(22, 10, 0),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260511024',
    contextData: {
      type: 'travel',
      data: {
        policyLimit: '¥5,000.00',
        estimatedActual: '¥4,200.00',
        yearTripCount: 3,
        advancePayment: '¥2,000 (预支差旅费)',
        relatedTask: {
          id: 'TK20260505005',
          title: '2026中国餐饮行业博览会（成都）参访学习',
          type: 'training',
          status: 'active',
          source: '运营部-培训组',
          issueDate: '2026-05-05',
          issuerName: '刘志远',
        },
        hotelStandard: '≤450元/晚',
        transportAllowance: '高铁二等座',
        dailyAllowance: '¥180/天',
        similarTrips: [
          { destination: '杭州', dates: '05/19-05/21', amount: '¥1,280' },
        ],
      },
    },
    formFields: [
      { label: '目的地', value: '成都', type: 'text' },
      { label: '开始日期', value: '2026-05-08', type: 'date' },
      { label: '结束日期', value: '2026-05-11', type: 'date' },
      { label: '出差天数', value: 4, type: 'number' },
      { label: '预算金额', value: '¥4,200.00', type: 'text' },
      { label: '出差事由', value: '参加中国餐饮行业博览会，考察新产品和新设备', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '郭明', status: 'completed', action: '提交申请', time: fmt(daysAgo(22, 10, 0)) },
      { id: 'fn2', role: '门店店长', userName: '刘志远', status: 'completed', action: '同意', time: fmt(daysAgo(21, 14, 0)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-guoming', userName: '郭明', content: '成都餐博会是国内规模最大的行业展会之一，计划参观学习并考察几款感兴趣的设备。', createdAt: daysAgo(22, 10, 0) },
      { id: 'c2', userId: 'u-liuzhiyuan', userName: '刘志远', content: '行业展会参与有助于了解市场动态和技术趋势，支持出差。回来请分享参展报告。', createdAt: daysAgo(21, 14, 0) },
    ],
  },

  {
    id: 'ap0025',
    type: 'reimbursement',
    title: '费用报销',
    applicantName: '罗斌',
    summary: '餐饮费 · ¥356.00',
    status: 'approved',
    createdAt: daysAgo(24, 14, 30),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260509025',
    contextData: {
      type: 'reimbursement',
      data: {
        items: [
          { date: '2026-05-05', category: '餐饮费', description: '总部巡检组工作午餐招待（6人）', amount: '¥356.00', receiptNo: 'CY20260505001' },
        ],
        monthlyTotal: '¥356.00',
        monthlyBudget: '¥2,000.00',
        monthlyUsagePercent: 18,
        yearTotal: '¥2,890.00',
        yearBudget: '¥12,000.00',
        receiptCount: 1,
        policyNotes: '业务招待费需提前申请，人均标准不超过80元/餐，超标准需特别说明',
      },
    },
    formFields: [
      { label: '报销类型', value: '餐饮费', type: 'select' },
      { label: '报销金额', value: '¥356.00', type: 'text' },
      { label: '费用日期', value: '2026-05-05', type: 'date' },
      { label: '费用说明', value: '接待总部巡检组工作午餐', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '罗斌', status: 'completed', action: '提交报销', time: fmt(daysAgo(24, 14, 30)) },
      { id: 'fn2', role: '门店店长', userName: '王建国', status: 'completed', action: '同意', time: fmt(daysAgo(23, 10, 0)) },
      { id: 'fn3', role: '财务审核', userName: '郑晓峰', status: 'completed', action: '审核通过', time: fmt(daysAgo(22, 15, 0)) },
      { id: 'fn4', role: '财务审核', userName: '吴芳华', status: 'completed', action: '批准', time: fmt(daysAgo(21, 9, 0)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-luobin', userName: '罗斌', content: '5日总部巡检组莅临指导，工作午餐招待费用356元，发票已上传。', createdAt: daysAgo(24, 14, 30) },
      { id: 'c2', userId: 'u-wangjianguo', userName: '王建国', content: '业务招待费用属实，用餐标准合规，同意报销。', createdAt: daysAgo(23, 10, 0) },
      { id: 'c3', userId: 'u-zhengxiaofeng', userName: '郑晓峰', content: '招待费用凭证完整，金额在公司招待标准内，审核通过。', createdAt: daysAgo(22, 15, 0) },
    ],
  },

  {
    id: 'ap0026',
    type: 'requisition',
    title: '物品领用',
    applicantName: '胡娜',
    summary: '制服工装 · 3套',
    status: 'approved',
    createdAt: daysAgo(26, 9, 0),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260507026',
    contextData: {
      type: 'requisition',
      data: {
        stockQuantity: 8,
        stockUnit: '套',
        monthlyAvg: 4,
        lastRequisitionDate: '2026-04-25',
        lastRequisitionQuantity: 5,
        departmentBudget: '¥1,500/月',
        departmentUsed: '¥980/月',
        supplierInfo: '制服工装供应商（季度结算）',
        leadTimeDays: 7,
      },
    },
    formFields: [
      { label: '物品类别', value: '制服工装', type: 'select' },
      { label: '物品名称', value: '夏季服务员制服(L码)', type: 'text' },
      { label: '数量', value: 3, type: 'number' },
      { label: '用途说明', value: '新入职员工配备制服，3名新员工均需L码', type: 'textarea' },
      { label: '期望日期', value: '2026-05-10', type: 'date' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '胡娜', status: 'completed', action: '提交申请', time: fmt(daysAgo(26, 9, 0)) },
      { id: 'fn2', role: '门店店长', userName: '刘志远', status: 'completed', action: '同意', time: fmt(daysAgo(25, 11, 0)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-huna', userName: '胡娜', content: '本月新入职3名服务员，都需要L码夏季制服，请在10日前配发到位。', createdAt: daysAgo(26, 9, 0) },
      { id: 'c2', userId: 'u-liuzhiyuan', userName: '刘志远', content: '新员工制服配置属标准流程，数量准确，批准领用。', createdAt: daysAgo(25, 11, 0) },
    ],
  },

  {
    id: 'ap0027',
    type: 'leave',
    title: '年假申请',
    applicantName: CURRENT_USER_NAME,
    summary: '年假 · 3天 · 回老家探亲',
    status: 'approved',
    createdAt: daysAgo(28, 8, 0),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260505027',
    contextData: {
      type: 'leave',
      data: {
        annualTotal: 13,
        annualUsed: 3,
        annualRemaining: 10,
        sickBalance: 10,
        personalBalance: 5,
        yearLeaveCount: 1,
        consecutiveDaysWarning: false,
        lastLeaveDate: '2026-04-30',
        leaveTrend: [
          { month: '4月', days: 3, type: '年假' },
        ],
      },
    },
    formFields: [
      { label: '请假类型', value: '年假', type: 'select' },
      { label: '开始时间', value: '2026-04-28 09:00', type: 'date' },
      { label: '结束时间', value: '2026-04-30 18:00', type: 'date' },
      { label: '请假时长', value: 3, type: 'number' },
      { label: '请假原因', value: '清明节后回老家探望父母', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: CURRENT_USER_NAME, status: 'completed', action: '提交申请', time: fmt(daysAgo(28, 8, 0)) },
      { id: 'fn2', role: '门店店长', userName: '王建国', status: 'completed', action: '同意', time: fmt(daysAgo(27, 10, 0)) },
      { id: 'fn3', role: '人事审批', userName: '陈美华', status: 'completed', action: '同意', time: fmt(daysAgo(26, 14, 30)) },
      { id: 'fn4', role: '归档', userName: '系统', status: 'completed', action: '已归档', time: fmt(daysAgo(26, 14, 31)) },
    ],
    comments: [
      { id: 'c1', userId: 'u-me', userName: CURRENT_USER_NAME, content: '利用年假回老家看望父母，计划28-30号三天，工作已安排好交接。', createdAt: daysAgo(28, 8, 0) },
      { id: 'c2', userId: 'u-wangjianguo', userName: '王建国', content: '年假使用合理，交接安排妥当，同意休假。', createdAt: daysAgo(27, 10, 0) },
      { id: 'c3', userId: 'u-chenmeihua', userName: '陈美华', content: '年假余额充足，扣除3天后剩余5天，核准通过。', createdAt: daysAgo(26, 14, 30) },
    ],
  },

  {
    id: 'ap0028',
    type: 'overtime',
    title: '加班申请',
    applicantName: '杨洋',
    summary: '工作日加班 · 2小时',
    status: 'processing',
    createdAt: daysAgo(2, 19, 0),
    urgency: 'normal' as UrgencyLevel,
    approvalNo: 'AP20260531028',
    contextData: {
      type: 'overtime',
      data: {
        monthHours: 18,
        monthLimit: 36,
        monthUsagePercent: 50,
        quarterHours: 42,
        quarterLimit: 100,
        deptAvgHours: 12.3,
        compensateType: '调休',
        compensatoryBalance: 16,
        recentRecords: [
          { date: '05-28', hours: 2, type: '工作日加班' },
          { date: '05-25', hours: 3, type: '周末加班' },
          { date: '05-18', hours: 4, type: '工作日加班' },
          { date: '05-10', hours: 5, type: '节假日加班' },
          { date: '05-03', hours: 4, type: '节假日加班' },
        ],
      },
      riskWarning: {
        level: 'danger',
        category: 'health',
        title: '⚠️ 连续高频加班健康风险警告',
        description: '该员工近30天内已累计加班5次共18小时，且存在以下风险指标：\n• 近两周内出现3次工作日加班（05-10/18/28），间隔不足72小时\n• 05-03至05-10期间仅间隔3天即再次加班（节假日→工作日）\n• 本月工时使用率已达50%，按当前频率月底可能超限\n\n根据《劳动法》第四十一条及公司《员工健康保护制度》第三章，长时间高强度加班可能导致：\n· 心血管系统负荷过载（心源性猝死高风险因素）\n· 免疫力下降、慢性疲劳综合征\n· 认知能力下降，增加操作失误风险',
        evidence: '本月已加班18h/上限36h(50%) | 近30天加班5次 | 最短间隔3天 | 含2次节假日+3次工作日加班',
        suggestion: '建议审批人：(1) 核实本次加班的必要性，是否可通过任务优先级调整避免；(2) 如确需批准，建议强制要求申请人在本次加班后安排至少1天完整休息；(3) 建议HR在审批通过后7日内对该员工进行健康回访。',
        source: 'hr_system_auto',
        generatedAt: daysAgo(2, 19, 30),
        reviewerName: undefined,
        regulationRef: '《劳动法》第四十一条 / 公司《员工健康保护制度》第三章第十二条',
      },
    },
    formFields: [
      { label: '加班类型', value: '工作日加班', type: 'select' },
      { label: '加班日期', value: '2026-06-01', type: 'date' },
      { label: '加班时段', value: '19:00 - 21:00', type: 'text' },
      { label: '加班时长', value: 2, type: 'number' },
      { label: '加班原因', value: '月底结账日，财务系统对账延时至晚间完成', type: 'textarea' },
    ],
    flowNodes: [
      { id: 'fn1', role: '发起人', userName: '杨洋', status: 'completed', action: '提交申请', time: fmt(daysAgo(2, 19, 0)) },
      { id: 'fn2', role: '门店店长', userName: '王建国', status: 'completed', action: '同意', time: fmt(daysAgo(1, 10, 30)) },
      { id: 'fn3', role: 'HR备案', userName: '陈美华', status: 'current' },
    ],
    comments: [
      { id: 'c1', userId: 'u-yangyang', userName: '杨洋', content: '6月1日月结日，银行对账单晚间才到账，需加班完成对账和凭证录入。', createdAt: daysAgo(2, 19, 0) },
      { id: 'c2', userId: 'u-wangjianguo', userName: '王建国', content: '月结加班属于正常业务需要，同意。HR请及时备案以便核算加班费。', createdAt: daysAgo(1, 10, 30) },
    ],
  },
] as RawApproval[]

/** 初始化Mock数据库（仅在首次调用时填充数据） */
export function seedApprovalData(): void {
  if (!isDbEmpty()) return

  const allApprovals: ApprovalDetail[] = []

  for (const item of DATA) {
    // 为每条数据添加 applicantLevel（默认为员工层级）
    const approval = {
      ...item,
      applicantLevel: item.applicantLevel ?? UserLevel.STAFF,
    } as ApprovalDetail
    allApprovals.push(approval)
    setApproval(approval.id, approval)
  }

  // 管理者演示数据：将部分 pending/processing 审批的"L2主管"节点替换为管理者姓名
  // 确保管理者登录后能在"待我审批"中看到需要其审批的单据
  let managerAssignedCount = 0
  const TARGET_MANAGER_COUNT = 8
  for (const approval of allApprovals) {
    if (managerAssignedCount >= TARGET_MANAGER_COUNT) break
    if (approval.status !== 'pending' && approval.status !== 'processing') continue

    const updatedNodes = approval.flowNodes.map(node => {
      if ((node.role === 'L2主管' || node.role === 'L3经理') && node.userName !== MANAGER_NAME) {
        return { ...node, userName: MANAGER_NAME }
      }
      return node
    })

    if (updatedNodes !== approval.flowNodes) {
      setApproval(approval.id, { ...approval, flowNodes: updatedNodes })
      managerAssignedCount++
    }
  }

  // 层级化权限过滤：根据当前用户层级过滤可见数据
  // 注意：此处在 Mock 阶段模拟后端基于层级的过滤逻辑
  // 生产环境应由后端 API 根据用户层级返回对应数据
  applyLevelBasedFilter()
}

/**
 * 基于用户层级的可见性过滤（Mock阶段）
 * 
 * 过滤规则：
 * - L1(员工): 只能看到自己发起的申请
 * - L2(主管): 可以看到 L1 发起的待审批 + 自己发起的
 * - L3(经理): 可以看到 L2 发起的待审批 + 自己发起的 + L1 的
 * - L4+(区域经理/超级管理员): 可以看到所有待审批数据
 */
async function applyLevelBasedFilter(): Promise<void> {
  // 动态导入 permissionStore 以避免循环依赖
  try {
    const { usePermissionStore } = await import('@/stores/permission')
    const permission = usePermissionStore()
    const currentLevel = permission.userLevel

    // Mock阶段：仅在开发环境执行过滤，生产环境由后端处理
    if (typeof window !== 'undefined' && currentLevel) {
      // 此处预留过滤逻辑接口
      // 实际过滤在 API 调用层（approvalApi.getList）中根据 tab 参数执行
    }
  } catch {
    // Store 可能尚未初始化，忽略错误
  }
}

/**
 * 根据用户层级获取可见的审批数据ID列表
 * @param currentLevel - 当前用户层级
 * @param tab - 当前Tab类型
 * @returns 可见的审批ID列表（空数组表示无限制）
 */
export function getVisibleApprovalIdsByLevel(
  currentLevel: number,
  tab: string
): string[] | null {
  // 返回 null 表示不限制（显示所有数据）
  // 返回空数组表示无可见数据
  // 返回 ID 数组表示只显示这些数据
  
  // "我发起的" Tab - 所有层级都只能看到自己发起的（通过 applicantName=CURRENT_USER_NAME 过滤）
  if (tab === 'initiated') {
    return null // 不限制，由调用方根据 applicantName 过滤
  }
  
  // "待我审批" Tab - 基于层级过滤
  if (tab === 'pending') {
    switch (currentLevel) {
      case UserLevel.STAFF:
        // L1 没有审批权限，看不到待审批
        return []
      case UserLevel.SUPERVISOR:
        // L2 只能看到 L1(员工)发起的待审批单据
        return null // 由调用方过滤 applicantLevel <= 1
      case UserLevel.MANAGER:
        // L3 可以看到 L1+L2 发起的待审批单据
        return null // 由调用方过滤 applicantLevel <= 2
      case UserLevel.REGION_MANAGER:
      case UserLevel.ADMIN:
        // L4/L5 可以看到所有待审批单据
        return null
      default:
        return null
    }
  }
  
  // 其他 Tab 不做层级限制
  return null
}
