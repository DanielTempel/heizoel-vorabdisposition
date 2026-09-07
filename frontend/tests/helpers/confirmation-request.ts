import { env } from 'node:process'
import { expect, type APIRequestContext } from '@playwright/test'

export const BACKEND_URL =
  env.E2E_BACKEND_URL ?? 'http://localhost:8080'

type ConfirmationRequestOptions = {
  deliveryDateOffsetDays?: number
}

function createFutureDeliveryWindow(
  deliveryDateOffsetDays: number,
) {
  const start = new Date(Date.now() + 60 * 60 * 1_000)
  start.setDate(
    start.getDate() + deliveryDateOffsetDays,
  )
  start.setMinutes(0, 0, 0)

  const end = new Date(start.getTime() + 60 * 60 * 1_000)
  const pad = (value: number) => String(value).padStart(2, '0')
  const formatDate = (value: Date) =>
    `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`
  const formatTime = (value: Date) =>
    `${pad(value.getHours())}:${pad(value.getMinutes())}`

  return {
    deliveryDate: formatDate(start),
    deliveryWindowStart: formatTime(start),
    deliveryWindowEnd: formatTime(end),
  }
}

export async function createConfirmationRequest(
  request: APIRequestContext,
  externalOrderId: string,
  customerEmail: string,
  options: ConfirmationRequestOptions = {},
) {
  const apiKey = env.E2E_API_KEY
  const deliveryWindow = createFutureDeliveryWindow(
    options.deliveryDateOffsetDays ?? 0,
  )

  if (!apiKey) {
    throw new Error('E2E_API_KEY environment variable is required')
  }

  const response = await request.post(
    `${BACKEND_URL}/api/dispo/confirmation-requests`,
    {
      headers: {
        'X-API-Key': apiKey,
      },
      data: {
        externalOrderId,
        tourNumber: '17',
        vehicleLicensePlate: 'WUE-AB 123',
        customerName: 'Max Müller',
        communicationChannel: 'EMAIL',
        customerEmail,
        customerPhoneNumber: null,
        deliveryAddress: 'Domstraße 40, 97070 Würzburg',
        product: 'Heizöl Standard',
        quantityLiters: 3000,
        deliveryDate: deliveryWindow.deliveryDate,
        deliveryWindowStart: deliveryWindow.deliveryWindowStart,
        deliveryWindowEnd: deliveryWindow.deliveryWindowEnd,
        responseDeadlineHours: 24,
        priceDisplayText: '100 EUR',
      },
    },
  )

  const responseBody = await response.text()

  await expect(
    response,
    `Failed to create confirmation request. HTTP ${response.status()}: ${responseBody}`,
  ).toBeOK()

  return deliveryWindow
}
