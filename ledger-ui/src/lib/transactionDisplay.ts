export function getDisplayTransactionType(txn: any) {
    if (!txn) return 'UNKNOWN';
    if (txn.transactionType === 'SETTLEMENT' || txn.channel === 'SETTLEMENT') return 'SETTLEMENT';
    return txn.transactionType || 'UNKNOWN';
}

export function isNegativeTransaction(txn: any) {
    return ['WITHDRAWAL', 'CHARGE', 'FEE'].includes(getDisplayTransactionType(txn));
}

export function isPositiveTransaction(txn: any) {
    return ['DEPOSIT', 'SETTLEMENT', 'PAYMENT', 'REFUND', 'REVERSAL'].includes(getDisplayTransactionType(txn));
}

