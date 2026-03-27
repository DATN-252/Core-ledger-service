'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowLeft, faCalendarDays, faFileInvoiceDollar, faRotate } from '@fortawesome/free-solid-svg-icons';
import { generateLoanMonthlyStatement, getLoan, getLoanMonthlyStatements } from '@/lib/api';

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

function toDateInputValue(date = new Date()) {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function getBillingDateForMonth(year: number, monthIndex: number, billingDay: number) {
    const lastDay = new Date(year, monthIndex + 1, 0).getDate();
    const day = Math.min(billingDay, lastDay);
    return toDateInputValue(new Date(year, monthIndex, day));
}

function getSuggestedBillingDate(billingDay: number) {
    const now = new Date();
    const currentMonthCandidate = new Date(
        now.getFullYear(),
        now.getMonth(),
        Math.min(billingDay, new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate())
    );

    if (now <= currentMonthCandidate) {
        return toDateInputValue(currentMonthCandidate);
    }

    return getBillingDateForMonth(now.getFullYear(), now.getMonth() + 1, billingDay);
}

function getExpectedBillingDateForSelection(selectedDate: string, billingDay: number) {
    const baseDate = new Date(`${selectedDate}T00:00:00`);
    return getBillingDateForMonth(baseDate.getFullYear(), baseDate.getMonth(), billingDay);
}

export default function LoanStatementsPage() {
    const params = useParams<{ loanId: string }>();
    const loanId = decodeURIComponent(params.loanId);

    const [loan, setLoan] = useState<any>(null);
    const [statements, setStatements] = useState<any[]>([]);
    const [billingDate, setBillingDate] = useState('');
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        loadData();
    }, [loanId]);

    async function loadData() {
        setLoading(true);
        setError('');
        try {
            const [loanData, statementData] = await Promise.all([
                getLoan(loanId),
                getLoanMonthlyStatements(loanId),
            ]);
            setLoan(loanData);
            setStatements(statementData || []);
            if (loanData?.billingDayOfMonth) {
                setBillingDate(getSuggestedBillingDate(Number(loanData.billingDayOfMonth)));
            }
        } catch (e: any) {
            setError(e.message || 'Không thể tải dữ liệu sao kê');
            setStatements([]);
        } finally {
            setLoading(false);
        }
    }

    async function handleGenerate(e: React.FormEvent) {
        e.preventDefault();
        const billingDay = Number(loan?.billingDayOfMonth || 0);
        const expectedBillingDate = billingDay && billingDate
            ? getExpectedBillingDateForSelection(billingDate, billingDay)
            : billingDate;
        if (billingDay && billingDate !== expectedBillingDate) {
            setError(`Ngày sao kê hợp lệ của tháng đã chọn là ${expectedBillingDate}.`);
            return;
        }

        setSubmitting(true);
        setError('');
        try {
            await generateLoanMonthlyStatement(loanId, billingDate);
            await loadData();
        } catch (e: any) {
            setError(e.message || 'Không thể generate sao kê');
        } finally {
            setSubmitting(false);
        }
    }

    const currency = loan?.currency?.code || loan?.currency || 'USD';
    const creditLimit = Number(loan?.principal || 0);
    const outstanding = Number(loan?.principalOutstanding || 0);
    const available = creditLimit - outstanding;
    const billingDayOfMonth = Number(loan?.billingDayOfMonth || 0);

    return (
        <div className="animate-fade-in">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '1rem', marginBottom: '2rem' }}>
                <div>
                    <div style={{ marginBottom: '0.75rem' }}>
                        <Link href="/dashboard/loans" className="btn-secondary" style={{ textDecoration: 'none' }}>
                            <FontAwesomeIcon icon={faArrowLeft} /> Quay lại danh sách
                        </Link>
                    </div>
                    <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                        <FontAwesomeIcon icon={faFileInvoiceDollar} style={{ marginRight: '0.5rem' }} />
                        Sao kê tín dụng
                    </h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                        Tài khoản {loanId} {loan?.clientName ? `• ${loan.clientName}` : ''}
                    </p>
                </div>
            </div>

            {error && <div className="error-banner">{error}</div>}

            <div className="card" style={{ marginBottom: '1rem' }}>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', marginBottom: '1.25rem' }}>
                    <div>
                        <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Hạn mức</div>
                        <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{formatMoney(creditLimit, currency)}</div>
                    </div>
                    <div>
                        <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Dư nợ hiện tại</div>
                        <div style={{ fontWeight: 700, fontSize: '1.125rem', color: outstanding > 0 ? 'var(--warning)' : 'var(--text-primary)' }}>
                            {formatMoney(outstanding, currency)}
                        </div>
                    </div>
                    <div>
                        <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Khả dụng</div>
                        <div style={{ fontWeight: 700, fontSize: '1.125rem', color: available > 0 ? 'var(--success)' : 'var(--danger)' }}>
                            {formatMoney(available, currency)}
                        </div>
                    </div>
                    <div>
                        <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginBottom: '0.35rem' }}>Billing day</div>
                        <div style={{ fontWeight: 700, fontSize: '1.125rem' }}>{billingDayOfMonth || '—'}</div>
                    </div>
                </div>

                <div style={{ paddingTop: '1rem', borderTop: '1px solid var(--border)' }}>
                    <div style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '0.35rem' }}>Generate sao kê</div>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginBottom: '1rem' }}>
                        {billingDayOfMonth
                            ? `Chỉ có thể tạo sao kê vào ngày ${billingDayOfMonth} của từng tháng.`
                            : 'Chọn billing date để tạo snapshot sao kê.'}
                    </p>

                    <form onSubmit={handleGenerate} style={{ display: 'flex', gap: '0.75rem', alignItems: 'end', flexWrap: 'wrap' }}>
                        <div style={{ minWidth: '220px' }}>
                            <label className="info-label" style={{ display: 'block', marginBottom: '0.4rem' }}>Billing Date</label>
                            <input
                                className="input"
                                type="date"
                                value={billingDate}
                                onChange={e => setBillingDate(e.target.value)}
                                required
                            />
                        </div>
                        <button className="btn-primary" type="submit" disabled={submitting}>
                            <FontAwesomeIcon icon={faRotate} />
                            {submitting ? 'Đang xử lý...' : 'Generate'}
                        </button>
                    </form>
                </div>
            </div>

            <div className="card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', gap: '1rem' }}>
                    <div>
                        <div style={{ fontSize: '1rem', fontWeight: 600 }}>Lịch sử sao kê</div>
                        <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                            {statements.length} kỳ sao kê đã được tạo
                        </div>
                    </div>
                </div>

                {loading ? (
                    <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>Đang tải dữ liệu sao kê...</div>
                ) : (
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>Billing Date</th>
                                    <th>Kỳ sao kê</th>
                                    <th>Trạng thái</th>
                                    <th>Ngày đến hạn</th>
                                    <th>Dư nợ cuối kỳ</th>
                                    <th>Còn phải trả tối thiểu</th>
                                    <th>Còn dư nợ</th>
                                    <th>Số GD</th>
                                    <th>Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                {statements.length === 0 ? (
                                    <tr>
                                        <td colSpan={9} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
                                            Chưa có kỳ sao kê nào
                                        </td>
                                    </tr>
                                ) : statements.map((statement) => (
                                    <tr key={statement.statementId ?? statement.billingDate}>
                                        <td style={{ fontFamily: 'monospace', fontSize: '0.8125rem' }}>{statement.billingDate}</td>
                                        <td>{statement.statementPeriodStart} đến {statement.statementPeriodEnd}</td>
                                        <td>
                                            <span className={`badge ${STATEMENT_STATUS_BADGE[statement.statementStatus] || 'badge-pending'}`}>
                                                {STATEMENT_STATUS_LABEL[statement.statementStatus] || statement.statementStatus || 'Chưa thanh toán'}
                                            </span>
                                        </td>
                                        <td>{statement.dueDate}</td>
                                        <td style={{ fontWeight: 600 }}>{formatMoney(statement.newBalance, currency)}</td>
                                        <td>{formatMoney(statement.remainingMinimumDue ?? statement.minimumDue, currency)}</td>
                                        <td>{formatMoney(statement.remainingBalance ?? statement.newBalance, currency)}</td>
                                        <td>{statement.transactionCount || 0}</td>
                                        <td>
                                            <Link
                                                href={`/dashboard/loans/${loanId}/statements/${statement.billingDate}`}
                                                className="btn-secondary"
                                                style={{ padding: '0.4rem 0.8rem', textDecoration: 'none' }}
                                            >
                                                Chi tiết
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
