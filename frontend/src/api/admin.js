import { api } from './client';

export const checkAdminPassword = () => api.get('/admin/auth/check', { admin: true });

export const listAdminTests = () => api.get('/admin/tests', { admin: true });

export const uploadTest = (formData) =>
  api.post('/admin/tests', formData, { admin: true, isMultipart: true });

export const listAdminCategories = () => api.get('/admin/categories', { admin: true });

export const createCategory = (payload) => api.post('/admin/categories', payload, { admin: true });

export const updateCategory = (id, payload) => api.put(`/admin/categories/${id}`, payload, { admin: true });

export const deleteCategory = (id) => api.del(`/admin/categories/${id}`, { admin: true });

export const updateCategoryPrepLinks = (categoryId, links) =>
  api.put(`/admin/categories/${categoryId}/prep-links`, { links }, { admin: true });

export const searchAttempts = (params = {}) => {
  const query = new URLSearchParams();
  if (params.team) query.set('team', params.team);
  query.set('page', params.page ?? 0);
  query.set('size', params.size ?? 20);
  return api.get(`/admin/attempts?${query.toString()}`, { admin: true });
};

export const getAttemptDetail = (attemptId) => api.get(`/admin/attempts/${attemptId}`, { admin: true });

export const getMetrics = (params = {}) => {
  const query = new URLSearchParams();
  if (params.categoryId) query.set('categoryId', params.categoryId);
  if (params.from) query.set('from', params.from);
  if (params.to) query.set('to', params.to);
  const qs = query.toString();
  return api.get(`/admin/metrics${qs ? `?${qs}` : ''}`, { admin: true });
};

export const listEmployees = () => api.get('/admin/employees', { admin: true });

export const getEmployeeCard = (firstName, lastName, team) => {
  const query = new URLSearchParams({ firstName, lastName, team });
  return api.get(`/admin/employees/card?${query.toString()}`, { admin: true });
};
