import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/authContextValue'
import { jwtDecode } from 'jwt-decode'
import { API_BASE_URL } from '../services/api'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { setUser } = useAuth()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await fetch(`${API_BASE_URL}/api/v1/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      })
      if (!res.ok) throw new Error('Invalid email or password')
      const data = await res.json()
      localStorage.setItem('token', data.token)
      const decoded = jwtDecode(data.token)
      setUser({ email: decoded.sub, role: decoded.role, companyId: decoded.companyId, firstName: decoded.firstName, lastName: decoded.lastName, token: data.token })
      navigate('/dashboard')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen grid lg:grid-cols-2 bg-base-200">
      <div className="hidden lg:flex flex-col justify-end p-14 bg-base-100 border-r border-base-300 relative overflow-hidden">
        <div className="absolute -top-24 -right-24 w-96 h-96 rounded-full bg-primary/20 blur-3xl" />
        <div className="flex items-center gap-3 mb-10">
          <div className="w-10 h-10 rounded-xl bg-primary text-primary-content grid place-items-center font-bold text-lg">⚡</div>
          <div>
            <p className="font-display font-bold">OptiGrid</p>
            <p className="text-xs text-base-content/60">Energy optimisation for SA business</p>
          </div>
        </div>
        <h1 className="font-display text-4xl font-semibold leading-tight max-w-md">
          Cut your electricity costs, even through load-shedding.
        </h1>
        <p className="mt-4 text-base-content/70 max-w-md">
          AI-driven scheduling, virtual battery simulation, and real-time grid awareness — built for South African SMEs.
        </p>
        <div className="flex gap-8 mt-10">
          <div><p className="font-display text-2xl font-bold text-primary">15–30%</p><p className="text-xs text-base-content/60">simulated savings</p></div>
          <div><p className="font-display text-2xl font-bold text-primary">5</p><p className="text-xs text-base-content/60">role-based views</p></div>
          <div><p className="font-display text-2xl font-bold text-primary">&lt;50ms</p><p className="text-xs text-base-content/60">inference latency</p></div>
        </div>
      </div>

      <div className="flex items-center justify-center p-8">
        <div className="card w-full max-w-sm bg-base-100 shadow-xl border border-base-300">
          <div className="card-body">
            <h2 className="font-display text-2xl font-semibold">Welcome back</h2>
            <p className="text-sm text-base-content/60 -mt-2 mb-2">Log in to view your energy dashboard.</p>

            <form onSubmit={handleSubmit} className="space-y-3">
              <label className="form-control">
                <span className="label-text text-xs mb-1">Email</span>
                <input type="email" className="input input-bordered w-full" value={email}
                  onChange={e => setEmail(e.target.value)} required placeholder="you@company.co.za" />
              </label>
              <label className="form-control">
                <span className="label-text text-xs mb-1">Password</span>
                <input type="password" className="input input-bordered w-full" value={password}
                  onChange={e => setPassword(e.target.value)} required placeholder="••••••••" />
              </label>

              {error && <div className="alert alert-error py-2 text-sm">{error}</div>}

              <button type="submit" className="btn btn-primary w-full" disabled={loading}>
                {loading ? <span className="loading loading-spinner loading-sm" /> : 'Log in'}
              </button>
            </form>

            <p className="text-center text-sm text-base-content/60 mt-3">
              Don't have an account? <a href="/register" className="link link-primary">Register</a>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}