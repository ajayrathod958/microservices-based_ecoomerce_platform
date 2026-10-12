import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ConfirmDialog } from '../components/Modal';
import { EmptyState, Loading } from '../components/feedback';
import { useCart } from '../context/CartContext';
import { money } from '../lib/format';

export default function Cart() {
  const { cart, loading, busy, setQuantity, remove, clear } = useCart();
  const [confirmClear, setConfirmClear] = useState(false);

  if (loading && cart.items.length === 0) return <Loading label="Loading your cart" />;
  if (cart.items.length === 0) {
    return <EmptyState title="Your cart is empty" text="Add something from the shop and it will show up here." actionLabel="Browse products" actionTo="/" />;
  }

  return (
    <div>
      <div className="flex items-center justify-between gap-4">
        <h1 className="text-3xl font-bold">Your cart</h1>
        <button type="button" className="btn btn-ghost text-rust" onClick={() => setConfirmClear(true)}>
          Empty cart
        </button>
      </div>

      <div className="mt-6 grid gap-8 lg:grid-cols-[1fr_20rem]">
        <ul className="panel divide-y divide-line">
          {cart.items.map((item) => (
            <li key={item.productId} className="flex gap-4 p-4">
              <img src={item.imageUrl || undefined} alt="" className="h-20 w-20 shrink-0 rounded bg-chalk-deep object-cover" />
              <div className="flex flex-1 flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <Link to={`/products/${item.productId}`} className="font-semibold hover:underline">
                    {item.name}
                  </Link>
                  <p className="text-sm text-moss">{money(item.unitPrice)} each</p>
                </div>
                <div className="flex items-center gap-3">
                  <div className="flex items-center rounded-md border border-line">
                    <button type="button" className="px-3 py-1.5" aria-label={`Decrease quantity of ${item.name}`} disabled={busy || item.quantity <= 1} onClick={() => setQuantity(item.productId, item.quantity - 1)}>
                      −
                    </button>
                    <span className="min-w-[2rem] text-center text-sm" aria-live="polite">
                      {item.quantity}
                    </span>
                    <button type="button" className="px-3 py-1.5" aria-label={`Increase quantity of ${item.name}`} disabled={busy} onClick={() => setQuantity(item.productId, item.quantity + 1)}>
                      +
                    </button>
                  </div>
                  <p className="w-24 text-right font-semibold">{money(item.lineTotal)}</p>
                  <button type="button" className="text-sm text-moss underline hover:text-rust" disabled={busy} onClick={() => remove(item.productId)}>
                    Remove
                  </button>
                </div>
              </div>
            </li>
          ))}
        </ul>

        <aside className="panel h-fit p-5">
          <h2 className="text-lg font-semibold">Order summary</h2>
          <dl className="mt-4 space-y-2 text-sm">
            <div className="flex justify-between">
              <dt className="text-moss">Items</dt>
              <dd>{cart.totalItems}</dd>
            </div>
            <div className="flex items-center justify-between border-t border-line pt-3">
              <dt className="font-semibold">Subtotal</dt>
              <dd className="price-tag text-lg">{money(cart.subtotal)}</dd>
            </div>
          </dl>
          <p className="mt-3 text-xs text-moss">Prices are confirmed again at checkout.</p>
          <Link to="/checkout" className="btn btn-tag mt-5 w-full">
            Go to checkout
          </Link>
        </aside>
      </div>

      {confirmClear && (
        <ConfirmDialog
          title="Empty your cart?"
          message="All items will be removed from your cart."
          confirmLabel="Empty cart"
          busy={busy}
          onCancel={() => setConfirmClear(false)}
          onConfirm={async () => {
            await clear();
            setConfirmClear(false);
          }}
        />
      )}
    </div>
  );
}
