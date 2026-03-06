'use client';
import { useState, useEffect, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { createLoan, getClient } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCreditCard, faArrowLeft, faSave, faUser } from '@fortawesome/free-solid-svg-icons';
import Link from 'next/link';

function NewLoanForm() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const defaultClientId = searchParams.get('clientId') || '';

    const [loading, setLoading] = useState(false);
    const [clientName, setClientName] = useState('');
    const [formData, setFormData] = useState({
        clientId: defaultClientId,
        accountNumber: '',
        principal: '10000',
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
            await createLoan({
                clientId: formData.clientId,
                accountNumber: formData.accountNumber,
                principal: Number(formData.principal),
            });
            alert('Tạo tài khoản tín dụng thành công!');
            if (defaultClientId) {
                router.push(`/dashboard/clients/${defaultClientId}`);
            } else {
                router.push('/dashboard/loans');
            }
        } catch (err: any) {
            alert(err.message || 'Lỗi khi tạo tài khoản tín dụng');
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
                    <FontAwesomeIcon icon={faCreditCard} style={{ marginRight: '0.5rem' }} />
                    Mở Tài Khoản Tín Dụng Mới
                </h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                    Tạo tài khoản tín dụng (Loan Account) cho khách hàng
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
                            Mã tài khoản *
                        </label>
                        <input
                            required
                            name="accountNumber"
                            value={formData.accountNumber}
                            onChange={e => setFormData({ ...formData, accountNumber: e.target.value })}
                            className="input"
                            placeholder="Nhập mã tài khoản..."
                        />
                    </div>

                    <div>
                        <label style={{ display: 'block', marginBottom: '0.5rem', fontSize: '0.875rem', fontWeight: 500 }}>
                            Hạn Mức Tín Dụng (USD) *
                        </label>
                        <input
                            required
                            type="number"
                            name="principal"
                            value={formData.principal}
                            onChange={e => setFormData({ ...formData, principal: e.target.value })}
                            className="input"
                            placeholder="10000"
                        />
                    </div>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
                    <button type="button" onClick={() => router.back()} className="btn-secondary">Hủy bỏ</button>
                    <button type="submit" disabled={loading} className="btn-primary" style={{ padding: '0.5rem 1.5rem' }}>
                        {loading ? 'Đang tạo...' : <><FontAwesomeIcon icon={faSave} /> Tạo Tài Khoản Tín Dụng</>}
                    </button>
                </div>
            </form>
        </div>
    );
}

export default function NewLoanPage() {
    return (
        <Suspense fallback={<div>Đang tải form...</div>}>
            <NewLoanForm />
        </Suspense>
    );
}
