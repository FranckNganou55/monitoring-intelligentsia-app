import api from './api'
import type { ApiResponse, PageResponse } from '@/types/api'
import type {
  AlertEvent,
  AlertFilters,
  CreateAlertRequest,
  MonitoringAlert,
  UpdateAlertStatusRequest,
} from '../types/alerts'

const BASE_URL = '/monitoring/alerts'

export const alertService = {
  create: (request: CreateAlertRequest) =>
    api.post<MonitoringAlert>(BASE_URL, request).then((response) => response.data),

  list: (params?: AlertFilters) =>
    api
      .get<ApiResponse<PageResponse<MonitoringAlert>>>(
        BASE_URL,
        { params }
      )
      .then((response) => response.data.data),

  getById: (id: number) =>
    api
      .get<MonitoringAlert>(
        `${BASE_URL}/${id}`
      )
      .then((response) => response.data),

  getEvents: (id: number) =>
    api
      .get<ApiResponse<AlertEvent[]>>(
        `${BASE_URL}/${id}/events`
      )
      .then((response) => response.data.data),

  updateStatus: (
    id: number,
    request: UpdateAlertStatusRequest
  ) =>
    api
      .patch<MonitoringAlert>(
        `${BASE_URL}/${id}/status`,
        request
      )
      .then((response) => response.data),

  sendConfiguredEmail: (id: number, institutionId: number) =>
    api.post<ApiResponse<string>>(`${BASE_URL}/${id}/email`, null, { params: { institutionId } })
      .then((response) => response.data),

  exportArchive: (params: AlertFilters & { includeActive?: boolean }, format: 'WORD' | 'PDF' | 'EXCEL') =>
    api.get(`${BASE_URL}/archives/export`, { params: { ...params, format }, responseType: 'blob' }),
}
