import { useState, useEffect } from 'react'
import { apiFetch } from '../services/api'
import { AuthContext } from './authContextValue'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    localStorage.removeItem('token')
    let active = true
    apiFetch('/api/v1/auth/me')
      .then((currentUser) => {
        if (active) setUser(currentUser)
      })
      .catch(() => {
        if (active) setUser(null)
      })
      .finally(() => {
        if (active) setReady(true)
      })
    return () => { active = false }
  }, [])

  const logout = () => {
    apiFetch('/api/v1/auth/logout', { method: 'POST' })
      .catch(() => {})
      .finally(() => setUser(null))
  }

  return (
    <AuthContext.Provider value={{ user, setUser, logout, ready }}>
      {children}
    </AuthContext.Provider>
  )
}