import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { ShieldCheck, Plus, X } from 'lucide-react'
import { roleService, type RoleOption } from '@/services/roleService'
import { useAuth } from '@/hooks/useAuth'
import { Card } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { PageLoader } from '@/components/feedback/PageLoader'

export function RolePage() {
  const { isAdmin } = useAuth()
  const client = useQueryClient()
  const roles = useQuery({ queryKey: ['roles'], queryFn: roleService.list })
  const catalogue = useQuery({ queryKey: ['permission-catalogue'], queryFn: roleService.permissions, enabled: isAdmin })
  const [editor, setEditor] = useState<RoleOption | 'new' | null>(null)
  const [code, setCode] = useState('ROLE_')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [selected, setSelected] = useState<string[]>([])
  const [message, setMessage] = useState('')
  const save = useMutation({
    mutationFn: () => editor === 'new' ? roleService.create({ code, name, description, permissions: selected }) : roleService.updatePermissions((editor as RoleOption).id, selected),
    onSuccess: () => { setEditor(null); setMessage('Les droits ont été enregistrés et sont appliqués aux sessions connectées.'); void client.invalidateQueries({ queryKey: ['roles'] }) },
  })
  const open = (role: RoleOption | 'new') => {
    save.reset(); setMessage(''); setEditor(role)
    setSelected(role === 'new' ? [] : [...role.permissions])
    setCode('ROLE_'); setName(''); setDescription('')
  }
  const groups = [...new Set(catalogue.data?.map(p => p.resource) ?? [])].sort()
  if (roles.isLoading) return <PageLoader />
  return <div className="space-y-5">
    <div className="page-header flex flex-wrap items-center justify-between gap-4"><div><p className="page-eyebrow">Administration</p><h1 className="page-heading">Rôles & Permissions</h1><p className="page-description">Gérez les droits d’accès. Les changements s’appliquent aussi aux utilisateurs connectés.</p></div>{isAdmin && <button className="btn-primary inline-flex items-center gap-2" onClick={() => open('new')}><Plus size={18} />Ajouter un rôle</button>}</div>
    {message && <p role="status" className="rounded-xl border border-green-200 bg-green-50 p-4 text-green-800">{message}</p>}
    {roles.isError && <div role="alert" className="rounded-xl bg-red-50 p-4 text-red-800">Impossible de charger les rôles. <button onClick={() => void roles.refetch()}>Réessayer</button></div>}
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{roles.data?.map(role => <Card key={role.id} className="self-start">
      <div className="mb-4 flex justify-between"><ShieldCheck className="text-gold-600" /><Badge>{role.permissions.length} permissions</Badge></div>
      <h2 className="text-lg font-semibold">{role.name}</h2><p className="mt-1 text-xs font-mono text-gray-400">{role.code}</p><p className="mt-3 text-sm text-gray-500">{role.description}</p>
      <details className="mt-5 border-t pt-4"><summary className="cursor-pointer font-medium">Consulter les permissions</summary><div className="mt-3 flex flex-wrap gap-2">{[...role.permissions].sort().map(p => <Badge key={p}>{p}</Badge>)}{role.permissions.length === 0 && <p>Aucune permission.</p>}</div></details>
      {isAdmin && (role.code === 'ROLE_ADMIN' ? <p className="mt-4 text-xs text-gray-500">Rôle protégé : accès administrateur conservé.</p> : <button className="btn-secondary mt-4" onClick={() => open(role)}>Modifier les permissions</button>)}
    </Card>)}</div>
    {editor && <div className="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/60 p-4"><section role="dialog" aria-modal="true" aria-labelledby="role-editor-title" className="flex max-h-[90vh] w-full max-w-3xl flex-col rounded-2xl bg-white shadow-xl">
      <div className="flex items-center justify-between border-b p-5"><h2 id="role-editor-title" className="text-xl font-semibold">{editor === 'new' ? 'Ajouter un rôle' : `Permissions : ${editor.name}`}</h2><button aria-label="Fermer" disabled={save.isPending} onClick={() => setEditor(null)}><X /></button></div>
      <form className="overflow-y-auto p-5 space-y-5" onSubmit={e => { e.preventDefault(); save.mutate() }}>
        {editor === 'new' && <div className="grid gap-4 sm:grid-cols-2"><label>Nom du rôle<input required maxLength={100} className="mt-1 w-full rounded-lg border p-3" value={name} onChange={e => setName(e.target.value)} /></label><label>Code unique<input required pattern="ROLE_[A-Z][A-Z0-9_]*" maxLength={50} className="mt-1 w-full rounded-lg border p-3" value={code} onChange={e => setCode(e.target.value.toUpperCase())} /><span className="text-xs text-gray-500">Exemple : ROLE_SUPERVISEUR</span></label><label className="sm:col-span-2">Description<textarea maxLength={255} className="mt-1 w-full rounded-lg border p-3" value={description} onChange={e => setDescription(e.target.value)} /></label></div>}
        <p className="text-sm text-gray-500">{selected.length} permissions sélectionnées. Les droits de plusieurs rôles se cumulent ; le périmètre institutionnel reste applicable. Attribuez ensuite le rôle depuis la page Utilisateurs.</p>
        {catalogue.isLoading && <p>Chargement des permissions…</p>}{catalogue.isError && <p role="alert" className="text-red-700">Impossible de charger les permissions. <button type="button" onClick={() => void catalogue.refetch()}>Réessayer</button></p>}
        {groups.map(group => <fieldset key={group} className="rounded-xl border p-4"><legend className="px-2 font-semibold text-gold-700">{group}</legend><div className="grid gap-3 sm:grid-cols-2">{catalogue.data?.filter(p => p.resource === group).sort((a,b) => a.code.localeCompare(b.code)).map(p => <label key={p.code} className="flex items-start gap-3 text-sm"><input type="checkbox" className="mt-1" checked={selected.includes(p.code)} onChange={e => setSelected(old => e.target.checked ? [...old, p.code] : old.filter(c => c !== p.code))} /><span>{p.name}<span className="block text-xs text-gray-400">{p.code}</span></span></label>)}</div></fieldset>)}
        {save.isError && <p role="alert" className="text-red-700">{axios.isAxiosError(save.error) ? save.error.response?.data?.message ?? 'Enregistrement impossible.' : 'Enregistrement impossible.'}</p>}
        <div className="flex justify-end gap-3 border-t pt-4"><button type="button" className="btn-secondary" disabled={save.isPending} onClick={() => setEditor(null)}>Annuler</button><button className="btn-primary" disabled={save.isPending || !catalogue.data}>{save.isPending ? 'Enregistrement…' : 'Enregistrer'}</button></div>
      </form>
    </section></div>}
  </div>
}
