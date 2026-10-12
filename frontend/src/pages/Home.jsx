import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { categoriesApi, productsApi } from '../api/products';
import { inventoryApi } from '../api/inventory';
import { EmptyState, ErrorState, Loading } from '../components/feedback';
import Pagination from '../components/Pagination';
import ProductCard from '../components/ProductCard';
import { useFetch } from '../hooks/useFetch';

const SORTS = [
  { value: 'createdAt,desc', label: 'Newest' },
  { value: 'price,asc', label: 'Price: low to high' },
  { value: 'price,desc', label: 'Price: high to low' },
  { value: 'name,asc', label: 'Name: A to Z' },
];

export default function Home() {
  const [params, setParams] = useSearchParams();
  const q = params.get('q') || '';
  const categoryId = params.get('categoryId') || '';
  const minPrice = params.get('minPrice') || '';
  const maxPrice = params.get('maxPrice') || '';
  const sort = params.get('sort') || SORTS[0].value;
  const page = Number(params.get('page') || 0);

  const [search, setSearch] = useState(q);
  useEffect(() => setSearch(q), [q]);

  const update = (changes, resetPage = true) => {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([k, v]) => (v === '' || v === null || v === undefined ? next.delete(k) : next.set(k, v)));
    if (resetPage) next.delete('page');
    setParams(next, { replace: true });
  };

  // Debounce the search box so we don't query on every keystroke.
  useEffect(() => {
    if (search === q) return undefined;
    const t = setTimeout(() => update({ q: search.trim() }), 400);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search]);

  const categories = useFetch(() => categoriesApi.list(), []);
  const products = useFetch(
    () => productsApi.search({ q, categoryId, minPrice, maxPrice, sort, page, size: 12 }),
    [q, categoryId, minPrice, maxPrice, sort, page],
  );
  const ids = (products.data?.content || []).map((p) => p.id).join(',');
  const stock = useFetch(() => inventoryApi.availability(ids ? ids.split(',') : []).catch(() => []), [ids]);
  const availability = Object.fromEntries((stock.data || []).map((a) => [a.productId, a]));
  const hasFilters = q || categoryId || minPrice || maxPrice;

  return (
    <div>
      <h1 className="text-3xl font-bold sm:text-4xl">Everything in stock, priced plainly</h1>

      <div className="mt-6 grid gap-3 md:grid-cols-[2fr_1fr_1fr_1fr_1fr]">
        <input className="input" type="search" placeholder="Search products" aria-label="Search products" value={search} onChange={(e) => setSearch(e.target.value)} />
        <select className="input" aria-label="Category" value={categoryId} onChange={(e) => update({ categoryId: e.target.value })}>
          <option value="">All categories</option>
          {(categories.data || []).map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <input className="input" type="number" min="0" placeholder="Min price" aria-label="Minimum price" value={minPrice} onChange={(e) => update({ minPrice: e.target.value })} />
        <input className="input" type="number" min="0" placeholder="Max price" aria-label="Maximum price" value={maxPrice} onChange={(e) => update({ maxPrice: e.target.value })} />
        <select className="input" aria-label="Sort by" value={sort} onChange={(e) => update({ sort: e.target.value })}>
          {SORTS.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </div>

      <div className="mt-8">
        {products.loading && !products.data ? (
          <Loading label="Loading products" />
        ) : products.error && !products.data ? (
          <ErrorState message="We couldn't load products." onRetry={products.reload} />
        ) : products.data.content.length === 0 ? (
          <EmptyState
            title={hasFilters ? 'No products match those filters' : 'No products yet'}
            text={hasFilters ? 'Try a different search or clear the filters.' : 'Check back soon.'}
          />
        ) : (
          <>
            <p className="mb-4 text-sm text-moss">
              {products.data.totalElements} {products.data.totalElements === 1 ? 'product' : 'products'}
            </p>
            <div className={`grid gap-x-6 gap-y-10 sm:grid-cols-2 lg:grid-cols-3 ${products.loading ? 'opacity-60' : ''}`}>
              {products.data.content.map((p) => (
                <ProductCard key={p.id} product={p} availability={availability[p.id]} />
              ))}
            </div>
            <Pagination page={products.data.page} totalPages={products.data.totalPages} onChange={(p) => update({ page: p || '' }, false)} />
          </>
        )}
      </div>
    </div>
  );
}
