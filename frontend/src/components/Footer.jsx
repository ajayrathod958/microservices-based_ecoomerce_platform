import { Link } from 'react-router-dom';
import { MailIcon, RefreshIcon, ShieldIcon, TruckIcon } from './icons';

const PROMISES = [
  { Icon: ShieldIcon, title: 'Secure checkout', text: 'Prices are re-checked on the server' },
  { Icon: RefreshIcon, title: 'Live stock', text: 'Items are reserved when you order' },
  { Icon: TruckIcon, title: 'Order tracking', text: 'Follow every step to confirmation' },
  { Icon: MailIcon, title: 'Email updates', text: 'A message when your order is confirmed' },
];

export function PromiseStrip() {
  return (
    <ul className="grid grid-cols-2 gap-4 rounded-xl border border-line p-5 lg:grid-cols-4">
      {PROMISES.map(({ Icon, title, text }) => (
        <li key={title} className="flex items-center gap-3">
          <Icon className="h-7 w-7 shrink-0" />
          <div>
            <p className="text-sm font-semibold">{title}</p>
            <p className="text-xs text-moss">{text}</p>
          </div>
        </li>
      ))}
    </ul>
  );
}

export default function Footer() {
  return (
    <footer className="mt-16 border-t border-line bg-chalk-deep">
      <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-4 px-4 py-8 text-sm text-moss">
        <p>MicroShop is a demo store built on microservices. No real payments are taken.</p>
        <nav aria-label="Footer" className="flex gap-5">
          <Link to="/shop" className="hover:text-ink">Shop</Link>
          <Link to="/orders" className="hover:text-ink">Orders</Link>
        </nav>
      </div>
    </footer>
  );
}
