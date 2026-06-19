import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import { StatusBadge } from '../components/StatusBadge'
import type { FaultEventSummary, DeviceResponse } from '../types'

const STATUS_TABS = ['all', 'pending', 'processing', 'done', 'failed'] as const

function fmt(iso: string): string {
  return new Date(iso).toLocaleString([], {
    month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

export function FaultsPage() {
  const [faults, setFaults]   = useState<FaultEventSummary[]>([])
  const [deviceMap, setDeviceMap] = useState<Record<string, string>>({})
  const [tab, setTab]         = useState<typeof STATUS_TABS[number]>('all')
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState<string | null>(null)

  useEffect(() => {
    api.getDevices()
      .then(devs => {
        const m: Record<string, string> = {}
        devs.forEach((d: DeviceResponse) => { m[d.id] = d.hostname })
        setDeviceMap(m)
      })
      .catch(() => null)
  }, [])

  useEffect(() => {
    setLoading(true)
    const params = tab !== 'all' ? { status: tab } : undefined
    api.getFaults(params)
      .then(setFaults)
      .catch(e => setError(String(e)))
      .finally(() => setLoading(false))
  }, [tab])

  return (
    <div>
      <h1 className="text-lg font-semibold text-gray-900 mb-5">Fault Events</h1>

      <div className="flex gap-1 mb-5 flex-wrap">
        {STATUS_TABS.map(t => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-3 py-1 text-xs rounded-full font-medium capitalize transition-colors ${
              tab === t
                ? 'bg-indigo-600 text-white'
                : 'bg-white border border-gray-200 text-gray-600 hover:bg-gray-50'
            }`}
          >
            {t}
          </button>
        ))}
      </div>

      {loading && <p className="text-gray-500 text-sm">Loading…</p>}
      {error   && <p className="text-red-600 text-sm">Error: {error}</p>}

      {!loading && faults.length === 0 && (
        <p className="text-gray-500 text-sm">No fault events found.</p>
      )}

      {!loading && faults.length > 0 && (
        <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
          <table className="min-w-full text-sm divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                {['Time', 'Device', 'Fault Type', 'Process', 'Status', ''].map(h => (
                  <th key={h} className="px-4 py-2.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {faults.map(f => (
                <tr key={f.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-600 whitespace-nowrap">{fmt(f.occurredAt)}</td>
                  <td className="px-4 py-3 font-medium text-gray-900">
                    {deviceMap[f.deviceId] ?? f.deviceId.slice(0, 8)}
                  </td>
                  <td className="px-4 py-3 font-mono text-xs text-gray-700">{f.faultType}</td>
                  <td className="px-4 py-3 text-gray-600">{f.processName ?? '—'}</td>
                  <td className="px-4 py-3"><StatusBadge value={f.status} /></td>
                  <td className="px-4 py-3">
                    {f.status === 'done' && (
                      <Link
                        to={`/reports?faultEventId=${f.id}`}
                        className="text-xs text-indigo-600 hover:text-indigo-800"
                      >
                        Report →
                      </Link>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
