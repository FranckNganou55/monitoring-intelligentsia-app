import api from './api'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { NotificationDto } from '@/types/notifications'

interface UnreadCountResponse {
  count: number
}

export const notificationService = {
  list: (params?: { page?: number; size?: number; institutionId?: number }) =>
    api
      .get<ApiResponse<PageResponse<NotificationDto>>>('/notifications', { params })
      .then((r) => r.data.data),

  unreadCount: (institutionId?: number) =>
    api
      .get<ApiResponse<UnreadCountResponse>>('/notifications/unread-count', { params: { institutionId } })
      .then((r) => r.data.data.count),

  openAlertIds: (institutionId?: number) =>
    api.get<ApiResponse<number[]>>('/notifications/open-alert-ids', { params: { institutionId } }).then((r) => r.data.data ?? []),

  markAllRead: (institutionId?: number) => api.post('/notifications/mark-all-read', null, { params: { institutionId } }),

  markRead: (id: number, institutionId?: number) =>
    api.patch(`/notifications/${id}/read`, null, { params: { institutionId } }),
}
