/**
 * 公司初始化 API
 * 对应后端: /v1/system/init
 */
import { get, post } from './request'
import type { CompanyInitFormData, SystemInitStatus } from '@/types/company-init'

export const companyInitApi = {
  /**
   * 检查系统是否已初始化
   */
  async checkInitialized(): Promise<boolean> {
    const res = await get<boolean | null>('/v1/system/init/status/check')
    return res ?? false
  },

  /**
   * 获取当前初始化状态
   */
  async getInitStatus(): Promise<SystemInitStatus | null> {
    const res = await get<SystemInitStatus | null>('/v1/system/init/status')
    return res
  },

  /**
   * 开始初始化流程
   */
  async startInit(): Promise<SystemInitStatus | null> {
    const res = await post<SystemInitStatus | null>('/v1/system/init/start')
    return res
  },

  /**
   * 初始化公司信息（一键初始化公司基础信息及默认组织架构）
   * @param data 公司初始化表单
   */
  async initializeCompany(data: CompanyInitFormData): Promise<boolean> {
    const res = await post<boolean>('/v1/system/init/company', data)
    return res ?? false
  },

  /**
   * 完成初始化
   */
  async completeInit(): Promise<boolean> {
    const res = await post<boolean>('/v1/system/init/complete')
    return res ?? false
  },

  /**
   * 跳过初始化向导
   */
  async skipInit(): Promise<boolean> {
    const res = await post<boolean>('/v1/system/init/skip')
    return res ?? false
  },

  /**
   * 重置初始化状态（仅测试环境使用）
   */
  async resetInitStatus(): Promise<boolean> {
    const res = await post<boolean>('/v1/system/init/reset')
    return res ?? false
  },
}

export default companyInitApi
