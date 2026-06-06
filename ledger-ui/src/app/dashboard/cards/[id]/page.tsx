'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import {
  blockCard,
  cancelCard,
  changeCardStatus,
  getCardDetail,
  renewCard,
  replaceCard,
  unblockCard,
} from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
  faArrowLeft,
  faBan,
  faCheckCircle,
  faCreditCard,
  faIdCard,
  faLock,
  faRotate,
  faSave,
  faTrash,
} from '@fortawesome/free-solid-svg-icons';
import CardNetworkLogo from '@/components/CardNetworkLogo';
import AppModal from '@/components/AppModal';

const STATUS_BADGE: Record<string, string> = {
  ACTIVE: 'badge-active',
  LOCKED: 'badge-locked',
  EXPIRED: 'badge-pending',
  CANCELLED: 'badge-locked',
};

const NETWORK_OPTIONS = ['VISA', 'MASTERCARD', 'AMEX', 'JCB', 'DISCOVER', 'NAPAS', 'UNKNOWN'];

function buildFutureDate(years = 4) {
  const next = new Date();
  next.setFullYear(next.getFullYear() + years);
  next.setMonth(11);
  next.setDate(31);
  return next.toISOString().slice(0, 10);
}

function generateRandomDigits(length: number) {
  return Array.from({ length }, () => Math.floor(Math.random() * 10)).join('');
}

export default function CardDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const cardId = params?.id;
  const [card, setCard] = useState<any | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [renewDate, setRenewDate] = useState(buildFutureDate());
  const [manualStatus, setManualStatus] = useState('LOCKED');
  const [noticeModal, setNoticeModal] = useState<{ title: string; message: string; onClose?: () => void } | null>(null);
  const [confirmCancelOpen, setConfirmCancelOpen] = useState(false);
  const [replacement, setReplacement] = useState({
    pan: '',
    cvv: '',
    expirationDate: buildFutureDate(),
    cardholderName: '',
    network: 'UNKNOWN',
  });

  useEffect(() => {
    if (!cardId) return;
    loadCard();
  }, [cardId]);

  async function loadCard() {
    setLoading(true);
    setError('');
    try {
      const data = await getCardDetail(cardId as string);
      setCard(data);
      setManualStatus(data?.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE');
      setRenewDate(data?.expirationDate || buildFutureDate());
      setReplacement({
        pan: '',
        cvv: '',
        expirationDate: data?.expirationDate || buildFutureDate(),
        cardholderName: data?.cardholderName || '',
        network: data?.network || 'UNKNOWN',
      });
    } catch (err: any) {
      setError(err.message || 'Không thể tải chi tiết thẻ');
    } finally {
      setLoading(false);
    }
  }

  async function runAction(action: () => Promise<any>, successMessage: string, redirect = false) {
    setSubmitting(true);
    setError('');
    try {
      await action();
      setNoticeModal({
        title: 'Thao tác thành công',
        message: successMessage,
        onClose: redirect ? () => router.push('/dashboard/cards') : () => { void loadCard(); },
      });
    } catch (err: any) {
      setError(err.message || 'Không thể thực hiện thao tác');
    } finally {
      setSubmitting(false);
    }
  }

  function fillRandomReplacement() {
    setReplacement((prev) => ({
      ...prev,
      pan: generateRandomDigits(16),
      cvv: generateRandomDigits(3),
    }));
  }

  if (loading) {
    return <div className="card">Đang tải chi tiết thẻ...</div>;
  }

  if (!card) {
    return <div className="card">Không tìm thấy thẻ.</div>;
  }

  function closeNoticeModal() {
    const next = noticeModal?.onClose;
    setNoticeModal(null);
    next?.();
  }

  const status = card.status || 'UNKNOWN';
  const canBlock = status === 'ACTIVE';
  const canUnblock = status === 'LOCKED' || status === 'EXPIRED';
  const canCancel = status !== 'CANCELLED';
  const canRenew = status !== 'CANCELLED';
  const canReplace = status !== 'CANCELLED';

  return (
    <div className="animate-fade-in">
      <AppModal
        open={!!noticeModal}
        title={noticeModal?.title || ''}
        onClose={closeNoticeModal}
        footer={<button className="btn-primary" onClick={closeNoticeModal}>Đã hiểu</button>}
      >
        <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7 }}>{noticeModal?.message}</p>
      </AppModal>
      <AppModal
        open={confirmCancelOpen}
        title="Xác nhận hủy thẻ"
        onClose={() => setConfirmCancelOpen(false)}
        footer={
          <>
            <button className="btn-secondary" onClick={() => setConfirmCancelOpen(false)}>Giữ lại</button>
            <button className="btn-danger" onClick={() => {
              setConfirmCancelOpen(false);
              void runAction(() => cancelCard(card.id), 'Đã hủy thẻ');
            }}>Hủy thẻ</button>
          </>
        }
      >
        <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7 }}>
          Thẻ này sẽ bị hủy và không nên hoàn tác. Bạn có chắc muốn tiếp tục?
        </p>
      </AppModal>
      <div style={{ marginBottom: '1.5rem' }}>
        <Link href="/dashboard/cards" style={{ color: 'var(--text-secondary)', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', fontSize: '0.875rem' }}>
          <FontAwesomeIcon icon={faArrowLeft} />
          Quay lại danh sách thẻ
        </Link>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
          <FontAwesomeIcon icon={faIdCard} style={{ marginRight: '0.5rem' }} />
          Chi tiết thẻ #{card.id}
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
          Quản lý vòng đời thẻ từ CMS.
        </p>
      </div>

      {error && <div className="alert-error" style={{ marginBottom: '1rem' }}>{error}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(320px, 1.2fr) minmax(380px, 1fr)', gap: '1rem' }}>
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
            <div>
              <div style={{ fontSize: '1.125rem', fontWeight: 700 }}>{card.maskedPan}</div>
              <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem', marginTop: '0.25rem' }}>{card.cardholderName || '—'}</div>
            </div>
            <span className={`badge ${STATUS_BADGE[status] || 'badge-pending'}`}>{status}</span>
          </div>

          <div className="table-container">
            <table>
              <tbody>
                <tr><th>ID</th><td>{card.id}</td></tr>
                <tr><th>Loại thẻ</th><td>{card.cardType || '—'}</td></tr>
                <tr><th>Network</th><td><CardNetworkLogo network={card.network} width={64} height={24} /></td></tr>
                <tr><th>Số tài khoản liên kết</th><td>{card.linkedAccountNumber || card.accountId || '—'}</td></tr>
                <tr><th>Ngày hết hạn</th><td>{card.expirationDate || '—'}</td></tr>
                <tr><th>Hạn mức</th><td>{card.cardType === 'CREDIT' ? `${Number(card.creditLimit || 0).toLocaleString('en-US')} USD` : 'N/A'}</td></tr>
                <tr><th>Dư nợ</th><td>{card.cardType === 'CREDIT' ? `${Number(card.outstandingBalance || 0).toLocaleString('en-US')} USD` : 'N/A'}</td></tr>
              </tbody>
            </table>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: '0.75rem', marginTop: '1rem' }}>
            <button className="btn-secondary" disabled={!canBlock || submitting} onClick={() => runAction(() => blockCard(card.id), 'Đã khóa thẻ')}>
              <FontAwesomeIcon icon={faLock} /> Khóa thẻ
            </button>
            <button className="btn-secondary" disabled={!canUnblock || submitting} onClick={() => runAction(() => unblockCard(card.id), 'Đã mở khóa thẻ')}>
              <FontAwesomeIcon icon={faCheckCircle} /> Mở khóa
            </button>
            <button className="btn-secondary" disabled={!canCancel || submitting} onClick={() => setConfirmCancelOpen(true)}>
              <FontAwesomeIcon icon={faBan} /> Hủy thẻ
            </button>
            <button className="btn-secondary" disabled={submitting} onClick={() => runAction(() => changeCardStatus(card.id, manualStatus), `Đã chuyển trạng thái sang ${manualStatus}`)}>
              <FontAwesomeIcon icon={faSave} /> Set {manualStatus}
            </button>
          </div>
        </div>

        <div style={{ display: 'grid', gap: '1rem' }}>
          <div className="card">
            <h2 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '0.75rem' }}>
              <FontAwesomeIcon icon={faRotate} style={{ marginRight: '0.5rem' }} />
              Gia hạn thẻ
            </h2>
            <div style={{ display: 'grid', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Ngày hết hạn mới</label>
                <input type="date" className="input" value={renewDate} onChange={(e) => setRenewDate(e.target.value)} />
              </div>
              <button className="btn-primary" disabled={!canRenew || submitting} onClick={() => runAction(() => renewCard(card.id, renewDate), 'Đã gia hạn thẻ')}>
                <FontAwesomeIcon icon={faRotate} /> Gia hạn
              </button>
            </div>
          </div>

          <div className="card">
            <h2 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '0.75rem' }}>
              <FontAwesomeIcon icon={faCreditCard} style={{ marginRight: '0.5rem' }} />
              Cấp lại thẻ
            </h2>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div style={{ gridColumn: '1 / -1' }}>
                <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>PAN mới</label>
                <input className="input" value={replacement.pan} onChange={(e) => setReplacement((prev) => ({ ...prev, pan: e.target.value }))} placeholder="4111111111112222" />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>CVV mới</label>
                <input className="input" value={replacement.cvv} onChange={(e) => setReplacement((prev) => ({ ...prev, cvv: e.target.value }))} placeholder="123" />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Ngày hết hạn mới</label>
                <input type="date" className="input" value={replacement.expirationDate} onChange={(e) => setReplacement((prev) => ({ ...prev, expirationDate: e.target.value }))} />
              </div>
              <div style={{ gridColumn: '1 / -1' }}>
                <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Tên chủ thẻ</label>
                <input className="input" value={replacement.cardholderName} onChange={(e) => setReplacement((prev) => ({ ...prev, cardholderName: e.target.value.toUpperCase() }))} placeholder="NGUYEN VAN A" />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Network</label>
                <select className="input" value={replacement.network} onChange={(e) => setReplacement((prev) => ({ ...prev, network: e.target.value }))}>
                  {NETWORK_OPTIONS.map((option) => (
                    <option key={option} value={option}>{option}</option>
                  ))}
                </select>
              </div>
              <div style={{ display: 'flex', alignItems: 'end' }}>
                <button type="button" className="btn-secondary" style={{ width: '100%' }} onClick={fillRandomReplacement}>
                  Sinh PAN/CVV
                </button>
              </div>
            </div>
            <button
              className="btn-primary"
              style={{ marginTop: '1rem', width: '100%' }}
              disabled={!canReplace || submitting}
              onClick={() => runAction(
                () => replaceCard(card.id, replacement),
                'Đã cấp lại thẻ, thẻ cũ đã bị hủy',
                true,
              )}
            >
              <FontAwesomeIcon icon={faTrash} /> Cấp lại thẻ
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
