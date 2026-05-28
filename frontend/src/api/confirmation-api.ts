import type {
  CustomerAnswerRequest,
  CustomerConfirmationPreview,
} from '../types/confirmation'

type ConfirmationApiMode = 'mock' | 'backend'

const apiMode = getApiMode()
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

const mockConfirmationPreview: CustomerConfirmationPreview = {
  externalOrderId: 'A-3002',
  customerName: 'Max Müller',
  deliveryAddress: 'Beispielstraße 12, 97070 Würzburg',
  product: 'Heizöl Standard',
  quantityLiters: 3000,
  deliveryDate: '2026-06-12',
  deliveryWindowStart: '10:00:00',
  deliveryWindowEnd: '11:00:00',
}

function getApiMode(): ConfirmationApiMode {
  const mode = import.meta.env.VITE_CONFIRMATION_API_MODE

  if (mode === 'backend') {
    return 'backend'
  }

  return 'mock'
}

function delay(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

async function handleBackendResponse(response: Response) {
  if (response.ok) {
    return
  }

  throw new Error(`Backend request failed with status ${response.status}`)
}

export async function getConfirmationPreview(
  token: string,
): Promise<CustomerConfirmationPreview> {
  if (apiMode === 'mock') {
    console.log('Using mock confirmation preview:', { token })

    await delay(300)

    return mockConfirmationPreview
  }

  const response = await fetch(
    `${apiBaseUrl}/api/customer/confirmations/${token}`,
  )

  await handleBackendResponse(response)

  return response.json() as Promise<CustomerConfirmationPreview>
}

export async function confirmDelivery(
  token: string,
  request: CustomerAnswerRequest,
): Promise<void> {
  if (apiMode === 'mock') {
    console.log('Using mock confirm:', { token, request })

    await delay(300)
    return
  }

  const response = await fetch(
    `${apiBaseUrl}/api/customer/confirmations/${token}/confirm`,
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

export async function rejectDelivery(
  token: string,
  request: CustomerAnswerRequest,
): Promise<void> {
  if (apiMode === 'mock') {
    console.log('Using mock reject:', { token, request })

    await delay(300)
    return
  }

  const response = await fetch(
    `${apiBaseUrl}/api/customer/confirmations/${token}/reject`,
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
