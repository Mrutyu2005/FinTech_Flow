import { useState, useEffect } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { authService } from '../services/api'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError]     = useState('')
  const [success, setSuccess] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate  = useNavigate()
  const location = useLocation()

  useEffect(() => {
    if (location.state?.message) {
      setSuccess(location.state.message)
      // clear state so it doesn't persist on refresh
      window.history.replaceState({}, document.title)
    }
  }, [location])

  const handleLogin = async (e) => {
    e.preventDefault(); setError(''); setSuccess(''); setLoading(true)
    try {
      const res = await authService.login(username, password)
      login(res.data.token, res.data.username, res.data.role)
      navigate('/dashboard')
    } catch (err) {
      setError(err?.message || 'Invalid credentials. Please try again.')
    } finally { setLoading(false) }
  }

  return (
    <div className="auth-layout">
      <div className="auth-box">
        {/* Logo */}
        <div className="auth-logo">
          <div className="logo-main">🏦 FintechFlow</div>
          <div className="logo-tagline">Secure Authentication Gateway</div>
        </div>

        <div className="card">
          <h2 style={{ textAlign: 'center', marginBottom: '1.5rem' }}>Access Your Account</h2>

          {error   && <div className="alert alert-error"   style={{ marginBottom: 20 }}>⚠ {error}</div>}
          {success && <div className="alert alert-success" style={{ marginBottom: 20 }}>✓ {success}</div>}

          <form onSubmit={handleLogin}>
            <div className="form-group">
              <label htmlFor="username">Username</label>
              <input
                id="username"
                className="form-control"
                type="text"
                placeholder="e.g. alice"
                value={username}
                onChange={e => setUsername(e.target.value)}
                required
                autoFocus
              />
            </div>
            <div className="form-group" style={{ marginBottom: 24 }}>
              <label htmlFor="password">Password</label>
              <input
                id="password"
                className="form-control"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={e => setPassword(e.target.value)}
                required
              />
            </div>
            <button
              id="btn-login"
              className="btn btn-primary btn-full"
              type="submit"
              disabled={loading}
            >
              {loading ? <span className="spinner" /> : null} Sign In
            </button>
          </form>

          <p style={{ textAlign: 'center', marginTop: 20, fontSize: '0.82rem', color: 'var(--text-muted)' }}>
            Don't have an account? <Link to="/signup" style={{ color: 'var(--brand-primary)', fontWeight: 'bold' }}>Create one here</Link>
          </p>
        </div>
      </div>
    </div>
  )
}
