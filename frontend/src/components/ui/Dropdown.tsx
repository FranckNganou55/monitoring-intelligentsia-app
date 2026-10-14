import { type ReactNode, useEffect, useRef, useState } from 'react'
import { cn } from '@/utils/cn'

export type DropdownItem =
  | { type: 'item'; label: string; icon?: ReactNode; onClick?: () => void; danger?: boolean; disabled?: boolean }
  | { type: 'separator' }
  | { type: 'label'; text: string }

interface Props {
  trigger: ReactNode
  items: DropdownItem[]
  align?: 'left' | 'right'
  className?: string
}

export function Dropdown({ trigger, items, align = 'right', className }: Props) {
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  useEffect(() => {
    const handler = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false)
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [])

  return (
    <div ref={ref} className={cn('relative', className)}>
      <div onClick={() => setOpen((v) => !v)} className="cursor-pointer">
        {trigger}
      </div>

      {open && (
        <div
          className={cn(
            'absolute z-50 mt-1 w-52 rounded-xl border border-gray-100 bg-white py-1 shadow-xl',
            'animate-fade-up',
            align === 'right' ? 'right-0' : 'left-0',
          )}
        >
          {items.map((item, i) => {
            if (item.type === 'separator') {
              return <div key={i} className="my-1 border-t border-gray-100" />
            }
            if (item.type === 'label') {
              return (
                <p key={i} className="px-4 py-1.5 text-xs font-semibold uppercase tracking-wider text-gray-400">
                  {item.text}
                </p>
              )
            }
            return (
              <button
                key={i}
                disabled={item.disabled}
                onClick={() => { item.onClick?.(); setOpen(false) }}
                className={cn(
                  'flex w-full items-center gap-2.5 px-4 py-2 text-sm transition-colors',
                  item.danger
                    ? 'text-red-600 hover:bg-red-50'
                    : 'text-gray-700 hover:bg-gray-50',
                  item.disabled && 'opacity-40 cursor-not-allowed pointer-events-none',
                )}
              >
                {item.icon && <span className="shrink-0 text-current">{item.icon}</span>}
                {item.label}
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}
