import type {
  ConfirmationRequestStatus,
  ConfirmationStatus,
  OrderConfirmationStatus,
} from './confirmation'

export type CommunicationChannel = 'EMAIL' | 'SMS' | 'WHATSAPP'

export type DashboardFilters = {
  search: string
  tourNumbers: string[]
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

export type CustomerResponse = {
  responseType: 'CONFIRM' | 'REJECT'
  comment: string | null
  receivedAt: string
}

export type ConfirmationRequestHistoryItem = {
  requestId: number
  communicationChannel: CommunicationChannel
  deliveryDate: string
  deliveryWindowStart: string
  deliveryWindowEnd: string
  sentAt: string | null
  expiresAt: string | null
  responseDeadlineHours: number
  active: boolean
  status: ConfirmationRequestStatus
  customerResponse: CustomerResponse | null
}

export type OrderInformation = {
  externalOrderId: string
  customerName: string
  customerEmail: string | null
  customerPhoneNumber: string | null
  deliveryAddress: string
  product: string
  quantityLiters: number
  priceDisplayText: string | null
  tourNumber: string
  vehicleLicensePlate: string
  confirmationStatus: OrderConfirmationStatus
}

export type OrderDetail = {
  order: OrderInformation
  currentRequest: ConfirmationRequestHistoryItem | null
  previousRequests: ConfirmationRequestHistoryItem[]
}

export type ResendConfirmationInput = {
  communicationChannel: CommunicationChannel
  responseDeadlineHours: number
}

export type ResendConfirmationResult = {
  externalOrderId: string
  confirmationStatus: OrderConfirmationStatus
}
