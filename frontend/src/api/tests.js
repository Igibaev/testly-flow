import { api } from './client';

export const listTests = () => api.get('/tests');

export const getTest = (testId) => api.get(`/tests/${testId}`);

export const startAttempt = (testId, payload) =>
  api.post(`/tests/${testId}/attempts/start`, payload);

export const submitAttempt = (attemptId, payload) =>
  api.post(`/attempts/${attemptId}/submit`, payload);
