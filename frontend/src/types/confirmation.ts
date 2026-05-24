export type CustomerConfirmationPreview = {
  externalOrderId: string
  customerName: string
  deliveryAddress: string
  product: string
  quantityLiters: number
  deliveryDate: string
  deliveryWindowStart: string
  deliveryWindowEnd: string
}

export type CustomerAnswerRequest = {
  customerComment?: string
}

export type CustomerAnswerType = 'confirm' | 'reject'
