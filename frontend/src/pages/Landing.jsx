import { Link } from 'react-router-dom';
import { inventoryApi } from '../api/inventory';
import { categoriesApi, productsApi } from '../api/products';
import { ArrowRight } from '../components/icons';
import { PromiseStrip } from '../components/Footer';
import ProductCard from '../components/ProductCard';
import { Scroller, SectionHeader } from '../components/Section';
import { useAuth } from '../context/AuthContext';
import { useAddToCart } from '../context/CartContext';
import { useFetch } from '../hooks/useFetch';
import { money } from '../lib/format';

function Hero({ products }) {
  const withImage = products.filter((p) => p.imageUrl);
  const [main, a, b] = withImage;
  const floating = (p, pos) =>
    p && (
      <Link
        key={p.id}
        to={`/products/${p.id}`}
        className={`absolute hidden w-36 rounded-xl bg-white p-2.5 shadow-card sm:block ${pos}`}
      >
        <img src={p.imageUrl} alt="" className="aspect-[4/3] w-full rounded-lg bg-chalk-deep object-cover" />
        <p className="mt-2 truncate text-xs font-medium">{p.name}</p>
        <p className="text-xs font-bold">{money(p.price)}</p>
      </Link>
    );

  return (
    <section className="grid items-center gap-8 overflow-hidden rounded-3xl bg-gradient-to-r from-chalk-deep to-white px-6 py-10 md:grid-cols-2 md:px-12 md:py-14">
      <div>
        <p className="text-sm font-semibold text-brand">Trending now</p>
        <h1 className="mt-3 text-4xl leading-[1.1] sm:text-5xl">
          Discover products
          <br />
          you&apos;ll love
        </h1>
        <p className="mt-4 max-w-md text-moss">Shop the latest arrivals from our catalogue, with live stock and order tracking all the way to your door.</p>
        <div className="mt-7 flex flex-wrap gap-3">
          <Link to="/shop" className="btn btn-brand px-6 py-3">
            Shop now <ArrowRight className="h-4 w-4" />
          </Link>
          <Link to="/shop?sort=createdAt,desc" className="btn btn-secondary px-6 py-3">
            See new arrivals
          </Link>
        </div>
      </div>

      <div className="relative mx-auto aspect-square w-full max-w-md">
        <div className="absolute inset-[6%] rounded-[45%_55%_50%_50%] bg-brand" aria-hidden="true" />
        {main && (
          <Link to={`/products/${main.id}`} className="absolute inset-[16%] block overflow-hidden rounded-3xl bg-white shadow-card">
            <img src={main.imageUrl} alt={main.name} className="h-full w-full object-cover" />
          </Link>
        )}
        {floating(a, '-left-2 top-4')}
        {floating(b, '-right-2 bottom-6')}
      </div>
    </section>
  );
}

function CategoryTiles({ categories, products }) {
  if (!categories.length) return null;
  const imageFor = (id) => products.find((p) => p.categoryId === id && p.imageUrl)?.imageUrl;
  return (
    <section>
      <SectionHeader title="Shop by category" to="/shop" linkLabel="View all products" />
      <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
        {categories.slice(0, 6).map((c) => {
          const img = imageFor(c.id);
          return (
            <li key={c.id}>
              <Link to={`/shop?categoryId=${c.id}`} className="group relative block aspect-[4/5] overflow-hidden rounded-xl bg-ink">
                {img ? (
                  <img src={img} alt="" loading="lazy" className="h-full w-full object-cover opacity-90 transition-transform duration-300 group-hover:scale-105" />
                ) : (
                  <div className="h-full w-full bg-gradient-to-br from-brand-soft to-chalk-deep" />
                )}
                <span className="absolute inset-x-2 bottom-2 flex items-center justify-between rounded-lg bg-white/95 px-3 py-2 text-sm font-semibold">
                  {c.name}
                  <ArrowRight className="h-4 w-4" />
                </span>
              </Link>
            </li>
          );
        })}
      </ul>
    </section>
  );
}

function ValueRow({ product, availability }) {
  const addToCart = useAddToCart();
  const soldOut = availability && !availability.inStock;
  return (
    <li className="flex items-center gap-4 rounded-xl bg-chalk-deep p-4">
      <Link to={`/products/${product.id}`} className="h-28 w-28 shrink-0 overflow-hidden rounded-lg bg-white">
        {product.imageUrl && <img src={product.imageUrl} alt="" loading="lazy" className="h-full w-full object-cover" />}
      </Link>
      <div className="min-w-0 flex-1">
        <Link to={`/products/${product.id}`} className="block truncate font-semibold hover:underline">
          {product.name}
        </Link>
        <p className="mt-0.5 text-lg font-bold">{money(product.price)}</p>
        <p className="line-clamp-2 text-xs text-moss">{product.description}</p>
        <button type="button" className="btn btn-primary mt-3 px-4 py-1.5" disabled={soldOut} onClick={() => addToCart(product.id, 1)}>
          {soldOut ? 'Sold out' : 'Quick add'}
        </button>
      </div>
    </li>
  );
}

export default function Landing() {
  const { isAuthenticated } = useAuth();
  const categories = useFetch(() => categoriesApi.list(), []);
  const newest = useFetch(() => productsApi.search({ sort: 'createdAt,desc', size: 12 }), []);
  const value = useFetch(() => productsApi.search({ sort: 'price,asc', size: 3 }), []);

  const newList = newest.data?.content || [];
  const valueList = value.data?.content || [];
  const ids = [...new Set([...newList, ...valueList].map((p) => p.id))].join(',');
  const stock = useFetch(() => inventoryApi.availability(ids ? ids.split(',') : []).catch(() => []), [ids]);
  const availability = Object.fromEntries((stock.data || []).map((a) => [a.productId, a]));

  return (
    <div className="space-y-14">
      <Hero products={newList} />
      <PromiseStrip />
      <CategoryTiles categories={categories.data || []} products={newList} />

      {newList.length > 0 && (
        <section>
          <SectionHeader title="New arrivals" to="/shop?sort=createdAt,desc" />
          <Scroller label="New arrivals">
            {newList.map((p) => (
              <li key={p.id} className="w-44 shrink-0 snap-start sm:w-52">
                <ProductCard product={p} availability={availability[p.id]} />
              </li>
            ))}
          </Scroller>
        </section>
      )}

      {valueList.length > 0 && (
        <section>
          <SectionHeader title="Great value" to="/shop?sort=price,asc" />
          <ul className="grid gap-5 lg:grid-cols-3">
            {valueList.map((p) => (
              <ValueRow key={p.id} product={p} availability={availability[p.id]} />
            ))}
          </ul>
        </section>
      )}

      {newest.error && !newest.data && (
        <p className="rounded-xl bg-rust-soft p-4 text-sm text-rust" role="alert">
          We couldn&apos;t load products right now.{' '}
          <button type="button" className="font-semibold underline" onClick={newest.reload}>
            Try again
          </button>
        </p>
      )}

      <section className="grid gap-5 md:grid-cols-2">
        <div className="rounded-2xl bg-gradient-to-br from-brand to-orange-400 p-8 text-white">
          <h2 className="text-3xl">The full catalogue</h2>
          <p className="mt-2 max-w-sm text-white/90">Search, filter by category and price, and sort to find what you need.</p>
          <Link to="/shop" className="btn mt-6 bg-white text-ink hover:bg-chalk-deep">
            Browse products
          </Link>
        </div>
        <div className="rounded-2xl bg-ink p-8 text-white">
          <h2 className="text-3xl">Follow your order</h2>
          <p className="mt-2 max-w-sm text-white/80">See stock reserved, payment and confirmation as each step happens.</p>
          <Link to={isAuthenticated ? '/orders' : '/login'} className="btn mt-6 bg-white text-ink hover:bg-chalk-deep">
            {isAuthenticated ? 'View your orders' : 'Sign in to track'}
          </Link>
        </div>
      </section>
    </div>
  );
}
