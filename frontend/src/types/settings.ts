/**
 * 系统设置模块 - 类型定义
 */

export interface SettingsMenu {
  key: string;
  title: string;
  icon: string;
  subtitle?: string;
  tabs?: SettingsTab[];
  component?: () => Promise<unknown>;
  children?: SettingsMenu[];
  permission?: string;
}

export interface SettingsTab {
  key: string;
  label: string;
  permission?: string;
}

export interface SettingsState {
  activeMenu: string;
  activeTab: string;
  sidebarCollapsed: boolean;
}
