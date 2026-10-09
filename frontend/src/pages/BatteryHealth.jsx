import { useEffect, useState } from 'react'
import { useAuth } from '../context/authContextValue'
import { apiFetch } from '../services/api'

const healthBadge = { GOOD: 'badge-success', WARNING: 'badge-warning', CRITICAL: 'badge-error' }

export default function BatteryHealth() {
  const { user } = useAuth()
  const [battery, setBattery] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user?.companyId) return
    apiFetch(`/api/v1/simulation/battery/health/${user.companyId}`)
      .then(setBattery)
      .catch(() => setError('Could not load battery data.'))
  }, [user])

  return (
    <div>
      <h1 className="font-display text-2xl font-bold mb-1">Battery Health</h1>
      <p className="text-sm text-base-content/60 mb-6">Virtual battery simulation detail</p>

      {error && <div className="alert alert-error mb-6">{error}</div>}

      {battery && (
        <div className="grid md:grid-cols-2 gap-6">
          <div className="card bg-base-100 shadow border border-base-300">
            <div className="card-body items-center">
              <div className="radial-progress text-primary" style={{ '--value': battery.soc, '--size': '12rem', '--thickness': '12px' }} role="progressbar">
                <span className="font-display text-4xl font-bold">{Number(battery.soc).toFixed(1)}%</span>
              </div>
              <span className={`badge ${healthBadge[battery.healthStatus] || 'badge-ghost'} badge-lg mt-3`}>{battery.healthStatus}</span>
            </div>
          </div>

          <div className="card bg-base-100 shadow border border-base-300">
            <div className="card-body">
              <h2 className="card-title font-display">Metrics</h2>
              <table className="table">
                <tbody>
                  <tr><td className="text-base-content/60">State of charge</td><td className="text-right font-bold">{Number(battery.soc).toFixed(1)}%</td></tr>
                  <tr><td className="text-base-content/60">Cycle count</td><td className="text-right font-bold">{battery.cycleCount}</td></tr>
                  <tr><td className="text-base-content/60">Efficiency</td><td className="text-right font-bold">{battery.efficiencyPct}%</td></tr>
                  <tr><td className="text-base-content/60">Health status</td><td className="text-right font-bold">{battery.healthStatus}</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}