'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { getFraudSettings, updateFraudSettings } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowLeft, faGear, faShieldHalved } from '@fortawesome/free-solid-svg-icons';

type FraudSettingsForm = {
  highRiskEmailActionExpirationMinutes: number;
  mediumRiskEmailActionExpirationMinutes: number;
  highRiskNoResponseTimeoutMinutes: number;
  highRiskNoResponseAction: string;
  mediumRiskNoResponseTimeoutMinutes: number;
  mediumRiskNoResponseAction: string;
};

const DEFAULT_FORM: FraudSettingsForm = {
  highRiskEmailActionExpirationMinutes: 15,
  mediumRiskEmailActionExpirationMinutes: 30,
  highRiskNoResponseTimeoutMinutes: 15,
  highRiskNoResponseAction: 'LOCK_CARD',
  mediumRiskNoResponseTimeoutMinutes: 60,
  mediumRiskNoResponseAction: 'NONE',
};

export default function FraudAlertSettingsPage() {
  const [form, setForm] = useState<FraudSettingsForm>(DEFAULT_FORM);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void loadSettings();
  }, []);

  async function loadSettings() {
    setLoading(true);
    setError(null);
    try {
      const data = await getFraudSettings();
      setForm({
        highRiskEmailActionExpirationMinutes: Number(data?.highRiskEmailActionExpirationMinutes ?? 15),
        mediumRiskEmailActionExpirationMinutes: Number(data?.mediumRiskEmailActionExpirationMinutes ?? 30),
        highRiskNoResponseTimeoutMinutes: Number(data?.highRiskNoResponseTimeoutMinutes ?? 15),
        highRiskNoResponseAction: String(data?.highRiskNoResponseAction ?? 'LOCK_CARD'),
        mediumRiskNoResponseTimeoutMinutes: Number(data?.mediumRiskNoResponseTimeoutMinutes ?? 60),
        mediumRiskNoResponseAction: String(data?.mediumRiskNoResponseAction ?? 'NONE'),
      });
    } catch (err: any) {
      setError(err?.message || 'Không thể tải fraud settings');
    } finally {
      setLoading(false);
    }
  }

  async function handleSave() {
    setSaving(true);
    setMessage(null);
    setError(null);
    try {
      const data = await updateFraudSettings(form);
      setForm({
        highRiskEmailActionExpirationMinutes: Number(data?.highRiskEmailActionExpirationMinutes ?? form.highRiskEmailActionExpirationMinutes),
        mediumRiskEmailActionExpirationMinutes: Number(data?.mediumRiskEmailActionExpirationMinutes ?? form.mediumRiskEmailActionExpirationMinutes),
        highRiskNoResponseTimeoutMinutes: Number(data?.highRiskNoResponseTimeoutMinutes ?? form.highRiskNoResponseTimeoutMinutes),
        highRiskNoResponseAction: String(data?.highRiskNoResponseAction ?? form.highRiskNoResponseAction),
        mediumRiskNoResponseTimeoutMinutes: Number(data?.mediumRiskNoResponseTimeoutMinutes ?? form.mediumRiskNoResponseTimeoutMinutes),
        mediumRiskNoResponseAction: String(data?.mediumRiskNoResponseAction ?? form.mediumRiskNoResponseAction),
      });
      setMessage('Đã lưu fraud settings');
    } catch (err: any) {
      setError(err?.message || 'Không thể lưu fraud settings');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="animate-fade-in" style={{ display: 'grid', gap: '1rem' }}>
      <div>
        <div style={{ marginBottom: '0.75rem' }}>
          <Link href="/dashboard/fraud-alerts" className="btn-secondary" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
            <FontAwesomeIcon icon={faArrowLeft} />
            Quay lại fraud alerts
          </Link>
        </div>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
          <FontAwesomeIcon icon={faGear} style={{ marginRight: '0.5rem' }} />
          Fraud Settings
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
          Điều chỉnh thời hạn link email và policy xử lý fraud alert khi khách hàng không phản hồi.
        </p>
      </div>

      <div className="card" style={{ display: 'grid', gap: '1rem', maxWidth: '920px' }}>
        {loading ? (
          <div style={{ color: 'var(--text-secondary)' }}>Đang tải fraud settings...</div>
        ) : (
          <>
            <div className="card" style={{ display: 'grid', gap: '1rem', padding: '1.25rem' }}>
              <div>
                <div style={{ fontWeight: 700, marginBottom: '0.25rem' }}>Email Link Settings</div>
                <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                  Cấu hình thời gian hết hạn của link xác nhận / từ chối gửi qua email theo từng mức rủi ro.
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
                <div>
                  <label className="label">HIGH risk email expiry (phút)</label>
                  <input
                    type="number"
                    min={1}
                    max={1440}
                    className="input"
                    value={form.highRiskEmailActionExpirationMinutes}
                    onChange={(e) => setForm((prev) => ({ ...prev, highRiskEmailActionExpirationMinutes: Number(e.target.value) }))}
                  />
                  <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginTop: '0.35rem' }}>
                    Áp dụng cho email action link của alert `HIGH`.
                  </div>
                </div>

                <div>
                  <label className="label">MEDIUM risk email expiry (phút)</label>
                  <input
                    type="number"
                    min={1}
                    max={1440}
                    className="input"
                    value={form.mediumRiskEmailActionExpirationMinutes}
                    onChange={(e) => setForm((prev) => ({ ...prev, mediumRiskEmailActionExpirationMinutes: Number(e.target.value) }))}
                  />
                  <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginTop: '0.35rem' }}>
                    Áp dụng cho email action link của alert `MEDIUM`.
                  </div>
                </div>
              </div>
            </div>

            <div className="card" style={{ display: 'grid', gap: '1rem', padding: '1.25rem' }}>
              <div>
                <div style={{ fontWeight: 700, marginBottom: '0.25rem' }}>No-Response Policy</div>
                <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                  Cấu hình hệ thống sẽ chờ bao lâu và tự làm gì nếu khách hàng không phản hồi fraud alert.
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
                <div>
                  <label className="label">HIGH risk timeout (phút)</label>
                  <input
                    type="number"
                    min={1}
                    max={10080}
                    className="input"
                    value={form.highRiskNoResponseTimeoutMinutes}
                    onChange={(e) => setForm((prev) => ({ ...prev, highRiskNoResponseTimeoutMinutes: Number(e.target.value) }))}
                  />
                  <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginTop: '0.35rem' }}>
                    Tính từ `notifiedAt`, fallback về `createdAt` nếu chưa có notification timestamp.
                  </div>
                </div>

                <div>
                  <label className="label">HIGH risk action</label>
                  <select
                    className="input"
                    value={form.highRiskNoResponseAction}
                    onChange={(e) => setForm((prev) => ({ ...prev, highRiskNoResponseAction: e.target.value }))}
                  >
                    <option value="NONE">Không tự xử lý</option>
                    <option value="LOCK_CARD">Tự khóa thẻ</option>
                    <option value="RESOLVE_CASE">Tự chuyển sang RESOLVED</option>
                  </select>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
                <div>
                  <label className="label">MEDIUM risk timeout (phút)</label>
                  <input
                    type="number"
                    min={1}
                    max={10080}
                    className="input"
                    value={form.mediumRiskNoResponseTimeoutMinutes}
                    onChange={(e) => setForm((prev) => ({ ...prev, mediumRiskNoResponseTimeoutMinutes: Number(e.target.value) }))}
                  />
                  <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginTop: '0.35rem' }}>
                    Áp dụng cho alert `MEDIUM`.
                  </div>
                </div>

                <div>
                  <label className="label">MEDIUM risk action</label>
                  <select
                    className="input"
                    value={form.mediumRiskNoResponseAction}
                    onChange={(e) => setForm((prev) => ({ ...prev, mediumRiskNoResponseAction: e.target.value }))}
                  >
                    <option value="NONE">Không tự xử lý</option>
                    <option value="LOCK_CARD">Tự khóa thẻ</option>
                    <option value="RESOLVE_CASE">Tự chuyển sang RESOLVED</option>
                  </select>
                </div>
              </div>

              <div style={{ color: 'var(--text-secondary)', fontSize: '0.8rem' }}>
                Policy chỉ áp dụng cho alert đang `OPEN` hoặc `WAITING_CUSTOMER_CONFIRMATION` và `customerResponse = NO_RESPONSE`.
              </div>
            </div>

            <div className="card" style={{ padding: '1rem', background: 'rgba(99,102,241,0.08)', borderColor: 'rgba(99,102,241,0.2)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem', fontWeight: 700 }}>
                <FontAwesomeIcon icon={faShieldHalved} />
                Policy hiện tại sẽ làm gì
              </div>
              <div style={{ color: 'var(--text-secondary)', lineHeight: 1.7 }}>
                Link email cho alert <strong>HIGH</strong> có hiệu lực trong <strong>{form.highRiskEmailActionExpirationMinutes} phút</strong>. Link email cho alert <strong>MEDIUM</strong> có hiệu lực trong <strong>{form.mediumRiskEmailActionExpirationMinutes} phút</strong>. Với alert <strong>HIGH</strong>, hệ thống chờ{' '}
                <strong>{form.highRiskNoResponseTimeoutMinutes} phút</strong> rồi{' '}
                <strong>
                  {form.highRiskNoResponseAction === 'LOCK_CARD'
                    ? 'tự khóa thẻ'
                    : form.highRiskNoResponseAction === 'RESOLVE_CASE'
                      ? 'tự chuyển sang RESOLVED'
                      : 'không tự xử lý'}
                </strong>.
                {' '}Với alert <strong>MEDIUM</strong>, hệ thống chờ <strong>{form.mediumRiskNoResponseTimeoutMinutes} phút</strong> rồi{' '}
                <strong>
                  {form.mediumRiskNoResponseAction === 'LOCK_CARD'
                    ? 'tự khóa thẻ'
                    : form.mediumRiskNoResponseAction === 'RESOLVE_CASE'
                      ? 'tự chuyển sang RESOLVED'
                      : 'không tự xử lý'}
                </strong>.
              </div>
            </div>

            {message ? <div style={{ color: '#34d399', fontSize: '0.9rem' }}>{message}</div> : null}
            {error ? <div style={{ color: '#f87171', fontSize: '0.9rem' }}>{error}</div> : null}

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button className="btn-primary" onClick={() => void handleSave()} disabled={saving}>
                {saving ? 'Đang lưu...' : 'Lưu fraud settings'}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
