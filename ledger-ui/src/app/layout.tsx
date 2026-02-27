import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'BkBank Ledger Admin',
  description: 'Core Ledger Management System',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi">
      <body>{children}</body>
    </html>
  );
}
