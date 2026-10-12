import { Link } from 'react-router-dom';
import PriceTag from './PriceTag';
import { useAddToCart } from '../context/CartContext';

export function StockNote({ availability }) {
  if (!availability) return null;
  if (!availability.inStock) return <span className="text-sm font-medium text-rust">Sold out</span>;
  if (availability.available <= 5) return <span className="text-sm font-medium text-rust">Only {availability.available} left</span>;
  return <span className="text-sm text-leaf">In stock</span>;
}

export default function ProductCard({ product, availability }) {
  const addToCart = useAddToCart();
  const soldOut = availability && !availability.inStock;

  return (
    <article className="group flex flex-col">
      <Link to={`/products/${product.id}`} className="relative block overflow-hidden rounded-md bg-chalk-deep">
        <img
          src={product.imageUrl || undefined}
          alt=""
          loading="lazy"
          className="aspect-[4/3] w-full object-cover transition-transform duration-300 group-hover:scale-[1.03]"
        />
        <PriceTag amount={product.price} className="absolute bottom-3 left-0 text-lg" />
      </Link>
      <div className="mt-3 flex flex-1 flex-col">
        <p className="text-sm text-moss">{product.categoryName}</p>
        <h3 className="text-lg font-semibold leading-snug">
          <Link to={`/products/${product.id}`} className="hover:underline">
            {product.name}
          </Link>
        </h3>
        <div className="mt-auto flex items-center justify-between gap-3 pt-3">
          <StockNote availability={availability} />
          <button type="button" className="btn btn-primary ml-auto" disabled={soldOut} onClick={() => addToCart(product.id, 1)}>
            Add to cart
          </button>
        </div>
      </div>
    </article>
  );
}
