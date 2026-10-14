import { ChevronLeft, ChevronRight } from 'lucide-react'
import { cn } from '@/utils/cn'

interface Props {
  page: number
  totalPages: number
  onPageChange: (p: number) => void
  className?: string
}

export function Pagination({ page, totalPages, onPageChange, className }: Props) {
  if (totalPages <= 1) return null

  return (
    <div className={cn('flex items-center gap-1', className)}>
      <button
        className="btn-secondary p-1.5"
        disabled={page === 0}
        onClick={() => onPageChange(page - 1)}
      >
        <ChevronLeft className="h-4 w-4" />
      </button>
      <span className="px-3 text-sm text-gray-600">
        {page + 1} / {totalPages}
      </span>
      <button
        className="btn-secondary p-1.5"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        <ChevronRight className="h-4 w-4" />
      </button>
    </div>
  )
}
