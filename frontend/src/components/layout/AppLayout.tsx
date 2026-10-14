import { useCallback, useEffect, useRef } from 'react'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { LogOut, Clock, Database, RefreshCw } from 'lucide-react'
import { Sidebar } from './Sidebar'
import { Header } from './Header'
import { CriticalAlertOverlay } from './CriticalAlertOverlay'
import { ErrorBoundary } from '@/components/feedback/ErrorBoundary'
import { useSse } from '@/features/notifications/hooks/useSse'
import { useIdleTimeout } from '@/hooks/useIdleTimeout'
import { useInstitutionStore } from '@/store/institutionStore'
import { useNotificationStore } from '@/store/notificationStore'
import { useDataAvailabilityStore } from '@/store/dataAvailabilityStore'
import api from '@/services/api'
import { queryClient } from '@/app/queryClient'
import { usePermissions } from '@/hooks/usePermissions'
import { useAuthStore } from '@/store/authStore'

const IDLE_TIMEOUT_MS  = 30 * 60 * 1_000  // fermeture après 30 minutes d'inactivité
const IDLE_WARNING_MS  = 60 * 1_000        // avertissement à 1 minute restante

export function AppLayout() {
  const { pathname } = useLocation()
  const { hasPermission, isAdmin } = usePermissions()
  const permissionRoutes: [string, string][] = [
    ['/atms', 'ATM_READ'], ['/pos', 'POS_READ'], ['/transactions', 'TRANSACTION_READ'],
    ['/fraud', 'FRAUD_READ'], ['/monitoring', 'MONITORING_ALERT_VIEW'],
    ['/reports/fraud', 'FRAUD_REPORT_VIEW'], ['/fleet', 'FLEET_READ'],
    ['/extraction', 'EXTRACTION_READ'], ['/admin/users', 'USER_READ'],
    ['/admin/institutions', 'INSTITUTION_READ'], ['/admin/roles', 'ROLE_READ'],
    ['/admin/scheduler', 'SCHEDULER_READ'], ['/audit', 'AUDIT_READ'], ['/admin/rules', 'MONITORING_RULE_VIEW'],
  ]
  const requiredPermission = pathname === '/' ? 'DASHBOARD_READ' : permissionRoutes.find(([route]) => pathname === route || pathname.startsWith(route + '/'))?.[1]
  const forbidden = (requiredPermission !== undefined && !hasPermission(requiredPermission)) || (pathname === '/admin/settings' && !isAdmin())
  const unavailable = useDataAvailabilityStore(s => s.unavailable)
  useSse(true)
  const backendInstance = useRef<string | null>(null)
  const checkDatabase = useCallback(async () => {
    try {
      const response = await api.get<{ connected: boolean; configurationAvailable?: boolean; backendInstance: string }>('/system/data-status', { timeout: 5000 })
      if (response.data.connected || response.data.configurationAvailable) {
        const firstResponse = backendInstance.current === null
        const changed = backendInstance.current !== null && backendInstance.current !== response.data.backendInstance
        backendInstance.current = response.data.backendInstance
        if (changed) {
          useInstitutionStore.getState().setSelectedInstitution(null)
          useNotificationStore.getState().setNotifications([])
          useNotificationStore.getState().setUnreadCount(0)
        }
        if (firstResponse || changed || useDataAvailabilityStore.getState().unavailable !== !response.data.connected) {
          void queryClient.resetQueries()
          useDataAvailabilityStore.getState().setUnavailable(!response.data.connected)
          window.dispatchEvent(new Event('monitoring:backend-ready'))
        }
      }
    } catch { /* The interceptor marks database/network outages; never invent data. */ }
  }, [])
  useEffect(() => {
    void checkDatabase()
    const timer = window.setInterval(() => void checkDatabase(), 15000)
    return () => window.clearInterval(timer)
  }, [checkDatabase])

  const logout   = useAuthStore(s => s.logout)
  const navigate = useNavigate()

  useEffect(() => {
    const openUrgentAlert = (event: Event) => {
      const alertId = (event as CustomEvent<{ alertId?: number }>).detail?.alertId
      navigate(alertId ? `/monitoring/alerts?alertId=${alertId}` : '/monitoring/alerts')
    }
    window.addEventListener('monitoring:urgent-alert', openUrgentAlert)
    return () => window.removeEventListener('monitoring:urgent-alert', openUrgentAlert)
  }, [navigate])

  const handleLogout = useCallback(() => {
    logout()
    navigate('/login', { replace: true })
  }, [logout, navigate])

  const { showWarning, secondsLeft, stayConnected } = useIdleTimeout({
    timeoutMs: IDLE_TIMEOUT_MS,
    warningMs: IDLE_WARNING_MS,
    onLogout:  handleLogout,
  })

  return (
    <div className="flex h-screen overflow-hidden bg-gray-50">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden min-w-0">
        <Header />
        <main className="app-content flex-1 overflow-y-auto p-4 sm:p-6 xl:p-8">
          <ErrorBoundary>
            {unavailable && <div role="status" className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"><span className="flex items-center gap-2"><Database size={18} />Base métier déconnectée : aucune institution, aucun GAB, aucune alerte ni fraude. Les pages et la configuration restent accessibles.</span><button onClick={() => void checkDatabase()} className="inline-flex items-center gap-2 font-semibold"><RefreshCw size={16} />Vérifier</button></div>}
            {forbidden ? <section className="rounded-xl border bg-white p-6"><h1 className="text-xl font-semibold">Accès non autorisé</h1><p className="mt-2 text-gray-500">Vos permissions actuelles ne permettent pas de consulter cette page.</p><button className="btn-secondary mt-4" onClick={() => navigate('/')}>Retour au tableau de bord</button></section> : <Outlet />}
          </ErrorBoundary>
        </main>
      </div>

      {!unavailable && hasPermission('MONITORING_ALERT_VIEW') && <CriticalAlertOverlay />}

      {/* Idle warning overlay */}
      {showWarning && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-gray-900/60 backdrop-blur-sm" />
          <div className="relative w-full max-w-sm rounded-2xl bg-white shadow-2xl p-6 text-center space-y-4">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-amber-100">
              <Clock className="h-7 w-7 text-amber-600" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-gray-900">Session sur le point d'expirer</h2>
              <p className="mt-1 text-sm text-gray-500">
                Vous serez déconnecté dans{' '}
                <span className="font-semibold text-amber-600">{secondsLeft} seconde{secondsLeft > 1 ? 's' : ''}</span>
                {' '}en raison d'inactivité.
              </p>
            </div>
            <div className="flex flex-col gap-2">
              <button
                onClick={stayConnected}
                className="w-full rounded-lg bg-gold-500 px-4 py-2.5 text-sm font-medium text-white hover:bg-gold-600 transition-colors"
              >
                Rester connecté
              </button>
              <button
                onClick={handleLogout}
                className="flex w-full items-center justify-center gap-2 rounded-lg border border-gray-200 px-4 py-2.5 text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors"
              >
                <LogOut className="h-4 w-4" />
                Se déconnecter maintenant
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
