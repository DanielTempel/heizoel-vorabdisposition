import { delay, http, HttpResponse } from 'msw'
import type {
  ConfirmationStatus,
  CustomerConfirmationPreview,
} from '../types/confirmation'
import type { DriverLocation, TrackingInfo } from '../types/tracking'
import { dashboardHandlers } from './dashboard-handlers'

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

const mockTrackingInfo: TrackingInfo = {
  trackingAvailable: true,
  targetLocationX: 9.9372,
  targetLocationY: 49.7935,
}

const mockDriverRoute: DriverLocation[] = [
  { locationX: 9.882, locationY: 49.8166 },
  { locationX: 9.8974, locationY: 49.8108 },
  { locationX: 9.9149, locationY: 49.804 },
  { locationX: 9.9281, locationY: 49.7975 },
]

const mockConfirmationStatusByToken = new Map<string, ConfirmationStatus>()
const mockDriverRouteIndexByToken = new Map<string, number>()

function getToken(tokenParam: string | readonly string[] | undefined) {
  if (Array.isArray(tokenParam)) {
    return tokenParam[0] ?? 'mock-token'
  }

  return tokenParam ?? 'mock-token'
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

function getMockTrackingInfo(token: string): TrackingInfo {
  if (token === 'mock-no-tracking') {
    return {
      trackingAvailable: false,
      targetLocationX: null,
      targetLocationY: null,
    }
  }

  return { ...mockTrackingInfo }
}

function getMockDriverLocation(token: string): DriverLocation {
  if (token === 'mock-arrived') {
    return {
      locationX: mockTrackingInfo.targetLocationX ?? 9.9372,
      locationY: mockTrackingInfo.targetLocationY ?? 49.7935,
    }
  }

  const routeIndex = mockDriverRouteIndexByToken.get(token) ?? 0
  const currentLocation = mockDriverRoute[routeIndex]
  mockDriverRouteIndexByToken.set(
    token,
    (routeIndex + 1) % mockDriverRoute.length,
  )

  return currentLocation
}

export const handlers = [
  http.get(
    `${apiBaseUrl}/api/customer/confirmations/:token`,
    async ({ params }) => {
      await delay(300)

      const token = getToken(params.token)

      if (token === 'mock-error') {
        return new HttpResponse(null, { status: 500 })
      }

      return HttpResponse.json(getMockConfirmationPreview(token))
    },
  ),

  http.post(
    `${apiBaseUrl}/api/customer/confirmations/:token/confirm`,
    async ({ params }) => {
      await delay(300)

      const token = getToken(params.token)
      mockConfirmationStatusByToken.set(token, 'CONFIRMED')

      return new HttpResponse(null, { status: 204 })
    },
  ),

  http.post(
    `${apiBaseUrl}/api/customer/confirmations/:token/reject`,
    async ({ params }) => {
      await delay(300)

      const token = getToken(params.token)
      mockConfirmationStatusByToken.set(token, 'REJECTED')

      return new HttpResponse(null, { status: 204 })
    },
  ),

  http.get(
    `${apiBaseUrl}/api/customer/confirmations/:token/tracking-info`,
    async ({ params }) => {
      await delay(300)

      return HttpResponse.json(getMockTrackingInfo(getToken(params.token)))
    },
  ),

  http.get(
    `${apiBaseUrl}/api/customer/confirmations/:token/driver-location`,
    async ({ params }) => {
      await delay(300)

      const token = getToken(params.token)

      if (token === 'mock-driver-error') {
        return new HttpResponse(null, { status: 502 })
      }

      return HttpResponse.json(getMockDriverLocation(token))
    },
  ),
  ...dashboardHandlers,
]
