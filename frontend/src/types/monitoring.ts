export interface AtmDto {
  id: number
  institutionId: number
  institutionName: string
  terminalId: string
  address?: string
  city?: string
  currency1Alpha?: string
  threshold1?: number
  currency2Alpha?: string
  threshold2?: number
  svfePid?: number
  createdAt: string
  updatedAt: string
  // Latest status fields are populated by the backend for list and detail views.
  statusId?: string
  statusDesc?: string
  allOk?: boolean
  threshold1Exceeded?: boolean
  threshold2Exceeded?: boolean
}

export interface AtmSnapshotDto {
  id: number
  atmId: number
  snapshotTime: string
  currency1Alpha?: string
  note1?: number
  quantity1?: number
  currency2Alpha?: string
  note2?: number
  quantity2?: number
  currency3Alpha?: string
  note3?: number
  quantity3?: number
  currency4Alpha?: string
  note4?: number
  quantity4?: number
  statusId?: string
  statusDesc?: string
  allOk: boolean
  statusMessage?: string
  threshold1Exceeded: boolean
  threshold2Exceeded: boolean
}

export type AtmStatus = 'ok' | 'warning' | 'error' | 'unknown'

export interface NetworkSnapshotDto {
  id: number
  networkCode: string
  description?: string
  operatingStatus?: string
  processingStatus?: string
  transactionMode?: string
  snapshotTime: string
}

export interface HostSnapshotDto {
  id: number
  hostname: string
  description?: string
  ipAddress?: string
  operatingStatus?: string
  processingStatus?: string
  snapshotTime: string
}

export interface TransactionRateDto {
  id: number
  network: string
  acquirer?: string
  issuer?: string
  successCount: number
  successRate?: number
  failureCount: number
  failureRate?: number
  snapshotTime: string
}

export interface TransactionErrorDto {
  id: number
  network: string
  responseCode?: string
  responseDesc?: string
  count: number
  snapshotTime: string
}

export interface FraudRateDto {
  id: number
  institutionId?: number
  bin: string
  suspectCount: number
  fraudCount: number
  snapshotTime: string
}

export interface FraudDetailDto {
  id: number
  institutionId?: number
  institutionName?: string
  bin: string
  ruleCode?: string
  ruleDesc?: string
  sourceReference?: string
  sourceRiskLevel?: string
  riskLevel?: string
  suspectCount: number
  fraudCount: number
  bins?: string
  description?: string
  snapshotTime: string
}

export interface FraudAlertBreakdown {
  label: string
  description?: string
  count: number
}

export interface FraudAlertRow {
  id: number
  detectedAt: string
  riskLevel: 'TYPIQUE' | 'MOINS_CRITIQUE' | 'MOYEN' | 'CRITIQUE'
  cardNumber: string
  amount?: number
  currency?: string
  terminalId?: string
  institutionId?: number
  institutionName?: string
  ruleId: number
  ruleCode: string
  ruleName: string
  title: string
  status: 'OPEN' | 'ACKNOWLEDGED' | 'IN_PROGRESS' | 'RESOLVED' | 'ARCHIVED'
  transactionReference?: string
}

export interface FraudAlertOverview {
    criticalCount: number
    mediumCount: number
    typicalCount: number
    lessCriticalCount: number
    lowCount: number
  toQualifyCount: number
  confirmedCount: number
  hourlyAlerts: Array<{
    time: string
    count: number
    criticalCount: number
    mediumCount: number
    typicalCount: number
    lessCriticalCount: number
  }>
  topBins: FraudAlertBreakdown[]
  topRules: FraudAlertBreakdown[]
  alerts: FraudAlertRow[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface FleetAccountDto {
  id: number
  institutionId?: number
  accountNumber: string
  currency?: string
  balance?: number
  previousBalance?: number
  delta1?: number
  delta2?: number
  delta1Exceeded: boolean
  delta2Exceeded: boolean
  snapshotTime: string
}

export interface CardOverLimitDto {
  id: number
  institutionId?: number
  cardNumber: string
  currency?: string
  limitAmount?: number
  currentAmount?: number
  exceeded: boolean
  snapshotTime: string
}

export interface PosTerminalDto {
  id: number
  institutionId: number
  institutionName: string
  terminalId: string
  merchant?: string
  city?: string
  district?: string
  extraName?: string
  technicianId?: number
  technicianName?: string
}

export interface TechnicianDto {
  id: number
  institutionId: number
  institutionName: string
  name: string
  phone?: string
  cards?: string
}

export interface ExtractionBatchDto {
  id: number
  institutionId: number
  institutionName: string
  type: 'ATM' | 'POS'
  startDate: string
  endDate: string
  merchant?: string
  city?: string
  createdAt: string
  createdBy: number
  terminalIds: string[]
}

export interface InstitutionDto {
  id: number
  code: string
  name: string
  svfeId?: string
  hostPid?: string
  mailRecharge?: string
  createdAt: string
  updatedAt: string
}

export interface SchedulerTaskDto {
  id: number
  code: string
  name: string
  enabled: boolean
  intervalMs: number
  lastRun?: string
  lastStatus?: string
  lastError?: string
}

export interface DashboardHistoryDto {
  atmAvailabilityHistory: Array<{ time: string; percentage: number }>
  atmAlertImpact: Array<{ time: string; terminalId: string; count: number }>
  fraudHistory: Array<{ time: string; count: number }>
}
export interface DashboardSummaryDto {
  totalAtms: number
  atmsOnline: number
  atmsOffline: number
  atmsWarning: number
  atmsUnknown: number

  totalPos: number
  posWithoutTelecollect: number

  gabAlertsToday: number
  totalFrauds: number

  transactionSuccessRate?: number
  
  networks: NetworkSnapshotDto[]
  hosts: HostSnapshotDto[]
}

export interface AuditLogDto {
  id: number
  userId?: number
  userLogin?: string
  action: string
  entityType?: string
  entityId?: string
  oldValue?: Record<string, unknown>
  newValue?: Record<string, unknown>
  ipAddress?: string
  userAgent?: string
  createdAt: string
}
