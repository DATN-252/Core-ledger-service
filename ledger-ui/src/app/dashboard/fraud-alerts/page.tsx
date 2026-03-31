'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { getFraudAlerts } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faShieldHalved, faEye } from '@fortawesome/free-solid-svg-icons';

const STATUS_BADGE: Record<string, string> = {
  OPEN: 'badge-locked',
  WAITING_CUSTOMER_CONFIRMATION: 'badge-pending',
  CONFIRMED_BY_CUSTOMER: 'badge-active',
  REJECTED_BY_CUSTOMER: 'badge-locked',
  CARD_LOCKED: 'badge-locked',
  RESOLVED: 'badge-active',
  FALSE_POSITIVE: 'badge-pending',
};

const RISK_BADGE: Record<string, string> = {
  HIGH: 'badge-locked',
  MEDIUM: 'badge-pending',
  LOW: 'badge-active',
};

function formatResponseLabel(value?: string | null) {
  if (!value) return 'No response';
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function formatEnumLabel(value: string) {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export default function FraudAlertsPage() {
  const [alerts, setAlerts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState('ALL');
  const [riskLevel, setRiskLevel] = useState('ALL');

  useEffect(() => {
    loadAlerts();
  }, [status, riskLevel]);

  async function loadAlerts() {
    setLoading(true);
    try {
      const data = await getFraudAlerts({ status, riskLevel });
      setAlerts(data || []);
    } catch (err) {
      console.error('Failed to load fraud alerts', err);
      setAlerts([]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="animate-fade-in">
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
          <FontAwesomeIcon icon={faShieldHalved} style={{ marginRight: '0.5rem' }} />
          Fraud Alerts
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
          Theo dõi giao dịch bị nghi ngờ gian lận. Các thao tác xử lý được đặt trong trang chi tiết từng case.
        </p>
      </div>

      <div className="card">
        <div style={{ marginBottom: '1rem', display: 'grid', gridTemplateColumns: 'repeat(2, minmax(180px, 220px))', gap: '0.75rem' }}>
          <select className="input" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="ALL">Tất cả trạng thái</option>
            <option value="OPEN">{formatEnumLabel('OPEN')}</option>
            <option value="WAITING_CUSTOMER_CONFIRMATION">{formatEnumLabel('WAITING_CUSTOMER_CONFIRMATION')}</option>
            <option value="CONFIRMED_BY_CUSTOMER">{formatEnumLabel('CONFIRMED_BY_CUSTOMER')}</option>
            <option value="CARD_LOCKED">{formatEnumLabel('CARD_LOCKED')}</option>
            <option value="RESOLVED">{formatEnumLabel('RESOLVED')}</option>
            <option value="FALSE_POSITIVE">{formatEnumLabel('FALSE_POSITIVE')}</option>
          </select>
          <select className="input" value={riskLevel} onChange={(e) => setRiskLevel(e.target.value)}>
            <option value="ALL">Tất cả mức rủi ro</option>
            <option value="HIGH">{formatEnumLabel('HIGH')}</option>
            <option value="MEDIUM">{formatEnumLabel('MEDIUM')}</option>
            <option value="LOW">{formatEnumLabel('LOW')}</option>
          </select>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>Đang tải fraud alerts...</div>
        ) : alerts.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
            Không có fraud alert nào.
          </div>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>#Alert</th>
                  <th>Thời gian</th>
                  <th>Thẻ</th>
                  <th>Tài khoản</th>
                  <th>Merchant</th>
                  <th>Số tiền</th>
                  <th>Risk</th>
                  <th>Trạng thái</th>
                  <th>Notification</th>
                  <th>Chi tiết</th>
                </tr>
              </thead>
              <tbody>
                {alerts.map((alert: any) => (
                  <tr key={alert.id}>
                    <td>#{alert.id}</td>
                    <td>{alert.transactionTime ? new Date(alert.transactionTime).toLocaleString('vi-VN') : '—'}</td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{alert.maskedPan || '—'}</div>
                      <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginTop: '0.125rem' }}>paymentId: {alert.paymentId || '—'}</div>
                    </td>
                    <td>
                      <div>{alert.accountId || '—'}</div>
                      <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginTop: '0.125rem' }}>{alert.accountType || '—'}</div>
                    </td>
                    <td>
                      <div>{alert.merchantName || '—'}</div>
                      <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginTop: '0.125rem' }}>{alert.merchantId || '—'}</div>
                    </td>
                    <td style={{ fontWeight: 700, color: 'var(--warning)' }}>
                      {Number(alert.amount || 0).toLocaleString('en-US')} {alert.currency || 'USD'}
                    </td>
                    <td>
                      <span className={`badge ${RISK_BADGE[alert.riskLevel] || 'badge-pending'}`}>
                        {alert.riskLevel || 'UNKNOWN'}
                      </span>
                      <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginTop: '0.125rem' }}>
                        score: {alert.riskScore != null ? Number(alert.riskScore).toFixed(2) : '—'}
                      </div>
                    </td>
                      <td>
                        <span className={`badge ${STATUS_BADGE[alert.status] || 'badge-pending'}`}>
                          {alert.status}
                        </span>
                        {alert.customerResponse ? (
                          <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginTop: '0.125rem' }}>
                            customer: {formatResponseLabel(alert.customerResponse)}
                          </div>
                        ) : null}
                      </td>
                    <td>
                      <span className={`badge ${alert.notificationSent ? 'badge-active' : 'badge-pending'}`}>
                        {alert.notificationSent ? 'PUSH_SENT' : 'PENDING'}
                      </span>
                    </td>
                    <td style={{ minWidth: '150px' }}>
                      <Link href={`/dashboard/fraud-alerts/${alert.id}`} className="btn-secondary" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', padding: '0.45rem 0.8rem' }}>
                        <FontAwesomeIcon icon={faEye} />
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
