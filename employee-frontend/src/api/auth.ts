import request from './request'

interface LoginRequest {
  username: string
  password: string
}

interface LoginResponse {
  token: string
  refreshToken: string
  expiresIn: number
  userInfo: {
    employeeId: string
    fullName: string
    storeName: string
    roles: string[]
  }
}

interface RefreshTokenResponse {
  token: string
  refreshToken: string
  expiresIn: number
}

export const authApi = {
  // eslint-disable-next-line @typescript-eslint/no-explicit any -- 响应拦截器已自动解包 response.data.data
  login(data: LoginRequest): Promise<LoginResponse> {
    return request.post<LoginResponse>('/v1/auth/login', data) as any
  },

  // eslint-disable-next-line @typescript-eslint/no-explicit any -- 响应拦截器已自动解包 response.data.data
  refreshToken(refreshToken: string): Promise<RefreshTokenResponse> {
    return request.post<RefreshTokenResponse>('/v1/auth/refresh', { refreshToken }) as any
  },

  logout(): Promise<void> {
    return request.post('/v1/auth/logout')
  },
}
