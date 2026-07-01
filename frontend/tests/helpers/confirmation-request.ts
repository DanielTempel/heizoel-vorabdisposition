import { expect, type APIRequestContext } from '@playwright/test'

export const BACKEND_URL = 'http://localhost:8080'

function todayIsoDate() {
  return new Date().toISOString().slice(0, 10)
}

export async function createConfirmationRequest(
  request: APIRequestContext,
  externalOrderId: string,
  customerEmail: string,
) {
  const response = await request.post(
    `${BACKEND_URL}/api/dispo/confirmation-requests`,
    {
      data: {
        externalOrderId,
        customerName: 'Max Müller',
        communicationChannel: 'EMAIL',
        customerEmail,
        customerPhoneNumber: null,
        deliveryAddress: 'Domstraße 40, 97070 Würzburg',
        product: 'Heizöl Standard',
        quantityLiters: 3000,
        deliveryDate: todayIsoDate(),
        deliveryWindowStart: '10:00',
        deliveryWindowEnd: '11:00',
        responseDeadlineHours: 24,
        priceDisplayText: '100 EUR',
      },
    },
  )

  expect(response.ok()).toBeTruthy()
}
