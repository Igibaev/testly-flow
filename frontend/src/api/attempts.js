import { api } from './client';

export const startAttempt = (payload) => api.post('/attempts/start', payload);

export const getAttemptState = (attemptId) => api.get(`/attempts/${attemptId}`);

export const updateAnswer = (attemptId, questionId, payload) =>
  api.put(`/attempts/${attemptId}/answers/${questionId}`, payload);

export const submitAttempt = (attemptId, payload) => api.post(`/attempts/${attemptId}/submit`, payload);
