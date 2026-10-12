import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import Field from '../components/Field';
import { Spinner } from '../components/feedback';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { getErrorMessage } from '../lib/errors';

export default function Login() {
  const { login, isAuthenticated } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from || '/';
  const [form, setForm] = useState({ email: '', password: '' });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  if (isAuthenticated && !busy) return <Navigate to={from} replace />;

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));
  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const user = await login(form.email.trim(), form.password);
      toast.success(`Welcome back, ${user.firstName}`);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err?.response?.status === 401 ? 'Email or password is incorrect.' : getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="text-3xl font-bold">Sign in</h1>
      <form onSubmit={submit} className="mt-6 space-y-4" noValidate>
        {error && (
          <p className="rounded-md bg-rust-soft p-3 text-sm text-rust" role="alert">
            {error}
          </p>
        )}
        <Field label="Email" type="email" autoComplete="email" required value={form.email} onChange={set('email')} />
        <Field label="Password" type="password" autoComplete="current-password" required value={form.password} onChange={set('password')} />
        <button type="submit" className="btn btn-primary w-full" disabled={busy || !form.email || !form.password}>
          {busy && <Spinner className="h-4 w-4 text-chalk" />} Sign in
        </button>
      </form>
      <p className="mt-6 text-sm text-moss">
        New here?{' '}
        <Link to="/register" state={location.state} className="font-semibold text-ink underline">
          Create an account
        </Link>
      </p>
    </div>
  );
}
