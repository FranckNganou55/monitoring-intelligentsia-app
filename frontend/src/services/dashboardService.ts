import api from './api'
import type { ApiResponse } from '@/types/api'
import type { DashboardHistoryDto, DashboardSummaryDto } from '@/types/monitoring'

export const dashboardService = {
  summary: (institutionId?: number) =>
    api
      .get<ApiResponse<DashboardSummaryDto>>('/dashboard/summary', { params: { institutionId } })
      .then((r) => r.data.data),
  history: (institutionId?: number) =>
    api
      .get<ApiResponse<DashboardHistoryDto>>('/dashboard/history', { params: { institutionId } })
      .then((r) => r.data.data),
}
