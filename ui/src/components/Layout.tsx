import { Link, useLocation } from 'react-router-dom'

const nav = [
  { to: '/devices', label: 'Devices' },
  { to: '/faults',  label: 'Faults' },
  { to: '/reports', label: 'Reports' },
]

export function Layout({ children }: { children: React.ReactNode }) {
  const { pathname } = useLocation()

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-6 h-14 flex items-center gap-8">
          <span className="text-indigo-600 font-semibold text-sm tracking-wide">
            Edge Triage
          </span>
          <div className="flex gap-1">
            {nav.map(({ to, label }) => (
              <Link
                key={to}
                to={to}
                className={`px-3 py-1.5 rounded text-sm font-medium transition-colors ${
                  pathname.startsWith(to)
                    ? 'bg-indigo-50 text-indigo-700'
                    : 'text-gray-500 hover:text-gray-800 hover:bg-gray-100'
                }`}
              >
                {label}
              </Link>
            ))}
          </div>
        </div>
      </nav>
      <main className="max-w-7xl mx-auto px-6 py-8">{children}</main>
    </div>
  )
}
