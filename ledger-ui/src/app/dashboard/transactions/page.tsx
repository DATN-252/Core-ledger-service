'use client';
import { useEffect, useState } from 'react';
import { getTransactions } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faListUl } from '@fortawesome/free-solid-svg-icons';
import { Pagination } from '@/components/Pagination';

export default function TransactionsPage() {
    const [txns, setTxns] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);

    useEffect(() => {
        setLoading(true);
        getTransactions(undefined, page, 10)
            .then(data => {
                setTxns(data?.content || []);
                setTotalPages(data?.totalPages || 1);
            })
            .catch(() => setTxns([]))
            .finally(() => setLoading(false));
    }, [page]);

    const filtered = txns.filter(t =>
        !search ||
        t.merchantName?.toLowerCase().includes(search.toLowerCase()) ||
        t.merchantId?.toLowerCase().includes(search.toLowerCase()) ||
        String(t.id).includes(search)
    );

    return (
        <div className="animate-fade-in">
            <div style={{ marginBottom: '2rem' }}>
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                    <FontAwesomeIcon icon={faListUl} style={{ marginRight: '0.5rem' }} />
                    Lịch sử giao dịch
                </h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                    {txns.length} giao dịch tổng cộng
                </p>
            </div>

            <div className="card">
                <div style={{ marginBottom: '1rem' }}>
                    <input
                        className="input"
                        placeholder="Tìm theo merchant, ID giao dịch..."
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
                                    <th>#ID</th>
                                    <th>Số tiền</th>
                                    <th>Loại</th>
                                    <th>Merchant ID</th>
                                    <th>Merchant Name</th>
                                    <th>Vị trí</th>
                                    <th>Trạng thái</th>
                                    <th>Tài khoản</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filtered.length === 0 ? (
                                    <tr>
                                        <td colSpan={6} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
                                            Không có giao dịch
                                        </td>
                                    </tr>
                                ) : filtered.map((txn: any) => (
                                    <tr key={txn.id}>
                                        <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>#{txn.id}</td>
                                        <td style={{ fontWeight: 600, color: txn.transactionType === 'CREDIT' || txn.transactionType === 'DEPOSIT' ? 'var(--success)' : 'var(--text-primary)' }}>
                                            {txn.transactionType === 'CREDIT' || txn.transactionType === 'DEPOSIT' ? '+' : '-'}
                                            {Number(txn.amount).toLocaleString('en-US')} {txn.currency || 'USD'}
                                        </td>
                                        <td>
                                            <span className={`badge ${txn.transactionType === 'CREDIT' || txn.transactionType === 'DEPOSIT' ? 'badge-active' : 'badge-pending'}`}>
                                                {txn.transactionType === 'CHARGE' ? (txn.merchantName ? 'CREDIT' : 'CHARGE/FEE') : (txn.transactionType || 'UNKNOWN')}
                                            </span>
                                        </td>
                                        <td style={{ fontFamily: 'monospace', fontSize: '0.8125rem' }}>
                                            {txn.merchantId || <span style={{ color: 'var(--text-secondary)' }}>—</span>}
                                        </td>
                                        <td>{txn.merchantName || <span style={{ color: 'var(--text-secondary)' }}>—</span>}</td>
                                        <td>
                                            {txn.location ? (
                                                <span title={`Tọa độ: ${txn.latitude}, ${txn.longitude}`}>
                                                    📍 {txn.location}
                                                </span>
                                            ) : (
                                                <span style={{ color: 'var(--text-secondary)' }}>—</span>
                                            )}
                                        </td>
                                        <td>
                                            {txn.status === 'FAILED' ? (
                                                <span className="badge" style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)', color: 'var(--error)' }}>THẤT BẠI</span>
                                            ) : (
                                                <span className="badge badge-active">THÀNH CÔNG</span>
                                            )}
                                        </td>
                                        <td style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>{txn.accountId || txn.loanId || '—'}</td>
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
