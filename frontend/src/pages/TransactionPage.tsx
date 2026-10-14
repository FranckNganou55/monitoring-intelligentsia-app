import { PageIntro, SectionHeading } from '@/components/ui/PageIntro'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { TrendingUp, TrendingDown, AlertCircle, Activity, RefreshCw, Calendar } from 'lucide-react'
import { startOfDay, startOfWeek, startOfMonth, endOfDay, subDays } from 'date-fns'
import { usePagination } from '@/hooks/usePagination'
import { transactionService } from '@/services/transactionService'
import { Pagination } from '@/components/ui/Pagination'
import { Card } from '@/components/ui/Card'
import { PageLoader } from '@/components/feedback/PageLoader'
import { EmptyState } from '@/components/feedback/EmptyState'
import { formatDate, formatPercent, formatNumber } from '@/utils/formatters'
import { cn } from '@/utils/cn'
import type { TransactionRateDto, TransactionErrorDto } from '@/types/monitoring'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { operatingStatusKey } from '@/utils/colorMap'

type Tab = 'rates' | 'errors'

type Period = 'today' | 'yesterday' | 'week' | 'month' | 'all'

interface DateRange {
  from?: string
  to?: string
}

function periodToRange(p: Period): DateRange {
  const now = new Date()
  switch (p) {
    case 'today':
      return { from: startOfDay(now).toISOString(), to: endOfDay(now).toISOString() }
    case 'yesterday': {
      const y = subDays(now, 1)
      return { from: startOfDay(y).toISOString(), to: endOfDay(y).toISOString() }
    }
    case 'week':
      return { from: startOfWeek(now, { weekStartsOn: 1 }).toISOString(), to: now.toISOString() }
    case 'month':
      return { from: startOfMonth(now).toISOString(), to: now.toISOString() }
    case 'all':
    default:
      return {}
  }
}

// ── Success rate bar ─────────────────────────────────────────────────────────

function RateBar({ value }: { value: number | undefined | null }) {
  if (value == null) return <span className="text-gray-300">—</span>
  const pct = Math.min(100, Math.max(0, value))
  const color = pct >= 95 ? '#22C55E' : pct >= 80 ? '#F59E0B' : '#EF4444'
  return (
    <div className="flex items-center gap-2">
      <div className="w-20 h-1.5 rounded-full bg-gray-100 overflow-hidden shrink-0">
        <div className="h-full rounded-full" style={{ width: `${pct}%`, backgroundColor: color }} />
      </div>
      <span className="text-xs font-semibold tabular-nums" style={{ color }}>
        {formatPercent(value)}
      </span>
    </div>
  )
}

// ── KPI strip at top ─────────────────────────────────────────────────────────

function RateSummary({ data }: { data: TransactionRateDto[] | undefined }) {
  if (!data?.length) return null
  const totalSuccess = data.reduce((s, r) => s + r.successCount, 0)
  const totalFailure = data.reduce((s, r) => s + r.failureCount, 0)
  const totalTransactions = totalSuccess + totalFailure
  const avgSuccess = totalTransactions > 0 ? (totalSuccess * 100) / totalTransactions : 0

  const kpis = [
    {
      label: 'Taux global · dernier relevé',
      value: formatPercent(avgSuccess),
      icon: avgSuccess >= 95 ? TrendingUp : TrendingDown,
      color: avgSuccess >= 95 ? 'text-green-600' : avgSuccess >= 80 ? 'text-yellow-600' : 'text-red-600',
      bg:   avgSuccess >= 95 ? 'bg-green-50'   : avgSuccess >= 80 ? 'bg-yellow-50'   : 'bg-red-50',
    },
    {
      label: 'Succès · dernier relevé',
      value: formatNumber(totalSuccess),
      icon: TrendingUp,
      color: 'text-green-600',
      bg:   'bg-green-50',
    },
    {
      label: 'Échecs · dernier relevé',
      value: formatNumber(totalFailure),
      icon: TrendingDown,
      color: 'text-red-600',
      bg:   'bg-red-50',
    },
    {
      label: 'Réseaux surveillés',
      value: String(new Set(data.map((r) => r.network)).size),
      icon: Activity,
      color: 'text-gray-600',
      bg:   'bg-gray-50',
    },
  ]

  return (
    <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
      {kpis.map((k) => (
        <div key={k.label} className={cn('rounded-xl border border-gray-100 px-4 py-3 flex items-center gap-3', k.bg)}>
          <div className={cn('flex h-9 w-9 items-center justify-center rounded-xl bg-white/70', k.color)}>
            <k.icon className="h-4 w-4" />
          </div>
          <div>
            <p className="text-lg font-bold text-gray-900 tabular-nums leading-none">{k.value}</p>
            <p className="text-xs text-gray-500 mt-0.5">{k.label}</p>
          </div>
        </div>
      ))}
    </div>
  )
}

// ── Period picker ─────────────────────────────────────────────────────────────

const PERIODS: { key: Period; label: string }[] = [
  { key: 'today',     label: "Aujourd'hui" },
  { key: 'yesterday', label: 'Hier' },
  { key: 'week',      label: 'Cette semaine' },
  { key: 'month',     label: 'Ce mois' },
  { key: 'all',       label: 'Tout' },
]

function PeriodPicker({ value, onChange }: { value: Period; onChange: (p: Period) => void }) {
  return (
    <div className="flex items-center gap-1 bg-gray-50 rounded-lg p-1 border border-gray-100">
      <Calendar className="h-3.5 w-3.5 text-gray-400 ml-1 shrink-0" />
      {PERIODS.map((p) => (
        <button
          key={p.key}
          onClick={() => onChange(p.key)}
          className={cn(
            'px-2.5 py-1 rounded-md text-xs font-medium transition-colors whitespace-nowrap',
            value === p.key
              ? 'bg-white shadow-sm text-gold-600 border border-gray-200'
              : 'text-gray-500 hover:text-gray-700',
          )}
        >
          {p.label}
        </button>
      ))}
    </div>
  )
}

// ── Page ─────────────────────────────────────────────────────────────────────

export function TransactionPage() {
  const institutionId = useInstitutionFilter()
  const [tab, setTab] = useState<Tab>('rates')
  const [period, setPeriod] = useState<Period>('all')
  const [network, setNetwork] = useState('')
  const [networkState, setNetworkState] = useState('')
  const { page, size, setPage, reset } = usePagination()

  const dateRange = periodToRange(period)
  const networkQuery = useQuery({ queryKey: ['network-states', institutionId], queryFn: transactionService.networkStates, refetchInterval: 30_000 })
  const networkOptionsQuery = useQuery({ queryKey: ['network-options', institutionId], queryFn: transactionService.networks })
  const networkStates = networkQuery.data ?? []
  const shownNetworks = networkStates.filter((item) => (!network || item.networkCode === network || item.description === network)
    && (!networkState || (item.operatingStatus ?? 'UNKNOWN').toUpperCase() === networkState))
  const availableStates = [...new Set(['UP', 'DOWN', 'UNKNOWN', ...networkStates.map((item) => (item.operatingStatus ?? 'UNKNOWN').toUpperCase())])]

  const ratesQuery = useQuery({
    queryKey: ['transaction-rates', page, size, period, institutionId, network, networkState],
    queryFn: () => transactionService.rates({ page, size, ...dateRange, institutionId, network: network || undefined, state: networkState || undefined }),
    refetchInterval: 60_000,
  })

  const latestRatesQuery = useQuery({
    queryKey: ['transaction-rates-latest', institutionId],
    queryFn: () => transactionService.latestRates(),
    refetchInterval: 60_000,
  })

  const errorsQuery = useQuery({
    queryKey: ['transaction-errors', page, size, period, institutionId, network, networkState],
    queryFn: () => transactionService.errors({ page, size, ...dateRange, institutionId, network: network || undefined, state: networkState || undefined }),
    enabled: tab === 'errors',
    refetchInterval: 60_000,
  })

  const activeQuery = tab === 'rates' ? ratesQuery : errorsQuery
  const { isLoading, refetch, isFetching } = activeQuery

  function handlePeriodChange(p: Period) {
    setPeriod(p)
    reset()
  }

  return (
    <div className="space-y-5">

      {/* ── Header ── */}
      <div className="page-header flex flex-wrap items-start justify-between gap-3">
        <PageIntro icon={Activity} eyebrow="Supervision" title="Réseaux" description="Consultez l’état des réseaux et les performances des transactions." />
        <div className="flex items-center gap-2 flex-wrap">
          <PeriodPicker value={period} onChange={handlePeriodChange} />
          <button
            onClick={() => refetch()}
            disabled={isFetching}
            className="flex items-center gap-2 rounded-xl border border-gray-200 bg-white px-4 py-2.5 text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors disabled:opacity-40"
          >
            <RefreshCw className={cn('h-3.5 w-3.5', isFetching && 'animate-spin')} />
            Actualiser
          </button>
        </div>
      </div>

      {institutionId != null && (
        <div className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
          Les relevés Réseaux/transactions de la source actuelle ne comportent pas de code institutionnel. Ils sont masqués pour cette institution afin de ne pas afficher de données agrégées d’autres institutions.
        </div>
      )}

      <Card className="space-y-4">
        <SectionHeading title="Filtrer les réseaux" description="Sélectionnez un réseau et son dernier état connu." />
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-[1fr_1fr_auto]">
          <label className="text-sm font-medium text-gray-700">Réseau<select value={network} onChange={(e) => { setNetwork(e.target.value); reset() }} className="mt-2 block w-full rounded-lg border border-gray-200 p-2">
            <option value="">Tous les réseaux</option>{networkOptionsQuery.data?.map((code) => <option key={code} value={code}>{code}</option>)}
          </select></label>
          <label className="text-sm font-medium text-gray-700">État actuel du réseau<select value={networkState} onChange={(e) => { setNetworkState(e.target.value); reset() }} className="mt-2 block w-full rounded-lg border border-gray-200 p-2">
            <option value="">Tous les états</option>{availableStates.map((state) => <option key={state} value={state}>{state}</option>)}
          </select></label>
          <button type="button" onClick={() => { setNetwork(''); setNetworkState(''); reset() }} className="self-end rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-700">Réinitialiser</button>
        </div>
        <p className="text-sm text-gray-500">Les tableaux ci-dessous sont filtrés par réseau et par son dernier état connu.</p>
        {networkQuery.isError ? <p role="alert" className="text-red-600">Impossible de charger les états réseau.</p> : <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {shownNetworks.map((item) => <div key={item.id} className="rounded-xl border border-gray-200 bg-gray-50/60 p-5">
            <p className="font-bold text-gray-900">{item.networkCode}</p><p className="mb-3 text-sm text-gray-500">{item.description}</p>
            <StatusBadge status={operatingStatusKey(item.operatingStatus)} className="!text-base !font-bold !px-3 !py-1.5" />
            <span className="ml-2 text-sm font-semibold">{item.operatingStatus ?? 'UNKNOWN'}</span>
            <p className="mt-2 text-xs text-gray-500">{formatDate(item.snapshotTime)}</p>
          </div>)}
          {!shownNetworks.length && <p className="text-sm text-gray-500">{networkQuery.isLoading ? 'Chargement…' : 'Aucun réseau pour ces filtres.'}</p>}
        </div>}
      </Card>
      {/* ── KPI strip ── */}
      {tab === 'rates' && (
        <RateSummary data={latestRatesQuery.data?.filter((item) => (!network || item.network === network) && (!networkState || shownNetworks.some((n) => n.networkCode === item.network || n.description === item.network)))} />
      )}

      <Card className="p-0 overflow-hidden">
        <div className="border-b border-gray-100 px-5 py-4"><SectionHeading title="Historique des transactions" description="Analysez les taux de succès et les codes d’erreur sur la période choisie." /></div>
        {/* ── Tabs ── */}
        <div className="flex border-b border-gray-100">
          {([
            { key: 'rates',  label: 'Taux de succès',  icon: TrendingUp },
            { key: 'errors', label: 'Codes d\'erreur',  icon: AlertCircle },
          ] as const).map((t) => (
            <button
              key={t.key}
              onClick={() => { setTab(t.key); reset() }}
              className={cn(
                'flex items-center gap-2 px-5 py-3.5 text-sm font-medium border-b-2 -mb-px transition-colors',
                tab === t.key
                  ? 'border-gold-500 text-gold-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700',
              )}
            >
              <t.icon className="h-4 w-4" />
              {t.label}
            </button>
          ))}
        </div>

        {/* ── Rates table ── */}
        {tab === 'rates' && (
          isLoading ? <PageLoader /> : !ratesQuery.data?.content?.length ? <EmptyState /> : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-100 bg-gray-50/60">
                      {['Réseau', 'Acquéreur', 'Émetteur', 'Succès', 'Taux succès', 'Échecs', 'Horodatage'].map((h) => (
                        <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-50">
                    {ratesQuery.data!.content.map((r: TransactionRateDto) => (
                      <tr key={r.id} className="hover:bg-gray-50/80 transition-colors">
                        <td className="px-4 py-3">
                          <span className="font-semibold text-gray-900">{r.network}</span>
                        </td>
                        <td className="px-4 py-3 text-gray-500">{r.acquirer ?? '—'}</td>
                        <td className="px-4 py-3 text-gray-500">{r.issuer ?? '—'}</td>
                        <td className="px-4 py-3 font-medium text-green-700">{formatNumber(r.successCount)}</td>
                        <td className="px-4 py-3"><RateBar value={r.successRate} /></td>
                        <td className="px-4 py-3 font-medium text-red-600">{formatNumber(r.failureCount)}</td>
                        <td className="px-4 py-3 text-xs text-gray-400">{formatDate(r.snapshotTime)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50/40">
                <p className="text-xs text-gray-500">{ratesQuery.data?.totalElements} enregistrement(s)</p>
                <Pagination page={page} totalPages={ratesQuery.data?.totalPages ?? 1} onPageChange={setPage} />
              </div>
            </>
          )
        )}

        {/* ── Errors table ── */}
        {tab === 'errors' && (
          isLoading ? <PageLoader /> : !errorsQuery.data?.content?.length ? <EmptyState /> : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-gray-100 bg-gray-50/60">
                      {['Réseau', 'Code réponse', 'Description', 'Occurrences', 'Horodatage'].map((h) => (
                        <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-50">
                    {errorsQuery.data!.content.map((r: TransactionErrorDto) => (
                      <tr key={r.id} className="hover:bg-gray-50/80 transition-colors">
                        <td className="px-4 py-3 font-semibold text-gray-900">{r.network}</td>
                        <td className="px-4 py-3">
                          <span className="font-mono text-xs bg-red-50 text-red-700 px-2 py-0.5 rounded border border-red-100">
                            {r.responseCode ?? '—'}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-gray-500 max-w-[200px] truncate">{r.responseDesc ?? '—'}</td>
                        <td className="px-4 py-3">
                          <span className="font-semibold text-red-600">{formatNumber(r.count)}</span>
                        </td>
                        <td className="px-4 py-3 text-xs text-gray-400">{formatDate(r.snapshotTime)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50/40">
                <p className="text-xs text-gray-500">{errorsQuery.data?.totalElements} erreur(s)</p>
                <Pagination page={page} totalPages={errorsQuery.data?.totalPages ?? 1} onPageChange={setPage} />
              </div>
            </>
          )
        )}
      </Card>
    </div>
  )
}
