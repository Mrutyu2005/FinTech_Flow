import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { accountService, transactionService } from '../services/api'

export default function DashboardPage() {
  const { user, token } = useAuth()
  const navigate = useNavigate()
  const [account, setAccount]           = useState(null)
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading]           = useState(true)

  useEffect(() => {
    const load = async () => {
      try {
        const accRes = await accountService(token).getMe()
        setAccount(accRes.data)
        
        // Only load transactions if account genuinely exists natively
        if (accRes.data) {
          const txRes = await transactionService(token).getMy()
          setTransactions(txRes.data.slice(0, 5))
        }
      } catch (e) {
        console.error(e)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [user, token])

  const totalBalance = account ? account.balance : 0
  const successTx    = transactions.filter(t => t.status === 'SUCCESS').length

  return (
    <div className="fade-in" style={{ position: 'relative' }}>
        <div className="page-header">
          <h1>Welcome back, {user} 👋</h1>
          <p>Here's your financial overview</p>
        </div>

        {/* Stats */}
        <div className="stats-grid">
          <div className="stat-card" style={{ '--delay': '0s', '--accent-color': 'var(--brand-primary)' }}>
            <div className="stat-label">Total Balance</div>
            <div className="stat-value">₹{totalBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}</div>
            <div className="stat-sub">Central Account</div>
          </div>
          <div className="stat-card" style={{ '--delay': '0.08s', '--accent-color': 'var(--brand-secondary)' }}>
            <div className="stat-label">Active Profile</div>
            <div className="stat-value">{account ? '1' : '0'}</div>
            <div className="stat-sub">Identity mapped</div>
          </div>
          <div className="stat-card" style={{ '--delay': '0.16s', '--accent-color': 'var(--warning)' }}>
            <div className="stat-label">Recent Transactions</div>
            <div className="stat-value">{transactions.length}</div>
            <div className="stat-sub">{successTx} successful</div>
          </div>
          <div className="stat-card" style={{ '--delay': '0.24s', '--accent-color': 'var(--brand-accent)' }}>
            <div className="stat-label">Services Online</div>
            <div className="stat-value" style={{ color: 'var(--success)', fontSize: '1.4rem' }}>●●</div>
            <div className="stat-sub">Core Logic active</div>
          </div>
        </div>

        {/* Accounts preview */}
        <div className="section-header">
          <h2>My Virtual Profile</h2>
          <Link to="/accounts" className="btn btn-secondary btn-sm">Configuration</Link>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: 40 }}><span className="spinner" /></div>
        ) : !account ? (
          <div className="card">
            <div className="empty-state">
              <div className="empty-icon">💳</div>
              <h3>Registration Incomplete</h3>
              <p>Complete Regulatory mapping immediately to construct wallet framework.</p>
              <br />
              <Link to="/accounts" className="btn btn-primary btn-sm">Register Profile</Link>
            </div>
          </div>
        ) : (
          <div className="accounts-grid" style={{ marginBottom: 32 }}>
              <div className="account-card" style={{ '--delay': `0s` }}>
                <div className="account-number">{account.accountNumber}</div>
                <div className="account-balance">
                  ₹{account.balance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 14 }}>
                  <span className="account-type">{account.accountType}</span>
                  <span className="badge badge-success">Active</span>
                </div>
              </div>
          </div>
        )}

        {/* Recent transactions */}
        <div className="section-header">
          <h2>Recent Activity</h2>
          <Link to="/transactions" className="btn btn-secondary btn-sm">See All</Link>
        </div>

        <div className="card" style={{ padding: 0 }}>
          {transactions.length === 0 ? (
            <div className="empty-state">
              <div className="empty-icon">📋</div>
              <h3>No transactions yet</h3>
              <p>Make a deposit or transfer to append events natively.</p>
            </div>
          ) : (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>Type</th>
                    <th>From → To</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Date</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map(tx => (
                    <TxRow key={tx.id} tx={tx} />
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

       {/* Conditional Registration Overlay Overlay */}
       {loading ? null : !account && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div className="card fade-in" style={{ padding: '2.5rem', textAlign: 'center', maxWidth: 450 }}>
             <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>🏦</div>
             <h3>Account Required</h3>
             <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>You must complete the regulatory account opening process before accessing digital banking features.</p>
             <Link to="/accounts" className="btn btn-primary btn-full" style={{ display: 'block' }}>Complete KYC Now</Link>
          </div>
        </div>
      )}
    </div>
  )
}

function TxRow({ tx }) {
  const icons = { TRANSFER: '↔', DEPOSIT: '↓', WITHDRAWAL: '↑' }
  const iconClass = { TRANSFER: 'tx-icon-transfer', DEPOSIT: 'tx-icon-deposit', WITHDRAWAL: 'tx-icon-withdraw' }
  const date = tx.timestamp ? new Date(tx.timestamp).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'
  return (
    <tr>
      <td>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span className={`tx-icon ${iconClass[tx.type] || ''}`}>{icons[tx.type] || '?'}</span>
          {tx.type}
        </div>
      </td>
      <td style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
        #{tx.fromAccountId} → #{tx.toAccountId}
      </td>
      <td className="amount-negative">₹{tx.amount?.toLocaleString('en-IN')}</td>
      <td><span className={`badge ${tx.status === 'SUCCESS' ? 'badge-success' : 'badge-danger'}`}>{tx.status}</span></td>
      <td style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>{date}</td>
    </tr>
  )
}
