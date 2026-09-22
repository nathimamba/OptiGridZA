import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ThemeProvider } from './context/ThemeContext'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import BatteryHealth from './pages/BatteryHealth'
import Savings from './pages/Savings'
import Alerts from './pages/Alerts'
import Companies from './pages/Companies'

export default function App() {
  return (
    <AuthProvider>
      <ThemeProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<Login />} />
            <Route path="/register" element={<Register />} />

            <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/battery" element={<BatteryHealth />} />
              <Route path="/savings" element={<Savings />} />
              <Route path="/alerts" element={<Alerts />} />
              <Route path="/companies" element={<ProtectedRoute roles={['SYSTEM_ADMIN']}><Companies /></ProtectedRoute>} />
            </Route>
          </Routes>
        </BrowserRouter>
      </ThemeProvider>
    </AuthProvider>
  )
}