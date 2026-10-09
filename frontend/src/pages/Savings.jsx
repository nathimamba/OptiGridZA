import { useEffect, useState } from 'react'
import { Bar } from 'react-chartjs-2'
import { Chart as ChartJS, BarElement, LinearScale, CategoryScale, Tooltip } from 'chart.js'
import { useAuth } from '../context/authContextValue'
import { apiFetch } from '../services/api'

ChartJS.register(BarElement, LinearScale, CategoryScale, Tooltip)

export default function Savings() {
  const { user } = useAuth()
  const [recommendation, setRecommendation] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user?.companyId) return
    apiFetch(`/api/v1/prediction/recommend/${user.companyId}`)
      .then(setRecommendation)
      .catch(() => setError('Could not load savings data.'))
  }, [user])

  const daily = recommendation?.estimatedSavingsRand || 0
  const projected30 = daily * 30

  const chartData = {
    labels: ['No optimisation', 'With OptiGrid ZA'],
    datasets: [{
      label: 'Estimated monthly cost (R)',
      data: [projected30 * 2.5, projected30 * 2.5 - projected30],
      backgroundColor: ['#C0341B', '#1E7A6B'],
      borderRadius: 8,
    }],
  }

  return (
    <div>
      <h1 className="font-display text-2xl font-bold mb-1">Savings Projection</h1>
      <p className="text-sm text-base-content/60 mb-6">Based on today's recommendation, extrapolated over 30 days</p>

      {error && <div className="alert alert-error mb-6">{error}</div>}

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
          <div className="stat-value text-lg">{recommendation?.action ?? '—'}</div>
        </div>
      </div>

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