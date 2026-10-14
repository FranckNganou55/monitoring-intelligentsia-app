import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { LogOut, User, Settings, Building2, ChevronDown, X } from 'lucide-react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useAuth } from '@/hooks/useAuth'
import { authService } from '@/services/authService'
import { institutionService } from '@/services/institutionService'
import { useInstitutionStore } from '@/store/institutionStore'
import { queryClient } from '@/app/queryClient'
import { NotificationBell } from '@/features/notifications/NotificationBell'
import { NotificationPanel } from '@/features/notifications/NotificationPanel'
import { Breadcrumb } from '@/components/layout/Breadcrumb'
import { SearchBar } from '@/components/ui/SearchBar'
import { Dropdown, type DropdownItem } from '@/components/ui/Dropdown'
import { Avatar } from '@/components/ui/Avatar'
import { cn } from '@/utils/cn'

// ─── Institution selector (GLOBAL / ADMIN only) ───────────────────────────────

function InstitutionSelector() {
  const [open, setOpen] = useState(false)
  const { selectedInstitution, setSelectedInstitution } = useInstitutionStore()
  const selectInstitution = (institution: Parameters<typeof setSelectedInstitution>[0]) => {
    setSelectedInstitution(institution)
    void queryClient.invalidateQueries()
  }

  const { data: institutions = [] } = useQuery({
    queryKey: ['institutions-all'],
    queryFn: () => institutionService.listAll(),
    staleTime: 5 * 60 * 1000,
  })

  const label = selectedInstitution ? selectedInstitution.name : 'Toutes les institutions'

  return (
    <div className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        className={cn(
          'flex items-center gap-2 rounded-lg border px-3 py-1.5 text-sm font-medium transition-colors',
          selectedInstitution
            ? 'border-gold-300 bg-gold-50 text-gold-700 hover:bg-gold-100'
            : 'border-gray-200 bg-gray-50 text-gray-600 hover:bg-gray-100',
        )}
      >
        <Building2 className="h-4 w-4 shrink-0" />
        <span className="hidden sm:block max-w-[160px] truncate">{label}</span>
        <ChevronDown className="h-3.5 w-3.5 shrink-0" />
      </button>

      {open && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setOpen(false)} />
          <div className="absolute right-0 top-full z-50 mt-1 w-64 rounded-xl border border-gray-100 bg-white shadow-lg overflow-hidden animate-fade-up">
            {/* All institutions option */}
            <button
              onClick={() => { selectInstitution(null); setOpen(false) }}
              className={cn(
                'flex w-full items-center gap-2 px-4 py-2.5 text-sm transition-colors hover:bg-gray-50',
                !selectedInstitution ? 'text-gold-600 font-medium bg-gold-50/50' : 'text-gray-700',
              )}
            >
              <X className="h-3.5 w-3.5 shrink-0 text-gray-400" />
              Toutes les institutions
            </button>

            <div className="border-t border-gray-50" />

            <div className="max-h-60 overflow-y-auto">
              {institutions.map((inst) => (
                <button
                  key={inst.id}
                  onClick={() => { selectInstitution({ id: inst.id, code: inst.code, name: inst.name }); setOpen(false) }}
                  className={cn(
                    'flex w-full items-center gap-2 px-4 py-2.5 text-sm transition-colors hover:bg-gray-50',
                    selectedInstitution?.id === inst.id
                      ? 'text-gold-600 font-medium bg-gold-50/50'
                      : 'text-gray-700',
                  )}
                >
                  <Building2 className="h-3.5 w-3.5 shrink-0 text-gray-400" />
                  <span className="truncate">{inst.name}</span>
                  <span className="ml-auto text-[10px] text-gray-400 font-mono shrink-0">{inst.code}</span>
                </button>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  )
}

// ─── Header ───────────────────────────────────────────────────────────────────

export function Header() {
  const { user, isGlobal, logout } = useAuth()
  const navigate = useNavigate()
  const [notifOpen, setNotifOpen] = useState(false)
  const [search, setSearch] = useState('')

  const logoutMutation = useMutation({
    mutationFn: authService.logout,
    onSettled: () => {
      logout()
      queryClient.clear()
      navigate('/login')
    },
  })

  const profileItems: DropdownItem[] = [
    { type: 'label', text: `${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim() },
    { type: 'separator' },
    {
      type: 'item',
      label: 'Mon profil',
      icon: <User className="h-4 w-4" />,
      onClick: () => navigate('/profile'),
    },
    {
      type: 'item',
      label: 'Paramètres',
      icon: <Settings className="h-4 w-4" />,
      onClick: () => navigate('/admin/settings'),
    },
    { type: 'separator' },
    {
      type: 'item',
      label: 'Déconnexion',
      icon: <LogOut className="h-4 w-4" />,
      onClick: () => logoutMutation.mutate(),
      danger: true,
    },
  ]

  return (
    <header className="flex h-16 shrink-0 items-center justify-between border-b border-gray-200/70 bg-white px-4 sm:px-6 gap-3">
      {/* Left: breadcrumb */}
      <Breadcrumb />

      {/* Right: institution selector + search + actions */}
      <div className="flex items-center gap-3 ml-auto">
        {/* Institution context selector — GLOBAL and ADMIN only */}
        {isGlobal && <InstitutionSelector />}

        <SearchBar
          value={search}
          onChange={setSearch}
          placeholder="Rechercher…"
          className="hidden w-56 xl:block"
        />

        <NotificationBell onClick={() => setNotifOpen((v) => !v)} />

        <Dropdown
          trigger={
            <button className="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-gray-50 transition-colors">
              <Avatar
                firstName={user?.firstName}
                lastName={user?.lastName}
                size="sm"
              />
              <span className="hidden md:block text-sm font-medium text-gray-700">
                {user?.firstName}
              </span>
            </button>
          }
          items={profileItems}
          align="right"
        />
      </div>

      <NotificationPanel open={notifOpen} onClose={() => setNotifOpen(false)} />
    </header>
  )
}
