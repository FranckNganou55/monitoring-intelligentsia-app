import api from './api'
import type { ApiResponse, PageResponse, FilterRequest } from '@/types/api'
import type { TransactionRateDto, TransactionErrorDto } from '@/types/monitoring'

type TransactionFilter = FilterRequest & { from?: string; to?: string; institutionId?: number; network?: string; state?: string }

export interface NetworkState { id: number; networkCode: string; description?: string; operatingStatus?: string; snapshotTime: string }

export interface TransactionOperation { id: number; detectedAt: string; transactionType?: string; amount?: number; currency?: string; merchant?: string; reference?: string; responseCode?: string; status: string; ruleCode: string; riskLevel: string; institution?: string }

export const transactionService = {
  types: () => api.get<ApiResponse<string[]>>('/transactions/types').then(r => r.data.data ?? []),
  operations: (params: { page: number; size: number; institutionId?: number; from?: string; until?: string; type?: string; search?: string }) => api.get<ApiResponse<PageResponse<TransactionOperation>>>('/transactions/operations', { params }).then(r => r.data.data!),
  networkStates: () => api.get<ApiResponse<NetworkState[]>>('/network/latest').then((r) => r.data.data),
  networks: () => api.get<ApiResponse<string[]>>('/transactions/networks').then((r) => r.data.data),
  rates: (params: TransactionFilter) =>
    api.get<ApiResponse<PageResponse<TransactionRateDto>>>('/transactions/rates', { params }).then((r) => r.data.data),

  errors: (params: TransactionFilter) =>
    api.get<ApiResponse<PageResponse<TransactionErrorDto>>>('/transactions/errors', { params }).then((r) => r.data.data),

  latestRates: () =>
    api.get<ApiResponse<TransactionRateDto[]>>('/transactions/rates/latest').then((r) => r.data.data),
}
