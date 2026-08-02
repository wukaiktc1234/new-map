export type NotificationType =
  | "SYSTEM"
  | "REJECT_AUDIT"
  | "APPROVE_AUDIT"
  | "PENDING_AUDIT"
  | "INVENTORY_WARNING"
  | "CONTRACT_EXPIRY"
  | "HEALTH_CERT_EXPIRY"
  | "TASK_ASSIGN"
  | "TASK_COMPLETE"
  | "ORDER_STATUS";

export type NotificationReadStatus = 0 | 1;

export type NotificationPriority = "LOW" | "NORMAL" | "HIGH" | "URGENT";

export interface NotificationConfig {
  id: number;
  userId: number;
  title: string;
  content: string;
  type: NotificationType;
  businessId: number | null;
  businessType: string | null;
  isRead: NotificationReadStatus;
  priority: NotificationPriority;
  senderId: number | null;
  senderName: string | null;
  extraData: string | null;
  readTime: string | null;
  createTime: string;
  updateTime: string;
}

export interface NotificationQueryForm {
  type?: NotificationType;
  isRead?: NotificationReadStatus;
  title?: string;
  startTime?: string;
  endTime?: string;
  priority?: NotificationPriority;
}

export const NotificationTypeOptions: { label: string; value: NotificationType }[] = [
  { label: "系统通知", value: "SYSTEM" },
  { label: "审批拒绝", value: "REJECT_AUDIT" },
  { label: "审批通过", value: "APPROVE_AUDIT" },
  { label: "待审核", value: "PENDING_AUDIT" },
  { label: "库存预警", value: "INVENTORY_WARNING" },
  { label: "合同到期", value: "CONTRACT_EXPIRY" },
  { label: "健康证到期", value: "HEALTH_CERT_EXPIRY" },
  { label: "任务分配", value: "TASK_ASSIGN" },
  { label: "任务完成", value: "TASK_COMPLETE" },
  { label: "订单状态", value: "ORDER_STATUS" },
];

export const NotificationTypeText: Record<NotificationType, string> = {
  SYSTEM: "系统通知",
  REJECT_AUDIT: "审批拒绝",
  APPROVE_AUDIT: "审批通过",
  PENDING_AUDIT: "待审核",
  INVENTORY_WARNING: "库存预警",
  CONTRACT_EXPIRY: "合同到期",
  HEALTH_CERT_EXPIRY: "健康证到期",
  TASK_ASSIGN: "任务分配",
  TASK_COMPLETE: "任务完成",
  ORDER_STATUS: "订单状态",
};

export const NotificationTypeIcon: Record<NotificationType, string> = {
  SYSTEM: "Bell",
  REJECT_AUDIT: "CircleClose",
  APPROVE_AUDIT: "CircleCheck",
  PENDING_AUDIT: "Clock",
  INVENTORY_WARNING: "Warning",
  CONTRACT_EXPIRY: "Document",
  HEALTH_CERT_EXPIRY: "FirstAidKit",
  TASK_ASSIGN: "Plus",
  TASK_COMPLETE: "SuccessFilled",
  ORDER_STATUS: "ShoppingCart",
};

export const NotificationPriorityText: Record<NotificationPriority, string> = {
  LOW: "低",
  NORMAL: "普通",
  HIGH: "高",
  URGENT: "紧急",
};

export const NotificationIsReadOptions = [
  { label: "未读", value: 0 },
  { label: "已读", value: 1 },
];

export const NotificationIsReadText: Record<NotificationReadStatus, string> = {
  0: "未读",
  1: "已读",
};
