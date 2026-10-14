import { Input } from './Input'

interface Props {
  startDate: string
  endDate: string
  onStartChange: (v: string) => void
  onEndChange: (v: string) => void
  label?: string
}

export function DateRangePicker({ startDate, endDate, onStartChange, onEndChange, label }: Props) {
  return (
    <div>
      {label && <p className="label-base">{label}</p>}
      <div className="flex items-center gap-2">
        <Input type="date" value={startDate} onChange={(e) => onStartChange(e.target.value)} />
        <span className="text-gray-400">→</span>
        <Input type="date" value={endDate} onChange={(e) => onEndChange(e.target.value)} />
      </div>
    </div>
  )
}
