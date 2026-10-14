import { LoadingSpinner } from './LoadingSpinner'

export function PageLoader() {
  return (
    <div className="flex h-full min-h-[300px] items-center justify-center">
      <LoadingSpinner size="lg" />
    </div>
  )
}
