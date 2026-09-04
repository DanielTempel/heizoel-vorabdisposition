import type {
  CustomerAnswerRequest,
  CustomerConfirmationPreview,
} from '../types/confirmation'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

async function handleBackendResponse(response: Response) {
  if (response.ok) {
    return response
  }

  throw new Error(`Backend request failed with status ${response.status}`)
}

export async function getConfirmationPreview(
  token: string,
): Promise<CustomerConfirmationPreview> {
  const response = await fetch(
    `${apiBaseUrl}/api/customer/confirmations/${token}`,
  )

  return (await handleBackendResponse(response))
    .json() as Promise<CustomerConfirmationPreview>
}

export async function submitCustomerResponse(
  token: string,
  request: CustomerAnswerRequest,
): Promise<void> {
  const response = await fetch(
    `${apiBaseUrl}/api/customer/confirmations/${token}/response`,
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
