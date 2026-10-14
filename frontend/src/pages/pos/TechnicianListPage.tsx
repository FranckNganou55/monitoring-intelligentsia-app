import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Search, Wrench, Phone, Building2 } from 'lucide-react'
import { technicianService } from '@/services/posService'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { useDebounce } from '@/hooks/useDebounce'
import { usePagination } from '@/hooks/usePagination'
import { Pagination } from '@/components/ui/Pagination'
import { Card } from '@/components/ui/Card'
import { PageLoader } from '@/components/feedback/PageLoader'
import { EmptyState } from '@/components/feedback/EmptyState'
import type { TechnicianDto } from '@/types/monitoring'

const techColors = [
  ['#DBEAFE', '#1E40AF'], ['#D1FAE5', '#065F46'], ['#FCE7F3', '#9D174D'],
  ['#EDE9FE', '#4C1D95'], ['#FEF3C7', '#92400E'], ['#FEE2E2', '#991B1B'],
]

function TechAvatar({ name, index }: { name: string; index: number }) {
  const [bg, text] = techColors[index % techColors.length]
  return (
    <div
      className="flex h-10 w-10 items-center justify-center rounded-xl text-sm font-bold shrink-0"
      style={{ backgroundColor: bg, color: text }}
    >
      {name.slice(0, 2).toUpperCase()}
    </div>
  )
}

export function TechnicianListPage() {
  const institutionId = useInstitutionFilter()
  const [search, setSearch] = useState('')
  const debouncedSearch = useDebounce(search)
  const { page, size, setPage, reset } = usePagination()

  const { data, isLoading } = useQuery({
    queryKey: ['technicians', page, size, debouncedSearch, institutionId],
    queryFn: () => technicianService.list({ page, size, search: debouncedSearch, institutionId }),
  })

  const techs = data?.content ?? []

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Techniciens</h1>
          <p className="text-xs text-gray-400 mt-0.5">{data?.totalElements ?? 0} technicien(s) enregistré(s)</p>
        </div>
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-100">
          <Wrench className="h-5 w-5 text-blue-600" />
        </div>
      </div>

      <Card className="p-0 overflow-hidden">
        <div className="px-4 py-3 border-b border-gray-100">
          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            <input
              type="text"
              value={search}
              onChange={(e) => { setSearch(e.target.value); reset() }}
              placeholder="Rechercher un technicien…"
              className="w-full rounded-lg border border-gray-200 bg-gray-50 py-2 pl-9 pr-3 text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-gold-500/30 focus:border-gold-500"
            />
          </div>
        </div>

        {isLoading ? (
          <PageLoader />
        ) : !techs.length ? (
          <EmptyState title="Aucun technicien" description="Aucun résultat pour cette recherche." />
        ) : (
          <>
            <div className="divide-y divide-gray-50">
              {techs.map((t: TechnicianDto, i) => (
                <div key={t.id} className="flex items-center gap-4 px-4 py-3.5 hover:bg-gray-50/80 transition-colors">
                  <TechAvatar name={t.name} index={i} />
                  <div className="flex-1 min-w-0">
                    <p className="font-semibold text-sm text-gray-900">{t.name}</p>
                    <div className="flex items-center gap-3 mt-0.5 flex-wrap">
                      {t.phone && (
                        <div className="flex items-center gap-1 text-xs text-gray-500">
                          <Phone className="h-3 w-3" />
                          {t.phone}
                        </div>
                      )}
                      <div className="flex items-center gap-1 text-xs text-gray-400">
                        <Building2 className="h-3 w-3" />
                        {t.institutionName}
                      </div>
                    </div>
                  </div>
                  {t.cards && (
                    <div className="text-xs text-gray-400 hidden sm:block">
                      {t.cards.split(',').length} carte(s)
                    </div>
                  )}
                </div>
              ))}
            </div>
            <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50/40">
              <p className="text-xs text-gray-500">{data?.totalElements} technicien(s)</p>
              <Pagination page={page} totalPages={data?.totalPages ?? 1} onPageChange={setPage} />
            </div>
          </>
        )}
      </Card>
    </div>
  )
}
