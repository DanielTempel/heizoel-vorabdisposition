import type {
  ConfirmationStatus,
  CustomerAnswerRequest,
  CustomerConfirmationPreview,
} from '../types/confirmation'

type ConfirmationApiMode = 'mock' | 'backend'

const apiMode = getApiMode()
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

const baseMockConfirmationPreview: CustomerConfirmationPreview = {
  externalOrderId: 'A-3002',
  customerName: 'Max Müller',
  deliveryAddress: 'Domstrasse 40, 97070 Würzburg',
  product: 'Heizöl Standard',
  quantityLiters: 3000,
  deliveryDate: '2026-06-29',
  deliveryWindowStart: '10:00:00',
  deliveryWindowEnd: '11:00:00',
  priceDisplayText: '100 EUR',
  confirmationStatus: 'SENT',
}

const mockConfirmationStatusByToken = new Map<string, ConfirmationStatus>()

function getApiMode(): ConfirmationApiMode {
  const mode = import.meta.env.VITE_CONFIRMATION_API_MODE

  if (mode === 'mock') {
    return 'mock'
  }

  return 'backend'
}

function delay(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

function getInitialMockStatus(token: string): ConfirmationStatus {
  switch (token) {
    case 'mock-confirmed':
    case 'mock-no-tracking':
    case 'mock-arrived':
    case 'mock-driver-error':
      return 'CONFIRMED'
    case 'mock-rejected':
      return 'REJECTED'
    case 'mock-no-response':
    case 'mock-expired':
      return 'NO_RESPONSE'
    default:
      return 'SENT'
  }
}

function getMockConfirmationPreview(token: string): CustomerConfirmationPreview {
  if (token === 'mock-error') {
    throw new Error('Mock confirmation request failed')
  }

  const confirmationStatus =
    mockConfirmationStatusByToken.get(token) ?? getInitialMockStatus(token)

  return {
    ...baseMockConfirmationPreview,
    externalOrderId: token.startsWith('mock-')
      ? token.toUpperCase().replaceAll('-', '_')
      : baseMockConfirmationPreview.externalOrderId,
    confirmationStatus,
  }
}

async function handleBackendResponse(response: Response) {
  if (response.ok) {
    return response
  }

  throw new Error(`Backend request failed with status ${response.status}`)
}

export async function getConfirmationPreview(
  token: string,
): Promise<CustomerConfirmationPreview> {
  if (apiMode === 'mock') {
    console.log('Using mock confirmation preview:', { token })

    await delay(300)

    return getMockConfirmationPreview(token)
  }

  const response = await fetch(
    `${apiBaseUrl}/api/customer/confirmations/${token}`,
  )

  return (await handleBackendResponse(response))
    .json() as Promise<CustomerConfirmationPreview>
}

export async function confirmDelivery(
  token: string,
  request: CustomerAnswerRequest,
): Promise<void> {
  if (apiMode === 'mock') {
    console.log('Using mock confirm:', { token, request })

    await delay(300)
    mockConfirmationStatusByToken.set(token, 'CONFIRMED')
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
    mockConfirmationStatusByToken.set(token, 'REJECTED')
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
