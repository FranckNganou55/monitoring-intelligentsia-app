import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangle, CheckCircle2 } from 'lucide-react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { alertService } from '@/services/alertService'
import { notificationService } from '@/services/notificationService'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { startAlertSoundForDuration, stopAlertSound, unlockAlertSound, type AlertSoundLevel } from '@/utils/alertSound'
import { settingsService } from '@/services/settingsService'

const themes = {
  CRITIQUE: { outer: 'bg-red-700', border: 'border-red-300', icon: 'bg-red-100 text-red-700', eyebrow: 'text-red-700', title: 'text-red-800', panel: 'bg-red-50', button: 'bg-red-700 hover:bg-red-800' },
  MOYEN: { outer: 'bg-orange-600', border: 'border-orange-200', icon: 'bg-orange-100 text-orange-700', eyebrow: 'text-orange-700', title: 'text-orange-800', panel: 'bg-orange-50', button: 'bg-orange-600 hover:bg-orange-700' },
  MOINS_CRITIQUE: { outer: 'bg-yellow-500', border: 'border-yellow-200', icon: 'bg-yellow-100 text-yellow-700', eyebrow: 'text-yellow-700', title: 'text-yellow-800', panel: 'bg-yellow-50', button: 'bg-yellow-600 hover:bg-yellow-700' },
  TYPIQUE: { outer: 'bg-blue-700', border: 'border-blue-200', icon: 'bg-blue-100 text-blue-700', eyebrow: 'text-blue-700', title: 'text-blue-800', panel: 'bg-blue-50', button: 'bg-blue-700 hover:bg-blue-800' },
} as const

const riskPriority: Record<AlertSoundLevel, number> = {
  CRITIQUE: 4,
  MOYEN: 3,
  MOINS_CRITIQUE: 2,
  TYPIQUE: 1,
}

const flashClass: Record<AlertSoundLevel, string> = {
  CRITIQUE: 'alert-flash-critical',
  MOYEN: 'alert-flash-medium',
  MOINS_CRITIQUE: 'alert-flash-low',
  TYPIQUE: 'alert-flash-typical',
}

export function CriticalAlertOverlay() {
  const dialogRef = useRef<HTMLDivElement>(null)
  const [soundBlocked, setSoundBlocked] = useState(false)
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const institutionId = useInstitutionFilter()
  const queryKey = ['global-open-risk-alerts', institutionId]
  const soundCadenceQuery = useQuery({
    queryKey: ['alert-sound-cadence'],
    queryFn: settingsService.getAlertSoundCadence,
    staleTime: 30_000,
    refetchOnWindowFocus: true,
  })
  const signaledAlertIdsQuery = useQuery({
    queryKey: ['signaled-open-alert-ids', institutionId],
    queryFn: () => notificationService.openAlertIds(institutionId),
    refetchInterval: 5_000,
    refetchOnWindowFocus: true,
  })
  const alertsQuery = useQuery({
    queryKey,
    queryFn: () => alertService.list({
      status: 'OPEN',
      institutionId,
      page: 0,
      size: 100,
    }),
    refetchInterval: 5_000,
    refetchOnWindowFocus: true,
  })
  const acknowledgeMutation = useMutation({
    mutationFn: (alertId: number) => alertService.updateStatus(alertId, {
      status: 'ACKNOWLEDGED',
      comment: 'Prise en compte depuis le signal critique plein écran.',
    }),
    onSuccess: async (updated) => {
      queryClient.setQueryData(queryKey, (current: Awaited<ReturnType<typeof alertService.list>> | undefined) => {
        if (!current) return current
        return {
          ...current,
          content: current.content.filter((alert) => alert.id !== updated.id),
          totalElements: Math.max(0, current.totalElements - 1),
        }
      })
      await Promise.all([
        queryClient.invalidateQueries({ queryKey }),
        queryClient.invalidateQueries({ queryKey: ['monitoring-alerts'] }),
        queryClient.invalidateQueries({ queryKey: ['monitoring-alert', updated.id] }),
        queryClient.invalidateQueries({ queryKey: ['monitoring-alert-events', updated.id] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard-alerts'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      ])
      navigate(`/monitoring/alerts?alertId=${updated.id}`, { state: { alertOverlayDetailsId: updated.id } })
    },
  })

  const signaledAlertIds = new Set(signaledAlertIdsQuery.data ?? [])
  const alerts = (alertsQuery.data?.content ?? [])
    .filter((alert) => signaledAlertIds.has(alert.id))
    .sort((left, right) => {
      const priorityDifference = (riskPriority[right.riskLevel as AlertSoundLevel] ?? 0)
        - (riskPriority[left.riskLevel as AlertSoundLevel] ?? 0)
      return priorityDifference || new Date(left.detectedAt).getTime() - new Date(right.detectedAt).getTime()
    })
  const detailAlertId = Number(searchParams.get('alertId'))
  const overlayDetailsId = (location.state as { alertOverlayDetailsId?: number } | null)
    ?.alertOverlayDetailsId
  // Only an intentional click on “Ouvrir le détail” suppresses the overlay.
  // Automatic SSE navigation still leaves the blocking signal visible.
  const isShowingAlertDetails = location.pathname === '/monitoring/alerts'
    && Number.isInteger(detailAlertId)
    && detailAlertId > 0
    && overlayDetailsId === detailAlertId
  const activeAlert = alerts[0]
  const soundFallback: Record<AlertSoundLevel, number> = { TYPIQUE: 15_000, MOINS_CRITIQUE: 15_000, MOYEN: 30_000, CRITIQUE: 30_000 }
  const activeSoundLevel = activeAlert?.riskLevel as AlertSoundLevel | undefined
  const activeSoundDuration = activeSoundLevel
    ? soundCadenceQuery.data?.[activeSoundLevel] ?? soundFallback[activeSoundLevel]
    : 0
  const shouldShowBlockingOverlay = Boolean(activeAlert)
    // Keep the sound-unlock prompt available even when the user opened this
    // alert's detail before the browser allowed audio playback.
    && (!(isShowingAlertDetails && activeAlert?.id === detailAlertId) || soundBlocked)

  useEffect(() => {
    if (activeAlert && shouldShowBlockingOverlay) dialogRef.current?.focus()
  }, [activeAlert?.id, shouldShowBlockingOverlay])

  useEffect(() => {
    const unlock = () => {
      void unlockAlertSound().then((started) => {
        if (started) setSoundBlocked(false)
      })
    }
    // Capture the gesture before page controls or dialogs can stop bubbling.
    // This is required by browsers that block Web Audio until user activation.
    window.addEventListener('pointerdown', unlock, true)
    window.addEventListener('keydown', unlock, true)
    return () => {
      window.removeEventListener('pointerdown', unlock, true)
      window.removeEventListener('keydown', unlock, true)
    }
  }, [])

  useEffect(() => {
    const refreshSignaledAlerts = (event: Event) => {
      const alertId = (event as CustomEvent<{ alertId?: number }>).detail?.alertId
      if (alertId != null) {
        queryClient.setQueryData<number[]>(['signaled-open-alert-ids', institutionId], (current = []) =>
          current.includes(alertId) ? current : [...current, alertId],
        )
      }
      void queryClient.invalidateQueries({ queryKey: ['global-open-risk-alerts', institutionId] })
    }
    window.addEventListener('monitoring:urgent-alert', refreshSignaledAlerts)
    return () => window.removeEventListener('monitoring:urgent-alert', refreshSignaledAlerts)
  }, [queryClient, institutionId])

  useEffect(() => {
    if (!activeAlert) {
      stopAlertSound()
      setSoundBlocked(false)
      return
    }
    const risk = activeAlert.riskLevel as AlertSoundLevel
    let current = true
    void startAlertSoundForDuration(risk, activeSoundDuration)
      .then((started) => { if (current) setSoundBlocked(!started) })
    return () => { current = false; stopAlertSound() }
  }, [activeAlert?.id, activeAlert?.riskLevel, activeSoundDuration])

  if (!activeAlert || !shouldShowBlockingOverlay) return null
  const risk = activeAlert.riskLevel as keyof typeof themes
  const theme = themes[risk] ?? themes.CRITIQUE

  return (
    <div
      ref={dialogRef}
      tabIndex={-1}
      className={`fixed inset-0 z-[9999] flex items-center justify-center overflow-y-auto ${theme.outer} ${flashClass[risk as AlertSoundLevel] ?? flashClass.CRITIQUE} p-4 text-white`}
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="critical-alert-title"
      aria-describedby="critical-alert-description"
      onKeyDown={(event) => {
        if (event.key === 'Escape') {
          event.preventDefault()
          event.stopPropagation()
          return
        }
        if (event.key !== 'Tab') return
        const focusable = dialogRef.current?.querySelectorAll<HTMLElement>('a[href], button:not([disabled])')
        if (!focusable?.length) return
        const first = focusable[0]
        const last = focusable[focusable.length - 1]
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault()
          last.focus()
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault()
          first.focus()
        }
      }}
    >
      <div className={`w-full max-w-2xl rounded-2xl border-4 ${risk === 'CRITIQUE' ? 'border-red-950' : theme.border} bg-white p-6 text-gray-900 shadow-2xl sm:p-10`}>
        {risk === 'CRITIQUE' && (
          <div className="-mx-6 -mt-6 mb-5 rounded-t-xl bg-red-700 px-6 py-3 text-center text-sm font-black tracking-[0.2em] text-white sm:-mx-10 sm:-mt-10 sm:px-10">
            RED ALERT · ALERTE CRITIQUE
          </div>
        )}
        <div className={`flex items-center gap-4 border-b ${theme.border} pb-5`}>
          <div className={`flex h-14 w-14 shrink-0 items-center justify-center rounded-full ${theme.icon}`}>
            <AlertTriangle className="h-8 w-8" aria-hidden="true" />
          </div>
          <div>
            <p className={`text-sm font-bold uppercase tracking-[0.18em] ${theme.eyebrow}`}>Signal de sécurité</p>
            <h1 id="critical-alert-title" className={`text-2xl font-black ${theme.title} sm:text-3xl`}>
              ALERTE {activeAlert.riskLevel.replace('_', ' ')}
            </h1>
          </div>
          {alerts.length > 1 && (
            <span className={`ml-auto rounded-full px-3 py-1 text-sm font-bold ${theme.icon}`}>
              {alerts.length} à prendre en compte
            </span>
          )}
        </div>

        <div className="space-y-4 py-6">
          <p className="text-xl font-semibold">{activeAlert.title}</p>
          <p id="critical-alert-description" className="whitespace-pre-wrap text-sm leading-6 text-gray-600">
            {activeAlert.description || 'Une règle de surveillance a signalé un événement critique.'}
          </p>
          <dl className={`grid grid-cols-1 gap-3 rounded-xl ${theme.panel} p-4 text-sm sm:grid-cols-2`}>
            <div><dt className="text-gray-500">Règle</dt><dd className="font-semibold text-gray-900">{activeAlert.ruleCode} — {activeAlert.ruleName}</dd></div>
            <div><dt className="text-gray-500">Catégorie</dt><dd className="font-semibold text-gray-900">{activeAlert.category}</dd></div>
            <div><dt className="text-gray-500">Institution</dt><dd className="font-semibold text-gray-900">{activeAlert.institutionId ?? 'Non renseignée'}</dd></div>
            <div><dt className="text-gray-500">Détection</dt><dd className="font-semibold text-gray-900">{new Date(activeAlert.detectedAt).toLocaleString('fr-FR')}</dd></div>
          </dl>
          {acknowledgeMutation.isError && (
            <p className="rounded-lg bg-red-100 px-3 py-2 text-sm font-medium text-red-800" role="status">
              L’alerte n’a pas pu être prise en compte. Réessayez.
            </p>
          )}
          {soundBlocked && (
            <div className={`rounded-lg ${theme.panel} p-3`} role="status" aria-live="assertive">
              <p className={`mb-2 text-sm ${theme.title}`}>
                Le son n’a pas démarré. Cliquez pour l’activer et vérifiez que le son de cet onglet n’est pas coupé.
              </p>
              <button
                type="button"
                onClick={() => {
                  void unlockAlertSound()
                    .then((started) => setSoundBlocked(!started))
                }}
                className={`rounded-lg border-2 ${theme.border} px-4 py-3 text-sm font-bold ${theme.title}`}
              >
                Activer le son de l’alerte
              </button>
            </div>
          )}
        </div>

        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-between">
          <button
            type="button"
            onClick={() => navigate(`/monitoring/alerts?alertId=${activeAlert.id}`, { state: { alertOverlayDetailsId: activeAlert.id } })}
            className="inline-flex items-center justify-center rounded-lg border border-gray-300 px-4 py-3 text-sm font-semibold text-gray-700 hover:bg-gray-50"
          >
            Ouvrir le détail
          </button>
          <button
            type="button"
            onClick={() => acknowledgeMutation.mutate(activeAlert.id)}
            disabled={acknowledgeMutation.isPending}
            className={`inline-flex items-center justify-center gap-2 rounded-lg px-5 py-3 text-sm font-bold text-white shadow disabled:cursor-wait disabled:opacity-60 ${theme.button}`}
          >
            <CheckCircle2 className="h-5 w-5" aria-hidden="true" />
            {acknowledgeMutation.isPending ? 'Prise en compte…' : 'Prendre en compte l’alerte'}
          </button>
        </div>
      </div>
    </div>
  )
}
