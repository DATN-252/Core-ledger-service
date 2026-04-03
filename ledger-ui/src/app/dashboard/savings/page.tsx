'use client';
import { useEffect, useState } from 'react';
import { getAllSavingsAccounts, getBranches, savingsCommand } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPiggyBank, faCheck, faLock } from '@fortawesome/free-solid-svg-icons';
import { Pagination } from '@/components/Pagination';
import AppModal from '@/components/AppModal';

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
    const [status, setStatus] = useState('ALL');
    const [branchId, setBranchId] = useState('ALL');
    const [branches, setBranches] = useState<any[]>([]);
    const [sortBy, setSortBy] = useState('createdAt');
    const [sortDir, setSortDir] = useState('desc');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const [totalElements, setTotalElements] = useState(0);
    const [modal, setModal] = useState<{ title: string; message: string } | null>(null);

    useEffect(() => { getBranches().then(setBranches).catch(() => setBranches([])); }, []);
    useEffect(() => { loadAccounts(); }, [page, search, status, branchId, sortBy, sortDir]);

    async function loadAccounts() {
        setLoading(true);
        try {
            const data = await getAllSavingsAccounts(page, 10, {
                q: search || undefined,
                status,
                branchId,
                sortBy,
                sortDir,
            });
            setAccounts(data?.content || []);
            setTotalPages(data?.totalPages || 1);
            setTotalElements(data?.totalElements || 0);
        }
        catch { setAccounts([]); }
        finally { setLoading(false); }
    }

    async function handleCommand(id: string, command: 'activate' | 'lock') {
        setActionLoading(id + command);
        try { await savingsCommand(id, command); await loadAccounts(); }
        catch (e: any) {
            setModal({
                title: 'Không thể thực hiện thao tác',
                message: e.message || 'Đã có lỗi xảy ra.',
            });
        }
        finally { setActionLoading(null); }
    }

    return (
        <div className="animate-fade-in">
            <AppModal
                open={!!modal}
                title={modal?.title || ''}
                onClose={() => setModal(null)}
                footer={<button className="btn-primary" onClick={() => setModal(null)}>Đã hiểu</button>}
            >
                <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7 }}>{modal?.message}</p>
            </AppModal>
            <div style={{ marginBottom: '2rem' }}>
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                    <FontAwesomeIcon icon={faPiggyBank} style={{ marginRight: '0.5rem' }} />
                    Savings Accounts
                </h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Quản lý tài khoản tiền gửi ({totalElements} tài khoản)</p>
            </div>

            <div className="card">
                <div style={{ marginBottom: '1rem', display: 'grid', gridTemplateColumns: 'minmax(260px, 1.5fr) repeat(4, minmax(160px, 1fr))', gap: '0.75rem' }}>
                    <input
                        className="input"
                        placeholder="Tìm kiếm theo ID hoặc tên khách hàng..."
                        value={search}
                        onChange={e => { setSearch(e.target.value); setPage(0); }}
                    />
                    <select className="input" value={status} onChange={e => { setStatus(e.target.value); setPage(0); }}>
                        <option value="ALL">Tất cả trạng thái</option>
                        <option value="PENDING">PENDING</option>
                        <option value="ACTIVE">ACTIVE</option>
                        <option value="LOCKED">LOCKED</option>
                        <option value="CLOSED">CLOSED</option>
                    </select>
                    <select className="input" value={branchId} onChange={e => { setBranchId(e.target.value); setPage(0); }}>
                        <option value="ALL">Tất cả chi nhánh</option>
                        {branches.map((branch: any) => (
                            <option key={branch.branchId} value={branch.branchId}>
                                {branch.branchName}
                            </option>
                        ))}
                    </select>
                    <select className="input" value={sortBy} onChange={e => { setSortBy(e.target.value); setPage(0); }}>
                        <option value="createdAt">Mới tạo</option>
                        <option value="accountNumber">Số tài khoản</option>
                        <option value="clientName">Khách hàng</option>
                        <option value="balance">Số dư</option>
                        <option value="currency">Tiền tệ</option>
                        <option value="status">Trạng thái</option>
                    </select>
                    <select className="input" value={sortDir} onChange={e => { setSortDir(e.target.value); setPage(0); }}>
                        <option value="desc">Giảm dần</option>
                        <option value="asc">Tăng dần</option>
                    </select>
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
                                    <th>Chi nhánh</th>
                                    <th>Số dư</th>
                                    <th>Tiền tệ</th>
                                    <th>Trạng thái</th>
                                    <th>Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                {accounts.length === 0 ? (
                                    <tr>
                                        <td colSpan={7} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
                                            Không có tài khoản
                                        </td>
                                    </tr>
                                ) : accounts.map((acc: any) => {
                                    const status = acc.status || 'UNKNOWN';
                                    return (
                                        <tr key={acc.id}>
                                            <td style={{ fontFamily: 'monospace', fontSize: '0.8125rem', color: 'var(--accent-hover)' }}>{acc.id}</td>
                                            <td style={{ fontWeight: 500 }}>{acc.clientName || '—'}</td>
                                            <td>{acc.branchName || '—'}</td>
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

                <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
            </div>
        </div>
    );
}
