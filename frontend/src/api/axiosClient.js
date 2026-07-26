import axios from 'axios';

// The access token lives only in memory (this module-level variable) - never in
// localStorage/sessionStorage, so it isn't reachable by an XSS payload reading
// browser storage. It is lost on a hard page reload by design; AuthContext
// recovers it via a silent /auth/refresh call (which relies on the httpOnly
// refresh cookie) when the app mounts.
let accessToken = null;
let logoutHandler = null;
let refreshPromise = null;

export function setAccessToken(token) {
  accessToken = token;
}

export function getAccessToken() {
  return accessToken;
}

// AuthContext registers itself here so this module (outside the React tree)
// can clear React auth state when a refresh ultimately fails.
export function registerLogoutHandler(handler) {
  logoutHandler = handler;
}

const axiosClient = axios.create({
  baseURL: '/api/v1',
  withCredentials: true,
});

axiosClient.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { config, response } = error;
    const isAuthEndpoint = config?.url?.startsWith('/auth/');

    if (response?.status === 401 && config && !config._retry && !isAuthEndpoint) {
      config._retry = true;
      try {
        if (!refreshPromise) {
          refreshPromise = axiosClient
            .post('/auth/refresh')
            .finally(() => {
              refreshPromise = null;
            });
        }
        const refreshResponse = await refreshPromise;
        setAccessToken(refreshResponse.data.accessToken);
        config.headers.Authorization = `Bearer ${refreshResponse.data.accessToken}`;
        return axiosClient(config);
      } catch (refreshError) {
        setAccessToken(null);
        if (logoutHandler) {
          logoutHandler();
        }
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default axiosClient;
