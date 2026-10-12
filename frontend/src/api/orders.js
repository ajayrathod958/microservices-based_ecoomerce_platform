import { http, unwrap } from './http';

export const ordersApi = {
  /** The Idempotency-Key makes a double-click or a retry return the same order instead of a second one. */
  checkout: (body, idempotencyKey) =>
    unwrap(http.post('/api/orders', body, { headers: { 'Idempotency-Key': idempotencyKey } })),
  list: (page = 0, size = 10) => unwrap(http.get('/api/orders', { params: { page, size } })),
  get: (id) => unwrap(http.get(`/api/orders/${id}`)),
};
