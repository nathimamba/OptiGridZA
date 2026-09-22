const GATEWAY_URL = 'http://localhost:8080'

export async function apiFetch(path, options = {}) {
  const token = localStorage.getItem('token')
  const response = await fetch(`${GATEWAY_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })
  if (!response.ok) {
    const body = await response.text().catch(() => '')
    throw new Error(body || `Request failed: ${response.status}`)
  }
  return response.status === 204 ? null : response.json()
}