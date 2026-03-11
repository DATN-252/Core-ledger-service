'use client';
import { useEffect, useState } from 'react';
import { getAllClients } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faUsers, faPlus, faEye } from '@fortawesome/free-solid-svg-icons';
import Link from 'next/link';
import { Pagination } from '@/components/Pagination';

export default function ClientsPage() {
    const [clients, setClients] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);

    useEffect(() => {
        setLoading(true);
        getAllClients(page, 10)
            .then(data => {
                setClients(data?.content || []);
                setTotalPages(data?.totalPages || 1);
            })
            .catch(() => setClients([]))
            .finally(() => setLoading(false));
    }, [page]);

    const filtered = clients.filter(c =>
        !search ||
        c.fullName?.toLowerCase().includes(search.toLowerCase()) ||
        c.clientId?.toLowerCase().includes(search.toLowerCase())
    );

    return (
        <div className="animate-fade-in">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '2rem' }}>
                <div>
                    <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                        <FontAwesomeIcon icon={faUsers} style={{ marginRight: '0.5rem' }} />
                        Khách hàng
                    </h1>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                        Quản lý hồ sơ khách hàng ({clients.length} hồ sơ)
                    </p>
                </div>
                <Link href="/dashboard/clients/new" className="btn-primary" style={{ textDecoration: 'none' }}>
                    <FontAwesomeIcon icon={faPlus} />
                    Thêm khách hàng
                </Link>
            </div>

            <div className="card">
                <div style={{ marginBottom: '1rem' }}>
                    <input
                        className="input"
                        placeholder="Tìm kiếm theo ID hoặc Tên khách hàng..."
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                        style={{ maxWidth: '360px' }}
                    />
                </div>

                {loading ? (
                    <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>Đang tải...</div>
                ) : (
                    <div className="table-container">
                        <table>
                            <thead>
                                <tr>
                                    <th>Mã KH</th>
                                    <th>Họ tên</th>
                                    <th>Email</th>
                                    <th>Số điện thoại</th>
                                    <th>Tổng TK</th>
                                    <th>Trạng thái</th>
                                    <th>Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filtered.length === 0 ? (
                                    <tr>
                                        <td colSpan={7} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
                                            Không có khách hàng
                                        </td>
                                    </tr>
                                ) : filtered.map((client: any) => (
                                    <tr key={client.clientId}>
                                        <td style={{ color: 'var(--text-secondary)', fontSize: '0.8125rem' }}>#{client.clientId}</td>
                                        <td style={{ fontWeight: 600 }}>{client.fullName}</td>
                                        <td>{client.email || '—'}</td>
                                        <td>{client.phoneNumber || '—'}</td>
                                        <td style={{ fontWeight: 600 }}>{client.totalAccounts}</td>
                                        <td>
                                            <span className={`badge ${client.status === 'ACTIVE' ? 'badge-active' : 'badge-pending'}`}>
                                                {client.status}
                                            </span>
                                        </td>
                                        <td>
                                            <div style={{ display: 'flex', gap: '0.5rem' }}>
                                                <Link href={`/dashboard/clients/${client.clientId}`} className="btn-secondary" style={{ padding: '0.4rem 0.8rem', textDecoration: 'none' }}>
                                                    <FontAwesomeIcon icon={faEye} /> Chi tiết
                                                </Link>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
            </div>
        </div>
    );
}
