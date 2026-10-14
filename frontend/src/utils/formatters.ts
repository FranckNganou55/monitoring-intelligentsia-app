import { format, formatDistanceToNow, parseISO } from 'date-fns'
import { fr } from 'date-fns/locale'

export function formatDate(iso: string | undefined | null, fmt = 'dd/MM/yyyy HH:mm'): string {
  if (!iso) return '—'
  try {
    return format(parseISO(iso), fmt, { locale: fr })
  } catch {
    return '—'
  }
}

export function formatDateShort(iso: string | undefined | null): string {
  return formatDate(iso, 'dd/MM/yyyy')
}

export function formatRelative(iso: string | undefined | null): string {
  if (!iso) return '—'
  try {
    return formatDistanceToNow(parseISO(iso), { addSuffix: true, locale: fr })
  } catch {
    return '—'
  }
}

export function formatAmount(value: number | undefined | null, currency = 'XOF'): string {
  if (value === undefined || value === null) return '—'
  return new Intl.NumberFormat('fr-FR', {
    style: 'currency',
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(value)
}

export function formatNumber(value: number | undefined | null): string {
  if (value === undefined || value === null) return '—'
  return new Intl.NumberFormat('fr-FR').format(value)
}

export function formatPercent(value: number | undefined | null): string {
  if (value === undefined || value === null) return '—'
  return `${value.toFixed(2)} %`
}

export function maskCard(card: string): string {
  if (card.length < 4) return card
  return '*'.repeat(card.length - 4) + card.slice(-4)
}
