import type {
  CustomerAnswerRequest,
  CustomerConfirmationPreview,
} from '../types/confirmation'

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

function delay(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

export async function getConfirmationPreview(
  token: string,
): Promise<CustomerConfirmationPreview> {
  console.log('Using mock token:', token)

  await delay(300)

  return mockConfirmationPreview
}

export async function confirmDelivery(
  token: string,
  request: CustomerAnswerRequest,
): Promise<void> {
  console.log('Mock confirm:', { token, request })

  await delay(300)
}

export async function rejectDelivery(
  token: string,
  request: CustomerAnswerRequest,
): Promise<void> {
  console.log('Mock reject:', { token, request })

  await delay(300)
}
