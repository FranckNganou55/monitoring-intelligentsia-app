import { cn } from '@/utils/cn'
import { statusBadgeClass, statusLabel, type StatusKey } from '@/utils/colorMap'

interface Props {
  status: StatusKey
  className?: string
}

export function StatusBadge({ status, className }: Props) {
  return (
    <span className={cn(statusBadgeClass[status], className)}>
      {statusLabel[status]}
    </span>
  )
}
