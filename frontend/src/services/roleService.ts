import api from './api'
import type { ApiResponse } from '@/types/api'

export interface RoleOption {
  id: number
  code: string
  name: string
  description?: string
  permissions: string[]
}

export interface PermissionOption { code: string; name: string; resource: string; action: string }
export interface RoleCreateRequest { code: string; name: string; description: string; permissions: string[] }
export const roleService = {
  permissions: () => api.get<ApiResponse<PermissionOption[]>>('/roles/permissions').then(r => r.data.data),
  create: (request: RoleCreateRequest) => api.post<ApiResponse<RoleOption>>('/roles', request).then(r => r.data.data),
  updatePermissions: (id: number, permissions: string[]) => api.put<ApiResponse<RoleOption>>(`/roles/${id}/permissions`, { permissions }).then(r => r.data.data),
  list: () => api.get<ApiResponse<RoleOption[]>>('/roles').then((response) => response.data.data),
}
