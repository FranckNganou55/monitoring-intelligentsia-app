import api from './api'
import type { ApiResponse } from '@/types/api'
import type { AuthResponse, LoginRequest, UserDto } from '@/types/auth'

export const authService = {
  login: (req: LoginRequest) =>
    api.post<ApiResponse<AuthResponse>>('/auth/login', req).then((r) => r.data.data),

  refresh: (refreshToken: string) =>
    api.post<ApiResponse<AuthResponse>>('/auth/refresh', { refreshToken }).then((r) => r.data.data),

  logout: () => api.post('/auth/logout'),

  forgotPassword: (email: string) =>
    api.post('/auth/forgot-password', null, { params: { email } }),

  resetPassword: (token: string, newPassword: string) =>
    api.post('/auth/reset-password', { token, newPassword }),

  me: () => api.get<ApiResponse<UserDto>>('/users/me').then((r) => r.data.data),
}
