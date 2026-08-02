// 健康证管理相关工具函
import type { HealthCertificate, HealthCertificateStatus } from '@/types/healthCertificate';

// 状态类型映射
export const getStatusType = (
  status: string,
): 'primary' | 'success' | 'warning' | 'danger' | 'info' => {
  const statusMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    valid: 'success',
    expiring: 'warning',
    expired: 'danger'
  };
  return statusMap[status] || 'info';
};

// 状态文本映射
export const getStatusText = (status: string): string => {

  const statusMap: Record<string, string> = {
    valid: '有效',
    expiring: '即将过期',
    expired: '已过'
};
  return statusMap[status] || status;
};

// 报销状态类型映
export const getExpenseStatusType = (
  status: string,
): 'primary' | 'success' | 'warning' | 'danger' | 'info' => {
  const statusMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    pending: 'warning',
    approved: 'primary',
    reimbursed: 'success',
    rejected: 'danger'
  };
  return statusMap[status] || 'info';
};

// 报销状态文本映
export const getExpenseStatusText = (status: string): string => {

  const statusMap: Record<string, string> = {
    pending: '待审核',
    approved: '已审核',
    reimbursed: '已报销',
    rejected: '已拒绝'
};
  return statusMap[status] || status;
};

// 操作类型标签映射
export const getOperationTypeTag = (operation: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' => {
  const operationMap: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    '新增健康证': 'primary',
    '编辑健康证': 'success',
    '删除健康证': 'danger',
    '取消删除健康证': 'info',
    '申请报销': 'warning',
    '查看健康证': 'info'
  };
  return operationMap[operation] || 'info';
};

// 计算两个日期之间的天
export const calculateDaysBetween = (startDate: string, endDate: string):
number => {

  const start = new Date(startDate);
  const end = new Date(endDate);
  const diffTime = end.getTime() - start.getTime();
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  return diffDays;
};

// 计算有效期进度百分比
export const calculateExpiryProgress = (expiryDate: string, issueDate: string):
number => {
  const issue = new Date(issueDate);
  const expiry = new Date(expiryDate);
  const today = new Date();
  
  const totalDays = expiry.getTime() - issue.getTime();
  const passedDays = today.getTime() - issue.getTime();
  
  if (totalDays <= 0) return 100;
  if (passedDays <= 0) return 0;
  if (passedDays >= totalDays) return 100;
  
  return Math.round((passedDays / totalDays) * 100);
};

// 获取状态态对应的颜色
export const getStatusColor = (status: string): string => {
  const colorMap: Record<string, string> = {
    valid: '#10b981',
    expiring: '#f59e0b',
    expired: '#ef'
  };
  return colorMap[status] || '#6b';
};

// 计算健康证状态
export const calculateHealthCertStatus = (expiryDate: string, expiryReminderDays: number):
HealthCertificateStatus => {

  const today = new Date();
  const expiry = new Date(expiryDate);
  const diffTime = expiry.getTime() - today.getTime();
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  
  if (diffDays <= 0) {
    return 'expired';
  } else if (diffDays <= expiryReminderDays) {
    return 'expiring';
  } else {
    return 'valid';
  }
};

// 归档超过30天的已删除健康证证
export const archiveDeletedHealthCertificates = (certificates: HealthCertificate[]) => {
  const today = new Date();
  const thirtyDaysAgo = new Date();
  thirtyDaysAgo.setDate(today.getDate() - 30);
  
  // 筛选出已删除且删除时间超过30天的健康
const certificatesToArchive = certificates.filter(cert => {
    if (!cert.deleted || !cert.deletedAt) return false;
    
    const deletedDate = new Date(cert.deletedAt);
    return deletedDate < thirtyDaysAgo;
  });
  
  // 这里可以添加归档逻辑，例如将这些健康证移动到归档表或文件
  // 目前我们只返回需要归档的健康证列表
  return certificatesToArchive;
};
