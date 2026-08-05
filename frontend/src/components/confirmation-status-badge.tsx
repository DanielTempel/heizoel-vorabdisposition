import { cn } from '@/lib/utils'
import type { ConfirmationStatus } from '@/types/confirmation'

type ConfirmationStatusBadgeProps = {
  status: ConfirmationStatus
  count?: number
  className?: string
}

const statusPresentation = {
  SENT: {
    label: 'Versendet',
    className: 'border-blue-200 bg-blue-50 text-blue-800',
  },
  CONFIRMED: {
    label: 'Bestätigt',
    className: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  },
  REJECTED: {
    label: 'Abgelehnt',
    className: 'border-red-200 bg-red-50 text-red-800',
  },
  NO_RESPONSE: {
    label: 'Keine Rückmeldung',
    className: 'border-amber-200 bg-amber-50 text-amber-900',
  },
} satisfies Record<
  ConfirmationStatus,
  { label: string; className: string }
>

export function ConfirmationStatusBadge({
  status,
  count,
  className,
}: ConfirmationStatusBadgeProps) {
  const presentation = statusPresentation[status]

  return (
    <span
      className={cn(
        'inline-flex rounded-full border px-2 py-0.5 text-xs font-medium',
        presentation.className,
        className,
      )}
      data-status={status}
    >
      {presentation.label}
      {count === undefined ? null : `: ${count}`}
    </span>
  )
}
