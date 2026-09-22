import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

const GATEWAY_URL = 'http://localhost:8080'

export default function Register() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState('ENERGY_MANAGER')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await fetch(`${GATEWAY_URL}/api/v1/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password, role }),
      })
      if (!res.ok) throw new Error('Registration failed — email may already be in use')
      navigate('/')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-base-200 p-8">
      <div className="card w-full max-w-sm bg-base-100 shadow-xl border border-base-300">
        <div className="card-body">
          <h2 className="font-display text-2xl font-semibold">Create account</h2>
          <form onSubmit={handleSubmit} className="space-y-3 mt-2">
            <label className="form-control">
              <span className="label-text text-xs mb-1">Email</span>
              <input type="email" className="input input-bordered w-full" value={email}
                onChange={e => setEmail(e.target.value)} required />
            </label>
            <label className="form-control">
              <span className="label-text text-xs mb-1">Password</span>
              <input type="password" className="input input-bordered w-full" value={password}
                onChange={e => setPassword(e.target.value)} required />
            </label>
            <label className="form-control">
              <span className="label-text text-xs mb-1">Role</span>
              <select className="select select-bordered w-full" value={role} onChange={e => setRole(e.target.value)}>
                <option value="SYSTEM_ADMIN">System Admin</option>
                <option value="ENERGY_MANAGER">Energy Manager</option>
                <option value="BUSINESS_OWNER">Business Owner</option>
                <option value="TECHNICIAN">Technician</option>
                <option value="VIEWER">Viewer</option>
              </select>
            </label>

            {error && <div className="alert alert-error py-2 text-sm">{error}</div>}

            <button type="submit" className="btn btn-primary w-full" disabled={loading}>
              {loading ? <span className="loading loading-spinner loading-sm" /> : 'Register'}
            </button>
          </form>
          <p className="text-center text-sm text-base-content/60 mt-3">
            Already have an account? <a href="/" className="link link-primary">Log in</a>
          </p>
        </div>
      </div>
    </div>
  )
}