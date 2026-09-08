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
  const pad = (value: number) => String(value).padStart(2, '0')
  const now = new Date()
  const berlinParts = Object.fromEntries(
    new Intl.DateTimeFormat('en-US', {
      timeZone: 'Europe/Berlin',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      hourCycle: 'h23',
    })
      .formatToParts(now)
      .map((part) => [part.type, part.value]),
  )
  const currentHour = Number(berlinParts.hour)
  let effectiveDateOffsetDays = deliveryDateOffsetDays
  let deliveryWindowStart: string
  let deliveryWindowEnd: string

  if (deliveryDateOffsetDays > 0) {
    deliveryWindowStart = '10:00'
    deliveryWindowEnd = '11:00'
  } else if (currentHour < 22) {
    deliveryWindowStart = `${pad(currentHour + 1)}:00`
    deliveryWindowEnd = `${pad(currentHour + 2)}:00`
  } else if (currentHour === 22) {
    deliveryWindowStart = '23:00'
    deliveryWindowEnd = '23:59'
  } else {
    effectiveDateOffsetDays = 1
    deliveryWindowStart = '00:30'
    deliveryWindowEnd = '01:30'
  }

  const deliveryDate = new Date(
    Date.UTC(
      Number(berlinParts.year),
      Number(berlinParts.month) - 1,
      Number(berlinParts.day),
    ),
  )
  deliveryDate.setUTCDate(
    deliveryDate.getUTCDate() +
      effectiveDateOffsetDays,
  )

  return {
    deliveryDate:
      `${deliveryDate.getUTCFullYear()}-` +
      `${pad(deliveryDate.getUTCMonth() + 1)}-` +
      pad(deliveryDate.getUTCDate()),
    deliveryWindowStart,
    deliveryWindowEnd,
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
