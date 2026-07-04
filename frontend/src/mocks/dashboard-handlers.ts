import { delay, http, HttpResponse } from 'msw'
import type { DashboardConfirmation } from '../types/dashboard'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

function addHours(date: Date, hours: number) {
  return new Date(date.getTime() + hours * 60 * 60 * 1000).toISOString()
}

function getMockDashboardConfirmations(): DashboardConfirmation[] {
  const now = new Date()

  return [
    {
      externalOrderId: 'A-3002',
      customerName: 'Max Müller',
      deliveryDate: '2026-07-03',
      deliveryWindowStart: '10:00:00',
      deliveryWindowEnd: '11:00:00',
      confirmationStatus: 'SENT',
      sentAt: addHours(now, -18),
      expiresAt: addHours(now, 6),
    },
    {
      externalOrderId: 'A-3003',
      customerName: 'Sabine Schneider',
      deliveryDate: '2026-07-03',
      deliveryWindowStart: '12:00:00',
      deliveryWindowEnd: '13:30:00',
      confirmationStatus: 'CONFIRMED',
      sentAt: addHours(now, -22),
      expiresAt: addHours(now, 2),
    },
    {
      externalOrderId: 'A-3004',
      customerName: 'Thomas Weber',
      deliveryDate: '2026-07-04',
      deliveryWindowStart: '08:30:00',
      deliveryWindowEnd: '10:00:00',
      confirmationStatus: 'REJECTED',
      sentAt: addHours(now, -8),
      expiresAt: addHours(now, 16),
    },
    {
      externalOrderId: 'A-3005',
      customerName: 'Petra Hofmann',
      deliveryDate: '2026-07-04',
      deliveryWindowStart: '14:00:00',
      deliveryWindowEnd: '15:00:00',
      confirmationStatus: 'NO_RESPONSE',
      sentAt: addHours(now, -30),
      expiresAt: addHours(now, -6),
    },
    {
      externalOrderId: 'A-3006',
      customerName: 'Andreas Krüger',
      deliveryDate: '2026-07-05',
      deliveryWindowStart: '09:00:00',
      deliveryWindowEnd: '10:30:00',
      confirmationStatus: 'SENT',
      sentAt: addHours(now, -3),
      expiresAt: addHours(now, 21),
    },
    {
      externalOrderId: 'A-3007',
      customerName: 'Nina Bauer',
      deliveryDate: '2026-07-05',
      deliveryWindowStart: '11:00:00',
      deliveryWindowEnd: '12:30:00',
      confirmationStatus: 'CONFIRMED',
      sentAt: addHours(now, -20),
      expiresAt: addHours(now, 4),
    },
  ]
}

export const dashboardHandlers = [
  http.get(`${apiBaseUrl}/api/dashboard/confirmations`, async () => {
    await delay(300)

    return HttpResponse.json(getMockDashboardConfirmations())
  }),
]
