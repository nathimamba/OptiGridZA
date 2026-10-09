import { useEffect, useState, useCallback } from 'react'
import { API_BASE_URL } from '../services/api'

const SERVICES = [
  { key: 'auth',         label: 'Auth Service',         port: 8081, path: '/api/v1/health/auth' },
  { key: 'company',      label: 'Company Service',      port: 8082, path: '/api/v1/health/company' },
  { key: 'etl',          label: 'ETL Service',          port: 8083, path: '/api/v1/health/etl' },
  { key: 'prediction',   label: 'Prediction Service',   port: 8084, path: '/api/v1/health/prediction' },
  { key: 'simulation',   label: 'Simulation Service',   port: 8085, path: '/api/v1/health/simulation' },
  { key: 'notification', label: 'Notification Service', port: 8086, path: '/api/v1/health/notification' },
]

const STATUS_BADGE = {
  UP:      'badge-success',
  DOWN:    'badge-error',
  UNKNOWN: 'badge-warning',
  LOADING: 'badge-ghost',
}

const STATUS_ICON = { UP: '✓', DOWN: '✕', UNKNOWN: '?', LOADING: '…' }

export default function SystemHealth() {
  const [statuses, setStatuses] = useState(
    Object.fromEntries(SERVICES.map(s => [s.key, { status: 'LOADING', latency: null }]))
  )
  const [lastChecked, setLastChecked] = useState(null)

  const checkAll = useCallback(async () => {
    const token = localStorage.getItem('token')
    const results = await Promise.all(
      SERVICES.map(async (svc) => {
        const start = performance.now()
        try {
          const res = await fetch(`${API_BASE_URL}${svc.path}`, {
            headers: token ? { Authorization: `Bearer ${token}` } : {},
          })
          const latency = Math.round(performance.now() - start)
          if (res.ok) {
            const body = await res.json().catch(() => ({}))
            return { key: svc.key, status: body.status === 'UP' ? 'UP' : 'UP', latency }
          }
          return { key: svc.key, status: 'DOWN', latency }
        } catch {
          return { key: svc.key, status: 'DOWN', latency: Math.round(performance.now() - start) }
        }
      })
    )
    setStatuses(Object.fromEntries(results.map(r => [r.key, { status: r.status, latency: r.latency }])))
    setLastChecked(new Date())
  }, [])

  useEffect(() => {
    const initialCheck = setTimeout(checkAll, 0)
    const id = setInterval(checkAll, 30000)
    return () => {
      clearTimeout(initialCheck)
      clearInterval(id)
    }
  }, [checkAll])

  const upCount = Object.values(statuses).filter(s => s.status === 'UP').length
  const allUp = upCount === SERVICES.length

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="font-display text-2xl font-bold">System Health</h1>
          <p className="text-sm text-base-content/60">
            Live status of all microservices
            {lastChecked && ` · last checked ${lastChecked.toLocaleTimeString('en-ZA')}`}
          </p>
        </div>
        <button className="btn btn-sm btn-outline" onClick={checkAll}>Refresh</button>
      </div>

      <div className="stats shadow w-full mb-6 grid grid-cols-3 bg-base-100">
        <div className="stat">
          <div className="stat-title">Services Up</div>
          <div className={`stat-value ${allUp ? 'text-success' : 'text-warning'}`}>
            {upCount} / {SERVICES.length}
          </div>
        </div>
        <div className="stat">
          <div className="stat-title">Overall Status</div>
          <div className={`stat-value ${allUp ? 'text-success' : 'text-error'}`}>
            {allUp ? 'Healthy' : 'Degraded'}
          </div>
        </div>
        <div className="stat">
          <div className="stat-title">Avg Latency</div>
          <div className="stat-value text-primary">
            {(() => {
              const latencies = Object.values(statuses).filter(s => s.latency != null).map(s => s.latency)
              return latencies.length ? `${Math.round(latencies.reduce((a, b) => a + b, 0) / latencies.length)}ms` : '—'
            })()}
          </div>
        </div>
      </div>

      <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
        {SERVICES.map(svc => {
          const s = statuses[svc.key]
          return (
            <div key={svc.key} className="card bg-base-100 shadow border border-base-300">
              <div className="card-body py-5">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="font-display font-bold">{svc.label}</h3>
                    <p className="text-xs text-base-content/60">Port {svc.port}</p>
                  </div>
                  <span className={`badge ${STATUS_BADGE[s.status]} gap-1`}>
                    {STATUS_ICON[s.status]} {s.status}
                  </span>
                </div>
                {s.latency != null && (
                  <div className="mt-2">
                    <p className="text-xs text-base-content/60">
                      Response time: <span className="font-medium">{s.latency}ms</span>
                    </p>
                    <progress
                      className={`progress w-full mt-1 ${s.latency < 200 ? 'progress-success' : s.latency < 500 ? 'progress-warning' : 'progress-error'}`}
                      value={Math.min(s.latency, 1000)}
                      max="1000"
                    />
                  </div>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
