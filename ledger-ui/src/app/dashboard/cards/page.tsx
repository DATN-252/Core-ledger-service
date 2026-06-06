'use client';
import { useEffect, useState } from 'react';
import { getCreditCards } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faIdCard, faCreditCard, faMoneyBillWave, faLock, faCheckCircle, faExclamationTriangle, faPlus, faEye } from '@fortawesome/free-solid-svg-icons';
import Link from 'next/link';
import CardNetworkLogo from '@/components/CardNetworkLogo';

const STATUS_BADGE: Record<string, string> = {
    ACTIVE: 'badge-active',
    PENDING: 'badge-pending',
    LOCKED: 'badge-locked',
    EXPIRED: 'badge-locked',
    CANCELLED: 'badge-locked',
};

export default function CardsPage() {
    const [cards, setCards] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState('');
    const [status, setStatus] = useState('ALL');
    const [cardType, setCardType] = useState('ALL');
    const [network, setNetwork] = useState('ALL');
    const [sortBy, setSortBy] = useState('cardholderName');
    const [sortDir, setSortDir] = useState('asc');

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

    const filtered = cards
        .filter(c =>
            (!search ||
                c.cardholderName?.toLowerCase().includes(search.toLowerCase()) ||
                c.maskedPan?.includes(search) ||
                c.linkedAccountNumber?.toLowerCase().includes(search.toLowerCase()) ||
                c.accountId?.toLowerCase().includes(search.toLowerCase())) &&
            (status === 'ALL' || c.status === status) &&
            (cardType === 'ALL' || c.cardType === cardType) &&
            (network === 'ALL' || (c.network || 'UNKNOWN').toUpperCase() === network)
        )
        .sort((a, b) => {
            const direction = sortDir === 'asc' ? 1 : -1;
            const aValue = sortBy === 'creditLimit'
                ? Number(a.creditLimit || 0)
                : sortBy === 'outstandingBalance'
                    ? Number(a.outstandingBalance || 0)
                    : String(a[sortBy] || '').toLowerCase();
            const bValue = sortBy === 'creditLimit'
                ? Number(b.creditLimit || 0)
                : sortBy === 'outstandingBalance'
                    ? Number(b.outstandingBalance || 0)
                    : String(b[sortBy] || '').toLowerCase();
            if (aValue < bValue) return -1 * direction;
            if (aValue > bValue) return 1 * direction;
            return 0;
        });

    return (
        <div className="animate-fade-in">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '2rem' }}>
                <div>
                    <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                        <FontAwesomeIcon icon={faIdCard} style={{ marginRight: '0.5rem' }} />
                        Quản lý Thẻ (CMS)
                    </h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                        Tích hợp từ hệ thống Card Management System ({filtered.length}/{cards.length} thẻ)
                    </p>
                </div>
                <Link href="/dashboard/cards/new" className="btn-primary" style={{ textDecoration: 'none' }}>
                    <FontAwesomeIcon icon={faPlus} style={{ marginRight: '0.5rem' }} />
                    Phát hành thẻ mới
                </Link>
            </div>

            <div className="card">
                <div style={{ marginBottom: '1rem', display: 'grid', gridTemplateColumns: 'minmax(260px, 2fr) repeat(5, minmax(140px, 1fr))', gap: '0.75rem' }}>
                    <input
                        className="input"
                        placeholder="Tìm theo chủ thẻ, số thẻ, mã tài khoản..."
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                    />
                    <select className="input" value={status} onChange={e => setStatus(e.target.value)}>
                        <option value="ALL">Tất cả trạng thái</option>
                        <option value="ACTIVE">ACTIVE</option>
                        <option value="PENDING">PENDING</option>
                        <option value="LOCKED">LOCKED</option>
                        <option value="EXPIRED">EXPIRED</option>
                        <option value="CANCELLED">CANCELLED</option>
                    </select>
                    <select className="input" value={cardType} onChange={e => setCardType(e.target.value)}>
                        <option value="ALL">Tất cả loại thẻ</option>
                        <option value="CREDIT">CREDIT</option>
                        <option value="DEBIT">DEBIT</option>
                    </select>
                    <select className="input" value={network} onChange={e => setNetwork(e.target.value)}>
                        <option value="ALL">Tất cả network</option>
                        <option value="VISA">VISA</option>
                        <option value="MASTERCARD">MASTERCARD</option>
                        <option value="AMEX">AMEX</option>
                        <option value="JCB">JCB</option>
                        <option value="DISCOVER">DISCOVER</option>
                        <option value="NAPAS">NAPAS</option>
                    </select>
                    <select className="input" value={sortBy} onChange={e => setSortBy(e.target.value)}>
                        <option value="cardholderName">Chủ thẻ</option>
                        <option value="maskedPan">Số thẻ</option>
                        <option value="status">Trạng thái</option>
                        <option value="network">Network</option>
                        <option value="creditLimit">Hạn mức</option>
                        <option value="outstandingBalance">Dư nợ</option>
                    </select>
                    <select className="input" value={sortDir} onChange={e => setSortDir(e.target.value)}>
                        <option value="asc">Tăng dần</option>
                        <option value="desc">Giảm dần</option>
                    </select>
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
                                    <th>Số tài khoản liên kết</th>
                                    <th>Hạn mức</th>
                                        <th>Dư nợ</th>
                                        <th>Hết hạn</th>
                                        <th>Trạng thái</th>
                                        <th>Thao tác</th>
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
                                                <CardNetworkLogo network={card.network} />
                                            </td>
                                            <td style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                                                {card.linkedAccountNumber || card.accountId || '—'}
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
                                            <td>
                                                <Link href={`/dashboard/cards/${card.id}`} className="btn-secondary" style={{ textDecoration: 'none', whiteSpace: 'nowrap' }}>
                                                    <FontAwesomeIcon icon={faEye} style={{ marginRight: '0.375rem' }} />
                                                    Chi tiết
                                                </Link>
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
