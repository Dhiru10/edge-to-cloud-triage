export interface DeviceResponse {
  id: string
  hostname: string
  osInfo: string | null
  agentVersion: string | null
  registeredAt: string
  lastSeenAt: string | null
}

export interface DeviceDetailResponse extends DeviceResponse {
  latestSnapshot: TelemetrySnapshotDto | null
}

export interface TelemetrySnapshotDto {
  capturedAt: string
  cpuPct: number
  memUsedMb: number
  memTotalMb: number
  diskUsedGb: number
  diskTotalGb: number
  loadAvg1m: number
}

export interface FaultEventSummary {
  id: string
  deviceId: string
  occurredAt: string
  faultType: string
  processName: string | null
  exitCode: number | null
  status: string
  createdAt: string
}

export interface FaultEventResponse extends FaultEventSummary {
  rawLog: string | null
  processingStartedAt: string | null
}

export interface TriageReportSummary {
  id: string
  faultEventId: string
  analyzedAt: string
  rootCause: string
  confidence: string
  affectedModule: string | null
}

export interface TriageReportResponse extends TriageReportSummary {
  recommendation: string
  rawAnalysis: Record<string, unknown>
}
