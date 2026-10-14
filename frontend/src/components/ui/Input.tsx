import { type InputHTMLAttributes, forwardRef } from 'react'
import { cn } from '@/utils/cn'

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
}

export const Input = forwardRef<HTMLInputElement, Props>(({ label, error, className, id, ...rest }, ref) => {
  return (
    <div className="w-full">
      {label && <label htmlFor={id} className="label-base">{label}</label>}
      <input
        ref={ref}
        id={id}
        className={cn('input-base', error && 'border-red-400 focus:border-red-400 focus:ring-red-400', className)}
        {...rest}
      />
      {error && <p className="mt-1 text-xs text-red-500">{error}</p>}
    </div>
  )
})
Input.displayName = 'Input'
