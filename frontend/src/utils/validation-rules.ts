/**
 * 公共验证规则文件
 * 提供常用的表单验证规则
 */
import type { FormRule } from '@/types/common-types';

/**
 * 必填验证规则
 */
export const requiredRule = (message: string = '此项不能为空'): FormRule => ({
  required: true,
  message,
  trigger: 'blur'
});

/**
 * 必选验证规则（用于选择器）
 */
export const requiredSelectRule = (message: string = '请选择此项'): FormRule => ({
  required: true,
  message,
  trigger: 'change'
});

/**
 * 长度验证规则
 */
export const lengthRule = (min: number, max: number, message: string): FormRule => ({
  min,
  max,
  message: message || `长度在 ${min} 到 ${max} 个字符`,
  trigger: 'blur'
});

/**
 * 最小长度验证规则
 */
export const minLengthRule = (min: number, message: string): FormRule => ({
  min,
  message: message || `长度不能少于 ${min} 个字符`,
  trigger: 'blur'
});

/**
 * 最大长度验证规则
 */
export const maxLengthRule = (max: number, message: string): FormRule => ({
  max,
  message: message || `长度不能超过 ${max} 个字符`,
  trigger: 'blur'
});

/**
 * 邮箱验证规则
 */
export const emailRule = (message: string = '请输入正确的邮箱地址'): FormRule => ({
  type: 'email',
  message,
  trigger: 'blur'
});

/**
 * 手机号验证规则
 */
export const phoneRule = (message: string = '请输入正确的手机号码'): FormRule => ({
  pattern: /^1[3-9]\d{9}$/,
  message,
  trigger: 'blur'
});

/**
 * 固定电话验证规则
 */
export const landlineRule = (message: string = '请输入正确的固定电话号码'): FormRule => ({
  pattern: /^0\d{2,3}-\d{7,8}$/,
  message,
  trigger: 'blur'
});

/**
 * 身份证号验证规则
 */
export const idCardRule = (message: string = '请输入正确的身份证号'): FormRule => ({
  pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/,
  message,
  trigger: 'blur'
});

/**
 * 密码验证规则
 */
export const passwordRule = (message: string = '密码至少8位，且包含字母和数字'): FormRule => ({
  pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/,
  message,
  trigger: 'blur'
});

/**
 * 强密码验证规则
 */
export const strongPasswordRule = (message: string = '密码至少8位，包含大小写字母、数字和特殊字符'): FormRule => ({
  pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*&])[A-Za-z\d@$!%*&]{8,}$/,
  message,
  trigger: 'blur'
});

/**
 * 数字验证规则
 */
export const numberRule = (message: string = '请输入数字'): FormRule => ({
  pattern: /^\d+$/,
  message,
  trigger: 'blur'
});

/**
 * 正整数验证规则
 */
export const positiveIntegerRule = (message: string = '请输入正整数'): FormRule => ({
  pattern: /^[1-9]\d*$/,
  message,
  trigger: 'blur'
});

/**
 * 非负整数验证规则
 */
export const nonNegativeIntegerRule = (message: string = '请输入非负整数'): FormRule => ({
  pattern: /^(0|[1-9]\d*)$/,
  message,
  trigger: 'blur'
});

/**
 * 小数验证规则
 */
export const decimalRule = (message: string = '请输入正确的小数'): FormRule => ({
  pattern: /^\d+(\.\d+)$/,
  message,
  trigger: 'blur'
});

/**
 * 正数验证规则
 */
export const positiveNumberRule = (message: string = '请输入正数'): FormRule => ({
  pattern: /^([1-9]\d*\.\d*)|(0\.\d*[1-9])$/,
  message,
  trigger: 'blur'
});

/**
 * 非负数验证规则
 */
export const nonNegativeNumberRule = (message: string = '请输入非负数'): FormRule => ({
  pattern: /^(0|[1-9]\d*\.\d*)$/,
  message,
  trigger: 'blur'
});

/**
 * 金额验证规则（最多两位小数）
 */
export const amountRule = (message: string = '请输入正确的金额'): FormRule => ({
  pattern: /^([1-9]\d*\.\d{0,2})|(0\.\d*[1-9]\d)$/,
  message,
  trigger: 'blur'
});

/**
 * URL验证规则
 */
export const urlRule = (message: string = '请输入正确的URL地址'): FormRule => ({
  pattern: /^https:\/\/.+/,
  message,
  trigger: 'blur'
});

/**
 * IP地址验证规则
 */
export const ipRule = (message: string = '请输入正确的IP地址'): FormRule => ({
  pattern: /^(\d{1,3}\.){3}\d{1,3}$/,
  message,
  trigger: 'blur'
});

/**
 * 端口验证规则
 */
export const portRule = (message: string = '请输入正确的端口号'): FormRule => ({
  pattern: /^([1-9]\d{0,3}|[1-5]\d{4}|6[0-4]\d{3}|65[0-4]\d{2}|655[0-2]\d|6553[0-5])$/,
  message,
  trigger: 'blur'
});

/**
 * 日期验证规则
 */
export const dateRule = (message: string = '请输入正确的日期格式（YYYY-MM-DD）'): FormRule => ({
  pattern: /^\d{4}-\d{2}-\d{2}$/,
  message,
  trigger: 'blur'
});

/**
 * 时间验证规则
 */
export const timeRule = (message: string = '请输入正确的时间格式（HH:mm:ss）'): FormRule => ({
  pattern: /^([01]\d|2[0-3]):[0-5]\d:[0-5]\d$/,
  message,
  trigger: 'blur'
});

/**
 * 日期时间验证规则
 */
export const dateTimeRule = (message: string = '请输入正确的日期时间格式（YYYY-MM-DD HH:mm:ss）'): FormRule => ({
  pattern: /^\d{4}-\d{2}-\d{2} ([01]\d|2[0-3]):[0-5]\d:[0-5]\d$/,
  message,
  trigger: 'blur'
});

/**
 * 中文名称验证规则
 */
export const chineseNameRule = (message: string = '请输入正确的中文姓名'): FormRule => ({
  pattern: /^[\u4e00-\u9fa5]{2,10}$/,
  message,
  trigger: 'blur'
});

/**
 * 英文名称验证规则
 */
export const englishNameRule = (message: string = '请输入正确的英文姓名'): FormRule => ({
  pattern: /^[A-Za-z\s]{2,50}$/,
  message,
  trigger: 'blur'
});

/**
 * 用户名验证规则
 */
export const usernameRule = (message: string = '用户名由字母、数字、下划线组成（4-20位）'): FormRule => ({
  pattern: /^[a-zA-Z0-9_]{4,20}$/,
  message,
  trigger: 'blur'
});

/**
 * 邮政编码验证规则
 */
export const postalCodeRule = (message: string = '请输入正确的邮政编码'): FormRule => ({
  pattern: /^[1-9]\d{5}$/,
  message,
  trigger: 'blur'
});

/**
 * 车牌号验证规则
 */
export const licensePlateRule = (message: string = '请输入正确的车牌号'): FormRule => ({
  pattern: /^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领A-Z][A-Z][A-Z0-9]{4,5}[A-Z0-9挂学警港澳]$/,
  message,
  trigger: 'blur'
});

/**
 * 社会统一信用代码验证规则
 */
export const creditCodeRule = (message: string = '请输入正确的统一社会信用代码'): FormRule => ({
  pattern: /^[0-9A-HJ-NPQRTUWXY]{2}\d{6}[0-9A-HJ-NPQRTUWXY]{10}$/,
  message,
  trigger: 'blur'
});

/**
 * QQ号验证规则
 */
export const qqRule = (message: string = '请输入正确的QQ号'): FormRule => ({
  pattern: /^[1-9]\d{4,10}$/,
  message,
  trigger: 'blur'
});

/**
 * 微信号验证规则
 */
export const wechatRule = (message: string = '请输入正确的微信号'): FormRule => ({
  pattern: /^[a-zA-Z][-_a-zA-Z0-9]{5,19}$/,
  message,
  trigger: 'blur'
});

/**
 * 银行卡号验证规则
 */
export const bankCardRule = (message: string = '请输入正确的银行卡号'): FormRule => ({
  pattern: /^[1-9]\d{12,19}$/,
  message,
  trigger: 'blur'
});

/**
 * 年龄验证规则
 */
export const ageRule = (message: string = '请输入正确的年龄（1-120岁）'): FormRule => ({
  pattern: /^(1[0-1]\d|[1-9]\d)$/,
  message,
  trigger: 'blur'
});

/**
 * 自定义正则验证规则
 */
export const patternRule = (pattern: RegExp, message: string): FormRule => ({
  pattern,
  message,
  trigger: 'blur'
});

/**
 * 自定义验证器
 */
export const customValidator = (
  validator: (rule: FormRule, value: unknown, callback: (error: Error) => void) => void,
  trigger: 'blur' | 'change' = 'blur'
): FormRule => ({
  validator,
  trigger
});

/**
 * 常用表单验证规则集合
 */
export const commonRules = {
  required: requiredRule,
  requiredSelect: requiredSelectRule,
  length: lengthRule,
  minLength: minLengthRule,
  maxLength: maxLengthRule,
  email: emailRule,
  phone: phoneRule,
  landline: landlineRule,
  idCard: idCardRule,
  password: passwordRule,
  strongPassword: strongPasswordRule,
  number: numberRule,
  positiveInteger: positiveIntegerRule,
  nonNegativeInteger: nonNegativeIntegerRule,
  decimal: decimalRule,
  positiveNumber: positiveNumberRule,
  nonNegativeNumber: nonNegativeNumberRule,
  amount: amountRule,
  url: urlRule,
  ip: ipRule,
  port: portRule,
  date: dateRule,
  time: timeRule,
  dateTime: dateTimeRule,
  chineseName: chineseNameRule,
  englishName: englishNameRule,
  username: usernameRule,
  postalCode: postalCodeRule,
  licensePlate: licensePlateRule,
  creditCode: creditCodeRule,
  qq: qqRule,
  wechat: wechatRule,
  bankCard: bankCardRule,
  age: ageRule,
  pattern: patternRule,
  custom: customValidator
};

/**
 * 员工表单验证规则
 */
export const employeeFormRules = {
  name: [requiredRule('请输入员工姓名'), chineseNameRule()],
  gender: [requiredSelectRule('请选择性别')],
  departmentName: [requiredRule('请输入部门名称')],
  positionName: [requiredRule('请输入职位名称')],
  phone: [phoneRule()],
  email: [emailRule()],
  hireDate: [requiredRule('请选择入职日期'), dateRule()],
  status: [requiredSelectRule('请选择员工状态')]
};

/**
 * 财务报表验证规则
 */
export const financeReportFormRules = {
  reportType: [requiredSelectRule('请选择报表类型')],
  periodType: [requiredSelectRule('请选择周期类型')],
  dateRange: [requiredRule('请选择日期范围')],
  departmentId: [],
  projectId: [],
  accountSubjectId: []
};

/**
 * 健康证表单验证规则
 */
export const healthCertificateFormRules = {
  employeeId: [requiredRule('请选择员工')],
  certificateNo: [requiredRule('请输入健康证编号')],
  issueDate: [requiredRule('请选择发证日期'), dateRule()],
  expiryDate: [requiredRule('请选择有效期'), dateRule()],
  issuingAuthority: [requiredRule('请输入发证机构')],
  status: [requiredSelectRule('请选择状态')]
};

/**
 * 菜单表单验证规则
 */
export const menuFormRules = {
  name: [requiredRule('请输入菜单名称')],
  code: [requiredRule('请输入菜单编码')],
  price: [requiredRule('请输入价格'), amountRule()],
  category: [requiredSelectRule('请选择分类')],
  status: [requiredSelectRule('请选择状态')]
};

/**
 * 用户表单验证规则
 */
export const userFormRules = {
  username: [requiredRule('请输入用户名'), usernameRule()],
  password: [requiredRule('请输入密码'), passwordRule()],
  name: [requiredRule('请输入姓名'), chineseNameRule()],
  email: [emailRule()],
  phone: [phoneRule()],
  status: [requiredSelectRule('请选择状态')]
};

/**
 * 角色表单验证规则
 */
export const roleFormRules = {
  name: [requiredRule('请输入角色名称')],
  code: [requiredRule('请输入角色编码')],
  description: [],
  status: [requiredSelectRule('请选择状态')]
};

/**
 * 部门表单验证规则
 */
export const deptFormRules = {
  name: [requiredRule('请输入部门名称')],
  code: [requiredRule('请输入部门编码')],
  leader: [],
  phone: [phoneRule()],
  email: [emailRule()],
  status: [requiredSelectRule('请选择状态')]
};

/**
 * 字典表单验证规则
 */
export const dictFormRules = {
  name: [requiredRule('请输入字典名称')],
  code: [requiredRule('请输入字典编码')],
  description: [],
  status: [requiredSelectRule('请选择状态')]
};

/**
 * 字典项表单验证规则
 */
export const dictItemFormRules = {
  label: [requiredRule('请输入字典项标签')],
  value: [requiredRule('请输入字典项值')],
  sort: [positiveIntegerRule()],
  status: [requiredSelectRule('请选择状态')]
};

/**
 * 系统配置表单验证规则
 */
export const configFormRules = {
  name: [requiredRule('请输入配置名称')],
  code: [requiredRule('请输入配置编码')],
  value: [requiredRule('请输入配置值')],
  description: [],
  type: [requiredSelectRule('请选择配置类型')],
  isSystem: []
};

/**
 * 默认导出所有规则
 */
export default {
  ...commonRules,
  employeeFormRules,
  financeReportFormRules,
  healthCertificateFormRules,
  menuFormRules,
  userFormRules,
  roleFormRules,
  deptFormRules,
  dictFormRules,
  dictItemFormRules,
  configFormRules
};
