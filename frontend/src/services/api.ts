import axios from 'axios'
import { useDataAvailabilityStore } from '@/store/dataAvailabilityStore'
import { useNotificationStore } from '@/store/notificationStore'
import { useAuthStore } from '@/store/authStore'
import { useInstitutionStore } from '@/store/institutionStore'
import { queryClient } from '@/app/queryClient'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const { accessToken, user } = useAuthStore.getState()
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`

  // Inject ?institutionId= for GLOBAL/ADMIN users who have selected a context institution
  if (user?.scope !== 'INSTITUTION' && config.method?.toLowerCase() === 'get') {
    const { selectedInstitution } = useInstitutionStore.getState()
    if (selectedInstitution) {
      config.params = { ...config.params, institutionId: selectedInstitution.id }
    }
  }

  return config
})

let isRefreshing = false
let queue: Array<(token: string) => void> = []

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    if (['DATABASE_UNAVAILABLE', 'BUSINESS_DATABASE_DISCONNECTED'].includes(error.response?.data?.code) || (!error.response && (error.code === 'ERR_NETWORK' || error.code === 'ECONNABORTED'))) {
      if (!useDataAvailabilityStore.getState().unavailable) {
        useDataAvailabilityStore.getState().setUnavailable(true)
        const businessOnly = (query: { queryKey: readonly unknown[] }) => !['roles', 'permission-catalogue', 'settings', 'app-settings', 'scheduler', 'users', 'alert-sound-cadence', 'fraud-alert-cadence'].some(key => String(query.queryKey[0] ?? '').includes(key))
        void queryClient.cancelQueries({ predicate: businessOnly }).then(() => queryClient.removeQueries({ predicate: businessOnly }))
        useInstitutionStore.getState().setSelectedInstitution(null)
        useNotificationStore.getState().setNotifications([])
        useNotificationStore.getState().setUnreadCount(0)
      }
      return Promise.reject(error)
    }
    // Ne pas intercepter les 401 des endpoints d'auth (login, refresh) — pas de retry
    const isAuthEndpoint = original?.url?.includes('/auth/')
    if (error.response?.status !== 401 || original?._retry || isAuthEndpoint) {
      return Promise.reject(error)
    }
    original._retry = true

    if (isRefreshing) {
      return new Promise((resolve) => {
        queue.push((token) => {
          original.headers.Authorization = `Bearer ${token}`
          resolve(api(original))
        })
      })
    }

    isRefreshing = true
    try {
      const refreshToken = useAuthStore.getState().refreshToken
      if (!refreshToken) throw new Error('No refresh token')

      const { data } = await axios.post(
        `${import.meta.env.VITE_API_BASE_URL ?? '/api'}/auth/refresh`,
        { refreshToken }
      )
      const newToken = data.data.accessToken
      useAuthStore.getState().setAccessToken(newToken)
      queue.forEach((cb) => cb(newToken))
      queue = []
      original.headers.Authorization = `Bearer ${newToken}`
      return api(original)
    } catch {
      useAuthStore.getState().logout()
      queryClient.clear()
      window.location.href = '/login'
      return Promise.reject(error)
    } finally {
      isRefreshing = false
    }
  }
)

export default api
