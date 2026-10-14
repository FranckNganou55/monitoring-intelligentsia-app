import { useAuthStore } from '@/store/authStore'
import type { UserScope } from '@/types/auth'

export function useAuth() {
  const user = useAuthStore((s) => s.user)
  const accessToken = useAuthStore((s) => s.accessToken)
  const logout = useAuthStore((s) => s.logout)

  // The built-in roles are the source of truth for the app-wide scope.
  const scope: UserScope =
    user?.roles?.includes('ROLE_ADMIN')
      ? 'ADMIN'
      : user?.roles?.includes('ROLE_MANAGER')
        ? 'GLOBAL'
        : user?.roles?.includes('ROLE_OPERATOR')
          ? 'INSTITUTION'
          : user?.scope ?? 'INSTITUTION'

  const isAdmin = scope === 'ADMIN' || !!user?.roles?.includes('ROLE_ADMIN')
  const isGlobal = scope === 'GLOBAL' || scope === 'ADMIN'
  const isInstitutionScoped = scope === 'INSTITUTION'

  return {
    user,
    accessToken,
    isAuthenticated: !!accessToken && !!user,
    logout,
    scope,
    isAdmin,
    isGlobal,
    isInstitutionScoped,
  }
}
