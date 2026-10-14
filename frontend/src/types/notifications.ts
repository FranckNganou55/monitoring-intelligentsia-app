export type NotificationRiskLevel =
  | "TYPIQUE"
  | "MOINS_CRITIQUE"
  | "MOYEN"
  | "CRITIQUE"

export interface NotificationDto {
  id: number
  type: string
  title: string
  message?: string
  read: boolean
  entityType?: string
  entityId?: number
  alertId?: number
  category?: string
  institutionId?: number
  ruleCode?: string
  ruleName?: string
  transactionReference?: string
  terminalId?: string
  detectedAt?: string
  riskLevel?: NotificationRiskLevel
  soundRequired: boolean
  visualAlertRequired: boolean
  createdAt: string
}
