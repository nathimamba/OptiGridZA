import { useEffect, useMemo, useState } from 'react'
import { useAuth } from './authContextValue'
import { apiFetch } from '../services/api'
import { CompanyScopeContext } from './companyScopeContextValue'

const STORAGE_KEY = 'optigrid.adminCompanyId'

/**
 * Which company's data the pages should show.
 * - Company users: always their own companyId (from the JWT / /auth/me).
 * - System administrators have no companyId, so they pick one company to inspect.
 */
export function CompanyScopeProvider({ children }) {
  const { user } = useAuth()
  const isAdmin = user?.role === 'SYSTEM_ADMIN'
  const [companies, setCompanies] = useState([])
  const [selectedId, setSelectedId] = useState(() => sessionStorage.getItem(STORAGE_KEY) || '')

  useEffect(() => {
    if (!isAdmin) return undefined
    let active = true
    apiFetch('/api/v1/companies')
      .then(list => { if (active) setCompanies(list.filter(c => c.active !== false)) })
      .catch(() => { if (active) setCompanies([]) })
    return () => { active = false }
  }, [isAdmin])

  const activeCompanyId = isAdmin
    ? (companies.some(c => c.id === selectedId) ? selectedId : (companies[0]?.id ?? null))
    : (user?.companyId ?? null)

  const selectCompany = (id) => {
    setSelectedId(id)
    sessionStorage.setItem(STORAGE_KEY, id)
  }

  const value = useMemo(
    () => ({ isAdmin, companies, activeCompanyId, selectCompany }),
    [isAdmin, companies, activeCompanyId],
  )

  return <CompanyScopeContext.Provider value={value}>{children}</CompanyScopeContext.Provider>
}
