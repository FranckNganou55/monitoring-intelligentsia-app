import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export interface InstitutionOption {
  id: number
  code: string
  name: string
}

interface InstitutionState {
  selectedInstitution: InstitutionOption | null
  setSelectedInstitution: (institution: InstitutionOption | null) => void
}

export const useInstitutionStore = create<InstitutionState>()(
  persist(
    (set) => ({
      selectedInstitution: null,
      setSelectedInstitution: (institution) => set({ selectedInstitution: institution }),
    }),
    {
      name: 'institution-storage',
      partialize: (s) => ({ selectedInstitution: s.selectedInstitution }),
    }
  )
)
