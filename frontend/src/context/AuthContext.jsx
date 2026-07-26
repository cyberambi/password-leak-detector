import { createContext, useCallback, useEffect, useState } from 'react';
import * as authApi from '../api/authApi';
import { registerLogoutHandler, setAccessToken } from '../api/axiosClient';

export const AuthContext = createContext(null);

// Decodes the "sub" claim from an access token purely for display purposes
// (e.g. showing the username in the nav bar). The signature is never
// verified client-side - this is not used for any authorization decision,
// the backend re-validates the token on every request.
function usernameFromToken(token) {
  try {
    const payload = token.split('.')[1];
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
    return decoded.sub ?? null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [username, setUsername] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  // True until the initial silent-refresh attempt (using the httpOnly refresh
  // cookie, if any) resolves - avoids flashing a "logged out" UI on reload.
  const [isLoading, setIsLoading] = useState(true);

  const clearSession = useCallback(() => {
    setAccessToken(null);
    setUsername(null);
    setIsAuthenticated(false);
  }, []);

  useEffect(() => {
    registerLogoutHandler(clearSession);
  }, [clearSession]);

  useEffect(() => {
    authApi
      .refresh()
      .then((data) => {
        setAccessToken(data.accessToken);
        setUsername(usernameFromToken(data.accessToken));
        setIsAuthenticated(true);
      })
      .catch(() => {
        clearSession();
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [clearSession]);

  const login = useCallback(async (usernameValue, password) => {
    const data = await authApi.login(usernameValue, password);
    setAccessToken(data.accessToken);
    setUsername(usernameFromToken(data.accessToken));
    setIsAuthenticated(true);
  }, []);

  const register = useCallback(async (usernameValue, password) => {
    await authApi.register(usernameValue, password);
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      clearSession();
    }
  }, [clearSession]);

  const value = {
    username,
    isAuthenticated,
    isLoading,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
