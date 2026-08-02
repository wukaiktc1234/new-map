import request from './request'

interface UserInfo {
  employeeId: string
  fullName: string
  phone: string
  avatarUrl: string
  departmentName: string
  storeName: string
  position: string
  roles: string[]
  entryDate: string
}

interface HealthCertificate {
  id: string
  certificateNo: string
  issuingAuthority: string
  validFrom: string
  validUntil: string
  status: 'valid' | 'expiring' | 'expired'
  daysRemaining: number
  imageUrl?: string
}

export const profileApi = {
  // eslint-disable-next-line @typescript-eslint/no-explicit any -- 响应拦截器已自动解包 response.data.data
  getUserInfo(): Promise<UserInfo> {
    return request.get<UserInfo>('/v1/employee/profile') as any
  },

  updateProfile(data: Partial<Pick<UserInfo, 'phone' | 'avatarUrl'>>): Promise<void> {
    return request.put('/v1/employee/profile', data)
  },

  // eslint-disable-next-line @typescript-eslint/no-explicit any -- 响应拦截器已自动解包 response.data.data
  getHealthCertificate(): Promise<HealthCertificate> {
    return request.get<HealthCertificate>('/v1/employee/profile/health-certificate') as any
  },

  // eslint-disable-next-line @typescript-eslint/no-explicit any -- 响应拦截器已自动解包 response.data.data
  updateHealthCertificate(data: {
    certificateNo: string
    issuingAuthority: string
    validFrom: string
    validUntil: string
    imageUrl?: string
  }): Promise<HealthCertificate> {
    return request.post<HealthCertificate>('/v1/employee/profile/health-certificate', data) as any
  },

  changePassword(data: { oldPassword: string; newPassword: string }): Promise<void> {
    return request.post('/v1/employee/profile/change-password', data)
  },

  // eslint-disable-next-line @typescript-eslint/no-explicit any -- 响应拦截器已自动解包 response.data.data
  uploadAvatar(file: File): Promise<{ url: string }> {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<{ url: string }>('/v1/employee/profile/avatar', formData) as any
  },
}
