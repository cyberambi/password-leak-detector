import axiosClient from './axiosClient';

export function register(username, password) {
  return axiosClient.post('/auth/register', { username, password }).then((res) => res.data);
}

export function login(username, password) {
  return axiosClient.post('/auth/login', { username, password }).then((res) => res.data);
}

export function refresh() {
  return axiosClient.post('/auth/refresh').then((res) => res.data);
}

export function logout() {
  return axiosClient.post('/auth/logout').then((res) => res.data);
}
