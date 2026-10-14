import api from './api'
import type { ApiResponse, PageResponse, FilterRequest } from '@/types/api'
import type { AuditLogDto } from '@/types/monitoring'

export const auditService = {
  list: (params: FilterRequest & { action?: string; entityType?: string; userId?: number }) =>
    api.get<ApiResponse<PageResponse<AuditLogDto>>>('/audit', { params }).then((r) => r.data.data),
}
