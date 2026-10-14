import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery } from '@tanstack/react-query'
import { User, Lock, Activity, Building2 } from 'lucide-react'
import toast from 'react-hot-toast'
import { useAuth } from '@/hooks/useAuth'
import { useAuthStore } from '@/store/authStore'
import { userService } from '@/services/userService'
import type { ChangePasswordRequest, UpdateMeRequest } from '@/services/userService'
import { Card } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Button } from '@/components/ui/Button'
import { Avatar } from '@/components/ui/Avatar'
import { Skeleton } from '@/components/ui/Skeleton'
import { formatDate, formatRelative } from '@/utils/formatters'
import { cn } from '@/utils/cn'
import type { AuditLogDto } from '@/types/monitoring'
import api from '@/services/api'
import type { ApiResponse, PageResponse } from '@/types/api'

// ─── Schemas ─────────────────────────────────────────────────────────────────

const profileSchema = z.object({
  firstName:    z.string().min(1, 'Prénom requis'),
  lastName:     z.string().min(1, 'Nom requis'),
  email:        z.string().email('Email invalide'),
  phone:        z.string().optional(),
  languageCode: z.string().min(1),
})
type ProfileForm = z.infer<typeof profileSchema>

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Requis'),
    newPassword:     z.string().min(8, '8 caractères minimum'),
    confirm:         z.string().min(1, 'Requis'),
  })
  .refine((d) => d.newPassword === d.confirm, {
    message: 'Les mots de passe ne correspondent pas',
    path: ['confirm'],
  })
type PasswordForm = z.infer<typeof passwordSchema>

// ─── Section card ────────────────────────────────────────────────────────────

function SectionCard({
  icon: Icon, title, children,
}: { icon: React.ElementType; title: string; children: React.ReactNode }) {
  return (
    <Card>
      <div className="flex items-center gap-3 mb-5 pb-4 border-b border-gray-100">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gold-100">
          <Icon className="h-4 w-4 text-gold-600" />
        </div>
        <h2 className="text-base font-semibold text-gray-900">{title}</h2>
      </div>
      {children}
    </Card>
  )
}

// ─── Action badge ─────────────────────────────────────────────────────────────

const actionColors: Record<string, string> = {
  LOGIN:  'bg-green-50 text-green-700',
  LOGOUT: 'bg-gray-100 text-gray-600',
  UPDATE: 'bg-blue-50 text-blue-700',
  CREATE: 'bg-gold-100 text-gold-700',
  DELETE: 'bg-red-50 text-red-700',
}

function ActionBadge({ action }: { action: string }) {
  return (
    <span className={cn('text-[10px] font-bold uppercase px-1.5 py-0.5 rounded', actionColors[action] ?? 'bg-gray-100 text-gray-600')}>
      {action}
    </span>
  )
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export function ProfilePage() {
  const { user } = useAuth()
  const setAuth   = useAuthStore((s) => s.setAuth)
  const accessToken  = useAuthStore((s) => s.accessToken)
  const refreshToken = useAuthStore((s) => s.refreshToken)

  // Profile form
  const { register, handleSubmit, reset: resetProfile, formState: { errors: pe } } = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
  })

  // Populate form once user is known
  useEffect(() => {
    if (user) {
      resetProfile({
        firstName:    user.firstName,
        lastName:     user.lastName,
        email:        user.email,
        phone:        '',
        languageCode: user.languageCode ?? 'fr',
      })
    }
  }, [user, resetProfile])

  // Password form
  const { register: regPwd, handleSubmit: handlePwd, reset: resetPwd, formState: { errors: pwe } } = useForm<PasswordForm>({
    resolver: zodResolver(passwordSchema),
  })

  // Recent audit logs
  const { data: logs, isLoading: logsLoading } = useQuery({
    queryKey: ['audit-me'],
    queryFn: () =>
      api
        .get<ApiResponse<PageResponse<AuditLogDto>>>('/audit', { params: { size: 10, page: 0, search: user?.login } })
        .then((r) => r.data.data),
    enabled: !!user,
  })

  // Mutations
  const profileMutation = useMutation({
    mutationFn: (req: UpdateMeRequest) => userService.updateMe(req),
    onSuccess: (updated) => {
      if (accessToken && refreshToken && user) {
        setAuth(accessToken, refreshToken, { ...user, ...updated })
      }
      toast.success('Profil mis à jour')
    },
    onError: () => toast.error('Échec de la mise à jour'),
  })

  const passwordMutation = useMutation({
    mutationFn: (req: ChangePasswordRequest) => userService.changePassword(req),
    onSuccess: () => { toast.success('Mot de passe modifié'); resetPwd() },
    onError:   () => toast.error('Mot de passe actuel incorrect'),
  })

  const langOptions = [
    { value: 'fr', label: 'Français' },
    { value: 'en', label: 'English' },
  ]

  return (
    <div className="space-y-6 max-w-2xl">
      <h1 className="text-xl font-bold text-gray-900">Mon profil</h1>

      {/* ── Identity card ── */}
      <Card>
        <div className="flex items-center gap-4 mb-6 pb-5 border-b border-gray-100">
          <Avatar firstName={user?.firstName} lastName={user?.lastName} size="lg" />
          <div>
            <p className="font-bold text-gray-900 text-base">
              {user?.firstName} {user?.lastName}
            </p>
            <p className="text-sm text-gray-500 mt-0.5">{user?.email}</p>
            <div className="flex flex-wrap gap-1 mt-2">
              {user?.roles.map((r) => (
                <span key={r} className="text-[10px] font-semibold uppercase tracking-wide bg-gold-100 text-gold-700 px-2 py-0.5 rounded-full">
                  {r.replace('ROLE_', '')}
                </span>
              ))}
              {user?.institutionName && (
                <span className="flex items-center gap-1 text-[10px] text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full">
                  <Building2 className="h-3 w-3" />
                  {user.institutionName}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Profile form */}
        <form
          onSubmit={handleSubmit((d) => profileMutation.mutate(d))}
          className="space-y-4"
        >
          <div className="grid grid-cols-2 gap-4">
            <Input label="Prénom" error={pe.firstName?.message} {...register('firstName')} />
            <Input label="Nom"    error={pe.lastName?.message}  {...register('lastName')} />
          </div>
          <Input label="Email" type="email" error={pe.email?.message} {...register('email')} />
          <div className="grid grid-cols-2 gap-4">
            <Input label="Téléphone" type="tel" placeholder="+221 XX XXX XX XX" {...register('phone')} />
            <Select label="Langue" options={langOptions} {...register('languageCode')} />
          </div>
          <div className="flex justify-end pt-2">
            <Button type="submit" loading={profileMutation.isPending}>
              <User className="h-4 w-4" />
              Enregistrer
            </Button>
          </div>
        </form>
      </Card>

      {/* ── Password ── */}
      <SectionCard icon={Lock} title="Changer le mot de passe">
        <form onSubmit={handlePwd((d) => passwordMutation.mutate(d))} className="space-y-4">
          <Input
            label="Mot de passe actuel"
            type="password"
            autoComplete="current-password"
            error={pwe.currentPassword?.message}
            {...regPwd('currentPassword')}
          />
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Nouveau mot de passe"
              type="password"
              autoComplete="new-password"
              error={pwe.newPassword?.message}
              {...regPwd('newPassword')}
            />
            <Input
              label="Confirmer"
              type="password"
              autoComplete="new-password"
              error={pwe.confirm?.message}
              {...regPwd('confirm')}
            />
          </div>
          <p className="text-xs text-gray-400">Au moins 8 caractères, incluant majuscule, chiffre et caractère spécial.</p>
          <div className="flex justify-end">
            <Button type="submit" loading={passwordMutation.isPending}>
              <Lock className="h-4 w-4" />
              Modifier le mot de passe
            </Button>
          </div>
        </form>
      </SectionCard>

      {/* ── Recent activity ── */}
      <SectionCard icon={Activity} title="Activité récente">
        {logsLoading ? (
          <div className="space-y-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-10 w-full" />
            ))}
          </div>
        ) : !logs?.content?.length ? (
          <p className="text-sm text-gray-400 text-center py-4">Aucune activité récente</p>
        ) : (
          <div className="space-y-1">
            {logs.content.map((log) => (
              <div key={log.id} className="flex items-center gap-3 rounded-lg px-3 py-2 hover:bg-gray-50 transition-colors">
                <ActionBadge action={log.action} />
                <span className="flex-1 text-sm text-gray-700 truncate">
                  {log.entityType ? `${log.entityType} ${log.entityId ?? ''}`.trim() : log.action}
                </span>
                {log.ipAddress && (
                  <span className="text-xs text-gray-400 font-mono hidden sm:block">{log.ipAddress}</span>
                )}
                <span className="text-xs text-gray-400 whitespace-nowrap" title={formatDate(log.createdAt)}>
                  {formatRelative(log.createdAt)}
                </span>
              </div>
            ))}
          </div>
        )}
      </SectionCard>
    </div>
  )
}
