import api from './api'
import type { ApiResponse, PageResponse, FilterRequest } from '@/types/api'
import type { FleetAccountDto, CardOverLimitDto } from '@/types/monitoring'

export const fleetService = {
  accounts: (params: FilterRequest & { institutionId?: number }) =>
    api.get<ApiResponse<PageResponse<FleetAccountDto>>>('/fleet/accounts', { params }).then((r) => r.data.data),

  cardsOverLimit: (params: FilterRequest & { institutionId?: number }) =>
    api.get<ApiResponse<PageResponse<CardOverLimitDto>>>('/fleet/cards-over-limit', { params }).then((r) => r.data.data),
}
