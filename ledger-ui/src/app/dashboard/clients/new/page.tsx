'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { createClient } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faUserPlus, faArrowLeft, faSave } from '@fortawesome/free-solid-svg-icons';
import Link from 'next/link';

export default function NewClientPage() {
    const router = useRouter();
    const [loading, setLoading] = useState(false);
    const [formData, setFormData] = useState({
        fullName: '',
        dateOfBirth: '',
        gender: 'MALE',
        email: '',
        phoneNumber: '',
        address: '',
        city: '',
        country: 'Vietnam',
        idNumber: '',
        idType: 'CITIZEN_ID',
        idIssueDate: '',
        idExpiryDate: '',
        occupation: '',
        employerName: '',
        monthlyIncome: '',
    });

    const handleChange = (e: any) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        try {
            await createClient({
                ...formData,
                monthlyIncome: Number(formData.monthlyIncome) || 0
            });
            alert('Tạo khách hàng thành công!');
            router.push('/dashboard/clients');
        } catch (err: any) {
            alert(err.message || 'Lỗi khi tạo khách hàng');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="animate-fade-in">
            <div style={{ marginBottom: '2rem' }}>
                <Link href="/dashboard/clients" style={{ color: 'var(--text-secondary)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', fontSize: '0.875rem' }}>
                    <FontAwesomeIcon icon={faArrowLeft} /> Quay lại danh sách
                </Link>
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                    <FontAwesomeIcon icon={faUserPlus} style={{ marginRight: '0.5rem' }} />
                    Thêm Khách Hàng Mới
                </h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                    Nhập thông tin chi tiết để tạo hồ sơ khách hàng
                </p>
            </div>

            <form onSubmit={handleSubmit} className="card" style={{ maxWidth: '800px' }}>
                <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>Thông tin cơ bản</h2>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '2rem' }}>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Họ và tên *</label>
                        <input required name="fullName" value={formData.fullName} onChange={handleChange} className="input" placeholder="Nguyễn Văn A" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Giới tính *</label>
                        <select required name="gender" value={formData.gender} onChange={handleChange} className="input">
                            <option value="MALE">Nam</option>
                            <option value="FEMALE">Nữ</option>
                            <option value="OTHER">Chưa rõ</option>
                        </select>
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Ngày sinh *</label>
                        <input required type="date" name="dateOfBirth" value={formData.dateOfBirth} onChange={handleChange} className="input" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Email *(duy nhất)</label>
                        <input required type="email" name="email" value={formData.email} onChange={handleChange} className="input" placeholder="nguyenvana@example.com" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Số điện thoại</label>
                        <input required name="phoneNumber" value={formData.phoneNumber} onChange={handleChange} className="input" placeholder="0901234567" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Quốc gia</label>
                        <input required name="country" value={formData.country} onChange={handleChange} className="input" />
                    </div>
                    <div style={{ gridColumn: '1 / -1' }}>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Địa chỉ</label>
                        <input required name="address" value={formData.address} onChange={handleChange} className="input" placeholder="Số nhà, đường, phường/xã..." />
                    </div>
                    <div style={{ gridColumn: '1 / -1' }}>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Thành phố / Tỉnh</label>
                        <input required name="city" value={formData.city} onChange={handleChange} className="input" placeholder="Hà Nội, TP.HCM..." />
                    </div>
                </div>

                <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>Giấy tờ tùy thân</h2>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '2rem' }}>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Loại giấy tờ</label>
                        <select required name="idType" value={formData.idType} onChange={handleChange} className="input">
                            <option value="CITIZEN_ID">CCCD</option>
                            <option value="PASSPORT">Hộ chiếu</option>
                            <option value="DRIVERS_LICENSE">GPLX</option>
                        </select>
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Số giấy tờ</label>
                        <input required name="idNumber" value={formData.idNumber} onChange={handleChange} className="input" placeholder="001099000001" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Ngày cấp</label>
                        <input required type="date" name="idIssueDate" value={formData.idIssueDate} onChange={handleChange} className="input" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Ngày hết hạn</label>
                        <input required type="date" name="idExpiryDate" value={formData.idExpiryDate} onChange={handleChange} className="input" />
                    </div>
                </div>

                <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>Thông tin nghề nghiệp</h2>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '2rem' }}>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Nghề nghiệp</label>
                        <input name="occupation" value={formData.occupation} onChange={handleChange} className="input" placeholder="Kỹ sư phần mềm" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Thu nhập hàng tháng (USD)</label>
                        <input type="number" name="monthlyIncome" value={formData.monthlyIncome} onChange={handleChange} className="input" placeholder="1000" />
                    </div>
                    <div style={{ gridColumn: '1 / -1' }}>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Tên công ty / Nơi làm việc</label>
                        <input name="employerName" value={formData.employerName} onChange={handleChange} className="input" placeholder="Công ty TNHH ABC" />
                    </div>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
                    <Link href="/dashboard/clients" className="btn-secondary" style={{ textDecoration: 'none' }}>Hủy bỏ</Link>
                    <button type="submit" disabled={loading} className="btn-primary" style={{ padding: '0.5rem 1.5rem' }}>
                        {loading ? 'Đang tạo...' : <><FontAwesomeIcon icon={faSave} /> Lưu Khách Hàng</>}
                    </button>
                </div>
            </form>
        </div>
    );
}
