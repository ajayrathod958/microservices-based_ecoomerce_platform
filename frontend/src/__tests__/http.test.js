import { beforeEach, describe, expect, it, vi } from 'vitest';
import { http, setAuthFailureHandler } from '../api/http';
import { tokenStore } from '../lib/tokenStore';

// axios.post (used for the refresh call) goes through the default adapter, so mock it there.
import axios from 'axios';

function adapterFor(handler) {
  return (config) =>
    new Promise((resolve, reject) => {
      const { status, data } = handler(config);
      const response = { status, data, headers: {}, config, statusText: '' };
      if (status >= 200 && status < 300) resolve(response);
      else reject(Object.assign(new Error(`HTTP ${status}`), { response, config, isAxiosError: true }));
    });
}

describe('http interceptor', () => {
  beforeEach(() => {
    tokenStore.clear();
    setAuthFailureHandler(null);
    axios.defaults.adapter = undefined;
  });

  it('refreshes once for concurrent 401s and retries each request', async () => {
    tokenStore.setTokens('old', 'refresh-1');
    let refreshCalls = 0;
    axios.defaults.adapter = adapterFor(() => {
      refreshCalls += 1;
      return { status: 200, data: { data: { accessToken: 'new', refreshToken: 'refresh-2' } } };
    });
    http.defaults.adapter = adapterFor((config) =>
      config.headers.Authorization === 'Bearer new' ? { status: 200, data: { data: config.url } } : { status: 401, data: {} },
    );

    const [a, b, c] = await Promise.all([http.get('/api/a'), http.get('/api/b'), http.get('/api/c')]);
    expect([a.data.data, b.data.data, c.data.data]).toEqual(['/api/a', '/api/b', '/api/c']);
    expect(refreshCalls).toBe(1);
    expect(tokenStore.getRefreshToken()).toBe('refresh-2');
  });

  it('clears the session and notifies when refresh fails', async () => {
    tokenStore.setTokens('old', 'refresh-1');
    const onFail = vi.fn();
    setAuthFailureHandler(onFail);
    axios.defaults.adapter = adapterFor(() => ({ status: 401, data: {} }));
    http.defaults.adapter = adapterFor(() => ({ status: 401, data: {} }));

    await expect(http.get('/api/orders')).rejects.toBeTruthy();
    expect(onFail).toHaveBeenCalledTimes(1);
    expect(tokenStore.getRefreshToken()).toBeNull();
  });

  it('does not try to refresh failed login attempts', async () => {
    tokenStore.setTokens(null, 'refresh-1');
    const onFail = vi.fn();
    setAuthFailureHandler(onFail);
    http.defaults.adapter = adapterFor(() => ({ status: 401, data: {} }));
    await expect(http.post('/api/auth/login', {})).rejects.toBeTruthy();
    expect(onFail).not.toHaveBeenCalled();
    expect(tokenStore.getRefreshToken()).toBe('refresh-1');
  });

  it('sends the bearer token', async () => {
    tokenStore.setTokens('tkn', null);
    http.defaults.adapter = adapterFor((c) => ({ status: 200, data: { auth: c.headers.Authorization } }));
    const res = await http.get('/api/x');
    expect(res.data.auth).toBe('Bearer tkn');
  });
});
