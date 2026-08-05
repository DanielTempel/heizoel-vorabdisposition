import { delay, http, HttpResponse } from 'msw'
import type {
  ConfirmationStatus,
  CustomerConfirmationPreview,
} from '../types/confirmation'
import type { DriverLocation, TrackingInfo } from '../types/tracking'
import { dashboardHandlers } from './dashboard-handlers'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

const defaultPreview: CustomerConfirmationPreview = {
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

const rejectedPreview: CustomerConfirmationPreview = {
  externalOrderId: 'A-3008',
  customerName: 'Max Mustermann',
  deliveryAddress: 'Valentin-Becker-Straße 2, 97070 Würzburg',
  product: 'Heizöl',
  quantityLiters: 3000,
  deliveryDate: '2026-06-25',
  deliveryWindowStart: '16:00:00',
  deliveryWindowEnd: '17:00:00',
  priceDisplayText: '112,50 EUR / 100 Liter',
  confirmationStatus: 'REJECTED',
}

const rejectedEveningPreview: CustomerConfirmationPreview = {
  externalOrderId: 'A-3012',
  customerName: 'Weber Haustechnik',
  deliveryAddress: 'Gartenstraße 89, 70173 Stuttgart',
  product: 'Heizoel Standard',
  quantityLiters: 2500,
  deliveryDate: '2026-06-25',
  deliveryWindowStart: '11:00:00',
  deliveryWindowEnd: '12:00:00',
  priceDisplayText: '109,90 EUR / 100 Liter',
  confirmationStatus: 'REJECTED',
}

const noResponsePreview: CustomerConfirmationPreview = {
  externalOrderId: 'A-4016',
  customerName: 'Neumann Brennstoffe',
  deliveryAddress: 'Schloßallee 56, 14059 Berlin',
  product: 'Heizoel Premium',
  quantityLiters: 3200,
  deliveryDate: '2026-06-24',
  deliveryWindowStart: '14:00:00',
  deliveryWindowEnd: '15:00:00',
  priceDisplayText: '118,40 EUR / 100 Liter',
  confirmationStatus: 'NO_RESPONSE',
}

const trackingInfo: TrackingInfo = {
  trackingAvailable: true,
  targetLocationX: 9.9372,
  targetLocationY: 49.7935,
}

const driverRoute: DriverLocation[] = [
  { locationX: 9.882, locationY: 49.8166 },
  { locationX: 9.8974, locationY: 49.8108 },
  { locationX: 9.9149, locationY: 49.804 },
  { locationX: 9.9281, locationY: 49.7975 },
]

const statusByToken = new Map<string, ConfirmationStatus>()
const routeIndexByToken = new Map<string, number>()

function getToken(tokenParam: string | readonly string[] | undefined) {
  if (Array.isArray(tokenParam)) {
    return tokenParam[0] ?? 'mock-token'
  }

  return tokenParam ?? 'mock-token'
}

function getBasePreview(token: string) {
  if (token === 'mock-rejected') {
    return rejectedPreview
  }

  if (token === 'mock-rejected-evening') {
    return rejectedEveningPreview
  }

  if (token === 'mock-no-response' || token === 'mock-expired') {
    return noResponsePreview
  }

  return defaultPreview
}

function getInitialStatus(token: string): ConfirmationStatus {
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

function getPreview(token: string): CustomerConfirmationPreview {
  const basePreview = getBasePreview(token)

  return {
    ...basePreview,
    confirmationStatus: statusByToken.get(token) ?? getInitialStatus(token),
  }
}

function getTrackingInfo(token: string): TrackingInfo {
  if (token === 'mock-no-tracking') {
    return {
      trackingAvailable: false,
      targetLocationX: null,
      targetLocationY: null,
    }
  }

  return trackingInfo
}

function getDriverLocation(token: string): DriverLocation {
  if (token === 'mock-arrived') {
    return {
      locationX: trackingInfo.targetLocationX ?? 9.9372,
      locationY: trackingInfo.targetLocationY ?? 49.7935,
    }
  }

  const routeIndex = routeIndexByToken.get(token) ?? 0
  routeIndexByToken.set(token, (routeIndex + 1) % driverRoute.length)

  return driverRoute[routeIndex]
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

      return HttpResponse.json(getPreview(token))
    },
  ),

  http.post(
    `${apiBaseUrl}/api/customer/confirmations/:token/confirm`,
    async ({ params }) => {
      await delay(300)
      statusByToken.set(getToken(params.token), 'CONFIRMED')

      return new HttpResponse(null, { status: 204 })
    },
  ),

  http.post(
    `${apiBaseUrl}/api/customer/confirmations/:token/reject`,
    async ({ params }) => {
      await delay(300)
      statusByToken.set(getToken(params.token), 'REJECTED')

      return new HttpResponse(null, { status: 204 })
    },
  ),

  http.get(
    `${apiBaseUrl}/api/customer/confirmations/:token/tracking-info`,
    async ({ params }) => {
      await delay(300)

      return HttpResponse.json(getTrackingInfo(getToken(params.token)))
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

      return HttpResponse.json(getDriverLocation(token))
    },
  ),

  ...dashboardHandlers,
]
