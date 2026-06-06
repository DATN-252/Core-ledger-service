'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowLeft, faFloppyDisk, faStore } from '@fortawesome/free-solid-svg-icons';
import AppModal from '@/components/AppModal';
import { createMerchant } from '@/lib/api';

export default function NewMerchantPage() {
  const router = useRouter();
  const [saving, setSaving] = useState(false);
  const [modal, setModal] = useState<{ title: string; message: string; onClose?: () => void } | null>(null);
  const [formData, setFormData] = useState({
    merchantId: '',
    name: '',
    category: 'shopping_pos',
    addressLine: '',
    ward: '',
    district: '',
    postalCode: '',
    latitude: '',
    longitude: '',
    cityName: '',
    country: 'Vietnam',
    cityPopulation: '',
    settlementAccountNumber: '',
    settlementAccountName: '',
    settlementBankName: '',
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      const response = await createMerchant({
        merchantId: formData.merchantId.trim(),
        name: formData.name.trim(),
        category: formData.category.trim(),
        addressLine: formData.addressLine.trim(),
        ward: formData.ward.trim() || null,
        district: formData.district.trim() || null,
        postalCode: formData.postalCode.trim() || null,
        latitude: formData.latitude ? Number(formData.latitude) : null,
        longitude: formData.longitude ? Number(formData.longitude) : null,
        cityName: formData.cityName.trim(),
        country: formData.country.trim(),
        cityPopulation: Number(formData.cityPopulation),
        settlementAccountNumber: formData.settlementAccountNumber.trim() || null,
        settlementAccountName: formData.settlementAccountName.trim() || null,
        settlementBankName: formData.settlementBankName.trim() || null,
      });

      setModal({
        title: 'Dang ky merchant thanh cong',
        message: `Merchant ${response.merchantId} da duoc tao.`,
        onClose: () => router.push(`/dashboard/merchants/${response.merchantId}`),
      });
    } catch (err: any) {
      setModal({
        title: 'Khong the tao merchant',
        message: err.message || 'Da xay ra loi khi dang ky merchant.',
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="animate-fade-in">
      <AppModal
        open={!!modal}
        title={modal?.title || ''}
        onClose={() => {
          const next = modal?.onClose;
          setModal(null);
          next?.();
        }}
        footer={<button className="btn-primary" onClick={() => {
          const next = modal?.onClose;
          setModal(null);
          next?.();
        }}>Da hieu</button>}
      >
        <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7 }}>{modal?.message}</p>
      </AppModal>

      <div style={{ marginBottom: '2rem' }}>
        <Link href="/dashboard/merchants" style={{ color: 'var(--text-secondary)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', fontSize: '0.875rem' }}>
          <FontAwesomeIcon icon={faArrowLeft} /> Quay lại danh sách merchant
        </Link>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
          <FontAwesomeIcon icon={faStore} style={{ marginRight: '0.5rem' }} />
          Đăng ký merchant
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
          Tạo mới đơn vị chấp nhận thanh toán và tài khoản settlement tương ứng.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="card" style={{ maxWidth: '900px' }}>
        <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>Thông tin merchant</h2>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '2rem' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Merchant ID *</label>
            <input required name="merchantId" value={formData.merchantId} onChange={handleChange} className="input" placeholder="SP1001" />
          </div>
          <div>
            <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Tên merchant *</label>
            <input required name="name" value={formData.name} onChange={handleChange} className="input" placeholder="BKBank Coffee" />
          </div>
          <div>
            <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Danh mục *</label>
            <select required name="category" value={formData.category} onChange={handleChange} className="input">
              <option value="shopping_pos">shopping_pos</option>
              <option value="grocery_pos">grocery_pos</option>
              <option value="health_fitness">health_fitness</option>
              <option value="entertainment">entertainment</option>
              <option value="food_dining">food_dining</option>
              <option value="shopping_net">shopping_net</option>
              <option value="UTILITY">UTILITY</option>
              <option value="RETAIL">RETAIL</option>
            </select>
          </div>
          <div>
            <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Mã bưu chính</label>
            <input name="postalCode" value={formData.postalCode} onChange={handleChange} className="input" placeholder="700000" />
          </div>
          <div style={{ gridColumn: '1 / -1' }}>
            <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Địa chỉ *</label>
            <input required name="addressLine" value={formData.addressLine} onChange={handleChange} className="input" placeholder="123 Nguyen Hue" />
          </div>
          <div>
            <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Phường / Xã</label>
            <input name="ward" value={formData.ward} onChange={handleChange} className="input" />
          </div>
          <div>
            <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Quận / Huyện</label>
            <input name="district" value={formData.district} onChange={handleChange} className="input" />
          </div>
        </div>

        <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>Thông tin vị trí</h2>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '2rem' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Thành phố *</label>
            <input required name="cityName" value={formData.cityName} onChange={handleChange} className="input" placeholder="Ho Chi Minh City" />
          </div>
          <div>
            <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Quốc gia *</label>
            <input required name="country" value={formData.country} onChange={handleChange} className="input" />
          </div>
          <div>
            <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Dân số thành phố *</label>
            <input required type="number" min="1" name="cityPopulation" value={formData.cityPopulation} onChange={handleChange} className="input" placeholder="9300000" />
          </div>
          <div>
            <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Latitude</label>
            <input type="number" step="0.000001" name="latitude" value={formData.latitude} onChange={handleChange} className="input" placeholder="10.7768" />
          </div>
          <div>
            <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Longitude</label>
            <input type="number" step="0.000001" name="longitude" value={formData.longitude} onChange={handleChange} className="input" placeholder="106.7002" />
          </div>
        </div>

        <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>Settlement</h2>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '2rem' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Số tài khoản settlement</label>
            <input name="settlementAccountNumber" value={formData.settlementAccountNumber} onChange={handleChange} className="input" placeholder="Để trống để tự sinh" />
          </div>
          <div>
            <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Tên tài khoản settlement</label>
            <input name="settlementAccountName" value={formData.settlementAccountName} onChange={handleChange} className="input" placeholder="Mặc định theo tên merchant" />
          </div>
          <div style={{ gridColumn: '1 / -1' }}>
            <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Ngân hàng settlement</label>
            <input name="settlementBankName" value={formData.settlementBankName} onChange={handleChange} className="input" placeholder="BKBank Merchant Network" />
          </div>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
          <Link href="/dashboard/merchants" className="btn-secondary" style={{ textDecoration: 'none' }}>
            Hủy
          </Link>
          <button type="submit" disabled={saving} className="btn-primary" style={{ padding: '0.5rem 1.5rem' }}>
            {saving ? 'Đang lưu...' : <><FontAwesomeIcon icon={faFloppyDisk} /> Lưu merchant</>}
          </button>
        </div>
      </form>
    </div>
  );
}
