import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Plus, Save, X } from 'lucide-react'
import toast from 'react-hot-toast'
import { useDebounce } from '@/hooks/useDebounce'
import { usePagination } from '@/hooks/usePagination'
import { institutionService, type InstitutionCreateRequest, type InstitutionDto } from '@/services/institutionService'
import { Table } from '@/components/ui/Table'
import { SearchBar } from '@/components/ui/SearchBar'
import { Pagination } from '@/components/ui/Pagination'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { PageLoader } from '@/components/feedback/PageLoader'
import { EmptyState } from '@/components/feedback/EmptyState'
import { formatDate } from '@/utils/formatters'

type InstitutionDraft = Pick<InstitutionCreateRequest, 'code'>
const emptyDraft = (): InstitutionDraft => ({ code: '' })

export function InstitutionPage() {
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<InstitutionDto | null>(null)
  const [draft, setDraft] = useState<InstitutionDraft>(emptyDraft)
  const debouncedSearch = useDebounce(search)
  const { page, size, setPage, reset } = usePagination()

  const { data, isLoading } = useQuery({
    queryKey: ['institutions', page, size, debouncedSearch],
    queryFn: () => institutionService.list({ page, size, search: debouncedSearch }),
  })

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload: InstitutionCreateRequest = { code: draft.code.trim(), name: draft.code.trim(), svfeId: draft.code.trim() }
      return editing ? institutionService.update(editing.id, payload) : institutionService.create(payload)
    },
    onSuccess: async () => {
      toast.success(editing ? 'Institution mise à jour.' : 'Institution ajoutée.')
      setFormOpen(false)
      setEditing(null)
      setDraft(emptyDraft())
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['institutions'] }),
        queryClient.invalidateQueries({ queryKey: ['institutions-all'] }),
        queryClient.invalidateQueries({ queryKey: ['institution-alert-routing'] }),
      ])
    },
    onError: () => toast.error('Impossible d’enregistrer l’institution. Vérifiez que son code est unique.'),
  })

  const beginCreate = () => {
    setFormOpen(true)
    setEditing(null)
    setDraft(emptyDraft())
  }
  const beginEdit = (institution: InstitutionDto) => {
    setFormOpen(true)
    setEditing(institution)
    setDraft({ code: institution.code })
  }
  const closeForm = () => {
    setFormOpen(false)
    setEditing(null)
    setDraft(emptyDraft())
  }

  const columns = [
    { key: 'code', header: 'Code', render: (r: InstitutionDto) => <span className="font-mono text-xs">{r.code}</span> },
    { key: 'name', header: 'Nom', render: (r: InstitutionDto) => r.name },
    { key: 'createdAt', header: 'Créée le', render: (r: InstitutionDto) => formatDate(r.createdAt) },
    { key: 'actions', header: 'Action', render: (r: InstitutionDto) => <Button type="button" size="sm" variant="secondary" onClick={() => beginEdit(r)}>Modifier</Button> },
  ]

  return (
    <div className="space-y-4">
      <div className="page-header flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Institutions</h1>
          <p className="mt-1 text-sm text-gray-500">Mode test : renseignez uniquement le code. Les institutions réelles seront récupérées depuis la base connectée.</p>
        </div>
        <Button icon={<Plus className="h-4 w-4" />} onClick={beginCreate}>Ajouter une institution</Button>
      </div>

      {formOpen && (
        <Card>
          <div className="flex items-center justify-between border-b border-gray-100 px-6 py-4">
            <h2 className="font-semibold text-gray-900">{editing ? `Modifier ${editing.name}` : 'Nouvelle institution'}</h2>
            <button type="button" aria-label="Fermer le formulaire" onClick={closeForm} className="rounded-lg p-2 text-gray-500 hover:bg-gray-100"><X className="h-4 w-4" /></button>
          </div>
          <form className="grid gap-4 p-6" onSubmit={(event) => { event.preventDefault(); saveMutation.mutate() }}>
            <label className="max-w-xl text-sm font-medium text-gray-700">Code institution
              <input required maxLength={50} value={draft.code} onChange={(event) => setDraft({ code: event.target.value })} placeholder="Ex. 00003" className="mt-1.5 w-full rounded-lg border border-gray-200 px-3 py-2 text-sm font-normal" />
            </label>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="ghost" onClick={closeForm}>Annuler</Button>
              <Button type="submit" icon={<Save className="h-4 w-4" />} loading={saveMutation.isPending}>{editing ? 'Enregistrer' : 'Créer'}</Button>
            </div>
          </form>
        </Card>
      )}

      <Card className="p-0">
        <div className="p-4 border-b border-gray-50">
          <SearchBar value={search} onChange={(value) => { setSearch(value); reset() }} placeholder="Rechercher une institution…" className="w-72" />
        </div>
        {isLoading ? <PageLoader /> : !data?.content?.length ? <EmptyState /> : (
          <>
            <Table columns={columns} data={data.content} keyFn={(r) => r.id} />
            <div className="flex items-center justify-between p-4 border-t border-gray-50">
              <p className="text-sm text-gray-500">{data.totalElements} institution(s)</p>
              <Pagination page={page} totalPages={data.totalPages} onPageChange={setPage} />
            </div>
          </>
        )}
      </Card>
    </div>
  )
}
