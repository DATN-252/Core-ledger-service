const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8083';

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

// ─── Loans ───────────────────────────────────────────────────────────────────
export async function getLoan(loanId: string) {
  return request<any>(`/loans/${loanId}`);
}

export async function getAllLoans() {
  return request<any[]>('/loans');
}

export async function createLoan(data: any) {
  return request<any>('/loans', { method: 'POST', body: JSON.stringify(data) });
}

export async function loanCommand(loanId: string, command: 'activate' | 'lock' | 'unlock') {
  return request<any>(`/loans/${loanId}?command=${command}`, { method: 'POST', body: '{}' });
}

// ─── Savings Accounts ────────────────────────────────────────────────────────
export async function getSavingsAccount(id: string) {
  return request<any>(`/savingsaccounts/${id}`);
}

export async function getAllSavingsAccounts() {
  return request<any[]>('/savingsaccounts');
}

export async function createSavingsAccount(data: any) {
  return request<any>('/savingsaccounts', { method: 'POST', body: JSON.stringify(data) });
}

export async function savingsCommand(id: string, command: 'activate' | 'lock') {
  return request<any>(`/savingsaccounts/${id}?command=${command}`, { method: 'POST', body: '{}' });
}

// ─── Transactions ─────────────────────────────────────────────────────────────
export async function getTransactions(accountId?: string) {
  const path = accountId ? `/transactions?accountId=${accountId}` : '/transactions';
  return request<any[]>(path);
}
