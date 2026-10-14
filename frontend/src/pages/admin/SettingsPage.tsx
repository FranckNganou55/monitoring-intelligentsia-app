import { PageIntro, SectionHeading } from '@/components/ui/PageIntro'
import { useState, useCallback } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Mail, Clock, Shield, Bell, Globe,
  Save, RefreshCw, Eye, EyeOff,
  AlertCircle, Loader2, Info,
} from 'lucide-react'
import toast from 'react-hot-toast'
import { settingsService } from '@/services/settingsService'
import { InstitutionAlertRoutingSettings } from '@/components/admin/InstitutionAlertRoutingSettings'
import { AtmInterfaceSettings } from '@/components/admin/AtmInterfaceSettings'
import { AlertCadenceSettings } from '@/components/admin/AlertCadenceSettings'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { cn } from '@/utils/cn'
import type { AppSettingDto, SettingGroupe } from '@/types/settings'

// ─── Types ────────────────────────────────────────────────────────────────────

type TabKey = SettingGroupe
type DirtyMap = Record<string, string>

// ─── Tab definition ───────────────────────────────────────────────────────────

const TABS: { key: TabKey; label: string; icon: React.ElementType }[] = [
  { key: 'MAIL',          label: 'Email',         icon: Mail   },
  { key: 'CRON',          label: 'Crons',         icon: Clock  },
  { key: 'ALERTES',       label: 'Alertes',       icon: Bell   },
  { key: 'SECURITE',      label: 'Sécurité',      icon: Shield },
  { key: 'NOTIFICATIONS', label: 'Notifications', icon: Bell   },
  { key: 'GENERAL',       label: 'Général',       icon: Globe  },
]

// ─── Generic field renderer ───────────────────────────────────────────────────

function SettingField({
  setting, value, dirty, onChange,
}: {
  setting: AppSettingDto
  value: string
  dirty: boolean
  onChange: (val: string) => void
}) {
  const [revealed, setRevealed] = useState(false)
  const isPassword = setting.type === 'PASSWORD' || setting.sensible
  const isBoolean  = setting.type === 'BOOLEAN'
  const isInteger  = setting.type === 'INTEGER'

  return (
    <div className="grid gap-4 py-5 lg:grid-cols-[minmax(0,1fr)_18rem] border-b border-gray-100 last:border-0">
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 mb-0.5">
          <span className="text-sm font-medium text-gray-900">{setting.label}</span>
          {dirty && (
            <span className="flex items-center gap-1 text-[10px] font-semibold text-amber-600 bg-amber-50 border border-amber-200 px-1.5 py-0.5 rounded-full">
              <span className="w-1.5 h-1.5 rounded-full bg-amber-500 inline-block" />
              Non sauvegardé
            </span>
          )}
          {setting.sensible && (
            <span className="text-[10px] text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded-full">Chiffré</span>
          )}
          {setting.sensibleConfigured && (
            <span className="text-[10px] text-green-700 bg-green-50 px-1.5 py-0.5 rounded-full">Secret enregistré</span>
          )}
        </div>
        {setting.description && (
          <p className="text-xs text-gray-500 mb-2">{setting.description}</p>
        )}
        <code className="text-[10px] text-gray-400 font-mono">{setting.cle}</code>
      </div>

      <div className="w-full">
        {isBoolean ? (
          <button
            type="button"
            onClick={() => onChange(value === 'true' ? 'false' : 'true')}
            className={cn(
              'relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none',
              value === 'true' ? 'bg-gold-500' : 'bg-gray-200',
            )}
          >
            <span className={cn(
              'inline-block h-4 w-4 rounded-full bg-white shadow transform transition-transform',
              value === 'true' ? 'translate-x-6' : 'translate-x-1',
            )} />
          </button>
        ) : isPassword ? (
          <div className="relative">
            <input
              type={revealed ? 'text' : 'password'}
              value={value}
              onChange={e => onChange(e.target.value)}
              placeholder={setting.sensible && !value
                ? setting.sensibleConfigured ? 'Enregistré — vide pour conserver' : 'Saisir pour configurer'
                : ''}
              className={cn(
                'w-full pr-9 pl-3 py-2 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-gold-500/30',
                dirty ? 'border-amber-300 bg-amber-50' : 'border-gray-200',
              )}
            />
            <button
              type="button"
              onClick={() => setRevealed(r => !r)}
              className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            >
              {revealed ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
          </div>
        ) : isInteger ? (
          <input
            type="number"
            value={value}
            onChange={e => onChange(e.target.value)}
            className={cn(
              'w-full px-3 py-2 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-gold-500/30',
              dirty ? 'border-amber-300 bg-amber-50' : 'border-gray-200',
            )}
          />
        ) : (
          <input
            type={setting.type === 'EMAIL' ? 'email' : 'text'}
            value={value}
            onChange={e => onChange(e.target.value)}
            className={cn(
              'w-full px-3 py-2 text-sm border rounded-lg focus:outline-none focus:ring-2 focus:ring-gold-500/30',
              dirty ? 'border-amber-300 bg-amber-50' : 'border-gray-200',
            )}
          />
        )}
      </div>
    </div>
  )
}

// ─── Settings tab panel ───────────────────────────────────────────────────────

function SettingsTab({ groupe }: { groupe: TabKey }) {
  const qc = useQueryClient()
  const [dirty, setDirty] = useState<DirtyMap>({})
  const [values, setValues] = useState<Record<string, string>>({})
  const [initialized, setInitialized] = useState(false)

  const { data: settings, isLoading, isError, error } = useQuery({
    queryKey: ['settings', groupe],
    queryFn:  () => settingsService.getByGroupe(groupe),
    staleTime: 30_000,
    retry: false,
  })

  // Initialize local values once data arrives
  if (settings && !initialized) {
    const initial: Record<string, string> = {}
    settings.forEach(s => { initial[s.cle] = s.valeur ?? '' })
    setValues(initial)
    setInitialized(true)
  }

  const saveMutation = useMutation({
    mutationFn: async (dirtyEntries: DirtyMap) => {
      await Promise.all(
        Object.entries(dirtyEntries).map(([cle, val]) => settingsService.update(cle, val))
      )
    },
    onSuccess: () => {
      toast.success('Paramètres enregistrés.')
      setDirty({})
      qc.invalidateQueries({ queryKey: ['settings', groupe] })
      setInitialized(false)
    },
    onError: () => toast.error('Erreur lors de la sauvegarde.'),
  })

  const handleChange = useCallback((cle: string, val: string) => {
    setValues(v => ({ ...v, [cle]: val }))
    setDirty(d => ({ ...d, [cle]: val }))
  }, [])

  const dirtyCount = Object.keys(dirty).length

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-16 text-gray-400">
        <Loader2 className="w-5 h-5 animate-spin mr-2" /> Chargement…
      </div>
    )
  }

  if (isError) {
    const status = (error as any)?.response?.status
    const msg = status === 403
      ? 'Accès refusé (403) — vérifiez les droits du compte.'
      : status === 401
      ? 'Non authentifié (401) — reconnectez-vous.'
      : `Erreur ${status ?? ''} — impossible de charger les paramètres.`
    return (
      <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
        <AlertCircle className="w-4 h-4 shrink-0" />
        {msg}
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {dirtyCount > 0 && (
        <div className="flex items-center justify-between bg-amber-50 border border-amber-200 rounded-lg px-4 py-3">
          <span className="text-sm text-amber-700">
            <Info className="w-4 h-4 inline mr-1.5" />
            {dirtyCount} modification{dirtyCount > 1 ? 's' : ''} non sauvegardée{dirtyCount > 1 ? 's' : ''}
          </span>
          <div className="flex gap-2">
            <Button
              variant="ghost" size="sm"
              onClick={() => { setDirty({}); setInitialized(false) }}
            >
              Annuler
            </Button>
            <Button
              size="sm"
              icon={<Save className="w-4 h-4" />}
              loading={saveMutation.isPending}
              onClick={() => saveMutation.mutate(dirty)}
            >
              Enregistrer les modifications
            </Button>
          </div>
        </div>
      )}

      <Card>
        <div className="px-6">
          {settings?.filter(s => !s.cle.startsWith('alerts.fraud.interval.') && !s.cle.startsWith('alerts.sound.interval.') && !s.cle.startsWith('alerts.atm.interface.')).map(s => (
            <SettingField
              key={s.cle}
              setting={s}
              value={values[s.cle] ?? ''}
              dirty={s.cle in dirty}
              onChange={val => handleChange(s.cle, val)}
            />
          ))}
        </div>
      </Card>
    </div>
  )
}

function AlertsTab() {
  return (
    <div className="space-y-4">
      <SettingsTab groupe="ALERTES" />
      <AtmInterfaceSettings />
      <AlertCadenceSettings />
    </div>
  )
}

// ─── Mail settings ────────────────────────────────────────────────────────────

function MailTab() {
  return (
    <div className="space-y-4">
      <SettingsTab groupe="MAIL" />
      <InstitutionAlertRoutingSettings />
    </div>
  )
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export function SettingsPage() {
  const [activeTab, setActiveTab] = useState<TabKey>('MAIL')
  const qc = useQueryClient()

  const reloadMutation = useMutation({
    mutationFn: () => settingsService.reload(),
    onSuccess:  () => {
      toast.success('Cache rechargé depuis la base de données.')
      TABS.forEach(t => qc.invalidateQueries({ queryKey: ['settings', t.key] }))
    },
  })

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="page-header flex flex-wrap items-start justify-between gap-4">
        <PageIntro icon={Globe} eyebrow="Administration" title="Paramètres de la plateforme" description="Configurez les services, les alertes et la sécurité de l’application." />
        <Button
          variant="secondary"
          icon={<RefreshCw className={cn('w-4 h-4', reloadMutation.isPending && 'animate-spin')} />}
          loading={reloadMutation.isPending}
          onClick={() => reloadMutation.mutate()}
        >
          Recharger le cache
        </Button>
      </div>

      {/* Tab navigation */}
      <div className="grid items-start gap-5 lg:grid-cols-[220px_minmax(0,1fr)]">
      <nav aria-label="Catégories des paramètres" className="panel grid gap-1 p-3 sm:grid-cols-3 lg:grid-cols-1">
        <div className="px-3 py-2 sm:col-span-3 lg:col-span-1"><SectionHeading title="Configuration" /></div>
        {TABS.map(tab => {
          const Icon = tab.icon
          return (
            <button
              key={tab.key}
              aria-pressed={activeTab === tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={cn(
                'flex items-center gap-3 rounded-xl border px-3 py-3 text-left text-sm font-medium transition-colors',
                activeTab === tab.key
                  ? 'border-gold-200 bg-gold-50 text-gold-700'
                  : 'border-transparent text-gray-500 hover:bg-gray-50 hover:text-gray-900',
              )}
            >
              <Icon className="w-4 h-4" />
              {tab.label}
            </button>
          )
        })}
      </nav>

      {/* Tab content */}
      <div className="min-w-0 space-y-4">
        <SectionHeading title={TABS.find(tab => tab.key === activeTab)?.label ?? 'Configuration'} description={{ MAIL: 'Serveur d’envoi et destinataires des alertes par email.', CRON: 'Planification des tâches et des synchronisations automatiques.', ALERTES: 'Cadence et comportement des alertes de surveillance.', SECURITE: 'Paramètres de protection et d’accès à la plateforme.', NOTIFICATIONS: 'Configuration des notifications de l’application.', GENERAL: 'Réglages généraux de la plateforme.' }[activeTab]} />
        {activeTab === 'MAIL'          && <MailTab />}
        {activeTab === 'CRON'          && <SettingsTab groupe="CRON" />}
        {activeTab === 'ALERTES'       && <AlertsTab />}
        {activeTab === 'SECURITE'      && <SettingsTab groupe="SECURITE" />}
        {activeTab === 'NOTIFICATIONS' && <SettingsTab groupe="NOTIFICATIONS" />}
        {activeTab === 'GENERAL'       && <SettingsTab groupe="GENERAL" />}
      </div>
      </div>
    </div>
  )
}
