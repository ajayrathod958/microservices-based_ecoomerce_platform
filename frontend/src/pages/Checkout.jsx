import { useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ordersApi } from '../api/orders';
import Field from '../components/Field';
import { EmptyState, Loading, Spinner } from '../components/feedback';
import { useCart } from '../context/CartContext';
import { useToast } from '../context/ToastContext';
import { getErrorMessage, getFieldErrors } from '../lib/errors';
import { money } from '../lib/format';
import { uuid } from '../lib/uuid';

// The mock payment gateway decides the outcome from the token, so the shopper can try each path.
const CARDS = [
  { token: 'tok_success', label: 'Visa ending 4242 (payment succeeds)' },
  { token: 'tok_decline', label: 'Visa ending 0002 (card declined)' },
  { token: 'tok_insufficient_funds', label: 'Visa ending 9995 (insufficient funds)' },
  { token: 'tok_gateway_error', label: 'Visa ending 0119 (gateway error)' },
];

function validate(a) {
  const e = {};
  if (!a.fullName.trim()) e.fullName = 'Enter the recipient name.';
  if (!a.line1.trim()) e.line1 = 'Enter the street address.';
  if (!a.city.trim()) e.city = 'Enter the city.';
  if (!a.postalCode.trim()) e.postalCode = 'Enter the postal code.';
  if (!/^[A-Za-z]{2}$/.test(a.country.trim())) e.country = 'Use a 2-letter country code, like US.';
  return e;
}

export default function Checkout() {
  const { cart, loading, refresh } = useCart();
  const toast = useToast();
  const navigate = useNavigate();
  // One key per visit to this page: retries and double clicks resolve to the same order.
  const idempotencyKey = useRef(uuid());
  const [address, setAddress] = useState({ fullName: '', line1: '', line2: '', city: '', postalCode: '', country: 'US' });
  const [token, setToken] = useState(CARDS[0].token);
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);

  if (loading && cart.items.length === 0) return <Loading label="Loading your cart" />;
  if (cart.items.length === 0 && !busy) {
    return <EmptyState title="Nothing to check out" text="Your cart is empty." actionLabel="Browse products" actionTo="/" />;
  }

  const set = (k) => (e) => setAddress((a) => ({ ...a, [k]: e.target.value }));
  const submit = async (e) => {
    e.preventDefault();
    const v = validate(address);
    setErrors(v);
    setFormError('');
    if (Object.keys(v).length) return;
    setBusy(true);
    try {
      const shippingAddress = { ...address, country: address.country.trim().toUpperCase() };
      if (!shippingAddress.line2.trim()) delete shippingAddress.line2;
      const order = await ordersApi.checkout({ shippingAddress, paymentToken: token }, idempotencyKey.current);
      toast.success('Order placed. We are confirming stock and payment.');
      navigate(`/orders/${order.id}`, { replace: true });
      refresh(); // the server emptied the cart
    } catch (err) {
      const fields = getFieldErrors(err);
      setErrors(fields);
      setFormError(Object.keys(fields).length ? 'Fix the highlighted fields.' : getErrorMessage(err, 'We could not place your order.'));
      setBusy(false);
    }
  };

  return (
    <div>
      <h1 className="text-3xl font-bold">Checkout</h1>
      <form onSubmit={submit} noValidate className="mt-6 grid gap-8 lg:grid-cols-[1fr_20rem]">
        <div className="space-y-8">
          {formError && (
            <p className="rounded-md bg-rust-soft p-3 text-sm text-rust" role="alert">
              {formError}
            </p>
          )}
          <section className="panel space-y-4 p-5">
            <h2 className="text-lg font-semibold">Shipping address</h2>
            <Field label="Full name" autoComplete="name" value={address.fullName} onChange={set('fullName')} error={errors.fullName} />
            <Field label="Address" autoComplete="address-line1" value={address.line1} onChange={set('line1')} error={errors.line1} />
            <Field label="Apartment, suite (optional)" autoComplete="address-line2" value={address.line2} onChange={set('line2')} error={errors.line2} />
            <div className="grid gap-4 sm:grid-cols-3">
              <Field label="City" autoComplete="address-level2" value={address.city} onChange={set('city')} error={errors.city} />
              <Field label="Postal code" autoComplete="postal-code" value={address.postalCode} onChange={set('postalCode')} error={errors.postalCode} />
              <Field label="Country" autoComplete="country" maxLength={2} value={address.country} onChange={set('country')} error={errors.country} />
            </div>
          </section>

          <section className="panel space-y-3 p-5">
            <h2 className="text-lg font-semibold">Payment</h2>
            <p className="text-sm text-moss">This is a demo store. Pick a test card to see how each payment result looks.</p>
            <Field as="select" label="Test card" value={token} onChange={(e) => setToken(e.target.value)}>
              {CARDS.map((c) => (
                <option key={c.token} value={c.token}>
                  {c.label}
                </option>
              ))}
            </Field>
          </section>
        </div>

        <aside className="panel h-fit p-5">
          <h2 className="text-lg font-semibold">Order summary</h2>
          <ul className="mt-4 space-y-2 text-sm">
            {cart.items.map((i) => (
              <li key={i.productId} className="flex justify-between gap-3">
                <span>
                  {i.quantity} × {i.name}
                </span>
                <span className="shrink-0">{money(i.lineTotal)}</span>
              </li>
            ))}
          </ul>
          <div className="mt-4 flex items-center justify-between border-t border-line pt-4">
            <span className="font-semibold">Total</span>
            <span className="price-tag text-lg">{money(cart.subtotal)}</span>
          </div>
          <button type="submit" className="btn btn-tag mt-5 w-full" disabled={busy}>
            {busy && <Spinner className="h-4 w-4" />} Place order
          </button>
          <Link to="/cart" className="mt-3 block text-center text-sm text-moss hover:text-ink">
            Back to cart
          </Link>
        </aside>
      </form>
    </div>
  );
}
