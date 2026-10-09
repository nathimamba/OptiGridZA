import { useState, useEffect } from 'react'
import { jwtDecode } from 'jwt-decode'
import { AuthContext } from './authContextValue'

function getInitialAuth() {
  const token = localStorage.getItem('token')
  if (!token) return { user: null, invalidToken: false }

  try {
    const decoded = jwtDecode(token)
    return {
      user: {
        email: decoded.sub,
        role: decoded.role,
        companyId: decoded.companyId,
        firstName: decoded.firstName,
        lastName: decoded.lastName,
        token,
      },
      invalidToken: false,
    }
  } catch {
    return { user: null, invalidToken: true }
  }
}

export function AuthProvider({ children }) {
  const [initialAuth] = useState(getInitialAuth)
  const [user, setUser] = useState(initialAuth.user)
  const ready = true

  useEffect(() => {
    if (initialAuth.invalidToken) localStorage.removeItem('token')
  }, [initialAuth.invalidToken])

  const logout = () => {
    localStorage.removeItem('token')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, setUser, logout, ready }}>
      {children}
    </AuthContext.Provider>
  )
}