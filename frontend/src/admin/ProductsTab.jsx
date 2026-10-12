import { useState } from 'react';
import { categoriesApi, productsApi } from '../api/products';
import Field from '../components/Field';
import { EmptyState, ErrorState, Loading } from '../components/feedback';
import Modal, { ConfirmDialog } from '../components/Modal';
import Pagination from '../components/Pagination';
import { useToast } from '../context/ToastContext';
import { useFetch } from '../hooks/useFetch';
import { getErrorMessage, getFieldErrors } from '../lib/errors';
import { money } from '../lib/format';

const BLANK = { name: '', description: '', sku: '', price: '', imageUrl: '', categoryId: '' };

function ProductForm({ product, categories, onClose, onSaved }) {
  const toast = useToast();
  const [form, setForm] = useState(
    product
      ? { name: product.name, description: product.description || '', sku: product.sku, price: product.price, imageUrl: product.imageUrl || '', categoryId: product.categoryId || '' }
      : BLANK,
  );
  const [errors, setErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    const local = {};
    if (!form.name.trim()) local.name = 'Enter a name.';
    if (!form.sku.trim()) local.sku = 'Enter a SKU.';
    if (form.price === '' || Number(form.price) <= 0) local.price = 'Enter a price above 0.';
    setErrors(local);
    if (Object.keys(local).length) return;
    setBusy(true);
    const body = {
      ...form,
      name: form.name.trim(),
      sku: form.sku.trim(),
      price: Number(form.price),
      categoryId: form.categoryId || null,
      imageUrl: form.imageUrl.trim() || null,
    };
    try {
      if (product) await productsApi.update(product.id, body);
      else await productsApi.create(body);
      toast.success(product ? 'Product saved' : 'Product created');
      onSaved();
    } catch (err) {
      const fields = getFieldErrors(err);
      setErrors(fields);
      if (!Object.keys(fields).length) toast.error(getErrorMessage(err));
      setBusy(false);
    }
  };

  return (
    <Modal title={product ? 'Edit product' : 'New product'} onClose={onClose} wide>
      <form onSubmit={submit} className="space-y-4" noValidate>
        <Field label="Name" value={form.name} onChange={set('name')} error={errors.name} />
        <div className="grid gap-4 sm:grid-cols-3">
          <Field label="SKU" value={form.sku} onChange={set('sku')} error={errors.sku} />
          <Field label="Price" type="number" min="0" step="0.01" value={form.price} onChange={set('price')} error={errors.price} />
          <Field as="select" label="Category" value={form.categoryId} onChange={set('categoryId')} error={errors.categoryId}>
            <option value="">No category</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </Field>
        </div>
        <Field label="Image URL" type="url" value={form.imageUrl} onChange={set('imageUrl')} error={errors.imageUrl} />
        <Field as="textarea" rows={4} label="Description" value={form.description} onChange={set('description')} error={errors.description} />
        <div className="flex justify-end gap-3">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={busy}>
            Save product
          </button>
        </div>
      </form>
    </Modal>
  );
}

export default function ProductsTab() {
  const toast = useToast();
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState(null); // null | 'new' | product
  const [deleting, setDeleting] = useState(null);
  const [busy, setBusy] = useState(false);
  const products = useFetch(() => productsApi.search({ page, size: 10, sort: 'createdAt,desc' }), [page]);
  const categories = useFetch(() => categoriesApi.list(), []);

  const remove = async () => {
    setBusy(true);
    try {
      await productsApi.remove(deleting.id);
      toast.success('Product deleted');
      setDeleting(null);
      products.reload();
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
          New product
        </button>
      </div>
      {products.loading && !products.data ? (
        <Loading />
      ) : products.error && !products.data ? (
        <ErrorState message="We couldn't load products." onRetry={products.reload} />
      ) : products.data.content.length === 0 ? (
        <EmptyState title="No products yet" text="Create the first product to start selling." />
      ) : (
        <>
          <div className="panel overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-line bg-chalk text-moss">
                <tr>
                  <th className="p-3 font-medium">Name</th>
                  <th className="p-3 font-medium">SKU</th>
                  <th className="p-3 font-medium">Category</th>
                  <th className="p-3 text-right font-medium">Price</th>
                  <th className="p-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {products.data.content.map((p) => (
                  <tr key={p.id}>
                    <td className="p-3 font-medium">{p.name}</td>
                    <td className="p-3">{p.sku}</td>
                    <td className="p-3">{p.categoryName || '-'}</td>
                    <td className="p-3 text-right">{money(p.price)}</td>
                    <td className="space-x-2 p-3 text-right">
                      <button type="button" className="underline" onClick={() => setEditing(p)}>
                        Edit
                      </button>
                      <button type="button" className="text-rust underline" onClick={() => setDeleting(p)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={products.data.page} totalPages={products.data.totalPages} onChange={setPage} />
        </>
      )}

      {editing && (
        <ProductForm
          product={editing === 'new' ? null : editing}
          categories={categories.data || []}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            products.reload();
          }}
        />
      )}
      {deleting && (
        <ConfirmDialog
          title="Delete product?"
          message={`"${deleting.name}" will be removed from the shop.`}
          confirmLabel="Delete product"
          busy={busy}
          onCancel={() => setDeleting(null)}
          onConfirm={remove}
        />
      )}
    </div>
  );
}
