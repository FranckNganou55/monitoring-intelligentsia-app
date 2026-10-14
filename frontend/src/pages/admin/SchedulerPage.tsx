import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Clock, Play, Pause, CheckCircle, XCircle, Loader2, Timer, RefreshCw } from 'lucide-react'
import toast from 'react-hot-toast'
import api from '@/services/api'
import type { ApiResponse } from '@/types/api'
import type { SchedulerTaskDto } from '@/types/monitoring'
import { Card } from '@/components/ui/Card'
import { PageLoader } from '@/components/feedback/PageLoader'
import { formatDate, formatRelative } from '@/utils/formatters'
import { cn } from '@/utils/cn'

const schedulerService = {
  list: () => api.get<ApiResponse<SchedulerTaskDto[]>>('/scheduler/tasks').then((r) => r.data.data),
  toggle: (id: number, enabled: boolean) =>
    api.patch<ApiResponse<SchedulerTaskDto>>(`/scheduler/tasks/${id}/toggle`, null, { params: { enabled } }).then((r) => r.data.data),
}

// ── Last status indicator ────────────────────────────────────────────────────

function LastStatus({ status }: { status?: string }) {
  if (!status) return null
  if (status === 'SUCCESS') return (
    <span className="inline-flex items-center gap-1 text-xs font-medium text-green-700 bg-green-50 border border-green-100 px-2 py-0.5 rounded-full">
      <CheckCircle className="h-3 w-3" />
      Succès
    </span>
  )
  if (status === 'ERROR') return (
    <span className="inline-flex items-center gap-1 text-xs font-medium text-red-700 bg-red-50 border border-red-100 px-2 py-0.5 rounded-full">
      <XCircle className="h-3 w-3" />
      Erreur
    </span>
  )
  if (status === 'RUNNING') return (
    <span className="inline-flex items-center gap-1 text-xs font-medium text-blue-700 bg-blue-50 border border-blue-100 px-2 py-0.5 rounded-full">
      <Loader2 className="h-3 w-3 animate-spin" />
      En cours
    </span>
  )
  return null
}

// ── Interval display ─────────────────────────────────────────────────────────

function formatInterval(ms: number): string {
  if (ms < 1_000)   return `${ms}ms`
  if (ms < 60_000)  return `${(ms / 1_000).toFixed(0)}s`
  if (ms < 3_600_000) return `${(ms / 60_000).toFixed(0)}min`
  return `${(ms / 3_600_000).toFixed(1)}h`
}

// ── Page ─────────────────────────────────────────────────────────────────────

export function SchedulerPage() {
  const qc = useQueryClient()

  const { data, isLoading, refetch, isFetching } = useQuery({
    queryKey: ['scheduler-tasks'],
    queryFn: schedulerService.list,
    refetchInterval: 30_000,
  })

  const toggle = useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) => schedulerService.toggle(id, enabled),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['scheduler-tasks'] }); toast.success('Tâche mise à jour') },
    onError: () => toast.error('Erreur'),
  })

  if (isLoading) return <PageLoader />

  const tasks = data ?? []
  const activeCount  = tasks.filter((t) => t.enabled).length
  const errorCount   = tasks.filter((t) => t.lastStatus === 'ERROR').length

  return (
    <div className="space-y-5">

      {/* ── Header ── */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Planificateur de tâches</h1>
          <p className="text-xs text-gray-400 mt-0.5">
            {activeCount}/{tasks.length} tâche(s) active(s)
            {errorCount > 0 && <span className="ml-1.5 text-red-500">· {errorCount} en erreur</span>}
          </p>
        </div>
        <button
          onClick={() => refetch()}
          disabled={isFetching}
          className="flex items-center gap-1.5 text-xs text-gray-500 hover:text-gray-700 transition-colors disabled:opacity-40"
        >
          <RefreshCw className={cn('h-3.5 w-3.5', isFetching && 'animate-spin')} />
          Actualiser
        </button>
      </div>

      {/* ── Task grid ── */}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {tasks.map((task) => (
          <Card
            key={task.id}
            className={cn(
              'relative overflow-hidden transition-all',
              task.lastStatus === 'ERROR' && '!border-red-200',
              !task.enabled && 'opacity-70',
            )}
          >
            {/* Top accent bar */}
            <div
              className="absolute top-0 left-0 right-0 h-0.5 rounded-t-xl"
              style={{ backgroundColor: task.enabled ? (task.lastStatus === 'ERROR' ? '#EF4444' : '#22C55E') : '#9CA3AF' }}
            />

            <div className="flex items-start justify-between gap-3 mb-4 mt-1">
              <div className="flex items-start gap-3">
                <div className={cn(
                  'flex h-9 w-9 items-center justify-center rounded-xl shrink-0',
                  task.enabled
                    ? task.lastStatus === 'ERROR' ? 'bg-red-100' : 'bg-green-100'
                    : 'bg-gray-100',
                )}>
                  {task.lastStatus === 'RUNNING'
                    ? <Loader2 className="h-4 w-4 text-blue-600 animate-spin" />
                    : task.enabled
                    ? <Play className={cn('h-4 w-4', task.lastStatus === 'ERROR' ? 'text-red-600' : 'text-green-600')} />
                    : <Pause className="h-4 w-4 text-gray-400" />
                  }
                </div>
                <div>
                  <p className="font-semibold text-sm text-gray-900">{task.name}</p>
                  <p className="text-[10px] font-mono text-gray-400 mt-0.5">{task.code}</p>
                </div>
              </div>
              <span className={cn(
                'text-xs font-medium px-2 py-0.5 rounded-full border shrink-0',
                task.enabled
                  ? 'bg-green-50 text-green-700 border-green-200'
                  : 'bg-gray-100 text-gray-500 border-gray-200',
              )}>
                {task.enabled ? 'Actif' : 'Inactif'}
              </span>
            </div>

            {/* Metrics */}
            <div className="grid grid-cols-2 gap-3 mb-4">
              <div className="rounded-lg bg-gray-50 px-3 py-2.5 flex items-center gap-2">
                <Timer className="h-3.5 w-3.5 text-gray-400 shrink-0" />
                <div>
                  <p className="text-xs font-semibold text-gray-900">{formatInterval(task.intervalMs)}</p>
                  <p className="text-[10px] text-gray-400">Intervalle</p>
                </div>
              </div>
              <div className="rounded-lg bg-gray-50 px-3 py-2.5 flex items-center gap-2">
                <Clock className="h-3.5 w-3.5 text-gray-400 shrink-0" />
                <div>
                  <p className="text-xs font-semibold text-gray-900 truncate">
                    {task.lastRun ? formatRelative(task.lastRun) : '—'}
                  </p>
                  <p className="text-[10px] text-gray-400">Dernière exec.</p>
                </div>
              </div>
            </div>

            {/* Last status + error */}
            <div className="flex items-start justify-between gap-2 mb-4">
              <LastStatus status={task.lastStatus} />
              {task.lastRun && (
                <span className="text-[10px] text-gray-400 text-right shrink-0">
                  {formatDate(task.lastRun)}
                </span>
              )}
            </div>

            {task.lastError && (
              <div className="mb-4 rounded-lg bg-red-50 border border-red-100 px-3 py-2 text-xs text-red-700 line-clamp-2">
                {task.lastError}
              </div>
            )}

            {/* Toggle button */}
            <button
              onClick={() => toggle.mutate({ id: task.id, enabled: !task.enabled })}
              disabled={toggle.isPending}
              className={cn(
                'w-full flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-xs font-semibold border transition-all disabled:opacity-50',
                task.enabled
                  ? 'bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100'
                  : 'bg-green-50 text-green-700 border-green-200 hover:bg-green-100',
              )}
            >
              {task.enabled ? (
                <><Pause className="h-3.5 w-3.5" /> Désactiver</>
              ) : (
                <><Play className="h-3.5 w-3.5" /> Activer</>
              )}
            </button>
          </Card>
        ))}
      </div>
    </div>
  )
}
