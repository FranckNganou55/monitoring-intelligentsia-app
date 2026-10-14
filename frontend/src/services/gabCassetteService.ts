import api from './api'
import type { ApiResponse } from '@/types/api'

export interface GabCassette {
  cassetteNum: number | null
  devise: string | null
  valeurBillet: number | null
  billetsRestants: number | null
  billetsRejetes: number | null
  billetsDispenses: number | null
  soldeCassette: number | null
  derniereMaj: string | null
}
export interface GabCassettes {
  source: 'ORACLE' | 'TEST_EXCEL'
  codeGab: number
  numeroGab: string | null
  nomGab: string | null
  cassettes: GabCassette[]
}
export const gabCassetteService = {
  list: (params: { atmId?: number; institutionId?: number } = {}) =>
    api.get<ApiResponse<GabCassettes[]>>('/gabs/cassettes', { params }).then(response => response.data.data),
}
