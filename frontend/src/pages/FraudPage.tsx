import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Activity, AlertTriangle, Inbox, RefreshCw, Search, ShieldAlert, ShieldCheck } from 'lucide-react'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { usePagination } from '@/hooks/usePagination'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { useDebounce } from '@/hooks/useDebounce'
import { fraudService } from '@/services/fraudService'
import type { FraudDetailDto } from '@/types/monitoring'
import { Pagination } from '@/components/ui/Pagination'
import { Card } from '@/components/ui/Card'
import { PageLoader } from '@/components/feedback/PageLoader'
import { formatDate, formatNumber } from '@/utils/formatters'
import { cn } from '@/utils/cn'

type Period = 24 | 168 | 720

function sourceErrorMessage(error: unknown) {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string; detail?: string } } }).response
    if (response?.data?.message || response?.data?.detail) return response.data.message ?? response.data.detail!
  }
  return 'Impossible de lire la source SVFE. Vérifiez son activation et sa connexion Oracle.'
}

const riskLabels: Record<string, string> = {
  CRITIQUE: 'Critique', MOYEN: 'Moyen', MOINS_CRITIQUE: 'Moins critique', TYPIQUE: 'Typique',
}
const statusLabels: Record<string, string> = {
  OPEN: 'Nouvelle', ACKNOWLEDGED: 'Prise en compte', IN_PROGRESS: 'En cours', RESOLVED: 'Résolue', ARCHIVED: 'Archivée',
}

function Kpi({ label, value, tone, icon: Icon }: {
  label: string
  value: number
  tone: 'red' | 'amber' | 'blue' | 'gold' | 'green'
  icon: typeof ShieldAlert
}) {
  const styles = {
    red: 'bg-red-50 text-red-700 border-red-100',
    amber: 'bg-amber-50 text-amber-700 border-amber-100',
    blue: 'bg-sky-50 text-sky-700 border-sky-100',
    gold: 'bg-yellow-50 text-yellow-800 border-yellow-100',
    green: 'bg-emerald-50 text-emerald-700 border-emerald-100',
  }[tone]
  return (
    <Card className={cn('flex items-center gap-3 border px-4 py-3', styles)}>
      <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white/80">
        <Icon className="h-5 w-5" />
      </span>
      <span className="min-w-0">
        <span className="block text-2xl font-bold leading-none tabular-nums text-gray-900">{formatNumber(value)}</span>
        <span className="mt-1 block text-xs text-gray-600">{label}</span>
      </span>
    </Card>
  )
}

function riskClass(level: string) {
  if (level === 'CRITIQUE') return 'border-red-200 bg-red-50 text-red-700'
  if (level === 'MOYEN') return 'border-amber-200 bg-amber-50 text-amber-700'
  if (level === 'MOINS_CRITIQUE') return 'border-yellow-200 bg-yellow-50 text-yellow-800'
  return 'border-sky-200 bg-sky-50 text-sky-700'
}

export function FraudPage() {
  const institutionId = useInstitutionFilter()
  const [period, setPeriod] = useState<Period>(24)
  const [search, setSearch] = useState('')
  const [riskLevel, setRiskLevel] = useState('')
  const [status, setStatus] = useState('')
  const [sourcePage, setSourcePage] = useState(0)
  const debouncedSearch = useDebounce(search)
  const { page, size, setPage, reset } = usePagination()

  const { data, isLoading, isError, isFetching, refetch } = useQuery({
    queryKey: ['fraud-overview', period, page, size, institutionId, debouncedSearch, riskLevel, status],
    queryFn: () => fraudService.overview({
      periodHours: period, page, size, institutionId,
      search: debouncedSearch || undefined,
      riskLevel: riskLevel || undefined,
      status: status || undefined,
    }),
    refetchInterval: 10_000,
    refetchOnWindowFocus: true,
  })
  const sourceFraudQuery = useQuery({
    queryKey: ['fraud-source-details', institutionId, debouncedSearch, sourcePage],
    queryFn: () => fraudService.source({
      page: sourcePage, size: 10, institutionId, search: debouncedSearch || undefined,
      sortBy: 'snapshotTime', direction: 'DESC',
    }),
    refetchInterval: 15_000,
    refetchOnWindowFocus: true,
  })
  const chartData = useMemo(() => (data?.hourlyAlerts ?? []).map((point) => {
    const date = new Date(point.time)
    return {
      time: period === 24
        ? date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
        : date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' }),
      criticalCount: point.criticalCount,
      mediumCount: point.mediumCount,
      typicalCount: point.typicalCount,
      lessCriticalCount: point.lessCriticalCount,
    }
  }), [data?.hourlyAlerts, period])

  const setPeriodAndReset = (nextPeriod: Period) => { setPeriod(nextPeriod); reset() }

  return (
    <div className="space-y-5">
      <div className="page-header flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Surveillance Fraude</h1>
          <p className="mt-1 text-sm text-gray-400">Alertes du moteur de règles et enregistrements fraude SVFE affichés séparément.</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <div className="flex rounded-lg border border-gray-200 bg-white p-1">
            {([{ value: 24, label: '24 h' }, { value: 168, label: '7 jours' }, { value: 720, label: '30 jours' }] as const).map((item) => (
              <button key={item.value} onClick={() => setPeriodAndReset(item.value)}
                className={cn('rounded-md px-3 py-1.5 text-xs font-medium', period === item.value ? 'bg-gray-100 text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-800')}>
                {item.label}
              </button>
            ))}
          </div>
          <button onClick={() => { void refetch(); void sourceFraudQuery.refetch() }} disabled={isFetching || sourceFraudQuery.isFetching}
            className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm text-gray-500 hover:bg-white disabled:opacity-50">
            <RefreshCw className={cn('h-4 w-4', (isFetching || sourceFraudQuery.isFetching) && 'animate-spin')} /> Actualiser
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-5">
        <Kpi label="Critique" value={data?.criticalCount ?? 0} tone="red" icon={ShieldAlert} />
        <Kpi label="Moyen" value={data?.mediumCount ?? 0} tone="amber" icon={AlertTriangle} />
        <Kpi label="Typique" value={data?.typicalCount ?? 0} tone="blue" icon={Activity} />
        <Kpi label="Moins critique" value={data?.lessCriticalCount ?? 0} tone="gold" icon={Inbox} />
        <Kpi label="Fraudes confirmées" value={data?.confirmedCount ?? 0} tone="red" icon={ShieldCheck} />
      </div>

      <div className="grid gap-4 xl:grid-cols-3">
        <Card className="p-4 xl:col-span-2">
          <h2 className="mb-4 text-sm font-semibold text-gray-800">Alertes générées par les règles, par heure</h2>
          <div className="h-64 overflow-x-auto">
            {chartData.length ? (
              <div style={{ width: `max(100%, ${Math.max(720, chartData.length * (period === 24 ? 26 : 34))}px)`, height: '100%' }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData} barCategoryGap="35%">
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="time" tick={{ fontSize: 10 }} interval={period === 24 ? 1 : Math.max(0, Math.floor(chartData.length / 12))} />
                    <YAxis allowDecimals={false} tick={{ fontSize: 10 }} />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="criticalCount" stackId="risk" name="Critique" fill="#ef4444" maxBarSize={18} />
                    <Bar dataKey="mediumCount" stackId="risk" name="Moyen" fill="#f59e0b" maxBarSize={18} />
                    <Bar dataKey="typicalCount" stackId="risk" name="Typique" fill="#0ea5e9" maxBarSize={18} />
                    <Bar dataKey="lessCriticalCount" stackId="risk" name="Moins critique" fill="#94a3b8" radius={[3, 3, 0, 0]} maxBarSize={18} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            ) : <div className="flex h-full items-center justify-center text-sm text-gray-400">Aucune alerte sur cette période.</div>}
          </div>
        </Card>
        <Card className="space-y-5 p-4">
          <section>
            <h2 className="mb-2 text-sm font-semibold text-gray-800">BINs des alertes de règles</h2>
            {data?.topBins.length ? data.topBins.map((bin) => (
              <div key={bin.label} className="flex justify-between py-1.5 text-sm"><strong className="font-mono">{bin.label}</strong><span className="text-gray-500">{formatNumber(bin.count)}</span></div>
            )) : <p className="text-sm text-gray-400">Aucune donnée.</p>}
          </section>
          <section>
            <h2 className="mb-2 text-sm font-semibold text-gray-800">Règles les plus déclenchées</h2>
            {data?.topRules.length ? data.topRules.map((rule, index) => (
              <div key={`${rule.label}-${rule.description}-${index}`} className="flex items-center justify-between gap-3 py-1.5 text-sm">
                <span className="min-w-0 truncate"><strong className="mr-1 font-mono">{rule.label}</strong><span className="text-gray-600">{rule.description}</span></span>
                <span className="shrink-0 text-gray-500">{formatNumber(rule.count)}</span>
              </div>
            )) : <p className="text-sm text-gray-400">Aucune donnée.</p>}
          </section>
        </Card>
      </div>

      <Card className="overflow-hidden p-0">
        <div className="border-b border-gray-100 px-4 py-3">
          <h2 className="text-sm font-semibold text-gray-800">Fraudes remontées par SVFE</h2>
          <p className="mt-1 text-xs text-gray-500">
            {sourceFraudQuery.data
              ? `${formatNumber(sourceFraudQuery.data.totalElements)} enregistrement(s) lus directement depuis SVFE`
              : 'Lecture directe des fraudes depuis la source SVFE.'}
          </p>
        </div>
        {sourceFraudQuery.isLoading ? <PageLoader /> : sourceFraudQuery.isError ? (
          <div className="p-8 text-center text-sm text-red-700">
            {sourceErrorMessage(sourceFraudQuery.error)}
          </div>
        ) : !sourceFraudQuery.data?.content.length ? (
          <div className="p-8 text-center text-sm text-gray-500">
            Aucune fraude correspondante n’a été renvoyée par SVFE pour ces filtres.
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[1000px] text-sm">
                <thead><tr className="border-b border-gray-100 bg-gray-50/70">
                  {['Niveau source', 'Institution', 'BIN', 'Référence source', 'Règle SVFE', 'État', 'Détail', 'Consulté le'].map((heading) =>
                    <th key={heading} className="px-4 py-3 text-left text-[11px] font-semibold uppercase tracking-wide text-gray-500">{heading}</th>)}
                </tr></thead>
                <tbody className="divide-y divide-gray-50">
                  {sourceFraudQuery.data.content.map((fraud: FraudDetailDto) => (
                    <tr key={fraud.id} className="hover:bg-sky-50/40">
                      <td className="px-4 py-3">
                        {fraud.riskLevel ? (
                          <span className={cn('inline-flex whitespace-nowrap items-center rounded-full border px-2.5 py-1 text-xs font-medium', riskClass(fraud.riskLevel))}>
                            {riskLabels[fraud.riskLevel] ?? fraud.riskLevel}
                          </span>
                        ) : <span className="text-xs text-gray-500">{fraud.sourceRiskLevel || 'Non fourni par SVFE'}</span>}
                      </td>
                      <td className="px-4 py-3 text-xs text-gray-700">{fraud.institutionName ?? '—'}</td>
                      <td className="px-4 py-3 font-mono text-xs font-semibold text-gray-900">{fraud.bin}</td>
                      <td className="px-4 py-3 font-mono text-xs text-gray-600">{fraud.sourceReference ?? '—'}</td>
                      <td className="px-4 py-3"><span className="font-mono text-xs text-sky-700">{fraud.ruleCode ?? '—'}</span><span className="ml-1 text-gray-600">{fraud.ruleDesc ?? ''}</span></td>
                      <td className="px-4 py-3 text-xs text-gray-700">{fraud.fraudCount > 0 ? 'Fraude confirmée' : fraud.suspectCount > 0 ? 'Suspecte' : 'Signalement'}</td>
                      <td className="max-w-sm px-4 py-3 text-xs text-gray-600">{fraud.description ?? '—'}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-xs text-gray-600">{formatDate(fraud.snapshotTime)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="flex items-center justify-between border-t border-gray-100 bg-gray-50/40 px-4 py-3">
              <p className="text-xs text-gray-500">{formatNumber(sourceFraudQuery.data.totalElements)} enregistrement(s) SVFE</p>
              <Pagination page={sourcePage} totalPages={sourceFraudQuery.data.totalPages || 1} onPageChange={setSourcePage} />
            </div>
          </>
        )}
      </Card>

      <Card className="overflow-hidden p-0">
        <div className="flex flex-wrap items-center gap-3 border-b border-gray-100 px-4 py-3">
          <label className="relative min-w-[240px] max-w-lg flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <input value={search} onChange={(event) => { setSearch(event.target.value); setSourcePage(0); reset() }}
              placeholder="BIN, carte, terminal, RRN, règle…"
              className="w-full rounded-lg border border-gray-200 bg-gray-50 py-2 pl-9 pr-3 text-sm outline-none focus:border-yellow-500 focus:ring-2 focus:ring-yellow-500/20" />
          </label>
          <select value={riskLevel} onChange={(event) => { setRiskLevel(event.target.value); reset() }}
            className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-700">
            <option value="">Tous les niveaux</option>
            <option value="CRITIQUE">Critique</option><option value="MOYEN">Moyen</option>
            <option value="MOINS_CRITIQUE">Moins critique</option><option value="TYPIQUE">Typique</option>
          </select>
          <select value={status} onChange={(event) => { setStatus(event.target.value); reset() }}
            className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-700">
            <option value="">Tous les statuts</option>
            <option value="OPEN">Nouvelle</option><option value="ACKNOWLEDGED">Prise en compte</option>
            <option value="IN_PROGRESS">En cours</option><option value="RESOLVED">Résolue</option><option value="ARCHIVED">Archivée</option>
          </select>
        </div>

        {isLoading ? <PageLoader /> : isError ? (
          <div className="p-10 text-center text-sm text-red-700">Impossible de charger les alertes fraude. Vérifiez la session et la connexion au serveur.</div>
        ) : !data?.alerts.length ? (
          <div className="p-10 text-center text-sm text-gray-400">Aucune alerte fraude trouvée sur cette période.</div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full min-w-[1050px] text-sm">
                <thead><tr className="border-b border-gray-100 bg-gray-50/70">
                  {['Niveau', 'Transaction', 'Carte', 'Montant', 'Terminal', 'Institution', 'Règle', 'Statut'].map((heading) =>
                    <th key={heading} className="px-4 py-3 text-left text-[11px] font-semibold uppercase tracking-wide text-gray-500">{heading}</th>)}
                </tr></thead>
                <tbody className="divide-y divide-gray-50">
                  {data.alerts.map((alert) => (
                    <tr key={alert.id} className="hover:bg-sky-50/40">
                      <td className="px-4 py-3"><span className={cn('inline-flex whitespace-nowrap items-center rounded-full border px-2.5 py-1 text-xs font-medium', riskClass(alert.riskLevel))}><span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-current" />{riskLabels[alert.riskLevel] ?? alert.riskLevel}</span></td>
                      <td className="whitespace-nowrap px-4 py-3 text-xs text-gray-600">{formatDate(alert.detectedAt)}</td>
                      <td className="whitespace-nowrap px-4 py-3 font-mono text-xs font-semibold text-gray-900">{alert.cardNumber}</td>
                      <td className="whitespace-nowrap px-4 py-3 tabular-nums text-gray-700">{alert.amount == null ? '—' : `${new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(alert.amount)} ${alert.currency ?? ''}`}</td>
                      <td className="px-4 py-3 font-mono text-xs text-gray-600">{alert.terminalId ?? '—'}</td>
                      <td className="px-4 py-3 text-gray-600">{alert.institutionName ?? alert.institutionId ?? '—'}</td>
                      <td className="max-w-[260px] px-4 py-3"><span className="font-mono text-xs text-sky-700">{alert.ruleCode}</span><span className="ml-1 truncate text-gray-600">{alert.ruleName}</span></td>
                      <td className="px-4 py-3"><span className="whitespace-nowrap rounded-full border border-amber-200 bg-amber-50 px-2.5 py-1 text-xs font-medium text-amber-700">{statusLabels[alert.status] ?? alert.status}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="flex items-center justify-between border-t border-gray-100 bg-gray-50/40 px-4 py-3">
              <p className="text-xs text-gray-500">{formatNumber(data.totalElements)} alerte(s)</p>
              <Pagination page={page} totalPages={data.totalPages || 1} onPageChange={setPage} />
            </div>
          </>
        )}
      </Card>
    </div>
  )
}
