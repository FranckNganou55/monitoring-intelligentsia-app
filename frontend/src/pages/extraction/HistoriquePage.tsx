import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Download, RefreshCw, Clock, CheckCircle2, XCircle, Loader2, AlertCircle } from 'lucide-react'
import toast from 'react-hot-toast'
import { extractionJobService } from '@/services/extractionEngineService'
import api from '@/services/api'
import { usePagination } from '@/hooks/usePagination'
import { Card } from '@/components/ui/Card'
import { Pagination } from '@/components/ui/Pagination'
import { EmptyState } from '@/components/feedback/EmptyState'
import { formatDate } from '@/utils/formatters'
import { cn } from '@/utils/cn'
import type { ExtractionJobDto, JobStatus } from '@/types/extractions'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'

// ─── Status config ─────────────────────────────────────────────────────────────

const statusConfig: Record<JobStatus, { label: string; className: string; icon: React.ElementType }> = {
  EN_ATTENTE: { label: 'En attente', className: 'bg-gray-100 text-gray-600 border-gray-200',   icon: Clock         },
  EN_COURS:   { label: 'En cours',   className: 'bg-blue-50 text-blue-600 border-blue-200',    icon: Loader2       },
  SUCCES:     { label: 'Succès',     className: 'bg-green-50 text-green-700 border-green-200', icon: CheckCircle2  },
  ECHEC:      { label: 'Échec',      className: 'bg-red-50 text-red-600 border-red-200',       icon: XCircle       },
}

function StatusBadge({ status }: { status: JobStatus }) {
  const cfg = statusConfig[status] ?? statusConfig.ECHEC
  const Icon = cfg.icon
  return (
    <span className={cn('inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full border', cfg.className)}>
      <Icon className={cn('w-3 h-3', status === 'EN_COURS' && 'animate-spin')} />
      {cfg.label}
    </span>
  )
}

function formatDuration(ms?: number) {
  if (ms == null) return '—'
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

// ─── Component ────────────────────────────────────────────────────────────────

async function downloadJob(jobId: number) {
  try {
    // URL relative au baseURL de l'instance Axios (/api) — sans le préfixe /api
    const response = await api.get(`extraction/jobs/${jobId}/download`, {
      responseType: 'blob',
    })

    // Si la réponse est du JSON (erreur backend renvoyée en blob), la lire et l'afficher
    if (response.data instanceof Blob && response.data.type.includes('application/json')) {
      const text = await response.data.text()
      const json = JSON.parse(text)
      toast.error(json?.message ?? 'Erreur lors du téléchargement.', { duration: 6000 })
      return
    }

    const disposition = response.headers['content-disposition'] ?? ''
    const match = disposition.match(/filename="?([^";\n]+)"?/)
    const filename = match?.[1] ?? `extraction_${jobId}`

    const url = URL.createObjectURL(new Blob([response.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  } catch (err: unknown) {
    const axiosErr = err as { response?: { data?: Blob } }
    if (axiosErr.response?.data instanceof Blob) {
      try {
        const text = await axiosErr.response.data.text()
        const json = JSON.parse(text)
        toast.error(json?.message ?? 'Erreur lors du téléchargement.', { duration: 6000 })
        return
      } catch { /* ignore parse error */ }
    }
    toast.error('Impossible de télécharger le fichier.', { duration: 6000 })
  }
}

export function HistoriquePage() {
  const institutionId = useInstitutionFilter()
  const { page, setPage, size } = usePagination()
  const [expandedError, setExpandedError] = useState<number | null>(null)
  const [downloading, setDownloading] = useState<number | null>(null)

  const { data, isLoading, refetch, isFetching } = useQuery({
    queryKey: ['extraction-jobs', institutionId, page],
    queryFn: () => extractionJobService.list({ page, size, institutionId }),
    refetchInterval: 10_000,
  })

  const running = data?.content.some(j => j.status === 'EN_ATTENTE' || j.status === 'EN_COURS')

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-gray-900">Historique des extractions</h1>
        <button
          onClick={() => refetch()}
          disabled={isFetching}
          className="flex items-center gap-2 text-sm text-gray-500 hover:text-gray-900 disabled:opacity-50">
          <RefreshCw className={cn('w-4 h-4', isFetching && 'animate-spin')} />
          Actualiser
        </button>
      </div>

      {running && (
        <div className="flex items-center gap-2 text-sm text-blue-600 bg-blue-50 border border-blue-200 rounded-lg px-4 py-2">
          <Loader2 className="w-4 h-4 animate-spin" />
          Des extractions sont en cours — cette page se rafraîchit automatiquement.
        </div>
      )}

      <Card>
        {isLoading ? (
          <div className="p-8 text-center text-gray-400 text-sm">Chargement...</div>
        ) : !data?.content.length ? (
          <EmptyState title="Aucun historique" description="Les extractions déclenchées apparaîtront ici." />
        ) : (
          <>
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-100 bg-gray-50">
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">ID</th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Configuration</th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Institution</th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Déclenchement</th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Statut</th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Durée</th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Lignes</th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Lancé par</th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Date</th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {data.content.map((job: ExtractionJobDto) => (
                  <>
                    <tr key={job.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 font-mono text-xs text-gray-400">#{job.id}</td>
                      <td className="px-4 py-3 font-medium text-gray-900">{job.configName}</td>
                      <td className="px-4 py-3 text-gray-600">{job.institutionName}</td>
                      <td className="px-4 py-3">
                        <span className={cn('text-xs px-2 py-0.5 rounded font-mono',
                          job.triggerType === 'CRON'
                            ? 'bg-purple-50 text-purple-600'
                            : 'bg-gray-100 text-gray-600')}>
                          {job.triggerType}
                        </span>
                      </td>
                      <td className="px-4 py-3"><StatusBadge status={job.status} /></td>
                      <td className="px-4 py-3 text-gray-500">{formatDuration(job.durationMs)}</td>
                      <td className="px-4 py-3 text-gray-500">{job.rowCount ?? '—'}</td>
                      <td className="px-4 py-3 text-gray-500">{job.triggeredByLogin ?? '—'}</td>
                      <td className="px-4 py-3 text-gray-500 text-xs">{formatDate(job.createdAt)}</td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-1">
                          {job.downloadable && (
                            <button
                              onClick={async () => {
                                setDownloading(job.id)
                                await downloadJob(job.id)
                                setDownloading(null)
                              }}
                              disabled={downloading === job.id}
                              className="p-1.5 hover:bg-green-50 rounded text-gray-400 hover:text-green-600 disabled:opacity-50"
                              title="Télécharger le fichier">
                              {downloading === job.id
                                ? <Loader2 className="w-4 h-4 animate-spin" />
                                : <Download className="w-4 h-4" />}
                            </button>
                          )}
                          {job.errorMessage && (
                            <button
                              onClick={() => setExpandedError(expandedError === job.id ? null : job.id)}
                              className="p-1.5 hover:bg-red-50 rounded text-gray-400 hover:text-red-500"
                              title="Voir l'erreur">
                              <AlertCircle className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                    {expandedError === job.id && job.errorMessage && (
                      <tr key={`${job.id}-error`}>
                        <td colSpan={10} className="px-4 pb-3 bg-red-50">
                          <pre className="text-xs text-red-700 whitespace-pre-wrap">{job.errorMessage}</pre>
                        </td>
                      </tr>
                    )}
                  </>
                ))}
              </tbody>
            </table>

            {data.totalPages > 1 && (
              <div className="p-4 border-t border-gray-100">
                <Pagination page={page} totalPages={data.totalPages} onPageChange={setPage} />
              </div>
            )}
          </>
        )}
      </Card>
    </div>
  )
}
