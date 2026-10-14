import { type InputHTMLAttributes, forwardRef } from 'react'
import { cn } from '@/utils/cn'

interface Props extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: string
  error?: string
}

export const Checkbox = forwardRef<HTMLInputElement, Props>(
  ({ label, error, className, id, disabled, ...rest }, ref) => (
    <div className="w-full">
      <label
        htmlFor={id}
        className={cn(
          'flex items-center gap-2.5 cursor-pointer select-none',
          disabled && 'opacity-50 cursor-not-allowed',
        )}
      >
        <input
          ref={ref}
          id={id}
          type="checkbox"
          disabled={disabled}
          className={cn(
            'h-4 w-4 rounded border-gray-300 text-gold-500 focus:ring-gold-500 focus:ring-offset-0',
            className,
          )}
          {...rest}
        />
        <span className="text-sm text-gray-700">{label}</span>
      </label>
      {error && <p className="mt-1 text-xs text-red-500">{error}</p>}
    </div>
  )
)
Checkbox.displayName = 'Checkbox'
