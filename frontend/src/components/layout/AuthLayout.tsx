import { type ReactNode } from 'react'
import { Activity } from 'lucide-react'

interface Props {
  children: ReactNode
  title: string
  subtitle?: string
}

function NetworkSvg() {
  const nodes = [
    { cx: 80,  cy: 80,  r: 6,  delay: '0s',    label: 'GAB-001' },
    { cx: 220, cy: 60,  r: 5,  delay: '0.3s',  label: 'GAB-045' },
    { cx: 340, cy: 100, r: 6,  delay: '0.6s',  label: 'GAB-112' },
    { cx: 140, cy: 180, r: 5,  delay: '0.9s',  label: 'TPE-230' },
    { cx: 280, cy: 200, r: 7,  delay: '1.2s',  label: 'HOST-01' },
    { cx: 60,  cy: 250, r: 4,  delay: '1.5s',  label: 'TPE-089' },
    { cx: 380, cy: 260, r: 5,  delay: '1.8s',  label: 'GAB-067' },
    { cx: 200, cy: 310, r: 6,  delay: '2.1s',  label: 'GAB-200' },
    { cx: 320, cy: 340, r: 4,  delay: '2.4s',  label: 'TPE-301' },
    { cx: 100, cy: 350, r: 5,  delay: '2.7s',  label: 'GAB-155' },
  ]

  const edges = [
    [0, 1], [1, 2], [1, 4], [0, 3], [3, 4],
    [2, 4], [4, 6], [3, 5], [4, 7], [7, 8], [7, 9],
  ]

  return (
    <svg viewBox="0 0 440 400" className="w-full h-full opacity-60" aria-hidden>
      <defs>
        <linearGradient id="lineGrad" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#C9A84C" stopOpacity="0.1" />
          <stop offset="50%" stopColor="#C9A84C" stopOpacity="0.6" />
          <stop offset="100%" stopColor="#C9A84C" stopOpacity="0.1" />
        </linearGradient>
      </defs>

      {edges.map(([a, b], i) => (
        <line
          key={i}
          x1={nodes[a].cx} y1={nodes[a].cy}
          x2={nodes[b].cx} y2={nodes[b].cy}
          stroke="url(#lineGrad)"
          strokeWidth="1"
          strokeDasharray="4 4"
          style={{ animation: `dash 1.5s linear ${i * 0.15}s both` }}
        />
      ))}

      {nodes.map((n, i) => (
        <g key={i}>
          <circle
            cx={n.cx} cy={n.cy} r={n.r * 3.5}
            fill="#C9A84C"
            opacity="0"
            style={{
              animation: `pulse-ring 2.5s ease-out ${n.delay} infinite`,
              transformOrigin: `${n.cx}px ${n.cy}px`,
            }}
          />
          <circle cx={n.cx} cy={n.cy} r={n.r} fill="#C9A84C" opacity="0.9" />
          <circle cx={n.cx} cy={n.cy} r={n.r * 0.45} fill="#2C2C2C" />
        </g>
      ))}
    </svg>
  )
}

const stats = [
  { label: 'GABs actifs',     value: '247',   color: '#22C55E' },
  { label: 'TPE connectés',   value: '1 284', color: '#C9A84C' },
  { label: 'Uptime réseau',   value: '99.7%', color: '#22C55E' },
  { label: 'Alertes actives', value: '3',     color: '#F59E0B' },
]

export function AuthLayout({ children, title, subtitle }: Props) {
  return (
    <div className="flex h-screen overflow-hidden">

      {/* ── Left panel ─────────────────────────────────────────── */}
      <div className="hidden lg:flex lg:w-[55%] flex-col relative overflow-hidden bg-gray-900">

        <div
          className="absolute inset-0"
          style={{
            backgroundImage: `
              linear-gradient(rgba(201,168,76,0.04) 1px, transparent 1px),
              linear-gradient(90deg, rgba(201,168,76,0.04) 1px, transparent 1px)
            `,
            backgroundSize: '48px 48px',
          }}
        />
        <div className="absolute inset-0 bg-gradient-to-br from-gray-900 via-gray-900/95 to-gray-800 pointer-events-none" />
        <div className="absolute bottom-0 left-0 right-0 h-40 bg-gradient-to-t from-gray-900 to-transparent pointer-events-none" />

        <div className="relative z-10 flex flex-col h-full px-12 py-10">

          <div className="flex items-center gap-3 animate-fade-up">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gold-500 shadow-lg">
              <Activity className="h-5 w-5 text-white" />
            </div>
            <div>
              <p className="text-white font-bold text-lg leading-none">MonitoringApp</p>
              <p className="text-gold-400 text-xs font-medium tracking-widest uppercase">Réseau Monétique</p>
            </div>
          </div>

          <div className="mt-14 animate-fade-up" style={{ animationDelay: '0.15s', opacity: 0 }}>
            <h2 className="text-4xl font-bold text-white leading-tight">
              Surveillance<br />
              <span className="text-gold-500">en temps réel</span>
            </h2>
            <p className="mt-4 text-gray-400 text-base leading-relaxed max-w-sm">
              Pilotez votre réseau de GABs, TPEs et hôtes depuis un tableau de bord unifié, avec alertes instantanées et analyses détaillées.
            </p>
          </div>

          <div className="flex-1 mt-8 animate-fade-up" style={{ animationDelay: '0.3s', opacity: 0 }}>
            <NetworkSvg />
          </div>

          <div className="mt-6 grid grid-cols-2 gap-3 animate-fade-up" style={{ animationDelay: '0.45s', opacity: 0 }}>
            {stats.map((s) => (
              <div key={s.label} className="rounded-xl bg-white/5 border border-white/10 px-4 py-3 backdrop-blur-sm">
                <div className="flex items-center gap-2 mb-1">
                  <span
                    className="animate-blink h-2 w-2 rounded-full shrink-0"
                    style={{ backgroundColor: s.color }}
                  />
                  <span className="text-xs text-gray-400">{s.label}</span>
                </div>
                <p className="text-xl font-bold text-white">{s.value}</p>
              </div>
            ))}
          </div>

          <p className="mt-6 text-xs text-gray-600">
            © {new Date().getFullYear()} Intelligentsia — Tous droits réservés
          </p>
        </div>
      </div>

      {/* ── Right panel ────────────────────────────────────────── */}
      <div className="flex flex-1 items-center justify-center bg-gray-50 px-6 animate-slide-in-right">
        <div className="w-full max-w-sm">

          {/* Card */}
          <div className="bg-white rounded-2xl shadow-xl border border-gray-100 overflow-hidden">

            {/* Card header — dark with logo */}
            <div className="bg-gray-900 px-8 py-6 flex flex-col items-center">
              <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-gold-500 shadow-lg mb-3">
                <Activity className="h-6 w-6 text-white" />
              </div>
              <p className="text-white font-bold text-base leading-none">MonitoringApp</p>
              <p className="text-gold-400 text-[10px] font-medium tracking-widest uppercase mt-0.5">
                Réseau Monétique
              </p>
            </div>

            {/* Gold accent line */}
            <div className="h-0.5 bg-gradient-to-r from-gold-700 via-gold-500 to-gold-700" />

            {/* Card body */}
            <div className="px-8 py-7">
              <div className="mb-6">
                <h1 className="text-xl font-bold text-gray-900">{title}</h1>
                {subtitle && (
                  <p className="mt-1 text-sm text-gray-500">{subtitle}</p>
                )}
              </div>
              {children}
            </div>
          </div>

          {/* Footer under card */}
          <p className="mt-4 text-center text-xs text-gray-400">
            MonitoringApp · v1.0.0
          </p>
        </div>
      </div>

    </div>
  )
}
