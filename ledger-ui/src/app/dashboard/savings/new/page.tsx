'use client';
import { useState, useEffect, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { createSavingsAccount, getClient } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faPiggyBank, faArrowLeft, faSave, faUser } from '@fortawesome/free-solid-svg-icons';

function NewSavingsForm() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const defaultClientId = searchParams.get('clientId') || '';

    const [loading, setLoading] = useState(false);
    const [clientName, setClientName] = useState('');
    const [formData, setFormData] = useState({
        clientId: defaultClientId,
        accountNumber: '',
        currency: 'VND',
    });

    useEffect(() => {
        if (formData.clientId && formData.clientId.length > 3) {
            getClient(formData.clientId).then(c => {
                if (c && c.fullName) setClientName(c.fullName);
            }).catch(() => setClientName(''));
        }
    }, [formData.clientId]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        try {
            await createSavingsAccount({
                clientId: formData.clientId,
                accountNumber: formData.accountNumber,
                currency: formData.currency,
            });
            alert('Mở tài khoản thanh toán thành công!');
            if (defaultClientId) {
                router.push(`/dashboard/clients/${defaultClientId}`);
            } else {
                router.push('/dashboard/savings');
            }
        } catch (err: any) {
            alert(err.message || 'Lỗi khi mở tài khoản');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="animate-fade-in">
            <div style={{ marginBottom: '2rem' }}>
                <button onClick={() => router.back()} style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', fontSize: '0.875rem' }}>
                    <FontAwesomeIcon icon={faArrowLeft} /> Quay lại
                </button>
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                    <FontAwesomeIcon icon={faPiggyBank} style={{ marginRight: '0.5rem' }} />
                    Mở Tài Khoản Thanh Toán
                </h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                    Tạo tài khoản Debit cho khách hàng
                </p>
            </div>

            <form onSubmit={handleSubmit} className="card" style={{ maxWidth: '600px' }}>
                <div style={{ display: 'grid', gap: '1.5rem', marginBottom: '2rem' }}>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.5rem', fontSize: '0.875rem', fontWeight: 500 }}>
                            Mã Khách Hàng (Client ID) *
                        </label>
                        <input
                            required
                            name="clientId"
                            value={formData.clientId}
                            onChange={e => setFormData({ ...formData, clientId: e.target.value })}
                            className="input"
                            placeholder="Nhập Client ID..."
                        />
                        {clientName && (
                            <div style={{ marginTop: '0.5rem', fontSize: '0.8125rem', color: 'var(--success)', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                                <FontAwesomeIcon icon={faUser} /> Tìm thấy khách hàng: <strong>{clientName}</strong>
                            </div>
                        )}
                    </div>

                    <div>
                        <label style={{ display: 'block', marginBottom: '0.5rem', fontSize: '0.875rem', fontWeight: 500 }}>
                            Số Tài Khoản (Bắt đầu bằng S) *
                        </label>
                        <input
                            required
                            name="accountNumber"
                            value={formData.accountNumber}
                            onChange={e => setFormData({ ...formData, accountNumber: e.target.value })}
                            className="input"
                            placeholder="S-0001"
                        />
                    </div>

                    <div>
                        <label style={{ display: 'block', marginBottom: '0.5rem', fontSize: '0.875rem', fontWeight: 500 }}>
                            Loại Tiền Tệ *
                        </label>
                        <select
                            required
                            name="currency"
                            value={formData.currency}
                            onChange={e => setFormData({ ...formData, currency: e.target.value })}
                            className="input"
                        >
                            <option value="VND">VND - Việt Nam Đồng</option>
                            <option value="USD">USD - Đô la Mỹ</option>
                            <option value="EUR">EUR - Đồng Euro</option>
                        </select>
                    </div>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
                    <button type="button" onClick={() => router.back()} className="btn-secondary">Hủy bỏ</button>
                    <button type="submit" disabled={loading} className="btn-primary" style={{ padding: '0.5rem 1.5rem' }}>
                        {loading ? 'Đang tạo...' : <><FontAwesomeIcon icon={faSave} /> Tạo Tài Khoản</>}
                    </button>
                </div>
            </form>
        </div>
    );
}

export default function NewSavingsPage() {
    return (
        <Suspense fallback={<div>Đang tải form...</div>}>
            <NewSavingsForm />
        </Suspense>
    );
}
