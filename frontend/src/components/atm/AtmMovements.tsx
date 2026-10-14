import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { atmService } from '@/services/atmService'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { Card } from '@/components/ui/Card'
import { StatusBadge } from '@/components/ui/StatusBadge'
import { Pagination } from '@/components/ui/Pagination'
import { formatDate } from '@/utils/formatters'
import { AtmReportControls, dateFilters, movementStatuses } from './AtmReportControls'

export function AtmMovements({ atmId }: { atmId: number }) {
  const institutionId = useInstitutionFilter()
  const [status, setStatus] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [page, setPage] = useState(0)
  const validPeriod = !from || !to || from <= to
  const filters = { status: status || undefined, ...dateFilters(from, to) }
  const query = useQuery({ queryKey: ['atm-movements', atmId, institutionId, status, from, to, page],
    queryFn: () => atmService.movements(atmId, { ...filters, page, size: 20 }), enabled: !!atmId && validPeriod, refetchInterval: 30_000 })
  return <Card className="space-y-4">
    <div><h2 className="text-lg font-bold text-gray-900">Mouvements et changements de statut</h2>
      <p className="mt-1 text-sm text-gray-500">Changements observés lors des collectes GAB et interfaces. Le premier relevé de chaque source indique son état initial connu.</p></div>
    <div className="flex flex-wrap items-end gap-3">
      <label className="text-sm text-gray-700">Statut<select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }} className="mt-1 block rounded-lg border border-gray-200 p-2">
        {movementStatuses.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </select></label>
      <label className="text-sm text-gray-700">Du<input type="date" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0) }} className="mt-1 block rounded-lg border border-gray-200 p-2" /></label>
      <label className="text-sm text-gray-700">Au<input type="date" value={to} onChange={(e) => { setTo(e.target.value); setPage(0) }} className="mt-1 block rounded-lg border border-gray-200 p-2" /></label>
      <AtmReportControls disabled={!validPeriod} params={{ id: atmId, movementStatus: status || undefined, ...dateFilters(from, to), institutionId }} />
    </div>
    {!validPeriod ? <p role="alert" className="text-red-600">La date de début doit précéder la date de fin.</p>
      : query.isError ? <p role="alert" className="text-red-600">Impossible de charger les mouvements. <button onClick={() => void query.refetch()} className="underline">Réessayer</button></p>
      : query.isLoading ? <p>Chargement des mouvements…</p>
      : <><div className="overflow-x-auto"><table className="w-full text-sm"><thead><tr className="border-b border-gray-200 text-left text-gray-500">
        {['Date', 'Source', 'Statut précédent', 'Nouveau statut', 'État', 'Message'].map((label) => <th key={label} className="px-3 py-3">{label}</th>)}
      </tr></thead><tbody>{query.data?.content.map((movement) => <tr key={`${movement.source}-${movement.id}`} className="border-b border-gray-100">
        <td className="whitespace-nowrap px-3 py-3">{formatDate(movement.observedAt)}</td><td className="px-3 py-3">{movement.source}</td>
        <td className="px-3 py-3">{movement.previousStatus ?? 'État initial'}</td><td className="px-3 py-3 font-bold">{movement.status}</td>
        <td className="px-3 py-3"><StatusBadge status={movement.state} className="!text-sm !font-bold !px-3 !py-1.5" /></td><td className="px-3 py-3">{movement.message || '—'}</td>
      </tr>)}</tbody></table></div>
      {!query.data?.content.length && <p className="py-5 text-center text-gray-500">Aucun mouvement pour ces filtres.</p>}
      <div className="flex items-center justify-between"><p className="text-sm text-gray-500">{query.data?.totalElements ?? 0} mouvement(s)</p>
        <Pagination page={page} totalPages={query.data?.totalPages ?? 1} onPageChange={setPage} /></div></>}
  </Card>
}
