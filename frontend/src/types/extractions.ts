export type TemplateType = 'SQL_QUERY'
export type OutputFormat = 'CSV' | 'EXCEL'
export type TriggerType = 'MANUEL' | 'CRON'
export type JobStatus = 'EN_ATTENTE' | 'EN_COURS' | 'SUCCES' | 'ECHEC'

export interface ParameterDef {
  name: string
  type: 'STRING' | 'INTEGER' | 'LONG' | 'DECIMAL' | 'DATE' | 'DATETIME' | 'BOOLEAN'
  label: string
  required: boolean
  defaultValue?: string
}

export interface ExtractionTemplateDto {
  id: number
  name: string
  description?: string
  type: TemplateType
  content: string
  parameters: ParameterDef[]
  module?: string
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface ExtractionTemplateRequest {
  name: string
  description?: string
  type: TemplateType
  content: string
  parameters?: ParameterDef[]
  module?: string
  active: boolean
}

export interface DestinataireDto {
  id: number
  email: string
  nom?: string
  active: boolean
}

export interface DestinataireRequest {
  email: string
  nom?: string
  active: boolean
}

export interface ExtractionConfigDto {
  id: number
  institutionId: number
  institutionName: string
  templateId: number
  templateName: string
  templateType: TemplateType
  templateParameters: ParameterDef[]
  name: string
  description?: string
  paramValues: Record<string, string>
  outputFormat: OutputFormat
  cronExpression?: string
  active: boolean
  destinataires: DestinataireDto[]
  createdAt: string
  updatedAt: string
}

export interface ExtractionConfigRequest {
  institutionId: number
  templateId: number
  name: string
  description?: string
  paramValues?: Record<string, string>
  outputFormat: OutputFormat
  cronExpression?: string
  active: boolean
  destinataires?: DestinataireRequest[]
}

export interface ExtractionJobDto {
  id: number
  configId: number
  configName: string
  institutionId: number
  institutionName: string
  triggerType: TriggerType
  status: JobStatus
  startedAt?: string
  endedAt?: string
  durationMs?: number
  rowCount?: number
  downloadable: boolean
  errorMessage?: string
  triggeredByLogin?: string
  createdAt: string
}
