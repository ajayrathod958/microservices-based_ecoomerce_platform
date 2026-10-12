import { useEffect, useRef } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ordersApi } from '../api/orders';
import { ErrorState, Loading, Spinner } from '../components/feedback';
import OrderTimeline from '../components/OrderTimeline';
import StatusBadge from '../components/StatusBadge';
import { useFetch } from '../hooks/useFetch';
import { dateTime, money, shortId } from '../lib/format';
import { isTerminal } from '../lib/timeline';

const POLL_MS = 2500;
const MAX_POLLS = 60; // stop after ~2.5 minutes; the page still loads fresh data on reload

export default function OrderTracking() {
  const { id } = useParams();
  const { data: order, loading, error, reload } = useFetch(() => ordersApi.get(id), [id]);
  const polls = useRef(0);

  // The saga finishes asynchronously, so keep refreshing until the order reaches a final state.
  useEffect(() => {
    polls.current = 0;
  }, [id]);
  useEffect(() => {
    if (!order || isTerminal(order.status) || polls.current >= MAX_POLLS) return undefined;
    const t = setTimeout(() => {
      polls.current += 1;
      reload();
    }, POLL_MS);
    return () => clearTimeout(t);
  }, [order, reload]);

  if (loading && !order) return <Loading label="Loading your order" />;
  if (error && !order) {
    const notFound = error?.response?.status === 404;
    return <ErrorState message={notFound ? "We couldn't find that order." : "We couldn't load this order."} onRetry={notFound ? undefined : reload} />;
  }

  const waiting = !isTerminal(order.status);
  return (
    <div>
      <Link to="/orders" className="text-sm text-moss hover:text-ink">
        All orders
      </Link>
      <div className="mt-2 flex flex-wrap items-center gap-3">
        <h1 className="text-3xl font-bold">Order {shortId(order.id)}</h1>
        <StatusBadge status={order.status} />
      </div>
      <p className="mt-1 text-sm text-moss">Placed {dateTime(order.createdAt)}</p>

      <div className="mt-8 grid gap-8 lg:grid-cols-2">
        <section className="panel p-5">
          <h2 className="mb-5 text-lg font-semibold">Progress</h2>
          <OrderTimeline order={order} />
          {waiting && (
            <p className="mt-6 flex items-center gap-2 text-sm text-moss" role="status">
              <Spinner className="h-4 w-4" /> Checking for updates
            </p>
          )}
          {order.status === 'CANCELLED' && order.failureReason && (
            <p className="mt-6 rounded-md bg-rust-soft p-3 text-sm text-rust" role="alert">
              {order.failureReason}
            </p>
          )}
        </section>

        <div className="space-y-8">
          <section className="panel p-5">
            <h2 className="mb-3 text-lg font-semibold">Items</h2>
            <ul className="divide-y divide-line text-sm">
              {order.items.map((i) => (
                <li key={i.productId} className="flex justify-between gap-3 py-2">
                  <span>
                    {i.quantity} × {i.productName}
                  </span>
                  <span>{money(i.lineTotal, order.currency)}</span>
                </li>
              ))}
            </ul>
            <div className="mt-3 flex items-center justify-between border-t border-line pt-3">
              <span className="font-semibold">Total</span>
              <span className="price-tag text-lg">{money(order.totalAmount, order.currency)}</span>
            </div>
          </section>

          {order.shippingAddress && (
            <section className="panel p-5 text-sm">
              <h2 className="mb-3 text-lg font-semibold">Shipping to</h2>
              <address className="not-italic leading-relaxed">
                {order.shippingAddress.fullName}
                <br />
                {order.shippingAddress.line1}
                {order.shippingAddress.line2 && (
                  <>
                    <br />
                    {order.shippingAddress.line2}
                  </>
                )}
                <br />
                {order.shippingAddress.city} {order.shippingAddress.postalCode}, {order.shippingAddress.country}
              </address>
            </section>
          )}
        </div>
      </div>
    </div>
  );
}
