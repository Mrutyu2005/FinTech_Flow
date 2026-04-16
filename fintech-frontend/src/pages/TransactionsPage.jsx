import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { transactionService, accountService } from '../services/api'

const TYPE_ICONS  = { TRANSFER: '↔', DEPOSIT: '↓', WITHDRAWAL: '↑' }
const TYPE_CLASS  = { TRANSFER: 'tx-icon-transfer', DEPOSIT: 'tx-icon-deposit', WITHDRAWAL: 'tx-icon-withdraw' }
const STATUS_BADGE = { SUCCESS: 'badge-success', FAILED: 'badge-danger' }

export default function TransactionsPage() {
  const { token } = useAuth()
  const [transactions, setTransactions] = useState([])
  const [loading, setLoading]           = useState(true)
  const [filter, setFilter]             = useState('ALL')   // ALL | TRANSFER | DEPOSIT | WITHDRAWAL
  const [statusFilter, setStatusFilter] = useState('ALL')   // ALL | SUCCESS | FAILED
  const [myAccountId, setMyAccountId]   = useState(null)

  useEffect(() => {
    transactionService(token).getMy()
      .then(r => setTransactions(r.data))
      .catch(() => {})
      .finally(() => setLoading(false))

    accountService(token).getMe()
      .then(r => setMyAccountId(r.data.id))
      .catch(() => {})
  }, [token])

  const filtered = transactions.filter(tx => {
    const typeOk   = filter === 'ALL'       || tx.type   === filter
    const statusOk = statusFilter === 'ALL' || tx.status === statusFilter
    return typeOk && statusOk
  })

  const totalTransferred = transactions
    .filter(t => t.status === 'SUCCESS' && t.type === 'TRANSFER')
    .reduce((s, t) => s + t.amount, 0)

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1>Transaction History</h1>
        <p>Full audit trail of your financial activity</p>
      </div>

      <div className="stats-grid" style={{ marginBottom: 28 }}>
        <div className="stat-card" style={{ '--delay': '0s', '--accent-color': 'var(--brand-primary)' }}>
          <div className="stat-label">Total Transactions</div>
          <div className="stat-value">{transactions.length}</div>
          <div className="stat-sub">all time</div>
        </div>
        <div className="stat-card" style={{ '--delay': '0.06s', '--accent-color': 'var(--success)' }}>
          <div className="stat-label">Successful</div>
          <div className="stat-value" style={{ color: 'var(--success)' }}>
            {transactions.filter(t => t.status === 'SUCCESS').length}
          </div>
          <div className="stat-sub">completed without error</div>
        </div>
        <div className="stat-card" style={{ '--delay': '0.12s', '--accent-color': 'var(--danger)' }}>
          <div className="stat-label">Failed</div>
          <div className="stat-value" style={{ color: 'var(--danger)' }}>
            {transactions.filter(t => t.status === 'FAILED').length}
          </div>
          <div className="stat-sub">insufficient balance etc.</div>
        </div>
        <div className="stat-card" style={{ '--delay': '0.18s', '--accent-color': 'var(--brand-secondary)' }}>
          <div className="stat-label">Total Transferred</div>
          <div className="stat-value">₹{totalTransferred.toLocaleString('en-IN', { minimumFractionDigits: 0 })}</div>
          <div className="stat-sub">successful transfers only</div>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 10, marginBottom: 20, flexWrap: 'wrap' }}>
        {['ALL','TRANSFER','DEPOSIT','WITHDRAWAL'].map(t => (
          <button
            key={t}
            id={`filter-type-${t.toLowerCase()}`}
            className={`btn btn-sm ${filter === t ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setFilter(t)}
          >{t}</button>
        ))}
        <div style={{ marginLeft: 'auto', display: 'flex', gap: 8 }}>
          {['ALL','SUCCESS','FAILED'].map(s => (
            <button
              key={s}
              id={`filter-status-${s.toLowerCase()}`}
              className={`btn btn-sm ${statusFilter === s ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setStatusFilter(s)}
            >{s}</button>
          ))}
        </div>
      </div>

      <div className="card" style={{ padding: 0 }}>
        {loading ? (
          <div style={{ textAlign: 'center', padding: 60 }}><span className="spinner" /></div>
        ) : filtered.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">📋</div>
            <h3>No transactions found</h3>
            <p>{transactions.length === 0 ? 'Make your first transfer to see records here.' : 'Try adjusting the filters.'}</p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>#ID</th>
                  <th>Type</th>
                  <th>From → To</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Note</th>
                  <th>Date &amp; Time</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(tx => {
                  const dt = tx.timestamp
                    ? new Date(tx.timestamp).toLocaleString('en-IN', {
                        day: '2-digit', month: 'short', year: 'numeric',
                        hour: '2-digit', minute: '2-digit',
                      })
                    : '—'
                  const isCredit = tx.toAccountId === myAccountId;
                  
                  return (
                    <tr key={tx.id}>
                      <td style={{ color: 'var(--text-muted)', fontSize: '0.82rem' }}>#{tx.id}</td>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                          <span className={`tx-icon ${TYPE_CLASS[tx.type] || ''}`}>{TYPE_ICONS[tx.type] || '?'}</span>
                          <span style={{ fontWeight: 600, fontSize: '0.88rem' }}>{tx.type}</span>
                        </div>
                      </td>
                      <td style={{ fontSize: '0.86rem', color: 'var(--text-secondary)' }}>
                        {isCredit ? (
                          <span><span style={{color:'var(--success)'}}>From:</span> <b>{tx.senderName || `#${tx.fromAccountId}`}</b></span>
                        ) : (
                          <span><span style={{color:'var(--danger)'}}>To:</span> <b>{tx.receiverName || `#${tx.toAccountId}`}</b></span>
                        )}
                      </td>
                      <td className={isCredit ? "amount-positive" : "amount-negative"}>
                        {isCredit ? '+' : '-'} ₹{tx.amount?.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </td>
                      <td>
                        <span className={`badge ${STATUS_BADGE[tx.status] || 'badge-warning'}`}>
                          {tx.status}
                        </span>
                      </td>
                      <td style={{ color: 'var(--text-muted)', fontSize: '0.82rem', maxWidth: 160, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {tx.note || '—'}
                      </td>
                      <td style={{ color: 'var(--text-secondary)', fontSize: '0.82rem', whiteSpace: 'nowrap' }}>{dt}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {filtered.length > 0 && (
        <p style={{ textAlign: 'right', marginTop: 12, fontSize: '0.8rem', color: 'var(--text-muted)' }}>
          Showing {filtered.length} of {transactions.length} records
        </p>
      )}
    </div>
  )
}
