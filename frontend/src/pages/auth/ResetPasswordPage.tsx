import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { Eye, EyeOff, Lock, ShieldCheck } from 'lucide-react'
import toast from 'react-hot-toast'
import { cn } from '@/utils/cn'
import { authService } from '@/services/authService'

const schema = z
  .object({
    newPassword: z
      .string()
      .min(8, 'Au moins 8 caractères')
      .regex(/[A-Z]/, 'Une majuscule requise')
      .regex(/[0-9]/, 'Un chiffre requis'),
    confirm: z.string(),
  })
  .refine((d) => d.newPassword === d.confirm, {
    message: 'Les mots de passe ne correspondent pas',
    path: ['confirm'],
  })

type FormData = z.infer<typeof schema>

function PasswordStrength({ value }: { value: string }) {
  const checks = [
    { label: '8 caractères min.', ok: value.length >= 8 },
    { label: 'Une majuscule', ok: /[A-Z]/.test(value) },
    { label: 'Un chiffre', ok: /[0-9]/.test(value) },
  ]
  const score = checks.filter((c) => c.ok).length
  const colors = ['bg-red-400', 'bg-yellow-400', 'bg-green-400', 'bg-green-500']

  if (!value) return null

  return (
    <div className="mt-2 space-y-1.5">
      <div className="flex gap-1">
        {[0, 1, 2].map((i) => (
          <div
            key={i}
            className={cn(
              'h-1 flex-1 rounded-full transition-all duration-300',
              i < score ? colors[score] : 'bg-gray-200',
            )}
          />
        ))}
      </div>
      <div className="flex flex-wrap gap-x-3 gap-y-0.5">
        {checks.map((c) => (
          <span
            key={c.label}
            className={cn('text-xs transition-colors', c.ok ? 'text-green-600' : 'text-gray-400')}
          >
            {c.ok ? '✓' : '○'} {c.label}
          </span>
        ))}
      </div>
    </div>
  )
}

export function ResetPasswordPage() {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const token = params.get('token') ?? ''
  const [showPw, setShowPw] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  const pw = watch('newPassword', '')

  const mutation = useMutation({
    mutationFn: (d: FormData) => authService.resetPassword(token, d.newPassword),
    onSuccess: () => {
      toast.success('Mot de passe réinitialisé avec succès')
      navigate('/login')
    },
    onError: () => toast.error('Lien invalide ou expiré'),
  })

  if (!token) {
    return (
      <div className="text-center space-y-3">
        <p className="text-sm text-red-600">Lien de réinitialisation invalide ou manquant.</p>
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="space-y-5" noValidate>

      <div className="flex items-center gap-3 rounded-xl bg-gold-100 px-4 py-3">
        <ShieldCheck className="h-5 w-5 text-gold-500 shrink-0" />
        <p className="text-sm text-gray-700">
          Choisissez un mot de passe fort que vous n'utilisez nulle part ailleurs.
        </p>
      </div>

      <div>
        <label htmlFor="pw" className="block text-sm font-medium text-gray-700 mb-1.5">
          Nouveau mot de passe
        </label>
        <div className="relative">
          <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
          <input
            id="pw"
            type={showPw ? 'text' : 'password'}
            placeholder="••••••••"
            className={cn('input-base pl-9 pr-10', errors.newPassword && 'border-red-400')}
            {...register('newPassword')}
          />
          <button
            type="button"
            onClick={() => setShowPw((v) => !v)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
          >
            {showPw ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        </div>
        <PasswordStrength value={pw} />
        {errors.newPassword && (
          <p className="mt-1 text-xs text-red-500">{errors.newPassword.message}</p>
        )}
      </div>

      <div>
        <label htmlFor="confirm" className="block text-sm font-medium text-gray-700 mb-1.5">
          Confirmer le mot de passe
        </label>
        <div className="relative">
          <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
          <input
            id="confirm"
            type={showConfirm ? 'text' : 'password'}
            placeholder="••••••••"
            className={cn('input-base pl-9 pr-10', errors.confirm && 'border-red-400')}
            {...register('confirm')}
          />
          <button
            type="button"
            onClick={() => setShowConfirm((v) => !v)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
          >
            {showConfirm ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        </div>
        {errors.confirm && (
          <p className="mt-1 text-xs text-red-500">{errors.confirm.message}</p>
        )}
      </div>

      <button
        type="submit"
        disabled={mutation.isPending}
        className={cn(
          'w-full flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold text-white',
          'bg-gold-500 hover:bg-gold-400 active:scale-[0.98] transition-all shadow-sm',
          'disabled:opacity-60 disabled:cursor-not-allowed',
        )}
      >
        {mutation.isPending ? (
          <>
            <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
            Réinitialisation…
          </>
        ) : (
          'Réinitialiser le mot de passe'
        )}
      </button>
    </form>
  )
}
