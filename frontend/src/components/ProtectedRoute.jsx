import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Loading } from './feedback';
import { EmptyState } from './feedback';

export default function ProtectedRoute({ children, admin = false }) {
  const { status, isAdmin } = useAuth();
  const location = useLocation();

  if (status === 'loading') return <Loading label="Checking your session" />;
  if (status !== 'authenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }
  if (admin && !isAdmin) {
    return <EmptyState title="Admins only" text="Your account doesn't have access to this page." actionLabel="Back to the shop" actionTo="/" />;
  }
  return children;
}
