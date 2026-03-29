'use client';
import { useEffect, useState } from 'react';
import { getBranches, getTransactions } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowRight, faListUl } from '@fortawesome/free-solid-svg-icons';
import { Pagination } from '@/components/Pagination';
import Link from 'next/link';
import {
    getCounterpartyColumnTitle,
    getDisplayCounterpartyId,
    getDisplayCounterpartyName,
    getDisplayTransactionType,
    isNegativeTransaction,
    isPositiveTransaction,
} from '@/lib/transactionDisplay';

export default function TransactionsPage() {
    const [txns, setTxns] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState('');
    const [status, setStatus] = useState('ALL');
    const [type, setType] = useState('ALL');
    const [accountType, setAccountType] = useState('ALL');
    const [branchId, setBranchId] = useState('ALL');
    const [branches, setBranches] = useState<any[]>([]);
    const [sortBy, setSortBy] = useState('transactionDate');
    const [sortDir, setSortDir] = useState('desc');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const [totalElements, setTotalElements] = useState(0);

    useEffect(() => {
        getBranches().then(setBranches).catch(() => setBranches([]));
    }, []);

    useEffect(() => {
        setLoading(true);
        getTransactions(undefined, page, 50, {
            q: search || undefined,
            status,
            type,
            accountType,
            branchId,
            sortBy,
            sortDir,
        })
            .then(data => {
                setTxns(data?.content || []);
                setTotalPages(data?.totalPages || 1);
                setTotalElements(data?.totalElements || 0);
            })
            .catch(() => setTxns([]))
            .finally(() => setLoading(false));
    }, [page, search, status, type, accountType, branchId, sortBy, sortDir]);

    const formatAmount = (txn: any) => {
        const isNegative = isNegativeTransaction(txn);
        return `${isNegative ? '-' : '+'}${Number(txn.amount || 0).toLocaleString('en-US')} ${txn.currency || 'USD'}`;
    };

    const formatLocation = (txn: any) => {
        if (txn.location) return txn.location;
        if (txn.latitude != null && txn.longitude != null) {
            return `${txn.latitude.toFixed(4)}, ${txn.longitude.toFixed(4)}`;
        }
        return '—';
    };

    const formatTransactionType = (txn: any) => {
        return getDisplayTransactionType(txn);
    };

    const formatStatus = (status: string) => {
        switch (status) {
            case 'FAILED':
                return { label: 'THẤT BẠI', className: '', style: { backgroundColor: 'rgba(239, 68, 68, 0.1)', color: 'var(--error)' } };
            case 'REVERSED':
                return { label: 'ĐÃ ĐẢO', className: 'badge-pending', style: {} };
            case 'REFUNDED':
                return { label: 'ĐÃ HOÀN', className: 'badge-pending', style: {} };
            default:
                return { label: 'THÀNH CÔNG', className: 'badge-active', style: {} };
        }
    };

    return (
        <div className="animate-fade-in">
            <div style={{ marginBottom: '2rem' }}>
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                    <FontAwesomeIcon icon={faListUl} style={{ marginRight: '0.5rem' }} />
                    Lịch sử giao dịch
                </h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                    {totalElements} giao dịch tổng cộng
                </p>
            </div>

            <div className="card">
                <div style={{ marginBottom: '1rem', display: 'grid', gridTemplateColumns: 'minmax(260px, 2fr) repeat(6, minmax(140px, 1fr))', gap: '0.75rem' }}>
                    <input
                        className="input"
                        placeholder="Tìm theo merchant, ID giao dịch..."
                        value={search}
                        onChange={e => { setSearch(e.target.value); setPage(0); }}
                    />
                    <select className="input" value={status} onChange={e => { setStatus(e.target.value); setPage(0); }}>
                        <option value="ALL">Tất cả trạng thái</option>
                        <option value="SUCCESS">SUCCESS</option>
                        <option value="FAILED">FAILED</option>
                        <option value="REVERSED">REVERSED</option>
                        <option value="REFUNDED">REFUNDED</option>
                    </select>
                    <select className="input" value={type} onChange={e => { setType(e.target.value); setPage(0); }}>
                        <option value="ALL">Tất cả loại</option>
                        <option value="CHARGE">CHARGE</option>
                        <option value="WITHDRAWAL">WITHDRAWAL</option>
                        <option value="PAYMENT">PAYMENT</option>
                        <option value="REFUND">REFUND</option>
                        <option value="REVERSAL">REVERSAL</option>
                        <option value="SETTLEMENT">SETTLEMENT</option>
                        <option value="DEPOSIT">DEPOSIT</option>
                    </select>
                    <select className="input" value={accountType} onChange={e => { setAccountType(e.target.value); setPage(0); }}>
                        <option value="ALL">Tất cả tài khoản</option>
                        <option value="LOAN">LOAN</option>
                        <option value="SAVINGS">SAVINGS</option>
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
                        <option value="transactionDate">Thời gian</option>
                        <option value="amount">Số tiền</option>
                        <option value="transactionType">Loại</option>
                        <option value="merchantName">Merchant</option>
                        <option value="status">Trạng thái</option>
                        <option value="accountNumber">Tài khoản</option>
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
                        <table className="transaction-table transaction-table--full">
                            <thead>
                                <tr>
                                    <th>#ID</th>
                                    <th>Thời gian</th>
                                    <th>Số tiền</th>
                                    <th>Loại</th>
                                    <th>Đối tác ID</th>
                                    <th>Đối tác</th>
                                    <th>Chi nhánh / Vị trí</th>
                                    <th>Trạng thái</th>
                                    <th>Tài khoản</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                                {txns.length === 0 ? (
                                    <tr>
                                        <td colSpan={10} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
                                            Không có giao dịch
                                        </td>
                                    </tr>
                                ) : txns.map((txn: any) => (
                                    <tr key={txn.id}>
                                        <td className="transaction-cell-id">#{txn.id}</td>
                                        <td className="transaction-cell-time">
                                            {txn.transactionDate ? new Date(txn.transactionDate).toLocaleString('vi-VN') : '—'}
                                        </td>
                                        <td style={{ fontWeight: 600, color: isPositiveTransaction(txn) ? 'var(--success)' : 'var(--text-primary)' }}>
                                            {formatAmount(txn)}
                                        </td>
                                        <td>
                                            <span className={`badge ${isPositiveTransaction(txn) ? 'badge-active' : 'badge-pending'}`}>
                                                {formatTransactionType(txn)}
                                            </span>
                                        </td>
                                        <td style={{ fontFamily: 'monospace', fontSize: '0.8125rem' }} title={getCounterpartyColumnTitle(txn)}>
                                            {getDisplayCounterpartyId(txn) || <span style={{ color: 'var(--text-secondary)' }}>—</span>}
                                        </td>
                                        <td title={getCounterpartyColumnTitle(txn)}>{getDisplayCounterpartyName(txn) || <span style={{ color: 'var(--text-secondary)' }}>—</span>}</td>
                                        <td className="transaction-cell-location" title={formatLocation(txn)}>
                                            <div>{txn.branchName || '—'}</div>
                                            <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginTop: '0.125rem' }}>{formatLocation(txn)}</div>
                                        </td>
                                        <td>
                                            {(() => {
                                                const badge = formatStatus(txn.status);
                                                return (
                                                    <span className={`badge ${badge.className}`} style={badge.style}>
                                                        {badge.label}
                                                    </span>
                                                );
                                            })()}
                                        </td>
                                        <td className="transaction-cell-account">
                                            <div style={{ fontFamily: 'monospace' }}>{txn.accountNumber || '—'}</div>
                                            <div style={{ color: 'var(--text-secondary)', marginTop: '0.125rem' }}>{txn.accountType || ''}</div>
                                        </td>
                                        <td className="transaction-cell-action">
                                            <Link
                                                href={`/dashboard/transactions/${txn.id}`}
                                                className="transaction-detail-link"
                                            >
                                                Chi tiết
                                                <FontAwesomeIcon icon={faArrowRight} />
                                            </Link>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
            </div>
        </div>
    );
}
