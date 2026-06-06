export function getDisplayTransactionType(txn: any) {
    if (!txn) return 'UNKNOWN';
    if (txn.transactionType === 'SETTLEMENT' || txn.channel === 'SETTLEMENT') return 'SETTLEMENT';
    return txn.transactionType || 'UNKNOWN';
}

function normalizeFailureMessage(message: string) {
    return message
        .replace(/^payment failed:\s*/i, '')
        .replace(/^failed (charge|withdrawal):\s*/i, '')
        .trim();
}

export function isStatementPaymentTransaction(txn: any) {
    return !!txn && txn.channel === 'STATEMENT_PAYMENT';
}

export function getDisplayCounterpartyId(txn: any) {
    if (!txn) return '—';
    if (isStatementPaymentTransaction(txn)) {
        return '—';
    }
    return txn.merchantId || '—';
}

export function getDisplayCounterpartyName(txn: any) {
    if (!txn) return '—';
    if (isStatementPaymentTransaction(txn)) {
        return txn.merchantName || 'Thanh toán sao kê';
    }
    return txn.merchantName || '—';
}

export function getCounterpartyColumnTitle(txn: any) {
    return isStatementPaymentTransaction(txn) ? 'Đích thanh toán' : 'Merchant';
}

export function isNegativeTransaction(txn: any) {
    return ['WITHDRAWAL', 'CHARGE', 'FEE'].includes(getDisplayTransactionType(txn));
}

export function isPositiveTransaction(txn: any) {
    return ['DEPOSIT', 'SETTLEMENT', 'PAYMENT', 'REFUND', 'REVERSAL'].includes(getDisplayTransactionType(txn));
}

export function getTransactionStatusBadge(txnOrStatus: any) {
    const status = typeof txnOrStatus === 'string' ? txnOrStatus : txnOrStatus?.status;
    switch (status) {
        case 'FAILED':
            return {
                label: 'THẤT BẠI',
                className: '',
                style: { backgroundColor: 'rgba(239, 68, 68, 0.1)', color: 'var(--error)' },
            };
        case 'REVERSED':
            return { label: 'ĐÃ ĐẢO', className: 'badge-pending', style: {} };
        case 'REFUNDED':
            return { label: 'ĐÃ HOÀN', className: 'badge-pending', style: {} };
        default:
            return { label: 'THÀNH CÔNG', className: 'badge-active', style: {} };
    }
}

export function getTransactionFailureMessage(txn: any) {
    if (!txn || txn.status !== 'FAILED') return '';
    const rawMessage = txn.responseMessage || txn.failureReason || txn.description || '';
    if (!rawMessage) return '';
    return normalizeFailureMessage(String(rawMessage));
}

export function getTransactionFailureCode(txn: any) {
    if (!txn || txn.status !== 'FAILED') return '';
    return txn.responseCode || '';
}

export function getTransactionFailureSummary(txn: any) {
    const code = getTransactionFailureCode(txn);
    const message = getTransactionFailureMessage(txn);
    if (code && message) return `${code} · ${message}`;
    return message || code || '';
}
