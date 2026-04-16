import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { accountService } from '../services/api'
import { useNavigate } from 'react-router-dom'

export default function AccountsPage() {
  const { token, user } = useAuth()
  const navigate = useNavigate()
  
  const [form, setForm] = useState({
    name: '', address: '', mobile: '', aadhaar: '', email: '', 
    accountType: 'SAVINGS', initialBalance: '', transactionPassword: '' 
  })
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [hasAccount, setHasAccount] = useState(false)
  const [checking, setChecking] = useState(true)

  useEffect(() => {
    accountService(token).getMe()
      .then(res => setHasAccount(!!res.data))
      .catch(() => setHasAccount(false))
      .finally(() => setChecking(false))
  }, [token])

  const handleCreate = async (e) => {
    e.preventDefault()
    setCreating(true); setError(''); setSuccess('')
    try {
      await accountService(token).create({ 
        ...form, 
        initialBalance: parseFloat(form.initialBalance) 
      })
      setSuccess('Banking Profile configured successfully! Redirecting to Dashboard...')
      setTimeout(() => navigate('/dashboard'), 2000)
    } catch (err) {
      setError(err?.message || 'Failed to construct Account profile.')
    } finally { setCreating(false) }
  }

  if (checking) return <div style={{ textAlign: 'center', padding: 60 }}><span className="spinner" /></div>
  
  if (hasAccount) return (
     <div className="fade-in">
      <div className="page-header">
        <h1>Account Structure Active</h1>
        <p>You have already successfully mapped a core backend banking profile.</p>
      </div>
      <div className="card">
        <div className="empty-state">
           <div className="empty-icon" style={{color: 'var(--success)'}}>✓</div>
           <h3>Account Confirmed</h3>
           <p>Your banking profile is intrinsically active on this microservice grid.</p>
           <button onClick={() => navigate('/dashboard')} className="btn btn-primary" style={{marginTop: '1rem'}}>Go to Dashboard</button>
        </div>
      </div>
     </div>
  )

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1>Open Digital Account</h1>
        <p>Complete your regulatory KYC configurations accurately.</p>
      </div>

      <div className="card" style={{ maxWidth: '800px', margin: '0 auto' }}>
        <h3 style={{ marginBottom: 20 }}>Regulatory Profile Mapping</h3>
        
        {error   && <div className="alert alert-error"   style={{ marginBottom: 20 }}>⚠ {error}</div>}
        {success && <div className="alert alert-success" style={{ marginBottom: 20 }}>✓ {success}</div>}

        <form onSubmit={handleCreate}>
          <div className="two-col">
            <div className="form-group">
              <label>Full Legal Name</label>
              <input className="form-control" type="text" value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} required />
            </div>
            <div className="form-group">
              <label>Email ID</label>
              <input className="form-control" type="email" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} required />
            </div>
          </div>

          <div className="form-group">
            <label>Residential Address</label>
            <input className="form-control" type="text" value={form.address} onChange={e => setForm(f => ({ ...f, address: e.target.value }))} required />
          </div>

          <div className="two-col">
            <div className="form-group">
               <label>Mobile Number (10 Digits)</label>
               <input className="form-control" type="text" pattern="\d{10}" maxLength={10} value={form.mobile} onChange={e => setForm(f => ({ ...f, mobile: e.target.value }))} placeholder="98XXXXXXXX" required />
            </div>
            <div className="form-group">
               <label>Aadhaar / SSN (12 Digits)</label>
               <input className="form-control" type="text" pattern="\d{12}" maxLength={12} value={form.aadhaar} onChange={e => setForm(f => ({ ...f, aadhaar: e.target.value }))} placeholder="123412341234" required />
            </div>
          </div>
          
          <div className="two-col">
            <div className="form-group">
              <label>Account Type</label>
              <select className="form-control" value={form.accountType} onChange={e => setForm(f => ({ ...f, accountType: e.target.value }))}>
                <option value="SAVINGS">Savings</option>
                <option value="CURRENT">Current</option>
              </select>
            </div>
            <div className="form-group">
              <label>Initial Deposit Balance (₹)</label>
              <input className="form-control" type="number" min="0" step="0.01" value={form.initialBalance} onChange={e => setForm(f => ({ ...f, initialBalance: e.target.value }))} required />
            </div>
          </div>

          <hr style={{ margin: '2rem 0', borderColor: '#eee' }} />

          <div className="form-group">
            <label style={{ color: 'var(--brand-primary)' }}>Secure Transaction Password</label>
            <input className="form-control" type="password" value={form.transactionPassword} onChange={e => setForm(f => ({ ...f, transactionPassword: e.target.value }))} required placeholder="Used to dynamically authorize fund movements" />
            <small style={{ color: 'var(--text-muted)' }}>This is securely hashed natively by the Account-Service endpoint.</small>
          </div>

          <button className="btn btn-primary btn-full" type="submit" disabled={creating} style={{ marginTop: '1rem', padding: '1rem' }}>
            {creating ? <span className="spinner" /> : null} Register Account Profile natively
          </button>
        </form>
      </div>
    </div>
  )
}
