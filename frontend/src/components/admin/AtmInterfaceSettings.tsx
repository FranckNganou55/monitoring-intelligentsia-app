import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { MonitorOff, Unplug, Save } from 'lucide-react'
import toast from 'react-hot-toast'
import api from '@/services/api'
import { settingsService } from '@/services/settingsService'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'

type Status = 'DOWN' | 'SIGNOFF'
function StatusSettings({ status }: { status: Status }) {
  const client = useQueryClient()
  const query = useQuery({ queryKey: ['settings', 'ALERTES'], queryFn: () => settingsService.getByGroupe('ALERTES') })
  const [samples, setSamples] = useState(5)
  const [intervalMs, setIntervalMs] = useState(180000)
  const prefix = `alerts.atm.interface.${status}`
  const savedSamples = Number(query.data?.find(s => s.cle === `${prefix}.consecutive.samples`)?.valeur ?? 5)
  const savedInterval = Number(query.data?.find(s => s.cle === `${prefix}.interval.ms`)?.valeur ?? 180000)
  useEffect(() => { setSamples(savedSamples); setIntervalMs(savedInterval) }, [savedSamples, savedInterval])
  const save = useMutation({ mutationFn: () => api.put(`/settings/atm-interface/${status}`, { samples, intervalMs }),
    onSuccess: () => { toast.success(`Paramètres ${status} appliqués.`); void client.invalidateQueries({ queryKey: ['settings', 'ALERTES'] }) },
    onError: () => toast.error(`Impossible d’enregistrer les paramètres ${status}.`) })
  const Icon = status === 'DOWN' ? MonitorOff : Unplug
  return <Card className="p-5 space-y-4">
    <div className="flex items-center gap-3"><span className={status === 'DOWN' ? 'rounded-xl bg-red-50 p-3 text-red-600' : 'rounded-xl bg-amber-50 p-3 text-amber-600'}><Icon className="h-5 w-5" /></span><div><h3 className="font-semibold">Règle {status} ({status === 'DOWN' ? '0' : '3'})</h3><p className="text-sm text-gray-500">{status === 'DOWN' ? 'Liaison coupée avec le GAB.' : 'GAB déconnecté logiquement.'}</p></div></div>
    {query.isError ? <p className="text-red-600 text-sm">Impossible de charger ces paramètres.</p> : <>
    <label className="grid gap-2 text-sm">Relevés consécutifs avant alerte<input type="number" min={1} max={20} value={samples} disabled={query.isLoading || save.isPending} onChange={e => setSamples(Number(e.target.value))} className="rounded-lg border px-3 py-2" /></label>
    <label className="grid gap-2 text-sm">Fréquence des relevés<select value={intervalMs} disabled={query.isLoading || save.isPending} onChange={e => setIntervalMs(Number(e.target.value))} className="rounded-lg border px-3 py-2">{Array.from({ length: 15 }, (_, i) => (i + 1) * 60000).map(value => <option key={value} value={value}>{value / 60000} minute(s)</option>)}</select></label>
    <p className="text-xs text-gray-500">Une série contient uniquement le statut {status}. Un changement de statut recommence la série. Ces réglages ne modifient pas l’autre règle.</p>
    <Button icon={<Save className="h-4 w-4" />} loading={save.isPending} disabled={query.isLoading || samples < 1 || samples > 20 || !Number.isInteger(samples) || (samples === savedSamples && intervalMs === savedInterval)} onClick={() => save.mutate()}>Valider {status}</Button></>}
  </Card>
}
export function AtmInterfaceSettings() { return <section className="space-y-3"><h2 className="font-semibold">Surveillance des interfaces GAB</h2><div className="grid gap-4 xl:grid-cols-2"><StatusSettings status="DOWN" /><StatusSettings status="SIGNOFF" /></div></section> }
