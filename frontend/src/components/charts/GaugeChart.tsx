interface Props {
  value: number
  max: number
  threshold?: number
  label?: string
  size?: number
}

export function GaugeChart({ value, max, threshold, label, size = 80 }: Props) {
  const pct = Math.min(value / max, 1)
  const color = threshold && value >= threshold ? '#EF4444' : pct > 0.7 ? '#F59E0B' : '#22C55E'
  const r = (size / 2) * 0.8
  const cx = size / 2
  const cy = size / 2
  const circumference = Math.PI * r
  const offset = circumference * (1 - pct)

  return (
    <div className="flex flex-col items-center gap-1">
      <svg width={size} height={size / 2 + 10} viewBox={`0 0 ${size} ${size / 2 + 10}`}>
        <path
          d={`M ${cx - r} ${cy} A ${r} ${r} 0 0 1 ${cx + r} ${cy}`}
          fill="none"
          stroke="#E5E7EB"
          strokeWidth={8}
          strokeLinecap="round"
        />
        <path
          d={`M ${cx - r} ${cy} A ${r} ${r} 0 0 1 ${cx + r} ${cy}`}
          fill="none"
          stroke={color}
          strokeWidth={8}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={offset}
        />
        <text x={cx} y={cy + 4} textAnchor="middle" fontSize={12} fontWeight={600} fill="#2C2C2C">
          {Math.round(pct * 100)}%
        </text>
      </svg>
      {label && <p className="text-xs text-gray-600 text-center">{label}</p>}
    </div>
  )
}
