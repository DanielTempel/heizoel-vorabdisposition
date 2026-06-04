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
  deliveryAddress: 'Domstraße 40, 97070 Würzburg',
  locationX: 9.882,
  locationY: 49.8166,
  targetLocationX: 9.9372,
  targetLocationY: 49.7935,
  product: 'Heizöl Standard',
  quantityLiters: 3000,
  deliveryDate: '2026-06-12',
  deliveryWindowStart: '10:00:00',
  deliveryWindowEnd: '11:00:00',
  confirmationStatus: 'SENT',
}

let mockSimulationTimer: number | null = null

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
    return response
  }

  throw new Error(`Backend request failed with status ${response.status}`)
}

function distanceInKilometers(
  startLatitude: number,
  startLongitude: number,
  targetLatitude: number,
  targetLongitude: number,
) {
  const earthRadiusKilometers = 6371
  const latitudeDistance = ((targetLatitude - startLatitude) * Math.PI) / 180
  const longitudeDistance =
    ((targetLongitude - startLongitude) * Math.PI) / 180
  const a =
    Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2) +
    Math.cos((startLatitude * Math.PI) / 180) *
      Math.cos((targetLatitude * Math.PI) / 180) *
      Math.sin(longitudeDistance / 2) *
      Math.sin(longitudeDistance / 2)

  return earthRadiusKilometers * (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)))
}

function advanceMockVehicle() {
  const remainingKilometers = distanceInKilometers(
    mockConfirmationPreview.locationY,
    mockConfirmationPreview.locationX,
    mockConfirmationPreview.targetLocationY,
    mockConfirmationPreview.targetLocationX,
  )

  if (remainingKilometers <= 0.08) {
    mockConfirmationPreview.locationX = mockConfirmationPreview.targetLocationX
    mockConfirmationPreview.locationY = mockConfirmationPreview.targetLocationY

    if (mockSimulationTimer !== null) {
      window.clearInterval(mockSimulationTimer)
      mockSimulationTimer = null
    }

    return
  }

  const nextStepKilometers = Math.min(
    Math.max(remainingKilometers * 0.14, 0.18),
    0.65,
  )
  const stepRatio = Math.min(nextStepKilometers / remainingKilometers, 0.18)

  mockConfirmationPreview.locationX +=
    (mockConfirmationPreview.targetLocationX - mockConfirmationPreview.locationX) *
    stepRatio
  mockConfirmationPreview.locationY +=
    (mockConfirmationPreview.targetLocationY - mockConfirmationPreview.locationY) *
    stepRatio
}

export async function getConfirmationPreview(
  token: string,
): Promise<CustomerConfirmationPreview> {
  if (apiMode === 'mock') {
    console.log('Using mock confirmation preview:', { token })

    await delay(300)

    return { ...mockConfirmationPreview }
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
    mockConfirmationPreview.confirmationStatus = 'CONFIRMED'
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
    mockConfirmationPreview.confirmationStatus = 'REJECTED'
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

export async function startVehicleSimulation(
  externalOrderId: string,
): Promise<void> {
  if (apiMode === 'mock') {
    console.log('Using mock vehicle simulation start:', { externalOrderId })

    if (mockSimulationTimer === null) {
      mockSimulationTimer = window.setInterval(advanceMockVehicle, 1000)
    }

    return
  }

  const response = await fetch(
    `${apiBaseUrl}/api/dispo/confirmation-requests/${externalOrderId}/vehicle-simulation/start`,
    {
      method: 'POST',
    },
  )

  await handleBackendResponse(response)
}
