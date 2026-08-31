import { api } from './client';

export const checkAdminPassword = () => api.get('/admin/auth/check', { admin: true });

export const listAdminTests = () => api.get('/admin/tests', { admin: true });

export const uploadTest = (formData) =>
  api.post('/admin/tests', formData, { admin: true, isMultipart: true });

export const updatePrepLinks = (testId, links) =>
  api.put(`/admin/tests/${testId}/prep-links`, { links }, { admin: true });

export const searchAttempts = (params = {}) => {
  const query = new URLSearchParams();
  if (params.testId) query.set('testId', params.testId);
  if (params.team) query.set('team', params.team);
  query.set('page', params.page ?? 0);
  query.set('size', params.size ?? 20);
  return api.get(`/admin/attempts?${query.toString()}`, { admin: true });
};

export const getAttemptDetail = (attemptId) => api.get(`/admin/attempts/${attemptId}`, { admin: true });

export const getMetrics = (testId) => {
  const query = testId ? `?testId=${testId}` : '';
  return api.get(`/admin/metrics${query}`, { admin: true });
};
