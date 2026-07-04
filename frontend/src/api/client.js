const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
    ...options,
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    const error = new Error(errorBody?.message || `Request failed: ${response.status}`);
    error.status = response.status;
    error.body = errorBody;
    throw error;
  }

  return response.json();
}

export function getDashboard() {
  return request('/api/dashboard');
}

export function getReviewPackets() {
  return request('/api/review-packets');
}

export function getRepositoryProfiles() {
  return request('/api/repository-profiles');
}

export function createRepositoryProfile(payload) {
  return request('/api/repository-profiles', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getSessions() {
  return request('/api/sessions');
}

export function createSession(payload) {
  return request('/api/sessions', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function importEvents(sessionId, events) {
  return request(`/api/sessions/${sessionId}/events/import`, {
    method: 'POST',
    body: JSON.stringify({ events }),
  });
}

export function importDiff(sessionId, diffText) {
  return request(`/api/sessions/${sessionId}/diff`, {
    method: 'POST',
    body: JSON.stringify({ diffText }),
  });
}

export function importTestOutput(sessionId, payload) {
  return request(`/api/sessions/${sessionId}/test-output`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function runRiskAnalysis(sessionId) {
  return request(`/api/sessions/${sessionId}/risk-analysis`, {
    method: 'POST',
  });
}

export function generateReviewPacket(sessionId) {
  return request(`/api/sessions/${sessionId}/review-packet`, {
    method: 'POST',
  });
}

export function getAiSummary(reviewPacketId) {
  return request(`/api/review-packets/${reviewPacketId}/ai-summary`);
}

export function generateAiSummary(reviewPacketId) {
  return request(`/api/review-packets/${reviewPacketId}/ai-summary`, {
    method: 'POST',
  });
}
