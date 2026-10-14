import { type TextareaHTMLAttributes, forwardRef } from 'react'
import { cn } from '@/utils/cn'

interface Props extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string
  error?: string
}

export const Textarea = forwardRef<HTMLTextAreaElement, Props>(
  ({ label, error, className, id, rows = 4, ...rest }, ref) => (
    <div className="w-full">
      {label && <label htmlFor={id} className="label-base">{label}</label>}
      <textarea
        ref={ref}
        id={id}
        rows={rows}
        className={cn(
          'input-base resize-none',
          error && 'border-red-400 focus:border-red-400 focus:ring-red-400',
          className,
        )}
        {...rest}
      />
      {error && <p className="mt-1 text-xs text-red-500">{error}</p>}
    </div>
  )
)
Textarea.displayName = 'Textarea'
