import { type ElementType } from 'react'
import { NavLink } from 'react-router-dom'
import {
  Activity,
  LayoutDashboard,
  Monitor,
  CreditCard,
  TrendingUp,
  Shield,
  Briefcase,
  Download,
  Users,
  Building2,
  ShieldCheck,
  Clock,
  FileText,
  ChevronLeft,
  ChevronRight,
  Code2,
  Settings2,
  History,
  SlidersHorizontal,
  ListChecks,
  AlertTriangle,
  Archive,
  FileBarChart,
} from 'lucide-react'
import { cn } from '@/utils/cn'
import { usePermissions } from '@/hooks/usePermissions'
import { useAuth } from '@/hooks/useAuth'
import { useUiStore } from '@/store/uiStore'
import { Tooltip } from '@/components/ui/Tooltip'

interface NavItem {
  to: string
  label: string
  icon: ElementType
  permission: string | null
}

const navItems: NavItem[] = [
  {
    to: '/',
    label: 'Tableau de bord',
    icon: LayoutDashboard,
    permission: 'DASHBOARD_READ',
  },
  {
    to: '/atms',
    label: 'GABs / ATMs',
    icon: Monitor,
    permission: 'ATM_READ',
  },
  {
    to: '/pos',
    label: 'TPE / POS',
    icon: CreditCard,
    permission: 'POS_READ',
  },
  {
    to: '/transactions',
    label: 'Réseaux',
    icon: TrendingUp,
    permission: 'TRANSACTION_READ',
  },
  { to: '/transactions/operations', label: 'Transactions', icon: ListChecks, permission: 'TRANSACTION_READ' },
  {
    to: '/fraud',
    label: 'Fraude',
    icon: Shield,
    permission: 'FRAUD_READ',
  },

  // Surveillance
  {
    to: '/monitoring/alerts',
    label: 'Alertes',
    icon: AlertTriangle,
    permission: 'MONITORING_ALERT_VIEW',
  },
  {
    to: '/monitoring/archives',
    label: 'Archives',
    icon: Archive,
    permission: 'MONITORING_ALERT_VIEW',
  },
  {
    to: '/reports/fraud/daily',
    label: 'Rapports journaliers',
    icon: FileBarChart,
    permission: 'FRAUD_REPORT_VIEW',
  },

  {
    to: '/fleet',
    label: 'Flotte',
    icon: Briefcase,
    permission: 'FLEET_READ',
  },
  {
    to: '/extraction/atm',
    label: 'Extraction ATM',
    icon: Download,
    permission: 'EXTRACTION_READ',
  },
  {
    to: '/extraction/pos',
    label: 'Extraction POS',
    icon: Download,
    permission: 'EXTRACTION_READ',
  },
]

const extractionEngineItems: NavItem[] = [
  { to: '/extraction/templates',      label: 'Templates SQL',    icon: Code2,     permission: 'EXTRACTION_READ' },
  { to: '/extraction/configurations', label: 'Configurations',   icon: Settings2, permission: 'EXTRACTION_READ' },
  { to: '/extraction/historique',     label: 'Historique',       icon: History,   permission: 'EXTRACTION_READ' },
]

const adminItems: NavItem[] = [
  { to: '/admin/users', label: 'Utilisateurs', icon: Users, permission: 'USER_READ' },
  { to: '/admin/institutions', label: 'Institutions', icon: Building2, permission: 'INSTITUTION_READ' },
  { to: '/admin/roles', label: 'Rôles', icon: ShieldCheck, permission: 'ROLE_READ' },
  { to: '/admin/scheduler', label: 'Planificateur', icon: Clock, permission: 'SCHEDULER_READ' },
  { to: '/audit',            label: "Journal d'audit",   icon: FileText,          permission: 'AUDIT_READ'     },
  { to: '/admin/settings',  label: 'Paramètres',         icon: SlidersHorizontal, permission: null             },
  { to: '/admin/rules',    label: 'Règles',     icon: ListChecks,        permission: 'MONITORING_RULE_VIEW'    },
]

function NavItemLink({ item, collapsed }: { item: NavItem; collapsed: boolean }) {
  const linkClass = ({ isActive }: { isActive: boolean }) =>
    cn(
      'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
      collapsed ? 'justify-center' : '',
      isActive
        ? 'bg-gold-500 text-white'
        : 'text-gray-400 hover:bg-gray-800 hover:text-white',
    )

  const link = (
    <NavLink to={item.to} end={item.to === '/' || item.to === '/transactions'} className={linkClass}>
      <item.icon className="h-4 w-4 shrink-0" />
      {!collapsed && <span>{item.label}</span>}
    </NavLink>
  )

  if (collapsed) {
    return <Tooltip content={item.label} placement="right">{link}</Tooltip>
  }
  return link
}

export function Sidebar() {
  const { hasPermission } = usePermissions()
  const { user, isAdmin, scope } = useAuth()
  const { sidebarCollapsed, toggleSidebar } = useUiStore()

  const scopeLabel: Record<string, string> = {
    ADMIN: 'Administrateur',
    GLOBAL: 'Global',
    INSTITUTION: user?.institutionName ?? 'Opérateur',
  }
  const roleLabel = scopeLabel[scope] ?? 'Utilisateur'

  return (
    <aside
      className={cn(
        'flex h-full flex-col bg-gray-900 transition-all duration-300 ease-in-out shrink-0',
        sidebarCollapsed ? 'w-16' : 'w-60',
      )}
    >
      {/* Logo */}
      <div
        className={cn(
          'flex h-16 items-center border-b border-gray-800 px-4',
          sidebarCollapsed ? 'justify-center' : 'gap-3',
        )}
      >
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-gold-500">
          <Activity className="h-4 w-4 text-white" />
        </div>
        {!sidebarCollapsed && (
          <div className="min-w-0">
            <p className="truncate text-sm font-bold text-white leading-none">MonitoringApp</p>
            <p className="truncate text-[10px] font-medium tracking-widest uppercase text-gold-400 mt-0.5">
              Réseau Monétique
            </p>
          </div>
        )}
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto px-2 py-4 space-y-0.5">
        {navItems.map((item) => {
          if (item.permission && !hasPermission(item.permission)) return null
          return <NavItemLink key={item.to} item={item} collapsed={sidebarCollapsed} />
        })}

        {/* Extraction engine sub-section */}
        {hasPermission('EXTRACTION_READ') && (
          <>
            <div className="my-3 border-t border-gray-800" />
            {!sidebarCollapsed && (
              <p className="px-3 pb-1 text-[10px] font-semibold uppercase tracking-wider text-gray-500">
                Moteur d'extractions
              </p>
            )}
            {extractionEngineItems.map((item) => (
              <NavItemLink key={item.to} item={item} collapsed={sidebarCollapsed} />
            ))}
          </>
        )}

        {/* Admin section — ADMIN scope only */}
        {(isAdmin || adminItems.some(item => item.permission && hasPermission(item.permission))) && (
          <>
            <div className="my-3 border-t border-gray-800" />

            {!sidebarCollapsed && (
              <p className="px-3 pb-1 text-[10px] font-semibold uppercase tracking-wider text-gray-500">
                Administration
              </p>
            )}

            {adminItems.map((item) => {
              if ((!item.permission && !isAdmin) || (item.permission && !hasPermission(item.permission))) return null
              return <NavItemLink key={item.to} item={item} collapsed={sidebarCollapsed} />
            })}
          </>
        )}
      </nav>

      {/* User role + collapse toggle */}
      <div className="border-t border-gray-800 p-3 space-y-2">
        {!sidebarCollapsed && user && (
          <div className="rounded-lg bg-gray-800 px-3 py-2">
            <p className="truncate text-xs font-medium text-white">
              {user.firstName} {user.lastName}
            </p>
            <p className="truncate text-[10px] text-gold-400 mt-0.5">{roleLabel}</p>
          </div>
        )}
        <button
          onClick={toggleSidebar}
          title={sidebarCollapsed ? 'Développer' : 'Réduire'}
          className={cn(
            'flex w-full items-center rounded-lg px-3 py-2 text-xs text-gray-400 transition-colors hover:bg-gray-800 hover:text-white',
            sidebarCollapsed ? 'justify-center' : 'gap-2',
          )}
        >
          {sidebarCollapsed
            ? <ChevronRight className="h-4 w-4" />
            : <><ChevronLeft className="h-4 w-4" /><span>Réduire</span></>
          }
        </button>
      </div>
    </aside>
  )
}
