import type { RiskLevel } from './rules'
import type { PageResponse } from './api'

export type AlertCategory = 'FRAUD' | 'ATM' | 'NETWORK'

export type AlertRiskLevel = RiskLevel

export type AlertStatus =
  | 'OPEN'
  | 'ACKNOWLEDGED'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'ARCHIVED'

export type AlertEventType =
  | 'CREATED'
  | 'ACKNOWLEDGED'
  | 'ASSIGNED'
  | 'COMMENTED'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'ARCHIVED'
  | 'REOPENED'

export interface MonitoringAlert {
  id: number

  ruleId: number
  ruleCode: string
  ruleName: string

  category: AlertCategory
  riskLevel: RiskLevel

  title: string
  description?: string

  status: AlertStatus

  institutionId?: number
  atmId?: number
  detectedFrom?: string
  detectedUntil?: string
  year?: number
  month?: number
  day?: number
  hour?: number
  fraudDetectionId?: number
  transactionReference?: string

  detectedAt: string
  acknowledgedAt?: string
  resolvedAt?: string
  archivedAt?: string

  assignedTo?: number
}

export interface AlertEvent {
  id: number
  alertId: number
  eventType: AlertEventType

  actorUserId?: number

  oldStatus?: string
  newStatus?: string

  comment?: string
  details?: string

  createdAt: string
}

export interface AlertFilters {
  status?: AlertStatus
  category?: AlertCategory
  riskLevel?: RiskLevel

  ruleId?: number
  institutionId?: number
  atmId?: number

  search?: string

  detectedFrom?: string
  detectedUntil?: string
  year?: number
  month?: number
  day?: number
  hour?: number
  includeArchived?: boolean

  page?: number
  size?: number
}

export interface UpdateAlertStatusRequest {
  status: AlertStatus
  comment?: string
}

export interface CreateAlertRequest {
  ruleId: number
  category: AlertCategory
  title: string
  description: string
  institutionId: number
  atmId: number
}

export type AlertsPageResponse = PageResponse<MonitoringAlert>
