const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8083';
const CMS_API_URL = process.env.NEXT_PUBLIC_CMS_API_URL || 'http://localhost:8082/api';

function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('ledger_token');
}

export function getRole(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('ledger_role');
}

export function isLoggedIn(): boolean {
  return !!getToken();
}

export function logout() {
  localStorage.removeItem('ledger_token');
  localStorage.removeItem('ledger_role');
  localStorage.removeItem('ledger_user');
  window.location.href = '/login';
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const res = await fetch(`${API_URL}${path}`, { ...options, headers });

  if (res.status === 401) {
    logout();
    throw new Error('Session expired');
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message || `HTTP ${res.status}`);
  }

  const json = await res.json();
  // Auto-unwrap the new standard ApiResponse format (code 1000)
  if (json && json.code === 1000 && 'result' in json) {
    return json.result;
  }
  return json;
}

async function cmsRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const res = await fetch(`${CMS_API_URL}${path}`, { ...options, headers });

  if (res.status === 401) {
    logout();
    throw new Error('Session expired');
  }

  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message || `HTTP ${res.status}`);
  }

  return res.json();
}

// ─── Auth ────────────────────────────────────────────────────────────────────
export async function login(username: string, password: string) {
  const data = await request<{
    token: string;
    username: string;
    fullName: string;
    role: string;
    expiresIn: number;
  }>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  localStorage.setItem('ledger_token', data.token);
  localStorage.setItem('ledger_role', data.role);
  localStorage.setItem('ledger_user', JSON.stringify(data));
  return data;
}

export function getUser() {
  if (typeof window === 'undefined') return null;
  const raw = localStorage.getItem('ledger_user');
  return raw ? JSON.parse(raw) : null;
}

export async function registerCustomer(clientId: string, password: string) {
  return request<any>('/auth/register-customer', {
    method: 'POST',
    body: JSON.stringify({ clientId, password })
  });
}

// ─── Loans ───────────────────────────────────────────────────────────────────
export async function getLoan(loanId: string) {
  return request<any>(`/loans/${loanId}`);
}

export async function getAllLoans(page = 0, size = 10) {
  return request<any>(`/loans?page=${page}&size=${size}`);
}

export async function createLoan(data: any) {
  return request<any>('/loans', { method: 'POST', body: JSON.stringify(data) });
}

export async function loanCommand(loanId: string, command: 'activate' | 'lock' | 'unlock') {
  return request<any>(`/loans/${loanId}?command=${command}`, { method: 'POST', body: '{}' });
}

export async function getLoanMonthlyStatements(loanId: string) {
  return request<any[]>(`/loans/${loanId}/monthly-statements`);
}

export async function getLoanMonthlyStatementDetail(loanId: string, billingDate: string) {
  return request<any>(`/loans/${loanId}/monthly-statements/${billingDate}`);
}

export async function generateLoanMonthlyStatement(loanId: string, billingDate: string) {
  return request<any>(`/loans/${loanId}/monthly-statements/generate?billingDate=${encodeURIComponent(billingDate)}`, {
    method: 'POST',
    body: '{}',
  });
}

// ─── Savings Accounts ────────────────────────────────────────────────────────
export async function getSavingsAccount(id: string) {
  return request<any>(`/savingsaccounts/${id}`);
}

export async function getAllSavingsAccounts(page = 0, size = 10) {
  return request<any>(`/savingsaccounts?page=${page}&size=${size}`);
}

export async function createSavingsAccount(data: any) {
  return request<any>('/savingsaccounts', { method: 'POST', body: JSON.stringify(data) });
}

export async function savingsCommand(id: string, command: 'activate' | 'lock') {
  return request<any>(`/savingsaccounts/${id}?command=${command}`, { method: 'POST', body: '{}' });
}

// ─── Transactions ─────────────────────────────────────────────────────────────
export async function getTransactions(accountId?: string, page = 0, size = 50) {
  const path = accountId
    ? `/transactions?accountId=${accountId}&page=${page}&size=${size}`
    : `/transactions?page=${page}&size=${size}`;
  return request<any>(path);
}

export async function getTransactionDetail(id: string | number) {
  return request<any>(`/transactions/${id}`);
}

export async function getDashboardSummary() {
  return request<any>('/dashboard/summary');
}

// ─── Merchants / Settlements ─────────────────────────────────────────────────
export async function getMerchants(page = 0, size = 100) {
  return request<any>(`/merchants?page=${page}&size=${size}`);
}

export async function getSettlementPreview(merchantId: string, fromDate: string, toDate: string, feeRate = 0) {
  return request<any>(
    `/merchants/${merchantId}/settlement/preview?fromDate=${encodeURIComponent(fromDate)}&toDate=${encodeURIComponent(toDate)}&feeRate=${feeRate}`
  );
}

export async function generateSettlementBatch(
  merchantId: string,
  fromDate: string,
  toDate: string,
  feeRate = 0,
  note?: string,
) {
  return request<any>(
    `/merchants/${merchantId}/settlements/generate?fromDate=${encodeURIComponent(fromDate)}&toDate=${encodeURIComponent(toDate)}&feeRate=${feeRate}`,
    {
      method: 'POST',
      body: JSON.stringify({ note: note || '' }),
    },
  );
}

export async function executeSettlementBatch(merchantId: string, batchId: number, note?: string) {
  return request<any>(`/merchants/${merchantId}/settlements/${batchId}/execute`, {
    method: 'POST',
    body: JSON.stringify({ note: note || '' }),
  });
}

export async function getSettlementBatches(merchantId: string, page = 0, size = 20) {
  return request<any>(`/merchants/${merchantId}/settlements?page=${page}&size=${size}`);
}

export async function getSettlementBatchDetail(merchantId: string, batchId: number) {
  return request<any>(`/merchants/${merchantId}/settlements/${batchId}`);
}

// ─── Clients ──────────────────────────────────────────────────────────────────
export async function getAllClients(page = 0, size = 10) {
  return request<any>(`/clients?page=${page}&size=${size}`);
}

export async function getClient(clientId: string) {
  return request<any>(`/clients/${clientId}`);
}

export async function getClientAccounts(clientId: string) {
  return request<any>(`/clients/${clientId}/accounts`);
}

export async function createClient(data: any) {
  return request<any>('/clients', { method: 'POST', body: JSON.stringify(data) });
}

// ─── Cards (CMS) ──────────────────────────────────────────────────────────────
export async function getCreditCards(clientId?: string) {
  const path = clientId ? `/cards/client/${clientId}` : `/cards`;
  return cmsRequest<any[]>(path);
}

export async function issueDebitCard(data: { pan: string, cvv: string, expirationDate: string, accountId: string, cardholderName: string, network?: string }) {
  const params = new URLSearchParams(data as any);
  return cmsRequest<any>(`/cards/issue?${params.toString()}`, { method: 'POST' });
}

export async function issueCreditCard(data: { pan: string, cvv: string, expirationDate: string, creditLimit: number, loanAccountId: string, cardholderName: string, network?: string }) {
  const params = new URLSearchParams(data as any);
  return cmsRequest<any>(`/cards/issue/credit?${params.toString()}`, { method: 'POST' });
}

export async function changeCardStatus(cardNumber: string, status: string) {
  return cmsRequest<any>(`/cards/${cardNumber}/status?status=${status}`, { method: 'PUT' });
}
