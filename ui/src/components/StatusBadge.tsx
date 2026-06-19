const palette: Record<string, string> = {
  pending:    'bg-yellow-100 text-yellow-800',
  processing: 'bg-blue-100  text-blue-800',
  done:       'bg-green-100 text-green-800',
  failed:     'bg-red-100   text-red-800',
  high:       'bg-green-100 text-green-800',
  medium:     'bg-yellow-100 text-yellow-800',
  low:        'bg-red-100   text-red-800',
}

export function StatusBadge({ value }: { value: string }) {
  const cls = palette[value.toLowerCase()] ?? 'bg-gray-100 text-gray-700'
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${cls}`}>
      {value}
    </span>
  )
}
