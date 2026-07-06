export type ConfirmationCaseType = 'abgelehnt' | 'keine_rueckmeldung'

export type ConfirmationCaseStatus = 'open' | 'resolved'

export type CommunicationChannel = 'email' | 'sms' | 'whatsapp'

export type ConfirmationCaseSummary = {
  orderId: string
  customerName: string
  deliveryAddress: string
  deliveryDate: string
  deliveryWindowStart: string
  deliveryWindowEnd: string
  tourLabel: string
  problemType: ConfirmationCaseType
  sentAt: string
  timeSinceSentLabel: string
  customerComment: string | null
  status: ConfirmationCaseStatus
}

export type RequestSnapshot = {
  channel: CommunicationChannel
  deliveryDate: string
  deliveryWindowStart: string
  deliveryWindowEnd: string
  responseDeadlineHours: number
  sentAt: string
  validUntil: string
  active: boolean
}

export type CustomerResponse = {
  answerLabel: string
  receivedAt: string
  customerComment: string | null
}

export type ConfirmationCaseHistoryEvent = {
  dateLabel: string
  text: string
  type: 'sent' | 'rejected' | 'warning' | 'current'
  isCurrent?: boolean
}

export type ConfirmationCaseDetail = ConfirmationCaseSummary & {
  product: string
  quantityLiters: number
  priceDisplayText: string | null
  previousRequest: RequestSnapshot
  customerResponse: CustomerResponse | null
  recommendedDeliveryDate: string
  recommendedWindowStart: string
  recommendedWindowEnd: string
  defaultResponseDeadlineHours: number
  history: ConfirmationCaseHistoryEvent[]
}

export type NewTimeWindowRequest = {
  channel: CommunicationChannel
  deliveryDate: string
  deliveryWindowStart: string
  deliveryWindowEnd: string
  responseDeadlineHours: number
  dispatcherNote?: string
}
