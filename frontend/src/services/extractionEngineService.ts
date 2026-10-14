import api from './api'
import type {
  ExtractionTemplateDto,
  ExtractionTemplateRequest,
  ExtractionConfigDto,
  ExtractionConfigRequest,
  ExtractionJobDto,
} from '@/types/extractions'
import type { ApiResponse, PageResponse, FilterRequest } from '@/types/api'

// --- Templates ---
export const extractionTemplateService = {
  list: (filter: Partial<FilterRequest> = {}) =>
    api.get<ApiResponse<PageResponse<ExtractionTemplateDto>>>('/extraction/templates', { params: filter })
      .then(r => r.data.data!),

  listAll: () =>
    api.get<ApiResponse<ExtractionTemplateDto[]>>('/extraction/templates/all')
      .then(r => r.data.data!),

  get: (id: number) =>
    api.get<ApiResponse<ExtractionTemplateDto>>(`/extraction/templates/${id}`)
      .then(r => r.data.data!),

  create: (req: ExtractionTemplateRequest) =>
    api.post<ApiResponse<ExtractionTemplateDto>>('/extraction/templates', req)
      .then(r => r.data.data!),

  update: (id: number, req: ExtractionTemplateRequest) =>
    api.put<ApiResponse<ExtractionTemplateDto>>(`/extraction/templates/${id}`, req)
      .then(r => r.data.data!),

  delete: (id: number) =>
    api.delete(`/extraction/templates/${id}`),
}

// --- Configs ---
export const extractionConfigService = {
  list: (filter: Partial<FilterRequest> = {}) =>
    api.get<ApiResponse<PageResponse<ExtractionConfigDto>>>('/extraction/configs', { params: filter })
      .then(r => r.data.data!),

  get: (id: number) =>
    api.get<ApiResponse<ExtractionConfigDto>>(`/extraction/configs/${id}`)
      .then(r => r.data.data!),

  create: (req: ExtractionConfigRequest) =>
    api.post<ApiResponse<ExtractionConfigDto>>('/extraction/configs', req)
      .then(r => r.data.data!),

  update: (id: number, req: ExtractionConfigRequest) =>
    api.put<ApiResponse<ExtractionConfigDto>>(`/extraction/configs/${id}`, req)
      .then(r => r.data.data!),

  delete: (id: number) =>
    api.delete(`/extraction/configs/${id}`),
}

// --- Jobs ---
export const extractionJobService = {
  list: (filter: Partial<FilterRequest> & { configId?: number; institutionId?: number } = {}) =>
    api.get<ApiResponse<PageResponse<ExtractionJobDto>>>('/extraction/jobs', { params: filter })
      .then(r => r.data.data!),

  get: (id: number) =>
    api.get<ApiResponse<ExtractionJobDto>>(`/extraction/jobs/${id}`)
      .then(r => r.data.data!),

  trigger: (configId: number, extraParams: Record<string, string> = {}) =>
    api.post<ApiResponse<ExtractionJobDto>>(`/extraction/jobs/trigger/${configId}`, extraParams)
      .then(r => r.data.data!),

  downloadUrl: (jobId: number) => `/api/extraction/jobs/${jobId}/download`,
}
