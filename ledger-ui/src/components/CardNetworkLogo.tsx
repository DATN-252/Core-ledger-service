const NETWORK_LOGO_MAP: Record<string, string> = {
  VISA: '/card-networks/visa.png',
  MASTERCARD: '/card-networks/mastercard.png',
  MASTER: '/card-networks/mastercard.png',
  NAPAS: '/card-networks/napas.png',
  AMEX: '/card-networks/amex.png',
  JCB: '/card-networks/jcb.png',
};

export default function CardNetworkLogo({
  network,
  width = 54,
  height = 20,
}: {
  network?: string;
  width?: number;
  height?: number;
}) {
  const key = (network || 'UNKNOWN').toUpperCase();
  const src = NETWORK_LOGO_MAP[key];

  if (!src) {
    return (
      <span
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          minWidth: `${width}px`,
          height: `${height}px`,
          padding: '0 0.4rem',
          borderRadius: '6px',
          border: '1px solid var(--border)',
          background: 'rgba(255, 255, 255, 0.03)',
          color: 'var(--text-secondary)',
          fontSize: '0.7rem',
          fontWeight: 700,
          letterSpacing: '0.04em',
        }}
      >
        {key}
      </span>
    );
  }

  return (
    <img
      src={src}
      alt={key}
      title={key}
      style={{
        width: `${width}px`,
        height: `${height}px`,
        objectFit: 'contain',
        display: 'block',
      }}
    />
  );
}
