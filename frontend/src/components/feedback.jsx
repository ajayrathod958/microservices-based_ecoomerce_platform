import { Link } from 'react-router-dom';

export function Spinner({ className = 'h-5 w-5' }) {
  return (
    <svg className={`animate-spin text-ink ${className}`} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeOpacity="0.2" strokeWidth="3" />
      <path d="M22 12a10 10 0 0 0-10-10" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}

export function Loading({ label = 'Loading' }) {
  return (
    <div className="flex items-center justify-center gap-3 py-16 text-moss" role="status">
      <Spinner />
      <span>{label}</span>
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="rounded-md border border-rust/40 bg-rust-soft p-6 text-center" role="alert">
      <p className="font-medium text-rust">{message}</p>
      {onRetry && (
        <button type="button" className="btn btn-secondary mt-4" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}

export function EmptyState({ title, text, actionLabel, actionTo }) {
  return (
    <div className="panel px-6 py-14 text-center">
      <h2 className="text-xl font-semibold">{title}</h2>
      {text && <p className="mx-auto mt-2 max-w-md text-moss">{text}</p>}
      {actionTo && (
        <Link to={actionTo} className="btn btn-primary mt-6">
          {actionLabel}
        </Link>
      )}
    </div>
  );
}
