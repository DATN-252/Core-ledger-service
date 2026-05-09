'use client';

import { useEffect, useState } from 'react';
import { getMerchants } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faMoneyCheckDollar, faPlus, faStore } from '@fortawesome/free-solid-svg-icons';
import Link from 'next/link';
import { Pagination } from '@/components/Pagination';

export default function MerchantsPage() {
  const [merchants, setMerchants] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('ACTIVE');
  const [category, setCategory] = useState('ALL');
  const [sortBy, setSortBy] = useState('merchantId');
  const [sortDir, setSortDir] = useState('asc');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  useEffect(() => {
    setLoading(true);
    getMerchants(page, 20, {
      q: search || undefined,
      status,
      category,
      sortBy,
      sortDir,
    })
      .then((data) => {
        setMerchants(data?.content || []);
        setTotalPages(data?.totalPages || 1);
        setTotalElements(data?.totalElements || 0);
      })
      .catch(() => setMerchants([]))
      .finally(() => setLoading(false));
  }, [page, search, status, category, sortBy, sortDir]);

  return (
    <div className="animate-fade-in">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '2rem', gap: '1rem', flexWrap: 'wrap' }}>
        <div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
            <FontAwesomeIcon icon={faStore} style={{ marginRight: '0.5rem' }} />
            Merchants
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
            Quản lý merchant, địa chỉ và tài khoản settlement ({totalElements} merchant)
          </p>
        </div>
        <Link href="/dashboard/merchants/new" className="btn-primary" style={{ textDecoration: 'none' }}>
          <FontAwesomeIcon icon={faPlus} />
          Đăng ký merchant
        </Link>
      </div>

      <div className="card">
        <div style={{ marginBottom: '1rem', display: 'grid', gridTemplateColumns: 'minmax(260px, 2fr) repeat(4, minmax(140px, 1fr))', gap: '0.75rem' }}>
          <input
            className="input"
            placeholder="Tim theo merchant ID, ten, category..."
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          />
          <select className="input" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
            <option value="ALL">Tất cả trạng thái</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="INACTIVE">INACTIVE</option>
          </select>
          <select className="input" value={category} onChange={(e) => { setCategory(e.target.value); setPage(0); }}>
            <option value="ALL">Tất cả category</option>
            <option value="UTILITY">UTILITY</option>
            <option value="RETAIL">RETAIL</option>
            <option value="shopping_pos">shopping_pos</option>
            <option value="grocery_pos">grocery_pos</option>
            <option value="health_fitness">health_fitness</option>
            <option value="entertainment">entertainment</option>
            <option value="food_dining">food_dining</option>
            <option value="shopping_net">shopping_net</option>
          </select>
          <select className="input" value={sortBy} onChange={(e) => { setSortBy(e.target.value); setPage(0); }}>
            <option value="merchantId">Merchant ID</option>
            <option value="name">Tên merchant</option>
            <option value="category">Category</option>
            <option value="cityName">Thành phố</option>
            <option value="status">Trạng thái</option>
          </select>
          <select className="input" value={sortDir} onChange={(e) => { setSortDir(e.target.value); setPage(0); }}>
            <option value="asc">Tăng dần</option>
            <option value="desc">Giảm dần</option>
          </select>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>Dang tai...</div>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Merchant ID</th>
                  <th>Tên merchant</th>
                  <th>Category</th>
                  <th>Địa chỉ</th>
                  <th>Tài khoản</th>
                  <th>Số dư</th>
                  <th>Trạng thái</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {merchants.length === 0 ? (
                  <tr>
                    <td colSpan={8} style={{ textAlign: 'center', padding: '3rem', color: 'var(--text-secondary)' }}>
                      Không có merchant
                    </td>
                  </tr>
                ) : merchants.map((merchant) => (
                  <tr key={merchant.merchantId}>
                    <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>{merchant.merchantId}</td>
                    <td style={{ fontWeight: 600 }}>{merchant.name}</td>
                    <td>{merchant.category || '-'}</td>
                    <td style={{ maxWidth: '320px' }}>{merchant.address || '-'}</td>
                    <td>
                      <div style={{ fontFamily: 'monospace' }}>{merchant.settlementAccountNumber || '-'}</div>
                      <div style={{ color: 'var(--text-secondary)', marginTop: '0.125rem' }}>{merchant.settlementBankName || '-'}</div>
                    </td>
                    <td style={{ fontWeight: 700 }}>
                      {Number(merchant.settlementAccountBalance || 0).toLocaleString('en-US', { maximumFractionDigits: 2 })} USD
                    </td>
                    <td>
                      <span className={`badge ${merchant.status === 'ACTIVE' ? 'badge-active' : 'badge-pending'}`}>
                        {merchant.status}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                        <Link href={`/dashboard/merchants/${merchant.merchantId}`} className="btn-secondary" style={{ padding: '0.45rem 0.8rem', textDecoration: 'none' }}>
                          Chi tiết
                        </Link>
                        <Link href="/dashboard/settlements" className="btn-primary" style={{ padding: '0.45rem 0.8rem', textDecoration: 'none' }}>
                          <FontAwesomeIcon icon={faMoneyCheckDollar} />
                          Settlement
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
