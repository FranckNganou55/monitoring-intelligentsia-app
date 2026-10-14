import api from './api'
import type { ApiResponse, PageResponse, FilterRequest } from '@/types/api'
import type { AtmDto, AtmSnapshotDto } from '@/types/monitoring'

export interface AtmMovement {
  id: number
  observedAt: string
  source: string
  previousStatus: string | null
  status: string
  state: 'ok' | 'warning' | 'error' | 'unknown'
  message: string
}
export interface MovementFilters { status?: string; from?: string; to?: string }
export type ReportFormat = 'PDF' | 'WORD' | 'EXCEL'

export const atmService = {
  movements: (id: number, params: MovementFilters & { page?: number; size?: number }) =>
    api.get<ApiResponse<PageResponse<AtmMovement>>>(`/atms/${id}/movements`, { params }).then((r) => r.data.data),

  exportReport: async (format: ReportFormat, params: { id?: number; search?: string; status?: string; movementStatus?: string; from?: string; to?: string; institutionId?: number }) => {
    const { id, ...filters } = params
    const response = await api.get<Blob>(id == null ? '/atms/report' : `/atms/${id}/movements/export`, { params: { ...filters, format }, responseType: 'blob' })
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = `rapport-gabs${id == null ? '' : `-${id}`}.${format === 'PDF' ? 'pdf' : format === 'WORD' ? 'docx' : 'xlsx'}`
    link.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  },
  list: (params: FilterRequest & { institutionId?: number; status?: 'ok' | 'warning' | 'error' | 'unknown' }) =>
    api.get<ApiResponse<PageResponse<AtmDto>>>('/atms', { params }).then((r) => r.data.data),

  listByInstitution: (institutionId: number) =>
    api.get<ApiResponse<AtmDto[]>>(`/atms/by-institution/${institutionId}`).then((r) => r.data.data ?? []),

  getById: (id: number) =>
    api.get<ApiResponse<AtmDto>>(`/atms/${id}`).then((r) => r.data.data),

  snapshots: (id: number, limit = 10) =>
    api.get<ApiResponse<AtmSnapshotDto[]>>(`/atms/${id}/snapshots`, { params: { limit } }).then((r) => r.data.data),

  latestSnapshot: (id: number) =>
    api.get<ApiResponse<AtmSnapshotDto>>(`/atms/${id}/snapshots/latest`).then((r) => r.data.data),
}
