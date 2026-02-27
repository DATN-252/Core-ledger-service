'use client';
import { useEffect, useState } from 'react';
import { getAllSavingsAccounts, savingsCommand } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPiggyBank, faCheck, faLock } from '@fortawesome/free-solid-svg-icons';

const STATUS_BADGE: Record<string, string> = {
    ACTIVE: 'badge-active',
    PENDING: 'badge-pending',
    LOCKED: 'badge-locked',
};

export default function SavingsPage() {
    const [accounts, setAccounts] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState<string | null>(null);
    const [search, setSearch] = useState('');

    useEffect(() => { loadAccounts(); }, []);

    async function loadAccounts() {
        setLoading(true);
        try { setAccounts(await getAllSavingsAccounts()); }
        catch { setAccounts([]); }
        finally { setLoading(false); }
    }

    async function handleCommand(id: string, command: 'activate' | 'lock') {
        setActionLoading(id + command);
        try { await savingsCommand(id, command); await loadAccounts(); }
        catch (e: any) { alert(e.message); }
        finally { setActionLoading(null); }
    }

    const filtered = accounts.filter(a =>
        !search || a.id?.toLowerCase().includes(search.toLowerCase()) ||
        a.clientName?.toLowerCase().includes(search.toLowerCase())
    );

    return (
        <div className="animate-fade-in">
            <div style={{ marginBottom: '2rem' }}>
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                    <FontAwesomeIcon icon={faPiggyBank} style={{ marginRight: '0.5rem' }} />
                    Savings Accounts
                </h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Quản lý tài khoản tiền gửi</p>
            </div>

            <div className="card">
                <div style={{ marginBottom: '1rem' }}>
                    <input
                        className="input"
                        placeholder="Tìm kiếm theo ID hoặc tên khách hàng..."
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        style={{ maxWidth: '360px' }}
                    />
                </div>

                {loading ? (
                    <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>Đang tải...</div>
                ) : (
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>ID Tài khoản</th>
                                    <th>Khách hàng</th>
                                    <th>Số dư</th>
                                    <th>Tiền tệ</th>
                                    <th>Trạng thái</th>
                                    <th>Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filtered.length === 0 ? (
                                    <tr>
                                        <td colSpan={6} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
                                            Không có tài khoản
                                        </td>
                                    </tr>
                                ) : filtered.map((acc: any) => {
                                    const status = acc.status || 'UNKNOWN';
                                    return (
                                        <tr key={acc.id}>
                                            <td style={{ fontFamily: 'monospace', fontSize: '0.8125rem', color: 'var(--accent-hover)' }}>{acc.id}</td>
                                            <td style={{ fontWeight: 500 }}>{acc.clientName || '—'}</td>
                                            <td style={{ fontWeight: 600 }}>
                                                {Number(acc.balance || 0).toLocaleString('en-US')} {acc.currency || 'USD'}
                                            </td>
                                            <td style={{ color: 'var(--text-secondary)' }}>{acc.currency || 'VND'}</td>
                                            <td>
                                                <span className={`badge ${STATUS_BADGE[status] || 'badge-pending'}`}>
                                                    {status}
                                                </span>
                                            </td>
                                            <td>
                                                <div style={{ display: 'flex', gap: '0.5rem' }}>
                                                    {status === 'PENDING' && (
                                                        <button className="btn-success" disabled={!!actionLoading} onClick={() => handleCommand(acc.id, 'activate')}>
                                                            {actionLoading === acc.id + 'activate' ? '⏳' : <><FontAwesomeIcon icon={faCheck} /> Kích hoạt</>}
                                                        </button>
                                                    )}
                                                    {status === 'ACTIVE' && (
                                                        <button className="btn-danger" disabled={!!actionLoading} onClick={() => handleCommand(acc.id, 'lock')}>
                                                            {actionLoading === acc.id + 'lock' ? '⏳' : <><FontAwesomeIcon icon={faLock} /> Khóa</>}
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
