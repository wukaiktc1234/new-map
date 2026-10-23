/**
 * APK 设备激活白名单 API
 * 对应后端: /v1/device-registrations
 */

import { get, post } from './request'

/** 设备登记信息 */
export interface DeviceRegistrationInfo {
  registrationId: string
  deviceId: string
  deviceName?: string
  activationCode: string
  status: 'PENDING' | 'ACTIVE' | 'DISABLED'
  boundUsername?: string
  codeExpireAt?: string
  activatedAt?: string
  lastSeenAt?: string
  createTime: string
}

interface DeviceRegistrationBackend {
  registrationId: number
  deviceId: string | null
  deviceName: string | null
  activationCode: string
  status: string
  boundUsername: string | null
  codeExpireAt: string | null
  activatedAt: string | null
  lastSeenAt: string | null
  createTime: string | null
}

function convert(backend: DeviceRegistrationBackend): DeviceRegistrationInfo {
  return {
    registrationId: String(backend.registrationId),
    deviceId: backend.deviceId ?? '',
    deviceName: backend.deviceName ?? undefined,
    activationCode: backend.activationCode,
    status: (backend.status as DeviceRegistrationInfo['status']) ?? 'PENDING',
    boundUsername: backend.boundUsername ?? undefined,
    codeExpireAt: backend.codeExpireAt ?? undefined,
    activatedAt: backend.activatedAt ?? undefined,
    lastSeenAt: backend.lastSeenAt ?? undefined,
    createTime: backend.createTime ?? '',
  }
}

export const deviceRegistrationApi = {
  /** 管理端：生成一次性激活码 */
  async generateCode(): Promise<DeviceRegistrationInfo> {
    const res = await post<DeviceRegistrationBackend>('/v1/device-registrations/codes', {})
    return convert(res)
  },

  /** APK：查询设备当前状态（UNKNOWN/PENDING/ACTIVE/DISABLED） */
  async getStatus(deviceId: string): Promise<string> {
    const res = await get<{ data: string } | string>('/v1/device-registrations/status', { deviceId })
    const raw = typeof res === 'string' ? res : (res as { data: string }).data
    return raw || 'UNKNOWN'
  },

  /** APK：凭激活码激活设备 */
  async activate(code: string, deviceId: string, deviceName?: string): Promise<DeviceRegistrationInfo> {
    const res = await post<DeviceRegistrationBackend>('/v1/device-registrations/activate', null, {
      params: { code, deviceId, deviceName },
    })
    return convert(res)
  },

  /** 管理端：分页查询设备白名单 */
  async getPage(
    current: number,
    size: number,
    keyword?: string,
  ): Promise<{ records: DeviceRegistrationInfo[]; total: number }> {
    const res = await get<{ records: DeviceRegistrationBackend[]; total: number } | null>(
      '/v1/device-registrations',
      { current, size, keyword },
    )
    return {
      records: (res?.records ?? []).map(convert),
      total: res?.total ?? 0,
    }
  },

  /** 管理端：禁用设备 */
  async disable(registrationId: string): Promise<void> {
    await post(`/v1/device-registrations/${registrationId}/disable`, {})
  },

  /** 管理端：恢复启用设备 */
  async enable(registrationId: string): Promise<void> {
    await post(`/v1/device-registrations/${registrationId}/enable`, {})
  },
}
