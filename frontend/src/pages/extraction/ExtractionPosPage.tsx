import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Download, FileText, Calendar, Store, MapPin, Loader2, Plus, CheckSquare, Square } from 'lucide-react'
import toast from 'react-hot-toast'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { usePagination } from '@/hooks/usePagination'
import { extractionService } from '@/services/extractionService'
import { posService } from '@/services/posService'
import { Pagination } from '@/components/ui/Pagination'
import { Card } from '@/components/ui/Card'
import { Modal } from '@/components/ui/Modal'
import { PageLoader } from '@/components/feedback/PageLoader'
import { EmptyState } from '@/components/feedback/EmptyState'
import { formatDate } from '@/utils/formatters'
import { cn } from '@/utils/cn'
import type { ExtractionBatchDto, PosTerminalDto } from '@/types/monitoring'

function toInstant(date: string, eod = false): string {
  return new Date(date + (eod ? 'T23:59:59.999Z' : 'T00:00:00.000Z')).toISOString()
}

export function ExtractionPosPage() {
  const institutionId = useInstitutionFilter()
  const { page, size, setPage } = usePagination()
  const [downloading, setDownloading] = useState<number | null>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const [merchant, setMerchant] = useState('')
  const [city, setCity] = useState('')
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['extractions', 'POS', page, size, institutionId],
    queryFn: () => extractionService.list({ page, size, institutionId, type: 'POS' }),
  })

  // Load POS terminals for the selected institution when the modal is open
  const { data: posData, isLoading: posLoading } = useQuery({
    queryKey: ['pos-for-extraction', institutionId],
    queryFn: () => posService.list({ size: 500, institutionId }),
    enabled: showCreate && !!institutionId,
  })

  const terminals: PosTerminalDto[] = posData?.content ?? []

  const toggleTerminal = (terminalId: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      next.has(terminalId) ? next.delete(terminalId) : next.add(terminalId)
      return next
    })
  }

  const selectAll = () => setSelectedIds(new Set(terminals.map((t) => t.terminalId)))
  const deselectAll = () => setSelectedIds(new Set())

  const createMutation = useMutation({
    mutationFn: () =>
      extractionService.create({
        institutionId: institutionId as number,
        type: 'POS',
        startDate: toInstant(startDate),
        endDate: toInstant(endDate, true),
        terminalIds: Array.from(selectedIds),
        merchant: merchant.trim() || undefined,
        city: city.trim() || undefined,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['extractions', 'POS'] })
      setShowCreate(false)
      setStartDate('')
      setEndDate('')
      setSelectedIds(new Set())
      setMerchant('')
      setCity('')
      toast.success('Extraction créée avec succès')
    },
    onError: () => toast.error("Erreur lors de la création de l'extraction"),
  })

  const handleDownload = async (batch: ExtractionBatchDto) => {
    setDownloading(batch.id)
    try {
      const response = await extractionService.download(batch.id)
      const url = window.URL.createObjectURL(new Blob([response.data]))
      const link = document.createElement('a')
      link.href = url
      link.download = `extraction-pos-${batch.id}.csv`
      link.click()
      window.URL.revokeObjectURL(url)
    } catch {
      toast.error('Échec du téléchargement')
    } finally {
      setDownloading(null)
    }
  }

  const canCreate = !!institutionId && !!startDate && !!endDate && selectedIds.size > 0

  const batches = data?.content ?? []

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Extraction TPE / POS</h1>
          <p className="text-xs text-gray-400 mt-0.5">Téléchargement des transactions par commerçant et période</p>
        </div>
        <button
          onClick={() => setShowCreate(true)}
          className="flex items-center gap-2 rounded-lg bg-gold-500 px-4 py-2 text-sm font-semibold text-white hover:bg-gold-400 transition-colors shadow-sm"
        >
          <Plus className="h-4 w-4" />
          Nouvelle extraction
        </button>
      </div>

      {/* ── Creation Modal ── */}
      <Modal
        open={showCreate}
        onClose={() => setShowCreate(false)}
        title="Nouvelle extraction TPE / POS"
        size="md"
        footer={
          <>
            <button
              onClick={() => setShowCreate(false)}
              className="rounded-lg border border-gray-200 px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors"
            >
              Annuler
            </button>
            <button
              onClick={() => createMutation.mutate()}
              disabled={!canCreate || createMutation.isPending}
              className={cn(
                'flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-semibold text-white transition-all',
                'bg-gold-500 hover:bg-gold-400',
                'disabled:opacity-50 disabled:cursor-not-allowed',
              )}
            >
              {createMutation.isPending && <Loader2 className="h-4 w-4 animate-spin" />}
              Créer l'extraction
            </button>
          </>
        }
      >
        <div className="space-y-4">
          {!institutionId && (
            <p className="text-sm text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2">
              Sélectionnez une institution dans le menu en haut pour créer une extraction.
            </p>
          )}

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Date de début</label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                max={endDate || undefined}
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-gold-400 focus:outline-none focus:ring-1 focus:ring-gold-400"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">Date de fin</label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                min={startDate || undefined}
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-gold-400 focus:outline-none focus:ring-1 focus:ring-gold-400"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">
                Commerçant <span className="text-gray-400 font-normal">(optionnel)</span>
              </label>
              <input
                type="text"
                value={merchant}
                onChange={(e) => setMerchant(e.target.value)}
                placeholder="Nom du commerçant"
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-gold-400 focus:outline-none focus:ring-1 focus:ring-gold-400"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">
                Ville <span className="text-gray-400 font-normal">(optionnel)</span>
              </label>
              <input
                type="text"
                value={city}
                onChange={(e) => setCity(e.target.value)}
                placeholder="Ville"
                className="w-full rounded-lg border border-gray-200 px-3 py-2 text-sm focus:border-gold-400 focus:outline-none focus:ring-1 focus:ring-gold-400"
              />
            </div>
          </div>

          {/* Terminal selector */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-xs font-medium text-gray-700">
                Terminaux POS
                {terminals.length > 0 && (
                  <span className="ml-1.5 text-gray-400 font-normal">
                    — {selectedIds.size}/{terminals.length} sélectionné(s)
                  </span>
                )}
              </label>
              {terminals.length > 0 && (
                <div className="flex gap-2">
                  <button type="button" onClick={selectAll} className="text-xs text-gold-600 hover:text-gold-500 font-medium">
                    Tout sélectionner
                  </button>
                  <span className="text-gray-300">|</span>
                  <button type="button" onClick={deselectAll} className="text-xs text-gray-400 hover:text-gray-600">
                    Tout décocher
                  </button>
                </div>
              )}
            </div>

            <div className="border border-gray-200 rounded-lg overflow-hidden">
              {!institutionId ? (
                <p className="px-4 py-6 text-center text-sm text-gray-400">Sélectionnez d'abord une institution</p>
              ) : posLoading ? (
                <div className="flex items-center justify-center gap-2 py-6 text-gray-400">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  <span className="text-sm">Chargement des TPE…</span>
                </div>
              ) : terminals.length === 0 ? (
                <p className="px-4 py-6 text-center text-sm text-gray-400">Aucun TPE configuré pour cette institution</p>
              ) : (
                <div className="max-h-52 overflow-y-auto divide-y divide-gray-50">
                  {terminals.map((t) => {
                    const checked = selectedIds.has(t.terminalId)
                    return (
                      <label
                        key={t.id}
                        className={cn(
                          'flex items-center gap-3 px-4 py-2.5 cursor-pointer hover:bg-gray-50 transition-colors',
                          checked && 'bg-gold-50/60',
                        )}
                      >
                        <div className={cn('text-gray-300', checked && 'text-gold-500')}>
                          {checked ? <CheckSquare className="h-4 w-4" /> : <Square className="h-4 w-4" />}
                        </div>
                        <input
                          type="checkbox"
                          className="sr-only"
                          checked={checked}
                          onChange={() => toggleTerminal(t.terminalId)}
                        />
                        <div className="flex-1 min-w-0">
                          <span className="text-sm font-mono font-semibold text-gray-900">{t.terminalId}</span>
                          {t.merchant && <span className="ml-2 text-xs text-gray-500">{t.merchant}</span>}
                          {t.city && <span className="ml-1 text-xs text-gray-400">— {t.city}</span>}
                        </div>
                      </label>
                    )
                  })}
                </div>
              )}
            </div>
          </div>
        </div>
      </Modal>

      <Card className="p-0 overflow-hidden">
        {isLoading ? (
          <PageLoader />
        ) : !batches.length ? (
          <EmptyState title="Aucune extraction" description='Cliquez sur "Nouvelle extraction" pour créer un lot.' />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100 bg-gray-50/60">
                    {['Lot', 'Institution', 'Commerçant', 'Période', 'Créé le', ''].map((h) => (
                      <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {batches.map((b: ExtractionBatchDto) => (
                    <tr key={b.id} className="hover:bg-gray-50/80 transition-colors">
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gray-100">
                            <FileText className="h-3.5 w-3.5 text-gray-500" />
                          </div>
                          <span className="font-mono text-xs font-semibold text-gray-700">#{b.id}</span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-gray-700">{b.institutionName}</td>
                      <td className="px-4 py-3">
                        {b.merchant || b.city ? (
                          <div>
                            {b.merchant && (
                              <div className="flex items-center gap-1 text-xs font-medium text-gray-700">
                                <Store className="h-3 w-3 text-gray-400" />
                                {b.merchant}
                              </div>
                            )}
                            {b.city && (
                              <div className="flex items-center gap-1 text-xs text-gray-400">
                                <MapPin className="h-3 w-3" />
                                {b.city}
                              </div>
                            )}
                          </div>
                        ) : (
                          <span className="text-gray-300">—</span>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-1.5 text-xs text-gray-600">
                          <Calendar className="h-3.5 w-3.5 text-gray-400 shrink-0" />
                          <span>{formatDate(b.startDate, 'dd/MM/yyyy')}</span>
                          <span className="text-gray-300">→</span>
                          <span>{formatDate(b.endDate, 'dd/MM/yyyy')}</span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-xs text-gray-400">{formatDate(b.createdAt)}</td>
                      <td className="px-4 py-3">
                        <button
                          onClick={() => handleDownload(b)}
                          disabled={downloading === b.id}
                          className={cn(
                            'flex items-center gap-1.5 text-xs font-medium px-3 py-1.5 rounded-lg border transition-all',
                            'border-gold-300 text-gold-700 bg-gold-50 hover:bg-gold-100',
                            'disabled:opacity-50 disabled:cursor-not-allowed',
                          )}
                        >
                          {downloading === b.id
                            ? <Loader2 className="h-3.5 w-3.5 animate-spin" />
                            : <Download className="h-3.5 w-3.5" />
                          }
                          Télécharger
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50/40">
              <p className="text-xs text-gray-500">{data?.totalElements} lot(s)</p>
              <Pagination page={page} totalPages={data?.totalPages ?? 1} onPageChange={setPage} />
            </div>
          </>
        )}
      </Card>
    </div>
  )
}
