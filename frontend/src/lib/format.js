export function money(amount, currency = 'USD') {
  if (amount === null || amount === undefined || amount === '') return '-';
  try {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(Number(amount));
  } catch {
    return `${Number(amount).toFixed(2)} ${currency}`;
  }
}

export function dateTime(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '' : d.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

export function shortId(id) {
  return id ? `#${String(id).slice(0, 8).toUpperCase()}` : '';
}
