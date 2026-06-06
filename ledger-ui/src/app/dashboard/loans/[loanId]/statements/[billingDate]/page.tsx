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
    OPEN: 'Chưa thanh toán',
    PARTIALLY_PAID: 'Đã thanh toán một phần',
    PAID: 'Đã thanh toán',
    OVERDUE: 'Quá hạn',
};

function formatMoney(value: number | null | undefined, currency = 'USD') {
    return `${Number(value || 0).toLocaleString('en-US')} ${currency}`;
}

function formatRate(value: number | null | undefined) {
    return `${Number(value || 0).toLocaleString('en-US', { maximumFractionDigits: 2 })}%/năm`;
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
        return `${dueDays} ngày sau ngày sao kê`;
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
        return `Đã quá hạn ${Math.max(dayDiff(dueDate, today), 1)} ngày`;
    }

    if (today.getTime() === dueDate.getTime()) {
        return 'Đến hạn hôm nay';
    }

    if (today.getTime() < dueDate.getTime()) {
        return `Còn ${dayDiff(today, dueDate)} ngày đến hạn`;
    }

    return 'Quá hạn';
}

function getStatementStatusText(statement: any) {
    const status = statement?.statementStatus;
    const remainingMinimumDue = Number(statement?.remainingMinimumDue ?? statement?.totalMinimumDueNow ?? statement?.minimumDue ?? 0);
    const remainingCurrentMinimumDue = Number(statement?.remainingCurrentMinimumDue ?? statement?.currentMinimumDue ?? statement?.minimumDue ?? 0);
    const remainingPastDueMinimum = Number(statement?.remainingPastDueMinimum ?? statement?.pastDueMinimum ?? 0);
    const remainingBalance = Number(statement?.remainingBalance ?? statement?.newBalance ?? 0);

    if (status === 'OVERDUE' && remainingMinimumDue <= 0 && remainingBalance > 0) {
        return 'Quá hạn - còn phí/lãi';
    }

    return STATEMENT_STATUS_LABEL[status] || status || 'Chưa thanh toán';
}

function getCurrentDueSummary(statement: any, currency: string) {
    const remainingMinimumDue = Number(statement?.remainingMinimumDue ?? statement?.totalMinimumDueNow ?? statement?.minimumDue ?? 0);
    const remainingCurrentMinimumDue = Number(statement?.remainingCurrentMinimumDue ?? statement?.currentMinimumDue ?? statement?.minimumDue ?? 0);
    const remainingPastDueMinimum = Number(statement?.remainingPastDueMinimum ?? statement?.pastDueMinimum ?? 0);
    const remainingBalance = Number(statement?.remainingBalance ?? statement?.newBalance ?? 0);
    const interestCharged = Number(statement?.interestCharged ?? 0);
    const lateFeeCharged = Number(statement?.lateFeeCharged ?? 0);

    if (remainingBalance <= 0) {
        return 'Kỳ sao kê này đã được thanh toán đủ.';
    }
    if (remainingMinimumDue <= 0 && (interestCharged > 0 || lateFeeCharged > 0)) {
        return 'Sao kê gốc đã được thanh toán, hiện còn phí/lãi phát sinh sau hạn của kỳ sao kê này.';
    }
    if (remainingPastDueMinimum > 0) {
        return `Cần xử lý ${formatMoney(remainingPastDueMinimum, currency)} tối thiểu quá hạn từ kỳ trước và ${formatMoney(remainingCurrentMinimumDue, currency)} tối thiểu của kỳ hiện tại.`;
    }
    return 'Trả tối thiểu hiện tại, toàn bộ còn nợ của kỳ sao kê này hoặc nhập số tiền tùy chỉnh.';
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
            setError(e.message || 'Không thể tải chi tiết sao kê');
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
    const remainingMinimumDue = Number(statement?.remainingMinimumDue ?? statement?.totalMinimumDueNow ?? statement?.minimumDue ?? 0);
    const remainingCurrentMinimumDue = Number(statement?.remainingCurrentMinimumDue ?? statement?.currentMinimumDue ?? statement?.minimumDue ?? 0);
    const remainingPastDueMinimum = Number(statement?.remainingPastDueMinimum ?? statement?.pastDueMinimum ?? 0);
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
                            <FontAwesomeIcon icon={faArrowLeft} /> Quay lại lịch sử
                        </Link>
                    </div>
                    <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                        <FontAwesomeIcon icon={faFileInvoiceDollar} style={{ marginRight: '0.5rem' }} />
                        Chi tiết sao kê
                    </h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                        Tài khoản {loanId} • Billing date {billingDate}
                    </p>
                </div>
            </div>

            {error && <div className="error-banner">{error}</div>}
            {paymentError && <div className="error-banner">{paymentError}</div>}
            {paymentMessage && <div className="success-banner">{paymentMessage}</div>}

            {loading ? (
                <div className="card">
                    <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>Đang tải chi tiết sao kê...</div>
                </div>
            ) : !statement ? (
                <div className="card">
                    <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>Không có dữ liệu sao kê.</div>
                </div>
            ) : (
                <>
                    <div className="card" style={{ marginBottom: '1rem' }}>
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', marginBottom: '1rem' }}>
                            <div>
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Dư nợ khi chốt sao kê</div>
                                <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{formatMoney(statement.newBalance, currency)}</div>
                            </div>
                            <div>
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Tổng tối thiểu cần trả hiện tại</div>
                                <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{formatMoney(remainingMinimumDue, currency)}</div>
                            </div>
                            <div>
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Còn nợ của kỳ sao kê này</div>
                                <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{formatMoney(remainingBalance, currency)}</div>
                            </div>
                            <div>
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Dư nợ toàn tài khoản hiện tại</div>
                                <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{formatMoney(accountOutstanding, currency)}</div>
                            </div>
                            <div>
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Trạng thái</div>
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
                                    <div style={{ fontSize: '1rem', fontWeight: 600 }}>Thanh toán sao kê</div>
                                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                                        {getCurrentDueSummary(statement, currency)}
                                    </div>
                                </div>
                                {loan?.clientId && (
                                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                                        Khách hàng: {loan.clientName || loan.clientId}
                                    </div>
                                )}
                            </div>

                            {isPaid ? (
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
                                    Kỳ sao kê này đã được thanh toán đủ.
                                </div>
                            ) : (
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.9rem' }}>
                                    <div>
                                        <label className="label">Tùy chọn thanh toán</label>
                                        <select className="input" value={paymentOption} onChange={(e) => setPaymentOption(e.target.value)}>
                                            <option value="MINIMUM_DUE" disabled={remainingMinimumDue <= 0}>Thanh toán tối thiểu</option>
                                            <option value="STATEMENT_BALANCE">{hasOnlyOverdueChargesLeft ? 'Thanh toán Còn nợ của kỳ sao kê này' : 'Thanh toán toàn bộ sao kê'}</option>
                                            <option value="CUSTOM">Thanh toán số khác</option>
                                        </select>
                                    </div>

                                    <div>
                                        <label className="label">Nguồn thanh toán</label>
                                        <select className="input" value={paymentSource} onChange={(e) => setPaymentSource(e.target.value)}>
                                            <option value="INTERNAL_SAVINGS">Tài khoản nội bộ</option>
                                            <option value="CASH_COUNTER">Thu tiền tại quầy</option>
                                        </select>
                                    </div>

                                    {paymentOption === 'CUSTOM' && (
                                        <div>
                                            <label className="label">Số tiền</label>
                                            <input
                                                className="input"
                                                type="number"
                                                min="0"
                                                step="0.01"
                                                value={customAmount}
                                                onChange={(e) => setCustomAmount(e.target.value)}
                                                placeholder="Nhập số tiền cần trả"
                                            />
                                        </div>
                                    )}

                                    {paymentSource === 'INTERNAL_SAVINGS' && (
                                        <div>
                                            <label className="label">Tài khoản</label>
                                            <select
                                                className="input"
                                                value={sourceAccountNumber}
                                                onChange={(e) => setSourceAccountNumber(e.target.value)}
                                                disabled={!canUseInternalSavings}
                                            >
                                                {!canUseInternalSavings ? (
                                                    <option value="">Không có tài khoản nào</option>
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
                                        <label className="label">Ghi chú</label>
                                        <input
                                            className="input"
                                            value={paymentNote}
                                            onChange={(e) => setPaymentNote(e.target.value)}
                                            placeholder="Ghi chú thanh toán"
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
                                            {paymentLoading ? 'Đang thanh toán...' : 'Xác nhận thanh toán'}
                                        </button>
                                        <div style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>
                                            {paymentOption === 'MINIMUM_DUE'
                                                ? `Sẽ trả ${formatMoney(remainingMinimumDue, currency)}`
                                                : paymentOption === 'STATEMENT_BALANCE'
                                                    ? `Sẽ trả ${formatMoney(remainingBalance, currency)}`
                                                    : 'Nhập số tiền tùy chỉnh'}
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>

                        <div style={{ paddingTop: '1rem', borderTop: '1px solid var(--border)' }}>
                            <div style={{ marginBottom: '1rem' }}>
                                <div style={{ fontSize: '0.95rem', fontWeight: 600, marginBottom: '0.75rem' }}>Thông tin kỳ sao kê</div>
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.75rem' }}>
                                    {detailBox('Kỳ sao kê', statement.statementPeriod)}
                                    {detailBox('Ngày sao kê', statement.billingDate)}
                                    {detailBox('Ngày đến hạn', statement.dueDate)}
                                    {detailBox('Lần thanh toán gần nhất', formatDateTime(statement.lastPaymentDate))}
                                </div>
                            </div>

                            <div style={{ marginBottom: '1rem' }}>
                                <div style={{ fontSize: '0.95rem', fontWeight: 600, marginBottom: '0.75rem' }}> Biến động tài chính</div>
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.75rem' }}>
                                    {detailBox('Dư nợ đầu kỳ', formatMoney(statement.previousBalance, currency))}
                                    {detailBox('Tổng phát sinh trong kỳ', formatMoney(statement.totalCharges, currency))}
                                    {detailBox('Tổng thanh toán', formatMoney(statement.totalPayments, currency))}
                                    {detailBox('Đã thanh toán sau sao kê', formatMoney(statement.paidAmountAfterStatement, currency))}
                                    {detailBox('Lãi đã áp dụng', formatMoney(statement.interestCharged, currency))}
                                    {detailBox('Phí trễ hạn đã áp dụng', formatMoney(statement.lateFeeCharged, currency))}
                                    {detailBox('Tối thiểu kỳ hiện tại', formatMoney(statement.currentMinimumDue, currency))}
                                    {detailBox('Tối thiểu quá hạn kỳ trước', formatMoney(statement.pastDueMinimum, currency))}
                                    {detailBox('Tổng tối thiểu cần trả', formatMoney(statement.totalMinimumDueNow, currency))}
                                    {detailBox('Còn tối thiểu kỳ hiện tại', formatMoney(remainingCurrentMinimumDue, currency))}
                                    {detailBox('Còn tối thiểu quá hạn', formatMoney(remainingPastDueMinimum, currency))}
                                    {detailBox('Tổng tối thiểu còn lại', formatMoney(remainingMinimumDue, currency))}
                                    {detailBox('Còn nợ của kỳ sao kê này', formatMoney(remainingBalance, currency))}
                                </div>
                            </div>

                            <div>
                                <div style={{ fontSize: '0.95rem', fontWeight: 600, marginBottom: '0.75rem' }}>Cấu hình thanh toán</div>
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.75rem' }}>
                                    {detailBox('Billing day', String(statement.billingDayOfMonth ?? '—'))}
                                    {detailBox('Số ngày đến hạn', String(statement.paymentDueDays ?? '—'))}
                                    {detailBox('Chính sách đến hạn', getDuePolicyText(statement))}
                                    {detailBox('Tình trạng quá hạn', getOverdueText(statement))}
                                    {detailBox('Còn đủ điều kiện miễn lãi', statement.gracePeriodEligible ? 'Có' : 'Không')}
                                    {detailBox('Lãi suất sao kê', formatRate(statement.interestRateAnnual))}
                                    {detailBox('Thời điểm áp lãi', formatDateTime(statement.interestAppliedAt))}
                                    {detailBox('Tỷ lệ phí trễ hạn', `${Number(statement.lateFeeRate || 0).toLocaleString('en-US', { maximumFractionDigits: 2 })}%`)}
                                    {detailBox('Phí trễ hạn cố định', formatMoney(statement.lateFeeFixed, currency))}
                                    {detailBox('Thời điểm áp phí trễ hạn', formatDateTime(statement.lateFeeAppliedAt))}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="card" style={{ marginBottom: '1rem' }}>
                        <div style={{ marginBottom: '1rem' }}>
                            <div style={{ fontSize: '1rem', fontWeight: 600 }}>Giao dịch trong kỳ</div>
                            <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                                Bao gồm toàn bộ giao dịch nằm trong khoảng sao kê
                            </div>
                        </div>
                        {renderTransactionTable(items, currency, 'Không có giao dịch nào trong kỳ này')}
                    </div>

                    <div className="card">
                        <div style={{ marginBottom: '1rem' }}>
                            <div style={{ fontSize: '1rem', fontWeight: 600 }}> Phát sinh sau sao kê</div>
                            <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                                Bao gồm thanh toán sau sao kê, lãi, phí trễ hạn và các điều chỉnh phát sinh sau ngày chốt sao kê
                            </div>
                        </div>
                        {renderTransactionTable(postStatementItems, currency, 'Không có phát sinh nào sau sao kê')}
                    </div>
                </>
            )}
        </div>
    );
}


