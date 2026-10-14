import { useAuth } from './useAuth'
import { useInstitutionStore } from '@/store/institutionStore'

export function useInstitutionFilter(): number | undefined {
  const { user } = useAuth()
  const selectedInstitution = useInstitutionStore((s) => s.selectedInstitution)

  // Institution-scoped users always use their own assigned institution (from JWT)
  if (user?.scope === 'INSTITUTION') {
    return user.institutionId ?? undefined
  }

  // Global / Admin users use the institution selected in the header dropdown
  return selectedInstitution?.id ?? undefined
}
