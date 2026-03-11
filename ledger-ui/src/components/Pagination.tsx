export function Pagination({
    page,
    totalPages,
    onPageChange
}: {
    page: number;
    totalPages: number;
    onPageChange: (page: number) => void
}) {
    if (totalPages <= 1) return null;

    return (
        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1.5rem', justifyContent: 'center', alignItems: 'center' }}>
            <button
                className="btn-secondary"
                disabled={page === 0}
                onClick={() => onPageChange(page - 1)}
                style={{ padding: '0.4rem 0.8rem', fontSize: '0.875rem' }}
            >
                Trang trước
            </button>
            <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                Trang {page + 1} / {totalPages}
            </span>
            <button
                className="btn-secondary"
                disabled={page >= totalPages - 1}
                onClick={() => onPageChange(page + 1)}
                style={{ padding: '0.4rem 0.8rem', fontSize: '0.875rem' }}
            >
                Trang sau
            </button>
        </div>
    );
}
