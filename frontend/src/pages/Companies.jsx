import { useEffect, useState } from 'react'
import { apiFetch } from '../services/api'

async function geocodeCity(city) {
  const res = await fetch(`https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(city)}&count=1&country=ZA`)
  const data = await res.json()
  if (!data.results || data.results.length === 0) {
    throw new Error('City not found — try a major South African city name')
  }
  return { latitude: data.results[0].latitude, longitude: data.results[0].longitude }
}

export default function Companies() {
  const [companies, setCompanies] = useState([])
  const [error, setError] = useState('')
  const [form, setForm] = useState({ name: '', address: '', industryType: '', contactEmail: '', contactPhone: '', city: '' })
  const [showModal, setShowModal] = useState(false)
  const [saving, setSaving] = useState(false)

  const [selectedCompany, setSelectedCompany] = useState(null)
  const [companyUsers, setCompanyUsers] = useState([])
  const [userForm, setUserForm] = useState({ firstName: '', lastName: '', email: '', password: '', role: 'ENERGY_MANAGER' })
  const [userError, setUserError] = useState('')
  const [userSaving, setUserSaving] = useState(false)

  const load = () => {
    apiFetch('/api/v1/companies')
      .then(setCompanies)
      .catch(() => setError('Could not load companies.'))
  }
  useEffect(load, [])

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setSaving(true)
    try {
      let latitude = null
      let longitude = null
      if (form.city.trim()) {
        const coords = await geocodeCity(form.city.trim())
        latitude = coords.latitude
        longitude = coords.longitude
      }

      await apiFetch('/api/v1/companies', {
        method: 'POST',
        body: JSON.stringify({
          name: form.name,
          address: form.address,
          industryType: form.industryType,
          contactEmail: form.contactEmail,
          contactPhone: form.contactPhone,
          latitude,
          longitude,
        }),
      })
      setShowModal(false)
      setForm({ name: '', address: '', industryType: '', contactEmail: '', contactPhone: '', city: '' })
      load()
    } catch (err) {
      setError(err.message || 'Could not create company — name may already be in use.')
    } finally {
      setSaving(false)
    }
  }

  const deactivate = async (id) => {
    try {
      await apiFetch(`/api/v1/companies/${id}/deactivate`, { method: 'PUT' })
      load()
    } catch {
      setError('Could not deactivate company.')
    }
  }

  const openUsers = async (company) => {
    setSelectedCompany(company)
    setUserError('')
    try {
      const users = await apiFetch(`/api/v1/companies/${company.id}/users`)
      setCompanyUsers(users)
    } catch {
      setUserError('Could not load users for this company.')
    }
  }

  const assignUser = async (e) => {
    e.preventDefault()
    setUserError('')
    setUserSaving(true)
    try {
      try {
        await apiFetch('/api/v1/auth/register', {
          method: 'POST',
          body: JSON.stringify({
            firstName: userForm.firstName,
            lastName: userForm.lastName,
            email: userForm.email,
            password: userForm.password,
            role: userForm.role,
          }),
        })
      } catch (regErr) {
        if (!regErr.message.includes('already')) throw regErr
      }

      await apiFetch(`/api/v1/companies/${selectedCompany.id}/users`, {
        method: 'POST',
        body: JSON.stringify({ email: userForm.email, role: userForm.role }),
      })

      setUserForm({ firstName: '', lastName: '', email: '', password: '', role: 'ENERGY_MANAGER' })
      openUsers(selectedCompany)
    } catch {
      setUserError('Could not register/assign user — check the details and try again.')
    } finally {
      setUserSaving(false)
    }
  }

  const removeUser = async (email) => {
    try {
      await apiFetch(`/api/v1/companies/${selectedCompany.id}/users/${email}`, { method: 'DELETE' })
      openUsers(selectedCompany)
    } catch {
      setUserError('Could not remove user.')
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="font-display text-2xl font-bold">Companies</h1>
          <p className="text-sm text-base-content/60">System administration</p>
        </div>
        <button className="btn btn-primary btn-sm" onClick={() => setShowModal(true)}>
          + Add company
        </button>
      </div>

      {error && <div className="alert alert-error mb-6">{error}</div>}

      <div className="card bg-base-100 shadow border border-base-300">
        <div className="card-body overflow-x-auto">
          <table className="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Industry</th>
                <th>Location</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {companies.length === 0 && (
                <tr><td colSpan={5} className="text-center text-base-content/60 py-8">No companies yet.</td></tr>
              )}
              {companies.map(c => (
                <tr key={c.id}>
                  <td className="font-medium">{c.name}</td>
                  <td>{c.industryType || '—'}</td>
                  <td className="text-xs text-base-content/60">
                    {c.latitude ? `${c.latitude.toFixed(2)}, ${c.longitude.toFixed(2)}` : 'Default (Pretoria)'}
                  </td>
                  <td>
                    <span className={`badge ${c.active ? 'badge-success' : 'badge-ghost'}`}>
                      {c.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td>
                    <div className="flex gap-2">
                      <button className="btn btn-xs btn-outline" onClick={() => openUsers(c)}>
                        Users
                      </button>
                      {c.active && (
                        <button className="btn btn-xs btn-error btn-outline" onClick={() => deactivate(c.id)}>
                          Deactivate
                        </button>
                      )}
                    </div>
                  </td>
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
              <input className="input input-bordered w-full" placeholder="City (e.g. Cape Town) — used for weather/solar data" value={form.city}
                onChange={e => setForm({ ...form, city: e.target.value })} />
              <input className="input input-bordered w-full" placeholder="Industry type" value={form.industryType}
                onChange={e => setForm({ ...form, industryType: e.target.value })} />
              <input className="input input-bordered w-full" placeholder="Contact email" value={form.contactEmail}
                onChange={e => setForm({ ...form, contactEmail: e.target.value })} />
              <input className="input input-bordered w-full" placeholder="Contact phone" value={form.contactPhone}
                onChange={e => setForm({ ...form, contactPhone: e.target.value })} />

              <div className="modal-action">
                <button type="button" className="btn" onClick={() => setShowModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={saving}>
                  {saving ? <span className="loading loading-spinner loading-sm" /> : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {selectedCompany && (
        <div className="modal modal-open">
          <div className="modal-box max-w-lg">
            <h3 className="font-display font-bold text-lg mb-1">Users — {selectedCompany.name}</h3>
            <p className="text-xs text-base-content/60 mb-4">
              Registers the user (if new) and assigns them to this company in one step.
            </p>

            {userError && <div className="alert alert-error py-2 text-xs mb-3">{userError}</div>}

            <table className="table table-sm mb-4">
              <thead><tr><th>Email</th><th>Role</th><th></th></tr></thead>
              <tbody>
                {companyUsers.length === 0 && (
                  <tr><td colSpan={3} className="text-center text-base-content/60 py-4">No users assigned yet.</td></tr>
                )}
                {companyUsers.map(u => (
                  <tr key={u.id}>
                    <td className="text-sm">{u.email}</td>
                    <td><span className="badge badge-ghost badge-sm">{u.role}</span></td>
                    <td>
                      <button className="btn btn-xs btn-error btn-outline" onClick={() => removeUser(u.email)}>
                        Remove
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <form onSubmit={assignUser} className="space-y-2">
              <div className="flex gap-2">
                <input
                  className="input input-bordered input-sm w-full"
                  placeholder="First name"
                  value={userForm.firstName}
                  onChange={e => setUserForm({ ...userForm, firstName: e.target.value })}
                  required
                />
                <input
                  className="input input-bordered input-sm w-full"
                  placeholder="Last name"
                  value={userForm.lastName}
                  onChange={e => setUserForm({ ...userForm, lastName: e.target.value })}
                  required
                />
              </div>
              <input
                type="email"
                className="input input-bordered input-sm w-full"
                placeholder="user@company.co.za"
                value={userForm.email}
                onChange={e => setUserForm({ ...userForm, email: e.target.value })}
                required
              />
              <input
                type="password"
                className="input input-bordered input-sm w-full"
                placeholder="temporary password"
                value={userForm.password}
                onChange={e => setUserForm({ ...userForm, password: e.target.value })}
                required
              />
              <div className="flex gap-2">
                <select
                  className="select select-bordered select-sm flex-1"
                  value={userForm.role}
                  onChange={e => setUserForm({ ...userForm, role: e.target.value })}
                >
                  <option value="ENERGY_MANAGER">Energy Manager</option>
                  <option value="BUSINESS_OWNER">Business Owner</option>
                  <option value="TECHNICIAN">Technician</option>
                  <option value="VIEWER">Viewer</option>
                </select>
                <button type="submit" className="btn btn-primary btn-sm" disabled={userSaving}>
                  {userSaving ? <span className="loading loading-spinner loading-xs" /> : 'Register & Assign'}
                </button>
              </div>
            </form>

            <div className="modal-action">
              <button className="btn btn-sm" onClick={() => setSelectedCompany(null)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}