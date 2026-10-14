import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Edit2, Trash2, Search, Play, Settings2, Mail, Repeat, Zap, Download, Loader2 } from 'lucide-react'
import toast from 'react-hot-toast'
import { extractionConfigService, extractionJobService, extractionTemplateService } from '@/services/extractionEngineService'
import { institutionService } from '@/services/institutionService'
import { useDebounce } from '@/hooks/useDebounce'
import { usePagination } from '@/hooks/usePagination'
import { useAuth } from '@/hooks/useAuth'
import { useInstitutionFilter } from '@/hooks/useInstitutionFilter'
import { Modal } from '@/components/ui/Modal'
import { Input } from '@/components/ui/Input'
import { Select } from '@/components/ui/Select'
import { Button } from '@/components/ui/Button'
import { CronPickerField } from '@/components/ui/CronPickerField'
import { Card } from '@/components/ui/Card'
import { Pagination } from '@/components/ui/Pagination'
import { EmptyState } from '@/components/feedback/EmptyState'
import { cn } from '@/utils/cn'
import type { ExtractionConfigDto, ExtractionJobDto, ParameterDef } from '@/types/extractions'

// ─── Schema ───────────────────────────────────────────────────────────────────

const destinataireSchema = z.object({
  email:  z.string().email('Email invalide'),
  nom:    z.string().optional(),
  active: z.boolean().default(true),
})

const schema = z.object({
  institutionId:  z.coerce.number().min(1, 'Requis'),
  templateId:     z.coerce.number().min(1, 'Requis'),
  name:           z.string().min(1, 'Requis'),
  description:    z.string().optional(),
  outputFormat:   z.enum(['CSV', 'EXCEL']),
  cronExpression: z.string().optional(),
  active:         z.boolean().default(true),
  paramValues:    z.record(z.string()).default({}),
  destinataires:  z.array(destinataireSchema).default([]),
})
type FormValues = z.infer<typeof schema>

// ─── Component ────────────────────────────────────────────────────────────────

export function ConfigurationsPage() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const { isGlobal, user } = useAuth()
  const institutionId = useInstitutionFilter()
  const [search, setSearch] = useState('')
  const debouncedSearch = useDebounce(search, 300)
  const { page, setPage, size } = usePagination()
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<ExtractionConfigDto | null>(null)
  const [destRows, setDestRows] = useState<{ email: string; nom: string; active: boolean }[]>([])
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null)

  const [triggeringId, setTriggeringId] = useState<number | null>(null)
  const [quickConfig, setQuickConfig] = useState<ExtractionConfigDto | null>(null)
  const [quickParams, setQuickParams] = useState<Record<string, string>>({})
  const [quickJob, setQuickJob] = useState<ExtractionJobDto | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['extraction-configs', debouncedSearch, page, institutionId],
    queryFn: () => extractionConfigService.list({ search: debouncedSearch, page, size }),
  })

  const { data: templates } = useQuery({
    queryKey: ['extraction-templates-all'],
    queryFn: () => extractionTemplateService.listAll(),
  })

  const { data: institutions } = useQuery({
    queryKey: ['institutions-all'],
    queryFn: () => institutionService.listAll(),
    enabled: isGlobal,
  })

  const selectedTemplate = templates?.find(t => t.id === selectedTemplateId)
  const paramDefs: ParameterDef[] = selectedTemplate?.parameters ?? []

  const { register, handleSubmit, reset, setValue, control, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { outputFormat: 'CSV', active: true, paramValues: {}, destinataires: [] },
  })

  const saveMutation = useMutation({
    mutationFn: (values: FormValues) => {
      const req = { ...values, destinataires: destRows }
      return editing
        ? extractionConfigService.update(editing.id, req)
        : extractionConfigService.create(req)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['extraction-configs'] })
      toast.success(editing ? 'Configuration mise à jour.' : 'Configuration créée.')
      closeModal()
    },
    onError: (error: unknown) => {
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
        ?? 'Erreur lors de la sauvegarde.'
      toast.error(msg, { duration: 6000 })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => extractionConfigService.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['extraction-configs'] })
      toast.success('Configuration supprimée.')
    },
    onError: (error: unknown) => {
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
        ?? 'Impossible de supprimer cette configuration.'
      toast.error(msg, { duration: 6000 })
    },
  })

  const triggerMutation = useMutation({
    mutationFn: (id: number) => {
      setTriggeringId(id)
      return extractionJobService.trigger(id)
    },
    onSuccess: () => {
      setTriggeringId(null)
      toast.success("Extraction démarrée — redirection vers l'historique.")
      navigate('/extraction/historique')
    },
    onError: (error: unknown) => {
      setTriggeringId(null)
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
        ?? 'Erreur lors du déclenchement.'
      toast.error(msg, { duration: 6000 })
    },
  })

  const quickExtractMutation = useMutation({
    mutationFn: ({ id, params }: { id: number; params: Record<string, string> }) =>
      extractionJobService.trigger(id, params),
    onSuccess: (job: ExtractionJobDto) => {
      setQuickJob(job)
    },
    onError: (error: unknown) => {
      const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
        ?? 'Erreur lors du déclenchement.'
      toast.error(msg, { duration: 6000 })
    },
  })

  function openQuickExtract(c: ExtractionConfigDto) {
    setQuickConfig(c)
    setQuickParams({ ...c.paramValues })
    setQuickJob(null)
  }

  function closeQuickExtract() {
    setQuickConfig(null)
    setQuickParams({})
    setQuickJob(null)
  }

  function openCreate() {
    setEditing(null)
    setDestRows([])
    setSelectedTemplateId(null)
    reset({
      outputFormat: 'CSV',
      active: true,
      paramValues: {},
      destinataires: [],
      // Pre-fill institution for scoped users (field is hidden but still required by Zod)
      institutionId: !isGlobal && user?.institutionId ? user.institutionId : undefined,
    })
    setModalOpen(true)
  }

  function openEdit(c: ExtractionConfigDto) {
    setEditing(c)
    setSelectedTemplateId(c.templateId)
    setDestRows(c.destinataires.map(d => ({ email: d.email, nom: d.nom ?? '', active: d.active })))
    reset({
      institutionId: c.institutionId,
      templateId: c.templateId,
      name: c.name,
      description: c.description ?? '',
      outputFormat: c.outputFormat as 'CSV' | 'EXCEL',
      cronExpression: c.cronExpression ?? '',
      active: c.active,
      paramValues: c.paramValues ?? {},
      destinataires: [],
    })
    setModalOpen(true)
  }

  function closeModal() {
    setModalOpen(false)
    setEditing(null)
    setDestRows([])
    setSelectedTemplateId(null)
    reset()
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-gray-900">Configurations d'extraction</h1>
        <Button onClick={openCreate} icon={<Plus className="w-4 h-4" />}>
          Nouvelle configuration
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
          <EmptyState title="Aucune configuration" description="Créez votre première configuration d'extraction." />
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Nom</th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Institution</th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Template</th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Format</th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Cron</th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Statut</th>
                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {data.content.map(c => (
                <tr key={c.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">{c.name}</td>
                  <td className="px-4 py-3 text-gray-600">{c.institutionName}</td>
                  <td className="px-4 py-3 text-gray-600">{c.templateName}</td>
                  <td className="px-4 py-3">
                    <span className="text-xs font-mono bg-gray-100 px-2 py-0.5 rounded">{c.outputFormat}</span>
                  </td>
                  <td className="px-4 py-3">
                    {c.cronExpression
                      ? <span className="text-xs font-mono text-purple-600 bg-purple-50 px-2 py-0.5 rounded flex items-center gap-1 w-fit">
                          <Repeat className="w-3 h-3" />{c.cronExpression}
                        </span>
                      : <span className="text-xs text-gray-400">Manuel</span>}
                  </td>
                  <td className="px-4 py-3">
                    <span className={cn('text-xs px-2 py-0.5 rounded-full border',
                      c.active ? 'bg-green-50 text-green-700 border-green-200' : 'bg-gray-100 text-gray-500 border-gray-200')}>
                      {c.active ? 'Actif' : 'Inactif'}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => { if (triggeringId === null) triggerMutation.mutate(c.id) }}
                        disabled={triggeringId === c.id}
                        className="p-1.5 hover:bg-green-50 rounded text-gray-400 hover:text-green-600 disabled:opacity-50 disabled:cursor-not-allowed"
                        title="Déclencher avec paramètres configurés"
                      >
                        {triggeringId === c.id
                          ? <Loader2 className="w-4 h-4 animate-spin text-green-600" />
                          : <Play className="w-4 h-4" />
                        }
                      </button>
                      <button onClick={() => openQuickExtract(c)}
                        className="p-1.5 hover:bg-amber-50 rounded text-gray-400 hover:text-amber-600"
                        title="Extraction urgente (paramètres personnalisés)">
                        <Zap className="w-4 h-4" />
                      </button>
                      <button onClick={() => openEdit(c)}
                        className="p-1.5 hover:bg-blue-50 rounded text-gray-400 hover:text-blue-600">
                        <Edit2 className="w-4 h-4" />
                      </button>
                      <button onClick={() => { if (confirm('Supprimer cette configuration ?')) deleteMutation.mutate(c.id) }}
                        className="p-1.5 hover:bg-red-50 rounded text-gray-400 hover:text-red-500">
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {data && data.totalPages > 1 && (
          <div className="p-4 border-t border-gray-100">
            <Pagination page={page} totalPages={data.totalPages} onPageChange={setPage} />
          </div>
        )}
      </Card>

      {/* ── Quick Extract modal ── */}
      <Modal open={quickConfig !== null} onClose={closeQuickExtract}
        title="Extraction urgente" size="md">
        {quickConfig && (
          <div className="space-y-4">
            <div className="text-sm text-gray-600">
              <span className="font-medium text-gray-900">{quickConfig.name}</span>
              <span className="mx-1.5 text-gray-400">·</span>
              <span className="font-mono text-xs text-gray-500">{quickConfig.templateName}</span>
            </div>

            {quickConfig.templateParameters.length > 0 ? (
              <div className="space-y-2">
                <p className="text-xs font-medium text-gray-500 uppercase tracking-wide">Paramètres</p>
                {quickConfig.templateParameters.map(p => (
                  <div key={p.name} className="flex items-center gap-3">
                    <label className="w-32 text-xs font-mono text-gold-600 flex-shrink-0">
                      :{p.name}
                      {p.required && <span className="text-red-400 ml-0.5">*</span>}
                    </label>
                    <input
                      className="flex-1 border border-gray-200 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/30"
                      placeholder={p.label}
                      value={quickParams[p.name] ?? ''}
                      onChange={e => setQuickParams(prev => ({ ...prev, [p.name]: e.target.value }))}
                    />
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-gray-500 bg-gray-50 rounded-lg p-3">
                Ce template n'a pas de paramètres. L'extraction sera lancée immédiatement.
              </p>
            )}

            {quickJob && (
              <div className="flex items-center gap-3 bg-green-50 border border-green-200 rounded-lg p-3 text-sm text-green-700">
                <Download className="w-4 h-4 flex-shrink-0" />
                <div>
                  <p className="font-medium">Extraction démarrée (job #{quickJob.id})</p>
                  <p className="text-xs text-green-600 mt-0.5">
                    Consultez l'historique pour télécharger le fichier une fois terminé.
                  </p>
                </div>
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2 border-t border-gray-100">
              <Button type="button" variant="ghost" onClick={closeQuickExtract}>Fermer</Button>
              {!quickJob && (
                <Button
                  type="button"
                  icon={<Zap className="w-4 h-4" />}
                  loading={quickExtractMutation.isPending}
                  onClick={() => quickExtractMutation.mutate({ id: quickConfig.id, params: quickParams })}>
                  Lancer l'extraction
                </Button>
              )}
            </div>
          </div>
        )}
      </Modal>

      <Modal open={modalOpen} onClose={closeModal}
        title={editing ? 'Modifier la configuration' : 'Nouvelle configuration'}
        size="xl">
        <form onSubmit={handleSubmit(v => saveMutation.mutate(v))} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            {isGlobal && (
              <Select label="Institution *" {...register('institutionId')}
                error={errors.institutionId?.message}
                options={(institutions ?? []).map(i => ({ value: String(i.id), label: i.name }))} />
            )}
            <Select label="Template *" {...register('templateId')}
              error={errors.templateId?.message}
              options={(templates ?? []).map(t => ({ value: String(t.id), label: t.name }))}
              onChange={e => {
                const id = Number(e.target.value)
                setValue('templateId', id)
                setSelectedTemplateId(id)
              }} />
            <div className="col-span-2">
              <Input label="Nom *" {...register('name')} error={errors.name?.message} />
            </div>
            <div className="col-span-2">
              <Input label="Description" {...register('description')} />
            </div>
            <Select label="Format de sortie" {...register('outputFormat')}
              options={[{ value: 'CSV', label: 'CSV' }, { value: 'EXCEL', label: 'Excel (.xlsx)' }]} />
          </div>

          <Controller
            control={control}
            name="cronExpression"
            render={({ field }) => (
              <CronPickerField
                value={field.value ?? ''}
                onChange={v => field.onChange(v ?? '')}
                label="Planification automatique"
              />
            )}
          />

          {paramDefs.length > 0 && (
            <div>
              <p className="text-sm font-medium text-gray-700 mb-2">
                <Settings2 className="w-4 h-4 inline mr-1" />Valeurs des paramètres
              </p>
              <div className="space-y-2 bg-gray-50 rounded-lg p-3">
                {paramDefs.map(p => (
                  <div key={p.name} className="flex items-center gap-3">
                    <label className="w-32 text-xs font-mono text-gold-600 flex-shrink-0">
                      :{p.name}
                      {p.required && <span className="text-red-400 ml-0.5">*</span>}
                    </label>
                    <div className="flex-1">
                      <Input
                        {...register(`paramValues.${p.name}`)}
                        placeholder={p.defaultValue ?? p.label}
                        className="h-8 text-sm"
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          <div>
            <div className="flex items-center justify-between mb-2">
              <p className="text-sm font-medium text-gray-700">
                <Mail className="w-4 h-4 inline mr-1" />Destinataires
              </p>
              <Button type="button" variant="ghost" size="sm" icon={<Plus className="w-3 h-3" />}
                onClick={() => setDestRows(d => [...d, { email: '', nom: '', active: true }])}>
                Ajouter
              </Button>
            </div>
            {destRows.map((row, i) => (
              <div key={i} className="flex items-center gap-2 mb-2">
                <input
                  className="flex-1 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/30"
                  placeholder="email@exemple.com"
                  value={row.email}
                  onChange={e => setDestRows(d => d.map((r, j) => j === i ? { ...r, email: e.target.value } : r))}
                />
                <input
                  className="w-32 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/30"
                  placeholder="Nom"
                  value={row.nom}
                  onChange={e => setDestRows(d => d.map((r, j) => j === i ? { ...r, nom: e.target.value } : r))}
                />
                <button type="button"
                  onClick={() => setDestRows(d => d.filter((_, j) => j !== i))}
                  className="p-1.5 hover:bg-red-50 rounded text-gray-400 hover:text-red-500">
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>

          <div className="flex items-center gap-2">
            <input type="checkbox" {...register('active')} id="active-cfg" />
            <label htmlFor="active-cfg" className="text-sm text-gray-700">Actif</label>
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
