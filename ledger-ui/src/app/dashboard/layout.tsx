'use client';
import { useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import Link from 'next/link';
import { isLoggedIn, getUser, logout } from '@/lib/api';

import '@fortawesome/fontawesome-svg-core/styles.css';
import { config } from '@fortawesome/fontawesome-svg-core';
config.autoAddCss = false;

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faChartLine, faCreditCard, faPiggyBank, faListUl, faUsers, faSignOutAlt } from '@fortawesome/free-solid-svg-icons';

const navItems = [
    { href: '/dashboard', icon: faChartLine, label: 'Dashboard' },
    { href: '/dashboard/clients', icon: faUsers, label: 'Khách hàng' },
    { href: '/dashboard/loans', icon: faCreditCard, label: 'Tài khoản tín dụng' },
    { href: '/dashboard/savings', icon: faPiggyBank, label: 'Tài khoản ghi nợ' },
    { href: '/dashboard/transactions', icon: faListUl, label: 'Giao dịch' },
];

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
    const router = useRouter();
    const pathname = usePathname();
    const [user, setUser] = useState<any>(null);

    useEffect(() => {
        if (!isLoggedIn()) { router.replace('/login'); return; }
        setUser(getUser());
    }, [router]);

    if (!user) return (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh' }}>
            <div style={{ color: 'var(--text-secondary)' }}>Đang tải...</div>
        </div>
    );

    return (
        <div style={{ display: 'flex', minHeight: '100vh' }}>
            {/* Sidebar */}
            <aside style={{
                width: '240px', flexShrink: 0,
                background: 'var(--bg-secondary)',
                borderRight: '1px solid var(--border)',
                display: 'flex', flexDirection: 'column',
                position: 'fixed', top: 0, left: 0, bottom: 0,
            }}>
                {/* Logo */}
                <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--border)' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <div style={{
                            width: '36px', height: '36px', borderRadius: '10px',
                            background: 'linear-gradient(135deg, #6366f1, #818cf8)',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            fontSize: '1rem', flexShrink: 0, color: 'white'
                        }}>
                            <FontAwesomeIcon icon={faPiggyBank} />
                        </div>
                        <div>
                            <div style={{ fontWeight: 700, fontSize: '0.9rem' }}>BkBank Ledger</div>
                            <div style={{ color: 'var(--text-secondary)', fontSize: '0.7rem' }}>Admin Portal</div>
                        </div>
                    </div>
                </div>

                {/* Nav */}
                <nav style={{ flex: 1, padding: '1rem 0.75rem', display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                    {navItems.map(item => {
                        const active = pathname === item.href || (item.href !== '/dashboard' && pathname.startsWith(item.href));
                        return (
                            <Link key={item.href} href={item.href} style={{
                                display: 'flex', alignItems: 'center', gap: '0.75rem',
                                padding: '0.625rem 0.875rem', borderRadius: '8px', textDecoration: 'none',
                                transition: 'all 0.15s',
                                background: active ? 'var(--accent-dim)' : 'transparent',
                                color: active ? 'var(--accent-hover)' : 'var(--text-secondary)',
                                fontWeight: active ? 600 : 400,
                                fontSize: '0.875rem',
                                borderLeft: active ? '2px solid var(--accent)' : '2px solid transparent',
                            }}>
                                <span style={{ width: '24px', textAlign: 'center' }}>
                                    <FontAwesomeIcon icon={item.icon} />
                                </span>
                                <span>{item.label}</span>
                            </Link>
                        );
                    })}
                </nav>

                {/* User info */}
                <div style={{ padding: '1rem 0.75rem', borderTop: '1px solid var(--border)' }}>
                    <div style={{
                        display: 'flex', alignItems: 'center', gap: '0.75rem',
                        padding: '0.75rem', background: 'var(--bg-card)', borderRadius: '8px',
                        marginBottom: '0.5rem',
                    }}>
                        <div style={{
                            width: '32px', height: '32px', borderRadius: '50%',
                            background: 'linear-gradient(135deg, #6366f1, #a78bfa)',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            fontSize: '0.875rem', flexShrink: 0,
                        }}>
                            {user.fullName?.[0] || '?'}
                        </div>
                        <div style={{ minWidth: 0 }}>
                            <div style={{ fontSize: '0.8125rem', fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                                {user.fullName || user.username}
                            </div>
                            <div style={{ fontSize: '0.7rem', color: 'var(--text-secondary)' }}>
                                {user.role}
                            </div>
                        </div>
                    </div>
                    <button className="btn-secondary" onClick={logout} style={{ width: '100%', justifyContent: 'center', fontSize: '0.8125rem', gap: '0.5rem' }}>
                        <FontAwesomeIcon icon={faSignOutAlt} /> Đăng xuất
                    </button>
                </div>
            </aside>

            {/* Main content */}
            <main style={{ flex: 1, marginLeft: '240px', padding: '2rem', minHeight: '100vh' }}>
                {children}
            </main>
        </div>
    );
}
