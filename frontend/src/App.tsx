import { Routes, Route, Navigate } from 'react-router-dom'
import { AppLayout } from '@/components/layout/AppLayout'
import { AuthLayout } from '@/components/layout/AuthLayout'
import { ProtectedRoute } from '@/features/auth/components/ProtectedRoute'
import { useAuth } from '@/hooks/useAuth'

import { LoginPage } from '@/pages/auth/LoginPage'
import { ForgotPasswordPage } from '@/pages/auth/ForgotPasswordPage'
import { ResetPasswordPage } from '@/pages/auth/ResetPasswordPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { AtmListPage } from '@/pages/atm/AtmListPage'
import { AtmDetailPage } from '@/pages/atm/AtmDetailPage'
import { PosListPage } from '@/pages/pos/PosListPage'
import { TechnicianListPage } from '@/pages/pos/TechnicianListPage'
import { TransactionsOperationsPage } from '@/pages/TransactionsOperationsPage'
import { TransactionPage } from '@/pages/TransactionPage'
import { FraudPage } from '@/pages/FraudPage'
import { FleetPage } from '@/pages/FleetPage'
import { ExtractionAtmPage } from '@/pages/extraction/ExtractionAtmPage'
import { ExtractionPosPage } from '@/pages/extraction/ExtractionPosPage'
import { TemplatesPage } from '@/pages/extraction/TemplatesPage'
import { ConfigurationsPage } from '@/pages/extraction/ConfigurationsPage'
import { HistoriquePage } from '@/pages/extraction/HistoriquePage'
import { UserManagementPage } from '@/pages/admin/UserManagementPage'
import { InstitutionPage } from '@/pages/admin/InstitutionPage'
import { RolePage } from '@/pages/admin/RolePage'
import { SchedulerPage } from '@/pages/admin/SchedulerPage'
import { SettingsPage } from '@/pages/admin/SettingsPage'
import { AuditLogPage } from '@/pages/AuditLogPage'
import { NotificationsPage } from '@/pages/NotificationsPage'
import { ProfilePage } from '@/pages/ProfilePage'

import  RulesPage  from '@/pages/admin/RulesPage'
import AlertsPage from '@/pages/monitoring/AlertsPage'
import ArchivesPage from '@/pages/monitoring/ArchivesPage'
import { DailyFraudReportsPage } from '@/pages/reports/DailyFraudReportsPage'

export default function App() {
  const { isAuthenticated } = useAuth()

  return (
    <Routes>
      <Route
        path="/login"
        element={
          isAuthenticated
            ? <Navigate to="/" replace />
            : <AuthLayout title="Connexion" subtitle="Accédez à votre espace de monitoring"><LoginPage /></AuthLayout>
        }
      />
      <Route path="/forgot-password" element={<AuthLayout title="Mot de passe oublié"><ForgotPasswordPage /></AuthLayout>} />
      <Route path="/reset-password" element={<AuthLayout title="Réinitialiser le mot de passe"><ResetPasswordPage /></AuthLayout>} />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/atms" element={<AtmListPage />} />
          <Route path="/atms/:id" element={<AtmDetailPage />} />
          <Route path="/pos" element={<PosListPage />} />
          <Route path="/pos/technicians" element={<TechnicianListPage />} />
          <Route path="/transactions" element={<TransactionPage />} />
          <Route path="/transactions/operations" element={<TransactionsOperationsPage />} />
          <Route path="/fraud" element={<FraudPage />} />
          <Route path="/fleet" element={<FleetPage />} />
          <Route path="/extraction" element={<Navigate to="/extraction/atm" replace />} />
          <Route path="/extraction/atm" element={<ExtractionAtmPage />} />
          <Route path="/extraction/pos" element={<ExtractionPosPage />} />
          <Route path="/extraction/templates" element={<TemplatesPage />} />
          <Route path="/extraction/configurations" element={<ConfigurationsPage />} />
          <Route path="/extraction/historique" element={<HistoriquePage />} />
          <Route path="/admin/users" element={<UserManagementPage />} />
          <Route path="/admin/institutions" element={<InstitutionPage />} />
          <Route path="/admin/roles" element={<RolePage />} />
          <Route path="/admin/scheduler" element={<SchedulerPage />} />
          <Route path="/admin/settings" element={<SettingsPage />} />
          <Route path="/audit" element={<AuditLogPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/monitoring/alerts" element={<AlertsPage />} />
          <Route path="/monitoring/archives" element={<ArchivesPage />} />
          <Route path="/reports/fraud/daily" element={<DailyFraudReportsPage />} />

          <Route path="/admin/rules" element={<RulesPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
