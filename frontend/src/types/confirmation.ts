export type CustomerConfirmationPreview = {
  externalOrderId: string
  customerName: string
  deliveryAddress: string
  product: string
  quantityLiters: number
  deliveryDate: string
  deliveryWindowStart: string
  deliveryWindowEnd: string
  priceDisplayText: string | null
  confirmationStatus: ConfirmationStatus
}

export type ConfirmationStatus =
  | 'SENT'
  | 'CONFIRMED'
  | 'REJECTED'
  | 'NO_RESPONSE'

export type OrderConfirmationStatus = ConfirmationStatus | 'OPEN'

export type ConfirmationRequestStatus =
  | ConfirmationStatus
  | 'PENDING'
  | 'FAILED'

export type ConfirmationDisplayStatus =
  | OrderConfirmationStatus
  | ConfirmationRequestStatus

export type CustomerAnswerRequest = {
  responseType: 'CONFIRM' | 'REJECT'
  customerComment?: string
}

export type CustomerAnswerType = 'confirm' | 'reject'
