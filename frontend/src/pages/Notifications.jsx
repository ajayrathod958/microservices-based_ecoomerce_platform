import { useState } from 'react';
import { Link } from 'react-router-dom';
import { notificationsApi } from '../api/notifications';
import { EmptyState, ErrorState, Loading } from '../components/feedback';
import Pagination from '../components/Pagination';
import { useToast } from '../context/ToastContext';
import { useFetch } from '../hooks/useFetch';
import { getErrorMessage } from '../lib/errors';
import { dateTime } from '../lib/format';

export default function Notifications() {
  const toast = useToast();
  const [page, setPage] = useState(0);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const list = useFetch(() => notificationsApi.list(page, unreadOnly), [page, unreadOnly]);

  const act = async (fn) => {
    try {
      await fn();
      list.reload();
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  if (list.loading && !list.data) return <Loading label="Loading notifications" />;
  if (list.error && !list.data) return <ErrorState message="We couldn't load notifications." onRetry={list.reload} />;

  return (
    <div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-3xl font-bold">Notifications</h1>
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={unreadOnly}
              onChange={(e) => {
                setPage(0);
                setUnreadOnly(e.target.checked);
              }}
            />
            Unread only
          </label>
          <button type="button" className="btn btn-secondary" onClick={() => act(notificationsApi.markAllRead)}>
            Mark all as read
          </button>
        </div>
      </div>

      {list.data.content.length === 0 ? (
        <div className="mt-6">
          <EmptyState title={unreadOnly ? 'No unread notifications' : 'No notifications yet'} text="Order updates will appear here." />
        </div>
      ) : (
        <ul className="panel mt-6 divide-y divide-line">
          {list.data.content.map((n) => (
            <li key={n.id} className={`flex items-start gap-3 p-4 ${n.read ? '' : 'bg-tag/10'}`}>
              <span className={`mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full ${n.read ? 'bg-line' : 'bg-tag-deep'}`} aria-hidden="true" />
              <div className="flex-1">
                <p className="font-semibold">
                  {n.title}
                  {!n.read && <span className="sr-only"> (unread)</span>}
                </p>
                <p className="text-sm">{n.message}</p>
                <p className="mt-1 text-xs text-moss">
                  {dateTime(n.createdAt)}
                  {n.orderId && (
                    <>
                      {' '}
                      <Link to={`/orders/${n.orderId}`} className="underline">
                        View order
                      </Link>
                    </>
                  )}
                </p>
              </div>
              {!n.read && (
                <button type="button" className="btn btn-ghost px-2 py-1 text-xs" onClick={() => act(() => notificationsApi.markRead(n.id))}>
                  Mark as read
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
      <Pagination page={list.data.page} totalPages={list.data.totalPages} onChange={setPage} />
    </div>
  );
}
