'use client';

import { useEffect, useState, use } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowLeft, faFloppyDisk, faUserPen } from '@fortawesome/free-solid-svg-icons';
import AppModal from '@/components/AppModal';
import { getBranches, getClient, updateClient } from '@/lib/api';

type ClientFormState = {
    fullName: string;
    dateOfBirth: string;
    gender: string;
    email: string;
    phoneNumber: string;
    address: string;
    city: string;
    country: string;
    homeBranchId: string;
    idNumber: string;
    idType: string;
    idIssueDate: string;
    idExpiryDate: string;
    occupation: string;
    employerName: string;
    employerAddress: string;
    employmentType: string;
    monthlyIncome: string;
    yearsAtCurrentJob: string;
    status: string;
};

const EMPTY_FORM: ClientFormState = {
    fullName: '',
    dateOfBirth: '',
    gender: 'MALE',
    email: '',
    phoneNumber: '',
    address: '',
    city: '',
    country: 'Vietnam',
    homeBranchId: '',
    idNumber: '',
    idType: '',
    idIssueDate: '',
    idExpiryDate: '',
    occupation: '',
    employerName: '',
    employerAddress: '',
    employmentType: 'FULL_TIME',
    monthlyIncome: '',
    yearsAtCurrentJob: '',
    status: 'ACTIVE',
};

function toDateInput(value: string | null | undefined) {
    if (!value) return '';
    return value.slice(0, 10);
}

export default function EditClientPage({ params }: { params: Promise<{ clientId: string }> }) {
    const { clientId } = use(params);
    const router = useRouter();
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [branches, setBranches] = useState<any[]>([]);
    const [formData, setFormData] = useState<ClientFormState>(EMPTY_FORM);
    const [modal, setModal] = useState<{ title: string; message: string; onClose?: () => void } | null>(null);

    useEffect(() => {
        async function loadData() {
            try {
                const [client, branchList] = await Promise.all([
                    getClient(clientId),
                    getBranches(),
                ]);

                setBranches(branchList || []);
                setFormData({
                    fullName: client.fullName || '',
                    dateOfBirth: toDateInput(client.dateOfBirth),
                    gender: client.gender || 'MALE',
                    email: client.email || '',
                    phoneNumber: client.phoneNumber || '',
                    address: client.address || '',
                    city: client.city || '',
                    country: client.country || 'Vietnam',
                    homeBranchId: client.homeBranchId || branchList?.[0]?.branchId || '',
                    idNumber: client.idNumber || '',
                    idType: client.idType || '',
                    idIssueDate: toDateInput(client.idIssueDate),
                    idExpiryDate: toDateInput(client.idExpiryDate),
                    occupation: client.occupation || '',
                    employerName: client.employerName || '',
                    employerAddress: client.employerAddress || '',
                    employmentType: client.employmentType || 'FULL_TIME',
                    monthlyIncome: client.monthlyIncome != null ? String(client.monthlyIncome) : '',
                    yearsAtCurrentJob: client.yearsAtCurrentJob != null ? String(client.yearsAtCurrentJob) : '',
                    status: client.status || 'ACTIVE',
                });
            } catch (err: any) {
                setModal({
                    title: 'Không thể tải hồ sơ khách hàng',
                    message: err.message || 'Không lấy được dữ liệu khách hàng.',
                    onClose: () => router.push(`/dashboard/clients/${clientId}`),
                });
            } finally {
                setLoading(false);
            }
        }

        loadData();
    }, [clientId, router]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true);
        try {
            await updateClient(clientId, {
                fullName: formData.fullName,
                dateOfBirth: formData.dateOfBirth || null,
                gender: formData.gender,
                email: formData.email,
                phoneNumber: formData.phoneNumber,
                address: formData.address,
                city: formData.city,
                country: formData.country,
                homeBranchId: formData.homeBranchId,
                idIssueDate: formData.idIssueDate || null,
                idExpiryDate: formData.idExpiryDate || null,
                occupation: formData.occupation || null,
                employerName: formData.employerName || null,
                employerAddress: formData.employerAddress || null,
                employmentType: formData.employmentType || null,
                monthlyIncome: formData.monthlyIncome ? Number(formData.monthlyIncome) : null,
                yearsAtCurrentJob: formData.yearsAtCurrentJob ? Number(formData.yearsAtCurrentJob) : null,
                status: formData.status,
            });
            setModal({
                title: 'Cập nhật thành công',
                message: 'Thông tin khách hàng đã được cập nhật.',
                onClose: () => router.push(`/dashboard/clients/${clientId}`),
            });
        } catch (err: any) {
            setModal({
                title: 'Không thể cập nhật khách hàng',
                message: err.message || 'Đã xảy ra lỗi khi lưu thay đổi.',
            });
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Đang tải...</div>;
    }

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
                }}>Đã hiểu</button>}
            >
                <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7 }}>{modal?.message}</p>
            </AppModal>

            <div style={{ marginBottom: '2rem' }}>
                <Link href={`/dashboard/clients/${clientId}`} style={{ color: 'var(--text-secondary)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', fontSize: '0.875rem' }}>
                    <FontAwesomeIcon icon={faArrowLeft} /> Quay lại chi tiết khách hàng
                </Link>
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                    <FontAwesomeIcon icon={faUserPen} style={{ marginRight: '0.5rem' }} />
                    Chỉnh sửa khách hàng
                </h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                    Mã khách hàng: {clientId}
                </p>
            </div>

            <form onSubmit={handleSubmit} className="card" style={{ maxWidth: '900px' }}>
                <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>Thông tin cơ bản</h2>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '2rem' }}>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Họ và tên *</label>
                        <input required name="fullName" value={formData.fullName} onChange={handleChange} className="input" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Giới tính *</label>
                        <select required name="gender" value={formData.gender} onChange={handleChange} className="input">
                            <option value="MALE">Nam</option>
                            <option value="FEMALE">Nữ</option>
                            <option value="OTHER">Khác</option>
                        </select>
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Ngày sinh *</label>
                        <input required type="date" name="dateOfBirth" value={formData.dateOfBirth} onChange={handleChange} className="input" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Email *</label>
                        <input required type="email" name="email" value={formData.email} onChange={handleChange} className="input" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Số điện thoại *</label>
                        <input required name="phoneNumber" value={formData.phoneNumber} onChange={handleChange} className="input" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Quốc gia *</label>
                        <input required name="country" value={formData.country} onChange={handleChange} className="input" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Chi nhánh quản lý *</label>
                        <select required name="homeBranchId" value={formData.homeBranchId} onChange={handleChange} className="input">
                            <option value="">Chọn chi nhánh</option>
                            {branches.map((branch: any) => (
                                <option key={branch.branchId} value={branch.branchId}>
                                    {branch.branchId} - {branch.branchName}
                                </option>
                            ))}
                        </select>
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Trạng thái *</label>
                        <select required name="status" value={formData.status} onChange={handleChange} className="input">
                            <option value="ACTIVE">ACTIVE</option>
                            <option value="SUSPENDED">SUSPENDED</option>
                            <option value="CLOSED">CLOSED</option>
                        </select>
                    </div>
                    <div style={{ gridColumn: '1 / -1' }}>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Địa chỉ *</label>
                        <input required name="address" value={formData.address} onChange={handleChange} className="input" />
                    </div>
                    <div style={{ gridColumn: '1 / -1' }}>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Thành phố / Tỉnh *</label>
                        <input required name="city" value={formData.city} onChange={handleChange} className="input" />
                    </div>
                </div>

                <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>Giấy tờ tùy thân</h2>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '2rem' }}>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Loại giấy tờ</label>
                        <input value={formData.idType} className="input" disabled />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Số giấy tờ</label>
                        <input value={formData.idNumber} className="input" disabled />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Ngày cấp</label>
                        <input type="date" name="idIssueDate" value={formData.idIssueDate} onChange={handleChange} className="input" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Ngày hết hạn</label>
                        <input type="date" name="idExpiryDate" value={formData.idExpiryDate} onChange={handleChange} className="input" />
                    </div>
                </div>

                <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>Thông tin nghề nghiệp</h2>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '2rem' }}>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Nghề nghiệp</label>
                        <input name="occupation" value={formData.occupation} onChange={handleChange} className="input" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Hình thức làm việc</label>
                        <select name="employmentType" value={formData.employmentType} onChange={handleChange} className="input">
                            <option value="FULL_TIME">Toàn thời gian</option>
                            <option value="PART_TIME">Bán thời gian</option>
                            <option value="SELF_EMPLOYED">Tự doanh</option>
                            <option value="UNEMPLOYED">Chưa có việc</option>
                            <option value="RETIRED">Nghỉ hưu</option>
                            <option value="STUDENT">Sinh viên</option>
                        </select>
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Tên công ty / Nơi làm việc</label>
                        <input name="employerName" value={formData.employerName} onChange={handleChange} className="input" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Thu nhập hàng tháng (USD)</label>
                        <input type="number" min="0" step="0.01" name="monthlyIncome" value={formData.monthlyIncome} onChange={handleChange} className="input" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Số năm làm việc hiện tại</label>
                        <input type="number" min="0" name="yearsAtCurrentJob" value={formData.yearsAtCurrentJob} onChange={handleChange} className="input" />
                    </div>
                    <div style={{ gridColumn: '1 / -1' }}>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Địa chỉ nơi làm việc</label>
                        <input name="employerAddress" value={formData.employerAddress} onChange={handleChange} className="input" />
                    </div>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
                    <Link href={`/dashboard/clients/${clientId}`} className="btn-secondary" style={{ textDecoration: 'none' }}>
                        Hủy
                    </Link>
                    <button type="submit" disabled={saving} className="btn-primary" style={{ padding: '0.5rem 1.5rem' }}>
                        {saving ? 'Đang lưu...' : <><FontAwesomeIcon icon={faFloppyDisk} /> Lưu thay đổi</>}
                    </button>
                </div>
            </form>
        </div>
    );
}
