import { useState } from 'react';
import { categoriesApi } from '../api/products';
import Field from '../components/Field';
import { EmptyState, ErrorState, Loading } from '../components/feedback';
import Modal, { ConfirmDialog } from '../components/Modal';
import { useToast } from '../context/ToastContext';
import { useFetch } from '../hooks/useFetch';
import { getErrorMessage, getFieldErrors } from '../lib/errors';

function CategoryForm({ category, onClose, onSaved }) {
  const toast = useToast();
  const [form, setForm] = useState({ name: category?.name || '', description: category?.description || '' });
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    if (!form.name.trim()) return setErrors({ name: 'Enter a name.' });
    setBusy(true);
    try {
      const body = { name: form.name.trim(), description: form.description.trim() };
      if (category) await categoriesApi.update(category.id, body);
      else await categoriesApi.create(body);
      toast.success(category ? 'Category saved' : 'Category created');
      return onSaved();
    } catch (err) {
      const fields = getFieldErrors(err);
      setErrors(fields);
      if (!Object.keys(fields).length) toast.error(getErrorMessage(err));
      return setBusy(false);
    }
  };

  return (
    <Modal title={category ? 'Edit category' : 'New category'} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4" noValidate>
        <Field label="Name" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} error={errors.name} />
        <Field as="textarea" rows={3} label="Description" value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} error={errors.description} />
        <div className="flex justify-end gap-3">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={busy}>
            Save category
          </button>
        </div>
      </form>
    </Modal>
  );
}

export default function CategoriesTab() {
  const toast = useToast();
  const list = useFetch(() => categoriesApi.list(), []);
  const [editing, setEditing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [busy, setBusy] = useState(false);

  const remove = async () => {
    setBusy(true);
    try {
      await categoriesApi.remove(deleting.id);
      toast.success('Category deleted');
      setDeleting(null);
      list.reload();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <div className="mb-4 flex justify-end">
        <button type="button" className="btn btn-primary" onClick={() => setEditing('new')}>
          New category
        </button>
      </div>
      {list.loading && !list.data ? (
        <Loading />
      ) : list.error && !list.data ? (
        <ErrorState message="We couldn't load categories." onRetry={list.reload} />
      ) : list.data.length === 0 ? (
        <EmptyState title="No categories yet" text="Categories help shoppers filter the catalogue." />
      ) : (
        <ul className="panel divide-y divide-line">
          {list.data.map((c) => (
            <li key={c.id} className="flex items-center justify-between gap-4 p-4">
              <div>
                <p className="font-semibold">{c.name}</p>
                {c.description && <p className="text-sm text-moss">{c.description}</p>}
              </div>
              <div className="space-x-3 text-sm">
                <button type="button" className="underline" onClick={() => setEditing(c)}>
                  Edit
                </button>
                <button type="button" className="text-rust underline" onClick={() => setDeleting(c)}>
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
      {editing && (
        <CategoryForm
          category={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            list.reload();
          }}
        />
      )}
      {deleting && (
        <ConfirmDialog
          title="Delete category?"
          message={`"${deleting.name}" will be removed. This fails if products still use it.`}
          confirmLabel="Delete category"
          busy={busy}
          onCancel={() => setDeleting(null)}
          onConfirm={remove}
        />
      )}
    </div>
  );
}
