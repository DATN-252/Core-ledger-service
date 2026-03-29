'use client';
import { useEffect, useState, use } from 'react';
import { depositToSavingsAccount, getClientAccounts, getTransactions } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowLeft, faArrowRight, faUser, faCreditCard, faPiggyBank, faHistory, faLocationDot, faMobileAlt, faMoneyBillTransfer } from '@fortawesome/free-solid-svg-icons';
import Link from 'next/link';
import { getDisplayCounterpartyName, getDisplayTransactionType, isNegativeTransaction, isPositiveTransaction } from '@/lib/transactionDisplay';

export default function ClientDetailPage({ params }: { params: Promise<{ clientId: string }> }) {
    const { clientId } = use(params);
    const [clientData, setClientData] = useState<any>(null);
    const [transactions, setTransactions] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [depositAccountId, setDepositAccountId] = useState<string | null>(null);
    const [depositAmount, setDepositAmount] = useState('');
    const [depositLoadingId, setDepositLoadingId] = useState<string | null>(null);

    async function fetchData() {
        try {
            const data = await getClientAccounts(clientId);
            setClientData(data);

            const allAccounts = [
                ...(data.savingsAccounts || []),
                ...(data.loanAccounts || [])
            ];

            const txPromises = allAccounts.map((acc: any) => getTransactions(acc.accountNumber, 0, 50));
            const txResults = await Promise.all(txPromises);

            let allTx = txResults.map((res: any) => res?.content || []).flat();
            allTx.sort((a: any, b: any) => new Date(b.transactionDate).getTime() - new Date(a.transactionDate).getTime());
            setTransactions(allTx.slice(0, 50));
        } catch (err) {
            console.error(err);
            alert('Không tìm thấy thông tin khách hàng');
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        fetchData();
    }, [clientId]);

    async function handleDeposit(accountNumber: string) {
        const amount = Number(depositAmount);
        if (!amount || amount <= 0) {
            alert('Số tiền nạp phải lớn hơn 0');
            return;
        }

        setDepositLoadingId(accountNumber);
        try {
            await depositToSavingsAccount(accountNumber, amount);
            setDepositAmount('');
            setDepositAccountId(null);
            await fetchData();
            alert('Nạp tiền thành công');
        } catch (e: any) {
            alert(e.message || 'Không thể nạp tiền');
        } finally {
            setDepositLoadingId(null);
        }
    }

    if (loading) return <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Đang tải...</div>;
    if (!clientData) return <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Không có dữ liệu</div>;

    const { clientName, homeBranchName, homeBranchId, savingsAccounts = [], loanAccounts = [] } = clientData;

    const formatAmount = (tx: any) => {
        const isNegative = isNegativeTransaction(tx);
        return `${isNegative ? '-' : '+'}${Number(tx.amount || 0).toLocaleString('en-US')} ${tx.currency || 'USD'}`;
    };

    const formatType = (tx: any) => getDisplayTransactionType(tx);

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

    const formatLocation = (tx: any) => {
        if (tx.location) return tx.location;
        if (tx.latitude != null && tx.longitude != null) {
            return `${tx.latitude.toFixed(4)}, ${tx.longitude.toFixed(4)}`;
        }
        return '—';
    };

    return (
        <div className="animate-fade-in">
            <div style={{ marginBottom: '2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                    <Link href="/dashboard/clients" style={{ color: 'var(--text-secondary)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', fontSize: '0.875rem' }}>
                        <FontAwesomeIcon icon={faArrowLeft} /> Quay lại danh sách
                    </Link>
                    <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                        <FontAwesomeIcon icon={faUser} style={{ marginRight: '0.5rem' }} />
                        {clientName}
                    </h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                        Mã khách hàng: {clientId}
                    </p>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
                        Chi nhánh quản lý: {homeBranchName ? `${homeBranchName} (${homeBranchId})` : '—'}
                    </p>
                </div>

                <Link
                    href={`/dashboard/clients/${clientId}/register-app?clientName=${encodeURIComponent(clientName)}`}
                    className="btn-primary"
                    style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.5rem 1rem', textDecoration: 'none' }}>
                    <FontAwesomeIcon icon={faMobileAlt} />
                    Tạo tài khoản App
                </Link>
            </div>

            <div style={{ display: 'grid', gap: '2rem', gridTemplateColumns: '1fr 1fr', marginBottom: '2rem' }}>
                <div className="card">
                    <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span><FontAwesomeIcon icon={faPiggyBank} style={{ marginRight: '0.5rem', color: 'var(--accent)' }} />Tài khoản Debit</span>
                        <Link href={`/dashboard/savings/new?clientId=${clientId}`} className="btn-primary" style={{ fontSize: '0.75rem', padding: '0.3rem 0.6rem', textDecoration: 'none' }}>+ Thêm</Link>
                    </h2>
                    {savingsAccounts.length === 0 ? (
                        <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                            Chưa có tài khoản
                        </div>
                    ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                            {savingsAccounts.map((acc: any) => (
                                <div key={acc.accountNumber} style={{ border: '1px solid var(--border)', padding: '1rem', borderRadius: '8px' }}>
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
                                        <div>
                                            <div style={{ fontWeight: 600, fontFamily: 'monospace' }}>{acc.accountNumber}</div>
                                            <div style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>Trạng thái: <span className={`badge ${acc.status === 'ACTIVE' ? 'badge-active' : 'badge-pending'}`}>{acc.status}</span></div>
                                            <div style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem', marginTop: '0.25rem' }}>Chi nhánh: {acc.branchName || '—'}</div>
                                        </div>
                                        <div style={{ textAlign: 'right' }}>
                                            <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>
                                                {Number(acc.balance || 0).toLocaleString('en-US')} USD
                                            </div>
                                            {acc.status === 'ACTIVE' ? (
                                                <button
                                                    className="btn-primary"
                                                    style={{ marginTop: '0.75rem', padding: '0.45rem 0.85rem', fontSize: '0.8125rem' }}
                                                    onClick={() => {
                                                        setDepositAccountId(depositAccountId === acc.accountNumber ? null : acc.accountNumber);
                                                        setDepositAmount('');
                                                    }}
                                                >
                                                    <FontAwesomeIcon icon={faMoneyBillTransfer} />
                                                    Nạp tiền
                                                </button>
                                            ) : null}
                                        </div>
                                    </div>

                                    {depositAccountId === acc.accountNumber ? (
                                        <div style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid var(--border)', display: 'flex', gap: '0.75rem', alignItems: 'end', flexWrap: 'wrap' }}>
                                            <div style={{ minWidth: '180px' }}>
                                                <label className="info-label" style={{ display: 'block', marginBottom: '0.4rem' }}>Số tiền nạp (USD)</label>
                                                <input
                                                    className="input"
                                                    type="number"
                                                    min="0"
                                                    step="0.01"
                                                    value={depositAmount}
                                                    onChange={(e) => setDepositAmount(e.target.value)}
                                                    placeholder="Nhập số tiền"
                                                />
                                            </div>
                                            <button
                                                className="btn-primary"
                                                onClick={() => handleDeposit(acc.accountNumber)}
                                                disabled={depositLoadingId === acc.accountNumber}
                                            >
                                                {depositLoadingId === acc.accountNumber ? 'Đang nạp...' : 'Xác nhận nạp'}
                                            </button>
                                            <button
                                                className="btn-secondary"
                                                onClick={() => {
                                                    setDepositAccountId(null);
                                                    setDepositAmount('');
                                                }}
                                                disabled={depositLoadingId === acc.accountNumber}
                                            >
                                                Hủy
                                            </button>
                                        </div>
                                    ) : null}
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <div className="card">
                    <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span><FontAwesomeIcon icon={faCreditCard} style={{ marginRight: '0.5rem', color: 'var(--accent)' }} />Tài khoản Credit</span>
                        <Link href={`/dashboard/loans/new?clientId=${clientId}`} className="btn-primary" style={{ fontSize: '0.75rem', padding: '0.3rem 0.6rem', textDecoration: 'none' }}>+ Thêm</Link>
                    </h2>
                    {loanAccounts.length === 0 ? (
                        <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                            Chưa có tài khoản tín dụng
                        </div>
                    ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                            {loanAccounts.map((acc: any) => {
                                const used = Number(acc.principalOutstanding || 0);
                                const limit = Number(acc.principal || 0);
                                const percent = limit > 0 ? (used / limit) * 100 : 0;
                                return (
                                    <div key={acc.accountNumber} style={{ border: '1px solid var(--border)', padding: '1rem', borderRadius: '8px' }}>
                                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                                            <div>
                                                <div style={{ fontWeight: 600, fontFamily: 'monospace' }}>{acc.accountNumber}</div>
                                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>Trạng thái: <span className={`badge ${acc.status === 'ACTIVE' ? 'badge-active' : 'badge-pending'}`}>{acc.status}</span></div>
                                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem', marginTop: '0.25rem' }}>Chi nhánh: {acc.branchName || '—'}</div>
                                            </div>
                                            <div style={{ textAlign: 'right' }}>
                                                <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>
                                                    {limit.toLocaleString('en-US')} USD
                                                </div>
                                                <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Hạn mức tổng</div>
                                            </div>
                                        </div>

                                        <div style={{ marginTop: '1rem' }}>
                                            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', marginBottom: '0.25rem' }}>
                                                <span style={{ color: 'var(--text-secondary)' }}>Đã dùng: <span style={{ color: 'var(--warning)', fontWeight: 600 }}>{used.toLocaleString('en-US')}</span></span>
                                                <span style={{ color: 'var(--text-secondary)' }}>Khả dụng: <span style={{ color: 'var(--success)', fontWeight: 600 }}>{(limit - used).toLocaleString('en-US')}</span></span>
                                            </div>
                                            <div style={{ height: '6px', background: 'var(--bg-secondary)', borderRadius: '3px', overflow: 'hidden' }}>
                                                <div style={{ height: '100%', background: percent > 90 ? 'var(--danger)' : 'var(--warning)', width: `${percent}%` }}></div>
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>
            </div>

            <div className="card">
                <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', display: 'flex', alignItems: 'center' }}>
                    <FontAwesomeIcon icon={faHistory} style={{ marginRight: '0.5rem', color: 'var(--accent)' }} />
                    Lịch sử giao dịch
                </h2>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '1rem' }}>
                    Hiển thị 50 giao dịch mới nhất của toàn bộ tài khoản thuộc khách hàng này.
                </p>

                {transactions.length === 0 ? (
                    <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                        Không có giao dịch nào
                    </div>
                ) : (
                    <div className="table-container">
                        <table className="transaction-table transaction-table--client">
                            <thead>
                                <tr>
                                    <th>Mã GD</th>
                                    <th>Ngày giờ</th>
                                    <th>Tài khoản</th>
                                    <th>Thương hiệu</th>
                                    <th>Vị trí</th>
                                    <th>Loại</th>
                                    <th>Số tiền</th>
                                    <th>Trạng thái</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                                {transactions.map((tx: any) => (
                                    <tr key={tx.id}>
                                        <td className="transaction-cell-id">#{tx.id}</td>
                                        <td className="transaction-cell-time">{new Date(tx.transactionDate).toLocaleString('vi-VN')}</td>
                                        <td className="transaction-cell-account">
                                            <div style={{ fontWeight: 600 }}>{tx.accountNumber}</div>
                                            <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginTop: '0.125rem' }}>{tx.accountType || ''}</div>
                                        </td>
                                        <td style={{ color: 'var(--text-secondary)' }}>{getDisplayCounterpartyName(tx)}</td>
                                        <td className="transaction-cell-location" title={formatLocation(tx)}>
                                            <span className="transaction-location-content">
                                                <FontAwesomeIcon icon={faLocationDot} style={{ color: 'var(--text-secondary)' }} />
                                                {formatLocation(tx)}
                                            </span>
                                        </td>
                                        <td>
                                            <span className={`badge ${isPositiveTransaction(tx) ? 'badge-active' : 'badge-pending'}`}>
                                                {formatType(tx)}
                                            </span>
                                        </td>
                                        <td style={{
                                            fontWeight: 700,
                                            color: isNegativeTransaction(tx) ? 'var(--warning)' : 'var(--success)'
                                        }}>
                                            {formatAmount(tx)}
                                        </td>
                                        <td>
                                            {(() => {
                                                const badge = formatStatus(tx.status);
                                                return (
                                                    <span className={`badge ${badge.className}`} style={badge.style}>
                                                        {badge.label}
                                                    </span>
                                                );
                                            })()}
                                        </td>
                                        <td className="transaction-cell-action">
                                            <Link
                                                href={`/dashboard/transactions/${tx.id}`}
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
            </div>
        </div>
    );
}
