'use client';

import { useEffect, useMemo, useState } from 'react';
import {
  executeSettlementBatch,
  generateSettlementBatch,
  getMerchants,
  getSettlementBatchDetail,
  getSettlementAdjustments,
  getSettlementBatches,
  getSettlementPreview,
  runAutoSettlement,
} from '@/lib/api';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faClockRotateLeft, faMoneyCheckDollar, faRotate, faWandMagicSparkles } from '@fortawesome/free-solid-svg-icons';
import { Pagination } from '@/components/Pagination';

type MerchantItem = {
  merchantId: string;
  name: string;
  category?: string | null;
  status: string;
  settlementAccountNumber?: string | null;
  settlementAccountName?: string | null;
  settlementBankName?: string | null;
  settlementAccountBalance?: number | null;
};

type SettlementBatch = {
  id: number;
  merchantId: string;
  merchantName: string;
  settlementAccountNumber: string;
  settlementAccountName: string;
  settlementBankName: string;
  settlementAccountBalance?: number | null;
  currency: string;
  fromDate: string;
  toDate: string;
  transactionCount: number;
  grossAmount: number;
  feeRate: number;
  feeAmount: number;
  netAmount: number;
  status: 'PENDING' | 'SETTLED' | 'FAILED' | 'CANCELLED';
  executedAt?: string | null;
  executionReference?: string | null;
  note?: string | null;
  createdAt?: string | null;
  items?: SettlementBatchItem[] | null;
  adjustmentCount?: number | null;
  adjustmentAmount?: number | null;
  adjustments?: SettlementAdjustment[] | null;
};

type SettlementBatchItem = {
  id: number;
  transactionId: number;
  paymentId?: string | null;
  transactionType: string;
  signedAmount: number;
  currency?: string | null;
  accountNumber: string;
  accountType: string;
  transactionDate?: string | null;
  status: string;
  description?: string | null;
};

type AutoSettlementMerchantResult = {
  merchantId: string;
  merchantName: string;
  status: string;
  message: string;
  batchId?: number | null;
  executionReference?: string | null;
};

type AutoSettlementRunResult = {
  settlementDate: string;
  feeRate: number;
  autoExecuted: boolean;
  generatedCount: number;
  executedCount: number;
  skippedCount: number;
  failedCount: number;
  runAt?: string | null;
  results: AutoSettlementMerchantResult[];
};

type SettlementAdjustment = {
  id: number;
  merchantId: string;
  merchantName: string;
  originalTransactionId: number;
  originalPaymentId?: string | null;
  adjustmentTransactionId: number;
  originalBatchId: number;
  adjustmentType: string;
  amount: number;
  currency?: string | null;
  status: string;
  reason?: string | null;
  reservedBatchId?: number | null;
  appliedBatchId?: number | null;
  createdAt?: string | null;
};

function formatMoney(value?: number | null, currency = 'USD') {
  return `${Number(value || 0).toLocaleString('en-US', { maximumFractionDigits: 2 })} ${currency}`;
}

function formatDateTime(value?: string | null) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('vi-VN');
}

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function startOfMonthIso() {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10);
}

export default function SettlementsPage() {
  const [merchants, setMerchants] = useState<MerchantItem[]>([]);
  const [selectedMerchantId, setSelectedMerchantId] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [feeRate, setFeeRate] = useState('1.5');
  const [note, setNote] = useState('Daily T+1 settlement batch');
  const [preview, setPreview] = useState<any | null>(null);
  const [selectedBatch, setSelectedBatch] = useState<SettlementBatch | null>(null);
  const [batches, setBatches] = useState<SettlementBatch[]>([]);
  const [adjustments, setAdjustments] = useState<SettlementAdjustment[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [adjustmentPage, setAdjustmentPage] = useState(0);
  const [adjustmentTotalPages, setAdjustmentTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [batchLoading, setBatchLoading] = useState(false);
  const [executingId, setExecutingId] = useState<number | null>(null);
  const [autoSettlementDate, setAutoSettlementDate] = useState('');
  const [autoExecute, setAutoExecute] = useState(true);
  const [autoRunning, setAutoRunning] = useState(false);
  const [autoRunResult, setAutoRunResult] = useState<AutoSettlementRunResult | null>(null);
  const [error, setError] = useState('');

  const selectedMerchant = useMemo(
    () => merchants.find((merchant) => merchant.merchantId === selectedMerchantId) || null,
    [merchants, selectedMerchantId],
  );

  useEffect(() => {
    setFromDate(startOfMonthIso());
    setToDate(todayIso());
    setAutoSettlementDate(todayIso());
  }, []);

  useEffect(() => {
    setLoading(true);
    getMerchants(0, 200)
      .then((data) => {
        const merchantItems = data?.content || [];
        setMerchants(merchantItems);
        if (!selectedMerchantId && merchantItems.length > 0) {
          setSelectedMerchantId(merchantItems[0].merchantId);
        }
      })
      .catch((err) => setError(err.message || 'Không thể tải danh sách merchant'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!selectedMerchantId) return;
    setBatchLoading(true);
    getSettlementBatches(selectedMerchantId, page, 20)
      .then((data) => {
        setBatches(data?.content || []);
        setTotalPages(data?.totalPages || 1);
      })
      .catch((err) => setError(err.message || 'Không thể tải settlement batches'))
      .finally(() => setBatchLoading(false));
  }, [selectedMerchantId, page]);

  useEffect(() => {
    if (!selectedMerchantId) return;
    getSettlementAdjustments(selectedMerchantId, adjustmentPage, 10)
      .then((data) => {
        setAdjustments(data?.content || []);
        setAdjustmentTotalPages(data?.totalPages || 1);
      })
      .catch(() => setAdjustments([]));
  }, [selectedMerchantId, adjustmentPage]);

  async function handlePreview() {
    if (!selectedMerchantId) return;
    setPreviewLoading(true);
    setError('');
    try {
      const data = await getSettlementPreview(selectedMerchantId, fromDate, toDate, Number(feeRate || 0));
      setPreview(data);
    } catch (err: any) {
      setError(err.message || 'Không thể preview settlement');
    } finally {
      setPreviewLoading(false);
    }
  }

  async function handleGenerate() {
    if (!selectedMerchantId) return;
    setPreviewLoading(true);
    setError('');
    try {
      const batch = await generateSettlementBatch(selectedMerchantId, fromDate, toDate, Number(feeRate || 0), note);
      setPreview(batch);
      setSelectedBatch(batch);
      setPage(0);
      const data = await getSettlementBatches(selectedMerchantId, 0, 20);
      setBatches(data?.content || []);
      setTotalPages(data?.totalPages || 1);
    } catch (err: any) {
      setError(err.message || 'Không thể tạo settlement batch');
    } finally {
      setPreviewLoading(false);
    }
  }

  async function handleViewBatch(batchId: number) {
    if (!selectedMerchantId) return;
    try {
      const data = await getSettlementBatchDetail(selectedMerchantId, batchId);
      setSelectedBatch(data);
    } catch (err: any) {
      setError(err.message || 'Không thể tải chi tiết batch');
    }
  }

  async function handleExecute(batch: SettlementBatch) {
    if (!selectedMerchantId) return;
    setExecutingId(batch.id);
    setError('');
    try {
      const data = await executeSettlementBatch(selectedMerchantId, batch.id, `Executed from dashboard for batch #${batch.id}`);
      setSelectedBatch(data);
      const merchantsData = await getMerchants(0, 200);
      const merchantItems = merchantsData?.content || [];
      setMerchants(merchantItems);
      const batchesData = await getSettlementBatches(selectedMerchantId, page, 20);
      setBatches(batchesData?.content || []);
      setTotalPages(batchesData?.totalPages || 1);
    } catch (err: any) {
      setError(err.message || 'Không thể execute settlement batch');
    } finally {
      setExecutingId(null);
    }
  }

  async function handleAutoRun() {
    setAutoRunning(true);
    setError('');
    try {
      const result = await runAutoSettlement(autoSettlementDate, Number(feeRate || 0), autoExecute);
      setAutoRunResult(result);

      const merchantsData = await getMerchants(0, 200);
      setMerchants(merchantsData?.content || []);

      if (selectedMerchantId) {
        const batchesData = await getSettlementBatches(selectedMerchantId, page, 20);
        setBatches(batchesData?.content || []);
        setTotalPages(batchesData?.totalPages || 1);
      }
    } catch (err: any) {
      setError(err.message || 'Không thể chạy auto settlement');
    } finally {
      setAutoRunning(false);
    }
  }

  return (
    <div className="animate-fade-in">
      <div className="page-header">
        <div>
          <h1 className="page-title">
            <FontAwesomeIcon icon={faMoneyCheckDollar} style={{ marginRight: '0.5rem' }} />
            Merchant Settlement
          </h1>
          <p className="page-subtitle">
            Preview, generate va execute batch settlement T+1 cho merchant.
          </p>
        </div>
      </div>

      {error ? <div className="error-banner">{error}</div> : null}

      <div className="stats-grid" style={{ marginBottom: '1rem' }}>
        <div className="stat-card">
          <div className="stat-label">Merchant dang chon</div>
          <div className="stat-value">{selectedMerchant?.name || '—'}</div>
          <div className="stat-note">{selectedMerchant?.merchantId || 'Chua chon merchant'}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Settlement account</div>
          <div className="stat-value" style={{ fontSize: '1rem' }}>{selectedMerchant?.settlementAccountNumber || '—'}</div>
          <div className="stat-note">{selectedMerchant?.settlementAccountName || '—'}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Settlement bank</div>
          <div className="stat-value" style={{ fontSize: '1rem' }}>{selectedMerchant?.settlementBankName || '—'}</div>
          <div className="stat-note">So du hien tai: {formatMoney(selectedMerchant?.settlementAccountBalance)}</div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: '1rem' }}>
        <div style={{ marginBottom: '1rem' }}>
          <h2 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '0.35rem' }}>
            <FontAwesomeIcon icon={faClockRotateLeft} style={{ marginRight: '0.5rem' }} />
            Auto Settlement Run
          </h2>
          <p className="page-subtitle">
            Chạy settlement tự động cho toàn bộ merchant active theo một ngày đối soát.
          </p>
        </div>

        <div className="form-inline" style={{ marginBottom: '1rem' }}>
          <div className="form-field">
            <label className="info-label" style={{ display: 'block', marginBottom: '0.5rem' }}>Settlement date</label>
            <input className="input" type="date" value={autoSettlementDate} onChange={(e) => setAutoSettlementDate(e.target.value)} />
          </div>
          <div className="form-field" style={{ minWidth: '120px', maxWidth: '140px' }}>
            <label className="info-label" style={{ display: 'block', marginBottom: '0.5rem' }}>Fee rate (%)</label>
            <input className="input" type="number" min="0" step="0.1" value={feeRate} onChange={(e) => setFeeRate(e.target.value)} />
          </div>
          <div className="form-field" style={{ display: 'flex', alignItems: 'end' }}>
            <label style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-primary)', fontSize: '0.875rem', paddingBottom: '0.75rem' }}>
              <input type="checkbox" checked={autoExecute} onChange={(e) => setAutoExecute(e.target.checked)} />
              Execute ngay sau khi generate
            </label>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', marginBottom: autoRunResult ? '1rem' : 0 }}>
          <button className="btn-primary" onClick={handleAutoRun} disabled={autoRunning}>
            <FontAwesomeIcon icon={faClockRotateLeft} />
            {autoRunning ? 'Đang chạy auto settlement...' : 'Run Auto Settlement'}
          </button>
        </div>

        {autoRunResult ? (
          <div style={{ display: 'grid', gap: '1rem' }}>
            <div className="stats-grid">
              <div className="stat-card">
                <div className="stat-label">Settlement date</div>
                <div className="stat-value" style={{ fontSize: '1.1rem' }}>{autoRunResult.settlementDate}</div>
                <div className="stat-note">Run at: {formatDateTime(autoRunResult.runAt)}</div>
              </div>
              <div className="stat-card">
                <div className="stat-label">Generated / Executed</div>
                <div className="stat-value" style={{ fontSize: '1.1rem' }}>{autoRunResult.generatedCount} / {autoRunResult.executedCount}</div>
                <div className="stat-note">Execute mode: {autoRunResult.autoExecuted ? 'Generate + Execute' : 'Generate only'}</div>
              </div>
              <div className="stat-card">
                <div className="stat-label">Skipped</div>
                <div className="stat-value" style={{ fontSize: '1.1rem' }}>{autoRunResult.skippedCount}</div>
                <div className="stat-note">Fee rate: {autoRunResult.feeRate}%</div>
              </div>
              <div className="stat-card">
                <div className="stat-label">Failed</div>
                <div className="stat-value" style={{ fontSize: '1.1rem' }}>{autoRunResult.failedCount}</div>
                <div className="stat-note">{autoRunResult.results?.length || 0} merchant processed</div>
              </div>
            </div>

            <div className="table-container">
              <table className="settlement-table">
                <thead>
                  <tr>
                    <th>Merchant</th>
                    <th>Status</th>
                    <th>Message</th>
                    <th>Batch</th>
                    <th>Execution ref</th>
                  </tr>
                </thead>
                <tbody>
                  {autoRunResult.results.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="empty-state">Không có merchant nào được xử lý.</td>
                    </tr>
                  ) : autoRunResult.results.map((item) => (
                    <tr key={`${item.merchantId}-${item.batchId ?? 'none'}`}>
                      <td>
                        <div style={{ fontWeight: 600 }}>{item.merchantName}</div>
                        <div className="page-subtitle">{item.merchantId}</div>
                      </td>
                      <td>
                        <span className={`badge ${item.status === 'EXECUTED' ? 'badge-active' : item.status === 'GENERATED' || item.status === 'SKIPPED' ? 'badge-pending' : 'badge-locked'}`}>
                          {item.status}
                        </span>
                      </td>
                      <td>{item.message || '—'}</td>
                      <td>{item.batchId ? `#${item.batchId}` : '—'}</td>
                      <td style={{ fontFamily: 'monospace', fontSize: '0.8125rem' }}>{item.executionReference || '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : null}
      </div>

      <div className="panel-grid" style={{ marginBottom: '1rem' }}>
        <div className="card">
          <div style={{ marginBottom: '1rem' }}>
            <h2 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '0.35rem' }}>Preview / Generate</h2>
            <p className="page-subtitle">Chon merchant, khoang ngay va phi de tao settlement batch.</p>
          </div>

          <div className="form-inline" style={{ marginBottom: '0.9rem' }}>
            <div className="form-field">
              <label className="info-label" style={{ display: 'block', marginBottom: '0.5rem' }}>Merchant</label>
              <select className="input" value={selectedMerchantId} onChange={(e) => { setSelectedMerchantId(e.target.value); setSelectedBatch(null); }}>
                {loading ? <option>Dang tai...</option> : merchants.map((merchant) => (
                  <option key={merchant.merchantId} value={merchant.merchantId}>
                    {merchant.merchantId} - {merchant.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label className="info-label" style={{ display: 'block', marginBottom: '0.5rem' }}>From date</label>
              <input className="input" type="date" value={fromDate} onChange={(e) => setFromDate(e.target.value)} />
            </div>
            <div className="form-field">
              <label className="info-label" style={{ display: 'block', marginBottom: '0.5rem' }}>To date</label>
              <input className="input" type="date" value={toDate} onChange={(e) => setToDate(e.target.value)} />
            </div>
            <div className="form-field" style={{ minWidth: '120px', maxWidth: '140px' }}>
              <label className="info-label" style={{ display: 'block', marginBottom: '0.5rem' }}>Fee rate (%)</label>
              <input className="input" type="number" min="0" step="0.1" value={feeRate} onChange={(e) => setFeeRate(e.target.value)} />
            </div>
          </div>

          <div style={{ marginBottom: '1rem' }}>
            <label className="info-label" style={{ display: 'block', marginBottom: '0.5rem' }}>Note</label>
            <input className="input" value={note} onChange={(e) => setNote(e.target.value)} />
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
            <button className="btn-secondary" onClick={handlePreview} disabled={previewLoading || !selectedMerchantId}>
              <FontAwesomeIcon icon={faRotate} />
              {previewLoading ? 'Dang preview...' : 'Preview'}
            </button>
            <button className="btn-primary" onClick={handleGenerate} disabled={previewLoading || !selectedMerchantId}>
              <FontAwesomeIcon icon={faWandMagicSparkles} />
              {previewLoading ? 'Dang tao batch...' : 'Generate batch'}
            </button>
          </div>
        </div>

        <div className="card">
          <div style={{ marginBottom: '1rem' }}>
            <h2 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '0.35rem' }}>Preview summary</h2>
            <p className="page-subtitle">Tong quan so tien merchant se nhan.</p>
          </div>
          {preview ? (
            <div className="info-list">
              <div className="info-row"><span className="info-label">Transactions</span><span className="info-value">{preview.transactionCount}</span></div>
              <div className="info-row"><span className="info-label">Pending adjustments</span><span className="info-value">{preview.adjustmentCount || 0}</span></div>
              <div className="info-row"><span className="info-label">Gross amount</span><span className="info-value">{formatMoney(preview.grossAmount, preview.currency)}</span></div>
              <div className="info-row"><span className="info-label">Adjustment amount</span><span className="info-value">{formatMoney(preview.adjustmentAmount, preview.currency)}</span></div>
              <div className="info-row"><span className="info-label">Fee amount</span><span className="info-value">{formatMoney(preview.feeAmount, preview.currency)}</span></div>
              <div className="info-row"><span className="info-label">Net amount</span><span className="info-value">{formatMoney(preview.netAmount, preview.currency)}</span></div>
              <div className="info-row"><span className="info-label">Current balance</span><span className="info-value">{formatMoney(preview.settlementAccountBalance, preview.currency)}</span></div>
              <div className="info-row"><span className="info-label">Projected balance</span><span className="info-value">{formatMoney((preview.settlementAccountBalance || 0) + (preview.netAmount || 0), preview.currency)}</span></div>
            </div>
          ) : (
            <div className="empty-state">Chua co preview. Bam Preview de tinh settlement.</div>
          )}
        </div>
      </div>

      <div className="panel-grid">
        <div className="card">
          <div style={{ marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem' }}>
            <div>
              <h2 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '0.35rem' }}>Settlement batch history</h2>
              <p className="page-subtitle">Danh sach batch da tao cho merchant duoc chon.</p>
            </div>
          </div>

          {batchLoading ? (
            <div className="empty-state">Dang tai batch history...</div>
          ) : (
            <>
              <div className="table-container">
                <table className="settlement-table">
                  <thead>
                    <tr>
                      <th>Batch ID</th>
                      <th>Ky doi soat</th>
                      <th>Gross</th>
                      <th>Fee</th>
                      <th>Net</th>
                      <th>Status</th>
                      <th>Hanh dong</th>
                    </tr>
                  </thead>
                  <tbody>
                    {batches.length === 0 ? (
                      <tr>
                        <td colSpan={7} className="empty-state">Chua co settlement batch nao.</td>
                      </tr>
                    ) : batches.map((batch) => (
                      <tr key={batch.id}>
                        <td>#{batch.id}</td>
                        <td>{batch.fromDate} → {batch.toDate}</td>
                        <td>{formatMoney(batch.grossAmount, batch.currency)}</td>
                        <td>{formatMoney(batch.feeAmount, batch.currency)}</td>
                        <td style={{ fontWeight: 700 }}>{formatMoney(batch.netAmount, batch.currency)}</td>
                        <td>
                          <span className={`badge ${batch.status === 'SETTLED' ? 'badge-active' : batch.status === 'PENDING' ? 'badge-pending' : 'badge-locked'}`}>
                            {batch.status}
                          </span>
                        </td>
                        <td>
                          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                            <button className="btn-secondary" style={{ padding: '0.45rem 0.75rem' }} onClick={() => handleViewBatch(batch.id)}>
                              Chi tiet
                            </button>
                            {batch.status === 'PENDING' ? (
                              <button
                                className="btn-primary"
                                style={{ padding: '0.45rem 0.75rem' }}
                                onClick={() => handleExecute(batch)}
                                disabled={executingId === batch.id}
                              >
                                {executingId === batch.id ? 'Dang execute...' : 'Execute'}
                              </button>
                            ) : null}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
            </>
          )}
        </div>

        <div className="card">
          <div style={{ marginBottom: '1rem' }}>
            <h2 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '0.35rem' }}>Batch detail</h2>
            <p className="page-subtitle">Chon mot batch de xem chi tiet va ket qua execute.</p>
          </div>

          {selectedBatch ? (
            <div style={{ display: 'grid', gap: '1rem' }}>
              <div className="info-list">
                <div className="info-row"><span className="info-label">Batch</span><span className="info-value">#{selectedBatch.id}</span></div>
                <div className="info-row"><span className="info-label">Merchant</span><span className="info-value">{selectedBatch.merchantName}</span></div>
                <div className="info-row"><span className="info-label">Settlement account</span><span className="info-value">{selectedBatch.settlementAccountNumber}</span></div>
                <div className="info-row"><span className="info-label">Current balance</span><span className="info-value">{formatMoney(selectedBatch.settlementAccountBalance, selectedBatch.currency)}</span></div>
                <div className="info-row"><span className="info-label">Status</span><span className="info-value">{selectedBatch.status}</span></div>
                <div className="info-row"><span className="info-label">Adjustment count</span><span className="info-value">{selectedBatch.adjustmentCount || 0}</span></div>
                <div className="info-row"><span className="info-label">Adjustment amount</span><span className="info-value">{formatMoney(selectedBatch.adjustmentAmount, selectedBatch.currency)}</span></div>
                <div className="info-row"><span className="info-label">Execution ref</span><span className="info-value">{selectedBatch.executionReference || '—'}</span></div>
                <div className="info-row"><span className="info-label">Created at</span><span className="info-value">{formatDateTime(selectedBatch.createdAt)}</span></div>
                <div className="info-row"><span className="info-label">Executed at</span><span className="info-value">{formatDateTime(selectedBatch.executedAt)}</span></div>
                <div className="info-row"><span className="info-label">Note</span><span className="info-value">{selectedBatch.note || '—'}</span></div>
              </div>

              <div>
                <div style={{ fontSize: '0.95rem', fontWeight: 700, marginBottom: '0.75rem' }}>Batch items</div>
                {selectedBatch.items && selectedBatch.items.length > 0 ? (
                  <div className="table-container">
                    <table className="settlement-table">
                      <thead>
                        <tr>
                          <th>Txn ID</th>
                          <th>Thoi gian</th>
                          <th>Loai</th>
                          <th>Tai khoan</th>
                          <th>So tien</th>
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {selectedBatch.items.map((item) => (
                          <tr key={item.id}>
                            <td>#{item.transactionId}</td>
                            <td>{formatDateTime(item.transactionDate)}</td>
                            <td>{item.transactionType}</td>
                            <td>
                              <div>{item.accountNumber}</div>
                              <div className="page-subtitle">{item.accountType}</div>
                            </td>
                            <td style={{ fontWeight: 700 }}>{formatMoney(item.signedAmount, item.currency || selectedBatch.currency)}</td>
                            <td>{item.status}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="empty-state">Batch nay chua co item detail.</div>
                )}
              </div>

              <div>
                <div style={{ fontSize: '0.95rem', fontWeight: 700, marginBottom: '0.75rem' }}>Applied adjustments</div>
                {selectedBatch.adjustments && selectedBatch.adjustments.length > 0 ? (
                  <div className="table-container">
                    <table className="settlement-table">
                      <thead>
                        <tr>
                          <th>ID</th>
                          <th>Loai</th>
                          <th>Original txn</th>
                          <th>So tien</th>
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {selectedBatch.adjustments.map((adjustment) => (
                          <tr key={adjustment.id}>
                            <td>#{adjustment.id}</td>
                            <td>{adjustment.adjustmentType}</td>
                            <td>#{adjustment.originalTransactionId}</td>
                            <td style={{ fontWeight: 700 }}>{formatMoney(adjustment.amount, adjustment.currency || selectedBatch.currency)}</td>
                            <td>{adjustment.status}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="empty-state">Batch nay khong co adjustment nao.</div>
                )}
              </div>
            </div>
          ) : (
            <div className="empty-state">Chua chon batch nao.</div>
          )}
        </div>
      </div>

      <div className="card" style={{ marginTop: '1rem' }}>
        <div style={{ marginBottom: '1rem' }}>
          <h2 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '0.35rem' }}>Settlement adjustments</h2>
          <p className="page-subtitle">Refund/Reversal sau khi batch da settle se duoc khau tru vao ky settlement tiep theo.</p>
        </div>
        <div className="table-container">
          <table className="settlement-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Loai</th>
                <th>Original txn</th>
                <th>Original batch</th>
                <th>So tien</th>
                <th>Status</th>
                <th>Applied batch</th>
                <th>Created at</th>
              </tr>
            </thead>
            <tbody>
              {adjustments.length === 0 ? (
                <tr>
                  <td colSpan={8} className="empty-state">Chua co settlement adjustment nao.</td>
                </tr>
              ) : adjustments.map((adjustment) => (
                <tr key={adjustment.id}>
                  <td>#{adjustment.id}</td>
                  <td>{adjustment.adjustmentType}</td>
                  <td>#{adjustment.originalTransactionId}</td>
                  <td>#{adjustment.originalBatchId}</td>
                  <td style={{ fontWeight: 700 }}>{formatMoney(adjustment.amount, adjustment.currency || 'USD')}</td>
                  <td>
                    <span className={`badge ${adjustment.status === 'APPLIED' ? 'badge-active' : adjustment.status === 'RESERVED' ? 'badge-pending' : 'badge-locked'}`}>
                      {adjustment.status}
                    </span>
                  </td>
                  <td>{adjustment.appliedBatchId ? `#${adjustment.appliedBatchId}` : adjustment.reservedBatchId ? `Reserved #${adjustment.reservedBatchId}` : '—'}</td>
                  <td>{formatDateTime(adjustment.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <Pagination page={adjustmentPage} totalPages={adjustmentTotalPages} onPageChange={setAdjustmentPage} />
      </div>
    </div>
  );
}
