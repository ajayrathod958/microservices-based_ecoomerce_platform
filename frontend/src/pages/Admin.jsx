import { useState } from 'react';
import CategoriesTab from '../admin/CategoriesTab';
import InventoryTab from '../admin/InventoryTab';
import ProductsTab from '../admin/ProductsTab';

const TABS = [
  { id: 'products', label: 'Products', Component: ProductsTab },
  { id: 'inventory', label: 'Inventory', Component: InventoryTab },
  { id: 'categories', label: 'Categories', Component: CategoriesTab },
];

export default function Admin() {
  const [tab, setTab] = useState('products');
  const Active = TABS.find((t) => t.id === tab).Component;
  return (
    <div>
      <h1 className="text-3xl font-bold">Admin</h1>
      <div className="mt-6 flex gap-1 border-b border-line" role="tablist">
        {TABS.map((t) => (
          <button
            key={t.id}
            type="button"
            role="tab"
            aria-selected={tab === t.id}
            onClick={() => setTab(t.id)}
            className={`-mb-px border-b-4 px-4 py-2 text-sm font-semibold ${tab === t.id ? 'border-tag' : 'border-transparent text-moss hover:text-ink'}`}
          >
            {t.label}
          </button>
        ))}
      </div>
      <div className="mt-6" role="tabpanel">
        <Active />
      </div>
    </div>
  );
}
