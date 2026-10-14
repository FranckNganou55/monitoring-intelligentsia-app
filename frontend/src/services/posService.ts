import api from './api'
import type { ApiResponse, PageResponse, FilterRequest } from '@/types/api'
import type { PosTerminalDto, TechnicianDto } from '@/types/monitoring'

export const posService = {
  list: (params: FilterRequest & { institutionId?: number }) =>
    api.get<ApiResponse<PageResponse<PosTerminalDto>>>('/pos', { params }).then((r) => r.data.data),

  getById: (id: number) =>
    api.get<ApiResponse<PosTerminalDto>>(`/pos/${id}`).then((r) => r.data.data),
}

export const technicianService = {
  list: (params: FilterRequest & { institutionId?: number }) =>
    api.get<ApiResponse<PageResponse<TechnicianDto>>>('/technicians', { params }).then((r) => r.data.data),

  getById: (id: number) =>
    api.get<ApiResponse<TechnicianDto>>(`/technicians/${id}`).then((r) => r.data.data),
}
