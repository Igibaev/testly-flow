const BASE_URL = '/api';

async function request(path, { method = 'GET', body, admin = false, isMultipart = false, keepalive = false } = {}) {
  const headers = {};
  if (!isMultipart) {
    headers['Content-Type'] = 'application/json';
  }
  if (admin) {
    headers['X-Admin-Password'] = sessionStorage.getItem('adminPassword') || '';
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    keepalive,
    body: isMultipart ? body : body ? JSON.stringify(body) : undefined,
  });

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const message = data?.error || `Ошибка запроса (${response.status})`;
    const error = new Error(message);
    error.status = response.status;
    error.details = data?.details || [];
    throw error;
  }

  return data;
}

export const api = {
  get: (path, opts) => request(path, { ...opts, method: 'GET' }),
  post: (path, body, opts) => request(path, { ...opts, method: 'POST', body }),
  put: (path, body, opts) => request(path, { ...opts, method: 'PUT', body }),
  del: (path, opts) => request(path, { ...opts, method: 'DELETE' }),
};

/**
 * Fire-and-forget flush of an answer's accumulated timing right before the tab closes.
 * The answers endpoint is PUT, and `navigator.sendBeacon` only ever sends POST, so this
 * relies on `fetch(..., { keepalive: true })`, which Chrome/Firefox/Safari all keep alive
 * past `beforeunload`/`visibilitychange` for small bodies.
 */
export function putAnswerKeepalive(attemptId, questionId, body) {
  const url = `${BASE_URL}/attempts/${attemptId}/answers/${questionId}`;
  fetch(url, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
    keepalive: true,
  }).catch(() => {});
}
