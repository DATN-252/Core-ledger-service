'use client';
import { useEffect, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSun, faMoon } from '@fortawesome/free-solid-svg-icons';

export default function ThemeToggle({ className }: { className?: string }) {
    const [theme, setTheme] = useState<'dark' | 'light'>('dark');

    useEffect(() => {
        // Read theme from document attributes (set by SSR/preloader inline script) or localStorage
        const activeTheme = (document.documentElement.getAttribute('data-theme') || 'dark') as 'dark' | 'light';
        setTheme(activeTheme);
    }, []);

    const toggleTheme = () => {
        const nextTheme = theme === 'dark' ? 'light' : 'dark';
        setTheme(nextTheme);
        localStorage.setItem('theme', nextTheme);
        document.documentElement.setAttribute('data-theme', nextTheme);
        // Trigger a custom event to notify other components if needed
        window.dispatchEvent(new Event('themechange'));
    };

    return (
        <button
            onClick={toggleTheme}
            className={`icon-btn ${className || ''}`}
            aria-label="Toggle theme"
            style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                transition: 'all 0.2s ease',
                background: 'var(--bg-card-hover)',
                border: '1px solid var(--border)',
                color: 'var(--accent)',
                cursor: 'pointer',
                fontSize: '1rem',
                boxShadow: '0 2px 8px rgba(0, 0, 0, 0.05)',
            }}
            onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'scale(1.05)';
                e.currentTarget.style.borderColor = 'var(--accent)';
            }}
            onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'scale(1)';
                e.currentTarget.style.borderColor = 'var(--border)';
            }}
        >
            <FontAwesomeIcon 
                icon={theme === 'dark' ? faSun : faMoon} 
                style={{ 
                    transition: 'transform 0.3s ease',
                    transform: theme === 'dark' ? 'rotate(0deg)' : 'rotate(360deg)'
                }} 
            />
        </button>
    );
}
