import { PageIntro, SectionHeading } from '@/components/ui/PageIntro'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import axios from 'axios'
import { Monitor, Wifi, AlertTriangle, TrendingUp, RefreshCw } from 'lucide-react'
import { dashboardService } from '@/services/dashboardService'
import { alertService } from '@/services/alertService'
import { useDataAvailabilityStore } from '@/store/dataAvailabilityStore'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { Card } from '@/components/ui/Card'
import { PageLoader } from '@/components/feedback/PageLoader'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { DonutChart } from '@/components/charts/DonutChart'
import { LineChart } from '@/components/charts/LineChart'
import { operatingStatusKey, statusColors } from '@/utils/colorMap'
import { formatDate, formatRelative } from '@/utils/formatters'
import { cn } from '@/utils/cn'

// ─── KPI Card ────────────────────────────────────────────────────────────────

interface KpiProps {
  label: string
  value: number | string
  sub?: string
  icon: React.ElementType
  accent: string
  accentBg: string
  trend?: 'up' | 'down' | 'neutral'
  trendLabel?: string
}

function KpiCard({ label, value, sub, icon: Icon, accent, accentBg, trend, trendLabel }: KpiProps) {
  return (
    <Card className="relative overflow-hidden !p-5 flex flex-col gap-4 group">
      {/* Decorative top bar */}
      <div className="absolute top-0 left-0 right-0 h-0.5 rounded-t-xl" style={{ background: accent }} />

      <div className="flex items-start justify-between">
        <div
          className="flex h-11 w-11 items-center justify-center rounded-xl transition-transform group-hover:scale-110"
          style={{ backgroundColor: accentBg }}
        >
          <Icon className="h-5 w-5" style={{ color: accent }} />
        </div>
        {trend && trendLabel && (
          <span
            className={cn(
              'text-xs font-medium px-2 py-0.5 rounded-full',
              trend === 'up'      && 'bg-green-100 text-green-700',
              trend === 'down'    && 'bg-red-100 text-red-700',
              trend === 'neutral' && 'bg-gray-100 text-gray-500',
            )}
          >
            {trendLabel}
          </span>
        )}
      </div>

      <div>
        <p className="text-3xl font-bold text-gray-900 tabular-nums leading-none">{value}</p>
        <p className="mt-1.5 text-sm font-medium text-gray-600">{label}</p>
        {sub && <p className="mt-0.5 text-xs text-gray-400">{sub}</p>}
      </div>
    </Card>
  )
}

// ─── Section header ──────────────────────────────────────────────────────────

function SectionHeader({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div className="mb-4">
      <h2 className="text-sm font-semibold text-gray-900">{title}</h2>
      {subtitle && <p className="text-xs text-gray-400 mt-0.5">{subtitle}</p>}
    </div>
  )
}

// ─── Status dot ──────────────────────────────────────────────────────────────

function LiveDot() {
  const unavailable = useDataAvailabilityStore(s => s.unavailable)
  if (unavailable) return <span className="inline-flex h-2 w-2 rounded-full bg-amber-500" />
  return (
    <span className="relative flex h-2 w-2">
      <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75" />
      <span className="relative inline-flex h-2 w-2 rounded-full bg-green-500" />
    </span>
  )
}

function dashboardErrorMessage(error: unknown): string {
  if (!axios.isAxiosError(error)) {
    return 'Une erreur inattendue est survenue pendant le chargement.'
  }

  const status = error.response?.status
  if (status === 401) return 'Votre session a expiré ou a été renouvelée sur un autre appareil. Reconnectez-vous.'
  if (status === 403) return 'Votre compte ne dispose pas du droit nécessaire pour consulter le tableau de bord.'
  if (status) {
    const data = error.response?.data as { message?: string; error?: { message?: string } } | undefined
    const detail = data?.message ?? data?.error?.message
    return detail ? `Le serveur a répondu ${status} : ${detail}` : `Le serveur a répondu avec le code ${status}.`
  }
  return 'Le backend ne répond pas sur son adresse configurée. Vérifiez qu’il fonctionne et que le proxy API pointe vers le port 8089.'
}

// ─── Page ────────────────────────────────────────────────────────────────────

export function DashboardPage() {
  const institutionId = useInstitutionFilter()
  const recentAlertsQuery = useQuery({
    queryKey: ['dashboard-recent-alerts', institutionId],
    queryFn: () => alertService.list({ page: 0, size: 5, institutionId: institutionId ?? undefined }),
    refetchInterval: 10_000,
    refetchOnWindowFocus: true,
  })
  const gabAlertsQuery = useQuery({
    queryKey: ['dashboard-gab-alerts', institutionId],
    queryFn: async () => {
      const filters = { page: 0, size: 100, institutionId: institutionId ?? undefined }
      const [activeAlerts, archivedAlerts] = await Promise.all([
        alertService.list(filters),
        alertService.list({ ...filters, status: 'ARCHIVED' }),
      ])
      const cutoff = Date.now() - 24 * 60 * 60 * 1000
      const unique = new Map([...activeAlerts.content, ...archivedAlerts.content].map((alert) => [alert.id, alert]))
      return [...unique.values()].filter((alert) =>
        (alert.category === 'ATM' || alert.atmId != null || ['RULE_ATM_INTERFACE_UNAVAILABLE_5X', 'RULE_ATM_INTERFACE_DOWN_5X', 'RULE_ATM_INTERFACE_SIGNOFF_5X'].includes(alert.ruleCode ?? ''))
        && new Date(alert.detectedAt).getTime() >= cutoff,
      )
    },
    refetchInterval: 15_000,
    refetchOnWindowFocus: true,
  })
  const historyQuery = useQuery({
    queryKey: ['dashboard-history', institutionId],
    queryFn: () => dashboardService.history(institutionId),
    refetchInterval: 15_000,
  })
  const { data: s, isLoading, isError, error: summaryError, dataUpdatedAt, refetch, isFetching } = useQuery({
    queryKey: ['dashboard', institutionId],
    queryFn: () => dashboardService.summary(institutionId),
    refetchInterval: 15_000,
    refetchOnWindowFocus: true,
  })
  if (isLoading) return <PageLoader />
  if (isError || !s) return (
    <div className="rounded-xl border border-red-200 bg-red-50 p-6 text-sm text-red-800">
      <p className="font-semibold">Le tableau de bord n’a pas reçu de réponse du serveur.</p>
      <p className="mt-1">{dashboardErrorMessage(summaryError)}</p>
      <button onClick={() => void refetch()} className="mt-3 rounded-lg bg-white px-3 py-2 font-medium text-red-700 shadow-sm">Réessayer</button>
      <Link to="/login" className="ml-3 inline-block rounded-lg px-3 py-2 font-medium text-red-700 underline">Se reconnecter</Link>
    </div>
  )

  const totalAtms = s?.totalAtms ?? 0
  const atmsOnline = s?.atmsOnline ?? 0
  const atmsOffline = s?.atmsOffline ?? 0
  const atmsWarning = s?.atmsWarning ?? 0
  const onlinePct = totalAtms ? Math.round((atmsOnline / totalAtms) * 100) : 0
  const gabAlertCount = s?.gabAlertsToday ?? gabAlertsQuery.data?.length ?? 0
  const apiImpactPoints = historyQuery.data?.atmAlertImpact ?? []
  const fallbackBuckets = new Map<string, number>()
  for (const alert of gabAlertsQuery.data ?? []) {
    const hourDate = new Date(alert.detectedAt)
    hourDate.setMinutes(0, 0, 0)
    const hour = hourDate.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
    const identifiedAtm = alert.description?.match(/(?:identifiant|terminal)\s*:\s*([^\r\n,;]+)/i)?.[1]?.trim()
    const terminalId = identifiedAtm || (alert.atmId ? `GAB #${alert.atmId}` : alert.ruleCode)
    const key = `${hour}\u0000${terminalId}`
    fallbackBuckets.set(key, (fallbackBuckets.get(key) ?? 0) + 1)
  }
  const fallbackImpactPoints = [...fallbackBuckets.entries()].map(([key, count]) => {
    const [time, terminalId] = key.split('\u0000')
    return { time, terminalId, count }
  })
  const atmImpactPoints = apiImpactPoints.length > 0 ? apiImpactPoints : fallbackImpactPoints
  const atmTerminalIds = [...new Set(atmImpactPoints.map((point) => point.terminalId))]
  const atmImpactByHour = new Map<string, Record<string, unknown>>()
  for (const point of atmImpactPoints) {
    const time = new Date(point.time).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
    const row = atmImpactByHour.get(time) ?? { time }
    row[point.terminalId] = point.count
    atmImpactByHour.set(time, row)
  }
  const atmImpactData = [...atmImpactByHour.values()]
  const fraudHistoryData = (historyQuery.data?.fraudHistory ?? []).map((point) => ({
    time: new Date(point.time).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }),
    count: point.count,
  }))

  const atmDonut = [
    { name: 'En ligne',      value: atmsOnline,  color: statusColors.ok },
    { name: 'Avertissement', value: atmsWarning,  color: statusColors.warning },
    { name: 'Hors ligne',    value: atmsOffline,  color: statusColors.error },
    { name: 'Inconnu', value: s?.atmsUnknown ?? 0, color: statusColors.unknown },
  ].filter((d) => d.value > 0)

  return (
    <div className="space-y-7">

      {/* ── Header ── */}
      <div className="page-header flex items-center justify-between">
        <PageIntro icon={Monitor} eyebrow="Supervision" title="Tableau de bord" description="Disponibilité du parc et activité des alertes en temps réel."><div className="flex items-center gap-2 mt-1">
            <LiveDot />
            <span className="text-xs text-gray-400">
              Mis à jour {dataUpdatedAt ? formatDate(new Date(dataUpdatedAt).toISOString()) : '—'}
            </span>
          </div></PageIntro>
        <button
          onClick={() => { void Promise.all([refetch(), historyQuery.refetch(), recentAlertsQuery.refetch(), gabAlertsQuery.refetch()]) }}
          disabled={isFetching}
          className="flex items-center gap-2 rounded-xl border border-gray-200 bg-white px-4 py-2.5 text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors disabled:opacity-40"
        >
          <RefreshCw className={cn('h-3.5 w-3.5', isFetching && 'animate-spin')} />
          Actualiser
        </button>
      </div>

      <SectionHeading title="Vue d’ensemble" description="Les principaux indicateurs du parc surveillé." />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Card className="relative overflow-hidden !p-5 flex flex-col gap-4 group">
          <div
            className="absolute top-0 left-0 right-0 h-0.5 rounded-t-xl"
            style={{ background: '#C9A84C' }}
          />

          <div className="flex items-start justify-between">
            <div
              className="flex h-11 w-11 items-center justify-center rounded-xl transition-transform group-hover:scale-110"
              style={{ backgroundColor: '#FDF8EC' }}
            >
              <Monitor className="h-5 w-5" style={{ color: '#C9A84C' }} />
            </div>

            <span
              className={cn(
                'text-xs font-medium px-2 py-0.5 rounded-full',
                onlinePct >= 95 && 'bg-green-100 text-green-700',
                onlinePct >= 80 && onlinePct < 95 && 'bg-gray-100 text-gray-500',
                onlinePct < 80 && 'bg-red-100 text-red-700',
              )}
            >
              {onlinePct}% en ligne
            </span>
          </div>

          <div>
            <p className="text-3xl font-bold text-gray-900 tabular-nums leading-none">
              {totalAtms}
            </p>

            <p className="mt-1.5 text-sm font-medium text-gray-600">
              Total GABs
            </p>

            <div className="mt-3 space-y-1.5 border-t border-gray-100 pt-3">
              <div className="flex items-center justify-between text-xs">
                <span className="text-gray-500">Nbre en ligne</span>
                <span className="font-semibold text-green-600">
                  {atmsOnline}
                </span>
              </div>

              <div className="flex items-center justify-between text-xs">
                <span className="text-gray-500">Nbre hors ligne</span>
                <span className="font-semibold text-red-600">
                  {atmsOffline}
                </span>
              </div>

              <div className="flex items-center justify-between text-xs">
                <span className="text-gray-500">GABs actifs</span>
                <span className="font-semibold text-gray-900">
                  {atmsOnline + atmsWarning}
                </span>
              </div>

              <div className="flex items-center justify-between text-xs">
                <span className="text-gray-500">GABs inactifs</span>
                <span className="font-semibold text-gray-900">
                  {atmsOffline}
                </span>
              </div>
            </div>
            <Link to="/fraud" className="mt-3 inline-flex text-xs font-semibold text-red-700 hover:text-red-800">
              Ouvrir le suivi des fraudes
            </Link>
          </div>
        </Card>
        <KpiCard
          label="Total TPE"
          value={s?.totalPos ?? 0}
          sub={`${s?.posWithoutTelecollect ?? 0} sans télécollecte`}
          icon={Wifi}
          accent="#6B7280"
          accentBg="#F9FAFB"
        />
        <Card className="relative overflow-hidden !p-5 flex flex-col gap-4 group">
          <div
            className="absolute top-0 left-0 right-0 h-0.5 rounded-t-xl"
            style={{ background: '#EF4444' }}
          />

          <div className="flex items-start justify-between">
            <div
              className="flex h-11 w-11 items-center justify-center rounded-xl transition-transform group-hover:scale-110"
              style={{ backgroundColor: '#FEF2F2' }}
            >
              <AlertTriangle
                className="h-5 w-5"
                style={{ color: '#EF4444' }}
              />
            </div>

            <span
              className={cn(
                'text-xs font-medium px-2 py-0.5 rounded-full',
                gabAlertCount > 0
                  ? 'bg-red-100 text-red-700'
                  : 'bg-green-100 text-green-700',
              )}
            >
              {gabAlertCount > 0 ? 'Attention' : 'OK'}
            </span>
          </div>

          <div>
            <p className="text-3xl font-bold text-gray-900 tabular-nums leading-none">
              {gabAlertCount}
            </p>

            <p className="mt-1.5 text-sm font-medium text-gray-600">
              Alertes fraude (GABs)
            </p>

            <div className="mt-3 space-y-1.5 border-t border-gray-100 pt-3">
              <div className="flex items-center justify-between text-xs">
                <span className="text-gray-500">
                  Alertes GAB des dernières 24 h
                </span>
                <span className="font-semibold text-red-600">
                  {gabAlertCount}
                </span>
              </div>

              <div className="flex items-center justify-between text-xs">
                <span className="text-gray-500">
                  Total fraudes
                </span>
                <span className="font-semibold text-gray-900">
                  {s?.totalFrauds ?? 0}
                </span>
              </div>
            </div>
          </div>
        </Card>
        <KpiCard
          label="Taux de succès"
          value={s?.transactionSuccessRate == null ? '—' : `${s.transactionSuccessRate.toFixed(1)}%`}
          sub={institutionId == null ? 'dernier relevé réseau disponible' : 'relevé sans rattachement institutionnel'}
          icon={TrendingUp}
          accent="#22C55E"
          accentBg="#F0FDF4"
          trend={
            s?.transactionSuccessRate != null && s.transactionSuccessRate >= 95 ? 'up' : 'neutral'
          }
          trendLabel={
            s?.transactionSuccessRate == null ? undefined : s.transactionSuccessRate >= 95 ? 'Excellent' : 'Moyen'
          }
        />
      </div>

      <SectionHeading title="Disponibilité des équipements" description="État des GABs, des réseaux et des hôtes." />
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">

        {/* Donut GABs */}
        <Card className="flex flex-col">
          <SectionHeader title="État des GABs" subtitle={`${totalAtms} terminaux surveillés`} />
          {atmDonut.length > 0 ? (
            <>
              <DonutChart data={atmDonut} height={200} innerRadius={55} />
              <div className="mt-3 grid grid-cols-3 gap-2">
                {[
                  { label: 'En ligne',  val: atmsOnline,  color: statusColors.ok },
                  { label: 'Avertissement',   val: atmsWarning, color: statusColors.warning },
                  { label: 'Hors ligne',val: atmsOffline, color: statusColors.error },
                  { label: 'Inconnu', val: s?.atmsUnknown ?? 0, color: statusColors.unknown },
                ].map((item) => (
                  <div key={item.label} className="text-center">
                    <p className="text-lg font-bold text-gray-900" style={{ color: item.color }}>
                      {item.val}
                    </p>
                    <p className="text-xs text-gray-400">{item.label}</p>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <p className="flex-1 flex items-center justify-center text-sm text-gray-400">
              Aucune donnée
            </p>
          )}
        </Card>

        {/* Networks */}
        <Card className="lg:col-span-2">
          <SectionHeader title="Réseaux" subtitle={institutionId == null ? 'État opérationnel en temps réel' : 'Source sans rattachement institutionnel'} />
          <div className="divide-y divide-gray-50">
            {s?.networks?.length ? (
              s.networks.map((n) => {
                const sk = operatingStatusKey(n.operatingStatus)
                return (
                  <div key={n.id} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                    <div className="flex items-center gap-3">
                      <div
                        className="h-2 w-2 rounded-full shrink-0 animate-blink"
                        style={{ backgroundColor: statusColors[sk] }}
                      />
                      <div>
                        <p className="text-sm font-semibold text-gray-900">{n.networkCode}</p>
                        {n.description && (
                          <p className="text-xs text-gray-400">{n.description}</p>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      {n.transactionMode && (
                        <span className="text-xs text-gray-400 hidden sm:block">{n.transactionMode}</span>
                      )}
                      <StatusBadge status={sk} />
                    </div>
                  </div>
                )
              })
            ) : (
              <p className="py-6 text-center text-sm text-gray-400">{institutionId == null ? 'Aucune donnée réseau' : 'Les relevés réseau ne sont pas rattachés aux institutions dans la source actuelle.'}</p>
            )}
          </div>
        </Card>
      </div>

      {/* ── Hosts ── */}
      <Card>
          <SectionHeader title="Hôtes" subtitle={s?.hosts?.length ? `${s.hosts.length} hôtes surveillés` : institutionId == null ? 'Aucun hôte synchronisé' : 'Source sans rattachement institutionnel'} />
          {s?.hosts?.length ? (
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6">
            {s.hosts.map((h) => {
              const sk = operatingStatusKey(h.operatingStatus)
              return (
                <div
                  key={h.id}
                  className="relative flex flex-col gap-1 rounded-xl border border-gray-100 bg-gray-50 px-3 py-3 hover:bg-white hover:border-gray-200 transition-all"
                >
                  <div
                    className="absolute top-2 right-2 h-2 w-2 rounded-full animate-blink"
                    style={{ backgroundColor: statusColors[sk] }}
                  />
                  <p className="text-xs font-semibold text-gray-900 truncate pr-4">{h.hostname}</p>
                  {h.description && (
                    <p className="text-xs text-gray-400 font-mono break-words">{h.description}</p>
                  )}
                  {h.ipAddress && (
                    <p className="text-xs text-gray-400 font-mono">{h.ipAddress}</p>
                  )}
                  <p className="text-xs text-gray-500">État opérationnel : <span className="font-medium text-gray-700">{h.operatingStatus ?? 'Inconnu'}</span></p>
                  <p className="text-xs text-gray-500">État de traitement : <span className="font-medium text-gray-700">{h.processingStatus ?? 'Inconnu'}</span></p>
                  <StatusBadge status={sk} className="mt-1 self-start !text-[10px]" />
                  <p className="text-[10px] text-gray-400">Relevé {formatDate(h.snapshotTime)}</p>
                </div>
              )
            })}
          </div>
          ) : (
            <p className="py-6 text-center text-sm text-gray-400">{institutionId == null ? 'Aucun relevé hôte reçu. La connexion SVFE doit être active et la tâche HOST_QUERY doit avoir réussi pour charger l’identifiant, le libellé et l’état depuis INTERFACES.' : 'Les relevés hôtes ne sont pas rattachés aux institutions dans la source actuelle.'}</p>
          )}
      </Card>
      <SectionHeading title="Activité des alertes" description="Les événements récents et leur impact sur le parc." />
      <Card className="overflow-hidden !p-0">
        <div className="flex items-center justify-between border-b border-gray-100 px-4 py-3">
          <div>
            <h2 className="text-sm font-semibold text-gray-900">Alertes récentes</h2>
            <p className="mt-0.5 text-xs text-gray-400">Les 5 dernières alertes de toutes catégories</p>
          </div>
          <Link to="/monitoring/alerts" className="text-xs font-semibold text-amber-700 hover:text-amber-800">Voir toutes les alertes</Link>
        </div>
        {recentAlertsQuery.isLoading ? (
          <p className="p-6 text-center text-sm text-gray-400">Chargement des alertes…</p>
        ) : recentAlertsQuery.isError ? (
          <p className="p-6 text-center text-sm text-red-700">Impossible de charger les alertes récentes.</p>
        ) : recentAlertsQuery.data?.content.length ? (
          <div className="divide-y divide-gray-50">
            {recentAlertsQuery.data.content.map((alert) => (
              <Link key={alert.id} to={`/monitoring/alerts?alertId=${alert.id}`} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 hover:bg-gray-50">
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-gray-800">{alert.title}</p>
                  <p className="mt-0.5 truncate text-xs text-gray-400">{alert.ruleCode} · {formatRelative(alert.detectedAt)}</p>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  <span className="rounded-full bg-gray-100 px-2.5 py-1 text-[11px] font-medium text-gray-600">{alert.category}</span>
                  <span className={cn('rounded-full px-2.5 py-1 text-[11px] font-medium', alert.riskLevel === 'CRITIQUE' ? 'bg-red-50 text-red-700' : alert.riskLevel === 'MOYEN' ? 'bg-orange-50 text-orange-700' : alert.riskLevel === 'MOINS_CRITIQUE' ? 'bg-yellow-50 text-yellow-800' : 'bg-sky-50 text-sky-700')}>{alert.riskLevel.replace('_', ' ')}</span>
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <p className="p-6 text-center text-sm text-gray-400">Aucune alerte enregistrée.</p>
        )}
      </Card>
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card>
          <SectionHeader title="GABs les plus touchés" subtitle="Nombre d’alertes par GAB et par heure · dernières 24 h" />
          {atmImpactData.length > 0 ? (
            <LineChart
              data={atmImpactData}
              xKey="time"
              height={240}
              series={atmTerminalIds.map((terminalId, index) => ({
                key: terminalId,
                label: terminalId,
                color: ['#C9A84C', '#22C55E', '#EF4444', '#0EA5E9', '#8B5CF6'][index % 5],
              }))}
            />
          ) : (
            <p className="py-8 text-center text-sm text-gray-400">
              {historyQuery.isLoading || gabAlertsQuery.isLoading ? 'Chargement des alertes GAB…' : historyQuery.isError && gabAlertsQuery.isError ? 'Impossible de charger les alertes GAB. Actualisez ou vérifiez le serveur.' : 'Aucune alerte GAB sur les dernières 24 h.'}
            </p>
          )}
        </Card>
        <Card>
          <div className="mb-4 flex items-center justify-between gap-3">
            <SectionHeader title="Fraudes détectées" subtitle="Évolution sur les dernières 24 heures" />
            <Link to="/fraud" className="text-xs font-semibold text-red-700 hover:text-red-800">Voir les fraudes</Link>
          </div>
          {fraudHistoryData.length > 0 ? (
            <LineChart
              data={fraudHistoryData}
              xKey="time"
              height={240}
              series={[{ key: 'count', label: 'Fraudes', color: '#EF4444' }]}
            />
          ) : (
            <p className="py-8 text-center text-sm text-gray-400">
              {historyQuery.isLoading ? 'Chargement de l’historique…' : historyQuery.isError ? 'Impossible de charger l’historique des fraudes. Actualisez ou vérifiez le serveur.' : 'Aucune alerte fraude enregistrée sur les dernières 24 h'}
            </p>
          )}
        </Card>
      </div>
    </div>
  )
}
