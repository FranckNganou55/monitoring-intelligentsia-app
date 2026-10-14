import { cn } from '@/utils/cn'
import { type ReactNode } from 'react'

interface Props {
  children: ReactNode
  className?: string
  hover?: boolean
}

export function Card({ children, className, hover }: Props) {
  return (
    <div className={cn(hover ? 'card-hover' : 'card', className)}>
      {children}
    </div>
  )
}
