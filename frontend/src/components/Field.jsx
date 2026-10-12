import { useId } from 'react';

/** Label + input + error message wired together for screen readers. */
export default function Field({ label, error, hint, as = 'input', className = '', children, ...props }) {
  const id = useId();
  const Tag = as;
  return (
    <div className={className}>
      <label htmlFor={id} className="label">
        {label}
      </label>
      <Tag
        id={id}
        className={`input ${error ? 'input-error' : ''}`}
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error ? `${id}-error` : hint ? `${id}-hint` : undefined}
        {...props}
      >
        {children}
      </Tag>
      {hint && !error && (
        <p id={`${id}-hint`} className="mt-1 text-xs text-moss">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} className="mt-1 text-xs text-rust">
          {error}
        </p>
      )}
    </div>
  );
}
