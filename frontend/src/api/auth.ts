/**
 * 认证相关 API
 * 登录、注册、令牌管理等
 */

import { post, get, put } from './request'

/** 登录请求参数 */
export interface LoginParams {
  username: string
  password: string
  captcha?: string
  captchaId?: string
  rememberMe?: boolean
}

/** 用户信息 */


export interface UserInfo {
  id: string
  username: string
  name?: string
  email?: string
  avatar?: string
  roles: string[]
  permissions: string[]
}

/** 登录响应 */
export interface LoginResult {
  token: string
  refreshToken: string
  userInfo: UserInfo
  requireCaptcha?: boolean
  captchaId?: string
  captchaImage?: string
  failedAttempts?: number
}

/** 验证码响应 */
export interface CaptchaResult {
  captchaId: string
  captchaImage: string
}

/** 邀请码校验响应 */
export interface InvitationCodeValidation {
  /** 是否有效 */
  valid: boolean
  /** 绑定姓名（仅有效时返回） */
  boundName?: string
  /** 提示信息 */
  message: string
}

export const authApi = {
  /** 用户登录 */
  login(params: LoginParams): Promise<LoginResult> {
    return post<LoginResult>('/v1/auth/login', params)
  },

  /** 获取验证码 */
  getCaptcha(): Promise<CaptchaResult> {
    return get<CaptchaResult>('/v1/auth/captcha')
  },

  /** 获取当前用户信息 */
  getCurrentUser(): Promise<UserInfo> {
    return get<UserInfo>('/v1/auth/me')
  },

  /** 刷新令牌 */
  refreshToken(token: string): Promise<string> {
    return post<string>('/v1/auth/refresh', {}, {
      headers: { Authorization: `Bearer ${token}` }
    })
  },

  /** 注销登录 */
  logout(): Promise<string> {
    return post<string>('/v1/auth/logout')
  },

  /** 注册（邀请制） */
  register(data: {
    username: string
    password: string
    confirmPassword: string
    fullName: string
    email: string
    phone: string
    invitationCode: string
    captcha: string
    captchaId: string
  }): Promise<unknown> {
    return post<unknown>('/v1/auth/register', data)
  },

  /**
   * 校验邀请码有效性（注册前预校验）
   * 公开接口，无需认证。仅校验邀请码是否存在、未使用、未过期。
   * @param code - 邀请码
   * @returns 校验结果，包含 valid、boundName、message
   */
  validateInvitationCode(code: string): Promise<InvitationCodeValidation> {
    return post<InvitationCodeValidation>(
      `/v1/auth/validate-invitation-code/${encodeURIComponent(code)}`,
      {}
    )
  },

  /**
   * 修改密码
   * 后端端点：PUT /v1/auth/password
   * 请求体：ChangePasswordRequest { oldPassword, newPassword, confirmPassword }
   */
  changePassword(data: {
    oldPassword: string
    newPassword: string
    confirmPassword: string
  }): Promise<string> {
    return put<string>('/v1/auth/password', data)
  },

  /**
   * 重置用户登录失败计数（管理员运维接口）
   * 后端端点：POST /v1/auth/reset-fail-count/{username}
   * 用于用户被锁定或失败次数过多时手动重置
   */
  resetFailCount(username: string): Promise<string> {
    return post<string>(`/v1/auth/reset-fail-count/${encodeURIComponent(username)}`, {})
  },
}
