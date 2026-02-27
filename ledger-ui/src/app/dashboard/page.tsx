'use client';
import { useEffect, useState } from 'react';
import { getAllLoans, getAllSavingsAccounts, getTransactions } from '@/lib/api';

interface StatCard { label: string; value: string | number; icon: string; color: string; }

function StatCard({ label, value, icon, color }: StatCard) {
    return (
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div style={{
                width: '48px', height: '48px', borderRadius: '12px',
                background: color, display: 'flex', alignItems: 'center',
                justifyContent: 'center', fontSize: '1.25rem', flexShrink: 0,
            }}>{icon}</div>
            <div>
                <div style={{ fontSize: '1.5rem', fontWeight: 700 }}>{value}</div>
                <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>{label}</div>
            </div>
        </div>
    );
}

export default function DashboardPage() {
    const [loans, setLoans] = useState<any[]>([]);
    const [savings, setSavings] = useState<any[]>([]);
    const [txns, setTxns] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        Promise.all([
            getAllLoans().catch(() => []),
            getAllSavingsAccounts().catch(() => []),
            getTransactions().catch(() => []),
        ]).then(([l, s, t]) => {
            setLoans(l || []);
            setSavings(s || []);
            setTxns(t || []);
            setLoading(false);
        });
    }, []);

    const activeLoans = loans.filter(l => l.status?.value === 'ACTIVE').length;
    const activeSavings = savings.filter(s => s.status === 'ACTIVE').length;
    const todayTxns = txns.slice(0, 10);

    return (
        <div className="animate-fade-in">
            <div style={{ marginBottom: '2rem' }}>
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>Dashboard</h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Tổng quan hệ thống Core Ledger</p>
            </div>

            {loading ? (
                <div style={{ color: 'var(--text-secondary)' }}>Đang tải dữ liệu...</div>
            ) : (
                <>
                    {/* Stats Grid */}
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
                        <StatCard label="Credit Accounts" value={loans.length} icon="💳" color="rgba(99,102,241,0.2)" />
                        <StatCard label="Đang hoạt động" value={activeLoans} icon="✅" color="rgba(16,185,129,0.2)" />
                        <StatCard label="Savings Accounts" value={savings.length} icon="🏦" color="rgba(245,158,11,0.2)" />
                        <StatCard label="Giao dịch gần nhất" value={txns.length} icon="📋" color="rgba(139,92,246,0.2)" />
                    </div>

                    {/* Recent Transactions */}
                    <div className="card">
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
                            <h2 style={{ fontSize: '1rem', fontWeight: 600 }}>📋 Giao dịch gần đây</h2>
                            <a href="/dashboard/transactions" style={{ fontSize: '0.8125rem', color: 'var(--accent-hover)', textDecoration: 'none' }}>Xem tất cả →</a>
                        </div>
                        {todayTxns.length === 0 ? (
                            <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', textAlign: 'center', padding: '2rem' }}>
                                Chưa có giao dịch nào
                            </div>
                        ) : (
                            <div className="table-container">
                                <table>
                                    <thead>
                                        <tr>
                                            <th>ID</th>
                                            <th>Số tiền</th>
                                            <th>Loại</th>
                                            <th>Merchant</th>
                                            <th>Tài khoản</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {todayTxns.map((txn: any) => (
                                            <tr key={txn.id}>
                                                <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>#{txn.id}</td>
                                                <td style={{ fontWeight: 600 }}>
                                                    {txn.transactionType === 'CREDIT' ? '+' : '-'}
                                                    {Number(txn.amount).toLocaleString('en-US')} {txn.currency || 'USD'}
                                                </td>
                                                <td>
                                                    <span className={`badge ${txn.transactionType === 'CREDIT' ? 'badge-active' : 'badge-pending'}`}>
                                                        {txn.transactionType}
                                                    </span>
                                                </td>
                                                <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                                                    {txn.merchantName || '-'}
                                                </td>
                                                <td style={{ fontSize: '0.8125rem', fontFamily: 'monospace' }}>
                                                    {txn.accountId || '-'}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                </>
            )}
        </div>
    );
}
