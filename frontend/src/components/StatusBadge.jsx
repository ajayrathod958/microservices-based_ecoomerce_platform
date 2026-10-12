const STYLES = {
  PENDING: ['Processing', 'bg-chalk-deep text-ink'],
  STOCK_RESERVED: ['Processing', 'bg-chalk-deep text-ink'],
  CONFIRMED: ['Confirmed', 'bg-leaf text-white'],
  CANCELLED: ['Cancelled', 'bg-rust text-white'],
};

export default function StatusBadge({ status }) {
  const [label, classes] = STYLES[status] || [status, 'bg-chalk-deep text-ink'];
  return <span className={`inline-block rounded px-2 py-0.5 text-xs font-semibold ${classes}`}>{label}</span>;
}
