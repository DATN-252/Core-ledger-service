'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { getAllLoans, getBranches, loanCommand } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCreditCard, faCheck, faLock, faLockOpen, faFileInvoiceDollar } from '@fortawesome/free-solid-svg-icons';
import { Pagination } from '@/components/Pagination';
import AppModal from '@/components/AppModal';

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
    useEffect(() => { loadLoans(); }, [page, search, status, branchId, sortBy, sortDir]);

    async function loadLoans() {
        setLoading(true);
        try {
            const data = await getAllLoans(page, 10, {
                q: search || undefined,
                status,
                branchId,
                sortBy,
                sortDir,
            });
            setLoans(data?.content || []);
            setTotalPages(data?.totalPages || 1);
            setTotalElements(data?.totalElements || 0);
        }
        catch { setLoans([]); }
        finally { setLoading(false); }
    }

    async function handleCommand(loanId: string, command: 'activate' | 'lock' | 'unlock') {
        setActionLoading(loanId + command);
        try { await loanCommand(loanId, command); await loadLoans(); }
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
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '2rem' }}>
                <div>
                    <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                        <FontAwesomeIcon icon={faCreditCard} style={{ marginRight: '0.5rem' }} />
                        Credit Accounts
                    </h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Quản lý tài khoản tín dụng ({totalElements} tài khoản)</p>
                </div>
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
                        <option value="principal">Hạn mức</option>
                        <option value="outstanding">Dư nợ</option>
                        <option value="status">Trạng thái</option>
                    </select>
                    <select className="input" value={sortDir} onChange={e => { setSortDir(e.target.value); setPage(0); }}>
                        <option value="desc">Giảm dần</option>
                        <option value="asc">Tăng dần</option>
                    </select>
                </div>

                {loading ? (
                    <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>Đang tải...</div>
                ) : loans.length === 0 ? (
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
                                    <th>Chi nhánh</th>
                                    <th>Hạn mức</th>
                                    <th>Đã dùng</th>
                                    <th>Khả dụng</th>
                                    <th>Trạng thái</th>
                                    <th>Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                {loans.map((loan: any) => {
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
                                            <td>{loan.branchName || '—'}</td>
                                            <td>{limit.toLocaleString('en-US')} {loan.currency?.code || 'USD'}</td>
                                            <td style={{ color: used > 0 ? 'var(--warning)' : 'var(--text-secondary)' }}>
                                                {used.toLocaleString('en-US')} {loan.currency?.code || 'USD'}
                                            </td>
                                            <td style={{ color: available > 0 ? 'var(--success)' : 'var(--danger)', fontWeight: 600 }}>
                                                {available.toLocaleString('en-US')} {loan.currency?.code || 'USD'}
                                            </td>
                                            <td>
                                                <span className={`badge ${STATUS_BADGE[status] || 'badge-pending'}`}>
                                                    {status}
                                                </span>
                                            </td>
                                            <td>
                                                <div style={{ display: 'flex', gap: '0.5rem' }}>
                                                    <Link
                                                        href={`/dashboard/loans/${loan.id || loan.accountNo}/statements`}
                                                        className="btn-secondary"
                                                        style={{ fontSize: '0.8125rem', padding: '0.5rem 0.9rem', textDecoration: 'none' }}
                                                    >
                                                        <FontAwesomeIcon icon={faFileInvoiceDollar} /> Sao kê
                                                    </Link>
                                                    {status === 'PENDING' && (
                                                        <button
                                                            className="btn-success"
                                                            disabled={!!actionLoading}
                                                            onClick={() => handleCommand(loan.id || loan.accountNo, 'activate')}
                                                        >
                                                            {actionLoading === (loan.id || loan.accountNo) + 'activate' ? '⏳' : <><FontAwesomeIcon icon={faCheck} /> Kích hoạt</>}
                                                        </button>
                                                    )}
                                                    {status === 'ACTIVE' && (
                                                        <button
                                                            className="btn-danger"
                                                            disabled={!!actionLoading}
                                                            onClick={() => handleCommand(loan.id || loan.accountNo, 'lock')}
                                                        >
                                                            {actionLoading === (loan.id || loan.accountNo) + 'lock' ? '⏳' : <><FontAwesomeIcon icon={faLock} /> Khóa</>}
                                                        </button>
                                                    )}
                                                    {status === 'LOCKED' && (
                                                        <button
                                                            className="btn-success"
                                                            disabled={!!actionLoading}
                                                            onClick={() => handleCommand(loan.id || loan.accountNo, 'unlock')}
                                                        >
                                                            {actionLoading === (loan.id || loan.accountNo) + 'unlock' ? '⏳' : <><FontAwesomeIcon icon={faLockOpen} /> Mở khóa</>}
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
