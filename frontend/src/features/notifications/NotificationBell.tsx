import { Bell } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { notificationService } from '@/services/notificationService'

interface Props {
  onClick: () => void
}

export function NotificationBell({ onClick }: Props) {
  const institutionId = useInstitutionFilter()
  const unreadCountQuery = useQuery({
    queryKey: ['notifications-count', institutionId],
    queryFn: () => notificationService.unreadCount(institutionId),
    refetchInterval: 30_000,
    refetchOnWindowFocus: true,
  })
  const count = unreadCountQuery.data ?? 0

  return (
    <button
      onClick={onClick}
      className="relative p-2 text-gray-400 hover:text-gray-700 transition-colors"
      aria-label="Notifications"
    >
      <Bell className="h-5 w-5" />
      {count > 0 && (
        <span className="absolute right-1 top-1 flex h-4 w-4 items-center justify-center rounded-full bg-status-error text-[10px] font-bold text-white">
          {count > 9 ? '9+' : count}
        </span>
      )}
    </button>
  )
}
