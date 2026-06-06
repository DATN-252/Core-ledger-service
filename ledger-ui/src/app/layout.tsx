import type { Metadata } from 'next';
import './globals.css';
import '@fortawesome/fontawesome-svg-core/styles.css';
import { config } from '@fortawesome/fontawesome-svg-core';
import BrowserAlertBridge from '@/components/BrowserAlertBridge';
config.autoAddCss = false;

export const metadata: Metadata = {
  title: 'BkBank Ledger Admin',
  description: 'Core Ledger Management System',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi" suppressHydrationWarning>
      <head>
        <script
          dangerouslySetInnerHTML={{
            __html: `
              (function() {
                var theme = localStorage.getItem('theme') || 'dark';
                document.documentElement.setAttribute('data-theme', theme);
              })();
            `,
          }}
        />
      </head>
      <body suppressHydrationWarning>
        {children}
        <BrowserAlertBridge />
      </body>
    </html>
  );
}
