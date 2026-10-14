import { Link, useLocation } from 'react-router-dom'
import { ChevronRight, Home } from 'lucide-react'

const ROUTE_LABELS: Record<string, string> = {
  monitoring: 'Surveillance',
  alerts: 'Alertes',
  archives: 'Archives',
  reports: 'Rapports',
  daily: 'Rapports journaliers',
  rules: 'Règles',
  atms: 'GABs / ATMs',
  pos: 'TPE / POS',
  transactions: 'Réseaux',
  operations: 'Transactions',
  fraud: 'Fraude',
  fleet: 'Flotte',
  extraction: 'Extraction',
  atm: 'ATM',
  templates: 'Templates SQL',
  configurations: 'Configurations',
  historique: 'Historique',
  admin: 'Administration',
  users: 'Utilisateurs',
  institutions: 'Institutions',
  roles: 'Rôles',
  scheduler: 'Planificateur',
  audit: "Journal d'audit",
  technicians: 'Techniciens',
  notifications: 'Notifications',
  profile: 'Mon profil',
  settings: 'Paramètres',
}

function segmentLabel(seg: string): string {
  return ROUTE_LABELS[seg] ?? seg
}

export function Breadcrumb() {
  const { pathname } = useLocation()
  const segments = pathname.split('/').filter(Boolean)

  if (segments.length === 0) return null

  const crumbs = segments.map((seg, i) => ({
    label: segmentLabel(seg),
    to: '/' + segments.slice(0, i + 1).join('/'),
    isLast: i === segments.length - 1,
  }))

  return (
    <nav aria-label="Fil d'Ariane" className="flex min-w-0 items-center gap-1 overflow-x-auto whitespace-nowrap text-sm text-gray-500">
      <Link to="/" className="flex items-center transition-colors hover:text-gray-700">
        <Home className="h-3.5 w-3.5" />
      </Link>
      {crumbs.map((crumb) => (
        <span key={crumb.to} className="flex items-center gap-1">
          <ChevronRight className="h-3.5 w-3.5 text-gray-300" />
          {crumb.isLast ? (
            <span className="font-medium text-gray-700">{crumb.label}</span>
          ) : (
            <Link to={crumb.to} className="transition-colors hover:text-gray-700">
              {crumb.label}
            </Link>
          )}
        </span>
      ))}
    </nav>
  )
}
