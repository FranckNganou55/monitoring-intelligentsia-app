import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, MapPin, Building2, RefreshCw, Clock, Cpu } from 'lucide-react'
import { atmService } from '@/services/atmService'
import { Card } from '@/components/ui/Card'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { PageLoader } from '@/components/feedback/PageLoader'
import { formatDate } from '@/utils/formatters'
import { atmStatusKey, statusColors } from '@/utils/colorMap'
import { cn } from '@/utils/cn'
import { AtmCassettes } from '@/components/atm/AtmCassettes'
import { AtmMovements } from '@/components/atm/AtmMovements'

// ── Info row ─────────────────────────────────────────────────────────────────

function InfoRow({ label, value, mono }: { label: string; value?: string | null; mono?: boolean }) {
  return (
    <div className="flex items-start justify-between gap-4 py-2.5 border-b border-gray-50 last:border-0">
      <span className="text-sm text-gray-500 shrink-0">{label}</span>
      <span className={cn('text-sm font-medium text-gray-900 text-right', mono && 'font-mono text-xs bg-gray-100 px-2 py-0.5 rounded')}>
        {value ?? '—'}
      </span>
    </div>
  )
}

// ── Page ─────────────────────────────────────────────────────────────────────

export function AtmDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const atmId = Number(id)

  const { data: atm, isLoading: atmLoading } = useQuery({
    queryKey: ['atm', atmId],
    queryFn: () => atmService.getById(atmId),
  })

  const { data: snapshot, refetch, isFetching } = useQuery({
    queryKey: ['atm-snapshot-latest', atmId],
    queryFn: () => atmService.latestSnapshot(atmId),
    enabled: !!atmId,
    refetchInterval: 30_000,
  })

  if (atmLoading) return <PageLoader />

  const status = atmStatusKey(snapshot)
  const statusDot = statusColors[status]

  return (
    <div className="space-y-6">

      {/* ── Header ── */}
      <div className="flex items-center gap-3">
        <button
          onClick={() => navigate(-1)}
          className="flex h-9 w-9 items-center justify-center rounded-xl border border-gray-200 bg-white text-gray-500 hover:text-gray-800 hover:border-gray-300 transition-all"
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <div className="flex-1">
          <div className="flex items-center gap-2.5">
            <span
              className="h-10 w-10 rounded-full shrink-0 animate-blink"
              style={{ backgroundColor: statusDot }}
            />
            <h1 className="text-xl font-bold text-gray-900 font-mono">{atm?.terminalId ?? `ATM #${atmId}`}</h1>
            <StatusBadge status={status} className="!text-lg !font-bold !px-2.5 !py-2" />
          </div>
          <p className="text-xs text-gray-400 mt-0.5 ml-5.5">
            {atm?.city && `${atm.city} · `}
            {atm?.institutionName}
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

      <div className="grid grid-cols-1 gap-5">

        {/* ── Info card ── */}
        <Card>
          <div className="flex items-center gap-2 mb-4">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gray-100">
              <Cpu className="h-4 w-4 text-gray-500" />
            </div>
            <h2 className="text-sm font-semibold text-gray-700">Informations</h2>
          </div>
          <InfoRow label="Terminal ID"  value={atm?.terminalId} mono />
          <InfoRow label="Institution"  value={atm?.institutionName} />
          <div className="flex items-start justify-between gap-4 py-2.5 border-b border-gray-50">
            <div className="flex items-center gap-1.5 text-sm text-gray-500 shrink-0">
              <MapPin className="h-3.5 w-3.5" />
              Ville
            </div>
            <span className="text-sm font-medium text-gray-900 text-right">{atm?.city ?? '—'}</span>
          </div>
          <div className="flex items-start justify-between gap-4 py-2.5 border-b border-gray-50">
            <div className="flex items-center gap-1.5 text-sm text-gray-500 shrink-0">
              <Building2 className="h-3.5 w-3.5" />
              Adresse
            </div>
            <span className="text-sm font-medium text-gray-900 text-right max-w-[160px]">{atm?.address ?? '—'}</span>
          </div>
          <div className="flex items-start justify-between gap-4 py-2.5">
            <div className="flex items-center gap-1.5 text-sm text-gray-500 shrink-0">
              <Clock className="h-3.5 w-3.5" />
              Mis à jour
            </div>
            <span className="text-xs font-medium text-gray-900 text-right">{formatDate(atm?.updatedAt)}</span>
          </div>
        </Card>

      </div>
      <AtmCassettes atmId={atmId} />
      {snapshot?.statusMessage && <div className="rounded-xl border border-gray-100 bg-white px-5 py-4 text-sm text-gray-600"><span className="font-medium">Message du GAB : </span>{snapshot.statusMessage}</div>}

      <AtmMovements atmId={atmId} />
    </div>
  )
}
