import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { DeviceResponse } from '../types'

function relativeTime(iso: string | null): string {
  if (!iso) return 'never'
  const mins = Math.floor((Date.now() - new Date(iso).getTime()) / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  return `${Math.floor(hrs / 24)}d ago`
}

function isOnline(iso: string | null): boolean {
  if (!iso) return false
  return Date.now() - new Date(iso).getTime() < 2 * 60 * 1000
}

export function DevicesPage() {
  const [devices, setDevices] = useState<DeviceResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.getDevices()
      .then(setDevices)
      .catch(e => setError(String(e)))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p className="text-gray-500 text-sm">Loading devices…</p>
  if (error)   return <p className="text-red-600 text-sm">Error: {error}</p>

  return (
    <div>
      <h1 className="text-lg font-semibold text-gray-900 mb-6">Devices</h1>
      {devices.length === 0 && (
        <p className="text-gray-500 text-sm">No devices registered yet. Start the edge client.</p>
      )}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {devices.map(d => (
          <div key={d.id} className="bg-white rounded-lg border border-gray-200 p-5 flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <span className="font-medium text-gray-900">{d.hostname}</span>
              <span className={`h-2 w-2 rounded-full ${isOnline(d.lastSeenAt) ? 'bg-green-500' : 'bg-gray-300'}`} />
            </div>
            <div className="text-xs text-gray-500 space-y-0.5">
              {d.osInfo && <p>{d.osInfo}</p>}
              <p>Agent {d.agentVersion ?? 'unknown'}</p>
              <p>Last seen: {relativeTime(d.lastSeenAt)}</p>
            </div>
            <Link
              to={`/devices/${d.id}/telemetry`}
              className="mt-auto text-xs text-indigo-600 hover:text-indigo-800 font-medium"
            >
              View telemetry →
            </Link>
          </div>
        ))}
      </div>
    </div>
  )
}
