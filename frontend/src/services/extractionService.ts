import api from './api'
import type { ApiResponse, PageResponse, FilterRequest } from '@/types/api'
import type { ExtractionBatchDto } from '@/types/monitoring'

interface ExtractionCreateRequest {
  institutionId: number
  type: 'ATM' | 'POS'
  startDate: string
  endDate: string
  merchant?: string
  city?: string
  terminalIds: string[]
}

export const extractionService = {
  list: (params: FilterRequest & { institutionId?: number; type?: string }) =>
    api.get<ApiResponse<PageResponse<ExtractionBatchDto>>>('/extractions', { params }).then((r) => r.data.data),

  create: (req: ExtractionCreateRequest) =>
    api.post<ApiResponse<ExtractionBatchDto>>('/extractions', req).then((r) => r.data.data),

  download: (id: number) =>
    api.get(`/extractions/${id}/download`, { responseType: 'blob' }),
}
