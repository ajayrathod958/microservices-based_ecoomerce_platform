import { useState } from 'react';
import { inventoryApi } from '../api/inventory';
import { productsApi } from '../api/products';
import { EmptyState, ErrorState, Loading } from '../components/feedback';
import Pagination from '../components/Pagination';
import { useToast } from '../context/ToastContext';
import { useFetch } from '../hooks/useFetch';
import { getErrorMessage } from '../lib/errors';

function StockRow({ product, item, onChanged }) {
  const toast = useToast();
  const [value, setValue] = useState(item ? String(item.quantityOnHand) : '');
  const [busy, setBusy] = useState(false);

  const save = async () => {
    const qty = Number(value);
    if (value === '' || !Number.isInteger(qty) || qty < 0) return toast.error('Enter a whole number of 0 or more.');
    setBusy(true);
    try {
      await inventoryApi.setStock(product.id, qty);
      toast.success(`Stock for ${product.name} saved`);
      onChanged();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
    return undefined;
  };

  const adjust = async (delta) => {
    setBusy(true);
    try {
      await inventoryApi.adjust(product.id, delta);
      onChanged();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <tr>
      <td className="p-3 font-medium">{product.name}</td>
      <td className="p-3 text-right">{item ? item.quantityOnHand : <span className="text-rust">Not set</span>}</td>
      <td className="p-3 text-right">{item ? item.quantityReserved : '-'}</td>
      <td className="p-3 text-right">{item ? item.available : '-'}</td>
      <td className="p-3">
        <div className="flex items-center justify-end gap-2">
          {item && (
            <>
              <button type="button" className="btn btn-secondary px-2 py-1" disabled={busy} onClick={() => adjust(-1)} aria-label={`Remove one from ${product.name}`}>
                −1
              </button>
              <button type="button" className="btn btn-secondary px-2 py-1" disabled={busy} onClick={() => adjust(1)} aria-label={`Add one to ${product.name}`}>
                +1
              </button>
            </>
          )}
          <input
            type="number"
            min="0"
            className="input w-24"
            aria-label={`Quantity on hand for ${product.name}`}
            value={value}
            onChange={(e) => setValue(e.target.value)}
          />
          <button type="button" className="btn btn-primary px-3 py-1.5" disabled={busy} onClick={save}>
            Set stock
          </button>
        </div>
      </td>
    </tr>
  );
}

export default function InventoryTab() {
  const [page, setPage] = useState(0);
  const products = useFetch(() => productsApi.search({ page, size: 10, sort: 'name,asc' }), [page]);
  const inventory = useFetch(() => inventoryApi.list(0, 200), []);
  const byProduct = Object.fromEntries((inventory.data?.content || []).map((i) => [i.productId, i]));

  if (products.loading && !products.data) return <Loading />;
  if (products.error && !products.data) return <ErrorState message="We couldn't load products." onRetry={products.reload} />;
  if (products.data.content.length === 0) return <EmptyState title="No products yet" text="Create a product before setting its stock." />;

  return (
    <div>
      {inventory.error && <ErrorState message="We couldn't load stock levels." onRetry={inventory.reload} />}
      <div className="panel mt-2 overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-line bg-chalk text-moss">
            <tr>
              <th className="p-3 font-medium">Product</th>
              <th className="p-3 text-right font-medium">On hand</th>
              <th className="p-3 text-right font-medium">Reserved</th>
              <th className="p-3 text-right font-medium">Available</th>
              <th className="p-3 text-right font-medium">Update</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {products.data.content.map((p) => (
              <StockRow key={`${p.id}-${byProduct[p.id]?.quantityOnHand ?? 'x'}`} product={p} item={byProduct[p.id]} onChanged={inventory.reload} />
            ))}
          </tbody>
        </table>
      </div>
      <Pagination page={products.data.page} totalPages={products.data.totalPages} onChange={setPage} />
    </div>
  );
}
