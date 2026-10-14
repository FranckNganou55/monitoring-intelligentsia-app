import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { PlusCircle, Edit3, Trash2, LogIn, LogOut, Search, Shield, Clock } from 'lucide-react'
import { useDebounce } from '@/hooks/useDebounce'
import { usePagination } from '@/hooks/usePagination'
import { auditService } from '@/services/auditService'
import { Pagination } from '@/components/ui/Pagination'
import { Card } from '@/components/ui/Card'
import { PageLoader } from '@/components/feedback/PageLoader'
import { EmptyState } from '@/components/feedback/EmptyState'
import { formatDate, formatRelative } from '@/utils/formatters'
import { cn } from '@/utils/cn'
import type { AuditLogDto } from '@/types/monitoring'

// ── Action config ─────────────────────────────────────────────────────────────

const actionConfig: Record<string, { icon: React.ElementType; label: string; bg: string; color: string; border: string }> = {
  CREATE:  { icon: PlusCircle, label: 'Création',    bg: 'bg-green-50',  color: 'text-green-700',  border: 'border-green-200' },
  UPDATE:  { icon: Edit3,      label: 'Modification', bg: 'bg-blue-50',   color: 'text-blue-700',   border: 'border-blue-200' },
  DELETE:  { icon: Trash2,     label: 'Suppression',  bg: 'bg-red-50',    color: 'text-red-700',    border: 'border-red-200' },
  LOGIN:   { icon: LogIn,      label: 'Connexion',    bg: 'bg-gray-50',   color: 'text-gray-600',   border: 'border-gray-200' },
  LOGOUT:  { icon: LogOut,     label: 'Déconnexion',  bg: 'bg-gray-50',   color: 'text-gray-600',   border: 'border-gray-200' },
}

function ActionBadge({ action }: { action: string }) {
  const cfg = actionConfig[action] ?? { icon: Shield, label: action, bg: 'bg-gray-50', color: 'text-gray-600', border: 'border-gray-200' }
  const Icon = cfg.icon
  return (
    <span className={cn('inline-flex items-center gap-1.5 text-xs font-medium border px-2 py-0.5 rounded-full', cfg.bg, cfg.color, cfg.border)}>
      <Icon className="h-3 w-3" />
      {cfg.label}
    </span>
  )
}

// ── Avatar ───────────────────────────────────────────────────────────────────

function UserInitial({ login }: { login?: string | null }) {
  const letter = (login ?? '?')[0].toUpperCase()
  return (
    <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gold-100 text-gold-600 text-xs font-bold shrink-0">
      {letter}
    </div>
  )
}

// ── Page ─────────────────────────────────────────────────────────────────────

export function AuditLogPage() {
  const [search, setSearch] = useState('')
  const [actionFilter, setActionFilter] = useState<string>('all')
  const debouncedSearch = useDebounce(search)
  const { page, size, setPage, reset } = usePagination()

  const { data, isLoading } = useQuery({
    queryKey: ['audit', page, size, debouncedSearch],
    queryFn: () => auditService.list({ page, size, search: debouncedSearch }),
  })

  const logs = data?.content ?? []
  const filtered = actionFilter === 'all'
    ? logs
    : logs.filter((l) => l.action === actionFilter)

  return (
    <div className="space-y-5">

      {/* ── Header ── */}
      <div>
        <h1 className="text-xl font-bold text-gray-900">Journal d'audit</h1>
        <p className="text-xs text-gray-400 mt-0.5">Historique complet des actions utilisateurs</p>
      </div>

      <Card className="p-0 overflow-hidden">
        {/* ── Toolbar ── */}
        <div className="flex flex-col gap-3 px-4 pt-4 pb-3 border-b border-gray-100 sm:flex-row sm:items-center">
          <div className="relative w-full sm:w-64">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            <input
              type="text"
              value={search}
              onChange={(e) => { setSearch(e.target.value); reset() }}
              placeholder="Rechercher utilisateur, entité…"
              className="w-full rounded-lg border border-gray-200 bg-gray-50 py-2 pl-9 pr-3 text-sm placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-gold-500/30 focus:border-gold-500"
            />
          </div>

          {/* Action filter */}
          <div className="flex gap-1 flex-wrap rounded-lg bg-gray-100 p-1 w-fit">
            {(['all', 'CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT'] as const).map((a) => {
              const cfg = actionConfig[a]
              return (
                <button
                  key={a}
                  onClick={() => { setActionFilter(a); reset() }}
                  className={cn(
                    'rounded-md px-3 py-1.5 text-xs font-medium transition-all',
                    actionFilter === a
                      ? 'bg-white text-gray-900 shadow-sm'
                      : 'text-gray-500 hover:text-gray-700',
                  )}
                >
                  {a === 'all' ? 'Toutes' : cfg?.label ?? a}
                </button>
              )
            })}
          </div>
        </div>

        {isLoading ? (
          <PageLoader />
        ) : !filtered.length ? (
          <EmptyState title="Aucune entrée" description="Aucune action enregistrée pour ces critères." />
        ) : (
          <>
            {/* ── Timeline list ── */}
            <div className="divide-y divide-gray-50">
              {filtered.map((log: AuditLogDto) => {
                const cfg = actionConfig[log.action]
                const Icon = cfg?.icon ?? Shield
                return (
                  <div key={log.id} className="flex items-start gap-4 px-4 py-3.5 hover:bg-gray-50/80 transition-colors">
                    {/* Icon */}
                    <div className={cn(
                      'flex h-8 w-8 items-center justify-center rounded-full border shrink-0 mt-0.5',
                      cfg?.bg ?? 'bg-gray-50',
                      cfg?.border ?? 'border-gray-200',
                    )}>
                      <Icon className={cn('h-3.5 w-3.5', cfg?.color ?? 'text-gray-500')} />
                    </div>

                    {/* Content */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <ActionBadge action={log.action} />
                        {log.entityType && (
                          <span className="text-xs font-medium text-gray-700 bg-gray-100 px-2 py-0.5 rounded">
                            {log.entityType}
                            {log.entityId && ` #${log.entityId}`}
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-3 mt-1">
                        <div className="flex items-center gap-1.5">
                          <UserInitial login={log.userLogin} />
                          <span className="text-xs font-medium text-gray-700">{log.userLogin ?? 'Système'}</span>
                        </div>
                        {log.ipAddress && (
                          <span className="text-xs text-gray-400 font-mono">{log.ipAddress}</span>
                        )}
                      </div>
                    </div>

                    {/* Timestamp */}
                    <div className="text-right shrink-0">
                      <div className="flex items-center gap-1 text-xs text-gray-400 justify-end">
                        <Clock className="h-3 w-3" />
                        {formatRelative(log.createdAt)}
                      </div>
                      <p className="text-[10px] text-gray-300 mt-0.5">{formatDate(log.createdAt)}</p>
                    </div>
                  </div>
                )
              })}
            </div>

            <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50/40">
              <p className="text-xs text-gray-500">{data?.totalElements} entrée(s)</p>
              <Pagination page={page} totalPages={data?.totalPages ?? 1} onPageChange={setPage} />
            </div>
          </>
        )}
      </Card>
    </div>
  )
}
