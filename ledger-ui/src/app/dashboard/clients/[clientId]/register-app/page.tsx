'use client';
import { useState, use } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { registerCustomer } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowLeft, faMobileAlt } from '@fortawesome/free-solid-svg-icons';
import Link from 'next/link';

export default function RegisterAppAccountPage({ params }: { params: Promise<{ clientId: string }> }) {
    const { clientId } = use(params);
    const router = useRouter();
    const searchParams = useSearchParams();
    const clientName = searchParams.get('clientName') || 'Không rõ';

    const [password, setPassword] = useState('');
    const [isRegistering, setIsRegistering] = useState(false);

    const handleRegister = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!password || password.length < 6) {
            alert('Mật khẩu đăng nhập phải có ít nhất 6 ký tự.');
            return;
        }

        setIsRegistering(true);
        try {
            const res = await registerCustomer(clientId, password);
            alert(`Tạo tài khoản Mobile App thành công! Tên đăng nhập là số điện thoại của khách hàng: ${res.username || ''}`);
            router.push(`/dashboard/clients/${clientId}`);
        } catch (error: any) {
            alert(error.message || 'Lỗi khi tạo tài khoản App');
        } finally {
            setIsRegistering(false);
        }
    };

    return (
        <div className="animate-fade-in" style={{ maxWidth: '600px', margin: '0 auto' }}>
            <div style={{ marginBottom: '2rem' }}>
                <Link href={`/dashboard/clients/${clientId}`} style={{ color: 'var(--text-secondary)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', fontSize: '0.875rem' }}>
                    <FontAwesomeIcon icon={faArrowLeft} /> Quay lại chi tiết khách hàng
                </Link>
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <FontAwesomeIcon icon={faMobileAlt} style={{ color: 'var(--accent)' }} />
                    Tạo tài khoản Mobile App
                </h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                    Cấp tài khoản đăng nhập cho <strong>{clientName}</strong> ({clientId})
                </p>
            </div>

            <div className="card">
                <form onSubmit={handleRegister}>
                    <div style={{ marginBottom: '1.5rem', padding: '1rem', background: 'var(--bg-secondary)', borderRadius: '8px', border: '1px solid var(--border)' }}>
                        <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', display: 'flex', gap: '0.5rem', alignItems: 'flex-start' }}>
                            <span style={{ color: 'var(--accent)' }}>ℹ️</span>
                            Hệ thống sẽ tự động dùng số điện thoại của khách hàng làm <strong>Tên đăng nhập</strong>. Bạn chỉ cần khởi tạo mật khẩu mặc định.
                        </div>
                    </div>

                    <div style={{ marginBottom: '2rem' }}>
                        <label htmlFor="reg-password" style={{ display: 'block', marginBottom: '0.5rem', fontSize: '0.875rem', fontWeight: 500, color: 'var(--text-secondary)' }}>
                            Mật khẩu đăng nhập <span style={{ color: 'var(--danger)' }}>*</span>
                        </label>
                        <input
                            id="reg-password"
                            type="text"
                            className="input"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Nhập mật khẩu ít nhất 6 ký tự..."
                            disabled={isRegistering}
                        />
                    </div>

                    <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end', paddingTop: '1rem', borderTop: '1px solid var(--border)' }}>
                        <button
                            type="button"
                            className="btn-secondary"
                            onClick={() => router.push(`/dashboard/clients/${clientId}`)}
                            disabled={isRegistering}
                        >
                            Hủy
                        </button>
                        <button
                            type="submit"
                            className="btn-primary"
                            disabled={isRegistering}
                        >
                            {isRegistering ? 'Đang tạo...' : 'Xác nhận tạo tài khoản'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
