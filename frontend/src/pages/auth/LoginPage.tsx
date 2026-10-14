import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { Eye, EyeOff, Lock, User } from 'lucide-react'
import toast from 'react-hot-toast'
import { cn } from '@/utils/cn'
import { authService } from '@/services/authService'
import { useAuthStore } from '@/store/authStore'
import { useInstitutionStore } from '@/store/institutionStore'

const schema = z.object({
  login: z.string().min(1, 'Login requis'),
  password: z.string().min(1, 'Mot de passe requis'),
})
type FormData = z.infer<typeof schema>

export function LoginPage() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)
  const setSelectedInstitution = useInstitutionStore((s) => s.setSelectedInstitution)
  const [showPassword, setShowPassword] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  const mutation = useMutation({
    mutationFn: authService.login,
    onSuccess: (data) => {
      // Institution selection is browser-persisted and must not leak between accounts.
      setSelectedInstitution(null)
      setAuth(data.accessToken, data.refreshToken, data.user)
      navigate('/', { replace: true })
    },
    onError: (err: unknown) => {
      const status = (err as { response?: { status?: number } })?.response?.status
      if (!status) toast.error('Serveur inaccessible. Vérifiez que le backend est démarré.')
      else if (status === 401 || status === 403) toast.error('Identifiants incorrects.')
      else toast.error(`Erreur serveur (${status}). Vérifiez les logs backend.`)
    },
  })

  return (
    <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="space-y-5" noValidate>

      {/* Login field */}
      <div>
        <label htmlFor="login" className="block text-sm font-medium text-gray-700 mb-1.5">
          Identifiant
        </label>
        <div className="relative">
          <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
          <input
            id="login"
            type="text"
            autoComplete="username"
            placeholder="Votre login"
            className={cn(
              'input-base pl-9 transition-all',
              errors.login && 'border-red-400 focus:border-red-400 focus:ring-red-400',
            )}
            {...register('login')}
          />
        </div>
        {errors.login && (
          <p className="mt-1 text-xs text-red-500">{errors.login.message}</p>
        )}
      </div>

      {/* Password field */}
      <div>
        <div className="flex items-center justify-between mb-1.5">
          <label htmlFor="password" className="block text-sm font-medium text-gray-700">
            Mot de passe
          </label>
          <Link
            to="/forgot-password"
            className="text-xs text-gold-500 hover:text-gold-400 transition-colors"
          >
            Mot de passe oublié ?
          </Link>
        </div>
        <div className="relative">
          <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
          <input
            id="password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            placeholder="••••••••"
            className={cn(
              'input-base pl-9 pr-10 transition-all',
              errors.password && 'border-red-400 focus:border-red-400 focus:ring-red-400',
            )}
            {...register('password')}
          />
          <button
            type="button"
            onClick={() => setShowPassword((v) => !v)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
            aria-label={showPassword ? 'Masquer' : 'Afficher'}
          >
            {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        </div>
        {errors.password && (
          <p className="mt-1 text-xs text-red-500">{errors.password.message}</p>
        )}
      </div>

      {/* Submit */}
      <button
        type="submit"
        disabled={mutation.isPending}
        className={cn(
          'w-full flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold text-white transition-all',
          'bg-gold-500 hover:bg-gold-400 active:scale-[0.98]',
          'disabled:opacity-60 disabled:cursor-not-allowed',
          'shadow-sm hover:shadow-md',
        )}
      >
        {mutation.isPending ? (
          <>
            <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
            Connexion…
          </>
        ) : (
          'Se connecter'
        )}
      </button>

      {/* Error banner */}
      {mutation.isError && (
        <div className="rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700 animate-fade-up">
          {(mutation.error as { response?: { status?: number } })?.response?.status
            ? `Erreur ${(mutation.error as { response?: { status?: number } })?.response?.status} — vérifiez les logs backend.`
            : 'Identifiants incorrects ou serveur inaccessible.'}
        </div>
      )}
    </form>
  )
}
