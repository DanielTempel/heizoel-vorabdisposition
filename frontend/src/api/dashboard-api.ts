import type { DashboardFilters, ToursPage } from '../types/dashboard'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

type GetToursInput = DashboardFilters & {
  page: number,
}

export async function getTours(
  input: GetToursInput,
  signal?: AbortSignal,
): Promise<ToursPage> {
  const searchParams = new URLSearchParams({ page: input.page.toString() })

  if (input.search.trim() !== '') {
    searchParams.set('search', input.search.trim())
  }

  input.statuses.forEach((status) => searchParams.append('statuses', status))

  if (input.dateFrom !== '') {
    searchParams.set('dateFrom', input.dateFrom)
  }

  if (input.dateTo !== '') {
    searchParams.set('dateTo', input.dateTo)
  }

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
