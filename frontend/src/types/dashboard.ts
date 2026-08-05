import type { ConfirmationStatus } from './confirmation'

export type CommunicationChannel = 'EMAIL' | 'SMS' | 'WHATSAPP'

export type DashboardFilters = {
  search: string
  statuses: ConfirmationStatus[]
  dateFrom: string
  dateTo: string
}

export type OrderSummary = {
  externalOrderId: string
  customerName: string
  deliveryAddress: string
  deliveryWindowStart: string
  deliveryWindowEnd: string
  communicationChannel: CommunicationChannel
  confirmationStatus: ConfirmationStatus
  expiresAt: string | null
}

export type TourStatusCounts = {
  sent: number
  confirmed: number
  rejected: number
  noResponse: number
}

export type TourSummary = {
  tourNumber: string
  vehicleLicensePlate: string
  deliveryDate: string
  statusCounts: TourStatusCounts
  orders: OrderSummary[]
}

export type ToursPage = {
  items: TourSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
