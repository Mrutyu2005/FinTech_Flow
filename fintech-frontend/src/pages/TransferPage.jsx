import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { accountService, transactionService } from '../services/api'

export default function TransferPage() {
  const { user, token } = useAuth()
  const [accounts, setAccounts] = useState([])
  const [form, setForm]         = useState({ fromAccountId: '', toAccountId: '', amount: '', note: '', transactionPassword: '' })
  const [result, setResult]     = useState(null)
  const [error, setError]       = useState('')
  const [loading, setLoading]   = useState(false)

  useEffect(() => {
    accountService(token).getMe()
      .then(r => setAccounts([r.data])) // getMe() returns the organic user account now
      .catch(() => {})
  }, [token])

  const handleTransfer = async (e) => {
    e.preventDefault()
    if (form.fromAccountId === form.toAccountId) {
      setError('Source and destination accounts must be different.'); return
    }
    setLoading(true); setError(''); setResult(null)
    try {
      const res = await transactionService(token).transfer({
        fromAccountId: Number(form.fromAccountId),
        toAccountId:   Number(form.toAccountId),
        amount:        parseFloat(form.amount),
        transactionPassword: form.transactionPassword,
        note:          form.note || undefined,
      })
      setResult(res.data)
      setForm({ fromAccountId: '', toAccountId: '', amount: '', note: '', transactionPassword: '' })
    } catch (err) {
      setError(err?.message || 'Transfer failed. Check balance and transaction password.')
    } finally { setLoading(false) }
  }

  const fromAcc  = accounts.find(a => a.id === Number(form.fromAccountId))

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1>Transfer Funds</h1>
        <p>Move money globally securely</p>
      </div>

      <div className="two-col" style={{ alignItems: 'flex-start' }}>
        <div className="card">
          <h3 style={{ marginBottom: 24 }}>New Transfer</h3>
          {error && <div className="alert alert-error" style={{ marginBottom: 20 }}>⚠ {error}</div>}
          {result && (
            <div className={`alert ${result.status === 'SUCCESS' ? 'alert-success' : 'alert-error'}`} style={{ marginBottom: 20 }}>
              {result.status === 'SUCCESS'
                ? `✓ Transfer of ₹${result.amount} completed! Ref #${result.id}`
                : `✕ Transfer failed — ${result.message}`}
            </div>
          )}

          <form onSubmit={handleTransfer}>
            <div className="form-group">
              <label htmlFor="from-acc">From Account</label>
              <select
                id="from-acc"
                className="form-control"
                value={form.fromAccountId}
                onChange={e => setForm(f => ({ ...f, fromAccountId: e.target.value }))}
                required
              >
                <option value="">— Select source account —</option>
                {accounts.map(a => (
                  <option key={a.id} value={a.id}>
                    #{a.id} · {a.accountNumber} · {a.accountType} · ₹{a.balance.toLocaleString('en-IN')}
                  </option>
                ))}
              </select>
            </div>

            <div style={{ textAlign: 'center', margin: '4px 0', color: 'var(--text-muted)', fontSize: '1.3rem' }}>↓</div>

            <div className="form-group">
              <label htmlFor="to-acc">Destination Account ID</label>
              <input
                id="to-acc"
                className="form-control"
                type="number"
                placeholder="Enter receiver's Account ID"
                value={form.toAccountId}
                onChange={e => setForm(f => ({ ...f, toAccountId: e.target.value }))}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="tx-amount">Amount (₹)</label>
              <input
                id="tx-amount"
                className="form-control"
                type="number"
                min="1"
                step="0.01"
                placeholder="0.00"
                value={form.amount}
                onChange={e => setForm(f => ({ ...f, amount: e.target.value }))}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="tx-note">Note (optional)</label>
              <input
                id="tx-note"
                className="form-control"
                type="text"
                placeholder="e.g. Rent payment"
                maxLength={100}
                value={form.note}
                onChange={e => setForm(f => ({ ...f, note: e.target.value }))}
              />
            </div>

            <div className="form-group" style={{ marginTop: '1.5rem', padding: '1rem', background: '#ffebee', borderRadius: '8px', border: '1px solid #ffcdd2' }}>
              <label htmlFor="tx-password" style={{ color: '#d32f2f' }}>Transaction Password</label>
              <input
                id="tx-password"
                className="form-control"
                type="password"
                placeholder="Authorize transfer"
                value={form.transactionPassword}
                onChange={e => setForm(f => ({ ...f, transactionPassword: e.target.value }))}
                required
                style={{ borderColor: '#ef9a9a' }}
              />
               <small style={{ color: '#d32f2f', display: 'block', marginTop: '0.5rem' }}>Required to authenticate external network routing.</small>
            </div>

            <button
              id="btn-transfer"
              className="btn btn-primary btn-full"
              type="submit"
              disabled={loading}
              style={{ marginTop: '1rem' }}
            >
              {loading ? <span className="spinner" /> : '↔'}
              {loading ? ' Processing...' : ' Transfer Now'}
            </button>
          </form>
        </div>

        {/* Info panel */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
          {fromAcc && (
            <div className="card" style={{ padding: 22 }}>
              <div className="stat-label" style={{ marginBottom: 8 }}>Source Account</div>
              <div style={{ fontFamily: 'monospace', fontSize: '0.82rem', color: 'var(--text-secondary)', marginBottom: 8 }}>
                {fromAcc.accountNumber}
              </div>
              <div style={{ fontSize: '1.6rem', fontWeight: 800, letterSpacing: '-0.03em' }}>
                ₹{fromAcc.balance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </div>
              <div className="account-type" style={{ marginTop: 4 }}>{fromAcc.accountType}</div>
              {form.amount && !isNaN(form.amount) && (
                <>
                  <hr className="divider" />
                  <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>After transfer:</div>
                  <div style={{ fontSize: '1.2rem', fontWeight: 700, color: fromAcc.balance - Number(form.amount) < 0 ? 'var(--danger)' : 'var(--success)' }}>
                    ₹{(fromAcc.balance - Number(form.amount)).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </div>
                  {fromAcc.balance - Number(form.amount) < 0 && (
                    <div className="badge badge-danger" style={{ marginTop: 6 }}>Insufficient Balance</div>
                  )}
                </>
              )}
            </div>
          )}

          <div className="card" style={{ padding: 18, background: 'rgba(108,99,255,0.06)', borderColor: 'rgba(108,99,255,0.2)' }}>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', lineHeight: 1.7 }}>
              🔒 <strong style={{ color: 'var(--brand-primary)' }}>Secured by IDOR Prevention</strong><br />
              All transfers are explicitly verified against your secure PIN natively within the Account-Service.
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
