import { useQuery } from '@tanstack/react-query'
import { isAxiosError } from 'axios'
import { Banknote, RefreshCw } from 'lucide-react'
import { gabCassetteService } from '@/services/gabCassetteService'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { Card } from '@/components/ui/Card'
import { formatDate, formatNumber } from '@/utils/formatters'

export function AtmCassettes({ atmId }: { atmId: number }) {
  const institutionId = useInstitutionFilter()
  const query = useQuery({
    queryKey: ['gab-cassettes', atmId, institutionId],
    queryFn: () => gabCassetteService.list({ atmId, institutionId }),
    enabled: Number.isFinite(atmId) && atmId > 0,
    refetchInterval: 30_000,
    retry: false,
  })
  const gabs = query.data ?? []
  const isTest = gabs.some(gab => gab.source === 'TEST_EXCEL')
  const count = gabs.reduce((total, gab) => total + gab.cassettes.length, 0)
  const error = isAxiosError(query.error) ? query.error.response?.data?.message : undefined
  return <Card className="p-0 overflow-hidden">
    <div className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-100 px-5 py-4">
      <div className="flex items-center gap-3"><span className="rounded-xl bg-gold-50 p-2.5 text-gold-600"><Banknote className="h-5 w-5" /></span><div><h2 className="font-semibold text-gray-900">Cassettes du GAB</h2><p className="mt-1 text-xs text-gray-500">{isTest ? 'Jeu de test BALANCE.xlsx · valeurs du fichier Excel' : 'Dernier relevé de chaque cassette dans Oracle'}{!query.isPending && !query.isError ? ` · ${count} cassette(s)` : ''}</p></div></div>
      <button type="button" onClick={() => query.refetch()} disabled={query.isFetching} className="flex items-center gap-2 rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-600 disabled:opacity-50"><RefreshCw className={`h-4 w-4 ${query.isFetching ? 'animate-spin' : ''}`} />Actualiser</button>
    </div>
    {query.isPending ? <p role="status" className="p-8 text-center text-sm text-gray-500">Chargement des cassettes…</p>
      : query.isError ? <div role="alert" className="m-5 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error ?? 'Impossible de charger les cassettes. Vérifiez la connexion puis réessayez.'}</div>
      : !count ? <p className="p-8 text-center text-sm text-gray-500">Aucun relevé de cassette disponible pour ce GAB.</p>
      : gabs.map(gab => <div key={gab.codeGab}>
        <div className="bg-gray-50/60 px-5 py-3"><h3 className="text-sm font-semibold text-gray-800">{gab.nomGab ?? 'GAB'}</h3><p className="mt-1 text-xs text-gray-500">Code GAB : {gab.codeGab} · Numéro : {gab.numeroGab ?? '—'}</p></div>
        <div className="overflow-x-auto"><table className="w-full min-w-[950px] text-sm"><thead className="bg-gray-50"><tr>{['Cassette n°', 'Devise (code)', 'Valeur du billet', 'Billets restants', 'Rejetés', 'Dispensés', 'Solde cassette', 'Dernière mise à jour'].map((label, index) => <th key={label} className={`px-4 py-3 ${index >= 2 && index <= 6 ? 'text-right' : 'text-left'}`}>{label}</th>)}</tr></thead>
          <tbody>{gab.cassettes.map((cassette, index) => <tr key={`${cassette.cassetteNum}-${index}`} className="border-t border-gray-100"><td className="px-4 py-4 font-semibold">{cassette.cassetteNum ?? '—'}</td><td className="px-4 py-4">{cassette.devise ?? '—'}</td>{[cassette.valeurBillet, cassette.billetsRestants, cassette.billetsRejetes, cassette.billetsDispenses, cassette.soldeCassette].map((value, column) => <td key={column} className={`px-4 py-4 text-right tabular-nums ${column === 4 ? 'font-semibold' : ''}`}>{formatNumber(value)}</td>)}<td className="whitespace-nowrap px-4 py-4 text-gray-500">{formatDate(cassette.derniereMaj, 'dd/MM/yyyy HH:mm:ss')}</td></tr>)}</tbody></table></div>
      </div>)}
  </Card>
}
