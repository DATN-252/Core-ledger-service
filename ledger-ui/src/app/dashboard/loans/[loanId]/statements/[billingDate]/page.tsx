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
    const [paymentNote, setPaymentNote] = useState('Thanh toán sao kê');

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
                `Đã thanh toán ${formatMoney(response.paymentAmount, response.currency)}. Trạng thái mới: ${
                    STATEMENT_STATUS_LABEL[response.statementStatusAfter] || response.statementStatusAfter
                }`,
            );
            if (paymentOption === 'CUSTOM') {
                setCustomAmount('');
            }
            await loadDetail();
        } catch (e: any) {
            setPaymentError(e.message || 'Không thể thanh toán sao kê');
        } finally {
            setPaymentLoading(false);
        }
    }

    const currency = statement?.currency || 'USD';
    const items = statement?.items || [];
    const isPaid = (statement?.remainingBalance ?? statement?.newBalance ?? 0) <= 0;
    const canUseInternalSavings = savingsAccounts.length > 0;

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
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Dư nợ cuối kỳ</div>
                                <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{formatMoney(statement.newBalance, currency)}</div>
                            </div>
                            <div>
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Còn phải trả tối thiểu</div>
                                <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{formatMoney(statement.remainingMinimumDue ?? statement.minimumDue, currency)}</div>
                            </div>
                            <div>
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Còn dư nợ</div>
                                <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{formatMoney(statement.remainingBalance ?? statement.newBalance, currency)}</div>
                            </div>
                            <div>
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Trạng thái</div>
                                <div>
                                    <span className={`badge ${STATEMENT_STATUS_BADGE[statement.statementStatus] || 'badge-pending'}`}>
                                        {STATEMENT_STATUS_LABEL[statement.statementStatus] || statement.statementStatus || 'Chưa thanh toán'}
                                    </span>
                                </div>
                            </div>
                        </div>

                        <div style={{ paddingTop: '1rem', borderTop: '1px solid var(--border)', marginTop: '1rem' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem', alignItems: 'flex-start', flexWrap: 'wrap', marginBottom: '1rem' }}>
                                <div>
                                    <div style={{ fontSize: '1rem', fontWeight: 600 }}>Thanh toán sao kê</div>
                                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                                        Trả tối thiểu, toàn bộ sao kê hoặc nhập số tiền tùy chỉnh.
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
                                        <label className="label">Tuỳ chọn thanh toán</label>
                                        <select className="input" value={paymentOption} onChange={(e) => setPaymentOption(e.target.value)}>
                                            <option value="MINIMUM_DUE">Thanh toán tối thiểu</option>
                                            <option value="STATEMENT_BALANCE">Thanh toán toàn bộ sao kê</option>
                                            <option value="CUSTOM">Thanh toán số khác</option>
                                        </select>
                                    </div>

                                    <div>
                                        <label className="label">Nguồn thanh toán</label>
                                        <select className="input" value={paymentSource} onChange={(e) => setPaymentSource(e.target.value)}>
                                            <option value="INTERNAL_SAVINGS">Tài khoản debit nội bộ</option>
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
                                            <label className="label">Tài khoản debit</label>
                                            <select
                                                className="input"
                                                value={sourceAccountNumber}
                                                onChange={(e) => setSourceAccountNumber(e.target.value)}
                                                disabled={!canUseInternalSavings}
                                            >
                                                {!canUseInternalSavings ? (
                                                    <option value="">Không có tài khoản ACTIVE</option>
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
                                                ? `Sẽ trả ${formatMoney(statement.remainingMinimumDue ?? statement.minimumDue, currency)}`
                                                : paymentOption === 'STATEMENT_BALANCE'
                                                    ? `Sẽ trả ${formatMoney(statement.remainingBalance ?? statement.newBalance, currency)}`
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
                                    {detailBox('Lần thanh toán gần nhất', statement.lastPaymentDate ? String(statement.lastPaymentDate).replace('T', ' ') : '—')}
                                </div>
                            </div>

                            <div style={{ marginBottom: '1rem' }}>
                                <div style={{ fontSize: '0.95rem', fontWeight: 600, marginBottom: '0.75rem' }}>Biến động tài chính</div>
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.75rem' }}>
                                    {detailBox('Dư nợ đầu kỳ', formatMoney(statement.previousBalance, currency))}
                                    {detailBox('Tổng chi tiêu', formatMoney(statement.totalCharges, currency))}
                                    {detailBox('Tổng thanh toán', formatMoney(statement.totalPayments, currency))}
                                    {detailBox('Đã thanh toán sau sao kê', formatMoney(statement.paidAmountAfterStatement, currency))}
                                    {detailBox('Còn phải trả tối thiểu', formatMoney(statement.remainingMinimumDue, currency))}
                                    {detailBox('Còn dư nợ', formatMoney(statement.remainingBalance, currency))}
                                </div>
                            </div>

                            <div>
                                <div style={{ fontSize: '0.95rem', fontWeight: 600, marginBottom: '0.75rem' }}>Cấu hình thanh toán</div>
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.75rem' }}>
                                    {detailBox('Billing day', String(statement.billingDayOfMonth ?? '—'))}
                                    {detailBox('Số ngày đến hạn', String(statement.paymentDueDays ?? '—'))}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="card">
                        <div style={{ marginBottom: '1rem' }}>
                            <div style={{ fontSize: '1rem', fontWeight: 600 }}>Giao dịch trong kỳ</div>
                            <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                                Bao gồm toàn bộ giao dịch nằm trong khoảng sao kê
                            </div>
                        </div>

                        <div className="table-container">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Thời gian</th>
                                        <th>Payment ID</th>
                                        <th>Loại</th>
                                        <th>Merchant</th>
                                        <th>Số tiền</th>
                                        <th>Dư nợ sau GD</th>
                                        <th>Trạng thái</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {items.length === 0 ? (
                                        <tr>
                                            <td colSpan={7} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
                                                Không có giao dịch nào trong kỳ này
                                            </td>
                                        </tr>
                                    ) : items.map((item: any) => (
                                        <tr key={`${item.paymentId}-${item.transactionDate}`}>
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
                    </div>
                </>
            )}
        </div>
    );
}
