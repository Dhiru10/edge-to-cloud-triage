import type {
  DeviceResponse, DeviceDetailResponse,
  TelemetrySnapshotDto, FaultEventSummary,
  TriageReportSummary, TriageReportResponse,
} from '../types'

const API_KEY = import.meta.env.VITE_API_KEY ?? 'dev-api-key-change-in-prod'

async function get<T>(path: string, params?: Record<string, string>): Promise<T> {
  let url = path
  if (params && Object.keys(params).length > 0) {
    url += '?' + new URLSearchParams(params).toString()
  }
  const res = await fetch(url, { headers: { 'X-Api-Key': API_KEY } })
  if (!res.ok) throw new Error(`HTTP ${res.status} — ${path}`)
  return res.json() as Promise<T>
}

export const api = {
  getDevices: () =>
    get<DeviceResponse[]>('/api/devices'),

  getDevice: (id: string) =>
    get<DeviceDetailResponse>(`/api/devices/${id}`),

  getTelemetry: (deviceId: string, from: string, to: string) =>
    get<TelemetrySnapshotDto[]>(`/api/telemetry/${deviceId}`, { from, to, limit: '500' }),

  getFaults: (params?: Record<string, string>) =>
    get<FaultEventSummary[]>('/api/faults', { limit: '50', ...params }),

  getReports: () =>
    get<TriageReportSummary[]>('/api/reports'),

  getReport: (id: string) =>
    get<TriageReportResponse>(`/api/reports/${id}`),
}
