'use client';
import { useEffect, useState, use } from 'react';
import { useRouter } from 'next/navigation';
import { getClientAccounts, getTransactions, registerCustomer } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowLeft, faUser, faCreditCard, faPiggyBank, faHistory, faMobileAlt } from '@fortawesome/free-solid-svg-icons';
import Link from 'next/link';

export default function ClientDetailPage({ params }: { params: Promise<{ clientId: string }> }) {
    const { clientId } = use(params);
    const router = useRouter();
    const [clientData, setClientData] = useState<any>(null);
    const [transactions, setTransactions] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const data = await getClientAccounts(clientId);
                setClientData(data);

                // Fetch transactions for all accounts
                const allAccounts = [
                    ...(data.savingsAccounts || []),
                    ...(data.loanAccounts || [])
                ];

                const txPromises = allAccounts.map((acc: any) => getTransactions(acc.accountNumber));
                const txResults = await Promise.all(txPromises);

                // Flatten and sort
                let allTx = txResults.flat();
                allTx.sort((a, b) => new Date(b.transactionDate).getTime() - new Date(a.transactionDate).getTime());
                setTransactions(allTx);
            } catch (err) {
                console.error(err);
                alert('Không tìm thấy thông tin khách hàng');
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, [clientId]);

    if (loading) return <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Đang tải...</div>;
    if (!clientData) return <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Không có dữ liệu</div>;

    const { clientName, savingsAccounts = [], loanAccounts = [] } = clientData;

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
                {/* Savings Accounts */}
                <div className="card">
                    <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span><FontAwesomeIcon icon={faPiggyBank} style={{ marginRight: '0.5rem', color: 'var(--accent)' }} />Tài khoản Debit (Tiền gửi)</span>
                        <Link href={`/dashboard/savings/new?clientId=${clientId}`} className="btn-primary" style={{ fontSize: '0.75rem', padding: '0.3rem 0.6rem', textDecoration: 'none' }}>+ Thêm</Link>
                    </h2>
                    {savingsAccounts.length === 0 ? (
                        <div style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                            Chưa có tài khoản
                        </div>
                    ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                            {savingsAccounts.map((acc: any) => (
                                <div key={acc.accountNumber} style={{ border: '1px solid var(--border)', padding: '1rem', borderRadius: '8px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <div>
                                        <div style={{ fontWeight: 600, fontFamily: 'monospace' }}>{acc.accountNumber}</div>
                                        <div style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>Trạng thái: <span className={`badge ${acc.status === 'ACTIVE' ? 'badge-active' : 'badge-pending'}`}>{acc.status}</span></div>
                                    </div>
                                    <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>
                                        {Number(acc.balance || 0).toLocaleString('en-US')} USD
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* Loan Accounts */}
                <div className="card">
                    <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span><FontAwesomeIcon icon={faCreditCard} style={{ marginRight: '0.5rem', color: 'var(--accent)' }} />Tài khoản Credit (Tín dụng)</span>
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
                                            </div>
                                            <div style={{ textAlign: 'right' }}>
                                                <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>
                                                    {limit.toLocaleString('en-US')} USD
                                                </div>
                                                <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Hạn mức tổng</div>
                                            </div>
                                        </div>

                                        {/* Progress bar */}
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

            {/* Transaction History Section */}
            <div className="card">
                <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', display: 'flex', alignItems: 'center' }}>
                    <FontAwesomeIcon icon={faHistory} style={{ marginRight: '0.5rem', color: 'var(--accent)' }} />
                    Lịch sử giao dịch
                </h2>

                {transactions.length === 0 ? (
                    <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                        Không có giao dịch nào
                    </div>
                ) : (
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>Mã GD</th>
                                    <th>Ngày giờ</th>
                                    <th>Tài khoản</th>
                                    <th>Thương hiệu</th>
                                    <th>Loại</th>
                                    <th>Số tiền</th>
                                    <th>Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody>
                                {transactions.map((tx: any) => (
                                    <tr key={tx.id}>
                                        <td style={{ fontFamily: 'monospace', color: 'var(--text-secondary)' }}>#{tx.id}</td>
                                        <td>{new Date(tx.transactionDate).toLocaleString('vi-VN')}</td>
                                        <td style={{ fontWeight: 600 }}>{tx.accountNumber}</td>
                                        <td style={{ color: 'var(--text-secondary)' }}>{tx.merchantName || '—'}</td>
                                        <td>
                                            <span className={`badge ${tx.transactionType === 'DEPOSIT' || tx.transactionType === 'PAYMENT' ? 'badge-active' : 'badge-pending'}`}>
                                                {tx.transactionType}
                                            </span>
                                        </td>
                                        <td style={{
                                            fontWeight: 700,
                                            color: tx.transactionType === 'WITHDRAWAL' || tx.transactionType === 'CHARGE' ? 'var(--warning)' : 'var(--success)'
                                        }}>
                                            {tx.transactionType === 'WITHDRAWAL' || tx.transactionType === 'CHARGE' ? '-' : '+'}
                                            {tx.amount?.toLocaleString('en-US')} {tx.currency}
                                        </td>
                                        <td>
                                            {tx.status === 'FAILED' ? (
                                                <span className="badge" style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)', color: 'var(--error)' }}>THẤT BẠI</span>
                                            ) : (
                                                <span className="badge badge-active">THÀNH CÔNG</span>
                                            )}
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
