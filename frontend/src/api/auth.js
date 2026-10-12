import { http, unwrap } from './http';

export const authApi = {
  login: (email, password) => unwrap(http.post('/api/auth/login', { email, password })),
  register: (payload) => unwrap(http.post('/api/auth/register', payload)),
  logout: (refreshToken) => http.post('/api/auth/logout', { refreshToken }),
  updateProfile: (payload) => unwrap(http.put('/api/users/me', payload)),
};
