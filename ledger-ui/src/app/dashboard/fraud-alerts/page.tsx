'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { getFraudAlerts, getFraudAlertSummary } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEye, faGear, faShieldHalved } from '@fortawesome/free-solid-svg-icons';

const STATUS_BADGE: Record<string, string> = {
  OPEN: 'badge-locked',
  WAITING_CUSTOMER_CONFIRMATION: 'badge-pending',
  CONFIRMED_BY_CUSTOMER: 'badge-active',
  REJECTED_BY_CUSTOMER: 'badge-locked',
  CARD_LOCKED: 'badge-locked',
  RESOLVED: 'badge-active',
  FALSE_POSITIVE: 'badge-pending',
  ACTIVE: 'badge-active',
  LOCKED: 'badge-locked',
  EXPIRED: 'badge-pending',
  CANCELLED: 'badge-locked',
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

const SUMMARY_CARDS: Array<{ key: string; label: string }> = [
  { key: 'totalAlerts', label: 'Tổng alert' },
  { key: 'openAlerts', label: 'Open' },
  { key: 'waitingCustomerConfirmation', label: 'Chờ khách xác nhận' },
  { key: 'highRiskAlerts', label: 'High risk' },
  { key: 'cardLockedAlerts', label: 'Đã khóa thẻ' },
  { key: 'resolvedAlerts', label: 'Resolved' },
];

export default function FraudAlertsPage() {
  const [alerts, setAlerts] = useState<any[]>([]);
  const [summary, setSummary] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState('ALL');
  const [riskLevel, setRiskLevel] = useState('ALL');
  const [customerResponse, setCustomerResponse] = useState('ALL');
  const [notificationStatus, setNotificationStatus] = useState('ALL');
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');

  useEffect(() => {
    void loadSummary();
  }, []);

  useEffect(() => {
    void loadAlerts();
  }, [status, riskLevel, customerResponse, notificationStatus, search]);

  async function loadSummary() {
    try {
      const data = await getFraudAlertSummary();
      setSummary(data || {});
    } catch (err) {
      console.error('Failed to load fraud alert summary', err);
      setSummary({});
    }
  }

  async function loadAlerts() {
    setLoading(true);
    try {
      const data = await getFraudAlerts({
        status,
        riskLevel,
        customerResponse,
        notificationStatus,
        search,
      });
      setAlerts(data || []);
    } catch (err) {
      console.error('Failed to load fraud alerts', err);
      setAlerts([]);
    } finally {
      setLoading(false);
    }
  }

  function applySearch() {
    setSearch(searchInput.trim());
  }

  return (
    <div className="animate-fade-in">
      <div style={{ marginBottom: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem', alignItems: 'flex-start', flexWrap: 'wrap' }}>
          <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
              <FontAwesomeIcon icon={faShieldHalved} style={{ marginRight: '0.5rem' }} />
              Fraud Alerts
            </h1>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
              Theo dõi giao dịch nghi ngờ gian lận, trạng thái phản hồi khách hàng và tiến độ xử lý của đội vận hành.
            </p>
          </div>
          <Link href="/dashboard/fraud-alerts/settings" className="btn-secondary" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
            <FontAwesomeIcon icon={faGear} />
            Fraud Settings
          </Link>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))', gap: '0.75rem', marginBottom: '1rem' }}>
        {SUMMARY_CARDS.map((item) => (
          <div key={item.key} className="card" style={{ padding: '1rem' }}>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginBottom: '0.35rem' }}>{item.label}</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 700 }}>{summary[item.key] ?? 0}</div>
          </div>
        ))}
      </div>

      <div className="card">
        <div
          style={{
            marginBottom: '1rem',
            display: 'grid',
            gridTemplateColumns: 'minmax(220px, 1.2fr) repeat(4, minmax(160px, 220px)) auto',
            gap: '0.75rem',
            alignItems: 'end',
          }}
        >
          <div>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginBottom: '0.35rem' }}>Tìm kiếm</div>
            <input
              className="input"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') applySearch();
              }}
              placeholder="paymentId, accountId, merchant, PAN..."
            />
          </div>
          <div>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginBottom: '0.35rem' }}>Trạng thái case</div>
            <select className="input" value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="ALL">Tất cả trạng thái</option>
              <option value="OPEN">{formatEnumLabel('OPEN')}</option>
              <option value="WAITING_CUSTOMER_CONFIRMATION">{formatEnumLabel('WAITING_CUSTOMER_CONFIRMATION')}</option>
              <option value="CONFIRMED_BY_CUSTOMER">{formatEnumLabel('CONFIRMED_BY_CUSTOMER')}</option>
              <option value="CARD_LOCKED">{formatEnumLabel('CARD_LOCKED')}</option>
              <option value="RESOLVED">{formatEnumLabel('RESOLVED')}</option>
              <option value="FALSE_POSITIVE">{formatEnumLabel('FALSE_POSITIVE')}</option>
            </select>
          </div>
          <div>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginBottom: '0.35rem' }}>Mức rủi ro</div>
            <select className="input" value={riskLevel} onChange={(e) => setRiskLevel(e.target.value)}>
              <option value="ALL">Tất cả mức rủi ro</option>
              <option value="HIGH">{formatEnumLabel('HIGH')}</option>
              <option value="MEDIUM">{formatEnumLabel('MEDIUM')}</option>
              <option value="LOW">{formatEnumLabel('LOW')}</option>
            </select>
          </div>
          <div>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginBottom: '0.35rem' }}>Phản hồi khách</div>
            <select className="input" value={customerResponse} onChange={(e) => setCustomerResponse(e.target.value)}>
              <option value="ALL">Tất cả phản hồi</option>
              <option value="NO_RESPONSE">{formatResponseLabel('NO_RESPONSE')}</option>
              <option value="CONFIRMED">{formatResponseLabel('CONFIRMED')}</option>
              <option value="REJECTED">{formatResponseLabel('REJECTED')}</option>
            </select>
          </div>
          <div>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginBottom: '0.35rem' }}>Thông báo</div>
            <select className="input" value={notificationStatus} onChange={(e) => setNotificationStatus(e.target.value)}>
              <option value="ALL">Tất cả</option>
              <option value="PUSH_SENT">Push Sent</option>
              <option value="PENDING">Pending</option>
            </select>
          </div>
          <button className="btn-primary" onClick={applySearch} style={{ height: '44px' }}>
            Tìm
          </button>
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
                        {formatEnumLabel(alert.riskLevel || 'UNKNOWN')}
                      </span>
                      <div style={{ color: 'var(--text-secondary)', fontSize: '0.75rem', marginTop: '0.125rem' }}>
                        score: {alert.riskScore != null ? Number(alert.riskScore).toFixed(2) : '—'}
                      </div>
                    </td>
                    <td>
                      <span className={`badge ${STATUS_BADGE[alert.status] || 'badge-pending'}`}>
                        {formatEnumLabel(alert.status || 'UNKNOWN')}
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
                      <Link
                        href={`/dashboard/fraud-alerts/${alert.id}`}
                        className="btn-secondary"
                        style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', padding: '0.45rem 0.8rem' }}
                      >
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
