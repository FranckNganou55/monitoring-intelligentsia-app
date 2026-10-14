import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Search, CreditCard, MapPin, Building2, ChevronRight } from 'lucide-react'
import { posService } from '@/services/posService'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { useDebounce } from '@/hooks/useDebounce'
import { usePagination } from '@/hooks/usePagination'
import { Pagination } from '@/components/ui/Pagination'
import { Card } from '@/components/ui/Card'
import { PageLoader } from '@/components/feedback/PageLoader'
import { EmptyState } from '@/components/feedback/EmptyState'
import type { PosTerminalDto } from '@/types/monitoring'

export function PosListPage() {
  const institutionId = useInstitutionFilter()
  const [search, setSearch] = useState('')
  const debouncedSearch = useDebounce(search)
  const { page, size, setPage, reset } = usePagination()

  const { data, isLoading } = useQuery({
    queryKey: ['pos', page, size, debouncedSearch, institutionId],
    queryFn: () => posService.list({ page, size, search: debouncedSearch, institutionId }),
  })

  const terminals = data?.content ?? []

  return (
    <div className="space-y-5">

      {/* ── Header ── */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">TPE / POS</h1>
          <p className="text-xs text-gray-400 mt-0.5">{data?.totalElements ?? 0} terminaux enregistrés</p>
        </div>
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gold-100">
          <CreditCard className="h-5 w-5 text-gold-600" />
        </div>
      </div>

      <Card className="p-0 overflow-hidden">
        {/* ── Search ── */}
        <div className="px-4 py-3 border-b border-gray-100">
          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            <input
              type="text"
              value={search}
              onChange={(e) => { setSearch(e.target.value); reset() }}
              placeholder="Rechercher terminal, commerçant, ville…"
              className="w-full rounded-lg border border-gray-200 bg-gray-50 py-2 pl-9 pr-3 text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-gold-500/30 focus:border-gold-500"
            />
          </div>
        </div>

        {isLoading ? (
          <PageLoader />
        ) : !terminals.length ? (
          <EmptyState title="Aucun TPE" description="Aucun résultat pour cette recherche." />
        ) : (
          <>
            {/* ── Card grid for small screens / table for large ── */}
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-100 bg-gray-50/60">
                    {['Terminal ID', 'Commerçant', 'Localisation', 'Technicien', 'Institution', ''].map((h) => (
                      <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {terminals.map((t: PosTerminalDto) => (
                    <tr key={t.id} className="group hover:bg-gray-50/80 transition-colors">
                      <td className="px-4 py-3">
                        <span className="font-mono text-xs font-semibold bg-gray-100 px-2 py-0.5 rounded text-gray-900">
                          {t.terminalId}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <p className="font-medium text-gray-900 truncate max-w-[160px]">{t.merchant ?? '—'}</p>
                        {t.extraName && (
                          <p className="text-xs text-gray-400 truncate max-w-[160px]">{t.extraName}</p>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-1 text-gray-600">
                          <MapPin className="h-3.5 w-3.5 text-gray-400 shrink-0" />
                          <span className="text-xs">
                            {[t.city, t.district].filter(Boolean).join(' · ') || '—'}
                          </span>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        {t.technicianName ? (
                          <div className="flex items-center gap-1.5">
                            <div className="flex h-6 w-6 items-center justify-center rounded-full bg-gold-100 text-gold-600 text-[10px] font-bold">
                              {t.technicianName[0].toUpperCase()}
                            </div>
                            <span className="text-xs text-gray-700">{t.technicianName}</span>
                          </div>
                        ) : (
                          <span className="text-gray-300">—</span>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-1 text-xs text-gray-600">
                          <Building2 className="h-3.5 w-3.5 text-gray-400 shrink-0" />
                          {t.institutionName}
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <ChevronRight className="h-4 w-4 text-gray-300 group-hover:text-gray-500 transition-colors" />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-gray-50/40">
              <p className="text-xs text-gray-500">{data?.totalElements} terminal(s)</p>
              <Pagination page={page} totalPages={data?.totalPages ?? 1} onPageChange={setPage} />
            </div>
          </>
        )}
      </Card>
    </div>
  )
}
