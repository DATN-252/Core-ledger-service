'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { login } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faBuildingColumns, faExclamationTriangle, faSpinner, faLock, faCrown, faBriefcase } from '@fortawesome/free-solid-svg-icons';
import ThemeToggle from '@/components/ThemeToggle';

export default function LoginPage() {
    const router = useRouter();
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    async function handleSubmit(e: React.FormEvent) {
        e.preventDefault();
        setLoading(true);
        setError('');
        try {
            await login(username, password);
            router.push('/dashboard');
        } catch {
            setError('Sai tên đăng nhập hoặc mật khẩu');
        } finally {
            setLoading(false);
        }
    }

    return (
        <div style={{
            minHeight: '100vh',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: 'linear-gradient(135deg, var(--bg-primary) 0%, var(--bg-secondary) 50%, var(--bg-primary) 100%)',
            padding: '1rem',
            position: 'relative',
        }}>
            {/* Theme Toggle Top Right */}
            <div style={{ position: 'absolute', top: '1.5rem', right: '1.5rem', zIndex: 10 }}>
                <ThemeToggle />
            </div>
            {/* Glow effects */}
            <div style={{
                position: 'fixed', top: '20%', left: '30%',
                width: '400px', height: '400px',
                background: 'radial-gradient(circle, rgba(99,102,241,0.12) 0%, transparent 70%)',
                pointerEvents: 'none',
            }} />

            <div className="animate-fade-in" style={{ width: '100%', maxWidth: '420px' }}>
                {/* Logo */}
                <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
                    <div style={{
                        width: '56px', height: '56px', borderRadius: '16px',
                        background: 'linear-gradient(135deg, #6366f1, #818cf8)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        margin: '0 auto 1rem',
                        boxShadow: '0 8px 32px rgba(99,102,241,0.4)',
                        fontSize: '1.5rem',
                        color: 'white',
                    }}>
                        <FontAwesomeIcon icon={faBuildingColumns} />
                    </div>
                    <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '0.25rem' }}>
                        BkBank Ledger
                    </h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                        Core Banking Management System
                    </p>
                </div>

                {/* Card */}
                <div className="card" style={{ padding: '2rem' }}>
                    <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1.5rem' }}>Đăng nhập</h2>

                    {error && (
                        <div style={{
                            background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)',
                            borderRadius: '8px', padding: '0.75rem 1rem', marginBottom: '1rem',
                            color: 'var(--danger)', fontSize: '0.875rem',
                        }}>
                            <FontAwesomeIcon icon={faExclamationTriangle} style={{ marginRight: '0.5rem' }} />
                            {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit}>
                        <div style={{ marginBottom: '1rem' }}>
                            <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 500, color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
                                Tên đăng nhập
                            </label>
                            <input
                                className="input"
                                type="text"
                                placeholder="admin, teller01..."
                                value={username}
                                onChange={e => setUsername(e.target.value)}
                                required
                            />
                        </div>

                        <div style={{ marginBottom: '1.5rem' }}>
                            <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 500, color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
                                Mật khẩu
                            </label>
                            <input
                                className="input"
                                type="password"
                                placeholder="••••••••"
                                value={password}
                                onChange={e => setPassword(e.target.value)}
                                required
                            />
                        </div>

                        <button className="btn-primary" type="submit" disabled={loading} style={{ width: '100%', justifyContent: 'center', gap: '0.5rem' }}>
                            {loading ? (
                                <><FontAwesomeIcon icon={faSpinner} spin /> Đang đăng nhập...</>
                            ) : (
                                <><FontAwesomeIcon icon={faLock} /> Đăng nhập</>
                            )}
                        </button>
                    </form>

                    <div style={{ marginTop: '1.5rem', padding: '1rem', background: 'var(--bg-secondary)', borderRadius: '8px', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                        <div style={{ fontWeight: 600, marginBottom: '0.5rem', color: 'var(--text-primary)' }}>Tài khoản mặc định:</div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                            <FontAwesomeIcon icon={faCrown} style={{ color: 'var(--warning)' }} /> admin / admin123 (ADMIN)
                        </div>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <FontAwesomeIcon icon={faBriefcase} style={{ color: 'var(--accent)' }} /> teller01 / teller123 (TELLER)
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
