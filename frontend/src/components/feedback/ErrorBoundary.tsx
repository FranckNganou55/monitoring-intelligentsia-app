import { Component, type ReactNode } from 'react'
import { AlertTriangle } from 'lucide-react'

interface Props { children: ReactNode }
interface State { hasError: boolean; error?: Error }

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex flex-col items-center justify-center gap-4 py-16 text-center">
          <AlertTriangle className="h-12 w-12 text-status-error" />
          <p className="text-lg font-semibold text-gray-900">Une erreur est survenue</p>
          <p className="text-sm text-gray-600">{this.state.error?.message}</p>
          <button className="btn-secondary" onClick={() => this.setState({ hasError: false })}>
            Réessayer
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
