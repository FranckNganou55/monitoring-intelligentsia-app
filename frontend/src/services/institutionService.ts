import api from './api'
import type { ApiResponse, PageResponse, FilterRequest } from '@/types/api'

export interface InstitutionSelectDto {
  id: number
  code: string
  name: string
}

export interface InstitutionDto extends InstitutionSelectDto {
  svfeId?: string
  hostPid?: string
  mailRecharge?: string
  alertEmailTypique?: string | null
  alertEmailMoinsCritique?: string | null
  alertEmailMoyen?: string | null
  alertEmailCritique?: string | null
  countryNumericCode?: string | null
  createdAt: string
  updatedAt: string
  currencies: CurrencyThresholdDto[]
}

export interface CurrencyThresholdDto {
  currencyId: number
  isoAlpha: string
  label: string
  atmThreshold?: number
  cardPpLimit?: number
}

export interface InstitutionCreateRequest {
  code: string
  name: string
  svfeId?: string
  hostPid?: string
  mailRecharge?: string
}

export const institutionService = {
  list: (params?: FilterRequest) =>
    api.get<ApiResponse<PageResponse<InstitutionDto>>>('/institutions', { params }).then((r) => r.data.data),

  listAll: () =>
    api.get<ApiResponse<InstitutionDto[]>>('/institutions/all').then((r) => r.data.data),

  getById: (id: number) =>
    api.get<ApiResponse<InstitutionDto>>(`/institutions/${id}`).then((r) => r.data.data),

  create: (req: InstitutionCreateRequest) =>
    api.post<ApiResponse<InstitutionDto>>('/institutions', req).then((r) => r.data.data),

  update: (id: number, req: InstitutionCreateRequest) =>
    api.put<ApiResponse<InstitutionDto>>(`/institutions/${id}`, req).then((r) => r.data.data),

  delete: (id: number) =>
    api.delete<ApiResponse<void>>(`/institutions/${id}`).then((r) => r.data),
}
