import { useAuth } from './useAuth'

export function usePermissions() {
  const { user, isAdmin: hasAdminAccess } = useAuth()

  const hasPermission = (permission: string): boolean =>
    hasAdminAccess || (user?.permissions.includes(permission) ?? false)

  const hasRole = (role: string): boolean =>
    user?.roles.includes(role) ?? false

  const isAdmin = (): boolean =>
    hasAdminAccess || hasRole('ROLE_ADMIN')

  return { hasPermission, hasRole, isAdmin }
}
