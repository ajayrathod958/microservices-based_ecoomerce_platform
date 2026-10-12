import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authApi } from '../api/auth';
import { refreshSession, setAuthFailureHandler } from '../api/http';
import { tokenStore } from '../lib/tokenStore';
import { useToast } from './ToastContext';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const toast = useToast();
  const [user, setUser] = useState(null);
  const [status, setStatus] = useState('loading'); // loading | authenticated | anonymous

  // If a refresh fails mid-session the axios layer calls this; the router then sends the user to /login.
  useEffect(() => {
    setAuthFailureHandler(() => {
      setUser(null);
      setStatus('anonymous');
      toast.info('Your session expired. Sign in again to continue.');
    });
    return () => setAuthFailureHandler(null);
  }, [toast]);

  // Restore the session after a reload by exchanging the stored refresh token.
  useEffect(() => {
    let active = true;
    if (!tokenStore.getRefreshToken()) {
      setStatus('anonymous');
      return undefined;
    }
    refreshSession()
      .then((data) => {
        if (!active) return;
        setUser(data.user);
        setStatus('authenticated');
      })
      .catch(() => active && setStatus('anonymous'));
    return () => {
      active = false;
    };
  }, []);

  const login = useCallback(async (email, password) => {
    const data = await authApi.login(email, password);
    tokenStore.setTokens(data.accessToken, data.refreshToken);
    setUser(data.user);
    setStatus('authenticated');
    return data.user;
  }, []);

  const register = useCallback(
    async (payload) => {
      await authApi.register(payload);
      return login(payload.email, payload.password);
    },
    [login],
  );

  const logout = useCallback(async () => {
    const refreshToken = tokenStore.getRefreshToken();
    tokenStore.clear();
    setUser(null);
    setStatus('anonymous');
    if (refreshToken) {
      try {
        await authApi.logout(refreshToken);
      } catch {
        /* already signed out locally; the token expires on its own */
      }
    }
  }, []);

  const value = useMemo(
    () => ({
      user,
      status,
      isAuthenticated: status === 'authenticated',
      isAdmin: Boolean(user?.roles?.includes('ADMIN')),
      login,
      register,
      logout,
    }),
    [user, status, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
