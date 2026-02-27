'use client';
import { useEffect, useState } from 'react';
import { getAllLoans, loanCommand } from '@/lib/api';

const STATUS_BADGE: Record<string, string> = {
    ACTIVE: 'badge-active',
    PENDING: 'badge-pending',
    LOCKED: 'badge-locked',
};

export default function LoansPage() {
    const [loans, setLoans] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState<string | null>(null);
    const [search, setSearch] = useState('');

    useEffect(() => { loadLoans(); }, []);

    async function loadLoans() {
        setLoading(true);
        try { setLoans(await getAllLoans()); }
        catch { setLoans([]); }
        finally { setLoading(false); }
    }

    async function handleCommand(loanId: string, command: 'activate' | 'lock' | 'unlock') {
        setActionLoading(loanId + command);
        try { await loanCommand(loanId, command); await loadLoans(); }
        catch (e: any) { alert(e.message); }
        finally { setActionLoading(null); }
    }

    const filtered = loans.filter(l =>
        !search || l.id?.toLowerCase().includes(search.toLowerCase()) ||
        l.clientName?.toLowerCase().includes(search.toLowerCase())
    );

    return (
        <div className="animate-fade-in">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '2rem' }}>
                <div>
                    <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>💳 Credit Accounts</h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Quản lý tài khoản tín dụng</p>
                </div>
            </div>

            <div className="card">
                <div style={{ marginBottom: '1rem' }}>
                    <input
                        className="input"
                        placeholder="🔍 Tìm kiếm theo ID hoặc tên khách hàng..."
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        style={{ maxWidth: '360px' }}
                    />
                </div>

                {loading ? (
                    <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>Đang tải...</div>
                ) : filtered.length === 0 ? (
                    <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
                        Không tìm thấy tài khoản nào
                    </div>
                ) : (
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>ID Tài khoản</th>
                                    <th>Khách hàng</th>
                                    <th>Hạn mức</th>
                                    <th>Đã dùng</th>
                                    <th>Khả dụng</th>
                                    <th>Trạng thái</th>
                                    <th>Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filtered.map((loan: any) => {
                                    const limit = Number(loan.principal || 0);
                                    const used = Number(loan.principalOutstanding || 0);
                                    const available = limit - used;
                                    const status = loan.status?.value || 'UNKNOWN';
                                    return (
                                        <tr key={loan.id}>
                                            <td style={{ fontFamily: 'monospace', fontSize: '0.8125rem', color: 'var(--accent-hover)' }}>
                                                {loan.id || loan.accountNo}
                                            </td>
                                            <td style={{ fontWeight: 500 }}>{loan.clientName || '—'}</td>
                                            <td>{limit.toLocaleString('vi-VN')} ₫</td>
                                            <td style={{ color: used > 0 ? 'var(--warning)' : 'var(--text-secondary)' }}>
                                                {used.toLocaleString('vi-VN')} ₫
                                            </td>
                                            <td style={{ color: available > 0 ? 'var(--success)' : 'var(--danger)', fontWeight: 600 }}>
                                                {available.toLocaleString('vi-VN')} ₫
                                            </td>
                                            <td>
                                                <span className={`badge ${STATUS_BADGE[status] || 'badge-pending'}`}>
                                                    {status}
                                                </span>
                                            </td>
                                            <td>
                                                <div style={{ display: 'flex', gap: '0.5rem' }}>
                                                    {status === 'PENDING' && (
                                                        <button
                                                            className="btn-success"
                                                            disabled={!!actionLoading}
                                                            onClick={() => handleCommand(loan.id || loan.accountNo, 'activate')}
                                                        >
                                                            {actionLoading === (loan.id || loan.accountNo) + 'activate' ? '⏳' : '✅ Kích hoạt'}
                                                        </button>
                                                    )}
                                                    {status === 'ACTIVE' && (
                                                        <button
                                                            className="btn-danger"
                                                            disabled={!!actionLoading}
                                                            onClick={() => handleCommand(loan.id || loan.accountNo, 'lock')}
                                                        >
                                                            {actionLoading === (loan.id || loan.accountNo) + 'lock' ? '⏳' : '🔒 Khóa'}
                                                        </button>
                                                    )}
                                                    {status === 'LOCKED' && (
                                                        <button
                                                            className="btn-success"
                                                            disabled={!!actionLoading}
                                                            onClick={() => handleCommand(loan.id || loan.accountNo, 'unlock')}
                                                        >
                                                            {actionLoading === (loan.id || loan.accountNo) + 'unlock' ? '⏳' : '🔓 Mở khóa'}
                                                        </button>
                                                    )}
                                                </div>
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
}
