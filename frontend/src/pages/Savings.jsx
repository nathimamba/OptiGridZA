import { useEffect, useState } from 'react'
import { Bar } from 'react-chartjs-2'
import { Chart as ChartJS, BarElement, LinearScale, CategoryScale, Tooltip } from 'chart.js'
import { useCompanyScope } from '../context/companyScopeContextValue'
import { apiFetch } from '../services/api'

ChartJS.register(BarElement, LinearScale, CategoryScale, Tooltip)

export default function Savings() {
  const { activeCompanyId: companyId, isAdmin } = useCompanyScope()
  const [recommendation, setRecommendation] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!companyId) return undefined
    let active = true
    setLoading(true)
    setError('')
    apiFetch(`/api/v1/prediction/recommend/${companyId}`)
      .then(data => { if (active) setRecommendation(data) })
      .catch(err => { if (active) { setRecommendation(null); setError(`Could not load savings data: ${err.message}`) } })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [companyId])

  const daily = recommendation?.estimatedSavingsRand ?? 0
  const projected30 = daily * 30
  const action = recommendation?.action
  const noSaving = recommendation && daily <= 0

  
  const baseline = projected30 > 0 ? projected30 * 2.5 : 0
  const chartData = {
    labels: ['No optimisation', 'With OptiGrid ZA'],
    datasets: [{
      label: 'Estimated monthly cost (R)',
      data: [baseline, Math.max(0, baseline - projected30)],
      backgroundColor: ['#C0341B', '#1E7A6B'],
      borderRadius: 8,
    }],
  }

  return (
    <div>
      <h1 className="font-display text-2xl font-bold mb-1">Savings Projection</h1>
      <p className="text-sm text-base-content/60 mb-6">Based on today's recommendation, extrapolated over 30 days</p>

      {!companyId && (
        <div className="alert alert-info mb-6">
          {isAdmin
            ? 'No company selected. Create a company first, then choose it under "Viewing company".'
            : 'No company is linked to your account. Ask an administrator to assign you to a company.'}
        </div>
      )}
      {error && <div className="alert alert-error mb-6">{error}</div>}
      {loading && <div className="mb-6"><span className="loading loading-spinner loading-md" /> Loading savings…</div>}

      <div className="grid md:grid-cols-3 gap-6 mb-6">
        <div className="stat bg-base-100 rounded-box shadow border border-base-300">
          <div className="stat-title">Today's estimated saving</div>
          <div className="stat-value text-success">R{daily.toFixed(2)}</div>
        </div>
        <div className="stat bg-base-100 rounded-box shadow border border-base-300">
          <div className="stat-title">Projected 30-day saving</div>
          <div className="stat-value text-primary">R{projected30.toFixed(2)}</div>
        </div>
        <div className="stat bg-base-100 rounded-box shadow border border-base-300">
          <div className="stat-title">Current action</div>
          <div className="stat-value text-lg">{action ?? '—'}</div>
          {recommendation && (
            <div className="stat-desc">
              Battery {Number(recommendation.currentSoc ?? 0).toFixed(0)}% · stage {recommendation.loadSheddingStage ?? 0}
            </div>
          )}
        </div>
      </div>

      {noSaving && (
        <div className="alert mb-6">
          <span>
            No saving is projected right now: the recommendation is <b>{action}</b>, and savings are only
            estimated for actions that avoid peak-tariff or outage cost (for example DISCHARGE during
            load-shedding). {recommendation.reasoning}
          </span>
        </div>
      )}

      <div className="card bg-base-100 shadow border border-base-300">
        <div className="card-body">
          <h2 className="card-title font-display mb-2">Baseline vs optimised (30-day projection)</h2>
          <div className="h-64"><Bar data={chartData} options={{ maintainAspectRatio: false, plugins: { legend: { display: false } } }} /></div>
          <p className="text-xs text-base-content/50 mt-2">Projection extrapolated from today's single recommendation; not a validated forecast.</p>
        </div>
      </div>
    </div>
  )
}