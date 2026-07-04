import { delay, http, HttpResponse } from 'msw'
import type {
  ConfirmationStatus,
  CustomerConfirmationPreview,
} from '../types/confirmation'
import type {
  NewTimeWindowRequest,
  ConfirmationCaseDetail,
  ConfirmationCaseStatus,
  ConfirmationCaseSummary,
} from '../types/confirmation-cases'
import type { DriverLocation, TrackingInfo } from '../types/tracking'
import { dashboardHandlers } from './dashboard-handlers'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

const defaultMockConfirmationPreview: CustomerConfirmationPreview = {
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

const rejectedMockConfirmationPreview: CustomerConfirmationPreview = {
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

const rejectedEveningMockConfirmationPreview: CustomerConfirmationPreview = {
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

const noResponseMockConfirmationPreview: CustomerConfirmationPreview = {
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

const dashboardMockConfirmationPreviews: CustomerConfirmationPreview[] = [
  {
    externalOrderId: 'A-3002',
    customerName: 'Max Müller',
    deliveryAddress: 'Domstrasse 40, 97070 Würzburg',
    product: 'Heizöl Standard',
    quantityLiters: 3000,
    deliveryDate: '2026-07-03',
    deliveryWindowStart: '10:00:00',
    deliveryWindowEnd: '11:00:00',
    priceDisplayText: '100 EUR',
    confirmationStatus: 'SENT',
  },
  {
    externalOrderId: 'A-3003',
    customerName: 'Sabine Schneider',
    deliveryAddress: 'Theaterstraße 11, 97070 Würzburg',
    product: 'Heizöl Premium',
    quantityLiters: 2800,
    deliveryDate: '2026-07-03',
    deliveryWindowStart: '12:00:00',
    deliveryWindowEnd: '13:30:00',
    priceDisplayText: '114,20 EUR / 100 Liter',
    confirmationStatus: 'CONFIRMED',
  },
  {
    externalOrderId: 'A-3004',
    customerName: 'Thomas Weber',
    deliveryAddress: 'Bismarckstraße 14, 97080 Würzburg',
    product: 'Heizöl Standard',
    quantityLiters: 3200,
    deliveryDate: '2026-07-04',
    deliveryWindowStart: '08:30:00',
    deliveryWindowEnd: '10:00:00',
    priceDisplayText: '108,70 EUR / 100 Liter',
    confirmationStatus: 'REJECTED',
  },
  {
    externalOrderId: 'A-3005',
    customerName: 'Petra Hofmann',
    deliveryAddress: 'Mainaustraße 27, 97082 Würzburg',
    product: 'Heizöl Eco',
    quantityLiters: 2500,
    deliveryDate: '2026-07-04',
    deliveryWindowStart: '14:00:00',
    deliveryWindowEnd: '15:00:00',
    priceDisplayText: '105,90 EUR / 100 Liter',
    confirmationStatus: 'NO_RESPONSE',
  },
  {
    externalOrderId: 'A-3006',
    customerName: 'Andreas Krüger',
    deliveryAddress: 'Rottendorfer Straße 96, 97074 Würzburg',
    product: 'Heizöl Premium',
    quantityLiters: 3400,
    deliveryDate: '2026-07-05',
    deliveryWindowStart: '09:00:00',
    deliveryWindowEnd: '10:30:00',
    priceDisplayText: '116,80 EUR / 100 Liter',
    confirmationStatus: 'SENT',
  },
  {
    externalOrderId: 'A-3007',
    customerName: 'Nina Bauer',
    deliveryAddress: 'Leistenstraße 55, 97082 Würzburg',
    product: 'Heizöl Standard',
    quantityLiters: 2600,
    deliveryDate: '2026-07-05',
    deliveryWindowStart: '11:00:00',
    deliveryWindowEnd: '12:30:00',
    priceDisplayText: '107,40 EUR / 100 Liter',
    confirmationStatus: 'CONFIRMED',
  },
]

const dashboardMockConfirmationPreviewByOrderId = new Map(
  dashboardMockConfirmationPreviews.map((preview) => [
    preview.externalOrderId,
    preview,
  ]),
)

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

type MockConfirmationCaseRecord = ConfirmationCaseDetail & {
  status: ConfirmationCaseStatus
}

const initialConfirmationCaseRecords: MockConfirmationCaseRecord[] = [
  {
    orderId: rejectedMockConfirmationPreview.externalOrderId,
    customerName: rejectedMockConfirmationPreview.customerName,
    deliveryAddress: rejectedMockConfirmationPreview.deliveryAddress,
    deliveryDate: rejectedMockConfirmationPreview.deliveryDate,
    deliveryWindowStart: rejectedMockConfirmationPreview.deliveryWindowStart,
    deliveryWindowEnd: rejectedMockConfirmationPreview.deliveryWindowEnd,
    tourLabel: 'Tour 17',
    problemType: 'abgelehnt',
    sentAt: '2026-06-18T11:42:00',
    timeSinceSentLabel: 'vor 2 Tagen',
    customerComment: 'Bitte erst ab 18 Uhr. Vorher ist niemand zuhause.',
    status: 'open',
    product: rejectedMockConfirmationPreview.product,
    quantityLiters: rejectedMockConfirmationPreview.quantityLiters,
    priceDisplayText: rejectedMockConfirmationPreview.priceDisplayText,
    previousRequest: {
      channel: 'email',
      deliveryDate: rejectedMockConfirmationPreview.deliveryDate,
      deliveryWindowStart: rejectedMockConfirmationPreview.deliveryWindowStart,
      deliveryWindowEnd: rejectedMockConfirmationPreview.deliveryWindowEnd,
      responseDeadlineHours: 1,
      sentAt: '2026-06-18T11:42:00',
      validUntil: '2026-06-18T12:42:00',
      active: false,
    },
    customerResponse: {
      answerLabel: 'Abgelehnt',
      receivedAt: '2026-06-18T11:55:00',
      customerComment: 'Bitte erst ab 18 Uhr. Vorher ist niemand zuhause.',
    },
    recommendedDeliveryDate: '2026-06-26',
    recommendedWindowStart: '18:00:00',
    recommendedWindowEnd: '19:00:00',
    defaultResponseDeadlineHours: 12,
    history: [
      {
        dateLabel: '18.06.2026, 11:42',
        text: 'Anfrage per E-Mail versendet',
        type: 'sent',
      },
      {
        dateLabel: '18.06.2026, 11:55',
        text: 'Kunde hat das Lieferfenster abgelehnt',
        type: 'rejected',
      },
      {
        dateLabel: 'Jetzt',
        text: 'Neue Rueckbestaetigung wird vorbereitet',
        type: 'current',
        isCurrent: true,
      },
    ],
  },
  {
    orderId: rejectedEveningMockConfirmationPreview.externalOrderId,
    customerName: rejectedEveningMockConfirmationPreview.customerName,
    deliveryAddress: rejectedEveningMockConfirmationPreview.deliveryAddress,
    deliveryDate: rejectedEveningMockConfirmationPreview.deliveryDate,
    deliveryWindowStart:
      rejectedEveningMockConfirmationPreview.deliveryWindowStart,
    deliveryWindowEnd: rejectedEveningMockConfirmationPreview.deliveryWindowEnd,
    tourLabel: 'Tour 17',
    problemType: 'abgelehnt',
    sentAt: '2026-06-18T10:15:00',
    timeSinceSentLabel: 'vor 2 Tagen',
    customerComment: 'Termin passt nicht, bitte spaeter am Abend anbieten.',
    status: 'open',
    product: rejectedEveningMockConfirmationPreview.product,
    quantityLiters: rejectedEveningMockConfirmationPreview.quantityLiters,
    priceDisplayText: rejectedEveningMockConfirmationPreview.priceDisplayText,
    previousRequest: {
      channel: 'email',
      deliveryDate: rejectedEveningMockConfirmationPreview.deliveryDate,
      deliveryWindowStart:
        rejectedEveningMockConfirmationPreview.deliveryWindowStart,
      deliveryWindowEnd: rejectedEveningMockConfirmationPreview.deliveryWindowEnd,
      responseDeadlineHours: 4,
      sentAt: '2026-06-18T10:15:00',
      validUntil: '2026-06-18T14:15:00',
      active: false,
    },
    customerResponse: {
      answerLabel: 'Abgelehnt',
      receivedAt: '2026-06-18T12:03:00',
      customerComment: 'Termin passt nicht, bitte spaeter am Abend anbieten.',
    },
    recommendedDeliveryDate: '2026-06-26',
    recommendedWindowStart: '18:00:00',
    recommendedWindowEnd: '19:30:00',
    defaultResponseDeadlineHours: 12,
    history: [
      {
        dateLabel: '18.06.2026, 10:15',
        text: 'Anfrage per E-Mail versendet',
        type: 'sent',
      },
      {
        dateLabel: '18.06.2026, 12:03',
        text: 'Kunde hat das Lieferfenster abgelehnt',
        type: 'rejected',
      },
      {
        dateLabel: 'Jetzt',
        text: 'Disponent prueft Alternativfenster',
        type: 'current',
        isCurrent: true,
      },
    ],
  },
  {
    orderId: noResponseMockConfirmationPreview.externalOrderId,
    customerName: noResponseMockConfirmationPreview.customerName,
    deliveryAddress: noResponseMockConfirmationPreview.deliveryAddress,
    deliveryDate: noResponseMockConfirmationPreview.deliveryDate,
    deliveryWindowStart: noResponseMockConfirmationPreview.deliveryWindowStart,
    deliveryWindowEnd: noResponseMockConfirmationPreview.deliveryWindowEnd,
    tourLabel: 'Tour 20',
    problemType: 'keine_rueckmeldung',
    sentAt: '2026-06-17T15:45:00',
    timeSinceSentLabel: 'vor 3 Tagen (> 24h)',
    customerComment: null,
    status: 'open',
    product: noResponseMockConfirmationPreview.product,
    quantityLiters: noResponseMockConfirmationPreview.quantityLiters,
    priceDisplayText: noResponseMockConfirmationPreview.priceDisplayText,
    previousRequest: {
      channel: 'sms',
      deliveryDate: noResponseMockConfirmationPreview.deliveryDate,
      deliveryWindowStart: noResponseMockConfirmationPreview.deliveryWindowStart,
      deliveryWindowEnd: noResponseMockConfirmationPreview.deliveryWindowEnd,
      responseDeadlineHours: 24,
      sentAt: '2026-06-17T15:45:00',
      validUntil: '2026-06-18T15:45:00',
      active: false,
    },
    customerResponse: null,
    recommendedDeliveryDate: '2026-06-26',
    recommendedWindowStart: '09:30:00',
    recommendedWindowEnd: '11:00:00',
    defaultResponseDeadlineHours: 8,
    history: [
      {
        dateLabel: '17.06.2026, 15:45',
        text: 'Anfrage per SMS versendet',
        type: 'sent',
      },
      {
        dateLabel: '18.06.2026, 15:45',
        text: 'Antwortfrist ohne Rueckmeldung abgelaufen',
        type: 'warning',
      },
      {
        dateLabel: 'Jetzt',
        text: 'Neue Rueckbestaetigung wird vorbereitet',
        type: 'current',
        isCurrent: true,
      },
    ],
  },
]

const mockConfirmationCaseRecords = initialConfirmationCaseRecords.map((record) => ({
  ...record,
  previousRequest: { ...record.previousRequest },
  customerResponse: record.customerResponse
    ? { ...record.customerResponse }
    : null,
  history: record.history.map((event) => ({ ...event })),
}))

function getBaseConfirmationPreview(token: string) {
  const dashboardPreview = dashboardMockConfirmationPreviewByOrderId.get(token)

  if (dashboardPreview) {
    return dashboardPreview
  }

  if (token === 'mock-rejected') {
    return rejectedMockConfirmationPreview
  }

  if (token === 'mock-rejected-evening') {
    return rejectedEveningMockConfirmationPreview
  }

  if (token === 'mock-no-response' || token === 'mock-expired') {
    return noResponseMockConfirmationPreview
  }

  return defaultMockConfirmationPreview
}

function getToken(tokenParam: string | readonly string[] | undefined) {
  if (Array.isArray(tokenParam)) {
    return tokenParam[0] ?? 'mock-token'
  }

  return tokenParam ?? 'mock-token'
}

function getInitialMockStatus(token: string): ConfirmationStatus {
  const dashboardPreview = dashboardMockConfirmationPreviewByOrderId.get(token)

  if (dashboardPreview) {
    return dashboardPreview.confirmationStatus
  }

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
  const basePreview = getBaseConfirmationPreview(token)

  return {
    ...basePreview,
    externalOrderId:
      token.startsWith('mock-') || dashboardMockConfirmationPreviewByOrderId.has(token)
        ? basePreview.externalOrderId
        : defaultMockConfirmationPreview.externalOrderId,
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

function formatDateTimeLabel(value: string) {
  return new Intl.DateTimeFormat('de-DE', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

function getConfirmationCaseSummary(
  record: MockConfirmationCaseRecord,
): ConfirmationCaseSummary {
  return {
    orderId: record.orderId,
    customerName: record.customerName,
    deliveryAddress: record.deliveryAddress,
    deliveryDate: record.deliveryDate,
    deliveryWindowStart: record.deliveryWindowStart,
    deliveryWindowEnd: record.deliveryWindowEnd,
    tourLabel: record.tourLabel,
    problemType: record.problemType,
    sentAt: record.sentAt,
    timeSinceSentLabel: record.timeSinceSentLabel,
    customerComment: record.customerComment,
    status: record.status,
  }
}

function getConfirmationCaseRecord(orderId: string) {
  return mockConfirmationCaseRecords.find((record) => record.orderId === orderId)
}

function buildConfirmationCaseDetailFromConfirmation(
  confirmation: CustomerConfirmationPreview,
): ConfirmationCaseDetail {
  const template =
    confirmation.confirmationStatus === 'REJECTED'
      ? mockConfirmationCaseRecords.find((record) => record.problemType === 'abgelehnt')
      : mockConfirmationCaseRecords.find(
          (record) => record.problemType === 'keine_rueckmeldung',
        )

  const baseTemplate = template ?? mockConfirmationCaseRecords[0]
  const hasCustomerResponse = confirmation.confirmationStatus === 'REJECTED'
  const problemType =
    confirmation.confirmationStatus === 'REJECTED'
      ? 'abgelehnt'
      : 'keine_rueckmeldung'

  return {
    ...baseTemplate,
    orderId: confirmation.externalOrderId,
    customerName: confirmation.customerName,
    deliveryAddress: confirmation.deliveryAddress,
    deliveryDate: confirmation.deliveryDate,
    deliveryWindowStart: confirmation.deliveryWindowStart,
    deliveryWindowEnd: confirmation.deliveryWindowEnd,
    product: confirmation.product,
    quantityLiters: confirmation.quantityLiters,
    priceDisplayText: confirmation.priceDisplayText,
    problemType,
    customerComment: hasCustomerResponse
      ? 'Bitte neues Zeitfenster am Abend anbieten.'
      : null,
    previousRequest: {
      ...baseTemplate.previousRequest,
      deliveryDate: confirmation.deliveryDate,
      deliveryWindowStart: confirmation.deliveryWindowStart,
      deliveryWindowEnd: confirmation.deliveryWindowEnd,
      active: false,
    },
    customerResponse: hasCustomerResponse
      ? {
          answerLabel: 'Abgelehnt',
          receivedAt: '2026-07-03T11:55:00',
          customerComment: 'Bitte neues Zeitfenster am Abend anbieten.',
        }
      : null,
    recommendedDeliveryDate: confirmation.deliveryDate,
    recommendedWindowStart:
      confirmation.deliveryWindowStart.slice(0, 2) >= '18'
        ? confirmation.deliveryWindowStart
        : confirmation.confirmationStatus === 'REJECTED'
          ? '18:00:00'
          : confirmation.deliveryWindowStart,
    recommendedWindowEnd:
      confirmation.deliveryWindowEnd.slice(0, 2) >= '19'
        ? confirmation.deliveryWindowEnd
        : confirmation.confirmationStatus === 'REJECTED'
          ? '19:00:00'
          : confirmation.deliveryWindowEnd,
    history: hasCustomerResponse
      ? [
          {
            dateLabel: '03.07.2026, 11:42',
            text: 'Anfrage per E-Mail versendet',
            type: 'sent',
          },
          {
            dateLabel: '03.07.2026, 11:55',
            text: 'Kunde hat das Lieferfenster abgelehnt',
            type: 'rejected',
          },
          {
            dateLabel: 'Jetzt',
            text: 'Neue Rueckbestaetigung wird vorbereitet',
            type: 'current',
            isCurrent: true,
          },
        ]
      : [
          {
            dateLabel: '03.07.2026, 11:42',
            text: 'Anfrage per E-Mail versendet',
            type: 'sent',
          },
          {
            dateLabel:
              confirmation.confirmationStatus === 'CONFIRMED'
                ? '03.07.2026, 12:05'
                : '04.07.2026, 11:42',
            text:
              confirmation.confirmationStatus === 'CONFIRMED'
                ? 'Kunde hat das Lieferfenster bestaetigt'
                : 'Antwortfrist ohne Rueckmeldung abgelaufen',
            type:
              confirmation.confirmationStatus === 'CONFIRMED'
                ? 'sent'
                : 'warning',
          },
          {
            dateLabel: 'Jetzt',
            text:
              confirmation.confirmationStatus === 'CONFIRMED'
                ? 'Lieferung ist erfolgreich eingeplant'
                : 'Neue Rueckbestaetigung wird vorbereitet',
            type: 'current',
            isCurrent: true,
          },
        ],
  }
}

function ensureConfirmationCaseRecord(orderId: string) {
  const existingRecord = getConfirmationCaseRecord(orderId)

  if (existingRecord) {
    return existingRecord
  }

  const preview = dashboardMockConfirmationPreviewByOrderId.get(orderId)

  if (!preview) {
    return null
  }

  const derivedRecord = buildConfirmationCaseDetailFromConfirmation(preview)
  mockConfirmationCaseRecords.push(derivedRecord)

  return derivedRecord
}

function markConfirmationCaseResolved(
  orderId: string,
  request?: NewTimeWindowRequest,
) {
  const record = getConfirmationCaseRecord(orderId)

  if (!record) {
    return null
  }

  record.status = 'resolved'
  record.previousRequest.active = false

  if (request) {
    record.history = [
      {
        dateLabel: formatDateTimeLabel(new Date().toISOString()),
        text: `Neues Lieferfenster ${request.deliveryWindowStart.slice(0, 5)} - ${request.deliveryWindowEnd.slice(0, 5)} versendet`,
        type: 'sent',
      },
      ...record.history.map((event) => ({ ...event, isCurrent: false })),
    ]
  }

  return record
}

export const handlers = [
  http.get(`${apiBaseUrl}/api/confirmation-cases`, async () => {
    await delay(250)

    return HttpResponse.json(
      mockConfirmationCaseRecords
        .filter((record) => record.status === 'open')
        .map(getConfirmationCaseSummary),
    )
  }),

  http.get(`${apiBaseUrl}/api/confirmation-cases/:orderId`, async ({ params }) => {
    await delay(250)

    const orderId = getToken(params.orderId)
    const record = ensureConfirmationCaseRecord(orderId)

    if (!record) {
      return new HttpResponse(null, { status: 404 })
    }

    return HttpResponse.json(record satisfies ConfirmationCaseDetail)
  }),

  http.post(
    `${apiBaseUrl}/api/confirmation-cases/:orderId/reschedule`,
    async ({ params, request }) => {
      await delay(350)

      const orderId = getToken(params.orderId)
      const payload = (await request.json()) as NewTimeWindowRequest
      ensureConfirmationCaseRecord(orderId)
      const updatedRecord = markConfirmationCaseResolved(orderId, payload)

      if (!updatedRecord) {
        return new HttpResponse(null, { status: 404 })
      }

      return new HttpResponse(null, { status: 204 })
    },
  ),

  http.post(
    `${apiBaseUrl}/api/confirmation-cases/:orderId/resolve`,
    async ({ params }) => {
      await delay(200)

      const orderId = getToken(params.orderId)
      ensureConfirmationCaseRecord(orderId)
      const updatedRecord = markConfirmationCaseResolved(orderId)

      if (!updatedRecord) {
        return new HttpResponse(null, { status: 404 })
      }

      return new HttpResponse(null, { status: 204 })
    },
  ),

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
