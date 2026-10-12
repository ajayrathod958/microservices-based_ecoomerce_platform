export const TERMINAL = ['CONFIRMED', 'CANCELLED'];
export const isTerminal = (status) => TERMINAL.includes(status);

/**
 * Turns an order into the steps shown on the tracking page.
 * Events from different services can arrive out of order (e.g. payment before "stock reserved"), so every
 * step before the furthest one reached counts as done even if it has no timestamp of its own.
 */
export function buildTimeline(order) {
  const entries = {};
  (order?.history || []).forEach((h) => {
    if (!entries[h.status]) entries[h.status] = h;
  });
  const cancelled = order?.status === 'CANCELLED';
  const steps = [
    { key: 'PENDING', label: 'Order placed' },
    { key: 'STOCK_RESERVED', label: 'Items reserved' },
    cancelled ? { key: 'CANCELLED', label: 'Order cancelled' } : { key: 'CONFIRMED', label: 'Order confirmed' },
  ];
  let lastReached = -1;
  steps.forEach((s, i) => {
    if (entries[s.key]) lastReached = i;
  });
  return steps.map((s, i) => ({
    ...s,
    done: i <= lastReached,
    at: entries[s.key]?.at,
    note: entries[s.key]?.note,
    danger: s.key === 'CANCELLED',
  }));
}
