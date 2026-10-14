import { useState, useEffect, useRef, useMemo } from 'react'
import { Clock, Calendar, Info } from 'lucide-react'
import { cn } from '@/utils/cn'

// ─── Types ────────────────────────────────────────────────────────────────────

type CronMode = 'off' | 'daily' | 'workdays' | 'weekly' | 'monthly' | 'custom'

interface Props {
  value?: string
  onChange: (value: string | undefined) => void
  label?: string
  error?: string
}

// ─── Data ─────────────────────────────────────────────────────────────────────

const MODES: { key: CronMode; label: string }[] = [
  { key: 'off',      label: 'Désactivé'    },
  { key: 'daily',    label: 'Quotidien'    },
  { key: 'workdays', label: 'Jours ouvrés' },
  { key: 'weekly',   label: 'Hebdomadaire' },
  { key: 'monthly',  label: 'Mensuel'      },
  { key: 'custom',   label: 'Avancé'       },
]

const DAYS_FR = [
  { value: 'MON', label: 'Lundi'    },
  { value: 'TUE', label: 'Mardi'    },
  { value: 'WED', label: 'Mercredi' },
  { value: 'THU', label: 'Jeudi'    },
  { value: 'FRI', label: 'Vendredi' },
  { value: 'SAT', label: 'Samedi'   },
  { value: 'SUN', label: 'Dimanche' },
]

const HOURS = Array.from({ length: 24 }, (_, i) => ({
  value: String(i),
  label: `${String(i).padStart(2, '0')}h`,
}))

const MINUTES = ['00', '05', '10', '15', '20', '25', '30', '35', '40', '45', '50', '55'].map(m => ({
  value: m,
  label: m,
}))

const DAYS_OF_MONTH = Array.from({ length: 28 }, (_, i) => ({
  value: String(i + 1),
  label: String(i + 1),
}))

// ─── Helpers ──────────────────────────────────────────────────────────────────

function detectMode(cron?: string): CronMode {
  if (!cron) return 'off'
  if (/^0 \d+ \d+ \* \* MON-FRI$/.test(cron)) return 'workdays'
  if (/^0 \d+ \d+ \* \* \*$/.test(cron))        return 'daily'
  if (/^0 \d+ \d+ \* \* (MON|TUE|WED|THU|FRI|SAT|SUN)$/.test(cron)) return 'weekly'
  if (/^0 \d+ \d+ \d+ \* \*$/.test(cron))        return 'monthly'
  return 'custom'
}

function parseInitial(cron?: string) {
  const defaults = { hour: '8', minute: '00', day: 'MON', dayOfMonth: '1' }
  if (!cron) return defaults
  const parts = cron.split(' ')
  if (parts.length < 6) return defaults
  return {
    hour:       parts[2] ?? '8',
    minute:     String(parseInt(parts[1] ?? '0')).padStart(2, '0'),
    day:        /^(MON|TUE|WED|THU|FRI|SAT|SUN)$/.test(parts[5]) ? parts[5] : 'MON',
    dayOfMonth: parts[3] !== '*' ? parts[3] : '1',
  }
}

function buildCron(mode: CronMode, hour: string, minute: string, day: string, dayOfMonth: string): string | undefined {
  const h = hour || '8'
  const m = parseInt(minute || '0').toString()
  switch (mode) {
    case 'off':      return undefined
    case 'daily':    return `0 ${m} ${h} * * *`
    case 'workdays': return `0 ${m} ${h} * * MON-FRI`
    case 'weekly':   return `0 ${m} ${h} * * ${day || 'MON'}`
    case 'monthly':  return `0 ${m} ${h} ${dayOfMonth || '1'} * *`
    default:         return undefined
  }
}

function humanReadable(cron: string | undefined): string {
  if (!cron) return 'Aucune planification — déclenchement manuel uniquement'

  const daily    = cron.match(/^0 (\d+) (\d+) \* \* \*$/)
  if (daily)    return `Tous les jours à ${daily[2].padStart(2,'0')}h${daily[1].padStart(2,'0')}`

  const wd       = cron.match(/^0 (\d+) (\d+) \* \* MON-FRI$/)
  if (wd)       return `Du lundi au vendredi à ${wd[2].padStart(2,'0')}h${wd[1].padStart(2,'0')}`

  const weekly   = cron.match(/^0 (\d+) (\d+) \* \* (MON|TUE|WED|THU|FRI|SAT|SUN)$/)
  if (weekly) {
    const dayMap: Record<string, string> = {
      MON: 'lundi', TUE: 'mardi', WED: 'mercredi',
      THU: 'jeudi', FRI: 'vendredi', SAT: 'samedi', SUN: 'dimanche',
    }
    return `Chaque ${dayMap[weekly[3]]} à ${weekly[2].padStart(2,'0')}h${weekly[1].padStart(2,'0')}`
  }

  const monthly  = cron.match(/^0 (\d+) (\d+) (\d+) \* \*$/)
  if (monthly)  return `Le ${monthly[3]} de chaque mois à ${monthly[2].padStart(2,'0')}h${monthly[1].padStart(2,'0')}`

  return `Expression personnalisée : ${cron}`
}

// ─── Select helper ────────────────────────────────────────────────────────────

function CronSelect({
  value, onChange, options,
}: { value: string; onChange: (v: string) => void; options: { value: string; label: string }[] }) {
  return (
    <select
      value={value}
      onChange={e => onChange(e.target.value)}
      className="border border-gray-200 rounded-lg px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-gold-500/30 focus:border-gold-400"
    >
      {options.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
    </select>
  )
}

// ─── Component ────────────────────────────────────────────────────────────────

export function CronPickerField({ value, onChange, label = 'Planification', error }: Props) {
  const parsed    = useMemo(() => parseInitial(value), [])
  const [mode,       setMode]       = useState<CronMode>(() => detectMode(value))
  const [hour,       setHour]       = useState(parsed.hour)
  const [minute,     setMinute]     = useState(parsed.minute)
  const [day,        setDay]        = useState(parsed.day)
  const [dayOfMonth, setDayOfMonth] = useState(parsed.dayOfMonth)
  const [customExpr, setCustomExpr] = useState(mode === 'custom' ? (value ?? '') : '')

  const prevRef = useRef(value)

  const cronExpr = useMemo(() => {
    if (mode === 'custom') return customExpr || undefined
    return buildCron(mode, hour, minute, day, dayOfMonth)
  }, [mode, hour, minute, day, dayOfMonth, customExpr])

  useEffect(() => {
    if (cronExpr !== prevRef.current) {
      prevRef.current = cronExpr
      onChange(cronExpr)
    }
  }, [cronExpr])

  const summary = humanReadable(cronExpr)
  const isActive = mode !== 'off'

  return (
    <div className="space-y-2">
      {label && (
        <label className="block text-sm font-medium text-gray-700">{label}</label>
      )}

      {/* Mode buttons */}
      <div className="flex flex-wrap gap-1.5">
        {MODES.map(m => (
          <button
            key={m.key}
            type="button"
            onClick={() => setMode(m.key)}
            className={cn(
              'px-3 py-1.5 text-xs font-medium rounded-lg border transition-all',
              mode === m.key
                ? 'bg-gold-500 text-white border-gold-500 shadow-sm'
                : 'bg-white text-gray-600 border-gray-200 hover:border-gold-300 hover:text-gray-900',
            )}
          >
            {m.label}
          </button>
        ))}
      </div>

      {/* Contextual fields */}
      {isActive && mode !== 'custom' && (
        <div className="flex flex-wrap items-center gap-2 rounded-lg bg-gray-50 border border-gray-200 px-3 py-2.5">
          {mode === 'weekly' && (
            <>
              <span className="text-sm text-gray-500">Chaque</span>
              <CronSelect value={day} onChange={setDay} options={DAYS_FR} />
            </>
          )}
          {mode === 'monthly' && (
            <>
              <span className="text-sm text-gray-500">Le</span>
              <CronSelect value={dayOfMonth} onChange={setDayOfMonth} options={DAYS_OF_MONTH} />
              <span className="text-sm text-gray-500">du mois</span>
            </>
          )}
          {mode === 'workdays' && (
            <span className="text-sm text-gray-500">Du lundi au vendredi</span>
          )}
          {mode === 'daily' && (
            <span className="text-sm text-gray-500">Tous les jours</span>
          )}

          <div className="flex items-center gap-1.5 ml-auto">
            <Clock className="w-3.5 h-3.5 text-gray-400" />
            <span className="text-sm text-gray-500">à</span>
            <CronSelect value={hour} onChange={setHour} options={HOURS} />
            <span className="text-sm text-gray-500">:</span>
            <CronSelect value={minute} onChange={setMinute} options={MINUTES} />
          </div>
        </div>
      )}

      {mode === 'custom' && (
        <div className="space-y-1">
          <input
            value={customExpr}
            onChange={e => setCustomExpr(e.target.value)}
            placeholder="0 30 8 * * MON-FRI"
            className="w-full font-mono border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/30"
          />
          <p className="flex items-center gap-1 text-xs text-gray-400">
            <Info className="w-3 h-3" />
            Format Spring Cron : seconde minute heure jour-du-mois mois jour-semaine
          </p>
        </div>
      )}

      {/* Summary badge */}
      <div className={cn(
        'flex items-center gap-2 rounded-lg px-3 py-2 text-sm',
        isActive ? 'bg-gold-50 text-gold-800 border border-gold-200' : 'bg-gray-50 text-gray-500 border border-gray-200',
      )}>
        <Calendar className="w-4 h-4 shrink-0" />
        <span className="flex-1">{summary}</span>
        {cronExpr && (
          <span className="font-mono text-xs text-gray-400 shrink-0">{cronExpr}</span>
        )}
      </div>

      {error && <p className="text-xs text-red-500">{error}</p>}
    </div>
  )
}
