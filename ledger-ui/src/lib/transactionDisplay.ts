export function getDisplayTransactionType(txn: any) {
    if (!txn) return 'UNKNOWN';
    if (txn.transactionType === 'SETTLEMENT' || txn.channel === 'SETTLEMENT') return 'SETTLEMENT';
    return txn.transactionType || 'UNKNOWN';
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
