const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export async function exchangeDashboardAccessCode(code: string): Promise<void> {
  const response = await fetch(`${apiBaseUrl}/api/dashboard/auth/exchange`, {
    method: 'POST',
    headers: {
      'Content-Type': 'text/plain',
    },
    credentials: 'include',
    body: code,
  })

  if (!response.ok) {
    throw new Error(
      `Dashboard authentication failed with status ${response.status}.`,
    )
  }
}
