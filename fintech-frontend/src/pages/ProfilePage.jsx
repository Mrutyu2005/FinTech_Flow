import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { accountService } from '../services/api'

export default function ProfilePage() {
  const { user, role, token } = useAuth()
  const [account, setAccount] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    accountService(token).getMe()
      .then(res => setAccount(res.data))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [token])

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1>User Profile</h1>
        <p>Identity mapping & regulatory configuration details.</p>
      </div>

      <div className="two-col" style={{ alignItems: 'flex-start' }}>
        <div className="card">
          <h3 style={{ marginBottom: 20 }}>System Identity (JWT)</h3>
          
          <div className="profile-detail">
            <span className="profile-label">Authorized Username:</span>
            <span className="profile-value">{user}</span>
          </div>

          <div className="profile-detail">
            <span className="profile-label">Network Privilege Rule:</span>
            <span className="profile-value"><span className={`badge ${role === 'ADMIN' ? 'badge-danger' : 'badge-success'}`}>{role}</span></span>
          </div>
        </div>

        <div className="card">
          <h3 style={{ marginBottom: 20 }}>KYC Profile Parameters</h3>

          {loading ? (
             <div style={{ textAlign: 'center', padding: 20 }}><span className="spinner" /></div>
          ) : !account ? (
             <div className="alert alert-error">You have not completed Account Profile verification.</div>
          ) : (
            <>
              <div className="profile-detail">
                <span className="profile-label">Legal Name:</span>
                <span className="profile-value">{account.name}</span>
              </div>
              <div className="profile-detail">
                <span className="profile-label">Email ID:</span>
                <span className="profile-value">{account.email}</span>
              </div>
              <div className="profile-detail">
                <span className="profile-label">Mobile Number:</span>
                <span className="profile-value">{account.mobile}</span>
              </div>
              <div className="profile-detail">
                <span className="profile-label">Aadhaar (SSN):</span>
                <span className="profile-value">XXXX-XXXX-{account.aadhaar.substring(8)}</span>
              </div>
              <div className="profile-detail">
                <span className="profile-label">Physical Address:</span>
                <span className="profile-value">{account.address}</span>
              </div>
              <div className="profile-detail">
                <span className="profile-label">Active Account Number:</span>
                <span className="profile-value" style={{ fontFamily: 'monospace' }}>{account.accountNumber}</span>
              </div>
            </>
          )}
        </div>
      </div>
      
      <style>{`
        .profile-detail { display: flex; justify-content: space-between; border-bottom: 1px solid rgba(0,0,0,0.05); padding: 12px 0; }
        .profile-label { color: var(--text-secondary); font-size: 0.9rem; font-weight: 500; }
        .profile-value { color: var(--text-primary); font-size: 0.95rem; font-weight: 600; text-align: right; }
        .profile-detail:last-child { border-bottom: none; }
      `}</style>
    </div>
  )
}
