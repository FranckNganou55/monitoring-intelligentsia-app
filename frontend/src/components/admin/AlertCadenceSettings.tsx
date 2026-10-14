import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { BellRing, CheckCircle2, Clock3, RefreshCw, Save } from 'lucide-react'
import toast from 'react-hot-toast'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { settingsService } from '@/services/settingsService'

type RiskKey = 'TYPIQUE' | 'MOINS_CRITIQUE' | 'MOYEN'
type RiskSetting = { key: string; risk: RiskKey; label: string; description: string }
type SoundRiskKey = RiskKey | 'CRITIQUE'

const riskSettings: RiskSetting[] = [
  { key: 'alerts.fraud.interval.TYPIQUE', risk: 'TYPIQUE', label: 'Typique', description: 'Délai avant l’envoi de la notification.' },
  { key: 'alerts.fraud.interval.MOINS_CRITIQUE', risk: 'MOINS_CRITIQUE', label: 'Moins critique', description: 'Délai avant l’envoi de la notification.' },
  { key: 'alerts.fraud.interval.MOYEN', risk: 'MOYEN', label: 'Moyen', description: 'Délai avant l’envoi de la notification.' },
]
const soundRiskSettings: { key: string; risk: SoundRiskKey; label: string }[] = [
  { key: 'alerts.sound.interval.TYPIQUE', risk: 'TYPIQUE', label: 'Typique' },
  { key: 'alerts.sound.interval.MOINS_CRITIQUE', risk: 'MOINS_CRITIQUE', label: 'Moins critique' },
  { key: 'alerts.sound.interval.MOYEN', risk: 'MOYEN', label: 'Moyen' },
  { key: 'alerts.sound.interval.CRITIQUE', risk: 'CRITIQUE', label: 'Critique' },
]

const intervals = [
  { value: 5_000, label: 'Temps réel (cycle de 5 secondes)' },
  { value: 60_000, label: 'Chaque minute' },
  { value: 300_000, label: 'Toutes les 5 minutes' },
  { value: 600_000, label: 'Toutes les 10 minutes' },
  { value: 900_000, label: 'Toutes les 15 minutes' },
  { value: 1_800_000, label: 'Toutes les 30 minutes' },
  { value: 3_600_000, label: 'Toutes les heures' },
]
const soundDurations = [
  { value: 5_000, label: '5 secondes' },
  { value: 10_000, label: '10 secondes' },
  { value: 15_000, label: '15 secondes' },
  { value: 30_000, label: '30 secondes' },
  { value: 60_000, label: '1 minute' },
  { value: 120_000, label: '2 minutes' },
  { value: 300_000, label: '5 minutes' },
]

export function AlertCadenceSettings() {
  const queryClient = useQueryClient()
  const [draft, setDraft] = useState<Record<RiskKey, number>>({ TYPIQUE: 600_000, MOINS_CRITIQUE: 300_000, MOYEN: 60_000 })
  const [soundDraft, setSoundDraft] = useState<Record<SoundRiskKey, number>>({ TYPIQUE: 15_000, MOINS_CRITIQUE: 15_000, MOYEN: 30_000, CRITIQUE: 30_000 })

  const settingsQuery = useQuery({
    queryKey: ['settings', 'ALERTES'],
    queryFn: () => settingsService.getByGroupe('ALERTES'),
    retry: false,
  })


  useEffect(() => {
    const values = settingsQuery.data
    if (!values) return
    setDraft((current) => {
      const next = { ...current }
      riskSettings.forEach(({ key, risk }) => {
        const configured = Number(values.find((setting) => setting.cle === key)?.valeur)
        if (Number.isFinite(configured) && configured > 0) next[risk] = configured
      })
      return next
    })
    setSoundDraft((current) => {
      const next = { ...current }
      soundRiskSettings.forEach(({ key, risk }) => {
        const configured = Number(values.find((setting) => setting.cle === key)?.valeur)
        if (Number.isFinite(configured) && configured > 0) next[risk] = configured
      })
      return next
    })
  }, [settingsQuery.data])

  const persisted = useMemo(() => {
    const values = settingsQuery.data ?? []
    return Object.fromEntries(riskSettings.map(({ key, risk }) => {
      const value = Number(values.find((setting) => setting.cle === key)?.valeur)
      const fallback = risk === 'TYPIQUE' ? 600_000 : risk === 'MOINS_CRITIQUE' ? 300_000 : 60_000
      return [risk, Number.isFinite(value) && value > 0 ? value : fallback]
    })) as Record<RiskKey, number>
  }, [settingsQuery.data])
  const dirty = riskSettings.some(({ risk }) => draft[risk] !== persisted[risk])
  const persistedSound = useMemo(() => {
    const values = settingsQuery.data ?? []
    const defaults: Record<SoundRiskKey, number> = { TYPIQUE: 15_000, MOINS_CRITIQUE: 15_000, MOYEN: 30_000, CRITIQUE: 30_000 }
    return Object.fromEntries(soundRiskSettings.map(({ key, risk }) => {
      const value = Number(values.find((setting) => setting.cle === key)?.valeur)
      return [risk, soundDurations.some((option) => option.value === value) ? value : defaults[risk]]
    })) as Record<SoundRiskKey, number>
  }, [settingsQuery.data])
  const soundDirty = soundRiskSettings.some(({ risk }) => soundDraft[risk] !== persistedSound[risk])

  const saveMutation = useMutation({
    mutationFn: (values: Record<RiskKey, number>) => settingsService.updateFraudAlertCadence({
      typiqueMs: values.TYPIQUE,
      moinsCritiqueMs: values.MOINS_CRITIQUE,
      moyenMs: values.MOYEN,
    }),
    onSuccess: () => {
      toast.success('Fréquences enregistrées et appliquées.')
      void queryClient.invalidateQueries({ queryKey: ['settings', 'ALERTES'] })
      void queryClient.invalidateQueries({ queryKey: ['fraud-alert-cadence'] })
    },
    onError: () => toast.error('Impossible d’enregistrer les fréquences des alertes.'),
  })

  const saveSoundMutation = useMutation({
    mutationFn: (values: Record<SoundRiskKey, number>) => Promise.all(soundRiskSettings.map(({ key, risk }) => settingsService.update(key, String(values[risk])))),
    onSuccess: () => {
      toast.success('Fréquences sonores enregistrées et appliquées.')
      void queryClient.invalidateQueries({ queryKey: ['settings', 'ALERTES'] })
      void queryClient.invalidateQueries({ queryKey: ['alert-sound-cadence'] })
    },
    onError: () => toast.error('Impossible d’enregistrer les fréquences sonores.'),
  })


  return (
    <Card>
      <div className="space-y-5 px-6 py-5">
        <div className="flex items-start justify-between gap-3">
          <div>
            <h2 className="flex items-center gap-2 text-sm font-semibold text-gray-900">
              <BellRing className="h-4 w-4 text-gold-600" /> Délai avant signalement des alertes
            </h2>
            <p className="mt-1 max-w-3xl text-xs text-gray-500">
              Les alertes sont enregistrées et visibles immédiatement. Ce délai règle quand l’alerte déclenche sa notification plein écran et son signal sonore.
            </p>
          </div>
          <Button variant="secondary" size="sm" icon={<RefreshCw className="h-4 w-4" />}
            loading={settingsQuery.isFetching}
            onClick={() => { void settingsQuery.refetch() }}>
            Actualiser
          </Button>
        </div>

        {settingsQuery.isError ? <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">Impossible de charger les fréquences configurées.</p> : null}

        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          {riskSettings.map(({ risk, label, description }) => (
            <label key={risk} className="block rounded-xl border border-gray-200 bg-white p-4 text-sm font-medium text-gray-800">
              <span className="flex items-center gap-2">
                <Clock3 className="h-4 w-4 text-gold-600" /> {label}
              </span>
              <select
                value={draft[risk]}
                disabled={settingsQuery.isLoading || saveMutation.isPending}
                onChange={(event) => setDraft((current) => ({ ...current, [risk]: Number(event.target.value) }))}
                className="mt-3 w-full rounded-lg border border-gray-200 bg-white px-3 py-2.5 text-sm font-normal text-gray-900 disabled:opacity-60"
              >
                {intervals.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
              </select>
              <span className="mt-2 block text-xs font-normal text-gray-500">{description}</span>
            </label>
          ))}
          <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-gray-800">
            <span className="flex items-center gap-2 font-medium text-red-800">
              <CheckCircle2 className="h-4 w-4" /> Critique
            </span>
            <div className="mt-3 rounded-lg border border-red-100 bg-white px-3 py-2.5 font-semibold text-red-700">
              Temps réel — prochain cycle (5 secondes maximum)
            </div>
            <span className="mt-2 block text-xs text-red-700">Ce niveau est toujours envoyé immédiatement et ne peut pas être ralenti.</span>
          </div>
        </div>

        {dirty ? (
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3">
            <span className="text-sm text-amber-800">Des fréquences fraude ne sont pas encore validées.</span>
            <div className="flex gap-2">
              <Button variant="ghost" size="sm" disabled={saveMutation.isPending}
                onClick={() => setDraft(persisted)}>
                Annuler
              </Button>
              <Button size="sm" icon={<Save className="h-4 w-4" />} loading={saveMutation.isPending}
                onClick={() => saveMutation.mutate(draft)}>
                Valider et appliquer
              </Button>
            </div>
          </div>
        ) : null}

        <div className="rounded-xl border border-gray-200 bg-gray-50/70 p-4">
          <div>
            <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-900">
              <BellRing className="h-4 w-4 text-gold-600" /> Durée du signal sonore par niveau
            </h3>
            <p className="mt-1 text-xs text-gray-500">Après le délai de signalement, le son joue en continu pendant la durée choisie. L’écran reste affiché jusqu’à la prise en compte de l’alerte.</p>
          </div>
          <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            {soundRiskSettings.map(({ risk, label }) => (
              <label key={risk} className="block text-sm font-medium text-gray-800">
                {label}
                <select
                  value={soundDraft[risk]}
                  disabled={settingsQuery.isLoading || saveSoundMutation.isPending}
                  onChange={(event) => setSoundDraft((current) => ({ ...current, [risk]: Number(event.target.value) }))}
                  className="mt-1.5 w-full rounded-lg border border-gray-200 bg-white px-3 py-2.5 text-sm font-normal text-gray-900 disabled:opacity-60"
                >
                  {soundDurations.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
                </select>
              </label>
            ))}
          </div>
          {soundDirty ? (
            <div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3">
              <span className="text-sm text-amber-800">Les durées sonores ne sont pas encore validées.</span>
              <div className="flex gap-2">
                <Button variant="ghost" size="sm" disabled={saveSoundMutation.isPending} onClick={() => setSoundDraft(persistedSound)}>Annuler</Button>
                <Button size="sm" icon={<Save className="h-4 w-4" />} loading={saveSoundMutation.isPending} onClick={() => saveSoundMutation.mutate(soundDraft)}>Valider et appliquer</Button>
              </div>
            </div>
          ) : null}
        </div>


      </div>
    </Card>
  )
}

