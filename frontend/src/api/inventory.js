import { http, unwrap } from './http';

export const inventoryApi = {
  availability: (ids) =>
    ids.length ? unwrap(http.get('/api/inventory/availability', { params: { productIds: ids.join(',') } })) : Promise.resolve([]),
  list: (page = 0, size = 20) => unwrap(http.get('/api/inventory', { params: { page, size } })),
  setStock: (productId, quantityOnHand) => unwrap(http.put(`/api/inventory/${productId}`, { quantityOnHand })),
  adjust: (productId, delta) => unwrap(http.patch(`/api/inventory/${productId}/adjust`, { delta })),
};
