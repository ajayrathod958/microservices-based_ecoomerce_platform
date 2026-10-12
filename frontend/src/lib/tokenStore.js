// The access token lives only in memory (gone on reload, unreachable from storage-reading XSS).
// The refresh token lives in sessionStorage: it survives a reload in this tab but not a closed tab.
// Production hardening: serve the refresh token as an httpOnly, SameSite cookie from the auth service instead.
const REFRESH_KEY = 'microshop.refreshToken';
let accessToken = null;

function storage() {
  try {
    return window.sessionStorage;
  } catch {
    return null;
  }
}

export const tokenStore = {
  getAccessToken: () => accessToken,
  getRefreshToken() {
    try {
      return storage()?.getItem(REFRESH_KEY) || null;
    } catch {
      return null;
    }
  },
  setTokens(access, refresh) {
    accessToken = access;
    if (refresh) {
      try {
        storage()?.setItem(REFRESH_KEY, refresh);
      } catch {
        /* storage unavailable: the session just won't survive a reload */
      }
    }
  },
  clear() {
    accessToken = null;
    try {
      storage()?.removeItem(REFRESH_KEY);
    } catch {
      /* ignore */
    }
  },
  hasSession() {
    return Boolean(accessToken) || Boolean(this.getRefreshToken());
  },
};
