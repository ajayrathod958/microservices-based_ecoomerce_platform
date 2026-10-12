import { useEffect, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { notificationsApi } from '../api/notifications';
import { useAuth } from '../context/AuthContext';
import { useCart } from '../context/CartContext';

const linkClass = ({ isActive }) =>
  `rounded px-2 py-1 text-sm font-medium hover:bg-chalk-deep ${isActive ? 'underline decoration-tag decoration-4 underline-offset-8' : ''}`;

function Badge({ count }) {
  if (!count) return null;
  return (
    <span className="absolute -right-2 -top-2 min-w-[1.25rem] rounded-full bg-tag px-1 text-center text-xs font-bold leading-5 text-ink">
      {count > 99 ? '99+' : count}
    </span>
  );
}

export default function Navbar() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const { count } = useCart();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);

  // Poll the notification badge while signed in (cheap endpoint, 30 s).
  useEffect(() => {
    if (!isAuthenticated) {
      setUnread(0);
      return undefined;
    }
    let active = true;
    const load = () => notificationsApi.unreadCount().then((n) => active && setUnread(n)).catch(() => {});
    load();
    const timer = setInterval(load, 30000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [isAuthenticated]);

  const close = () => setOpen(false);
  const onLogout = async () => {
    close();
    await logout();
    navigate('/');
  };

  return (
    <header className="sticky top-0 z-30 border-b border-line bg-chalk/95 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center gap-4 px-4 py-3">
        <Link to="/" className="flex items-center gap-2 font-display text-xl font-bold" onClick={close}>
          <svg viewBox="0 0 64 64" className="h-7 w-7" aria-hidden="true">
            <rect width="64" height="64" rx="12" fill="#0E2F2A" />
            <path d="M14 22h30l8 10-8 10H14z" fill="#FFCF3F" />
            <circle cx="22" cy="32" r="3.5" fill="#0E2F2A" />
          </svg>
          MicroShop
        </Link>

        <button
          type="button"
          className="btn btn-ghost ml-auto px-2 sm:hidden"
          aria-expanded={open}
          aria-controls="main-nav"
          onClick={() => setOpen((o) => !o)}
        >
          {open ? 'Close' : 'Menu'}
        </button>

        <nav
          id="main-nav"
          className={`${open ? 'flex' : 'hidden'} absolute inset-x-0 top-full flex-col gap-1 border-b border-line bg-chalk p-4 sm:static sm:ml-auto sm:flex sm:flex-row sm:items-center sm:gap-3 sm:border-0 sm:bg-transparent sm:p-0`}
        >
          <NavLink to="/" end className={linkClass} onClick={close}>
            Shop
          </NavLink>
          {isAuthenticated && (
            <NavLink to="/orders" className={linkClass} onClick={close}>
              Orders
            </NavLink>
          )}
          {isAdmin && (
            <NavLink to="/admin" className={linkClass} onClick={close}>
              Admin
            </NavLink>
          )}
          {isAuthenticated && (
            <>
              <NavLink to="/notifications" className={`${linkClass({ isActive: false })} relative`} onClick={close} aria-label={`Notifications${unread ? `, ${unread} unread` : ''}`}>
                Notifications
                <Badge count={unread} />
              </NavLink>
              <NavLink to="/cart" className={`${linkClass({ isActive: false })} relative`} onClick={close} aria-label={`Cart${count ? `, ${count} items` : ''}`}>
                Cart
                <Badge count={count} />
              </NavLink>
              <span className="hidden text-sm text-moss lg:inline">{user?.firstName}</span>
              <button type="button" className="btn btn-secondary" onClick={onLogout}>
                Sign out
              </button>
            </>
          )}
          {!isAuthenticated && (
            <>
              <Link to="/login" className="btn btn-secondary" onClick={close}>
                Sign in
              </Link>
              <Link to="/register" className="btn btn-primary" onClick={close}>
                Create account
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
