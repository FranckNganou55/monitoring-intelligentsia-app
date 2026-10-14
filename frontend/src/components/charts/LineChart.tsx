import {
  LineChart as ReLineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts'

interface Series {
  key: string
  label: string
  color?: string
}

interface Props {
  data: Record<string, unknown>[]
  series: Series[]
  xKey: string
  height?: number
}

const defaultColors = ['#C9A84C', '#22C55E', '#EF4444', '#F59E0B', '#6B7280']

export function LineChart({ data, series, xKey, height = 280 }: Props) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <ReLineChart data={data} margin={{ top: 4, right: 8, left: 0, bottom: 4 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#F3F4F6" />
        <XAxis dataKey={xKey} tick={{ fontSize: 11, fill: '#6B7280' }} />
        <YAxis tick={{ fontSize: 11, fill: '#6B7280' }} />
        <Tooltip contentStyle={{ fontSize: 12 }} />
        <Legend wrapperStyle={{ fontSize: 12 }} />
        {series.map((s, i) => (
          <Line
            key={s.key}
            type="monotone"
            dataKey={s.key}
            name={s.label}
            stroke={s.color ?? defaultColors[i % defaultColors.length]}
            dot={false}
            strokeWidth={2}
          />
        ))}
      </ReLineChart>
    </ResponsiveContainer>
  )
}
