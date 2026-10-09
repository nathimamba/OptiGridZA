import { useCallback, useEffect, useState, useRef } from 'react'
import { Line } from 'react-chartjs-2'
import { Chart as ChartJS, LineElement, PointElement, LinearScale, CategoryScale, Tooltip, Filler } from 'chart.js'
import { useAuth } from '../context/authContextValue'
import { apiFetch } from '../services/api'
import { toDisplayName } from '../utils/displayName'

ChartJS.register(LineElement, PointElement, LinearScale, CategoryScale, Tooltip, Filler)

const healthBadge = { GOOD: 'badge-success', WARNING: 'badge-warning', CRITICAL: 'badge-error' }

function AdminOverview() {
  const [companies, setCompanies] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    apiFetch('/api/v1/companies')
      .then(setCompanies)
      .catch(() => setError('Could not load companies.'))
  }, [])

  const activeCount = companies.filter(c => c.active).length

  return (
    <div>
      <h1 className="font-display text-2xl font-bold mb-1">Admin Overview</h1>
      <p className="text-sm text-base-content/60 mb-6">
        System-wide summary — no single company is selected for an administrator
      </p>

      {error && <div className="alert alert-error mb-6">{error}</div>}

      <div className="stats shadow w-full mb-6 grid grid-cols-2 bg-base-100">
        <div className="stat">
          <div className="stat-title">Total companies</div>
          <div className="stat-value text-primary">{companies.length}</div>
        </div>
        <div className="stat">
          <div className="stat-title">Active companies</div>
          <div className="stat-value text-success">{activeCount}</div>
        </div>
      </div>

      <div className="card bg-base-100 shadow border border-base-300">
        <div className="card-body">
          <h2 className="card-title font-display">Recent companies</h2>
          <table className="table">
            <thead><tr><th>Name</th><th>Industry</th><th>Status</th></tr></thead>
            <tbody>
              {companies.length === 0 && (
                <tr><td colSpan={3} className="text-center text-base-content/60 py-6">No companies yet.</td></tr>
              )}
              {companies.slice(0, 5).map(c => (
                <tr key={c.id}>
                  <td>{c.name}</td>
                  <td>{c.industryType || '—'}</td>
                  <td>
                    <span className={`badge ${c.active ? 'badge-success' : 'badge-ghost'}`}>
                      {c.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <a href="/companies" className="link link-primary text-sm mt-2">Manage all companies →</a>
        </div>
      </div>
    </div>
  )
}

export default function Dashboard() {
  const { user } = useAuth()

  if (user?.role === 'SYSTEM_ADMIN') {
    return <AdminOverview />
  }

  return <CompanyDashboard />
}

function CompanyDashboard() {
  const { user } = useAuth()
  const [recommendation, setRecommendation] = useState(null)
  const [battery, setBattery] = useState(null)
  const [company, setCompany] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [trend, setTrend] = useState([])
  const intervalRef = useRef(null)

  const [mode, setMode] = useState('HYBRID')
  const [action, setAction] = useState('CHARGE')
  const [kwh, setKwh] = useState(5)
  const [simLoading, setSimLoading] = useState(false)
  const [simError, setSimError] = useState('')
  const [simSuccess, setSimSuccess] = useState('')

  const load = useCallback(async () => {
    try {
      const [rec, health, comp] = await Promise.all([
        apiFetch(`/api/v1/prediction/recommend/${user.companyId}`),
        apiFetch(`/api/v1/simulation/battery/health/${user.companyId}`),
        apiFetch(`/api/v1/companies/${user.companyId}`),
      ])
      setRecommendation(rec)
      setBattery(health)
      setCompany(comp)
      setTrend(prev => [...prev, {
        t: new Date().toLocaleTimeString('en-ZA', { hour: '2-digit', minute: '2-digit' }),
        soc: health.soc,
      }].slice(-20))
      setError('')
    } catch {
      setError('Could not load dashboard data — check that a company and battery exist.')
    } finally {
      setLoading(false)
    }
  }, [user.companyId])

  useEffect(() => {
    if (!user?.companyId) return
    const initialLoad = setTimeout(load, 0)
    intervalRef.current = setInterval(load, 30000)
    return () => {
      clearTimeout(initialLoad)
      clearInterval(intervalRef.current)
    }
  }, [load, user?.companyId])

  const runSimulation = async (e) => {
    e.preventDefault()
    setSimError('')
    setSimSuccess('')
    setSimLoading(true)
    try {
      await apiFetch('/api/v1/simulation/run', {
        method: 'POST',
        body: JSON.stringify({ companyId: user.companyId, action, kwh: Number(kwh), mode }),
      })
      await load()
      setSimSuccess('Simulation applied — battery updated.')
    } catch {
      setSimError('Simulation failed — check the values and try again.')
    } finally {
      setSimLoading(false)
    }
  }

  const chartData = {
    labels: trend.map(p => p.t),
    datasets: [{
      label: 'Battery SOC (%)',
      data: trend.map(p => p.soc),
      borderColor: '#F2610C',
      backgroundColor: 'rgba(242,97,12,0.12)',
      fill: true,
      tension: 0.35,
      pointRadius: 3,
    }],
  }

  const canRunSimulation = user?.role === 'ENERGY_MANAGER' || user?.role === 'SYSTEM_ADMIN'

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="font-display text-2xl font-bold">
            {company ? company.name : 'Dashboard'}
          </h1>
          <p className="text-sm text-base-content/60">
            {company?.address ? `${company.address} · ` : ''}{toDisplayName(user?.email)}
          </p>
        </div>
        <button className="btn btn-sm btn-outline" onClick={load}>Refresh</button>
      </div>

      {error && <div className="alert alert-error mb-6">{error}</div>}

      <div className="stats shadow w-full mb-6 grid grid-cols-2 lg:grid-cols-4 bg-base-100">
        <div className="stat">
          <div className="stat-title">Battery SOC</div>
          <div className="stat-value text-primary">{battery ? `${Number(battery.soc).toFixed(1)}%` : '—'}</div>
        </div>
        <div className="stat">
          <div className="stat-title">Load-Shedding Stage</div>
          <div className="stat-value">{recommendation?.loadSheddingStage ?? '—'}</div>
        </div>
        <div className="stat">
          <div className="stat-title">Solar Forecast</div>
          <div className="stat-value text-secondary">
            {recommendation ? `${recommendation.solarForecastKwh.toFixed(1)}kWh` : '—'}
          </div>
        </div>
        <div className="stat">
          <div className="stat-title">Est. Savings</div>
          <div className="stat-value text-success">
            {recommendation ? `R${recommendation.estimatedSavingsRand.toFixed(2)}` : '—'}
          </div>
        </div>
      </div>

      <div className="grid lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 flex flex-col gap-6">
          <div className="card bg-base-100 shadow border border-base-300">
            <div className="card-body">
              <div className="flex items-center justify-between">
                <h2 className="card-title font-display">AI Recommendation</h2>
                {recommendation && (
                  <span className="badge badge-primary badge-lg">{recommendation.action}</span>
                )}
              </div>
              {loading && <span className="loading loading-spinner" />}
              {recommendation && (
                <>
                  <div className="flex gap-8 mt-2">
                    <div>
                      <p className="text-xs text-base-content/60">Confidence</p>
                      <p className="font-display text-2xl font-bold">
                        {(recommendation.confidence * 100).toFixed(0)}%
                      </p>
                    </div>
                    <div>
                      <p className="text-xs text-base-content/60">Mode</p>
                      <p className="font-display text-2xl font-bold">{recommendation.operatingMode}</p>
                    </div>
                  </div>
                  <div className="alert bg-primary/10 border border-primary/30 mt-4">
                    <span className="text-sm">{recommendation.reasoning}</span>
                  </div>
                </>
              )}

              <div className="mt-6">
                <p className="text-sm font-medium mb-2">
                  Battery SOC trend (live, last {trend.length} readings)
                </p>
                <div className="h-48">
                  <Line
                    data={chartData}
                    options={{
                      maintainAspectRatio: false,
                      plugins: { legend: { display: false } },
                      scales: { y: { min: 0, max: 100 } },
                    }}
                  />
                </div>
              </div>
            </div>
          </div>

          {canRunSimulation && (
            <div className="card bg-base-100 shadow border border-base-300">
              <div className="card-body">
                <h2 className="card-title font-display">Run Battery Simulation</h2>
                <p className="text-xs text-base-content/60 -mt-2 mb-2">
                  Manually apply a charge/discharge action and operating mode
                </p>

                <form onSubmit={runSimulation} className="grid grid-cols-2 gap-3">
                  <label className="form-control">
                    <span className="label-text text-xs mb-1">Action</span>
                    <select
                      className="select select-bordered select-sm"
                      value={action}
                      onChange={e => setAction(e.target.value)}
                    >
                      <option value="CHARGE">Charge</option>
                      <option value="DISCHARGE">Discharge</option>
                      <option value="HOLD">Hold</option>
                      <option value="SOLAR_PRIORITY">Solar Priority</option>
                    </select>
                  </label>

                  <label className="form-control">
                    <span className="label-text text-xs mb-1">Mode</span>
                    <select
                      className="select select-bordered select-sm"
                      value={mode}
                      onChange={e => setMode(e.target.value)}
                    >
                      <option value="GRID">Electricity (Grid-connected)</option>
                      <option value="HYBRID">Hybrid</option>
                      <option value="OFF_GRID">Off-grid</option>
                    </select>
                  </label>

                  <label className="form-control col-span-2">
                    <span className="label-text text-xs mb-1">kWh</span>
                    <input
                      type="number"
                      min="0"
                      step="0.5"
                      className="input input-bordered input-sm"
                      value={kwh}
                      onChange={e => setKwh(e.target.value)}
                    />
                  </label>

                  {simError && (
                    <div className="alert alert-error col-span-2 py-2 text-xs">{simError}</div>
                  )}
                  {simSuccess && (
                    <div className="alert alert-success col-span-2 py-2 text-xs">{simSuccess}</div>
                  )}

                  <button
                    type="submit"
                    className="btn btn-primary btn-sm col-span-2"
                    disabled={simLoading}
                  >
                    {simLoading
                      ? <span className="loading loading-spinner loading-xs" />
                      : 'Run Simulation'}
                  </button>
                </form>
              </div>
            </div>
          )}
        </div>

        <div className="card bg-base-100 shadow border border-base-300">
          <div className="card-body items-center text-center">
            <h2 className="card-title font-display self-start">Battery Health</h2>
            <div
              className="radial-progress text-primary my-4"
              style={{ '--value': battery?.soc || 0, '--size': '10rem', '--thickness': '10px' }}
              role="progressbar"
            >
              <span className="font-display text-3xl font-bold">
                {battery ? `${Number(battery.soc).toFixed(1)}%` : '—'}
              </span>
            </div>
            {battery && (
              <span className={`badge ${healthBadge[battery.healthStatus] || 'badge-ghost'} mb-2`}>
                {battery.healthStatus}
              </span>
            )}
            <div className="flex justify-between w-full text-sm mt-2">
              <span className="text-base-content/60">
                Cycles: <b>{battery?.cycleCount ?? '—'}</b>
              </span>
              <span className="text-base-content/60">
                Efficiency: <b>{battery ? `${battery.efficiencyPct}%` : '—'}</b>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}