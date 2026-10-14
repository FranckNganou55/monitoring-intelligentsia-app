import { useQuery } from '@tanstack/react-query'
import { TrendingUp, TrendingDown, Minus, Wallet, AlertTriangle, RefreshCw, CreditCard } from 'lucide-react'
import { usePagination } from '@/hooks/usePagination'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { fleetService } from '@/services/fleetService'
import { Pagination } from '@/components/ui/Pagination'
import { Card } from '@/components/ui/Card'
import { PageLoader } from '@/components/feedback/PageLoader'
import { EmptyState } from '@/components/feedback/EmptyState'
import { formatDate, formatAmount, formatNumber } from '@/utils/formatters'
import { cn } from '@/utils/cn'
import type { FleetAccountDto } from '@/types/monitoring'

// ── Delta trend indicator ────────────────────────────────────────────────────

function DeltaCell({ value, exceeded, currency }: { value: number | undefined | null; exceeded?: boolean; currency?: string }) {
  if (value == null) return <span className="text-gray-300">—</span>
  const Icon = value > 0 ? TrendingUp : value < 0 ? TrendingDown : Minus
  const color = exceeded ? 'text-red-600' : value > 0 ? 'text-green-600' : value < 0 ? 'text-blue-600' : 'text-gray-400'
  return (
    <div className={cn('flex items-center gap-1.5', color)}>
      <Icon className="h-3.5 w-3.5 shrink-0" />
      <span className="font-semibold text-xs tabular-nums">{formatAmount(value, currency)}</span>
      {exceeded && (
        <AlertTriangle className="h-3.5 w-3.5 text-red-500" />
      )}
    </div>
  )
}

// ── Page ─────────────────────────────────────────────────────────────────────

export function FleetPage() {
  const institutionId = useInstitutionFilter()
  const { page, size, setPage } = usePagination()

  const { data, isLoading, refetch, isFetching } = useQuery({
    queryKey: ['fleet-accounts', page, size, institutionId],
    queryFn: () => fleetService.accounts({ page, size, institutionId }),
    refetchInterval: 60_000,
  })

  const accounts = data?.content ?? []
  const alertCount = accounts.filter((a) => a.delta1Exceeded || a.delta2Exceeded).length
  const totalBalance = accounts.reduce((s, a) => s + (a.balance ?? 0), 0)

  return (
    <div className="space-y-5">

      {/* ── Header ── */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Flotte</h1>
          <p className="text-xs text-gray-400 mt-0.5">Gestion des comptes et dépassements de seuil</p>
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

      {/* ── Alert banner ── */}
      {alertCount > 0 && (
        <div className="flex items-center gap-3 rounded-xl bg-amber-50 border border-amber-200 px-4 py-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-amber-100 shrink-0">
            <AlertTriangle className="h-5 w-5 text-amber-600" />
          </div>
          <div>
            <p className="text-sm font-semibold text-amber-900">
              {alertCount} compte(s) avec seuil dépassé
            </p>
            <p className="text-xs text-amber-700 mt-0.5">
              Vérifiez les comptes en alerte ci-dessous
            </p>
          </div>
        </div>
      )}

      {/* ── KPI strip ── */}
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-3">
        {[
          { label: 'Comptes surveillés', value: data?.totalElements ?? 0, icon: Wallet,         color: 'text-blue-600',   bg: 'bg-blue-50',   format: formatNumber },
          { label: 'Alertes actives',    value: alertCount,               icon: AlertTriangle,  color: 'text-amber-600',  bg: 'bg-amber-50',  format: formatNumber },
          { label: 'Solde total',        value: totalBalance,             icon: CreditCard,     color: 'text-gray-600',   bg: 'bg-gray-50',   format: (v: number) => formatAmount(v) },
        ].map((k) => (
          <div key={k.label} className={cn('rounded-xl border border-gray-100 px-4 py-3 flex items-center gap-3', k.bg)}>
            <div className={cn('flex h-9 w-9 items-center justify-center rounded-xl bg-white/70', k.color)}>
              <k.icon className="h-4 w-4" />
            </div>
            <div>
              <p className="text-lg font-bold text-gray-900 tabular-nums leading-none">{k.format(k.value as number)}</p>
              <p className="text-xs text-gray-500 mt-0.5">{k.label}</p>
            </div>
          </div>
        ))}
      </div>

      <Card className="p-0 overflow-hidden">
        {isLoading ? (
          <PageLoader />
        ) : !accounts.length ? (
          <EmptyState title="Aucun compte" description="Aucune donnée de flotte disponible." />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100 bg-gray-50/60">
                    {['Compte', 'Devise', 'Solde actuel', 'Solde précédent', 'Delta 1', 'Delta 2', 'Statut', 'Horodatage'].map((h) => (
                      <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {accounts.map((r: FleetAccountDto) => {
                    const hasAlert = r.delta1Exceeded || r.delta2Exceeded
                    return (
                      <tr
                        key={r.id}
                        className={cn(
                          'hover:bg-gray-50/80 transition-colors',
                          hasAlert && 'bg-amber-50/40',
                        )}
                      >
                        <td className="px-4 py-3">
                          <span className="font-mono text-xs font-semibold bg-gray-100 px-2 py-0.5 rounded text-gray-900">
                            {r.accountNumber}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-gray-600 font-medium">{r.currency ?? '—'}</td>
                        <td className="px-4 py-3 font-semibold text-gray-900 tabular-nums">
                          {formatAmount(r.balance, r.currency)}
                        </td>
                        <td className="px-4 py-3 text-gray-500 tabular-nums">
                          {formatAmount(r.previousBalance, r.currency)}
                        </td>
                        <td className="px-4 py-3">
                          <DeltaCell value={r.delta1} exceeded={r.delta1Exceeded} currency={r.currency} />
                        </td>
                        <td className="px-4 py-3">
                          <DeltaCell value={r.delta2} exceeded={r.delta2Exceeded} currency={r.currency} />
                        </td>
                        <td className="px-4 py-3">
                          {hasAlert ? (
                            <span className="inline-flex items-center gap-1 text-xs font-medium text-amber-700 bg-amber-50 border border-amber-200 px-2 py-0.5 rounded-full">
                              <AlertTriangle className="h-3 w-3" />
                              Seuil dépassé
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-xs font-medium text-green-700 bg-green-50 border border-green-100 px-2 py-0.5 rounded-full">
                              Normal
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3 text-xs text-gray-400">{formatDate(r.snapshotTime)}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
            <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50/40">
              <p className="text-xs text-gray-500">{data?.totalElements} compte(s)</p>
              <Pagination page={page} totalPages={data?.totalPages ?? 1} onPageChange={setPage} />
            </div>
          </>
        )}
      </Card>
    </div>
  )
}
