export default function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <nav className="mt-8 flex items-center justify-between gap-4" aria-label="Pagination">
      <button type="button" className="btn btn-secondary" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        Previous
      </button>
      <p className="text-sm text-moss">
        Page {page + 1} of {totalPages}
      </p>
      <button type="button" className="btn btn-secondary" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
        Next
      </button>
    </nav>
  );
}
