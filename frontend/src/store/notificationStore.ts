import { create } from 'zustand'
import type { NotificationDto } from '@/types/notifications'

interface NotificationState {
  notifications: NotificationDto[]
  unreadCount: number
  addNotification: (n: NotificationDto, countAsUnread?: boolean) => void
  setNotifications: (ns: NotificationDto[]) => void
  setUnreadCount: (count: number) => void
  markAllRead: () => void
}

export const useNotificationStore = create<NotificationState>((set) => ({
  notifications: [],
  unreadCount: 0,
  addNotification: (n, countAsUnread = true) =>
    set((s) => ({
      notifications: [n, ...s.notifications].slice(0, 50),
      unreadCount: n.read || !countAsUnread ? s.unreadCount : s.unreadCount + 1,
    })),
  setNotifications: (ns) => set({ notifications: ns }),
  setUnreadCount: (count) => set({ unreadCount: count }),
  markAllRead: () =>
    set((s) => ({
      notifications: s.notifications.map((n) => ({ ...n, read: true })),
      unreadCount: 0,
    })),
}))
