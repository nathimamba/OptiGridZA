import { useEffect, useState } from 'react'
import { apiFetch } from '../services/api'

export default function Companies() {
  const [companies, setCompanies] = useState([])
  const [error, setError] = useState('')
  const [form, setForm] = useState({ name: '', address: '', industryType: '' })
  const [showModal, setShowModal] = useState(false)

  const load = () => {
    apiFetch('/api/v1/companies').then(setCompanies).catch(() => setError('Could not load companies.'))
  }
  useEffect(load, [])

  const submit = async (e) => {
    e.preventDefault()
    try {
      await apiFetch('/api/v1/companies', { method: 'POST', body: JSON.stringify(form) })
      setShowModal(false)
      setForm({ name: '', address: '', industryType: '' })
      load()
    } catch { setError('Could not create company.') }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="font-display text-2xl font-bold">Companies</h1>
          <p className="text-sm text-base-content/60">System administration</p>
        </div>
        <button className="btn btn-primary btn-sm" onClick={() => setShowModal(true)}>+ Add company</button>
      </div>

      {error && <div className="alert alert-error mb-6">{error}</div>}

      <div className="card bg-base-100 shadow border border-base-300">
        <div className="card-body overflow-x-auto">
          <table className="table">
            <thead><tr><th>Name</th><th>Industry</th><th>Active</th></tr></thead>
            <tbody>
              {companies.map(c => (
                <tr key={c.id}>
                  <td>{c.name}</td>
                  <td>{c.industryType}</td>
                  <td><span className={`badge ${c.active ? 'badge-success' : 'badge-ghost'}`}>{c.active ? 'Active' : 'Inactive'}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {showModal && (
        <div className="modal modal-open">
          <div className="modal-box">
            <h3 className="font-display font-bold text-lg mb-4">Add company</h3>
            <form onSubmit={submit} className="space-y-3">
              <input className="input input-bordered w-full" placeholder="Name" value={form.name}
                onChange={e => setForm({ ...form, name: e.target.value })} required />
              <input className="input input-bordered w-full" placeholder="Address" value={form.address}
                onChange={e => setForm({ ...form, address: e.target.value })} />
              <input className="input input-bordered w-full" placeholder="Industry type" value={form.industryType}
                onChange={e => setForm({ ...form, industryType: e.target.value })} />
              <div className="modal-action">
                <button type="button" className="btn" onClick={() => setShowModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Create</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}