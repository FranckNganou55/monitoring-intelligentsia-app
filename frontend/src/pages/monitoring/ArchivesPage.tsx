import { useEffect, useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Archive, Download, Eye, FileText, RefreshCw, Search, X } from 'lucide-react'

import { alertService } from '@/services/alertService'
import type { AlertCategory, MonitoringAlert } from '@/types/alerts'
import { cn } from '@/utils/cn'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { institutionService } from '@/services/institutionService'
import { Pagination } from '@/components/ui/Pagination'
import toast from 'react-hot-toast'

const riskLabels: Record<string, string> = {
  CRITIQUE: 'Critique', MOYEN: 'Moyen', MOINS_CRITIQUE: 'Moins critique', TYPIQUE: 'Typique',
}

function riskClass(level: string) {
  switch (level) {
    case 'CRITIQUE':
      return 'bg-red-100 text-red-700 border-red-200'
    case 'MOYEN':
      return 'bg-orange-100 text-orange-700 border-orange-200'
    case 'MOINS_CRITIQUE':
      return 'bg-yellow-100 text-yellow-700 border-yellow-200'
    case 'TYPIQUE':
      return 'bg-blue-100 text-blue-700 border-blue-200'
    default:
      return 'bg-gray-100 text-gray-600 border-gray-200'
  }
}

export default function ArchivesPage() {
  const institutionId = useInstitutionFilter()
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState<AlertCategory | ''>('')
  const [archiveInstitutionId, setArchiveInstitutionId] = useState('')
  const [year, setYear] = useState('')
  const [month, setMonth] = useState('')
  const [day, setDay] = useState('')
  const [hour, setHour] = useState('')
  const [includeActive, setIncludeActive] = useState(true)
  const [page, setPage] = useState(0)
  const [exporting, setExporting] = useState<'WORD' | 'PDF' | 'EXCEL' | null>(null)
  const [selectedAlert, setSelectedAlert] =
    useState<MonitoringAlert | null>(null)

  const institutionsQuery = useQuery({ queryKey: ['institutions-all'], queryFn: institutionService.listAll })
  useEffect(() => setArchiveInstitutionId(institutionId ? String(institutionId) : ''), [institutionId])
  const filters = useMemo(() => ({
    // The archive is the permanent history of every alert, including alerts
    // that have not yet reached the ARCHIVED workflow status.
    includeArchived: true,
    institutionId: archiveInstitutionId ? Number(archiveInstitutionId) : undefined,
    category: category || undefined,
    search: search || undefined,
    year: year ? Number(year) : undefined,
    month: month ? Number(month) : undefined,
    day: day ? Number(day) : undefined,
    hour: hour ? Number(hour) : undefined,
  }), [archiveInstitutionId, category, search, year, month, day, hour])
  const alertsQuery = useQuery({
    queryKey: ['monitoring-alert-archives', filters, page],
    queryFn: () => alertService.list({ ...filters, page, size: 50 }),
  })

  const eventsQuery = useQuery({
    queryKey: ['monitoring-alert-archive-events', selectedAlert?.id],
    queryFn: () => alertService.getEvents(selectedAlert!.id),
    enabled: !!selectedAlert,
  })

  const archives = alertsQuery.data?.content ?? []

  async function downloadArchive(format: 'WORD' | 'PDF' | 'EXCEL') {
    setExporting(format)
    try {
      const response = await alertService.exportArchive({ ...filters, includeActive }, format)
      const url = URL.createObjectURL(response.data)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `archives_alertes${format === 'WORD' ? '.docx' : format === 'PDF' ? '.pdf' : '.xlsx'}`
      anchor.click()
      URL.revokeObjectURL(url)
    } catch {
      toast.error('Impossible d’exporter les archives avec ces filtres.')
    } finally {
      setExporting(null)
    }
  }

  const updateFilter = (setter: (value: string) => void) => (value: string) => {
    setter(value)
    setPage(0)
  }

  return (
    <div className="space-y-6 p-6">
      <div className="page-header">
        <div>
          <div className="flex items-center gap-2">
            <Archive className="h-6 w-6 text-gray-700" />
            <h1 className="text-2xl font-bold text-gray-900">
              Archives
            </h1>
          </div>

          <p className="mt-1 text-sm text-gray-500">
            Historique complet conservé sans limite de durée
            {alertsQuery.data ? ` · ${alertsQuery.data.totalElements} alerte(s)` : ''}.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <select aria-label="Alertes à exporter" value={includeActive ? 'ALL' : 'ARCHIVED'} onChange={(event) => setIncludeActive(event.target.value === 'ALL')} className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm">
            <option value="ALL">Toutes les alertes (actives et archivées)</option>
            <option value="ARCHIVED">Alertes archivées uniquement</option>
          </select>
          <button onClick={() => downloadArchive('EXCEL')} disabled={!!exporting} className="inline-flex items-center gap-2 rounded-lg border border-green-200 bg-white px-3 py-2 text-sm font-medium text-green-700 disabled:opacity-50"><Download className="h-4 w-4" />{exporting === 'EXCEL' ? 'Export…' : 'Exporter Excel'}</button>
          <button onClick={() => downloadArchive('WORD')} disabled={!!exporting} className="inline-flex items-center gap-2 rounded-lg border border-blue-200 bg-white px-3 py-2 text-sm font-medium text-blue-700 disabled:opacity-50"><FileText className="h-4 w-4" />{exporting === 'WORD' ? 'Export…' : 'Exporter Word'}</button>
          <button onClick={() => downloadArchive('PDF')} disabled={!!exporting} className="inline-flex items-center gap-2 rounded-lg border border-red-200 bg-white px-3 py-2 text-sm font-medium text-red-700 disabled:opacity-50"><Download className="h-4 w-4" />{exporting === 'PDF' ? 'Export…' : 'Exporter PDF'}</button>
          <button onClick={() => alertsQuery.refetch()} className="inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"><RefreshCw className={cn('h-4 w-4', alertsQuery.isFetching && 'animate-spin')} />Actualiser</button>
        </div>
      </div>

      <div className="rounded-xl border border-gray-100 bg-white p-4 shadow-sm">
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <div className="relative w-full max-w-md">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />

          <input
            value={search}
            onChange={(e) => updateFilter(setSearch)(e.target.value)}
            placeholder="Rechercher dans l’historique des alertes..."
            className="w-full rounded-lg border border-gray-200 py-2 pl-9 pr-3 text-sm outline-none focus:border-gold-500"
          />
        </div>
        <select
          value={category}
          onChange={(event) => updateFilter((value) => setCategory(value as AlertCategory | ''))(event.target.value)}
          aria-label="Filtrer les archives par catégorie"
          className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm"
        >
          <option value="">Toutes les catégories</option>
          <option value="FRAUD">Fraude</option>
          <option value="ATM">GAB</option>
          <option value="NETWORK">Réseau</option>
        </select>
        <select value={archiveInstitutionId} onChange={(event) => updateFilter(setArchiveInstitutionId)(event.target.value)} aria-label="Filtrer par institution" className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm">
          <option value="">Toutes les institutions</option>
          {institutionsQuery.data?.map((institution) => <option key={institution.id} value={institution.id}>{institution.name} · {institution.code}</option>)}
        </select>
        <input type="number" value={year} onChange={(event) => updateFilter(setYear)(event.target.value)} placeholder="Année" aria-label="Filtrer par année" min="2000" max="2100" className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm" />
        <select value={month} onChange={(event) => updateFilter(setMonth)(event.target.value)} aria-label="Filtrer par mois" className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm"><option value="">Tous les mois</option>{Array.from({ length: 12 }, (_, index) => index + 1).map((value) => <option key={value} value={value}>{String(value).padStart(2, '0')}</option>)}</select>
        <input type="number" value={day} onChange={(event) => updateFilter(setDay)(event.target.value)} placeholder="Jour" aria-label="Filtrer par jour" min="1" max="31" className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm" />
        <select value={hour} onChange={(event) => updateFilter(setHour)(event.target.value)} aria-label="Filtrer par heure" className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm"><option value="">Toutes les heures</option>{Array.from({ length: 24 }, (_, value) => value).map((value) => <option key={value} value={value}>{String(value).padStart(2, '0')}:00–{String(value).padStart(2, '0')}:59</option>)}</select>
        </div>
      </div>

      {alertsQuery.data && alertsQuery.data.totalPages > 1 && <div className="rounded-xl border border-gray-100 bg-white p-4"><Pagination page={page} totalPages={alertsQuery.data.totalPages} onPageChange={setPage} /></div>}

      <div className="rounded-xl border border-gray-100 bg-white shadow-sm overflow-hidden">
        {alertsQuery.isLoading ? (
          <div className="p-10 text-center text-sm text-gray-500">
            Chargement des archives...
          </div>
        ) : alertsQuery.isError ? (
          <div className="p-10 text-center text-sm text-red-600">
            Impossible de charger les archives.
          </div>
        ) : archives.length === 0 ? (
          <div className="p-10 text-center text-sm text-gray-500">
            Aucune alerte conservée pour ces filtres.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="border-b bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left font-semibold">
                    Alerte
                  </th>
                  <th className="px-4 py-3 text-left font-semibold">
                    Catégorie
                  </th>
                  <th className="px-4 py-3 text-left font-semibold">
                    Risque
                  </th>
                  <th className="px-4 py-3 text-left font-semibold">
                    Détection
                  </th>
                  <th className="px-4 py-3 text-left font-semibold">
                    Statut
                  </th>
                  <th className="px-4 py-3 text-right font-semibold">
                    Action
                  </th>
                </tr>
              </thead>

              <tbody className="divide-y">
                {archives.map((alert) => (
                  <tr
                    key={alert.id}
                    className="hover:bg-gray-50"
                  >
                    <td className="px-4 py-4">
                      <div className="font-medium text-gray-900">
                        {alert.title}
                      </div>
                      <div className="mt-1 text-xs text-gray-400">
                        #{alert.id} · {alert.ruleCode}
                      </div>
                      {alert.transactionReference && (
                        <div className="mt-1 text-xs text-gray-500">
                          Transaction : {alert.transactionReference}
                        </div>
                      )}
                    </td>

                    <td className="px-4 py-4">
                      <span className="rounded-full bg-gray-100 px-2.5 py-1 text-xs font-medium">
                        {alert.category}
                      </span>
                    </td>

                    <td className="px-4 py-4">
                      <span
                        className={cn(
                          'alert-status-badge border',
                          riskClass(alert.riskLevel)
                        )}
                      >
                        {riskLabels[alert.riskLevel] ?? alert.riskLevel}
                      </span>
                    </td>

                    <td className="whitespace-nowrap px-4 py-4 text-gray-500">
                      {new Date(alert.detectedAt).toLocaleString(
                        'fr-FR'
                      )}
                    </td>

                    <td className="whitespace-nowrap px-4 py-4 text-gray-500">
                      {alert.status === 'ARCHIVED' && alert.archivedAt
                        ? `Archivée le ${new Date(alert.archivedAt).toLocaleString('fr-FR')}`
                        : alert.status}
                    </td>

                    <td className="px-4 py-4 text-right">
                      <button
                        onClick={() => setSelectedAlert(alert)}
                        className="inline-flex items-center gap-2 rounded-lg border border-gray-200 px-3 py-2 text-xs font-medium text-gray-700 hover:bg-gray-100"
                      >
                        <Eye className="h-4 w-4" />
                        Détails
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {selectedAlert && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-2xl bg-white shadow-2xl">
            <div className="flex items-center justify-between border-b px-6 py-4">
              <div>
                <h2 className="text-lg font-bold text-gray-900">
                  Alerte archivée
                </h2>
                <p className="text-xs text-gray-400">
                  #{selectedAlert.id}
                </p>
              </div>

              <button
                onClick={() => setSelectedAlert(null)}
                className="rounded-lg p-2 text-gray-400 hover:bg-gray-100"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="space-y-6 p-6">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="text-xl font-semibold text-gray-900">
                    {selectedAlert.title}
                  </h3>

                  <span
                    className={cn(
                      'alert-status-badge border',
                      riskClass(selectedAlert.riskLevel)
                    )}
                  >
                    {riskLabels[selectedAlert.riskLevel] ?? selectedAlert.riskLevel}
                  </span>

                  <span className="rounded-full bg-gray-100 px-2.5 py-1 text-xs font-medium text-gray-600">
                    ARCHIVÉE
                  </span>
                </div>

                {selectedAlert.description && (
                  <p className="mt-3 text-sm text-gray-600">
                    {selectedAlert.description}
                  </p>
                )}
              </div>

              <div className="grid gap-4 rounded-xl bg-gray-50 p-4 sm:grid-cols-2 lg:grid-cols-3">
                <Info label="Règle" value={selectedAlert.ruleName} />
                <Info label="Code" value={selectedAlert.ruleCode} />
                <Info
                  label="Catégorie"
                  value={selectedAlert.category}
                />
                <Info
                  label="Transaction"
                  value={selectedAlert.transactionReference ?? '—'}
                />
                <Info
                  label="Détection"
                  value={new Date(
                    selectedAlert.detectedAt
                  ).toLocaleString('fr-FR')}
                />
                <Info
                  label="Archivage"
                  value={
                    selectedAlert.archivedAt
                      ? new Date(
                          selectedAlert.archivedAt
                        ).toLocaleString('fr-FR')
                      : '—'
                  }
                />
              </div>

              <div>
                <h3 className="mb-4 font-semibold">
                  Historique de traitement
                </h3>

                {eventsQuery.isLoading ? (
                  <p className="text-sm text-gray-500">
                    Chargement...
                  </p>
                ) : eventsQuery.data?.length ? (
                  <div className="space-y-4">
                    {eventsQuery.data.map((event) => (
                      <div
                        key={event.id}
                        className="flex gap-3"
                      >
                        <div className="mt-1 h-2.5 w-2.5 rounded-full bg-gray-400" />

                        <div>
                          <p className="text-sm font-medium">
                            {event.eventType}
                          </p>

                          {event.oldStatus && event.newStatus && (
                            <p className="text-xs text-gray-500">
                              {event.oldStatus} → {event.newStatus}
                            </p>
                          )}

                          {event.comment && (
                            <p className="mt-1 text-xs text-gray-500">
                              {event.comment}
                            </p>
                          )}

                          <p className="mt-1 text-xs text-gray-400">
                            {new Date(
                              event.createdAt
                            ).toLocaleString('fr-FR')}
                          </p>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-gray-500">
                    Aucun événement.
                  </p>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function Info({
  label,
  value,
}: {
  label: string
  value: string
}) {
  return (
    <div>
      <p className="text-xs text-gray-400">{label}</p>
      <p className="mt-1 text-sm font-medium text-gray-800">
        {value}
      </p>
    </div>
  )
}
