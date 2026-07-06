import type {
  CommunicationChannel,
  ConfirmationCaseType,
} from '@/types/confirmation-cases'

export type PageStatus = 'loading' | 'ready' | 'submitting' | 'success' | 'error'
export type ConfirmationCaseFilter = 'alle' | ConfirmationCaseType

export const inputClassName =
  'h-12 w-full rounded-2xl border border-slate-200 bg-white px-4 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus-visible:border-slate-400 focus-visible:ring-4 focus-visible:ring-slate-200/70'

export function channelLabel(channel: CommunicationChannel) {
  if (channel === 'sms') {
    return 'SMS'
  }

  if (channel === 'whatsapp') {
    return 'WhatsApp'
  }

  return 'E-Mail'
}

export function confirmationCaseLabel(type: ConfirmationCaseType) {
  return type === 'abgelehnt' ? 'Abgelehnt' : 'Keine Rückmeldung'
}

export function formatDate(value: string) {
  return new Intl.DateTimeFormat('de-DE', {
    dateStyle: 'medium',
  }).format(new Date(value))
}

export function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('de-DE', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

export function formatTime(value: string) {
  return value.slice(0, 5)
}

export function parseLocalDateTime(date: string, time: string) {
  const value = new Date(`${date}T${time}`)

  if (Number.isNaN(value.getTime())) {
    return null
  }

  return value
}

export function formatDateInputValue(value: Date) {
  return value.toLocaleDateString('en-CA')
}

export function formatTimeInputValue(value: Date) {
  return value.toTimeString().slice(0, 5)
}
