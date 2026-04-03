'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useEffect, useState } from 'react';
import {
  getFraudAlertDetail,
  lockFraudAlertCard,
  markFraudAlertFalsePositive,
  resolveFraudAlert,
} from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faArrowLeft,
  faCircleCheck,
  faLock,
  faShieldHalved,
  faTriangleExclamation,
} from '@fortawesome/free-solid-svg-icons';
import AppModal from '@/components/AppModal';

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

function renderStateChange(
  label: string,
  before: string | null | undefined,
  after: string | null | undefined,
  badgeClass?: string,
) {
  if (!after) return null;
  const normalizedBefore = before || null;
  const normalizedAfter = after || null;

  return (
    <span className={`badge ${badgeClass || 'badge-pending'}`}>
      {normalizedBefore && normalizedBefore !== normalizedAfter
        ? `${label}: ${formatResponseLabel(normalizedBefore)} -> ${formatResponseLabel(normalizedAfter)}`
        : `${label} status at event: ${formatResponseLabel(normalizedAfter)}`}
    </span>
  );
}

export default function FraudAlertDetailPage() {
  const params = useParams<{ id: string }>();
  const alertId = params?.id;
  const [alertDetail, setAlertDetail] = useState<any | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [noticeModal, setNoticeModal] = useState<{ title: string; message: string } | null>(null);
  const [actionModal, setActionModal] = useState<{ action: 'lock' | 'resolve' | 'false-positive'; title: string; note: string } | null>(null);

  useEffect(() => {
    if (!alertId) return;
    loadDetail();
  }, [alertId]);

  async function loadDetail() {
    if (!alertId) return;
    setLoading(true);
    try {
      const data = await getFraudAlertDetail(alertId);
      setAlertDetail(data);
    } catch (err) {
      console.error('Failed to load fraud alert detail', err);
      setAlertDetail(null);
    } finally {
      setLoading(false);
    }
  }

  async function submitAction(action: 'lock' | 'resolve' | 'false-positive', note: string) {
    if (!alertId) return;
    setSubmitting(true);
    try {
      if (action === 'lock') {
        await lockFraudAlertCard(alertId, note);
      } else if (action === 'resolve') {
        await resolveFraudAlert(alertId, note);
      } else {
        await markFraudAlertFalsePositive(alertId, note);
      }
      await loadDetail();
      setActionModal(null);
    } catch (err: any) {
      setNoticeModal({
        title: 'Không thể thực hiện thao tác',
        message: err.message || 'Không thể thực hiện thao tác',
      });
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <div className="card">Đang tải fraud alert...</div>;
  }

  if (!alertDetail) {
    return (
      <div className="card">
        <p style={{ color: 'var(--text-secondary)' }}>Không tìm thấy fraud alert.</p>
        <Link href="/dashboard/fraud-alerts" className="btn-secondary" style={{ marginTop: '1rem', display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
          <FontAwesomeIcon icon={faArrowLeft} />
          Quay lại danh sách
        </Link>
      </div>
    );
  }

  return (
    <div className="animate-fade-in" style={{ display: 'grid', gap: '1rem' }}>
      <AppModal
        open={!!noticeModal}
        title={noticeModal?.title || ''}
        onClose={() => setNoticeModal(null)}
        footer={<button className="btn-primary" onClick={() => setNoticeModal(null)}>Đã hiểu</button>}
      >
        <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7 }}>{noticeModal?.message}</p>
      </AppModal>
      <AppModal
        open={!!actionModal}
        title={actionModal?.title || ''}
        onClose={() => !submitting && setActionModal(null)}
        footer={
          <>
            <button className="btn-secondary" disabled={submitting} onClick={() => setActionModal(null)}>Hủy</button>
            <button
              className="btn-primary"
              disabled={submitting}
              onClick={() => actionModal && void submitAction(actionModal.action, actionModal.note)}
            >
              {submitting ? 'Đang xử lý...' : 'Xác nhận'}
            </button>
          </>
        }
      >
        <div style={{ display: 'grid', gap: '0.75rem' }}>
          <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7 }}>
            Bạn có thể thêm ghi chú vận hành cho hành động này.
          </p>
          <textarea
            className="input"
            rows={4}
            value={actionModal?.note || ''}
            onChange={(e) => setActionModal((prev) => prev ? ({ ...prev, note: e.target.value }) : prev)}
            placeholder="Nhập ghi chú xử lý..."
            style={{ resize: 'vertical' }}
          />
        </div>
      </AppModal>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem', flexWrap: 'wrap' }}>
        <div>
          <div style={{ marginBottom: '0.75rem' }}>
            <Link href="/dashboard/fraud-alerts" className="btn-secondary" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
              <FontAwesomeIcon icon={faArrowLeft} />
              Quay lại danh sách
            </Link>
          </div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
            <FontAwesomeIcon icon={faShieldHalved} style={{ marginRight: '0.5rem' }} />
            Fraud Alert #{alertDetail.id}
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
            Xem chi tiết case, phản hồi khách hàng và thao tác khóa thẻ hoặc đóng case.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <button
            className="btn-secondary"
            disabled={submitting || alertDetail.status === 'CARD_LOCKED'}
            onClick={() => setActionModal({ action: 'lock', title: 'Khóa thẻ từ fraud alert', note: '' })}
          >
            <FontAwesomeIcon icon={faLock} /> Khóa thẻ
          </button>
          <button
            className="btn-secondary"
            disabled={submitting || alertDetail.status === 'RESOLVED'}
            onClick={() => setActionModal({ action: 'resolve', title: 'Đóng case fraud alert', note: '' })}
          >
            <FontAwesomeIcon icon={faCircleCheck} /> Resolve
          </button>
          <button
            className="btn-secondary"
            disabled={submitting || alertDetail.status === 'FALSE_POSITIVE'}
            onClick={() => setActionModal({ action: 'false-positive', title: 'Đánh dấu false positive', note: '' })}
          >
            <FontAwesomeIcon icon={faTriangleExclamation} /> False positive
          </button>
        </div>
      </div>

      <div className="card" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
        <div>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginBottom: '0.35rem' }}>Trạng thái</div>
          <span className={`badge ${STATUS_BADGE[alertDetail.status] || 'badge-pending'}`}>{alertDetail.status}</span>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginTop: '0.5rem' }}>
            customer: {formatResponseLabel(alertDetail.customerResponse)}
          </div>
        </div>
        <div>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginBottom: '0.35rem' }}>Mức rủi ro</div>
          <span className={`badge ${RISK_BADGE[alertDetail.riskLevel] || 'badge-pending'}`}>{alertDetail.riskLevel || 'UNKNOWN'}</span>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginTop: '0.5rem' }}>
            score: {alertDetail.riskScore != null ? Number(alertDetail.riskScore).toFixed(2) : '—'}
          </div>
        </div>
        <div>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginBottom: '0.35rem' }}>Notification</div>
          <span className={`badge ${alertDetail.notificationSent ? 'badge-active' : 'badge-pending'}`}>
            {alertDetail.notificationSent ? 'PUSH_SENT' : 'PENDING'}
          </span>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginTop: '0.5rem' }}>
            notifiedAt: {alertDetail.notifiedAt ? new Date(alertDetail.notifiedAt).toLocaleString('vi-VN') : '—'}
          </div>
        </div>
        <div>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginBottom: '0.35rem' }}>Số tiền</div>
          <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--warning)' }}>
            {Number(alertDetail.amount || 0).toLocaleString('en-US')} {alertDetail.currency || 'USD'}
          </div>
        </div>
        <div>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginBottom: '0.35rem' }}>Trạng thái thẻ hiện tại</div>
          <span className={`badge ${STATUS_BADGE[alertDetail.currentCardStatus] || 'badge-pending'}`}>
            {formatResponseLabel(alertDetail.currentCardStatus)}
          </span>
        </div>
      </div>

      <div className="card" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1rem' }}>
        <div>
          <h3 style={{ marginBottom: '0.75rem' }}>Thông tin giao dịch</h3>
          <div style={{ display: 'grid', gap: '0.5rem' }}>
            <div><strong>Payment ID:</strong> {alertDetail.paymentId || '—'}</div>
            <div><strong>Thời gian:</strong> {alertDetail.transactionTime ? new Date(alertDetail.transactionTime).toLocaleString('vi-VN') : '—'}</div>
            <div><strong>Merchant:</strong> {alertDetail.merchantName || '—'}</div>
            <div><strong>Merchant ID:</strong> {alertDetail.merchantId || '—'}</div>
            <div><strong>Dự đoán:</strong> {alertDetail.fraudPrediction || '—'}</div>
            <div><strong>Lý do:</strong> {alertDetail.fraudReason || '—'}</div>
          </div>
        </div>
        <div>
          <h3 style={{ marginBottom: '0.75rem' }}>Thông tin thẻ / tài khoản</h3>
          <div style={{ display: 'grid', gap: '0.5rem' }}>
            <div><strong>Masked PAN:</strong> {alertDetail.maskedPan || '—'}</div>
            <div><strong>Card ID:</strong> {alertDetail.cardId || '—'}</div>
            <div><strong>Account ID:</strong> {alertDetail.accountId || '—'}</div>
            <div><strong>Account Type:</strong> {alertDetail.accountType || '—'}</div>
            <div><strong>Card locked at:</strong> {alertDetail.cardLockedAt ? new Date(alertDetail.cardLockedAt).toLocaleString('vi-VN') : '—'}</div>
          </div>
        </div>
        <div>
          <h3 style={{ marginBottom: '0.75rem' }}>Theo dõi xử lý</h3>
          <div style={{ display: 'grid', gap: '0.5rem' }}>
            <div><strong>Customer responded at:</strong> {alertDetail.customerRespondedAt ? new Date(alertDetail.customerRespondedAt).toLocaleString('vi-VN') : '—'}</div>
            <div><strong>Resolved at:</strong> {alertDetail.resolvedAt ? new Date(alertDetail.resolvedAt).toLocaleString('vi-VN') : '—'}</div>
            <div><strong>Resolved by:</strong> {alertDetail.resolvedBy || '—'}</div>
            <div><strong>Admin note:</strong> {alertDetail.adminNote || '—'}</div>
          </div>
        </div>
      </div>

      <div className="card">
        <h3 style={{ marginBottom: '1rem' }}>Timeline</h3>
        {!Array.isArray(alertDetail.events) || alertDetail.events.length === 0 ? (
          <div style={{ color: 'var(--text-secondary)' }}>Chưa có audit event nào.</div>
        ) : (
          <div style={{ display: 'grid', gap: '0.85rem' }}>
            {alertDetail.events.map((event: any) => (
              <div
                key={event.id}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '220px 1fr',
                  gap: '1rem',
                  paddingBottom: '0.85rem',
                  borderBottom: '1px solid rgba(255,255,255,0.06)',
                }}
              >
                <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                  <div>{event.createdAt ? new Date(event.createdAt).toLocaleString('vi-VN') : '—'}</div>
                  <div style={{ marginTop: '0.25rem' }}>actor: {event.actor || 'system'}</div>
                </div>
                <div>
                  <div style={{ fontWeight: 700, marginBottom: '0.25rem' }}>{formatResponseLabel(event.eventType)}</div>
                  <div style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>{event.note || '—'}</div>
                  <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginTop: '0.5rem' }}>
                    {renderStateChange(
                      'Case',
                      event.statusBefore,
                      event.statusAfter,
                      STATUS_BADGE[event.statusAfter] || 'badge-pending',
                    )}
                    {renderStateChange(
                      'Customer',
                      event.customerResponseBefore,
                      event.customerResponseAfter,
                      'badge-pending',
                    )}
                    {renderStateChange(
                      'Card',
                      event.cardStatusBefore,
                      event.cardStatusAfter,
                      STATUS_BADGE[event.cardStatusAfter] || 'badge-pending',
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
