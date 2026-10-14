import { useState } from 'react'
import { useForm, useFieldArray } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Edit2, Trash2, Search, Code2, ChevronDown, ChevronUp } from 'lucide-react'
import toast from 'react-hot-toast'
import { extractionTemplateService } from '@/services/extractionEngineService'
import { useDebounce } from '@/hooks/useDebounce'
import { usePagination } from '@/hooks/usePagination'
import { Modal } from '@/components/ui/Modal'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Button } from '@/components/ui/Button'
import { Card } from '@/components/ui/Card'
import { Pagination } from '@/components/ui/Pagination'
import { EmptyState } from '@/components/feedback/EmptyState'
import { formatDate } from '@/utils/formatters'
import { cn } from '@/utils/cn'
import type { ExtractionTemplateDto } from '@/types/extractions'

// ─── Schema ───────────────────────────────────────────────────────────────────

const paramSchema = z.object({
  name:         z.string().min(1, 'Requis'),
  type:         z.enum(['STRING', 'INTEGER', 'LONG', 'DECIMAL', 'DATE', 'DATETIME', 'BOOLEAN']),
  label:        z.string().min(1, 'Requis'),
  required:     z.boolean().default(false),
  defaultValue: z.string().optional(),
})

const schema = z.object({
  name:        z.string().min(1, 'Requis'),
  description: z.string().optional(),
  type:        z.enum(['SQL_QUERY']),
  content:     z.string().min(10, 'Contenu requis'),
  module:      z.string().optional(),
  active:      z.boolean().default(true),
  parameters:  z.array(paramSchema).default([]),
})
type FormValues = z.infer<typeof schema>

const paramTypeOptions = [
  { value: 'STRING',   label: 'Texte'    },
  { value: 'INTEGER',  label: 'Entier'   },
  { value: 'LONG',     label: 'Long'     },
  { value: 'DECIMAL',  label: 'Décimal'  },
  { value: 'DATE',     label: 'Date'     },
  { value: 'DATETIME', label: 'Date/Heure' },
  { value: 'BOOLEAN',  label: 'Booléen'  },
]

// ─── Component ────────────────────────────────────────────────────────────────

export function TemplatesPage() {
  const qc = useQueryClient()
  const [search, setSearch] = useState('')
  const debouncedSearch = useDebounce(search, 300)
  const { page, setPage, size } = usePagination()
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<ExtractionTemplateDto | null>(null)
  const [expandedId, setExpandedId] = useState<number | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['extraction-templates', debouncedSearch, page],
    queryFn: () => extractionTemplateService.list({ search: debouncedSearch, page, size }),
  })

  const { register, handleSubmit, control, reset, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { type: 'SQL_QUERY', active: true, parameters: [] },
  })

  const { fields, append, remove } = useFieldArray({ control, name: 'parameters' })

  const saveMutation = useMutation({
    mutationFn: (values: FormValues) =>
      editing
        ? extractionTemplateService.update(editing.id, values)
        : extractionTemplateService.create(values),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['extraction-templates'] })
      toast.success(editing ? 'Template mis à jour.' : 'Template créé.')
      closeModal()
    },
    onError: (error: unknown) => {
      const msg =
        (error as { response?: { data?: { message?: string } } })?.response?.data?.message
        ?? 'Erreur lors de la sauvegarde.'
      toast.error(msg, { duration: 6000 })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => extractionTemplateService.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['extraction-templates'] })
      toast.success('Template supprimé.')
    },
    onError: () => toast.error('Impossible de supprimer ce template.'),
  })

  function openCreate() {
    setEditing(null)
    reset({ type: 'SQL_QUERY', active: true, parameters: [] })
    setModalOpen(true)
  }

  function openEdit(t: ExtractionTemplateDto) {
    setEditing(t)
    reset({
      name: t.name,
      description: t.description ?? '',
      type: 'SQL_QUERY',
      content: t.content,
      module: t.module ?? '',
      active: t.active,
      parameters: t.parameters ?? [],
    })
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setEditing(null)
    reset()
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-gray-900">Templates d'extraction</h1>
        <Button onClick={openCreate} icon={<Plus className="w-4 h-4" />}>
          Nouveau template
        </Button>
      </div>

      <Card>
        <div className="p-4 border-b border-gray-100">
          <div className="relative max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              className="pl-9 pr-4 py-2 w-full border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/30"
              placeholder="Rechercher..."
              value={search}
              onChange={e => { setSearch(e.target.value); setPage(0) }}
            />
          </div>
        </div>

        {isLoading ? (
          <div className="p-8 text-center text-gray-400 text-sm">Chargement...</div>
        ) : !data?.content.length ? (
          <EmptyState title="Aucun template" description="Créez votre premier template d'extraction SQL." />
        ) : (
          <div className="divide-y divide-gray-100">
            {data.content.map(t => (
              <div key={t.id}>
                <div className="flex items-center justify-between px-4 py-3 hover:bg-gray-50">
                  <div className="flex items-center gap-3 min-w-0">
                    <div className="flex-shrink-0 w-8 h-8 rounded-lg bg-gold-100 flex items-center justify-center">
                      <Code2 className="w-4 h-4 text-gold-600" />
                    </div>
                    <div className="min-w-0">
                      <p className="font-medium text-gray-900 text-sm truncate">{t.name}</p>
                      <p className="text-xs text-gray-500">{t.module || 'Sans module'} · {formatDate(t.updatedAt)}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 ml-4 flex-shrink-0">
                    <span className={cn('text-xs px-2 py-0.5 rounded-full border',
                      t.active ? 'bg-green-50 text-green-700 border-green-200' : 'bg-gray-100 text-gray-500 border-gray-200')}>
                      {t.active ? 'Actif' : 'Inactif'}
                    </span>
                    <button onClick={() => setExpandedId(expandedId === t.id ? null : t.id)}
                      className="p-1.5 hover:bg-gray-100 rounded text-gray-400">
                      {expandedId === t.id ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                    </button>
                    <button onClick={() => openEdit(t)}
                      className="p-1.5 hover:bg-blue-50 rounded text-gray-400 hover:text-blue-600">
                      <Edit2 className="w-4 h-4" />
                    </button>
                    <button onClick={() => { if (confirm('Supprimer ce template ?')) deleteMutation.mutate(t.id) }}
                      className="p-1.5 hover:bg-red-50 rounded text-gray-400 hover:text-red-500">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
                {expandedId === t.id && (
                  <div className="px-4 pb-4 bg-gray-50 border-t border-gray-100">
                    {t.description && <p className="text-xs text-gray-600 mt-2 mb-2">{t.description}</p>}
                    <pre className="text-xs bg-gray-900 text-green-400 rounded-lg p-3 overflow-x-auto max-h-40">
                      {t.content}
                    </pre>
                    {t.parameters.length > 0 && (
                      <div className="mt-2">
                        <p className="text-xs font-medium text-gray-700 mb-1">Paramètres</p>
                        <div className="flex flex-wrap gap-1">
                          {t.parameters.map(p => (
                            <span key={p.name} className="text-xs bg-white border border-gray-200 rounded px-2 py-0.5">
                              <span className="font-mono text-gold-600">:{p.name}</span>
                              <span className="text-gray-400 ml-1">({p.type})</span>
                              {p.required && <span className="text-red-400 ml-1">*</span>}
                            </span>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        {data && data.totalPages > 1 && (
          <div className="p-4 border-t border-gray-100">
            <Pagination page={page} totalPages={data.totalPages} onPageChange={setPage} />
          </div>
        )}
      </Card>

      <Modal
        open={modalOpen}
        onClose={closeModal}
        title={editing ? 'Modifier le template' : 'Nouveau template'}
        size="xl"
      >
        <form onSubmit={handleSubmit(v => saveMutation.mutate(v))} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="col-span-2">
              <Input label="Nom *" {...register('name')} error={errors.name?.message} />
            </div>
            <Input label="Module" {...register('module')} />
            <Select label="Format" {...register('type')}
              options={[{ value: 'SQL_QUERY', label: 'Requête SQL' }]} />
            <div className="col-span-2">
              <Input label="Description" {...register('description')} />
            </div>
            <div className="col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Requête SQL * <span className="text-xs text-gray-400">(utilisez :paramName pour les paramètres)</span>
              </label>
              <textarea
                {...register('content')}
                rows={8}
                className={cn(
                  'w-full font-mono text-sm border rounded-lg p-3 focus:outline-none focus:ring-2 focus:ring-gold-500/30 bg-gray-900 text-green-400',
                  errors.content && 'border-red-300'
                )}
                placeholder="SELECT col1, col2 FROM table WHERE date >= :startDate"
              />
              {errors.content && <p className="text-xs text-red-500 mt-1">{errors.content.message}</p>}
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between mb-2">
              <p className="text-sm font-medium text-gray-700">Paramètres</p>
              <Button type="button" variant="ghost" size="sm"
                icon={<Plus className="w-3 h-3" />}
                onClick={() => append({ name: '', type: 'STRING', label: '', required: false, defaultValue: '' })}>
                Ajouter
              </Button>
            </div>
            {fields.length > 0 && (
              <div className="space-y-2">
                {fields.map((field, i) => (
                  <div key={field.id} className="grid grid-cols-12 gap-2 items-end bg-gray-50 rounded-lg p-2">
                    <div className="col-span-3">
                      <Input label="Clé" {...register(`parameters.${i}.name`)} placeholder=":nomParam" />
                    </div>
                    <div className="col-span-3">
                      <Input label="Libellé" {...register(`parameters.${i}.label`)} />
                    </div>
                    <div className="col-span-2">
                      <Select label="Type" {...register(`parameters.${i}.type`)} options={paramTypeOptions} />
                    </div>
                    <div className="col-span-2">
                      <Input label="Défaut" {...register(`parameters.${i}.defaultValue`)} />
                    </div>
                    <div className="col-span-1 flex items-center gap-1 pb-1">
                      <input type="checkbox" {...register(`parameters.${i}.required`)} id={`req-${i}`} />
                      <label htmlFor={`req-${i}`} className="text-xs text-gray-600">Requis</label>
                    </div>
                    <div className="col-span-1 pb-1">
                      <button type="button" onClick={() => remove(i)}
                        className="p-1 hover:bg-red-50 rounded text-gray-400 hover:text-red-500">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="flex items-center gap-2">
            <input type="checkbox" {...register('active')} id="active-tpl" />
            <label htmlFor="active-tpl" className="text-sm text-gray-700">Actif</label>
          </div>

          <div className="flex justify-end gap-2 pt-2 border-t border-gray-100">
            <Button type="button" variant="ghost" onClick={closeModal}>Annuler</Button>
            <Button type="submit" loading={saveMutation.isPending}>
              {editing ? 'Mettre à jour' : 'Créer'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
