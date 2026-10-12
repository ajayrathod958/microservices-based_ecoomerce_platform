import { money } from '../lib/format';

export default function PriceTag({ amount, currency, className = '' }) {
  return <span className={`price-tag ${className}`}>{money(amount, currency)}</span>;
}
