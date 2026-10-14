import { useEffect, useRef } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/authStore'
import { useNotificationStore } from '@/store/notificationStore'
import { notificationService } from '@/services/notificationService'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { authService } from '@/services/authService'
import type { NotificationDto } from '@/types/notifications'

export function useSse(enabled = true) {
  const queryClient = useQueryClient()
  const token = useAuthStore((s) => s.accessToken)
  const institutionId = useInstitutionFilter()

  const addNotification = useNotificationStore((s) => s.addNotification)
  const setUnreadCount = useNotificationStore((s) => s.setUnreadCount)

  const ref = useRef<EventSource | null>(null)

  useEffect(() => {
    if (!token || !enabled) return

    let cancelled = false

    // Récupération du nombre réel de notifications non lues
    notificationService
      .unreadCount(institutionId)
      .then((count) => {
        if (!cancelled) {
          setUnreadCount(count)
        }
      })
      .catch(() => {
        // Le compteur n'empêche pas le fonctionnement du SSE.
      })

    const sseUrl = `${
      import.meta.env.VITE_SSE_URL ?? '/api/sse/stream'
    }?token=${encodeURIComponent(token)}`

    const es = new EventSource(sseUrl)

    ref.current = es
    const refreshPermissions = async () => {
      try {
        const profile = await authService.me()
        if (cancelled || useAuthStore.getState().accessToken !== token) return
        const previous = useAuthStore.getState().user
        const changed = JSON.stringify(previous?.permissions.slice().sort()) !== JSON.stringify(profile.permissions.slice().sort()) ||
            JSON.stringify(previous?.roles.slice().sort()) !== JSON.stringify(profile.roles.slice().sort())
        if (changed) await queryClient.cancelQueries()
        useAuthStore.getState().setUser({ ...profile, institutionId: profile.institutionId ?? null, institutionName: profile.institutionName ?? null })
        if (changed) void queryClient.resetQueries()
      } catch { /* Retry on reconnect, focus or the fallback interval. */ }
    }
    es.addEventListener('permissions-changed', () => void refreshPermissions())
    es.addEventListener('open', () => void refreshPermissions())
    window.addEventListener('focus', refreshPermissions)
    const permissionsTimer = window.setInterval(() => void refreshPermissions(), 15000)

    es.addEventListener('notification', (event) => {
      try {
        const notification = JSON.parse(
          (event as MessageEvent).data
        ) as NotificationDto

        const matchesInstitution = institutionId == null || notification.institutionId === institutionId
        if (notification.type === 'ALERT' && matchesInstitution) {
          window.dispatchEvent(new CustomEvent('monitoring:urgent-alert', {
            detail: { alertId: notification.alertId },
          }))
        }

        addNotification(notification, matchesInstitution)
        if (matchesInstitution) {
          void queryClient.invalidateQueries({ queryKey: ['notifications-count', institutionId] })
        }
      } catch {
        // Ignore les événements invalides.
      }
    })

    return () => {
      cancelled = true
      window.clearInterval(permissionsTimer)
      window.removeEventListener('focus', refreshPermissions)
      es.close()
      ref.current = null
    }
  }, [enabled, token, institutionId, addNotification, setUnreadCount, queryClient])
}
