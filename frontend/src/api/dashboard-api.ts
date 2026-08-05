import type { ToursPage } from '../types/dashboard'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export async function getTours(
  page: number,
  signal?: AbortSignal,
): Promise<ToursPage> {
  const searchParams = new URLSearchParams({ page: page.toString() })
  const response = await fetch(
    `${apiBaseUrl}/api/dispo/dashboard/tours?${searchParams}`,
    {
      cache: 'no-store',
      signal,
    },
  )

  if (!response.ok) {
    throw new Error(`Dashboard request failed with status ${response.status}.`)
  }

  return response.json() as Promise<ToursPage>
}
