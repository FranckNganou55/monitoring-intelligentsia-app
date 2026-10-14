import { PackageOpen } from 'lucide-react'

interface Props {
  title?: string
  description?: string
}

export function EmptyState({ title = 'Aucun résultat', description = 'Il n\'y a rien à afficher pour le moment.' }: Props) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-16 text-gray-600">
      <PackageOpen className="h-10 w-10 text-gray-200" />
      <p className="font-medium text-gray-700">{title}</p>
      <p className="text-sm">{description}</p>
    </div>
  )
}
