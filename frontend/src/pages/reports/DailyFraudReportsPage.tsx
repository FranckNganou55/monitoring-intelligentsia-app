import { PageIntro, SectionHeading } from '@/components/ui/PageIntro'
import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CalendarDays, Download, FileSpreadsheet, FileText, Loader2, RefreshCw } from 'lucide-react'
import toast from 'react-hot-toast'
import { Card } from '@/components/ui/Card'
import { EmptyState } from '@/components/feedback/EmptyState'
import { Pagination } from '@/components/ui/Pagination'
import { usePagination } from '@/hooks/usePagination'
import { reportService } from '@/services/reportService'
import { alertService } from '@/services/alertService'
import type { MonitoringAlert } from '@/types/alerts'
import { formatDate } from '@/utils/formatters'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'

function today() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

function nextDate(date: string) {
  const value = new Date(`${date}T00:00:00`)
  value.setDate(value.getDate() + 1)
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`
}

function nextMinute(date: string, time: string) {
  const value = new Date(`${date}T${time}:00`)
  value.setMinutes(value.getMinutes() + 1)
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}T${String(value.getHours()).padStart(2, '0')}:${String(value.getMinutes()).padStart(2, '0')}:00`
}

export function DailyFraudReportsPage() {
  const queryClient = useQueryClient()
  const institutionId = useInstitutionFilter()
  const { page, setPage, size } = usePagination()
  const [reportDate, setReportDate] = useState(today())
  const [endDate, setEndDate] = useState(today())
  const [fromTime, setFromTime] = useState('')
  const [untilTime, setUntilTime] = useState('')
  const [comment, setComment] = useState('')
  const [validatedComment, setValidatedComment] = useState('')
  const dateWasSelected = useRef(false)
  const [downloading, setDownloading] = useState<string | null>(null)
  const validDates = Boolean(reportDate && endDate && reportDate <= endDate)
  const reportsQuery = useQuery({
    enabled: validDates,
    queryKey: ['daily-fraud-reports', institutionId, page, reportDate, endDate],
    queryFn: () => reportService.list({ page, size, institutionId, fromDate: reportDate || undefined, toDate: endDate || undefined }),
  })
  const alertsQuery = useQuery({
    enabled: validDates,
    queryKey: ['daily-report-alert-preview', institutionId, reportDate, endDate, fromTime, untilTime],
    queryFn: () => alertService.list({
      category: 'FRAUD',
      institutionId,
      page: 0,
      size: 100,
      includeArchived: true,
      detectedFrom: reportDate ? `${reportDate}T${fromTime || '00:00'}:00` : undefined,
      detectedUntil: reportDate ? untilTime ? nextMinute(endDate, untilTime) : `${nextDate(endDate)}T00:00:00` : undefined,
    }),
  })
  const dayAlerts = alertsQuery.data?.content ?? []
  useEffect(() => {
    if (alertsQuery.isSuccess && !dateWasSelected.current && dayAlerts.length) {
      const dates = dayAlerts
        .map((alert) => alert.detectedAt.slice(0, 10))
        .sort()
      const newestDate = dates[dates.length - 1]
      if (newestDate && newestDate <= today()) { setReportDate(newestDate); setEndDate(newestDate) }
    }
  }, [alertsQuery.isSuccess, dayAlerts])
  const generateMutation = useMutation({
    mutationFn: async (format: 'WORD' | 'PDF' | 'EXCEL') => {
      if (reportDate === endDate) return reportService.generate(reportDate, format, comment, fromTime || undefined, untilTime || undefined, institutionId)
      const response = await reportService.exportPeriod(reportDate, endDate, format, comment, fromTime || undefined, untilTime || undefined, institutionId)
      const url = URL.createObjectURL(response.data)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `rapport_fraude_${reportDate}_${endDate}.${format === 'PDF' ? 'pdf' : format === 'WORD' ? 'docx' : 'xlsx'}`
      anchor.click()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
      return null
    },
    onSuccess: async (report) => {
      await queryClient.invalidateQueries({ queryKey: ['daily-fraud-reports'] })
      if (!report) toast.success('Rapport de la période téléchargé.')
      else if (report.status === 'FAILED') toast.error(report.errorMessage || 'Le serveur n’a pas pu créer le document.')
      else toast.success(`Rapport du ${report.reportDate} généré.`)
    },
    onError: (error: unknown) => {
      const apiMessage = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(apiMessage || 'Impossible de générer le rapport journalier. Vérifiez les droits de génération et la disponibilité du backend.')
    },
  })

  async function download(id: number, format: 'WORD' | 'PDF' | 'EXCEL') {
    const key = `${id}-${format}`
    setDownloading(key)
    try {
      const response = await reportService.download(id, format, institutionId)
      const url = URL.createObjectURL(response.data)
      const anchor = document.createElement('a')
      anchor.href = url
      const extension = format === 'WORD' ? 'docx' : format === 'PDF' ? 'pdf' : 'xlsx'
      anchor.download = `rapport_fraude_journalier_${reportsQuery.data?.content.find((item) => item.id === id)?.reportDate ?? id}.${extension}`
      anchor.click()
      URL.revokeObjectURL(url)
    } catch {
      toast.error('Le document du rapport est indisponible.')
    } finally {
      setDownloading(null)
    }
  }

  const reports = reportsQuery.data?.content ?? []
  const commentIsValidated = comment.trim().length > 0 && validatedComment === comment.trim()
  const invalidTimeRange = Boolean(!reportDate || !endDate || reportDate > endDate || (reportDate === endDate && fromTime && untilTime && fromTime > untilTime))
  return (
    <div className="space-y-5">
      <div className="page-header flex flex-wrap items-end justify-between gap-4">
        <PageIntro icon={CalendarDays} eyebrow="Documents et suivi" title="Rapports journaliers" description="Préparez vos synthèses quotidiennes et retrouvez les documents générés." />
        <button onClick={() => reportsQuery.refetch()} className="flex items-center gap-2 rounded-lg border px-3 py-2 text-sm text-gray-600 hover:bg-gray-50">
          <RefreshCw className="h-4 w-4" /> Actualiser
        </button>
      </div>

      <Card className="p-5 sm:p-6">
        <SectionHeading title="1. Choisir la période" description="Sélectionnez les dates de début et de fin, puis les heures si nécessaire." />
        {institutionId != null && (
          <p className="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
            L’aperçu ci-dessous suit l’institution sélectionnée. L’historique et les exports journaliers ne sont pas encore rattachés à une institution dans la base actuelle; leur génération et leurs anciens documents sont donc masqués dans ce contexte.
          </p>
        )}
        <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <label className="grid gap-1 text-sm text-gray-600">
            Du (date de début)
            <span className="relative">
              <CalendarDays className="absolute left-3 top-2.5 h-4 w-4 text-gray-400" />
              <input type="date" value={reportDate} max={today()} onChange={(event) => { dateWasSelected.current = true; setReportDate(event.target.value); if (event.target.value > endDate) setEndDate(event.target.value); setPage(0) }} className="w-full rounded-lg border border-gray-300 py-2 pl-9 pr-3" />
            </span>
          </label>
          <label className="grid gap-1 text-sm text-gray-600">Au (date de fin)<input type="date" value={endDate} min={reportDate} max={today()} onChange={(event) => { dateWasSelected.current = true; setEndDate(event.target.value); setPage(0) }} className="w-full rounded-lg border border-gray-300 px-3 py-2" /></label>
          <label className="grid gap-1 text-sm text-gray-600">Heure de début (facultative)<input type="time" value={fromTime} onChange={(event) => setFromTime(event.target.value)} className="w-full rounded-lg border border-gray-300 px-3 py-2" /></label>
          <label className="grid gap-1 text-sm text-gray-600">Heure de fin (facultative)<input type="time" value={untilTime} onChange={(event) => setUntilTime(event.target.value)} className="w-full rounded-lg border border-gray-300 px-3 py-2" /></label>

        </div>
        {invalidTimeRange && <p className="mt-2 text-sm text-red-600">Choisissez une date de fin et une heure postérieures au début de la période.</p>}
        <div className="mt-6 border-t border-gray-100 pt-5"><SectionHeading title="2. Ajouter les observations" /></div>
        <label className="mt-3 grid gap-1 text-sm text-gray-600">
          Commentaire à joindre au rapport
          <textarea
            value={comment}
            onChange={(event) => { setComment(event.target.value); setValidatedComment('') }}
            maxLength={2000}
            rows={3}
            required
            placeholder="Contexte, observations ou décisions à consigner…"
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-800 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
          />
        </label>
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <button type="button" onClick={() => setValidatedComment(comment.trim())} disabled={!comment.trim() || commentIsValidated} className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm font-medium text-emerald-800 hover:bg-emerald-100 disabled:cursor-not-allowed disabled:opacity-50">
            {commentIsValidated ? 'Commentaire validé' : 'Valider'}
          </button>
          <p className="text-xs text-gray-500">Validez le commentaire pour activer les exports. Toute modification du commentaire demandera une nouvelle validation.</p>
        </div>
        <p className="mt-2 text-xs text-gray-500">Le commentaire validé est intégré dans les trois formats. Laissez les heures vides pour inclure les journées entières; une heure de fin inclut la minute sélectionnée.</p>
        <div className="mt-6 space-y-3 border-t border-gray-100 pt-5"><SectionHeading title="3. Générer le document" />
          <div className="flex flex-wrap gap-2">
            <button onClick={() => generateMutation.mutate('WORD')} disabled={institutionId != null || !reportDate || invalidTimeRange || !commentIsValidated || generateMutation.isPending} className="flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
              {generateMutation.isPending && generateMutation.variables === 'WORD' ? <Loader2 className="h-4 w-4 animate-spin" /> : <FileText className="h-4 w-4" />}Générer le document Word
            </button>
            <button onClick={() => generateMutation.mutate('PDF')} disabled={institutionId != null || !reportDate || invalidTimeRange || !commentIsValidated || generateMutation.isPending} className="flex items-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50">
              {generateMutation.isPending && generateMutation.variables === 'PDF' ? <Loader2 className="h-4 w-4 animate-spin" /> : <FileText className="h-4 w-4" />}Générer le document PDF
            </button>
            <button onClick={() => generateMutation.mutate('EXCEL')} disabled={institutionId != null || !reportDate || invalidTimeRange || !commentIsValidated || generateMutation.isPending} className="flex items-center gap-2 rounded-lg bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50">
              {generateMutation.isPending && generateMutation.variables === 'EXCEL' ? <Loader2 className="h-4 w-4 animate-spin" /> : <FileSpreadsheet className="h-4 w-4" />}Générer le document Excel
            </button>
          </div>
        </div>
      </Card>

      <Card className="p-0 overflow-hidden">
        <div className="flex flex-wrap gap-3 items-center justify-between border-b px-4 py-3">
          <div>
            <h2 className="font-semibold text-gray-900">Alertes de la période sélectionnée</h2>
            <p className="text-sm text-gray-500">Alertes fraude de la période · {dayAlerts.length} affichée(s) sur {alertsQuery.data?.totalElements ?? 0} · l’export inclut toutes les alertes</p>
          </div>
          <button onClick={() => alertsQuery.refetch()} className="rounded-lg border px-3 py-2 text-sm text-gray-600 hover:bg-gray-50">Actualiser</button>
        </div>
        {alertsQuery.isLoading ? <div className="p-8 text-center text-sm text-gray-500">Chargement des alertes...</div>
          : alertsQuery.isError ? <div className="p-8 text-center text-sm text-red-600">Impossible de charger les alertes pour l’aperçu.</div>
          : !dayAlerts.length ? <EmptyState title="Aucune alerte pour cette période" description="Choisissez une période contenant des alertes, puis générez le document." />
          : <div className="overflow-x-auto"><table className="w-full text-sm"><thead><tr className="border-b bg-gray-50 text-left text-xs uppercase text-gray-500"><th className="px-4 py-3">Détection</th><th className="px-4 py-3">Alerte</th><th className="px-4 py-3">Catégorie</th><th className="px-4 py-3">Risque</th><th className="px-4 py-3">Statut</th><th className="px-4 py-3">GAB</th></tr></thead><tbody>{dayAlerts.map((alert: MonitoringAlert) => <tr key={alert.id} className="border-b last:border-0"><td className="whitespace-nowrap px-4 py-3">{new Date(alert.detectedAt).toLocaleString('fr-FR')}</td><td className="px-4 py-3"><span className="font-medium">{alert.title}</span><span className="ml-2 text-xs text-gray-400">#{alert.id} · {alert.ruleCode}</span></td><td className="px-4 py-3">{alert.category}</td><td className="px-4 py-3">{alert.riskLevel}</td><td className="px-4 py-3">{alert.status}</td><td className="px-4 py-3">{alert.atmId ?? '—'}</td></tr>)}</tbody></table></div>}
      </Card>

      <Card className="p-0 overflow-hidden">
        <div className="border-b border-gray-100 px-5 py-4"><SectionHeading title="Documents générés" description="Historique filtré du début à la fin de la période. Les rapports de plusieurs jours sont téléchargés directement; les rapports journaliers restent dans cet historique." /></div>
        {reportsQuery.isLoading ? <div className="p-8 text-center text-sm text-gray-500">Chargement...</div>
          : reportsQuery.isError ? <div className="p-8 text-center text-sm text-red-600">Impossible de charger l’historique des rapports. Vérifiez vos droits de consultation.</div>
          : !reports.length ? <EmptyState title="Aucun document généré" description={institutionId != null ? 'Aucun rapport institutionnel ne peut être affiché : les rapports existants ne sont pas rattachés à une institution.' : 'Les alertes ci-dessus sont déjà enregistrées. Choisissez un format pour créer le document journalier.'} />
          : <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead><tr className="border-b bg-gray-50 text-left text-xs uppercase text-gray-500">
                  <th className="px-4 py-3">Journée</th><th className="px-4 py-3">Alertes détectées</th><th className="px-4 py-3">Traitées</th><th className="px-4 py-3">En attente</th><th className="px-4 py-3">Généré le</th><th className="px-4 py-3">État</th><th className="px-4 py-3">Documents</th>
                </tr></thead>
                <tbody>{reports.map((report) => <tr key={report.id} className="border-b last:border-0">
                  <td className="px-4 py-3 font-medium">{report.reportDate}</td><td className="px-4 py-3">{report.totalDetected}</td><td className="px-4 py-3">{report.totalProcessed}</td><td className="px-4 py-3">{report.totalUnprocessed}</td><td className="px-4 py-3 text-gray-500">{formatDate(report.generatedAt)}</td>
                  <td className="px-4 py-3"><span className={report.status === 'GENERATED' ? 'text-green-700' : 'text-red-600'}>{report.status === 'GENERATED' ? 'Généré' : report.status === 'PARTIAL' ? 'Partiel' : 'Échec'}</span>{report.errorMessage && <div className="max-w-xs text-xs text-red-500">{report.errorMessage}</div>}</td>
                  <td className="px-4 py-3"><div className="flex flex-wrap gap-1">{report.availableFormats?.map((format) => <button key={format} onClick={() => download(report.id, format)} disabled={downloading === `${report.id}-${format}`} className={`inline-flex items-center gap-1 rounded-md border px-2 py-1 text-xs hover:bg-gray-50 disabled:opacity-50 ${format === 'WORD' ? 'text-blue-700' : format === 'PDF' ? 'text-red-700' : 'text-green-700'}`}><Download className="h-3 w-3" />{downloading === `${report.id}-${format}` ? 'Préparation...' : format}</button>)}</div></td>
                </tr>)}</tbody>
              </table>
            </div>
            {reportsQuery.data && reportsQuery.data.totalPages > 1 && <div className="border-t p-4"><Pagination page={page} totalPages={reportsQuery.data.totalPages} onPageChange={setPage} /></div>}
          </>}
      </Card>
    </div>
  )
}
