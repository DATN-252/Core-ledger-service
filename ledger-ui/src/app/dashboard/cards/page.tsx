'use client';
import { useEffect, useState } from 'react';
import { getCreditCards } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faIdCard, faCreditCard, faMoneyBillWave, faLock, faCheckCircle, faExclamationTriangle, faPlus } from '@fortawesome/free-solid-svg-icons';
import Link from 'next/link';

const STATUS_BADGE: Record<string, string> = {
    ACTIVE: 'badge-active',
    PENDING: 'badge-pending',
    LOCKED: 'badge-locked',
    EXPIRED: 'badge-locked'
};

const NETWORK_STYLES: Record<string, { bg: string; color: string; label: string }> = {
    VISA:       { bg: '#1a1f71', color: '#fff', label: 'VISA' },
    MASTERCARD: { bg: '#eb001b', color: '#fff', label: 'MASTERCARD' },
    MASTER:     { bg: '#eb001b', color: '#fff', label: 'MASTER' },
    AMEX:       { bg: '#007bc1', color: '#fff', label: 'AMEX' },
    JCB:        { bg: '#003087', color: '#fff', label: 'JCB' },
    DISCOVER:   { bg: '#f76f20', color: '#fff', label: 'DISCOVER' },
    NAPAS:      { bg: '#d62828', color: '#fff', label: 'NAPAS' },
    UNKNOWN:    { bg: '#444', color: '#aaa', label: 'UNKNOWN' },
};

function NetworkBadge({ network }: { network?: string }) {
    const key = (network || 'UNKNOWN').toUpperCase();
    const style = NETWORK_STYLES[key] || NETWORK_STYLES['UNKNOWN'];
    return (
        <span style={{
            display: 'inline-block',
            background: style.bg,
            color: style.color,
            fontSize: '0.7rem',
            fontWeight: 700,
            letterSpacing: '0.05em',
            borderRadius: '4px',
            padding: '2px 7px',
            fontFamily: 'monospace',
            textTransform: 'uppercase'
        }}>
            {style.label}
        </span>
    );
}

export default function CardsPage() {
    const [cards, setCards] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState('');

    useEffect(() => {
        loadCards();
    }, []);

    async function loadCards() {
        setLoading(true);
        try {
            const data = await getCreditCards();
            setCards(data || []);
        } catch (e: any) {
            console.error('Failed to load cards:', e);
            setCards([]);
        } finally {
            setLoading(false);
        }
    }

    const filtered = cards.filter(c =>
        !search ||
        c.cardholderName?.toLowerCase().includes(search.toLowerCase()) ||
        c.maskedPan?.includes(search) ||
        c.accountId?.toLowerCase().includes(search.toLowerCase())
    );

    return (
        <div className="animate-fade-in">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '2rem' }}>
                <div>
                    <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                        <FontAwesomeIcon icon={faIdCard} style={{ marginRight: '0.5rem' }} />
                        Quản lý Thẻ (CMS)
                    </h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                        Tích hợp từ hệ thống Card Management System
                    </p>
                </div>
                <Link href="/dashboard/cards/new" className="btn-primary" style={{ textDecoration: 'none' }}>
                    <FontAwesomeIcon icon={faPlus} style={{ marginRight: '0.5rem' }} />
                    Phát hành thẻ mới
                </Link>
            </div>

            <div className="card">
                <div style={{ marginBottom: '1rem' }}>
                    <input
                        className="input"
                        placeholder="Tìm theo chủ thẻ, số thẻ, mã tài khoản..."
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        style={{ maxWidth: '400px' }}
                    />
                </div>

                {loading ? (
                    <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>Đang tải dữ liệu từ CMS...</div>
                ) : filtered.length === 0 ? (
                    <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
                        Không tìm thấy thẻ nào trên hệ thống
                    </div>
                ) : (
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>Số Thẻ (PAN)</th>
                                    <th>Chủ thẻ</th>
                                    <th>Loại Thẻ</th>
                                    <th>Network</th>
                                    <th>Tài khoản l/kết</th>
                                    <th>Hạn mức</th>
                                    <th>Dư nợ</th>
                                    <th>Hết hạn</th>
                                    <th>Trạng thái</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filtered.map((card: any) => {
                                    const limit = Number(card.creditLimit || 0);
                                    const used = Number(card.outstandingBalance || 0);
                                    const status = card.status || 'UNKNOWN';

                                    return (
                                        <tr key={card.id}>
                                            <td style={{ fontFamily: 'monospace', fontWeight: 600, color: 'var(--text-primary)' }}>
                                                {card.maskedPan}
                                            </td>
                                            <td style={{ fontWeight: 500 }}>{card.cardholderName || '—'}</td>
                                            <td>
                                                {card.cardType === 'CREDIT' ? (
                                                    <span style={{ color: 'var(--accent)', fontWeight: 600, fontSize: '0.8125rem' }}>
                                                        <FontAwesomeIcon icon={faCreditCard} style={{ marginRight: '0.25rem' }} /> TÍN DỤNG
                                                    </span>
                                                ) : (
                                                    <span style={{ color: 'var(--success)', fontWeight: 600, fontSize: '0.8125rem' }}>
                                                        <FontAwesomeIcon icon={faMoneyBillWave} style={{ marginRight: '0.25rem' }} /> GHI NỢ
                                                    </span>
                                                )}
                                            </td>
                                            <td>
                                                <NetworkBadge network={card.network} />
                                            </td>
                                            <td style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                                                {card.accountId || '—'}
                                            </td>
                                            <td>
                                                <span style={{ color: card.cardType === 'CREDIT' ? 'inherit' : 'var(--text-secondary)' }}>
                                                    {card.cardType === 'CREDIT' ? `${limit.toLocaleString('en-US')} USD` : 'N/A'}
                                                </span>
                                            </td>
                                            <td style={{ color: used > 0 ? 'var(--warning)' : 'inherit', fontWeight: used > 0 ? 600 : 400 }}>
                                                {card.cardType === 'CREDIT' ? `${used.toLocaleString('en-US')} USD` : 'N/A'}
                                            </td>
                                            <td style={{ fontSize: '0.8125rem' }}>{card.expirationDate}</td>
                                            <td>
                                                <span className={`badge ${STATUS_BADGE[status] || 'badge-pending'}`}>
                                                    {status === 'ACTIVE' && <FontAwesomeIcon icon={faCheckCircle} style={{ marginRight: '0.25rem' }} />}
                                                    {status === 'LOCKED' && <FontAwesomeIcon icon={faLock} style={{ marginRight: '0.25rem' }} />}
                                                    {status === 'EXPIRED' && <FontAwesomeIcon icon={faExclamationTriangle} style={{ marginRight: '0.25rem' }} />}
                                                    {status}
                                                </span>
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>
        </div>
    );
}
