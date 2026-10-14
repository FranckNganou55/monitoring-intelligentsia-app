import api from './api'
import type { ApiResponse, PageResponse, FilterRequest } from '@/types/api'
import type { FraudDailyReportDto } from '@/types/reports'

export const reportService = {
  list: (params: Partial<FilterRequest> & { institutionId?: number; fromDate?: string; toDate?: string } = {}) =>
    api.get<ApiResponse<PageResponse<FraudDailyReportDto>>>('/fraud/reports/daily', { params })
      .then((response) => response.data.data!),

  generate: (date: string, format: 'WORD' | 'PDF' | 'EXCEL', comment?: string, fromTime?: string, untilTime?: string, institutionId?: number) =>
    api.post<ApiResponse<FraudDailyReportDto>>('/fraud/reports/daily', null, { params: { date, format, comment, fromTime, untilTime, institutionId } })
      .then((response) => response.data.data!),

  exportPeriod: (fromDate: string, toDate: string, format: 'WORD' | 'PDF' | 'EXCEL', comment?: string, fromTime?: string, untilTime?: string, institutionId?: number) =>
    api.get('/fraud/reports/daily/export-period', { params: { fromDate, toDate, format, comment, fromTime, untilTime, institutionId }, responseType: 'blob' }),

  download: (id: number, format: 'WORD' | 'PDF' | 'EXCEL', institutionId?: number) =>
    api.get(`/fraud/reports/daily/${id}/download`, { params: { format, institutionId }, responseType: 'blob' }),
}
