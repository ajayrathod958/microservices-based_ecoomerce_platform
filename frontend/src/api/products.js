import { http, unwrap } from './http';

const clean = (params) =>
  Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== ''));

export const productsApi = {
  search: (params) => unwrap(http.get('/api/products', { params: clean(params) })),
  get: (id) => unwrap(http.get(`/api/products/${id}`)),
  create: (body) => unwrap(http.post('/api/products', body)),
  update: (id, body) => unwrap(http.put(`/api/products/${id}`, body)),
  remove: (id) => http.delete(`/api/products/${id}`),
};

export const categoriesApi = {
  list: () => unwrap(http.get('/api/categories')),
  create: (body) => unwrap(http.post('/api/categories', body)),
  update: (id, body) => unwrap(http.put(`/api/categories/${id}`, body)),
  remove: (id) => http.delete(`/api/categories/${id}`),
};
