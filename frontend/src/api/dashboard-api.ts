import type { DashboardConfirmation } from '../types/dashboard'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

async function handleBackendResponse(response: Response) {
  if (response.ok) {
    return response
  }

  throw new Error(`Backend request failed with status ${response.status}`)
}

export async function getDashboardConfirmations(): Promise<
  DashboardConfirmation[]
> {
  const response = await fetch(`${apiBaseUrl}/api/dashboard/confirmations`, {
    cache: 'no-store',
  })

  return (await handleBackendResponse(response))
    .json() as Promise<DashboardConfirmation[]>
}
