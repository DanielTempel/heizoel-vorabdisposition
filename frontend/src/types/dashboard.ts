import type { ConfirmationStatus } from './confirmation'

export type DashboardConfirmation = {
  externalOrderId: string
  customerName: string
  deliveryDate: string
  deliveryWindowStart: string
  deliveryWindowEnd: string
  confirmationStatus: ConfirmationStatus
  sentAt: string | null
  expiresAt: string | null
}
