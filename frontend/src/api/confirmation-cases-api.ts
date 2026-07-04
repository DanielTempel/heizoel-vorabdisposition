import type {
  NewTimeWindowRequest,
  ConfirmationCaseDetail,
  ConfirmationCaseSummary,
} from '../types/confirmation-cases'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

async function handleBackendResponse(response: Response) {
  if (response.ok) {
    return response
  }

  throw new Error(`Backend request failed with status ${response.status}`)
}

export async function getConfirmationCases(): Promise<ConfirmationCaseSummary[]> {
  const response = await fetch(`${apiBaseUrl}/api/confirmation-cases`, {
    cache: 'no-store',
  })

  return (await handleBackendResponse(response)).json() as Promise<
    ConfirmationCaseSummary[]
  >
}

export async function getConfirmationCaseDetail(
  orderId: string,
): Promise<ConfirmationCaseDetail> {
  const response = await fetch(`${apiBaseUrl}/api/confirmation-cases/${orderId}`, {
    cache: 'no-store',
  })

  return (await handleBackendResponse(response)).json() as Promise<ConfirmationCaseDetail>
}

export async function sendNewTimeWindow(
  orderId: string,
  request: NewTimeWindowRequest,
): Promise<void> {
  const response = await fetch(
    `${apiBaseUrl}/api/confirmation-cases/${orderId}/reschedule`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    },
  )

  await handleBackendResponse(response)
}

export async function resolveConfirmationCase(orderId: string): Promise<void> {
  const response = await fetch(`${apiBaseUrl}/api/confirmation-cases/${orderId}/resolve`, {
    method: 'POST',
  })

  await handleBackendResponse(response)
}
