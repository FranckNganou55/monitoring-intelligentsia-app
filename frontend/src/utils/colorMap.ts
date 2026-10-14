export type StatusKey = 'ok' | 'warning' | 'error' | 'unknown'

export function atmStatusKey(snapshot?: {
  statusId?: string | null
  statusDesc?: string | null
  allOk?: boolean | null
  threshold1Exceeded?: boolean
  threshold2Exceeded?: boolean
}): StatusKey {
  if (!snapshot) return 'unknown'
  const rawStatus = (snapshot.statusId?.trim() || snapshot.statusDesc?.trim() || '').toUpperCase().replace(/[\s-]+/g, '_')
  const offline = ['0', 'DOWN', 'OFFLINE', 'OUTOFSERVICE', 'OUT_OF_SERVICE']
  const operational = ['4', 'SIGNON', 'SIGN_ON', 'ONLINE', 'INSERVICE', 'IN_SERVICE', 'ACTIVE', 'OK']
  if (offline.includes(rawStatus) || rawStatus.includes('OUT_OF_SERVICE')) return 'error'

  if (['1', 'UP', '3', 'SIGNOFF', 'SIGN_OFF', 'WARN', 'WARNING'].includes(rawStatus)) return 'warning'
  if (operational.includes(rawStatus)) return 'ok'
  return 'unknown'
}

export function operatingStatusKey(status?: string): StatusKey {
  if (!status) return 'unknown'
  const s = status.toUpperCase()
  if (s.includes('INSERVICE') || s === 'UP' || s === 'ACTIVE' || s === 'OK') return 'ok'
  if (s.includes('WARN') || s.includes('DEGRADE')) return 'warning'
  if (s.includes('OUTOFSERVICE') || s === 'DOWN' || s === 'ERROR' || s === 'NOK') return 'error'
  return 'unknown'
}

export const statusColors: Record<StatusKey, string> = {
  ok: '#22C55E',
  warning: '#F59E0B',
  error: '#EF4444',
  unknown: '#6B7280',
}

export const statusBadgeClass: Record<StatusKey, string> = {
  ok: 'badge-ok',
  warning: 'badge-warning',
  error: 'badge-error',
  unknown: 'badge-unknown',
}

export const statusLabel: Record<StatusKey, string> = {
  ok: 'Opérationnel',
  warning: 'Avertissement',
  error: 'Hors ligne',
  unknown: 'Inconnu',
}
