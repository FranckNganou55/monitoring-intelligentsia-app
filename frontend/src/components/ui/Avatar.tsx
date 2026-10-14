import { cn } from '@/utils/cn'

interface Props {
  firstName?: string | null
  lastName?: string | null
  src?: string | null
  size?: 'xs' | 'sm' | 'md' | 'lg'
  className?: string
}

const sizes = {
  xs: 'h-6 w-6 text-[10px]',
  sm: 'h-7 w-7 text-xs',
  md: 'h-9 w-9 text-sm',
  lg: 'h-11 w-11 text-base',
}

export function Avatar({ firstName, lastName, src, size = 'md', className }: Props) {
  const initials = `${firstName?.[0] ?? ''}${lastName?.[0] ?? ''}`.toUpperCase() || '?'

  if (src) {
    return (
      <img
        src={src}
        alt={`${firstName ?? ''} ${lastName ?? ''}`.trim()}
        className={cn('rounded-full object-cover shrink-0', sizes[size], className)}
      />
    )
  }

  return (
    <div
      className={cn(
        'flex items-center justify-center rounded-full bg-gold-500 font-semibold text-white shrink-0',
        sizes[size],
        className,
      )}
    >
      {initials}
    </div>
  )
}
