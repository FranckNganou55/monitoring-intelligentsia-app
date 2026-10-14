import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Mail, RefreshCw, Save, Send } from 'lucide-react'
import toast from 'react-hot-toast'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { institutionService } from '@/services/institutionService'
import { settingsService, type InstitutionAlertRouting } from '@/services/settingsService'
import { alertService } from '@/services/alertService'
import type { AlertRiskLevel } from '@/types/alerts'

type EmailKey = 'alertEmailTypique' | 'alertEmailMoinsCritique' | 'alertEmailMoyen' | 'alertEmailCritique'
type RoutingDraft = Record<EmailKey, string>
type RiskOption = { key: EmailKey; risk: AlertRiskLevel; label: string }

function exclusiveEndOfMinute(localDateTime: string): string {
  const end = new Date(`${localDateTime}:00`)
  end.setMinutes(end.getMinutes() + 1)
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())}T${pad(end.getHours())}:${pad(end.getMinutes())}:${pad(end.getSeconds())}`
}

const levels: RiskOption[] = [
  { key: 'alertEmailTypique', risk: 'TYPIQUE', label: 'Typique' },
  { key: 'alertEmailMoinsCritique', risk: 'MOINS_CRITIQUE', label: 'Moins critique' },
  { key: 'alertEmailMoyen', risk: 'MOYEN', label: 'Moyen' },
  { key: 'alertEmailCritique', risk: 'CRITIQUE', label: 'Critique' },
]

function draftFrom(institution: InstitutionAlertRouting): RoutingDraft {
  return {
    alertEmailTypique: institution.alertEmailTypique ?? '',
    alertEmailMoinsCritique: institution.alertEmailMoinsCritique ?? '',
    alertEmailMoyen: institution.alertEmailMoyen ?? '',
    alertEmailCritique: institution.alertEmailCritique ?? '',
  }
}

function InstitutionRoutingCard({ institution }: { institution: InstitutionAlertRouting }) {
  const queryClient = useQueryClient()
  const [draft, setDraft] = useState<RoutingDraft>(() => draftFrom(institution))
  const [detectedFrom, setDetectedFrom] = useState('')
  const [detectedUntil, setDetectedUntil] = useState('')
  const [selectedAlerts, setSelectedAlerts] = useState<Record<AlertRiskLevel, string>>({ TYPIQUE: '', MOINS_CRITIQUE: '', MOYEN: '', CRITIQUE: '' })

  useEffect(() => setDraft(draftFrom(institution)), [institution])

  const validPeriod = Boolean(detectedFrom && detectedUntil && detectedFrom < detectedUntil)
  const alertsQuery = useQuery({
    queryKey: ['institution-fraud-alerts-for-email', institution.id, detectedFrom, detectedUntil],
    queryFn: async () => {
      const filters = {
        category: 'FRAUD' as const,
        institutionId: institution.id,
        detectedFrom: `${detectedFrom}:00`,
        // The API uses an exclusive end bound. Include the whole selected minute.
        detectedUntil: exclusiveEndOfMinute(detectedUntil),
        size: 100,
      }
      const firstPage = await alertService.list({ ...filters, page: 0 })
      const remainingPages = await Promise.all(
        Array.from({ length: Math.max(firstPage.totalPages - 1, 0) }, (_, index) =>
          alertService.list({ ...filters, page: index + 1 }),
        ),
      )
      return { content: [...firstPage.content, ...remainingPages.flatMap((page) => page.content)] }
    },
    enabled: validPeriod,
    refetchInterval: 30_000,
  })

  const routingPayload = () => ({
      alertEmailTypique: draft.alertEmailTypique.trim() || null,
      alertEmailMoinsCritique: draft.alertEmailMoinsCritique.trim() || null,
      alertEmailMoyen: draft.alertEmailMoyen.trim() || null,
      alertEmailCritique: draft.alertEmailCritique.trim() || null,
  })

  const saveMutation = useMutation({
    mutationFn: () => settingsService.updateInstitutionAlertRouting(institution.id, routingPayload()),
    onSuccess: () => {
      toast.success(`Destinataires enregistrés pour ${institution.name}.`)
      void queryClient.invalidateQueries({ queryKey: ['institution-alert-routing'] })
      void queryClient.invalidateQueries({ queryKey: ['institutions'] })
    },
    onError: () => toast.error('Impossible d’enregistrer les adresses email.'),
  })

  const sendMutation = useMutation({
    mutationFn: async ({ alertId }: { alertId: number }) => {
      // Save the current addresses first so a just-entered recipient is used
      // for this explicit send, without enabling automatic notifications.
      await settingsService.updateInstitutionAlertRouting(institution.id, routingPayload())
      return alertService.sendConfiguredEmail(alertId, institution.id)
    },
    onSuccess: async () => {
      toast.success('Demande d’envoi enregistrée pour le destinataire configuré.')
      await queryClient.invalidateQueries({ queryKey: ['institution-alert-routing'] })
    },
    onError: (error: unknown) => {
      const response = (error as { response?: { status?: number; data?: { message?: string; error?: string } } })?.response
      const reason = response?.data?.message ?? response?.data?.error
      if (response?.status === 404 && (!reason || reason === 'Ressource introuvable.')) {
        toast.error('La route d’envoi est absente du backend actif. Recompilez et redémarrez le backend de cette version, puis réessayez.', { id: 'alert-email-send-error' })
        return
      }
      toast.error(reason
        ? `Envoi refusé (${response?.status ?? 'erreur'}): ${reason}`
        : 'Envoi refusé. Vérifiez l’adresse du niveau de risque et l’institution de l’alerte sélectionnée.', { id: 'alert-email-send-error' })
    },
  })

  const updateDraft = (key: EmailKey, value: string) => setDraft((current) => ({ ...current, [key]: value }))
  const alerts = alertsQuery.data?.content ?? []
  const alertsWithinPeriod = validPeriod
    ? alerts.filter((alert) => {
        const detectedAt = new Date(alert.detectedAt).getTime()
        const from = new Date(`${detectedFrom}:00`).getTime()
        const until = new Date(exclusiveEndOfMinute(detectedUntil)).getTime()
        return Number.isFinite(detectedAt) && detectedAt >= from && detectedAt < until
      })
    : []

  return (
    <section className="rounded-xl border border-gray-200 bg-gray-50/70 p-4">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-gray-900">{institution.name}</p>
          <p className="text-xs text-gray-500">Institution {institution.code}</p>
        </div>
        <Button size="sm" icon={<Save className="h-4 w-4" />} loading={saveMutation.isPending} onClick={() => saveMutation.mutate()}>
          Enregistrer les destinataires
        </Button>
      </div>

      <div className="mb-4 grid max-w-3xl gap-3 sm:grid-cols-2">
        <label className="grid gap-1 text-xs font-semibold text-gray-700">
          Détectée depuis
          <input type="datetime-local" value={detectedFrom} onChange={(event) => {
            setDetectedFrom(event.target.value)
            setSelectedAlerts({ TYPIQUE: '', MOINS_CRITIQUE: '', MOYEN: '', CRITIQUE: '' })
          }} className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm font-normal" />
        </label>
        <label className="grid gap-1 text-xs font-semibold text-gray-700">
          Détectée jusqu’au
          <input type="datetime-local" value={detectedUntil} onChange={(event) => {
            setDetectedUntil(event.target.value)
            setSelectedAlerts({ TYPIQUE: '', MOINS_CRITIQUE: '', MOYEN: '', CRITIQUE: '' })
          }} className="rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm font-normal" />
        </label>
        {detectedFrom && detectedUntil && detectedFrom >= detectedUntil && (
          <p className="text-xs text-red-700 sm:col-span-2">La date de fin doit être postérieure à la date de début.</p>
        )}
      </div>

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {levels.map(({ key, risk, label }) => {
          // Keep the picker strict even if a stale or incorrectly filtered API
          // response contains records outside the requested date/time range.
          const matchingAlerts = alertsWithinPeriod.filter((alert) => alert.riskLevel === risk)
          const selectedAlertIsAvailable = matchingAlerts.some((alert) => String(alert.id) === selectedAlerts[risk])
          return (
            <div key={risk} className="rounded-lg border border-gray-200 bg-white p-3">
              <label className="block text-xs font-semibold text-gray-700">
                Email — {label}
                <input
                  type="email"
                  value={draft[key]}
                  onChange={(event) => updateDraft(key, event.target.value)}
                  placeholder="Facultatif — destinataire"
                  className="mt-1.5 w-full rounded-lg border border-gray-200 px-3 py-2 text-sm font-normal text-gray-900"
                />
              </label>
              <label className="mt-3 block text-xs font-semibold text-gray-700">
                Choisir une alerte à envoyer
                <select
                  value={selectedAlerts[risk]}
                  onChange={(event) => setSelectedAlerts((current) => ({ ...current, [risk]: event.target.value }))}
                  disabled={!validPeriod || alertsQuery.isLoading || matchingAlerts.length === 0}
                  className="mt-1.5 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-xs font-normal text-gray-800 disabled:bg-gray-100"
                >
                  <option value="">{!validPeriod ? 'Choisissez une période' : alertsQuery.isLoading ? 'Chargement…' : matchingAlerts.length ? 'Sélectionner une alerte' : 'Aucune alerte sur cette période'}</option>
                  {matchingAlerts.map((alert) => (
                    <option key={alert.id} value={alert.id}>
                      #{alert.id} · {alert.title} · {new Date(alert.detectedAt).toLocaleString('fr-FR')}
                    </option>
                  ))}
                </select>
              </label>
              <Button
                variant="secondary"
                size="sm"
                className="mt-3 w-full justify-center"
                icon={<Send className="h-3.5 w-3.5" />}
                loading={sendMutation.isPending && sendMutation.variables?.alertId === Number(selectedAlerts[risk])}
                disabled={!draft[key].trim() || !selectedAlertIsAvailable || sendMutation.isPending}
                onClick={() => sendMutation.mutate({ alertId: Number(selectedAlerts[risk]) })}
              >
                Envoyer cette alerte
              </Button>
            </div>
          )
        })}
      </div>
      {alertsQuery.isError && <p className="mt-3 text-xs text-red-700">Impossible de charger les alertes fraude de cette institution.</p>}
    </section>
  )
}

export function InstitutionAlertRoutingSettings() {
  const queryClient = useQueryClient()
  const [selectedInstitutionId, setSelectedInstitutionId] = useState<number | null>(null)
  const institutionsQuery = useQuery({ queryKey: ['institution-alert-routing'], queryFn: institutionService.listAll, retry: false })
  const institutions = institutionsQuery.data ?? []

  useEffect(() => {
    if (institutions.length === 0) {
      setSelectedInstitutionId(null)
      return
    }
    if (!institutions.some((institution) => institution.id === selectedInstitutionId)) {
      setSelectedInstitutionId(institutions[0].id)
    }
  }, [institutions, selectedInstitutionId])

  const selectedInstitution = institutions.find((institution) => institution.id === selectedInstitutionId)

  return (
    <Card>
      <div className="space-y-5 px-6 py-5">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h2 className="flex items-center gap-2 text-sm font-semibold text-gray-900">
              <Mail className="h-4 w-4 text-gold-600" /> Destinataires des alertes fraude par niveau
            </h2>
            <p className="mt-1 text-xs text-gray-500">
              Choisissez une institution, renseignez une adresse par niveau, filtrez la période date/heure, puis sélectionnez l’alerte à transmettre. Aucun email n’est envoyé automatiquement.
            </p>
          </div>
          <Button variant="secondary" size="sm" icon={<RefreshCw className="h-4 w-4" />} loading={institutionsQuery.isFetching}
            onClick={() => void queryClient.invalidateQueries({ queryKey: ['institution-alert-routing'] })}>
            Actualiser
          </Button>
        </div>

        {institutionsQuery.isLoading && <p className="py-4 text-sm text-gray-500">Chargement des institutions…</p>}
        {institutionsQuery.isError && <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">Impossible de charger les institutions. Vérifiez vos droits d’administration.</p>}
        {institutionsQuery.data?.length === 0 && <p className="py-4 text-sm text-gray-500">Aucune institution configurée.</p>}
        {institutions.length > 0 && (
          <div className="space-y-4">
            <label className="grid max-w-xl gap-1 text-xs font-semibold text-gray-700">
              Institution destinataire
              <select
                value={selectedInstitutionId ?? ''}
                onChange={(event) => setSelectedInstitutionId(event.target.value ? Number(event.target.value) : null)}
                className="rounded-lg border border-gray-200 bg-white px-3 py-2.5 text-sm font-normal text-gray-900"
              >
                {institutions.map((institution) => (
                  <option key={institution.id} value={institution.id}>{institution.code} — {institution.name}</option>
                ))}
              </select>
            </label>
            {selectedInstitution && <InstitutionRoutingCard key={selectedInstitution.id} institution={selectedInstitution} />}
          </div>
        )}
      </div>
    </Card>
  )
}
