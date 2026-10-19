import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { API_BASE_URL } from '../services/api'
import { useAuth } from '../context/authContextValue'

export default function Register() {
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
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
      const res = await fetch(`${API_BASE_URL}/api/v1/auth/register`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ firstName, lastName, email, password }),
      })
      if (!res.ok) throw new Error('Registration failed — email may already be in use')
      setUser(await res.json())
      navigate('/dashboard')
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
            <div className="grid grid-cols-2 gap-3">
              <label className="form-control">
                <span className="label-text text-xs mb-1">First name</span>
                <input className="input input-bordered w-full" value={firstName}
                  onChange={e => setFirstName(e.target.value)} required />
              </label>
              <label className="form-control">
                <span className="label-text text-xs mb-1">Last name</span>
                <input className="input input-bordered w-full" value={lastName}
                  onChange={e => setLastName(e.target.value)} required />
              </label>
            </div>

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