import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeftRight, RefreshCw } from 'lucide-react'
import { PageIntro, SectionHeading } from '@/components/ui/PageIntro'
import { Card } from '@/components/ui/Card'
import { Pagination } from '@/components/ui/Pagination'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { transactionService } from '@/services/transactionService'
import { formatDate } from '@/utils/formatters'

export function TransactionsOperationsPage() {
  const institutionId = useInstitutionFilter()
  const [view, setView] = useState<'operations' | 'rates' | 'errors'>('operations')
  const [page, setPage] = useState(0)
  const [from, setFrom] = useState('')
  const [until, setUntil] = useState('')
  const [type, setType] = useState('')
  const [search, setSearch] = useState('')
  const [network, setNetwork] = useState('')
  const validPeriod = !from || !until || from < until
  const types = useQuery({ queryKey: ['transaction-types', institutionId], queryFn: transactionService.types })
  const networks = useQuery({ queryKey: ['transaction-networks', institutionId], queryFn: transactionService.networks })
  const operations = useQuery({ queryKey: ['transaction-operations', institutionId, page, from, until, type, search], enabled: view === 'operations' && validPeriod,
    queryFn: () => transactionService.operations({ page, size: 25, institutionId, from: from || undefined, until: until || undefined, type: type || undefined, search: search || undefined }) })
  const params = { page, size: 25, institutionId, network: network || undefined, from: from ? new Date(from).toISOString() : undefined, to: until ? new Date(until).toISOString() : undefined }
  const rates = useQuery({ queryKey: ['transaction-flows', params], enabled: view === 'rates' && validPeriod, queryFn: () => transactionService.rates(params) })
  const errors = useQuery({ queryKey: ['transaction-responses', params], enabled: view === 'errors' && validPeriod, queryFn: () => transactionService.errors(params) })
  const active = view === 'operations' ? operations : view === 'rates' ? rates : errors
  const empty = !active.data?.content.length
  const reset = (action: () => void) => { action(); setPage(0) }
  return <div className="space-y-5">
    <div className="page-header flex flex-wrap justify-between gap-3"><PageIntro icon={ArrowLeftRight} eyebrow="Activité monétique" title="Transactions" description="Consultez les transactions disponibles, leurs types et les réponses observées." /><button className="btn-secondary inline-flex items-center gap-2" onClick={() => void active.refetch()}><RefreshCw className="h-4 w-4" />Actualiser</button></div>
    <Card className="p-5 space-y-4"><div className="flex flex-wrap gap-2">{(['operations', 'rates', 'errors'] as const).map(value => <button key={value} className={view === value ? 'rounded-lg bg-gold-500 px-4 py-2 text-sm font-medium text-white' : 'rounded-lg bg-gray-100 px-4 py-2 text-sm text-gray-600'} onClick={() => reset(() => setView(value))}>{value === 'operations' ? 'Transactions détectées' : value === 'rates' ? 'Volumes et taux de succès' : 'Codes de réponse et erreurs'}</button>)}</div>
    <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4"><label className="grid gap-1 text-sm">Depuis<input type="datetime-local" value={from} onChange={e => reset(() => setFrom(e.target.value))} className="rounded-lg border px-3 py-2" /></label><label className="grid gap-1 text-sm">Jusqu’au (exclu)<input type="datetime-local" value={until} onChange={e => reset(() => setUntil(e.target.value))} className="rounded-lg border px-3 py-2" /></label>
    {view === 'operations' ? <><label className="grid gap-1 text-sm">Type de transaction<select value={type} onChange={e => reset(() => setType(e.target.value))} className="rounded-lg border px-3 py-2"><option value="">Tous les types</option>{types.data?.map(t => <option key={t} value={t}>{t}</option>)}</select></label><label className="grid gap-1 text-sm">Référence ou commerçant<input value={search} onChange={e => reset(() => setSearch(e.target.value))} placeholder="Rechercher…" className="rounded-lg border px-3 py-2" /></label></> : <label className="grid gap-1 text-sm">Réseau<select value={network} onChange={e => reset(() => setNetwork(e.target.value))} className="rounded-lg border px-3 py-2"><option value="">Tous les réseaux</option>{networks.data?.map(n => <option key={n} value={n}>{n}</option>)}</select></label>}</div>
    {!validPeriod && <p className="text-sm text-red-600">La fin doit être postérieure au début.</p>}
    <p className="text-xs text-gray-500">{view === 'operations' ? 'Cette vue utilise les détails de transaction associés aux détections enregistrées. Les types sont ceux fournis par la base; une même transaction peut déclencher plusieurs règles.' : 'Les relevés sont des agrégats par réseau et par période. Le backend considère le code de réponse 00 comme un succès.'}</p>
    {view !== 'operations' && institutionId != null && <p className="text-sm text-amber-700">Ces agrégats ne sont pas rattachés à une institution; ils sont disponibles dans le contexte « Toutes les institutions ».</p>}
    </Card>
    <Card className="overflow-hidden"><div className="p-5 border-b"><SectionHeading title={view === 'operations' ? 'Détails des transactions' : view === 'rates' ? 'Historique des volumes' : 'Réponses en échec'} description={`${active.data?.totalElements ?? 0} relevé(s) correspondant aux filtres`} /></div>
    {active.isError ? <p className="p-8 text-center text-red-600">Impossible de charger les données. Réessayez.</p> : active.isLoading ? <p className="p-8 text-center text-gray-500">Chargement…</p> : empty ? <p className="p-8 text-center text-gray-500">Aucune donnée pour ces filtres.</p> : <div className="overflow-x-auto"><table className="w-full text-sm"><thead className="bg-gray-50 text-left text-xs uppercase text-gray-500"><tr>{(view === 'operations' ? ['Détection', 'Référence', 'Type', 'Institution', 'Montant', 'Commerçant', 'Réponse', 'Règle / risque'] : view === 'rates' ? ['Relevé', 'Réseau', 'Acquéreur', 'Émetteur', 'Succès', 'Échecs', 'Taux de succès'] : ['Relevé', 'Réseau', 'Code', 'Libellé de réponse', 'Nombre']).map(h => <th key={h} className="px-4 py-3">{h}</th>)}</tr></thead><tbody>
    {view === 'operations' && operations.data?.content.map(t => <tr key={t.id} className="border-t"><td className="px-4 py-3 whitespace-nowrap">{formatDate(t.detectedAt)}</td><td className="px-4 py-3 font-mono">{t.reference || '—'}</td><td className="px-4 py-3">{t.transactionType || 'Non renseigné'}</td><td className="px-4 py-3">{t.institution || '—'}</td><td className="px-4 py-3 whitespace-nowrap">{t.amount == null ? '—' : `${Number(t.amount).toLocaleString('fr-FR')} ${t.currency || ''}`}</td><td className="px-4 py-3">{t.merchant || '—'}</td><td className="px-4 py-3">{t.responseCode || '—'}</td><td className="px-4 py-3">{t.ruleCode}<span className="block text-xs text-gray-500">{t.riskLevel.replace(/_/g, ' ')}</span></td></tr>)}
    {view === 'rates' && rates.data?.content.map(t => <tr key={t.id} className="border-t">{[formatDate(t.snapshotTime), t.network, t.acquirer || '—', t.issuer || '—', t.successCount, t.failureCount, t.successRate == null ? '—' : `${t.successRate.toFixed(2)} %`].map((v, i) => <td key={i} className="px-4 py-3">{v}</td>)}</tr>)}
    {view === 'errors' && errors.data?.content.map(t => <tr key={t.id} className="border-t">{[formatDate(t.snapshotTime), t.network, t.responseCode || '—', t.responseDesc || '—', t.count].map((v, i) => <td key={i} className="px-4 py-3">{v}</td>)}</tr>)}
    </tbody></table></div>}
    {active.data && active.data.totalPages > 1 && <div className="p-4 border-t"><Pagination page={page} totalPages={active.data.totalPages} onPageChange={setPage} /></div>}</Card>
  </div>
}
