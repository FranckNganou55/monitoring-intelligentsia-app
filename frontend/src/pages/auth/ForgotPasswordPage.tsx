import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { Mail, ArrowLeft, CheckCircle } from 'lucide-react'
import toast from 'react-hot-toast'
import { cn } from '@/utils/cn'
import { authService } from '@/services/authService'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)

  const mutation = useMutation({
    mutationFn: () => authService.forgotPassword(email),
    onSuccess: () => setSent(true),
    onError: () => toast.error('Une erreur est survenue'),
  })

  if (sent) {
    return (
      <div className="text-center space-y-4 animate-fade-up">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-green-100">
          <CheckCircle className="h-7 w-7 text-green-600" />
        </div>
        <div>
          <p className="font-semibold text-gray-900">Email envoyé !</p>
          <p className="mt-1 text-sm text-gray-500">
            Si l'adresse <span className="font-medium text-gray-700">{email}</span> est enregistrée,
            un lien de réinitialisation vous a été envoyé.
          </p>
        </div>
        <p className="text-xs text-gray-400">Pensez à vérifier vos spams.</p>
        <Link
          to="/login"
          className="inline-flex items-center gap-1.5 text-sm text-gold-500 hover:text-gold-400 transition-colors"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          Retour à la connexion
        </Link>
      </div>
    )
  }

  return (
    <div className="space-y-5">
      <p className="text-sm text-gray-500">
        Saisissez votre adresse email. Si un compte existe, vous recevrez un lien de réinitialisation.
      </p>

      <div>
        <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1.5">
          Adresse email
        </label>
        <div className="relative">
          <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
          <input
            id="email"
            type="email"
            placeholder="votre@email.com"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && mutation.mutate()}
            className="input-base pl-9"
          />
        </div>
      </div>

      <button
        type="button"
        onClick={() => mutation.mutate()}
        disabled={mutation.isPending || !email}
        className={cn(
          'w-full flex items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-semibold text-white transition-all',
          'bg-gold-500 hover:bg-gold-400 active:scale-[0.98]',
          'disabled:opacity-60 disabled:cursor-not-allowed shadow-sm',
        )}
      >
        {mutation.isPending ? (
          <>
            <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
            Envoi…
          </>
        ) : (
          'Envoyer le lien'
        )}
      </button>

      <Link
        to="/login"
        className="flex items-center justify-center gap-1.5 text-sm text-gray-500 hover:text-gray-700 transition-colors"
      >
        <ArrowLeft className="h-3.5 w-3.5" />
        Retour à la connexion
      </Link>
    </div>
  )
}
