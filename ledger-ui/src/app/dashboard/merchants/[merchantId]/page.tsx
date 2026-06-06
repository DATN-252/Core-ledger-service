'use client';

import { useEffect, useState, use } from 'react';
import { getMerchantDetail, getMerchantTransactions, getSettlementBatches } from '@/lib/api';
import {
  getDisplayTransactionType,
  getTransactionFailureSummary,
  getTransactionStatusBadge,
  isNegativeTransaction,
  isPositiveTransaction,
} from '@/lib/transactionDisplay';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowLeft, faArrowRight, faLocationDot, faMoneyCheckDollar, faStore } from '@fortawesome/free-solid-svg-icons';
import Link from 'next/link';

export default function MerchantDetailPage({ params }: { params: Promise<{ merchantId: string }> }) {
  const { merchantId } = use(params);
  const [merchant, setMerchant] = useState<any>(null);
  const [transactions, setTransactions] = useState<any[]>([]);
  const [batches, setBatches] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [merchantData, txData, batchData] = await Promise.all([
          getMerchantDetail(merchantId),
          getMerchantTransactions(merchantId, 0, 10),
          getSettlementBatches(merchantId, 0, 5),
        ]);
        setMerchant(merchantData);
        setTransactions(txData?.content || []);
        setBatches(batchData?.content || []);
      } catch (error) {
        console.error(error);
        alert('Không thể tải thông tin merchant');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [merchantId]);

  const formatAmount = (txn: any) =>
    `${isNegativeTransaction(txn) ? '-' : '+'}${Number(txn.amount || 0).toLocaleString('en-US')} ${txn.currency || 'USD'}`;

  if (loading) return <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Đang tải...</div>;
  if (!merchant) return <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Không có dữ liệu</div>;

  return (
    <div className="animate-fade-in">
      <div style={{ marginBottom: '2rem' }}>
        <Link href="/dashboard/merchants" style={{ color: 'var(--text-secondary)', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', fontSize: '0.875rem' }}>
          <FontAwesomeIcon icon={faArrowLeft} /> Quay lại danh sách merchant
        </Link>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
          <FontAwesomeIcon icon={faStore} style={{ marginRight: '0.5rem' }} />
          {merchant.name}
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
          Merchant ID: {merchant.merchantId}
        </p>
      </div>

      <div className="stats-grid" style={{ marginBottom: '1.5rem' }}>
        <div className="stat-card">
          <div className="stat-label">Settlement account</div>
          <div className="stat-value" style={{ fontSize: '1.1rem' }}>{merchant.settlementAccountNumber || '—'}</div>
          <div className="stat-note">{merchant.settlementAccountName || '—'}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Số dư settlement</div>
          <div className="stat-value">{Number(merchant.settlementAccountBalance || 0).toLocaleString('en-US', { maximumFractionDigits: 2 })} USD</div>
          <div className="stat-note">{merchant.settlementBankName || '—'}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Category</div>
          <div className="stat-value" style={{ fontSize: '1.1rem' }}>{merchant.category || '—'}</div>
          <div className="stat-note">{merchant.status}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">City / Population</div>
          <div className="stat-value" style={{ fontSize: '1.1rem' }}>{merchant.cityName || '—'}</div>
          <div className="stat-note">{merchant.cityPopulation ? merchant.cityPopulation.toLocaleString('en-US') : '—'} dân</div>
        </div>
      </div>

      <div className="panel-grid" style={{ marginBottom: '1.5rem' }}>
        <div className="card">
          <h2 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '0.75rem' }}>Merchant profile</h2>
          <div className="info-list">
            <div className="info-row"><span className="info-label">Địa chỉ</span><span className="info-value">{merchant.address || '—'}</span></div>
            <div className="info-row"><span className="info-label">Postal code</span><span className="info-value">{merchant.postalCode || '—'}</span></div>
            <div className="info-row"><span className="info-label">Tọa độ</span><span className="info-value">{merchant.latitude != null && merchant.longitude != null ? `${merchant.latitude}, ${merchant.longitude}` : '—'}</span></div>
            <div className="info-row"><span className="info-label">Country</span><span className="info-value">{merchant.country || '—'}</span></div>
          </div>
        </div>

        <div className="card">
          <h2 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '0.75rem' }}>
            <FontAwesomeIcon icon={faMoneyCheckDollar} style={{ marginRight: '0.5rem' }} />
            Settlement summary
          </h2>
          <div className="info-list">
            <div className="info-row"><span className="info-label">Settlement bank</span><span className="info-value">{merchant.settlementBankName || '—'}</span></div>
            <div className="info-row"><span className="info-label">Settlement account name</span><span className="info-value">{merchant.settlementAccountName || '—'}</span></div>
            <div className="info-row"><span className="info-label">Settlement account number</span><span className="info-value">{merchant.settlementAccountNumber || '—'}</span></div>
            <div className="info-row"><span className="info-label">Current balance</span><span className="info-value">{Number(merchant.settlementAccountBalance || 0).toLocaleString('en-US', { maximumFractionDigits: 2 })} USD</span></div>
          </div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h2 style={{ fontSize: '1.05rem', fontWeight: 700 }}>Giao dịch gần đây</h2>
          <Link href="/dashboard/transactions" style={{ color: 'var(--accent)', textDecoration: 'none', fontSize: '0.875rem' }}>
            Xem tất cả
          </Link>
        </div>
        <div className="table-container">
          <table className="transaction-table transaction-table--dashboard">
            <thead>
              <tr>
                <th>#ID</th>
                <th>Ngày giờ</th>
                <th>Tài khoản</th>
                <th>Vị trí</th>
                <th>Loại</th>
                <th>Số tiền</th>
                <th>Trạng thái</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {transactions.length === 0 ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)' }}>Không có giao dịch</td>
                </tr>
              ) : transactions.map((txn) => (
                <tr key={txn.id}>
                  <td className="transaction-cell-id">#{txn.id}</td>
                  <td className="transaction-cell-time">{txn.transactionDate ? new Date(txn.transactionDate).toLocaleString('vi-VN') : '—'}</td>
                  <td className="transaction-cell-account">
                    <div style={{ fontFamily: 'monospace' }}>{txn.accountNumber || '—'}</div>
                    <div style={{ color: 'var(--text-secondary)', marginTop: '0.125rem' }}>{txn.accountType || ''}</div>
                  </td>
                  <td className="transaction-cell-location" title={txn.location || '—'}>
                    <span className="transaction-location-content">
                      <FontAwesomeIcon icon={faLocationDot} style={{ color: 'var(--text-secondary)' }} />
                      {txn.location || '—'}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${isPositiveTransaction(txn) ? 'badge-active' : 'badge-pending'}`}>
                      {getDisplayTransactionType(txn)}
                    </span>
                  </td>
                  <td style={{ fontWeight: 700, color: isNegativeTransaction(txn) ? 'var(--warning)' : 'var(--success)' }}>{formatAmount(txn)}</td>
                  <td>
                    {(() => {
                      const badge = getTransactionStatusBadge(txn);
                      const failureSummary = getTransactionFailureSummary(txn);
                      return (
                        <div>
                          <span className={`badge ${badge.className}`} style={badge.style}>
                            {badge.label}
                          </span>
                          {failureSummary ? (
                            <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginTop: '0.375rem', maxWidth: '180px', lineHeight: 1.4 }}>
                              {failureSummary}
                            </div>
                          ) : null}
                        </div>
                      );
                    })()}
                  </td>
                  <td className="transaction-cell-action">
                    <Link href={`/dashboard/transactions/${txn.id}`} className="transaction-detail-link">
                      Chi tiết
                      <FontAwesomeIcon icon={faArrowRight} />
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h2 style={{ fontSize: '1.05rem', fontWeight: 700 }}>Settlement batches gần đây</h2>
          <Link href="/dashboard/settlements" style={{ color: 'var(--accent)', textDecoration: 'none', fontSize: '0.875rem' }}>
            Mở settlement
          </Link>
        </div>
        <div className="table-container">
          <table className="settlement-table">
            <thead>
              <tr>
                <th>Batch</th>
                <th>Kỳ</th>
                <th>Gross</th>
                <th>Net</th>
                <th>Status</th>
                <th>Executed at</th>
              </tr>
            </thead>
            <tbody>
              {batches.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)' }}>Chưa có settlement batch</td>
                </tr>
              ) : batches.map((batch) => (
                <tr key={batch.id}>
                  <td>#{batch.id}</td>
                  <td>{batch.fromDate} → {batch.toDate}</td>
                  <td>{Number(batch.grossAmount || 0).toLocaleString('en-US', { maximumFractionDigits: 2 })} {batch.currency || 'USD'}</td>
                  <td style={{ fontWeight: 700 }}>{Number(batch.netAmount || 0).toLocaleString('en-US', { maximumFractionDigits: 2 })} {batch.currency || 'USD'}</td>
                  <td>
                    <span className={`badge ${batch.status === 'SETTLED' ? 'badge-active' : batch.status === 'PENDING' ? 'badge-pending' : 'badge-locked'}`}>
                      {batch.status}
                    </span>
                  </td>
                  <td>{batch.executedAt ? new Date(batch.executedAt).toLocaleString('vi-VN') : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

