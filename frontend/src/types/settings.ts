export type SettingType   = 'STRING' | 'INTEGER' | 'BOOLEAN' | 'EMAIL' | 'PASSWORD' | 'JSON'
export type SettingGroupe = 'MAIL' | 'CRON' | 'ALERTES' | 'SECURITE' | 'GENERAL' | 'NOTIFICATIONS'

export interface AppSettingDto {
  cle:                string
  valeur:             string | null   // null si sensible
  type:               SettingType
  groupe:             SettingGroupe
  label:              string
  description?:       string
  modifiableParAdmin: boolean
  sensible:           boolean
  sensibleConfigured: boolean
  updatedAt?:         string
  updatedBy?:         string
}
