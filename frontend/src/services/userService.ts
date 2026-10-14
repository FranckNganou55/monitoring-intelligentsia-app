import api from './api'
import type { ApiResponse, PageResponse, FilterRequest } from '@/types/api'
import type { UserDto, UserCreateRequest, UserUpdateRequest } from '@/types/auth'

export interface UpdateMeRequest {
  firstName: string
  lastName: string
  email: string
  phone?: string
  languageCode?: string
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}


export const userService = {
  me: () =>
    api.get<ApiResponse<UserDto>>('/users/me').then((r) => r.data.data),

  updateMe: (req: UpdateMeRequest) =>
    api.put<ApiResponse<UserDto>>('/users/me', req).then((r) => r.data.data),

  changePassword: (req: ChangePasswordRequest) =>
    api.post<ApiResponse<void>>('/users/me/password', req).then((r) => r.data),

  list: (params?: FilterRequest) =>
    api.get<ApiResponse<PageResponse<UserDto>>>('/users', { params }).then((r) => r.data.data),

  getById: (id: number) =>
    api.get<ApiResponse<UserDto>>(`/users/${id}`).then((r) => r.data.data),

  create: (req: UserCreateRequest) =>
    api.post<ApiResponse<UserDto>>('/users', req).then((r) => r.data.data),

  update: (id: number, req: UserUpdateRequest) =>
    api.put<ApiResponse<UserDto>>(`/users/${id}`, req).then((r) => r.data.data),

  setStatus: (id: number, status: string) =>
    api.patch<ApiResponse<UserDto>>(`/users/${id}/status`, null, { params: { status } }).then((r) => r.data.data),

  resetPassword: (id: number) =>
    api.post<ApiResponse<void>>(`/users/${id}/reset-password`).then((r) => r.data),
}
