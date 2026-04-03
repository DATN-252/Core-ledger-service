'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { issueDebitCard, issueCreditCard } from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faCreditCard, faArrowLeft, faSave, faRandom } from '@fortawesome/free-solid-svg-icons';
import Link from 'next/link';
import AppModal from '@/components/AppModal';

export default function NewCardPage() {
    const router = useRouter();
    const [loading, setLoading] = useState(false);
    const [formError, setFormError] = useState('');
    const [modal, setModal] = useState<{ title: string; message: string; onClose?: () => void } | null>(null);
    const [formData, setFormData] = useState({
        type: 'DEBIT',
        pan: '',
        cvv: '',
        expirationDate: '',
        cardholderName: '',
        accountId: '', // For debit
        loanAccountId: '', // For credit
        creditLimit: 0, // For credit
        network: 'VISA', // Card network
    });

    const handleChange = (e: any) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleTypeChange = (e: any) => {
        setFormData({ ...formData, type: e.target.value });
    };

    // Generate random 16 digit PAN
    function generateRandomCard() {
        const pan = Array.from({ length: 16 }, () => Math.floor(Math.random() * 10)).join('');
        const cvv = Array.from({ length: 3 }, () => Math.floor(Math.random() * 10)).join('');
        const nextYear = new Date();
        nextYear.setFullYear(nextYear.getFullYear() + 4);
        const yyyy = nextYear.getFullYear();
        const mm = String(nextYear.getMonth() + 1).padStart(2, '0');
        const dd = '28'; // Ensure validity for Feb

        setFormData({ ...formData, pan, cvv, expirationDate: `${yyyy}-${mm}-${dd}` });
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setFormError('');
        try {
            if (formData.type === 'DEBIT') {
                await issueDebitCard({
                    pan: formData.pan,
                    cvv: formData.cvv,
                    expirationDate: formData.expirationDate,
                    accountId: formData.accountId,
                    cardholderName: formData.cardholderName,
                    network: formData.network,
                });
            } else {
                await issueCreditCard({
                    pan: formData.pan,
                    cvv: formData.cvv,
                    expirationDate: formData.expirationDate,
                    loanAccountId: formData.loanAccountId,
                    creditLimit: formData.creditLimit,
                    cardholderName: formData.cardholderName,
                    network: formData.network,
                });
            }
            setModal({
                title: 'Phát hành thẻ thành công',
                message: 'Thẻ mới đã được phát hành thành công.',
                onClose: () => router.push('/dashboard/cards'),
            });
        } catch (err: any) {
            setFormError(err.message || 'Lỗi khi phát hành thẻ');
        } finally {
            setLoading(false);
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
                }}>Đã hiểu</button>}
            >
                <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7 }}>{modal?.message}</p>
            </AppModal>
            <div style={{ marginBottom: '2rem' }}>
                <Link href="/dashboard/cards" style={{ color: 'var(--text-secondary)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', fontSize: '0.875rem' }}>
                    <FontAwesomeIcon icon={faArrowLeft} /> Quay lại danh sách
                </Link>
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.25rem' }}>
                    <FontAwesomeIcon icon={faCreditCard} style={{ marginRight: '0.5rem' }} />
                    Phát hành thẻ mới
                </h1>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                    Nhập thông tin chi tiết để phát hành thẻ cho khách hàng
                </p>
            </div>

            <form onSubmit={handleSubmit} className="card" style={{ maxWidth: '800px' }}>
                {formError && <div className="alert-error" style={{ marginBottom: '1rem', padding: '1rem', background: 'rgba(239, 68, 68, 0.1)', color: 'var(--danger)', borderRadius: '8px' }}>{formError}</div>}

                <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>Loại thẻ phát hành</h2>
                <div style={{ display: 'flex', gap: '2rem', marginBottom: '2rem' }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '1rem' }}>
                        <input required type="radio" name="type" value="DEBIT" checked={formData.type === 'DEBIT'} onChange={handleTypeChange} style={{ transform: 'scale(1.2)' }} />
                        Thẻ Ghi nợ (Debit)
                    </label>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '1rem' }}>
                        <input required type="radio" name="type" value="CREDIT" checked={formData.type === 'CREDIT'} onChange={handleTypeChange} style={{ transform: 'scale(1.2)' }} />
                        Thẻ Tín dụng (Credit)
                    </label>
                </div>

                <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>Thông tin thẻ</h2>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '2rem' }}>
                    <div style={{ gridColumn: '1 / -1' }}>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Tên chủ thẻ (In Hoa Không Dấu) *</label>
                        <input required name="cardholderName" value={formData.cardholderName} onChange={(e) => setFormData({ ...formData, cardholderName: e.target.value.toUpperCase() })} className="input" placeholder="VD: NGUYEN VAN A" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Số PAN (16 số) *</label>
                        <div style={{ display: 'flex', gap: '0.5rem' }}>
                            <input required name="pan" minLength={16} maxLength={16} value={formData.pan} onChange={handleChange} className="input" placeholder="0000111122223333" />
                            <button type="button" className="btn-secondary" onClick={generateRandomCard} title="Sinh số ngẫu nhiên">
                                <FontAwesomeIcon icon={faRandom} />
                            </button>
                        </div>
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>CVV *</label>
                        <input required name="cvv" maxLength={3} value={formData.cvv} onChange={handleChange} className="input" placeholder="123" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Ngày hết hạn *</label>
                        <input required type="date" name="expirationDate" value={formData.expirationDate} onChange={handleChange} className="input" />
                    </div>
                    <div>
                        <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Network *</label>
                        <select name="network" value={formData.network} onChange={handleChange} className="input" required>
                            <option value="VISA">VISA</option>
                            <option value="MASTERCARD">MASTERCARD</option>
                            <option value="NAPAS">NAPAS</option>
                            <option value="JCB">JCB</option>
                            <option value="AMEX">AMEX</option>
                            <option value="DISCOVER">DISCOVER</option>
                        </select>
                    </div>
                </div>

                <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem', borderBottom: '1px solid var(--border)', paddingBottom: '0.5rem' }}>
                    {formData.type === 'DEBIT' ? 'Liên kết tài khoản' : 'Liên kết hạn mức tín dụng'}
                </h2>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '2rem' }}>
                    {formData.type === 'DEBIT' ? (
                        <div style={{ gridColumn: '1 / -1' }}>
                            <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Mã Tài khoản Ghi nợ (Savings Account ID) *</label>
                            <input required name="accountId" value={formData.accountId} onChange={handleChange} className="input" placeholder="VD: SAV_00123" />
                        </div>
                    ) : (
                        <>
                            <div>
                                <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Mã KH Tín dụng (Loan Account ID) *</label>
                                <input required name="loanAccountId" value={formData.loanAccountId} onChange={handleChange} className="input" placeholder="VD: LOAN_00456" />
                            </div>
                            <div>
                                <label style={{ display: 'block', marginBottom: '0.25rem', fontSize: '0.875rem' }}>Hạn mức được cấp (USD) *</label>
                                <input required type="number" min="0" step="1000" name="creditLimit" value={formData.creditLimit} onChange={(e) => setFormData({ ...formData, creditLimit: Number(e.target.value) })} className="input" placeholder="10000" />
                            </div>
                        </>
                    )}
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
                    <Link href="/dashboard/cards" className="btn-secondary" style={{ textDecoration: 'none' }}>Hủy bỏ</Link>
                    <button type="submit" disabled={loading} className="btn-primary" style={{ padding: '0.5rem 1.5rem' }}>
                        {loading ? 'Đang xử lý...' : <><FontAwesomeIcon icon={faSave} /> Phát hành thẻ</>}
                    </button>
                </div>
            </form>
        </div>
    );
}
