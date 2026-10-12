import axios from 'axios';
import { tokenStore } from '../lib/tokenStore';

const baseURL = import.meta.env.VITE_API_BASE_URL || '';

export const http = axios.create({
  baseURL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

let onAuthFailure = () => {};
export function setAuthFailureHandler(fn) {
  onAuthFailure = fn || (() => {});
}

let refreshPromise = null;

async function performRefresh() {
  const refreshToken = tokenStore.getRefreshToken();
  if (!refreshToken) throw new Error('No refresh token');
  // plain axios (not `http`) so this call can never trigger the 401 handler below
  const res = await axios.post(`${baseURL}/api/auth/refresh`, { refreshToken }, { timeout: 15000 });
  const data = res.data.data;
  tokenStore.setTokens(data.accessToken, data.refreshToken);
  return data;
}

/**
 * One refresh at a time. Refresh tokens rotate and are single-use, so two parallel refreshes would make the
 * server treat the second as token theft and sign the user out everywhere. Concurrent callers share one promise.
 */
export function refreshSession() {
  if (!refreshPromise) {
    refreshPromise = performRefresh()
      .catch((err) => {
        tokenStore.clear();
        throw err;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

http.interceptors.request.use((config) => {
  const token = tokenStore.getAccessToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { response, config } = error;
    const isAuthCall = Boolean(config?.url?.startsWith('/api/auth/'));
    if (response?.status === 401 && config && !config._retried && !isAuthCall) {
      config._retried = true;
      const hadSession = tokenStore.hasSession();
      try {
        await refreshSession();
      } catch {
        if (hadSession) onAuthFailure();
        return Promise.reject(error);
      }
      config.headers.Authorization = `Bearer ${tokenStore.getAccessToken()}`;
      return http(config);
    }
    return Promise.reject(error);
  },
);

/** All endpoints answer { success, data, message }. */
export const unwrap = (promise) => promise.then((res) => res.data.data);
