import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { UserCheck, UserX, Lock, Users, Plus, Edit2, Search, ShieldCheck } from 'lucide-react'
import toast from 'react-hot-toast'
import { userService } from '@/services/userService'
import { roleService } from '@/services/roleService'
import { institutionService } from '@/services/institutionService'
import { useDebounce } from '@/hooks/useDebounce'
import { usePagination } from '@/hooks/usePagination'
import { Modal } from '@/components/ui/Modal'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Button } from '@/components/ui/Button'
import { Avatar } from '@/components/ui/Avatar'
import { Pagination } from '@/components/ui/Pagination'
import { Card } from '@/components/ui/Card'
import { SkeletonTable } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/feedback/EmptyState'
import { formatDate } from '@/utils/formatters'
import { cn } from '@/utils/cn'
import type { UserDto, UserCreateRequest, UserUpdateRequest } from '@/types/auth'

// ─── Schemas ──────────────────────────────────────────────────────────────────

const roleScope = (code?: string): 'INSTITUTION' | 'GLOBAL' | 'ADMIN' =>
  code === 'ROLE_ADMIN' ? 'ADMIN' : code === 'ROLE_MANAGER' ? 'GLOBAL' : 'INSTITUTION'

function primaryRoleCode(codes: string[] = []) {
  return ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_OPERATOR'].find((code) => codes.includes(code)) ?? codes[0] ?? ''
}

const createSchema = z.object({
  login:         z.string().min(3, 'Minimum 3 caractères'),
  email:         z.string().email('Email invalide'),
  password:      z.string().min(8, '8 caractères minimum'),
  firstName:     z.string().min(1, 'Requis'),
  lastName:      z.string().min(1, 'Requis'),
  phone:         z.string().optional(),
  languageCode:  z.string().default('fr'),
  institutionId: z.coerce.number().optional(),
  roleId:        z.string().min(1, 'Sélectionnez un rôle'),
})
type CreateForm = z.infer<typeof createSchema>

const editSchema = z.object({
  firstName:     z.string().min(1, 'Requis'),
  lastName:      z.string().min(1, 'Requis'),
  email:         z.string().email('Email invalide'),
  phone:         z.string().optional(),
  languageCode:  z.string().default('fr'),
  institutionId: z.coerce.number().optional(),
  roleId:        z.string().min(1, 'Sélectionnez un rôle'),
})
type EditForm = z.infer<typeof editSchema>

// ─── Status helpers ───────────────────────────────────────────────────────────

const statusConfig: Record<string, { label: string; className: string; icon: React.ElementType }> = {
  ACTIVE:   { label: 'Actif',       className: 'bg-green-50 text-green-700 border-green-200', icon: UserCheck },
  INACTIVE: { label: 'Inactif',     className: 'bg-gray-100 text-gray-600 border-gray-200',   icon: UserX },
  LOCKED:   { label: 'Verrouillé',  className: 'bg-red-50   text-red-700   border-red-200',   icon: Lock },
}

function StatusChip({ status }: { status: string }) {
  const cfg = statusConfig[status] ?? { label: status, className: 'bg-gray-100 text-gray-600 border-gray-200', icon: Users }
  return (
    <span className={cn('inline-flex items-center gap-1 text-xs font-medium border px-2 py-0.5 rounded-full', cfg.className)}>
      <cfg.icon className="h-3 w-3" />
      {cfg.label}
    </span>
  )
}

const scopeChipConfig: Record<string, { label: string; className: string }> = {
  INSTITUTION: { label: 'Institution', className: 'bg-blue-50 text-blue-700' },
  GLOBAL:      { label: 'Global',      className: 'bg-purple-50 text-purple-700' },
  ADMIN:       { label: 'Admin',       className: 'bg-gold-100 text-gold-700' },
}

function ScopeChip({ scope }: { scope?: string }) {
  if (!scope) return null
  const cfg = scopeChipConfig[scope] ?? { label: scope, className: 'bg-gray-100 text-gray-600' }
  return (
    <span className={cn('text-[10px] font-semibold uppercase tracking-wide px-1.5 py-0.5 rounded', cfg.className)}>
      {cfg.label}
    </span>
  )
}

// ─── Institution options hook ─────────────────────────────────────────────────

function useInstitutionOptions() {
  const { data = [] } = useQuery({
    queryKey: ['institutions-all'],
    queryFn: () => institutionService.listAll(),
    staleTime: 5 * 60 * 1000,
  })
  return [
    { value: '', label: '— Aucune institution —' },
    ...data.map((i) => ({ value: String(i.id), label: i.name })),
  ]
}

// ─── Create modal ─────────────────────────────────────────────────────────────

function CreateUserModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const qc = useQueryClient()
  const institutionOptions = useInstitutionOptions()
  const { data: roles = [] } = useQuery({ queryKey: ['roles'], queryFn: roleService.list })

  const { register, handleSubmit, reset, watch, getValues, setValue, formState: { errors } } = useForm<CreateForm>({
    resolver: zodResolver(createSchema),
    defaultValues: { languageCode: 'fr', roleId: '' },
  })

  const selectedRoleId = watch('roleId')
  const selectedRole = roles.find((role) => String(role.id) === selectedRoleId)
  const selectedScope = roleScope(selectedRole?.code)

  useEffect(() => {
    if (getValues('roleId')) return
    const operatorRole = roles.find((role) => role.code === 'ROLE_OPERATOR')
    if (operatorRole) setValue('roleId', String(operatorRole.id))
  }, [getValues, roles, setValue])

  const mutation = useMutation({
    mutationFn: (req: UserCreateRequest) => userService.create(req),
    onSuccess: () => {
      toast.success('Utilisateur créé')
      qc.invalidateQueries({ queryKey: ['users'] })
      reset()
      onClose()
    },
    onError: () => toast.error('Échec de la création'),
  })

  function onSubmit(data: CreateForm) {
    const role = roles.find((item) => String(item.id) === data.roleId)
    const scope = roleScope(role?.code)
    const { roleId, ...userData } = data
    mutation.mutate({
      ...userData,
      scope,
      institutionId: scope === 'INSTITUTION' ? data.institutionId || undefined : undefined,
      roleIds: [Number(roleId)],
    } as UserCreateRequest)
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Nouvel utilisateur"
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Annuler</Button>
          <Button type="submit" form="create-user-form" loading={mutation.isPending}>Créer</Button>
        </>
      }
    >
      <form id="create-user-form" onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <Input label="Prénom"  error={errors.firstName?.message} {...register('firstName')} />
          <Input label="Nom"     error={errors.lastName?.message}  {...register('lastName')} />
        </div>
        <Input label="Login"    error={errors.login?.message}    {...register('login')} autoComplete="off" />
        <Input label="Email"    error={errors.email?.message}    {...register('email')} type="email" />
        <Input
          label="Mot de passe"
          error={errors.password?.message}
          {...register('password')}
          type="password"
          autoComplete="new-password"
        />
        <div className="grid grid-cols-2 gap-3">
          <Input label="Téléphone" {...register('phone')} type="tel" />
          <Select
            label="Langue"
            options={[{ value: 'fr', label: 'Français' }, { value: 'en', label: 'English' }]}
            {...register('languageCode')}
          />
        </div>
        <Select label="Rôle unique" options={roles.map((role) => ({ value: String(role.id), label: role.name }))} {...register('roleId')} />
        <p className="text-xs text-gray-500">Le rôle détermine les pages accessibles et le périmètre institutionnel : administrateur (toutes les institutions), responsable (lecture globale) ou opérateur (son institution).</p>
        {selectedScope === 'INSTITUTION' && (
          <Select label="Institution" options={institutionOptions} {...register('institutionId')} />
        )}
      </form>
    </Modal>
  )
}

// ─── Edit modal ───────────────────────────────────────────────────────────────

function EditUserModal({ user, onClose }: { user: UserDto; onClose: () => void }) {
  const qc = useQueryClient()
  const institutionOptions = useInstitutionOptions()
  const { data: roles = [] } = useQuery({ queryKey: ['roles'], queryFn: roleService.list })

  const { register, handleSubmit, watch, setValue, formState: { errors } } = useForm<EditForm>({
    resolver: zodResolver(editSchema),
    defaultValues: {
      firstName:     user.firstName,
      lastName:      user.lastName,
      email:         user.email,
      languageCode:  user.languageCode ?? 'fr',
      institutionId: user.institutionId ?? undefined,
      roleId:        '',
    },
  })

  const selectedRoleId = watch('roleId')
  const selectedRole = roles.find((role) => String(role.id) === selectedRoleId)
  const selectedScope = roleScope(selectedRole?.code)

  useEffect(() => {
    if (!roles.length) return
    const code = primaryRoleCode(user.roles)
    const role = roles.find((item) => item.code === code) ?? roles.find((item) => item.code === 'ROLE_OPERATOR')
    if (role) setValue('roleId', String(role.id))
  }, [roles, setValue, user.id, user.roles])

  const mutation = useMutation({
    mutationFn: (req: UserUpdateRequest) => userService.update(user.id, req),
    onSuccess: () => {
      toast.success('Utilisateur mis à jour')
      qc.invalidateQueries({ queryKey: ['users'] })
      onClose()
    },
    onError: () => toast.error('Échec de la mise à jour'),
  })

  function onSubmit(data: EditForm) {
    const role = roles.find((item) => String(item.id) === data.roleId)
    const scope = roleScope(role?.code)
    const { roleId, ...userData } = data
    mutation.mutate({
      ...userData,
      scope,
      institutionId: scope === 'INSTITUTION' ? data.institutionId || undefined : undefined,
      roleIds: [Number(roleId)],
    } as UserUpdateRequest)
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={`Modifier — ${user.firstName} ${user.lastName}`}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Annuler</Button>
          <Button type="submit" form="edit-user-form" loading={mutation.isPending}>Enregistrer</Button>
        </>
      }
    >
      <form id="edit-user-form" onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <Input label="Prénom" error={errors.firstName?.message} {...register('firstName')} />
          <Input label="Nom"    error={errors.lastName?.message}  {...register('lastName')} />
        </div>
        <Input label="Email" type="email" error={errors.email?.message} {...register('email')} />
        <div className="grid grid-cols-2 gap-3">
          <Input label="Téléphone" {...register('phone')} type="tel" />
          <Select
            label="Langue"
            options={[{ value: 'fr', label: 'Français' }, { value: 'en', label: 'English' }]}
            {...register('languageCode')}
          />
        </div>
        <Select label="Rôle unique" options={roles.map((role) => ({ value: String(role.id), label: role.name }))} {...register('roleId')} />
        <p className="text-xs text-gray-500">Le rôle détermine les pages accessibles et le périmètre institutionnel : administrateur (toutes les institutions), responsable (lecture globale) ou opérateur (son institution).</p>
        {selectedScope === 'INSTITUTION' && (
          <Select label="Institution" options={institutionOptions} {...register('institutionId')} />
        )}
      </form>
    </Modal>
  )
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export function UserManagementPage() {
  const qc = useQueryClient()
  const [search, setSearch]      = useState('')
  const [createOpen, setCreate]  = useState(false)
  const [editUser, setEditUser]  = useState<UserDto | null>(null)
  const debouncedSearch          = useDebounce(search)
  const { page, size, setPage, reset } = usePagination()

  const { data, isLoading } = useQuery({
    queryKey: ['users', page, size, debouncedSearch],
    queryFn: () => userService.list({ page, size, search: debouncedSearch }),
  })

  const toggleStatus = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) => userService.setStatus(id, status),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['users'] }); toast.success('Statut mis à jour') },
    onError:   () => toast.error('Erreur lors de la mise à jour'),
  })

  const users         = data?.content ?? []
  const activeCount   = users.filter((u) => u.status === 'ACTIVE').length
  const inactiveCount = users.filter((u) => u.status === 'INACTIVE').length
  const lockedCount   = users.filter((u) => u.status === 'LOCKED').length

  return (
    <div className="space-y-5">

      {/* ── Header ── */}
      <div className="page-header flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Gestion des Utilisateurs</h1>
          <p className="text-xs text-gray-400 mt-0.5">{data?.totalElements ?? 0} compte(s)</p>
        </div>
        <div className="flex items-center gap-2">
          {[
            { label: 'Actifs',      count: activeCount,   color: 'bg-green-50 text-green-700 border-green-200' },
            { label: 'Inactifs',    count: inactiveCount, color: 'bg-gray-100 text-gray-600 border-gray-200' },
            { label: 'Verrouillés', count: lockedCount,   color: 'bg-red-50 text-red-700 border-red-200' },
          ].map((s) => (
            <div key={s.label} className={cn('rounded-xl border px-3 py-2 flex items-center gap-2', s.color)}>
              <p className="text-base font-bold leading-none">{s.count}</p>
              <p className="text-[10px]">{s.label}</p>
            </div>
          ))}
          <Button size="sm" onClick={() => setCreate(true)}>
            <Plus className="h-4 w-4" />
            Nouveau
          </Button>
        </div>
      </div>

      <Card className="p-0 overflow-hidden">
        {/* ── Search ── */}
        <div className="flex items-center gap-3 px-4 py-3 border-b border-gray-100">
          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            <input
              type="text"
              value={search}
              onChange={(e) => { setSearch(e.target.value); reset() }}
              placeholder="Rechercher un utilisateur…"
              className="w-full rounded-lg border border-gray-200 bg-gray-50 py-2 pl-9 pr-3 text-sm placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-gold-500/30 focus:border-gold-500"
            />
          </div>
        </div>

        {/* ── List ── */}
        {isLoading ? (
          <SkeletonTable rows={6} cols={4} />
        ) : !users.length ? (
          <EmptyState title="Aucun utilisateur" description="Aucun résultat pour cette recherche." />
        ) : (
          <>
            <div className="divide-y divide-gray-50">
              {users.map((user) => (
                <div
                  key={user.id}
                  className="flex items-center gap-4 px-4 py-3.5 hover:bg-gray-50/80 transition-colors"
                >
                  <Avatar firstName={user.firstName} lastName={user.lastName} size="sm" />

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-semibold text-sm text-gray-900">
                        {user.firstName} {user.lastName}
                      </span>
                      <StatusChip status={user.status} />
                      <ScopeChip scope={user.scope} />
                    </div>
                    <div className="flex items-center gap-3 mt-0.5 flex-wrap">
                      <span className="text-xs text-gray-500 font-mono">{user.login}</span>
                      <span className="text-xs text-gray-400">{user.email}</span>
                      {user.institutionName && (
                        <span className="text-xs text-gray-400 flex items-center gap-0.5">
                          <ShieldCheck className="h-3 w-3 text-gold-500" />
                          {user.institutionName}
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="hidden lg:flex items-center gap-1 flex-wrap justify-end max-w-[160px]">
                    {user.roles.map((role) => (
                      <span key={role} className="text-[10px] font-medium bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded">
                        {role.replace('ROLE_', '')}
                      </span>
                    ))}
                  </div>

                  <span className="hidden xl:block text-xs text-gray-400 w-28 text-right shrink-0">
                    {formatDate(user.createdAt)}
                  </span>

                  {/* Actions */}
                  <div className="flex items-center gap-1 shrink-0">
                    <button
                      onClick={() => setEditUser(user)}
                      className="p-1.5 rounded-lg text-gray-400 hover:bg-gray-100 hover:text-gray-700 transition-colors"
                      title="Modifier"
                    >
                      <Edit2 className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => toggleStatus.mutate({
                        id: user.id,
                        status: user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE',
                      })}
                      disabled={toggleStatus.isPending}
                      className={cn(
                        'text-xs font-medium px-3 py-1.5 rounded-lg border transition-all disabled:opacity-40',
                        user.status === 'ACTIVE'
                          ? 'border-red-200   text-red-600   bg-red-50   hover:bg-red-100'
                          : 'border-green-200 text-green-700 bg-green-50 hover:bg-green-100',
                      )}
                    >
                      {user.status === 'ACTIVE' ? 'Désactiver' : 'Activer'}
                    </button>
                  </div>
                </div>
              ))}
            </div>

            <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50/40">
              <p className="text-xs text-gray-500">{data?.totalElements} utilisateur(s)</p>
              <Pagination page={page} totalPages={data?.totalPages ?? 1} onPageChange={setPage} />
            </div>
          </>
        )}
      </Card>

      {/* ── Modals ── */}
      <CreateUserModal open={createOpen} onClose={() => setCreate(false)} />
      {editUser && <EditUserModal user={editUser} onClose={() => setEditUser(null)} />}
    </div>
  )
}
