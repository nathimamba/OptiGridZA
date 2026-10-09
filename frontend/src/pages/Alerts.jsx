import { useEffect, useState } from 'react'
import { useAuth } from '../context/authContextValue'
import { apiFetch } from '../services/api'

const sevBadge = { INFO: 'badge-info', WARNING: 'badge-warning', CRITICAL: 'badge-error' }

export default function Alerts() {
  const { user } = useAuth()
  const [alerts, setAlerts] = useState([])
  const [error, setError] = useState('')

  const load = () => {
    if (!user?.companyId) return
    apiFetch(`/api/v1/notifications/alerts/${user.companyId}`)
      .then(setAlerts)
      .catch(() => setError('Could not load alerts.'))
  }

  useEffect(load, [user])

  const acknowledge = async (id) => {
    try {
      await apiFetch(`/api/v1/notifications/alerts/${id}/acknowledge`, { method: 'PUT' })
      load()
    } catch { setError('Could not acknowledge alert.') }
  }

  return (
    <div>
      <h1 className="font-display text-2xl font-bold mb-1">Alerts</h1>
      <p className="text-sm text-base-content/60 mb-6">Low-SOC and system alerts for your company</p>

      {error && <div className="alert alert-error mb-6">{error}</div>}

      <div className="card bg-base-100 shadow border border-base-300">
        <div className="card-body">
          {alerts.length === 0 && <p className="text-base-content/60 text-center py-8">No alerts — everything looks fine.</p>}
          {alerts.map(a => (
            <div key={a.id} className="flex items-center gap-4 py-3 border-t border-base-200 first:border-t-0">
              <span className={`badge ${sevBadge[a.severity] || 'badge-ghost'}`}>{a.severity}</span>
              <div className="flex-1">
                <p className="font-medium text-sm">{a.alertType}</p>
                <p className="text-xs text-base-content/60">{a.message}</p>
              </div>
              {!a.acknowledged ? (
                <button className="btn btn-xs btn-outline" onClick={() => acknowledge(a.id)}>Acknowledge</button>
              ) : (
                <span className="badge badge-ghost">Acknowledged</span>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}