import { Volume2, Eye, X, CheckCheck } from 'lucide-react'
import { useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNotificationStore } from '@/store/notificationStore'
import { notificationService } from '@/services/notificationService'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { formatRelative } from '@/utils/formatters'
import { cn } from '@/utils/cn'
import type { NotificationRiskLevel } from '@/types/notifications'
import { useNavigate } from 'react-router-dom'

interface Props {
  open: boolean
  onClose: () => void
}

function getRiskStyle(riskLevel?: NotificationRiskLevel) {
  switch (riskLevel) {
    case 'CRITIQUE':
      return {
        label: 'CRITIQUE',
        className: 'bg-red-100 text-red-700 border-red-200',
      }

    case 'MOYEN':
      return {
        label: 'MOYEN',
        className: 'bg-orange-100 text-orange-700 border-orange-200',
      }

    case 'MOINS_CRITIQUE':
      return {
        label: 'MOINS CRITIQUE',
        className: 'bg-yellow-100 text-yellow-700 border-yellow-200',
      }

    case 'TYPIQUE':
      return {
        label: 'TYPIQUE',
        className: 'bg-blue-100 text-blue-700 border-blue-200',
      }

    default:
      return null
  }
}

export function NotificationPanel({ open, onClose }: Props) {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const institutionId = useInstitutionFilter()

  const notifications = useNotificationStore(
    (state) => state.notifications
  )

  const setNotifications = useNotificationStore((state) => state.setNotifications)
  const setUnreadCount = useNotificationStore((state) => state.setUnreadCount)
  const notificationsQuery = useQuery({
    queryKey: ['notifications', institutionId, 0, 50],
    queryFn: () => notificationService.list({ page: 0, size: 50, institutionId }),
    enabled: open,
    staleTime: 30_000,
    refetchInterval: open ? 60_000 : false,
    refetchOnWindowFocus: false,
  })

  useEffect(() => {
    if (notificationsQuery.data) setNotifications(notificationsQuery.data.content)
  }, [notificationsQuery.data, setNotifications])

  const markMutation = useMutation({
    mutationFn: () => notificationService.markAllRead(institutionId),

    onSuccess: () => {
      setNotifications(useNotificationStore.getState().notifications.map((notification) =>
        institutionId == null || notification.institutionId === institutionId
          ? { ...notification, read: true }
          : notification,
      ))
      void notificationService.unreadCount(institutionId).then(setUnreadCount)

      qc.invalidateQueries({
        queryKey: ['notifications-count'],
      })
      qc.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  const markOneMutation = useMutation({
    mutationFn: (id: number) => notificationService.markRead(id, institutionId),
    onSuccess: () => {
      void notificationService.unreadCount(institutionId).then(setUnreadCount)
      qc.invalidateQueries({ queryKey: ['notifications'] })
      qc.invalidateQueries({ queryKey: ['notifications-count'] })
    },
  })

  const visibleNotifications = institutionId
    ? notifications.filter((notification) => notification.institutionId === institutionId)
    : notifications

  if (!open) {
    return null
  }

  return (
    <>
      <div
        className="fixed inset-0 z-40"
        onClick={onClose}
      />

      <div className="fixed right-4 top-16 z-50 w-96 rounded-xl border border-gray-100 bg-white shadow-xl">

        {/* Header */}
        <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3">
          <h3 className="font-semibold text-gray-900">
            Notifications
          </h3>

          <div className="flex items-center gap-2">

            <button
              onClick={() => markMutation.mutate()}
              disabled={markMutation.isPending}
              className="text-xs text-gold-500 transition-colors hover:text-gold-400 disabled:opacity-50"
              title="Tout marquer comme lu"
            >
              <CheckCheck className="h-4 w-4" />
            </button>

            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-700"
              aria-label="Fermer"
            >
              <X className="h-4 w-4" />
            </button>

          </div>
        </div>

        {/* Notifications */}
        <div className="max-h-96 divide-y divide-gray-50 overflow-y-auto">

          {notificationsQuery.isLoading ? (
            <p className="py-8 text-center text-sm text-gray-500">Chargement des notifications…</p>
          ) : notificationsQuery.isError && notifications.length === 0 ? (
            <p className="py-8 text-center text-sm text-red-600">Impossible de charger les notifications.</p>
          ) : visibleNotifications.length === 0 ? (
            <p className="py-8 text-center text-sm text-gray-500">
              Aucune notification
            </p>
          ) : (
            visibleNotifications.map((n) => {
              const risk = getRiskStyle(n.riskLevel)

              const urgent =
                n.riskLevel === 'MOYEN' ||
                n.riskLevel === 'CRITIQUE'

              return (
                <div
                  key={n.id}
                  role="button"
                  tabIndex={0}
                  onClick={() => {
                    onClose()
                    if (!n.read) markOneMutation.mutate(n.id)
                    navigate(n.alertId ? `/monitoring/alerts?alertId=${n.alertId}` : '/monitoring/alerts')
                  }}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault()
                      onClose()
                      if (!n.read) markOneMutation.mutate(n.id)
                      navigate(n.alertId ? `/monitoring/alerts?alertId=${n.alertId}` : '/monitoring/alerts')
                    }
                  }}
                  className={cn(
                    'cursor-pointer px-4 py-3 transition-colors hover:bg-gray-50',
                    !n.read && 'bg-gold-100/30',
                    urgent &&
                      n.visualAlertRequired &&
                      !n.read &&
                      'animate-pulse border-l-4 border-red-500 bg-red-50'
                  )}
                >

                  {/* Title */}
                  <div className="flex items-start justify-between gap-3">

                    <p
                      className={cn(
                        'text-sm font-medium',
                        n.read
                          ? 'text-gray-700'
                          : 'text-gray-900'
                      )}
                    >
                      {n.title}
                    </p>

                    {risk && (
                      <span
                        className={cn(
                          'shrink-0 rounded-full border px-2 py-0.5 text-[10px] font-semibold',
                          risk.className
                        )}
                      >
                        {risk.label}
                      </span>
                    )}

                  </div>

                  {/* Message */}
                  {(n.ruleCode || n.ruleName || n.category) && (
                    <p className="mt-1 text-xs font-medium text-gray-600">
                      {[n.category === 'FRAUD' ? 'Fraude' : n.category, n.ruleCode, n.ruleName]
                        .filter(Boolean)
                        .join(' · ')}
                    </p>
                  )}

                  {n.message && (
                    <p className="mt-1 text-xs text-gray-500 line-clamp-2">
                      {n.message}
                    </p>
                  )}

                  {(n.transactionReference || n.terminalId || n.detectedAt) && (
                    <p className="mt-1 text-[11px] text-gray-400">
                      {[n.transactionReference && `Transaction ${n.transactionReference}`,
                        n.terminalId && `GAB ${n.terminalId}`,
                        n.detectedAt && formatRelative(n.detectedAt)]
                        .filter(Boolean)
                        .join(' · ')}
                    </p>
                  )}

                  {/* Alert capabilities */}
                  {(n.soundRequired ||
                    n.visualAlertRequired) && (
                    <div className="mt-2 flex items-center gap-3">

                      {n.soundRequired && (
                        <span
                          className="flex items-center gap-1 text-[10px] font-medium text-red-600"
                          title="Alarme sonore activée"
                        >
                          <Volume2 className="h-3.5 w-3.5" />
                          Son
                        </span>
                      )}

                      {n.visualAlertRequired && (
                        <span
                          className="flex items-center gap-1 text-[10px] font-medium text-red-600"
                          title="Alerte visuelle activée"
                        >
                          <Eye className="h-3.5 w-3.5" />
                          Alerte visuelle
                        </span>
                      )}

                    </div>
                  )}

                  {/* Date */}
                  <p className="mt-1.5 text-xs text-gray-400">
                    {formatRelative(n.createdAt)}
                  </p>

                </div>
              )
            })
          )}

        </div>
      </div>
    </>
  )
}
