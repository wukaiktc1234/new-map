/**
 * 认证模块类型定义
 * 从 api/auth.ts 迁移而来
 */

// 登录接口
export interface LoginRequest {
  username: string;
  password: string;
  captcha: string;
  captchaId: string;
  rememberMe?: boolean;
}

export interface LoginResponse {
  token: string;
  refreshToken?: string;
  userInfo: {
    id: string | number;
    username: string;
    name: string;
    email: string;
    avatar: string;
    roles: string[];
    permissions: string[];
  };
}

// 用户详情接口
export interface UserInfoResponse {
  id: string | number;
  username: string;
  name: string;
  email: string;
  avatar: string;
  phone: string;
  status: number;
  roles: string[];
  permissions: string[];
  createdAt: string;
  updatedAt: string;
}

// 注册接口
export interface RegisterRequest {
  username: string;
  name?: string;
  password: string;
  confirmPassword: string;
  email: string;
  phone?: string;
  registrationCode?: string;
  captcha: string;
  captchaId: string;
}

export interface RegisterResponse {
  id: number;
  username: string;
  email: string;
}

// 修改密码接口
export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

// 重置密码接口
export interface ResetPasswordRequest {
  email: string;
  captcha: string;
  captchaId: string;
  newPassword: string;
}

// 更新用户信息接口
export interface UpdateUserRequest {
  name?: string;
  email?: string;
  phone?: string;
  avatar?: string;
}

// 验证码生成接口
export interface CaptchaResponse {
  captchaId: string;
  captchaImage: string;
}

// 权限相关接口
export interface PermissionResponse {
  permissions: string[];
  roles: string[];
}
