import {
  NotificationType,
  NotificationTypeIcon,
  NotificationTypeText,
} from "@/types/notification-config";

export function getIconComponent(type: NotificationType): string {
  return NotificationTypeIcon[type] || "Bell";
}

export function getIconColor(type: NotificationType): string {
  const colorMap: Record<string, string> = {
    SYSTEM: "var(--fts-color-primary)",
    REJECT_AUDIT: "var(--fts-color-danger)",
    APPROVE_AUDIT: "var(--fts-color-success)",
    PENDING_AUDIT: "var(--fts-color-warning)",
    INVENTORY_WARNING: "var(--fts-color-warning)",
    CONTRACT_EXPIRY: "var(--fts-color-warning)",
    HEALTH_CERT_EXPIRY: "var(--fts-color-warning)",
    TASK_ASSIGN: "var(--fts-color-primary)",
    TASK_COMPLETE: "var(--fts-color-success)",
    ORDER_STATUS: "var(--fts-color-info)",
  };
  return colorMap[type] || "var(--fts-color-info)";
}

export function formatTime(time: string | null | undefined): string {
  if (!time) return "";

  const date = new Date(time);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const seconds = Math.floor(diff / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);

  if (seconds < 60) return "刚刚";
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 30) return `${days}天前`;

  return date.toLocaleDateString("zh-CN");
}

export const typeToFilterMap: Record<string, NotificationType[]> = {
  all: [
    "SYSTEM",
    "REJECT_AUDIT",
    "APPROVE_AUDIT",
    "PENDING_AUDIT",
    "INVENTORY_WARNING",
    "CONTRACT_EXPIRY",
    "HEALTH_CERT_EXPIRY",
    "TASK_ASSIGN",
    "TASK_COMPLETE",
    "ORDER_STATUS",
  ],
  info: ["SYSTEM", "APPROVE_AUDIT", "TASK_COMPLETE", "ORDER_STATUS"],
  warning: [
    "PENDING_AUDIT",
    "INVENTORY_WARNING",
    "CONTRACT_EXPIRY",
    "HEALTH_CERT_EXPIRY",
  ],
  success: ["APPROVE_AUDIT", "TASK_COMPLETE"],
  error: ["REJECT_AUDIT"],
};

export function getTypesByFilter(filter: string): NotificationType[] {
  return typeToFilterMap[filter] || typeToFilterMap.all;
}

export function getTypeLabel(type: NotificationType): string {
  return NotificationTypeText[type] || "未知类型";
}
