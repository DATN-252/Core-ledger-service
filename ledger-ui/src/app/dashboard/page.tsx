'use client';
import { useEffect, useState, useMemo } from 'react';
import { getAllLoans, getAllSavingsAccounts, getTransactions, getAllClients } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faCreditCard, faPiggyBank, faListUl, faUsers,
    faArrowTrendUp, faArrowTrendDown, faBuildingColumns, faChartBar
} from '@fortawesome/free-solid-svg-icons';
import Link from 'next/link';
import {
    AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
    PieChart, Pie, Cell, Legend, BarChart, Bar
} from 'recharts';

// ─── KPI Card ─────────────────────────────────────────────────────────────────
function KpiCard({ label, value, sub, icon, accent, trend }: {
    label: string; value: string | number; sub?: string;
    icon: any; accent: string; trend?: { value: number; label: string };
}) {
    return (
        <div className="card" style={{ position: 'relative', overflow: 'hidden' }}>
            {/* Glow blob */}
            <div style={{
                position: 'absolute', top: '-20px', right: '-20px',
                width: '100px', height: '100px', borderRadius: '50%',
                background: accent, opacity: 0.15, filter: 'blur(30px)',
                pointerEvents: 'none'
            }} />
            <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '1rem' }}>
                <div style={{
                    width: '44px', height: '44px', borderRadius: '12px',
                    background: accent + '22', display: 'flex', alignItems: 'center',
                    justifyContent: 'center', fontSize: '1.1rem', color: accent,
                }}>
                    <FontAwesomeIcon icon={icon} />
                </div>
                {trend && (
                    <div style={{
                        display: 'flex', alignItems: 'center', gap: '0.3rem',
                        fontSize: '0.75rem', fontWeight: 600,
                        color: trend.value >= 0 ? '#10b981' : '#ef4444'
                    }}>
                        <FontAwesomeIcon icon={trend.value >= 0 ? faArrowTrendUp : faArrowTrendDown} />
                        {Math.abs(trend.value)}% {trend.label}
                    </div>
                )}
            </div>
            <div style={{ fontSize: '2rem', fontWeight: 800, lineHeight: 1, marginBottom: '0.35rem' }}>{value}</div>
            <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '0.2rem' }}>{label}</div>
            {sub && <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>{sub}</div>}
        </div>
    );
}

// ─── Recharts Tooltip ─────────────────────────────────────────────────────────
function CustomTooltip({ active, payload, label }: any) {
    if (active && payload && payload.length) {
        return (
            <div style={{
                background: 'var(--bg-card)', border: '1px solid var(--border)',
                borderRadius: '8px', padding: '0.75rem 1rem', fontSize: '0.8125rem'
            }}>
                <div style={{ color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>{label}</div>
                {payload.map((p: any) => (
                    <div key={p.name} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                        <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: p.color }} />
                        <span style={{ color: 'var(--text-secondary)' }}>{p.name}:</span>
                        <span style={{ fontWeight: 600, color: p.color }}>{p.value}</span>
                    </div>
                ))}
            </div>
        );
    }
    return null;
}

// ─── Main Dashboard Page ──────────────────────────────────────────────────────
export default function DashboardPage() {
    const [loans, setLoans] = useState<any[]>([]);
    const [savings, setSavings] = useState<any[]>([]);
    const [txns, setTxns] = useState<any[]>([]);
    const [clients, setClients] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        Promise.all([
            getAllLoans(0, 10000).then(res => res.content || []).catch(() => []),
            getAllSavingsAccounts(0, 10000).then(res => res.content || []).catch(() => []),
            getTransactions(undefined, 0, 10000).then(res => res.content || []).catch(() => []),
            getAllClients(0, 10000).then(res => res.content || []).catch(() => []),
        ]).then(([l, s, t, c]) => {
            setLoans(l);
            setSavings(s);
            setTxns(t);
            setClients(c);
            setLoading(false);
        });
    }, []);

    // ── Derived stats ──────────────────────────────────────────────────────────
    const activeLoans = loans.filter(l => l.status?.value === 'ACTIVE').length;
    const lockedLoans = loans.filter(l => l.status?.value === 'LOCKED').length;
    const activeSavings = savings.filter(s => s.status === 'ACTIVE').length;

    const totalCreditLimit = useMemo(() =>
        loans.reduce((sum, l) => sum + Number(l.principal || 0), 0), [loans]);

    const totalOutstanding = useMemo(() =>
        loans.reduce((sum, l) => sum + Number(l.principalOutstanding || 0), 0), [loans]);

    const successCount = txns.filter(t => t.status !== 'FAILED').length;
    const failedCount = txns.filter(t => t.status === 'FAILED').length;

    // ── Chart: Group transactions by date ─────────────────────────────────────
    const txnByDay = useMemo(() => {
        const map: Record<string, { date: string; success: number; failed: number; total: number }> = {};
        txns.forEach(t => {
            const day = t.transactionDate
                ? new Date(t.transactionDate).toLocaleDateString('vi-VN', { month: '2-digit', day: '2-digit' })
                : 'N/A';
            if (!map[day]) map[day] = { date: day, success: 0, failed: 0, total: 0 };
            map[day].total++;
            if (t.status === 'FAILED') map[day].failed++;
            else map[day].success++;
        });
        return Object.values(map).slice(-14); // last 14 days
    }, [txns]);

    // ── Chart: Transaction types ──────────────────────────────────────────────
    const txnTypeData = useMemo(() => {
        const map: Record<string, number> = {};
        txns.forEach(t => {
            const type = t.transactionType || 'UNKNOWN';
            map[type] = (map[type] || 0) + 1;
        });
        return Object.entries(map).map(([name, value]) => ({ name, value }));
    }, [txns]);

    // ── Chart: Account status (Loan) ──────────────────────────────────────────
    const loanStatusData = [
        { name: 'Active', value: activeLoans, color: '#10b981' },
        { name: 'Locked', value: lockedLoans, color: '#ef4444' },
        { name: 'Pending', value: loans.length - activeLoans - lockedLoans, color: '#f59e0b' },
    ].filter(d => d.value > 0);

    const PIE_COLORS = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#14b8a6'];

    const recentTxns = txns.slice(0, 8);

    const now = new Date();

    if (loading) {
        return (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '40vh', gap: '1rem' }}>
                <div style={{ width: '40px', height: '40px', border: '3px solid var(--border)', borderTop: '3px solid var(--accent)', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
                <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Đang tải dữ liệu...</div>
            </div>
        );
    }

    return (
        <div className="animate-fade-in">
            {/* Header */}
            <div style={{ marginBottom: '2rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <div>
                        <h1 style={{ fontSize: '1.75rem', fontWeight: 800, marginBottom: '0.25rem', letterSpacing: '-0.02em' }}>
                            Tổng quan hệ thống
                        </h1>
                        <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                            BKBank Core Ledger Admin — Cập nhật lúc {now.toLocaleTimeString('vi-VN')}
                        </p>
                    </div>
                    <div style={{ display: 'flex', gap: '0.75rem' }}>
                        <Link href="/dashboard/clients/new" className="btn-secondary" style={{ textDecoration: 'none', fontSize: '0.875rem' }}>
                            + Khách hàng
                        </Link>
                        <Link href="/dashboard/loans/new" className="btn-primary" style={{ textDecoration: 'none', fontSize: '0.875rem' }}>
                            + Tài khoản tín dụng
                        </Link>
                    </div>
                </div>
            </div>

            {/* KPI Row */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem', marginBottom: '1.5rem' }}>
                <KpiCard
                    label="Khách hàng"
                    value={clients.length}
                    sub={`${clients.filter(c => c.status === 'ACTIVE').length} đang hoạt động`}
                    icon={faUsers}
                    accent="#6366f1"
                    trend={{ value: 12, label: 'tháng này' }}
                />
                <KpiCard
                    label="Tài khoản tín dụng"
                    value={loans.length}
                    sub={`${activeLoans} ACTIVE / ${lockedLoans} LOCKED`}
                    icon={faCreditCard}
                    accent="#8b5cf6"
                />
                <KpiCard
                    label="Tài khoản ghi nợ"
                    value={savings.length}
                    sub={`${activeSavings} đang hoạt động`}
                    icon={faPiggyBank}
                    accent="#f59e0b"
                />
                <KpiCard
                    label="Tổng giao dịch"
                    value={txns.length}
                    sub={`${successCount} thành công / ${failedCount} thất bại`}
                    icon={faListUl}
                    accent="#10b981"
                    trend={{ value: 8, label: 'so hôm qua' }}
                />
            </div>

            {/* Credit Utilization Banner */}
            <div className="card" style={{ marginBottom: '1.5rem', background: 'linear-gradient(135deg, rgba(99,102,241,0.15) 0%, rgba(139,92,246,0.1) 100%)' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                        <div style={{ width: '48px', height: '48px', borderRadius: '14px', background: 'rgba(99,102,241,0.25)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '1.3rem', color: '#818cf8' }}>
                            <FontAwesomeIcon icon={faBuildingColumns} />
                        </div>
                        <div>
                            <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginBottom: '0.2rem' }}>Tổng hạn mức tín dụng</div>
                            <div style={{ fontSize: '1.5rem', fontWeight: 800 }}>${totalCreditLimit.toLocaleString('en-US')}</div>
                        </div>
                    </div>
                    <div style={{ display: 'flex', gap: '3rem' }}>
                        <div style={{ textAlign: 'center' }}>
                            <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#f59e0b' }}>${totalOutstanding.toLocaleString('en-US')}</div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Dư nợ hiện tại</div>
                        </div>
                        <div style={{ textAlign: 'center' }}>
                            <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#10b981' }}>${(totalCreditLimit - totalOutstanding).toLocaleString('en-US')}</div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Còn khả dụng</div>
                        </div>
                        <div style={{ textAlign: 'center' }}>
                            <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#6366f1' }}>
                                {totalCreditLimit > 0 ? ((totalOutstanding / totalCreditLimit) * 100).toFixed(2) : 0}%
                            </div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Tỷ lệ sử dụng</div>
                        </div>
                    </div>
                </div>
                {/* Utilization bar */}
                <div style={{ marginTop: '1.25rem' }}>
                    <div style={{ height: '6px', background: 'rgba(255,255,255,0.1)', borderRadius: '3px', overflow: 'hidden' }}>
                        <div style={{
                            height: '100%',
                            width: `${totalCreditLimit > 0 ? Math.min((totalOutstanding / totalCreditLimit) * 100, 100) : 0}%`,
                            background: 'linear-gradient(90deg, #6366f1, #8b5cf6)',
                            borderRadius: '3px', transition: 'width 1s ease'
                        }} />
                    </div>
                </div>
            </div>

            {/* Charts Row */}
            <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1.5rem', marginBottom: '1.5rem' }}>
                {/* Area Chart - Transaction Volume */}
                <div className="card">
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.5rem' }}>
                        <div>
                            <h2 style={{ fontSize: '1rem', fontWeight: 700 }}>
                                <FontAwesomeIcon icon={faChartBar} style={{ marginRight: '0.5rem', color: 'var(--accent)' }} />
                                Lịch sử giao dịch
                            </h2>
                            <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>14 ngày gần nhất</p>
                        </div>
                        <Link href="/dashboard/transactions" style={{ fontSize: '0.8125rem', color: 'var(--accent)', textDecoration: 'none' }}>
                            Xem tất cả →
                        </Link>
                    </div>
                    {txnByDay.length === 0 ? (
                        <div style={{ height: '200px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-secondary)' }}>
                            Chưa có dữ liệu giao dịch
                        </div>
                    ) : (
                        <ResponsiveContainer width="100%" height={220}>
                            <AreaChart data={txnByDay} margin={{ top: 5, right: 10, left: -20, bottom: 0 }}>
                                <defs>
                                    <linearGradient id="successGrad" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                                        <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                                    </linearGradient>
                                    <linearGradient id="failedGrad" x1="0" y1="0" x2="0" y2="1">
                                        <stop offset="5%" stopColor="#ef4444" stopOpacity={0.3} />
                                        <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
                                    </linearGradient>
                                </defs>
                                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                                <XAxis dataKey="date" tick={{ fontSize: 11, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
                                <YAxis tick={{ fontSize: 11, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
                                <Tooltip content={<CustomTooltip />} />
                                <Area type="monotone" dataKey="success" name="Thành công" stroke="#10b981" strokeWidth={2} fill="url(#successGrad)" />
                                <Area type="monotone" dataKey="failed" name="Thất bại" stroke="#ef4444" strokeWidth={2} fill="url(#failedGrad)" />
                            </AreaChart>
                        </ResponsiveContainer>
                    )}
                </div>

                {/* Right column — two charts stacked */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                    {/* Pie Chart - Loan Status */}
                    <div className="card" style={{ flex: 1 }}>
                        <h2 style={{ fontSize: '0.9375rem', fontWeight: 700, marginBottom: '1rem' }}>Trạng thái tín dụng</h2>
                        {loanStatusData.length === 0 ? (
                            <div style={{ height: '120px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Chưa có dữ liệu</div>
                        ) : (
                            <ResponsiveContainer width="100%" height={160}>
                                <PieChart>
                                    <Pie data={loanStatusData} cx="50%" cy="50%" innerRadius={45} outerRadius={68} paddingAngle={3} dataKey="value">
                                        {loanStatusData.map((entry, i) => (
                                            <Cell key={i} fill={entry.color} />
                                        ))}
                                    </Pie>
                                    <Tooltip content={<CustomTooltip />} />
                                    <Legend iconType="circle" iconSize={8} wrapperStyle={{ fontSize: '0.75rem' }} />
                                </PieChart>
                            </ResponsiveContainer>
                        )}
                    </div>

                    {/* Bar Chart - Tx Types */}
                    <div className="card" style={{ flex: 1 }}>
                        <h2 style={{ fontSize: '0.9375rem', fontWeight: 700, marginBottom: '1rem' }}>Phân loại giao dịch</h2>
                        {txnTypeData.length === 0 ? (
                            <div style={{ height: '120px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>Chưa có dữ liệu</div>
                        ) : (
                            <ResponsiveContainer width="100%" height={140}>
                                <BarChart data={txnTypeData} margin={{ left: -30, right: 5, top: 5, bottom: 0 }}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                                    <XAxis dataKey="name" tick={{ fontSize: 10, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
                                    <YAxis tick={{ fontSize: 10, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
                                    <Tooltip content={<CustomTooltip />} />
                                    <Bar dataKey="value" name="Số lượng" radius={[4, 4, 0, 0]}>
                                        {txnTypeData.map((_, i) => (
                                            <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                                        ))}
                                    </Bar>
                                </BarChart>
                            </ResponsiveContainer>
                        )}
                    </div>
                </div>
            </div>

            {/* Recent Transactions */}
            <div className="card">
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1.25rem' }}>
                    <h2 style={{ fontSize: '1rem', fontWeight: 700 }}>
                        <FontAwesomeIcon icon={faListUl} style={{ marginRight: '0.5rem', color: 'var(--accent)' }} />
                        Giao dịch gần đây
                    </h2>
                    <Link href="/dashboard/transactions" style={{ fontSize: '0.8125rem', color: 'var(--accent)', textDecoration: 'none' }}>
                        Xem tất cả →
                    </Link>
                </div>
                {recentTxns.length === 0 ? (
                    <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Chưa có giao dịch nào</div>
                ) : (
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Ngày giờ</th>
                                    <th>Tài khoản</th>
                                    <th>Merchant</th>
                                    <th>Loại</th>
                                    <th>Số tiền</th>
                                    <th>Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody>
                                {recentTxns.map((txn: any) => (
                                    <tr key={txn.id}>
                                        <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem', fontFamily: 'monospace' }}>#{txn.id}</td>
                                        <td style={{ fontSize: '0.8125rem' }}>
                                            {txn.transactionDate ? new Date(txn.transactionDate).toLocaleString('vi-VN') : '—'}
                                        </td>
                                        <td style={{ fontFamily: 'monospace', fontSize: '0.8125rem' }}>{txn.accountNumber || '-'}</td>
                                        <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>{txn.merchantName || '—'}</td>
                                        <td>
                                            <span className={`badge ${txn.transactionType === 'CREDIT' || txn.transactionType === 'DEPOSIT' ? 'badge-active' : 'badge-pending'}`}>
                                                {txn.transactionType === 'CHARGE' ? (txn.merchantName ? 'CREDIT' : 'CHARGE/FEE') : txn.transactionType}
                                            </span>
                                        </td>
                                        <td style={{ fontWeight: 700 }}>
                                            <span style={{ color: txn.transactionType === 'CREDIT' || txn.transactionType === 'DEPOSIT' ? 'var(--success)' : 'var(--warning)' }}>
                                                {txn.transactionType === 'CREDIT' || txn.transactionType === 'DEPOSIT' ? '+' : '-'}
                                                {Number(txn.amount || 0).toLocaleString('en-US')} {txn.currency || 'USD'}
                                            </span>
                                        </td>
                                        <td>
                                            {txn.status === 'FAILED'
                                                ? <span className="badge" style={{ background: 'rgba(239,68,68,0.15)', color: 'var(--danger)' }}>THẤT BẠI</span>
                                                : <span className="badge badge-active">THÀNH CÔNG</span>
                                            }
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            <style jsx global>{`
                @keyframes spin { to { transform: rotate(360deg); } }
            `}</style>
        </div>
    );
}
