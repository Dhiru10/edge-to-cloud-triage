import { Routes, Route, Navigate } from 'react-router-dom'
import { Layout } from './components/Layout'
import { DevicesPage }  from './pages/DevicesPage'
import { TelemetryPage } from './pages/TelemetryPage'
import { FaultsPage }   from './pages/FaultsPage'
import { ReportsPage }  from './pages/ReportsPage'

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/"                          element={<Navigate to="/devices" replace />} />
        <Route path="/devices"                   element={<DevicesPage />} />
        <Route path="/devices/:id/telemetry"     element={<TelemetryPage />} />
        <Route path="/faults"                    element={<FaultsPage />} />
        <Route path="/reports"                   element={<ReportsPage />} />
      </Routes>
    </Layout>
  )
}
