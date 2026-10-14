import api from './api'
import type { ApiResponse, PageResponse, FilterRequest } from '@/types/api'
import type { FraudAlertOverview, FraudDetailDto, FraudRateDto } from '@/types/monitoring'

export const fraudService = {
  overview: (params: {
    periodHours: number
    page: number
    size: number
    institutionId?: number
    search?: string
    riskLevel?: string
    status?: string
  }) =>
    api.get<ApiResponse<FraudAlertOverview>>('/fraud/alerts', { params }).then((r) => r.data.data),

  rates: (params: FilterRequest & { institutionId?: number }) =>
    api.get<ApiResponse<PageResponse<FraudRateDto>>>('/fraud/rates', { params }).then((r) => r.data.data),

  details: (params: FilterRequest & { institutionId?: number }) =>
    api.get<ApiResponse<PageResponse<FraudDetailDto>>>('/fraud/details', { params }).then((r) => r.data.data),

  source: (params: FilterRequest & { institutionId?: number }) =>
    api.get<ApiResponse<PageResponse<FraudDetailDto>>>('/fraud/source', { params }).then((r) => r.data.data),
}
