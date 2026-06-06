'use client';

import { useEffect, useState } from 'react';
import AppModal from '@/components/AppModal';

export default function BrowserAlertBridge() {
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    const originalAlert = window.alert;

    window.alert = (value?: unknown) => {
      if (typeof value === 'string') {
        setMessage(value);
        return;
      }
      setMessage(value != null ? String(value) : '');
    };

    return () => {
      window.alert = originalAlert;
    };
  }, []);

  return (
    <AppModal
      open={message != null}
      title="Thông báo"
      onClose={() => setMessage(null)}
      footer={<button className="btn-primary" onClick={() => setMessage(null)}>Đã hiểu</button>}
    >
      <p style={{ color: 'var(--text-secondary)', lineHeight: 1.7 }}>{message}</p>
    </AppModal>
  );
}
