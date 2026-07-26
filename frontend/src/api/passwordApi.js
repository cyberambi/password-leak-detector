import axiosClient from './axiosClient';

export function checkBreach(password) {
  return axiosClient.post('/passwords/check-breach', { password }).then((res) => res.data);
}

export function analyzeStrength(password) {
  return axiosClient.post('/passwords/analyze-strength', { password }).then((res) => res.data);
}

export function generatePassword(options) {
  return axiosClient.post('/passwords/generate', options).then((res) => res.data);
}

export function listHistory() {
  return axiosClient.get('/passwords/history').then((res) => res.data);
}

export function getHistoryEntry(id) {
  return axiosClient.get(`/passwords/history/${id}`).then((res) => res.data);
}

export function createHistoryEntry(entry) {
  return axiosClient.post('/passwords/history', entry).then((res) => res.data);
}

export function updateHistoryEntry(id, entry) {
  return axiosClient.put(`/passwords/history/${id}`, entry).then((res) => res.data);
}

export function deleteHistoryEntry(id) {
  return axiosClient.delete(`/passwords/history/${id}`).then((res) => res.data);
}
