'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { getTransactionDetail } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowLeft, faLocationDot, faReceipt } from '@fortawesome/free-solid-svg-icons';
import {
    getDisplayCounterpartyId,
    getDisplayCounterpartyName,
    getDisplayTransactionType,
    getTransactionFailureCode,
    getTransactionFailureMessage,
    getTransactionStatusBadge,
    isStatementPaymentTransaction,
} from '@/lib/transactionDisplay';

type TransactionDetailPageProps = {
    params: Promise<{ id: string }>;
};

export default function TransactionDetailPage({ params }: TransactionDetailPageProps) {
    const [id, setId] = useState<string>('');
    const [txn, setTxn] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        params.then(({ id }) => setId(id));
    }, [params]);

    useEffect(() => {
        if (!id) return;

        setLoading(true);
        setError('');
        getTransactionDetail(id)
            .then(setTxn)
            .catch((err) => setError(err.message || 'Không thể tải chi tiết giao dịch'))
            .finally(() => setLoading(false));
    }, [id]);

    const counterpartyIdLabel = txn && isStatementPaymentTransaction(txn) ? 'Đích thanh toán' : 'Merchant ID';
    const counterpartyNameLabel = txn && isStatementPaymentTransaction(txn) ? 'Loại đối tác' : 'Merchant name';
    const statusBadge = txn ? getTransactionStatusBadge(txn) : null;
    const failureCode = txn ? getTransactionFailureCode(txn) : '';
    const failureMessage = txn ? getTransactionFailureMessage(txn) : '';

    const detailRows = txn ? [
        ['Mã nội bộ', `#${txn.id}`],
        ['Payment ID', txn.paymentId || '—'],
        ['Idempotency Key', txn.idempotencyKey || '—'],
        ['Giao dịch gốc', txn.originalTransactionId || '—'],
        ['Thời gian', txn.transactionDate ? new Date(txn.transactionDate).toLocaleString('vi-VN') : '—'],
        ['Loại giao dịch', getDisplayTransactionType(txn)],
        ['Trạng thái', txn.status || '—'],
        ['Số tiền', `${Number(txn.amount || 0).toLocaleString('en-US')} ${txn.currency || 'USD'}`],
        ['Dư sau giao dịch', txn.balanceAfter != null ? `${Number(txn.balanceAfter).toLocaleString('en-US')} ${txn.currency || 'USD'}` : '—'],
        ['Tài khoản', txn.accountNumber || '—'],
        ['Loại tài khoản', txn.accountType || '—'],
        [counterpartyIdLabel, getDisplayCounterpartyId(txn)],
        [counterpartyNameLabel, getDisplayCounterpartyName(txn)],
        ['Kênh', txn.channel || '—'],
        ['Card network', txn.cardNetwork || '—'],
        ['Auth code', txn.authCode || '—'],
        ['STAN', txn.stan || '—'],
        ['RRN', txn.rrn || '—'],
        ['External reference', txn.externalReference || '—'],
        ['Response code', txn.responseCode || '—'],
        ['Response message', txn.responseMessage || '—'],
        ['Mô tả', txn.description || '—'],
    ] : [];

    return (
        <div className="animate-fade-in">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '2rem', gap: '1rem' }}>
                <div>
                    <div style={{ marginBottom: '0.75rem' }}>
                        <Link href="/dashboard/transactions" style={{ color: 'var(--text-secondary)', textDecoration: 'none', fontSize: '0.875rem' }}>
                            <FontAwesomeIcon icon={faArrowLeft} style={{ marginRight: '0.5rem' }} />
                            Quay lại lịch sử giao dịch
                        </Link>
                    </div>
                    <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                        <FontAwesomeIcon icon={faReceipt} style={{ marginRight: '0.5rem' }} />
                        Chi tiết giao dịch
                    </h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                        Xem đầy đủ thông tin transaction, account, merchant và dữ liệu xử lý.
                    </p>
                </div>
            </div>

            {loading ? (
                <div className="card" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Đang tải...</div>
            ) : error ? (
                <div className="card" style={{ padding: '2rem', color: 'var(--error)' }}>{error}</div>
            ) : txn ? (
                <>
                    <div className="stats-grid" style={{ marginBottom: '1.5rem' }}>
                        <div className="stat-card">
                            <div className="stat-label">Số tiền</div>
                            <div className="stat-value">{Number(txn.amount || 0).toLocaleString('en-US')} {txn.currency || 'USD'}</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-label">Loại giao dịch</div>
                            <div className="stat-value">{getDisplayTransactionType(txn)}</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-label">Trạng thái</div>
                            <div className="stat-value">
                                {statusBadge ? (
                                    <span className={`badge ${statusBadge.className}`} style={statusBadge.style}>
                                        {statusBadge.label}
                                    </span>
                                ) : txn.status || '—'}
                            </div>
                            {failureMessage ? (
                                <div className="stat-note" style={{ marginTop: '0.6rem', color: 'var(--text-secondary)' }}>
                                    {failureCode ? `${failureCode} · ` : ''}{failureMessage}
                                </div>
                            ) : null}
                        </div>
                        <div className="stat-card">
                            <div className="stat-label">Tài khoản</div>
                            <div className="stat-value">{txn.accountNumber || '—'}</div>
                        </div>
                    </div>

                    <div className="card" style={{ marginBottom: '1.5rem' }}>
                        {failureMessage ? (
                            <div style={{
                                marginBottom: '1rem',
                                padding: '1rem 1.125rem',
                                borderRadius: '0.9rem',
                                border: '1px solid rgba(239, 68, 68, 0.25)',
                                background: 'rgba(239, 68, 68, 0.08)',
                            }}>
                                <div style={{ color: 'var(--error)', fontWeight: 700, marginBottom: '0.3rem' }}>Lỗi giao dịch</div>
                                <div style={{ color: 'var(--text-primary)', lineHeight: 1.6 }}>
                                    {failureCode ? `Mã ${failureCode}: ` : ''}{failureMessage}
                                </div>
                            </div>
                        ) : null}
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1rem' }}>
                            {detailRows.map(([label, value]) => (
                                <div key={label} style={{ border: '1px solid var(--border)', borderRadius: '0.9rem', padding: '1rem 1.125rem', background: 'rgba(255,255,255,0.02)' }}>
                                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem', marginBottom: '0.4rem' }}>{label}</div>
                                    <div style={{ fontWeight: 600, wordBreak: 'break-word' }}>{value}</div>
                                </div>
                            ))}
                        </div>
                    </div>

                    <div className="card">
                        <div style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '1rem' }}>
                            <FontAwesomeIcon icon={faLocationDot} style={{ marginRight: '0.5rem' }} />
                            Vị trí giao dịch
                        </div>
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem' }}>
                            <div style={{ border: '1px solid var(--border)', borderRadius: '0.9rem', padding: '1rem 1.125rem', background: 'rgba(255,255,255,0.02)' }}>
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem', marginBottom: '0.4rem' }}>Địa điểm</div>
                                <div style={{ fontWeight: 600 }}>{txn.location || '—'}</div>
                            </div>
                            <div style={{ border: '1px solid var(--border)', borderRadius: '0.9rem', padding: '1rem 1.125rem', background: 'rgba(255,255,255,0.02)' }}>
                                <div style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem', marginBottom: '0.4rem' }}>Tọa độ</div>
                                <div style={{ fontWeight: 600 }}>
                                    {txn.latitude != null && txn.longitude != null
                                        ? `${txn.latitude}, ${txn.longitude}`
                                        : '—'}
                                </div>
                            </div>
                        </div>
                    </div>
                </>
            ) : null}
        </div>
    );
}
