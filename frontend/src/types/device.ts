/**
 * 设备相关类型定义
 * 对应后端实体: Device (devices 表)
 *
 * 后端使用 Integer 编码，前端使用语义化字符串，
 * 通过 DataConverter 在 API 边界完成转换（见 src/api/device/index.ts）。
 */

/**
 * 设备类型枚举（后端 1-5）
 */
export type DeviceType =
  | 'PRINTER'   // 打印机（后端 1）
  | 'SCANNER'   // 扫码枪（后端 2）
  | 'SCALE'     // 称重秤（后端 3）
  | 'LOCKER'    // 取餐柜（后端 4）
  | 'OTHER';    // 其他（后端 5）

/**
 * 设备连接类型枚举（后端 1-4）
 */
export type DeviceConnectionType =
  | 'USB'        // USB（后端 1）
  | 'SERIAL'     // 串口（后端 2）
  | 'NETWORK'    // 网络（后端 3）
  | 'BLUETOOTH'; // 蓝牙（后端 4）

/**
 * 设备状态枚举（后端 0-3）
 */
export type DeviceStatus =
  | 'offline'      // 离线（后端 0）
  | 'online'       // 在线（后端 1）
  | 'fault'        // 故障（后端 2）
  | 'maintenance'; // 维护中（后端 3）

/**
 * 设备信息（对应后端 DeviceVO，字段已转换为前端语义化）
 */
export interface DeviceInfo {
  /** 设备ID（主键） */
  deviceId: number;
  /** 设备编号 */
  deviceCode: string;
  /** 设备名称 */
  deviceName: string;
  /** 设备类型 */
  deviceType: DeviceType;
  /** 设备型号 */
  deviceModel: string;
  /** 厂商 */
  manufacturer: string;
  /** 序列号 */
  serialNo: string;
  /** 连接方式 */
  connectionType: DeviceConnectionType;
  /** 连接参数（JSON字符串，如IP地址、端口等） */
  connectionParams: string;
  /** 安装位置 */
  location: string;
  /** 所属门店ID */
  storeId: number;
  /** 状态 */
  status: DeviceStatus;
  /** 最后心跳时间 */
  lastHeartbeatTime: string;
  /** 最后在线时间 */
  lastOnlineTime: string;
  /** 备注 */
  remark: string;
}

/**
 * 设备表单数据（用于新建/编辑）
 */
export interface DeviceFormData {
  /** 设备编号 */
  deviceCode: string;
  /** 设备名称 */
  deviceName: string;
  /** 设备类型 */
  deviceType: DeviceType;
  /** 设备型号 */
  deviceModel: string;
  /** 厂商 */
  manufacturer: string;
  /** 序列号 */
  serialNo: string;
  /** 连接方式 */
  connectionType: DeviceConnectionType;
  /** 连接参数（JSON字符串） */
  connectionParams: string;
  /** 安装位置 */
  location: string;
  /** 所属门店ID */
  storeId: number;
  /** 备注 */
  remark: string;
}

/**
 * 设备查询参数
 */
export interface DeviceQueryForm {
  /** 关键词（设备名称/编号模糊搜索） */
  keyword?: string;
  /** 设备类型 */
  deviceType?: DeviceType | null;
  /** 状态 */
  status?: DeviceStatus | null;
  /** 所属门店ID */
  storeId?: number | null;
}

/** 设备类型选项 */
export const DeviceTypeOptions = [
  { label: '打印机', value: 'PRINTER' },
  { label: '扫码枪', value: 'SCANNER' },
  { label: '称重秤', value: 'SCALE' },
  { label: '取餐柜', value: 'LOCKER' },
  { label: '其他', value: 'OTHER' },
] as const;

/** 设备类型文本映射 */
export const DeviceTypeText: Record<DeviceType, string> = {
  PRINTER: '打印机',
  SCANNER: '扫码枪',
  SCALE: '称重秤',
  LOCKER: '取餐柜',
  OTHER: '其他',
};

/** 设备连接类型选项 */
export const DeviceConnectionTypeOptions = [
  { label: 'USB', value: 'USB' },
  { label: '串口', value: 'SERIAL' },
  { label: '网络', value: 'NETWORK' },
  { label: '蓝牙', value: 'BLUETOOTH' },
] as const;

/** 设备连接类型文本映射 */
export const DeviceConnectionTypeText: Record<DeviceConnectionType, string> = {
  USB: 'USB',
  SERIAL: '串口',
  NETWORK: '网络',
  BLUETOOTH: '蓝牙',
};

/** 设备状态选项 */
export const DeviceStatusOptions = [
  { label: '在线', value: 'online' },
  { label: '离线', value: 'offline' },
  { label: '故障', value: 'fault' },
  { label: '维护中', value: 'maintenance' },
] as const;

/** 设备状态文本映射 */
export const DeviceStatusText: Record<DeviceStatus, string> = {
  online: '在线',
  offline: '离线',
  fault: '故障',
  maintenance: '维护中',
};

/**
 * 设备状态 → StatusTag status 映射
 * 用于 StatusTag 组件显示（fault 映射到 error 颜色分类）
 */
export const DeviceStatusColor: Record<DeviceStatus, string> = {
  online: 'online',
  offline: 'offline',
  fault: 'error',
  maintenance: 'maintenance',
};

// ============================================================
// 设备告警类型（对应后端 DeviceAlert / device_alerts 表）
// ============================================================

/**
 * 告警类型枚举（后端 1-5）
 */
export type DeviceAlertType =
  | 'offline_timeout'   // 离线超时（后端 1）
  | 'paper_out'          // 纸张缺（后端 2）
  | 'ribbon_out'         // 碳带缺（后端 3）
  | 'fault'              // 故障（后端 4）
  | 'maintenance_reminder'; // 维护提醒（后端 5）

/**
 * 告警级别枚举（后端 1-4）
 */
export type DeviceAlertLevel =
  | 'info'      // 信息（后端 1）
  | 'warning'   // 警告（后端 2）
  | 'critical'  // 严重（后端 3）
  | 'urgent';   // 紧急（后端 4）

/**
 * 告警状态枚举（后端 0-3）
 */
export type DeviceAlertStatus =
  | 'pending'   // 未处理（后端 0）
  | 'handling'  // 处理中（后端 1）
  | 'resolved'  // 已解决（后端 2）
  | 'ignored';   // 已忽略（后端 3）

/**
 * 设备告警信息（对应后端 DeviceAlertVO）
 */
export interface DeviceAlertInfo {
  alertId: number;
  deviceId: number;
  deviceName: string;
  deviceType: DeviceType | null;
  alertType: DeviceAlertType;
  alertLevel: DeviceAlertLevel;
  alertMessage: string;
  alertStatus: DeviceAlertStatus;
  isHandled: boolean;
  triggerTime: string;
  handleTime: string;
  handleResult: string;
  handleUserId: number | null;
  resolveTime: string;
  deviceStatusSnapshot: string;
  createTime: string;
}

/**
 * 告警查询参数
 */
export interface DeviceAlertQueryForm {
  deviceId?: number | null;
  alertType?: DeviceAlertType | null;
  alertLevel?: DeviceAlertLevel | null;
  alertStatus?: DeviceAlertStatus | null;
  isHandled?: boolean | null;
  startTime?: string;
  endTime?: string;
}

/** 告警类型选项 */
export const DeviceAlertTypeOptions = [
  { label: '离线超时', value: 'offline_timeout' },
  { label: '纸张缺', value: 'paper_out' },
  { label: '碳带缺', value: 'ribbon_out' },
  { label: '故障', value: 'fault' },
  { label: '维护提醒', value: 'maintenance_reminder' },
] as const;

/** 告警类型文本映射 */
export const DeviceAlertTypeText: Record<DeviceAlertType, string> = {
  offline_timeout: '离线超时',
  paper_out: '纸张缺',
  ribbon_out: '碳带缺',
  fault: '故障',
  maintenance_reminder: '维护提醒',
};

/** 告警级别选项 */
export const DeviceAlertLevelOptions = [
  { label: '信息', value: 'info' },
  { label: '警告', value: 'warning' },
  { label: '严重', value: 'critical' },
  { label: '紧急', value: 'urgent' },
] as const;

/** 告警级别文本映射 */
export const DeviceAlertLevelText: Record<DeviceAlertLevel, string> = {
  info: '信息',
  warning: '警告',
  critical: '严重',
  urgent: '紧急',
};

/** 告警级别 → StatusTag status 映射 */
export const DeviceAlertLevelColor: Record<DeviceAlertLevel, string> = {
  info: 'info',
  warning: 'warning',
  critical: 'error',
  urgent: 'error',
};

/** 告警状态选项 */
export const DeviceAlertStatusOptions = [
  { label: '未处理', value: 'pending' },
  { label: '处理中', value: 'handling' },
  { label: '已解决', value: 'resolved' },
  { label: '已忽略', value: 'ignored' },
] as const;

/** 告警状态文本映射 */
export const DeviceAlertStatusText: Record<DeviceAlertStatus, string> = {
  pending: '未处理',
  handling: '处理中',
  resolved: '已解决',
  ignored: '已忽略',
};

/** 告警状态 → StatusTag status 映射 */
export const DeviceAlertStatusColor: Record<DeviceAlertStatus, string> = {
  pending: 'error',
  handling: 'warning',
  resolved: 'online',
  ignored: 'inactive',
};

// ============================================================
// 设备状态历史类型（对应后端 DeviceStatusHistory / device_status_history 表）
// ============================================================

/**
 * 设备状态历史记录
 * 注意：后端 DeviceStatusHistory 实体使用 String deviceType（如 "PRINTER"），
 * 与 Device 实体的 Integer deviceType 不同，此处直接保留字符串。
 */
export interface DeviceStatusHistoryInfo {
  id: number;
  deviceType: string;
  deviceId: number;
  deviceName: string;
  deviceModel: string;
  online: boolean;
  responseTime: number;
  firmwareVersion: string;
  connectionType: string;
  ipAddress: string;
  port: string;
  checkTime: string;
  details: string;
  errorMessage: string;
  storeId: number | null;
  createdAt: string;
}

/**
 * 状态历史查询参数
 */
export interface DeviceStatusHistoryQueryForm {
  deviceType?: string;
  deviceId?: number | null;
  startTime?: string;
  endTime?: string;
  storeId?: number | null;
  pageSize?: number;
  pageNum?: number;
}

/**
 * 设备监控统计信息（对应后端 Map<String, Long>）
 */
export interface DeviceMonitorStatistics {
  total: number;
  online: number;
  offline: number;
  fault: number;
  maintenance: number;
}

/**
 * 最近状态变更记录（对应后端 List<Map<String, Object>>）
 */
export interface DeviceRecentStatusChange {
  deviceId: number;
  deviceName: string;
  oldStatus: string;
  newStatus: string;
  changeTime: string;
}
