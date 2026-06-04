export type CustomerConfirmationPreview = {
  externalOrderId: string
  customerName: string
  deliveryAddress: string
  locationX: number
  locationY: number
  targetLocationX: number
  targetLocationY: number
  product: string
  quantityLiters: number
  deliveryDate: string
  deliveryWindowStart: string
  deliveryWindowEnd: string
  confirmationStatus: ConfirmationStatus
}

export type ConfirmationStatus =
  | 'SENT'
  | 'CONFIRMED'
  | 'REJECTED'
  | 'NO_RESPONSE'

export type CustomerAnswerRequest = {
  customerComment?: string
}

export type CustomerAnswerType = 'confirm' | 'reject'
