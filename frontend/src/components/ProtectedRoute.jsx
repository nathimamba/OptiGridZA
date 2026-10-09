import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/authContextValue'

export default function ProtectedRoute({ children, roles }) {
  const { user, ready } = useAuth()
  if (!ready) return null
  if (!user) return <Navigate to="/" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to="/dashboard" replace />
  return children
}