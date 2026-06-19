import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { StatusBadge } from '../components/StatusBadge'
import type { TriageReportSummary, TriageReportResponse } from '../types'

function fmt(iso: string): string {
  return new Date(iso).toLocaleString([], {
    month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

function ReportRow({ summary }: { summary: TriageReportSummary }) {
  const [expanded, setExpanded] = useState(false)
  const [detail, setDetail] = useState<TriageReportResponse | null>(null)
  const [loading, setLoading] = useState(false)

  function toggle() {
    setExpanded(e => !e)
    if (!detail && !loading) {
      setLoading(true)
      api.getReport(summary.id)
        .then(setDetail)
        .finally(() => setLoading(false))
    }
  }

  return (
    <>
      <tr
        className="hover:bg-gray-50 cursor-pointer"
        onClick={toggle}
      >
        <td className="px-4 py-3 text-gray-500 whitespace-nowrap text-xs">{fmt(summary.analyzedAt)}</td>
        <td className="px-4 py-3 text-gray-700 max-w-xs truncate">{summary.rootCause}</td>
        <td className="px-4 py-3"><StatusBadge value={summary.confidence} /></td>
        <td className="px-4 py-3 text-gray-500 text-xs">{summary.affectedModule ?? '—'}</td>
        <td className="px-4 py-3 text-indigo-400 text-xs">{expanded ? '▲' : '▼'}</td>
      </tr>
      {expanded && (
        <tr className="bg-indigo-50">
          <td colSpan={5} className="px-4 py-4">
            {loading && <p className="text-xs text-gray-500">Loading…</p>}
            {detail && (
              <div className="space-y-3 text-sm">
                <div>
                  <p className="text-xs font-medium text-gray-500 uppercase mb-1">Root cause</p>
                  <p className="text-gray-800">{detail.rootCause}</p>
                </div>
                <div>
                  <p className="text-xs font-medium text-gray-500 uppercase mb-1">Recommendation</p>
                  <p className="text-gray-700">{detail.recommendation}</p>
                </div>
                <div>
                  <p className="text-xs font-medium text-gray-500 uppercase mb-1">Raw analysis</p>
                  <pre className="text-xs bg-white border border-gray-200 rounded p-3 overflow-x-auto text-gray-700">
                    {JSON.stringify(detail.rawAnalysis, null, 2)}
                  </pre>
                </div>
              </div>
            )}
          </td>
        </tr>
      )}
    </>
  )
}

export function ReportsPage() {
  const [reports, setReports] = useState<TriageReportSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState<string | null>(null)

  useEffect(() => {
    api.getReports()
      .then(setReports)
      .catch(e => setError(String(e)))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div>
      <h1 className="text-lg font-semibold text-gray-900 mb-5">Triage Reports</h1>

      {loading && <p className="text-gray-500 text-sm">Loading…</p>}
      {error   && <p className="text-red-600 text-sm">Error: {error}</p>}

      {!loading && reports.length === 0 && (
        <p className="text-gray-500 text-sm">No reports yet. Fault events are processed by the triage agent.</p>
      )}

      {!loading && reports.length > 0 && (
        <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
          <table className="min-w-full text-sm divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                {['Analyzed', 'Root Cause', 'Confidence', 'Module', ''].map(h => (
                  <th key={h} className="px-4 py-2.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {reports.map(r => <ReportRow key={r.id} summary={r} />)}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
