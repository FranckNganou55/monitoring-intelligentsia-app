import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Bell, CheckCheck, AlertTriangle, Info, CheckCircle, Zap } from 'lucide-react'
import { notificationService } from '@/services/notificationService'
import { useNotificationStore } from '@/store/notificationStore'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { SkeletonTable } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/feedback/EmptyState'
import { Pagination } from '@/components/ui/Pagination'
import { usePagination } from '@/hooks/usePagination'
import { formatRelative, formatDate } from '@/utils/formatters'
import { cn } from '@/utils/cn'
import type { NotificationDto } from '@/types/notifications'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'

// ─── Type icon & color map ────────────────────────────────────────────────────

interface TypeConfig { icon: React.ElementType; bg: string; color: string; label: string }

const typeConfig: Record<string, TypeConfig> = {
  ATM_ALERT:   { icon: AlertTriangle, bg: 'bg-red-50',    color: 'text-red-500',    label: 'Alerte GAB' },
  FRAUD_ALERT: { icon: Zap,           bg: 'bg-orange-50', color: 'text-orange-500', label: 'Fraude' },
  SYSTEM:      { icon: Info,          bg: 'bg-blue-50',   color: 'text-blue-500',   label: 'Système' },
  SUCCESS:     { icon: CheckCircle,   bg: 'bg-green-50',  color: 'text-green-500',  label: 'Succès' },
}

function typeOf(type: string): TypeConfig {
  return typeConfig[type] ?? { icon: Bell, bg: 'bg-gray-50', color: 'text-gray-500', label: type }
}

// ─── Single notification item ─────────────────────────────────────────────────

function NotificationItem({ n, onClick }: { n: NotificationDto; onClick: () => void }) {
  const cfg = typeOf(n.type)
  const Icon = cfg.icon

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onClick}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault()
          onClick()
        }
      }}
      className={cn(
        'flex cursor-pointer items-start gap-4 px-5 py-4 transition-colors hover:bg-gray-50',
        !n.read && 'bg-gold-50/30',
      )}
    >
      {/* Type icon */}
      <div className={cn('flex h-9 w-9 shrink-0 items-center justify-center rounded-xl mt-0.5', cfg.bg)}>
        <Icon className={cn('h-4 w-4', cfg.color)} />
      </div>

      {/* Content */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 mb-0.5 flex-wrap">
          <span className="text-[10px] font-semibold uppercase tracking-wide text-gray-400">
            {cfg.label}
          </span>
          {!n.read && (
            <span className="h-1.5 w-1.5 rounded-full bg-gold-500 shrink-0" />
          )}
        </div>
        <p className={cn('text-sm', !n.read ? 'font-semibold text-gray-900' : 'text-gray-700')}>
          {n.title}
        </p>
        {n.message && (
          <p className="mt-0.5 text-xs text-gray-500 line-clamp-2">{n.message}</p>
        )}
        <p className="mt-1 text-xs text-gray-400" title={formatDate(n.createdAt)}>
          {formatRelative(n.createdAt)}
        </p>
      </div>
    </div>
  )
}

// ─── Filter tabs ──────────────────────────────────────────────────────────────

type Filter = 'all' | 'unread'

function FilterTabs({ value, onChange }: { value: Filter; onChange: (f: Filter) => void }) {
  return (
    <div className="flex gap-1 rounded-lg bg-gray-100 p-1 w-fit">
      {(['all', 'unread'] as Filter[]).map((f) => (
        <button
          key={f}
          onClick={() => onChange(f)}
          className={cn(
            'rounded-md px-4 py-1.5 text-xs font-medium transition-all',
            value === f ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700',
          )}
        >
          {f === 'all' ? 'Toutes' : 'Non lues'}
        </button>
      ))}
    </div>
  )
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export function NotificationsPage() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const { setNotifications, setUnreadCount } = useNotificationStore()
  const institutionId = useInstitutionFilter()
  const [filter, setFilter] = useState<Filter>('all')
  const { page, size, setPage, reset } = usePagination()

  const { data, isLoading } = useQuery({
    queryKey: ['notifications-page', institutionId, page, size, filter],
    queryFn: () => notificationService.list({ page, size, institutionId }),
    refetchInterval: 60_000,
  })

  const markAllMutation = useMutation({
    mutationFn: () => notificationService.markAllRead(institutionId),
    onSuccess: () => {
      setNotifications(useNotificationStore.getState().notifications.map((n) =>
        institutionId == null || n.institutionId === institutionId ? { ...n, read: true } : n,
      ))
      void notificationService.unreadCount(institutionId).then(setUnreadCount)
      qc.invalidateQueries({ queryKey: ['notifications-page'] })
      qc.invalidateQueries({ queryKey: ['notifications-count'] })
    },
  })

  const markOneMutation = useMutation({
    mutationFn: (id: number) => notificationService.markRead(id, institutionId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['notifications-page'] })
      qc.invalidateQueries({ queryKey: ['notifications-count'] })
      void notificationService.unreadCount(institutionId).then(setUnreadCount)
    },
  })

  const notifications = (data?.content ?? []).filter((n) =>
    filter === 'unread' ? !n.read : true,
  )

  const unreadCount = (data?.content ?? []).filter((n) => !n.read).length

  return (
    <div className="space-y-5 max-w-3xl">

      {/* ── Header ── */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Notifications</h1>
          <p className="text-xs text-gray-400 mt-0.5">
            {data?.totalElements ?? 0} notification(s)
            {unreadCount > 0 && ` · ${unreadCount} non lue(s)`}
          </p>
        </div>
        {unreadCount > 0 && (
          <Button
            variant="secondary"
            size="sm"
            onClick={() => markAllMutation.mutate()}
            loading={markAllMutation.isPending}
          >
            <CheckCheck className="h-4 w-4" />
            Tout marquer lu
          </Button>
        )}
      </div>

      <FilterTabs value={filter} onChange={(f) => { setFilter(f); reset() }} />

      {/* ── List ── */}
      <Card className="p-0 overflow-hidden">
        {isLoading ? (
          <SkeletonTable rows={6} cols={3} />
        ) : notifications.length === 0 ? (
          <EmptyState
            title={filter === 'unread' ? 'Aucune notification non lue' : 'Aucune notification'}
            description="Vous êtes à jour !"
          />
        ) : (
          <div className="divide-y divide-gray-50">
            {notifications.map((n) => (
              <NotificationItem key={n.id} n={n} onClick={() => {
                if (!n.read) markOneMutation.mutate(n.id)
                if (n.alertId) navigate(`/monitoring/alerts?alertId=${n.alertId}`)
              }} />
            ))}
          </div>
        )}

        {(data?.totalPages ?? 0) > 1 && (
          <div className="flex items-center justify-between border-t border-gray-100 bg-gray-50/40 px-4 py-3">
            <p className="text-xs text-gray-500">{data?.totalElements} notification(s)</p>
            <Pagination page={page} totalPages={data!.totalPages} onPageChange={setPage} />
          </div>
        )}
      </Card>
    </div>
  )
}
