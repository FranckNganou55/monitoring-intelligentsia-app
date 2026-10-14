import { PageIntro, SectionHeading } from '@/components/ui/PageIntro'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Monitor, Search, WifiOff, AlertTriangle, Wifi, ChevronRight } from 'lucide-react'
import { atmService } from '@/services/atmService'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { useDebounce } from '@/hooks/useDebounce'
import { usePagination } from '@/hooks/usePagination'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { Card } from '@/components/ui/Card'
import { PageLoader } from '@/components/feedback/PageLoader'
import { EmptyState } from '@/components/feedback/EmptyState'
import { Pagination } from '@/components/ui/Pagination'
import { atmStatusKey, statusColors } from '@/utils/colorMap'
import { formatDate } from '@/utils/formatters'
import { cn } from '@/utils/cn'
import { AtmReportControls, dateFilters, movementStatuses } from '@/components/atm/AtmReportControls'

type FilterTab = 'all' | 'ok' | 'warning' | 'error'

const tabs: { key: FilterTab; label: string; icon: React.ElementType; color: string }[] = [
  { key: 'all',     label: 'Tous',           icon: Monitor,       color: 'text-gray-500' },
  { key: 'ok',      label: 'En ligne',        icon: Wifi,          color: 'text-green-600' },
  { key: 'warning', label: 'Avertissement',   icon: AlertTriangle, color: 'text-yellow-600' },
  { key: 'error',   label: 'Hors ligne',      icon: WifiOff,       color: 'text-red-600' },
]

function StatChip({ icon: Icon, label, count, color, active, onClick }: {
  icon: React.ElementType
  label: string
  count: number
  color: string
  active: boolean
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={cn(
        'flex flex-1 items-center gap-4 rounded-2xl border px-4 py-4 text-left transition-colors',
        active ? 'border-gold-300 bg-gold-50' : 'border-gray-100 bg-gray-50 hover:bg-white',
      )}
    >
      <Icon className={cn('h-6 w-6 shrink-0', color)} />
      <div>
        <p className="text-2xl font-bold text-gray-900 leading-none">{count}</p>
        <p className="text-xs text-gray-500 mt-1">{label}</p>
      </div>
    </button>
  )
}

export function AtmListPage() {
  const navigate = useNavigate()
  const institutionId = useInstitutionFilter()
  const [search, setSearch] = useState('')
  const [activeTab, setActiveTab] = useState<FilterTab>('all')
  const [movementStatus, setMovementStatus] = useState('')
  const [movementFrom, setMovementFrom] = useState('')
  const [movementTo, setMovementTo] = useState('')
  const debouncedSearch = useDebounce(search)
  const { page, size, setPage, reset } = usePagination()

  const { data, isLoading } = useQuery({
    queryKey: ['atms', page, size, debouncedSearch, institutionId, activeTab],
    queryFn: () => atmService.list({ page, size, search: debouncedSearch, institutionId, status: activeTab === 'all' ? undefined : activeTab }),
    refetchInterval: 60_000,
  })

  const countsQuery = useQuery({
    queryKey: ['atm-status-counts', debouncedSearch, institutionId],
    queryFn: async () => {
      const [online, warning, offline] = await Promise.all([
        atmService.list({ page: 0, size: 1, search: debouncedSearch, institutionId, status: 'ok' }),
        atmService.list({ page: 0, size: 1, search: debouncedSearch, institutionId, status: 'warning' }),
        atmService.list({ page: 0, size: 1, search: debouncedSearch, institutionId, status: 'error' }),
      ])
      return { ok: online.totalElements, warning: warning.totalElements, error: offline.totalElements }
    },
    refetchInterval: 60_000,
  })

  const atms = data?.content ?? []

  const totalAtms  = data?.totalElements ?? 0
  const onlineCount  = countsQuery.data?.ok ?? 0
  const warningCount = countsQuery.data?.warning ?? 0
  const offlineCount = countsQuery.data?.error ?? 0

  return (
    <div className="space-y-5">

      {/* ── Header ── */}
      <div className="page-header flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <PageIntro icon={Monitor} eyebrow="Parc monétique" title="GABs / ATMs" description="Suivez les terminaux, leurs statuts et leurs mouvements." />
      </div>
      <div className="grid gap-3 sm:grid-cols-3">
          <StatChip icon={Wifi} label="En ligne" count={onlineCount} color="text-green-600" active={activeTab === 'ok'} onClick={() => { setActiveTab(activeTab === 'ok' ? 'all' : 'ok'); reset() }} />
          <StatChip icon={AlertTriangle} label="Avertissement" count={warningCount} color="text-yellow-600" active={activeTab === 'warning'} onClick={() => { setActiveTab(activeTab === 'warning' ? 'all' : 'warning'); reset() }} />
          <StatChip icon={WifiOff} label="Hors ligne" count={offlineCount} color="text-red-600" active={activeTab === 'error'} onClick={() => { setActiveTab(activeTab === 'error' ? 'all' : 'error'); reset() }} />
        </div>


      <Card className="space-y-3">
        <SectionHeading title="Préparer un rapport" description="Les filtres de la liste déterminent les GABs inclus dans le document." />
        <div className="flex flex-wrap items-end gap-3">
          <label className="text-sm text-gray-700">Statut des mouvements<select value={movementStatus} onChange={(e) => setMovementStatus(e.target.value)} className="mt-1 block rounded-lg border border-gray-200 p-2">
            {movementStatuses.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select></label>
          <label className="text-sm text-gray-700">Du<input type="date" value={movementFrom} onChange={(e) => setMovementFrom(e.target.value)} className="mt-1 block rounded-lg border border-gray-200 p-2" /></label>
          <label className="text-sm text-gray-700">Au<input type="date" value={movementTo} onChange={(e) => setMovementTo(e.target.value)} className="mt-1 block rounded-lg border border-gray-200 p-2" /></label>
          <AtmReportControls disabled={!!movementFrom && !!movementTo && movementFrom > movementTo} params={{ search, institutionId, status: activeTab === 'all' ? undefined : activeTab, movementStatus: movementStatus || undefined, ...dateFilters(movementFrom, movementTo) }} />
        </div>
        {movementFrom && movementTo && movementFrom > movementTo && <p role="alert" className="text-red-600">La date de début doit précéder la date de fin.</p>}
        <p className="text-sm text-gray-500">Le rapport inclut tous les terminaux correspondant à la recherche et à l’état sélectionné ci-dessous, avec les mouvements de la période choisie.</p>
      </Card>
      <Card className="p-0 overflow-hidden">
        <div className="border-b border-gray-100 px-5 py-4"><SectionHeading title="Inventaire des GABs" description={`${totalAtms} terminal(aux) correspondant à la recherche`} /></div>
        {/* ── Toolbar ── */}
        <div className="flex flex-col gap-3 px-4 pt-4 pb-3 border-b border-gray-100 sm:flex-row sm:items-center sm:justify-between">
          {/* Search */}
          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            <input
              type="text"
              value={search}
              onChange={(e) => { setSearch(e.target.value); reset() }}
              placeholder="Rechercher terminal, ville…"
              className="w-full rounded-lg border border-gray-200 bg-gray-50 py-2 pl-9 pr-3 text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-gold-500/30 focus:border-gold-500"
            />
          </div>

          {/* Status tabs */}
          <div className="flex flex-wrap gap-1 rounded-lg bg-gray-100 p-1">
            {tabs.map((t) => (
              <button
                key={t.key}
                onClick={() => { setActiveTab(t.key); reset() }}
                aria-pressed={activeTab === t.key}
                className={cn(
                  'flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs font-medium transition-all',
                  activeTab === t.key
                    ? 'bg-white text-gray-900 shadow-sm'
                    : 'text-gray-500 hover:text-gray-700',
                )}
              >
                <t.icon className={cn('h-3.5 w-3.5', activeTab === t.key ? t.color : '')} />
                {t.label}
              </button>
            ))}
          </div>
        </div>

        {/* ── Table ── */}
        {isLoading ? (
          <PageLoader />
        ) : !atms.length ? (
          <EmptyState title="Aucun GAB" description="Aucun résultat pour cette recherche." />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100 bg-gray-50/60">
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">Statut</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">Terminal ID</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">Ville</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide hidden md:table-cell">Adresse</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide hidden lg:table-cell">Institution</th>
                    <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide hidden xl:table-cell">Mis à jour</th>
                    <th className="px-4 py-3 w-8" />
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {atms.map((atm) => {
                    const sk = atmStatusKey(atm)
                    return (
                      <tr
                        key={atm.id}
                        onClick={() => navigate(`/atms/${atm.id}`)}
                        className="group cursor-pointer hover:bg-gray-50/80 transition-colors"
                      >
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2">
                            <span
                              className="h-8 w-8 rounded-full shrink-0 animate-blink"
                              style={{ backgroundColor: statusColors[sk] }}
                            />
                            <StatusBadge status={sk} className="!text-base !font-bold !px-2 !py-2 whitespace-nowrap" />
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <span className="font-mono text-xs font-semibold text-gray-900 bg-gray-100 px-2 py-0.5 rounded">
                            {atm.terminalId}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-gray-700">{atm.city ?? '—'}</td>
                        <td className="px-4 py-3 text-gray-500 max-w-[180px] truncate hidden md:table-cell">
                          {atm.address ?? '—'}
                        </td>
                        <td className="px-4 py-3 text-gray-700 hidden lg:table-cell">{atm.institutionName}</td>
                        <td className="px-4 py-3 text-gray-400 text-xs hidden xl:table-cell">{formatDate(atm.updatedAt)}</td>
                        <td className="px-4 py-3">
                          <ChevronRight className="h-4 w-4 text-gray-300 group-hover:text-gray-500 transition-colors" />
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50/40">
              <p className="text-xs text-gray-500">{data?.totalElements ?? 0} terminal(s)</p>
              <Pagination page={page} totalPages={data?.totalPages ?? 1} onPageChange={setPage} />
            </div>
          </>
        )}
      </Card>
    </div>
  )
}
