'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowLeft, faFileInvoiceDollar } from '@fortawesome/free-solid-svg-icons';
import { getClientSavingsAccounts, getLoan, getLoanMonthlyStatementDetail, payLoanMonthlyStatement } from '@/lib/api';
import { getDisplayCounterpartyId, getDisplayCounterpartyName } from '@/lib/transactionDisplay';

const STATEMENT_STATUS_BADGE: Record<string, string> = {
    OPEN: 'badge-pending',
    PARTIALLY_PAID: 'badge-pending',
    PAID: 'badge-active',
    OVERDUE: 'badge-locked',
};

const STATEMENT_STATUS_LABEL: Record<string, string> = {
    OPEN: 'Chua thanh toan',
    PARTIALLY_PAID: 'Da thanh toan mot phan',
    PAID: 'Da thanh toan',
    OVERDUE: 'Qua han',
};

function formatMoney(value: number | null | undefined, currency = 'USD') {
    return `${Number(value || 0).toLocaleString('en-US')} ${currency}`;
}

function formatRate(value: number | null | undefined) {
    return `${Number(value || 0).toLocaleString('en-US', { maximumFractionDigits: 2 })}%/thang`;
}

function formatDateTime(value: string | null | undefined) {
    return value ? String(value).replace('T', ' ') : '—';
}

function parseLocalDate(value: string | null | undefined) {
    if (!value) return null;
    const parsed = new Date(`${value}T00:00:00`);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function dayDiff(from: Date, to: Date) {
    const msPerDay = 24 * 60 * 60 * 1000;
    return Math.floor((to.getTime() - from.getTime()) / msPerDay);
}

function getDuePolicyText(statement: any) {
    const dueDays = Number(statement?.paymentDueDays ?? 0);
    if (dueDays > 0) {
        return `${dueDays} ngay sau ngay sao ke`;
    }
    return '—';
}

function getOverdueText(statement: any) {
    const dueDate = parseLocalDate(statement?.dueDate);
    if (!dueDate) {
        return '—';
    }

    const today = parseLocalDate(new Date().toISOString().slice(0, 10));
    if (!today) {
        return '—';
    }

    if (statement?.statementStatus === 'OVERDUE') {
        return `Da qua han ${Math.max(dayDiff(dueDate, today), 1)} ngay`;
    }

    if (today.getTime() === dueDate.getTime()) {
        return 'Den han hom nay';
    }

    if (today.getTime() < dueDate.getTime()) {
        return `Con ${dayDiff(today, dueDate)} ngay den han`;
    }

    return 'Qua han';
}

function getStatementStatusText(statement: any) {
    const status = statement?.statementStatus;
    const remainingMinimumDue = Number(statement?.remainingMinimumDue ?? statement?.minimumDue ?? 0);
    const remainingBalance = Number(statement?.remainingBalance ?? statement?.newBalance ?? 0);

    if (status === 'OVERDUE' && remainingMinimumDue <= 0 && remainingBalance > 0) {
        return 'Qua han - con phi/lai';
    }

    return STATEMENT_STATUS_LABEL[status] || status || 'Chua thanh toan';
}

function getCurrentDueSummary(statement: any, currency: string) {
    const remainingMinimumDue = Number(statement?.remainingMinimumDue ?? statement?.minimumDue ?? 0);
    const remainingBalance = Number(statement?.remainingBalance ?? statement?.newBalance ?? 0);
    const interestCharged = Number(statement?.interestCharged ?? 0);
    const lateFeeCharged = Number(statement?.lateFeeCharged ?? 0);

    if (remainingBalance <= 0) {
        return 'Ky sao ke nay da duoc thanh toan du.';
    }
    if (remainingMinimumDue <= 0 && (interestCharged > 0 || lateFeeCharged > 0)) {
        return `Sao ke goc da duoc thanh toan, hien con  phi/lai phat sinh sau han cua ky sao ke nay.`;
    }
    return 'Tra toi thieu, toan bo con no cua ky sao ke nay hoac nhap so tien tuy chinh.';
}

function detailBox(label: string, value: string) {
    return (
        <div style={{
            padding: '0.9rem 1rem',
            border: '1px solid var(--border)',
            borderRadius: '10px',
            background: 'rgba(255,255,255,0.02)'
        }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.45rem' }}>{label}</div>
            <div style={{ fontWeight: 600, lineHeight: 1.45 }}>{value}</div>
        </div>
    );
}

function renderTransactionTable(items: any[], currency: string, emptyText: string) {
    return (
        <div className="table-container">
            <table>
                <thead>
                    <tr>
                        <th>Thoi gian</th>
                        <th>Payment ID</th>
                        <th>Loai</th>
                        <th>Merchant</th>
                        <th>So tien</th>
                        <th>Du no sau GD</th>
                        <th>Trang thai</th>
                    </tr>
                </thead>
                <tbody>
                    {items.length === 0 ? (
                        <tr>
                            <td colSpan={7} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
                                {emptyText}
                            </td>
                        </tr>
                    ) : items.map((item: any) => (
                        <tr key={`${item.paymentId}-${item.transactionDate}-${item.transactionType}`}>
                            <td style={{ whiteSpace: 'nowrap' }}>{item.transactionDate?.replace('T', ' ') || '—'}</td>
                            <td style={{ fontFamily: 'monospace', fontSize: '0.8125rem' }}>{item.paymentId || '—'}</td>
                            <td>{item.transactionType || '—'}</td>
                            <td>
                                <div style={{ fontWeight: 500 }}>{getDisplayCounterpartyName(item)}</div>
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem' }}>{getDisplayCounterpartyId(item)}</div>
                            </td>
                            <td>{formatMoney(item.amount, currency)}</td>
                            <td>{formatMoney(item.balanceAfter, currency)}</td>
                            <td>
                                <span className={`badge ${item.status === 'SUCCESS' ? 'badge-active' : item.status === 'FAILED' ? 'badge-locked' : 'badge-pending'}`}>
                                    {item.status || 'UNKNOWN'}
                                </span>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

export default function LoanStatementDetailPage() {
    const params = useParams<{ loanId: string; billingDate: string }>();
    const loanId = decodeURIComponent(params.loanId);
    const billingDate = decodeURIComponent(params.billingDate);

    const [statement, setStatement] = useState<any>(null);
    const [loan, setLoan] = useState<any>(null);
    const [savingsAccounts, setSavingsAccounts] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [paymentLoading, setPaymentLoading] = useState(false);
    const [paymentMessage, setPaymentMessage] = useState('');
    const [paymentError, setPaymentError] = useState('');
    const [paymentOption, setPaymentOption] = useState('MINIMUM_DUE');
    const [paymentSource, setPaymentSource] = useState('INTERNAL_SAVINGS');
    const [sourceAccountNumber, setSourceAccountNumber] = useState('');
    const [customAmount, setCustomAmount] = useState('');
    const [paymentNote, setPaymentNote] = useState('Thanh toan sao ke');

    useEffect(() => {
        loadDetail();
    }, [loanId, billingDate]);

    async function loadDetail() {
        setLoading(true);
        setError('');
        try {
            const [data, loanData] = await Promise.all([
                getLoanMonthlyStatementDetail(loanId, billingDate),
                getLoan(loanId),
            ]);
            setStatement(data);
            setLoan(loanData);
            const clientId = loanData?.clientId;
            if (clientId) {
                const savingsData = await getClientSavingsAccounts(clientId);
                const activeAccounts = (savingsData?.accounts || []).filter((account: any) => account.status === 'ACTIVE');
                setSavingsAccounts(activeAccounts);
                setSourceAccountNumber((current) => current || activeAccounts[0]?.accountNumber || '');
            } else {
                setSavingsAccounts([]);
                setSourceAccountNumber('');
            }
        } catch (e: any) {
            setError(e.message || 'Khong the tai chi tiet sao ke');
            setStatement(null);
            setLoan(null);
            setSavingsAccounts([]);
        } finally {
            setLoading(false);
        }
    }

    async function handlePayStatement() {
        if (!statement) return;

        setPaymentLoading(true);
        setPaymentError('');
        setPaymentMessage('');

        try {
            const amount =
                paymentOption === 'CUSTOM'
                    ? Number(customAmount || 0)
                    : null;

            const response = await payLoanMonthlyStatement(loanId, billingDate, {
                paymentOption,
                amount,
                paymentSource,
                sourceAccountNumber: paymentSource === 'INTERNAL_SAVINGS' ? sourceAccountNumber : null,
                note: paymentNote,
            });

            setPaymentMessage(
                `Da thanh toan ${formatMoney(response.paymentAmount, response.currency)}. Trang thai moi: ${STATEMENT_STATUS_LABEL[response.statementStatusAfter] || response.statementStatusAfter}`,
            );
            if (paymentOption === 'CUSTOM') {
                setCustomAmount('');
            }
            await loadDetail();
        } catch (e: any) {
            setPaymentError(e.message || 'Khong the thanh toan sao ke');
        } finally {
            setPaymentLoading(false);
        }
    }

    const currency = statement?.currency || 'USD';
    const items = statement?.items || [];
    const postStatementItems = statement?.postStatementItems || [];
    const remainingMinimumDue = Number(statement?.remainingMinimumDue ?? statement?.minimumDue ?? 0);
    const remainingBalance = Number(statement?.remainingBalance ?? statement?.newBalance ?? 0);
    const isPaid = remainingBalance <= 0;
    const canUseInternalSavings = savingsAccounts.length > 0;
    const accountOutstanding = Number(loan?.principalOutstanding || 0);
    const hasOnlyOverdueChargesLeft = !isPaid && remainingMinimumDue <= 0 && remainingBalance > 0;

    useEffect(() => {
        if (hasOnlyOverdueChargesLeft && paymentOption === 'MINIMUM_DUE') {
            setPaymentOption('STATEMENT_BALANCE');
        }
    }, [hasOnlyOverdueChargesLeft, paymentOption]);

    return (
        <div className="animate-fade-in">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '1rem', marginBottom: '2rem' }}>
                <div>
                    <div style={{ marginBottom: '0.75rem' }}>
                        <Link href={`/dashboard/loans/${loanId}/statements`} className="btn-secondary" style={{ textDecoration: 'none' }}>
                            <FontAwesomeIcon icon={faArrowLeft} /> Quay lai lich su
                        </Link>
                    </div>
                    <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                        <FontAwesomeIcon icon={faFileInvoiceDollar} style={{ marginRight: '0.5rem' }} />
                        Chi tiet sao ke
                    </h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                        Tai khoan {loanId} • Billing date {billingDate}
                    </p>
                </div>
            </div>

            {error && <div className="error-banner">{error}</div>}
            {paymentError && <div className="error-banner">{paymentError}</div>}
            {paymentMessage && <div className="success-banner">{paymentMessage}</div>}

            {loading ? (
                <div className="card">
                    <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>Dang tai chi tiet sao ke...</div>
                </div>
            ) : !statement ? (
                <div className="card">
                    <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>Khong co du lieu sao ke.</div>
                </div>
            ) : (
                <>
                    <div className="card" style={{ marginBottom: '1rem' }}>
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', marginBottom: '1rem' }}>
                            <div>
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Du no khi chot sao ke</div>
                                <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{formatMoney(statement.newBalance, currency)}</div>
                            </div>
                            <div>
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Toi thieu con lai</div>
                                <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{formatMoney(remainingMinimumDue, currency)}</div>
                            </div>
                            <div>
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Con no cua ky sao ke nay</div>
                                <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{formatMoney(remainingBalance, currency)}</div>
                            </div>
                            <div>
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Du no toan tai khoan hien tai</div>
                                <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{formatMoney(accountOutstanding, currency)}</div>
                            </div>
                            <div>
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Trang thai</div>
                                <div>
                                    <span className={`badge ${STATEMENT_STATUS_BADGE[statement.statementStatus] || 'badge-pending'}`}>
                                        {getStatementStatusText(statement)}
                                    </span>
                                </div>
                            </div>
                        </div>

                        <div style={{ paddingTop: '1rem', borderTop: '1px solid var(--border)', marginTop: '1rem' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem', alignItems: 'flex-start', flexWrap: 'wrap', marginBottom: '1rem' }}>
                                <div>
                                    <div style={{ fontSize: '1rem', fontWeight: 600 }}>Thanh toan sao ke</div>
                                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                                        {getCurrentDueSummary(statement, currency)}
                                    </div>
                                </div>
                                {loan?.clientId && (
                                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                                        Khach hang: {loan.clientName || loan.clientId}
                                    </div>
                                )}
                            </div>

                            {isPaid ? (
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
                                    Ky sao ke nay da duoc thanh toan du.
                                </div>
                            ) : (
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.9rem' }}>
                                    <div>
                                        <label className="label">Tuy chon thanh toan</label>
                                        <select className="input" value={paymentOption} onChange={(e) => setPaymentOption(e.target.value)}>
                                            <option value="MINIMUM_DUE" disabled={remainingMinimumDue <= 0}>Thanh toan toi thieu</option>
                                            <option value="STATEMENT_BALANCE">{hasOnlyOverdueChargesLeft ? 'Thanh toan Con no cua ky sao ke nay' : 'Thanh toan toan bo sao ke'}</option>
                                            <option value="CUSTOM">Thanh toan so khac</option>
                                        </select>
                                    </div>

                                    <div>
                                        <label className="label">Nguon thanh toan</label>
                                        <select className="input" value={paymentSource} onChange={(e) => setPaymentSource(e.target.value)}>
                                            <option value="INTERNAL_SAVINGS">Tai khoan debit noi bo</option>
                                            <option value="CASH_COUNTER">Thu tien tai quay</option>
                                        </select>
                                    </div>

                                    {paymentOption === 'CUSTOM' && (
                                        <div>
                                            <label className="label">So tien</label>
                                            <input
                                                className="input"
                                                type="number"
                                                min="0"
                                                step="0.01"
                                                value={customAmount}
                                                onChange={(e) => setCustomAmount(e.target.value)}
                                                placeholder="Nhap so tien can tra"
                                            />
                                        </div>
                                    )}

                                    {paymentSource === 'INTERNAL_SAVINGS' && (
                                        <div>
                                            <label className="label">Tai khoan debit</label>
                                            <select
                                                className="input"
                                                value={sourceAccountNumber}
                                                onChange={(e) => setSourceAccountNumber(e.target.value)}
                                                disabled={!canUseInternalSavings}
                                            >
                                                {!canUseInternalSavings ? (
                                                    <option value="">Khong co tai khoan ACTIVE</option>
                                                ) : (
                                                    savingsAccounts.map((account: any) => (
                                                        <option key={account.accountNumber} value={account.accountNumber}>
                                                            {account.accountNumber} • {formatMoney(account.balance, currency)}
                                                        </option>
                                                    ))
                                                )}
                                            </select>
                                        </div>
                                    )}

                                    <div style={{ gridColumn: '1 / -1' }}>
                                        <label className="label">Ghi chu</label>
                                        <input
                                            className="input"
                                            value={paymentNote}
                                            onChange={(e) => setPaymentNote(e.target.value)}
                                            placeholder="Ghi chu thanh toan"
                                        />
                                    </div>

                                    <div style={{ gridColumn: '1 / -1', display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'center' }}>
                                        <button
                                            className="btn-primary"
                                            onClick={handlePayStatement}
                                            disabled={
                                                paymentLoading
                                                || (paymentSource === 'INTERNAL_SAVINGS' && !sourceAccountNumber)
                                                || (paymentSource === 'INTERNAL_SAVINGS' && !canUseInternalSavings)
                                                || (paymentOption === 'CUSTOM' && !(Number(customAmount || 0) > 0))
                                            }
                                        >
                                            {paymentLoading ? 'Dang thanh toan...' : 'Xac nhan thanh toan'}
                                        </button>
                                        <div style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                                            {paymentOption === 'MINIMUM_DUE'
                                                ? `Se tra ${formatMoney(remainingMinimumDue, currency)}`
                                                : paymentOption === 'STATEMENT_BALANCE'
                                                    ? `Se tra ${formatMoney(remainingBalance, currency)}`
                                                    : 'Nhap so tien tuy chinh'}
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>

                        <div style={{ paddingTop: '1rem', borderTop: '1px solid var(--border)' }}>
                            <div style={{ marginBottom: '1rem' }}>
                                <div style={{ fontSize: '0.95rem', fontWeight: 600, marginBottom: '0.75rem' }}>Thong tin ky sao ke</div>
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.75rem' }}>
                                    {detailBox('Ky sao ke', statement.statementPeriod)}
                                    {detailBox('Ngay sao ke', statement.billingDate)}
                                    {detailBox('Ngay den han', statement.dueDate)}
                                    {detailBox('Lan thanh toan gan nhat', formatDateTime(statement.lastPaymentDate))}
                                </div>
                            </div>

                            <div style={{ marginBottom: '1rem' }}>
                                <div style={{ fontSize: '0.95rem', fontWeight: 600, marginBottom: '0.75rem' }}>Bien dong tai chinh</div>
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.75rem' }}>
                                    {detailBox('Du no dau ky', formatMoney(statement.previousBalance, currency))}
                                    {detailBox('Tong phat sinh trong ky', formatMoney(statement.totalCharges, currency))}
                                    {detailBox('Tong thanh toan', formatMoney(statement.totalPayments, currency))}
                                    {detailBox('Da thanh toan sau sao ke', formatMoney(statement.paidAmountAfterStatement, currency))}
                                    {detailBox('Lai da ap', formatMoney(statement.interestCharged, currency))}
                                    {detailBox('Phi tre han da ap', formatMoney(statement.lateFeeCharged, currency))}
                                    {detailBox('Toi thieu con lai', formatMoney(remainingMinimumDue, currency))}
                                    {detailBox('Con no cua ky sao ke nay', formatMoney(remainingBalance, currency))}
                                </div>
                            </div>

                            <div>
                                <div style={{ fontSize: '0.95rem', fontWeight: 600, marginBottom: '0.75rem' }}>Cau hinh thanh toan</div>
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.75rem' }}>
                                    {detailBox('Billing day', String(statement.billingDayOfMonth ?? '—'))}
                                    {detailBox('So ngay den han', String(statement.paymentDueDays ?? '—'))}
                                    {detailBox('Chinh sach den han', getDuePolicyText(statement))}
                                    {detailBox('Tinh trang qua han', getOverdueText(statement))}
                                    {detailBox('Lai suat sao ke', formatRate(statement.interestRateMonthly))}
                                    {detailBox('Thoi diem ap lai', formatDateTime(statement.interestAppliedAt))}
                                    {detailBox('Phi tre han co dinh', formatMoney(statement.lateFeeFixed, currency))}
                                    {detailBox('Thoi diem ap phi tre han', formatDateTime(statement.lateFeeAppliedAt))}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="card" style={{ marginBottom: '1rem' }}>
                        <div style={{ marginBottom: '1rem' }}>
                            <div style={{ fontSize: '1rem', fontWeight: 600 }}>Giao dich trong ky</div>
                            <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                                Bao gom toan bo giao dich nam trong khoang sao ke
                            </div>
                        </div>
                        {renderTransactionTable(items, currency, 'Khong co giao dich nao trong ky nay')}
                    </div>

                    <div className="card">
                        <div style={{ marginBottom: '1rem' }}>
                            <div style={{ fontSize: '1rem', fontWeight: 600 }}>Phat sinh sau sao ke</div>
                            <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                                Bao gom thanh toan sau sao ke, lai, phi tre han va cac dieu chinh phat sinh sau ngay chot sao ke
                            </div>
                        </div>
                        {renderTransactionTable(postStatementItems, currency, 'Khong co phat sinh nao sau sao ke')}
                    </div>
                </>
            )}
        </div>
    );
}


