import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import {
  Chart as ChartJS, CategoryScale, LinearScale,
  PointElement, LineElement, Tooltip, Filler,
} from 'chart.js'
import { Line } from 'react-chartjs-2'
import { api } from '../api/client'
import type { TelemetrySnapshotDto, DeviceDetailResponse } from '../types'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Filler)

const RANGES = [
  { label: '1h',  hours: 1 },
  { label: '6h',  hours: 6 },
  { label: '24h', hours: 24 },
]

function makeChart(
  snapshots: TelemetrySnapshotDto[],
  label: string,
  accessor: (s: TelemetrySnapshotDto) => number,
  color: string,
) {
  const labels = snapshots.map(s =>
    new Date(s.capturedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  )
  return {
    labels,
    datasets: [{
      label,
      data: snapshots.map(accessor),
      borderColor: color,
      backgroundColor: color + '18',
      borderWidth: 1.5,
      pointRadius: 0,
      fill: true,
      tension: 0.3,
    }],
  }
}

const CHART_OPTIONS = {
  responsive: true,
  plugins: { legend: { display: false } },
  scales: { x: { ticks: { maxTicksLimit: 8, font: { size: 10 } } }, y: { beginAtZero: true } },
}

export function TelemetryPage() {
  const { id } = useParams<{ id: string }>()
  const [device, setDevice] = useState<DeviceDetailResponse | null>(null)
  const [snapshots, setSnapshots] = useState<TelemetrySnapshotDto[]>([])
  const [range, setRange] = useState(1)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    api.getDevice(id).then(setDevice).catch(() => null)
  }, [id])

  useEffect(() => {
    if (!id) return
    setLoading(true)
    const to = new Date()
    const from = new Date(to.getTime() - range * 3600_000)
    api.getTelemetry(id, from.toISOString(), to.toISOString())
      .then(setSnapshots)
      .catch(e => setError(String(e)))
      .finally(() => setLoading(false))
  }, [id, range])

  const charts = [
    { title: 'CPU %',        data: makeChart(snapshots, 'CPU %',      s => s.cpuPct,                              '#6366f1') },
    { title: 'Memory %',     data: makeChart(snapshots, 'Memory %',   s => s.memTotalMb > 0 ? (s.memUsedMb / s.memTotalMb) * 100 : 0, '#10b981') },
    { title: 'Disk used GB', data: makeChart(snapshots, 'Disk GB',    s => s.diskUsedGb,                          '#f59e0b') },
    { title: 'Load avg 1m',  data: makeChart(snapshots, 'Load avg',   s => s.loadAvg1m,                           '#ef4444') },
  ]

  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <Link to="/devices" className="text-gray-400 hover:text-gray-600 text-sm">← Devices</Link>
        <span className="text-gray-300">/</span>
        <h1 className="text-lg font-semibold text-gray-900">
          {device?.hostname ?? id}
        </h1>
      </div>

      <div className="flex gap-1 mb-6">
        {RANGES.map(r => (
          <button
            key={r.hours}
            onClick={() => setRange(r.hours)}
            className={`px-3 py-1 text-xs rounded font-medium transition-colors ${
              range === r.hours
                ? 'bg-indigo-600 text-white'
                : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50'
            }`}
          >
            {r.label}
          </button>
        ))}
      </div>

      {loading && <p className="text-gray-500 text-sm">Loading…</p>}
      {error   && <p className="text-red-600 text-sm">Error: {error}</p>}
      {!loading && snapshots.length === 0 && (
        <p className="text-gray-500 text-sm">No telemetry in this time range.</p>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {charts.map(c => (
          <div key={c.title} className="bg-white rounded-lg border border-gray-200 p-4">
            <p className="text-xs font-medium text-gray-500 mb-3">{c.title}</p>
            <Line data={c.data} options={CHART_OPTIONS} />
          </div>
        ))}
      </div>
    </div>
  )
}
