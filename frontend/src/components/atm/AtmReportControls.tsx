import { useState } from 'react'
import toast from 'react-hot-toast'
import { atmService, type ReportFormat, type MovementFilters } from '@/services/atmService'

export const movementStatuses = [
  ['', 'Tous les statuts'], ['DOWN', 'DOWN (0)'], ['UP', 'UP (1)'], ['SIGNOFF', 'SIGNOFF (3)'], ['SIGNON', 'SIGNON (4)'],
  ['ok', 'Opérationnel'], ['warning', 'Avertissement'], ['error', 'Hors ligne'], ['unknown', 'Inconnu'],
] as const

export function dateFilters(from: string, to: string): MovementFilters {
  return { from: from ? new Date(`${from}T00:00:00`).toISOString() : undefined,
    to: to ? new Date(`${to}T23:59:59.999`).toISOString() : undefined }
}

export function AtmReportControls({ params, disabled = false }: {
  params: Parameters<typeof atmService.exportReport>[1]
  disabled?: boolean
}) {
  const [busy, setBusy] = useState(false)
  async function download(format: ReportFormat) {
    setBusy(true)
    try { await atmService.exportReport(format, params) }
    catch { toast.error('Impossible de générer le rapport. Vérifiez les filtres et réessayez.') }
    finally { setBusy(false) }
  }
  return <div className="flex flex-wrap items-center gap-2">
    <span className="text-sm font-medium text-gray-700">{busy ? 'Génération…' : 'Exporter :'}</span>
    {(['PDF', 'WORD', 'EXCEL'] as const).map((format) => <button key={format} type="button" disabled={busy || disabled}
      onClick={() => void download(format)} className="rounded-lg border border-gray-200 bg-white px-4 py-2 text-sm font-semibold text-gray-700 hover:border-gold-500 disabled:opacity-50">
      {format === 'WORD' ? 'Word' : format === 'EXCEL' ? 'Excel' : 'PDF'}
    </button>)}
  </div>
}
