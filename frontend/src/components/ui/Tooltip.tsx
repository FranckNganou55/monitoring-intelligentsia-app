import { type ReactNode, useState } from 'react'
import { cn } from '@/utils/cn'

interface Props {
  content: string
  children: ReactNode
  placement?: 'top' | 'right' | 'bottom' | 'left'
  className?: string
}

const placements = {
  top: {
    tooltip: 'bottom-full left-1/2 -translate-x-1/2 mb-2',
    arrow: 'top-full left-1/2 -translate-x-1/2 border-t-gray-900 border-x-transparent border-b-transparent',
  },
  bottom: {
    tooltip: 'top-full left-1/2 -translate-x-1/2 mt-2',
    arrow: 'bottom-full left-1/2 -translate-x-1/2 border-b-gray-900 border-x-transparent border-t-transparent',
  },
  right: {
    tooltip: 'left-full top-1/2 -translate-y-1/2 ml-2',
    arrow: 'right-full top-1/2 -translate-y-1/2 border-r-gray-900 border-y-transparent border-l-transparent',
  },
  left: {
    tooltip: 'right-full top-1/2 -translate-y-1/2 mr-2',
    arrow: 'left-full top-1/2 -translate-y-1/2 border-l-gray-900 border-y-transparent border-r-transparent',
  },
}

export function Tooltip({ content, children, placement = 'top', className }: Props) {
  const [visible, setVisible] = useState(false)
  const p = placements[placement]

  return (
    <div
      className={cn('relative inline-flex', className)}
      onMouseEnter={() => setVisible(true)}
      onMouseLeave={() => setVisible(false)}
    >
      {children}
      {visible && (
        <div className={cn('absolute z-50 whitespace-nowrap', p.tooltip)}>
          <div className="rounded-md bg-gray-900 px-2.5 py-1 text-xs text-white shadow-lg">
            {content}
          </div>
          <div className={cn('absolute border-4', p.arrow)} />
        </div>
      )}
    </div>
  )
}
