import api from './api'
import type { AppSettingDto, SettingGroupe } from '@/types/settings'
import type { ApiResponse } from '@/types/api'

export interface InstitutionAlertRouting {
  id: number
  code: string
  name: string
  countryNumericCode?: string | null
  alertEmailTypique?: string | null
  alertEmailMoinsCritique?: string | null
  alertEmailMoyen?: string | null
  alertEmailCritique?: string | null
}

export type AlertSoundCadence = Record<'TYPIQUE' | 'MOINS_CRITIQUE' | 'MOYEN' | 'CRITIQUE', number>
export type FraudAlertCadence = AlertSoundCadence

export const settingsService = {
  getByGroupe: (groupe: SettingGroupe) =>
    api.get<ApiResponse<AppSettingDto[]>>(`/settings/${groupe}`)
      .then(r => r.data.data!),

  update: (cle: string, valeur: string) =>
    api.put<ApiResponse<AppSettingDto>>(`/settings/${encodeURIComponent(cle)}`, { valeur })
      .then(r => r.data.data!),

  updateFraudAlertCadence: (cadence: { typiqueMs: number; moinsCritiqueMs: number; moyenMs: number }) =>
    api.put<ApiResponse<void>>('/settings/fraud-alert-cadence', cadence).then(r => r.data),

  getAlertSoundCadence: () =>
    api.get<ApiResponse<AlertSoundCadence>>('/settings/alert-sound-cadence').then(r => r.data.data!),

  getFraudAlertCadence: () =>
    api.get<ApiResponse<FraudAlertCadence>>('/settings/fraud-alert-cadence').then(r => r.data.data!),

  reload: () =>
    api.post('/settings/reload'),

  testMail: (to: string) =>
    api.post<ApiResponse<string>>('/settings/test-mail', { to })
      .then(r => r.data),

  getCountryCodes: () =>
    api.get<ApiResponse<string[]>>('/settings/country-codes')
      .then(r => r.data.data ?? []),

  addCountryCode: (code: string) =>
    api.post<ApiResponse<string>>('/settings/country-codes', { code })
      .then(r => r.data.data!),

  updateInstitutionAlertRouting: (id: number, request: {
    alertEmailTypique: string | null
    alertEmailMoinsCritique: string | null
    alertEmailMoyen: string | null
    alertEmailCritique: string | null
  }) =>
    api.patch<ApiResponse<InstitutionAlertRouting>>(`/institutions/${id}/alert-routing`, request)
      .then(r => r.data.data!),
}
