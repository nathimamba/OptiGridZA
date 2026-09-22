import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'

const NAV_ITEMS = [
    { path: '/dashboard', label: 'Overview', roles: null },
    { path: '/battery', label: 'Battery Health', roles: ['TECHNICIAN', 'ENERGY_MANAGER', 'SYSTEM_ADMIN'] },
    { path: '/savings', label: 'Savings', roles: ['BUSINESS_OWNER', 'ENERGY_MANAGER', 'SYSTEM_ADMIN'] },
    { path: '/alerts', label: 'Alerts', roles: null },
    { path: '/companies', label: 'Companies', roles: ['SYSTEM_ADMIN'] },
]

export default function Layout() {
    const { user, logout } = useAuth()
    const { theme, toggle } = useTheme()
    const navigate = useNavigate()
    const location = useLocation()
    const items = NAV_ITEMS.filter(i => !i.roles || i.roles.includes(user?.role))

    return (
        <div className="drawer lg:drawer-open min-h-screen bg-base-200">
            <input id="nav-drawer" type="checkbox" className="drawer-toggle" />

            <div className="drawer-content flex flex-col">
                <div className="navbar bg-base-100 border-b border-base-300 lg:hidden">
                    <label htmlFor="nav-drawer" className="btn btn-square btn-ghost">☰</label>
                    <span className="font-display font-bold ml-2">OptiGrid</span>
                </div>

                <div className="navbar bg-base-100 border-b border-base-300 hidden lg:flex justify-end px-6">
                    <button className="btn btn-ghost btn-sm" onClick={toggle}>
                        {theme === 'optigridlight' ? '🌙 Dark' : '☀️ Light'}
                    </button>
                    <div className="dropdown dropdown-end ml-3">
                        <div tabIndex={0} role="button" className="btn btn-ghost btn-sm">
                            {user?.email} <span className="badge badge-primary badge-sm ml-1">{user?.role}</span>
                        </div>
                        <ul tabIndex={0} className="dropdown-content menu bg-base-100 rounded-box shadow border border-base-300 w-40 mt-2">
                            <li><button onClick={logout}>Log out</button></li>
                        </ul>
                    </div>
                </div>

                <main className="flex-1 p-4 lg:p-8">
                    <Outlet />
                </main>
            </div>

            <div className="drawer-side z-20">
                <label htmlFor="nav-drawer" className="drawer-overlay"></label>
                <aside className="w-64 min-h-full bg-base-100 border-r border-base-300 p-4 flex flex-col">
                    <div className="flex items-center gap-2 px-2 py-4">
                        <div className="w-9 h-9 rounded-lg bg-primary text-primary-content grid place-items-center font-bold">⚡</div>
                        <div>
                            <p className="font-display font-bold leading-tight">OptiGrid</p>
                            <p className="text-xs text-base-content/60">Energy for SA business</p>
                        </div>
                    </div>

                    <ul className="menu w-full gap-1 flex-1">
                        {items.map(item => (
                            <li key={item.path}>
                                <button
                                    className={location.pathname === item.path ? 'active' : ''}
                                    onClick={() => navigate(item.path)}
                                >
                                    {item.label}
                                </button>
                            </li>
                        ))}
                </ul>

                <button className="btn btn-ghost btn-sm lg:hidden mt-2" onClick={toggle}>
                    {theme === 'optigridlight' ? '🌙 Dark mode' : '☀️ Light mode'}
                </button>
                <button className="btn btn-outline btn-sm mt-2" onClick={logout}>Log out</button>
            </aside>
        </div>
    </div >
  )
}