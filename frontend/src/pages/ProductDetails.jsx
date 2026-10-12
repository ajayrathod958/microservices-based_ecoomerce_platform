import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { inventoryApi } from '../api/inventory';
import { productsApi } from '../api/products';
import { ErrorState, Loading } from '../components/feedback';
import PriceTag from '../components/PriceTag';
import { StockNote } from '../components/ProductCard';
import { useAddToCart, useCart } from '../context/CartContext';
import { useFetch } from '../hooks/useFetch';

export default function ProductDetails() {
  const { id } = useParams();
  const addToCart = useAddToCart();
  const { busy } = useCart();
  const [qty, setQty] = useState(1);
  const product = useFetch(() => productsApi.get(id), [id]);
  const stock = useFetch(() => inventoryApi.availability([id]).catch(() => []), [id]);
  const availability = stock.data?.[0];

  if (product.loading && !product.data) return <Loading label="Loading product" />;
  if (product.error && !product.data) {
    const notFound = product.error?.response?.status === 404;
    return <ErrorState message={notFound ? 'This product no longer exists.' : "We couldn't load this product."} onRetry={notFound ? undefined : product.reload} />;
  }
  const p = product.data;
  const max = availability?.inStock ? Math.max(1, Math.min(availability.available, 99)) : 99;
  const soldOut = availability && !availability.inStock;

  return (
    <div>
      <Link to="/" className="text-sm text-moss hover:text-ink">
        Back to the shop
      </Link>
      <div className="mt-4 grid gap-8 md:grid-cols-2">
        <div className="relative overflow-hidden rounded-md bg-chalk-deep">
          <img src={p.imageUrl || undefined} alt={p.name} className="aspect-square w-full object-cover" />
          <PriceTag amount={p.price} className="absolute bottom-4 left-0 text-2xl" />
        </div>
        <div>
          <p className="text-moss">{p.categoryName}</p>
          <h1 className="mt-1 text-3xl font-bold sm:text-4xl">{p.name}</h1>
          <p className="mt-1 text-sm text-moss">SKU {p.sku}</p>
          <p className="mt-6 max-w-prose whitespace-pre-line leading-relaxed">{p.description}</p>
          <div className="mt-6">
            <StockNote availability={availability} />
          </div>
          <div className="mt-6 flex items-end gap-3">
            <div>
              <label htmlFor="qty" className="label">
                Quantity
              </label>
              <input
                id="qty"
                type="number"
                min="1"
                max={max}
                className="input w-24"
                value={qty}
                onChange={(e) => setQty(Math.max(1, Math.min(max, Number(e.target.value) || 1)))}
              />
            </div>
            <button type="button" className="btn btn-primary" disabled={soldOut || busy} onClick={() => addToCart(p.id, qty)}>
              {soldOut ? 'Sold out' : 'Add to cart'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
