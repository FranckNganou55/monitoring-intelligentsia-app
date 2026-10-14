export interface FraudDailyReportDto {
  id: number
  reportDate: string
  totalDetected: number
  totalProcessed: number
  totalUnprocessed: number
  generatedAt: string
  status: 'GENERATED' | 'PARTIAL' | 'FAILED'
  errorMessage?: string
  downloadable: boolean
  availableFormats?: Array<'WORD' | 'PDF' | 'EXCEL'>
}
