import { delay, http, HttpResponse } from 'msw'
import type { OrderSummary, ToursPage } from '../types/dashboard'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

function dateAfter(days: number) {
  const date = new Date()
  date.setDate(date.getDate() + days)

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function hoursAfter(hours: number) {
  return new Date(Date.now() + hours * 60 * 60 * 1000).toISOString()
}

const orders: OrderSummary[] = [
  {
    externalOrderId: 'DEMO-1001',
    customerName: 'Max Müller',
    deliveryAddress: 'Domstraße 40, 97070 Würzburg',
    deliveryWindowStart: '08:00:00',
    deliveryWindowEnd: '09:30:00',
    communicationChannel: 'EMAIL',
    confirmationStatus: 'SENT',
    expiresAt: hoursAfter(5),
  },
  {
    externalOrderId: 'DEMO-1002',
    customerName: 'Sabine Schneider',
    deliveryAddress: 'Theaterstraße 11, 97070 Würzburg',
    deliveryWindowStart: '10:00:00',
    deliveryWindowEnd: '11:00:00',
    communicationChannel: 'SMS',
    confirmationStatus: 'CONFIRMED',
    expiresAt: hoursAfter(2),
  },
  {
    externalOrderId: 'DEMO-1003',
    customerName: 'Thomas Weber',
    deliveryAddress: 'Bismarckstraße 14, 97080 Würzburg',
    deliveryWindowStart: '12:30:00',
    deliveryWindowEnd: '14:00:00',
    communicationChannel: 'EMAIL',
    confirmationStatus: 'REJECTED',
    expiresAt: hoursAfter(9),
  },
  {
    externalOrderId: 'DEMO-1004',
    customerName: 'Petra Hofmann',
    deliveryAddress: 'Mainaustraße 27, 97082 Würzburg',
    deliveryWindowStart: '15:00:00',
    deliveryWindowEnd: '16:30:00',
    communicationChannel: 'SMS',
    confirmationStatus: 'NO_RESPONSE',
    expiresAt: hoursAfter(-3),
  },
]

const toursPage: ToursPage = {
  items: [
    {
      tourNumber: 'T-17',
      vehicleLicensePlate: 'WÜ-AB 417',
      deliveryDate: dateAfter(1),
      statusCounts: {
        sent: 1,
        confirmed: 1,
        rejected: 1,
        noResponse: 0,
      },
      orders: orders.slice(0, 3),
    },
    {
      tourNumber: 'T-21',
      vehicleLicensePlate: 'WÜ-CD 221',
      deliveryDate: dateAfter(2),
      statusCounts: {
        sent: 0,
        confirmed: 0,
        rejected: 0,
        noResponse: 1,
      },
      orders: orders.slice(3),
    },
  ],
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
}

export const dashboardHandlers = [
  http.get(`${apiBaseUrl}/api/dispo/dashboard/tours`, async () => {
    await delay(300)

    return HttpResponse.json(toursPage)
  }),
]
