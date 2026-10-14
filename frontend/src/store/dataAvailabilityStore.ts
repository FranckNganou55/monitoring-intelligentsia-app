import { create } from 'zustand'

export const useDataAvailabilityStore = create<{ unavailable: boolean; setUnavailable: (value: boolean) => void }>(set => ({
  unavailable: false,
  setUnavailable: unavailable => set({ unavailable }),
}))
