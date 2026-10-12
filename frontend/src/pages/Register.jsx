import { useState } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import Field from '../components/Field';
import { Spinner } from '../components/feedback';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { getErrorMessage, getFieldErrors } from '../lib/errors';

function validate(f) {
  const e = {};
  if (!f.firstName.trim()) e.firstName = 'Enter your first name.';
  if (!f.lastName.trim()) e.lastName = 'Enter your last name.';
  if (!/^\S+@\S+\.\S+$/.test(f.email)) e.email = 'Enter a valid email address.';
  if (f.password.length < 8 || f.password.length > 72) e.password = 'Use 8 to 72 characters.';
  else if (!/[A-Za-z]/.test(f.password) || !/\d/.test(f.password)) e.password = 'Include at least one letter and one number.';
  return e;
}

export default function Register() {
  const { register, isAuthenticated } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from || '/';
  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', phone: '', password: '' });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);

  if (isAuthenticated && !busy) return <Navigate to={from} replace />;

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));
  const submit = async (e) => {
    e.preventDefault();
    const v = validate(form);
    setErrors(v);
    setFormError('');
    if (Object.keys(v).length) return;
    setBusy(true);
    try {
      const payload = { ...form, email: form.email.trim(), firstName: form.firstName.trim(), lastName: form.lastName.trim() };
      if (!payload.phone.trim()) delete payload.phone;
      const user = await register(payload);
      toast.success(`Account created. Welcome, ${user.firstName}`);
      navigate(from, { replace: true });
    } catch (err) {
      const fields = getFieldErrors(err);
      setErrors(fields);
      setFormError(Object.keys(fields).length ? 'Fix the highlighted fields.' : getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="mx-auto max-w-md">
      <h1 className="text-3xl font-bold">Create your account</h1>
      <form onSubmit={submit} className="mt-6 space-y-4" noValidate>
        {formError && (
          <p className="rounded-md bg-rust-soft p-3 text-sm text-rust" role="alert">
            {formError}
          </p>
        )}
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="First name" autoComplete="given-name" value={form.firstName} onChange={set('firstName')} error={errors.firstName} />
          <Field label="Last name" autoComplete="family-name" value={form.lastName} onChange={set('lastName')} error={errors.lastName} />
        </div>
        <Field label="Email" type="email" autoComplete="email" value={form.email} onChange={set('email')} error={errors.email} />
        <Field label="Phone (optional)" type="tel" autoComplete="tel" value={form.phone} onChange={set('phone')} error={errors.phone} />
        <Field
          label="Password"
          type="password"
          autoComplete="new-password"
          value={form.password}
          onChange={set('password')}
          error={errors.password}
          hint="At least 8 characters with a letter and a number."
        />
        <button type="submit" className="btn btn-primary w-full" disabled={busy}>
          {busy && <Spinner className="h-4 w-4 text-chalk" />} Create account
        </button>
      </form>
      <p className="mt-6 text-sm text-moss">
        Already have an account?{' '}
        <Link to="/login" state={location.state} className="font-semibold text-ink underline">
          Sign in
        </Link>
      </p>
    </div>
  );
}
