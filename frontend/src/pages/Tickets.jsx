import { useCallback, useEffect, useState } from 'react'
import { useAuth } from '../context/authContextValue'
import { apiFetch } from '../services/api'

const priorityBadge = {
  CRITICAL: 'badge-error',
  HIGH: 'badge-warning',
  MEDIUM: 'badge-info',
  LOW: 'badge-ghost'
}

const statusBadge = {
  OPEN: 'badge-error',
  IN_PROGRESS: 'badge-warning',
  RESOLVED: 'badge-success',
  CLOSED: 'badge-ghost'
}

export default function Tickets() {
  const { user } = useAuth()
  const [tickets, setTickets] = useState([])
  const [filter, setFilter] = useState('ALL')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [updating, setUpdating] = useState(null)

  const fetchTickets = useCallback(() => {
    if (!user?.companyId) return
    setLoading(true)
    apiFetch(`/api/v1/tickets/company/${user.companyId}`)
      .then(data => { setTickets(data); setError('') })
      .catch(() => setError('Could not load tickets.'))
      .finally(() => setLoading(false))
  }, [user])

  useEffect(() => {
    const initialFetch = setTimeout(fetchTickets, 0)
    return () => clearTimeout(initialFetch)
  }, [fetchTickets])

  const updateStatus = async (ticketId, status) => {
    setUpdating(ticketId)
    try {
      await apiFetch(`/api/v1/tickets/${ticketId}/status`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status })
      })
      fetchTickets()
    } catch { setError('Failed to update ticket.') }
    finally { setUpdating(null) }
  }

  const claimTicket = async (ticketId) => {
    setUpdating(ticketId)
    try {
      await apiFetch(`/api/v1/tickets/${ticketId}/assign`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: user.email })
      })
      fetchTickets()
    } catch { setError('Failed to claim ticket.') }
    finally { setUpdating(null) }
  }

  const filtered = filter === 'ALL' ? tickets : tickets.filter(t => t.status === filter)

  const stats = {
    open: tickets.filter(t => t.status === 'OPEN').length,
    inProgress: tickets.filter(t => t.status === 'IN_PROGRESS').length,
    resolved: tickets.filter(t => t.status === 'RESOLVED' || t.status === 'CLOSED').length,
    critical: tickets.filter(t => t.priority === 'CRITICAL' && t.status !== 'RESOLVED' && t.status !== 'CLOSED').length
  }

  return (
    <div>
      <h1 className="font-display text-2xl font-bold mb-1">Maintenance Tickets</h1>
      <p className="text-sm text-base-content/60 mb-6">Battery issue tracking &amp; resolution</p>

      {error && <div className="alert alert-error mb-4">{error}</div>}

      {/* Stats cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="card bg-base-100 shadow border border-base-300">
          <div className="card-body p-4 items-center">
            <span className="text-3xl font-bold text-error">{stats.open}</span>
            <span className="text-xs text-base-content/60">Open</span>
          </div>
        </div>
        <div className="card bg-base-100 shadow border border-base-300">
          <div className="card-body p-4 items-center">
            <span className="text-3xl font-bold text-warning">{stats.inProgress}</span>
            <span className="text-xs text-base-content/60">In Progress</span>
          </div>
        </div>
        <div className="card bg-base-100 shadow border border-base-300">
          <div className="card-body p-4 items-center">
            <span className="text-3xl font-bold text-success">{stats.resolved}</span>
            <span className="text-xs text-base-content/60">Resolved</span>
          </div>
        </div>
        <div className="card bg-base-100 shadow border border-base-300">
          <div className="card-body p-4 items-center">
            <span className="text-3xl font-bold text-error">{stats.critical}</span>
            <span className="text-xs text-base-content/60">Critical</span>
          </div>
        </div>
      </div>

      {/* Filter tabs */}
      <div className="tabs tabs-boxed bg-base-100 mb-4 w-fit">
        {['ALL', 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'].map(f => (
          <button key={f} className={`tab ${filter === f ? 'tab-active' : ''}`}
            onClick={() => setFilter(f)}>
            {f.replace('_', ' ')}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="flex justify-center py-12"><span className="loading loading-spinner loading-lg"></span></div>
      ) : filtered.length === 0 ? (
        <div className="card bg-base-100 shadow border border-base-300">
          <div className="card-body items-center py-12">
            <span className="text-4xl mb-2">✅</span>
            <p className="text-base-content/60">No tickets found</p>
          </div>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map(ticket => (
            <div key={ticket.id} className="card bg-base-100 shadow border border-base-300">
              <div className="card-body p-4">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className={`badge ${priorityBadge[ticket.priority] || 'badge-ghost'} badge-sm`}>{ticket.priority}</span>
                      <span className={`badge ${statusBadge[ticket.status] || 'badge-ghost'} badge-sm`}>{ticket.status}</span>
                      <span className="badge badge-outline badge-sm">{ticket.ticketType?.replace('_', ' ')}</span>
                    </div>
                    <h3 className="font-display font-bold text-sm">{ticket.title}</h3>
                    <p className="text-xs text-base-content/60 mt-1">{ticket.description}</p>

                    <div className="flex flex-wrap gap-4 mt-2 text-xs text-base-content/50">
                      {ticket.batterySoc != null && <span>SOC: <strong>{ticket.batterySoc.toFixed(1)}%</strong></span>}
                      {ticket.batteryHealth && <span>Health: <strong>{ticket.batteryHealth}</strong></span>}
                      {ticket.assignedTo && <span>Assigned: <strong>{ticket.assignedTo}</strong></span>}
                      <span>Created: {new Date(ticket.createdAt).toLocaleString()}</span>
                      {ticket.resolvedAt && <span>Resolved: {new Date(ticket.resolvedAt).toLocaleString()}</span>}
                    </div>

                    {ticket.resolutionNotes && (
                      <div className="mt-2 p-2 bg-base-200 rounded text-xs">
                        <strong>Resolution:</strong> {ticket.resolutionNotes}
                      </div>
                    )}
                  </div>

                  {/* Actions */}
                  {ticket.status !== 'CLOSED' && ticket.status !== 'RESOLVED' && (
                    <div className="flex gap-1 flex-shrink-0">
                      {ticket.status === 'OPEN' && !ticket.assignedTo && (
                        <button className="btn btn-primary btn-xs"
                          disabled={updating === ticket.id}
                          onClick={() => claimTicket(ticket.id)}>
                          {updating === ticket.id ? <span className="loading loading-spinner loading-xs"></span> : 'Claim'}
                        </button>
                      )}
                      {ticket.status === 'OPEN' && (
                        <button className="btn btn-warning btn-xs"
                          disabled={updating === ticket.id}
                          onClick={() => updateStatus(ticket.id, 'IN_PROGRESS')}>
                          Start
                        </button>
                      )}
                      {ticket.status === 'IN_PROGRESS' && (
                        <button className="btn btn-success btn-xs"
                          disabled={updating === ticket.id}
                          onClick={() => updateStatus(ticket.id, 'RESOLVED')}>
                          Resolve
                        </button>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
