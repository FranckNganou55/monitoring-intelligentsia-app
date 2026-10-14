import type { ElementType, ReactNode } from 'react'

export function PageIntro({ icon: Icon, eyebrow, title, description, children }: {
  icon: ElementType; eyebrow: string; title: string; description: string; children?: ReactNode
}) {
  return <div className="flex min-w-0 items-start gap-4">
    <span className="hidden h-12 w-12 shrink-0 items-center justify-center rounded-2xl border border-gold-200 bg-gold-50 text-gold-600 sm:flex"><Icon className="h-6 w-6" /></span>
    <div className="min-w-0"><p className="page-eyebrow">{eyebrow}</p><h1 className="page-heading">{title}</h1><p className="page-description">{description}</p>{children}</div>
  </div>
}

export function SectionHeading({ title, description }: { title: string; description?: string }) {
  return <div className="section-heading"><h2 className="text-base font-semibold text-gray-900">{title}</h2>{description && <p className="mt-1 text-sm text-gray-500">{description}</p>}</div>
}
