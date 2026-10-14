import { useEffect, useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import {
  Search,
  RefreshCw,
  Eye,
  X,
  CheckCircle2,
  Clock3,
  Archive,
  AlertTriangle,
  CirclePlus,
} from 'lucide-react'

import { alertService } from '@/services/alertService'
import { atmService } from '@/services/atmService'
import { ruleService } from '@/services/ruleService'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import type { MonitoringRule, RiskLevel } from '@/types/rules'
import type {
  AlertCategory,
  AlertRiskLevel,
  AlertStatus,
  MonitoringAlert,
} from '../../types/alerts'
import { formatRelative } from '@/utils/formatters'
import { cn } from '@/utils/cn'
import { Pagination } from '@/components/ui/Pagination'
import { notificationService } from '@/services/notificationService'

function riskClass(level: AlertRiskLevel) {
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

const alertFlashClass: Record<AlertRiskLevel, string> = {
  CRITIQUE: 'alert-flash-critical',
  MOYEN: 'alert-flash-medium',
  MOINS_CRITIQUE: 'alert-flash-low',
  TYPIQUE: 'alert-flash-typical',
}

function statusClass(status: AlertStatus) {
  switch (status) {
    case 'OPEN':
      return 'bg-red-100 text-red-700'
    case 'ACKNOWLEDGED':
      return 'bg-yellow-100 text-yellow-700'
    case 'IN_PROGRESS':
      return 'bg-blue-100 text-blue-700'
    case 'RESOLVED':
      return 'bg-green-100 text-green-700'
    case 'ARCHIVED':
      return 'bg-gray-100 text-gray-600'
  }
}

function statusLabel(status: AlertStatus) {
  switch (status) {
    case 'OPEN':
      return 'Ouverte'
    case 'ACKNOWLEDGED':
      return 'Prise en compte'
    case 'IN_PROGRESS':
      return 'En cours'
    case 'RESOLVED':
      return 'Résolue'
    case 'ARCHIVED':
      return 'Archivée'
  }
}

function nextAction(alert: MonitoringAlert): AlertStatus | null {
  switch (alert.status) {
    case 'OPEN':
      return 'ACKNOWLEDGED'
    case 'ACKNOWLEDGED':
      return 'IN_PROGRESS'
    case 'IN_PROGRESS':
      return 'RESOLVED'
    case 'RESOLVED':
      return 'ARCHIVED'
    default:
      return null
  }
}

function nextActionLabel(status: AlertStatus) {
  switch (status) {
    case 'ACKNOWLEDGED':
      return 'Prendre en compte'
    case 'IN_PROGRESS':
      return 'Mettre en cours'
    case 'RESOLVED':
      return 'Résoudre'
    case 'ARCHIVED':
      return 'Archiver'
    default:
      return ''
  }
}

export default function AlertsPage() {
  const queryClient = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  const institutionId = useInstitutionFilter()
  const signaledAlertIdsQuery = useQuery({
    queryKey: ['signaled-open-alert-ids', institutionId],
    queryFn: () => notificationService.openAlertIds(institutionId),
    refetchInterval: 5_000,
    refetchOnWindowFocus: true,
  })
  const [testOpen, setTestOpen] = useState(false)
  const [testRuleId, setTestRuleId] = useState('')
  const [testAtmId, setTestAtmId] = useState('')
  const [testMessage, setTestMessage] = useState<string | null>(null)
  const testRulesQuery = useQuery({ queryKey: ['test-alert-rules'], queryFn: () => ruleService.getAll() })
  const testAtmsQuery = useQuery({
    queryKey: ['test-alert-atms', institutionId],
    // Use the same default institution scoping as the GAB page. The API applies
    // the signed-in user's institution or the institution selected in the header.
    queryFn: async () => {
      // Use the institution endpoint so the modal sees every registered GAB,
      // independent of the current page size and JSON number/string coercion.
      return atmService.listByInstitution(Number(institutionId))
    },
    enabled: testOpen && !!institutionId,
  })
  const testRules = (testRulesQuery.data ?? []).filter((rule) => rule.enabled && rule.notificationEnabled)
  const testAtms = testAtmsQuery.data ?? []
  const riskLabels: Record<RiskLevel, string> = { TYPIQUE: 'Typique', MOINS_CRITIQUE: 'Moins critique', MOYEN: 'Moyen', CRITIQUE: 'Critique' }
  const testAlertMutation = useMutation({
    mutationFn: async () => {
      const rule = testRules.find((item) => item.id === Number(testRuleId)) as MonitoringRule | undefined
      const atm = testAtms.find((item) => item.id === Number(testAtmId))
      if (!rule || !atm) throw new Error('Rule or ATM missing')
      return alertService.create({
        ruleId: rule.id,
        category: rule.category,
        title: `[TEST] ${rule.code} - ${rule.name}`,
        description: `Alerte de test pour vérifier la notification et le niveau ${riskLabels[rule.riskLevel]}.`,
        institutionId: atm.institutionId,
        atmId: atm.id,
      })
    },
    onSuccess: async (alert) => {
      await queryClient.invalidateQueries({ queryKey: ['monitoring-alerts'] })
      await queryClient.invalidateQueries({ queryKey: ['notifications-page'] })
      await queryClient.invalidateQueries({ queryKey: ['notifications-count'] })
      setTestOpen(false)
      setTestMessage(`Alerte TEST créée (#${alert.id}). L’écran clignotant et le signal sonore suivront le délai configuré pour son niveau.`)
    },
    onError: () => setTestMessage('Impossible de créer l’alerte de test. Vérifiez les droits et réessayez.'),
  })

  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<AlertStatus | ''>('')
  const [category, setCategory] = useState<AlertCategory | ''>('')
  const [riskLevel, setRiskLevel] = useState<AlertRiskLevel | ''>('')
  const [detectedFrom, setDetectedFrom] = useState('')
  const [detectedUntil, setDetectedUntil] = useState('')
  const [showAllAlerts, setShowAllAlerts] = useState(false)
  const [alertPage, setAlertPage] = useState(0)
  const [selectedAlert, setSelectedAlert] =
    useState<MonitoringAlert | null>(null)
  const [resolveComment, setResolveComment] = useState('')
  const selectedAlertIsSignaled = selectedAlert?.status === 'OPEN'
    && (signaledAlertIdsQuery.data ?? []).includes(selectedAlert.id)

  const params = useMemo(
    () => ({
      page: showAllAlerts ? alertPage : 0,
      size: showAllAlerts ? 100 : 5,
      institutionId,
      search: search || undefined,
      status: status || undefined,
      category: category || undefined,
      riskLevel: riskLevel || undefined,
      detectedFrom: detectedFrom ? `${detectedFrom}:00` : undefined,
      detectedUntil: detectedUntil ? `${detectedUntil}:00` : undefined,
    }),
    [institutionId, search, status, category, riskLevel, detectedFrom, detectedUntil, showAllAlerts, alertPage]
  )

  useEffect(() => {
    setAlertPage(0)
  }, [search, status, category, riskLevel, detectedFrom, detectedUntil])

  const alertsQuery = useQuery({
    queryKey: ['monitoring-alerts', params],
    queryFn: () => alertService.list(params),
  })

  const requestedAlertId = Number(searchParams.get('alertId'))
  const requestedAlertQuery = useQuery({
    queryKey: ['monitoring-alert', requestedAlertId],
    queryFn: () => alertService.getById(requestedAlertId),
    enabled: Number.isInteger(requestedAlertId) && requestedAlertId > 0,
  })

  useEffect(() => {
    if (requestedAlertQuery.data) setSelectedAlert(requestedAlertQuery.data)
  }, [requestedAlertQuery.data])

  const eventsQuery = useQuery({
    queryKey: ['monitoring-alert-events', selectedAlert?.id],
    queryFn: () =>
      alertService.getEvents(selectedAlert!.id),
    enabled: !!selectedAlert,
  })

  const statusMutation = useMutation({
    mutationFn: ({
      id,
      nextStatus,
      comment,
    }: {
      id: number
      nextStatus: AlertStatus
      comment?: string
    }) =>
      alertService.updateStatus(id, {
        status: nextStatus,
        comment: comment ?? `Action effectuée depuis l'interface des alertes`,
      }),

    onSuccess: (updatedAlert) => {
      setSelectedAlert(
        updatedAlert.status === 'ARCHIVED' ? null : updatedAlert
      )
      setResolveComment('')

      queryClient.invalidateQueries({
        queryKey: ['monitoring-alerts'],
      })

      queryClient.invalidateQueries({
        queryKey: ['monitoring-alert-events', updatedAlert.id],
      })
    },
  })

  const alerts = alertsQuery.data?.content ?? []

  return (
    <div className="space-y-5">
      <div className="page-header">
        <div className="flex items-start gap-4">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl border border-gold-200 bg-gold-50 text-gold-700">
            <AlertTriangle className="h-6 w-6" />
          </div>
          <div>
            <p className="page-eyebrow">Surveillance opérationnelle</p>
            <h1 className="page-heading">Alertes</h1>
            <p className="page-description">Suivez les alertes de fraude et des GAB, de leur détection à leur résolution.</p>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <button type="button" onClick={() => { setTestMessage(null); setTestOpen(true) }} className="btn-secondary inline-flex items-center gap-2 whitespace-nowrap text-sm" title="Créer une alerte TEST pour un GAB de l’institution sélectionnée">
            <CirclePlus size={16} />Alerte de test
          </button>
          <button type="button" onClick={() => void alertsQuery.refetch()} className="inline-flex items-center gap-2 whitespace-nowrap rounded-lg bg-gray-900 px-4 py-2 text-sm font-semibold text-white hover:bg-gray-800">
            <RefreshCw className={cn('h-4 w-4', alertsQuery.isFetching && 'animate-spin')} />Actualiser
          </button>
        </div>
      </div>

      {testMessage && <p className="rounded-lg border border-blue-200 bg-white p-3 text-sm text-blue-800">{testMessage}</p>}

      {testOpen && <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" role="dialog" aria-modal="true" aria-labelledby="test-alert-title"><div className="w-full max-w-xl rounded-2xl bg-white p-6 shadow-2xl">
        <div className="mb-5 flex items-start justify-between"><div><h2 id="test-alert-title" className="text-lg font-bold">Nouvelle alerte de test</h2><p className="mt-1 text-sm text-gray-500">Elle sera préfixée [TEST] et associée au GAB sélectionné.</p></div><button onClick={() => setTestOpen(false)} aria-label="Fermer"><X /></button></div>
        <div className="space-y-4">
          <label className="block text-sm font-medium">Règle active avec notifications<select value={testRuleId} onChange={(event) => setTestRuleId(event.target.value)} className="mt-1 w-full rounded-lg border px-3 py-2"><option value="">Choisir une règle</option>{testRules.map((rule) => <option key={rule.id} value={rule.id}>{rule.code} — {rule.name} ({riskLabels[rule.riskLevel]})</option>)}</select></label>
          <label className="block text-sm font-medium">GAB de l’institution<select value={testAtmId} onChange={(event) => setTestAtmId(event.target.value)} className="mt-1 w-full rounded-lg border px-3 py-2" disabled={!institutionId || testAtmsQuery.isLoading}><option value="">{!institutionId ? 'Sélectionnez une institution' : testAtmsQuery.isLoading ? 'Chargement des GAB...' : 'Choisir un GAB'}</option>{testAtms.map((atm) => <option key={atm.id} value={atm.id}>{atm.terminalId} — {atm.institutionName}</option>)}</select></label>
          {testAtmsQuery.isError && <p className="text-sm text-red-700">Impossible de charger les GAB de cette institution.</p>}
          {!testAtmsQuery.isLoading && !testAtmsQuery.isError && institutionId && testAtms.length === 0 && <p className="text-sm text-amber-700">Aucun GAB enregistré dans cette institution.</p>}
          {!institutionId && <p className="text-sm text-amber-700">Choisissez d’abord l’institution dans l’en-tête.</p>}
          {testRules.length === 0 && !testRulesQuery.isLoading && <p className="text-sm text-amber-700">Aucune règle active avec notifications autorisées.</p>}
          <div className="flex justify-end gap-2 pt-2"><button onClick={() => setTestOpen(false)} className="rounded-lg border px-4 py-2">Annuler</button><button disabled={testAlertMutation.isPending || !testRuleId || !testAtmId} onClick={() => testAlertMutation.mutate()} className="rounded-lg bg-gray-900 px-4 py-2 font-medium text-white disabled:opacity-50">{testAlertMutation.isPending ? 'Création...' : 'Créer et notifier'}</button></div>
        </div>
      </div></div>}

      <div className="panel">
        <div className="panel-heading">
          <div><h2 className="text-sm font-semibold text-gray-900">Filtrer les alertes</h2><p className="mt-1 text-xs text-gray-500">Choisissez les critères et la période de détection.</p></div>
          {(search || status || category || riskLevel || detectedFrom || detectedUntil) && (
            <button type="button" onClick={() => { setSearch(''); setStatus(''); setCategory(''); setRiskLevel(''); setDetectedFrom(''); setDetectedUntil('') }} className="inline-flex items-center gap-1.5 text-xs font-semibold text-gray-600 hover:text-gray-900">
              <X size={14} />Réinitialiser les filtres
            </button>
          )}
        </div>
        <div className="grid gap-4 p-5 sm:grid-cols-2 xl:grid-cols-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />

            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Titre, règle ou référence…"
              aria-label="Rechercher une alerte"
              className="w-full rounded-lg border border-gray-200 py-2 pl-9 pr-3 text-sm outline-none focus:border-gold-500"
            />
          </div>

          <select
            aria-label="Statut des alertes"
            value={status}
            onChange={(e) =>
              setStatus(e.target.value as AlertStatus | '')
            }
            className="rounded-lg border border-gray-200 px-3 py-2 text-sm"
          >
            <option value="">Tous les statuts</option>
            <option value="OPEN">Ouverte</option>
            <option value="ACKNOWLEDGED">
              Prise en compte
            </option>
            <option value="IN_PROGRESS">En cours</option>
            <option value="RESOLVED">Résolue</option>
          </select>

          <select
            aria-label="Catégorie des alertes"
            value={category}
            onChange={(e) =>
              setCategory(e.target.value as AlertCategory | '')
            }
            className="rounded-lg border border-gray-200 px-3 py-2 text-sm"
          >
            <option value="">Toutes les catégories</option>
            <option value="FRAUD">Fraude</option>
            <option value="ATM">GAB</option>
            <option value="NETWORK">Réseau</option>
          </select>

          <select
            aria-label="Niveau de risque"
            value={riskLevel}
            onChange={(e) =>
              setRiskLevel(e.target.value as AlertRiskLevel | '')
            }
            className="rounded-lg border border-gray-200 px-3 py-2 text-sm"
          >
            <option value="">Tous les niveaux</option>
            <option value="TYPIQUE">Typique</option>
            <option value="MOINS_CRITIQUE">
              Moins critique
            </option>
            <option value="MOYEN">Moyen</option>
            <option value="CRITIQUE">Critique</option>
          </select>

          <label className="grid gap-1.5 text-xs font-medium text-gray-500 xl:col-span-2">
            Détectée depuis
            <input type="datetime-local" value={detectedFrom} onChange={(event) => setDetectedFrom(event.target.value)} className="rounded-lg border border-gray-200 px-3 py-2 text-sm font-normal text-gray-800" />
          </label>
          <label className="grid gap-1.5 text-xs font-medium text-gray-500 xl:col-span-2">
            Détectée jusqu’au
            <input type="datetime-local" value={detectedUntil} onChange={(event) => setDetectedUntil(event.target.value)} className="rounded-lg border border-gray-200 px-3 py-2 text-sm font-normal text-gray-800" />
          </label>
        </div>
      </div>

      <div className="panel overflow-hidden">
        <div className="panel-heading">
          <div className="flex items-center gap-3"><h2 className="text-sm font-semibold text-gray-900">Liste des alertes</h2>
            {!alertsQuery.isLoading && !alertsQuery.isError && <span className="rounded-full bg-gray-100 px-2.5 py-1 text-xs font-semibold text-gray-600">{alertsQuery.data?.totalElements ?? 0} résultat(s)</span>}
          </div>
          <p className="text-xs text-gray-500">{showAllAlerts ? 'Toutes les alertes correspondant aux filtres' : 'Les alertes les plus récentes'}</p>
        </div>
        {alertsQuery.isLoading ? (
          <div className="p-10 text-center text-sm text-gray-500">
            Chargement des alertes...
          </div>
        ) : alertsQuery.isError ? (
          <div className="p-10 text-center text-sm text-red-600">
            Impossible de charger les alertes.
          </div>
        ) : alerts.length === 0 ? (
          <div className="p-10 text-center text-sm text-gray-500">
            Aucune alerte trouvée.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1080px] text-sm">
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
                    Statut
                  </th>
                  <th className="px-4 py-3 text-left font-semibold">
                    Détection
                  </th>
                  <th className="px-4 py-3 text-right font-semibold">
                    Action
                  </th>
                </tr>
              </thead>

              <tbody className="divide-y">
                {alerts.map((alert) => {
                  const action = nextAction(alert)

                  return (
                    <tr
                      key={alert.id}
                      className="hover:bg-gray-50"
                    >
                      <td className="px-4 py-4">
                        <div className="max-w-[420px] min-w-[250px] font-semibold leading-relaxed text-gray-900">
                          {alert.title}
                        </div>
                        <div className="mt-1 text-xs text-gray-400">
                          #{alert.id} · {alert.ruleCode}
                        </div>
                      </td>

                      <td className="px-4 py-4">
                        <span className="alert-status-badge bg-gray-100 text-gray-600">
                          {alert.category === 'ATM' ? 'GAB' : alert.category === 'FRAUD' ? 'Fraude' : 'Réseau'}
                        </span>
                      </td>

                      <td className="px-4 py-4">
                        <span
                          className={cn(
                            'alert-status-badge border',
                            riskClass(alert.riskLevel)
                          )}
                        >
                          {riskLabels[alert.riskLevel]}
                        </span>
                      </td>

                      <td className="px-4 py-4">
                        <span
                          className={cn(
                            'alert-status-badge',
                            statusClass(alert.status)
                          )}
                        >
                          {statusLabel(alert.status)}
                        </span>
                      </td>

                      <td className="whitespace-nowrap px-4 py-4 text-gray-500">
                        {formatRelative(alert.detectedAt)}
                      </td>

                      <td className="px-4 py-4">
                        <div className="flex justify-end gap-2">
                          <button
                            onClick={() =>
                              setSelectedAlert(alert)
                            }
                            className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-gray-200 text-gray-500 hover:border-gold-300 hover:bg-gold-50 hover:text-gold-700"
                            title="Voir le détail"
                            aria-label={`Voir le détail de l’alerte ${alert.id}`}
                          >
                            <Eye className="h-4 w-4" />
                          </button>

                          {action && (
                            <button
                              onClick={() => {
                                if (action === 'RESOLVED') {
                                  setResolveComment('')
                                  setSelectedAlert(alert)
                                  return
                                }
                                statusMutation.mutate({
                                  id: alert.id,
                                  nextStatus: action,
                                })
                              }}
                              disabled={statusMutation.isPending}
                              className="inline-flex min-h-9 min-w-[125px] items-center justify-center whitespace-nowrap rounded-lg border border-gray-200 bg-white px-3 py-2 text-xs font-semibold text-gray-700 hover:border-gold-300 hover:bg-gold-50 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                              {nextActionLabel(action)}
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
        {!alertsQuery.isLoading && !alertsQuery.isError && (alertsQuery.data?.totalElements ?? 0) > 0 && (
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-gray-100 px-4 py-3">
            <p className="text-xs text-gray-500">
              {showAllAlerts
                ? `${alerts.length} sur ${alertsQuery.data?.totalElements ?? 0} alerte(s)`
                : `${alerts.length} dernières alertes sur ${alertsQuery.data?.totalElements ?? 0}`}
            </p>
            {showAllAlerts ? (
              <div className="flex items-center gap-3">
                <button onClick={() => { setShowAllAlerts(false); setAlertPage(0) }} className="text-sm font-medium text-gray-600 hover:text-gray-900">Réduire</button>
                <Pagination page={alertPage} totalPages={alertsQuery.data?.totalPages ?? 0} onPageChange={setAlertPage} />
              </div>
            ) : (alertsQuery.data?.totalElements ?? 0) > 5 ? (
              <button onClick={() => { setAlertPage(0); setShowAllAlerts(true) }} className="text-sm font-semibold text-yellow-700 hover:text-yellow-800">Afficher toutes les alertes</button>
            ) : null}
          </div>
        )}
      </div>

      {selectedAlert && (
        <div
          className={`fixed inset-0 ${selectedAlertIsSignaled ? `z-[9999] ${alertFlashClass[selectedAlert.riskLevel]}` : 'z-50 bg-black/40'} flex items-center justify-center overflow-y-auto p-4`}
          role={selectedAlertIsSignaled ? 'alertdialog' : 'dialog'}
          aria-modal="true"
          aria-labelledby="selected-alert-title"
        >
          <div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-2xl bg-white shadow-2xl">
            <div className="flex items-center justify-between border-b px-6 py-4">
              <div>
                <h2 id="selected-alert-title" className="text-lg font-bold text-gray-900">
                  Détail de l'alerte
                </h2>
                <p className="text-xs text-gray-400">
                  Alerte #{selectedAlert.id}
                </p>
              </div>

              <button
                type="button"
                aria-label={selectedAlertIsSignaled ? 'Prendre en compte l’alerte avant de fermer' : 'Fermer le détail'}
                title={selectedAlertIsSignaled ? 'Prenez en compte cette alerte avant de fermer le détail.' : 'Fermer le détail'}
                disabled={selectedAlertIsSignaled}
                onClick={() => {
                  setSelectedAlert(null)
                  if (searchParams.has('alertId')) setSearchParams({}, { replace: true })
                }}
                className="rounded-lg p-2 text-gray-400 hover:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="space-y-6 p-6">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="text-xl font-semibold">
                    {selectedAlert.title}
                  </h3>

                  <span
                    className={cn(
                      'alert-status-badge border',
                      riskClass(selectedAlert.riskLevel)
                    )}
                  >
                    {riskLabels[selectedAlert.riskLevel]}
                  </span>

                  <span
                    className={cn(
                      'alert-status-badge',
                      statusClass(selectedAlert.status)
                    )}
                  >
                    {statusLabel(selectedAlert.status)}
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
                <Info label="Code règle" value={selectedAlert.ruleCode} />
                <Info label="Catégorie" value={selectedAlert.category} />
                <Info label="Terminal GAB" value={selectedAlert.atmId?.toString() ?? '—'} />
                <Info label="Détection fraude" value={selectedAlert.fraudDetectionId?.toString() ?? '—'} />
                <Info
                  label="Transaction"
                  value={
                    selectedAlert.transactionReference ?? '—'
                  }
                />
                <Info
                  label="Détection"
                  value={new Date(
                    selectedAlert.detectedAt
                  ).toLocaleString('fr-FR')}
                />
                <Info
                  label="Institution"
                  value={
                    selectedAlert.institutionId?.toString() ?? '—'
                  }
                />
              </div>

              {nextAction(selectedAlert) && (
                <div className="space-y-3 rounded-xl border border-gray-200 bg-gray-50 p-4">
                  {nextAction(selectedAlert) === 'RESOLVED' && (
                    <label className="block text-sm font-medium text-gray-800">
                      Commentaire de résolution <span className="text-red-600">obligatoire</span>
                      <textarea
                        value={resolveComment}
                        onChange={(event) => setResolveComment(event.target.value)}
                        maxLength={2000}
                        rows={3}
                        placeholder="Décrivez les vérifications et l’action effectuée…"
                        className="mt-1.5 w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm font-normal outline-none focus:border-gold-500"
                      />
                    </label>
                  )}
                  <button
                    onClick={() => statusMutation.mutate({
                      id: selectedAlert.id,
                      nextStatus: nextAction(selectedAlert)!,
                      comment: nextAction(selectedAlert) === 'RESOLVED' ? resolveComment.trim() : undefined,
                    })}
                    disabled={statusMutation.isPending || (nextAction(selectedAlert) === 'RESOLVED' && !resolveComment.trim())}
                    className="rounded-lg bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:opacity-50"
                  >
                    {nextActionLabel(nextAction(selectedAlert)!)}
                  </button>
                </div>
              )}

              <div>
                <h3 className="mb-4 flex items-center gap-2 font-semibold">
                  <Clock3 className="h-4 w-4" />
                  Historique
                </h3>

                {eventsQuery.isLoading ? (
                  <p className="text-sm text-gray-500">
                    Chargement de l'historique...
                  </p>
                ) : eventsQuery.data?.length ? (
                  <div className="space-y-4">
                    {eventsQuery.data.map((event) => (
                      <div
                        key={event.id}
                        className="flex gap-3"
                      >
                        <div className="mt-1">
                          {event.eventType === 'RESOLVED' ? (
                            <CheckCircle2 className="h-4 w-4 text-green-500" />
                          ) : event.eventType === 'ARCHIVED' ? (
                            <Archive className="h-4 w-4 text-gray-500" />
                          ) : (
                            <AlertTriangle className="h-4 w-4 text-orange-500" />
                          )}
                        </div>

                        <div>
                          <p className="text-sm font-medium">
                            {event.eventType}
                          </p>

                          {event.comment && (
                            <p className="text-xs text-gray-500">
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
