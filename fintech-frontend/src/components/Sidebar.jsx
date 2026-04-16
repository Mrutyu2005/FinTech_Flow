import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Sidebar() {
  const { user, role, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const initials = user ? user.substring(0, 2).toUpperCase() : 'FF'

  const navItems = [
    { to: '/dashboard',    icon: '⬡', label: 'Dashboard'     },
    { to: '/accounts',     icon: '💳', label: 'Accounts'      },
    { to: '/transfer',     icon: '↔', label: 'Transfer'       },
    { to: '/transactions', icon: '📋', label: 'Transactions'  },
    { to: '/profile',      icon: '👤', label: 'Profile'       },
  ]

  if (role === 'ADMIN') {
    navItems.push({ to: '/admin', icon: '🔒', label: 'Admin Panel' })
  }

  return (
    <nav className="sidebar">
      <div className="sidebar-logo">
        <div className="logo-text">FintechFlow</div>
        <div className="logo-sub">Digital Banking</div>
      </div>

      <div className="nav-links">
        {navItems.map(({ to, icon, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
          >
            <span className="nav-icon">{icon}</span>
            {label}
          </NavLink>
        ))}
      </div>

      <div className="sidebar-bottom">
        <div className="user-pill">
          <div className="user-avatar">{initials}</div>
          <div className="user-name">{user} ({role})</div>
        </div>
        <button className="nav-link btn-danger" onClick={handleLogout} style={{ color: '#ff8a92', border: 'none', background: 'transparent', cursor: 'pointer', textAlign: 'left', width: '100%', padding: '0.75rem 1.5rem' }}>
          <span className="nav-icon">⎋</span>
          Logout
        </button>
      </div>
    </nav>
  )
}
