import { type ReactNode, useState } from 'react'
import { ChevronUp, ChevronDown, ChevronsUpDown } from 'lucide-react'
import { cn } from '@/utils/cn'
import { EmptyState } from '@/components/feedback/EmptyState'
import { SkeletonTable } from '@/components/ui/Skeleton'

export interface Column<T> {
  key: string
  header: string
  render: (row: T) => ReactNode
  sortable?: boolean
  className?: string
}

interface Props<T> {
  columns: Column<T>[]
  data: T[]
  keyFn: (row: T) => string | number
  onRowClick?: (row: T) => void
  onSort?: (key: string, dir: 'asc' | 'desc') => void
  sortKey?: string
  sortDir?: 'asc' | 'desc'
  loading?: boolean
  emptyTitle?: string
  emptyDescription?: string
  className?: string
}

export function Table<T>({
  columns, data, keyFn, onRowClick,
  onSort, sortKey: externalSortKey, sortDir: externalSortDir,
  loading, emptyTitle, emptyDescription, className,
}: Props<T>) {
  const [internalSortKey, setInternalSortKey] = useState<string | null>(null)
  const [internalSortDir, setInternalSortDir] = useState<'asc' | 'desc'>('asc')

  const isControlled = externalSortKey !== undefined
  const activeSortKey = isControlled ? externalSortKey : internalSortKey
  const activeSortDir = isControlled ? (externalSortDir ?? 'asc') : internalSortDir

  const handleSort = (key: string) => {
    const newDir = activeSortKey === key && activeSortDir === 'asc' ? 'desc' : 'asc'
    if (isControlled) {
      onSort?.(key, newDir)
    } else {
      setInternalSortKey(key)
      setInternalSortDir(newDir)
    }
  }

  if (loading) return <SkeletonTable cols={columns.length} />

  return (
    <div className={cn('overflow-x-auto', className)}>
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-gray-100 bg-gray-50/70">
            {columns.map((col) => (
              <th
                key={col.key}
                onClick={() => col.sortable && handleSort(col.key)}
                className={cn(
                  'px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500',
                  col.sortable && 'cursor-pointer select-none hover:text-gray-700 transition-colors',
                  col.className,
                )}
              >
                <span className="inline-flex items-center gap-1">
                  {col.header}
                  {col.sortable && (
                    activeSortKey === col.key ? (
                      activeSortDir === 'asc'
                        ? <ChevronUp className="h-3 w-3 text-gold-500" />
                        : <ChevronDown className="h-3 w-3 text-gold-500" />
                    ) : (
                      <ChevronsUpDown className="h-3 w-3 opacity-30" />
                    )
                  )}
                </span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-50">
          {data.length === 0 ? (
            <tr>
              <td colSpan={columns.length}>
                <EmptyState title={emptyTitle} description={emptyDescription} />
              </td>
            </tr>
          ) : (
            data.map((row) => (
              <tr
                key={keyFn(row)}
                onClick={() => onRowClick?.(row)}
                className={cn(
                  'bg-white hover:bg-gray-50/80 transition-colors',
                  onRowClick && 'cursor-pointer',
                )}
              >
                {columns.map((col) => (
                  <td key={col.key} className={cn('px-4 py-4 text-gray-700', col.className)}>
                    {col.render(row)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}
