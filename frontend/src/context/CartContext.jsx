import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { cartApi } from '../api/cart';
import { getErrorMessage } from '../lib/errors';
import { useAuth } from './AuthContext';
import { useToast } from './ToastContext';

const EMPTY = { items: [], totalItems: 0, subtotal: 0 };
const CartContext = createContext(null);

export function CartProvider({ children }) {
  const { status } = useAuth();
  const toast = useToast();
  const [cart, setCart] = useState(EMPTY);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      setCart(await cartApi.get());
    } catch {
      /* keep what we have; the next action will surface a real error */
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (status === 'authenticated') refresh();
    else setCart(EMPTY);
  }, [status, refresh]);

  const run = useCallback(
    async (action, successMessage) => {
      setBusy(true);
      try {
        setCart(await action());
        if (successMessage) toast.success(successMessage);
        return true;
      } catch (err) {
        toast.error(getErrorMessage(err));
        return false;
      } finally {
        setBusy(false);
      }
    },
    [toast],
  );

  const value = useMemo(
    () => ({
      cart,
      count: cart.totalItems || 0,
      loading,
      busy,
      refresh,
      add: (productId, quantity = 1) => run(() => cartApi.add(productId, quantity), 'Added to cart'),
      setQuantity: (productId, quantity) => run(() => cartApi.setQuantity(productId, quantity)),
      remove: (productId) => run(() => cartApi.remove(productId), 'Removed from cart'),
      clear: () => run(() => cartApi.clear(), 'Cart emptied'),
    }),
    [cart, loading, busy, refresh, run],
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export const useCart = () => useContext(CartContext);

/** Add-to-cart that sends signed-out shoppers to sign in first and brings them back afterwards. */
export function useAddToCart() {
  const { add } = useCart();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();

  return useCallback(
    (productId, quantity = 1) => {
      if (!isAuthenticated) {
        toast.info('Sign in to add items to your cart.');
        navigate('/login', { state: { from: location.pathname + location.search } });
        return Promise.resolve(false);
      }
      return add(productId, quantity);
    },
    [isAuthenticated, add, navigate, location, toast],
  );
}
