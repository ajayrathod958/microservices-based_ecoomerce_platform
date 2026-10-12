import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ordersApi } from '../api/orders';
import { EmptyState, ErrorState, Loading } from '../components/feedback';
import Pagination from '../components/Pagination';
import StatusBadge from '../components/StatusBadge';
import { useFetch } from '../hooks/useFetch';
import { dateTime, money, shortId } from '../lib/format';

export default function Orders() {
  const [page, setPage] = useState(0);
  const orders = useFetch(() => ordersApi.list(page, 10), [page]);

  if (orders.loading && !orders.data) return <Loading label="Loading your orders" />;
  if (orders.error && !orders.data) return <ErrorState message="We couldn't load your orders." onRetry={orders.reload} />;
  if (orders.data.content.length === 0) {
    return <EmptyState title="No orders yet" text="Orders you place will show up here." actionLabel="Start shopping" actionTo="/" />;
  }

  return (
    <div>
      <h1 className="text-3xl font-bold">Your orders</h1>
      <ul className="panel mt-6 divide-y divide-line">
        {orders.data.content.map((o) => (
          <li key={o.id}>
            <Link to={`/orders/${o.id}`} className="flex flex-wrap items-center justify-between gap-3 p-4 hover:bg-chalk">
              <div>
                <p className="font-semibold">{shortId(o.id)}</p>
                <p className="text-sm text-moss">
                  {dateTime(o.createdAt)}, {o.itemCount} {o.itemCount === 1 ? 'item' : 'items'}
                </p>
              </div>
              <div className="flex items-center gap-4">
                <StatusBadge status={o.status} />
                <span className="font-semibold">{money(o.totalAmount, o.currency)}</span>
              </div>
            </Link>
          </li>
        ))}
      </ul>
      <Pagination page={orders.data.page} totalPages={orders.data.totalPages} onChange={setPage} />
    </div>
  );
}
