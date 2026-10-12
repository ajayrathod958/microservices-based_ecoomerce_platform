import { http, unwrap } from './http';

export const cartApi = {
  get: () => unwrap(http.get('/api/cart')),
  add: (productId, quantity) => unwrap(http.post('/api/cart/items', { productId, quantity })),
  setQuantity: (productId, quantity) => unwrap(http.put(`/api/cart/items/${productId}`, { quantity })),
  remove: (productId) => unwrap(http.delete(`/api/cart/items/${productId}`)),
  clear: () => unwrap(http.delete('/api/cart')),
};
