import { type SelectHTMLAttributes, forwardRef } from 'react'
import { cn } from '@/utils/cn'

interface Option { value: string | number; label: string }

interface Props extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  error?: string
  options: Option[]
  placeholder?: string
}

export const Select = forwardRef<HTMLSelectElement, Props>(({ label, error, options, placeholder, className, id, ...rest }, ref) => {
  return (
    <div className="w-full">
      {label && <label htmlFor={id} className="label-base">{label}</label>}
      <select
        ref={ref}
        id={id}
        className={cn('input-base', error && 'border-red-400', className)}
        {...rest}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((o) => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
      {error && <p className="mt-1 text-xs text-red-500">{error}</p>}
    </div>
  )
})
Select.displayName = 'Select'
